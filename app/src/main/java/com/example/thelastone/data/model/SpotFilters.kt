// data/model/SpotFilters.kt
package com.example.thelastone.data.model

data class SpotFilters(
    val cities: Set<City> = emptySet(),
    val categories: Set<Category> = emptySet(),
    val minRating: Float = 0f,
    val maxRating: Float = 5f,
    val openNow: Boolean? = null
)

enum class City(
    val label: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int
) {
    // 北北基桃
    TAIPEI("台北", 25.0330, 121.5654, 25_000),
    NEW_TAIPEI("新北", 25.0169, 121.4628, 35_000),
    KEELUNG("基隆", 25.1283, 121.7419, 15_000),
    TAOYUAN("桃園", 24.9937, 121.3010, 30_000),

    // 竹苗
    HSINCHU_CITY("新竹市", 24.8039, 120.9647, 18_000),
    HSINCHU_COUNTY("新竹縣", 24.8380, 121.0070, 30_000),
    MIAOLI("苗栗", 24.5602, 120.8210, 30_000),

    // 中彰投
    TAICHUNG("台中", 24.1477, 120.6736, 30_000),
    CHANGHUA("彰化", 24.0722, 120.5410, 25_000),
    NANTOU("南投", 23.9609, 120.9719, 35_000),

    // 雲嘉
    YUNLIN("雲林", 23.7074, 120.5430, 30_000),       // 斗六
    CHIAYI_CITY("嘉義市", 23.4801, 120.4493, 18_000),
    CHIAYI_COUNTY("嘉義縣", 23.4595, 120.2940, 35_000), // 太保

    // 台南高雄屏
    TAINAN("台南", 22.9997, 120.2270, 25_000),
    KAOHSIUNG("高雄", 22.6273, 120.3014, 30_000),
    PINGTUNG("屏東", 22.6761, 120.4940, 35_000),

    // 宜花東
    YILAN("宜蘭", 24.7021, 121.7378, 25_000),
    HUALIEN("花蓮", 23.9872, 121.6015, 35_000),
    TAITUNG("台東", 22.7583, 121.1449, 35_000),

    // 離島
    PENGHU("澎湖", 23.5655, 119.5863, 20_000),     // 馬公
    KINMEN("金門", 24.4368, 118.3186, 20_000),     // 金城
    LIENCHIANG("連江", 26.1595, 119.9499, 20_000)  // 南竿
}


enum class Category(val label: String, val primaryTypes: List<String>) {
    FOOD("餐廳", listOf("restaurant")),
    DRINKS("飲料/咖啡", listOf("cafe")),
    NATURE("自然/風景", listOf("tourist_attraction", "park", "natural_feature")),
    AMUSEMENT("遊樂園", listOf("amusement_park")),
    SHOPPING("購物", listOf("shopping_mall", "department_store"))
}
