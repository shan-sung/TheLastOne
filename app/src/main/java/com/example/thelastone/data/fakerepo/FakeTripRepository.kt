package com.example.thelastone.data.fakerepo

// ====== FakeTripRepository（修正版，使用 java.time） ======

import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.DaySchedule
import com.example.thelastone.data.model.Place
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm
import com.example.thelastone.data.model.User
import com.example.thelastone.data.repo.TripRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class FakeTripRepository : TripRepository {

    private val trips = ConcurrentHashMap<String, Trip>()
    private val _allTripsFlow = MutableStateFlow<List<Trip>>(emptyList())

    // 共用日期格式 & 排序 helper
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private fun Trip.startLocalDate(): LocalDate = LocalDate.parse(startDate, dateFmt)

    init { seedDemoTrips(); emitAll() }

    private fun emitAll() {
        // 用 LocalDate 排序，避免字串排序未來改格式造成抖動
        _allTripsFlow.value = trips.values.sortedBy { it.startLocalDate() }
    }


    private fun seedDemoTrips() {
        val today = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun trip(
            name: String,
            start: LocalDate,
            daysCount: Long,
            createdBy: String = "demo-user", // 跟你的畫面 userId 對齊
            members: List<User> = listOf(User(id = "friend-1", email = "a@b.com", name = "Alice"))
        ): Trip {
            val startStr = start.format(fmt)
            val endStr = start.plusDays(daysCount - 1).format(fmt)
            val dayStrings = enumerateDates(startStr, endStr)
            val daySchedules = dayStrings.map { d ->
                DaySchedule(
                    date = d,
                    activities = mockActivitiesForDay(d, preferHighRating = true)
                )
            }
            return Trip(
                id = "trip-${UUID.randomUUID()}",
                createdBy = createdBy,
                name = name,
                totalBudget = 15000, // 單位：元
                startDate = startStr,
                endDate = endStr,
                transportPreferences = listOf("Walk", "MRT"),
                useGmapsRating = true,
                styles = listOf("Foodie", "Relax"),
                members = members,
                days = daySchedules
            )
        }

        val t1 = trip("台北 2 日小旅行", today.plusDays(1), 2)
        val t2 = trip("台中美食行", today.plusDays(10), 3)
        val t3 = trip("朋友邀請示例", today.plusDays(5), 2, createdBy = "someone-else")

        trips[t1.id] = t1
        trips[t2.id] = t2
        trips[t3.id] = t3
    }


    // === 建立預覽 Trip：模擬後端 AI 產生 ===
    override suspend fun createTrip(form: TripForm): Trip {
        delay(600)

        val days = enumerateDates(form.startDate, form.endDate).map { date ->
            val activities = mockActivitiesForDay(date, form.useGmapsRating)
            DaySchedule(date = date, activities = activities)
        }

        return Trip(
            id = "preview-${UUID.randomUUID()}",
            createdBy = "demo-user",
            name = form.name,
            totalBudget = form.totalBudget,                  // 單位：元（Int）
            startDate = form.startDate,
            endDate = form.endDate,
            transportPreferences = form.transportPreferences,
            useGmapsRating = form.useGmapsRating,
            styles = form.styles,
            members = emptyList(),
            days = days
        )
    }

    // === 確認儲存：把預覽 Trip 入庫並給正式 id ===
    override suspend fun saveTrip(trip: Trip): Trip {
        delay(200)
        val finalId = if (trip.id.startsWith("preview-")) "trip-${UUID.randomUUID()}" else trip.id
        val saved = trip.copy(id = finalId)
        trips[finalId] = saved
        emitAll()                            // ★ 通知觀察者
        return saved
    }

    override suspend fun getMyTrips(userId: String): List<Trip> {
        delay(120)
        return trips.values
            .filter { t -> t.createdBy == userId || t.members.any { m -> m.id == userId } }
            .sortedBy { it.startLocalDate() }
    }

    override fun observeMyTrips(userId: String): Flow<List<Trip>> {
        return _allTripsFlow
            .map { all ->
                all.filter { t -> t.createdBy == userId || t.members.any { m -> m.id == userId } }
                    .sortedBy { it.startLocalDate() }
            }
            .distinctUntilChanged()
    }

    override suspend fun getTripDetail(tripId: String): Trip {
        delay(80)
        return trips[tripId] ?: error("Trip not found: $tripId")
    }

    override suspend fun addActivity(tripId: String, dayIndex: Int, activity: Activity) {
        val t = trips[tripId] ?: error("Trip not found")
        val days = t.days.toMutableList()
        val day = days[dayIndex]
        days[dayIndex] = day.copy(activities = day.activities + activity)
        trips[tripId] = t.copy(days = days)
        emitAll()
    }

    override suspend fun updateActivity(tripId: String, dayIndex: Int, activityIndex: Int, updated: Activity) {
        val t = trips[tripId] ?: error("Trip not found")
        val days = t.days.toMutableList()
        val list = days[dayIndex].activities.toMutableList()
        list[activityIndex] = updated
        days[dayIndex] = days[dayIndex].copy(activities = list)
        trips[tripId] = t.copy(days = days)
        emitAll()
    }

    override suspend fun removeActivity(tripId: String, dayIndex: Int, activityIndex: Int) {
        val t = trips[tripId] ?: error("Trip not found")
        val days = t.days.toMutableList()
        val list = days[dayIndex].activities.toMutableList()
        list.removeAt(activityIndex)
        days[dayIndex] = days[dayIndex].copy(activities = list)
        trips[tripId] = t.copy(days = days)
        emitAll()
    }

    override suspend fun deleteTrip(tripId: String) {
        trips.remove(tripId)
        emitAll()
    }

    // ---------- helpers (mock AI / Google Places) ----------
    private fun enumerateDates(start: String, end: String): List<String> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val s = LocalDate.parse(start, fmt)
        val e = LocalDate.parse(end, fmt)
        return generateSequence(s) { prev ->
            val next = prev.plusDays(1)
            if (next.isAfter(e)) null else next
        }.map { it.format(fmt) }.toList()
    }

    private fun mockActivitiesForDay(date: String, preferHighRating: Boolean): List<Activity> {
        val count = Random.nextInt(2, 4)
        return (1..count).map { idx ->
            val rating = if (preferHighRating) Random.nextDouble(4.2, 4.9) else Random.nextDouble(3.5, 4.8)
            val place = Place(
                placeId = "gplace_${date}_$idx",
                name = "Place $idx on $date",
                rating = rating,                                // Double?
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
                startTime = listOf("09:00", "10:00", "13:30").random(),
                endTime   = listOf("11:00", "12:00", "15:00").random(),
                note = null
            )
        }
    }
}
