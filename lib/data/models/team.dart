class TeamModel {
  final int id;
  final String name;
  final String logoUrl;
  final int matchesPlayed;
  final int wins;
  final int losses;
  final int ties;
  final int noResults;
  final int totalRunsScored;
  final int totalBallsFaced;
  final int totalRunsConceded;
  final int totalBallsBowled;

  const TeamModel({
    this.id = 0,
    required this.name,
    this.logoUrl = '',
    this.matchesPlayed = 0,
    this.wins = 0,
    this.losses = 0,
    this.ties = 0,
    this.noResults = 0,
    this.totalRunsScored = 0,
    this.totalBallsFaced = 0,
    this.totalRunsConceded = 0,
    this.totalBallsBowled = 0,
  });

  double get netRunRate {
    final oversFaced = totalBallsFaced / 6.0;
    final oversBowled = totalBallsBowled / 6.0;
    final batRate = oversFaced > 0 ? totalRunsScored / oversFaced : 0.0;
    final bowlRate = oversBowled > 0 ? totalRunsConceded / oversBowled : 0.0;
    return batRate - bowlRate;
  }

  Map<String, dynamic> toMap() => {
    'id': id == 0 ? null : id,
    'name': name, 'logoUrl': logoUrl,
    'matchesPlayed': matchesPlayed, 'wins': wins, 'losses': losses,
    'ties': ties, 'noResults': noResults,
    'totalRunsScored': totalRunsScored, 'totalBallsFaced': totalBallsFaced,
    'totalRunsConceded': totalRunsConceded, 'totalBallsBowled': totalBallsBowled,
  };

  factory TeamModel.fromMap(Map<String, dynamic> m) => TeamModel(
    id: m['id'] as int? ?? 0,
    name: m['name'] as String,
    logoUrl: m['logoUrl'] as String? ?? '',
    matchesPlayed: m['matchesPlayed'] as int? ?? 0,
    wins: m['wins'] as int? ?? 0,
    losses: m['losses'] as int? ?? 0,
    ties: m['ties'] as int? ?? 0,
    noResults: m['noResults'] as int? ?? 0,
    totalRunsScored: m['totalRunsScored'] as int? ?? 0,
    totalBallsFaced: m['totalBallsFaced'] as int? ?? 0,
    totalRunsConceded: m['totalRunsConceded'] as int? ?? 0,
    totalBallsBowled: m['totalBallsBowled'] as int? ?? 0,
  );
}
