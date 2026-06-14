package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keyboard.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF030712)), // Sleek ultra-dark base background
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    GlassKeyboardWorkspace(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassKeyboardWorkspace(
    modifier: Modifier = Modifier,
    viewModel: KeyboardViewModel = viewModel()
) {
    val theme by viewModel.theme.collectAsState()
    val intensity by viewModel.intensity.collectAsState()
    val typedText by viewModel.typedText.collectAsState()
    val isKeyboardVisible by viewModel.isKeyboardVisible.collectAsState()
    val savedNotes by viewModel.savedNotes.collectAsState()
    val borderThickness by viewModel.borderThickness.collectAsState()
    val cornerRadius by viewModel.cornerRadius.collectAsState()

    // Screen dimensions to support Canonical Adaptive Layouts
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= 600

    // Tab index when on compact portrait phone
    var activeMobileTab by remember { mutableStateOf(0) } // 0 = Notepad & Notes Feed, 1 = Premium Designer Panel
    val localView = LocalView.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Steady AMOLED black layer
    ) {
        // Deep atmosphere particle light behind EVERYTHING
        GlassDriftingBackdrop(
            theme = theme,
            speedMultiplier = 0.4f,
            particlesCount = 4
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // HIGH-TECH APP MASTER HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(cornerRadius.dp))
                    .background(theme.boardBackground.copy(alpha = 0.2f))
                    .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.2f), RoundedCornerShape(cornerRadius.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(theme.accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "GLASSKEY LABS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = theme.textColor,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                "Frosted Glass Simulation • ${intensity.displayName} FPS Mode",
                                fontSize = 9.sp,
                                color = theme.textColor.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Toggle Keyboard Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.keyBackground.copy(alpha = 0.2f))
                            .border(1.dp, theme.borderNormal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleKeyboard() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("toggle_keyboard_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (isKeyboardVisible) Icons.Default.VisibilityOff else Icons.Default.Keyboard,
                                contentDescription = "Toggle board",
                                tint = theme.accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isKeyboardVisible) "Hide Keyboard" else "Show Keyboard",
                                color = theme.textColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // MAIN WORKSPACE SHEET
            if (isExpanded) {
                // Expanded Canonical Layout: Side-by-side tablet / desktop split
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Interactive notepad editor and saved feeds
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KeyboardSandboxNotepad(
                            typedText = typedText,
                            theme = theme,
                            borderThickness = borderThickness,
                            cornerRadius = cornerRadius,
                            onClear = { viewModel.onKeyPress("CLR", localView) },
                            onSave = { 
                                viewModel.saveCurrentNotepadToLibrary()
                                viewModel.onKeyPress("CLR", localView)
                            }
                        )

                        Text(
                            "SAVED GLASSPAD CODES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor.copy(alpha = 0.6f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        LibraryNotesFeed(
                            savedNotes = savedNotes,
                            theme = theme,
                            borderThickness = borderThickness,
                            cornerRadius = cornerRadius,
                            onDelete = { id -> viewModel.deleteSavedNote(id) },
                            onClearAll = { viewModel.clearAllSavedNotes() }
                        )
                    }

                    // Right Column: Professional Customizer Studio Controls
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(cornerRadius.dp))
                            .background(theme.boardBackground.copy(alpha = 0.15f))
                            .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.15f), RoundedCornerShape(cornerRadius.dp))
                    ) {
                        SettingsPanel(viewModel = viewModel)
                    }
                }
            } else {
                // Compact mobile layout: Segmented tab views
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.boardBackground.copy(alpha = 0.15f))
                        .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeMobileTab = 0 }
                            .background(if (activeMobileTab == 0) theme.keyPressedBackground.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(vertical = 10.dp)
                            .testTag("mobile_tab_notepad"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (activeMobileTab == 0) theme.accentColor else theme.textColor.copy(0.4f))
                            Text("Notepad Lab", color = if (activeMobileTab == 0) theme.accentColor else theme.textColor.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeMobileTab = 1 }
                            .background(if (activeMobileTab == 1) theme.keyPressedBackground.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(vertical = 10.dp)
                            .testTag("mobile_tab_customizer"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (activeMobileTab == 1) theme.accentColor else theme.textColor.copy(0.4f))
                            Text("Theme Designer", color = if (activeMobileTab == 1) theme.accentColor else theme.textColor.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    AnimatedContent(
                        targetState = activeMobileTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            }.using(SizeTransform(clip = false))
                        },
                        label = "mobile_tab_transition"
                    ) { tab ->
                        when (tab) {
                            0 -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    KeyboardSandboxNotepad(
                                        typedText = typedText,
                                        theme = theme,
                                        borderThickness = borderThickness,
                                        cornerRadius = cornerRadius,
                                        onClear = { viewModel.onKeyPress("CLR", localView) },
                                        onSave = { 
                                            viewModel.saveCurrentNotepadToLibrary()
                                            viewModel.onKeyPress("CLR", localView)
                                        }
                                    )

                                    Text(
                                        "SAVED NOTEPAD CODES",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textColor.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    LibraryNotesFeed(
                                        savedNotes = savedNotes,
                                        theme = theme,
                                        borderThickness = borderThickness,
                                        cornerRadius = cornerRadius,
                                        onDelete = { id -> viewModel.deleteSavedNote(id) },
                                        onClearAll = { viewModel.clearAllSavedNotes() },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            1 -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(cornerRadius.dp))
                                        .background(theme.boardBackground.copy(alpha = 0.1f))
                                        .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.1f), RoundedCornerShape(cornerRadius.dp))
                                ) {
                                    SettingsPanel(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // THE SLIDEABLE SLIDER FROSTED KEYBOARD DRAWER
            AnimatedVisibility(
                visible = isKeyboardVisible,
                enter = slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                    initialOffsetY = { it }
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    targetOffsetY = { it }
                ) + fadeOut(animationSpec = tween(200))
            ) {
                MainGlassKeyboard(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // Respect device navigation overlay
                )
            }
        }
    }
}

// Custom simple helper for tinting custom icon sizes
private fun sizeIndex(theme: GlassTheme, default: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp {
    return default
}

@Composable
fun KeyboardSandboxNotepad(
    typedText: String,
    theme: GlassTheme,
    borderThickness: Float,
    cornerRadius: Int,
    onClear: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(theme.boardBackground.copy(alpha = 0.3f))
            .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.25f), RoundedCornerShape(cornerRadius.dp))
            .padding(12.dp)
            .testTag("sandbox_notepad_notepad")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Glass Sandbox Editor",
                    fontSize = 11.sp,
                    color = theme.textColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Trash clean button
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear editor",
                            tint = theme.textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Direct save action button
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.size(24.dp),
                        enabled = typedText.isNotEmpty() && typedText != "Tap here to start typing with glass feedback..."
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save library",
                            tint = if (typedText.isNotEmpty() && typedText != "Tap here to start typing with glass feedback...") theme.accentColor else theme.textColor.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Text space with custom animated gradient cursor simulation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = typedText,
                    fontSize = 15.sp,
                    color = if (typedText == "Tap here to start typing with glass feedback...") theme.textColor.copy(alpha = 0.4f) else theme.textColor,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun LibraryNotesFeed(
    savedNotes: List<SavedNote>,
    theme: GlassTheme,
    borderThickness: Float,
    cornerRadius: Int,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(theme.boardBackground.copy(alpha = 0.15f))
            .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.1f), RoundedCornerShape(cornerRadius.dp))
            .padding(8.dp)
    ) {
        if (savedNotes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    tint = theme.textColor.copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Library is empty.\nType some prose and tap the keyboard's SAVE key (or floppy icon above) to log here!",
                    color = theme.textColor.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Wipe Library",
                        color = theme.accentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClearAll() }
                            .padding(4.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(savedNotes, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.keyBackground.copy(alpha = 0.15f))
                                .border(1.dp, theme.borderNormal.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.content,
                                        color = theme.textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = theme.textColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

