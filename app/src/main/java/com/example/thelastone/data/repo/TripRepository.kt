package com.example.thelastone.data.repo

import com.example.thelastone.data.model.Activity
import com.example.thelastone.data.model.Trip
import com.example.thelastone.data.model.TripForm
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    /** 建立行程（送出表單→後端 AI 產生預覽用行程，尚未入庫） */
    suspend fun createTrip(form: TripForm): Trip

    /** 確認並儲存行程（把預覽行程入庫，回傳正式 Trip） */
    suspend fun saveTrip(trip: Trip): Trip

    /** 取得某位使用者的所有行程清單 */
    suspend fun getMyTrips(userId: String): List<Trip>

    /** 取得某個行程的詳細內容 */
    suspend fun getTripDetail(tripId: String): Trip

    /** 新增行程點到某一天 */
    suspend fun addActivity(tripId: String, dayIndex: Int, activity: Activity)

    /** 修改某一天某個行程點的內容 */
    suspend fun updateActivity(tripId: String, dayIndex: Int, activityIndex: Int, updated: Activity)

    /** 刪除某一天的某個行程點 */
    suspend fun removeActivity(tripId: String, dayIndex: Int, activityIndex: Int)

    /** 刪除整個行程 */
    suspend fun deleteTrip(tripId: String)
}