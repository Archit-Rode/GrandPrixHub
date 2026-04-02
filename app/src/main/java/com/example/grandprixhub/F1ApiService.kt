package com.example.grandprixhub

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

interface F1ApiService {
    // --- ERGAST / JOLPICA ENDPOINTS (Existing) ---
    @GET("{year}/driverStandings.json")
    suspend fun getDriverStandings(@Path("year") year: String): StandingsResponse

    @GET("{year}/constructorStandings.json")
    suspend fun getConstructorStandings(@Path("year") year: String): StandingsResponse

    @GET("{year}.json")
    suspend fun getSeasonSchedule(@Path("year") year: String): ScheduleResponse

    @GET("{year}/results/1.json")
    suspend fun getSeasonResults(@Path("year") year: String): ScheduleResponse

    @GET("drivers/{driverId}/results.json?limit=1000")
    suspend fun getDriverCareerResults(@Path("driverId") driverId: String): ScheduleResponse

    @GET("{year}/results.json?limit=1000")
    suspend fun getFullSeasonResults(@Path("year") year: String): ScheduleResponse

    @GET("{year}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("year") year: String,
        @Path("round") round: String
    ): ScheduleResponse

    @GET("{year}/{round}/qualifying.json")
    suspend fun getQualifyingResults(
        @Path("year") year: String,
        @Path("round") round: String
    ): QualifyingResponse

    @GET("{year}/{round}/sprint.json")
    suspend fun getSprintResults(
        @Path("year") year: String,
        @Path("round") round: String
    ): SprintResponse

    // --- OPEN F1 ENDPOINTS (New for Practice Results) ---

    @GET
    suspend fun getOpenF1Sessions(
        @Url url: String = "https://api.openf1.org/v1/sessions",
        @Query("year") year: Int,
        @Query("circuit_short_name") circuitName: String,
        @Query("session_name") sessionName: String // e.g., "Practice 1"
    ): List<OpenF1Session>

    @GET
    suspend fun getOpenF1Laps(
        @Url url: String = "https://api.openf1.org/v1/laps",
        @Query("session_key") sessionKey: Int
    ): List<OpenF1Lap>

    @GET
    suspend fun getSessionWeather(
        @Url url: String // Works for OpenF1 Weather endpoint
    ): List<APIWeather>
}

interface YouTubeApiService {
    @GET("https://www.googleapis.com/youtube/v3/search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("channelId") channelId: String = "UCB_qr75-ydFVKSF9Dmo6izg",
        @Query("maxResults") maxResults: Int = 1,
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("key") apiKey: String
    ): YouTubeSearchResponse
}

// --- YOUTUBE MODELS ---
data class YouTubeSearchResponse(val items: List<YouTubeVideoItem>)
data class YouTubeVideoItem(val id: YouTubeVideoId, val snippet: YouTubeSnippet)
data class YouTubeSnippet(val title: String, val thumbnails: YouTubeThumbnails)
data class YouTubeThumbnails(val high: YouTubeThumbnailDetails)
data class YouTubeThumbnailDetails(val url: String)
data class YouTubeVideoId(val videoId: String)

// --- ERGAST SCHEDULE & RESULTS MODELS ---
data class ScheduleResponse(val MRData: MRDataSchedule)
data class MRDataSchedule(val RaceTable: RaceTable)
data class RaceTable(val Races: List<APIRace>)

data class APIRace(
    val round: String,
    val raceName: String,
    val Circuit: Circuit,
    val date: String,
    val time: String? = null,
    val FirstPractice: Session? = null,
    val SecondPractice: Session? = null,
    val ThirdPractice: Session? = null,
    val Qualifying: Session? = null,
    @SerializedName("SprintQualifying")
    val SprintShootout: Session?,
    val Sprint: Session? = null,
    val Results: List<RaceResult>? = null
)

data class Session(val date: String, val time: String)

data class RaceResult(
    val position: String,
    val points: String,
    val Driver: Driver,
    val Constructor: Constructor,
    val grid: String,
    val status: String
)

data class Circuit(
    val circuitId: String,
    val circuitName: String,
    val Location: Location
)

data class Location(val locality: String, val country: String)

// --- STANDINGS MODELS ---
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
    val wins: String,
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

// --- QUALIFYING & SPRINT MODELS ---
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

data class SprintResponse(val MRData: MRDataSprint)
data class MRDataSprint(val RaceTable: SprintRaceTable)
data class SprintRaceTable(val Races: List<SprintRace>)
data class SprintRace(
    val raceName: String,
    val SprintResults: List<RaceResult>
)

// --- OPEN F1 MODELS (New) ---
data class OpenF1Session(
    @SerializedName("session_key") val sessionKey: Int,
    @SerializedName("session_name") val sessionName: String,
    @SerializedName("circuit_short_name") val circuitName: String,
    val year: Int
)

data class OpenF1Lap(
    @SerializedName("driver_number") val driverNumber: Int,
    @SerializedName("lap_duration") val lapDuration: Double?,
    @SerializedName("is_pit_out_lap") val isPitOutLap: Boolean
)

data class APIWeather(
    val air_temperature: Double,
    val track_temperature: Double,
    val humidity: Double,
    val rainfall: Int,
    val wind_speed: Double
)

// UI Model for Practice Results
data class PracticeResultDisplay(
    val position: Int,
    val driverNumber: String,
    val driverName: String, // We will map driver number to name in ViewModel
    val bestLapTime: String,
    val gap: String
)