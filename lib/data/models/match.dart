class MatchModel {
  final int id;
  final int teamAId;
  final int teamBId;
  final String teamAName;
  final String teamBName;
  final String teamALogo;
  final String teamBLogo;
  final String venue;
  final String scheduledDate;
  final String scheduledTime;

  // Rules
  final int oversCount;
  final int ballsPerOver;
  final bool superOver;
  final bool freeHitRules;
  final int bowlerLimit;
  final int wideRulesRuns;
  final int noBallRulesRuns;
  final bool retiredOutRules;
  final String customLocalRules;

  // Setup state
  final int tossWinnerId;
  final String tossDecision; // BAT or BOWL
  final String status; // PRE_MATCH, LIVE, INNINGS_BREAK, COMPLETED, ABANDONED

  // Squads (comma-separated player IDs)
  final String teamASquadIds;
  final String teamBSquadIds;

  // Innings tracking
  final int currentInningsNo;
  final int firstInningsTeamId;
  final int secondInningsTeamId;
  final int firstInningsScore;
  final int firstInningsWickets;
  final int firstInningsOversBalls;
  final int secondInningsScore;
  final int secondInningsWickets;
  final int secondInningsOversBalls;

  // Result
  final int winnerId;
  final String winningMargin;
  final int playerOfTheMatchId;

  final bool isLocked;
  final String syncStatus;

  const MatchModel({
    this.id = 0,
    required this.teamAId,
    required this.teamBId,
    required this.teamAName,
    required this.teamBName,
    this.teamALogo = '',
    this.teamBLogo = '',
    required this.venue,
    required this.scheduledDate,
    required this.scheduledTime,
    this.oversCount = 20,
    this.ballsPerOver = 6,
    this.superOver = false,
    this.freeHitRules = true,
    this.bowlerLimit = 4,
    this.wideRulesRuns = 1,
    this.noBallRulesRuns = 1,
    this.retiredOutRules = true,
    this.customLocalRules = '',
    this.tossWinnerId = 0,
    this.tossDecision = '',
    this.status = 'PRE_MATCH',
    this.teamASquadIds = '',
    this.teamBSquadIds = '',
    this.currentInningsNo = 1,
    this.firstInningsTeamId = 0,
    this.secondInningsTeamId = 0,
    this.firstInningsScore = 0,
    this.firstInningsWickets = 0,
    this.firstInningsOversBalls = 0,
    this.secondInningsScore = 0,
    this.secondInningsWickets = 0,
    this.secondInningsOversBalls = 0,
    this.winnerId = 0,
    this.winningMargin = '',
    this.playerOfTheMatchId = 0,
    this.isLocked = false,
    this.syncStatus = 'SYNCED',
  });

  MatchModel copyWith({
    int? id, int? tossWinnerId, String? tossDecision, String? status,
    int? firstInningsTeamId, int? secondInningsTeamId, int? currentInningsNo,
    int? winnerId, String? winningMargin, int? playerOfTheMatchId,
    String? teamASquadIds, String? teamBSquadIds,
  }) {
    return MatchModel(
      id: id ?? this.id,
      teamAId: teamAId, teamBId: teamBId,
      teamAName: teamAName, teamBName: teamBName,
      teamALogo: teamALogo, teamBLogo: teamBLogo,
      venue: venue, scheduledDate: scheduledDate, scheduledTime: scheduledTime,
      oversCount: oversCount, ballsPerOver: ballsPerOver,
      superOver: superOver, freeHitRules: freeHitRules,
      bowlerLimit: bowlerLimit, wideRulesRuns: wideRulesRuns,
      noBallRulesRuns: noBallRulesRuns, retiredOutRules: retiredOutRules,
      customLocalRules: customLocalRules,
      tossWinnerId: tossWinnerId ?? this.tossWinnerId,
      tossDecision: tossDecision ?? this.tossDecision,
      status: status ?? this.status,
      teamASquadIds: teamASquadIds ?? this.teamASquadIds,
      teamBSquadIds: teamBSquadIds ?? this.teamBSquadIds,
      currentInningsNo: currentInningsNo ?? this.currentInningsNo,
      firstInningsTeamId: firstInningsTeamId ?? this.firstInningsTeamId,
      secondInningsTeamId: secondInningsTeamId ?? this.secondInningsTeamId,
      firstInningsScore: firstInningsScore, firstInningsWickets: firstInningsWickets,
      firstInningsOversBalls: firstInningsOversBalls,
      secondInningsScore: secondInningsScore, secondInningsWickets: secondInningsWickets,
      secondInningsOversBalls: secondInningsOversBalls,
      winnerId: winnerId ?? this.winnerId,
      winningMargin: winningMargin ?? this.winningMargin,
      playerOfTheMatchId: playerOfTheMatchId ?? this.playerOfTheMatchId,
      isLocked: isLocked, syncStatus: syncStatus,
    );
  }

  Map<String, dynamic> toMap() => {
    'id': id == 0 ? null : id,
    'teamAId': teamAId, 'teamBId': teamBId,
    'teamAName': teamAName, 'teamBName': teamBName,
    'teamALogo': teamALogo, 'teamBLogo': teamBLogo,
    'venue': venue, 'scheduledDate': scheduledDate, 'scheduledTime': scheduledTime,
    'oversCount': oversCount, 'ballsPerOver': ballsPerOver,
    'superOver': superOver ? 1 : 0, 'freeHitRules': freeHitRules ? 1 : 0,
    'bowlerLimit': bowlerLimit, 'wideRulesRuns': wideRulesRuns,
    'noBallRulesRuns': noBallRulesRuns, 'retiredOutRules': retiredOutRules ? 1 : 0,
    'customLocalRules': customLocalRules,
    'tossWinnerId': tossWinnerId, 'tossDecision': tossDecision, 'status': status,
    'teamASquadIds': teamASquadIds, 'teamBSquadIds': teamBSquadIds,
    'currentInningsNo': currentInningsNo,
    'firstInningsTeamId': firstInningsTeamId, 'secondInningsTeamId': secondInningsTeamId,
    'firstInningsScore': firstInningsScore, 'firstInningsWickets': firstInningsWickets,
    'firstInningsOversBalls': firstInningsOversBalls,
    'secondInningsScore': secondInningsScore, 'secondInningsWickets': secondInningsWickets,
    'secondInningsOversBalls': secondInningsOversBalls,
    'winnerId': winnerId, 'winningMargin': winningMargin,
    'playerOfTheMatchId': playerOfTheMatchId,
    'isLocked': isLocked ? 1 : 0, 'syncStatus': syncStatus,
  };

  factory MatchModel.fromMap(Map<String, dynamic> m) => MatchModel(
    id: m['id'] as int? ?? 0,
    teamAId: m['teamAId'] as int,
    teamBId: m['teamBId'] as int,
    teamAName: m['teamAName'] as String,
    teamBName: m['teamBName'] as String,
    teamALogo: m['teamALogo'] as String? ?? '',
    teamBLogo: m['teamBLogo'] as String? ?? '',
    venue: m['venue'] as String? ?? '',
    scheduledDate: m['scheduledDate'] as String? ?? '',
    scheduledTime: m['scheduledTime'] as String? ?? '',
    oversCount: m['oversCount'] as int? ?? 20,
    ballsPerOver: m['ballsPerOver'] as int? ?? 6,
    superOver: (m['superOver'] as int? ?? 0) == 1,
    freeHitRules: (m['freeHitRules'] as int? ?? 1) == 1,
    bowlerLimit: m['bowlerLimit'] as int? ?? 4,
    wideRulesRuns: m['wideRulesRuns'] as int? ?? 1,
    noBallRulesRuns: m['noBallRulesRuns'] as int? ?? 1,
    retiredOutRules: (m['retiredOutRules'] as int? ?? 1) == 1,
    customLocalRules: m['customLocalRules'] as String? ?? '',
    tossWinnerId: m['tossWinnerId'] as int? ?? 0,
    tossDecision: m['tossDecision'] as String? ?? '',
    status: m['status'] as String? ?? 'PRE_MATCH',
    teamASquadIds: m['teamASquadIds'] as String? ?? '',
    teamBSquadIds: m['teamBSquadIds'] as String? ?? '',
    currentInningsNo: m['currentInningsNo'] as int? ?? 1,
    firstInningsTeamId: m['firstInningsTeamId'] as int? ?? 0,
    secondInningsTeamId: m['secondInningsTeamId'] as int? ?? 0,
    firstInningsScore: m['firstInningsScore'] as int? ?? 0,
    firstInningsWickets: m['firstInningsWickets'] as int? ?? 0,
    firstInningsOversBalls: m['firstInningsOversBalls'] as int? ?? 0,
    secondInningsScore: m['secondInningsScore'] as int? ?? 0,
    secondInningsWickets: m['secondInningsWickets'] as int? ?? 0,
    secondInningsOversBalls: m['secondInningsOversBalls'] as int? ?? 0,
    winnerId: m['winnerId'] as int? ?? 0,
    winningMargin: m['winningMargin'] as String? ?? '',
    playerOfTheMatchId: m['playerOfTheMatchId'] as int? ?? 0,
    isLocked: (m['isLocked'] as int? ?? 0) == 1,
    syncStatus: m['syncStatus'] as String? ?? 'SYNCED',
  );
}
