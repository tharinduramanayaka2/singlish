package com.example

/**
 * Singlish to Sinhala Phonetic Transliterator Engine.
 *
 * Implements real-time phonetic conversion matching the popular Helakuru Singlish scheme:
 * - Vowels: a -> අ, aa/A -> ආ, ae/q -> ඇ, aee/Q -> ඈ, i -> ඉ, ii/I -> ඊ, u -> උ, uu/U -> ඌ, e -> එ, ee/E -> ඒ, o -> ඔ, oo/O -> ඕ, ai -> ඓ, au/ou -> ඖ
 * - Consonants (in hal / virama form when typed alone): k -> ක්, g -> ග්, t -> ට්, th -> ත්, d -> ඩ්, dh -> ද්, n -> න්, N -> ණ්, etc.
 * - Dynamic Pilla / Vowel attachment:
 *     k + a -> ක
 *     k + aa -> කා
 *     k + ae -> කැ
 *     k + i -> කි
 *     k + ii -> කී
 *     k + u -> කු
 *     k + uu -> කූ
 *     k + e -> කෙ
 *     k + ee -> කේ
 *     k + ai -> කෛ
 *     k + o -> කො
 *     k + oo -> කෝ
 *     k + au -> කෞ
 *     k + R / kru -> කෘ
 * - Multi-character combos (Rakaransaya, Yansaya, Bandi Akuru):
 *     kr + a -> ක්‍ර, ky + a -> ක්‍ය, kw + a -> ක්ව
 *     th -> ත්, dh -> ද්, sh -> ශ්, Sh -> ෂ්, ch -> ච්, Ch -> ඡ්, jh -> ඣ්
 *     nnd -> ඳ, nnb -> ඹ, nng -> ඟ, nd -> න්ද, ndh -> න්ධ, mb -> ම්බ
 */
object SinglishTransliterator {

    private const val VIRAMA = "\u0DCA" // ් (Hal Kirima)
    private const val ZWJ = "\u200D"   // Zero-Width Joiner (for Rakaransaya / Yansaya / Bandi Akuru)

    // Standalone vowels at the start of a syllable or word
    private val standaloneVowels = listOf(
        "aee" to "ඈ", "aae" to "ඈ", "ae" to "ඇ",
        "aa" to "ආ", "a" to "අ", "A" to "ආ",
        "ii" to "ඊ", "i" to "ඉ", "I" to "ඊ",
        "uu" to "ඌ", "u" to "උ", "U" to "ඌ",
        "ee" to "ඒ", "e" to "එ", "E" to "ඒ",
        "ai" to "ඓ",
        "oo" to "ඕ", "o" to "ඔ", "O" to "ඕ",
        "au" to "ඖ", "ou" to "ඖ",
        "q" to "ඇ", "Q" to "ඈ",
        "R" to "ඍ", "ru" to "ඍ"
    )

    // Dependent vowel signs (Pillam) applied to consonants
    // Note: Empty string for "a" represents inherent vowel removing the hal kirima
    private val vowelPillam = listOf(
        "aee" to "\u0DD1", // ෑ
        "aae" to "\u0DD1", // ෑ
        "ae" to "\u0DD0",  // ැ
        "aa" to "\u0DCF",  // ා
        "a" to "",         // Inherent vowel removes VIRAMA
        "A" to "\u0DCF",   // ා
        "ii" to "\u0DD3",  // ී
        "i" to "\u0DD2",   // ි
        "I" to "\u0DD3",   // ී
        "uu" to "\u0DD6",  // ූ
        "u" to "\u0DD4",   // ු
        "U" to "\u0DD6",   // ූ
        "ee" to "\u0DDA",  // ේ
        "e" to "\u0DD9",   // ෙ
        "E" to "\u0DDA",   // ේ
        "ai" to "\u0DDB",  // ෛ
        "oo" to "\u0DDD",  // ෝ
        "o" to "\u0DDC",   // ො
        "O" to "\u0DDD",   // ෝ
        "au" to "\u0DDE",  // ෞ
        "ou" to "\u0DDE",  // ෞ
        "q" to "\u0DD0",   // ැ
        "Q" to "\u0DD1",   // ෑ
        "R" to "\u0DD8",   // ෘ
        "ru" to "\u0DD8"   // ෘ
    )

    // Consonant root mappings (without VIRAMA)
    // Ordered with longer keys first for greedy matching
    private val consonantRoots = listOf(
        "nndh" to "ඳ",
        "nnd" to "ඳ",
        "nnb" to "ඹ",
        "nng" to "ඟ",
        "shh" to "ෂ",
        "Sh" to "ෂ",
        "sh" to "ශ",
        "th" to "ත",
        "Th" to "ථ",
        "dh" to "ද",
        "Dh" to "ධ",
        "ch" to "ච",
        "Ch" to "ඡ",
        "kh" to "ඛ",
        "Kh" to "ඛ",
        "gh" to "ඝ",
        "Gh" to "ඝ",
        "jh" to "ඣ",
        "Jh" to "ඣ",
        "ph" to "ඵ",
        "Ph" to "ඵ",
        "bh" to "භ",
        "Bh" to "භ",
        "kn" to "ඤ",
        "gn" to "ඥ",
        "k" to "ක", "K" to "ඛ",
        "g" to "ග", "G" to "ඝ",
        "t" to "ට", "T" to "ඨ",
        "d" to "ඩ", "D" to "ඪ",
        "n" to "න", "N" to "ණ",
        "p" to "ප", "P" to "ඵ",
        "b" to "බ", "B" to "භ",
        "m" to "ම", "M" to "ම",
        "y" to "ය", "Y" to "ය",
        "r" to "ර", "R" to "ර",
        "l" to "ල", "L" to "ළ",
        "w" to "ව", "W" to "ව", "v" to "ව", "V" to "ව",
        "s" to "ස", "S" to "ශ",
        "h" to "හ", "H" to "හ",
        "f" to "ෆ", "F" to "ෆ",
        "j" to "ජ", "J" to "ඣ",
        "c" to "ච", "C" to "ඡ"
    )

    // Special standalone markers
    private val specialMarkers = mapOf(
        "x" to "\u0D82", // ං (Anusvaraya)
        "X" to "\u0D83"  // ඃ (Visargaya)
    )

    /**
     * Common Sinhala vocabulary dataset for instant word predictions
     */
    val commonSinhalaWords: List<String> = listOf(
        "මම", "මගේ", "මගෙන්", "මට", "මමත්", "මමයි",
        "ඔයා", "ඔයාගේ", "ඔයාට", "ඔබ", "ඔබට", "ඔබගේ",
        "අපි", "අපේ", "අපිට", "අපෙන්", "අපිටත්",
        "එයා", "එයාගේ", "එයාට", "ඔහු", "ඇය", "ඔවුන්",
        "කොහොමද", "කොහේද", "මොකද", "මොනවාද", "කවුද", "කවදාද", "ඇයි",
        "ස්තූතියි", "බොහොම", "සුබ", "පැතුම්", "දවසක්", "සුබ උදෑසනක්", "සුබ රාත්‍රියක්",
        "හරි", "නෑ", "නැහැ", "ඔව්", "එපා", "ඕනේ", "පුළුවන්", "බෑ", "බැහැ",
        "ගෙදර", "යනවා", "එනවා", "කරනවා", "බලනවා", "කනවා", "බොනවා", "නිදාගන්නවා",
        "ලස්සනයි", "හොඳයි", "නරකයි", "කරුණාකර", "ආයුබෝවන්", "සමාවෙන්න",
        "පාසල", "රැකියාව", "කාර්යාලය", "මිතුරා", "යාලුවා", "අම්මා", "තාත්තා", "අයියා", "අක්කා", "මල්ලී", "නංගි",
        "ලංකාව", "ශ්‍රී ලංකා", "සිංහල", "භාෂාව", "පොත", "පෑන", "දුරකථනය", "වෙලාව",
        "ආදරෙයි", "සතුටුයි", "දුකයි", "බයයි", "සිතනවා", "දන්නවා", "හිතනවා"
    )

    /**
     * Transliterates a given Singlish string into Sinhala Unicode.
     */
    fun transliterate(input: String): String {
        if (input.isEmpty()) return ""

        val result = StringBuilder()
        var i = 0
        val len = input.length

        while (i < len) {
            val remaining = input.substring(i)

            // 1. Check special symbols or non-ASCII characters
            val charAt = input[i]
            if (charAt == 'x' || charAt == 'X') {
                result.append(specialMarkers[charAt.toString()] ?: charAt.toString())
                i++
                continue
            }
            if (!charAt.isLetter()) {
                result.append(charAt)
                i++
                continue
            }

            // 2. Check for Rakaransaya / Yansaya combos (e.g. kr, tr, pr, ky, ty, etc.)
            val comboMatch = matchConsonantCluster(remaining)
            if (comboMatch != null) {
                result.append(comboMatch.sinhalaOutput)
                i += comboMatch.consumedLength
                continue
            }

            // 3. Check for standard Consonant + optional Vowel
            val consMatch = matchConsonant(remaining)
            if (consMatch != null) {
                result.append(consMatch.sinhalaOutput)
                i += consMatch.consumedLength
                continue
            }

            // 4. Check for Standalone Vowel
            val vowelMatch = matchStandaloneVowel(remaining)
            if (vowelMatch != null) {
                result.append(vowelMatch.second)
                i += vowelMatch.first
                continue
            }

            // Fallback for unmatched character
            result.append(input[i])
            i++
        }

        return result.toString()
    }

    private class MatchResult(val consumedLength: Int, val sinhalaOutput: String)

    /**
     * Checks for compound clusters like Rakaransaya (kr, pr, tr), Yansaya (ky, py),
     * or Bandi Akuru (ndh, nd, mb, tth).
     */
    private fun matchConsonantCluster(text: String): MatchResult? {
        // Try to match first consonant
        for ((consKey, baseChar) in consonantRoots) {
            if (text.startsWith(consKey)) {
                val afterCons = text.substring(consKey.length)

                // Check for Rakaransaya: consonant + 'r' (e.g. "kr", "pr", "tr")
                if (afterCons.startsWith("r") && !afterCons.startsWith("ru")) {
                    val afterR = afterCons.substring(1)
                    val pillaMatch = matchVowelPilla(afterR)
                    val consumed = consKey.length + 1 + (pillaMatch?.first ?: 0)
                    val pilla = pillaMatch?.second ?: VIRAMA

                    // Format: base + VIRAMA + ZWJ + 'ර' + pilla
                    val out = "$baseChar$VIRAMA${ZWJ}ර$pilla"
                    return MatchResult(consumed, out)
                }

                // Check for Yansaya: consonant + 'y' (e.g. "ky", "py")
                if (afterCons.startsWith("y")) {
                    val afterY = afterCons.substring(1)
                    val pillaMatch = matchVowelPilla(afterY)
                    val consumed = consKey.length + 1 + (pillaMatch?.first ?: 0)
                    val pilla = pillaMatch?.second ?: VIRAMA

                    // Format: base + VIRAMA + ZWJ + 'ය' + pilla
                    val out = "$baseChar$VIRAMA${ZWJ}ය$pilla"
                    return MatchResult(consumed, out)
                }

                // Check for Bandi Akuru: consonant + 'w' (e.g. "kw", "sw", "tw")
                if (afterCons.startsWith("w")) {
                    val afterW = afterCons.substring(1)
                    val pillaMatch = matchVowelPilla(afterW)
                    val consumed = consKey.length + 1 + (pillaMatch?.first ?: 0)
                    val pilla = pillaMatch?.second ?: VIRAMA

                    // Format: base + VIRAMA + ZWJ + 'ව' + pilla
                    val out = "$baseChar$VIRAMA${ZWJ}ව$pilla"
                    return MatchResult(consumed, out)
                }

                break
            }
        }
        return null
    }

    /**
     * Matches a single base consonant and any subsequent vowel sound.
     * If no vowel follows, appends the VIRAMA (Hal Kirima), e.g. "k" -> "ක්".
     */
    private fun matchConsonant(text: String): MatchResult? {
        for ((consKey, baseChar) in consonantRoots) {
            if (text.startsWith(consKey)) {
                val afterCons = text.substring(consKey.length)
                val pillaMatch = matchVowelPilla(afterCons)

                return if (pillaMatch != null) {
                    // Consonant + Vowel sign
                    val consumed = consKey.length + pillaMatch.first
                    MatchResult(consumed, "$baseChar${pillaMatch.second}")
                } else {
                    // Consonant alone -> Hal letter (e.g. k -> ක්)
                    MatchResult(consKey.length, "$baseChar$VIRAMA")
                }
            }
        }
        return null
    }

    /**
     * Matches dependent vowel signs (Pillam) at the start of [text].
     */
    private fun matchVowelPilla(text: String): Pair<Int, String>? {
        if (text.isEmpty()) return null
        for ((vKey, pilla) in vowelPillam) {
            if (text.startsWith(vKey)) {
                return Pair(vKey.length, pilla)
            }
        }
        return null
    }

    /**
     * Matches standalone vowels at the start of [text].
     */
    private fun matchStandaloneVowel(text: String): Pair<Int, String>? {
        if (text.isEmpty()) return null
        for ((vKey, vChar) in standaloneVowels) {
            if (text.startsWith(vKey)) {
                return Pair(vKey.length, vChar)
            }
        }
        return null
    }

    /**
     * Generates intelligent word predictions and candidates:
     * 1. Direct phonetic transliteration
     * 2. Common Sinhala words matching the prefix
     * 3. Exact raw Latin Singlish candidate (so users can pick the raw English word)
     */
    fun getPredictions(rawInput: String, transliteratedText: String, limit: Int = 7): List<String> {
        val candidates = LinkedHashSet<String>()

        if (transliteratedText.isNotBlank()) {
            // First candidate: Current best transliteration
            candidates.add(transliteratedText.trim())

            // Prefix match against the common Sinhala vocabulary
            val query = transliteratedText.trim()
            val matches = commonSinhalaWords.filter { it.startsWith(query) && it != query }
            candidates.addAll(matches)
        }

        // Add raw Latin input as fallback option if it's alphanumeric
        if (rawInput.isNotBlank()) {
            candidates.add(rawInput.trim())
        }

        // Fill remaining slots with common words if query is short
        if (candidates.size < limit) {
            for (word in commonSinhalaWords) {
                if (!candidates.contains(word)) {
                    candidates.add(word)
                }
                if (candidates.size >= limit) break
            }
        }

        return candidates.take(limit)
    }

    data class TypingRule(
        val singlish: String,
        val sinhala: String,
        val description: String,
        val category: String
    )

    /**
     * Quick reference rules for the in-app Typing Guide
     */
    fun getTypingGuide(): List<TypingRule> {
        return listOf(
            TypingRule("k", "ක්", "Single consonant has Hal Kirima", "Consonants"),
            TypingRule("ka", "ක", "Consonant + 'a' inherent vowel", "Consonants"),
            TypingRule("kaa / kA", "කා", "Alapilla (long 'aa')", "Vowel Signs (Pillam)"),
            TypingRule("kae / kq", "කැ", "Aeda Pilla", "Vowel Signs (Pillam)"),
            TypingRule("kaee / kQ", "කෑ", "Diga Aeda Pilla", "Vowel Signs (Pillam)"),
            TypingRule("ki", "කි", "Ispilla", "Vowel Signs (Pillam)"),
            TypingRule("kii / kI", "කී", "Diga Ispilla", "Vowel Signs (Pillam)"),
            TypingRule("ku", "කු", "Papilla", "Vowel Signs (Pillam)"),
            TypingRule("kuu / kU", "කූ", "Diga Papilla", "Vowel Signs (Pillam)"),
            TypingRule("ke", "කෙ", "Kombuwa", "Vowel Signs (Pillam)"),
            TypingRule("kee / kE", "කේ", "Diga Kombuwa", "Vowel Signs (Pillam)"),
            TypingRule("ko", "කො", "Kombuwa + Aelapilla", "Vowel Signs (Pillam)"),
            TypingRule("koo / kO", "කෝ", "Diga Kombuwa + Aelapilla", "Vowel Signs (Pillam)"),
            TypingRule("kai", "කෛ", "Kombu Deka", "Vowel Signs (Pillam)"),
            TypingRule("kau / kou", "කෞ", "Kombuwa + Gayanukitta", "Vowel Signs (Pillam)"),
            TypingRule("kru / kR", "කෘ", "Gaetta Pilla", "Vowel Signs (Pillam)"),
            TypingRule("th", "ත් / ත", "Soft 'th' sound", "Consonants"),
            TypingRule("dh", "ද් / ද", "Soft 'dh' sound", "Consonants"),
            TypingRule("sh", "ශ් / ශ", "Talaja Sha", "Consonants"),
            TypingRule("Sh / shh", "ෂ් / ෂ", "Murdhaja Sha", "Consonants"),
            TypingRule("nnd", "ඳ", "Sanyaka 'Dha'", "Special Letters"),
            TypingRule("nnb", "ඹ", "Sanyaka 'Ba'", "Special Letters"),
            TypingRule("nng", "ඟ", "Sanyaka 'Ga'", "Special Letters"),
            TypingRule("kra", "ක්‍ර", "Rakaransaya combination", "Clusters"),
            TypingRule("kya", "ක්‍ය", "Yansaya combination", "Clusters"),
            TypingRule("kwa", "ක්ව", "Bandi Akuru combination", "Clusters"),
            TypingRule("mama", "මම", "Pronoun 'I'", "Examples"),
            TypingRule("oyaa", "ඔයා", "Pronoun 'You'", "Examples"),
            TypingRule("sthuuthiyi", "ස්තූතියි", "Thank you", "Examples"),
            TypingRule("kohomadha", "කොහොමද", "How are you", "Examples")
        )
    }
}
