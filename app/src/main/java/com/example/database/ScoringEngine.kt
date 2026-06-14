package com.example.database

import kotlin.math.roundToInt

data class BatterScore(
    val playerId: Int,
    val name: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isStriker: Boolean = false,
    val isNonStriker: Boolean = false,
    val isDismissed: Boolean = false,
    val dismissalDescription: String = "not out", // e.g., "c Rahul b Arjun"
    val orderIndex: Int = 0
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs.toDouble() / balls * 100.0) else 0.0
}

data class BowlerScore(
    val playerId: Int,
    val name: String,
    val ballsBowled: Int = 0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0,
    val activeInOver: Boolean = false
) {
    val oversString: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"
    val economy: Double
        get() = if (ballsBowled > 0) (runsConceded.toDouble() / (ballsBowled / 6.0)) else 0.0
}

data class PartnershipState(
    val batterAId: Int,
    val batterAName: String,
    val batterBId: Int,
    val batterBName: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val active: Boolean = true
)

data class FallOfWicket(
    val wicketNo: Int,
    val runs: Int,
    val oversBall: String,
    val batsmanName: String,
    val bowlerName: String
)

data class ExtrasState(
    val wides: Int = 0,
    val noBalls: Int = 0,
    val byes: Int = 0,
    val legByes: Int = 0,
    val total: Int = 0
)

data class InningsState(
    val inningsNo: Int,
    val battingTeamId: Int,
    val battingTeamName: String,
    val bowlingTeamId: Int,
    val bowlingTeamName: String,
    val score: Int = 0,
    val wickets: Int = 0,
    val ballsBowled: Int = 0, // total legal balls
    val batters: List<BatterScore> = emptyList(),
    val bowlers: List<BowlerScore> = emptyList(),
    val partnerships: List<PartnershipState> = emptyList(),
    val fallOfWickets: List<FallOfWicket> = emptyList(),
    val extras: ExtrasState = ExtrasState(),
    val currentOverBalls: List<DeliveryEntity> = emptyList(),
    val activeStrikerId: Int = 0,
    val activeNonStrikerId: Int = 0,
    val activeBowlerId: Int = 0,
    val isCompleted: Boolean = false
) {
    val oversString: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"
    val runRate: Double
        get() = if (ballsBowled > 0) (score.toDouble() / (ballsBowled / 6.0)) else 0.0
}

data class ComputedMatchState(
    val match: MatchEntity,
    val innings1: InningsState,
    val innings2: InningsState?,
    val currentInningsNo: Int,
    val activeInnings: InningsState,
    val winnerName: String = "",
    val resultSummary: String = "",
    val requiredRuns: Int = 0,
    val remainingBalls: Int = 0,
    val requiredRunRate: Double = 0.0,
    val isCompleted: Boolean = false
)

object ScoringEngine {

    fun computeMatchState(
        match: MatchEntity,
        deliveriesList: List<DeliveryEntity>,
        playersMap: Map<Int, PlayerEntity>
    ): ComputedMatchState {
        // Divide deliveries by innings
        val deliveriesI1 = deliveriesList.filter { it.inningsNo == 1 }
        val deliveriesI2 = deliveriesList.filter { it.inningsNo == 2 }

        // Figure out teams batting first vs second
        val i1BatTeamId = match.firstInningsTeamId.takeIf { it != 0 } ?: if (match.tossWinnerId != 0) {
            if (match.tossDecision == "BAT") match.tossWinnerId else (if (match.tossWinnerId == match.teamAId) match.teamBId else match.teamAId)
        } else match.teamAId

        val i1BowlTeamId = if (i1BatTeamId == match.teamAId) match.teamBId else match.teamAId
        val i1BatTeamName = if (i1BatTeamId == match.teamAId) match.teamAName else match.teamBName
        val i1BowlTeamName = if (i1BatTeamId == match.teamAId) match.teamBName else match.teamAName

        val i2BatTeamId = i1BowlTeamId
        val i2BowlTeamId = i1BatTeamId
        val i2BatTeamName = i1BowlTeamName
        val i2BowlTeamName = i1BatTeamName

        // Parse Squad rosters for starters
        val teamASquad = match.teamASquadIds.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        val teamBSquad = match.teamBSquadIds.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }

        val team1Squad = if (i1BatTeamId == match.teamAId) teamASquad else teamBSquad
        val team1BowlSquad = if (i1BowlTeamId == match.teamAId) teamASquad else teamBSquad

        val team2Squad = team1BowlSquad
        val team2BowlSquad = team1Squad

        // Compute Innings 1
        val i1State = computeInnings(
            inningsNo = 1,
            battingTeamId = i1BatTeamId,
            battingTeamName = i1BatTeamName,
            bowlingTeamId = i1BowlTeamId,
            bowlingTeamName = i1BowlTeamName,
            squadList = team1Squad,
            bowlSquadList = team1BowlSquad,
            deliveries = deliveriesI1,
            playersMap = playersMap,
            oversCount = match.oversCount,
            ballsPerOver = match.ballsPerOver
        )

        val innings2Triggered = match.currentInningsNo == 2 || deliveriesI2.isNotEmpty() || i1State.isCompleted
        val i2State = if (innings2Triggered) {
            computeInnings(
                inningsNo = 2,
                battingTeamId = i2BatTeamId,
                battingTeamName = i2BatTeamName,
                bowlingTeamId = i2BowlTeamId,
                bowlingTeamName = i2BowlTeamName,
                squadList = team2Squad,
                bowlSquadList = team2BowlSquad,
                deliveries = deliveriesI2,
                playersMap = playersMap,
                oversCount = match.oversCount,
                ballsPerOver = match.ballsPerOver,
                targetRuns = i1State.score + 1
            )
        } else {
            null
        }

        // Active State
        val currentInningsNo = if (i2State != null) 2 else 1
        val activeInnings = i2State ?: i1State

        // Calculate Target & Chasing details
        var requiredRuns = 0
        var remainingBalls = 0
        var reqRunRate = 0.0
        var matchWinnerName = ""
        var matchOutcomeSummary = ""
        var isMatchCompleted = false

        if (i2State != null) {
            val target = i1State.score + 1
            val maxMatchBalls = match.oversCount * match.ballsPerOver
            requiredRuns = target - i2State.score
            remainingBalls = maxMatchBalls - i2State.ballsBowled
            if (remainingBalls < 0) remainingBalls = 0

            reqRunRate = if (remainingBalls > 0) {
                (requiredRuns.toDouble() / (remainingBalls / 6.0))
            } else {
                0.0
            }

            // Did team 2 win/lose?
            if (i2State.score >= target) {
                matchWinnerName = i2State.battingTeamName
                val wicketsRemaining = 10 - i2State.wickets
                matchOutcomeSummary = "$matchWinnerName won by $wicketsRemaining wickets"
                isMatchCompleted = true
            } else if (remainingBalls <= 0 || i2State.wickets >= 10 || i2State.isCompleted) {
                if (i2State.score < i1State.score) {
                    matchWinnerName = i1State.battingTeamName
                    val margin = i1State.score - i2State.score
                    matchOutcomeSummary = "$matchWinnerName won by $margin runs"
                    isMatchCompleted = true
                } else if (i2State.score == i1State.score) {
                    matchOutcomeSummary = "Match Tied!"
                    isMatchCompleted = true
                }
            }
        } else {
            // First innings and runs are capped by overs/wickets
            if (i1State.isCompleted || i1State.wickets >= 10) {
                // First innings completed. Summary detail:
                matchOutcomeSummary = "${i1State.battingTeamName} scored ${i1State.score}/${i1State.wickets}. Target for ${i1State.bowlingTeamName} is ${i1State.score + 1}."
            }
        }

        // Overwrite completion if already locked
        if (match.status == "COMPLETED") {
            isMatchCompleted = true
            matchWinnerName = if (match.winnerId == match.teamAId) match.teamAName else if (match.winnerId == match.teamBId) match.teamBName else ""
            matchOutcomeSummary = match.winningMargin.ifEmpty { "Match finished" }
        }

        return ComputedMatchState(
            match = match,
            innings1 = i1State,
            innings2 = i2State,
            currentInningsNo = currentInningsNo,
            activeInnings = activeInnings,
            winnerName = matchWinnerName,
            resultSummary = matchOutcomeSummary,
            requiredRuns = requiredRuns,
            remainingBalls = remainingBalls,
            requiredRunRate = reqRunRate,
            isCompleted = isMatchCompleted
        )
    }

    private fun computeInnings(
        inningsNo: Int,
        battingTeamId: Int,
        battingTeamName: String,
        bowlingTeamId: Int,
        bowlingTeamName: String,
        squadList: List<Int>,
        bowlSquadList: List<Int>,
        deliveries: List<DeliveryEntity>,
        playersMap: Map<Int, PlayerEntity>,
        oversCount: Int,
        ballsPerOver: Int,
        targetRuns: Int? = null
    ): InningsState {
        var score = 0
        var wickets = 0
        var totalLegalBalls = 0

        // Track stats per player dynamically based on delivery sequence
        val batterMap = squadList.mapIndexed { idx, id ->
            id to BatterScore(
                playerId = id,
                name = playersMap[id]?.name ?: "Player $id",
                orderIndex = idx
            )
        }.toMap().toMutableMap()

        val bowlerMap = bowlSquadList.map { id ->
            id to BowlerScore(
                playerId = id,
                name = playersMap[id]?.name ?: "Bowler $id"
            )
        }.toMap().toMutableMap()

        val partnerships = mutableListOf<PartnershipState>()
        val fallOfWickets = mutableListOf<FallOfWicket>()

        var wideCount = 0
        var noBallCount = 0
        var byeCount = 0
        var legByeCount = 0

        // Handle Active Striker and Non-Striker rotation
        // Initial setup on first deliveries: If empty, we wait
        var strikerId = 0
        var nonStrikerId = 0
        var currentBowlerId = 0

        fun getNextAvailableBatsman(): Int {
            val usedIds = batterMap.values.filter { it.balls > 0 || it.isDismissed || it.playerId == strikerId || it.playerId == nonStrikerId }.map { it.playerId }
            return squadList.firstOrNull { it !in usedIds } ?: 0
        }

        // Sequentially process delivery history
        deliveries.forEachIndexed { deliveryIdx, delivery ->
            val bId = delivery.batsmanId
            val boId = delivery.bowlerId

            // Auto-initialize striker and nonStrikers if doing sequential replay
            if (strikerId == 0) strikerId = bId
            if (nonStrikerId == 0) {
                val remTeam = squadList.filter { it != strikerId }
                nonStrikerId = remTeam.firstOrNull() ?: 0
            }
            currentBowlerId = boId

            // Fetch batter and bowler scores
            var batter = batterMap[bId] ?: BatterScore(bId, delivery.batsmanName)
            var bowler = bowlerMap[boId] ?: BowlerScore(boId, delivery.bowlerName)

            // Evaluate delivery properties
            val extraType = delivery.extraType
            val runsSimple = delivery.runsSimple
            val extraRuns = delivery.extraRuns

            // Extra scores
            val isWide = extraType == "WIDE"
            val isNoBall = extraType == "NO_BALL"
            val isBye = extraType == "BYE"
            val isLegBye = extraType == "LEG_BYE"
            val isLegal = !isWide && !isNoBall

            // Core tallies
            var ballTotalScore = runsSimple
            if (isWide) {
                ballTotalScore += extraRuns // e.g. 1 (wide) + running
                wideCount += extraRuns
            } else if (isNoBall) {
                ballTotalScore += extraRuns // 1 (no-ball) + batsman runs
                noBallCount += extraRuns
            } else if (isBye) {
                ballTotalScore += extraRuns
                byeCount += extraRuns
            } else if (isLegBye) {
                ballTotalScore += extraRuns
                legByeCount += extraRuns
            }

            score += ballTotalScore

            // Update Batsman record
            var batsmanRunsScored = 0
            var batsmanBallsFaced = 0
            if (!isWide) {
                batsmanRunsScored = runsSimple
                batsmanBallsFaced = 1
            }

            batter = batter.copy(
                runs = batter.runs + batsmanRunsScored,
                balls = batter.balls + batsmanBallsFaced,
                fours = batter.fours + (if (runsSimple == 4) 1 else 0),
                sixes = batter.sixes + (if (runsSimple == 6) 1 else 0)
            )
            batterMap[bId] = batter

            // Update Bowler record
            var bowlerBallsBowled = 0
            var runsAddedToBowler = ballTotalScore
            if (isLegal) {
                bowlerBallsBowled = 1
            }
            if (isBye || isLegBye) {
                // Byes & LegByes are NOT charged to the bowler
                runsAddedToBowler = if (isNoBall) extraRuns else 0
            }

            bowler = bowler.copy(
                ballsBowled = bowler.ballsBowled + bowlerBallsBowled,
                runsConceded = bowler.runsConceded + runsAddedToBowler,
                wides = bowler.wides + (if (isWide) extraRuns else 0),
                noBalls = bowler.noBalls + (if (isNoBall) 1 else 0)
            )

            // Update totals
            if (isLegal) {
                totalLegalBalls++
            }

            // Handle Wickets
            val isWicket = delivery.isWicket
            if (isWicket) {
                wickets++
                val dType = delivery.dismissalType
                val dName = delivery.dismissedBatsmanName
                val fName = delivery.fielderName

                // Formulate description
                val desc = when (dType) {
                    "BOWLED" -> "b ${delivery.bowlerName}"
                    "CAUGHT" -> if (fName.isNotEmpty()) "c $fName b ${delivery.bowlerName}" else "c & b ${delivery.bowlerName}"
                    "LBW" -> "lbw b ${delivery.bowlerName}"
                    "STUMPED" -> "st $fName b ${delivery.bowlerName}"
                    "RUN_OUT" -> "run out ($fName)"
                    "HIT_WICKET" -> "hit wicket b ${delivery.bowlerName}"
                    "RETIRED_OUT" -> "retired out"
                    else -> "out"
                }

                val dismissedId = delivery.dismissedBatsmanId.takeIf { it != 0 } ?: bId
                var dPlayer = batterMap[dismissedId] ?: BatterScore(dismissedId, dName)
                dPlayer = dPlayer.copy(isDismissed = true, dismissalDescription = desc)
                batterMap[dismissedId] = dPlayer

                if (dType != "RETIRED_OUT" && dType != "RUN_OUT") {
                    bowler = bowler.copy(wickets = bowler.wickets + 1)
                }

                // Log fall of wicket
                val overRepresentation = "${totalLegalBalls / 6}.${totalLegalBalls % 6}"
                fallOfWickets.add(
                    FallOfWicket(
                        wicketNo = wickets,
                        runs = score,
                        oversBall = overRepresentation,
                        batsmanName = dName,
                        bowlerName = delivery.bowlerName
                    )
                )

                // Swap out the batsman
                if (dismissedId == strikerId) {
                    strikerId = getNextAvailableBatsman()
                } else if (dismissedId == nonStrikerId) {
                    nonStrikerId = getNextAvailableBatsman()
                }
            }

            // Save bowler scores
            bowlerMap[boId] = bowler

            // Strike rotation on legal simple runs or no-ball runs
            val runsRotated = if (isWide) {
                // Wides run rotation rules: if they ran odd number of runs
                (extraRuns - 1)
            } else {
                runsSimple + (if (isBye || isLegBye) extraRuns else 0)
            }

            if (runsRotated % 2 != 0) {
                val temp = strikerId
                strikerId = nonStrikerId
                nonStrikerId = temp
            }

            // End of over strike rotation (6 legal balls completes the over)
            val overCompleted = isLegal && (totalLegalBalls % 6 == 0)
            if (overCompleted) {
                val temp = strikerId
                strikerId = nonStrikerId
                nonStrikerId = temp
            }

            // Build partnerships
            // Simple partnership model: find/update current active partnership
            val currentPartIndex = partnerships.indexOfFirst { it.active }
            if (currentPartIndex != -1) {
                val part = partnerships[currentPartIndex]
                val updatedPart = part.copy(
                    runs = part.runs + ballTotalScore,
                    balls = part.balls + (if (!isWide) 1 else 0)
                )
                if (isWicket) {
                    partnerships[currentPartIndex] = updatedPart.copy(active = false)
                } else {
                    partnerships[currentPartIndex] = updatedPart
                }
            } else {
                // Start a brand new partnership!
                val bAName = playersMap[strikerId]?.name ?: "Striker"
                val bBName = playersMap[nonStrikerId]?.name ?: "Non-Striker"
                partnerships.add(
                    PartnershipState(
                        batterAId = strikerId,
                        batterAName = bAName,
                        batterBId = nonStrikerId,
                        batterBName = bBName,
                        runs = ballTotalScore,
                        balls = if (!isWide) 1 else 0,
                        active = !isWicket
                    )
                )
            }

            // If we hit target runs in 2nd innings, stop processing!
            if (targetRuns != null && score >= targetRuns) {
                return@forEachIndexed
            }
        }

        // Build active / non-active status labels for batter entities Map
        val finalBatters = batterMap.values.map { b ->
            b.copy(
                isStriker = b.playerId == strikerId && strikerId != 0,
                isNonStriker = b.playerId == nonStrikerId && nonStrikerId != 0
            )
        }.sortedBy { it.orderIndex }

        val finalBowlers = bowlerMap.values.map { bo ->
            bo.copy(activeInOver = bo.playerId == currentBowlerId && currentBowlerId != 0)
        }

        val totalMaxBalls = oversCount * ballsPerOver
        val inningsFinished = totalLegalBalls >= totalMaxBalls || wickets >= 10 || (targetRuns != null && score >= targetRuns)

        // Extras breakdown
        val totalExtrasCount = wideCount + noBallCount + byeCount + legByeCount
        val extras = ExtrasState(
            wides = wideCount,
            noBalls = noBallCount,
            byes = byeCount,
            legByes = legByeCount,
            total = totalExtrasCount
        )

        // Compile delivery items of current active over
        val ballsBowledInCurrentOver = totalLegalBalls % 6
        val completedOvers = totalLegalBalls / 6
        val currentOverMatchIndex = completedOvers

        // Deliveries in active over:
        val currentOverBalls = deliveries.filter { it.overIndex == currentOverMatchIndex }

        return InningsState(
            inningsNo = inningsNo,
            battingTeamId = battingTeamId,
            battingTeamName = battingTeamName,
            bowlingTeamId = bowlingTeamId,
            bowlingTeamName = bowlingTeamName,
            score = score,
            wickets = wickets,
            ballsBowled = totalLegalBalls,
            batters = finalBatters,
            bowlers = finalBowlers,
            partnerships = partnerships,
            fallOfWickets = fallOfWickets,
            extras = extras,
            currentOverBalls = currentOverBalls,
            activeStrikerId = strikerId,
            activeNonStrikerId = nonStrikerId,
            activeBowlerId = currentBowlerId,
            isCompleted = inningsFinished
        )
    }
}
