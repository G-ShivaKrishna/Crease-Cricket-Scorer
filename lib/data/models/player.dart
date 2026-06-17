class PlayerModel {
  final int id;
  final String name;
  final String clubName;
  final String avatarUrl;
  final String designRole; // BATS_MAN, BOWLER, ALL_ROUNDER, WICKET_KEEPER

  // Batting stats
  final int battingMatches;
  final int battingInnings;
  final int battingNotOuts;
  final int battingRuns;
  final int battingHighestScore;
  final int battingBalls;
  final int battingFours;
  final int battingSixes;
  final int battingFiftyCount;
  final int battingHundredCount;

  // Bowling stats
  final int bowlingOversBalls;
  final int bowlingMaidens;
  final int bowlingRunsConceded;
  final int bowlingWickets;
  final int bowlingBestWickets;
  final int bowlingBestRunsConceded;
  final int bowlingWides;
  final int bowlingNoBalls;

  // Fielding stats
  final int fieldingCatches;
  final int fieldingRunOuts;
  final int fieldingStumpings;

  const PlayerModel({
    this.id = 0,
    required this.name,
    this.clubName = '',
    this.avatarUrl = '',
    this.designRole = 'ALL_ROUNDER',
    this.battingMatches = 0,
    this.battingInnings = 0,
    this.battingNotOuts = 0,
    this.battingRuns = 0,
    this.battingHighestScore = 0,
    this.battingBalls = 0,
    this.battingFours = 0,
    this.battingSixes = 0,
    this.battingFiftyCount = 0,
    this.battingHundredCount = 0,
    this.bowlingOversBalls = 0,
    this.bowlingMaidens = 0,
    this.bowlingRunsConceded = 0,
    this.bowlingWickets = 0,
    this.bowlingBestWickets = 0,
    this.bowlingBestRunsConceded = 0,
    this.bowlingWides = 0,
    this.bowlingNoBalls = 0,
    this.fieldingCatches = 0,
    this.fieldingRunOuts = 0,
    this.fieldingStumpings = 0,
  });

  PlayerModel copyWith({int? id, String? name, String? clubName, String? designRole}) {
    return PlayerModel(
      id: id ?? this.id,
      name: name ?? this.name,
      clubName: clubName ?? this.clubName,
      avatarUrl: avatarUrl,
      designRole: designRole ?? this.designRole,
      battingMatches: battingMatches, battingInnings: battingInnings,
      battingNotOuts: battingNotOuts, battingRuns: battingRuns,
      battingHighestScore: battingHighestScore, battingBalls: battingBalls,
      battingFours: battingFours, battingSixes: battingSixes,
      battingFiftyCount: battingFiftyCount, battingHundredCount: battingHundredCount,
      bowlingOversBalls: bowlingOversBalls, bowlingMaidens: bowlingMaidens,
      bowlingRunsConceded: bowlingRunsConceded, bowlingWickets: bowlingWickets,
      bowlingBestWickets: bowlingBestWickets, bowlingBestRunsConceded: bowlingBestRunsConceded,
      bowlingWides: bowlingWides, bowlingNoBalls: bowlingNoBalls,
      fieldingCatches: fieldingCatches, fieldingRunOuts: fieldingRunOuts,
      fieldingStumpings: fieldingStumpings,
    );
  }

  Map<String, dynamic> toMap() => {
    'id': id == 0 ? null : id,
    'name': name, 'clubName': clubName, 'avatarUrl': avatarUrl,
    'designRole': designRole,
    'battingMatches': battingMatches, 'battingInnings': battingInnings,
    'battingNotOuts': battingNotOuts, 'battingRuns': battingRuns,
    'battingHighestScore': battingHighestScore, 'battingBalls': battingBalls,
    'battingFours': battingFours, 'battingSixes': battingSixes,
    'battingFiftyCount': battingFiftyCount, 'battingHundredCount': battingHundredCount,
    'bowlingOversBalls': bowlingOversBalls, 'bowlingMaidens': bowlingMaidens,
    'bowlingRunsConceded': bowlingRunsConceded, 'bowlingWickets': bowlingWickets,
    'bowlingBestWickets': bowlingBestWickets, 'bowlingBestRunsConceded': bowlingBestRunsConceded,
    'bowlingWides': bowlingWides, 'bowlingNoBalls': bowlingNoBalls,
    'fieldingCatches': fieldingCatches, 'fieldingRunOuts': fieldingRunOuts,
    'fieldingStumpings': fieldingStumpings,
  };

  factory PlayerModel.fromMap(Map<String, dynamic> m) => PlayerModel(
    id: m['id'] as int? ?? 0,
    name: m['name'] as String,
    clubName: m['clubName'] as String? ?? '',
    avatarUrl: m['avatarUrl'] as String? ?? '',
    designRole: m['designRole'] as String? ?? 'ALL_ROUNDER',
    battingMatches: m['battingMatches'] as int? ?? 0,
    battingInnings: m['battingInnings'] as int? ?? 0,
    battingNotOuts: m['battingNotOuts'] as int? ?? 0,
    battingRuns: m['battingRuns'] as int? ?? 0,
    battingHighestScore: m['battingHighestScore'] as int? ?? 0,
    battingBalls: m['battingBalls'] as int? ?? 0,
    battingFours: m['battingFours'] as int? ?? 0,
    battingSixes: m['battingSixes'] as int? ?? 0,
    battingFiftyCount: m['battingFiftyCount'] as int? ?? 0,
    battingHundredCount: m['battingHundredCount'] as int? ?? 0,
    bowlingOversBalls: m['bowlingOversBalls'] as int? ?? 0,
    bowlingMaidens: m['bowlingMaidens'] as int? ?? 0,
    bowlingRunsConceded: m['bowlingRunsConceded'] as int? ?? 0,
    bowlingWickets: m['bowlingWickets'] as int? ?? 0,
    bowlingBestWickets: m['bowlingBestWickets'] as int? ?? 0,
    bowlingBestRunsConceded: m['bowlingBestRunsConceded'] as int? ?? 0,
    bowlingWides: m['bowlingWides'] as int? ?? 0,
    bowlingNoBalls: m['bowlingNoBalls'] as int? ?? 0,
    fieldingCatches: m['fieldingCatches'] as int? ?? 0,
    fieldingRunOuts: m['fieldingRunOuts'] as int? ?? 0,
    fieldingStumpings: m['fieldingStumpings'] as int? ?? 0,
  );
}
