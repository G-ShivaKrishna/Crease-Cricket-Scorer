import '../database/cricket_database.dart';
import '../models/player.dart';
import '../models/team.dart';
import '../models/match.dart';
import '../models/delivery.dart';

class CricketRepository {
  final CricketDatabase _db = CricketDatabase.instance;
  bool _prepopulated = false;

  // ─── Streams ───────────────────────────────────────────────
  Stream<List<PlayerModel>> get players => _db.playersStream;
  Stream<List<TeamModel>> get teams => _db.teamsStream;
  Stream<List<MatchModel>> get matches => _db.matchesStream;
  Stream<List<DeliveryModel>> watchDeliveries(int matchId) => _db.watchDeliveries(matchId);

  // ─── Init / Prepopulate ────────────────────────────────────
  Future<void> initialize() async {
    if (_prepopulated) return;
    _prepopulated = true;
    final existing = await _db.getAllPlayers();
    if (existing.isNotEmpty) return; // already seeded

    // Seed sample teams and players
    final teams = [
      _TeamSeed('Mumbai Mavricks', ['Rohit Sharma', 'Ishan Kishan', 'Suryakumar Yadav', 'Tilak Varma', 'Naman Dhir', 'Hardik Pandya', 'Tim David', 'Karn Sharma', 'Gerald Coetzee', 'Deepak Chahar', 'Jasprit Bumrah']),
      _TeamSeed('Chennai Kings', ['Ruturaj Gaikwad', 'Devon Conway', 'Rachin Ravindra', 'Shivam Dube', 'Daryl Mitchell', 'MS Dhoni', 'Ravindra Jadeja', 'Mitchell Santner', 'Maheesh Theekshana', 'Tushar Deshpande', 'Matheesha Pathirana']),
    ];

    for (final ts in teams) {
      final teamId = await _db.insertTeam(TeamModel(name: ts.name));
      for (final playerName in ts.players) {
        await _db.insertPlayer(PlayerModel(name: playerName, clubName: ts.name));
      }
    }
  }

  // ─── Players ───────────────────────────────────────────────
  Future<List<PlayerModel>> getAllPlayers() => _db.getAllPlayers();

  Future<int> insertPlayer(PlayerModel p) => _db.insertPlayer(p);

  Future<void> updatePlayer(PlayerModel p) => _db.updatePlayer(p);

  Future<void> deletePlayer(int id) => _db.deletePlayer(id);

  // ─── Teams ─────────────────────────────────────────────────
  Future<List<TeamModel>> getAllTeams() => _db.getAllTeams();

  Future<int> insertTeam(TeamModel t) => _db.insertTeam(t);

  Future<void> updateTeam(TeamModel t) => _db.updateTeam(t);

  // ─── Matches ───────────────────────────────────────────────
  Future<List<MatchModel>> getAllMatches() => _db.getAllMatches();

  Future<MatchModel?> getMatchById(int id) => _db.getMatchById(id);

  Stream<MatchModel?> watchMatch(int id) => _db.watchMatch(id);

  Future<int> insertMatch(MatchModel m) => _db.insertMatch(m);

  Future<void> updateMatch(MatchModel m) => _db.updateMatch(m);

  Future<void> deleteAllMatches() => _db.deleteAllMatches();

  // ─── Deliveries ────────────────────────────────────────────
  Future<List<DeliveryModel>> getDeliveriesForMatch(int matchId) => _db.getDeliveriesForMatch(matchId);

  Future<int> insertDelivery(DeliveryModel d) => _db.insertDelivery(d);

  Future<void> deleteLastDelivery(int matchId) => _db.deleteLastDelivery(matchId);

  Future<void> clearDeliveriesForMatch(int matchId) => _db.clearDeliveriesForMatch(matchId);
}

class _TeamSeed {
  final String name;
  final List<String> players;
  const _TeamSeed(this.name, this.players);
}
