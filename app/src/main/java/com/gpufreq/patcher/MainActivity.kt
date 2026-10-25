package com.gpufreq.patcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import com.gpufreq.patcher.util.OppRow
import com.gpufreq.patcher.util.Issue
import com.gpufreq.patcher.util.Severity
import com.gpufreq.patcher.util.OppEngine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import com.gpufreq.patcher.data.ModulePatch
import com.gpufreq.patcher.ui.EditorUiState
import com.gpufreq.patcher.ui.MainViewModel
import com.gpufreq.patcher.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Custom Vectors for missing core icons to keep app light
val IconChevronRight: ImageVector
    get() = ImageVector.Builder(
        name = "ChevronRight",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(9f, 6f)
        lineTo(15f, 12f)
        lineTo(9f, 18f)
    }.build()

val IconInfo: ImageVector
    get() = ImageVector.Builder(
        name = "Info",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 12f)
        lineTo(12f, 16f)
        moveTo(12f, 8f)
        lineTo(12.01f, 8f)
        
        // Circle
        moveTo(12f, 20f)
        curveTo(16.4f, 20f, 20f, 16.4f, 20f, 12f)
        curveTo(20f, 7.6f, 16.4f, 4f, 12f, 4f)
        curveTo(7.6f, 4f, 4f, 7.6f, 4f, 12f)
        curveTo(4f, 16.4f, 7.6f, 20f, 12f, 20f)
    }.build()

val IconSettings: ImageVector
    get() = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Simple SF Style Gear
        moveTo(12f, 9.5f)
        curveTo(13.4f, 9.5f, 14.5f, 10.6f, 14.5f, 12f)
        curveTo(14.5f, 13.4f, 13.4f, 14.5f, 12f, 14.5f)
        curveTo(10.6f, 14.5f, 9.5f, 13.4f, 9.5f, 12f)
        curveTo(9.5f, 10.6f, 10.6f, 9.5f, 12f, 9.5f)
        close()

        moveTo(12f, 2f); lineTo(12f, 4f)
        moveTo(12f, 20f); lineTo(12f, 22f)
        moveTo(2f, 12f); lineTo(4f, 12f)
        moveTo(20f, 12f); lineTo(22f, 12f)
        
        moveTo(4.9f, 4.9f); lineTo(6.3f, 6.3f)
        moveTo(17.7f, 17.7f); lineTo(19.1f, 19.1f)
        moveTo(19.1f, 4.9f); lineTo(17.7f, 6.3f)
        moveTo(6.3f, 17.7f); lineTo(4.9f, 19.1f)
    }.build()

val IconRefresh: ImageVector
    get() = ImageVector.Builder(
        name = "Refresh",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 4f)
        curveTo(16.4f, 4f, 20f, 7.6f, 20f, 12f)
        curveTo(20f, 16.4f, 16.4f, 20f, 12f, 20f)
        curveTo(7.6f, 20f, 4f, 16.4f, 4f, 12f)
        curveTo(4f, 10f, 4.8f, 8.2f, 6f, 6.8f)
        
        moveTo(3.5f, 7f)
        lineTo(6f, 6.8f)
        lineTo(6.2f, 9.5f)
    }.build()

val IconEdit: ImageVector
    get() = ImageVector.Builder(
        name = "Edit",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Square
        moveTo(4f, 10f)
        verticalLineTo(18f)
        curveTo(4f, 19.1f, 4.9f, 20f, 6f, 20f)
        horizontalLineTo(14f)
        curveTo(15.1f, 20f, 16f, 19.1f, 16f, 18f)
        verticalLineTo(14f)

        // Pencil
        moveTo(18.5f, 3.5f)
        curveTo(19.3f, 2.7f, 20.7f, 2.7f, 21.5f, 3.5f)
        curveTo(22.3f, 4.3f, 22.3f, 5.7f, 21.5f, 6.5f)
        lineTo(12f, 16f)
        lineTo(8f, 17f)
        lineTo(9f, 13f)
        lineTo(18.5f, 3.5f)
        close()
    }.build()

val IconCheckCircle: ImageVector
    get() = ImageVector.Builder(
        name = "CheckCircle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Checkmark
        moveTo(9f, 12f)
        lineTo(11f, 14f)
        lineTo(15.5f, 9.5f)
        
        // Circle
        moveTo(12f, 20f)
        curveTo(16.4f, 20f, 20f, 16.4f, 20f, 12f)
        curveTo(20f, 7.6f, 16.4f, 4f, 12f, 4f)
        curveTo(7.6f, 4f, 4f, 7.6f, 4f, 12f)
        curveTo(4f, 16.4f, 7.6f, 20f, 12f, 20f)
    }.build()

val IconUploadFile: ImageVector
    get() = ImageVector.Builder(
        name = "UploadFile",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Tray
        moveTo(4f, 12f)
        lineTo(4f, 18f)
        curveTo(4f, 19.1f, 4.9f, 20f, 6f, 20f)
        horizontalLineTo(18f)
        curveTo(19.1f, 20f, 20f, 19.1f, 20f, 18f)
        lineTo(20f, 12f)
        
        // Arrow shaft
        moveTo(12f, 4f)
        lineTo(12f, 15f)
        
        // Arrow head
        moveTo(8.5f, 11.5f)
        lineTo(12f, 15f)
        lineTo(15.5f, 11.5f)
    }.build()

val IconMicrochip: ImageVector
    get() = ImageVector.Builder(
        name = "Microchip",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Main body
        moveTo(9f, 6f)
        lineTo(15f, 6f)
        curveTo(17.2f, 6f, 18f, 6.8f, 18f, 9f)
        lineTo(18f, 15f)
        curveTo(18f, 17.2f, 17.2f, 18f, 15f, 18f)
        lineTo(9f, 18f)
        curveTo(6.8f, 18f, 6f, 17.2f, 6f, 15f)
        lineTo(6f, 9f)
        curveTo(6f, 6.8f, 6.8f, 6f, 9f, 6f)
        close()

        // Center hub
        moveTo(10f, 10f)
        lineTo(14f, 10f)
        lineTo(14f, 14f)
        lineTo(10f, 14f)
        close()

        // Top pins
        moveTo(9f, 6f); lineTo(9f, 3.5f)
        moveTo(12f, 6f); lineTo(12f, 3.5f)
        moveTo(15f, 6f); lineTo(15f, 3.5f)

        // Bottom pins
        moveTo(9f, 18f); lineTo(9f, 20.5f)
        moveTo(12f, 18f); lineTo(12f, 20.5f)
        moveTo(15f, 18f); lineTo(15f, 20.5f)

        // Left pins
        moveTo(6f, 9f); lineTo(3.5f, 9f)
        moveTo(6f, 12f); lineTo(3.5f, 12f)
        moveTo(6f, 15f); lineTo(3.5f, 15f)

        // Right pins
        moveTo(18f, 9f); lineTo(20.5f, 9f)
        moveTo(18f, 12f); lineTo(20.5f, 12f)
        moveTo(18f, 15f); lineTo(20.5f, 15f)
    }.build()

val IconHistory: ImageVector
    get() = ImageVector.Builder(
        name = "History",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Clock circle arc (leaves a small gap at top for arrow)
        moveTo(12f, 4f)
        curveTo(16.4f, 4f, 20f, 7.6f, 20f, 12f)
        curveTo(20f, 16.4f, 16.4f, 20f, 12f, 20f)
        curveTo(7.6f, 20f, 4f, 16.4f, 4f, 12f)
        curveTo(4f, 8.5f, 6.2f, 5.5f, 9.5f, 4.5f)
        
        // Arrow head at top-left
        moveTo(7.5f, 7f)
        lineTo(9.5f, 4.5f)
        lineTo(6.5f, 3.5f)

        // Clock hands
        moveTo(12f, 12f)
        lineTo(12f, 8f)
        moveTo(12f, 12f)
        lineTo(15.5f, 12f)
    }.build()

val IconTuning: ImageVector
    get() = ImageVector.Builder(
        name = "Tuning",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Track 1
        moveTo(3f, 6f)
        lineTo(21f, 6f)
        // Knob 1
        moveTo(14f, 6f)
        curveTo(14f, 4.9f, 14.9f, 4f, 16f, 4f)
        curveTo(17.1f, 4f, 18f, 4.9f, 18f, 6f)
        curveTo(18f, 7.1f, 17.1f, 8f, 16f, 8f)
        curveTo(14.9f, 8f, 14f, 7.1f, 14f, 6f)
        close()

        // Track 2
        moveTo(3f, 12f)
        lineTo(21f, 12f)
        // Knob 2
        moveTo(6f, 12f)
        curveTo(6f, 10.9f, 6.9f, 10f, 8f, 10f)
        curveTo(9.1f, 10f, 10f, 10.9f, 10f, 12f)
        curveTo(10f, 13.1f, 9.1f, 14f, 8f, 14f)
        curveTo(6.9f, 14f, 6f, 13.1f, 6f, 12f)
        close()

        // Track 3
        moveTo(3f, 18f)
        lineTo(21f, 18f)
        // Knob 3
        moveTo(12f, 18f)
        curveTo(12f, 16.9f, 12.9f, 16f, 14f, 16f)
        curveTo(15.1f, 16f, 16f, 16.9f, 16f, 18f)
        curveTo(16f, 19.1f, 15.1f, 20f, 14f, 20f)
        curveTo(12.9f, 20f, 12f, 19.1f, 12f, 18f)
        close()
    }.build()

val IconLightMode: ImageVector
    get() = ImageVector.Builder(
        name = "LightMode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // Sun center circle
        moveTo(12f, 8f)
        curveTo(14.2f, 8f, 16f, 9.8f, 16f, 12f)
        curveTo(16f, 14.2f, 14.2f, 16f, 12f, 16f)
        curveTo(9.8f, 16f, 8f, 14.2f, 8f, 12f)
        curveTo(8f, 9.8f, 9.8f, 8f, 12f, 8f)
        close()

        // Sun rays (more elegant spacing)
        moveTo(12f, 3.5f); lineTo(12f, 5.5f) // top
        moveTo(12f, 18.5f); lineTo(12f, 20.5f) // bottom
        moveTo(3.5f, 12f); lineTo(5.5f, 12f) // left
        moveTo(18.5f, 12f); lineTo(20.5f, 12f) // right
        
        moveTo(6f, 6f); lineTo(7.4f, 7.4f) // top-left
        moveTo(16.6f, 16.6f); lineTo(18f, 18f) // bottom-right
        moveTo(18f, 6f); lineTo(16.6f, 7.4f) // top-right
        moveTo(7.4f, 16.6f); lineTo(6f, 18f) // bottom-left
    }.build()

val IconDarkMode: ImageVector
    get() = ImageVector.Builder(
        name = "DarkMode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        // iOS style crescent moon
        moveTo(17.8f, 3.2f)
        curveTo(12.6f, 3.2f, 8.4f, 7.4f, 8.4f, 12.6f)
        curveTo(8.4f, 17.8f, 12.6f, 22f, 17.8f, 22f)
        curveTo(18.9f, 22f, 19.9f, 21.8f, 20.9f, 21.4f)
        curveTo(15.2f, 20f, 11.2f, 15.6f, 11.2f, 11.6f)
        curveTo(11.2f, 7.6f, 15.2f, 3.2f, 20.9f, 1.8f)
        curveTo(19.9f, 1.4f, 18.9f, 1.2f, 17.8f, 1.2f)
        close()
    }.build()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val systemIsDark = isSystemInDarkTheme()
            val userThemePreference by viewModel.isDarkTheme.collectAsState()
            val isDark = userThemePreference ?: true
            
            KOEditorTheme(darkTheme = isDark) {
                MainScreen(viewModel = viewModel, isDarkTheme = isDark)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel(), isDarkTheme: Boolean) {
    val isDark = isDarkTheme
    val systemIsDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
    val surfaceColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    val navBarColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.85f) else Color(0xFFF9F9F9).copy(alpha = 0.85f)

    val uiState by viewModel.uiState.collectAsState()
    val patches by viewModel.patches.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val pagerState = rememberPagerState(pageCount = { 2 })
    var showExitDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.loadModule(it)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is EditorUiState.Loaded) {
            pagerState.scrollToPage(1)
        }
    }

    BackHandler {
        if (showExitDialog) {
            showExitDialog = false
        } else if (pagerState.currentPage == 1) {
            coroutineScope.launch { pagerState.animateScrollToPage(0) }
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                    Text("Exit", color = SystemRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary,
            textContentColor = textSecondary
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        GlassBackground()
        
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = textPrimary,
            topBar = {
                val topBarInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .glassCard(shape = RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = IconMicrochip,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "GPU Freq Patcher",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = textPrimary
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        interactionSource = topBarInteractionSource,
                        modifier = Modifier
                            .size(36.dp)
                            .bounceScale(topBarInteractionSource)
                            .glassCard(shape = CircleShape)
                    ) {
                        AnimatedContent(
                            targetState = isDark,
                            transitionSpec = {
                                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            label = "ThemeIconTransition"
                        ) { darkState ->
                            Icon(
                                imageVector = if (darkState) IconLightMode else IconDarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (darkState) Color(0xFFFFCC00) else Color(0xFF555555),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                val isDark = LocalThemeIsDark.current
                val glassBgTop = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.82f) else Color(0xFFFFFFFF).copy(alpha = 0.88f)
                val glassBgMid = if (isDark) Color(0xFF222226).copy(alpha = 0.88f) else Color(0xFFF8F8FC).copy(alpha = 0.92f)
                val glassBgBottom = if (isDark) Color(0xFF18181B).copy(alpha = 0.94f) else Color(0xFFEFEFF4).copy(alpha = 0.96f)
                
                val glassBorderTop = if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.90f)
                val glassBorderBottom = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                
                // Floating Glassy Custom Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp, start = 20.dp, end = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(32.dp),
                                clip = false,
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.25f)
                            )
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(glassBgTop, glassBgMid, glassBgBottom)
                                )
                            )
                            .border(
                                width = 1.2.dp, 
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(glassBorderTop, glassBorderBottom)
                                ),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val navItems = listOf(
                            "History" to IconHistory,
                            "Patcher" to IconTuning
                        )
                        
                        navItems.forEachIndexed { index, item ->
                            val isSelected = pagerState.currentPage == index
                            val interactionSource = remember { MutableInteractionSource() }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .bounceClickable(interactionSource = interactionSource, indication = null) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.second,
                                        contentDescription = item.first,
                                        tint = if (isSelected) SystemBlue else textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = item.first,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SystemBlue else textSecondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (pagerState.currentPage == 0) {
                    val fabInteractionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                    Button(
                        onClick = { filePicker.launch("*/*") },
                        interactionSource = fabInteractionSource,
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .height(52.dp)
                            .bounceScale(fabInteractionSource)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = IconUploadFile, 
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Import Module (.ko)", 
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.2).sp
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                    )
            ) { page ->
                when (page) {
                    0 -> {
                        ModificationListContent(
                            patches = patches, 
                            onDelete = { viewModel.deletePatch(it) },
                            onDeleteAll = { viewModel.deleteAllPatches() },
                            onImportModule = { filePicker.launch("*/*") }
                        )
                    }
                    1 -> {
                        when (val state = uiState) {
                            is EditorUiState.Idle -> {
                                EmptyEditorContent()
                            }
                            is EditorUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = SystemBlue)
                                }
                            }
                            is EditorUiState.Loaded -> {
                                EditorContent(
                                    state = state,
                                    rows = viewModel.rows,
                                    stockRows = viewModel.stockRows,
                                    issues = viewModel.issues,
                                    hasErrors = viewModel.hasErrors,
                                    allowAnchorChange = viewModel.allowAnchorChange,
                                    isAutoOcEnabled = viewModel.isAutoOcEnabled,
                                    autoOcTargetKhz = viewModel.autoOcTargetKhz,
                                    naturalStepKhz = viewModel.naturalStepKhz,
                                    stockMaxFreq = viewModel.stockMaxFreq,
                                    onToggleAllowAnchorChange = { viewModel.updateAllowAnchorChange(it) },
                                    onToggleAutoOc = { viewModel.setAutoOcSyncEnabled(it) },
                                    onUpdateRow = { index, row -> viewModel.updateRow(index, row) },
                                    onApplyOverclock = { topKhz, stepKhz, voltTop, autoSync ->
                                        viewModel.applyOverclock(topKhz, stepKhz, voltTop, autoSync)
                                    },
                                    onClearOc = { viewModel.clearOcTiers() },
                                    onApplyVoltageOffset = { offsetMv -> viewModel.applyVoltageOffset(offsetMv) },
                                    onResetStock = { viewModel.resetToStock() },
                                    onAutoSortDescending = { viewModel.autoSortDescending() },
                                    onAutoFixPllPosdiv = { viewModel.autoFixPllPosdiv() },
                                    onAutoFixVsram = { viewModel.autoFixVsram() },
                                    onLoadExternalKoTemplate = { uri ->
                                        viewModel.loadExternalKoTemplate(uri,
                                            onSuccess = {
                                                Toast.makeText(context, "OPP dari .ko lain berhasil dimuat & dicocokkan!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    onSave = { uri ->
                                        viewModel.patchAndSave(uri)
                                    },
                                    onCancel = { viewModel.reset() }
                                )
                            }
                            is EditorUiState.Success -> {
                                SuccessContent(state.message) { viewModel.reset() }
                            }
                            is EditorUiState.Error -> {
                                ErrorContent(state.message) { viewModel.reset() }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun EmptyEditorContent() {
    val isDark = LocalThemeIsDark.current
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(shape = RoundedCornerShape(26.dp))
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(SystemBlue, SystemPurple)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    IconMicrochip, 
                    contentDescription = null, 
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "No Module Loaded",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Silakan pilih dan import modul kernel (.ko) melalui tab Home/History di bawah.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GlassBackground() {
    val isDark = LocalThemeIsDark.current
    val bgColor = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    )
}

fun Modifier.glassCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp),
    borderWidth: Dp = 1.dp
) = composed {
    val isDark = LocalThemeIsDark.current
    
    // Translucent Frosted Glass Material
    val topColor = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.65f) else Color(0xFFFFFFFF).copy(alpha = 0.82f)
    val midColor = if (isDark) Color(0xFF1E1E22).copy(alpha = 0.72f) else Color(0xFFF8F8FD).copy(alpha = 0.88f)
    val bottomColor = if (isDark) Color(0xFF121215).copy(alpha = 0.82f) else Color(0xFFEFEFF6).copy(alpha = 0.94f)
    
    val borderTopColor = if (isDark) 
        Color(0xFFFFFFFF).copy(alpha = 0.35f) 
    else 
        Color(0xFFFFFFFF).copy(alpha = 0.95f)
        
    val borderBottomColor = if (isDark) 
        Color(0xFFFFFFFF).copy(alpha = 0.08f) 
    else 
        Color(0xFF000000).copy(alpha = 0.08f)
    
    this
        .shadow(
            elevation = if (isDark) 6.dp else 8.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.25f else 0.06f),
            spotColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.10f)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(topColor, midColor, bottomColor)
            )
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(borderTopColor, borderBottomColor)
            ),
            shape = shape
        )
}

fun Modifier.bounceClickable(
    interactionSource: MutableInteractionSource? = null,
    indication: androidx.compose.foundation.Indication? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) = composed {
    val realInteractionSource = interactionSource ?: androidx.compose.runtime.remember { MutableInteractionSource() }
    val isPressed by realInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 350f
        ),
        label = "BounceClick"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = realInteractionSource,
            indication = indication,
            enabled = enabled,
            onClick = onClick
        )
}

fun Modifier.bounceScale(interactionSource: MutableInteractionSource) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 350f
        ),
        label = "BounceScale"
    )
    
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ModificationListContent(
    patches: List<ModulePatch>, 
    onDelete: (ModulePatch) -> Unit, 
    onDeleteAll: () -> Unit,
    onImportModule: () -> Unit = {}
) {
    val isDark = LocalThemeIsDark.current
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    val surfaceColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val GlassBorder = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.15f) else Color(0xFF000000).copy(alpha = 0.10f)

    var patchToDelete by remember { mutableStateOf<ModulePatch?>(null) }
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }
    var patchToShowDetails by remember { mutableStateOf<ModulePatch?>(null) }

    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to delete all modification history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAll()
                    showDeleteAllConfirmation = false
                }) {
                    Text("Delete", color = SystemRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmation = false }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary,
            textContentColor = textSecondary
        )
    }

    patchToDelete?.let { patch ->
        AlertDialog(
            onDismissRequest = { patchToDelete = null },
            title = { Text("Delete History") },
            text = { Text("Are you sure you want to delete this modification record?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(patch)
                    patchToDelete = null
                }) {
                    Text("Delete", color = SystemRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { patchToDelete = null }) {
                    Text("Cancel", color = SystemBlue)
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary,
            textContentColor = textSecondary
        )
    }
    
    patchToShowDetails?.let { patch ->
        AlertDialog(
            onDismissRequest = { patchToShowDetails = null },
            title = { Text("Modification Details", style = MaterialTheme.typography.titleLarge) },
            text = { 
                Column {
                    Text("File: ${patch.fileName}", style = MaterialTheme.typography.bodyMedium, color = textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Date: ${java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(patch.timestamp))}", style = MaterialTheme.typography.bodyMedium, color = textSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("OPP Override Summary:", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val maxFreqMhz = patch.originalFreq / 1000
                    val minFreqMhz = patch.targetFreq / 1000
                    val maxVoltMv = patch.originalVolt / 1000
                    val minVoltMv = patch.targetVolt / 1000
                    
                    Text(
                        text = "Max: $maxFreqMhz MHz ($maxVoltMv mV)\nMin: $minFreqMhz MHz ($minVoltMv mV)\nSaved to: ${patch.outputPath}",
                        style = MaterialTheme.typography.bodyMedium, 
                        color = textPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { patchToShowDetails = null }) {
                    Text("Close", color = SystemBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    patchToDelete = patch
                    patchToShowDetails = null
                }) {
                    Text("Delete", color = SystemRed)
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary
        )
    }

    if (patches.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    IconHistory, 
                    contentDescription = null, 
                    tint = textSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Modifications Found",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your patch history will appear here once you begin modifying and saving kernel modules.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 40.dp, bottom = 100.dp)
        ) {
            item {
                Text(
                    text = "Modification List",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODIFICATION HISTORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { showDeleteAllConfirmation = true }, 
                        contentPadding = PaddingValues(0.dp), 
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Clear All", fontSize = 13.sp, color = SystemRed)
                    }
                }
            }
            items(patches, key = { it.id }) { patch ->
                PatchCard(
                    patch = patch, 
                    onClick = { patchToShowDetails = patch },
                    onDeleteClick = { patchToDelete = patch }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PatchCard(
    patch: ModulePatch, 
    onClick: () -> Unit, 
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    val isDark = LocalThemeIsDark.current
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .glassCard(shape = RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(SystemBlue, SystemPurple)),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(IconHistory, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = patch.fileName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateFormat.format(Date(patch.timestamp)),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                    }
                }
                
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SystemRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val maxFreqMhz = patch.originalFreq / 1000
            val minFreqMhz = patch.targetFreq / 1000
            val maxVoltMv = patch.originalVolt / 1000
            val minVoltMv = patch.targetVolt / 1000

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(SystemBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Max: ${maxFreqMhz} MHz (${maxVoltMv} mV)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SystemBlue)
                }

                Box(
                    modifier = Modifier
                        .background(SystemPurple.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Min: ${minFreqMhz} MHz (${minVoltMv} mV)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SystemPurple)
                }
            }
        }
    }
}

@Composable
fun EditorContent(
    state: EditorUiState.Loaded,
    rows: List<OppRow>,
    stockRows: List<OppRow>,
    issues: List<Issue>,
    hasErrors: Boolean,
    allowAnchorChange: Boolean,
    isAutoOcEnabled: Boolean,
    autoOcTargetKhz: Int?,
    naturalStepKhz: Int,
    stockMaxFreq: Int?,
    onToggleAllowAnchorChange: (Boolean) -> Unit,
    onToggleAutoOc: (Boolean) -> Unit,
    onUpdateRow: (Int, OppRow) -> Unit,
    onApplyOverclock: (topKhz: Int, stepKhz: Int, voltTop: Int, autoSync: Boolean) -> Unit,
    onClearOc: () -> Unit,
    onApplyVoltageOffset: (Int) -> Unit,
    onResetStock: () -> Unit,
    onAutoSortDescending: () -> Unit,
    onAutoFixPllPosdiv: () -> Unit,
    onAutoFixVsram: () -> Unit,
    onLoadExternalKoTemplate: (Uri) -> Unit,
    onSave: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    val surfaceColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val GlassBorder = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.15f) else Color(0xFF000000).copy(alpha = 0.10f)

    val hardwareId = state.detectedHardwareId?.uppercase(Locale.getDefault())
        ?: Regex("mt[0-9]+").find(state.fileName.lowercase())?.value?.uppercase(Locale.getDefault())
        ?: android.os.Build.HARDWARE.uppercase(Locale.getDefault())

    val baseMaxFreq = stockMaxFreq ?: rows.firstOrNull()?.freq ?: 1000000

    var showOcDialog by remember { mutableStateOf(false) }
    var showDiffDialog by remember { mutableStateOf(false) }
    var showVoltOffsetDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showPreflightDialog by remember { mutableStateOf(false) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) {
            onSave(uri)
        }
    }

    val externalKoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            onLoadExternalKoTemplate(it)
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard & Close?") },
            text = { Text("Are you sure you want to exit the editor? Any unsaved modifications to the kernel module will be discarded.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onCancel()
                }) {
                    Text("Discard", color = SystemRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Stay", color = SystemBlue)
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary,
            textContentColor = textSecondary
        )
    }

    if (showDiffDialog) {
        AlertDialog(
            onDismissRequest = { showDiffDialog = false },
            title = { Text("Diff: Stock vs Patched", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Perbandingan frekuensi & voltase antara modul asli (${stockRows.size} slot) dengan tabel saat ini (${rows.size} baris):",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val maxCount = maxOf(rows.size, stockRows.size)
                    for (i in 0 until maxCount) {
                        val stock = stockRows.getOrNull(i)
                        val curr = rows.getOrNull(i)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (stock?.freq != curr?.freq || stock?.volt != curr?.volt) SystemBlue.copy(alpha = 0.10f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "OPP #$i",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                color = textPrimary
                            )
                            if (stock != null && curr != null) {
                                val stockMhz = stock.freq / 1000.0
                                val currMhz = curr.freq / 1000.0
                                val diff = curr.freq - stock.freq
                                val diffText = when {
                                    diff > 0 -> "+${diff / 1000.0} MHz"
                                    diff < 0 -> "${diff / 1000.0} MHz"
                                    else -> "unchanged"
                                }
                                val diffColor = when {
                                    diff > 0 -> SystemGreen
                                    diff < 0 -> SystemRed
                                    else -> textSecondary
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${stockMhz.toInt()} MHz (${stock.volt / 100}mV) -> ${currMhz.toInt()} MHz (${curr.volt / 100}mV)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace),
                                        color = textPrimary
                                    )
                                    Text(
                                        diffText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = diffColor
                                    )
                                }
                            } else if (stock == null) {
                                Text("+ Added row (${(curr?.freq ?: 0) / 1000} MHz)", style = MaterialTheme.typography.bodySmall, color = SystemGreen)
                            } else {
                                Text("- Removed row (${stock.freq / 1000} MHz)", style = MaterialTheme.typography.bodySmall, color = SystemRed)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDiffDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue)
                ) {
                    Text("Tutup")
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary
        )
    }

    if (showVoltOffsetDialog) {
        var offsetMvText by remember { mutableStateOf("-25") }
        AlertDialog(
            onDismissRequest = { showVoltOffsetDialog = false },
            title = { Text("Global Voltage Offset (UV / OV)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Adds or subtracts voltage for core and vsram across ALL table rows uniformly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                    Text("Quick Presets (mV):", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-50, -25, -12, 12, 25).forEach { mv ->
                            val label = if (mv > 0) "+$mv" else "$mv"
                            AssistChip(
                                onClick = { offsetMvText = mv.toString() },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (offsetMvText == mv.toString()) SystemGreen.copy(0.2f) else Color.Transparent
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = offsetMvText,
                        onValueChange = { offsetMvText = it },
                        label = { Text("Delta Offset (mV)") },
                        supportingText = {
                            val parsed = offsetMvText.toIntOrNull() ?: 0
                            val desc = when {
                                parsed < 0 -> "Undervolt: ${-parsed} mV lower power consumption"
                                parsed > 0 -> "Overvolt: +$parsed mV for OC stability"
                                else -> "No voltage change"
                            }
                            Text(desc)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = offsetMvText.toIntOrNull() ?: 0
                        if (parsed != 0) {
                            onApplyVoltageOffset(parsed)
                        }
                        showVoltOffsetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemGreen)
                ) {
                    Text("Apply Offset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoltOffsetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary
        )
    }

    if (showOcDialog) {
        val initialTop = baseMaxFreq + naturalStepKhz * 2
        var topFreqText by remember { mutableStateOf(initialTop.toString()) }
        var stepFreqText by remember { mutableStateOf(naturalStepKhz.toString()) }
        val defaultVoltTop = OppEngine.snap625Up((rows.firstOrNull()?.volt ?: 85000) + 37500)
        var topVoltText by remember { mutableStateOf(defaultVoltTop.toString()) }
        var autoSyncChecked by remember { mutableStateOf(true) }

        val parsedTop = topFreqText.toIntOrNull() ?: initialTop
        val parsedStep = stepFreqText.toIntOrNull() ?: naturalStepKhz

        // Preview generated tiers
        val generatedTiers = remember(parsedTop, parsedStep) {
            val list = mutableListOf<Int>()
            val safeStep = if (parsedStep >= 1000) parsedStep else naturalStepKhz
            if (parsedTop > baseMaxFreq && safeStep > 0) {
                var f = baseMaxFreq + safeStep
                while (f <= parsedTop) {
                    list.add(f)
                    f += safeStep
                }
                if (list.isEmpty() || list.last() < parsedTop) {
                    list.add(parsedTop)
                }
                list.reverse()
            }
            list
        }

        AlertDialog(
            onDismissRequest = { showOcDialog = false },
            title = { Text("Gradual Overclock Wizard", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Builds step-by-step OPP entries above stock max (${baseMaxFreq / 1000} MHz) with step interval identical to stock (${naturalStepKhz / 1000} MHz).",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )

                    Text("Target Frequency Presets:", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(25000, 50000, 75000, 100000).forEach { delta ->
                            val target = baseMaxFreq + delta
                            val label = "+${delta / 1000} MHz"
                            AssistChip(
                                onClick = { topFreqText = target.toString() },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (topFreqText == target.toString()) SystemPurple.copy(0.2f) else Color.Transparent
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = topFreqText,
                        onValueChange = { topFreqText = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Top Freq (kHz)") },
                        supportingText = { Text("${parsedTop / 1000.0} MHz (${if (parsedTop > baseMaxFreq) "+${(parsedTop - baseMaxFreq) / 1000} MHz" else "equal/below stock"})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = stepFreqText,
                        onValueChange = { stepFreqText = it.filter { c -> c.isDigit() } },
                        label = { Text("Step Interval (kHz)") },
                        supportingText = { Text("${parsedStep / 1000.0} MHz (Matches stock interval)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = topVoltText,
                        onValueChange = { topVoltText = it.filter { c -> c.isDigit() } },
                        label = { Text("Top Voltage Ceiling (10uV)") },
                        supportingText = {
                            val v = topVoltText.toIntOrNull() ?: defaultVoltTop
                            Text("${v / 100.0} mV (Adjusted gradually)")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Auto-sync Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SystemPurple.copy(alpha = 0.08f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Auto-Sync Fixed Slots", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
                            Text("Maintains curve to ${rows.size} fixed slots without corrupting module layout", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = autoSyncChecked,
                            onCheckedChange = { autoSyncChecked = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SystemPurple)
                        )
                    }

                    if (generatedTiers.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SystemPurple.copy(alpha = 0.10f))
                                .padding(10.dp)
                        ) {
                            Text(
                                "Generated OC Tiers Preview (+${generatedTiers.size} rows):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SystemPurple
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                generatedTiers.joinToString(" -> ") { "${it / 1000} MHz" } + " -> ${baseMaxFreq / 1000} MHz (Stock Max)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                color = textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val volt = topVoltText.toIntOrNull() ?: defaultVoltTop
                        onApplyOverclock(parsedTop, parsedStep, volt, autoSyncChecked)
                        showOcDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemPurple)
                ) {
                    Text("Apply Overclock", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOcDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary
        )
    }

    if (showPreflightDialog) {
        val isDescending = rows.size <= 1 || rows.zipWithNext().all { it.first.freq > it.second.freq }
        val invalidPllCount = rows.count { !OppEngine.pllReadbackOk(it.freq, it.posdiv) }
        val invalidVsramCount = rows.count { it.vsram < it.volt }
        val maxVolt = rows.maxOfOrNull { it.volt } ?: 0
        val maxVoltMv = maxVolt / 100.0
        val maxFreqMhz = (rows.firstOrNull()?.freq ?: 0) / 1000.0
        val minFreqMhz = (rows.lastOrNull()?.freq ?: 0) / 1000.0
        val isSlotExact = rows.size == state.slotCount
        val isSafeToSave = !hasErrors && isDescending && invalidPllCount == 0 && invalidVsramCount == 0

        AlertDialog(
            onDismissRequest = { showPreflightDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (isSafeToSave) IconCheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSafeToSave) SystemGreen else Color(0xFFFFB703),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Pre-Flight Safety Check", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Audit module readiness before saving to storage to prevent bootloops or DVFS kernel crashes:",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )

                    // 1. Frequency Monotonicity
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDescending) SystemGreen.copy(alpha = 0.12f) else SystemRed.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isDescending) "✓ Monotonic Freq Order" else "⚠ Freq Not Descending Order!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDescending) SystemGreen else SystemRed
                                )
                                if (!isDescending) {
                                    TextButton(
                                        onClick = onAutoSortDescending,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Sort Now", fontSize = 11.sp, color = SystemPurple, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "Max: ${maxFreqMhz.toInt()} MHz → Min: ${minFreqMhz.toInt()} MHz (${rows.size} rows)",
                                fontSize = 11.sp,
                                color = textPrimary
                            )
                        }
                    }

                    // 2. PLL Posdiv Clock Readback
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (invalidPllCount == 0) SystemGreen.copy(alpha = 0.12f) else SystemRed.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (invalidPllCount == 0) "✓ PLL Posdiv Clock: 100% OK" else "⚠ $invalidPllCount Rows Invalid PLL Posdiv",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (invalidPllCount == 0) SystemGreen else SystemRed
                                )
                                if (invalidPllCount > 0) {
                                    TextButton(
                                        onClick = onAutoFixPllPosdiv,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Auto Fix", fontSize = 11.sp, color = SystemGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = if (invalidPllCount == 0) "All frequencies have valid PLL synthesizer bits." else "Invalid posdiv triggers __gpufreq_abort during DVFS clock switching.",
                                fontSize = 11.sp,
                                color = textPrimary
                            )
                        }
                    }

                    // 3. Voltage SRAM Invariant (Vsram >= Volt)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (invalidVsramCount == 0) SystemGreen.copy(alpha = 0.12f) else SystemRed.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (invalidVsramCount == 0) "✓ Vsram >= Volt Invariant: Valid" else "⚠ $invalidVsramCount Rows Vsram < Core Volt",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (invalidVsramCount == 0) SystemGreen else SystemRed
                                )
                                if (invalidVsramCount > 0) {
                                    TextButton(
                                        onClick = onAutoFixVsram,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Boost Vsram", fontSize = 11.sp, color = Color(0xFFFFB703), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "Peak Core Voltage: ${String.format(Locale.getDefault(), "%.2f mV", maxVoltMv)} ${if (maxVoltMv > 925.0) "(High Voltage - Watch Temp)" else "(Within safe limits)"}",
                                fontSize = 11.sp,
                                color = textPrimary
                            )
                        }
                    }

                    // 4. Slot Capacity Match
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSlotExact) SystemGreen.copy(alpha = 0.12f) else Color(0xFFFFB703).copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isSlotExact) "✓ Slot Capacity: Match (${rows.size} / ${state.slotCount})" else "ℹ Slot Capacity: ${rows.size} / ${state.slotCount} (${if (rows.size > state.slotCount) "+${rows.size - state.slotCount} rows" else "${rows.size - state.slotCount} rows"})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSlotExact) SystemGreen else Color(0xFFFFB703)
                            )
                            Text(
                                text = if (isSlotExact) "Table size matches original module exactly." else "Module will be allocated to match active table row count.",
                                fontSize = 11.sp,
                                color = textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPreflightDialog = false
                        saveLauncher.launch(state.fileName)
                    },
                    enabled = isSafeToSave,
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue)
                ) {
                    Text("Lanjut Simpan .ko")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreflightDialog = false }) {
                    Text("Edit Lagi", color = textSecondary)
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textPrimary
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            // Top Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showDiscardDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SystemBlue)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Patch GPU Frequencies",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    Text(
                        text = state.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }

                Button(
                    onClick = { showPreflightDialog = true },
                    enabled = !hasErrors,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemBlue,
                        disabledContainerColor = SystemBlue.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Apply", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))

            // Device Info Card
            Text(
                text = "DEVICE & TABLE INFO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                color = textSecondary,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .glassCard()
            ) {
                SettingsRow(
                    label = "Hardware Target",
                    value = hardwareId,
                    icon = IconMicrochip,
                    badgeColor = SystemBlue
                )
                HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(start = 56.dp))
                SettingsRow(
                    label = "Kapasitas Slot",
                    value = "${state.slotCount} Slot Tetap (Terkunci Aman)",
                    icon = IconTuning,
                    badgeColor = SystemGreen
                )
                HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(start = 56.dp))
                val minF = (rows.lastOrNull()?.freq ?: 0) / 1000
                val maxF = (rows.firstOrNull()?.freq ?: 0) / 1000
                SettingsRow(
                    label = "Rentang Frekuensi",
                    value = "$minF MHz - $maxF MHz",
                    icon = IconTuning,
                    badgeColor = SystemPurple
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))

            // Tools Card
            Text(
                text = "QUICK TUNING",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                color = textSecondary,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Auto-OC Active Status banner
                if (isAutoOcEnabled && autoOcTargetKhz != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SystemPurple.copy(alpha = 0.15f))
                            .border(1.dp, SystemPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("⚡ Auto-OC Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SystemPurple)
                                Text("(${autoOcTargetKhz / 1000} MHz)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            }
                            Text("Curve automatically maintains ${state.slotCount} slots limit", fontSize = 11.sp, color = textSecondary)
                        }
                        Switch(
                            checked = isAutoOcEnabled,
                            onCheckedChange = onToggleAutoOc,
                            colors = SwitchDefaults.colors(checkedThumbColor = SystemPurple)
                        )
                    }
                }

                // Primary Actions Row (Overclock & Voltage Offset)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showOcDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SystemPurple),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(IconTuning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-OC Wizard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showVoltOffsetDialog = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Volt Offset (UV/OV)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Secondary Toolbar (Compact 4 Utilities)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { externalKoLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Text("Import .ko", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showDiffDialog = true },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Text("Inspect Diff", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onAutoSortDescending,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Text("Sort", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onResetStock,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SystemRed.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Text("Reset Stock", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (rows.any { it.freq > baseMaxFreq }) {
                    OutlinedButton(
                        onClick = onClearOc,
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SystemRed.copy(alpha = 0.5f))
                    ) {
                        Text("Clear Overclock Tiers", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dedicated Efuse / AVS Anchor Card (Warm Amber Accent)
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFB703).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFFFB703).copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚓ AVS Efuse Anchor", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFFFB703))
                        }
                        Text(
                            text = if (allowAnchorChange) "Anchor frequency changes allowed (risk of bootloop if SoC efuse active)." else "3 stock anchor frequencies locked safely to prevent bootloops.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                    }
                    Switch(
                        checked = allowAnchorChange,
                        onCheckedChange = onToggleAllowAnchorChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFB703),
                            checkedTrackColor = Color(0xFFFFB703).copy(alpha = 0.4f),
                            uncheckedThumbColor = Color(0xFF8E8E93)
                        )
                    )
                }
            }
        }

        // Issues banner if any
        if (issues.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                val errCount = issues.count { it.severity == Severity.ERROR }
                val warnCount = issues.count { it.severity == Severity.WARNING }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (hasErrors) {
                                if (isDark) Color(0xFF381010).copy(0.7f) else Color(0xFFFFECEC)
                            } else {
                                if (isDark) Color(0xFF3A2800).copy(0.7f) else Color(0xFFFFF7DB)
                            }
                        )
                        .border(
                            1.dp,
                            if (hasErrors) SystemRed.copy(0.4f) else Color(0xFFFFB703).copy(0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasErrors) SystemRed else Color(0xFFFFB703),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (hasErrors) "$errCount Error, $warnCount Warning Terdeteksi" else "$warnCount Peringatan / Info Tabel",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasErrors) SystemRed else Color(0xFFFFB703)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    issues.take(8).forEach { issue ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (issue.severity == Severity.ERROR) "• [ERR]" else "• [INFO]",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = if (issue.severity == Severity.ERROR) SystemRed else Color(0xFFFFB703)
                            )
                            val rowPrefix = issue.row?.let { "OPP #$it: " } ?: ""
                            Text(
                                text = "$rowPrefix${issue.message}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textPrimary
                            )
                        }
                    }

                    if (issues.size > 8) {
                        Text(
                            text = "+ ${issues.size - 8} pesan lainnya...",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            // OPP Table Overrides Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPP TABLE OVERRIDES (${rows.size} ENTRIES)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                    color = textSecondary
                )
                if (rows.isNotEmpty()) {
                    Text(
                        text = "Max: ${rows.first().freq / 1000} MHz • Min: ${rows.last().freq / 1000} MHz",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = SystemBlue
                    )
                }
            }
        }

        itemsIndexed(
            items = rows,
            key = { index, _ -> index }
        ) { index, row ->
            val rowShape = RoundedCornerShape(18.dp)
            val rowIssue = issues.firstOrNull { it.row == index }
            val isOc = row.freq > baseMaxFreq

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 5.dp)
                    .glassCard(shape = rowShape)
                    .then(
                        if (rowIssue?.severity == Severity.ERROR) {
                            Modifier.border(1.5.dp, SystemRed.copy(alpha = 0.85f), rowShape)
                        } else if (rowIssue?.severity == Severity.WARNING) {
                            Modifier.border(1.2.dp, Color(0xFFFFB703).copy(alpha = 0.7f), rowShape)
                        } else Modifier
                    )
            ) {
                val isAnchor = state.anchorFreqs.contains(row.freq)
                OppEditRow(
                    index = index,
                    row = row,
                    isOcTier = isOc,
                    isAnchor = isAnchor,
                    rowIssue = rowIssue,
                    onUpdate = { newRow -> onUpdateRow(index, newRow) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))

            // Caution Banner
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF3A2800).copy(0.35f) else Color(0xFFFFF3CD).copy(0.85f))
                    .border(0.8.dp, if (isDark) Color(0xFFFFB703).copy(0.25f) else Color(0xFFFFC107).copy(0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFFB703) else Color(0xFF856404),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Text(
                        text = "Caution: Be careful when modifying frequency and voltage values in the kernel module (.ko). Extreme values may cause kernel panic, instability, or bootloop upon loading.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = if (isDark) Color(0xFFFFE082) else Color(0xFF856404)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OppEditRow(
    index: Int,
    row: OppRow,
    isOcTier: Boolean = false,
    isAnchor: Boolean = false,
    rowIssue: Issue? = null,
    onUpdate: (OppRow) -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    val inputBg = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.60f) else Color(0xFFE5E5EA).copy(alpha = 0.60f)
    val borderStroke = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    var freqInput by remember(row.freq) { mutableStateOf(row.freq.toString()) }
    var voltInput by remember(row.volt) { mutableStateOf(row.volt.toString()) }
    var vsramInput by remember(row.vsram) { mutableStateOf(row.vsram.toString()) }
    var posdivInput by remember(row.posdiv) { mutableStateOf(row.posdiv.toString()) }
    var vagingInput by remember(row.vaging) { mutableStateOf(row.vaging.toString()) }

    // Live preview formatting
    val currentFreqParsed = freqInput.toIntOrNull() ?: 0
    val freqMhz = currentFreqParsed / 1000.0
    val freqPreviewText = if (currentFreqParsed >= 1000) {
        if (freqMhz % 1.0 == 0.0) String.format(Locale.getDefault(), "%.0f MHz", freqMhz)
        else String.format(Locale.getDefault(), "%.1f MHz", freqMhz)
    } else {
        "$currentFreqParsed kHz"
    }

    val currentVoltParsed = voltInput.toIntOrNull() ?: 0
    val voltMv = currentVoltParsed / 100.0
    val voltPreviewText = String.format(Locale.getDefault(), "%.2f mV", voltMv)

    val isPllValid = OppEngine.pllReadbackOk(row.freq, row.posdiv)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Row Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                isAnchor -> Color(0xFFFFB703).copy(alpha = 0.20f)
                                isOcTier -> SystemPurple.copy(alpha = 0.20f)
                                else -> SystemBlue.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when {
                            isAnchor -> "⚓ ANCHOR #$index"
                            isOcTier -> "OC #$index"
                            else -> "OPP #$index"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = when {
                            isAnchor -> Color(0xFFFFB703)
                            isOcTier -> SystemPurple
                            else -> SystemBlue
                        }
                    )
                }

                Text(
                    text = "$freqPreviewText • $voltPreviewText",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 1: Freq and Volt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Frequency Input Box
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp, start = 2.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Freq (kHz)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = textSecondary
                    )
                    Text(
                        text = freqPreviewText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = SystemPurple
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(inputBg)
                        .border(1.dp, borderStroke, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = freqInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }.take(7)
                            freqInput = filtered
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null) {
                                onUpdate(row.copy(freq = parsed))
                            }
                        },
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(SystemPurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Stepper for Frequency
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(inputBg)
                            .clickable {
                                val newFreq = (row.freq - 25000).coerceAtLeast(50000)
                                val newPosdiv = (0..3).firstOrNull { OppEngine.pllReadbackOk(newFreq, it) } ?: row.posdiv
                                onUpdate(row.copy(freq = newFreq, posdiv = newPosdiv))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-25M", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(inputBg)
                            .clickable {
                                val newFreq = (row.freq + 25000).coerceAtMost(3000000)
                                val newPosdiv = (0..3).firstOrNull { OppEngine.pllReadbackOk(newFreq, it) } ?: row.posdiv
                                onUpdate(row.copy(freq = newFreq, posdiv = newPosdiv))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+25M", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SystemPurple)
                    }
                }
            }

            // Core Voltage Input Box
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp, start = 2.dp, end = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Volt (10uV)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = textSecondary
                    )
                    Text(
                        text = voltPreviewText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = SystemGreen
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(inputBg)
                        .border(1.dp, borderStroke, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = voltInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }.take(7)
                            voltInput = filtered
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null) {
                                onUpdate(row.copy(volt = parsed))
                            }
                        },
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(SystemGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Stepper for Voltage
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(inputBg)
                            .clickable {
                                val newVolt = maxOf(50000, OppEngine.snap625Up(row.volt - 6250))
                                val newVsram = maxOf(newVolt, row.vsram)
                                onUpdate(row.copy(volt = newVolt, vsram = newVsram))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-6.25m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SystemGreen)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(inputBg)
                            .clickable {
                                val newVolt = OppEngine.snap625Up((row.volt + 6250).coerceAtMost(1500000))
                                val newVsram = maxOf(newVolt, row.vsram)
                                onUpdate(row.copy(volt = newVolt, vsram = newVsram))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+6.25m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Vsram, Posdiv, Vaging
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Vsram Box
            Column(modifier = Modifier.weight(1.2f)) {
                val vsramParsed = vsramInput.toIntOrNull() ?: 0
                val vsramMv = String.format(Locale.getDefault(), "%.1fmV", vsramParsed / 100.0)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp, start = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vsram", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textSecondary)
                    Text(vsramMv, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = SystemPurple)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(inputBg)
                        .border(1.dp, borderStroke, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = vsramInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }.take(7)
                            vsramInput = filtered
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null) {
                                onUpdate(row.copy(vsram = parsed))
                            }
                        },
                        textStyle = TextStyle(color = textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(SystemPurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Posdiv Box
            Column(modifier = Modifier.weight(0.9f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp, start = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Posdiv", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textSecondary)
                    Text(
                        text = if (isPllValid) "PLL OK" else "PLL ERR",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                        color = if (isPllValid) SystemGreen else SystemRed,
                        modifier = if (!isPllValid) Modifier.clickable {
                            val valid = (0..3).firstOrNull { OppEngine.pllReadbackOk(row.freq, it) }
                            if (valid != null) onUpdate(row.copy(posdiv = valid))
                        } else Modifier
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(inputBg)
                        .border(
                            1.dp,
                            if (isPllValid) borderStroke else SystemRed.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = posdivInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }.take(2)
                            posdivInput = filtered
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null) {
                                onUpdate(row.copy(posdiv = parsed))
                            }
                        },
                        textStyle = TextStyle(color = textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(SystemPurple),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Vaging Box
            Column(modifier = Modifier.weight(1.1f)) {
                Text("Vaging", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textSecondary, modifier = Modifier.padding(bottom = 2.dp, start = 2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(inputBg)
                        .border(1.dp, borderStroke, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = vagingInput,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }.take(6)
                            vagingInput = filtered
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null) {
                                onUpdate(row.copy(vaging = parsed))
                            }
                        },
                        textStyle = TextStyle(color = textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(Color(0xFFFFB703)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (row.vsram < row.volt) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(SystemRed.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vsram (${row.vsram / 100}mV) < Core Volt (${row.volt / 100}mV)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SystemRed
                )
                TextButton(
                    onClick = { onUpdate(row.copy(vsram = row.volt)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text("Fix Vsram=Volt", fontSize = 11.sp, color = Color(0xFFFFB703), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (rowIssue != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (rowIssue.severity == Severity.ERROR) SystemRed.copy(alpha = 0.12f) else Color(0xFFFFB703).copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (rowIssue.severity == Severity.ERROR) SystemRed else Color(0xFFFFB703),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = rowIssue.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (rowIssue.severity == Severity.ERROR) SystemRed else Color(0xFFFFB703)
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
    label: String, 
    value: String, 
    icon: ImageVector? = null,
    badgeColor: Color = SystemBlue
) {
    val isDark = LocalThemeIsDark.current
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(badgeColor, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = textPrimary)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace), color = textSecondary)
    }
}

@Composable
fun SettingsInputRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = SystemBlue,
                fontSize = 17.sp,
                textAlign = TextAlign.End,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(SystemBlue),
            modifier = Modifier.width(120.dp)
        )
    }
}

@Composable
fun SuccessContent(message: String, onOk: () -> Unit) {
    val isDark = LocalThemeIsDark.current
    val SecondaryText = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.glassCard().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(SystemGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(IconCheckCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = SystemGreen)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Success", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, textAlign = TextAlign.Center, color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onOk,
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Done", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    val isDark = LocalThemeIsDark.current
    val SecondaryText = if (isDark) Color(0xFFEBEBF5).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f)
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.glassCard().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(SystemRed.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("!", style = MaterialTheme.typography.headlineLarge, color = SystemRed)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Error", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, textAlign = TextAlign.Center, color = SecondaryText, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Try Again", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}
