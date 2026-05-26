package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.WalletSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.PearlScreen
import com.example.ui.viewmodel.TransactTab
import com.example.ui.viewmodel.WalletViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PearlWalletApp(viewModel: WalletViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Stateful quick swap modal triggers
    var isQuickSwapVisible by remember { mutableStateOf(false) }

    // Seed phrase display dialog trigger
    var isSeedPhraseVisible by remember { mutableStateOf(false) }

    // Camera Scan view overlay
    var isCameraScanning by remember { mutableStateOf(false) }

    // Biometrics dialog trigger
    val isBiometricPromptVisible by viewModel.isBiometricPromptVisible.collectAsStateWithLifecycle()
    val biometricStatusMessage by viewModel.biometricStatusMessage.collectAsStateWithLifecycle()
    val biometricApproved by viewModel.biometricApproved.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianDeep,
        topBar = {
            PearlHeader(
                viewModel = viewModel,
                onProfileClick = {
                    isSeedPhraseVisible = true
                }
            )
        },
        bottomBar = {
            PearlBottomNavigation(
                currentScreen = currentScreen,
                onScreenSelected = { viewModel.setScreen(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen transition mapping
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                label = "ScreenSwitch"
            ) { targetScreen ->
                when (targetScreen) {
                    PearlScreen.WALLET -> {
                        WalletDashboard(
                            viewModel = viewModel,
                            onSwapClick = { isQuickSwapVisible = true },
                            onSendClick = {
                                viewModel.sendAsset.value = "PRL"
                                viewModel.setTransactTab(TransactTab.SEND)
                                viewModel.setScreen(PearlScreen.TRANSACT)
                            },
                            onReceiveClick = {
                                viewModel.sendAsset.value = "PRL"
                                viewModel.setTransactTab(TransactTab.RECEIVE)
                                viewModel.setScreen(PearlScreen.TRANSACT)
                            }
                        )
                    }
                    PearlScreen.HISTORY -> {
                        HistoryDashboard(viewModel = viewModel)
                    }
                    PearlScreen.TRANSACT -> {
                        TransactDashboard(
                            viewModel = viewModel,
                            onScanTrigger = { isCameraScanning = true }
                        )
                    }
                    PearlScreen.SETTINGS -> {
                        SettingsDashboard(
                            viewModel = viewModel,
                            onShowSeedClick = { isSeedPhraseVisible = true }
                        )
                    }
                }
            }

            // Quick Swap Floating Sheet
            if (isQuickSwapVisible) {
                QuickSwapSheet(
                    viewModel = viewModel,
                    onDismiss = { isQuickSwapVisible = false }
                )
            }

            // Biometric scan overlay simulation
            if (isBiometricPromptVisible) {
                BiometricPromptOverlay(
                    statusMessage = biometricStatusMessage,
                    isApproved = biometricApproved,
                    onDismiss = { viewModel.dismissBiometricPrompt() }
                )
            }

            // Custom Simulated QR Camera Scanner view
            if (isCameraScanning) {
                MockScannerView(
                    onScanSuccess = { scannedAddress ->
                        viewModel.sendAddress.value = scannedAddress
                        isCameraScanning = false
                        Toast.makeText(context, "Address matched securely", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { isCameraScanning = false }
                )
            }

            // Recovery phrase safety locker
            if (isSeedPhraseVisible) {
                SeedPhraseBackupDialog(
                    settings = viewModel.settings.collectAsStateWithLifecycle().value,
                    onBackupApproved = {
                        viewModel.markSeedBackedUp()
                        isSeedPhraseVisible = false
                        Toast.makeText(context, "Backup Verified", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { isSeedPhraseVisible = false }
                )
            }
        }
    }
}

// 1. Customized glassmorphic card helper
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DeepObsidian.copy(alpha = 0.75f),
    borderColor: Color = CyanPearl.copy(alpha = 0.25f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            borderColor,
                            SecondaryGold.copy(alpha = 0.12f),
                            AccentGreen.copy(alpha = 0.22f)
                        )
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}

// 2. Main Top Application Bar
@Composable
fun PearlHeader(
    viewModel: WalletViewModel,
    onProfileClick: () -> Unit
) {
    val livePing by viewModel.pingMs.collectAsStateWithLifecycle()
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()
    val isTestnet = currentSettings.developerModeEnabled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left sensors connectivity indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    val label = if (isTestnet) "Testnet" else "Mainnet"
                    Toast
                        .makeText(
                            viewModel.getApplication(),
                            "Connected: wss://${if (isTestnet) "testnet" else "mainnet"}.pearl.network",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                // Animated pulse glow
                val pulseScale by rememberInfiniteTransition(label = "").animateFloat(
                    initialValue = 0.7f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse"
                )
                val pulseAlpha by rememberInfiniteTransition(label = "").animateFloat(
                    initialValue = 0.8f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "alpha"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (isTestnet) SecondaryGold else CyanPearl,
                        radius = (size.minDimension / 3) * pulseScale,
                        alpha = pulseAlpha
                    )
                    drawCircle(
                        color = if (isTestnet) SecondaryGold else CyanPearl,
                        radius = size.minDimension / 4
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${livePing}ms${if(isTestnet) " dev" else ""}",
                color = if (isTestnet) SecondaryGold.copy(alpha=0.8f) else CyanPearl.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // PEARL Branding logo title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(x = (-8).dp)
        ) {
            // Elegant geometric custom emblem
            Canvas(modifier = Modifier.size(22.dp)) {
                drawArc(
                    color = Color.White,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width * 0.75f, size.height)
                )
                drawCircle(
                    color = CyanPearl,
                    radius = size.width * 0.2f,
                    center = Offset(size.width * 0.75f, size.height * 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "PEARL",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
        }

        // Profile / Backup phrase circular indicator
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .size(38.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clip(CircleShape)
                .background(DeepObsidian),
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Wallet profile key locker",
                tint = if (currentSettings.seedPhraseBackedUp) AccentGreen else OnSurfaceVariant
            )
        }
    }
}

// 3. Simulated Canvas bezier line performance sparklines
@Composable
fun PerformanceSparkline(points: List<Float>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return

    val pathAnimState = remember { Animatable(0f) }
    LaunchedEffect(points) {
        pathAnimState.snapTo(0f)
        pathAnimState.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
        )
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val maxVal = points.maxOrNull() ?: 1f
            val minVal = points.minOrNull() ?: 0f
            val range = (maxVal - minVal).coerceAtLeast(1f)

            val pxPoints = points.mapIndexed { idx, value ->
                val x = if (points.size > 1) {
                    (idx.toFloat() / (points.size - 1)) * width
                } else {
                    width / 2f
                }
                val y = height - (((value - minVal) / range) * (height - 24.dp.toPx()) + 12.dp.toPx())
                Offset(x, y)
            }

            val strokePath = Path().apply {
                if (pxPoints.isNotEmpty()) {
                    moveTo(pxPoints.first().x, pxPoints.first().y)
                    for (i in 1 until pxPoints.size) {
                        val prev = pxPoints[i - 1]
                        val curr = pxPoints[i]
                        // Bezier curve calculations
                        val cp1X = prev.x + (curr.x - prev.x) / 2
                        val cp1Y = prev.y
                        val cp2X = prev.x + (curr.x - prev.x) / 2
                        val cp2Y = curr.y
                        cubicTo(cp1X, cp1Y, cp2X, cp2Y, curr.x, curr.y)
                    }
                }
            }

            // Animate stroke length
            drawPath(
                path = strokePath,
                color = CyanPearl,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                alpha = pathAnimState.value
            )

            // Dynamic Gradient fill under curve
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CyanPearl.copy(alpha = 0.25f),
                        TransparentTealGlow.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                ),
                alpha = pathAnimState.value
            )
        }
    }
}

// 4. Custom Bottom Shell Navigation Component
@Composable
fun PearlBottomNavigation(
    currentScreen: PearlScreen,
    onScreenSelected: (PearlScreen) -> Unit
) {
    Surface(
        color = DeepObsidian.copy(alpha = 0.96f),
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .height(72.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                iconSelected = Icons.Default.Wallet,
                iconUnselected = Icons.Default.Wallet,
                label = "Wallet",
                isSelected = currentScreen == PearlScreen.WALLET,
                onClick = { onScreenSelected(PearlScreen.WALLET) },
                testTag = "nav_wallet"
            )
            BottomNavItem(
                iconSelected = Icons.Default.ReceiptLong,
                iconUnselected = Icons.Default.ReceiptLong,
                label = "History",
                isSelected = currentScreen == PearlScreen.HISTORY,
                onClick = { onScreenSelected(PearlScreen.HISTORY) },
                testTag = "nav_history"
            )
            BottomNavItem(
                iconSelected = Icons.Default.SwapHoriz,
                iconUnselected = Icons.Default.SwapHoriz,
                label = "Transact",
                isSelected = currentScreen == PearlScreen.TRANSACT,
                onClick = { onScreenSelected(PearlScreen.TRANSACT) },
                testTag = "nav_transact"
            )
            BottomNavItem(
                iconSelected = Icons.Default.Settings,
                iconUnselected = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentScreen == PearlScreen.SETTINGS,
                onClick = { onScreenSelected(PearlScreen.SETTINGS) },
                testTag = "nav_settings"
            )
        }
    }
}

@Composable
fun BottomNavItem(
    iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    iconUnselected: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val duration = 200
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 0.95f,
        animationSpec = tween(duration),
        label = "itemScale"
    )
    val glowSize by animateDpAsState(
        targetValue = if (isSelected) 10.dp else 0.dp,
        animationSpec = tween(duration),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .testTag(testTag)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(8.dp)
            .width(68.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .width(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) CyanPearl.copy(alpha = 0.15f) else Color.Transparent
                )
                .drawBehind {
                    if (isSelected) {
                        drawCircle(
                            color = CyanPearl,
                            alpha = 0.25f,
                            radius = (size.minDimension / 1.5f),
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconSelected else iconUnselected,
                contentDescription = label,
                tint = if (isSelected) CyanPearl else OnSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else OnSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// 5. VIEW A: MAIN WALLET OVERVIEW DASHBOARD
@Composable
fun WalletDashboard(
    viewModel: WalletViewModel,
    onSwapClick: () -> Unit,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val prlPrice by viewModel.prlPrice.collectAsStateWithLifecycle()
    val btcPrice by viewModel.btcPrice.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var chartRangeSelected by remember { mutableStateOf("1W") }
    val sparkPoints = remember(chartRangeSelected) {
        when (chartRangeSelected) {
            "1D" -> listOf(1.23f, 1.25f, 1.23f, 1.28f, 1.22f, 1.24f)
            "1W" -> listOf(1.15f, 1.22f, 1.18f, 1.25f, 1.19f, 1.23f, 1.24f)
            "1M" -> listOf(1.10f, 1.12f, 1.05f, 1.18f, 1.21f, 1.28f, 1.25f, 1.24f)
            else -> listOf(0.85f, 0.95f, 0.90f, 1.05f, 1.12f, 1.26f, 1.24f, 1.24f) // 1Y
        }
    }

    // Dynamic state evaluation
    val prlCount = 24592.80
    val btcCount = 0.45
    val currentPrlValue = prlCount * prlPrice
    val currentBtcValue = btcCount * btcPrice
    val totalWalletValue = currentPrlValue + currentBtcValue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High polish Balance Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL BALANCE",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%,.2f".format(totalWalletValue),
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "USD",
                        color = CyanPearl,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Wallet positive gains tracker",
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+$1,204.50 (5.2%)",
                        color = AccentGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSendClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_send"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPearl,
                            contentColor = ObsidianDeep
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReceiveClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_receive"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerHighest,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Receive", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSwapClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_swap"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceContainerHighest,
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Swap", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Performance Chart Glass Card
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERFORMANCE",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val ranges = listOf("1D", "1W", "1M", "1Y")
                    ranges.forEach { range ->
                        val isSel = chartRangeSelected == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) CyanPearl else Color.Transparent)
                                .clickable { chartRangeSelected = range }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) ObsidianDeep else OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graph canvas rendering
            PerformanceSparkline(
                points = sparkPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
        }

        // Your Assets list card panel
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "YOUR ASSETS",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dynamic flashing WS feedback state calculations
            val prlTickColor by viewModel.prlTickColor.collectAsStateWithLifecycle()
            val btcTickColor by viewModel.btcTickColor.collectAsStateWithLifecycle()

            val prlFlasherColor = when (prlTickColor) {
                1 -> AccentGreen.copy(alpha = 0.25f)
                -1 -> ErrorRed.copy(alpha = 0.25f)
                else -> Color.Transparent
            }
            val btcFlasherColor = when (btcTickColor) {
                1 -> AccentGreen.copy(alpha = 0.25f)
                -1 -> ErrorRed.copy(alpha = 0.25f)
                else -> Color.Transparent
            }

            // Asset Row: Pearl (PRL)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(prlFlasherColor)
                    .clickable { }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .border(1.dp, CyanPearl.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PRL", color = CyanPearl, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pearl", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$%.2f".format(prlPrice), color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%,2f".format(prlCount), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("+$%.2f".format(currentPrlValue), color = AccentGreen, style = MaterialTheme.typography.labelSmall)
                }
            }

            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))

            // Asset Row: Bitcoin (BTC)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(btcFlasherColor)
                    .clickable { }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .border(1.dp, SecondaryGold.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("BTC", color = SecondaryGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bitcoin", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$%,.2f".format(btcPrice), color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$btcCount", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("~$%,.2f".format(currentBtcValue), color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Recent Activity (displays top 3)
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT ACTIVITY",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "View All",
                    color = CyanPearl,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        viewModel.setScreen(PearlScreen.HISTORY)
                    }
                )
            }

            val recentTxs = transactions.take(3)
            if (recentTxs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions logged yet", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentTxs.forEach { tx ->
                        ActivityRow(tx = tx)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Inline Activity row utility
@Composable
fun ActivityRow(tx: Transaction) {
    val df = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedDate = df.format(Date(tx.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconColor = when (tx.type) {
            "RECEIVED" -> AccentGreen
            "SENT" -> OnSurfaceVariant
            "STAKED" -> SecondaryGold
            else -> CyanPearl // "SWAPPED"
        }
        val iconVector = when (tx.type) {
            "RECEIVED" -> Icons.Default.ArrowDownward
            "SENT" -> Icons.Default.ArrowUpward
            "STAKED" -> Icons.Default.LockClock
            else -> Icons.Default.SwapHoriz
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val title = when (tx.type) {
                "RECEIVED" -> "Received ${tx.assetSymbol}"
                "SENT" -> "Sent ${tx.assetSymbol}"
                "STAKED" -> "Backup Stake"
                "SWAPPED" -> "Swapped ${tx.counterSymbol} to ${tx.assetSymbol}"
                else -> "Smart Contract"
            }
            Text(
                text = title,
                color = if (tx.status == "REVERTED") ErrorRed else Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tx.address,
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val symbol = if (tx.type == "RECEIVED" || tx.type == "SWAPPED") "+" else "-"
            val amtText = "$symbol %,.2f %s".format(tx.amount, tx.assetSymbol)
            
            Text(
                text = amtText,
                color = if (tx.type == "RECEIVED" || tx.status == "SUCCESS" && tx.type == "SWAPPED") AccentGreen else Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formattedDate,
                color = OnSurfaceVariant.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// 6. VIEW B: DETAILED TRANSACTION HISTORY DIARY
@Composable
fun HistoryDashboard(viewModel: WalletViewModel) {
    val txs by viewModel.transactions.collectAsStateWithLifecycle()
    val filter by viewModel.historyFilter.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Dynamic Filter lists items
    val filteredTxs = remember(txs, filter, query) {
        txs.filter { tx ->
            val matchesFilter = when (filter) {
                "SENT" -> tx.type == "SENT"
                "RECEIVED" -> tx.type == "RECEIVED"
                "STAKED" -> tx.type == "STAKED"
                else -> true
            }
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                tx.address.contains(query, ignoreCase = true) ||
                tx.type.contains(query, ignoreCase = true) ||
                tx.assetSymbol.contains(query, ignoreCase = true) ||
                tx.amount.toString().contains(query)
            }
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Transaction History",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Review your sent, received, and staked Pearl assets.",
            color = OnSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Glass Search Input & Tab Controls
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_bar"),
            placeholder = { Text("Search by address, hash or amount...", color = OnSurfaceVariant.copy(alpha=0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = OnSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedBorderColor = CyanPearl,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal filter selectors
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf("ALL", "SENT", "RECEIVED", "STAKED")
            filterOptions.forEach { opt ->
                val isSel = filter == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) CyanPearl else Color.White.copy(alpha = 0.05f))
                        .clickable { viewModel.setHistoryFilter(opt) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) ObsidianDeep else OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List representation
        if (filteredTxs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = OnSurfaceVariant.copy(alpha=0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (query.isNotEmpty()) "No query results found." else "No history in this category",
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredTxs) { tx ->
                    HistoryCard(tx = tx)
                }
            }
        }
    }
}

// Stateful list item Card for History
@Composable
fun HistoryCard(tx: Transaction) {
    val df = remember { SimpleDateFormat("HH:mm, MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatted = df.format(Date(tx.timestamp))

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DeepObsidian.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = when (tx.status) {
                "REVERTED" -> ErrorRed
                "PENDING" -> SecondaryGold
                else -> AccentGreen
            }
            val statusIcon = when (tx.status) {
                "REVERTED" -> Icons.Default.Cancel
                "PENDING" -> Icons.Default.HourglassEmpty
                else -> Icons.Default.CheckCircle
            }

            // Direction symbol
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                val vector = when (tx.type) {
                    "RECEIVED" -> Icons.Default.ArrowDownward
                    "SENT" -> Icons.Default.ArrowUpward
                    "STAKED" -> Icons.Default.Lock
                    else -> Icons.Default.SwapHoriz
                }
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val title = when (tx.type) {
                    "RECEIVED" -> "Received ${tx.assetSymbol}"
                    "SENT" -> "Sent ${tx.assetSymbol}"
                    "STAKED" -> "Staking Committed"
                    "SWAPPED" -> "Uniswap Exchange"
                    else -> "Transaction"
                }
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tx.address,
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeFormatted,
                    color = OnSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val sign = if (tx.type == "RECEIVED" || tx.type == "SWAPPED") "+" else "-"
                Text(
                    text = "$sign %,.2f %s".format(tx.amount, tx.assetSymbol),
                    color = if (tx.status == "REVERTED") ErrorRed else Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = statusIcon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (tx.status == "PENDING") "Processing (2/12)" else tx.status.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 7. VIEW C: TRANSACT DIAL (SEND vs RECEIVE)
@Composable
fun TransactDashboard(
    viewModel: WalletViewModel,
    onScanTrigger: () -> Unit
) {
    val tabSelected by viewModel.currentTransactTab.collectAsStateWithLifecycle()
    val sendAssetSelected by viewModel.sendAsset.collectAsStateWithLifecycle()
    val prlPrice by viewModel.prlPrice.collectAsStateWithLifecycle()
    val btcPrice by viewModel.btcPrice.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val balanceLimit = if (sendAssetSelected == "PRL") 24592.80 else 0.45
    val assetPrice = if (sendAssetSelected == "PRL") prlPrice else btcPrice

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Tab Header send/receive
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val tabs = listOf(TransactTab.SEND, TransactTab.RECEIVE)
            tabs.forEach { t ->
                val isSw = tabSelected == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSw) CyanPearl else SurfaceContainer)
                        .clickable { viewModel.setTransactTab(t) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSw) ObsidianDeep else Color.White
                    )
                }
            }
        }

        // Active State Body
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tabSelected,
                transitionSpec = {
                    slideInHorizontally { width -> if (tabSelected == TransactTab.RECEIVE) width else -width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> if (tabSelected == TransactTab.RECEIVE) -width else width } + fadeOut()
                },
                label = "tabSwipe"
            ) { activeTab ->
                when (activeTab) {
                    TransactTab.SEND -> {
                        SendTab(
                            viewModel = viewModel,
                            sendAssetSelected = sendAssetSelected,
                            balanceLimit = balanceLimit,
                            assetPrice = assetPrice,
                            onScanTrigger = onScanTrigger,
                            onTransferInitiated = {
                                Toast.makeText(context, "Transacting successfully backed to chain!", Toast.LENGTH_LONG).show()
                                viewModel.setScreen(PearlScreen.WALLET)
                            }
                        )
                    }
                    TransactTab.RECEIVE -> {
                        ReceiveTab(
                            sendAssetSelected = sendAssetSelected,
                            onAssetChanged = { viewModel.sendAsset.value = it }
                        )
                    }
                }
            }
        }
    }
}

// Stateful Send Body View
@Composable
fun SendTab(
    viewModel: WalletViewModel,
    sendAssetSelected: String,
    balanceLimit: Double,
    assetPrice: Double,
    onScanTrigger: () -> Unit,
    onTransferInitiated: () -> Unit
) {
    val sendAmount by viewModel.sendAmount.collectAsStateWithLifecycle()
    val sendAddress by viewModel.sendAddress.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var isSelectorDropdownOpen by remember { mutableStateOf(false) }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Selector choice
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { isSelectorDropdownOpen = true }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sendAssetSelected,
                            color = if (sendAssetSelected == "PRL") CyanPearl else SecondaryGold,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (sendAssetSelected == "PRL") "Pearl" else "Bitcoin",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = sendAssetSelected,
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, tint = OnSurfaceVariant)
            }

            DropdownMenu(
                expanded = isSelectorDropdownOpen,
                onDismissRequest = { isSelectorDropdownOpen = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(SurfaceContainerLow)
            ) {
                DropdownMenuItem(
                    text = { Text("Pearl (PRL)", color = Color.White) },
                    onClick = {
                        viewModel.sendAsset.value = "PRL"
                        isSelectorDropdownOpen = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Bitcoin (BTC)", color = Color.White) },
                    onClick = {
                        viewModel.sendAsset.value = "BTC"
                        isSelectorDropdownOpen = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center big number input
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicTextField(
                value = sendAmount,
                onValueChange = {
                    // Lock digits filter
                    if (it.isEmpty() || it.toDoubleOrNull() != null || it.last() == '.') {
                        viewModel.sendAmount.value = it
                    }
                },
                modifier = Modifier.testTag("amount_input"),
                textStyle = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                cursorBrush = SolidColor(CyanPearl)
            )

            val parsedValue = sendAmount.toDoubleOrNull() ?: 0.0
            Text(
                text = "~ $%,.2f USD".format(parsedValue * assetPrice),
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // MAX tag pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CyanPearl.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.sendAmount.value = balanceLimit.toString()
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "MAX",
                    color = CyanPearl,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recipient Address text field container
        Text(
            text = "To Address",
            color = OnSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = sendAddress,
            onValueChange = { viewModel.sendAddress.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("address_input"),
            placeholder = { Text("Paste or scan address", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.02f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                focusedBorderColor = CyanPearl,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
            ),
            trailingIcon = {
                Row(modifier = Modifier.padding(end = 4.dp)) {
                    IconButton(onClick = {
                        clipboardManager.getText()?.let {
                            viewModel.sendAddress.value = it.text
                        } ?: Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste from clipboard", tint = OnSurfaceVariant)
                    }
                    IconButton(onClick = onScanTrigger) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan target QR", tint = OnSurfaceVariant)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // review button
        Button(
            onClick = {
                val inputAmt = sendAmount.toDoubleOrNull()
                if (inputAmt == null || inputAmt <= 0) {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (inputAmt > balanceLimit) {
                    Toast.makeText(context, "Insufficient balance!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (sendAddress.trim().isEmpty()) {
                    Toast.makeText(context, "Enter recipient address", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Call biometric simulation
                viewModel.showBiometricConfirmation {
                    viewModel.processSendTx(
                        address = sendAddress,
                        amountDouble = inputAmt,
                        asset = sendAssetSelected,
                        onComplete = onTransferInitiated
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("review_send_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPearl,
                contentColor = ObsidianDeep
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Review Send", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// Receive Tab Panel UI
@Composable
fun ReceiveTab(
    sendAssetSelected: String,
    onAssetChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val targetAddress = if (sendAssetSelected == "PRL") {
        "0x5aaee4b3734c679aab4a1c1d89bf1a4f06879f2b"
    } else {
        "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
    }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your ${if (sendAssetSelected == "PRL") "Pearl" else "Bitcoin"} Address",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Network: ${if (sendAssetSelected == "PRL") "Pearl Chain (PRL)" else "Bitcoin (BTC)"}",
                color = CyanPearl,
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Inverted white styled QR Code box
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(targetAddress))
                        Toast.makeText(context, "Address copied safely", Toast.LENGTH_SHORT).show()
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Draws a realistic block abstract pattern simulating a QR barcode
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val steps = 14
                    val stepW = size.width / steps
                    val stepH = size.height / steps

                    // Draw corner search anchor squares
                    drawRect(Color(0xFF060A14), topLeft = Offset(0f, 0f), size = Size(stepW * 4, stepH * 4))
                    drawRect(Color.White, topLeft = Offset(stepW, stepH), size = Size(stepW * 2, stepH * 2))

                    drawRect(Color(0xFF060A14), topLeft = Offset(size.width - (stepW * 4), 0f), size = Size(stepW * 4, stepH * 4))
                    drawRect(Color.White, topLeft = Offset(size.width - (stepW * 3), stepH), size = Size(stepW * 2, stepH * 2))

                    drawRect(Color(0xFF060A14), topLeft = Offset(0f, size.height - (stepH * 4)), size = Size(stepW * 4, stepH * 4))
                    drawRect(Color.White, topLeft = Offset(stepW, size.height - (stepH * 3)), size = Size(stepW * 2, stepH * 2))

                    // Draw randomized bits
                    val rand = Random(42)
                    for (c in 0 until steps) {
                        for (r in 0 until steps) {
                            if ((c < 4 && r < 4) || (c >= steps - 4 && r < 4) || (c < 4 && r >= steps - 4)) {
                                continue
                            }
                            if (rand.nextBoolean()) {
                                drawRect(
                                    Color(0xFF060A14),
                                    topLeft = Offset(c * stepW, r * stepH),
                                    size = Size(stepW, stepH)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Read Address Display box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(targetAddress))
                        Toast.makeText(context, "Address copied safely", Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = targetAddress,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy address", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share Save Bottom Row Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Share sheet simulation triggered", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    border = BorderStroke(1.dp, CyanPearl.copy(alpha=0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "QR saved securely to local gallery", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    border = BorderStroke(1.dp, CyanPearl.copy(alpha=0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick network change choices for test/main
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                val assets = listOf("PRL", "BTC")
                assets.forEach { item ->
                    val isS = sendAssetSelected == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isS) CyanPearl.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                            .clickable { onAssetChanged(item) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(item, color = if (isS) CyanPearl else OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 8. VIEW D: SYSTEM SETTINGS AND PREFERENCES WIDGET
@Composable
fun SettingsDashboard(
    viewModel: WalletViewModel,
    onShowSeedClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "Settings",
                color = Color.White,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure your wallet experience.",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Section: Security details
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SECURITY", color = CyanPearl, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)
            
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                // Biometrics toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = CyanPearl, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Biometric Authentication", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("FaceID / TouchID", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Switch(
                        checked = settings.biometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometrics(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianDeep,
                            checkedTrackColor = CyanPearl,
                            uncheckedThumbColor = OnSurfaceVariant,
                            uncheckedTrackColor = SurfaceContainerHighest
                        )
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // Recovery backup key phrase row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowSeedClick() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Recovery Phrase", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val backupText = if (settings.seedPhraseBackedUp) "Backup Verified" else "Backup your wallet"
                            Text(
                                text = backupText,
                                color = if (settings.seedPhraseBackedUp) AccentGreen else OnSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (settings.seedPhraseBackedUp) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }
            }
        }

        // Section: Network options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("NETWORK", color = CyanPearl, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)

            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Hub, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Active Network", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(settings.activeNetwork, color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Developer Mode", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Enable Testnet & Advanced Features", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Switch(
                        checked = settings.developerModeEnabled,
                        onCheckedChange = { viewModel.toggleDeveloperMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianDeep,
                            checkedTrackColor = CyanPearl,
                            uncheckedThumbColor = OnSurfaceVariant,
                            uncheckedTrackColor = SurfaceContainerHighest
                        )
                    )
                }
            }
        }

        // Section Preferences
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PREFERENCES", color = CyanPearl, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.5.sp)

            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                // Local currency Choice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val currencies = listOf("USD", "EUR", "GBP", "JPY")
                            val nextIdx = (currencies.indexOf(settings.localCurrency) + 1) % currencies.size
                            viewModel.setLocalCurrency(currencies[nextIdx])
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Local Currency", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(settings.localCurrency, color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }

                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // Languages Selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val langs = listOf("English (US)", "Español", "日本語", "Deutsch")
                            val nextIdx = (langs.indexOf(settings.language) + 1) % langs.size
                            viewModel.setLanguage(langs[nextIdx])
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Language", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(settings.language, color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Trigger: Disconnect trigger button
        TextButton(
            onClick = {
                viewModel.performDisconnectWallet()
                Toast.makeText(context, "Wallet cleared successfully. Reseeded starter balances.", Toast.LENGTH_LONG).show()
                viewModel.setScreen(PearlScreen.WALLET)
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(14.dp)
                )
                .height(54.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Disconnect Wallet",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // PEARL footer copyright
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PEARL Core v2.4.1",
                color = OnSurfaceVariant.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Build 89a4f2c",
                color = OnSurfaceVariant.copy(alpha = 0.25f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// 9. DIALOG MODULES & GLASS OVERLAYS

// Component A: Stateful Swap Popup Sheet
@Composable
fun QuickSwapSheet(
    viewModel: WalletViewModel,
    onDismiss: () -> Unit
) {
    var fromAsset by remember { mutableStateOf("USDC") }
    var toAsset by remember { mutableStateOf("PRL") }
    var fromAmount by remember { mutableStateOf("") }

    val prlPrice by viewModel.prlPrice.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Conversion mathematics
    val parsedAmt = fromAmount.toDoubleOrNull() ?: 0.0
    // USDC is 1 USD. PRL is prlPrice.
    val convertedAmt = if (fromAsset == "USDC") parsedAmt / prlPrice else parsedAmt * prlPrice

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {}
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick Swap", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Swap FROM Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh)
                        .padding(14.dp)
                ) {
                    Text("From Asset", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = fromAsset,
                            color = CyanPearl,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        BasicTextField(
                            value = fromAmount,
                            onValueChange = { fromAmount = it },
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                textAlign = TextAlign.End
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(140.dp),
                            cursorBrush = SolidColor(CyanPearl)
                        )
                    }
                }

                // Center arrows exchange trigger icon
                IconButton(
                    onClick = {
                        val temp = fromAsset
                        fromAsset = toAsset
                        toAsset = temp
                        fromAmount = ""
                    },
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CyanPearl),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = ObsidianDeep)
                ) {
                    Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                // Swap TO Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceContainerHigh)
                        .padding(14.dp)
                ) {
                    Text("To Asset (Estimate)", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = toAsset,
                            color = AccentGreen,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%,.4f".format(convertedAmt),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val inputVal = fromAmount.toDoubleOrNull()
                        if (inputVal == null || inputVal <= 0.0) {
                            Toast.makeText(context, "Please enter swap amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Process the swap transaction persistently
                        viewModel.processSwapTx(
                            fromAsset = fromAsset,
                            toAsset = toAsset,
                            fromAmount = inputVal,
                            toAmount = convertedAmt
                        )
                        Toast.makeText(context, "Swap executed persistently!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPearl,
                        contentColor = ObsidianDeep
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Confirm Quick Swap", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Component B: Biometric Auth Screen overlay
@Composable
fun BiometricPromptOverlay(
    statusMessage: String,
    isApproved: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Elegant glowing Fingerprint visual anchor
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (isApproved) AccentGreen.copy(alpha = 0.15f) else CyanPearl.copy(alpha = 0.1f)
                    )
                    .border(
                        2.dp,
                        if (isApproved) AccentGreen else CyanPearl.copy(alpha = 0.4f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                val duration = 800
                val scale by rememberInfiniteTransition(label = "").animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(duration, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "biometricScale"
                )

                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = if (isApproved) AccentGreen else CyanPearl,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = if (isApproved) 1f else scale
                            scaleY = if (isApproved) 1f else scale
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Biometric Authorization",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusMessage,
                color = if (isApproved) AccentGreen else OnSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (!isApproved) {
                Spacer(modifier = Modifier.height(32.dp))
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Cancel Signature", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Component C: Mock Camera Scan view
@Composable
fun MockScannerView(
    onScanSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Animated green search bar scan line
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanX"
    )

    // Triggers self success after delay
    LaunchedEffect(Unit) {
        delay(2600)
        onScanSuccess("0x5aaee4b3734c679aab4a1c1d89bf1a4f06879f2b")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Overlay Camera preview viewfinder
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.04f))
        ) {
            // Draws laser line
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * scanLineY
                drawLine(
                    color = CyanPearl,
                    start = Offset(12.dp.toPx(), y),
                    end = Offset(size.width - 12.dp.toPx(), y),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hold frame up to Pearl QR Code",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Will scan recipient address automatically...",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest)
            ) {
                Text("Dismiss Viewfinder", color = Color.White)
            }
        }
    }
}

// Component D: Safety Seed Phrase disclosures
@Composable
fun SeedPhraseBackupDialog(
    settings: WalletSettings,
    onBackupApproved: () -> Unit,
    onDismiss: () -> Unit
) {
    val seedWords = listOf(
        "obsidian", "iridescence", "pearl", "minimalist", 
        "security", "crypto", "transit", "hardware", 
        "tauri", "aurora", "whisper", "kinetic"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
                .border(BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.2f)), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recovery Phrase Lock", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Backup your 12-words key securely offline", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Words grid representation
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = seedWords.chunked(3)
                    rows.forEachIndexed { rIdx, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEachIndexed { cIdx, word ->
                                val wordNumber = rIdx * 3 + cIdx + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainerHigh)
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$wordNumber.",
                                            color = SecondaryGold,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Text(
                                            text = word,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                WarningNote()

                Spacer(modifier = Modifier.height(24.dp))

                if (!settings.seedPhraseBackedUp) {
                    Button(
                        onClick = onBackupApproved,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryGold,
                            contentColor = ObsidianDeep
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("I Have Copied Them Safely", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGreen.copy(alpha = 0.1f))
                            .border(1.dp, AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "This recovery seed phrase backup is attested to disk.",
                            color = AccentGreen,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WarningNote() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Red.copy(alpha = 0.06f))
            .border(1.dp, Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "CRITICAL: Never share these words with anyone. They grant full ownership access to all your assets. Pearl staff will never request these.",
            color = ErrorRed,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}
