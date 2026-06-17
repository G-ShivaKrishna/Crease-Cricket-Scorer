import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../providers/cricket_providers.dart';
import '../../data/models/match.dart';
import '../theme/app_theme.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final matchesAsync = ref.watch(matchesProvider);

    return Scaffold(
      backgroundColor: kPolishBg,
      body: CustomScrollView(
        slivers: [
          // Header
          SliverAppBar(
            expandedHeight: 140,
            pinned: true,
            backgroundColor: kTurfGreen,
            flexibleSpace: FlexibleSpaceBar(
              background: Container(
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    colors: [Color(0xFF065F46), kTurfGreen],
                    begin: Alignment.topLeft, end: Alignment.bottomRight,
                  ),
                ),
                child: const SafeArea(
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        Text('CREASE', style: TextStyle(color: Colors.white70, fontSize: 12, letterSpacing: 4, fontWeight: FontWeight.bold)),
                        Text('Cricket Scorer', style: TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.w900)),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.add_circle_outline, color: Colors.white, size: 28),
                onPressed: () => context.push('/setup'),
              ),
            ],
          ),

          // Match list
          matchesAsync.when(
            loading: () => const SliverFillRemaining(child: Center(child: CircularProgressIndicator(color: kTurfGreen))),
            error: (e, _) => SliverFillRemaining(child: Center(child: Text('Error: $e'))),
            data: (matches) {
              if (matches.isEmpty) {
                return SliverFillRemaining(
                  child: Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.sports_cricket, color: kSlate400, size: 64),
                        const SizedBox(height: 16),
                        const Text('No matches yet', style: TextStyle(color: kSlate400, fontSize: 18, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 8),
                        const Text('Tap + to set up your first match', style: TextStyle(color: kSlate600, fontSize: 14)),
                        const SizedBox(height: 24),
                        ElevatedButton.icon(
                          onPressed: () => context.push('/setup'),
                          icon: const Icon(Icons.add),
                          label: const Text('New Match'),
                        ),
                      ],
                    ),
                  ),
                );
              }

              return SliverPadding(
                padding: const EdgeInsets.all(12),
                sliver: SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (ctx, i) => _MatchCard(match: matches[i], ref: ref),
                    childCount: matches.length,
                  ),
                ),
              );
            },
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/setup'),
        backgroundColor: kTurfGreen,
        icon: const Icon(Icons.sports_cricket),
        label: const Text('New Match', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
    );
  }
}

class _MatchCard extends StatelessWidget {
  final MatchModel match;
  final WidgetRef ref;
  const _MatchCard({required this.match, required this.ref});

  @override
  Widget build(BuildContext context) {
    final isLive = match.status == 'LIVE';
    final isDone = match.status == 'COMPLETED';

    return GestureDetector(
      onTap: () {
        ref.read(cricketNotifierProvider.notifier).setActiveMatch(match.id);
        if (isLive) {
          context.push('/scorer/${match.id}');
        } else {
          context.push('/center/${match.id}');
        }
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 10),
        decoration: BoxDecoration(
          color: kSurface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isLive ? kTurfGreen.withOpacity(0.5) : kSlate700,
            width: isLive ? 1.5 : 0.5,
          ),
        ),
        child: Column(
          children: [
            // Status bar
            Container(
              decoration: BoxDecoration(
                color: isLive ? kTurfGreen : isDone ? kGoldAccent : kSlate700,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
              ),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    isLive ? 'LIVE' : isDone ? 'COMPLETED' : match.status,
                    style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 1.5),
                  ),
                  Text(match.scheduledDate, style: const TextStyle(color: Colors.white70, fontSize: 11)),
                ],
              ),
            ),
            // Teams
            Padding(
              padding: const EdgeInsets.all(14),
              child: Row(
                children: [
                  _TeamChip(name: match.teamAName),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 10),
                    child: Text('VS', style: TextStyle(color: kSlate400, fontWeight: FontWeight.w900, fontSize: 12)),
                  ),
                  _TeamChip(name: match.teamBName),
                  const Spacer(),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text('${match.oversCount} Ov', style: const TextStyle(color: kSlate400, fontSize: 12)),
                      Text(match.venue.isNotEmpty ? match.venue : 'TBD', style: const TextStyle(color: kSlate600, fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
                    ],
                  ),
                ],
              ),
            ),
            // Score summary if available
            if (match.firstInningsScore > 0 || match.status != 'PRE_MATCH')
              Padding(
                padding: const EdgeInsets.only(left: 14, right: 14, bottom: 12),
                child: Row(
                  children: [
                    Text(
                      '${match.firstInningsScore}/${match.firstInningsWickets}',
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18),
                    ),
                    if (match.secondInningsScore > 0) ...[
                      const Text(' • ', style: TextStyle(color: kSlate600)),
                      Text(
                        '${match.secondInningsScore}/${match.secondInningsWickets}',
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18),
                      ),
                    ],
                    if (match.winningMargin.isNotEmpty) ...[
                      const Spacer(),
                      Flexible(child: Text(match.winningMargin, style: const TextStyle(color: kGoldAccent, fontSize: 11, fontWeight: FontWeight.bold), textAlign: TextAlign.right)),
                    ],
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _TeamChip extends StatelessWidget {
  final String name;
  const _TeamChip({required this.name});

  @override
  Widget build(BuildContext context) {
    final initial = name.isNotEmpty ? name[0].toUpperCase() : '?';
    return Row(
      children: [
        CircleAvatar(backgroundColor: kTurfGreen, radius: 16, child: Text(initial, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 14))),
        const SizedBox(width: 8),
        Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
      ],
    );
  }
}
