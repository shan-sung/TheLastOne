package com.example.thelastone.data.repo.impl.fake

// ====== FakeTripRepository（修正版，使用 java.time） ======

import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.AgeBand
import com.example.thelastone.data.model.DaySchedule
import com.example.thelastone.data.model.Place
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm
import com.example.thelastone.data.model.User
import com.example.thelastone.data.repo.TripRepository
import com.example.thelastone.di.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakeTripRepository @Inject constructor(
    private val session: SessionManager
) : TripRepository {
    private val trips = ConcurrentHashMap<String, Trip>()
    private val tripsState = MutableStateFlow<Map<String, Trip>>(emptyMap())
    init {
        seedDemoTrips()
        emitAll()
    }
    private fun emitAll() { tripsState.value = trips.toMap() }

    // 建立預覽：由目前使用者當 createdBy
    override suspend fun createTrip(form: TripForm): Trip {
        delay(600)
        val me = session.currentUserId
        val days = enumerateDates(form.startDate, form.endDate).map { d ->
            DaySchedule(d, mockActivitiesForDay(d, form.useGmapsRating, form.activityStart, form.activityEnd))
        }
        return Trip(
            id = "preview-${UUID.randomUUID()}",
            createdBy = me,
            name = form.name,
            totalBudget = form.totalBudget,
            startDate = form.startDate,
            endDate = form.endDate,
            activityStart = form.activityStart,
            activityEnd = form.activityEnd,
            avgAge = form.avgAge,
            transportPreferences = form.transportPreferences,
            useGmapsRating = form.useGmapsRating,
            styles = form.styles,
            members = emptyList(),
            days = days
        )
    }

    // 儲存：也用目前使用者
    override suspend fun saveTrip(trip: Trip): Trip {
        delay(200)
        val me = session.currentUserId
        val finalId = if (trip.id.startsWith("preview-")) "trip-${UUID.randomUUID()}" else trip.id
        val saved = trip.copy(id = finalId, createdBy = me)
        trips[finalId] = saved
        emitAll()
        return saved
    }

    override suspend fun getMyTrips(): List<Trip> {
        delay(120)
        val me = session.currentUserId
        return trips.values
            .filter { it.createdBy == me || it.members.any { m -> m.id == me } }
            .sortedBy { LocalDate.parse(it.startDate) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyTrips(): Flow<List<Trip>> =
        // 關鍵：跟隨帳號切換
        session.auth.filterNotNull().flatMapLatest { auth ->
            tripsState
                .map { map ->
                    map.values
                        .filter { it.createdBy == auth.user.id || it.members.any { m -> m.id == auth.user.id } }
                        .sortedBy { LocalDate.parse(it.startDate) }
                }
                .distinctUntilChanged()
        }

    override suspend fun getTripDetail(tripId: String): Trip {
        delay(80)
        return trips[tripId] ?: error("Trip not found: $tripId")
    }

    override fun observeTripDetail(tripId: String): Flow<Trip> =
        tripsState
            .map { it[tripId] ?: error("Trip not found: $tripId") }
            .distinctUntilChanged()

    override suspend fun addActivity(tripId: String, dayIndex: Int, activity: Activity) {
        val t = trips[tripId] ?: error("Trip not found")
        val newDays = t.days.toMutableList().apply {
            val day = this[dayIndex]
            this[dayIndex] = day.copy(activities = day.activities + activity)
        }
        trips[tripId] = t.copy(days = newDays)
        emitAll()                // ✅ 推播（改回 emitAll）
    }

    override suspend fun updateActivity(tripId: String, dayIndex: Int, activityIndex: Int, updated: Activity) {
        val t = trips[tripId] ?: error("Trip not found")
        val days = t.days.toMutableList()
        val list = days[dayIndex].activities.toMutableList()
        list[activityIndex] = updated
        days[dayIndex] = days[dayIndex].copy(activities = list)
        trips[tripId] = t.copy(days = days)
        emitAll()                // ✅ 推播
    }

    override suspend fun removeActivity(tripId: String, dayIndex: Int, activityIndex: Int) {
        val t = trips[tripId] ?: error("Trip not found")
        val days = t.days.toMutableList()
        val list = days[dayIndex].activities.toMutableList()
        list.removeAt(activityIndex)
        days[dayIndex] = days[dayIndex].copy(activities = list)
        trips[tripId] = t.copy(days = days)
        emitAll()                // ✅ 推播
    }

    override suspend fun deleteTrip(tripId: String) {
        trips.remove(tripId)
        emitAll()                // ✅ 推播
    }

    private fun seedDemoTrips() {
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun mkTrip(
            name: String,
            start: LocalDate,
            daysCount: Long,
            createdBy: String = "demo-user",
            members: List<User> = listOf(User(id = "friend-1", email = "a@b.com", name = "Alice")),
            activityStart: String? = "09:00",
            activityEnd: String? = "18:00",
            avgAge: AgeBand = AgeBand.A18_25
        ): Trip {
            val startStr = start.format(fmt)
            val endStr = start.plusDays(daysCount - 1).format(fmt)
            val dayStrings = enumerateDates(startStr, endStr)
            val daySchedules = dayStrings.map { d ->
                DaySchedule(
                    date = d,
                    activities = mockActivitiesForDay(
                        date = d,
                        preferHighRating = true,
                        start = activityStart,
                        end = activityEnd
                    )
                )
            }
            return Trip(
                id = "trip-${UUID.randomUUID()}",
                createdBy = createdBy,
                name = name,
                totalBudget = 15000,
                startDate = startStr,
                endDate = endStr,
                activityStart = activityStart,      // ✅
                activityEnd = activityEnd,          // ✅
                avgAge = avgAge,                    // ✅
                transportPreferences = listOf("Walk", "MRT"),
                useGmapsRating = true,
                styles = listOf("Foodie", "Relax"),
                members = members,
                days = daySchedules
            )
        }

        val t1 = mkTrip("台北 2 日小旅行", today.plusDays(1), 2)
        val t2 = mkTrip("台中美食行", today.plusDays(10), 3)
        val t3 = mkTrip("朋友邀請示例", today.plusDays(5), 2, createdBy = "someone-else")

        trips[t1.id] = t1
        trips[t2.id] = t2
        trips[t3.id] = t3
    }

    // ---------- helpers ----------
    private fun enumerateDates(start: String, end: String): List<String> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val s = LocalDate.parse(start, fmt)
        val e = LocalDate.parse(end, fmt)
        return generateSequence(s) { prev ->
            val next = prev.plusDays(1)
            if (next.isAfter(e)) null else next
        }.map { it.format(fmt) }.toList()
    }

    private fun mockActivitiesForDay(
        date: String,
        preferHighRating: Boolean,
        start: String? = null,
        end: String? = null
    ): List<Activity> {
        val count = Random.nextInt(2, 4)
        val tfmt = DateTimeFormatter.ofPattern("HH:mm")
        val s = start?.let { LocalTime.parse(it, tfmt) } ?: LocalTime.of(9, 0)
        val e = end?.let { LocalTime.parse(it, tfmt) } ?: LocalTime.of(18, 0)

        fun randTimeInRange(): Pair<String, String> {
            val startHour = Random.nextInt(s.hour, maxOf(s.hour + 1, e.hour))
            val st = LocalTime.of(startHour, listOf(0, 30).random())
            val en = st.plusMinutes(listOf(60L, 90L, 120L).random())
            val capped = minOf(en, LocalTime.of(23, 59))   // ✅ 修掉 coerceAtMost
            return st.format(tfmt) to capped.format(tfmt)
        }

        return (1..count).map { idx ->
            val rating = if (preferHighRating) Random.nextDouble(4.2, 4.9) else Random.nextDouble(3.5, 4.8)
            val (st, en) = randTimeInRange()
            val place = Place(
                placeId = "gplace_${date}_$idx",
                name = "Place $idx",
                rating = rating,
                userRatingsTotal = Random.nextInt(50, 2500),
                address = "Some address $idx",
                openingHours = listOf("Mon–Sun: 09:00–18:00"),
                lat = 25.0 + Random.nextDouble(-0.2, 0.2),
                lng = 121.0 + Random.nextDouble(-0.2, 0.2),
                photoUrl = null,
                miniMapUrl = null
            )
            Activity(
                id = "act_${UUID.randomUUID()}",
                place = place,
                startTime = st,
                endTime = en,
                note = null
            )
        }
    }
}