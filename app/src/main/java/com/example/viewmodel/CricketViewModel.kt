package com.example.viewmodel

import android.app.Application
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class CricketViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CricketDatabase.getInstance(application)
    private val repository = CricketRepository(db)

    // Data lists
    val players: StateFlow<List<PlayerEntity>> = repository.allPlayers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val teams: StateFlow<List<TeamEntity>> = repository.allTeams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val matches: StateFlow<List<MatchEntity>> = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Live Scoring match ID
    val activeMatchId = MutableStateFlow<Int?>(null)

    // Undo-Redo memory stack
    private val undoneDeliveries = mutableListOf<DeliveryEntity>()

    // Loading status
    val isDbPrepopulating = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            isDbPrepopulating.value = false
        }
    }

    // Computed State of Active Match
    val activeMatchState: StateFlow<ComputedMatchState?> = activeMatchId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                val matchFlow = repository.getMatchByIdFlow(id)
                val deliveriesFlow = repository.getDeliveriesForMatch(id)
                val playersFlow = repository.allPlayers

                combine(matchFlow, deliveriesFlow, playersFlow) { match, deliveries, allPlayersList ->
                    if (match == null) {
                        null
                    } else {
                        val playersMap = allPlayersList.associateBy { it.id }
                        ScoringEngine.computeMatchState(match, deliveries, playersMap)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setActiveMatch(matchId: Int?) {
        activeMatchId.value = matchId
        undoneDeliveries.clear()
    }

    // ----------------------------------------------------
    // SCORES ENGINE COMMANDS
    // ----------------------------------------------------

    fun createMatch(
        teamAId: Int,
        teamBId: Int,
        venue: String,
        overs: Int,
        ballsPerOver: Int,
        teamASquad: List<Int>,
        teamBSquad: List<Int>,
        scheduledDate: String,
        scheduledTime: String,
        superOver: Boolean = false,
        freeHit: Boolean = true
    ) {
        viewModelScope.launch {
            val teamA = repository.getTeamById(teamAId) ?: return@launch
            val teamB = repository.getTeamById(teamBId) ?: return@launch

            val match = MatchEntity(
                teamAId = teamAId,
                teamBId = teamBId,
                teamAName = teamA.name,
                teamBName = teamB.name,
                teamALogo = teamA.logoUrl,
                teamBLogo = teamB.logoUrl,
                venue = venue,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                oversCount = overs,
                ballsPerOver = ballsPerOver,
                superOver = superOver,
                freeHitRules = freeHit,
                teamASquadIds = teamASquad.joinToString(","),
                teamBSquadIds = teamBSquad.joinToString(","),
                status = "PRE_MATCH"
            )

            val newId = repository.insertMatch(match).toInt()
            setActiveMatch(newId)
        }
    }

    fun createMatchWithCustomTeams(
        teamAName: String,
        teamBName: String,
        venue: String,
        overs: Int,
        ballsPerOver: Int,
        teamAPlayers: List<PlayerEntity>,
        teamBPlayers: List<PlayerEntity>,
        scheduledDate: String,
        scheduledTime: String,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedA = teamAName.trim().ifEmpty { "Team A" }
            val trimmedB = teamBName.trim().ifEmpty { "Team B" }

            val allT = repository.allTeams.first()
            val existingA = allT.find { it.name.equals(trimmedA, ignoreCase = true) }
            val teamAId = existingA?.id?.toInt() ?: repository.insertTeam(TeamEntity(name = trimmedA, logoUrl = "")).toInt()

            val existingB = allT.find { it.name.equals(trimmedB, ignoreCase = true) }
            val teamBId = existingB?.id?.toInt() ?: repository.insertTeam(TeamEntity(name = trimmedB, logoUrl = "")).toInt()

            val allP = repository.allPlayers.first()

            // Resolve and save selected player list for Team A
            val teamASavedIds = teamAPlayers.map { p ->
                val existingPlayer = allP.find { it.name.equals(p.name.trim(), ignoreCase = true) && it.clubName.equals(trimmedA, ignoreCase = true) }
                if (existingPlayer != null) {
                    existingPlayer.id
                } else {
                    repository.insertPlayer(p.copy(clubName = trimmedA)).toInt()
                }
            }

            // Resolve and save selected player list for Team B
            val teamBSavedIds = teamBPlayers.map { p ->
                val existingPlayer = allP.find { it.name.equals(p.name.trim(), ignoreCase = true) && it.clubName.equals(trimmedB, ignoreCase = true) }
                if (existingPlayer != null) {
                    existingPlayer.id
                } else {
                    repository.insertPlayer(p.copy(clubName = trimmedB)).toInt()
                }
            }

            val match = MatchEntity(
                teamAId = teamAId,
                teamBId = teamBId,
                teamAName = trimmedA,
                teamBName = trimmedB,
                teamALogo = existingA?.logoUrl ?: "",
                teamBLogo = existingB?.logoUrl ?: "",
                venue = venue,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                oversCount = overs,
                ballsPerOver = ballsPerOver,
                teamASquadIds = teamASavedIds.joinToString(","),
                teamBSquadIds = teamBSavedIds.joinToString(","),
                status = "PRE_MATCH"
            )

            val newId = repository.insertMatch(match).toInt()
            setActiveMatch(newId)
            onComplete(newId)
        }
    }

    fun submitTossResult(matchId: Int, winnerId: Int, decision: String) {
        viewModelScope.launch {
            val match = repository.getMatchById(matchId) ?: return@launch
            val firstBattingTeamId = if (decision == "BAT") winnerId else {
                if (winnerId == match.teamAId) match.teamBId else match.teamAId
            }
            val secondBattingTeamId = if (firstBattingTeamId == match.teamAId) match.teamBId else match.teamAId

            val updated = match.copy(
                tossWinnerId = winnerId,
                tossDecision = decision,
                firstInningsTeamId = firstBattingTeamId,
                secondInningsTeamId = secondBattingTeamId,
                status = "LIVE",
                currentInningsNo = 1
            )
            repository.updateMatch(updated)
        }
    }

    fun recordDelivery(
        runsSimple: Int,
        extraType: String, // "NONE", "WIDE", "NO_BALL", "BYE", "LEG_BYE"
        extraRuns: Int,
        isWicket: Boolean,
        dismissalType: String = "NONE",
        dismissedBatsmanId: Int = 0,
        fielderName: String = "",
        strikerId: Int,
        nonStrikerId: Int,
        bowlerId: Int
    ) {
        val mState = activeMatchState.value ?: return
        val match = mState.match
        val activeInnings = mState.activeInnings

        viewModelScope.launch {
            val batsmanEntity = repository.getPlayerById(strikerId) ?: return@launch
            val bowlerEntity = repository.getPlayerById(bowlerId) ?: return@launch

            // Calculate current ball indices
            val currentLegalBalls = activeInnings.ballsBowled
            val overIndexForThisBall = currentLegalBalls / match.ballsPerOver
            val ballInOverIndex = (currentLegalBalls % match.ballsPerOver) + 1

            val dismissedBatsmanName = if (isWicket) {
                val dId = if (dismissedBatsmanId != 0) dismissedBatsmanId else strikerId
                repository.getPlayerById(dId)?.name ?: "Batsman"
            } else ""

            val delivery = DeliveryEntity(
                matchId = match.id,
                inningsNo = mState.currentInningsNo,
                overIndex = overIndexForThisBall,
                ballIndexInOver = ballInOverIndex,
                batsmanId = strikerId,
                batsmanName = batsmanEntity.name,
                bowlerId = bowlerId,
                bowlerName = bowlerEntity.name,
                runsSimple = runsSimple,
                extraType = extraType,
                extraRuns = extraRuns,
                isWicket = isWicket,
                dismissalType = dismissalType,
                dismissedBatsmanId = dismissedBatsmanId.takeIf { it != 0 } ?: strikerId,
                dismissedBatsmanName = dismissedBatsmanName,
                fielderName = fielderName
            )

            repository.insertDelivery(delivery)
            undoneDeliveries.clear() // Clear redo stack on new action
        }
    }

    fun undoLastBall() {
        val matchId = activeMatchId.value ?: return
        viewModelScope.launch {
            val deliveriesList = repository.getDeliveriesForMatchList(matchId)
            if (deliveriesList.isNotEmpty()) {
                val last = deliveriesList.last()
                undoneDeliveries.add(last)
                repository.deleteLastDelivery(matchId)
            }
        }
    }

    fun redoLastBall() {
        if (undoneDeliveries.isNotEmpty()) {
            val delivery = undoneDeliveries.removeAt(undoneDeliveries.size - 1)
            viewModelScope.launch {
                repository.insertDelivery(delivery)
            }
        }
    }

    fun proceedToSecondInnings() {
        val mState = activeMatchState.value ?: return
        val match = mState.match
        viewModelScope.launch {
            val updated = match.copy(
                currentInningsNo = 2,
                firstInningsScore = mState.innings1.score,
                firstInningsWickets = mState.innings1.wickets,
                firstInningsOversBalls = mState.innings1.ballsBowled
            )
            repository.updateMatch(updated)
        }
    }

    fun completeMatch(winnerTeamId: Int, marginSummary: String, playerOfTheMatchId: Int) {
        val mState = activeMatchState.value ?: return
        val match = mState.match
        viewModelScope.launch {
            val finalMatch = match.copy(
                status = "COMPLETED",
                winnerId = winnerTeamId,
                winningMargin = marginSummary,
                playerOfTheMatchId = playerOfTheMatchId,
                secondInningsScore = mState.innings2?.score ?: 0,
                secondInningsWickets = mState.innings2?.wickets ?: 0,
                secondInningsOversBalls = mState.innings2?.ballsBowled ?: 0,
                isLocked = true
            )
            repository.updateMatch(finalMatch)

            // Update Statistics dynamically for Teams and Players!
            updateCumulativeStatsAfterCompletion(mState, winnerTeamId, marginSummary, playerOfTheMatchId)
        }
    }

    fun reopenMatch(matchId: Int) {
        viewModelScope.launch {
            val match = repository.getMatchById(matchId) ?: return@launch
            val updated = match.copy(
                status = "LIVE",
                isLocked = false
            )
            repository.updateMatch(updated)
            setActiveMatch(matchId)
        }
    }

    // Create a custom Player
    fun createPlayer(name: String, clubName: String, role: String) {
        viewModelScope.launch {
            val player = PlayerEntity(
                name = name,
                clubName = clubName,
                designRole = role
            )
            repository.insertPlayer(player)
        }
    }

    // Create custom Team
    fun createTeam(name: String, logoEmoji: String) {
        viewModelScope.launch {
            val team = TeamEntity(
                name = name,
                logoUrl = logoEmoji
            )
            repository.insertTeam(team)
        }
    }

    // Update cumulative player & team stats in Room on match finished
    private suspend fun updateCumulativeStatsAfterCompletion(
        mState: ComputedMatchState,
        winnerTeamId: Int,
        margin: String,
        potmId: Int
    ) {
        val match = mState.match
        val i1 = mState.innings1
        val i2 = mState.innings2 ?: return // Needs 2 innings to finalize stats reasonably

        // Update Team A Stats
        val teamA = repository.getTeamById(match.teamAId)
        if (teamA != null) {
            val iBats = if (match.firstInningsTeamId == match.teamAId) i1 else i2
            val iBowls = if (match.firstInningsTeamId == match.teamAId) i2 else i1

            val isWin = winnerTeamId == match.teamAId
            val isLoss = winnerTeamId != match.teamAId && winnerTeamId != 0
            val isTie = winnerTeamId == 0

            val updatedTeamA = teamA.copy(
                matchesPlayed = teamA.matchesPlayed + 1,
                wins = teamA.wins + (if (isWin) 1 else 0),
                losses = teamA.losses + (if (isLoss) 1 else 0),
                ties = teamA.ties + (if (isTie) 1 else 0),
                totalRunsScored = teamA.totalRunsScored + iBats.score,
                totalBallsFaced = teamA.totalBallsFaced + iBats.ballsBowled,
                totalRunsConceded = teamA.totalRunsConceded + iBowls.score,
                totalBallsBowled = teamA.totalBallsBowled + iBowls.ballsBowled
            )
            repository.updateTeam(updatedTeamA)
        }

        // Update Team B Stats
        val teamB = repository.getTeamById(match.teamBId)
        if (teamB != null) {
            val iBats = if (match.secondInningsTeamId == match.teamBId) i2 else i1
            val iBowls = if (match.secondInningsTeamId == match.teamBId) i1 else i2

            val isWin = winnerTeamId == match.teamBId
            val isLoss = winnerTeamId != match.teamBId && winnerTeamId != 0
            val isTie = winnerTeamId == 0

            val updatedTeamB = teamB.copy(
                matchesPlayed = teamB.matchesPlayed + 1,
                wins = teamB.wins + (if (isWin) 1 else 0),
                losses = teamB.losses + (if (isLoss) 1 else 0),
                ties = teamB.ties + (if (isTie) 1 else 0),
                totalRunsScored = teamB.totalRunsScored + iBats.score,
                totalBallsFaced = teamB.totalBallsFaced + iBats.ballsBowled,
                totalRunsConceded = teamB.totalRunsConceded + iBowls.score,
                totalBallsBowled = teamB.totalBallsBowled + iBowls.ballsBowled
            )
            repository.updateTeam(updatedTeamB)
        }

        // Update Batter Stats
        val allBatters = i1.batters + i2.batters
        allBatters.forEach { b ->
            if (b.balls > 0 || b.isDismissed) {
                val dbPlayer = repository.getPlayerById(b.playerId)
                if (dbPlayer != null) {
                    val isFifty = b.runs in 50..99
                    val isHundred = b.runs >= 100
                    val updatedPlayer = dbPlayer.copy(
                        battingMatches = dbPlayer.battingMatches + 1,
                        battingInnings = dbPlayer.battingInnings + 1,
                        battingNotOuts = dbPlayer.battingNotOuts + (if (b.isDismissed) 0 else 1),
                        battingRuns = dbPlayer.battingRuns + b.runs,
                        battingHighestScore = maxOf(dbPlayer.battingHighestScore, b.runs),
                        battingBalls = dbPlayer.battingBalls + b.balls,
                        battingFours = dbPlayer.battingFours + b.fours,
                        battingSixes = dbPlayer.battingSixes + b.sixes,
                        battingFiftyCount = dbPlayer.battingFiftyCount + (if (isFifty) 1 else 0),
                        battingHundredCount = dbPlayer.battingHundredCount + (if (isHundred) 1 else 0)
                    )
                    repository.updatePlayer(updatedPlayer)
                }
            }
        }

        // Update Bowler Stats
        val allBowlers = i1.bowlers + i2.bowlers
        allBowlers.forEach { bo ->
            if (bo.ballsBowled > 0) {
                val dbPlayer = repository.getPlayerById(bo.playerId)
                if (dbPlayer != null) {
                    val bestWickets = dbPlayer.bowlingBestWickets
                    val bestRuns = dbPlayer.bowlingBestRunsConceded
                    val isNewBest = bo.wickets > bestWickets || (bo.wickets == bestWickets && bo.runsConceded < bestRuns)

                    val updatedPlayer = dbPlayer.copy(
                        bowlingOversBalls = dbPlayer.bowlingOversBalls + bo.ballsBowled,
                        bowlingRunsConceded = dbPlayer.bowlingRunsConceded + bo.runsConceded,
                        bowlingWickets = dbPlayer.bowlingWickets + bo.wickets,
                        bowlingBestWickets = if (isNewBest) bo.wickets else bestWickets,
                        bowlingBestRunsConceded = if (isNewBest) bo.runsConceded else bestRuns,
                        bowlingWides = dbPlayer.bowlingWides + bo.wides,
                        bowlingNoBalls = dbPlayer.bowlingNoBalls + bo.noBalls
                    )
                    repository.updatePlayer(updatedPlayer)
                }
            }
        }
    }

    // ----------------------------------------------------
    // DATA EXPORT UTILITIES (JSON & PDF)
    // ----------------------------------------------------

    fun exportMatchToJson(matchId: Int): String {
        return try {
            val match = db.openHelper.writableDatabase // verify initialized
            val mState = activeMatchState.value ?: return "{}"
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(MatchEntity::class.java)
            adapter.toJson(mState.match)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun generatePdfReport(matchId: Int, onComplete: (File?) -> Unit) {
        val state = activeMatchState.value
        if (state == null) {
            onComplete(null)
            return
        }

        viewModelScope.launch {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }

                val headerPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 12f
                    isFakeBoldText = true
                }

                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                }

                val boldTextPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    isFakeBoldText = true
                }

                val accentPaint = Paint().apply {
                    color = Color.argb(255, 12, 128, 64)
                    textSize = 12f
                    isFakeBoldText = true
                }

                canvas.drawText("CREASE CRICKET REPORT", 297f, 40f, titlePaint)
                canvas.drawText("${state.match.teamAName} vs ${state.match.teamBName}", 297f, 60f, headerPaint)
                canvas.drawText("Venue: ${state.match.venue} | Date: ${state.match.scheduledDate}", 297f, 75f, textPaint)

                // Match Result Banner
                canvas.drawRect(40f, 95f, 555f, 125f, Paint().apply { color = Color.LTGRAY })
                canvas.drawText("RESULT SUMMARY: ${state.resultSummary.ifEmpty { "Live Match" }}", 50f, 114f, accentPaint)

                // Squads
                var yCoord = 150f
                canvas.drawText("FIRST INNINGS: ${state.innings1.battingTeamName} (${state.innings1.score}/${state.innings1.wickets} in ${state.innings1.oversString} ov)", 40f, yCoord, headerPaint)
                yCoord += 20f

                // Column names
                canvas.drawText("Batter", 40f, yCoord, boldTextPaint)
                canvas.drawText("Runs", 200f, yCoord, boldTextPaint)
                canvas.drawText("Balls", 250f, yCoord, boldTextPaint)
                canvas.drawText("4s", 300f, yCoord, boldTextPaint)
                canvas.drawText("6s", 350f, yCoord, boldTextPaint)
                canvas.drawText("S/R", 400f, yCoord, boldTextPaint)
                yCoord += 15f

                state.innings1.batters.take(10).forEach { b ->
                    canvas.drawText(b.name, 40f, yCoord, textPaint)
                    canvas.drawText(b.runs.toString(), 200f, yCoord, textPaint)
                    canvas.drawText(b.balls.toString(), 250f, yCoord, textPaint)
                    canvas.drawText(b.fours.toString(), 300f, yCoord, textPaint)
                    canvas.drawText(b.sixes.toString(), 350f, yCoord, textPaint)
                    canvas.drawText(String.format("%.1f", b.strikeRate), 400f, yCoord, textPaint)
                    yCoord += 15f
                }

                yCoord += 10f
                canvas.drawText("Bowling Figures:", 40f, yCoord, boldTextPaint)
                yCoord += 15f
                canvas.drawText("Bowler", 40f, yCoord, boldTextPaint)
                canvas.drawText("O", 200f, yCoord, boldTextPaint)
                canvas.drawText("M", 250f, yCoord, boldTextPaint)
                canvas.drawText("R", 300f, yCoord, boldTextPaint)
                canvas.drawText("W", 350f, yCoord, boldTextPaint)
                canvas.drawText("Econ", 400f, yCoord, boldTextPaint)
                yCoord += 15f

                state.innings1.bowlers.take(6).forEach { bo ->
                    canvas.drawText(bo.name, 40f, yCoord, textPaint)
                    canvas.drawText(bo.oversString, 200f, yCoord, textPaint)
                    canvas.drawText(bo.maidens.toString(), 250f, yCoord, textPaint)
                    canvas.drawText(bo.runsConceded.toString(), 300f, yCoord, textPaint)
                    canvas.drawText(bo.wickets.toString(), 350f, yCoord, textPaint)
                    canvas.drawText(String.format("%.1f", bo.economy), 400f, yCoord, textPaint)
                    yCoord += 15f
                }

                if (state.innings2 != null) {
                    yCoord += 20f
                    canvas.drawText("SECOND INNINGS: ${state.innings2.battingTeamName} (${state.innings2.score}/${state.innings2.wickets} in ${state.innings2.oversString} ov)", 40f, yCoord, headerPaint)
                    yCoord += 20f

                    canvas.drawText("Batter", 40f, yCoord, boldTextPaint)
                    canvas.drawText("Runs", 200f, yCoord, boldTextPaint)
                    canvas.drawText("Balls", 250f, yCoord, boldTextPaint)
                    canvas.drawText("4s", 300f, yCoord, boldTextPaint)
                    canvas.drawText("6s", 350f, yCoord, boldTextPaint)
                    canvas.drawText("S/R", 400f, yCoord, boldTextPaint)
                    yCoord += 15f

                    state.innings2.batters.take(10).forEach { b ->
                        canvas.drawText(b.name, 40f, yCoord, textPaint)
                        canvas.drawText(b.runs.toString(), 200f, yCoord, textPaint)
                        canvas.drawText(b.balls.toString(), 250f, yCoord, textPaint)
                        canvas.drawText(b.fours.toString(), 300f, yCoord, textPaint)
                        canvas.drawText(b.sixes.toString(), 350f, yCoord, textPaint)
                        canvas.drawText(String.format("%.1f", b.strikeRate), 400f, yCoord, textPaint)
                        yCoord += 15f
                    }
                }

                pdfDocument.finishPage(page)

                // Save to files path
                val dir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(dir, "Crease_Match_${state.match.id}.pdf")
                val fos = FileOutputStream(file)
                pdfDocument.writeTo(fos)
                fos.close()
                pdfDocument.close()

                onComplete(file)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }
}
