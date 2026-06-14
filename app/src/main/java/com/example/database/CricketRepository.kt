package com.example.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CricketRepository(private val db: CricketDatabase) {

    val allPlayers: Flow<List<PlayerEntity>> = db.playerDao.getAllPlayers()
    val allTeams: Flow<List<TeamEntity>> = db.teamDao.getAllTeams()
    val allMatches: Flow<List<MatchEntity>> = db.matchDao.getAllMatches()

    suspend fun getPlayerById(id: Int) = db.playerDao.getPlayerById(id)
    suspend fun getPlayersByIds(ids: List<Int>) = db.playerDao.getPlayersByIds(ids)
    suspend fun insertPlayer(player: PlayerEntity) = db.playerDao.insertPlayer(player)
    suspend fun updatePlayer(player: PlayerEntity) = db.playerDao.updatePlayer(player)
    suspend fun deletePlayer(player: PlayerEntity) = db.playerDao.deletePlayer(player)

    suspend fun getTeamById(id: Int) = db.teamDao.getTeamById(id)
    suspend fun insertTeam(team: TeamEntity) = db.teamDao.insertTeam(team)
    suspend fun updateTeam(team: TeamEntity) = db.teamDao.updateTeam(team)
    suspend fun deleteTeam(team: TeamEntity) = db.teamDao.deleteTeam(team)

    fun getMatchByIdFlow(id: Int) = db.matchDao.getMatchByIdFlow(id)
    suspend fun getMatchById(id: Int) = db.matchDao.getMatchById(id)
    suspend fun insertMatch(match: MatchEntity) = db.matchDao.insertMatch(match)
    suspend fun updateMatch(match: MatchEntity) = db.matchDao.updateMatch(match)
    suspend fun deleteMatch(match: MatchEntity) = db.matchDao.deleteMatch(match)

    fun getDeliveriesForMatch(matchId: Int) = db.deliveryDao.getDeliveriesForMatch(matchId)
    suspend fun getDeliveriesForMatchList(matchId: Int) = db.deliveryDao.getDeliveriesForMatchList(matchId)
    suspend fun insertDelivery(delivery: DeliveryEntity) = db.deliveryDao.insertDelivery(delivery)
    suspend fun deleteLastDelivery(matchId: Int) = db.deliveryDao.deleteLastDelivery(matchId)
    suspend fun clearDeliveriesForMatch(matchId: Int) = db.deliveryDao.clearDeliveriesForMatch(matchId)

    suspend fun clearAllData() {
        db.playerDao.deleteAllPlayers()
        db.teamDao.deleteAllTeams()
        db.matchDao.deleteAllMatches()
        db.deliveryDao.deleteAllDeliveries()
    }

    // Prepopulate Data if Database is Empty
    suspend fun prepopulateIfEmpty() {
        // Clear tables at launch to guarantee that all preexisting predefined data is cleanly wiped
        clearAllData()
    }
}
