import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../providers/cricket_providers.dart';
import '../../data/models/player.dart';
import '../theme/app_theme.dart';

class MatchSetupScreen extends ConsumerStatefulWidget {
  const MatchSetupScreen({super.key});

  @override
  ConsumerState<MatchSetupScreen> createState() => _MatchSetupScreenState();
}

class _MatchSetupScreenState extends ConsumerState<MatchSetupScreen> {
  final _teamACtrl = TextEditingController();
  final _teamBCtrl = TextEditingController();
  final _venueCtrl = TextEditingController();
  final _oversCtrl = TextEditingController(text: '5');

  final _teamASelected = <PlayerModel>[];
  final _teamBSelected = <PlayerModel>[];

  bool _showToss = false;
  bool _tossWinnerIsTeamA = true;
  String _tossDecision = 'BAT';
  bool _loading = false;

  @override
  void dispose() {
    _teamACtrl.dispose(); _teamBCtrl.dispose();
    _venueCtrl.dispose(); _oversCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final playersAsync = ref.watch(playersProvider);

    return Scaffold(
      backgroundColor: kPolishBg,
      appBar: AppBar(title: const Text('MATCH SETUP')),
      body: playersAsync.when(
        loading: () => const Center(child: CircularProgressIndicator(color: kTurfGreen)),
        error: (e, _) => Center(child: Text('Error: $e')),
        data: (allPlayers) => _buildBody(allPlayers),
      ),
    );
  }

  Widget _buildBody(List<PlayerModel> allPlayers) {
    final teamAName = _teamACtrl.text.trim();
    final teamBName = _teamBCtrl.text.trim();
    final teamAPlayers = allPlayers.where((p) => p.clubName.toLowerCase() == teamAName.toLowerCase()).toList();
    final teamBPlayers = allPlayers.where((p) => p.clubName.toLowerCase() == teamBName.toLowerCase()).toList();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _sectionHeader('Teams'),
          const SizedBox(height: 8),
          TextField(controller: _teamACtrl, decoration: const InputDecoration(labelText: 'Team A Name'), onChanged: (_) => setState(() {})),
          const SizedBox(height: 10),
          TextField(controller: _teamBCtrl, decoration: const InputDecoration(labelText: 'Team B Name'), onChanged: (_) => setState(() {})),
          const SizedBox(height: 20),

          _sectionHeader('Match Settings'),
          const SizedBox(height: 8),
          TextField(controller: _venueCtrl, decoration: const InputDecoration(labelText: 'Venue (optional)')),
          const SizedBox(height: 10),
          TextField(
            controller: _oversCtrl,
            decoration: const InputDecoration(labelText: 'Overs'),
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 20),

          if (teamAName.isNotEmpty && teamBPlayers.isNotEmpty) ...[
            _sectionHeader('${teamAName} Playing XI'),
            const SizedBox(height: 6),
            _PlayerSelector(
              players: teamAPlayers,
              allPlayers: allPlayers,
              selected: _teamASelected,
              teamName: teamAName,
              onChanged: (list) => setState(() { _teamASelected.clear(); _teamASelected.addAll(list); }),
            ),
            const SizedBox(height: 20),
          ],

          if (teamBName.isNotEmpty && teamBPlayers.isNotEmpty) ...[
            _sectionHeader('${teamBName} Playing XI'),
            const SizedBox(height: 6),
            _PlayerSelector(
              players: teamBPlayers,
              allPlayers: allPlayers,
              selected: _teamBSelected,
              teamName: teamBName,
              onChanged: (list) => setState(() { _teamBSelected.clear(); _teamBSelected.addAll(list); }),
            ),
            const SizedBox(height: 20),
          ],

          SizedBox(
            height: 52,
            child: ElevatedButton(
              onPressed: _loading ? null : _onProceed,
              child: _loading
                  ? const CircularProgressIndicator(color: Colors.white, strokeWidth: 2)
                  : const Text('Proceed to Toss & Play', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)),
            ),
          ),
          const SizedBox(height: 40),
        ],
      ),
    );
  }

  void _onProceed() {
    final a = _teamACtrl.text.trim();
    final b = _teamBCtrl.text.trim();
    if (a.isEmpty || b.isEmpty) { _toast('Enter both team names'); return; }
    if (a.toLowerCase() == b.toLowerCase()) { _toast('Teams must have distinct names'); return; }
    _buildTeamsIfNeeded();
    setState(() => _showToss = true);
    _showTossDialog();
  }

  void _buildTeamsIfNeeded() {
    final teamAName = _teamACtrl.text.trim();
    final teamBName = _teamBCtrl.text.trim();
    // If no players pre-selected, create quick players
    if (_teamASelected.isEmpty) {
      for (int i = 1; i <= 11; i++) {
        _teamASelected.add(PlayerModel(name: 'Player $i', clubName: teamAName));
      }
    }
    if (_teamBSelected.isEmpty) {
      for (int i = 1; i <= 11; i++) {
        _teamBSelected.add(PlayerModel(name: 'Player $i', clubName: teamBName));
      }
    }
  }

  void _showTossDialog() {
    final a = _teamACtrl.text.trim();
    final b = _teamBCtrl.text.trim();
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => StatefulBuilder(
        builder: (ctx, setDlg) => AlertDialog(
          backgroundColor: kSurface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Text('Toss', style: TextStyle(color: kTurfGreen, fontWeight: FontWeight.w900)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('$a  vs  $b', style: const TextStyle(color: kSlate400, fontSize: 13)),
              const Divider(color: kSlate700),
              const Text('Who won the toss?', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              Row(children: [
                Expanded(child: _TossBtn(label: a, selected: _tossWinnerIsTeamA, onTap: () => setDlg(() => _tossWinnerIsTeamA = true))),
                const SizedBox(width: 8),
                Expanded(child: _TossBtn(label: b, selected: !_tossWinnerIsTeamA, onTap: () => setDlg(() => _tossWinnerIsTeamA = false))),
              ]),
              const SizedBox(height: 14),
              Text('${_tossWinnerIsTeamA ? a : b} elected to:', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              Row(children: [
                Expanded(child: _TossBtn(label: 'BAT FIRST', selected: _tossDecision == 'BAT', onTap: () => setDlg(() => _tossDecision = 'BAT'))),
                const SizedBox(width: 8),
                Expanded(child: _TossBtn(label: 'BOWL FIRST', selected: _tossDecision == 'BOWL', onTap: () => setDlg(() => _tossDecision = 'BOWL'))),
              ]),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(color: kTurfGreen.withOpacity(0.12), borderRadius: BorderRadius.circular(8)),
                child: Text(
                  '${_tossWinnerIsTeamA ? a : b} won the toss and elected to ${_tossDecision == 'BAT' ? 'bat' : 'bowl'} first',
                  style: const TextStyle(color: kTurfLime, fontSize: 12, fontWeight: FontWeight.w600),
                  textAlign: TextAlign.center,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Back', style: TextStyle(color: kSlate400))),
            ElevatedButton(
              onPressed: () { Navigator.pop(ctx); _startMatch(); },
              child: const Text('Start Match'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _startMatch() async {
    setState(() => _loading = true);
    try {
      final date = DateFormat('dd MMM yyyy').format(DateTime.now());
      final time = DateFormat('HH:mm').format(DateTime.now());
      final newId = await ref.read(cricketNotifierProvider.notifier).createMatchWithCustomTeams(
        teamAName: _teamACtrl.text.trim(),
        teamBName: _teamBCtrl.text.trim(),
        venue: _venueCtrl.text.trim(),
        overs: int.tryParse(_oversCtrl.text) ?? 5,
        ballsPerOver: 6,
        teamAPlayers: _teamASelected,
        teamBPlayers: _teamBSelected,
        scheduledDate: date, scheduledTime: time,
        tossWinnerIsTeamA: _tossWinnerIsTeamA,
        tossDecision: _tossDecision,
      );
      if (mounted) context.replace('/scorer/$newId');
    } catch (e) {
      _toast('Error: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg), backgroundColor: kSurface));
  }

  Widget _sectionHeader(String label) => Text(label, style: const TextStyle(color: kTurfGreen, fontWeight: FontWeight.bold, fontSize: 13, letterSpacing: 0.5));
}

class _TossBtn extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _TossBtn({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
    onTap: onTap,
    child: Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: BoxDecoration(
        color: selected ? kTurfGreen : kDarkBg,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Center(child: Text(label, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12), maxLines: 1, overflow: TextOverflow.ellipsis)),
    ),
  );
}

class _PlayerSelector extends StatefulWidget {
  final List<PlayerModel> players;
  final List<PlayerModel> allPlayers;
  final List<PlayerModel> selected;
  final String teamName;
  final void Function(List<PlayerModel>) onChanged;

  const _PlayerSelector({required this.players, required this.allPlayers, required this.selected, required this.teamName, required this.onChanged});

  @override
  State<_PlayerSelector> createState() => _PlayerSelectorState();
}

class _PlayerSelectorState extends State<_PlayerSelector> {
  late final List<PlayerModel> _selected;

  @override
  void initState() {
    super.initState();
    _selected = List.from(widget.selected.isNotEmpty ? widget.selected : widget.players.take(11));
    WidgetsBinding.instance.addPostFrameCallback((_) => widget.onChanged(_selected));
  }

  @override
  Widget build(BuildContext context) {
    final pool = widget.players.isNotEmpty ? widget.players : widget.allPlayers.where((p) => p.clubName == widget.teamName).toList();
    return Container(
      decoration: BoxDecoration(color: kSurface, borderRadius: BorderRadius.circular(12), border: Border.all(color: kSlate700)),
      child: Column(
        children: pool.map((p) {
          final isSelected = _selected.any((s) => s.name == p.name);
          return ListTile(
            dense: true,
            leading: CircleAvatar(
              radius: 14, backgroundColor: isSelected ? kTurfGreen : kSlate700,
              child: Text(p.name[0], style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
            ),
            title: Text(p.name, style: TextStyle(color: isSelected ? Colors.white : kSlate400, fontSize: 13, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
            trailing: isSelected ? const Icon(Icons.check_circle, color: kTurfGreen, size: 18) : null,
            onTap: () {
              setState(() {
                if (isSelected) _selected.removeWhere((s) => s.name == p.name);
                else if (_selected.length < 11) _selected.add(p);
              });
              widget.onChanged(_selected);
            },
          );
        }).toList(),
      ),
    );
  }
}
