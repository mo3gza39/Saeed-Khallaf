package com.example.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import java.util.Locale

class KeyboardService : InputMethodService() {

    private lateinit var keysContainer: LinearLayout
    private val alphabeticTextViews = mutableListOf<android.widget.TextView>()
    private val suggestionButtons = mutableListOf<Button>()
    private var shiftButtonView: android.widget.TextView? = null

    private var currentLang = 0 // 0 = English, 1 = Arabic, 2 = Symbols
    private var lastLanguage = 0 // Backup for returning from symbols
    private var isShifted = false

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var repeatingDeleteRunnable: Runnable? = null

    private val dictionary = listOf(
        // English words
        "glass", "glassmorphism", "frosted", "premium", "modern", "typing", 
        "futuristic", "ambient", "cosmic", "glow", "haptic", "responsive", 
        "elegant", "smooth", "android", "compose", "material", "design", 
        "animation", "ultra", "balanced", "interface", "workspace", "shimmer", 
        "refraction", "particle", "vibration", "tactile", "feedback", "glowy", 
        "beautiful", "gorgeous", "fantastic", "speedy", "fluid", "excellent",
        "interactive", "highly", "satisfying", "experience", "sunset", "plasma",
        "keyboard", "smart", "input", "layout", "text", "the", "and", "you", 
        "that", "was", "for", "on", "are", "with", "this", "they", "have",
        // Arabic words
        "السلام", "عليكم", "صباح", "الخير", "مرحبا", "جميل", "رائع", "لوحة", 
        "مفاتيح", "تصميم", "ذكي", "سريع", "ممتاز", "عربي", "تقنية", "كتابة",
        "شاشة", "المستقبل", "تفاعل", "برنامج", "اندرويد", "تطبيق", "سهل", "جدا",
        "الله", "الحمد", "شكرا", "جزيل", "مبارك", "موفق", "بخير", "اليوم"
    )

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        // Parent container: Vertical LinearLayout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(0xFF0D0E11.toInt()) // Sleek, graphite near-black cosmic backdrop
        }

        // Add suggestion row (Exactly 5 buttons placeholder/word bar)
        rootLayout.addView(buildSuggestionBar())

        // Separator (thin translucent line for glass partition)
        val separator = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                setMargins(0, 0, 0, 0)
            }
            setBackgroundColor(0x1BFFFFFF)
        }
        rootLayout.addView(separator)

        // Keys container
        keysContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 0)
            }
            setPadding(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(6))
        }
        rootLayout.addView(keysContainer)

        // Initialize state and populate view
        currentLang = 0
        lastLanguage = 0
        isShifted = false
        buildKeysArea()
        updateSuggestions()

        return rootLayout
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        isShifted = false
        updateSuggestions()
        updateKeyboardLabels()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun buildSuggestionBar(): LinearLayout {
        val rootBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(46)
            )
            setBackgroundColor(0xFF0E1116.toInt()) // Deep frosted graphite Suggestions row
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }

        // 1. Emoji Button on far left
        val emojiButton = Button(this).apply {
            text = "😊"
            textSize = 17f
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                    setColor(0x22FFFFFF)
                    cornerRadius = dpToPx(20).toFloat()
                })
                addState(intArrayOf(), GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                })
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(40)).apply {
                setMargins(dpToPx(2), 0, dpToPx(2), 0)
            }
            setOnClickListener {
                playHaptic(this)
                showEmojiPopup(this)
            }
        }
        rootBar.addView(emojiButton)

        // 2. Suggestions center bar
        val suggestionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            gravity = Gravity.CENTER_VERTICAL
        }

        suggestionButtons.clear()
        for (i in 0 until 5) {
            val btn = Button(this).apply {
                textSize = 13.5f
                setTextColor(0xFF96C0EB.toInt()) // Premium icy blue accent color for word suggestions
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                isAllCaps = false
                setPadding(dpToPx(4), 0, dpToPx(4), 0)
                background = StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                        setColor(0x22FFFFFF)
                        cornerRadius = dpToPx(6).toFloat()
                    })
                    addState(intArrayOf(), GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                    })
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            }
            suggestionsLayout.addView(btn)
            suggestionButtons.add(btn)
        }
        rootBar.addView(suggestionsLayout)

        // 3. Grid Settings / Tools Button on far right
        val gridButton = Button(this).apply {
            text = "⚙️"
            textSize = 17f
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                    setColor(0x22FFFFFF)
                    cornerRadius = dpToPx(20).toFloat()
                })
                addState(intArrayOf(), GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                })
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), dpToPx(40)).apply {
                setMargins(dpToPx(2), 0, dpToPx(2), 0)
            }
            setOnClickListener {
                playHaptic(this)
                showToolsPopup(this)
            }
        }
        rootBar.addView(gridButton)

        return rootBar
    }

    private fun buildKeysArea() {
        keysContainer.removeAllViews()
        alphabeticTextViews.clear()
        shiftButtonView = null

        // Key rows matching exact English, Arabic, and Symbols structures
        val keysRow1: List<String>
        val keysRow2: List<String>
        val keysRow3: List<String>

        when (currentLang) {
            0 -> { // English
                keysRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
                keysRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
                keysRow3 = listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "BACK")
            }
            1 -> { // Arabic
                keysRow1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج")
                keysRow2 = listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
                keysRow3 = listOf("SHIFT", "ذ", "ء", "ؤ", "ر", "ى", "ة", "و", "ز", "ظ", "د", "BACK")
            }
            else -> { // Symbols Page 1
                keysRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                keysRow2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
                keysRow3 = listOf("!", "\"", "'", ":", ";", "/", "?", "،", "BACK")
            }
        }

        // Add Number row for alphabet languages (Arabic or English)
        if (currentLang < 2) {
            val numRow = createRowLayout()
            val numKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            for (num in numKeys) {
                numRow.addView(createKeyView(num, 52, 1.0f))
            }
            keysContainer.addView(numRow)
        }

        // Row 1
        val row1View = createRowLayout()
        for (k in keysRow1) {
            row1View.addView(createKeyView(k, 52, 1.0f))
        }
        keysContainer.addView(row1View)

        // Row 2
        val row2View = createRowLayout()
        for (k in keysRow2) {
            row2View.addView(createKeyView(k, 52, 1.0f))
        }
        keysContainer.addView(row2View)

        // Row 3 (With larger shift & backspace actions)
        val row3View = createRowLayout()
        for (k in keysRow3) {
            val isAction = k == "SHIFT" || k == "BACK"
            val weight = if (isAction) {
                if (currentLang == 1) 1.25f else 1.4f
            } else {
                1.0f
            }
            row3View.addView(createKeyView(k, 52, weight, isAccent = isAction))
        }
        keysContainer.addView(row3View)

        // Row 5 / Bottom Control Row
        val bottomRowView = createRowLayout()

        if (currentLang == 1) { // Arabic Bottom Control Row
            // [?123] [punctuation] [globe/language] [العربية] [more/options] [Enter/Search]
            bottomRowView.addView(createKeyView("?123", 52, 1.3f, isAccent = true))
            bottomRowView.addView(createKeyView("،", 52, 1.0f))
            bottomRowView.addView(createKeyView("LANG", 52, 1.2f, isAccent = true))
            bottomRowView.addView(createKeyView("SPACE", 52, 4.8f))
            bottomRowView.addView(createKeyView("...", 52, 1.0f, isAccent = true))
            bottomRowView.addView(createKeyView("ENTER", 52, 1.5f, isAccent = true))
        } else if (currentLang == 0) { // English Bottom Control Row
            // [?123] [punctuation] [globe/language] [Space] [more/options] [Enter/Search]
            bottomRowView.addView(createKeyView("?123", 52, 1.3f, isAccent = true))
            bottomRowView.addView(createKeyView(",", 52, 1.0f))
            bottomRowView.addView(createKeyView("LANG", 52, 1.2f, isAccent = true))
            bottomRowView.addView(createKeyView("SPACE", 52, 4.8f))
            bottomRowView.addView(createKeyView("...", 52, 1.0f, isAccent = true))
            bottomRowView.addView(createKeyView("ENTER", 52, 1.5f, isAccent = true))
        } else { // Symbols Bottom Control Row
            // [ABC/عربي] [emoji] [space] [more] [Enter/Search]
            val abcLabel = if (lastLanguage == 1) "عربي" else "ABC"
            bottomRowView.addView(createKeyView(abcLabel, 52, 1.8f, isAccent = true))
            bottomRowView.addView(createKeyView("EMOJI_KEY", 52, 1.2f, isAccent = true))
            bottomRowView.addView(createKeyView("SPACE", 52, 5.0f))
            bottomRowView.addView(createKeyView("...", 52, 1.2f, isAccent = true))
            bottomRowView.addView(createKeyView("ENTER", 52, 1.8f, isAccent = true))
        }

        keysContainer.addView(bottomRowView)
    }

    private fun createRowLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun createKeyView(label: String, heightDp: Int, weight: Float, isAccent: Boolean = false): View {
        val container = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(heightDp), weight).apply {
                setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3))
            }
            background = createKeyDrawable(isAccent)
            isClickable = true
            isFocusable = true
        }

        // Main Text Label
        val mainTextView = android.widget.TextView(this).apply {
            id = View.generateViewId()
            textSize = 15.5f
            setTextColor(0xFFE5E7EB.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            
            val lp = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            layoutParams = lp
        }

        // Determine main and secondary labels
        var mainLabelText = label
        var secondaryLabelText = ""

        when (label) {
            "SHIFT" -> {
                shiftButtonView = mainTextView
                mainLabelText = if (isShifted) "↑" else "⇧"
            }
            "BACK" -> mainLabelText = "⌫"
            "SPACE" -> mainLabelText = if (currentLang == 1) "العربية" else "Space"
            "LANG" -> mainLabelText = if (currentLang == 1) "ENG" else "عربي"
            "EMOJI_KEY" -> mainLabelText = "😊"
            "ABC" -> mainLabelText = "ABC"
            "عربي" -> mainLabelText = "عربي"
            "ENTER" -> {
                val editorInfo = currentInputEditorInfo
                val action = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                mainLabelText = when (action) {
                    EditorInfo.IME_ACTION_SEARCH -> "Search"
                    EditorInfo.IME_ACTION_GO -> "Go"
                    EditorInfo.IME_ACTION_SEND -> "Send"
                    EditorInfo.IME_ACTION_NEXT -> "Next"
                    EditorInfo.IME_ACTION_DONE -> "Done"
                    else -> "Enter"
                }
            }
            else -> {
                val isSingleLetter = label.length == 1 && label[0].isLetter()
                if (isSingleLetter) {
                    container.tag = label
                    alphabeticTextViews.add(mainTextView)
                    mainLabelText = if (isShifted) label.uppercase(Locale.getDefault()) else label.lowercase(Locale.getDefault())
                    
                    val alternateList = getAlternatesFor(label)
                    if (alternateList.isNotEmpty()) {
                        secondaryLabelText = alternateList.getOrNull(0) ?: ""
                    }
                } else {
                    mainLabelText = label
                }
            }
        }

        // Setup numbers secondary symbol
        if (label.length == 1 && label[0].isDigit()) {
            val numAltMap = mapOf(
                '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%',
                '6' to '^', '7' to '&', '8' to '*', '9' to '(', '0' to ')'
            )
            numAltMap[label[0]]?.let {
                secondaryLabelText = it.toString()
            }
        }

        // Safe setup for custom alternate displays
        if (currentLang == 1) { // Arabic letter secondary symbols
            when (label) {
                "ا" -> secondaryLabelText = "أ"
                "و" -> secondaryLabelText = "ؤ"
                "ي" -> secondaryLabelText = "ئ"
                "ه" -> secondaryLabelText = "ة"
                "لا" -> secondaryLabelText = "لأ"
            }
        }

        mainTextView.text = mainLabelText
        container.addView(mainTextView)

        // Add Secondary Label View in Top Right if matching secondary symbols rule
        if (secondaryLabelText.isNotEmpty()) {
            val secTextView = android.widget.TextView(this).apply {
                text = secondaryLabelText
                textSize = 8.5f
                setTextColor(0x99A5C5E8.toInt()) // Sleek translucent icy blue
                typeface = Typeface.MONOSPACE
                
                val lp = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    setMargins(0, dpToPx(3), dpToPx(4), 0)
                }
                layoutParams = lp
            }
            container.addView(secTextView)
        }

        // Backspace repeating delete touch, otherwise standard click listener
        if (label == "BACK") {
            container.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        playHaptic(v)
                        handleBackKeyPress()
                        // Schedule delayed repetition
                        handler.postDelayed({
                            if (repeatingDeleteRunnable == null) {
                                startRepeatingDelete()
                            }
                        }, 400)
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        stopRepeatingDelete()
                    }
                }
                true
            }
        } else {
            container.setOnClickListener {
                playHaptic(container)
                handleKeyInput(label)
            }

            // Safe Long Press Alternate Popup
            val alternates = getAlternatesFor(label)
            val isDigit = label.length == 1 && label[0].isDigit()
            if (alternates.isNotEmpty() || isDigit) {
                container.setOnLongClickListener {
                    playHaptic(container)
                    handleLongPress(container, label)
                    true
                }
            }
        }

        return container
    }

    private fun createKeyBackground(isPressedState: Boolean, isAccent: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(8).toFloat()
            
            // Subtle premium glassmorphic frosted fill color inside dark theme
            val fillColor = when {
                isAccent -> if (isPressedState) 0x6650637A.toInt() else 0x3350637A.toInt()
                isPressedState -> 0x44FFFFFF.toInt() // Transparent white highlight
                else -> 0x1AFFFFFF.toInt() // Premium translucent glass frosted white
            }
            setColor(fillColor)
            
            // Ultra elegant, clear thin translucent glowing border
            val strokeColor = if (isAccent || isPressedState) 0x7B96C0EB.toInt() else 0x3DFFFFFF.toInt()
            setStroke(dpToPx(1), strokeColor)
        }
    }

    private fun createKeyDrawable(isAccent: Boolean = false): StateListDrawable {
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), createKeyBackground(true, isAccent))
            addState(intArrayOf(), createKeyBackground(false, isAccent))
        }
    }

    private fun handleKeyInput(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "BACK" -> {
                handleBackKeyPress()
            }
            "SPACE" -> {
                ic.commitText(" ", 1)
                if (isShifted) {
                    isShifted = false
                    updateKeyboardLabels()
                }
                updateSuggestions()
            }
            "SHIFT" -> {
                isShifted = !isShifted
                updateKeyboardLabels()
            }
            "LANG" -> {
                // Toggle English (0) and Arabic (1)
                currentLang = if (currentLang == 1) 0 else 1
                lastLanguage = currentLang
                buildKeysArea()
                updateSuggestions()
            }
            "?123" -> {
                currentLang = 2
                buildKeysArea()
                updateSuggestions()
            }
            "ABC", "عربي", "ABC_LANG" -> {
                currentLang = lastLanguage
                buildKeysArea()
                updateSuggestions()
            }
            "EMOJI_KEY" -> {
                // Managed in onClick directly normally, added fallback
            }
            "ENTER" -> {
                val editorInfo = currentInputEditorInfo
                if (editorInfo != null) {
                    val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                    if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                        ic.performEditorAction(action)
                    } else {
                        ic.commitText("\n", 1)
                    }
                } else {
                    ic.commitText("\n", 1)
                }
            }
            else -> {
                val charToCommit = if (isShifted) {
                    key.uppercase(Locale.getDefault())
                } else {
                    key.lowercase(Locale.getDefault())
                }
                ic.commitText(charToCommit, 1)

                if (isShifted) {
                    isShifted = false
                    updateKeyboardLabels()
                }
                updateSuggestions()
            }
        }
    }

    private fun handleBackKeyPress() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
        updateSuggestions()
    }

    private fun startRepeatingDelete() {
        repeatingDeleteRunnable = object : Runnable {
            override fun run() {
                handleBackKeyPress()
                playHaptic(keysContainer)
                handler.postDelayed(this, 100) // delete every 100ms
            }
        }
        handler.post(repeatingDeleteRunnable!!)
    }

    private fun stopRepeatingDelete() {
        repeatingDeleteRunnable?.let {
            handler.removeCallbacks(it)
            repeatingDeleteRunnable = null
        }
    }

    private fun updateKeyboardLabels() {
        try {
            for (tv in alphabeticTextViews) {
                val label = tv.parent?.let { (it as View).tag as? String } ?: continue
                tv.text = if (isShifted) label.uppercase(Locale.getDefault()) else label.lowercase(Locale.getDefault())
            }
            shiftButtonView?.text = if (isShifted) "↑" else "⇧"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSuggestions() {
        try {
            if (suggestionButtons.isEmpty()) return

            val ic = currentInputConnection ?: return
            val textBefore = ic.getTextBeforeCursor(50, 0) ?: ""
            val lastWord = textBefore.split(" ", "\n").lastOrNull()?.toString()?.lowercase(Locale.getDefault()) ?: ""

            val fallbackArabic = listOf("أنا", "في", "من", "على", "مش")
            val fallbackEnglish = listOf("I", "the", "to", "and", "you")
            val fallback = if (currentLang == 1) fallbackArabic else fallbackEnglish

            val predicted = if (lastWord.isEmpty()) {
                emptyList()
            } else {
                dictionary.filter { it.startsWith(lastWord) && it != lastWord }
                    .take(5)
            }

            val finalSuggestions = (predicted + fallback)
                .filter { it.isNotBlank() }
                .distinct()
                .take(5)

            for (i in 0 until suggestionButtons.size) {
                val btn = suggestionButtons.getOrNull(i) ?: continue
                val word = finalSuggestions.getOrNull(i) ?: ""
                btn.text = word
                btn.setOnClickListener {
                    if (word.isNotEmpty()) {
                        playHaptic(btn)
                        applySuggestion(word)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySuggestion(suggestion: String) {
        val ic = currentInputConnection ?: return
        val textBeforeCursor = ic.getTextBeforeCursor(50, 0) ?: ""
        val lastWord = textBeforeCursor.split(" ", "\n").lastOrNull() ?: ""
        if (lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }
        ic.commitText("$suggestion ", 1)
        updateSuggestions()
    }

    private fun getAlternatesFor(key: String): List<String> {
        val lowerKey = key.lowercase(Locale.getDefault())
        return when (lowerKey) {
            "a" -> listOf("á", "à", "â", "ä", "æ")
            "e" -> listOf("é", "è", "ê", "ë")
            "i" -> listOf("í", "ì", "î", "ï")
            "o" -> listOf("ó", "ò", "ô", "ö")
            "u" -> listOf("ú", "ù", "û", "ü")
            "c" -> listOf("ç")
            "n" -> listOf("ñ")
            "s" -> listOf("ß")
            "y" -> listOf("ý", "ÿ")
            "." -> listOf(",", "?", "!", ":", ";")
            "-" -> listOf("_", "/", "\\")
            
            // Arabic alternates
            "ا" -> listOf("أ", "إ", "آ", "ٱ")
            "ل" -> listOf("لا", "لأ", "لإ", "لآ")
            "لا" -> listOf("لأ", "لإ", "لآ")
            "ه" -> listOf("ة")
            "ي" -> listOf("ى", "ئ")
            "ى" -> listOf("ي", "ئ")
            "و" -> listOf("ؤ")
            "ء" -> listOf("أ", "إ", "ؤ", "ئ")
            "ر" -> listOf("ز")
            "د" -> listOf("ذ")
            "س" -> listOf("ش")
            "ص" -> listOf("ض")
            "ط" -> listOf("ظ")
            "ع" -> listOf("غ")
            "ح" -> listOf("خ")
            "ق" -> listOf("ڤ")
            "ف" -> listOf("ڤ")
            "ك" -> listOf("گ")
            "ج" -> listOf("چ")
            "ب" -> listOf("پ")
            "ز" -> listOf("ژ")
            
            // Punctuation alternates (contains diacritics as well!)
            "،" -> listOf("،", "؛", "؟", "!", ":", ".", "َ", "ِ", "ُ", "ّ", "ْ", "ً", "ٍ", "ٌ")
            "," -> listOf(",", "?", "!", ":", ";")
            
            // Numbers
            "1" -> listOf("!")
            "2" -> listOf("@")
            "3" -> listOf("#")
            "4" -> listOf("$")
            "5" -> listOf("%")
            "6" -> listOf("^")
            "7" -> listOf("&")
            "8" -> listOf("*")
            "9" -> listOf("(")
            "0" -> listOf(")")
            
            else -> emptyList()
        }
    }

    private fun handleLongPress(anchorView: View, mainKey: String) {
        try {
            val alternates = getAlternatesFor(mainKey)
            if (alternates.isEmpty()) return

            val popupContent = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                
                // Premium glassmorphic backdrop for the alternate key caps
                background = GradientDrawable().apply {
                    setColor(0xEB13171E.toInt()) // Frosted deep gray
                    cornerRadius = dpToPx(10).toFloat()
                    setStroke(dpToPx(1), 0x8096C0EB.toInt()) // Soft glowing icy blue outline
                }
            }

            val popupWindow = android.widget.PopupWindow(
                popupContent,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(GradientDrawable().apply { setColor(Color.TRANSPARENT) })
            }

            for (alt in alternates) {
                val altButton = Button(this).apply {
                    text = alt
                    textSize = 15f
                    setTextColor(0xFFFFFFFF.toInt())
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                    gravity = Gravity.CENTER
                    isAllCaps = false
                    
                    background = StateListDrawable().apply {
                        addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                            setColor(0x44FFFFFF)
                            cornerRadius = dpToPx(6).toFloat()
                        })
                        addState(intArrayOf(), GradientDrawable().apply {
                            setColor(0x22FFFFFF)
                            cornerRadius = dpToPx(6).toFloat()
                        })
                    }
                    
                    layoutParams = LinearLayout.LayoutParams(
                        dpToPx(40), dpToPx(40)
                    ).apply {
                        setMargins(dpToPx(2), 0, dpToPx(2), 0)
                    }

                    setOnClickListener {
                        playHaptic(this)
                        currentInputConnection?.commitText(alt, 1)
                        updateSuggestions()
                        popupWindow.dismiss()
                    }
                }
                popupContent.addView(altButton)
            }

            popupContent.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupWidth = popupContent.measuredWidth
            val popupHeight = popupContent.measuredHeight

            val location = IntArray(2)
            anchorView.getLocationInWindow(location)
            val x = location[0] + (anchorView.width - popupWidth) / 2
            val y = location[1] - popupHeight - dpToPx(10)

            popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showEmojiPopup(anchorView: View) {
        try {
            val emojis = listOf("😊", "😂", "🔥", "👍", "❤️", "🙌", "🎉", "✨", "🤔", "😢", "🌟", "🤣")
            val popupContent = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
                background = GradientDrawable().apply {
                    setColor(0xEB13171E.toInt())
                    cornerRadius = dpToPx(10).toFloat()
                    setStroke(dpToPx(1), 0x8096C0EB.toInt())
                }
            }

            val popupWindow = android.widget.PopupWindow(
                popupContent,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(GradientDrawable().apply { setColor(Color.TRANSPARENT) })
            }

            for (emoji in emojis) {
                val btn = Button(this).apply {
                    text = emoji
                    textSize = 15.5f
                    setPadding(0, 0, 0, 0)
                    gravity = Gravity.CENTER
                    background = StateListDrawable().apply {
                        addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                            setColor(0x44FFFFFF)
                            cornerRadius = dpToPx(6).toFloat()
                        })
                        addState(intArrayOf(), GradientDrawable().apply {
                            setColor(Color.TRANSPARENT)
                        })
                    }
                    layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)).apply {
                        setMargins(dpToPx(2), 0, dpToPx(2), 0)
                    }
                    setOnClickListener {
                        playHaptic(this)
                        currentInputConnection?.commitText(emoji, 1)
                        popupWindow.dismiss()
                    }
                }
                popupContent.addView(btn)
            }

            popupContent.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupWidth = popupContent.measuredWidth
            val popupHeight = popupContent.measuredHeight

            val location = IntArray(2)
            anchorView.getLocationInWindow(location)
            val x = location[0]
            val y = location[1] - popupHeight - dpToPx(8)

            popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showToolsPopup(anchorView: View) {
        try {
            val popupContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                background = GradientDrawable().apply {
                    setColor(0xEB13171E.toInt())
                    cornerRadius = dpToPx(10).toFloat()
                    setStroke(dpToPx(1), 0x8096C0EB.toInt())
                }
            }

            val popupWindow = android.widget.PopupWindow(
                popupContent,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(GradientDrawable().apply { setColor(Color.TRANSPARENT) })
            }

            val options = listOf(
                "Clear Field" to {
                    val ic = currentInputConnection
                    ic?.performContextMenuAction(android.R.id.selectAll)
                    ic?.commitText("", 1)
                },
                "Vitreous Settings" to {
                    val intent = android.content.Intent(this, com.example.MainActivity::class.java).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            )

            for ((title, action) in options) {
                val btn = Button(this).apply {
                    text = title
                    textSize = 13f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(12), 0, dpToPx(12), 0)
                    isAllCaps = false
                    
                    background = StateListDrawable().apply {
                        addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply {
                            setColor(0x3396C0EB.toInt())
                            cornerRadius = dpToPx(6).toFloat()
                        })
                        addState(intArrayOf(), GradientDrawable().apply {
                            setColor(Color.TRANSPARENT)
                        })
                    }
                    layoutParams = LinearLayout.LayoutParams(dpToPx(140), dpToPx(38)).apply {
                        setMargins(0, dpToPx(2), 0, dpToPx(2))
                    }
                    setOnClickListener {
                        playHaptic(this)
                        action()
                        popupWindow.dismiss()
                    }
                }
                popupContent.addView(btn)
            }

            popupContent.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupWidth = popupContent.measuredWidth
            val popupHeight = popupContent.measuredHeight

            val location = IntArray(2)
            anchorView.getLocationInWindow(location)
            val x = location[0] + anchorView.width - popupWidth
            val y = location[1] - popupHeight - dpToPx(8)

            popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playHaptic(view: View) {
        try {
            val vib = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(12)
                }
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (e: Exception) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}
