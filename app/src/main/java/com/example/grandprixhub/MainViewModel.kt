package com.example.grandprixhub

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.foundation.lazy.LazyListState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- REQUIRED ENUMS (UNCOMMENTED FOR BUILD FIX) ---
enum class AuthStatus { LoggedOut, Onboarding, LoggedIn }
//enum class TimeMode { MY_TIME, TRACK_TIME }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repository = UserRepository()

    // --- Auth State ---
    var authStatus = mutableStateOf(if (auth.currentUser != null) AuthStatus.LoggedIn else AuthStatus.LoggedOut)
    val userName = mutableStateOf(auth.currentUser?.displayName ?: "F1 Fan")
    val userEmail = mutableStateOf(auth.currentUser?.email ?: "")
    val favDriverName = mutableStateOf("None Selected")

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

    val f1News = mutableStateOf<List<EspnArticle>>(emptyList())
    val isNewsLoading = mutableStateOf(false)

    private val espnApiService = Retrofit.Builder()
        .baseUrl("https://site.api.espn.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EspnApiService::class.java)

    private var liveTimingJob: Job? = null
    val isPollingLiveTiming = mutableStateOf(false)

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
        if (auth.currentUser != null) {
            fetchUserPreferences()
        }
    }

    // --- Auth Logic ---
    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                userName.value = auth.currentUser?.displayName ?: "F1 Fan"
                userEmail.value = auth.currentUser?.email ?: ""
                authStatus.value = AuthStatus.LoggedIn
                fetchUserPreferences()
                fetchData()
            }
    }

    fun signUp(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                val profileUpdates = userProfileChangeRequest { displayName = name }

                result.user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    if (uid != null) {
                        val initialUserData = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "favDriver" to "none"
                        )

                        db.collection("users")
                            .document(uid)
                            .set(initialUserData)
                            .addOnSuccessListener {
                                userName.value = name
                                userEmail.value = email
                                authStatus.value = AuthStatus.Onboarding
                                fetchUserPreferences()
                                fetchData()
                            }
                    }
                }
            }
    }

    fun saveUserPrefs(driverId: String) {
        repository.saveUserPreferences(driverId) { success ->
            if (success) {
                val matchingDriver = drivers.value.find { it.Driver.driverId == driverId }

                if (matchingDriver != null) {
                    favDriverName.value = "${matchingDriver.Driver.givenName} ${matchingDriver.Driver.familyName}"
                } else {
                    favDriverName.value = driverId.split("_")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                }
                authStatus.value = AuthStatus.LoggedIn
            }
        }
    }

    private fun fetchUserPreferences() {
        val uid = auth.currentUser?.uid ?: return
        android.util.Log.d("F1_HUB_DEBUG", "Fetching preferences for UID: $uid")

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val favId = doc.getString("favDriver")
                    android.util.Log.d("F1_HUB_DEBUG", "Found favDriver token in DB: $favId")

                    if (favId == null || favId == "none" || favId.isEmpty()) {
                        favDriverName.value = "None Selected"
                        return@addOnSuccessListener
                    }

                    val driver = drivers.value.find { it.Driver.driverId == favId }
                    favDriverName.value = if (driver != null) {
                        "${driver.Driver.givenName} ${driver.Driver.familyName}"
                    } else {
                        favId.split("_")
                            .filter { it.isNotEmpty() }
                            .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
                    }
                } else {
                    android.util.Log.d("F1_HUB_DEBUG", "No Firestore document exists for this UID yet!")
                    favDriverName.value = "None Selected"
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("F1_HUB_DEBUG", "Firestore read failed completely", e)
                favDriverName.value = "None Selected"
            }
    }

    fun logout() {
        auth.signOut()
        authStatus.value = AuthStatus.LoggedOut
        userName.value = "F1 Fan"
        userEmail.value = ""
        favDriverName.value = "None Selected"
    }

    private fun fetchData() {
        val year = selectedYear.value
        fetchSchedule(year)
        viewModelScope.launch {
            try {
                val standings = apiService.getDriverStandings(year)
                drivers.value = standings.MRData.StandingsTable.StandingsLists.firstOrNull()?.DriverStandings ?: emptyList()

                if (auth.currentUser != null) {
                    fetchUserPreferences()
                }

                val constStandings = apiService.getConstructorStandings(year)
                constructors.value = constStandings.MRData.StandingsTable.StandingsLists.firstOrNull()?.ConstructorStandings ?: emptyList()
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

                val resultsList = try { apiService.getSeasonResults(year).MRData.RaceTable.Races } catch (e: Exception) { emptyList() }

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

    fun fetchSessionResults(year: String, round: String, type: String) {
        val currentRace = selectedRace.value ?: return
        viewModelScope.launch {
            try {
                when (type.lowercase()) {
                    "fp1" -> fetchPracticeResults(year, currentRace.Circuit.circuitId, "Practice 1")
                    "fp2" -> fetchPracticeResults(year, currentRace.Circuit.circuitId, "Practice 2")
                    "fp3" -> fetchPracticeResults(year, currentRace.Circuit.circuitId, "Practice 3")
                    "sprint_qualifying" -> {
                        try {
                            val openF1CircuitName = mapErgastToOpenF1(currentRace.Circuit.circuitId)
                            val sessions = apiService.getOpenF1Sessions(
                                year = year.toInt(),
                                circuitName = openF1CircuitName,
                                sessionName = "Sprint Qualifying"
                            )

                            val sessionKey = sessions.lastOrNull()?.sessionKey ?: run {
                                selectedSessionResults.value = emptyList()
                                selectedSessionType.value = "SPRINT QUALIFYING"
                                isShowingResults.value = true
                                return@launch
                            }

                            val allLaps = apiService.getOpenF1Laps(sessionKey = sessionKey)

                            val results = allLaps.filter { it.lapDuration != null && !it.isPitOutLap }
                                .groupBy { it.driverNumber }
                                .map { (_, laps) -> laps.minByOrNull { it.lapDuration!! }!! }
                                .sortedBy { it.lapDuration }
                                .mapIndexed { index, lap ->
                                    val driverInfo = drivers.value.find { it.Driver.permanentNumber == lap.driverNumber.toString() }?.Driver
                                    PracticeResultDisplay(
                                        position = index + 1,
                                        driverNumber = lap.driverNumber.toString(),
                                        driverName = driverInfo?.familyName ?: "Driver ${lap.driverNumber}",
                                        bestLapTime = formatLapTime(lap.lapDuration!!),
                                        gap = if (index == 0) "INTERVAL" else "+${String.format(java.util.Locale.ENGLISH, "%.3f", lap.lapDuration!! - (allLaps.filter { it.lapDuration != null }.minOfOrNull { it.lapDuration!! } ?: 0.0))}"
                                    )
                                }

                            selectedSessionResults.value = results
                        } catch (e: Exception) {
                            selectedSessionResults.value = emptyList()
                        }
                        selectedSessionType.value = "SPRINT QUALIFYING"
                        isShowingResults.value = true
                    }
                    "sprint" -> {
                        val response = apiService.getSprintResults(year, round)
                        selectedSessionResults.value = response.MRData.RaceTable.Races.firstOrNull()?.SprintResults ?: emptyList()
                        selectedSessionType.value = "SPRINT RACE"
                        isShowingResults.value = true
                    }
                    "qualifying" -> {
                        val response = apiService.getQualifyingResults(year, round)
                        selectedSessionResults.value = response.MRData.RaceTable.Races.firstOrNull()?.QualifyingResults ?: emptyList()
                        selectedSessionType.value = "QUALIFYING"
                        isShowingResults.value = true
                    }
                    "results" -> {
                        val response = apiService.getRaceResults(year, round)
                        selectedSessionResults.value = response.MRData.RaceTable.Races.firstOrNull()?.Results ?: emptyList()
                        selectedSessionType.value = "GRAND PRIX"
                        isShowingResults.value = true
                    }
                }
            } catch (e: Exception) { isShowingResults.value = false }
        }
    }

    private fun formatLapTime(totalSeconds: Double): String {
        val minutes = (totalSeconds / 60).toInt()
        val seconds = totalSeconds % 60
        return String.format("%d:%06.3f", minutes, seconds)
    }

    private fun fetchPracticeResults(year: String, circuitId: String, sessionName: String) {
        viewModelScope.launch {
            try {
                val openF1CircuitName = mapErgastToOpenF1(circuitId)
                val sessions = apiService.getOpenF1Sessions(
                    year = year.toInt(),
                    circuitName = openF1CircuitName,
                    sessionName = sessionName
                )

                val sessionKey = sessions.lastOrNull()?.sessionKey ?: run {
                    selectedSessionResults.value = emptyList()
                    selectedSessionType.value = sessionName.uppercase()
                    isShowingResults.value = true
                    return@launch
                }

                val allLaps = apiService.getOpenF1Laps(sessionKey = sessionKey)

                val results = allLaps.filter { it.lapDuration != null && !it.isPitOutLap }
                    .groupBy { it.driverNumber }
                    .map { (_, laps) -> laps.minByOrNull { it.lapDuration!! }!! }
                    .sortedBy { it.lapDuration }
                    .mapIndexed { index, lap ->
                        val driverInfo = drivers.value.find { it.Driver.permanentNumber == lap.driverNumber.toString() }?.Driver
                        PracticeResultDisplay(
                            position = index + 1,
                            driverNumber = lap.driverNumber.toString(),
                            driverName = driverInfo?.familyName ?: "Driver ${lap.driverNumber}",
                            bestLapTime = formatLapTime(lap.lapDuration!!),
                            gap = if (index == 0) "INTERVAL" else "+${String.format("%.3f", lap.lapDuration!! - (allLaps.filter { it.lapDuration != null }.minOfOrNull { it.lapDuration!! } ?: 0.0))}"
                        )
                    }

                selectedSessionResults.value = results
                selectedSessionType.value = sessionName.uppercase()
                isShowingResults.value = true
            } catch (e: Exception) {
                isShowingResults.value = false
            }
        }
    }

    fun fetchLiveHighlight(raceName: String, sessionName: String) {
        viewModelScope.launch {
            try {
                val cleanRaceName = raceName.replace("Grand Prix", "", ignoreCase = true).trim()
                val query = "\"F1 ${sessionName.uppercase()} Highlights\" \"${selectedYear.value} $cleanRaceName\""

                val response = youtubeApi.searchVideos(
                    query = query,
                    apiKey = YOUTUBE_API_KEY,
                    channelId = "UCB_qr75-ydFVKSF9Dmo6izg",
                    order = "relevance"
                )

                val item = response.items.firstOrNull()
                selectedVideoId.value = item?.id?.videoId ?: ""
                selectedThumbnailUrl.value = item?.snippet?.thumbnails?.high?.url ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchF1News() {
        // If we are already loading news, do not spin up another duplicate network request thread!
        if (isNewsLoading.value) return

        viewModelScope.launch {
            isNewsLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    espnApiService.getLatestF1News()
                }
                // Safely map the incoming data array
                f1News.value = response.articles ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("F1_HUB_NEWS_ERR", "ESPN request interrupted safely", e)
            } finally {
                // This is guaranteed to run, preventing the app from getting stuck in a loading lock state
                isNewsLoading.value = false
            }
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

    fun startLiveTiming(year: String, round: String, type: String) {
        if (liveTimingJob?.isActive == true) return

        isPollingLiveTiming.value = true
        liveTimingJob = viewModelScope.launch {
            while (isPollingLiveTiming.value) {
                fetchSessionResults(year, round, type)
                delay(12000)
            }
        }
    }

    fun stopLiveTiming() {
        isPollingLiveTiming.value = false
        liveTimingJob?.cancel()
        liveTimingJob = null
    }

    private fun calculateRadarMetrics(results: List<RaceResult>): Map<String, Float> {
        if (results.isEmpty()) return emptyMap()
        val avgGrid = results.map { it.grid.toFloat() }.average().toFloat()
        val qualyScore = (11f - avgGrid).coerceIn(1f, 10f)
        val classified = results.filter { it.status == "Finished" || it.status.contains("Lap") }
        val avgGained = if (classified.isNotEmpty()) classified.map { it.grid.toInt() - it.position.toInt() }.average().toFloat() else 0f
        val craftScore = (5f + avgGained).coerceIn(1f, 10f)
        val peakScore = ((results.count { it.position == "1" } * 3f) + results.count { it.position.toInt() in 1..3 }).coerceIn(1f, 10f)
        return mapOf("Qualy Pace" to qualyScore, "Race Craft" to craftScore, "Peak Performance" to peakScore)
    }

    private fun mapErgastToOpenF1(ergastId: String): String = when (ergastId) {
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
        else -> ergastId.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveTiming()
    }
}