import '../data/models/player.dart';
import '../data/models/match.dart';
import '../data/models/delivery.dart';

// ─── Score Data Classes ────────────────────────────────────────────────────────

class BatterScore {
  final int playerId;
  final String name;
  final int runs;
  final int balls;
  final int fours;
  final int sixes;
  final bool isStriker;
  final bool isNonStriker;
  final bool isDismissed;
  final String dismissalDescription;
  final int orderIndex;

  const BatterScore({
    required this.playerId,
    required this.name,
    this.runs = 0,
    this.balls = 0,
    this.fours = 0,
    this.sixes = 0,
    this.isStriker = false,
    this.isNonStriker = false,
    this.isDismissed = false,
    this.dismissalDescription = 'not out',
    this.orderIndex = 0,
  });

  double get strikeRate => balls > 0 ? (runs / balls * 100.0) : 0.0;

  BatterScore copyWith({
    int? runs, int? balls, int? fours, int? sixes,
    bool? isStriker, bool? isNonStriker,
    bool? isDismissed, String? dismissalDescription,
  }) => BatterScore(
    playerId: playerId, name: name,
    runs: runs ?? this.runs, balls: balls ?? this.balls,
    fours: fours ?? this.fours, sixes: sixes ?? this.sixes,
    isStriker: isStriker ?? this.isStriker,
    isNonStriker: isNonStriker ?? this.isNonStriker,
    isDismissed: isDismissed ?? this.isDismissed,
    dismissalDescription: dismissalDescription ?? this.dismissalDescription,
    orderIndex: orderIndex,
  );
}

class BowlerScore {
  final int playerId;
  final String name;
  final int ballsBowled;
  final int maidens;
  final int runsConceded;
  final int wickets;
  final int wides;
  final int noBalls;
  final bool activeInOver;

  const BowlerScore({
    required this.playerId,
    required this.name,
    this.ballsBowled = 0,
    this.maidens = 0,
    this.runsConceded = 0,
    this.wickets = 0,
    this.wides = 0,
    this.noBalls = 0,
    this.activeInOver = false,
  });

  String get oversString => '${ballsBowled ~/ 6}.${ballsBowled % 6}';
  double get economy => ballsBowled > 0 ? (runsConceded / (ballsBowled / 6.0)) : 0.0;

  BowlerScore copyWith({
    int? ballsBowled, int? maidens, int? runsConceded,
    int? wickets, int? wides, int? noBalls, bool? activeInOver,
  }) => BowlerScore(
    playerId: playerId, name: name,
    ballsBowled: ballsBowled ?? this.ballsBowled,
    maidens: maidens ?? this.maidens,
    runsConceded: runsConceded ?? this.runsConceded,
    wickets: wickets ?? this.wickets,
    wides: wides ?? this.wides,
    noBalls: noBalls ?? this.noBalls,
    activeInOver: activeInOver ?? this.activeInOver,
  );
}

class PartnershipState {
  final int batterAId;
  final String batterAName;
  final int batterBId;
  final String batterBName;
  final int runs;
  final int balls;
  final bool active;

  const PartnershipState({
    required this.batterAId, required this.batterAName,
    required this.batterBId, required this.batterBName,
    this.runs = 0, this.balls = 0, this.active = true,
  });

  PartnershipState copyWith({int? runs, int? balls, bool? active}) => PartnershipState(
    batterAId: batterAId, batterAName: batterAName,
    batterBId: batterBId, batterBName: batterBName,
    runs: runs ?? this.runs, balls: balls ?? this.balls,
    active: active ?? this.active,
  );
}

class FallOfWicket {
  final int wicketNo;
  final int runs;
  final String oversBall;
  final String batsmanName;
  final String bowlerName;

  const FallOfWicket({
    required this.wicketNo, required this.runs,
    required this.oversBall, required this.batsmanName,
    required this.bowlerName,
  });
}

class ExtrasState {
  final int wides, noBalls, byes, legByes, total;
  const ExtrasState({this.wides=0, this.noBalls=0, this.byes=0, this.legByes=0, this.total=0});
}

class InningsState {
  final int inningsNo;
  final int battingTeamId;
  final String battingTeamName;
  final int bowlingTeamId;
  final String bowlingTeamName;
  final int score;
  final int wickets;
  final int ballsBowled;
  final List<BatterScore> batters;
  final List<BowlerScore> bowlers;
  final List<PartnershipState> partnerships;
  final List<FallOfWicket> fallOfWickets;
  final ExtrasState extras;
  final List<DeliveryModel> currentOverBalls;
  final int activeStrikerId;
  final int activeNonStrikerId;
  final int activeBowlerId;
  final bool isCompleted;

  const InningsState({
    required this.inningsNo,
    required this.battingTeamId, required this.battingTeamName,
    required this.bowlingTeamId, required this.bowlingTeamName,
    this.score = 0, this.wickets = 0, this.ballsBowled = 0,
    this.batters = const [], this.bowlers = const [],
    this.partnerships = const [], this.fallOfWickets = const [],
    this.extras = const ExtrasState(),
    this.currentOverBalls = const [],
    this.activeStrikerId = 0, this.activeNonStrikerId = 0,
    this.activeBowlerId = 0, this.isCompleted = false,
  });

  String get oversString => '${ballsBowled ~/ 6}.${ballsBowled % 6}';
  double get runRate => ballsBowled > 0 ? (score / (ballsBowled / 6.0)) : 0.0;
}

class ComputedMatchState {
  final MatchModel match;
  final InningsState innings1;
  final InningsState? innings2;
  final int currentInningsNo;
  final InningsState activeInnings;
  final String winnerName;
  final String resultSummary;
  final int requiredRuns;
  final int remainingBalls;
  final double requiredRunRate;
  final bool isCompleted;

  const ComputedMatchState({
    required this.match, required this.innings1,
    this.innings2, required this.currentInningsNo,
    required this.activeInnings,
    this.winnerName = '', this.resultSummary = '',
    this.requiredRuns = 0, this.remainingBalls = 0,
    this.requiredRunRate = 0.0, this.isCompleted = false,
  });
}

// ─── Scoring Engine ────────────────────────────────────────────────────────────

class ScoringEngine {
  static ComputedMatchState computeMatchState(
    MatchModel match,
    List<DeliveryModel> deliveries,
    Map<int, PlayerModel> playersMap,
  ) {
    final d1 = deliveries.where((d) => d.inningsNo == 1).toList();
    final d2 = deliveries.where((d) => d.inningsNo == 2).toList();

    // Determine batting/bowling teams for innings 1
    int i1BatTeamId = match.firstInningsTeamId != 0
        ? match.firstInningsTeamId
        : (match.tossWinnerId != 0
            ? (match.tossDecision == 'BAT' ? match.tossWinnerId : _other(match, match.tossWinnerId))
            : match.teamAId);

    final int i1BowlTeamId = _other(match, i1BatTeamId);
    final String i1BatTeamName = i1BatTeamId == match.teamAId ? match.teamAName : match.teamBName;
    final String i1BowlTeamName = i1BatTeamId == match.teamAId ? match.teamBName : match.teamAName;

    final teamASquad = _parseSquad(match.teamASquadIds);
    final teamBSquad = _parseSquad(match.teamBSquadIds);
    final team1Squad = i1BatTeamId == match.teamAId ? teamASquad : teamBSquad;
    final team1BowlSquad = i1BatTeamId == match.teamAId ? teamBSquad : teamASquad;

    final i1State = _computeInnings(
      inningsNo: 1,
      battingTeamId: i1BatTeamId, battingTeamName: i1BatTeamName,
      bowlingTeamId: i1BowlTeamId, bowlingTeamName: i1BowlTeamName,
      squadList: team1Squad, bowlSquadList: team1BowlSquad,
      deliveries: d1, playersMap: playersMap,
      oversCount: match.oversCount, ballsPerOver: match.ballsPerOver,
    );

    final innings2Triggered = match.currentInningsNo == 2 || d2.isNotEmpty || i1State.isCompleted;
    InningsState? i2State;
    if (innings2Triggered) {
      i2State = _computeInnings(
        inningsNo: 2,
        battingTeamId: i1BowlTeamId, battingTeamName: i1BowlTeamName,
        bowlingTeamId: i1BatTeamId, bowlingTeamName: i1BatTeamName,
        squadList: team1BowlSquad, bowlSquadList: team1Squad,
        deliveries: d2, playersMap: playersMap,
        oversCount: match.oversCount, ballsPerOver: match.ballsPerOver,
        targetRuns: i1State.score + 1,
      );
    }

    final activeInnings = i2State ?? i1State;
    final currentInningsNo = i2State != null ? 2 : 1;

    int requiredRuns = 0, remainingBalls = 0;
    double reqRunRate = 0.0;
    String winnerName = '', resultSummary = '';
    bool isCompleted = false;

    if (i2State != null) {
      final target = i1State.score + 1;
      final maxBalls = match.oversCount * match.ballsPerOver;
      requiredRuns = target - i2State.score;
      remainingBalls = (maxBalls - i2State.ballsBowled).clamp(0, maxBalls);
      if (remainingBalls > 0) reqRunRate = requiredRuns / (remainingBalls / 6.0);

      if (i2State.score >= target) {
        winnerName = i2State.battingTeamName;
        final wicketsLeft = 10 - i2State.wickets;
        resultSummary = '$winnerName won by $wicketsLeft wickets';
        isCompleted = true;
      } else if (remainingBalls <= 0 || i2State.wickets >= 10 || i2State.isCompleted) {
        if (i2State.score < i1State.score) {
          winnerName = i1State.battingTeamName;
          final margin = i1State.score - i2State.score;
          resultSummary = '$winnerName won by $margin runs';
          isCompleted = true;
        } else if (i2State.score == i1State.score) {
          resultSummary = 'Match Tied!';
          isCompleted = true;
        }
      }
    } else if (i1State.isCompleted || i1State.wickets >= 10) {
      resultSummary = '${i1State.battingTeamName} scored ${i1State.score}/${i1State.wickets}. Target for ${i1State.bowlingTeamName} is ${i1State.score + 1}.';
    }

    if (match.status == 'COMPLETED') {
      isCompleted = true;
      winnerName = match.winnerId == match.teamAId ? match.teamAName : match.winnerId == match.teamBId ? match.teamBName : '';
      resultSummary = match.winningMargin.isNotEmpty ? match.winningMargin : 'Match finished';
    }

    return ComputedMatchState(
      match: match, innings1: i1State, innings2: i2State,
      currentInningsNo: currentInningsNo, activeInnings: activeInnings,
      winnerName: winnerName, resultSummary: resultSummary,
      requiredRuns: requiredRuns, remainingBalls: remainingBalls,
      requiredRunRate: reqRunRate, isCompleted: isCompleted,
    );
  }

  static int _other(MatchModel m, int teamId) => teamId == m.teamAId ? m.teamBId : m.teamAId;

  static List<int> _parseSquad(String ids) =>
      ids.split(',').where((s) => s.isNotEmpty).map(int.tryParse).whereType<int>().toList();

  static InningsState _computeInnings({
    required int inningsNo,
    required int battingTeamId, required String battingTeamName,
    required int bowlingTeamId, required String bowlingTeamName,
    required List<int> squadList, required List<int> bowlSquadList,
    required List<DeliveryModel> deliveries,
    required Map<int, PlayerModel> playersMap,
    required int oversCount, required int ballsPerOver,
    int? targetRuns,
  }) {
    int score = 0, wickets = 0, totalLegalBalls = 0;
    int wideCount = 0, noBallCount = 0, byeCount = 0, legByeCount = 0;
    int strikerId = 0, nonStrikerId = 0, currentBowlerId = 0;

    final batterMap = <int, BatterScore>{};
    for (var i = 0; i < squadList.length; i++) {
      final id = squadList[i];
      batterMap[id] = BatterScore(playerId: id, name: playersMap[id]?.name ?? 'Player $id', orderIndex: i);
    }

    final bowlerMap = <int, BowlerScore>{};
    for (final id in bowlSquadList) {
      bowlerMap[id] = BowlerScore(playerId: id, name: playersMap[id]?.name ?? 'Bowler $id');
    }

    final partnerships = <PartnershipState>[];
    final fallOfWickets = <FallOfWicket>[];

    int _getNextBatsman() {
      final usedIds = batterMap.values
          .where((b) => b.balls > 0 || b.isDismissed || b.playerId == strikerId || b.playerId == nonStrikerId)
          .map((b) => b.playerId)
          .toSet();
      return squadList.firstWhere((id) => !usedIds.contains(id), orElse: () => 0);
    }

    bool done = false;
    for (final delivery in deliveries) {
      if (done) break;
      final bId = delivery.batsmanId;
      final boId = delivery.bowlerId;
      if (strikerId == 0) strikerId = bId;
      if (nonStrikerId == 0) {
        nonStrikerId = squadList.firstWhere((id) => id != strikerId, orElse: () => 0);
      }
      currentBowlerId = boId;

      var batter = batterMap[bId] ?? BatterScore(playerId: bId, name: delivery.batsmanName);
      var bowler = bowlerMap[boId] ?? BowlerScore(playerId: boId, name: delivery.bowlerName);

      final isWide = delivery.extraType == 'WIDE';
      final isNoBall = delivery.extraType == 'NO_BALL';
      final isBye = delivery.extraType == 'BYE';
      final isLegBye = delivery.extraType == 'LEG_BYE';
      final isLegal = !isWide && !isNoBall;

      int ballTotal = delivery.runsSimple;
      if (isWide) { ballTotal += delivery.extraRuns; wideCount += delivery.extraRuns; }
      else if (isNoBall) { ballTotal += delivery.extraRuns; noBallCount += delivery.extraRuns; }
      else if (isBye) { ballTotal += delivery.extraRuns; byeCount += delivery.extraRuns; }
      else if (isLegBye) { ballTotal += delivery.extraRuns; legByeCount += delivery.extraRuns; }

      score += ballTotal;

      if (!isWide) {
        batter = batter.copyWith(
          runs: batter.runs + delivery.runsSimple,
          balls: batter.balls + 1,
          fours: batter.fours + (delivery.runsSimple == 4 ? 1 : 0),
          sixes: batter.sixes + (delivery.runsSimple == 6 ? 1 : 0),
        );
      }
      batterMap[bId] = batter;

      int bowlerBalls = isLegal ? 1 : 0;
      int runsForBowler = isBye || isLegBye ? (isNoBall ? delivery.extraRuns : 0) : ballTotal;
      bowler = bowler.copyWith(
        ballsBowled: bowler.ballsBowled + bowlerBalls,
        runsConceded: bowler.runsConceded + runsForBowler,
        wides: bowler.wides + (isWide ? delivery.extraRuns : 0),
        noBalls: bowler.noBalls + (isNoBall ? 1 : 0),
      );

      if (isLegal) totalLegalBalls++;

      if (delivery.isWicket) {
        wickets++;
        final dType = delivery.dismissalType;
        final fName = delivery.fielderName;
        final desc = switch (dType) {
          'BOWLED' => 'b ${delivery.bowlerName}',
          'CAUGHT' => fName.isNotEmpty ? 'c $fName b ${delivery.bowlerName}' : 'c & b ${delivery.bowlerName}',
          'LBW' => 'lbw b ${delivery.bowlerName}',
          'STUMPED' => 'st $fName b ${delivery.bowlerName}',
          'RUN_OUT' => 'run out ($fName)',
          'HIT_WICKET' => 'hit wicket b ${delivery.bowlerName}',
          'RETIRED_OUT' => 'retired out',
          _ => 'out',
        };
        final dismissedId = delivery.dismissedBatsmanId != 0 ? delivery.dismissedBatsmanId : bId;
        var dPlayer = batterMap[dismissedId] ?? BatterScore(playerId: dismissedId, name: delivery.dismissedBatsmanName);
        batterMap[dismissedId] = dPlayer.copyWith(isDismissed: true, dismissalDescription: desc);

        if (dType != 'RETIRED_OUT' && dType != 'RUN_OUT') {
          bowler = bowler.copyWith(wickets: bowler.wickets + 1);
        }

        final overRep = '${totalLegalBalls ~/ 6}.${totalLegalBalls % 6}';
        fallOfWickets.add(FallOfWicket(wicketNo: wickets, runs: score, oversBall: overRep, batsmanName: delivery.dismissedBatsmanName, bowlerName: delivery.bowlerName));

        if (dismissedId == strikerId) strikerId = _getNextBatsman();
        else if (dismissedId == nonStrikerId) nonStrikerId = _getNextBatsman();
      }

      bowlerMap[boId] = bowler;

      // Strike rotation
      int runsRotated;
      if (isWide) runsRotated = delivery.extraRuns - 1;
      else runsRotated = delivery.runsSimple + (isBye || isLegBye ? delivery.extraRuns : 0);

      if (runsRotated % 2 != 0) {
        final temp = strikerId; strikerId = nonStrikerId; nonStrikerId = temp;
      }

      if (isLegal && totalLegalBalls % 6 == 0) {
        final temp = strikerId; strikerId = nonStrikerId; nonStrikerId = temp;
      }

      // Partnerships
      final partIdx = partnerships.indexWhere((p) => p.active);
      if (partIdx != -1) {
        final updated = partnerships[partIdx].copyWith(
          runs: partnerships[partIdx].runs + ballTotal,
          balls: partnerships[partIdx].balls + (isWide ? 0 : 1),
        );
        partnerships[partIdx] = delivery.isWicket ? updated.copyWith(active: false) : updated;
      } else {
        partnerships.add(PartnershipState(
          batterAId: strikerId, batterAName: playersMap[strikerId]?.name ?? 'Striker',
          batterBId: nonStrikerId, batterBName: playersMap[nonStrikerId]?.name ?? 'Non-Striker',
          runs: ballTotal, balls: isWide ? 0 : 1,
          active: !delivery.isWicket,
        ));
      }

      if (targetRuns != null && score >= targetRuns) done = true;
    }

    final finalBatters = batterMap.values.map((b) => b.copyWith(
      isStriker: b.playerId == strikerId && strikerId != 0,
      isNonStriker: b.playerId == nonStrikerId && nonStrikerId != 0,
    )).toList()..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));

    final finalBowlers = bowlerMap.values.map((b) => b.copyWith(
      activeInOver: b.playerId == currentBowlerId && currentBowlerId != 0,
    )).toList();

    final totalMaxBalls = oversCount * ballsPerOver;
    final inningsFinished = totalLegalBalls >= totalMaxBalls || wickets >= 10 || (targetRuns != null && score >= targetRuns);

    final totalExtras = wideCount + noBallCount + byeCount + legByeCount;
    final extras = ExtrasState(wides: wideCount, noBalls: noBallCount, byes: byeCount, legByes: legByeCount, total: totalExtras);

    final completedOvers = totalLegalBalls ~/ 6;
    final currentOverBalls = deliveries.where((d) => d.overIndex == completedOvers).toList();

    return InningsState(
      inningsNo: inningsNo,
      battingTeamId: battingTeamId, battingTeamName: battingTeamName,
      bowlingTeamId: bowlingTeamId, bowlingTeamName: bowlingTeamName,
      score: score, wickets: wickets, ballsBowled: totalLegalBalls,
      batters: finalBatters, bowlers: finalBowlers,
      partnerships: partnerships, fallOfWickets: fallOfWickets,
      extras: extras, currentOverBalls: currentOverBalls,
      activeStrikerId: strikerId, activeNonStrikerId: nonStrikerId,
      activeBowlerId: currentBowlerId, isCompleted: inningsFinished,
    );
  }
}
