import 'dart:async';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/player.dart';
import '../models/team.dart';
import '../models/match.dart';
import '../models/delivery.dart';

class CricketDatabase {
  static final CricketDatabase instance = CricketDatabase._internal();
  static Database? _db;

  CricketDatabase._internal();

  Future<Database> get database async {
    _db ??= await _initDb();
    return _db!;
  }

  Future<Database> _initDb() async {
    final path = join(await getDatabasesPath(), 'crease_scorer.db');
    return openDatabase(path, version: 1, onCreate: _onCreate);
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE players (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        clubName TEXT DEFAULT '',
        avatarUrl TEXT DEFAULT '',
        designRole TEXT DEFAULT 'ALL_ROUNDER',
        battingMatches INTEGER DEFAULT 0,
        battingInnings INTEGER DEFAULT 0,
        battingNotOuts INTEGER DEFAULT 0,
        battingRuns INTEGER DEFAULT 0,
        battingHighestScore INTEGER DEFAULT 0,
        battingBalls INTEGER DEFAULT 0,
        battingFours INTEGER DEFAULT 0,
        battingSixes INTEGER DEFAULT 0,
        battingFiftyCount INTEGER DEFAULT 0,
        battingHundredCount INTEGER DEFAULT 0,
        bowlingOversBalls INTEGER DEFAULT 0,
        bowlingMaidens INTEGER DEFAULT 0,
        bowlingRunsConceded INTEGER DEFAULT 0,
        bowlingWickets INTEGER DEFAULT 0,
        bowlingBestWickets INTEGER DEFAULT 0,
        bowlingBestRunsConceded INTEGER DEFAULT 0,
        bowlingWides INTEGER DEFAULT 0,
        bowlingNoBalls INTEGER DEFAULT 0,
        fieldingCatches INTEGER DEFAULT 0,
        fieldingRunOuts INTEGER DEFAULT 0,
        fieldingStumpings INTEGER DEFAULT 0
      )
    ''');

    await db.execute('''
      CREATE TABLE teams (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        logoUrl TEXT DEFAULT '',
        matchesPlayed INTEGER DEFAULT 0,
        wins INTEGER DEFAULT 0,
        losses INTEGER DEFAULT 0,
        ties INTEGER DEFAULT 0,
        noResults INTEGER DEFAULT 0,
        totalRunsScored INTEGER DEFAULT 0,
        totalBallsFaced INTEGER DEFAULT 0,
        totalRunsConceded INTEGER DEFAULT 0,
        totalBallsBowled INTEGER DEFAULT 0
      )
    ''');

    await db.execute('''
      CREATE TABLE matches (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        teamAId INTEGER NOT NULL,
        teamBId INTEGER NOT NULL,
        teamAName TEXT NOT NULL,
        teamBName TEXT NOT NULL,
        teamALogo TEXT DEFAULT '',
        teamBLogo TEXT DEFAULT '',
        venue TEXT DEFAULT '',
        scheduledDate TEXT DEFAULT '',
        scheduledTime TEXT DEFAULT '',
        oversCount INTEGER DEFAULT 20,
        ballsPerOver INTEGER DEFAULT 6,
        superOver INTEGER DEFAULT 0,
        freeHitRules INTEGER DEFAULT 1,
        bowlerLimit INTEGER DEFAULT 4,
        wideRulesRuns INTEGER DEFAULT 1,
        noBallRulesRuns INTEGER DEFAULT 1,
        retiredOutRules INTEGER DEFAULT 1,
        customLocalRules TEXT DEFAULT '',
        tossWinnerId INTEGER DEFAULT 0,
        tossDecision TEXT DEFAULT '',
        status TEXT DEFAULT 'PRE_MATCH',
        teamASquadIds TEXT DEFAULT '',
        teamBSquadIds TEXT DEFAULT '',
        currentInningsNo INTEGER DEFAULT 1,
        firstInningsTeamId INTEGER DEFAULT 0,
        secondInningsTeamId INTEGER DEFAULT 0,
        firstInningsScore INTEGER DEFAULT 0,
        firstInningsWickets INTEGER DEFAULT 0,
        firstInningsOversBalls INTEGER DEFAULT 0,
        secondInningsScore INTEGER DEFAULT 0,
        secondInningsWickets INTEGER DEFAULT 0,
        secondInningsOversBalls INTEGER DEFAULT 0,
        winnerId INTEGER DEFAULT 0,
        winningMargin TEXT DEFAULT '',
        playerOfTheMatchId INTEGER DEFAULT 0,
        isLocked INTEGER DEFAULT 0,
        syncStatus TEXT DEFAULT 'SYNCED'
      )
    ''');

    await db.execute('''
      CREATE TABLE deliveries (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        matchId INTEGER NOT NULL,
        inningsNo INTEGER NOT NULL,
        overIndex INTEGER NOT NULL,
        ballIndexInOver INTEGER NOT NULL,
        batsmanId INTEGER NOT NULL,
        batsmanName TEXT NOT NULL,
        bowlerId INTEGER NOT NULL,
        bowlerName TEXT NOT NULL,
        runsSimple INTEGER NOT NULL,
        extraType TEXT NOT NULL,
        extraRuns INTEGER NOT NULL,
        isWicket INTEGER NOT NULL,
        dismissalType TEXT DEFAULT 'NONE',
        dismissedBatsmanId INTEGER DEFAULT 0,
        dismissedBatsmanName TEXT DEFAULT '',
        fielderName TEXT DEFAULT '',
        timestamp INTEGER DEFAULT 0
      )
    ''');
  }

  // ─── Players ───────────────────────────────────────────────
  final _playersController = StreamController<List<PlayerModel>>.broadcast();
  Stream<List<PlayerModel>> get playersStream => _playersController.stream;

  Future<List<PlayerModel>> getAllPlayers() async {
    final db = await database;
    final rows = await db.query('players', orderBy: 'name ASC');
    return rows.map(PlayerModel.fromMap).toList();
  }

  Future<int> insertPlayer(PlayerModel p) async {
    final db = await database;
    final id = await db.insert('players', p.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
    _notifyPlayers();
    return id;
  }

  Future<void> insertPlayers(List<PlayerModel> players) async {
    final db = await database;
    final batch = db.batch();
    for (final p in players) batch.insert('players', p.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
    await batch.commit(noResult: true);
    _notifyPlayers();
  }

  Future<void> updatePlayer(PlayerModel p) async {
    final db = await database;
    await db.update('players', p.toMap(), where: 'id = ?', whereArgs: [p.id]);
    _notifyPlayers();
  }

  Future<void> deletePlayer(int id) async {
    final db = await database;
    await db.delete('players', where: 'id = ?', whereArgs: [id]);
    _notifyPlayers();
  }

  Future<void> deleteAllPlayers() async {
    final db = await database;
    await db.delete('players');
    _notifyPlayers();
  }

  Future<void> _notifyPlayers() async {
    _playersController.add(await getAllPlayers());
  }

  // ─── Teams ─────────────────────────────────────────────────
  final _teamsController = StreamController<List<TeamModel>>.broadcast();
  Stream<List<TeamModel>> get teamsStream => _teamsController.stream;

  Future<List<TeamModel>> getAllTeams() async {
    final db = await database;
    final rows = await db.query('teams', orderBy: 'name ASC');
    return rows.map(TeamModel.fromMap).toList();
  }

  Future<int> insertTeam(TeamModel t) async {
    final db = await database;
    final id = await db.insert('teams', t.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
    _notifyTeams();
    return id;
  }

  Future<void> updateTeam(TeamModel t) async {
    final db = await database;
    await db.update('teams', t.toMap(), where: 'id = ?', whereArgs: [t.id]);
    _notifyTeams();
  }

  Future<void> deleteAllTeams() async {
    final db = await database;
    await db.delete('teams');
    _notifyTeams();
  }

  Future<void> _notifyTeams() async {
    _teamsController.add(await getAllTeams());
  }

  // ─── Matches ───────────────────────────────────────────────
  final _matchesController = StreamController<List<MatchModel>>.broadcast();
  Stream<List<MatchModel>> get matchesStream => _matchesController.stream;

  Future<List<MatchModel>> getAllMatches() async {
    final db = await database;
    final rows = await db.query('matches', orderBy: 'id DESC');
    return rows.map(MatchModel.fromMap).toList();
  }

  Future<MatchModel?> getMatchById(int id) async {
    final db = await database;
    final rows = await db.query('matches', where: 'id = ?', whereArgs: [id]);
    if (rows.isEmpty) return null;
    return MatchModel.fromMap(rows.first);
  }

  Stream<MatchModel?> watchMatch(int id) async* {
    yield await getMatchById(id);
    await for (final _ in matchesStream) {
      yield await getMatchById(id);
    }
  }

  Future<int> insertMatch(MatchModel m) async {
    final db = await database;
    final id = await db.insert('matches', m.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
    _notifyMatches();
    return id;
  }

  Future<void> updateMatch(MatchModel m) async {
    final db = await database;
    await db.update('matches', m.toMap(), where: 'id = ?', whereArgs: [m.id]);
    _notifyMatches();
  }

  Future<void> deleteAllMatches() async {
    final db = await database;
    await db.delete('matches');
    _notifyMatches();
  }

  Future<void> _notifyMatches() async {
    _matchesController.add(await getAllMatches());
  }

  // ─── Deliveries ────────────────────────────────────────────
  final _deliveriesControllers = <int, StreamController<List<DeliveryModel>>>{};

  Stream<List<DeliveryModel>> watchDeliveries(int matchId) {
    _deliveriesControllers[matchId] ??= StreamController<List<DeliveryModel>>.broadcast();
    _notifyDeliveries(matchId);
    return _deliveriesControllers[matchId]!.stream;
  }

  Future<List<DeliveryModel>> getDeliveriesForMatch(int matchId) async {
    final db = await database;
    final rows = await db.query('deliveries', where: 'matchId = ?', whereArgs: [matchId], orderBy: 'id ASC');
    return rows.map(DeliveryModel.fromMap).toList();
  }

  Future<int> insertDelivery(DeliveryModel d) async {
    final db = await database;
    final id = await db.insert('deliveries', d.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
    _notifyDeliveries(d.matchId);
    return id;
  }

  Future<void> deleteLastDelivery(int matchId) async {
    final db = await database;
    await db.execute('DELETE FROM deliveries WHERE id = (SELECT MAX(id) FROM deliveries WHERE matchId = ?)', [matchId]);
    _notifyDeliveries(matchId);
  }

  Future<void> clearDeliveriesForMatch(int matchId) async {
    final db = await database;
    await db.delete('deliveries', where: 'matchId = ?', whereArgs: [matchId]);
    _notifyDeliveries(matchId);
  }

  Future<void> deleteAllDeliveries() async {
    final db = await database;
    await db.delete('deliveries');
  }

  Future<void> _notifyDeliveries(int matchId) async {
    final ctrl = _deliveriesControllers[matchId];
    if (ctrl != null) {
      ctrl.add(await getDeliveriesForMatch(matchId));
    }
  }
}
