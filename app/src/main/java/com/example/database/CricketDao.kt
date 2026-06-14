package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Int): PlayerEntity?

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun getPlayersByIds(ids: List<Int>): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)
}

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamById(id: Int): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    @Query("DELETE FROM teams")
    suspend fun deleteAllTeams()

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    fun getMatchByIdFlow(id: Int): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)
}

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries WHERE matchId = :matchId ORDER BY id ASC")
    fun getDeliveriesForMatch(matchId: Int): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE matchId = :matchId ORDER BY id ASC")
    suspend fun getDeliveriesForMatchList(matchId: Int): List<DeliveryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity): Long

    @Query("DELETE FROM deliveries WHERE id = (SELECT MAX(id) FROM deliveries WHERE matchId = :matchId)")
    suspend fun deleteLastDelivery(matchId: Int)

    @Query("DELETE FROM deliveries WHERE matchId = :matchId")
    suspend fun clearDeliveriesForMatch(matchId: Int)

    @Query("DELETE FROM deliveries")
    suspend fun deleteAllDeliveries()
}
