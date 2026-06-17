import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../screens/home_screen.dart';
import '../screens/match_setup_screen.dart';
import '../screens/scorer_console_screen.dart';
import '../screens/match_center_screen.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/',
        name: 'home',
        builder: (ctx, state) => const HomeScreen(),
      ),
      GoRoute(
        path: '/setup',
        name: 'setup',
        builder: (ctx, state) => const MatchSetupScreen(),
      ),
      GoRoute(
        path: '/scorer/:id',
        name: 'scorer',
        builder: (ctx, state) {
          final id = int.parse(state.pathParameters['id']!);
          return ScorerConsoleScreen(matchId: id);
        },
      ),
      GoRoute(
        path: '/center/:id',
        name: 'center',
        builder: (ctx, state) {
          final id = int.parse(state.pathParameters['id']!);
          return MatchCenterScreen(matchId: id);
        },
      ),
    ],
  );
});
