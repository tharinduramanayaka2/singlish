package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Singlish to Sinhala phonetic transliteration and candidate predictions.
 */
class ExampleUnitTest {

    @Test
    fun testBasicConsonantAndVowel() {
        assertEquals("ක්", SinglishTransliterator.transliterate("k"))
        assertEquals("ක", SinglishTransliterator.transliterate("ka"))
        assertEquals("කා", SinglishTransliterator.transliterate("kaa"))
        assertEquals("කි", SinglishTransliterator.transliterate("ki"))
        assertEquals("කී", SinglishTransliterator.transliterate("kii"))
        assertEquals("කු", SinglishTransliterator.transliterate("ku"))
        assertEquals("කූ", SinglishTransliterator.transliterate("kuu"))
        assertEquals("කෙ", SinglishTransliterator.transliterate("ke"))
        assertEquals("කේ", SinglishTransliterator.transliterate("kee"))
        assertEquals("කො", SinglishTransliterator.transliterate("ko"))
        assertEquals("කෝ", SinglishTransliterator.transliterate("koo"))
    }

    @Test
    fun testCommonWords() {
        assertEquals("මම", SinglishTransliterator.transliterate("mama"))
        assertEquals("ඔයා", SinglishTransliterator.transliterate("oyaa"))
        assertEquals("කොහොමද", SinglishTransliterator.transliterate("kohomadha"))
        assertEquals("ස්තූතියි", SinglishTransliterator.transliterate("sthuuthiyi"))
    }

    @Test
    fun testMultiCharacterConsonants() {
        assertEquals("ත්", SinglishTransliterator.transliterate("th"))
        assertEquals("ත", SinglishTransliterator.transliterate("tha"))
        assertEquals("තා", SinglishTransliterator.transliterate("thaa"))
        assertEquals("ද්", SinglishTransliterator.transliterate("dh"))
        assertEquals("ද", SinglishTransliterator.transliterate("dha"))
        assertEquals("ශ්", SinglishTransliterator.transliterate("sh"))
        assertEquals("ශ", SinglishTransliterator.transliterate("sha"))
    }

    @Test
    fun testPredictions() {
        val predictions = SinglishTransliterator.getPredictions("mama", "මම")
        assertTrue(predictions.contains("මම"))
        assertTrue(predictions.isNotEmpty())
    }
}
