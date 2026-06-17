import 'package:flutter/material.dart';

// Brand Colors (matching Android app exactly)
const kTurfGreen   = Color(0xFF10B981); // Emerald Green
const kTurfLime    = Color(0xFF3B82F6); // Royal Blue accent
const kGoldAccent  = Color(0xFFF59E0B); // Amber Gold
const kDarkBg      = Color(0xFF0F172A); // Slate 900
const kSurface     = Color(0xFF1E293B); // Slate 800
const kPolishBg    = Color(0xFF0A0F1D); // Deep dark
const kSlate400    = Color(0xFF94A3B8);
const kSlate600    = Color(0xFF475569);
const kSlate700    = Color(0xFF334155);

ThemeData buildAppTheme() {
  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: kPolishBg,
    colorScheme: const ColorScheme.dark(
      primary: kTurfGreen,
      secondary: kTurfLime,
      surface: kSurface,
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onSurface: Colors.white,
    ),
    cardTheme: const CardThemeData(
      color: kSurface,
      elevation: 0,
      margin: EdgeInsets.zero,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: kTurfGreen,
      foregroundColor: Colors.white,
      elevation: 0,
      centerTitle: true,
      titleTextStyle: TextStyle(
        color: Colors.white,
        fontSize: 16,
        fontWeight: FontWeight.bold,
        letterSpacing: 0.5,
      ),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: kTurfGreen,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        padding: const EdgeInsets.symmetric(vertical: 14),
        textStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: kSlate400,
        side: const BorderSide(color: kSlate700),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: kSurface,
      labelStyle: const TextStyle(color: kSlate400),
      hintStyle: const TextStyle(color: kSlate600),
      enabledBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: kSlate700),
        borderRadius: BorderRadius.circular(10),
      ),
      focusedBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: kTurfGreen, width: 2),
        borderRadius: BorderRadius.circular(10),
      ),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
    ),
    dividerTheme: const DividerThemeData(color: kSlate700, thickness: 0.5),
    radioTheme: RadioThemeData(fillColor: WidgetStateProperty.all(kTurfGreen)),
    checkboxTheme: CheckboxThemeData(fillColor: WidgetStateProperty.all(kTurfGreen)),
    textTheme: const TextTheme(
      bodyLarge: TextStyle(color: Colors.white),
      bodyMedium: TextStyle(color: Colors.white),
      bodySmall: TextStyle(color: kSlate400),
      titleLarge: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
      titleMedium: TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
    ),
  );
}
