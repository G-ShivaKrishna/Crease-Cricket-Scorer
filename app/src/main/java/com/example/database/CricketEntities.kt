package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val clubName: String = "",
    val avatarUrl: String = "",
    val designRole: String = "ALL_ROUNDER", // BATS_MAN, BOWLER, ALL_ROUNDER, WICKET_KEEPER
    
    // Batting Statistics
    val battingMatches: Int = 0,
    val battingInnings: Int = 0,
    val battingNotOuts: Int = 0,
    val battingRuns: Int = 0,
    val battingHighestScore: Int = 0,
    val battingBalls: Int = 0,
    val battingFours: Int = 0,
    val battingSixes: Int = 0,
    val battingFiftyCount: Int = 0,
    val battingHundredCount: Int = 0,
    
    // Bowling Statistics
    val bowlingOversBalls: Int = 0, // total balls bowled
    val bowlingMaidens: Int = 0,
    val bowlingRunsConceded: Int = 0,
    val bowlingWickets: Int = 0,
    val bowlingBestWickets: Int = 0,
    val bowlingBestRunsConceded: Int = 0,
    val bowlingWides: Int = 0,
    val bowlingNoBalls: Int = 0,
    
    // Fielding Statistics
    val fieldingCatches: Int = 0,
    val fieldingRunOuts: Int = 0,
    val fieldingStumpings: Int = 0
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val logoUrl: String = "",
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    val noResults: Int = 0,
    val totalRunsScored: Int = 0,
    val totalBallsFaced: Int = 0,
    val totalRunsConceded: Int = 0,
    val totalBallsBowled: Int = 0
) {
    val netRunRate: Double
        get() {
            val oversFaced = totalBallsFaced / 6.0
            val oversBowled = totalBallsBowled / 6.0
            val batRate = if (oversFaced > 0) totalRunsScored.toDouble() / oversFaced else 0.0
            val bowlRate = if (oversBowled > 0) totalRunsConceded.toDouble() / oversBowled else 0.0
            return batRate - bowlRate
        }
}

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamAId: Int,
    val teamBId: Int,
    val teamAName: String,
    val teamBName: String,
    val teamALogo: String = "",
    val teamBLogo: String = "",
    val venue: String,
    val scheduledDate: String,
    val scheduledTime: String,
    
    // Rules
    val oversCount: Int = 20,
    val ballsPerOver: Int = 6,
    val superOver: Boolean = false,
    val freeHitRules: Boolean = true,
    val bowlerLimit: Int = 4,
    val wideRulesRuns: Int = 1, // runs added for wide (excluding rebowl)
    val noBallRulesRuns: Int = 1,
    val retiredOutRules: Boolean = true,
    val customLocalRules: String = "",
    
    // Setup State
    val tossWinnerId: Int = 0, // teamAId or teamBId
    val tossDecision: String = "", // "BAT" or "BOWL"
    val status: String = "PRE_MATCH", // PRE_MATCH, LIVE, INNINGS_BREAK, COMPLETED, ABANDONED
    
    // Match Squads
    val teamASquadIds: String = "", // comma-separated player ids
    val teamBSquadIds: String = "", // comma-separated player ids
    
    // Live State (Can be derived or saved for quick retrieval)
    val currentInningsNo: Int = 1, // 1 or 2
    val firstInningsTeamId: Int = 0, // team that bats first
    val secondInningsTeamId: Int = 0,
    val firstInningsScore: Int = 0,
    val firstInningsWickets: Int = 0,
    val firstInningsOversBalls: Int = 0,
    val secondInningsScore: Int = 0,
    val secondInningsWickets: Int = 0,
    val secondInningsOversBalls: Int = 0,
    
    // Summary
    val winnerId: Int = 0,
    val winningMargin: String = "", // "by 5 wickets", "by 10 runs"
    val playerOfTheMatchId: Int = 0,
    
    // Offline Storage / Lock status
    val isLocked: Boolean = false,
    val syncStatus: String = "SYNCED" // SYNCED, OFFLINE_PENDING
)

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val inningsNo: Int,
    val overIndex: Int, // 0-based
    val ballIndexInOver: Int, // 1-based (legal balls)
    val batsmanId: Int,
    val batsmanName: String,
    val bowlerId: Int,
    val bowlerName: String,
    val runsSimple: Int, // runs from bat
    val extraType: String, // "NONE", "WIDE", "NO_BALL", "BYE", "LEG_BYE"
    val extraRuns: Int,  // runs from extra
    val isWicket: Boolean,
    val dismissalType: String = "NONE", // NONE, BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, RETIRED_OUT, etc.
    val dismissedBatsmanId: Int = 0,
    val dismissedBatsmanName: String = "",
    val fielderName: String = "", // Catch, Run-out, Stumping assistance
    val timestamp: Long = System.currentTimeMillis()
)
