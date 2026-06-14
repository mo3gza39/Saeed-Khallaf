package com.example.keyboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun GlassDriftingBackdrop(
    modifier: Modifier = Modifier,
    theme: GlassTheme,
    speedMultiplier: Float = 1.0f,
    particlesCount: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "plasma_drift")

    // Animate phase offsets for multiple drifting plasma nodes
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((12000 / speedMultiplier.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween((18000 / speedMultiplier.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width > 0 && height > 0) {
            // Draw premium deep solid background layer
            drawRect(color = theme.boardBackground)

            // Draw a very subtle top soft frosted glass sheen (2% white overlay gradient at the top edge)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                    startY = 0f,
                    endY = height * 0.4f
                )
            )
        }
    }
}

@Composable
fun GlassmorphicBorderModifier(
    cornerRadius: Dp,
    borderWidth: Dp,
    normalColor: Color,
    accentColor: Color,
    isPressed: Boolean
): Brush {
    // Top-left shiny highlights, bottom-right structured colors
    return if (isPressed) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.9f),
                accentColor.copy(alpha = 0.5f),
                normalColor.copy(alpha = 0.3f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                normalColor.copy(alpha = 0.50f),
                normalColor.copy(alpha = 0.30f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

@Composable
fun RowScope.PremiumGlassKey(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector? = null,
    theme: GlassTheme,
    intensity: AnimationIntensity,
    glowMultiplier: Float = 1.0f,
    borderThickness: Float = 1.2f,
    cornerRadius: Int = 12,
    weight: Float = 1f,
    keyHeight: Dp = 52.dp,
    horizontalPadding: Dp = 3.dp,
    onPress: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Animation values configured by AnimationIntensity level
    val targetScale = when (intensity) {
        AnimationIntensity.LOW -> if (isPressed) 0.95f else 1.0f
        AnimationIntensity.BALANCED -> if (isPressed) 0.91f else 1.0f
        AnimationIntensity.HIGH -> if (isPressed) 0.86f else 1.0f
        AnimationIntensity.ULTRA -> if (isPressed) 0.80f else 1.0f
    }

    val stiffness = when (intensity) {
        AnimationIntensity.LOW -> Spring.StiffnessLow
        AnimationIntensity.BALANCED -> Spring.StiffnessMediumLow
        AnimationIntensity.HIGH -> Spring.StiffnessMedium
        AnimationIntensity.ULTRA -> Spring.StiffnessHigh
    }

    val dampingRatio = when (intensity) {
        AnimationIntensity.LOW -> Spring.DampingRatioNoBouncy
        AnimationIntensity.BALANCED -> Spring.DampingRatioLowBouncy
        AnimationIntensity.HIGH -> Spring.DampingRatioMediumBouncy
        AnimationIntensity.ULTRA -> Spring.DampingRatioHighBouncy
    }

    val scaleAnimate by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        ),
        label = "key_scale"
    )

    // Key glows and shadow size based on status and level
    val glowSpec = when (intensity) {
        AnimationIntensity.LOW -> if (isPressed) 4.dp else 0.dp
        AnimationIntensity.BALANCED -> if (isPressed) 10.dp else 2.dp
        AnimationIntensity.HIGH -> if (isPressed) 18.dp else 4.dp
        AnimationIntensity.ULTRA -> if (isPressed) 28.dp else 6.dp
    }
    val glowAnimate by animateDpAsState(
        targetValue = glowSpec,
        animationSpec = spring(stiffness = stiffness),
        label = "key_glow"
    )

    val currentView = LocalView.current

    Box(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = horizontalPadding)
            .height(keyHeight)
            .weight(weight)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPress()
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            .testTag("key_item_$label")
    ) {
        // Soft matte physical key drop shadow for realistic 3D depth and clean key separation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 1.5.dp, start = 0.5.dp, end = 0.5.dp)
                .shadow(
                    elevation = if (isPressed) 0.5.dp else 2.5.dp,
                    shape = RoundedCornerShape(cornerRadius.dp),
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.4f),
                    ambientColor = Color.Black.copy(alpha = 0.3f)
                )
        )

        // The frosted glass key component
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnimate
                    scaleY = scaleAnimate
                }
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(
                                theme.keyPressedBackground.copy(alpha = 0.95f),
                                theme.keyPressedBackground.copy(alpha = 0.85f)
                            )
                        } else {
                            listOf(
                                theme.keyBackground.copy(alpha = 0.85f),
                                theme.keyBackground.copy(alpha = 0.72f)
                            )
                        }
                    )
                )
                .border(
                    width = borderThickness.dp,
                    brush = GlassmorphicBorderModifier(
                        cornerRadius = cornerRadius.dp,
                        borderWidth = borderThickness.dp,
                        normalColor = theme.borderNormal,
                        accentColor = theme.borderPressed,
                        isPressed = isPressed
                    ),
                    shape = RoundedCornerShape(cornerRadius.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Shiny diagonal reflection line on glass
            Canvas(modifier = Modifier.fillMaxSize()) {
                val shineWidth = size.width
                val shineHeight = size.height
                drawLine(
                    color = Color.White.copy(alpha = if (isPressed) 0.14f else 0.05f),
                    start = Offset(0f, 0f),
                    end = Offset(shineWidth * 0.7f, shineHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Key content
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = if (isPressed) theme.accentColor else theme.textColor
                    )
                } else {
                    Text(
                        text = label,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (label.length > 2) 13.sp else 19.sp,
                            color = if (isPressed) theme.accentColor else theme.textColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumSuggestionBar(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
    theme: GlassTheme,
    intensity: AnimationIntensity,
    borderThickness: Float,
    cornerRadius: Int
) {
    val suggestions by viewModel.suggestions.collectAsState()
    val localView = LocalView.current

    // Guarantee exactly 5 premium single-word items
    val predictionList = remember(suggestions) {
        if (suggestions.size >= 5) {
            suggestions.take(5)
        } else {
            val fallbacks = listOf("prime", "glass", "smart", "quick", "style")
            (suggestions + fallbacks).distinct().take(5)
        }
    }

    // Elegant matte dark-gray frosted panel containing predictions (no neon glow/sparkles)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(theme.keyBackground.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = theme.borderNormal.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
            )
            .testTag("suggestion_bar"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = predictionList,
            transitionSpec = {
                if (intensity == AnimationIntensity.LOW) {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                } else {
                    val slideStiffness = when (intensity) {
                        AnimationIntensity.BALANCED -> Spring.StiffnessMediumLow
                        else -> Spring.StiffnessMedium
                    }
                    (slideInHorizontally(spring(stiffness = slideStiffness)) { it / 2 } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(spring(stiffness = slideStiffness)) { -it / 2 } + fadeOut(tween(150)))
                }
            },
            label = "suggestion_bar_transition"
        ) { currentSuggestions ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentSuggestions.forEach { s ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.keyBackground.copy(alpha = 0.3f))
                            .border(
                                width = 1.dp,
                                color = theme.borderNormal.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = theme.accentColor)
                            ) {
                                viewModel.selectSuggestion(s, localView)
                            }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s,
                            fontSize = 12.sp,
                            color = theme.textColor,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainGlassKeyboard(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.theme.collectAsState()
    val intensity by viewModel.intensity.collectAsState()
    val borderThickness by viewModel.borderThickness.collectAsState()
    val cornerRadius by viewModel.cornerRadius.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val shiftActive by viewModel.shiftActive.collectAsState()
    val capsLock by viewModel.capsLock.collectAsState()
    val backlightsSpeed by viewModel.backlightsSpeed.collectAsState()
    val particlesCount by viewModel.particlesCount.collectAsState()
    val boardScale by viewModel.boardScale.collectAsState()
    val isImeMode by viewModel.isImeMode.collectAsState()
    val localView = LocalView.current

    // Build the keys matrix depending on language
    val keysRow1: List<String>
    val keysRow2: List<String>
    val keysRow3: List<String>

    when (language) {
        LanguagePreset.ENGLISH -> {
            keysRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            keysRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            keysRow3 = listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "BACK")
        }
        LanguagePreset.ARABIC -> {
            keysRow1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د")
            keysRow2 = listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
            keysRow3 = listOf("SHIFT", "ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ", "BACK")
        }
        LanguagePreset.NUMBERS_SYMBOLS -> {
            keysRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            keysRow2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
            keysRow3 = listOf("/", "\\", "=", "<", ">", "?", "!", "BACK")
        }
        LanguagePreset.EMOJIS -> {
            keysRow1 = emptyList()
            keysRow2 = emptyList()
            keysRow3 = emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = boardScale
                scaleY = boardScale
            }
            .clip(RoundedCornerShape(topStart = (cornerRadius + 6).dp, topEnd = (cornerRadius + 6).dp))
            .shadow(16.dp)
            .testTag("glass_keyboard")
    ) {
        // Drifting Backlights layer reflecting through the glass
        GlassDriftingBackdrop(
            theme = theme,
            speedMultiplier = backlightsSpeed,
            particlesCount = particlesCount
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // Elevated dynamic suggestion bar inside the board layout
            PremiumSuggestionBar(
                viewModel = viewModel,
                theme = theme,
                intensity = intensity,
                borderThickness = borderThickness,
                cornerRadius = cornerRadius
            )

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = language,
                transitionSpec = {
                    if (intensity == AnimationIntensity.LOW) {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                    } else {
                        (scaleIn(tween(250)) + fadeIn(tween(200))) togetherWith
                                (scaleOut(tween(200)) + fadeOut(tween(150)))
                    }
                },
                label = "input_shelf_transition"
            ) { currentLang ->
                if (currentLang == LanguagePreset.EMOJIS) {
                    EmojiShelfLayout(viewModel = viewModel, theme = theme, cornerRadius = cornerRadius, intensity = intensity)
                } else {
                    val isArabic = currentLang == LanguagePreset.ARABIC
                    val keyPadding = 1.5.dp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        // DEDICATED NUMBER ROW: Always above letter rows, styled with good spacing & high-contrast glass shapes
                        if (currentLang == LanguagePreset.ENGLISH || currentLang == LanguagePreset.ARABIC) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { num ->
                                    PremiumGlassKey(
                                        label = num,
                                        theme = theme,
                                        intensity = intensity,
                                        borderThickness = borderThickness,
                                        cornerRadius = cornerRadius,
                                        weight = 1.0f,
                                        keyHeight = 44.dp,
                                        horizontalPadding = keyPadding,
                                        onPress = { viewModel.onKeyPress(num, localView) }
                                    )
                                }
                            }
                        }

                        // ROW 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            keysRow1.forEach { key ->
                                val label = if (shiftActive || capsLock) key.uppercase(Locale.getDefault()) else key
                                PremiumGlassKey(
                                    label = label,
                                    theme = theme,
                                    intensity = intensity,
                                    borderThickness = borderThickness,
                                    cornerRadius = cornerRadius,
                                    weight = 1.0f,
                                    horizontalPadding = keyPadding,
                                    onPress = { viewModel.onKeyPress(key, localView) }
                                )
                            }
                        }

                        // ROW 2 (Fully stretched edge-to-edge for both English and Arabic)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            keysRow2.forEach { key ->
                                val label = if (shiftActive || capsLock) key.uppercase(Locale.getDefault()) else key
                                PremiumGlassKey(
                                    label = label,
                                    theme = theme,
                                    intensity = intensity,
                                    borderThickness = borderThickness,
                                    cornerRadius = cornerRadius,
                                    weight = 1.0f,
                                    horizontalPadding = keyPadding,
                                    onPress = { viewModel.onKeyPress(key, localView) }
                                )
                            }
                        }

                        // ROW 3 (Symmetric Shift and Backspace size alignment)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            keysRow3.forEach { key ->
                                val label = if (shiftActive || capsLock) key.uppercase(Locale.getDefault()) else key
                                val isShift = key == "SHIFT"
                                val isBack = key == "BACK"
                                val icon = when {
                                    isShift -> if (capsLock) Icons.Default.VerticalAlignTop else Icons.Default.ArrowUpward
                                    isBack -> Icons.Default.Backspace
                                    else -> null
                                }

                                // English layout has 7 middle letter keys while symbols layout has 7 middle symbols.
                                // We configure symmetric outer action keys to prevent any uneven key layouts.
                                val keyWeight = when {
                                    isShift || isBack -> {
                                        if (isArabic) 1.4f else 1.5f
                                    }
                                    else -> {
                                        1.0f
                                    }
                                }

                                PremiumGlassKey(
                                    label = if (isShift && capsLock) "CAPS" else label,
                                    icon = icon,
                                    theme = theme,
                                    intensity = intensity,
                                    borderThickness = borderThickness,
                                    cornerRadius = cornerRadius,
                                    weight = keyWeight,
                                    horizontalPadding = keyPadding,
                                    onPress = { viewModel.onKeyPress(key, localView) }
                                )
                            }
                        }

                        // ROW 4 (Bottom utility line)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Scale weights automatically based on language column grid size so bottom sizes remain identical
                            val bottomRowWeights = when (currentLang) {
                                LanguagePreset.ARABIC -> listOf(1.56f, 1.56f, 5.52f, 1.44f, 1.92f)
                                else -> listOf(1.3f, 1.3f, 4.6f, 1.2f, 1.6f)
                            }

                            // Language preset toggler
                            val langLabel = if (isArabic) "ENG" else "عربي"
                            PremiumGlassKey(
                                label = langLabel,
                                icon = Icons.Default.Language,
                                theme = theme,
                                intensity = intensity,
                                borderThickness = borderThickness,
                                cornerRadius = cornerRadius,
                                weight = bottomRowWeights[0],
                                horizontalPadding = keyPadding,
                                onPress = { viewModel.onKeyPress("LANG", localView) }
                            )

                            // Notepad clear button
                            PremiumGlassKey(
                                label = "CLR",
                                icon = Icons.Default.ClearAll,
                                theme = theme,
                                intensity = intensity,
                                borderThickness = borderThickness,
                                cornerRadius = cornerRadius,
                                weight = bottomRowWeights[1],
                                horizontalPadding = keyPadding,
                                onPress = { viewModel.onKeyPress("CLR", localView) }
                            )

                            // Space bar
                            val spaceLabel = if (isArabic) "مسافة" else "Space"
                            PremiumGlassKey(
                                label = spaceLabel,
                                theme = theme,
                                intensity = intensity,
                                borderThickness = borderThickness,
                                cornerRadius = cornerRadius,
                                weight = bottomRowWeights[2],
                                horizontalPadding = keyPadding,
                                onPress = { viewModel.onKeyPress("SPACE", localView) }
                            )

                            // Punctuation fast-dot key
                            PremiumGlassKey(
                                label = ".",
                                theme = theme,
                                intensity = intensity,
                                borderThickness = borderThickness,
                                cornerRadius = cornerRadius,
                                weight = bottomRowWeights[3],
                                horizontalPadding = keyPadding,
                                onPress = { viewModel.onKeyPress(".", localView) }
                            )

                            // Send key (inserts notepad record to database in app, acts as ENTER in IME)
                            val enterLabel = if (isImeMode) "ENTER" else "SAVE"
                            val enterIcon = if (isImeMode) Icons.Default.KeyboardReturn else Icons.Default.Save
                            PremiumGlassKey(
                                label = enterLabel,
                                icon = enterIcon,
                                theme = theme,
                                intensity = intensity,
                                borderThickness = borderThickness,
                                cornerRadius = cornerRadius,
                                weight = bottomRowWeights[4],
                                horizontalPadding = keyPadding,
                                onPress = { 
                                    if (isImeMode) {
                                        viewModel.onKeyPress("SAVE", localView)
                                    } else {
                                        viewModel.saveCurrentNotepadToLibrary()
                                        viewModel.onKeyPress("CLR", localView)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiShelfLayout(
    viewModel: KeyboardViewModel,
    theme: GlassTheme,
    cornerRadius: Int,
    intensity: AnimationIntensity
) {
    var selectedCategory by remember { mutableStateOf(0) }
    val localView = LocalView.current

    val categories = listOf("Recent", "Faces", "Activity", "Cosmo")
    val emojis = listOf(
        listOf("😀", "😂", "😍", "🔥", "🎉", "🚀", "❤️", "👍", "✨", "🌟", "🤩", "🥳", "🙌", "💥", "🎈", "🎵"),
        listOf("😎", "🤔", "😭", "😡", "🤯", "😴", "🤮", "🥳", "😱", "🥺", "🤠", "👽", "🤖", "👻", "😈", "🤡"),
        listOf("💻", "📱", "📚", "☕", "🍕", "🎮", "🎸", "⚽", "🏀", "🔑", "💡", "💰", "🚗", "✈️", "🏆", "🎁"),
        listOf("🌙", "🌸", "🪐", "❄️", "⚡", "🍀", "🌈", "🌊", "🐾", "🛸", "🌍", "🔮", "🔥", "☄️", "🌲", "💎")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp)
    ) {
        // Category selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedCategory == index) theme.keyPressedBackground.copy(0.3f)
                            else Color.Transparent
                        )
                        .clickable { selectedCategory = index }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (selectedCategory == index) theme.accentColor else theme.textColor.copy(0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Exit emojis icon
            IconButton(
                onClick = { 
                    viewModel.setLanguage(LanguagePreset.ENGLISH)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Text layout",
                    tint = theme.textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Horizontal line separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(theme.borderNormal.copy(0.25f))
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Grid of emojis
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val currentGroup = emojis.getOrNull(selectedCategory) ?: emojis[0]
            items(currentGroup) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(cornerRadius.dp))
                        .background(theme.keyBackground.copy(0.15f))
                        .border(1.dp, theme.borderNormal.copy(0.15f), RoundedCornerShape(cornerRadius.dp))
                        .clickable {
                            viewModel.onKeyPress(emoji, localView)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}
