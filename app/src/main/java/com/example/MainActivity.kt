package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.AppSettings
import com.example.data.model.PublicKasHelper
import com.example.data.repository.KasRepository
import com.example.ui.KasViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.IncomeScreen
import com.example.ui.screens.PublicReportScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.signOutAdmin
import com.example.ui.theme.KasTransparanTheme
import com.example.ui.theme.SoftTopBarColor
import com.example.ui.theme.SoftTopBarContentColor
import com.example.util.LanguageHelper
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var deepLinkPublicKasId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FirebaseApp init error", e)
        }

        deepLinkPublicKasId = PublicKasHelper.extractPublicKasIdFromUri(intent?.data)
        enableEdgeToEdge()

        setContent {
            val repository = remember { KasRepository.getInstance(applicationContext) }
            val appSettings by repository.observeSettings("local_admin")
                .collectAsState(initial = AppSettings())
            val themeMode = appSettings?.themeMode ?: "LIGHT"

            val incomingPublicId = deepLinkPublicKasId

            KasTransparanTheme(themeMode = themeMode) {
                if (incomingPublicId != null) {
                    // Direct Read-Only Public Report mode (NO treasurer menus, NO edit/add/delete)
                    PublicReportScreen(
                        publicKasId = incomingPublicId,
                        repository = repository,
                        isFromDeepLink = true,
                        onExitPublicMode = {
                            deepLinkPublicKasId = null
                        }
                    )
                } else {
                    var isSplashVisible by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(1200L)
                        isSplashVisible = false
                    }

                    val splashAlpha by animateFloatAsState(
                        targetValue = if (isSplashVisible) 1f else 0f,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        label = "splash_alpha"
                    )

                    val mainAlpha by animateFloatAsState(
                        targetValue = if (isSplashVisible) 0f else 1f,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                        label = "main_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (mainAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = mainAlpha }
                            ) {
                                AppMainContent(
                                    userId = "local_admin",
                                    onSignOut = {}
                                )
                            }
                        }

                        if (splashAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = splashAlpha }
                            ) {
                                SplashScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val extracted = PublicKasHelper.extractPublicKasIdFromUri(intent.data)
        if (extracted != null) {
            deepLinkPublicKasId = extracted
        }
    }
}

@Composable
fun SplashScreen() {
    val softTealGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F766E),
            Color(0xFF14B8A6),
            Color(0xFFCCFBF1),
            Color(0xFFF1F5F9)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(softTealGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_custom_logo),
                    contentDescription = "Logo Kas Transparan",
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Kas Transparan",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    fontSize = 28.sp
                ),
                color = Color(0xFF0F524C)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "by Rafline Dev",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    fontSize = 16.sp
                ),
                color = Color(0xFF0F766E)
            )
        }
    }
}

enum class MainNavScreen(val index: Int) {
    DASHBOARD(0),
    INCOME(1),
    EXPENSE(2),
    HISTORY(3),
    REPORT(4),
    SETTINGS(5)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainContent(
    userId: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val repository = remember(userId) { KasRepository.getInstance(context) }
    val viewModel: KasViewModel = remember(userId) { KasViewModel(repository, userId) }

    val uiState by viewModel.uiState.collectAsState()
    val lang = uiState.settings.language
    val themeMode = uiState.settings.themeMode

    var currentScreen by remember { mutableStateOf(MainNavScreen.DASHBOARD) }
    var activePublicPreviewId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Handle back button on sub-screens
    BackHandler(enabled = activePublicPreviewId == null && currentScreen != MainNavScreen.DASHBOARD) {
        currentScreen = MainNavScreen.DASHBOARD
    }

    KasTransparanTheme(themeMode = themeMode) {
        val previewId = activePublicPreviewId
        if (previewId != null) {
            // Full-screen Read-Only Public Report mode
            PublicReportScreen(
                publicKasId = previewId,
                repository = repository,
                isFromDeepLink = false,
                onExitPublicMode = { activePublicPreviewId = null }
            )
        } else if (currentScreen == MainNavScreen.SETTINGS) {
            SettingsScreen(
                state = uiState,
                viewModel = viewModel,
                onBack = { currentScreen = MainNavScreen.DASHBOARD },
                onSignOut = {
                    signOutAdmin(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = {
                            viewModel.refreshAuthStatus()
                            onSignOut()
                        },
                        scope = coroutineScope
                    )
                },
                onOpenPublicPreview = { kasId -> activePublicPreviewId = kasId }
            )
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "KAS TRANSPARAN",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = SoftTopBarContentColor
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { currentScreen = MainNavScreen.SETTINGS },
                                modifier = Modifier.testTag("top_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = LanguageHelper.getString("settings_title", lang),
                                    tint = SoftTopBarContentColor
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SoftTopBarColor
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == MainNavScreen.DASHBOARD,
                            onClick = { currentScreen = MainNavScreen.DASHBOARD },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = {
                                Text(
                                    text = LanguageHelper.getString("nav_dashboard", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_dashboard")
                        )

                        NavigationBarItem(
                            selected = currentScreen == MainNavScreen.INCOME,
                            onClick = { currentScreen = MainNavScreen.INCOME },
                            icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                            label = {
                                Text(
                                    text = LanguageHelper.getString("nav_income", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.ui.theme.IncomeGreen,
                                selectedTextColor = com.example.ui.theme.IncomeGreen,
                                indicatorColor = com.example.ui.theme.SoftSuccessBg
                            ),
                            modifier = Modifier.testTag("nav_item_income")
                        )

                        NavigationBarItem(
                            selected = currentScreen == MainNavScreen.EXPENSE,
                            onClick = { currentScreen = MainNavScreen.EXPENSE },
                            icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                            label = {
                                Text(
                                    text = LanguageHelper.getString("nav_expense", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.ui.theme.ExpenseRed,
                                selectedTextColor = com.example.ui.theme.ExpenseRed,
                                indicatorColor = com.example.ui.theme.SoftDangerBg
                            ),
                            modifier = Modifier.testTag("nav_item_expense")
                        )

                        NavigationBarItem(
                            selected = currentScreen == MainNavScreen.HISTORY,
                            onClick = { currentScreen = MainNavScreen.HISTORY },
                            icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
                            label = {
                                Text(
                                    text = LanguageHelper.getString("nav_history", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_history")
                        )

                        NavigationBarItem(
                            selected = currentScreen == MainNavScreen.REPORT,
                            onClick = { currentScreen = MainNavScreen.REPORT },
                            icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                            label = {
                                Text(
                                    text = LanguageHelper.getString("nav_report", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.tertiary,
                                selectedTextColor = MaterialTheme.colorScheme.tertiary,
                                indicatorColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_report")
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentScreen) {
                        MainNavScreen.DASHBOARD -> DashboardScreen(
                            state = uiState,
                            viewModel = viewModel,
                            onPeriodSelected = { period -> viewModel.setPeriodFilter(period) },
                            onNavigateToIncome = { currentScreen = MainNavScreen.INCOME },
                            onNavigateToExpense = { currentScreen = MainNavScreen.EXPENSE },
                            onNavigateToHistory = { currentScreen = MainNavScreen.HISTORY },
                            onAddInitialBalance = { amount, note -> viewModel.addInitialBalance(amount, note) },
                            onSetInitialBalance = { amount, note -> viewModel.setInitialBalance(amount, note) },
                            onOpenPublicPreview = { kasId -> activePublicPreviewId = kasId }
                        )
                        MainNavScreen.INCOME -> IncomeScreen(
                            state = uiState,
                            viewModel = viewModel,
                            onSuccessSaved = { currentScreen = MainNavScreen.DASHBOARD }
                        )
                        MainNavScreen.EXPENSE -> ExpenseScreen(
                            state = uiState,
                            viewModel = viewModel,
                            onSuccessSaved = { currentScreen = MainNavScreen.DASHBOARD }
                        )
                        MainNavScreen.HISTORY -> HistoryScreen(
                            state = uiState,
                            viewModel = viewModel
                        )
                        MainNavScreen.REPORT -> ReportScreen(
                            state = uiState,
                            viewModel = viewModel,
                            onOpenPublicPreview = { kasId -> activePublicPreviewId = kasId }
                        )
                        MainNavScreen.SETTINGS -> { /* Handled above */ }
                    }
                }
            }
        }
    }
}
