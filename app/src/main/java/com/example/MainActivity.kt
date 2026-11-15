package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AiInsightsRepository
import com.example.data.PlayPulseRepository
import com.example.firebase.configureAppCheck
import com.example.ui.PlayPulseViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.MonetizationScreen
import com.example.ui.screens.OpportunitiesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.PlayPulseTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_RADAR = "radar"
private const val ROUTE_MONETIZE = "monetize"
private const val ROUTE_AI_LAB = "ai_lab"
private const val ROUTE_EXPORT = "export"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

private val playPulseDestinations = listOf(
    NavDestination(ROUTE_DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard, "nav_dashboard"),
    NavDestination(ROUTE_RADAR, R.string.nav_radar, Icons.Filled.Analytics, "nav_radar"),
    NavDestination(ROUTE_MONETIZE, R.string.nav_monetize, Icons.Filled.Subscriptions, "nav_monetize"),
    NavDestination(ROUTE_AI_LAB, R.string.nav_ai_lab, Icons.Filled.AutoAwesome, "nav_ai_lab"),
    NavDestination(ROUTE_EXPORT, R.string.nav_export, Icons.Filled.FileDownload, "nav_export")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            PlayPulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            PlayPulseMainShell(
                currentUserId = user.uid,
                userEmail = user.email
            )
        }
    }
}

@Composable
fun PlayPulseMainShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: PlayPulseViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PlayPulseViewModel(
                    repository = PlayPulseRepository(db),
                    aiRepository = AiInsightsRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val scoutedApp by viewModel.scoutedApp.collectAsStateWithLifecycle()
    val scoutStatus by viewModel.scoutStatus.collectAsStateWithLifecycle()
    val strategyPreview by viewModel.strategyPreview.collectAsStateWithLifecycle()
    val strategyStatus by viewModel.strategyStatus.collectAsStateWithLifecycle()
    val forecastStreamText by viewModel.forecastStreamText.collectAsStateWithLifecycle()
    val forecastStreamStatus by viewModel.forecastStreamStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val localMessage = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    LaunchedEffect(localMessage.value) {
        localMessage.value?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            localMessage.value = null
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(ROUTE_DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        playPulseDestinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        playPulseDestinations.forEach { dest ->
                            NavigationRailItem(
                                selected = currentRoute == dest.route,
                                onClick = { navigateTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_DASHBOARD,
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxSize()
                        ) {
                            composable(ROUTE_DASHBOARD) {
                                DashboardScreen(
                                    uiState = uiState,
                                    forecastStreamText = forecastStreamText,
                                    forecastStreamStatus = forecastStreamStatus,
                                    userEmail = userEmail,
                                    onStreamForecast = viewModel::streamPortfolioForecast,
                                    onCreateAlertRule = viewModel::createAlertRule,
                                    onToggleAlertRule = viewModel::toggleAlertRule,
                                    onDeleteAlertRule = viewModel::deleteAlertRule,
                                    onNavigateToRadar = { navigateTab(ROUTE_RADAR) },
                                    onNavigateToMonetize = { navigateTab(ROUTE_MONETIZE) },
                                    onSignOut = {
                                        signOutUser(credentialManager, scope)
                                    }
                                )
                            }
                            composable(ROUTE_RADAR) {
                                OpportunitiesScreen(
                                    uiState = uiState,
                                    scoutedApp = scoutedApp,
                                    scoutStatus = scoutStatus,
                                    onScoutNiche = viewModel::scoutAbandonedNiche,
                                    onClaimCatalogApp = viewModel::claimCatalogApp,
                                    onClaimScoutedApp = viewModel::claimScoutedApp,
                                    onAdvanceAppStatus = viewModel::advanceAppStatus,
                                    onDeleteTrackedApp = viewModel::deleteTrackedApp,
                                    onGenerateStrategyForApp = { name, platform, rating, dl, dormant ->
                                        viewModel.generateCompetitiveStrategy(name, platform, rating, dl, dormant)
                                        navigateTab(ROUTE_AI_LAB)
                                    }
                                )
                            }
                            composable(ROUTE_MONETIZE) {
                                MonetizationScreen(
                                    uiState = uiState,
                                    onCreatePlan = viewModel::createSubscriptionPlan,
                                    onTogglePlan = viewModel::toggleSubscriptionPlan,
                                    onDeletePlan = viewModel::deleteSubscriptionPlan,
                                    onUpdateGateway = viewModel::updatePaymentGateway
                                )
                            }
                            composable(ROUTE_AI_LAB) {
                                InsightsScreen(
                                    uiState = uiState,
                                    strategyPreview = strategyPreview,
                                    strategyStatus = strategyStatus,
                                    onGenerateStrategy = viewModel::generateCompetitiveStrategy,
                                    onSaveBrief = viewModel::saveStrategyBrief,
                                    onDeleteBrief = viewModel::deleteStrategyBrief
                                )
                            }
                            composable(ROUTE_EXPORT) {
                                ReportsScreen(
                                    uiState = uiState,
                                    onSaveReportPreset = viewModel::saveReportConfig,
                                    onDeleteReportPreset = viewModel::deleteReportConfig,
                                    onShowMessage = { localMessage.value = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
