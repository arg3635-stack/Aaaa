package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.schedule.ScheduleEngine
import com.example.ui.admin.AdminAuthScreen
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.components.PeddiTicketCard
import com.example.ui.customer.CustomerAuthScreen
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.CustomerProfileScreen
import com.example.ui.customer.MyTicketsScreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RubyRed
import com.example.ui.viewmodel.AdminTab
import com.example.ui.viewmodel.AppMode
import com.example.ui.viewmodel.CustomerTab
import com.example.ui.viewmodel.LotteryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PeddiLotteryApp()
            }
        }
    }
}

@Composable
fun PeddiLotteryApp(viewModel: LotteryViewModel = viewModel()) {
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val customerTab by viewModel.customerTab.collectAsStateWithLifecycle()
    val adminTab by viewModel.adminTab.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val adminAuthError by viewModel.adminAuthError.collectAsStateWithLifecycle()

    val uiNotice by viewModel.uiNotice.collectAsStateWithLifecycle()
    val justPurchasedTicket by viewModel.justPurchasedTicket.collectAsStateWithLifecycle()
    val isPurchasing by viewModel.isPurchasing.collectAsStateWithLifecycle()

    val banner by viewModel.banner.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeSchedule by viewModel.activeSchedule.collectAsStateWithLifecycle()
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val allTickets by viewModel.allTickets.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val totalTicketsCount by viewModel.totalTicketsCount.collectAsStateWithLifecycle()
    val totalUsersCount by viewModel.totalUsersCount.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    val userTickets by viewModel.userTickets.collectAsStateWithLifecycle()
    val todayUserTicket by viewModel.todayUserTicket.collectAsStateWithLifecycle()
    val evaluation by viewModel.scheduleEvaluation.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiNotice) {
        uiNotice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotice()
        }
    }

    // Handle Android system back gestures appropriately
    BackHandler(enabled = appMode == AppMode.ADMIN || (appMode == AppMode.CUSTOMER && customerTab != CustomerTab.HOME)) {
        if (appMode == AppMode.ADMIN) {
            viewModel.setAppMode(AppMode.CUSTOMER)
        } else if (customerTab != CustomerTab.HOME) {
            viewModel.setCustomerTab(CustomerTab.HOME)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NavyDark,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NavySurface,
                    contentColor = Color.White,
                    actionColor = GoldLight,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            // Show bottom navigation bar only in Customer mode when user is logged in
            if (appMode == AppMode.CUSTOMER && currentUser != null) {
                NavigationBar(
                    containerColor = NavySurface,
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = customerTab == CustomerTab.HOME,
                        onClick = { viewModel.setCustomerTab(CustomerTab.HOME) },
                        icon = {
                            Icon(Icons.Default.Home, contentDescription = "Home")
                        },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDark,
                            selectedTextColor = GoldLight,
                            indicatorColor = GoldLight,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_home")
                    )

                    NavigationBarItem(
                        selected = customerTab == CustomerTab.MY_TICKETS,
                        onClick = { viewModel.setCustomerTab(CustomerTab.MY_TICKETS) },
                        icon = {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = "My Tickets")
                        },
                        label = { Text("My Passes") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDark,
                            selectedTextColor = GoldLight,
                            indicatorColor = GoldLight,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_tickets")
                    )

                    NavigationBarItem(
                        selected = customerTab == CustomerTab.PROFILE,
                        onClick = { viewModel.setCustomerTab(CustomerTab.PROFILE) },
                        icon = {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyDark,
                            selectedTextColor = GoldLight,
                            indicatorColor = GoldLight,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (appMode) {
                AppMode.CUSTOMER -> {
                    if (currentUser == null) {
                        // Customer Authentication Screen
                        CustomerAuthScreen(
                            onLogin = { email, pass -> viewModel.loginCustomer(email, pass) },
                            onRegister = { name, email, pass -> viewModel.registerCustomer(name, email, pass) },
                            isLoading = isAuthLoading,
                            errorMessage = authError
                        )
                    } else {
                        // Customer Logged In Views
                        when (customerTab) {
                            CustomerTab.HOME -> {
                                CustomerHomeScreen(
                                    user = currentUser,
                                    evaluation = evaluation,
                                    banner = banner,
                                    todayTicket = todayUserTicket,
                                    isPurchasing = isPurchasing,
                                    timezoneStr = activeSchedule?.timezoneId ?: ScheduleEngine.DEFAULT_TIMEZONE,
                                    onBuyTicket = { viewModel.buyDailyTicket() },
                                    onNavigateToMyTickets = { viewModel.setCustomerTab(CustomerTab.MY_TICKETS) },
                                    onNavigateToProfile = { viewModel.setCustomerTab(CustomerTab.PROFILE) },
                                    onOpenAdmin = { viewModel.setAppMode(AppMode.ADMIN) }
                                )
                            }
                            CustomerTab.MY_TICKETS -> {
                                MyTicketsScreen(
                                    tickets = userTickets,
                                    onBuyTicketClick = { viewModel.setCustomerTab(CustomerTab.HOME) }
                                )
                            }
                            CustomerTab.PROFILE -> {
                                CustomerProfileScreen(
                                    user = currentUser,
                                    deviceHash = viewModel.deviceHash,
                                    ticketsCount = userTickets.size,
                                    onLogout = { viewModel.logoutCustomer() },
                                    onOpenAdmin = { viewModel.setAppMode(AppMode.ADMIN) }
                                )
                            }
                        }
                    }
                }

                AppMode.ADMIN -> {
                    if (currentAdmin == null) {
                        // Admin Authentication Screen
                        AdminAuthScreen(
                            onLogin = { user, pass -> viewModel.loginAdmin(user, pass) },
                            onBackToCustomer = { viewModel.setAppMode(AppMode.CUSTOMER) },
                            errorMessage = adminAuthError
                        )
                    } else {
                        // Admin Dashboard
                        AdminDashboardScreen(
                            currentAdmin = currentAdmin,
                            activeTab = adminTab,
                            settings = settings,
                            banner = banner,
                            activeSchedule = activeSchedule,
                            schedules = allSchedules,
                            tickets = allTickets,
                            users = allUsers,
                            totalUsersCount = totalUsersCount,
                            totalTicketsCount = totalTicketsCount,
                            auditLogs = auditLogs,
                            evaluation = evaluation,
                            onTabSelected = { tab -> viewModel.setAdminTab(tab) },
                            onToggleMasterPurchasing = { enabled -> viewModel.toggleMasterTicketPurchase(enabled) },
                            onUpdateBanner = { updatedBanner -> viewModel.updateBanner(updatedBanner) },
                            onSaveSchedule = { schedule -> viewModel.saveSchedule(schedule) },
                            onDeleteSchedule = { scheduleId -> viewModel.deleteSchedule(scheduleId) },
                            onSetActiveSchedule = { scheduleId -> viewModel.setActiveSchedule(scheduleId) },
                            onUpdateTicketStatus = { id, status, notes -> viewModel.updateTicketStatus(id, status, notes) },
                            onUpdateUserStatus = { id, status -> viewModel.updateUserStatus(id, status) },
                            onBackToCustomer = { viewModel.setAppMode(AppMode.CUSTOMER) },
                            onLogoutAdmin = { viewModel.logoutAdmin() }
                        )
                    }
                }
            }

            // Ticket Purchase Success Celebration Dialog
            justPurchasedTicket?.let { ticket ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearPurchasedTicketDialog() },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = GoldLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Ticket Successfully Issued!",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Your daily unique lucky number has been generated and securely recorded on the server.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            PeddiTicketCard(ticket = ticket, showFullDetails = true)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearPurchasedTicketDialog()
                                viewModel.setCustomerTab(CustomerTab.MY_TICKETS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("View in My Passes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearPurchasedTicketDialog() }) {
                            Text("Dismiss", color = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    containerColor = NavySurface,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}
