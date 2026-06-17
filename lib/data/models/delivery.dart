class DeliveryModel {
  final int id;
  final int matchId;
  final int inningsNo;
  final int overIndex; // 0-based
  final int ballIndexInOver; // 1-based (legal balls)
  final int batsmanId;
  final String batsmanName;
  final int bowlerId;
  final String bowlerName;
  final int runsSimple; // runs from bat
  final String extraType; // NONE, WIDE, NO_BALL, BYE, LEG_BYE
  final int extraRuns;
  final bool isWicket;
  final String dismissalType; // NONE, BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, RETIRED_OUT
  final int dismissedBatsmanId;
  final String dismissedBatsmanName;
  final String fielderName;
  final int timestamp;

  const DeliveryModel({
    this.id = 0,
    required this.matchId,
    required this.inningsNo,
    required this.overIndex,
    required this.ballIndexInOver,
    required this.batsmanId,
    required this.batsmanName,
    required this.bowlerId,
    required this.bowlerName,
    required this.runsSimple,
    required this.extraType,
    required this.extraRuns,
    required this.isWicket,
    this.dismissalType = 'NONE',
    this.dismissedBatsmanId = 0,
    this.dismissedBatsmanName = '',
    this.fielderName = '',
    int? timestamp,
  }) : timestamp = timestamp ?? 0;

  Map<String, dynamic> toMap() => {
    'id': id == 0 ? null : id,
    'matchId': matchId, 'inningsNo': inningsNo,
    'overIndex': overIndex, 'ballIndexInOver': ballIndexInOver,
    'batsmanId': batsmanId, 'batsmanName': batsmanName,
    'bowlerId': bowlerId, 'bowlerName': bowlerName,
    'runsSimple': runsSimple, 'extraType': extraType, 'extraRuns': extraRuns,
    'isWicket': isWicket ? 1 : 0,
    'dismissalType': dismissalType,
    'dismissedBatsmanId': dismissedBatsmanId,
    'dismissedBatsmanName': dismissedBatsmanName,
    'fielderName': fielderName,
    'timestamp': timestamp,
  };

  factory DeliveryModel.fromMap(Map<String, dynamic> m) => DeliveryModel(
    id: m['id'] as int? ?? 0,
    matchId: m['matchId'] as int,
    inningsNo: m['inningsNo'] as int,
    overIndex: m['overIndex'] as int,
    ballIndexInOver: m['ballIndexInOver'] as int,
    batsmanId: m['batsmanId'] as int,
    batsmanName: m['batsmanName'] as String,
    bowlerId: m['bowlerId'] as int,
    bowlerName: m['bowlerName'] as String,
    runsSimple: m['runsSimple'] as int,
    extraType: m['extraType'] as String,
    extraRuns: m['extraRuns'] as int,
    isWicket: (m['isWicket'] as int? ?? 0) == 1,
    dismissalType: m['dismissalType'] as String? ?? 'NONE',
    dismissedBatsmanId: m['dismissedBatsmanId'] as int? ?? 0,
    dismissedBatsmanName: m['dismissedBatsmanName'] as String? ?? '',
    fielderName: m['fielderName'] as String? ?? '',
    timestamp: m['timestamp'] as int? ?? 0,
  );
}
