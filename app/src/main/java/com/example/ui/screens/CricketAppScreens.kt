package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.database.*
import com.example.viewmodel.CricketViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// Theme Colors (Polished Professional Slate & Blue)
val TurfGreen = Color(0xFF10B981)      // Emerald Green - Vibrant brand color
val TurfLime = Color(0xFF3B82F6)       // Royal Blue - Used for secondary headers/accents
val GoldAccent = Color(0xFFF59E0B)     // Amber Gold - Used for badges & highlights
val DarkBackground = Color(0xFF0F172A) // Slate 900 - Deepest slate for dark scoreboard and scorer background
val LightSurface = Color(0xFF1E293B)   // Rich Slate - Elegant container card color
val PolishBackground = Color(0xFF0A0F1D) // Professional Dark background

@Composable
fun getDarkTextFieldColors(focusedBorderColor: Color = TurfLime) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color(0xFFE2E8F0),
    focusedLabelColor = focusedBorderColor,
    unfocusedLabelColor = Color(0xFF64748B),
    focusedPlaceholderColor = Color(0xFF475569),
    unfocusedPlaceholderColor = Color(0xFF334155),
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = Color(0xFF334155),
    focusedContainerColor = Color(0xFF0F172A),
    unfocusedContainerColor = Color(0xFF0F172A)
)

// ----------------------------------------------------
// MAIN ROUTER / NAVIGATION CONTAINER
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketAppNavigation(viewModel: CricketViewModel) {
    var currentScreen by remember { mutableStateOf("dashboard") }
    var selectedMatchIdForCenter by remember { mutableStateOf<Int?>(null) }
    
    val context = LocalContext.current
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val activeMatchState by viewModel.activeMatchState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            if (currentScreen == "dashboard" || currentScreen == "teams" || currentScreen == "players") {
                NavigationBar(
                    containerColor = LightSurface,
                    contentColor = TurfGreen
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "dashboard",
                        onClick = { currentScreen = "dashboard" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TurfLime,
                            selectedTextColor = TurfGreen,
                            indicatorColor = Color(0xFFDBEAFE),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        icon = { Icon(Icons.Default.SportsCricket, contentDescription = "Matches") },
                        label = { Text("Matches", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "teams",
                        onClick = { currentScreen = "teams" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TurfLime,
                            selectedTextColor = TurfGreen,
                            indicatorColor = Color(0xFFDBEAFE),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        icon = { Icon(Icons.Default.Groups, contentDescription = "Teams") },
                        label = { Text("Teams", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen == "players",
                        onClick = { currentScreen = "players" },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TurfLime,
                            selectedTextColor = TurfGreen,
                            indicatorColor = Color(0xFFDBEAFE),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        ),
                        icon = { Icon(Icons.Default.Person, contentDescription = "Players") },
                        label = { Text("Players", fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    onCreateMatch = { currentScreen = "setup" },
                    onSelectMatch = { matchId ->
                        selectedMatchIdForCenter = matchId
                        currentScreen = "match_center"
                    },
                    onOpenScorer = { matchId ->
                        viewModel.setActiveMatch(matchId)
                        currentScreen = "scorer"
                    }
                )
                "teams" -> TeamsRosterScreen(viewModel)
                "players" -> PlayersRosterScreen(viewModel)
                "setup" -> MatchSetupScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "dashboard" },
                    onMatchStarted = { matchId ->
                        viewModel.setActiveMatch(matchId)
                        currentScreen = "scorer"
                    }
                )
                "scorer" -> {
                    activeMatchState?.let { state ->
                        if (state.match.status == "COMPLETED") {
                            // Automatically swap screen to read-only match center if completed
                            selectedMatchIdForCenter = state.match.id
                            currentScreen = "match_center"
                        } else {
                            ScorerConsoleScreen(
                                viewModel = viewModel,
                                state = state,
                                onBackMatches = {
                                    viewModel.setActiveMatch(null)
                                    currentScreen = "dashboard"
                                },
                                onOpenMatchCenter = {
                                    selectedMatchIdForCenter = state.match.id
                                    currentScreen = "match_center"
                                }
                            )
                        }
                    } ?: run {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TurfGreen)
                        }
                    }
                }
                "match_center" -> {
                    val matchId = selectedMatchIdForCenter ?: activeMatchState?.match?.id
                    if (matchId != null) {
                        // Dynamically update viewmodel activeMatchId so flow computed state represents it
                        LaunchedEffect(matchId) {
                            viewModel.setActiveMatch(matchId)
                        }
                        activeMatchState?.let { state ->
                            MatchCenterScreen(
                                viewModel = viewModel,
                                state = state,
                                onBack = {
                                    // if active matches and we have a scorer we came from, return to scorer
                                    if (activeMatchState?.match?.id == selectedMatchIdForCenter && activeMatchState?.match?.status == "LIVE") {
                                        currentScreen = "scorer"
                                    } else {
                                        currentScreen = "dashboard"
                                        viewModel.setActiveMatch(null)
                                    }
                                }
                            )
                        } ?: run {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = TurfGreen)
                            }
                        }
                    } else {
                        currentScreen = "dashboard"
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 1. DASHBOARD SCREEN
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CricketViewModel,
    onCreateMatch: () -> Unit,
    onSelectMatch: (Int) -> Unit,
    onOpenScorer: (Int) -> Unit
) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val isPrepopulating by viewModel.isDbPrepopulating.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var matchFilterTab by remember { mutableStateOf("ALL") } // ALL, LIVE, COMPLETED, PRE_MATCH

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        // Top Banner Design
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TurfGreen, Color(0xFF0F172A))
                    )
                )
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CREASE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldAccent,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "Cricket scoring & Spectator Match Center",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Connection Simulated Sync State Indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Online Sync", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search matches, venues, teams...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = getDarkTextFieldColors(TurfGreen),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("global_search_bar")
        )

        // Matches Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "LIVE", "COMPLETED", "PRE_MATCH").forEach { tab ->
                val selected = matchFilterTab == tab
                Button(
                    onClick = { matchFilterTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) TurfGreen else Color.White,
                        contentColor = if (selected) Color.White else TurfGreen
                    ),
                    border = BorderStroke(1.dp, TurfGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = tab.replace("_", " "), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isPrepopulating) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TurfGreen)
            }
        } else {
            // Filter Matches
            val filteredMatches = matches.filter {
                val matchesText = (it.teamAName + " " + it.teamBName + " " + it.venue).lowercase()
                matchesText.contains(searchQuery.lowercase()) && (
                    matchFilterTab == "ALL" || it.status == matchFilterTab
                )
            }

            if (filteredMatches.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = "No matches",
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Matches Found",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Text(
                            "Create a new cricket match to start live-scoring!",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredMatches) { match ->
                        MatchCardItem(
                            match = match,
                            onSelect = { onSelectMatch(match.id) },
                            onOpenScorer = { onOpenScorer(match.id) }
                        )
                    }
                }
            }
        }
    }

    // Floating Action Button to Create a Match
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onCreateMatch,
            containerColor = GoldAccent,
            contentColor = TurfGreen,
            modifier = Modifier.testTag("create_match_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Match")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Match", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MatchCardItem(
    match: MatchEntity,
    onSelect: () -> Unit,
    onOpenScorer: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_card_${match.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Status, Date, Venue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusTextAndColor = when (match.status) {
                        "LIVE" -> "LIVE" to Color.Red
                        "COMPLETED" -> "COMPLETED" to TurfGreen
                        "PRE_MATCH" -> "UPCOMING" to Color(0xFF94A3B8)
                        else -> "ABANDONED" to Color(0xFFCBD5E1)
                    }
                    Text(
                        text = statusTextAndColor.first,
                        color = statusTextAndColor.second,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${match.oversCount} Overs",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${match.scheduledDate} ${match.scheduledTime}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Teams and Scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team A logo and name
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        match.teamALogo.ifEmpty { match.teamAName.take(1).uppercase() },
                        fontSize = 24.sp,
                        modifier = Modifier
                            .size(36.dp)
                            .background(PolishBackground, shape = RoundedCornerShape(8.dp))
                            .wrapContentSize(Alignment.Center)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.teamAName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Team A Score display
                Text(
                    text = if (match.status != "PRE_MATCH") {
                        if (match.firstInningsTeamId == match.teamAId) {
                            "${match.firstInningsScore}/${match.firstInningsWickets}"
                        } else {
                            if (match.secondInningsTeamId == match.teamAId) "${match.secondInningsScore}/${match.secondInningsWickets}" else "Yet to bat"
                        }
                    } else "-",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = TurfGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team B logo and name
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        match.teamBLogo.ifEmpty { match.teamBName.take(1).uppercase() },
                        fontSize = 24.sp,
                        modifier = Modifier
                            .size(36.dp)
                            .background(PolishBackground, shape = RoundedCornerShape(8.dp))
                            .wrapContentSize(Alignment.Center)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = match.teamBName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Team B Score display
                Text(
                    text = if (match.status != "PRE_MATCH") {
                        if (match.firstInningsTeamId == match.teamBId) {
                            "${match.firstInningsScore}/${match.firstInningsWickets}"
                        } else {
                            if (match.secondInningsTeamId == match.teamBId) "${match.secondInningsScore}/${match.secondInningsWickets}" else "Yet to bat"
                        }
                    } else "-",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = TurfGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            // Footer info and action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "Venue",
                        tint = TurfLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = match.venue,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (match.status == "LIVE" || match.status == "PRE_MATCH") {
                    Button(
                        onClick = { onOpenScorer() },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("Score Match", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (match.status == "COMPLETED") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Cup",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = match.winningMargin.ifEmpty { "Match Finished" },
                            fontSize = 11.sp,
                            color = TurfGreen,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. MATCH SETUP SCREEN
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MatchSetupScreen(
    viewModel: CricketViewModel,
    onBack: () -> Unit,
    onMatchStarted: (Int) -> Unit
) {
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Team names decided by user (default values are standard teams from pre-population)
    var teamAName by remember { mutableStateOf("Chennai Chargers") }
    var teamBName by remember { mutableStateOf("Bangalore Blitz") }

    var venue by remember { mutableStateOf("Greenfield Oval, Mumbai") }
    var oversCount by remember { mutableStateOf("5") }
    val ballsPerOver by remember { mutableStateOf(6) }

    // Date/Time
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentDateText = dateFormat.format(Date())
    val currentTimeText = timeFormat.format(Date())

    // Local lists of players matching current team names
    val teamAPlayersLocal = remember { mutableStateListOf<PlayerEntity>() }
    val teamBPlayersLocal = remember { mutableStateListOf<PlayerEntity>() }

    // Selected playing XI lists of PlayerEntity
    val teamASelected = remember { mutableStateListOf<PlayerEntity>() }
    val teamBSelected = remember { mutableStateListOf<PlayerEntity>() }

    // Synchronize local roster whenever team names or db players change
    LaunchedEffect(teamAName, players) {
        val trimmed = teamAName.trim()
        if (trimmed.isNotEmpty()) {
            val matching = players.filter { it.clubName.equals(trimmed, ignoreCase = true) }
            teamAPlayersLocal.clear()
            teamAPlayersLocal.addAll(matching)

            // Auto select if currently empty or not matching the team
            val currentSelectedBelong = teamASelected.all { p ->
                matching.any { it.name.equals(p.name, ignoreCase = true) } || p.id <= 0
            }
            if (teamASelected.isEmpty() || !currentSelectedBelong) {
                teamASelected.clear()
                teamASelected.addAll(matching.take(11))
            }
        } else {
            teamAPlayersLocal.clear()
            teamASelected.clear()
        }
    }

    LaunchedEffect(teamBName, players) {
        val trimmed = teamBName.trim()
        if (trimmed.isNotEmpty()) {
            val matching = players.filter { it.clubName.equals(trimmed, ignoreCase = true) }
            teamBPlayersLocal.clear()
            teamBPlayersLocal.addAll(matching)

            // Auto select if currently empty or not matching the team
            val currentSelectedBelong = teamBSelected.all { p ->
                matching.any { it.name.equals(p.name, ignoreCase = true) } || p.id <= 0
            }
            if (teamBSelected.isEmpty() || !currentSelectedBelong) {
                teamBSelected.clear()
                teamBSelected.addAll(matching.take(11))
            }
        } else {
            teamBPlayersLocal.clear()
            teamBSelected.clear()
        }
    }

    // Inline custom player adding states
    var newPlayerNameA by remember { mutableStateOf("") }
    var newPlayerRoleA by remember { mutableStateOf("BATS_MAN") }

    var newPlayerNameB by remember { mutableStateOf("") }
    var newPlayerRoleB by remember { mutableStateOf("BATS_MAN") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        TopAppBar(
            title = { Text("Match Setup & Team Roster", fontWeight = FontWeight.Bold, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TurfGreen)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro message card
            Card(
                colors = CardDefaults.cardColors(containerColor = TurfLime.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, TurfLime.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.SportsCricket, contentDescription = "Info", tint = TurfLime, modifier = Modifier.size(28.dp))
                    Column {
                        Text("Establish Custom Match", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TurfLime)
                        Text("Type any custom team name you decide! You can quick-generate, select, or configure custom players on-the-fly for both playing Elevens.", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            // TEAM A SECTION
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BATTING TEAM A",
                        fontWeight = FontWeight.Bold,
                        color = TurfLime,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Team A Name Field
                    OutlinedTextField(
                        value = teamAName,
                        onValueChange = { teamAName = it },
                        label = { Text("Team A Name (Decided by User)") },
                        modifier = Modifier.fillMaxWidth().testTag("team_a_name_input"),
                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = "Team A", tint = TurfGreen) },
                        colors = getDarkTextFieldColors(TurfLime),
                        singleLine = true
                    )

                    // Suggested existing teams row for A
                    if (teams.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Predefined:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            teams.forEach { team ->
                                if (team.name != teamBName) {
                                    val isSelected = team.name.equals(teamAName.trim(), ignoreCase = true)
                                    val chipBg = if (isSelected) TurfLime.copy(alpha = 0.25f) else DarkBackground
                                    val chipBorder = if (isSelected) TurfLime else Color(0xFF334155)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                            .clickable { teamAName = team.name }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("${team.logoUrl} ${team.name}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Team A Players Section
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playing XI Squad (${teamASelected.size} Selected)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (teamASelected.size == 11) TurfLime else Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (teamASelected.size == 11) Color(0xFF064E3B) else Color(0xFF7F1D1D))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (teamASelected.size == 11) "Ideal XI" else "${teamASelected.size}/11",
                                fontSize = 11.sp,
                                color = if (teamASelected.size == 11) Color(0xFF34D399) else Color(0xFFF87171),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips selection
                    if (teamAPlayersLocal.isNotEmpty() || teamASelected.isNotEmpty()) {
                        val displayList = (teamAPlayersLocal + teamASelected).distinctBy { it.name.lowercase() }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayList.forEach { player ->
                                val isSelected = teamASelected.any { it.name.equals(player.name, ignoreCase = true) }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val existing = teamASelected.find { it.name.equals(player.name, ignoreCase = true) }
                                        if (existing != null) {
                                            teamASelected.remove(existing)
                                        } else {
                                            teamASelected.add(player)
                                        }
                                    },
                                    label = { Text(player.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFDBEAFE),
                                        selectedLabelColor = TurfLime
                                    )
                                )
                            }
                        }
                    } else {
                        Text("No players found for this team. Click below to instantly auto-generate or quick add!", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons/Interactive items to add or generate players
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val club = teamAName.trim().ifEmpty { "Team A" }
                                val existingNames = teamAPlayersLocal.map { it.name.lowercase() }
                                val realisticNames = listOf(
                                    "A. Sharma", "S. Tendulkar", "R. Dravid", "V. Sehwag", "M. Dhoni",
                                    "Y. Pathan", "B. Kumar", "Z. Khan", "J. Bumrah", "R. Jadeja", "V. Kohli"
                                )
                                var addedCount = 0
                                realisticNames.forEachIndexed { idx, name ->
                                    if (!existingNames.contains(name.lowercase()) && teamAPlayersLocal.size < 11) {
                                        val pRole = when (idx) {
                                            in 0..4 -> "BATS_MAN"
                                            5 -> "WICKET_KEEPER"
                                            in 6..7 -> "ALL_ROUNDER"
                                            else -> "BOWLER"
                                        }
                                        val newP = PlayerEntity(name = name, clubName = club, designRole = pRole)
                                        teamAPlayersLocal.add(newP)
                                        teamASelected.add(newP)
                                        addedCount++
                                    }
                                }
                                if (addedCount == 0 && teamAPlayersLocal.size < 11) {
                                    // Generate fallback standard players
                                    val needed = 11 - teamAPlayersLocal.size
                                    for (i in 1..needed) {
                                        val roleIdx = teamAPlayersLocal.size + 1
                                        val pRole = when (roleIdx) {
                                            in 1..5 -> "BATS_MAN"
                                            6 -> "WICKET_KEEPER"
                                            in 7..8 -> "ALL_ROUNDER"
                                            else -> "BOWLER"
                                        }
                                        val newP = PlayerEntity(name = "Player $roleIdx", clubName = club, designRole = pRole)
                                        teamAPlayersLocal.add(newP)
                                        teamASelected.add(newP)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = TurfLime),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("generate_players_a_btn")
                        ) {
                            Text("Auto-Fill XI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                teamAPlayersLocal.clear()
                                teamASelected.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear List", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom inline quick player registration
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PolishBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Quick Add Custom Player", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TurfGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlayerNameA,
                                    onValueChange = { newPlayerNameA = it },
                                    placeholder = { Text("Player name...", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    colors = getDarkTextFieldColors(TurfLime),
                                    singleLine = true
                                )

                                // Simple role picker
                                var expandedRoleA by remember { mutableStateOf(false) }
                                Box {
                                    Button(
                                        onClick = { expandedRoleA = true },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = TurfGreen),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                                    ) {
                                        Text(
                                            text = when (newPlayerRoleA) {
                                                "BATS_MAN" -> "BAT"
                                                "BOWLER" -> "BOWL"
                                                "ALL_ROUNDER" -> "ALL"
                                                else -> "WK"
                                            },
                                            fontSize = 10.sp
                                        )
                                    }
                                    DropdownMenu(expanded = expandedRoleA, onDismissRequest = { expandedRoleA = false }) {
                                        DropdownMenuItem(text = { Text("Batter") }, onClick = { newPlayerRoleA = "BATS_MAN"; expandedRoleA = false })
                                        DropdownMenuItem(text = { Text("Bowler") }, onClick = { newPlayerRoleA = "BOWLER"; expandedRoleA = false })
                                        DropdownMenuItem(text = { Text("All-Rounder") }, onClick = { newPlayerRoleA = "ALL_ROUNDER"; expandedRoleA = false })
                                        DropdownMenuItem(text = { Text("Wicket-Keeper") }, onClick = { newPlayerRoleA = "WICKET_KEEPER"; expandedRoleA = false })
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (newPlayerNameA.trim().isNotEmpty()) {
                                            val club = teamAName.trim().ifEmpty { "Team A" }
                                            val newP = PlayerEntity(
                                                name = newPlayerNameA.trim(),
                                                clubName = club,
                                                designRole = newPlayerRoleA
                                            )
                                            teamAPlayersLocal.add(newP)
                                            teamASelected.add(newP)
                                            newPlayerNameA = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(TurfLime, RoundedCornerShape(6.dp))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // TEAM B SECTION
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BOWLING TEAM B",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Team B Name Field
                    OutlinedTextField(
                        value = teamBName,
                        onValueChange = { teamBName = it },
                        label = { Text("Team B Name (Decided by User)") },
                        modifier = Modifier.fillMaxWidth().testTag("team_b_name_input"),
                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = "Team B", tint = TurfGreen) },
                        colors = getDarkTextFieldColors(Color(0xFFEF4444)),
                        singleLine = true
                    )

                    // Suggested existing teams row for B
                    if (teams.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Predefined:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            teams.forEach { team ->
                                if (team.name != teamAName) {
                                    val isSelected = team.name.equals(teamBName.trim(), ignoreCase = true)
                                    val chipBg = if (isSelected) TurfGreen.copy(alpha = 0.25f) else DarkBackground
                                    val chipBorder = if (isSelected) TurfGreen else Color(0xFF334155)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                            .clickable { teamBName = team.name }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("${team.logoUrl} ${team.name}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.White else Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Team B Players Section
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Playing XI Squad (${teamBSelected.size} Selected)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (teamBSelected.size == 11) TurfGreen else Color.White
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (teamBSelected.size == 11) Color(0xFF064E3B) else Color(0xFF7F1D1D))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (teamBSelected.size == 11) "Ideal XI" else "${teamBSelected.size}/11",
                                fontSize = 11.sp,
                                color = if (teamBSelected.size == 11) Color(0xFF34D399) else Color(0xFFF87171),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chips selection
                    if (teamBPlayersLocal.isNotEmpty() || teamBSelected.isNotEmpty()) {
                        val displayList = (teamBPlayersLocal + teamBSelected).distinctBy { it.name.lowercase() }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            displayList.forEach { player ->
                                val isSelected = teamBSelected.any { it.name.equals(player.name, ignoreCase = true) }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val existing = teamBSelected.find { it.name.equals(player.name, ignoreCase = true) }
                                        if (existing != null) {
                                            teamBSelected.remove(existing)
                                        } else {
                                            teamBSelected.add(player)
                                        }
                                    },
                                    label = { Text(player.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFEE2E2),
                                        selectedLabelColor = Color(0xFFEF4444)
                                    )
                                )
                            }
                        }
                    } else {
                        Text("No players found for this team. Click below to instantly auto-generate or quick add!", fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons/Interactive items to add or generate players
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val club = teamBName.trim().ifEmpty { "Team B" }
                                val existingNames = teamBPlayersLocal.map { it.name.lowercase() }
                                val realisticNames = listOf(
                                    "K. Rahul", "R. Sharma", "S. Dhawan", "V. Iyer", "R. Pant",
                                    "H. Pandya", "S. Thakur", "A. Patel", "R. Ashwin", "Y. Chahal", "B. Kumar"
                                )
                                var addedCount = 0
                                realisticNames.forEachIndexed { idx, name ->
                                    if (!existingNames.contains(name.lowercase()) && teamBPlayersLocal.size < 11) {
                                        val pRole = when (idx) {
                                            in 0..4 -> "BATS_MAN"
                                            5 -> "WICKET_KEEPER"
                                            in 6..7 -> "ALL_ROUNDER"
                                            else -> "BOWLER"
                                        }
                                        val newP = PlayerEntity(name = name, clubName = club, designRole = pRole)
                                        teamBPlayersLocal.add(newP)
                                        teamBSelected.add(newP)
                                        addedCount++
                                    }
                                }
                                if (addedCount == 0 && teamBPlayersLocal.size < 11) {
                                    val needed = 11 - teamBPlayersLocal.size
                                    for (i in 1..needed) {
                                        val roleIdx = teamBPlayersLocal.size + 1
                                        val pRole = when (roleIdx) {
                                            in 1..5 -> "BATS_MAN"
                                            6 -> "WICKET_KEEPER"
                                            in 7..8 -> "ALL_ROUNDER"
                                            else -> "BOWLER"
                                        }
                                        val newP = PlayerEntity(name = "Player $roleIdx", clubName = club, designRole = pRole)
                                        teamBPlayersLocal.add(newP)
                                        teamBSelected.add(newP)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("generate_players_b_btn")
                        ) {
                            Text("Auto-Fill XI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                teamBPlayersLocal.clear()
                                teamBSelected.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear List", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom inline quick player registration for B
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PolishBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Quick Add Custom Player", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TurfGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlayerNameB,
                                    onValueChange = { newPlayerNameB = it },
                                    placeholder = { Text("Player name...", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    colors = getDarkTextFieldColors(Color(0xFFEF4444)),
                                    singleLine = true
                                )

                                var expandedRoleB by remember { mutableStateOf(false) }
                                Box {
                                    Button(
                                        onClick = { expandedRoleB = true },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = TurfGreen),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                                    ) {
                                        Text(
                                            text = when (newPlayerRoleB) {
                                                "BATS_MAN" -> "BAT"
                                                "BOWLER" -> "BOWL"
                                                "ALL_ROUNDER" -> "ALL"
                                                else -> "WK"
                                            },
                                            fontSize = 10.sp
                                        )
                                    }
                                    DropdownMenu(expanded = expandedRoleB, onDismissRequest = { expandedRoleB = false }) {
                                        DropdownMenuItem(text = { Text("Batter") }, onClick = { newPlayerRoleB = "BATS_MAN"; expandedRoleB = false })
                                        DropdownMenuItem(text = { Text("Bowler") }, onClick = { newPlayerRoleB = "BOWLER"; expandedRoleB = false })
                                        DropdownMenuItem(text = { Text("All-Rounder") }, onClick = { newPlayerRoleB = "ALL_ROUNDER"; expandedRoleB = false })
                                        DropdownMenuItem(text = { Text("Wicket-Keeper") }, onClick = { newPlayerRoleB = "WICKET_KEEPER"; expandedRoleB = false })
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (newPlayerNameB.trim().isNotEmpty()) {
                                            val club = teamBName.trim().ifEmpty { "Team B" }
                                            val newP = PlayerEntity(
                                                name = newPlayerNameB.trim(),
                                                clubName = club,
                                                designRole = newPlayerRoleB
                                            )
                                            teamBPlayersLocal.add(newP)
                                            teamBSelected.add(newP)
                                            newPlayerNameB = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFEF4444), RoundedCornerShape(6.dp))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // VENUE & FORMAT SETTINGS
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MATCH ENVIRONMENT", fontWeight = FontWeight.Bold, color = TurfGreen, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Venue Location") },
                        modifier = Modifier.fillMaxWidth().testTag("venue_input"),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = "Venue", tint = TurfLime) },
                        colors = getDarkTextFieldColors(TurfLime)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Match Format (Overs)", fontWeight = FontWeight.Bold, color = TurfGreen, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("5", "10", "20", "50").forEach { ov ->
                            val selected = oversCount == ov
                            Button(
                                onClick = { oversCount = ov },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) TurfLime else Color(0xFFF1F5F9),
                                    contentColor = if (selected) Color.White else TurfGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(ov, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Proceed Button
            Button(
                onClick = {
                    val trimmedA = teamAName.trim()
                    val trimmedB = teamBName.trim()
                    if (trimmedA.isEmpty() || trimmedB.isEmpty()) {
                        Toast.makeText(context, "Please decide both team names", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (trimmedA.equals(trimmedB, ignoreCase = true)) {
                        Toast.makeText(context, "Teams must have distinct names", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (teamASelected.isEmpty() || teamBSelected.isEmpty()) {
                        Toast.makeText(context, "Please configure/select players for both playing Elevens", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Proceed via smart custom teams match creation in VM
                    viewModel.createMatchWithCustomTeams(
                        teamAName = trimmedA,
                        teamBName = trimmedB,
                        venue = venue,
                        overs = oversCount.toIntOrNull() ?: 5,
                        ballsPerOver = ballsPerOver,
                        teamAPlayers = teamASelected.toList(),
                        teamBPlayers = teamBSelected.toList(),
                        scheduledDate = currentDateText,
                        scheduledTime = currentTimeText,
                        onComplete = { newMatchId ->
                            onMatchStarted(newMatchId)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_match_builder_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TurfGreen)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Proceed to Toss & Play", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. SCORER SCREEN (The heart of the app)
// ----------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScorerConsoleScreen(
    viewModel: CricketViewModel,
    state: ComputedMatchState,
    onBackMatches: () -> Unit,
    onOpenMatchCenter: () -> Unit
) {
    val context = LocalContext.current
    val match = state.match
    val activeInnings = state.activeInnings

    // Check if Toss needs resolver
    var showTossDialog by remember { mutableStateOf(match.tossWinnerId == 0) }
    var selectedTossWinnerId by remember { mutableStateOf(match.teamAId) }
    var selectedTossDecision by remember { mutableStateOf("BAT") } // BAT, BOWL

    // Selected Batter / Striker / Bowler states
    var strikerId by remember { mutableStateOf(activeInnings.activeStrikerId) }
    var nonStrikerId by remember { mutableStateOf(activeInnings.activeNonStrikerId) }
    var bowlerId by remember { mutableStateOf(activeInnings.activeBowlerId) }

    // Synchronize selector state labels
    LaunchedEffect(activeInnings.activeStrikerId) {
        strikerId = activeInnings.activeStrikerId
    }
    LaunchedEffect(activeInnings.activeNonStrikerId) {
        nonStrikerId = activeInnings.activeNonStrikerId
    }
    LaunchedEffect(activeInnings.activeBowlerId) {
        bowlerId = activeInnings.activeBowlerId
    }

    // Active dropdowns
    var expandedStrikerDropdown by remember { mutableStateOf(false) }
    var expandedNonStrikerDropdown by remember { mutableStateOf(false) }
    var expandedBowlerDropdown by remember { mutableStateOf(false) }

    // Wicket Dialog states
    var showWicketDialog by remember { mutableStateOf(false) }
    var wicketDismissalType by remember { mutableStateOf("BOWLED") } // BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, RETIRED_OUT
    var wicketDismissedBatterId by remember { mutableStateOf(0) }
    var wicketFielderName by remember { mutableStateOf("") }

    // Match Completed dialog Suggest Player Of Match
    var showMatchCompletionDialog by remember { mutableStateOf(false) }
    var selectedPotmId by remember { mutableStateOf(0) }

    // End of Over bowel dialog loader
    val showBowlerChangeDialog = activeInnings.ballsBowled > 0 && 
            (activeInnings.ballsBowled % match.ballsPerOver == 0) && 
            (activeInnings.currentOverBalls.isEmpty() || activeInnings.currentOverBalls.last().ballIndexInOver == match.ballsPerOver)

    // Populate active striker if empty
    if (strikerId == 0 && activeInnings.batters.isNotEmpty()) {
        val nonDismissed = activeInnings.batters.filter { !it.isDismissed }
        if (nonDismissed.isNotEmpty()) strikerId = nonDismissed[0].playerId
        if (nonDismissed.size > 1) nonStrikerId = nonDismissed[1].playerId
    }

    // Fetch players profiles list maps
    val allPlayers by viewModel.players.collectAsStateWithLifecycle()
    val playersMap = allPlayers.associateBy { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Scorer Header Row Controls (Save, Undo, Redo, Match Center)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TurfGreen)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackMatches) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Backup", tint = Color.White)
            }

            Text(
                "SCORER CONSOLE",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Undo
                IconButton(
                    onClick = { viewModel.undoLastBall() },
                    modifier = Modifier.testTag("undo_btn")
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White)
                }

                // Redo
                IconButton(
                    onClick = { viewModel.redoLastBall() },
                    modifier = Modifier.testTag("redo_btn")
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = Color.White)
                }

                Button(
                    onClick = onOpenMatchCenter,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Live View", color = TurfGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Live score giant scoreboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TurfGreen, DarkBackground)
                    )
                )
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = activeInnings.battingTeamName.uppercase(),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${activeInnings.score}/${activeInnings.wickets}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${activeInnings.oversString} Ov)",
                        fontSize = 18.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Run rate calculation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "CRR: ${String.format("%.2f", activeInnings.runRate)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (state.currentInningsNo == 2) {
                        Text(
                            text = "REQ: ${String.format("%.2f", state.requiredRunRate)}",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (state.currentInningsNo == 2) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NEED ${state.requiredRuns} RUNS IN ${state.remainingBalls} BALLS",
                        color = Color.Yellow,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Squad Batter Indicators Selector Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("BATTERS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // Batter 1 (Striker) Selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.0f).clickable { expandedStrikerDropdown = true }) {
                        Text(
                            text = (playersMap[strikerId]?.name ?: "Click to Striker") + " *",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        val bScore = activeInnings.batters.find { it.playerId == strikerId }
                        Text(
                            text = if (bScore != null) "${bScore.runs} runs off ${bScore.balls} (4s:${bScore.fours}, 6s:${bScore.sixes})" else "0 (0)",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        DropdownMenu(expanded = expandedStrikerDropdown, onDismissRequest = { expandedStrikerDropdown = false }) {
                            activeInnings.batters.filter { !it.isDismissed && it.playerId != nonStrikerId }.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        strikerId = b.playerId
                                        expandedStrikerDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Batter 2 (Non Striker) row details
                    Column(
                        modifier = Modifier.weight(1.0f).clickable { expandedNonStrikerDropdown = true },
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = playersMap[nonStrikerId]?.name ?: "Choose Non-Striker",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        val b2Score = activeInnings.batters.find { it.playerId == nonStrikerId }
                        Text(
                            text = if (b2Score != null) "${b2Score.runs} runs off ${b2Score.balls}" else "0 (0)",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        DropdownMenu(expanded = expandedNonStrikerDropdown, onDismissRequest = { expandedNonStrikerDropdown = false }) {
                            activeInnings.batters.filter { !it.isDismissed && it.playerId != strikerId }.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        nonStrikerId = b.playerId
                                        expandedNonStrikerDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Bowler selection Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable { expandedBowlerDropdown = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACTIVE BOWLER", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playersMap[bowlerId]?.name ?: "Tap to select Bowler",
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                val bowScore = activeInnings.bowlers.find { it.playerId == bowlerId }
                Text(
                    text = if (bowScore != null) {
                        "${bowScore.wickets}-${bowScore.runsConceded} (${bowScore.oversString} Ov)"
                    } else "0.0 Ov",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )

                DropdownMenu(expanded = expandedBowlerDropdown, onDismissRequest = { expandedBowlerDropdown = false }) {
                    activeInnings.bowlers.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b.name) },
                            onClick = {
                                bowlerId = b.playerId
                                expandedBowlerDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Current Over Deliveries Row Log
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("This Over: ", color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                activeInnings.currentOverBalls.forEach { b ->
                    val txt = when {
                        b.isWicket -> "W"
                        b.extraType == "WIDE" -> "${b.runsSimple + b.extraRuns}Wd"
                        b.extraType == "NO_BALL" -> "${b.runsSimple + b.extraRuns}Nb"
                        b.extraType == "BYE" -> "${b.runsSimple + b.extraRuns}B"
                        b.extraType == "LEG_BYE" -> "${b.runsSimple + b.extraRuns}Lb"
                        else -> b.runsSimple.toString()
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (txt == "W") Color.Red else if (b.runsSimple >= 4) TurfGreen else Color.DarkGray)
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Text(txt, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main Scoring Controls Area (Runs 0-6, Extras options, Wickets trigger)
        Card(
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Batting score values grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(0, 1, 2, 3, 4, 6).forEach { scoreVal ->
                        val textAndColor = when (scoreVal) {
                            4 -> "4 Boundary" to TurfGreen
                            6 -> "6 Boundary" to TurfLime
                            else -> scoreVal.toString() to TurfGreen
                        }
                        Button(
                            onClick = {
                                if (strikerId == 0 || bowlerId == 0) {
                                    Toast.makeText(context, "Set striker and bowler first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.recordDelivery(
                                    runsSimple = scoreVal,
                                    extraType = "NONE",
                                    extraRuns = 0,
                                    isWicket = false,
                                    strikerId = strikerId,
                                    nonStrikerId = nonStrikerId,
                                    bowlerId = bowlerId
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("score_btn_$scoreVal"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scoreVal >= 4) textAndColor.second else LightSurface,
                                contentColor = if (scoreVal >= 4) Color.White else TurfGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (scoreVal >= 4) "${scoreVal}\nBound" else scoreVal.toString(),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Extras Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wide Ball
                    Button(
                        onClick = {
                            if (strikerId == 0 || bowlerId == 0) return@Button
                            viewModel.recordDelivery(
                                runsSimple = 0,
                                extraType = "WIDE",
                                extraRuns = 1,
                                isWicket = false,
                                strikerId = strikerId,
                                nonStrikerId = nonStrikerId,
                                bowlerId = bowlerId
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF1E40AF)),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("wide_btn")
                    ) {
                        Text("+1 Wide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // No ball
                    Button(
                        onClick = {
                            if (strikerId == 0 || bowlerId == 0) return@Button
                            viewModel.recordDelivery(
                                runsSimple = 0,
                                extraType = "NO_BALL",
                                extraRuns = 1,
                                isWicket = false,
                                strikerId = strikerId,
                                nonStrikerId = nonStrikerId,
                                bowlerId = bowlerId
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF1E40AF)),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("noball_btn")
                    ) {
                        Text("+1 NB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Leg Bye
                    Button(
                        onClick = {
                            if (strikerId == 0 || bowlerId == 0) return@Button
                            viewModel.recordDelivery(
                                runsSimple = 0,
                                extraType = "LEG_BYE",
                                extraRuns = 1,
                                isWicket = false,
                                strikerId = strikerId,
                                nonStrikerId = nonStrikerId,
                                bowlerId = bowlerId
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF1E40AF)),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("legbye_btn")
                    ) {
                        Text("1 Leg Bye", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // WICKET BIG DANGER RED BUTTON
                Button(
                    onClick = {
                        if (strikerId == 0 || bowlerId == 0) return@Button
                        wicketDismissedBatterId = strikerId
                        showWicketDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("out_btn")
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Out", tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OUT! Dismiss Batsman", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFDC2626))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Safe End Innings / Match Completed Control Workflow
                if (activeInnings.isCompleted) {
                    if (state.currentInningsNo == 1) {
                        Button(
                            onClick = { viewModel.proceedToSecondInnings() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = TurfGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("PROCEED TO 2ND INNINGS", fontWeight = FontWeight.Black)
                        }
                    } else {
                        Button(
                            onClick = {
                                // Default suggested POM to striker/highest runs
                                val topScorer = state.innings2?.batters?.maxByOrNull { it.runs }?.playerId ?: 0
                                selectedPotmId = topScorer
                                showMatchCompletionDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfGreen, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("FINALIZE MATCH RESULT", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // DIALOGS & OVERLAYS
    // ----------------------------------------------------

    // 1. Toss Selection Dialog
    if (showTossDialog) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Toss Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TurfGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Who won the toss?", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { selectedTossWinnerId = match.teamAId },
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedTossWinnerId == match.teamAId) TurfGreen else DarkBackground, contentColor = if (selectedTossWinnerId == match.teamAId) Color.White else Color.White),
                            modifier = Modifier.weight(1f)
                        ) { Text(match.teamAName) }

                        Button(
                            onClick = { selectedTossWinnerId = match.teamBId },
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedTossWinnerId == match.teamBId) TurfGreen else DarkBackground, contentColor = if (selectedTossWinnerId == match.teamBId) Color.White else Color.White),
                            modifier = Modifier.weight(1f)
                        ) { Text(match.teamBName) }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Winner chosen decision:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { selectedTossDecision = "BAT" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedTossDecision == "BAT") TurfGreen else DarkBackground, contentColor = if (selectedTossDecision == "BAT") Color.White else Color.White),
                            modifier = Modifier.weight(1f)
                        ) { Text("BAT FIRST") }

                        Button(
                            onClick = { selectedTossDecision = "BOWL" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedTossDecision == "BOWL") TurfGreen else DarkBackground, contentColor = if (selectedTossDecision == "BOWL") Color.White else Color.White),
                            modifier = Modifier.weight(1f)
                        ) { Text("BOWL FIRST") }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.submitTossResult(match.id, selectedTossWinnerId, selectedTossDecision)
                            showTossDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Match Live", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Wicket Fall Dismissal input dialog
    if (showWicketDialog) {
        Dialog(onDismissRequest = { showWicketDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp).testTag("wicket_dialog")
            ) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("Identify Wicket details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Red)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Dismissal Type", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    listOf("BOWLED", "CAUGHT", "LBW", "RUN_OUT", "STUMPED", "HIT_WICKET", "RETIRED_OUT").forEach { type ->
                        val isSel = wicketDismissalType == type
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wicketDismissalType = type }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = isSel, onClick = { wicketDismissalType = type })
                            Text(type, color = Color.DarkGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (wicketDismissalType == "RUN_OUT") {
                        Text("Dismissed Batter", fontSize = 12.sp, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { wicketDismissedBatterId = strikerId },
                                colors = ButtonDefaults.buttonColors(containerColor = if (wicketDismissedBatterId == strikerId) Color.Red else LightSurface, contentColor = if (wicketDismissedBatterId == strikerId) Color.White else Color.Red),
                                modifier = Modifier.weight(1f)
                            ) { Text(playersMap[strikerId]?.name ?: "Striker") }

                            Button(
                                onClick = { wicketDismissedBatterId = nonStrikerId },
                                colors = ButtonDefaults.buttonColors(containerColor = if (wicketDismissedBatterId == nonStrikerId) Color.Red else LightSurface, contentColor = if (wicketDismissedBatterId == nonStrikerId) Color.White else Color.Red),
                                modifier = Modifier.weight(1f)
                            ) { Text(playersMap[nonStrikerId]?.name ?: "Non-Striker") }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (wicketDismissalType == "CAUGHT" || wicketDismissalType == "RUN_OUT" || wicketDismissalType == "STUMPED") {
                        OutlinedTextField(
                            value = wicketFielderName,
                            onValueChange = { wicketFielderName = it },
                            label = { Text("Fielder Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = getDarkTextFieldColors(Color.Red)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.recordDelivery(
                                runsSimple = 0,
                                extraType = "NONE",
                                extraRuns = 0,
                                isWicket = true,
                                dismissalType = wicketDismissalType,
                                dismissedBatsmanId = if (wicketDismissalType == "RUN_OUT") wicketDismissedBatterId else strikerId,
                                fielderName = wicketFielderName,
                                strikerId = strikerId,
                                nonStrikerId = nonStrikerId,
                                bowlerId = bowlerId
                            )
                            showWicketDialog = false
                            wicketFielderName = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_wicket_btn")
                    ) {
                        Text("Confirm Wicket", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // 3. End of Over bowler selection popup
    if (showBowlerChangeDialog) {
        var localBowlerId by remember { mutableStateOf(0) }
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Over Completed!", fontWeight = FontWeight.Black, fontSize = 16.sp, color = TurfGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Please choose bowler for the next over:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(12.dp))

                    // List of eligible bowlers (exclude previous bowler)
                    val bowlersAvailable = activeInnings.bowlers.filter { it.playerId != activeInnings.activeBowlerId }
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(bowlersAvailable) { bow ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { localBowlerId = bow.playerId }
                                    .background(if (localBowlerId == bow.playerId) TurfLime.copy(alpha = 0.25f) else Color.Transparent)
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(bow.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${bow.wickets}-${bow.runsConceded}", color = Color(0xFF94A3B8))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (localBowlerId != 0) {
                                bowlerId = localBowlerId
                            }
                        },
                        enabled = localBowlerId != 0,
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Next Over")
                    }
                }
            }
        }
    }

    // 4. Finalize Match Outcomes Dialog
    if (showMatchCompletionDialog) {
         var marginSummaryText by remember { mutableStateOf(state.resultSummary) }
         val winningTeamId = if (state.innings2 != null) {
             val i1 = state.innings1
             val i2 = state.innings2
             if (i2.score >= i1.score + 1) match.secondInningsTeamId else match.firstInningsTeamId
         } else match.firstInningsTeamId

         Dialog(onDismissRequest = { showMatchCompletionDialog = false }) {
             Card(
                 colors = CardDefaults.cardColors(containerColor = LightSurface),
                 shape = RoundedCornerShape(16.dp),
                 modifier = Modifier.padding(16.dp)
             ) {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("Finalize Cricket Match?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TurfGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Winner: " + (if (winningTeamId == match.teamAId) match.teamAName else match.teamBName), fontWeight = FontWeight.Bold, color = TurfLime)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = marginSummaryText,
                        onValueChange = { marginSummaryText = it },
                        label = { Text("Winning Margin Summary Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDarkTextFieldColors(TurfGreen)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Player Of The Match Award", fontSize = 12.sp, color = Color.Gray)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var potmExpand by remember { mutableStateOf(false) }
                        Button(
                            onClick = { potmExpand = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LightSurface, contentColor = TurfGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(playersMap[selectedPotmId]?.name ?: "Select Player")
                        }
                        DropdownMenu(expanded = potmExpand, onDismissRequest = { potmExpand = false }) {
                            (state.innings1.batters + (state.innings2?.batters ?: emptyList())).forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        selectedPotmId = b.playerId
                                        potmExpand = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.completeMatch(winningTeamId, marginSummaryText, selectedPotmId)
                            showMatchCompletionDialog = false
                            onOpenMatchCenter()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Lock Scorecard", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. LIVE & COMPLETED MATCH CENTER
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCenterScreen(
    viewModel: CricketViewModel,
    state: ComputedMatchState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("LIVE") } // LIVE, SCORECARD, COMMENTARY, SQUADS, DETAIL
    val match = state.match

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        // Broadcaster Custom scoreboard card
        TopAppBar(
            title = { Text("${match.teamAName} vs ${match.teamBName}", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // PDF generator
                IconButton(
                    onClick = {
                        viewModel.generatePdfReport(match.id) { file ->
                            if (file != null) {
                                Toast.makeText(context, "PDF Report generated successfully", Toast.LENGTH_LONG).show()
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.fromFile(file), "application/pdf")
                                        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Saved at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Error generating report", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = "PDF")
                }

                // Share match QR code or JSON
                var showShareDialog by remember { mutableStateOf(false) }
                IconButton(onClick = { showShareDialog = true }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }

                if (showShareDialog) {
                    Dialog(onDismissRequest = { showShareDialog = false }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Share Cricket Match", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TurfGreen)
                                Spacer(modifier = Modifier.height(16.dp))

                                // QR Code Drawing Canvas!
                                Canvas(modifier = Modifier.size(140.dp)) {
                                    val sizeX = size.width
                                    val sizeY = size.height
                                    val points = 8
                                    drawRect(Color.White)
                                    // Beautiful simulation of QR barcode lines and squares!
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(10f, 10f), size = androidx.compose.ui.geometry.Size(40f, 40f))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(20f, 20f), size = androidx.compose.ui.geometry.Size(20f, 20f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(90f, 10f), size = androidx.compose.ui.geometry.Size(40f, 40f))
                                    drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(100f, 100f), size = androidx.compose.ui.geometry.Size(20f, 20f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(10f, 90f), size = androidx.compose.ui.geometry.Size(40f, 40f))
                                    
                                    // Simulated bits
                                    for (i in 0..50) {
                                        val x = (Math.random() * sizeX).toFloat()
                                        val y = (Math.random() * sizeY).toFloat()
                                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(x, y), size = androidx.compose.ui.geometry.Size(12f, 12f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Match QR Ticket", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val rawText = viewModel.exportMatchToJson(match.id)
                                        // Simple text sharing
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Follow Live Cricket Match! ${match.teamAName} vs ${match.teamBName} at ${match.venue}. Status: ${match.status}. Data: $rawText")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share match link"))
                                        showShareDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Copy Public Rink & Share")
                                }
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TurfGreen, titleContentColor = Color.White, navigationIconContentColor = Color.White)
        )

        // Tab Selector Row
        TabRow(
            selectedTabIndex = listOf("LIVE", "SCORECARD", "COMMENTARY", "SQUADS").indexOf(activeTab),
            containerColor = TurfGreen,
            contentColor = Color.White
        ) {
            listOf("LIVE", "SCORECARD", "COMMENTARY", "SQUADS").forEach { t ->
                Tab(
                    selected = activeTab == t,
                    onClick = { activeTab = t },
                    text = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Selected Tab Content Display
        Box(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            when (activeTab) {
                "LIVE" -> LiveCenterTab(state)
                "SCORECARD" -> ScorecardCenterTab(state)
                "COMMENTARY" -> CommentaryCenterTab(state)
                "SQUADS" -> SquadsCenterTab(state, viewModel)
            }
        }
    }
}

// ----------------------------------------------------
// MATCH CENTER SUBTAB - LIVE
// ----------------------------------------------------
@Composable
fun LiveCenterTab(state: ComputedMatchState) {
    val active = state.activeInnings
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Giant Hero Score details
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TurfGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${active.battingTeamName} Innings".uppercase(),
                        color = GoldAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${active.score}/${active.wickets}",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        "Overs: ${active.oversString} (${state.match.oversCount} overs max)",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.match.status == "COMPLETED") {
                        Card(colors = CardDefaults.cardColors(containerColor = GoldAccent)) {
                            Text(
                                state.resultSummary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = TurfGreen,
                                fontSize = 12.sp
                            )
                        }
                    } else if (state.currentInningsNo == 2) {
                        Text(
                            "Target: ${state.innings1.score + 1} | Need ${state.requiredRuns} from ${state.remainingBalls} balls",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            "Innings 1 Live Score",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Partnership display summary
        item {
            val part = active.partnerships.lastOrNull { it.active }
            if (part != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "CURRENT PARTNERSHIP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(part.batterAName, fontWeight = FontWeight.Bold, color = TurfGreen)
                            Text(
                                "${part.runs} runs (${part.balls} balls)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TurfLime
                            )
                            Text(part.batterBName, fontWeight = FontWeight.Bold, color = TurfGreen)
                        }
                    }
                }
            }
        }

        // 3. Current active batters table
        item {
            Card(colors = CardDefaults.cardColors(containerColor = LightSurface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE BATTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(8.dp))
                    active.batters.filter { it.isStriker || it.isNonStriker }.forEach { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                b.name + (if (b.isStriker) " *" else ""),
                                fontWeight = FontWeight.Bold,
                                color = if (b.isStriker) TurfLime else Color.DarkGray
                            )
                            Text("${b.runs} (${b.balls})", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// MATCH CENTER SUBTAB - SCORECARD
// ----------------------------------------------------
@Composable
fun ScorecardCenterTab(state: ComputedMatchState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Inning 1 Scorecard Card
        item {
            ScorecardDetailsWidget(title = "Innings 1 - ${state.innings1.battingTeamName}", state = state.innings1)
        }

        // Inning 2 if exists
        state.innings2?.let { i2 ->
            item {
                ScorecardDetailsWidget(title = "Innings 2 - ${i2.battingTeamName}", state = i2)
            }
        }
    }
}

@Composable
fun ScorecardDetailsWidget(title: String, state: InningsState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), fontWeight = FontWeight.Black, color = TurfGreen, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Batting scorecard table headers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Batsman", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text("R", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("B", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("4s", modifier = Modifier.weight(0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("6s", modifier = Modifier.weight(0.4f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("S/R", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Divider()
            Spacer(modifier = Modifier.height(6.dp))

            // Rows
            state.batters.forEach { b ->
                if (b.balls > 0 || b.isDismissed) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(b.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Text(b.dismissalDescription, fontSize = 10.sp, color = Color(0xFF94A3B8), fontStyle = FontStyle.Italic)
                        }
                        Text("${b.runs}", modifier = Modifier.weight(0.5f), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
                        Text("${b.balls}", modifier = Modifier.weight(0.5f), fontSize = 12.sp, color = Color(0xFFCBD5E1), textAlign = TextAlign.End)
                        Text("${b.fours}", modifier = Modifier.weight(0.4f), fontSize = 12.sp, color = Color(0xFFCBD5E1), textAlign = TextAlign.End)
                        Text("${b.sixes}", modifier = Modifier.weight(0.4f), fontSize = 12.sp, color = Color(0xFFCBD5E1), textAlign = TextAlign.End)
                        Text(String.format("%.1f", b.strikeRate), modifier = Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TurfLime, textAlign = TextAlign.End)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Extras Details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Extras", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "Total ${state.extras.total} (Wd:${state.extras.wides}, Nb:${state.extras.noBalls}, B:${state.extras.byes}, Lb:${state.extras.legByes})",
                    fontSize = 11.sp, color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bowling Cards Columns
            Text("BOWLING FIGURES", fontWeight = FontWeight.Bold, color = TurfGreen, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Bowler", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text("O", modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("M", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("R", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("W", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
                Text("Econ", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End)
            }

            Divider()

            state.bowlers.forEach { b ->
                if (b.ballsBowled > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(b.name, modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(b.oversString, modifier = Modifier.weight(0.6f), fontSize = 11.sp, color = Color.White, textAlign = TextAlign.End)
                        Text("${b.maidens}", modifier = Modifier.weight(0.5f), fontSize = 11.sp, color = Color.White, textAlign = TextAlign.End)
                        Text("${b.runsConceded}", modifier = Modifier.weight(0.5f), fontSize = 11.sp, color = Color.White, textAlign = TextAlign.End)
                        Text("${b.wickets}", modifier = Modifier.weight(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Red, textAlign = TextAlign.End)
                        Text(String.format("%.1f", b.economy), modifier = Modifier.weight(0.8f), fontSize = 11.sp, color = TurfLime, textAlign = TextAlign.End)
                    }
                }
            }

            // Fall of Wickets
            if (state.fallOfWickets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("FALL OF WICKETS", fontWeight = FontWeight.Bold, color = TurfGreen, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.fallOfWickets.joinToString(", ") { "${it.wicketNo}-${it.runs} (${it.batsmanName}, ${it.oversBall} ov)" },
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ----------------------------------------------------
// MATCH CENTER SUBTAB - COMMENTARY
// ----------------------------------------------------
@Composable
fun CommentaryCenterTab(state: ComputedMatchState) {
    val context = LocalContext.current
    val matchId = state.match.id
    val db = CricketDatabase.getInstance(context)
    val deliveriesFlow = db.deliveryDao.getDeliveriesForMatch(matchId).collectAsStateWithLifecycle(initialValue = emptyList())
    val deliveries = deliveriesFlow.value.reversed()

    if (deliveries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No balls bowled yet", color = Color.Gray, fontSize = 14.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(deliveries) { ball ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Giant Ball Descriptor index
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ball.isWicket) Color.Red else if (ball.runsSimple >= 4) TurfGreen else DarkBackground)
                                .wrapContentSize(Alignment.Center)
                        ) {
                            Text(
                                text = if (ball.isWicket) "W" else "${ball.runsSimple + ball.extraRuns}",
                                color = if (ball.isWicket || ball.runsSimple >= 4) Color.White else TurfGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Ov ${ball.overIndex}.${ball.ballIndexInOver} | ${ball.bowlerName} to ${ball.batsmanName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            
                            val desc = when {
                                ball.isWicket -> "OUT! ${ball.dismissalType} (${ball.dismissedBatsmanName})"
                                ball.extraType == "WIDE" -> "Wide ball (+${ball.extraRuns})"
                                ball.extraType == "NO_BALL" -> "No ball! Free hit (+${ball.extraRuns})"
                                ball.runsSimple == 4 -> "FOUR! Boundary cracked through covers!"
                                ball.runsSimple == 6 -> "SIX! Clean hit out of the park!"
                                else -> "Runs: ${ball.runsSimple}"
                            }
                            Text(desc, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// MATCH CENTER SUBTAB - SQUADS
// ----------------------------------------------------
@Composable
fun SquadsCenterTab(state: ComputedMatchState, viewModel: CricketViewModel) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    val playersMap = players.associateBy { it.id }

    val squadAIds = state.match.teamASquadIds.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
    val squadBIds = state.match.teamBSquadIds.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Squad A
        Column(modifier = Modifier.weight(1f)) {
            Text(state.match.teamAName, fontWeight = FontWeight.Black, color = TurfGreen, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(squadAIds) { id ->
                    val pl = playersMap[id]
                    Card(colors = CardDefaults.cardColors(containerColor = LightSurface)) {
                        Text(
                            text = pl?.name ?: "Player $id",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Squad B
        Column(modifier = Modifier.weight(1f)) {
            Text(state.match.teamBName, fontWeight = FontWeight.Black, color = TurfGreen, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(squadBIds) { id ->
                    val pl = playersMap[id]
                    Card(colors = CardDefaults.cardColors(containerColor = LightSurface)) {
                        Text(
                            text = pl?.name ?: "Player $id",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. TEAMS ROSTER VIEW
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamsRosterScreen(viewModel: CricketViewModel) {
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    var searchTeamQuery by remember { mutableStateOf("") }
    var showAddTeamDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TurfGreen)
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CLUBS & TEAM RECORDS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                Button(
                    onClick = { showAddTeamDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("+ Add Club", color = TurfGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Search
        OutlinedTextField(
            value = searchTeamQuery,
            onValueChange = { searchTeamQuery = it },
            placeholder = { Text("Search Clubs...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = getDarkTextFieldColors(TurfGreen)
        )

        val filteredTeams = teams.filter { it.name.lowercase().contains(searchTeamQuery.lowercase()) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredTeams) { team ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                team.logoUrl.ifEmpty { team.name.take(1).uppercase() },
                                fontSize = 32.sp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(PolishBackground, shape = RoundedCornerShape(8.dp))
                                    .wrapContentSize(Alignment.Center)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(team.name, fontWeight = FontWeight.Black, color = TurfGreen, fontSize = 16.sp)
                                Text(
                                    "Played: ${team.matchesPlayed} | Won: ${team.wins} | Lost: ${team.losses}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Net Run Rate (NRR):",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            val nrrVal = if (team.netRunRate.isNaN()) 0.0 else team.netRunRate
                            Text(
                                text = String.format("%+.3f", nrrVal),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (nrrVal >= 0) TurfLime else Color.Red
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTeamDialog) {
        var teamName by remember { mutableStateOf("") }
        var teamLogo by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddTeamDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Register New Club", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TurfGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Club Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDarkTextFieldColors(TurfGreen)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logo will be generated from initials automatically

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (teamName.isNotEmpty()) {
                                viewModel.createTeam(teamName, teamLogo)
                                showAddTeamDialog = false
                                teamName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Team")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. PLAYERS ROSTER VIEW
// ----------------------------------------------------
@Composable
fun PlayersRosterScreen(viewModel: CricketViewModel) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    var searchPlayerQuery by remember { mutableStateOf("") }
    var showAddPlayerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TurfGreen)
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PLAYERS & CAREER RECORDS", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                Button(
                    onClick = { showAddPlayerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("+ Add Player", color = TurfGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Search
        OutlinedTextField(
            value = searchPlayerQuery,
            onValueChange = { searchPlayerQuery = it },
            placeholder = { Text("Search Players by Name / Role...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = getDarkTextFieldColors(TurfGreen)
        )

        val filteredPlayers = playerRosterFilterAndSearch(products = players, query = searchPlayerQuery)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredPlayers) { player ->
                Card(colors = CardDefaults.cardColors(containerColor = LightSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TurfGreen)
                                    .wrapContentSize(Alignment.Center)
                            ) {
                                Text(
                                    player.name.firstOrNull()?.toString()?.uppercase() ?: "P",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(player.name, fontWeight = FontWeight.Black, color = TurfGreen, fontSize = 15.sp)
                                Text("Club: ${player.clubName.ifEmpty { "Free Agent" }} | Role: ${player.designRole}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Batting stats
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("BATTING RUNS", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                Text("${player.battingRuns}", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("HIGHEST SCORE", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                Text("${player.battingHighestScore}", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("WKTS TAKEN", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                Text("${player.bowlingWickets}", fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPlayerDialog) {
        var playerName by remember { mutableStateOf("") }
        var playerRole by remember { mutableStateOf("ALL_ROUNDER") } // BATS_MAN, BOWLER, ALL_ROUNDER, WICKET_KEEPER
        var clubName by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddPlayerDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Register New Player", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TurfGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Player Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDarkTextFieldColors(TurfGreen)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = clubName,
                        onValueChange = { clubName = it },
                        label = { Text("Club/Team Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getDarkTextFieldColors(TurfGreen)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Squad Role:", fontSize = 12.sp, color = Color.Gray)
                    listOf("BATS_MAN", "BOWLER", "ALL_ROUNDER", "WICKET_KEEPER").forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playerRole = r }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = playerRole == r, onClick = { playerRole = r })
                            Text(r.replace("_", " "), color = Color.DarkGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (playerName.isNotEmpty()) {
                                viewModel.createPlayer(playerName, clubName, playerRole)
                                showAddPlayerDialog = false
                                playerName = ""
                                clubName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurfGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register Player")
                    }
                }
            }
        }
    }
}

private fun playerRosterFilterAndSearch(products: List<PlayerEntity>, query: String): List<PlayerEntity> {
    return products.filter {
        it.name.lowercase().contains(query.lowercase()) ||
        it.clubName.lowercase().contains(query.lowercase()) ||
        it.designRole.lowercase().contains(query.lowercase())
    }
}
