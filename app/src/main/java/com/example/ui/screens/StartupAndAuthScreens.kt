package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.GoogleAuthManager
import com.example.data.system.HardwareProfile
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.ChromeBluePrimaryDark
import com.example.ui.theme.LocalFlexDeskTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DeveloperWpWarningScreen(
    hardwareProfile: HardwareProfile,
    onReenableWpWithSpace: () -> Unit,
    onContinueDevAndSuppressWarning: () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current
    val focusRequester = remember { FocusRequester() }
    var secondsRemaining by remember { mutableIntStateOf(10) }
    var handled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (secondsRemaining > 0 && !handled) {
            delay(1000L)
            secondsRemaining--
        }
        if (!handled) {
            handled = true
            onContinueDevAndSuppressWarning()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E131B))
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && !handled) {
                    when {
                        event.key == Key.Spacebar -> {
                            handled = true
                            onReenableWpWithSpace()
                            true
                        }
                        event.key == Key.D && event.isCtrlPressed -> {
                            handled = true
                            onContinueDevAndSuppressWarning()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .testTag("developer_wp_warning_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF171E29),
                tonalElevation = 10.dp,
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = tokens.offlineAmber.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(tokens.offlineAmber.copy(alpha = 0.16f))
                            .border(1.5.dp, tokens.offlineAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Write Protection Off Warning",
                            tint = tokens.offlineAmber,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = strings.wpWarningTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0C1017)
                    ) {
                        Text(
                            text = "DEV_MODE=1 • WP_STATUS=DISABLED • ${hardwareProfile.deviceName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = tokens.offlineAmber,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = strings.wpWarningSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD5DCE8),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Live 10-second timer progress
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1F2836),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.wpAutoBootTimerLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF9EB0C7)
                                )
                                Text(
                                    text = "${secondsRemaining}s",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = tokens.offlineAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (secondsRemaining / 10f).coerceIn(0f, 1f) },
                                color = tokens.offlineAmber,
                                trackColor = Color(0xFF0E131B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Touch / Keyboard Buttons
                    Button(
                        onClick = {
                            if (!handled) {
                                handled = true
                                onReenableWpWithSpace()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChromeBluePrimaryDark,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("wp_warning_space_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.wpSpaceButton,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            if (!handled) {
                                handled = true
                                onContinueDevAndSuppressWarning()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("wp_warning_ctrl_d_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = tokens.offlineAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.wpCtrlDButton,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StartupSplashScreen(
    hardwareProfile: HardwareProfile,
    reduceMotion: Boolean,
    developerModeEnabled: Boolean,
    warmUpProgressLabel: String,
    onContinueNow: () -> Unit
) {
    val strings = LocalStrings.current
    var visible by remember { mutableStateOf(reduceMotion) }
    val logoScale = remember { Animatable(if (reduceMotion) 1f else 0.86f) }
    val logoRotation = remember { Animatable(if (reduceMotion) 0f else -42f) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 420, easing = FastOutSlowInEasing),
        label = "startup_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        if (!reduceMotion) {
            launch {
                logoRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            delay(1150L)
        } else {
            delay(450L)
        }
        onContinueNow()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeDrawingPadding()
            .testTag("boot_animation_screen")
    ) {
        // Pure ChromeOS white boot screen centered horizontal "[Chrome emblem] chrome" lockup (always LTR)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .alpha(alpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ChromeBootLogo(
                    rotationDegrees = logoRotation.value,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(logoScale.value)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "chrome",
                    color = Color(0xFF5F6368),
                    fontSize = 38.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // Minimal, unobtrusive bottom bar for hardware warm-up state & skip action
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (developerModeEnabled) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFF4E5),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "DEV • WP OFF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFB06000),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${hardwareProfile.deviceName} · $warmUpProgressLabel",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9AA0A6),
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = onContinueNow,
                modifier = Modifier.testTag("startup_continue_button")
            ) {
                Text(
                    text = strings.skipBootAnimation,
                    color = Color(0xFF80868B),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ChromeBootLogo(
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val innerWhiteRadius = radius * 0.45f
        val blueRadius = radius * 0.36f

        val outerCircle = Path().apply {
            addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        }

        fun unitDir(deg: Float): Offset {
            val rad = deg * (PI.toFloat() / 180f)
            return Offset(cos(rad), sin(rad))
        }

        fun buildSectorPath(startRayDeg: Float, endRayDeg: Float): Path {
            val pStart = center + unitDir(startRayDeg - 90f) * innerWhiteRadius
            val qStart = pStart + unitDir(startRayDeg) * (radius * 3f)
            val qMid = center + unitDir(startRayDeg + 60f) * (radius * 4f)
            val pEnd = center + unitDir(endRayDeg - 90f) * innerWhiteRadius
            val qEnd = pEnd + unitDir(endRayDeg) * (radius * 3f)
            return Path().apply {
                moveTo(center.x, center.y)
                lineTo(pStart.x, pStart.y)
                lineTo(qStart.x, qStart.y)
                lineTo(qMid.x, qMid.y)
                lineTo(qEnd.x, qEnd.y)
                lineTo(pEnd.x, pEnd.y)
                close()
            }
        }

        rotate(degrees = rotationDegrees, pivot = center) {
            clipPath(outerCircle) {
                // Red top petal (tangent rays at 240 deg up-left and 0 deg right)
                drawPath(
                    path = buildSectorPath(startRayDeg = 240f, endRayDeg = 360f),
                    color = Color(0xFFEA4335)
                )
                // Yellow right/bottom petal (tangent rays at 0 deg right and 120 deg down-left)
                drawPath(
                    path = buildSectorPath(startRayDeg = 0f, endRayDeg = 120f),
                    color = Color(0xFFFBBC05)
                )
                // Green left/bottom petal (tangent rays at 120 deg down-left and 240 deg up-left)
                drawPath(
                    path = buildSectorPath(startRayDeg = 120f, endRayDeg = 240f),
                    color = Color(0xFF34A853)
                )
            }
        }

        // Crisp white inner separator ring + Google Blue core
        drawCircle(
            color = Color.White,
            radius = innerWhiteRadius,
            center = center
        )
        drawCircle(
            color = Color(0xFF4285F4),
            radius = blueRadius,
            center = center
        )
    }
}

@Composable
fun SignInGateScreen(
    hardwareProfile: HardwareProfile,
    isOnline: Boolean,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(
                id = if (tokens.isDark) R.drawable.img_wallpaper_dark else R.drawable.img_wallpaper_light
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = tokens.launcherSheetBackground,
                tonalElevation = 10.dp,
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .border(1.dp, tokens.borderColor, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.flexdesk_icon),
                            contentDescription = "FlexDesk Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = strings.signInTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = strings.signInSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onToggleLanguage,
                        modifier = Modifier.testTag("signin_language_button")
                    ) {
                        Text(if (strings.isArabic) "Switch to English" else "التبديل إلى العربية (RTL)")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureBulletRow(
                        icon = Icons.Rounded.Devices,
                        title = "Cross-Device Continuity",
                        subtitle = "Unified shelf, themes & docs on Chromebook and Android"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureBulletRow(
                        icon = Icons.Rounded.CloudDone,
                        title = "Offline-First Auto-Save",
                        subtitle = "Local cache writes every 600ms and syncs automatically"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureBulletRow(
                        icon = Icons.Rounded.Memory,
                        title = "Low-RAM & Icon Cache Ready",
                        subtitle = "${hardwareProfile.availRamMb} MB free of ${hardwareProfile.totalRamMb} MB RAM"
                    )

                    if (!isOnline) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.offlineAmber.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WifiOff,
                                    contentDescription = null,
                                    tint = tokens.offlineAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.offlineSignInNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            GoogleAuthManager.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                onAuthCancelled = {
                                    isLoading = false
                                },
                                scope = scope
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.signingIn, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.signInWithGoogle, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onContinueAsGuest,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("guest_sign_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.continueAsGuest, style = MaterialTheme.typography.labelLarge)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = strings.guestHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBulletRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
