package com.example.grandprixhub

import retrofit2.http.GET
import retrofit2.http.Path

interface F1ApiService {
    // UPDATED: Now uses {year} as a dynamic path parameter
    @GET("{year}/driverStandings.json")
    suspend fun getDriverStandings(
        @Path("year") year: String
    ): StandingsResponse

    // UPDATED: Now uses {year} as a dynamic path parameter
    @GET("{year}/constructorStandings.json")
    suspend fun getConstructorStandings(
        @Path("year") year: String
    ): StandingsResponse
}

// --- Data Structures (Unchanged) ---
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
    val Constructor: Constructor)

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