package github.sushkpavel.studentbsuby.ui.screens.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import github.sushkpavel.studentbsuby.navigation.Route
import github.sushkpavel.studentbsuby.resources.Res
import github.sushkpavel.studentbsuby.resources.app_locked
import github.sushkpavel.studentbsuby.resources.app_locked_hint
import github.sushkpavel.studentbsuby.resources.biometric_error
import github.sushkpavel.studentbsuby.resources.logo_text
import github.sushkpavel.studentbsuby.resources.unlock
import github.sushkpavel.studentbsuby.resources.unlock_with_password
import github.sushkpavel.studentbsuby.services.lock.AppLockManager
import github.sushkpavel.studentbsuby.services.lock.AuthResult
import github.sushkpavel.studentbsuby.services.lock.BiometryType
import github.sushkpavel.studentbsuby.ui.theme.values.Colors
import github.sushkpavel.studentbsuby.util.animatedSquaresBackground
import github.sushkpavel.studentbsuby.util.bsuBackgroundPattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppLockEffects(
    appLock: AppLockManager,
    navController: NavHostController,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appLock) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> appLock.onBackground()
                Lifecycle.Event.ON_START -> appLock.onForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    LaunchedEffect(route) {
        if (route != null)
            appLock.isSignedIn = route != Route.AuthScreen.route
    }
}

@Composable
fun AppLockOverlay(
    appLock: AppLockManager,
    onLoggedOut: () -> Unit,
) {
    val locked by appLock.locked.collectAsState()

    AnimatedVisibility(
        visible = locked,
        enter = fadeIn(),
        exit = fadeOut(tween(durationMillis = 400))
    ) {
        val scope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val authenticating by appLock.authenticating.collectAsState()
        val biometry = remember { appLock.biometryType }
        var error by remember { mutableStateOf<String?>(null) }

        fun unlock() {
            scope.launch {
                error = null
                val result = appLock.unlock()
                if (result is AuthResult.Failed)
                    error = result.message ?: getString(Res.string.biometric_error)
            }
        }

        LaunchedEffect(Unit) {
            lifecycleOwner.lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            unlock()
        }

        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = locked,
            onBackCompleted = {}
        )

        AppLockScreen(
            biometry = biometry,
            authenticating = authenticating,
            error = error,
            onUnlock = ::unlock,
            onLogout = {
                scope.launch {
                    appLock.logout()
                    onLoggedOut()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AppLockScreen(
    biometry: BiometryType,
    authenticating: Boolean,
    error: String?,
    onUnlock: () -> Unit,
    onLogout: () -> Unit,
) {
    val onBrand = MaterialTheme.colors.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.primaryVariant)
            .bsuBackgroundPattern(MaterialTheme.colors.onPrimary.copy(alpha = .1f))
            .animatedSquaresBackground(
                color = onBrand.copy(alpha = .04f),
                count = 8,
                size = 200.dp
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.logo_text),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 200.dp, height = 51.dp)
            )
            Spacer(modifier = Modifier.height(56.dp))

            Box(contentAlignment = Alignment.Center) {
                if (!authenticating)
                    PulseRing()
                Card(
                    onClick = onUnlock,
                    enabled = !authenticating,
                    shape = CircleShape,
                    backgroundColor = MaterialTheme.colors.secondary,
                    elevation = 8.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = biometry.icon,
                            contentDescription = stringResource(Res.string.unlock),
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(Res.string.app_locked),
                style = MaterialTheme.typography.subtitle1,
                color = onBrand,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: stringResource(Res.string.app_locked_hint),
                style = MaterialTheme.typography.body1,
                color = if (error != null) Colors.Red else onBrand.copy(alpha = .7f),
                textAlign = TextAlign.Center,
            )
        }

        TextButton(
            onClick = onLogout,
            enabled = !authenticating,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.unlock_with_password),
                color = onBrand.copy(alpha = .8f),
            )
        }
    }
}

@Composable
private fun PulseRing() {
    val transition = rememberInfiniteTransition()
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
        )
    )
    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                val scale = 1f + .45f * progress
                scaleX = scale
                scaleY = scale
                alpha = .5f * (1f - progress)
            }
            .clip(CircleShape)
            .background(MaterialTheme.colors.surface)
    )
}

private val BiometryType.icon: ImageVector
    get() = when (this) {
        BiometryType.Face -> Icons.Default.Face
        BiometryType.Fingerprint, BiometryType.Generic -> Icons.Default.Fingerprint
        BiometryType.None -> Icons.Default.Lock
    }
