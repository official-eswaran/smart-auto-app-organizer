package com.smartorganizer.launcher.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleEngineTest {

    private lateinit var ruleEngine: RuleEngine

    @Before
    fun setUp() {
        ruleEngine = RuleEngine()
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @Test
    fun `GPay classifies as Payments`() {
        val result = ruleEngine.classify("GPay", "com.google.android.apps.nbu.paisa.user")
        assertEquals("Payments", result.category)
        assertTrue("Expected confidence >= 0.3, got ${result.confidence}", result.confidence >= 0.3f)
    }

    @Test
    fun `PhonePe classifies as Payments`() {
        val result = ruleEngine.classify("PhonePe", "com.phonepe.app")
        assertEquals("Payments", result.category)
    }

    @Test
    fun `Paytm classifies as Payments`() {
        val result = ruleEngine.classify("Paytm", "net.one97.paytm")
        assertEquals("Payments", result.category)
    }

    // ── Games ─────────────────────────────────────────────────────────────────

    @Test
    fun `BGMI classifies as Games`() {
        val result = ruleEngine.classify("BGMI", "com.pubg.imobile")
        assertEquals("Games", result.category)
    }

    @Test
    fun `Clash of Clans classifies as Games`() {
        val result = ruleEngine.classify("Clash of Clans", "com.supercell.clashofclans")
        assertEquals("Games", result.category)
    }

    @Test
    fun `Chess classifies as Games`() {
        val result = ruleEngine.classify("Chess", "com.chess")
        assertEquals("Games", result.category)
    }

    // ── Music ─────────────────────────────────────────────────────────────────

    @Test
    fun `Spotify classifies as Music`() {
        val result = ruleEngine.classify("Spotify", "com.spotify.music")
        assertEquals("Music", result.category)
        assertTrue("Expected confidence >= 0.3, got ${result.confidence}", result.confidence >= 0.3f)
    }

    @Test
    fun `Gaana classifies as Music`() {
        val result = ruleEngine.classify("Gaana", "com.gaana")
        assertEquals("Music", result.category)
    }

    // ── Social ────────────────────────────────────────────────────────────────

    @Test
    fun `WhatsApp classifies as Social`() {
        val result = ruleEngine.classify("WhatsApp", "com.whatsapp")
        assertEquals("Social", result.category)
    }

    @Test
    fun `Telegram classifies as Social`() {
        val result = ruleEngine.classify("Telegram", "org.telegram.messenger")
        assertEquals("Social", result.category)
    }

    // ── Shopping ──────────────────────────────────────────────────────────────

    @Test
    fun `Amazon classifies as Shopping`() {
        val result = ruleEngine.classify("Amazon", "com.amazon.mShop.android.shopping")
        assertEquals("Shopping", result.category)
    }

    @Test
    fun `Flipkart classifies as Shopping`() {
        val result = ruleEngine.classify("Flipkart", "com.flipkart.android")
        assertEquals("Shopping", result.category)
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @Test
    fun `Health app classifies as Health`() {
        val result = ruleEngine.classify("Health Fitness Tracker", "com.health.fitness")
        assertEquals("Health", result.category)
    }

    // ── Travel ────────────────────────────────────────────────────────────────

    @Test
    fun `Uber classifies as Travel`() {
        val result = ruleEngine.classify("Uber", "com.ubercab")
        assertEquals("Travel", result.category)
    }

    @Test
    fun `Ola classifies as Travel`() {
        val result = ruleEngine.classify("Ola", "com.olacabs.customer")
        assertEquals("Travel", result.category)
    }

    // ── News ──────────────────────────────────────────────────────────────────

    @Test
    fun `Inshorts classifies as News`() {
        val result = ruleEngine.classify("Inshorts News", "com.nis.inshorts")
        assertEquals("News", result.category)
    }

    // ── Others (fallback) ─────────────────────────────────────────────────────

    @Test
    fun `Unknown app classifies as Others`() {
        val result = ruleEngine.classify("XYZ App", "com.unknown.xyz")
        assertEquals("Others", result.category)
        assertEquals(0.0f, result.confidence)
    }

    @Test
    fun `Empty package and name classifies as Others`() {
        val result = ruleEngine.classify("", "com.abc.def")
        assertEquals("Others", result.category)
    }

    // ── Confidence threshold boundary tests ───────────────────────────────────

    @Test
    fun `Confidence is between 0 and 1 for known categories`() {
        val result = ruleEngine.classify("Spotify Music", "com.spotify.music")
        assertTrue("Confidence should be >= 0", result.confidence >= 0f)
        assertTrue("Confidence should be <= 1", result.confidence <= 1f)
    }

    @Test
    fun `Others category has confidence 0`() {
        val result = ruleEngine.classify("RandomApp", "com.random.unknown.app")
        if (result.category == "Others") {
            assertEquals(0.0f, result.confidence)
        }
    }

    @Test
    fun `Multiple keyword matches increase confidence`() {
        val lowResult = ruleEngine.classify("Music", "com.music.app")
        val highResult = ruleEngine.classify("Music Song Audio Player", "com.music.song.player")
        // More keyword matches should give higher confidence
        assertTrue(
            "More matches should give higher or equal confidence",
            highResult.confidence >= lowResult.confidence
        )
    }
}
