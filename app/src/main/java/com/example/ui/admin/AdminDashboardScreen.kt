package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminUserEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.ScheduleEntity
import com.example.data.model.SettingsEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import com.example.domain.schedule.ScheduleEngine
import com.example.domain.schedule.ScheduleEvaluation
import com.example.ui.components.HomeBannerView
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RubyRed
import com.example.ui.viewmodel.AdminTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    currentAdmin: AdminUserEntity?,
    activeTab: AdminTab,
    settings: SettingsEntity?,
    banner: BannerEntity?,
    activeSchedule: ScheduleEntity?,
    schedules: List<ScheduleEntity>,
    tickets: List<TicketEntity>,
    users: List<UserEntity>,
    totalUsersCount: Int,
    totalTicketsCount: Int,
    auditLogs: List<AuditLogEntity>,
    evaluation: ScheduleEvaluation,
    onTabSelected: (AdminTab) -> Unit,
    onToggleMasterPurchasing: (Boolean) -> Unit,
    onUpdateBanner: (BannerEntity) -> Unit,
    onSaveSchedule: (ScheduleEntity) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onSetActiveSchedule: (Long) -> Unit,
    onUpdateTicketStatus: (Long, String, String?) -> Unit,
    onUpdateUserStatus: (Long, String) -> Unit,
    onBackToCustomer: () -> Unit,
    onLogoutAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavyDark,
        topBar = {
            AdminTopBar(
                adminName = currentAdmin?.username ?: "Admin",
                role = currentAdmin?.role ?: "SUPER_ADMIN",
                timezoneStr = activeSchedule?.timezoneId ?: ScheduleEngine.DEFAULT_TIMEZONE,
                onBackToCustomer = onBackToCustomer,
                onLogout = onLogoutAdmin
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Admin Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = NavySurface,
                contentColor = GoldLight,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                        color = GoldPrimary,
                        height = 3.dp
                    )
                }
            ) {
                AdminTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    AdminTab.OVERVIEW -> "Dashboard"
                                    AdminTab.TICKETS -> "Tickets (${tickets.size})"
                                    AdminTab.USERS -> "Users (${users.size})"
                                    AdminTab.SCHEDULES -> "Time Mgmt"
                                    AdminTab.BANNER -> "Banner"
                                    AdminTab.AUDIT_LOGS -> "Audit Logs"
                                },
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == tab) GoldLight else Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    AdminTab.OVERVIEW -> AdminOverviewTab(
                        settings = settings,
                        evaluation = evaluation,
                        activeSchedule = activeSchedule,
                        totalUsers = totalUsersCount,
                        totalTickets = totalTicketsCount,
                        todayTickets = tickets.count { it.purchaseDate == ScheduleEngine.getCurrentDateStr(activeSchedule?.timezoneId ?: ScheduleEngine.DEFAULT_TIMEZONE) },
                        recentTickets = tickets.take(5),
                        onToggleMaster = onToggleMasterPurchasing
                    )
                    AdminTab.TICKETS -> AdminTicketsTab(
                        tickets = tickets,
                        onUpdateTicketStatus = onUpdateTicketStatus
                    )
                    AdminTab.USERS -> AdminUsersTab(
                        users = users,
                        tickets = tickets,
                        onUpdateUserStatus = onUpdateUserStatus
                    )
                    AdminTab.SCHEDULES -> AdminSchedulesTab(
                        schedules = schedules,
                        activeSchedule = activeSchedule,
                        evaluation = evaluation,
                        onSaveSchedule = onSaveSchedule,
                        onDeleteSchedule = onDeleteSchedule,
                        onSetActiveSchedule = onSetActiveSchedule
                    )
                    AdminTab.BANNER -> AdminBannerTab(
                        banner = banner,
                        onPublishBanner = onUpdateBanner
                    )
                    AdminTab.AUDIT_LOGS -> AdminAuditLogsTab(
                        logs = auditLogs
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Top Bar
// -------------------------------------------------------------
@Composable
private fun AdminTopBar(
    adminName: String,
    role: String,
    timezoneStr: String,
    onBackToCustomer: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavySurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = NavyDark,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PeddiLottery Admin",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GoldLight.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = role,
                            color = GoldLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Logged in as @$adminName • $timezoneStr",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onBackToCustomer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyLight,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("switch_to_customer_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Customer App", fontSize = 11.sp)
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = RubyRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Dashboard Overview Tab
// -------------------------------------------------------------
@Composable
private fun AdminOverviewTab(
    settings: SettingsEntity?,
    evaluation: ScheduleEvaluation,
    activeSchedule: ScheduleEntity?,
    totalUsers: Int,
    totalTickets: Int,
    todayTickets: Int,
    recentTickets: List<TicketEntity>,
    onToggleMaster: (Boolean) -> Unit
) {
    val masterEnabled = settings?.ticketPurchaseEnabled ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Master Purchasing Control Switch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_master_switch_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (masterEnabled) EmeraldGreen.copy(alpha = 0.2f) else RubyRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (masterEnabled) EmeraldGreen else RubyRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ticket Purchasing: ${if (masterEnabled) "ENABLED" else "DISABLED"}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (masterEnabled) "Master switch is ON. Ticket sales follow active schedule." else "Ticket purchasing disabled manually by admin override.",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = masterEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldGreen,
                        checkedTrackColor = EmeraldDark.copy(alpha = 0.4f),
                        uncheckedThumbColor = RubyRed,
                        uncheckedTrackColor = NavyLight
                    ),
                    modifier = Modifier.testTag("master_purchase_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Total Users",
                value = "$totalUsers",
                icon = Icons.Default.People,
                color = Color(0xFF60A5FA),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Tickets",
                value = "$totalTickets",
                icon = Icons.Default.ConfirmationNumber,
                color = GoldLight,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Today's Tickets",
                value = "$todayTickets",
                icon = Icons.Default.Stars,
                color = EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Lottery Status",
                value = evaluation.statusText,
                icon = Icons.Default.Schedule,
                color = if (evaluation.isOpen) EmeraldGreen else RubyRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Schedule Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE LOTTERY SCHEDULE",
                        color = GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (evaluation.isOpen) EmeraldGreen.copy(alpha = 0.2f) else RubyRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = evaluation.statusText,
                            color = if (evaluation.isOpen) EmeraldGreen else RubyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = activeSchedule?.name ?: "No Schedule Active",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Hours: ${evaluation.timingText}  •  Draw: ${evaluation.nextDrawText}",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Live Countdown: ${evaluation.countdownFormatted} (${evaluation.countdownTargetLabel})",
                    color = GoldLight,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Ticket Activity
        Text(
            text = "Recent Ticket Activity",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (recentTickets.isEmpty()) {
            Text(
                text = "No tickets created yet.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        } else {
            recentTickets.forEach { ticket ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ticket.ticketNumber,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "User #${ticket.userId} • ${ticket.purchaseDate} ${ticket.purchaseTime}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (ticket.status) {
                                "WON" -> EmeraldGreen.copy(alpha = 0.2f)
                                "DRAWN" -> Color(0xFF60A5FA).copy(alpha = 0.2f)
                                else -> GoldPrimary.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = ticket.status,
                                color = when (ticket.status) {
                                    "WON" -> EmeraldGreen
                                    "DRAWN" -> Color(0xFF60A5FA)
                                    else -> GoldLight
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// -------------------------------------------------------------
// 2. Ticket Management Tab
// -------------------------------------------------------------
@Composable
private fun AdminTicketsTab(
    tickets: List<TicketEntity>,
    onUpdateTicketStatus: (Long, String, String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedTicketForEdit by remember { mutableStateOf<TicketEntity?>(null) }

    val filtered = remember(tickets, searchQuery, selectedFilter) {
        tickets.filter { t ->
            val matchesQuery = searchQuery.isBlank() ||
                t.ticketNumber.contains(searchQuery.trim(), ignoreCase = true) ||
                t.userId.toString() == searchQuery.trim() ||
                t.purchaseDate.contains(searchQuery.trim(), ignoreCase = true)
            val matchesFilter = if (selectedFilter == "ALL") true else t.status.equals(selectedFilter, ignoreCase = true)
            matchesQuery && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by ticket #, user ID, or date...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldLight) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_search_tickets"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = NavyLight,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "PENDING", "WON", "DRAWN", "CANCELLED").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = NavySurface,
                        labelColor = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Showing ${filtered.size} of ${tickets.size} tickets",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { ticket ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTicketForEdit = ticket }
                        .testTag("admin_ticket_row_${ticket.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ticket.ticketNumber,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (ticket.status) {
                                    "WON" -> EmeraldGreen.copy(alpha = 0.2f)
                                    "DRAWN" -> Color(0xFF60A5FA).copy(alpha = 0.2f)
                                    "CANCELLED" -> RubyRed.copy(alpha = 0.2f)
                                    else -> GoldPrimary.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = ticket.status,
                                    color = when (ticket.status) {
                                        "WON" -> EmeraldGreen
                                        "DRAWN" -> Color(0xFF60A5FA)
                                        "CANCELLED" -> RubyRed
                                        else -> GoldLight
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "User #${ticket.userId}  •  Device #${ticket.deviceId}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${ticket.purchaseDate} ${ticket.purchaseTime}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        if (!ticket.drawNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: ${ticket.drawNotes}",
                                color = GoldLight.copy(alpha = 0.9f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Ticket Status Edit Dialog
    selectedTicketForEdit?.let { ticket ->
        var newStatus by remember { mutableStateOf(ticket.status) }
        var notes by remember { mutableStateOf(ticket.drawNotes ?: "") }

        AlertDialog(
            onDismissRequest = { selectedTicketForEdit = null },
            title = {
                Text(
                    text = "Manage Ticket #${ticket.ticketNumber}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Purchased on ${ticket.purchaseDate} at ${ticket.purchaseTime} by User #${ticket.userId}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Change Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PENDING", "WON", "DRAWN", "CANCELLED").forEach { st ->
                            FilterChip(
                                selected = newStatus == st,
                                onClick = { newStatus = st },
                                label = { Text(st, fontSize = 10.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Draw Notes / Winner Remark") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTicketStatus(ticket.id, newStatus, notes)
                        selectedTicketForEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Save Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicketForEdit = null }) {
                    Text("Close")
                }
            },
            containerColor = NavySurface,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

// -------------------------------------------------------------
// 3. User Management Tab
// -------------------------------------------------------------
@Composable
private fun AdminUsersTab(
    users: List<UserEntity>,
    tickets: List<TicketEntity>,
    onUpdateUserStatus: (Long, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserForDetails by remember { mutableStateOf<UserEntity?>(null) }

    val filtered = remember(users, searchQuery) {
        users.filter { u ->
            searchQuery.isBlank() ||
                u.name.contains(searchQuery.trim(), ignoreCase = true) ||
                u.emailOrPhone.contains(searchQuery.trim(), ignoreCase = true) ||
                u.id.toString() == searchQuery.trim()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users by name, email, or ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldLight) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_search_users"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = NavyLight,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Total Registered Users: ${users.size}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { u ->
                val userTicketCount = tickets.count { it.userId == u.id }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedUserForDetails = u }
                        .testTag("user_row_${u.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = u.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (u.status == "ACTIVE") EmeraldGreen.copy(alpha = 0.2f) else RubyRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = u.status,
                                        color = if (u.status == "ACTIVE") EmeraldGreen else RubyRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = u.emailOrPhone,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tickets: $userTicketCount passes",
                                color = GoldLight,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                val nextStatus = if (u.status == "ACTIVE") "SUSPENDED" else "ACTIVE"
                                onUpdateUserStatus(u.id, nextStatus)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (u.status == "ACTIVE") RubyRed.copy(alpha = 0.8f) else EmeraldGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (u.status == "ACTIVE") "Suspend" else "Activate", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // User Details Modal
    selectedUserForDetails?.let { u ->
        val userTickets = tickets.filter { it.userId == u.id }

        AlertDialog(
            onDismissRequest = { selectedUserForDetails = null },
            title = { Text(u.name, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("ID: #${u.id}")
                    Text("Email/Phone: ${u.emailOrPhone}")
                    Text("Status: ${u.status}")
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    Text("Registered: ${sdf.format(Date(u.createdAt))}")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Purchased Tickets (${userTickets.size}):", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(modifier = Modifier.height(140.dp)) {
                        items(userTickets) { t ->
                            Text(
                                text = "#${t.ticketNumber} (${t.purchaseDate} - ${t.status})",
                                fontSize = 12.sp,
                                color = GoldLight
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedUserForDetails = null }) {
                    Text("Done")
                }
            },
            containerColor = NavySurface,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

// -------------------------------------------------------------
// 4. Lottery Time Management Tab (Section 10)
// -------------------------------------------------------------
@Composable
private fun AdminSchedulesTab(
    schedules: List<ScheduleEntity>,
    activeSchedule: ScheduleEntity?,
    evaluation: ScheduleEvaluation,
    onSaveSchedule: (ScheduleEntity) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onSetActiveSchedule: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var scheduleToEdit by remember { mutableStateOf<ScheduleEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Active Live Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE LOTTERY COUNTDOWN (CUSTOMER VIEW)",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (evaluation.isOpen) EmeraldGreen.copy(alpha = 0.2f) else RubyRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = evaluation.statusText,
                            color = if (evaluation.isOpen) EmeraldGreen else RubyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Status: ${evaluation.statusText}  •  Target: ${evaluation.countdownTargetLabel}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Countdown: ${evaluation.countdownFormatted}",
                    color = GoldLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Purchase Hours: ${evaluation.timingText}  •  Next Draw: ${evaluation.nextDrawText}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create New Schedule Button
        Button(
            onClick = {
                scheduleToEdit = null
                showCreateDialog = true
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("admin_add_schedule_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create New Lottery Schedule", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Configured Schedules (${schedules.size})",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        schedules.forEach { s ->
            val isCurrentActive = s.id == activeSchedule?.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("schedule_card_${s.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentActive) Color(0xFF1E293B) else NavySurface
                ),
                border = if (isCurrentActive) androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = s.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (isCurrentActive) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldPrimary
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row {
                            IconButton(onClick = {
                                scheduleToEdit = s
                                showCreateDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            if (schedules.size > 1) {
                                IconButton(onClick = { onDeleteSchedule(s.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RubyRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "📅 Date: ${s.dateStr}  •  🌐 Timezone: ${s.timezoneId}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "🕐 Open: ${ScheduleEngine.formatTo12Hour(s.openTime)}   🕐 Close: ${ScheduleEngine.formatTo12Hour(s.closeTime)}   🎯 Result: ${ScheduleEngine.formatTo12Hour(s.resultTime)}",
                        color = GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!isCurrentActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onSetActiveSchedule(s.id) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                        ) {
                            Text("Set As Active Schedule", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Schedule Dialog (Create / Edit)
    if (showCreateDialog) {
        val existing = scheduleToEdit
        var name by remember { mutableStateOf(existing?.name ?: "Daily Super Draw") }
        var dateStr by remember { mutableStateOf(existing?.dateStr ?: ScheduleEngine.getCurrentDateStr()) }
        var openTime by remember { mutableStateOf(existing?.openTime ?: "10:00") }
        var closeTime by remember { mutableStateOf(existing?.closeTime ?: "18:00") }
        var resultTime by remember { mutableStateOf(existing?.resultTime ?: "20:00") }
        var timezoneId by remember { mutableStateOf(existing?.timezoneId ?: "Asia/Kolkata") }
        var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(if (existing == null) "Create Schedule" else "Edit Schedule", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Schedule Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = openTime,
                            onValueChange = { openTime = it },
                            label = { Text("Open (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = closeTime,
                            onValueChange = { closeTime = it },
                            label = { Text("Close (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resultTime,
                        onValueChange = { resultTime = it },
                        label = { Text("Draw Result Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = timezoneId,
                        onValueChange = { timezoneId = it },
                        label = { Text("Timezone (e.g. Asia/Kolkata)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Schedule?")
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = ScheduleEntity(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            dateStr = dateStr.trim(),
                            openTime = openTime.trim(),
                            closeTime = closeTime.trim(),
                            resultTime = resultTime.trim(),
                            timezoneId = timezoneId.trim(),
                            isActive = isActive
                        )
                        onSaveSchedule(s)
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Save Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = NavySurface,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

// -------------------------------------------------------------
// 5. Banner Management Tab (Section 2)
// -------------------------------------------------------------
@Composable
private fun AdminBannerTab(
    banner: BannerEntity?,
    onPublishBanner: (BannerEntity) -> Unit
) {
    var enabled by remember(banner) { mutableStateOf(banner?.enabled ?: true) }
    var imageUrl by remember(banner) { mutableStateOf(banner?.imageUrl ?: "") }
    var title by remember(banner) { mutableStateOf(banner?.title ?: "") }
    var description by remember(banner) { mutableStateOf(banner?.description ?: "") }
    var buttonText by remember(banner) { mutableStateOf(banner?.buttonText ?: "") }
    var buttonAction by remember(banner) { mutableStateOf(banner?.buttonAction ?: "BUY_TICKET") }

    val presetImages = listOf(
        "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=1000&q=80" to "Gold & Glitter Jackpot",
        "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=1000&q=80" to "Luxury Amber Glow",
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=1000&q=80" to "Deep Midnight Neon"
    )

    val currentBannerPreview = remember(enabled, imageUrl, title, description, buttonText, buttonAction) {
        BannerEntity(
            id = 1,
            imageUrl = imageUrl,
            title = title,
            description = description,
            buttonText = buttonText,
            buttonAction = buttonAction,
            enabled = enabled
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Customer App Banner Settings",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Configure the top promotion banner visible to all lottery players.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Banner Live Preview
        Text(
            text = "LIVE INTERACTIVE PREVIEW",
            color = GoldLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        HomeBannerView(
            banner = currentBannerPreview,
            onActionClick = {},
            isPreview = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Form Fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Banner on Customer Home",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GoldLight)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Banner Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Banner Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buttonText,
                        onValueChange = { buttonText = it },
                        label = { Text("Button Text") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = buttonAction,
                        onValueChange = { buttonAction = it },
                        label = { Text("Action (BUY_TICKET)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Banner Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Or Choose a High-Resolution Preset:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetImages.forEach { (url, label) ->
                        FilterChip(
                            selected = imageUrl == url,
                            onClick = { imageUrl = url },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onPublishBanner(currentBannerPreview) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("publish_banner_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Publish Banner", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. Audit Logs Tab
// -------------------------------------------------------------
@Composable
private fun AdminAuditLogsTab(
    logs: List<AuditLogEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Administrator Security Audit Trail",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Every administrative configuration change and draw event is immutably logged.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Text("No audit events recorded.", color = Color.White.copy(alpha = 0.5f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val timeStr = sdf.format(Date(log.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = log.action,
                                        color = GoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = timeStr,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.details,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Admin: ${log.adminId}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
