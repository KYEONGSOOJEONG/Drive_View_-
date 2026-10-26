package com.driveview.prototype

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Place(
    val id: String,
    val name: String,
    val category: String,
    val distanceKm: Int,
    val etaMin: Int,
    val pinX: Float,
    val pinY: Float,
    val latitude: Double,
    val longitude: Double,
    val address: String,
)

object Places {
    val categories = listOf("산", "바다", "해안가", "계곡", "강", "야경", "관광지")

    val all = listOf(
        Place("sanjeong", "산 정상부근 명소", "산", 8, 18, 0.28f, 0.30f, 35.2321, 129.0684, "부산 금정산 정상 부근"),
        Place("hwangnyeong", "황령산 명소", "산", 12, 26, 0.46f, 0.62f, 35.1578, 129.0823, "부산 남구 황령산로"),
        Place("geumjeong", "금정산 명소", "산", 21, 40, 0.62f, 0.38f, 35.2834, 129.0552, "부산 금정구 금정산"),
        Place("haeundae", "해운대 해변", "바다", 6, 14, 0.72f, 0.58f, 35.1587, 129.1604, "부산 해운대구 해운대해변로"),
        Place("songjeong", "송정 해안", "해안가", 15, 28, 0.80f, 0.42f, 35.1784, 129.1996, "부산 해운대구 송정해변로"),
        Place("valley", "장안사 계곡", "계곡", 27, 48, 0.34f, 0.48f, 35.3182, 129.2281, "부산 기장군 장안사"),
        Place("nakdong", "낙동강 전망", "강", 11, 22, 0.22f, 0.70f, 35.1162, 128.9465, "부산 사하구 낙동강 하구"),
        Place("gwangan", "광안대교 야경", "야경", 9, 20, 0.58f, 0.74f, 35.1532, 129.1304, "부산 수영구 광안대교"),
        Place("taejongdae", "태종대", "관광지", 18, 34, 0.40f, 0.22f, 35.0534, 129.0868, "부산 영도구 태종대"),
    )

    fun byId(id: String): Place = all.first { it.id == id }

    fun inCategory(category: String): List<Place> =
        all.filter { it.category == category }.sortedBy { it.distanceKm }

    fun around(center: Place): List<Place> =
        all.filter { metersBetween(center, it) <= NEARBY_RADIUS_METERS }
            .sortedBy { metersBetween(center, it) }

    private fun metersBetween(from: Place, to: Place): Double {
        val earth = 6_371_000.0
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * earth * asin(sqrt(h))
    }

    private val playlists = mapOf(
        "hwangnyeong" to listOf("황령산 야경 드라이브", "부산 밤바다 팝"),
        "geumjeong" to listOf("산길 잔잔한 음악", "아침 하이킹 플레이리스트"),
        "haeundae" to listOf("바다 보며 듣는 노래", "여름 드라이브"),
        "songjeong" to listOf("해안도로 플레이리스트"),
        "nakdong" to listOf("강변 노을 음악"),
        "gwangan" to listOf("광안대교 야경 노래", "도시 야간 드라이브"),
        "taejongdae" to listOf("남해 드라이브"),
    )

    private const val NEARBY_RADIUS_METERS = 500.0

    fun playlistsFor(placeId: String): List<String> = playlists[placeId].orEmpty()
}
