package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminUserEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.BannerEntity
import com.example.data.model.ScheduleEntity
import com.example.data.model.SettingsEntity
import com.example.data.model.TicketEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AuthResult
import com.example.data.repository.LotteryRepository
import com.example.data.repository.PurchaseResult
import com.example.data.security.SecurityUtils
import com.example.domain.schedule.ScheduleEngine
import com.example.domain.schedule.ScheduleEvaluation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppMode {
    CUSTOMER,
    ADMIN
}

enum class CustomerTab {
    HOME,
    MY_TICKETS,
    PROFILE
}

enum class AdminTab {
    OVERVIEW,
    TICKETS,
    USERS,
    SCHEDULES,
    BANNER,
    AUDIT_LOGS
}

class LotteryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LotteryRepository(application)
    val deviceHash: String = SecurityUtils.getDeviceIdentifierHash(application)

    // Mode & Navigation
    private val _appMode = MutableStateFlow(AppMode.CUSTOMER)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _customerTab = MutableStateFlow(CustomerTab.HOME)
    val customerTab: StateFlow<CustomerTab> = _customerTab.asStateFlow()

    private val _adminTab = MutableStateFlow(AdminTab.OVERVIEW)
    val adminTab: StateFlow<AdminTab> = _adminTab.asStateFlow()

    // Customer Auth
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Admin Auth
    private val _currentAdmin = MutableStateFlow<AdminUserEntity?>(null)
    val currentAdmin: StateFlow<AdminUserEntity?> = _currentAdmin.asStateFlow()

    private val _adminAuthError = MutableStateFlow<String?>(null)
    val adminAuthError: StateFlow<String?> = _adminAuthError.asStateFlow()

    // Status Message / Alert (for dialogs & snackbars)
    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    private val _justPurchasedTicket = MutableStateFlow<TicketEntity?>(null)
    val justPurchasedTicket: StateFlow<TicketEntity?> = _justPurchasedTicket.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    // Data streams from repository
    val banner: StateFlow<BannerEntity?> = repository.bannerFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settings: StateFlow<SettingsEntity?> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeSchedule: StateFlow<ScheduleEntity?> = repository.activeScheduleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allSchedules: StateFlow<List<ScheduleEntity>> = repository.allSchedulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTickets: StateFlow<List<TicketEntity>> = repository.allTicketsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTicketsCount: StateFlow<Int> = repository.totalTicketsCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalUsersCount: StateFlow<Int> = repository.totalUsersCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User's tickets
    private val _userTickets = MutableStateFlow<List<TicketEntity>>(emptyList())
    val userTickets: StateFlow<List<TicketEntity>> = _userTickets.asStateFlow()

    private val _todayUserTicket = MutableStateFlow<TicketEntity?>(null)
    val todayUserTicket: StateFlow<TicketEntity?> = _todayUserTicket.asStateFlow()

    // Live Schedule Evaluation (Computed and refreshed every second)
    private val _scheduleEvaluation = MutableStateFlow(
        ScheduleEvaluation(
            isOpen = false,
            statusText = "CHECKING",
            timingText = "--",
            nextDrawText = "--",
            countdownFormatted = "00:00:00",
            countdownSeconds = 0,
            countdownTargetLabel = "Checking Schedule"
        )
    )
    val scheduleEvaluation: StateFlow<ScheduleEvaluation> = _scheduleEvaluation.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            // Auto login demo customer on fresh start if not logged in
            val demo = repository.loginUser("customer@peddilottery.com", "Customer@123", deviceHash)
            if (demo is AuthResult.Success) {
                _currentUser.value = demo.data
                loadUserTickets(demo.data.id)
            }
        }

        // Ticker loop: update evaluation every second for live countdown
        viewModelScope.launch {
            while (true) {
                updateEvaluation()
                delay(1000)
            }
        }
    }

    private fun updateEvaluation() {
        val currentSettings = settings.value
        val currentSched = activeSchedule.value
        _scheduleEvaluation.value = ScheduleEngine.evaluate(currentSettings, currentSched)
    }

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }

    fun setCustomerTab(tab: CustomerTab) {
        _customerTab.value = tab
    }

    fun setAdminTab(tab: AdminTab) {
        _adminTab.value = tab
    }

    fun clearNotice() {
        _uiNotice.value = null
    }

    fun clearPurchasedTicketDialog() {
        _justPurchasedTicket.value = null
    }

    // -----------------------------------------------------------------
    // Customer Auth
    // -----------------------------------------------------------------
    fun registerCustomer(name: String, emailOrPhone: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            when (val result = repository.registerUser(name, emailOrPhone, pass, deviceHash)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.data
                    loadUserTickets(result.data.id)
                    _uiNotice.value = "Registration successful! Welcome to PeddiLottery."
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun loginCustomer(emailOrPhone: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            when (val result = repository.loginUser(emailOrPhone, pass, deviceHash)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.data
                    loadUserTickets(result.data.id)
                }
                is AuthResult.Error -> {
                    _authError.value = result.message
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun logoutCustomer() {
        _currentUser.value = null
        _userTickets.value = emptyList()
        _todayUserTicket.value = null
        _customerTab.value = CustomerTab.HOME
    }

    private fun loadUserTickets(userId: Long) {
        viewModelScope.launch {
            repository.getUserTicketsFlow(userId).collect { tickets ->
                _userTickets.value = tickets
                val tz = activeSchedule.value?.timezoneId ?: ScheduleEngine.DEFAULT_TIMEZONE
                val todayStr = ScheduleEngine.getCurrentDateStr(tz)
                _todayUserTicket.value = tickets.firstOrNull { it.purchaseDate == todayStr }
            }
        }
    }

    // -----------------------------------------------------------------
    // Ticket Purchase (Server-Side Backend Atomic Enforcement)
    // -----------------------------------------------------------------
    fun buyDailyTicket() {
        val user = _currentUser.value ?: run {
            _uiNotice.value = "Please sign in or register to buy a ticket."
            return
        }

        viewModelScope.launch {
            _isPurchasing.value = true
            when (val result = repository.buyTicket(user.id, deviceHash)) {
                is PurchaseResult.Success -> {
                    _justPurchasedTicket.value = result.ticket
                    _todayUserTicket.value = result.ticket
                    _uiNotice.value = "Ticket #${result.ticket.ticketNumber} generated successfully!"
                    loadUserTickets(user.id)
                }
                is PurchaseResult.Error -> {
                    _uiNotice.value = result.message
                }
            }
            _isPurchasing.value = false
        }
    }

    // -----------------------------------------------------------------
    // Admin Auth & Controls
    // -----------------------------------------------------------------
    fun loginAdmin(username: String, pass: String) {
        viewModelScope.launch {
            _adminAuthError.value = null
            when (val res = repository.loginAdmin(username, pass)) {
                is AuthResult.Success -> {
                    _currentAdmin.value = res.data
                }
                is AuthResult.Error -> {
                    _adminAuthError.value = res.message
                }
            }
        }
    }

    fun logoutAdmin() {
        _currentAdmin.value = null
        _appMode.value = AppMode.CUSTOMER
    }

    fun toggleMasterTicketPurchase(enabled: Boolean) {
        val admin = _currentAdmin.value?.username ?: "ADMIN"
        viewModelScope.launch {
            repository.setTicketPurchasingMaster(admin, enabled)
            updateEvaluation()
        }
    }

    fun updateBanner(banner: BannerEntity) {
        val admin = _currentAdmin.value?.username ?: "ADMIN"
        viewModelScope.launch {
            repository.updateBanner(admin, banner)
            _uiNotice.value = "Banner configuration published!"
        }
    }

    fun saveSchedule(schedule: ScheduleEntity) {
        val admin = _currentAdmin.value?.username ?: "ADMIN"
        viewModelScope.launch {
            repository.saveSchedule(admin, schedule)
            updateEvaluation()
            _uiNotice.value = "Schedule '${schedule.name}' saved successfully!"
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        val admin = _currentAdmin.value?.username ?: "ADMIN"
        viewModelScope.launch {
            repository.deleteSchedule(admin, scheduleId)
            updateEvaluation()
            _uiNotice.value = "Schedule deleted."
        }
    }

    fun setActiveSchedule(scheduleId: Long) {
        val admin = _currentAdmin.value?.username ?: "ADMIN"
        viewModelScope.launch {
            repository.setActiveSchedule(admin, scheduleId)
            updateEvaluation()
            _uiNotice.value = "Active schedule updated."
        }
    }

    fun updateTicketStatus(ticketId: Long, status: String, notes: String?) {
        val admin = _currentAdmin.value?.username ?: "ADMIN"
        viewModelScope.launch {
            repository.updateTicketStatus(admin, ticketId, status, notes)
            _currentUser.value?.id?.let { loadUserTickets(it) }
            _uiNotice.value = "Ticket status updated to $status."
        }
    }

    fun updateUserStatus(userId: Long, status: String) {
        val admin = _currentAdmin.value?.username ?: "ADMIN"
        viewModelScope.launch {
            repository.updateUserStatus(admin, userId, status)
            _uiNotice.value = "User status set to $status."
        }
    }
}
