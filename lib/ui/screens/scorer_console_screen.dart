import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../providers/cricket_providers.dart';
import '../../domain/scoring_engine.dart';
import '../../data/models/player.dart';
import '../theme/app_theme.dart';

class ScorerConsoleScreen extends ConsumerStatefulWidget {
  final int matchId;
  const ScorerConsoleScreen({super.key, required this.matchId});

  @override
  ConsumerState<ScorerConsoleScreen> createState() => _ScorerConsoleScreenState();
}

class _ScorerConsoleScreenState extends ConsumerState<ScorerConsoleScreen> {
  int _strikerId = 0, _nonStrikerId = 0, _bowlerId = 0;
  bool _dismissedOverDialog = false;
  int _lastOverIndex = -1;

  // Wicket dialog state
  String _wicketType = 'BOWLED';
  int _wicketDismissedId = 0;
  String _wicketFielder = '';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(activeMatchIdProvider.notifier).state = widget.matchId;
    });
  }

  @override
  Widget build(BuildContext context) {
    final stateAsync = ref.watch(activeMatchStateProvider);
    return stateAsync.when(
      loading: () => const Scaffold(backgroundColor: kDarkBg, body: Center(child: CircularProgressIndicator(color: kTurfGreen))),
      error: (e, _) => Scaffold(body: Center(child: Text('Error: $e'))),
      data: (state) {
        if (state == null) return const Scaffold(body: Center(child: Text('Match not found')));
        _syncActiveIds(state.activeInnings);
        return _buildScorer(state);
      },
    );
  }

  void _syncActiveIds(InningsState innings) {
    if (_strikerId == 0 && innings.activeStrikerId != 0) _strikerId = innings.activeStrikerId;
    if (_nonStrikerId == 0 && innings.activeNonStrikerId != 0) _nonStrikerId = innings.activeNonStrikerId;
    if (_bowlerId == 0 && innings.activeBowlerId != 0) _bowlerId = innings.activeBowlerId;

    // Auto-populate from not-dismissed batters if still 0
    if (_strikerId == 0) {
      final nonDismissed = innings.batters.where((b) => !b.isDismissed).toList();
      if (nonDismissed.isNotEmpty) _strikerId = nonDismissed[0].playerId;
      if (nonDismissed.length > 1) _nonStrikerId = nonDismissed[1].playerId;
    }

    // Reset dismissed-over flag when the over index changes
    final overIdx = innings.ballsBowled ~/ (6);
    if (overIdx != _lastOverIndex && _lastOverIndex != -1) _dismissedOverDialog = false;
    _lastOverIndex = overIdx;
  }

  Widget _buildScorer(ComputedMatchState state) {
    final innings = state.activeInnings;
    final match = state.match;
    final playersMap = ref.watch(playersProvider).valueOrNull ?? [];
    final pMap = {for (final p in playersMap) p.id: p};

    // Over completion dialog trigger
    final showBowlerChange = !_dismissedOverDialog &&
        innings.ballsBowled > 0 &&
        innings.ballsBowled % match.ballsPerOver == 0 &&
        (innings.currentOverBalls.isEmpty || innings.currentOverBalls.last.ballIndexInOver == match.ballsPerOver);

    return Scaffold(
      backgroundColor: kDarkBg,
      body: Stack(
        children: [
          Column(
            children: [
              _buildHeader(state, context),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      _buildScoreBoard(innings, state),
                      _buildPlayerSelectors(innings, pMap, match),
                      _buildOverBalls(innings),
                      _buildRunButtons(state),
                      _buildExtrasRow(state),
                      _buildWicketButton(state, pMap),
                      const SizedBox(height: 80),
                    ],
                  ),
                ),
              ),
            ],
          ),
          if (showBowlerChange) _buildBowlerChangeOverlay(innings, pMap, match),
          if (state.isCompleted) _buildMatchCompletedBanner(state),
        ],
      ),
    );
  }

  // ─── Header ────────────────────────────────────────────────
  Widget _buildHeader(ComputedMatchState state, BuildContext context) {
    return Container(
      color: kTurfGreen,
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child: Row(
            children: [
              IconButton(icon: const Icon(Icons.arrow_back, color: Colors.white), onPressed: () => context.pop()),
              const Expanded(child: Text('SCORER CONSOLE', textAlign: TextAlign.center, style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14, letterSpacing: 1))),
              IconButton(icon: const Icon(Icons.undo, color: Colors.white), onPressed: () => ref.read(cricketNotifierProvider.notifier).undoLastBall(widget.matchId)),
              ElevatedButton(
                style: ElevatedButton.styleFrom(backgroundColor: kGoldAccent, foregroundColor: Colors.white, padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))),
                onPressed: () => context.push('/center/${widget.matchId}'),
                child: const Text('CENTER', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11)),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ─── Scoreboard ────────────────────────────────────────────
  Widget _buildScoreBoard(InningsState innings, ComputedMatchState state) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        gradient: LinearGradient(colors: [Color(0xFF064E3B), kDarkBg], begin: Alignment.topCenter, end: Alignment.bottomCenter),
      ),
      child: Column(
        children: [
          Text(innings.battingTeamName, style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 1)),
          const SizedBox(height: 6),
          Text('${innings.score}/${innings.wickets}', style: const TextStyle(color: Colors.white, fontSize: 52, fontWeight: FontWeight.w900, letterSpacing: -1)),
          Text('${innings.oversString} Overs', style: const TextStyle(color: kSlate400, fontSize: 14)),
          if (state.innings2 != null && state.currentInningsNo == 2) ...[
            const SizedBox(height: 4),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(color: kGoldAccent.withOpacity(0.2), borderRadius: BorderRadius.circular(20)),
              child: Text('Need ${state.requiredRuns} off ${state.remainingBalls} balls', style: const TextStyle(color: kGoldAccent, fontWeight: FontWeight.bold, fontSize: 12)),
            ),
          ],
        ],
      ),
    );
  }

  // ─── Player Selectors ──────────────────────────────────────
  Widget _buildPlayerSelectors(InningsState innings, Map<int, PlayerModel> pMap, match) {
    final striker = innings.batters.firstWhere((b) => b.playerId == _strikerId, orElse: () => const BatterScore(playerId: 0, name: 'Select Striker'));
    final nonStriker = innings.batters.firstWhere((b) => b.playerId == _nonStrikerId, orElse: () => const BatterScore(playerId: 0, name: 'Select Non-Striker'));
    final bowlerScore = innings.bowlers.firstWhere((b) => b.playerId == _bowlerId, orElse: () => const BowlerScore(playerId: 0, name: 'Select Bowler'));

    final bowlingSquad = _getBowlingSquad(innings, match, pMap);

    return Padding(
      padding: const EdgeInsets.all(8),
      child: Column(
        children: [
          // Batters Row
          Row(children: [
            Expanded(child: _PlayerCard(
              label: 'STRIKER *',
              name: striker.name,
              stat: '${striker.runs} (${striker.balls})',
              labelColor: kTurfGreen,
              players: innings.batters.where((b) => !b.isDismissed && b.playerId != _nonStrikerId).toList().map((b) => MapEntry(b.playerId, b.name)).toList(),
              onSelect: (id) => setState(() => _strikerId = id),
            )),
            const SizedBox(width: 8),
            Expanded(child: _PlayerCard(
              label: 'NON-STRIKER',
              name: nonStriker.name,
              stat: '${nonStriker.runs} (${nonStriker.balls})',
              labelColor: kSlate400,
              players: innings.batters.where((b) => !b.isDismissed && b.playerId != _strikerId).toList().map((b) => MapEntry(b.playerId, b.name)).toList(),
              onSelect: (id) => setState(() => _nonStrikerId = id),
            )),
          ]),
          const SizedBox(height: 8),
          // Bowler
          _PlayerCard(
            label: 'BOWLER',
            name: bowlerScore.name.isEmpty ? 'Tap to select' : bowlerScore.name,
            stat: '${bowlerScore.wickets}-${bowlerScore.runsConceded} (${bowlerScore.oversString} Ov)',
            labelColor: const Color(0xFF34D399),
            players: bowlingSquad,
            onSelect: (id) => setState(() => _bowlerId = id),
          ),
        ],
      ),
    );
  }

  List<MapEntry<int, String>> _getBowlingSquad(InningsState innings, match, Map<int, PlayerModel> pMap) {
    final bowlingTeamId = innings.bowlingTeamId;
    final squadStr = bowlingTeamId == match.teamAId ? match.teamASquadIds : match.teamBSquadIds;
    final ids = squadStr.split(',').where((s) => s.isNotEmpty).map(int.tryParse).whereType<int>().toList();
    return ids.map((id) => MapEntry(id, pMap[id]?.name ?? 'Player $id')).toList();
  }

  // ─── Current Over Balls ────────────────────────────────────
  Widget _buildOverBalls(InningsState innings) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      child: Row(
        children: [
          const Text('This Over: ', style: TextStyle(color: kSlate400, fontSize: 12)),
          ...innings.currentOverBalls.map((d) {
            String label;
            Color color = Colors.white;
            if (d.isWicket) { label = 'W'; color = Colors.red; }
            else if (d.extraType == 'WIDE') { label = 'Wd'; color = kGoldAccent; }
            else if (d.extraType == 'NO_BALL') { label = 'Nb'; color = kGoldAccent; }
            else if (d.extraType == 'BYE') { label = 'B${d.extraRuns}'; color = Colors.orange; }
            else if (d.extraType == 'LEG_BYE') { label = 'Lb${d.extraRuns}'; color = Colors.orange; }
            else { label = d.runsSimple == 0 ? '•' : '${d.runsSimple}'; color = d.runsSimple == 4 ? Colors.blue : d.runsSimple == 6 ? kGoldAccent : Colors.white; }
            return Container(
              margin: const EdgeInsets.only(right: 6),
              width: 30, height: 30,
              decoration: BoxDecoration(color: color.withOpacity(0.15), shape: BoxShape.circle, border: Border.all(color: color.withOpacity(0.5))),
              child: Center(child: Text(label, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 11))),
            );
          }),
        ],
      ),
    );
  }

  // ─── Run Buttons ───────────────────────────────────────────
  Widget _buildRunButtons(ComputedMatchState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Column(
        children: [
          Row(children: [0, 1, 2, 3, 4, 6].map((r) => Expanded(
            child: Padding(
              padding: const EdgeInsets.all(3),
              child: _RunBtn(
                label: r == 0 ? 'Dot' : '$r',
                color: r == 4 ? kTurfLime : r == 6 ? kGoldAccent : kSurface,
                onTap: () => _record(state, runs: r),
              ),
            ),
          )).toList()),
          const SizedBox(height: 4),
          Row(children: [5, 7].map((r) => Expanded(
            child: Padding(
              padding: const EdgeInsets.all(3),
              child: _RunBtn(label: '$r', color: kSurface, onTap: () => _record(state, runs: r)),
            ),
          )).toList()),
        ],
      ),
    );
  }

  // ─── Extras Row ────────────────────────────────────────────
  Widget _buildExtrasRow(ComputedMatchState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(children: [
        _ExtraBtn('WIDE', () => _record(state, extra: 'WIDE', extraRuns: 1)),
        _ExtraBtn('NO BALL', () => _record(state, extra: 'NO_BALL', extraRuns: 1)),
        _ExtraBtn('BYE', () => _record(state, extra: 'BYE', extraRuns: 1)),
        _ExtraBtn('LEG BYE', () => _record(state, extra: 'LEG_BYE', extraRuns: 1)),
      ].map((w) => Expanded(child: Padding(padding: const EdgeInsets.all(3), child: w))).toList()),
    );
  }

  // ─── Wicket Button ─────────────────────────────────────────
  Widget _buildWicketButton(ComputedMatchState state, Map<int, PlayerModel> pMap) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: SizedBox(
        height: 48, width: double.infinity,
        child: OutlinedButton(
          style: OutlinedButton.styleFrom(
            foregroundColor: Colors.red,
            side: const BorderSide(color: Colors.red, width: 1.5),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
          onPressed: () => _showWicketDialog(state, pMap),
          child: const Text('OUT - WICKET', style: TextStyle(fontWeight: FontWeight.w900, fontSize: 15, letterSpacing: 1)),
        ),
      ),
    );
  }

  // ─── Bowler Change Overlay ─────────────────────────────────
  Widget _buildBowlerChangeOverlay(InningsState innings, Map<int, PlayerModel> pMap, match) {
    int localBowlerId = 0;
    final bowlingSquad = _getBowlingSquad(innings, match, pMap)
        .where((e) => e.key != innings.activeBowlerId).toList();

    return StatefulBuilder(
      builder: (ctx, setOverlay) => Container(
        color: Colors.black87,
        child: Center(
          child: Container(
            margin: const EdgeInsets.all(24),
            decoration: BoxDecoration(color: kSurface, borderRadius: BorderRadius.circular(16)),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('Over Completed!', style: TextStyle(color: kTurfGreen, fontWeight: FontWeight.w900, fontSize: 18)),
                    SizedBox(height: 4),
                    Text('Choose bowler for the next over:', style: TextStyle(color: kSlate400, fontSize: 13)),
                  ]),
                ),
                const Divider(height: 0),
                SizedBox(
                  height: 220,
                  child: ListView.builder(
                    itemCount: bowlingSquad.length,
                    itemBuilder: (_, i) {
                      final e = bowlingSquad[i];
                      final isSelected = localBowlerId == e.key;
                      final bowled = innings.bowlers.firstWhere((b) => b.playerId == e.key, orElse: () => BowlerScore(playerId: e.key, name: e.value));
                      return ListTile(
                        selected: isSelected,
                        selectedTileColor: kTurfLime.withOpacity(0.1),
                        leading: CircleAvatar(radius: 16, backgroundColor: isSelected ? kTurfGreen : kSlate700, child: Text(e.value[0], style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold))),
                        title: Text(e.value, style: TextStyle(color: isSelected ? Colors.white : kSlate400, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
                        trailing: Text(bowled.ballsBowled > 0 ? '${bowled.wickets}-${bowled.runsConceded} (${bowled.oversString})' : 'Yet to bowl', style: const TextStyle(color: kSlate400, fontSize: 12)),
                        onTap: () => setOverlay(() => localBowlerId = e.key),
                      );
                    },
                  ),
                ),
                const Divider(height: 0),
                Padding(
                  padding: const EdgeInsets.all(12),
                  child: SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: localBowlerId != 0 ? () { setState(() { _bowlerId = localBowlerId; _dismissedOverDialog = true; }); } : null,
                      child: const Text('Start Next Over', style: TextStyle(fontWeight: FontWeight.bold)),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMatchCompletedBanner(ComputedMatchState state) {
    return Positioned(
      bottom: 0, left: 0, right: 0,
      child: Container(
        padding: const EdgeInsets.all(16),
        color: kGoldAccent,
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text('MATCH COMPLETE', style: const TextStyle(color: Colors.black, fontWeight: FontWeight.w900, fontSize: 16, letterSpacing: 1)),
          Text(state.resultSummary, style: const TextStyle(color: Colors.black87, fontSize: 13)),
          const SizedBox(height: 8),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.black, foregroundColor: Colors.white),
            onPressed: () => context.replace('/center/${widget.matchId}'),
            child: const Text('View Scorecard'),
          ),
        ]),
      ),
    );
  }

  // ─── Wicket Dialog ─────────────────────────────────────────
  void _showWicketDialog(ComputedMatchState state, Map<int, PlayerModel> pMap) {
    _wicketType = 'BOWLED';
    _wicketDismissedId = _strikerId;
    _wicketFielder = '';

    final innings = state.activeInnings;
    final match = state.match;
    final bowlingSquad = _getBowlingSquad(innings, match, pMap);

    showDialog(
      context: context,
      builder: (_) => StatefulBuilder(
        builder: (ctx, setDlg) {
          final needsFielder = ['CAUGHT', 'RUN_OUT', 'STUMPED'].contains(_wicketType);
          final needsBatterChoice = _wicketType == 'RUN_OUT';
          final striker = innings.batters.firstWhere((b) => b.playerId == _strikerId, orElse: () => const BatterScore(playerId: 0, name: 'Striker'));
          final nonStriker = innings.batters.firstWhere((b) => b.playerId == _nonStrikerId, orElse: () => const BatterScore(playerId: 0, name: 'Non-Striker'));

          return AlertDialog(
            backgroundColor: kSurface,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            title: const Text('Wicket Details', style: TextStyle(color: Colors.red, fontWeight: FontWeight.w900)),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Dismissal Type', style: TextStyle(color: kSlate400, fontSize: 12, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 4),
                  ...['BOWLED', 'CAUGHT', 'LBW', 'RUN_OUT', 'STUMPED', 'HIT_WICKET', 'RETIRED_OUT'].map((t) =>
                    RadioListTile<String>(
                      dense: true,
                      value: t, groupValue: _wicketType,
                      title: Text(t, style: const TextStyle(color: Colors.white, fontSize: 13)),
                      activeColor: Colors.red,
                      onChanged: (v) => setDlg(() => _wicketType = v!),
                    )
                  ),

                  const Divider(color: kSlate700),

                  if (needsBatterChoice) ...[
                    const Text('Who got Run Out?', style: TextStyle(color: kSlate400, fontSize: 12, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 6),
                    Row(children: [
                      Expanded(child: _SelectionBtn(label: striker.name, selected: _wicketDismissedId == _strikerId, onTap: () => setDlg(() => _wicketDismissedId = _strikerId))),
                      const SizedBox(width: 8),
                      Expanded(child: _SelectionBtn(label: nonStriker.name, selected: _wicketDismissedId == _nonStrikerId, onTap: () => setDlg(() => _wicketDismissedId = _nonStrikerId))),
                    ]),
                    const SizedBox(height: 12),
                  ] else ...[
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                      child: Row(children: [
                        const Icon(Icons.person, color: Colors.red, size: 16),
                        const SizedBox(width: 8),
                        Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          const Text('Dismissed (On Strike)', style: TextStyle(color: kSlate400, fontSize: 10)),
                          Text(striker.name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
                        ]),
                      ]),
                    ),
                    const SizedBox(height: 12),
                  ],

                  if (needsFielder) ...[
                    Text(_wicketType == 'STUMPED' ? 'Wicket-Keeper' : 'Select Fielder', style: const TextStyle(color: kSlate400, fontSize: 12, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    Container(
                      height: 150,
                      decoration: BoxDecoration(border: Border.all(color: kSlate700), borderRadius: BorderRadius.circular(8)),
                      child: ListView(
                        children: bowlingSquad.map((e) {
                          final isSelected = _wicketFielder == e.value;
                          return ListTile(
                            dense: true,
                            title: Text(e.value, style: TextStyle(color: isSelected ? Colors.red : Colors.white, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal, fontSize: 13)),
                            trailing: isSelected ? const Icon(Icons.check_circle, color: Colors.red, size: 18) : null,
                            onTap: () => setDlg(() => _wicketFielder = e.value),
                          );
                        }).toList(),
                      ),
                    ),
                    if (_wicketFielder.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text('Selected: $_wicketFielder', style: const TextStyle(color: Colors.red, fontSize: 11, fontWeight: FontWeight.w600)),
                    ],
                  ],
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel', style: TextStyle(color: kSlate400))),
              ElevatedButton(
                style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
                onPressed: () {
                  Navigator.pop(ctx);
                  _record(state, isWicket: true, dismissal: _wicketType, dismissedId: _wicketDismissedId, fielder: _wicketFielder);
                },
                child: const Text('Confirm Wicket', style: TextStyle(fontWeight: FontWeight.bold)),
              ),
            ],
          );
        },
      ),
    );
  }

  // ─── Record Delivery ───────────────────────────────────────
  Future<void> _record(ComputedMatchState state, {int runs = 0, String extra = 'NONE', int extraRuns = 0, bool isWicket = false, String dismissal = 'NONE', int dismissedId = 0, String fielder = ''}) async {
    if (_strikerId == 0 || _bowlerId == 0) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Select striker and bowler first'), backgroundColor: kSurface));
      return;
    }
    await ref.read(cricketNotifierProvider.notifier).recordDelivery(
      matchId: widget.matchId, state: state,
      runsSimple: runs, extraType: extra, extraRuns: extraRuns,
      isWicket: isWicket, dismissalType: isWicket ? dismissal : 'NONE',
      dismissedBatsmanId: isWicket ? (dismissedId != 0 ? dismissedId : _strikerId) : 0,
      fielderName: fielder, strikerId: _strikerId,
      nonStrikerId: _nonStrikerId, bowlerId: _bowlerId,
    );
  }
}

// ─── Helper Widgets ────────────────────────────────────────────────────────────

class _PlayerCard extends StatelessWidget {
  final String label, name, stat;
  final Color labelColor;
  final List<MapEntry<int, String>> players;
  final void Function(int) onSelect;

  const _PlayerCard({required this.label, required this.name, required this.stat, required this.labelColor, required this.players, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => showModalBottomSheet(
        context: context,
        backgroundColor: kSurface,
        shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
        builder: (_) => ListView(
          shrinkWrap: true,
          children: [
            Padding(padding: const EdgeInsets.all(14), child: Text(label, style: const TextStyle(color: kSlate400, fontWeight: FontWeight.bold))),
            ...players.map((e) => ListTile(
              title: Text(e.value, style: const TextStyle(color: Colors.white)),
              onTap: () { onSelect(e.key); Navigator.pop(context); },
            )),
          ],
        ),
      ),
      child: Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(color: Colors.white.withOpacity(0.07), borderRadius: BorderRadius.circular(10)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: TextStyle(color: labelColor, fontSize: 9, fontWeight: FontWeight.bold, letterSpacing: 0.5)),
          const SizedBox(height: 4),
          Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13), maxLines: 1, overflow: TextOverflow.ellipsis),
          Text(stat, style: const TextStyle(color: kSlate400, fontSize: 11)),
        ]),
      ),
    );
  }
}

class _RunBtn extends StatelessWidget {
  final String label;
  final Color color;
  final VoidCallback onTap;
  const _RunBtn({required this.label, required this.color, required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
    onTap: onTap,
    child: Container(
      height: 52,
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(10), border: Border.all(color: kSlate700, width: 0.5)),
      child: Center(child: Text(label, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 16))),
    ),
  );
}

class _ExtraBtn extends StatelessWidget {
  final String label;
  final VoidCallback onTap;
  const _ExtraBtn(this.label, this.onTap);

  @override
  Widget build(BuildContext context) => GestureDetector(
    onTap: onTap,
    child: Container(
      height: 36,
      decoration: BoxDecoration(color: kGoldAccent.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(8), border: Border.all(color: kGoldAccent.withValues(alpha: 0.3))),
      child: Center(child: Text(label, style: const TextStyle(color: kGoldAccent, fontWeight: FontWeight.bold, fontSize: 11))),
    ),
  );
}

class _SelectionBtn extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _SelectionBtn({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
    onTap: onTap,
    child: Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: BoxDecoration(
        color: selected ? Colors.red : kDarkBg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: selected ? Colors.red : kSlate700),
      ),
      child: Center(child: Text(label, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12), maxLines: 1, overflow: TextOverflow.ellipsis)),
    ),
  );
}
