package com.example.grandprixhub

import retrofit2.http.GET
import retrofit2.http.Path

interface F1ApiService {
    // EXISTING: Standings
    @GET("{year}/driverStandings.json")
    suspend fun getDriverStandings(@Path("year") year: String): StandingsResponse

    @GET("{year}/constructorStandings.json")
    suspend fun getConstructorStandings(@Path("year") year: String): StandingsResponse

    // NEW: Real-time Schedule
    @GET("{year}.json")
    suspend fun getSeasonSchedule(@Path("year") year: String): ScheduleResponse
}

// --- Schedule Data Classes ---
data class ScheduleResponse(val MRData: MRDataSchedule)
data class MRDataSchedule(val RaceTable: RaceTable)
data class RaceTable(val Races: List<APIRace>)

data class APIRace(
    val round: String,
    val raceName: String,
    val Circuit: Circuit,
    val date: String,
    val time: String? = null // Optional time field
)

data class Circuit(
    val circuitId: String, // ADDED: Required for lookup in CircuitRepository
    val circuitName: String,
    val Location: Location
)

data class Location(
    val locality: String,
    val country: String
)

// --- Standings Data Classes (Unchanged) ---
data class StandingsResponse(val MRData: MRData)
data class MRData(val StandingsTable: StandingsTable)
data class StandingsTable(val StandingsLists: List<StandingsList>)

data class StandingsList(
    val DriverStandings: List<DriverStanding>,
    val ConstructorStandings: List<ConstructorStanding>
)

data class DriverStanding(
    val position: String,
    val points: String,
    val Driver: Driver,
    val Constructors: List<Constructor>
)

data class ConstructorStanding(
    val position: String,
    val points: String,
    val Constructor: Constructor
)

data class Driver(
    val driverId: String,
    val permanentNumber: String,
    val givenName: String,
    val familyName: String,
    val nationality: String
)

data class Constructor(
    val constructorId: String,
    val name: String,
    val nationality: String
)