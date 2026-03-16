package com.example.grandprixhub

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url
import com.google.gson.annotations.SerializedName
interface F1ApiService {
    // EXISTING: Standings
    @GET("{year}/driverStandings.json")
    suspend fun getDriverStandings(@Path("year") year: String): StandingsResponse

    @GET("{year}/constructorStandings.json")
    suspend fun getConstructorStandings(@Path("year") year: String): StandingsResponse

    // NEW: Real-time Schedule
    @GET("{year}.json")
    suspend fun getSeasonSchedule(@Path("year") year: String): ScheduleResponse
    @GET("{year}/results/1.json")
    suspend fun getSeasonResults(@Path("year") year: String): ScheduleResponse
    @GET("drivers/{driverId}/results.json?limit=1000")
    suspend fun getDriverCareerResults(@Path("driverId") driverId: String): ScheduleResponse
    @GET("{year}/results.json?limit=1000")
    suspend fun getFullSeasonResults(@Path("year") year: String): ScheduleResponse
    @GET
    suspend fun getSessionWeather(
        @Url url: String // This will take the full OpenF1 link
    ): List<APIWeather>
    @GET("{year}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("year") year: String,
        @Path("round") round: String
    ): ScheduleResponse

    // Fetch Qualifying standings (Q1, Q2, Q3 times)
    @GET("{year}/{round}/qualifying.json")
    suspend fun getQualifyingResults(
        @Path("year") year: String,
        @Path("round") round: String
    ): QualifyingResponse

    // Fetch Sprint results
    @GET("{year}/{round}/sprint.json")
    suspend fun getSprintResults(
        @Path("year") year: String,
        @Path("round") round: String
    ): SprintResponse
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
    val time: String? = null, // Optional time field
    val FirstPractice: Session? = null,
    val SecondPractice: Session? = null,
    val ThirdPractice: Session? = null,
    val Qualifying: Session? = null,
    @SerializedName("SprintQualifying") // This tells Retrofit to map "SprintShootout" to this variable
    val SprintShootout: Session?,
    val Sprint: Session? = null,
    val Results: List<RaceResult>? = null, // NEW: Added to hold winner info
)
data class Session(
    val date: String,
    val time: String
)
data class RaceResult(
    val position: String,
    val points: String,
    val Driver: Driver,
    val Constructor: Constructor,
    val grid: String,
    val status: String // (e.g., "Finished", "Engine", "+1 Lap")
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
    val wins: String, // ADD THIS LINE
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

data class APIWeather(
    val air_temperature: Double,
    val track_temperature: Double,
    val humidity: Double,
    val rainfall: Int, // 0 for dry, 1 for wet
    val wind_speed: Double
)

// Qualifying-specific response
data class QualifyingResponse(val MRData: MRDataQualifying)
data class MRDataQualifying(val RaceTable: QualifyingRaceTable)
data class QualifyingRaceTable(val Races: List<QualifyingRace>)
data class QualifyingRace(
    val raceName: String,
    val QualifyingResults: List<QualifyingResult>
)

data class QualifyingResult(
    val position: String,
    val number: String,
    val Driver: Driver,
    val Constructor: Constructor,
    val Q1: String?,
    val Q2: String?,
    val Q3: String?
)

// Sprint-specific response
data class SprintResponse(val MRData: MRDataSprint)
data class MRDataSprint(val RaceTable: SprintRaceTable)
data class SprintRaceTable(val Races: List<SprintRace>)
data class SprintRace(
    val raceName: String,
    val SprintResults: List<RaceResult> // Sprint uses the same format as RaceResult
)