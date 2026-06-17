import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/player.dart';
import '../data/models/team.dart';
import '../data/models/match.dart';
import '../data/models/delivery.dart';
import '../data/repository/cricket_repository.dart';
import '../domain/scoring_engine.dart';

// ─── Repository Provider ───────────────────────────────────────────────────────
final repositoryProvider = Provider<CricketRepository>((ref) => CricketRepository());

// ─── Players Stream ───────────────────────────────────────────────────────────
final playersProvider = StreamProvider<List<PlayerModel>>((ref) async* {
  final repo = ref.watch(repositoryProvider);
  await repo.initialize();
  final initial = await repo.getAllPlayers();
  yield initial;
  yield* repo.players;
});

// ─── Teams Stream ─────────────────────────────────────────────────────────────
final teamsProvider = StreamProvider<List<TeamModel>>((ref) async* {
  final repo = ref.watch(repositoryProvider);
  final initial = await repo.getAllTeams();
  yield initial;
  yield* repo.teams;
});

// ─── Matches Stream ───────────────────────────────────────────────────────────
final matchesProvider = StreamProvider<List<MatchModel>>((ref) async* {
  final repo = ref.watch(repositoryProvider);
  final initial = await repo.getAllMatches();
  yield initial;
  yield* repo.matches;
});

// ─── Active Match ID ──────────────────────────────────────────────────────────
final activeMatchIdProvider = StateProvider<int?>((ref) => null);

// ─── Active Match State ───────────────────────────────────────────────────────
final activeMatchStateProvider = StreamProvider<ComputedMatchState?>((ref) async* {
  final matchId = ref.watch(activeMatchIdProvider);
  if (matchId == null) { yield null; return; }
  final repo = ref.watch(repositoryProvider);
  final players = await ref.read(playersProvider.future);
  final playersMap = {for (final p in players) p.id: p};

  // Watch match + deliveries and recompute on any change
  await for (final match in repo.watchMatch(matchId)) {
    if (match == null) { yield null; continue; }
    final deliveries = await repo.getDeliveriesForMatch(matchId);
    yield ScoringEngine.computeMatchState(match, deliveries, playersMap);
  }
});

// ─── Cricket Notifier (ViewModel equivalent) ──────────────────────────────────
final cricketNotifierProvider = NotifierProvider<CricketNotifier, void>(CricketNotifier.new);

class CricketNotifier extends Notifier<void> {
  @override
  void build() {}

  CricketRepository get _repo => ref.read(repositoryProvider);

  // ─── Players ─────────────────────────────────────────────
  Future<void> addPlayer(String name, String clubName, String role) async {
    await _repo.insertPlayer(PlayerModel(name: name, clubName: clubName, designRole: role));
  }

  Future<void> deletePlayer(int id) => _repo.deletePlayer(id);

  // ─── Match Creation ──────────────────────────────────────
  Future<int> createMatchWithCustomTeams({
    required String teamAName,
    required String teamBName,
    required String venue,
    required int overs,
    required int ballsPerOver,
    required List<PlayerModel> teamAPlayers,
    required List<PlayerModel> teamBPlayers,
    required String scheduledDate,
    required String scheduledTime,
    required bool tossWinnerIsTeamA,
    required String tossDecision,
  }) async {
    // Create/find teams
    final allTeams = await _repo.getAllTeams();
    int teamAId = allTeams.firstWhere((t) => t.name.toLowerCase() == teamAName.toLowerCase(), orElse: () => const TeamModel(id: -1, name: '')).id;
    if (teamAId <= 0) teamAId = await _repo.insertTeam(TeamModel(name: teamAName));

    int teamBId = allTeams.firstWhere((t) => t.name.toLowerCase() == teamBName.toLowerCase(), orElse: () => const TeamModel(id: -1, name: '')).id;
    if (teamBId <= 0) teamBId = await _repo.insertTeam(TeamModel(name: teamBName));

    // Upsert players and collect IDs
    final teamAIds = <int>[];
    for (final p in teamAPlayers) {
      final id = p.id > 0 ? p.id : await _repo.insertPlayer(p.copyWith(clubName: teamAName));
      teamAIds.add(id);
    }
    final teamBIds = <int>[];
    for (final p in teamBPlayers) {
      final id = p.id > 0 ? p.id : await _repo.insertPlayer(p.copyWith(clubName: teamBName));
      teamBIds.add(id);
    }

    // Determine first/second batting teams
    final tossWinnerId = tossWinnerIsTeamA ? teamAId : teamBId;
    final firstBatTeamId = tossDecision == 'BAT' ? tossWinnerId : (tossWinnerId == teamAId ? teamBId : teamAId);
    final secondBatTeamId = firstBatTeamId == teamAId ? teamBId : teamAId;

    final match = MatchModel(
      teamAId: teamAId, teamBId: teamBId,
      teamAName: teamAName, teamBName: teamBName,
      venue: venue.isNotEmpty ? venue : 'TBD',
      scheduledDate: scheduledDate, scheduledTime: scheduledTime,
      oversCount: overs, ballsPerOver: ballsPerOver,
      tossWinnerId: tossWinnerId, tossDecision: tossDecision,
      status: 'LIVE', currentInningsNo: 1,
      firstInningsTeamId: firstBatTeamId, secondInningsTeamId: secondBatTeamId,
      teamASquadIds: teamAIds.join(','), teamBSquadIds: teamBIds.join(','),
    );

    final newId = await _repo.insertMatch(match);
    ref.read(activeMatchIdProvider.notifier).state = newId;
    return newId;
  }

  void setActiveMatch(int matchId) {
    ref.read(activeMatchIdProvider.notifier).state = matchId;
  }

  // ─── Toss ────────────────────────────────────────────────
  Future<void> submitTossResult(int matchId, int tossWinnerId, String tossDecision) async {
    final match = await _repo.getMatchById(matchId);
    if (match == null) return;
    final firstBatTeamId = tossDecision == 'BAT' ? tossWinnerId : (tossWinnerId == match.teamAId ? match.teamBId : match.teamAId);
    final secondBatTeamId = firstBatTeamId == match.teamAId ? match.teamBId : match.teamAId;
    await _repo.updateMatch(match.copyWith(
      tossWinnerId: tossWinnerId, tossDecision: tossDecision,
      status: 'LIVE', currentInningsNo: 1,
      firstInningsTeamId: firstBatTeamId, secondInningsTeamId: secondBatTeamId,
    ));
  }

  // ─── Deliveries ──────────────────────────────────────────
  Future<void> recordDelivery({
    required int matchId,
    required ComputedMatchState state,
    required int runsSimple,
    required String extraType,
    required int extraRuns,
    required bool isWicket,
    required String dismissalType,
    required int dismissedBatsmanId,
    required String fielderName,
    required int strikerId,
    required int nonStrikerId,
    required int bowlerId,
  }) async {
    final innings = state.activeInnings;
    final playersMap = {for (final b in innings.batters) b.playerId: b.name};
    final bowlerName = innings.bowlers.firstWhere((b) => b.playerId == bowlerId, orElse: () => BowlerScore(playerId: bowlerId, name: '?')).name;

    final totalLegal = innings.ballsBowled;
    final overIdx = totalLegal ~/ state.match.ballsPerOver;
    final ballIdx = (totalLegal % state.match.ballsPerOver) + 1;

    final dismissedName = dismissedBatsmanId != 0
        ? (innings.batters.firstWhere((b) => b.playerId == dismissedBatsmanId, orElse: () => BatterScore(playerId: 0, name: '')).name)
        : (playersMap[strikerId] ?? '');

    final delivery = DeliveryModel(
      matchId: matchId,
      inningsNo: innings.inningsNo,
      overIndex: overIdx,
      ballIndexInOver: ballIdx,
      batsmanId: strikerId,
      batsmanName: playersMap[strikerId] ?? '',
      bowlerId: bowlerId,
      bowlerName: bowlerName,
      runsSimple: runsSimple,
      extraType: extraType,
      extraRuns: extraRuns,
      isWicket: isWicket,
      dismissalType: dismissalType,
      dismissedBatsmanId: isWicket ? (dismissedBatsmanId != 0 ? dismissedBatsmanId : strikerId) : 0,
      dismissedBatsmanName: isWicket ? dismissedName : '',
      fielderName: fielderName,
      timestamp: DateTime.now().millisecondsSinceEpoch,
    );
    await _repo.insertDelivery(delivery);

    // Check innings completion after recording
    await _checkAndTransitionInnings(matchId, state);
  }

  Future<void> _checkAndTransitionInnings(int matchId, ComputedMatchState oldState) async {
    final match = await _repo.getMatchById(matchId);
    if (match == null) return;
    final deliveries = await _repo.getDeliveriesForMatch(matchId);
    final players = await _repo.getAllPlayers();
    final playersMap = {for (final p in players) p.id: p};
    final newState = ScoringEngine.computeMatchState(match, deliveries, playersMap);

    if (newState.innings1.isCompleted && newState.innings2 == null && match.currentInningsNo == 1) {
      await _repo.updateMatch(match.copyWith(currentInningsNo: 2, status: 'INNINGS_BREAK'));
      await Future.delayed(const Duration(milliseconds: 300));
      await _repo.updateMatch((await _repo.getMatchById(matchId))!.copyWith(status: 'LIVE'));
    } else if (newState.isCompleted && match.status != 'COMPLETED') {
      await _repo.updateMatch(match.copyWith(
        status: 'COMPLETED',
        winnerId: newState.winnerName == match.teamAName ? match.teamAId : newState.winnerName == match.teamBName ? match.teamBId : 0,
        winningMargin: newState.resultSummary,
      ));
    }
  }

  Future<void> undoLastBall(int matchId) => _repo.deleteLastDelivery(matchId);

  // ─── Match Finalization ──────────────────────────────────
  Future<void> finalizeMatch(int matchId, {int? potmId, String? margin}) async {
    final match = await _repo.getMatchById(matchId);
    if (match == null) return;
    await _repo.updateMatch(match.copyWith(
      status: 'COMPLETED',
      playerOfTheMatchId: potmId,
      winningMargin: margin,
    ));
  }
}
