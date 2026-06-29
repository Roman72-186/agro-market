package ru.agromarket.data.model

import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.agromarket.data.ApiJson
import ru.agromarket.data.api.AdMyListResponse

/**
 * Проверяет, что kotlinx.serialization корректно мапит snake_case поля API на camelCase
 * в DTO (через @SerialName) и round-trip запросов. Чистый JVM-тест, без Android.
 * Используется тот же ApiJson, что и в проде (ignoreUnknownKeys + coerceInputValues).
 */
class ModelsSerializationTest {

    @Test
    fun `LoginResponse maps snake_case token fields`() {
        val json = """{"access_token":"acc","refresh_token":"ref","token_type":"bearer"}"""

        val parsed = ApiJson.decodeFromString<LoginResponse>(json)

        assertEquals("acc", parsed.accessToken)
        assertEquals("ref", parsed.refreshToken)
        assertEquals("bearer", parsed.tokenType)
    }

    @Test
    fun `CategoryTreeResponse parses nested children and defaults`() {
        val json = """
            {"id":1,"name":"Техника","children":[
                {"id":2,"name":"Тракторы","parent_id":1}
            ]}
        """.trimIndent()

        val parsed = ApiJson.decodeFromString<CategoryTreeResponse>(json)

        assertEquals(1, parsed.id)
        assertEquals(0, parsed.sortOrder)          // default when absent
        assertNull(parsed.icon)                    // default when absent
        assertEquals(1, parsed.children.size)
        assertEquals(1, parsed.children[0].parentId)
    }

    @Test
    fun `AdMyListResponse maps moderation_comment when present`() {
        val json = """
            {"id":"1","type":"sale","title":"Трактор","status":"rejected","created_at":"2026-06-01",
             "moderation_comment":"Добавьте фото техники"}
        """.trimIndent()

        val parsed = ApiJson.decodeFromString<AdMyListResponse>(json)

        assertEquals("Добавьте фото техники", parsed.moderationComment)
    }

    @Test
    fun `AdMyListResponse moderation_comment defaults to null when absent`() {
        val json = """{"id":"1","type":"sale","title":"Трактор","status":"active","created_at":"2026-06-01"}"""

        val parsed = ApiJson.decodeFromString<AdMyListResponse>(json)

        assertNull(parsed.moderationComment)
    }

    @Test
    fun `AdMyListResponse maps moderation_feedback with photos`() {
        val json = """
            {"id":"1","type":"sale","title":"Трактор","status":"needs_revision","created_at":"2026-06-01",
             "moderation_feedback":{"decision":"needs_revision","comment":"Добавьте фото VIN-номера",
             "photos":[{"id":5,"url":"https://example.com/p5.jpg","sort_order":0}],"created_at":"2026-06-12T10:00:00"}}
        """.trimIndent()

        val parsed = ApiJson.decodeFromString<AdMyListResponse>(json)

        assertEquals("needs_revision", parsed.moderationFeedback?.decision)
        assertEquals("Добавьте фото VIN-номера", parsed.moderationFeedback?.comment)
        assertEquals(1, parsed.moderationFeedback?.photos?.size)
        assertEquals("https://example.com/p5.jpg", parsed.moderationFeedback?.photos?.get(0)?.url)
    }

    @Test
    fun `AdMyListResponse moderation_feedback defaults to null when absent`() {
        val json = """{"id":"1","type":"sale","title":"Трактор","status":"active","created_at":"2026-06-01"}"""

        val parsed = ApiJson.decodeFromString<AdMyListResponse>(json)

        assertNull(parsed.moderationFeedback)
    }

    @Test
    fun `AdListResponse parses price as JSON number`() {
        val json = """
            {"id":"1","type":"sale","category_id":10,"region_id":5,"title":"Трактор",
             "price":1250000,"boost_level":"none","status":"active","created_at":"2026-06-01"}
        """.trimIndent()

        val parsed = ApiJson.decodeFromString<AdListResponse>(json)

        assertEquals(1250000.0, parsed.price!!, 0.0)
    }

    @Test
    fun `PushTokenRegisterRequest always encodes platform`() {
        // encodeDefaults=false опускает поля, равные дефолту: у platform нет дефолта,
        // поэтому контракт push-tokens (platform обязателен) не ломается.
        val json = ApiJson.encodeToString(PushTokenRegisterRequest(token = "abc", platform = "android"))

        assertTrue(json.contains("\"platform\""))
        assertTrue(json.contains("\"android\""))
        assertTrue(json.contains("\"token\""))
    }
}
