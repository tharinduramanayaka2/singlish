package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ThemeManager.KeyboardTheme

/**
 * SinhalaKeyboardService
 *
 * Core InputMethodService implementing:
 * 1. Live Singlish to Sinhala phonetic transliteration (e.g. k -> ක්, ka -> ක, kaa -> කා)
 * 2. Word Prediction / Suggestion bar above the keyboard
 * 3. Dynamic multi-theme styling (Light, Dark, Helakuru Royal Blue, Ceylon Emerald)
 * 4. Mode switching: Singlish Phonetic, English QWERTY, and Numbers/Symbols
 * 5. Haptic and sound feedback
 */
class SinhalaKeyboardService : InputMethodService() {

    enum class InputMode {
        SINGLISH,
        ENGLISH
    }

    private var currentMode: InputMode = InputMode.SINGLISH
    private var isShifted: Boolean = false
    private val composingBuffer = StringBuilder()

    // Views
    private var rootView: View? = null
    private var candidatesLayout: LinearLayout? = null
    private var qwertyContainer: LinearLayout? = null
    private var symbolsContainer: LinearLayout? = null
    private var btnQuickLang: Button? = null
    private var btnQuickTheme: ImageButton? = null
    private var btnQuickSettings: ImageButton? = null
    private var keySpace: Button? = null
    private var keyShift: ImageButton? = null
    private var keyEnter: ImageButton? = null

    // Alphabet keys reference for shift updates
    private val alphabetButtons = mutableListOf<Button>()

    // Feedback
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    override fun onCreateInputView(): View {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.keyboard_view, null)
        rootView = view

        initViews(view)
        setupKeyListeners(view)
        setupSymbolsListeners(view)
        applyCurrentTheme()
        updateCandidateBar("", "")

        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        resetComposingState()
        applyCurrentTheme()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        resetComposingState()
    }

    private fun resetComposingState() {
        composingBuffer.clear()
        isShifted = false
        updateShiftState()
        updateCandidateBar("", "")
    }

    private fun initViews(view: View) {
        candidatesLayout = view.findViewById(R.id.candidates_layout)
        qwertyContainer = view.findViewById(R.id.qwerty_container)
        symbolsContainer = view.findViewById(R.id.symbols_container)
        btnQuickLang = view.findViewById(R.id.btn_quick_lang)
        btnQuickTheme = view.findViewById(R.id.btn_quick_theme)
        btnQuickSettings = view.findViewById(R.id.btn_quick_settings)
        keySpace = view.findViewById(R.id.key_space)
        keyShift = view.findViewById(R.id.key_shift)
        keyEnter = view.findViewById(R.id.key_enter)

        // Find all 26 alphabet buttons
        alphabetButtons.clear()
        val letterIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )

        for (id in letterIds) {
            val btn = view.findViewById<Button>(id)
            if (btn != null) {
                alphabetButtons.add(btn)
            }
        }
    }

    private fun setupKeyListeners(view: View) {
        // Alphabet key clicks
        for (btn in alphabetButtons) {
            btn.setOnClickListener {
                val letter = btn.text.toString()
                handleCharacterInput(letter)
            }
        }

        // Shift key
        keyShift?.setOnClickListener {
            provideFeedback()
            isShifted = !isShifted
            updateShiftState()
        }

        // Backspace key
        view.findViewById<ImageButton>(R.id.key_backspace)?.setOnClickListener {
            provideFeedback()
            handleBackspace()
        }

        // Space key
        keySpace?.setOnClickListener {
            provideFeedback()
            handleSpace()
        }

        // Enter key
        keyEnter?.setOnClickListener {
            provideFeedback()
            handleEnter()
        }

        // Comma and Period
        view.findViewById<Button>(R.id.key_comma)?.setOnClickListener {
            handleSpecialCharacter(",")
        }
        view.findViewById<Button>(R.id.key_period)?.setOnClickListener {
            handleSpecialCharacter(".")
        }

        // Mode switch (?123)
        view.findViewById<Button>(R.id.key_mode_switch)?.setOnClickListener {
            provideFeedback()
            showSymbols(true)
        }

        // Quick Language Toggle (Singlish / English)
        btnQuickLang?.setOnClickListener {
            provideFeedback()
            toggleInputMode()
        }

        // Quick Theme Toggle
        btnQuickTheme?.setOnClickListener {
            provideFeedback()
            cycleNextTheme()
        }

        // Quick Settings Launcher
        btnQuickSettings?.setOnClickListener {
            provideFeedback()
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        // Emoji key: inserts common polite emojis directly
        view.findViewById<ImageButton>(R.id.key_emoji)?.setOnClickListener {
            provideFeedback()
            commitComposingTextIfExists()
            showEmojiCandidates()
        }
    }

    private fun setupSymbolsListeners(view: View) {
        val symbolButtonIds = listOf(
            R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5,
            R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_0,
            R.id.sym_at, R.id.sym_hash, R.id.sym_dollar, R.id.sym_percent,
            R.id.sym_ampersand, R.id.sym_star, R.id.sym_minus, R.id.sym_plus,
            R.id.sym_paren_open, R.id.sym_paren_close, R.id.sym_equals,
            R.id.sym_slash, R.id.sym_colon, R.id.sym_semicolon,
            R.id.sym_exclamation, R.id.sym_question, R.id.sym_quote, R.id.sym_doublequote
        )

        for (id in symbolButtonIds) {
            val btn = view.findViewById<Button>(id)
            btn?.setOnClickListener {
                provideFeedback()
                val text = btn.text.toString()
                handleSpecialCharacter(text)
            }
        }

        // Return to ABC
        view.findViewById<Button>(R.id.sym_to_abc)?.setOnClickListener {
            provideFeedback()
            showSymbols(false)
        }

        // Symbols Space
        view.findViewById<Button>(R.id.sym_space)?.setOnClickListener {
            provideFeedback()
            handleSpace()
        }

        // Symbols Backspace
        view.findViewById<ImageButton>(R.id.sym_backspace)?.setOnClickListener {
            provideFeedback()
            handleBackspace()
        }

        // Symbols Enter
        view.findViewById<ImageButton>(R.id.sym_enter)?.setOnClickListener {
            provideFeedback()
            handleEnter()
        }
    }

    /**
     * Handles typing an alphabetic letter
     */
    private fun handleCharacterInput(char: String) {
        provideFeedback()
        val inputConn = currentInputConnection ?: return

        if (currentMode == InputMode.SINGLISH) {
            // Append to composing buffer for real-time transliteration
            composingBuffer.append(char)
            val rawSinglish = composingBuffer.toString()
            val transliterated = SinglishTransliterator.transliterate(rawSinglish)

            // Update composing text in target edit field
            inputConn.setComposingText(transliterated, 1)

            // Update suggestions
            updateCandidateBar(rawSinglish, transliterated)
        } else {
            // Direct English typing
            inputConn.commitText(char, 1)
            updateCandidateBar(char, "")
        }

        // Reset shift after single letter if shifted
        if (isShifted) {
            isShifted = false
            updateShiftState()
        }
    }

    /**
     * Handles backspace deletion
     */
    private fun handleBackspace() {
        val inputConn = currentInputConnection ?: return

        if (composingBuffer.isNotEmpty()) {
            // Delete last character from composing buffer
            composingBuffer.deleteCharAt(composingBuffer.length - 1)

            if (composingBuffer.isNotEmpty()) {
                val rawSinglish = composingBuffer.toString()
                val transliterated = SinglishTransliterator.transliterate(rawSinglish)
                inputConn.setComposingText(transliterated, 1)
                updateCandidateBar(rawSinglish, transliterated)
            } else {
                inputConn.setComposingText("", 0)
                inputConn.finishComposingText()
                updateCandidateBar("", "")
            }
        } else {
            // Standard backspace key event
            inputConn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            inputConn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            updateCandidateBar("", "")
        }
    }

    /**
     * Handles Space key
     */
    private fun handleSpace() {
        val inputConn = currentInputConnection ?: return

        if (composingBuffer.isNotEmpty()) {
            val transliterated = SinglishTransliterator.transliterate(composingBuffer.toString())
            inputConn.commitText("$transliterated ", 1)
            composingBuffer.clear()
        } else {
            inputConn.commitText(" ", 1)
        }

        updateCandidateBar("", "")
    }

    /**
     * Handles Enter key
     */
    private fun handleEnter() {
        val inputConn = currentInputConnection ?: return

        if (composingBuffer.isNotEmpty()) {
            val transliterated = SinglishTransliterator.transliterate(composingBuffer.toString())
            inputConn.commitText(transliterated, 1)
            composingBuffer.clear()
        }

        val editorInfo = currentInputEditorInfo
        if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                inputConn.performEditorAction(action)
                return
            }
        }

        // Default newline
        sendKeyChar('\n')
        updateCandidateBar("", "")
    }

    private fun handleSpecialCharacter(char: String) {
        provideFeedback()
        val inputConn = currentInputConnection ?: return

        if (composingBuffer.isNotEmpty()) {
            val transliterated = SinglishTransliterator.transliterate(composingBuffer.toString())
            inputConn.commitText(transliterated, 1)
            composingBuffer.clear()
        }

        inputConn.commitText(char, 1)
        updateCandidateBar("", "")
    }

    private fun commitComposingTextIfExists() {
        val inputConn = currentInputConnection ?: return
        if (composingBuffer.isNotEmpty()) {
            val transliterated = SinglishTransliterator.transliterate(composingBuffer.toString())
            inputConn.commitText(transliterated, 1)
            composingBuffer.clear()
            updateCandidateBar("", "")
        }
    }

    /**
     * Updates candidate / prediction bar with options
     */
    private fun updateCandidateBar(raw: String, transliterated: String) {
        val layout = candidatesLayout ?: return
        layout.removeAllViews()

        val theme = ThemeManager.getCurrentTheme(this)
        val colors = ThemeManager.getThemeColors(theme)

        val suggestions = if (raw.isNotEmpty() || transliterated.isNotEmpty()) {
            SinglishTransliterator.getPredictions(raw, transliterated)
        } else {
            // Default quick words
            listOf("මම", "ඔයා", "කොහොමද", "ස්තූතියි", "හරි", "සුබ දවසක්", "නෑ", "ඔව්")
        }

        for ((index, candidate) in suggestions.withIndex()) {
            val isPrimary = (index == 0 && (raw.isNotEmpty() || transliterated.isNotEmpty()))
            val chip = Button(this).apply {
                text = candidate
                textSize = 14f
                isAllCaps = false
                setPadding(dpToPx(12), 0, dpToPx(12), 0)
                minHeight = dpToPx(36)
                minWidth = dpToPx(48)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(36)
                ).apply {
                    setMargins(dpToPx(3), 0, dpToPx(3), 0)
                }

                if (isPrimary) {
                    setTextColor(colors.candidatePrimaryChipTextColor)
                    background = ThemeManager.createKeyDrawable(
                        colors.candidatePrimaryChipBackground,
                        cornerRadiusDp = 18f
                    )
                } else {
                    setTextColor(colors.candidateChipTextColor)
                    background = ThemeManager.createKeyDrawable(
                        colors.candidateChipBackground,
                        cornerRadiusDp = 18f
                    )
                }

                setOnClickListener {
                    provideFeedback()
                    onCandidateSelected(candidate)
                }
            }
            layout.addView(chip)
        }
    }

    private fun showEmojiCandidates() {
        val layout = candidatesLayout ?: return
        layout.removeAllViews()

        val theme = ThemeManager.getCurrentTheme(this)
        val colors = ThemeManager.getThemeColors(theme)
        val popularEmojis = listOf("😊", "❤️", "👍", "🙏", "😂", "🔥", "✨", "🎉", "💐", "🇱🇰", "💯", "👌")

        for (emoji in popularEmojis) {
            val chip = Button(this).apply {
                text = emoji
                textSize = 18f
                minHeight = dpToPx(36)
                minWidth = dpToPx(40)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(36)
                ).apply {
                    setMargins(dpToPx(2), 0, dpToPx(2), 0)
                }
                setTextColor(colors.candidateChipTextColor)
                background = ThemeManager.createKeyDrawable(colors.candidateChipBackground, cornerRadiusDp = 18f)

                setOnClickListener {
                    provideFeedback()
                    currentInputConnection?.commitText(emoji, 1)
                }
            }
            layout.addView(chip)
        }
    }

    private fun onCandidateSelected(candidate: String) {
        val inputConn = currentInputConnection ?: return
        // Commit selected candidate with a trailing space
        inputConn.commitText("$candidate ", 1)
        composingBuffer.clear()
        updateCandidateBar("", "")
    }

    private fun toggleInputMode() {
        currentMode = if (currentMode == InputMode.SINGLISH) {
            InputMode.ENGLISH
        } else {
            InputMode.SINGLISH
        }

        btnQuickLang?.text = if (currentMode == InputMode.SINGLISH) "සිංහල" else "ENG"
        keySpace?.text = if (currentMode == InputMode.SINGLISH) "Singlish (සිංහල)" else "English (QWERTY)"

        commitComposingTextIfExists()
    }

    private fun updateShiftState() {
        keyShift?.isSelected = isShifted
        for (btn in alphabetButtons) {
            val text = btn.text.toString()
            btn.text = if (isShifted) text.uppercase() else text.lowercase()
        }
    }

    private fun showSymbols(show: Boolean) {
        qwertyContainer?.visibility = if (show) View.GONE else View.VISIBLE
        symbolsContainer?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun cycleNextTheme() {
        val current = ThemeManager.getCurrentTheme(this)
        val allThemes = KeyboardTheme.values()
        val nextIndex = (current.ordinal + 1) % allThemes.size
        val nextTheme = allThemes[nextIndex]

        ThemeManager.setTheme(this, nextTheme)
        applyCurrentTheme()
    }

    /**
     * Applies the current active theme colors dynamically to all keyboard components
     */
    private fun applyCurrentTheme() {
        val view = rootView ?: return
        val theme = ThemeManager.getCurrentTheme(this)
        val colors = ThemeManager.getThemeColors(theme)

        // Root Background
        view.findViewById<LinearLayout>(R.id.keyboard_root)?.setBackgroundColor(colors.keyboardBackground)

        // Top Suggestion Bar
        view.findViewById<LinearLayout>(R.id.suggestion_bar_container)?.setBackgroundColor(colors.candidateBarBackground)
        view.findViewById<View>(R.id.keyboard_divider)?.setBackgroundColor(colors.dividerColor)

        // Quick action buttons
        btnQuickTheme?.background = ThemeManager.createKeyDrawable(colors.candidateChipBackground, 18f)
        btnQuickSettings?.background = ThemeManager.createKeyDrawable(colors.candidateChipBackground, 18f)

        // Alphabet buttons
        for (btn in alphabetButtons) {
            btn.setTextColor(colors.keyTextColor)
            btn.background = ThemeManager.createKeyDrawable(colors.keyBackground, 6f)
        }

        // Functional keys
        keyShift?.background = ThemeManager.createKeyDrawable(colors.keyActionBackground, 6f)
        view.findViewById<ImageButton>(R.id.key_backspace)?.background = ThemeManager.createKeyDrawable(colors.keyActionBackground, 6f)
        view.findViewById<Button>(R.id.key_mode_switch)?.apply {
            setTextColor(colors.keyActionTextColor)
            background = ThemeManager.createKeyDrawable(colors.keyActionBackground, 6f)
        }
        view.findViewById<ImageButton>(R.id.key_emoji)?.background = ThemeManager.createKeyDrawable(colors.keyActionBackground, 6f)

        // Comma, Space, Period
        view.findViewById<Button>(R.id.key_comma)?.apply {
            setTextColor(colors.keyTextColor)
            background = ThemeManager.createKeyDrawable(colors.keyBackground, 6f)
        }
        view.findViewById<Button>(R.id.key_period)?.apply {
            setTextColor(colors.keyTextColor)
            background = ThemeManager.createKeyDrawable(colors.keyBackground, 6f)
        }
        keySpace?.apply {
            setTextColor(colors.keySubTextColor)
            background = ThemeManager.createKeyDrawable(colors.keyBackground, 6f)
        }

        // Enter key
        keyEnter?.background = ThemeManager.createKeyDrawable(colors.keyPrimaryBackground, 6f)

        // Symbols layer styling
        styleSymbolsLayer(view, colors)

        // Refresh suggestion chips colors
        val raw = composingBuffer.toString()
        val trans = if (raw.isNotEmpty()) SinglishTransliterator.transliterate(raw) else ""
        updateCandidateBar(raw, trans)
    }

    private fun styleSymbolsLayer(view: View, colors: ThemeManager.ThemeColors) {
        val symKeyIds = listOf(
            R.id.sym_1, R.id.sym_2, R.id.sym_3, R.id.sym_4, R.id.sym_5,
            R.id.sym_6, R.id.sym_7, R.id.sym_8, R.id.sym_9, R.id.sym_0,
            R.id.sym_at, R.id.sym_hash, R.id.sym_dollar, R.id.sym_percent,
            R.id.sym_ampersand, R.id.sym_star, R.id.sym_minus, R.id.sym_plus,
            R.id.sym_paren_open, R.id.sym_paren_close, R.id.sym_equals,
            R.id.sym_slash, R.id.sym_colon, R.id.sym_semicolon,
            R.id.sym_exclamation, R.id.sym_question, R.id.sym_quote, R.id.sym_doublequote
        )

        for (id in symKeyIds) {
            view.findViewById<Button>(id)?.apply {
                setTextColor(colors.keyTextColor)
                background = ThemeManager.createKeyDrawable(colors.keyBackground, 6f)
            }
        }

        view.findViewById<Button>(R.id.sym_to_abc)?.apply {
            setTextColor(colors.keyActionTextColor)
            background = ThemeManager.createKeyDrawable(colors.keyActionBackground, 6f)
        }
        view.findViewById<Button>(R.id.sym_space)?.apply {
            setTextColor(colors.keySubTextColor)
            background = ThemeManager.createKeyDrawable(colors.keyBackground, 6f)
        }
        view.findViewById<ImageButton>(R.id.sym_backspace)?.background = ThemeManager.createKeyDrawable(colors.keyActionBackground, 6f)
        view.findViewById<ImageButton>(R.id.sym_enter)?.background = ThemeManager.createKeyDrawable(colors.keyPrimaryBackground, 6f)
    }

    private fun provideFeedback() {
        // Haptic feedback
        if (ThemeManager.isHapticEnabled(this)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(18)
                }
            } catch (_: Exception) {}
        }

        // Sound feedback
        if (ThemeManager.isSoundEnabled(this)) {
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.4f)
            } catch (_: Exception) {}
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
