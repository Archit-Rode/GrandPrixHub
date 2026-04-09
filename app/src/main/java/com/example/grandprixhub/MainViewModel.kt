package com.example.grandprixhub

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyListState

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // --- UI State ---
    val isDriversTab = mutableStateOf(true)
    val selectedYear = mutableStateOf(LocalDate.now().year.toString())
    val drivers = mutableStateOf<List<DriverStanding>>(emptyList())
    val constructors = mutableStateOf<List<ConstructorStanding>>(emptyList())
    val schedule = mutableStateOf<List<APIRace>>(emptyList())
    val selectedRace = mutableStateOf<APIRace?>(null)
    val countdownText = mutableStateOf("")
    val scheduleListState = LazyListState()

    var selectedDriver1 by mutableStateOf<DriverStanding?>(null)
    var selectedDriver2 by mutableStateOf<DriverStanding?>(null)
    var timeMode by mutableStateOf(TimeMode.MY_TIME)
    var driver1DNA = mutableStateOf<Map<String, Float>>(emptyMap())
    var driver2DNA = mutableStateOf<Map<String, Float>>(emptyMap())

    val currentWeather = mutableStateOf<APIWeather?>(null)
    val selectedSessionResults = mutableStateOf<List<Any>>(emptyList())
    val selectedSessionType = mutableStateOf("")
    val isShowingResults = mutableStateOf(false)
    val selectedThumbnailUrl = mutableStateOf("")
    val selectedVideoId = mutableStateOf("")

    // --- Retrofit Setup ---
    private val YOUTUBE_API_KEY = BuildConfig.YOUTUBE_API_KEY

    private val youtubeApi = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YouTubeApiService::class.java)

    private val apiService = Retrofit.Builder()
        .baseUrl("https://api.jolpi.ca/ergast/f1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(F1ApiService::class.java)

    init {
        fetchData()
    }

    // --- NEW: PRACTICE RESULTS LOGIC (OpenF1) ---

    private fun fetchPracticeResults(year: String, circuitId: String, sessionName: String) {
        viewModelScope.launch {
            try {
                // 1. Map Ergast Circuit ID to OpenF1 Short Name
                val openF1CircuitName = mapErgastToOpenF1(circuitId)

                // 2. Find the Session Key
                val sessions = apiService.getOpenF1Sessions(
                    year = year.toInt(),
                    circuitName = openF1CircuitName,
                    sessionName = sessionName
                )
                val sessionKey = sessions.lastOrNull()?.sessionKey ?: return@launch

                // 3. Get all Laps for that session
                val allLaps = apiService.getOpenF1Laps(sessionKey = sessionKey)

                // 4. Process: Find best lap for each driver
                val results = allLaps
                    .filter { it.lapDuration != null && !it.isPitOutLap }
                    .groupBy { it.driverNumber }
                    .map { (driverNum, laps) ->
                        val bestLap = laps.minByOrNull { it.lapDuration!! }!!
                        bestLap
                    }
                    .sortedBy { it.lapDuration }
                    .mapIndexed { index, lap ->
                        val driverInfo = drivers.value.find { it.Driver.permanentNumber == lap.driverNumber.toString() }?.Driver
                        val driverName = if (driverInfo != null) "${driverInfo.givenName} ${driverInfo.familyName}" else "Driver ${lap.driverNumber}"

                        val gap = if (index == 0) "INTERVAL"
                        else "+${String.format("%.3f", lap.lapDuration!! - (allLaps.filter { it.lapDuration != null }.minByOrNull { it.lapDuration!! }?.lapDuration ?: 0.0))}"

                        PracticeResultDisplay(
                            position = index + 1,
                            driverNumber = lap.driverNumber.toString(),
                            driverName = driverName,
                            bestLapTime = formatLapTime(lap.lapDuration!!),
                            gap = gap
                        )
                    }

                selectedSessionResults.value = results
                selectedSessionType.value = sessionName.uppercase()
                isShowingResults.value = results.isNotEmpty()

            } catch (e: Exception) {
                e.printStackTrace()
                isShowingResults.value = false
            }
        }
    }

    private fun formatLapTime(totalSeconds: Double): String {
        val minutes = (totalSeconds / 60).toInt()
        val seconds = totalSeconds % 60
        return String.format("%d:%06.3f", minutes, seconds)
    }

    private fun mapErgastToOpenF1(ergastId: String): String {
        return when (ergastId) {
            "albert_park" -> "Melbourne"
            "bahrain" -> "Sakhir"
            "jeddah" -> "Jeddah"
            "suzuka" -> "Suzuka"
            "shanghai" -> "Shanghai"
            "miami" -> "Miami"
            "imola" -> "Imola"
            "monaco" -> "Monaco"
            "villeneuve" -> "Montreal"
            "catalunya" -> "Barcelona"
            "red_bull_ring" -> "Spielberg"
            "silverstone" -> "Silverstone"
            "hungaroring" -> "Budapest"
            "spa" -> "Spa-Francorchamps"
            "zandvoort" -> "Zandvoort"
            "monza" -> "Monza"
            "baku" -> "Baku"
            "marina_bay" -> "Singapore"
            "americas" -> "Austin"
            "rodriguez" -> "Mexico City"
            "interlagos" -> "São Paulo"
            "vegas" -> "Las Vegas"
            "losail" -> "Lusail"
            "yas_marina" -> "Abu Dhabi"
            else -> ergastId.replace("_", " ").capitalize()
        }
    }

    // --- UPDATED: Main Session Results Dispatcher ---

    fun fetchSessionResults(year: String, round: String, type: String) {
        val currentRace = selectedRace.value ?: return

        viewModelScope.launch {
            try {
                when (type.lowercase()) {
                    "fp1" -> fetchPracticeResults(year, currentRace.Circuit.circuitId, "Practice 1")
                    "fp2" -> fetchPracticeResults(year, currentRace.Circuit.circuitId, "Practice 2")
                    "fp3" -> fetchPracticeResults(year, currentRace.Circuit.circuitId, "Practice 3")
                    "qualifying" -> {
                        val response = apiService.getQualifyingResults(year, round)
                        val race = response.MRData.RaceTable.Races.firstOrNull()
                        selectedSessionResults.value = race?.QualifyingResults ?: emptyList()
                        selectedSessionType.value = "QUALIFYING"
                        isShowingResults.value = selectedSessionResults.value.isNotEmpty()
                    }
                    "sprint" -> {
                        val response = apiService.getSprintResults(year, round)
                        val race = response.MRData.RaceTable.Races.firstOrNull()
                        selectedSessionResults.value = race?.SprintResults ?: emptyList()
                        selectedSessionType.value = "SPRINT RACE"
                        isShowingResults.value = selectedSessionResults.value.isNotEmpty()
                    }
                    "results" -> {
                        val response = apiService.getRaceResults(year, round)
                        val race = response.MRData.RaceTable.Races.firstOrNull()
                        selectedSessionResults.value = race?.Results ?: emptyList()
                        selectedSessionType.value = "GRAND PRIX"
                        isShowingResults.value = selectedSessionResults.value.isNotEmpty()
                    }
                }
            } catch (e: Exception) {
                selectedSessionResults.value = emptyList()
                isShowingResults.value = false
            }
        }
    }

    // --- EXISTING FUNCTIONS (Unchanged) ---

    fun fetchLiveHighlight(raceName: String, sessionName: String) {
        viewModelScope.launch {
            try {
                // 1. Normalize to Uppercase so "Practice 1" matches "PRACTICE 1"
                val session = sessionName.uppercase()

                val highlightKeyword = when {
                    session.contains("QUALIFYING") -> "Qualifying Highlights"
                    session.contains("SPRINT") -> "Sprint Highlights"
                    session.contains("PRACTICE 1") || session.contains("FP1") -> "FP1 Highlights"
                    session.contains("PRACTICE 2") || session.contains("FP2") -> "FP2 Highlights"
                    session.contains("PRACTICE 3") || session.contains("FP3") -> "FP3 Highlights"
                    else -> "Race Highlights"
                }

                // 2. Refine the query to be exactly what F1 uses
                val query = "$highlightKeyword | ${selectedYear.value} $raceName"

                val response = youtubeApi.searchVideos(
                    query = query,
                    apiKey = YOUTUBE_API_KEY,
                    // Explicitly tell YouTube to ONLY look at the official F1 channel
                    channelId = "UCB_qr75-ydFVKSF9Dmo6izg",
                    order = "relevance"
                )

                val item = response.items.firstOrNull()
                selectedVideoId.value = item?.id?.videoId ?: ""
                selectedThumbnailUrl.value = item?.snippet?.thumbnails?.high?.url ?: ""
            } catch (e: Exception) {
                selectedVideoId.value = ""
                selectedThumbnailUrl.value = ""
            }
        }
    }

    private fun fetchData() {
        val year = selectedYear.value
        fetchSchedule(year)
        viewModelScope.launch {
            try {
                val driverResponse = apiService.getDriverStandings(year)
                drivers.value = driverResponse.MRData.StandingsTable.StandingsLists.firstOrNull()?.DriverStandings?.filterNotNull() ?: emptyList()
                val constructorResponse = apiService.getConstructorStandings(year)
                constructors.value = constructorResponse.MRData.StandingsTable.StandingsLists.firstOrNull()?.ConstructorStandings?.filterNotNull() ?: emptyList()
            } catch (e: Exception) {
                drivers.value = emptyList()
                constructors.value = emptyList()
            }
        }
    }

    private fun fetchSchedule(year: String) {
        viewModelScope.launch {
            try {
                val scheduleResponse = apiService.getSeasonSchedule(year)
                val fullCalendar = scheduleResponse.MRData.RaceTable.Races
                val resultsList = try { apiService.getFullSeasonResults(year).MRData.RaceTable.Races } catch (e: Exception) { emptyList() }
                val mergedList = fullCalendar.map { calendarRace ->
                    val matchingResult = resultsList.find { it.round == calendarRace.round }
                    if (matchingResult != null) calendarRace.copy(Results = matchingResult.Results) else calendarRace
                }
                schedule.value = mergedList
                updateCountdown()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updateYear(newYear: String) {
        selectedYear.value = newYear
        fetchData()
    }

    fun selectRace(race: APIRace) { selectedRace.value = race }
    fun clearSelectedRace() { selectedRace.value = null }
    fun toggleTimeMode() { timeMode = if (timeMode == TimeMode.MY_TIME) TimeMode.TRACK_TIME else TimeMode.MY_TIME }

    fun selectDriverForComparison(driver: DriverStanding) {
        when {
            selectedDriver1 == null -> selectedDriver1 = driver
            selectedDriver2 == null && driver != selectedDriver1 -> selectedDriver2 = driver
            driver == selectedDriver1 -> selectedDriver1 = null
            driver == selectedDriver2 -> selectedDriver2 = null
            else -> { selectedDriver1 = driver; selectedDriver2 = null }
        }
    }

    fun clearComparison() {
        selectedDriver1 = null
        selectedDriver2 = null
    }

    fun updateCountdown() {
        if (schedule.value.isEmpty()) return
        try {
            val now = LocalDateTime.now()
            val nextRace = schedule.value.firstOrNull { race ->
                val raceDateTime = LocalDate.parse(race.date).atTime(LocalTime.parse(race.time?.replace("Z", "") ?: "15:00:00"))
                raceDateTime.isAfter(now)
            }
            if (nextRace != null) {
                val raceDateTime = LocalDate.parse(nextRace.date).atTime(LocalTime.parse(nextRace.time?.replace("Z", "") ?: "15:00:00"))
                val diff = Duration.between(now, raceDateTime)
                countdownText.value = "${nextRace.raceName.uppercase()}: ${diff.toDays()}D ${diff.toHours() % 24}H ${diff.toMinutes() % 60}M"
            } else {
                countdownText.value = "SEASON COMPLETED"
            }
        } catch (e: Exception) { countdownText.value = "TIMER UNAVAILABLE" }
    }

    fun getDriversForTeam(constructorId: String): List<DriverStanding> {
        return drivers.value.filter { it.Constructors.lastOrNull()?.constructorId == constructorId }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            try {
                val openF1Url = "https://api.openf1.org/v1/weather?session_key=latest"
                val response = apiService.getSessionWeather(openF1Url)
                currentWeather.value = response.lastOrNull()
            } catch (e: Exception) { currentWeather.value = null }
        }
    }

    fun loadDriverStats(driverId: String, isDriverOne: Boolean) {
        viewModelScope.launch {
            try {
                val response = apiService.getDriverCareerResults(driverId)
                val results = response.MRData.RaceTable.Races.flatMap { it.Results ?: emptyList() }
                val scores = calculateRadarMetrics(results)
                if (isDriverOne) driver1DNA.value = scores else driver2DNA.value = scores
            } catch (e: Exception) { }
        }
    }

    private fun calculateRadarMetrics(results: List<RaceResult>): Map<String, Float> {
        if (results.isEmpty()) return emptyMap()
        val avgGrid = results.map { it.grid.toFloat() }.average().toFloat()
        val qualyScore = (11f - avgGrid).coerceIn(1f, 10f)
        val classified = results.filter { it.status == "Finished" || it.status.contains("Lap") }
        val avgGained = if (classified.isNotEmpty()) {
            classified.map { it.grid.toInt() - it.position.toInt() }.average().toFloat()
        } else 0f
        val craftScore = (5f + avgGained).coerceIn(1f, 10f)
        val wins = results.count { it.position == "1" }
        val podiums = results.count { it.position.toInt() in 1..3 }
        val peakScore = ((wins * 3f) + podiums).coerceIn(1f, 10f)
        return mapOf("Qualy Pace" to qualyScore, "Race Craft" to craftScore, "Peak Performance" to peakScore)
    }
}