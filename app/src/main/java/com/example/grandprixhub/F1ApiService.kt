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