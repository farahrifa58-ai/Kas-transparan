package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.KasRepository
import com.example.ui.KasViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.IncomeScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.signOutAdmin
import com.example.ui.theme.KasTransparanTheme
import com.example.util.LanguageHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            // Outer theme wrapper default
            KasTransparanTheme(themeMode = "SYSTEM") {
                if (currentUser == null) {
                    AuthScreen(onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    })
                } else {
                    AppMainContent(
                        userId = currentUser!!.uid,
                        onSignOut = {
                            currentUser = null
                        }
                    )
                }
            }
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
    val repository = remember(userId) { KasRepository(context) }
    val viewModel: KasViewModel = remember(userId) { KasViewModel(repository, userId) }

    val uiState by viewModel.uiState.collectAsState()
    val lang = uiState.settings.language
    val themeMode = uiState.settings.themeMode

    var currentScreen by remember { mutableStateOf(MainNavScreen.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen != MainNavScreen.DASHBOARD) {
        currentScreen = MainNavScreen.DASHBOARD
    }

    KasTransparanTheme(themeMode = themeMode) {
        if (currentScreen == MainNavScreen.SETTINGS) {
            SettingsScreen(
                state = uiState,
                viewModel = viewModel,
                onBack = { currentScreen = MainNavScreen.DASHBOARD },
                onSignOut = {
                    signOutAdmin(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = onSignOut,
                        scope = coroutineScope
                    )
                }
            )
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "KAS TRANSPARAN",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        actions = {
                            // Settings Icon in Top Right Corner as mandated
                            IconButton(
                                onClick = { currentScreen = MainNavScreen.SETTINGS },
                                modifier = Modifier.testTag("top_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = LanguageHelper.getString("settings_title", lang),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    // Exactly 5 Main Navigation Items
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        // 1. Dashboard
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
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("nav_item_dashboard")
                        )

                        // 2. Kas Masuk
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("nav_item_income")
                        )

                        // 3. Kas Keluar
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("nav_item_expense")
                        )

                        // 4. Riwayat
                        NavigationBarItem(
                            selected = currentScreen == MainNavScreen.HISTORY,
                            onClick = { currentScreen = MainNavScreen.HISTORY },
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("nav_item_history")
                        )

                        // 5. Laporan Keuangan
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
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
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
                            onPeriodSelected = { period -> viewModel.setPeriodFilter(period) },
                            onNavigateToIncome = { currentScreen = MainNavScreen.INCOME },
                            onNavigateToExpense = { currentScreen = MainNavScreen.EXPENSE },
                            onNavigateToHistory = { currentScreen = MainNavScreen.HISTORY }
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
                            state = uiState
                        )
                        MainNavScreen.SETTINGS -> { /* Handled above */ }
                    }
                }
            }
        }
    }
}
