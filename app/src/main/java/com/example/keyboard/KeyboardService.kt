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
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import java.util.Locale

class KeyboardService : InputMethodService() {

    private lateinit var keysContainer: LinearLayout
    private val alphabeticButtons = mutableListOf<Button>()
    private val suggestionButtons = mutableListOf<Button>()
    private var shiftButton: Button? = null

    private var currentLang = 0 // 0 = English, 1 = Arabic, 2 = Symbols
    private var isShifted = false

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
            setBackgroundColor(0xFF121417.toInt()) // Cosmic dark slate background
        }

        // Add suggestion row (Exactly 5 buttons placeholder/word bar)
        rootLayout.addView(buildSuggestionBar())

        // Separator (thin translucent line for glass-like partition)
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
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            )
            setBackgroundColor(0xCC0B0C0E.toInt()) // Sleek translucent suggestions bar
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }

        suggestionButtons.clear()
        for (i in 0 until 5) {
            val btn = Button(this).apply {
                textSize = 13.5f
                setTextColor(0xFFA5C5E8.toInt()) // Sophisticated icy blue accent for predictions
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                isAllCaps = false
                setPadding(dpToPx(4), 0, dpToPx(4), 0)
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            }
            bar.addView(btn)
            suggestionButtons.add(btn)
        }
        return bar
    }

    private fun buildKeysArea() {
        keysContainer.removeAllViews()
        alphabeticButtons.clear()
        shiftButton = null

        // Key rows matching exact English and Arabic keyboard layouts
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
                keysRow1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د")
                keysRow2 = listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
                keysRow3 = listOf("SHIFT", "ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ", "BACK")
            }
            else -> { // Symbols
                keysRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                keysRow2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
                keysRow3 = listOf("/", "\\", "=", "<", ">", "?", "!", "BACK")
            }
        }

        // Add Number row for alphabet languages (Arabic or English)
        if (currentLang < 2) {
            val numRow = createRowLayout()
            val numKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            for (num in numKeys) {
                numRow.addView(createKeyButton(num, 44, 1.0f))
            }
            keysContainer.addView(numRow)
        }

        // Row 1
        val row1View = createRowLayout()
        for (k in keysRow1) {
            row1View.addView(createKeyButton(k, 52, 1.0f))
        }
        keysContainer.addView(row1View)

        // Row 2
        val row2View = createRowLayout()
        for (k in keysRow2) {
            row2View.addView(createKeyButton(k, 52, 1.0f))
        }
        keysContainer.addView(row2View)

        // Row 3 (With larger shift & backspace actions)
        val row3View = createRowLayout()
        for (k in keysRow3) {
            val isAction = k == "SHIFT" || k == "BACK"
            val weight = if (isAction) {
                if (currentLang == 1) 1.4f else 1.5f
            } else {
                1.0f
            }
            row3View.addView(createKeyButton(k, 52, weight, isAccent = isAction))
        }
        keysContainer.addView(row3View)

        // Row 5 (Bottom Row - Dynamic weights for perfect spatial alignment)
        val bottomRowWeights = when (currentLang) {
            1 -> listOf(1.5f, 1.5f, 5.5f, 1.2f, 1.6f) // Arabic weights
            else -> listOf(1.3f, 1.3f, 4.6f, 1.2f, 1.6f) // English/Symbols weights
        }

        val bottomRowView = createRowLayout()

        // 1. Language Toggle
        val btnLang = createKeyButton("LANG", 52, bottomRowWeights[0], isAccent = true)
        bottomRowView.addView(btnLang)

        // 2. Symbols toggle (123 or ABC)
        val symLabel = if (currentLang == 2) "ABC" else "123"
        val btnSym = createKeyButton(symLabel, 52, bottomRowWeights[1], isAccent = true)
        bottomRowView.addView(btnSym)

        // 3. Spacebar
        val btnSpace = createKeyButton("SPACE", 52, bottomRowWeights[2])
        bottomRowView.addView(btnSpace)

        // 4. Dot
        val btnDot = createKeyButton(".", 52, bottomRowWeights[3])
        bottomRowView.addView(btnDot)

        // 5. Enter Key
        val btnEnter = createKeyButton("ENTER", 52, bottomRowWeights[4], isAccent = true)
        bottomRowView.addView(btnEnter)

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

    private fun createKeyButton(label: String, heightDp: Int, weight: Float, isAccent: Boolean = false): Button {
        val btn = Button(this).apply {
            textSize = 15.5f
            setTextColor(0xFFE5E7EB.toInt()) // Crisp gray/white text
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 0)
            gravity = Gravity.CENTER
            isAllCaps = false
            background = createKeyDrawable(isAccent)
            
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(heightDp), weight).apply {
                setMargins(dpToPx(1), dpToPx(3), dpToPx(1), dpToPx(3)) // Compact edge-to-edge layout spacing
            }
        }

        // Apply customized labels
        when (label) {
            "SHIFT" -> {
                shiftButton = btn
                btn.text = if (isShifted) "↑" else "⇧"
            }
            "BACK" -> btn.text = "⌫"
            "SPACE" -> btn.text = "Space"
            "LANG" -> btn.text = if (currentLang == 1) "ENG" else "عرب"
            else -> {
                val isSingleLetter = label.length == 1 && label[0].isLetter()
                if (isSingleLetter) {
                    btn.tag = label
                    alphabeticButtons.add(btn)
                    btn.text = if (isShifted) label.uppercase(Locale.getDefault()) else label.lowercase(Locale.getDefault())
                } else {
                    btn.text = label
                }
            }
        }

        btn.setOnClickListener {
            playHaptic(btn)
            handleKeyInput(label)
        }

        return btn
    }

    private fun createKeyBackground(isPressedState: Boolean, isAccent: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(8).toFloat()
            
            // Subtle premium glassmorphic frosted fill color inside dark theme
            val fillColor = when {
                isAccent -> if (isPressedState) 0x994E5564.toInt() else 0x4D4E5564.toInt()
                isPressedState -> 0x734E5564.toInt() // Pressed Highlight
                else -> 0x3B2C303B.toInt() // Standard background
            }
            setColor(fillColor)
            
            // Ultra elegant, thin translucent glowing border
            val strokeColor = if (isAccent || isPressedState) 0x60B3CADB.toInt() else 0x24FFFFFF.toInt()
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
                val selectedText = ic.getSelectedText(0)
                if (selectedText.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
                updateSuggestions()
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
                // Toggle between English (0) and Arabic (1)
                currentLang = if (currentLang == 1) 0 else 1
                buildKeysArea()
                updateSuggestions()
            }
            "123" -> {
                currentLang = 2
                buildKeysArea()
                updateSuggestions()
            }
            "ABC" -> {
                currentLang = 0
                buildKeysArea()
                updateSuggestions()
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

    private fun updateKeyboardLabels() {
        for (btn in alphabeticButtons) {
            val label = btn.tag as? String ?: continue
            btn.text = if (isShifted) label.uppercase(Locale.getDefault()) else label.lowercase(Locale.getDefault())
        }
        shiftButton?.text = if (isShifted) "↑" else "⇧"
    }

    private fun updateSuggestions() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(50, 0) ?: ""
        val lastWord = textBefore.split(" ", "\n").lastOrNull()?.toString()?.lowercase(Locale.getDefault()) ?: ""

        val wordsList = if (lastWord.isEmpty()) {
            listOf("glass", "ambient", "premium", "typing", "smooth")
        } else {
            val filtered = dictionary.filter { it.startsWith(lastWord) && it != lastWord }
                .take(5)
            if (filtered.size < 5) {
                val remain = 5 - filtered.size
                val fallback = dictionary.filter { !filtered.contains(it) && it != lastWord }
                    .shuffled()
                    .take(remain)
                (filtered + fallback).take(5)
            } else {
                filtered
            }
        }

        for (i in 0 until 5) {
            val word = wordsList.getOrElse(i) { "" }
            val btn = suggestionButtons[i]
            btn.text = word
            btn.setOnClickListener {
                if (word.isNotEmpty()) {
                    playHaptic(btn)
                    applySuggestion(word)
                }
            }
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
