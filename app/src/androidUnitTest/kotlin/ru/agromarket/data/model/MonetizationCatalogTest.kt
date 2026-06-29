package ru.agromarket.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationCatalogTest {

    @Test
    fun `kopecks format to whole rubles label`() {
        assertEquals("499 ₽", 49_900L.kopecksToRubLabel())
        assertEquals("99 ₽", 9_900L.kopecksToRubLabel())
        assertEquals("0 ₽", 0L.kopecksToRubLabel())
    }

    @Test
    fun `default plans expose a free and an unlimited pro plan`() {
        val plans = MonetizationCatalog.DEFAULT_PLANS.plans
        val free = plans.first { it.code == "FREE" }
        val pro = plans.first { it.code == "PRO" }
        assertEquals(0L, free.priceKopecks)
        assertNotNull(free.adLimit)
        assertNull("PRO — безлимит объявлений", pro.adLimit)
        assertTrue(pro.priceKopecks > 0)
    }

    @Test
    fun `boost payment types match backend enum values`() {
        assertEquals("boost_7", BoostPaymentType.BOOST_7)
        assertEquals("boost_30", BoostPaymentType.BOOST_30)
    }
}
