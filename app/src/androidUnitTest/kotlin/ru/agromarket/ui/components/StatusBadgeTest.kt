package ru.agromarket.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusBadgeTest {

    @Test
    fun `known statuses map to expected labels and tones`() {
        assertEquals("Черновик" to BadgeTone.NEUTRAL, adStatusBadge("draft"))
        assertEquals("На модерации" to BadgeTone.INFO, adStatusBadge("pending_moderation"))
        assertEquals("Активно" to BadgeTone.SUCCESS, adStatusBadge("active"))
        assertEquals("Отклонено" to BadgeTone.DANGER, adStatusBadge("rejected"))
        assertEquals("На доработке" to BadgeTone.WARNING, adStatusBadge("needs_revision"))
    }

    @Test
    fun `unknown status falls back to raw value with neutral tone`() {
        assertEquals("expired" to BadgeTone.NEUTRAL, adStatusBadge("expired"))
    }
}
