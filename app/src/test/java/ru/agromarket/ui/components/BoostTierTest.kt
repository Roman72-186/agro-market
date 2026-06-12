package ru.agromarket.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class BoostTierTest {

    @Test
    fun `none, empty and null map to tier 0`() {
        assertEquals(0, boostTier(null))
        assertEquals(0, boostTier(""))
        assertEquals(0, boostTier("none"))
        assertEquals(0, boostTier("NONE"))
    }

    @Test
    fun `numeric levels map directly`() {
        assertEquals(1, boostTier("1"))
        assertEquals(2, boostTier("2"))
        assertEquals(3, boostTier("3"))
    }

    @Test
    fun `named levels map to their tiers`() {
        assertEquals(1, boostTier("top"))
        assertEquals(1, boostTier("raise"))
        assertEquals(2, boostTier("highlight"))
        assertEquals(2, boostTier("premium"))
        assertEquals(3, boostTier("xl"))
        assertEquals(3, boostTier("XL"))
    }

    @Test
    fun `unknown values degrade to tier 0, out-of-range numbers are clamped`() {
        assertEquals(0, boostTier("mega-boost"))
        assertEquals(3, boostTier("7"))
        assertEquals(0, boostTier("-1"))
    }
}
