package com.example.keyboard

import android.app.Application
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class AnimationIntensity(val displayName: String) {
    LOW("Low"),
    BALANCED("Balanced"),
    HIGH("High"),
    ULTRA("Ultra")
}

enum class LanguagePreset(val displayName: String) {
    ENGLISH("ENG"),
    ARABIC("عرب"),
    NUMBERS_SYMBOLS("123"),
    EMOJIS("😀")
}

data class GlassTheme(
    val name: String,
    val displayName: String,
    val boardBackground: Color,
    val keyBackground: Color,
    val keyPressedBackground: Color,
    val borderNormal: Color,
    val borderPressed: Color,
    val textColor: Color,
    val accentColor: Color,
    val glowColor: Color,
    val backingColor1: Color,
    val backingColor2: Color,
    val isDark: Boolean = true
)

object PresetThemes {
    val Cosmic = GlassTheme(
        name = "COSMIC",
        displayName = "Graphite Slate",
        boardBackground = Color(0xFF121417),       // Smooth Slate Black
        keyBackground = Color(0x3B2C303B),         // Premium translucent frosted dark gray Gboard style
        keyPressedBackground = Color(0x734E5564),  // Highlight glass press gray
        borderNormal = Color(0x28FFFFFF),          // Subtly visible translucent light border
        borderPressed = Color(0x80B3CADB),         // Clean subtle icy/silver highlight border on press
        textColor = Color(0xFFE5E7EB),             // Safe, high-contrast crisp display white-grey
        accentColor = Color(0xFFA5C5E8),           // Sophisticated icy blue accent
        glowColor = Color(0x05FFFFFF),             // Minor white backlight reflection (no neon-bleeding glow)
        backingColor1 = Color(0xFF121417),         // Matches clean dark board background
        backingColor2 = Color(0xFF121417)
    )

    val AquaGlass = GlassTheme(
        name = "AQUA_GLASS",
        displayName = "Frosted Obsidian",
        boardBackground = Color(0xFF0F1115),       // Elegant deep obsidian
        keyBackground = Color(0x362C303B),         // Translucent charcoal grey
        keyPressedBackground = Color(0x614E5564),  // Saturated charcoal grey
        borderNormal = Color(0x2E8FA3B5),          // Subtle ice metal border
        borderPressed = Color(0x80B3CADB),         // Sleek metallic silver/blue
        textColor = Color(0xFFE2E8F0),             // Bright clear text
        accentColor = Color(0xFFA5C5E8),           // Sleek icy steel blue
        glowColor = Color(0x05FFFFFF),
        backingColor1 = Color(0xFF0F1115),
        backingColor2 = Color(0xFF0F1115)
    )

    val SunsetGlow = GlassTheme(
        name = "SUNSET_GLOW",
        displayName = "Bronze Ranger",
        boardBackground = Color(0xFF161412),       // Warm elegant dark granite
        keyBackground = Color(0x3B3D352E),         // Frosted warm charcoal
        keyPressedBackground = Color(0x6E524B43),
        borderNormal = Color(0x24FFF2E6),          // Subtle warm silver-gold border tint
        borderPressed = Color(0x70FFF2E6),
        textColor = Color(0xFFF7F2EC),
        accentColor = Color(0xFFEAD7C3),           // Warm brass accent
        glowColor = Color(0x05FFFFFF),
        backingColor1 = Color(0xFF161412),
        backingColor2 = Color(0xFF161412)
    )

    val EmeraldMint = GlassTheme(
        name = "EMERALD",
        displayName = "Sage Jade",
        boardBackground = Color(0xFF101412),       // Quiet desaturated sage graphite
        keyBackground = Color(0x3B2E3531),         // Sage charcoal key surfaces
        keyPressedBackground = Color(0x6E434C47),
        borderNormal = Color(0x28A7F3D0),          // Delicate sage border
        borderPressed = Color(0x60A7F3D0),
        textColor = Color(0xFFECFDF5),
        accentColor = Color(0xFFA7F3D0),           // Pastel sage mint accent
        glowColor = Color(0x05FFFFFF),
        backingColor1 = Color(0xFF101412),
        backingColor2 = Color(0xFF101412)
    )

    val Platinum = GlassTheme(
        name = "PLATINUM",
        displayName = "Steel Platinum",
        boardBackground = Color(0xFF18181B),       // Minimal deep silver-black
        keyBackground = Color(0x3B484848),         // Sandblasted silver glass keys
        keyPressedBackground = Color(0x6E5B5B5B),
        borderNormal = Color(0x2DFFFFFF),
        borderPressed = Color(0x70FFFFFF),
        textColor = Color(0xFFFAFAFA),
        accentColor = Color(0xFFFFFFFF),           // Solid white/platinum accent
        glowColor = Color(0x05FFFFFF),
        backingColor1 = Color(0xFF18181B),
        backingColor2 = Color(0xFF18181B)
    )

    val list = listOf(Cosmic, AquaGlass, SunsetGlow, EmeraldMint, Platinum)

    fun getByName(name: String): GlassTheme {
        return list.find { it.name == name } ?: Cosmic
    }
}

interface ImeListener {
    fun onCommitText(text: String)
    fun onDeleteBackward()
    fun onReplaceLastWord(suggestion: String)
}

class KeyboardViewModel(application: Application) : AndroidViewModel(application) {
    var imeListener: ImeListener? = null
    // Disable Room database initialization entirely to prevent any potential service crashes
    // private val db = KeyboardDatabase.getDatabase(application)
    // private val repository = KeyboardRepository(db.keyboardDao())

    // In-memory Notepad storage
    private val _savedNotes = MutableStateFlow<List<SavedNote>>(
        listOf(
            SavedNote(id = 1, content = "Welcome to the Glass Keyboard!"),
            SavedNote(id = 2, content = "Tap key characters above to compose notes.")
        )
    )
    val savedNotes: StateFlow<List<SavedNote>> = _savedNotes.asStateFlow()

    // Flowing States
    private val _theme = MutableStateFlow(PresetThemes.Cosmic)
    val theme: StateFlow<GlassTheme> = _theme.asStateFlow()

    private val _intensity = MutableStateFlow(AnimationIntensity.BALANCED)
    val intensity: StateFlow<AnimationIntensity> = _intensity.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _hapticWeight = MutableStateFlow(0.6f)
    val hapticWeight: StateFlow<Float> = _hapticWeight.asStateFlow()

    private val _boardScale = MutableStateFlow(1.0f)
    val boardScale: StateFlow<Float> = _boardScale.asStateFlow()

    private val _glowIntensity = MutableStateFlow(0.6f)
    val glowIntensity: StateFlow<Float> = _glowIntensity.asStateFlow()

    private val _borderThickness = MutableStateFlow(1.2f)
    val borderThickness: StateFlow<Float> = _borderThickness.asStateFlow()

    private val _cornerRadius = MutableStateFlow(12)
    val cornerRadius: StateFlow<Int> = _cornerRadius.asStateFlow()

    private val _backlightsSpeed = MutableStateFlow(1.2f)
    val backlightsSpeed: StateFlow<Float> = _backlightsSpeed.asStateFlow()

    private val _particlesCount = MutableStateFlow(12)
    val particlesCount: StateFlow<Int> = _particlesCount.asStateFlow()

    // Editor Text Entry State
    private val _typedText = MutableStateFlow("Tap here to start typing with glass feedback...")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    val selectionStart = MutableStateFlow(0)
    val selectionEnd = MutableStateFlow(0)

    // Layout configuration
    private val _currentLanguage = MutableStateFlow(LanguagePreset.ENGLISH)
    val currentLanguage: StateFlow<LanguagePreset> = _currentLanguage.asStateFlow()

    private val _shiftActive = MutableStateFlow(false)
    val shiftActive: StateFlow<Boolean> = _shiftActive.asStateFlow()

    private val _capsLock = MutableStateFlow(false)
    val capsLock: StateFlow<Boolean> = _capsLock.asStateFlow()

    val isImeMode = MutableStateFlow(false)

    private val _suggestions = MutableStateFlow<List<String>>(listOf("glass", "premium", "smart", "keyboard", "arabic"))
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isKeyboardVisible = MutableStateFlow(true)
    val isKeyboardVisible: StateFlow<Boolean> = _isKeyboardVisible.asStateFlow()

    // Trigger local animation feedback logs
    private val _lastPressedKey = MutableStateFlow<String?>(null)
    val lastPressedKey: StateFlow<String?> = _lastPressedKey.asStateFlow()

    private val _keystrokeCount = MutableStateFlow(0)
    val keystrokeCount: StateFlow<Int> = _keystrokeCount.asStateFlow()

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

    init {
        // Disabled Room settings initialization to prevent disk/thread crashes in IME service
    }

    fun saveCurrentSettings() {
        // No-op (Room database disabled to resolve crashes on start)
    }

    fun onKeyPress(key: String, view: View) {
        // Trigger haptics
        triggerHapticResponse(view)

        _lastPressedKey.value = key
        _keystrokeCount.value += 1

        val current = _typedText.value
        // Placeholder clear on first genuine action
        val base = if (current == "Tap here to start typing with glass feedback...") "" else current

        when (key) {
            "BACK" -> {
                if (base.isNotEmpty()) {
                    _typedText.value = base.dropLast(1)
                }
                imeListener?.onDeleteBackward()
            }
            "SPACE" -> {
                _typedText.value = "$base "
                _shiftActive.value = false // Auto lowercase after space standard helper
                imeListener?.onCommitText(" ")
            }
            "CLR" -> {
                _typedText.value = ""
            }
            "SAVE" -> {
                imeListener?.onCommitText("SAVE")
            }
            "SHIFT" -> {
                if (_shiftActive.value) {
                    _capsLock.value = !_capsLock.value
                    _shiftActive.value = false
                } else {
                    _shiftActive.value = true
                }
            }
            "LANG" -> {
                cycleLanguage()
            }
            else -> {
                val resolvedChar = if (_shiftActive.value || _capsLock.value) {
                    key.uppercase(Locale.getDefault())
                } else {
                    key.lowercase(Locale.getDefault())
                }
                _typedText.value = base + resolvedChar
                if (_shiftActive.value && !_capsLock.value) {
                    _shiftActive.value = false
                }
                imeListener?.onCommitText(resolvedChar)
            }
        }

        updatePredictions(_typedText.value)
    }

    fun toggleKeyboard() {
        _isKeyboardVisible.value = !_isKeyboardVisible.value
    }

    fun selectSuggestion(word: String, view: View) {
        triggerHapticResponse(view)
        val current = _typedText.value
        val words = current.split(" ").toMutableList()
        if (words.isNotEmpty()) {
            words.removeAt(words.lastIndex)
        }
        words.add(word)
        _typedText.value = words.joinToString(" ") + " "
        updatePredictions(_typedText.value)
        imeListener?.onReplaceLastWord(word)
    }

    private fun updatePredictions(text: String) {
        if (text.isEmpty() || text == "Tap here to start typing with glass feedback...") {
            _suggestions.value = listOf("glass", "ambient", "premium", "typing", "smooth")
            return
        }
        val lastWord = text.split(" ").lastOrNull()?.lowercase(Locale.getDefault()) ?: ""
        if (lastWord.isEmpty()) {
            _suggestions.value = listOf("glass", "ambient", "premium", "typing", "smooth")
            return
        }

        val filtered = dictionary.filter { it.startsWith(lastWord) && it != lastWord }
            .take(5)
        
        if (filtered.size < 5) {
            val remain = 5 - filtered.size
            val fallback = dictionary.filter { !filtered.contains(it) && it != lastWord }
                .shuffled()
                .take(remain)
            _suggestions.value = (filtered + fallback).take(5)
        } else {
            _suggestions.value = filtered
        }
    }

    private fun cycleLanguage() {
        val list = LanguagePreset.values()
        val index = (list.indexOf(_currentLanguage.value) + 1) % list.size
        _currentLanguage.value = list[index]
    }

    fun setLanguage(lang: LanguagePreset) {
        _currentLanguage.value = lang
    }

    fun setTheme(theme: GlassTheme) {
        _theme.value = theme
        saveCurrentSettings()
    }

    fun setIntensity(intensity: AnimationIntensity) {
        _intensity.value = intensity
        saveCurrentSettings()
    }

    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        saveCurrentSettings()
    }

    fun setHapticWeight(weight: Float) {
        _hapticWeight.value = weight
        saveCurrentSettings()
    }

    fun setBoardScale(scale: Float) {
        _boardScale.value = scale
        saveCurrentSettings()
    }

    fun setGlowIntensity(glow: Float) {
        _glowIntensity.value = glow
        saveCurrentSettings()
    }

    fun setBorderThickness(thickness: Float) {
        _borderThickness.value = thickness
        saveCurrentSettings()
    }

    fun setCornerRadius(radius: Int) {
        _cornerRadius.value = radius
        saveCurrentSettings()
    }

    fun setBacklightsSpeed(speed: Float) {
        _backlightsSpeed.value = speed
        saveCurrentSettings()
    }

    fun setParticlesCount(count: Int) {
        _particlesCount.value = count
        saveCurrentSettings()
    }

    fun saveCurrentNotepadToLibrary() {
        val current = _typedText.value
        if (current.isNotEmpty() && current != "Tap here to start typing with glass feedback...") {
            val nextId = (_savedNotes.value.maxOfOrNull { it.id } ?: 0) + 1
            val newNote = SavedNote(id = nextId, content = current)
            _savedNotes.value = _savedNotes.value + newNote
        }
    }

    fun deleteSavedNote(id: Int) {
        _savedNotes.value = _savedNotes.value.filter { it.id != id }
    }

    fun clearAllSavedNotes() {
        _savedNotes.value = emptyList()
    }

    private fun triggerHapticResponse(view: View) {
        if (!_hapticEnabled.value) return

        val vib = view.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vib != null && vib.hasVibrator()) {
            val weight = _hapticWeight.value
            when (_intensity.value) {
                AnimationIntensity.LOW -> {
                    // standard click
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                AnimationIntensity.BALANCED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val duration = (15 * weight).toLong().coerceAtLeast(5L)
                        val amplitude = (150 * weight).toInt().coerceIn(1, 255)
                        vib.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(12)
                    }
                }
                AnimationIntensity.HIGH -> {
                    // Polyrhythmic Double Tap
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val pattern = longArrayOf(0, 10, 20, 15)
                        val amplitudes = intArrayOf(0, (180 * weight).toInt(), 0, (120 * weight).toInt())
                        vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(longArrayOf(0, 12, 15, 10), -1)
                    }
                }
                AnimationIntensity.ULTRA -> {
                    // Futuristic Triple Tap Pulsation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val pattern = longArrayOf(0, 8, 15, 12, 10, 8)
                        val maxAmp = (255 * weight).toInt().coerceIn(1, 255)
                        val medAmp = (180 * weight).toInt().coerceIn(1, 255)
                        val softAmp = (100 * weight).toInt().coerceIn(1, 255)
                        val amplitudes = intArrayOf(0, maxAmp, 0, medAmp, 0, softAmp)
                        vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(longArrayOf(0, 15, 10, 10, 10, 5), -1)
                    }
                }
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}
