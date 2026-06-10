package ru.agromarket.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
