import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../providers/cricket_providers.dart';
import '../../domain/scoring_engine.dart';
import '../theme/app_theme.dart';

class MatchCenterScreen extends ConsumerStatefulWidget {
  final int matchId;
  const MatchCenterScreen({super.key, required this.matchId});

  @override
  ConsumerState<MatchCenterScreen> createState() => _MatchCenterScreenState();
}

class _MatchCenterScreenState extends ConsumerState<MatchCenterScreen> with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(activeMatchIdProvider.notifier).state = widget.matchId;
    });
  }

  @override
  void dispose() { _tabController.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    final stateAsync = ref.watch(activeMatchStateProvider);
    return stateAsync.when(
      loading: () => const Scaffold(backgroundColor: kDarkBg, body: Center(child: CircularProgressIndicator(color: kTurfGreen))),
      error: (e, _) => Scaffold(body: Center(child: Text('Error: $e'))),
      data: (state) {
        if (state == null) return const Scaffold(body: Center(child: Text('Match not found')));
        return _buildCenter(state);
      },
    );
  }

  Widget _buildCenter(ComputedMatchState state) {
    final match = state.match;
    final isLive = match.status == 'LIVE';

    return Scaffold(
      backgroundColor: kDarkBg,
      appBar: AppBar(
        title: Text('${match.teamAName} vs ${match.teamBName}'),
        actions: [
          if (isLive)
            TextButton(
              onPressed: () => context.push('/scorer/${widget.matchId}'),
              child: const Text('SCORE', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
        ],
        bottom: TabBar(
          controller: _tabController,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white60,
          indicatorColor: Colors.white,
          tabs: const [Tab(text: 'LIVE'), Tab(text: 'SCORECARD'), Tab(text: 'SQUADS')],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          _LiveTab(state: state),
          _ScorecardTab(state: state),
          _SquadsTab(state: state),
        ],
      ),
    );
  }
}

// ─── Live Tab ─────────────────────────────────────────────────────────────────
class _LiveTab extends StatelessWidget {
  final ComputedMatchState state;
  const _LiveTab({required this.state});

  @override
  Widget build(BuildContext context) {
    final innings = state.activeInnings;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(12),
      child: Column(
        children: [
          // Score card
          _Card(
            child: Column(
              children: [
                Text(innings.battingTeamName, style: const TextStyle(color: kSlate400, fontWeight: FontWeight.bold, fontSize: 13)),
                const SizedBox(height: 4),
                Text('${innings.score}/${innings.wickets}', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 44)),
                Text('${innings.oversString} Overs  •  RR: ${innings.runRate.toStringAsFixed(2)}', style: const TextStyle(color: kSlate400, fontSize: 12)),
                if (state.innings2 != null && state.currentInningsNo == 2) ...[
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(color: kGoldAccent.withOpacity(0.15), borderRadius: BorderRadius.circular(20)),
                    child: Text('Need ${state.requiredRuns} off ${state.remainingBalls} balls  •  RRR: ${state.requiredRunRate.toStringAsFixed(2)}',
                      style: const TextStyle(color: kGoldAccent, fontWeight: FontWeight.bold, fontSize: 12)),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 10),
          // Active batters
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _TableHeader(cols: ['Batter', 'R', 'B', '4s', '6s', 'SR']),
                ...innings.batters.where((b) => !b.isDismissed || b.isStriker || b.isNonStriker).map((b) =>
                  _BatterRow(batter: b),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          // Active bowler
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _TableHeader(cols: ['Bowler', 'O', 'M', 'R', 'W', 'Eco']),
                ...innings.bowlers.where((b) => b.activeInOver || b.ballsBowled > 0).map((b) =>
                  _BowlerRow(bowler: b),
                ),
              ],
            ),
          ),
          if (state.resultSummary.isNotEmpty) ...[
            const SizedBox(height: 10),
            _Card(
              child: Text(state.resultSummary, textAlign: TextAlign.center, style: const TextStyle(color: kGoldAccent, fontWeight: FontWeight.bold, fontSize: 15)),
            ),
          ],
        ],
      ),
    );
  }
}

// ─── Scorecard Tab ────────────────────────────────────────────────────────────
class _ScorecardTab extends StatelessWidget {
  final ComputedMatchState state;
  const _ScorecardTab({required this.state});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(12),
      child: Column(children: [
        _InningsScorecard(innings: state.innings1, label: 'Innings 1'),
        if (state.innings2 != null) ...[
          const SizedBox(height: 12),
          _InningsScorecard(innings: state.innings2!, label: 'Innings 2'),
        ],
      ]),
    );
  }
}

class _InningsScorecard extends StatelessWidget {
  final InningsState innings;
  final String label;
  const _InningsScorecard({required this.innings, required this.label});

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
            Text('$label — ${innings.battingTeamName}', style: const TextStyle(color: kTurfGreen, fontWeight: FontWeight.bold, fontSize: 13)),
            Text('${innings.score}/${innings.wickets} (${innings.oversString})', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ]),
          const SizedBox(height: 8),
          const _TableHeader(cols: ['Batter', 'R', 'B', '4s', '6s', 'SR']),
          ...innings.batters.map((b) => _BatterRow(batter: b)),
          const Divider(color: kSlate700),
          // Extras
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
            Text('Extras (Wd:${innings.extras.wides} Nb:${innings.extras.noBalls} B:${innings.extras.byes} Lb:${innings.extras.legByes})', style: const TextStyle(color: kSlate400, fontSize: 12)),
            Text('${innings.extras.total}', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ]),
          const SizedBox(height: 8),
          // Bowling
          const _TableHeader(cols: ['Bowler', 'O', 'M', 'R', 'W', 'Eco']),
          ...innings.bowlers.where((b) => b.ballsBowled > 0).map((b) => _BowlerRow(bowler: b)),
          if (innings.fallOfWickets.isNotEmpty) ...[
            const Divider(color: kSlate700),
            const Text('Fall of Wickets', style: TextStyle(color: kSlate400, fontSize: 11, fontWeight: FontWeight.bold)),
            Wrap(
              spacing: 8, runSpacing: 4,
              children: innings.fallOfWickets.map((f) =>
                Text('${f.runs}-${f.wicketNo} (${f.batsmanName})', style: const TextStyle(color: Colors.white, fontSize: 11)),
              ).toList(),
            ),
          ],
        ],
      ),
    );
  }
}

// ─── Squads Tab ───────────────────────────────────────────────────────────────
class _SquadsTab extends StatelessWidget {
  final ComputedMatchState state;
  const _SquadsTab({required this.state});

  @override
  Widget build(BuildContext context) {
    final match = state.match;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(12),
      child: Column(children: [
        _SquadCard(teamName: match.teamAName, innings: state.innings1, isBatting: state.innings1.battingTeamId == match.teamAId),
        const SizedBox(height: 12),
        _SquadCard(teamName: match.teamBName, innings: state.innings1, isBatting: state.innings1.battingTeamId == match.teamBId),
      ]),
    );
  }
}

class _SquadCard extends StatelessWidget {
  final String teamName;
  final InningsState innings;
  final bool isBatting;
  const _SquadCard({required this.teamName, required this.innings, required this.isBatting});

  @override
  Widget build(BuildContext context) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(children: [
            CircleAvatar(radius: 16, backgroundColor: kTurfGreen, child: Text(teamName[0], style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900))),
            const SizedBox(width: 8),
            Text(teamName, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15)),
          ]),
          const SizedBox(height: 8),
          if (isBatting) ...innings.batters.map((b) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 3),
            child: Row(children: [
              const Icon(Icons.sports_cricket, color: kSlate400, size: 14),
              const SizedBox(width: 6),
              Expanded(child: Text(b.name, style: TextStyle(color: b.isDismissed ? kSlate400 : Colors.white, fontSize: 13))),
              Text('${b.runs} (${b.balls})', style: const TextStyle(color: kSlate400, fontSize: 12)),
            ]),
          ))
          else ...innings.bowlers.map((b) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 3),
            child: Row(children: [
              const Icon(Icons.radio_button_unchecked, color: kSlate400, size: 14),
              const SizedBox(width: 6),
              Expanded(child: Text(b.name, style: const TextStyle(color: Colors.white, fontSize: 13))),
              if (b.ballsBowled > 0) Text('${b.wickets}-${b.runsConceded}', style: const TextStyle(color: kSlate400, fontSize: 12)),
            ]),
          )),
        ],
      ),
    );
  }
}

// ─── Shared Row Widgets ───────────────────────────────────────────────────────
class _Card extends StatelessWidget {
  final Widget child;
  const _Card({required this.child});

  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(color: kSurface, borderRadius: BorderRadius.circular(12)),
    child: child,
  );
}

class _TableHeader extends StatelessWidget {
  final List<String> cols;
  const _TableHeader({required this.cols});

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 4),
    child: Row(
      children: cols.asMap().entries.map((e) => e.key == 0
          ? Expanded(child: Text(e.value, style: const TextStyle(color: kSlate400, fontSize: 11, fontWeight: FontWeight.bold)))
          : SizedBox(width: 38, child: Text(e.value, style: const TextStyle(color: kSlate400, fontSize: 11, fontWeight: FontWeight.bold), textAlign: TextAlign.right))
      ).toList(),
    ),
  );
}

class _BatterRow extends StatelessWidget {
  final BatterScore batter;
  const _BatterRow({required this.batter});

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 3),
    child: Row(
      children: [
        Expanded(child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Text(batter.name, style: TextStyle(color: batter.isDismissed ? kSlate400 : Colors.white, fontWeight: batter.isStriker ? FontWeight.bold : FontWeight.normal, fontSize: 13)),
              if (batter.isStriker) const Text(' *', style: TextStyle(color: kTurfGreen, fontWeight: FontWeight.w900)),
            ]),
            if (batter.isDismissed) Text(batter.dismissalDescription, style: const TextStyle(color: kSlate600, fontSize: 10)),
          ],
        )),
        for (final v in [batter.runs, batter.balls, batter.fours, batter.sixes])
          SizedBox(width: 38, child: Text('$v', style: const TextStyle(color: Colors.white, fontSize: 12), textAlign: TextAlign.right)),
        SizedBox(width: 38, child: Text(batter.strikeRate.toStringAsFixed(1), style: const TextStyle(color: kSlate400, fontSize: 12), textAlign: TextAlign.right)),
      ],
    ),
  );
}

class _BowlerRow extends StatelessWidget {
  final BowlerScore bowler;
  const _BowlerRow({required this.bowler});

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 3),
    child: Row(
      children: [
        Expanded(child: Text(bowler.name, style: TextStyle(color: Colors.white, fontWeight: bowler.activeInOver ? FontWeight.bold : FontWeight.normal, fontSize: 13))),
        SizedBox(width: 38, child: Text(bowler.oversString, style: const TextStyle(color: Colors.white, fontSize: 12), textAlign: TextAlign.right)),
        SizedBox(width: 38, child: Text('${bowler.maidens}', style: const TextStyle(color: Colors.white, fontSize: 12), textAlign: TextAlign.right)),
        SizedBox(width: 38, child: Text('${bowler.runsConceded}', style: const TextStyle(color: Colors.white, fontSize: 12), textAlign: TextAlign.right)),
        SizedBox(width: 38, child: Text('${bowler.wickets}', style: const TextStyle(color: Colors.white, fontSize: 12), textAlign: TextAlign.right)),
        SizedBox(width: 38, child: Text(bowler.economy.toStringAsFixed(2), style: const TextStyle(color: kSlate400, fontSize: 12), textAlign: TextAlign.right)),
      ],
    ),
  );
}
