package ru.agromarket.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.agromarket.data.api.AdMyListResponse

/**
 * Проверяет, что Gson корректно мапит snake_case поля API на camelCase в DTO
 * (через @SerializedName). Чистый JVM-тест, без Android.
 */
class ModelsSerializationTest {

    private val gson = Gson()

    @Test
    fun `LoginResponse maps snake_case token fields`() {
        val json = """{"access_token":"acc","refresh_token":"ref","token_type":"bearer"}"""

        val parsed = gson.fromJson(json, LoginResponse::class.java)

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

        val parsed = gson.fromJson(json, CategoryTreeResponse::class.java)

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

        val parsed = gson.fromJson(json, AdMyListResponse::class.java)

        assertEquals("Добавьте фото техники", parsed.moderationComment)
    }

    @Test
    fun `AdMyListResponse moderation_comment defaults to null when absent`() {
        val json = """{"id":"1","type":"sale","title":"Трактор","status":"active","created_at":"2026-06-01"}"""

        val parsed = gson.fromJson(json, AdMyListResponse::class.java)

        assertNull(parsed.moderationComment)
    }

    @Test
    fun `AdMyListResponse maps moderation_feedback with photos`() {
        val json = """
            {"id":"1","type":"sale","title":"Трактор","status":"needs_revision","created_at":"2026-06-01",
             "moderation_feedback":{"decision":"needs_revision","comment":"Добавьте фото VIN-номера",
             "photos":[{"id":5,"url":"https://example.com/p5.jpg","sort_order":0}],"created_at":"2026-06-12T10:00:00"}}
        """.trimIndent()

        val parsed = gson.fromJson(json, AdMyListResponse::class.java)

        assertEquals("needs_revision", parsed.moderationFeedback?.decision)
        assertEquals("Добавьте фото VIN-номера", parsed.moderationFeedback?.comment)
        assertEquals(1, parsed.moderationFeedback?.photos?.size)
        assertEquals("https://example.com/p5.jpg", parsed.moderationFeedback?.photos?.get(0)?.url)
    }

    @Test
    fun `AdMyListResponse moderation_feedback defaults to null when absent`() {
        val json = """{"id":"1","type":"sale","title":"Трактор","status":"active","created_at":"2026-06-01"}"""

        val parsed = gson.fromJson(json, AdMyListResponse::class.java)

        assertNull(parsed.moderationFeedback)
    }
}
