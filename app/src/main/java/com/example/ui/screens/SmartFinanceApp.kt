package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.data.local.*
import com.example.ui.components.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

// Helper function to map hex colors cleanly
fun parseColorHex(hex: String, fallback: Color = Color.Gray): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

// Icon helper to map icon strings to Android vector drawables
fun getIconForName(name: String): ImageVector {
    return when (name.lowercase(Locale.getDefault())) {
        "restaurant", "makan" -> Icons.Default.Restaurant
        "directions_car", "transportasi" -> Icons.Default.DirectionsCar
        "shopping_cart", "belanja" -> Icons.Default.ShoppingCart
        "receipt", "tagihan" -> Icons.Default.Receipt
        "wifi", "internet" -> Icons.Default.Wifi
        "phone_android", "pulsa" -> Icons.Default.PhoneAndroid
        "school", "pendidikan" -> Icons.Default.School
        "local_hospital", "kesehatan" -> Icons.Default.LocalHospital
        "sports_esports", "hiburan" -> Icons.Default.SportsEsports
        "credit_card", "cicilan" -> Icons.Default.CreditCard
        "gavel", "pajak" -> Icons.Default.Gavel
        "favorite", "donasi" -> Icons.Default.Favorite
        "work", "gaji" -> Icons.Default.Work
        "star", "bonus" -> Icons.Default.Star
        "celebration", "thr" -> Icons.Default.Celebration
        "shopping_bag", "penjualan" -> Icons.Default.ShoppingBag
        "trending_up", "investasi" -> Icons.Default.TrendingUp
        "phone" -> Icons.Default.Phone
        "email" -> Icons.Default.Email
        else -> Icons.Default.MoreHoriz
    }
}

enum class FinanceTab(val id: String, val title: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    TRANSAKSI("transaksi", "Transaksi", Icons.Default.ReceiptLong),
    LAPORAN("laporan", "Laporan", Icons.Default.BarChart),
    BUDGET("budget", "Budget", Icons.Default.Shield),
    SAVINGS("savings", "Tabungan", Icons.Default.Savings),
    DEBTS("debts", "Hutang Piutang", Icons.Default.Handshake),
    CALENDAR("calendar", "Kalender", Icons.Default.CalendarMonth),
    CATEGORIES("categories", "Kategori", Icons.Default.Category),
    SETTINGS("settings", "Setting", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFinanceApp(
    viewModel: FinanceViewModel = viewModel(),
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(FinanceTab.DASHBOARD) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    // Observe data
    val transactionsList by viewModel.filteredTransactions.collectAsState()
    val rawTransactions by viewModel.transactions.collectAsState(emptyList())
    val budgetsList by viewModel.budgets.collectAsState(emptyList())
    val savingGoalsList by viewModel.savingGoals.collectAsState(emptyList())
    val debtsList by viewModel.debtsCredits.collectAsState(emptyList())
    val categoriesList by viewModel.categories.collectAsState(emptyList())
    val notificationsList by viewModel.notifications.collectAsState()
    val showUndoToast by viewModel.showUndoToast.collectAsState()

    val currencySymbol by viewModel.currency.collectAsState()
    val appLanguage by viewModel.language.collectAsState()

    // Trigger Undo deleted transactions Snackbar / Toast
    LaunchedEffect(showUndoToast) {
        showUndoToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            // Clear VM toast so it doesn't trigger repeatedly
            viewModel.clearUndoToast()
        }
    }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        AuthScreen(viewModel = viewModel)
    } else {
        // Edge to Edge Scaffold
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_logo_kasvora_1784045569654),
                            contentDescription = "Kasvora Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kasvora",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Quick Notification Indicator icon
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        IconButton(onClick = {
                            if (notificationsList.isNotEmpty()) {
                                Toast.makeText(context, "Ada ${notificationsList.size} Peringatan Keuangan!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Kondisi Keuangan Anda Aman!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = if (notificationsList.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Pemberitahuan",
                                tint = if (notificationsList.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (notificationsList.isNotEmpty()) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 4.dp),
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Text(notificationsList.size.toString(), color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }

                    // Logout Button
                    IconButton(onClick = {
                        viewModel.logoutUser()
                        Toast.makeText(context, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Logout,
                            contentDescription = "Keluar (Logout)",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Theme Toggle
                    IconButton(onClick = { onToggleDarkTheme(!isDarkTheme) }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti Tema"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // Adaptive Navigation: Mobile Bottom Navigation
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Main compact tabs for Bottom nav (Show first 5 major tabs for premium mobile, rest can be reached via a drawer or custom button, or let's include scrollable Bottom Navigation / grid so they are all accessible!)
                FinanceTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_${tab.id}")
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("quick_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { innerPadding ->

        // Notification Alerts Banner at top
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (notificationsList.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Peringatan", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Peringatan Sistem Keuangan",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Display the most recent warning
                        Text(
                            text = notificationsList.first(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Crossfade content based on active tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentTab) {
                    FinanceTab.DASHBOARD -> DashboardScreen(
                        rawTransactions = rawTransactions,
                        budgetsList = budgetsList,
                        goalsList = savingGoalsList,
                        categoriesList = categoriesList,
                        currency = currencySymbol,
                        onNavigateToTab = { currentTab = it },
                        viewModel = viewModel
                    )
                    FinanceTab.TRANSAKSI -> TransaksiScreen(
                        transactions = transactionsList,
                        categories = categoriesList,
                        currency = currencySymbol,
                        viewModel = viewModel
                    )
                    FinanceTab.LAPORAN -> LaporanScreen(
                        transactions = rawTransactions,
                        categories = categoriesList,
                        currency = currencySymbol,
                        viewModel = viewModel
                    )
                    FinanceTab.BUDGET -> BudgetScreen(
                        budgets = budgetsList,
                        transactions = rawTransactions,
                        categories = categoriesList,
                        currency = currencySymbol,
                        viewModel = viewModel
                    )
                    FinanceTab.SAVINGS -> SavingsScreen(
                        goals = savingGoalsList,
                        currency = currencySymbol,
                        viewModel = viewModel
                    )
                    FinanceTab.DEBTS -> DebtsScreen(
                        debts = debtsList,
                        currency = currencySymbol,
                        viewModel = viewModel
                    )
                    FinanceTab.CALENDAR -> CalendarScreen(
                        transactions = rawTransactions,
                        currency = currencySymbol,
                        viewModel = viewModel
                    )
                    FinanceTab.CATEGORIES -> CategoriesScreen(
                        categories = categoriesList,
                        viewModel = viewModel
                    )
                    FinanceTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        isDark = isDarkTheme,
                        onToggleDark = onToggleDarkTheme
                    )
                }
            }
        }

        // --- Quick Add Transaction Dialog ---
        if (showQuickAddDialog) {
            QuickAddDialog(
                categories = categoriesList,
                onDismiss = { showQuickAddDialog = false },
                onAddTransaction = { amount, type, category, note, paymentMethod, status, date, time, attachment ->
                    viewModel.addTransaction(amount, type, category, note, paymentMethod, status, date, time, attachment)
                    showQuickAddDialog = false
                    Toast.makeText(context, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
}

// ==========================================
// --- DASHBOARD SCREEN ---
// ==========================================
@Composable
fun DashboardScreen(
    rawTransactions: List<TransactionEntity>,
    budgetsList: List<BudgetEntity>,
    goalsList: List<SavingGoalEntity>,
    categoriesList: List<CategoryEntity>,
    currency: String,
    onNavigateToTab: (FinanceTab) -> Unit,
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    
    // --- 1. Compute stats variables ---
    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val totalPemasukan = rawTransactions.filter { it.type == "PEMASUKAN" && it.status == "BERHASIL" }.sumOf { it.amount }
    val totalPengeluaran = rawTransactions.filter { it.type == "PENGELUARAN" && it.status == "BERHASIL" }.sumOf { it.amount }
    val totalSaldo = totalPemasukan - totalPengeluaran

    val flowBulanIni = rawTransactions
        .filter { it.status == "BERHASIL" && it.date.startsWith(currentMonth) }
    val incomeBulanIni = flowBulanIni.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
    val expenseBulanIni = flowBulanIni.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
    val cashFlowBulanIni = incomeBulanIni - expenseBulanIni

    val persentasePengeluaran = if (totalPemasukan > 0) (totalPengeluaran / totalPemasukan * 100).coerceAtMost(100.0) else 0.0

    // Rata-rata harian (last 30 days)
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -30)
    val date30DaysAgo = sdfDate.format(calendar.time)
    val expenseLast30Days = rawTransactions
        .filter { it.type == "PENGELUARAN" && it.status == "BERHASIL" && it.date >= date30DaysAgo }
        .sumOf { it.amount }
    val avgDailyExpense = expenseLast30Days / 30.0

    // Quick format helper
    fun formatVal(amount: Double): String {
        return "$currency ${String.format("%,.0f", amount)}"
    }

    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.SlateDarkBg
    val cardBorder = if (isDark) null else BorderStroke(1.dp, Color(0xFFE2E8F0))

    val currentUserName by viewModel.currentUserName.collectAsState()
    val initials = remember(currentUserName) {
        if (currentUserName.isNotBlank()) {
            currentUserName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercase() }
                .joinToString("")
        } else {
            "DH"
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        
        // Welcome Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 0..11 -> "SELAMAT PAGI 🌅"
                    in 12..15 -> "SELAMAT SIANG ☀️"
                    in 16..18 -> "SELAMAT SORE 🌇"
                    else -> "SELAMAT MALAM 🌌"
                }
                Text(
                    text = greeting,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentUserName.ifBlank { "Diky Hermansyah" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.5.dp, if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- STAT CARDS GRID ---
        // 1. Total Saldo Main Banner (Premium High Density Styling with Gradient & Subtitles)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFF4F46E5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            } else {
                                listOf(Color(0xFF4F46E5), Color(0xFF4338CA))
                            }
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF818CF8) else Color(0xFFE0E7FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Total Saldo",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFFE0E7FF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PERSONAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatVal(totalSaldo),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Inside Balance Card Side-by-side Pemasukan and Pengeluaran (with thin divider)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PEMASUKAN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFFC7D2FE)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatVal(totalPemasukan),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = "PENGELUARAN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFFC7D2FE)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatVal(totalPengeluaran),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi stat cards
        Row(modifier = Modifier.fillMaxWidth()) {
            // Pemasukan
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE6F4EA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF137333), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pemasukan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formatVal(totalPemasukan), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Pengeluaran
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFCE8E6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFC5221F), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengeluaran", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(formatVal(totalPengeluaran), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC5221F))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Daily average & percentage row
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pengeluaran Harian (Rata-rata)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatVal(avgDailyExpense), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Batas Anggaran Terpakai", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${String.format("%.1f", persentasePengeluaran)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (persentasePengeluaran > 80.0) Color.Red else MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- QUICK SHORTCUT TILES ---
        Text("Fitur Cepat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val listShortcuts = listOf(
                ShortcutItem("Budget", Icons.Default.Shield, FinanceTab.BUDGET),
                ShortcutItem("Laporan", Icons.Default.BarChart, FinanceTab.LAPORAN),
                ShortcutItem("Tabungan", Icons.Default.Savings, FinanceTab.SAVINGS),
                ShortcutItem("Kalender", Icons.Default.CalendarMonth, FinanceTab.CALENDAR)
            )
            listShortcuts.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onNavigateToTab(item.tab) }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- CHARTS PREVIEW SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Analisis Arus Kas (7 Hari)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Detail Laporan",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToTab(FinanceTab.LAPORAN) }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic 7-day transactions preview
        val last7DaysTransactions = remember(rawTransactions) {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val listDays = mutableListOf<String>()
            for (i in 0..6) {
                listDays.add(df.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            listDays.reverse()

            val chartDataIncome = listDays.map { date ->
                val totalIn = rawTransactions.filter { it.date == date && it.type == "PEMASUKAN" && it.status == "BERHASIL" }.sumOf { it.amount }.toFloat()
                val dayLabel = date.substring(8, 10) // Only day number
                ChartData(dayLabel, totalIn, Color(0xFF059669))
            }
            val chartDataExpense = listDays.map { date ->
                val totalOut = rawTransactions.filter { it.date == date && it.type == "PENGELUARAN" && it.status == "BERHASIL" }.sumOf { it.amount }.toFloat()
                val dayLabel = date.substring(8, 10)
                ChartData(dayLabel, totalOut, Color(0xFFEF4444))
            }
            Pair(chartDataIncome, chartDataExpense)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Statistik 7 Hari Terakhir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF059669)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Masuk", fontSize = 9.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Keluar", fontSize = 9.sp, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                BarChart(
                    data = last7DaysTransactions.first,
                    compareData = last7DaysTransactions.second,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    barColor = Color(0xFF059669),
                    compareBarColor = Color(0xFFEF4444)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- RECENT ACTIVITY ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transaksi Terbaru", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Semua",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToTab(FinanceTab.TRANSAKSI) }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val recentTxs = rawTransactions.take(4)
        if (recentTxs.isEmpty()) {
            Text("Belum ada transaksi.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        } else {
            recentTxs.forEach { tx ->
                TransactionRow(
                    tx = tx,
                    currency = currency,
                    onFavorite = { viewModel.toggleFavorite(tx) },
                    onDuplicate = { viewModel.duplicateTransaction(tx) },
                    onDelete = { viewModel.deleteTransaction(tx) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

data class ShortcutItem(val title: String, val icon: ImageVector, val tab: FinanceTab)

// ==========================================
// --- TRANSACTION LIST SCREEN (CRUD) ---
// ==========================================
@Composable
fun TransaksiScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: String,
    viewModel: FinanceViewModel
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<Int>()) }
    var isBulkSelectActive by remember { mutableStateOf(false) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterCategory by viewModel.selectedFilterCategory.collectAsState()
    val minNominal by viewModel.minNominal.collectAsState()
    val maxNominal by viewModel.maxNominal.collectAsState()
    val paymentMethod by viewModel.filterPaymentMethod.collectAsState()
    val status by viewModel.filterStatus.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        
        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Semua Transaksi", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { 
                    isBulkSelectActive = !isBulkSelectActive
                    if (!isBulkSelectActive) selectedIds = emptySet()
                }) {
                    Icon(
                        imageVector = if (isBulkSelectActive) Icons.Default.Close else Icons.Default.Checklist,
                        contentDescription = "Pilih Banyak",
                        tint = if (isBulkSelectActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (selectedIds.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.deleteTransactionsBulk(selectedIds.toList())
                        selectedIds = emptySet()
                        isBulkSelectActive = false
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Hapus Terpilih", tint = Color.Red)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar & Filter Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Cari catatan atau kategori...") },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { showFilterSheet = !showFilterSheet },
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        if (filterCategory != null || minNominal != null || maxNominal != null || paymentMethod != null || status != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (filterCategory != null || minNominal != null || maxNominal != null || paymentMethod != null || status != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Active Filter Banner indicator
        if (filterCategory != null || minNominal != null || maxNominal != null || paymentMethod != null || status != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter aktif diaktifkan", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Hapus Semua",
                    fontSize = 11.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.clearFilters() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Filtering View Drawer
        if (showFilterSheet) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Saring Transaksi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Category filter row
                    Text("Kategori", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable { viewModel.setFilterCategory(null) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .background(
                                    if (filterCategory == null) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text("Semua", color = if (filterCategory == null) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                        categories.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .clickable { viewModel.setFilterCategory(cat.name) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .background(
                                        if (filterCategory == cat.name) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(cat.name, color = if (filterCategory == cat.name) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Nominal range min/max
                    Text("Rentang Nominal", fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = minNominal?.toString() ?: "",
                            onValueChange = { viewModel.setMinNominal(it.toDoubleOrNull()) },
                            placeholder = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                                .height(48.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = maxNominal?.toString() ?: "",
                            onValueChange = { viewModel.setMaxNominal(it.toDoubleOrNull()) },
                            placeholder = { Text("Max") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                                .height(48.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Payment Method
                    Text("Metode Pembayaran", fontSize = 11.sp, color = Color.Gray)
                    val paymentMethodsList = listOf("Cash", "Transfer", "QRIS", "Debit", "Kredit", "E-Wallet")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        paymentMethodsList.forEach { method ->
                            Box(
                                modifier = Modifier
                                    .clickable { viewModel.setFilterPaymentMethod(if (paymentMethod == method) null else method) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .background(
                                        if (paymentMethod == method) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Text(method, color = if (paymentMethod == method) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Transactions List with Bulk Selection and Multi Controls
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tidak ada transaksi ditemukan", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(transactions) { tx ->
                    val isSelected = selectedIds.contains(tx.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isBulkSelectActive) {
                                    selectedIds = if (isSelected) selectedIds - tx.id else selectedIds + tx.id
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBulkSelectActive) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + tx.id else selectedIds - tx.id
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        TransactionRow(
                            tx = tx,
                            currency = currency,
                            onFavorite = { viewModel.toggleFavorite(tx) },
                            onDuplicate = { viewModel.duplicateTransaction(tx) },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    tx: TransactionEntity,
    currency: String,
    onFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == com.example.ui.theme.SlateDarkBg
    val cardBorder = if (isDark) null else BorderStroke(1.dp, Color(0xFFF1F5F9))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Category Icon Wrapper
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (tx.type == "PEMASUKAN") Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconForName(tx.category),
                        contentDescription = tx.category,
                        tint = if (tx.type == "PEMASUKAN") Color(0xFF137333) else Color(0xFFC5221F),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (tx.note.isNotEmpty()) tx.note else tx.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tx.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tx.paymentMethod,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Amount, Status & Actions
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.type == "PEMASUKAN") "+" else "-"} $currency ${String.format("%,.0f", tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (tx.type == "PEMASUKAN") Color(0xFF059669) else Color(0xFFEF4444)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (tx.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorit",
                            tint = if (tx.isFavorite) Color.Red else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplikat",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// --- REPORTS SCREEN ---
// ==========================================
@Composable
fun LaporanScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: String,
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    var selectedFilterTime by remember { mutableStateOf("Bulan Ini") }
    var showExportLoading by remember { mutableStateOf(false) }

    val filteredList = remember(transactions, selectedFilterTime) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val today = sdf.format(Date())

        when (selectedFilterTime) {
            "Hari Ini" -> transactions.filter { it.date == today }
            "Minggu Ini" -> {
                // Approximate 7 days
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val sevenDaysAgo = sdf.format(cal.time)
                transactions.filter { it.date >= sevenDaysAgo }
            }
            "Bulan Ini" -> transactions.filter { it.date.startsWith(currentMonth) }
            "Tahun Ini" -> transactions.filter { it.date.startsWith(currentYear) }
            else -> transactions
        }
    }

    val totalIn = filteredList.filter { it.type == "PEMASUKAN" && it.status == "BERHASIL" }.sumOf { it.amount }
    val totalOut = filteredList.filter { it.type == "PENGELUARAN" && it.status == "BERHASIL" }.sumOf { it.amount }
    val netBalance = totalIn - totalOut

    // Get Largest expense category
    val largestCategory = remember(filteredList) {
        val expenseGroup = filteredList
            .filter { it.type == "PENGELUARAN" && it.status == "BERHASIL" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        expenseGroup.maxByOrNull { it.value }
    }

    // Chart category data mapper
    val categoryChartData = remember(filteredList) {
        val expenseGroup = filteredList
            .filter { it.type == "PENGELUARAN" && it.status == "BERHASIL" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount }.toFloat() }

        expenseGroup.map { entry ->
            val matchingCat = categories.find { it.name == entry.key }
            val col = matchingCat?.let { parseColorHex(it.colorHex) } ?: Color.Gray
            ChartData(entry.key, entry.value, col)
        }
    }

    fun triggerExportSimulation(format: String) {
        showExportLoading = true
        // Delay toast to simulate export file writing
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showExportLoading = false
            Toast.makeText(context, "Laporan berhasil diexport ke format $format!", Toast.LENGTH_LONG).show()
        }, 1500)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Laporan Keuangan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Time filters row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val listFilters = listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Tahun Ini", "Semua")
            listFilters.forEach { filterName ->
                Box(
                    modifier = Modifier
                        .clickable { selectedFilterTime = filterName }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .background(
                            if (selectedFilterTime == filterName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Text(
                        filterName,
                        color = if (selectedFilterTime == filterName) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showExportLoading) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sedang menyusun dan mengekspor berkas laporan...", fontSize = 11.sp)
                }
            }
        }

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ringkasan Filter ($selectedFilterTime)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Pemasukan", fontSize = 11.sp, color = Color.Gray)
                        Text("$currency ${String.format("%,.0f", totalIn)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Pengeluaran", fontSize = 11.sp, color = Color.Gray)
                        Text("$currency ${String.format("%,.0f", totalOut)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Arus Kas Netto", fontSize = 11.sp, color = Color.Gray)
                        Text("$currency ${String.format("%,.0f", netBalance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFEF4444))
                    }
                    largestCategory?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Kategori Terbesar", fontSize = 11.sp, color = Color.Gray)
                            Text(it.key, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("$currency ${String.format("%,.0f", it.value)}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Donut Chart Breakdown
        Text("Rasio Pengeluaran Per Kategori", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    data = categoryChartData,
                    modifier = Modifier.size(180.dp),
                    centerLabel = "Belanja Keluar",
                    centerValue = "$currency ${String.format("%,.0f", totalOut)}"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export Options Panel
        Text("Bagikan & Ekspor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val listExports = listOf(
                ExportButton("PDF", Icons.Default.PictureAsPdf),
                ExportButton("Excel", Icons.Default.GridOn),
                ExportButton("CSV", Icons.Default.Description),
                ExportButton("Print", Icons.Default.Print)
            )
            listExports.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { triggerExportSimulation(item.label) }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

data class ExportButton(val label: String, val icon: ImageVector)

// ==========================================
// --- BUDGETS SCREEN ---
// ==========================================
@Composable
fun BudgetScreen(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: String,
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    // Group actual spending by category for this month
    val actualSpentGroup = remember(transactions) {
        transactions
            .filter { it.type == "PENGELUARAN" && it.status == "BERHASIL" && it.date.startsWith(currentMonth) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Anggaran Bulanan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddBudgetDialog = true }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah Anggaran", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada anggaran bulanan dibuat.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            budgets.forEach { budget ->
                val spent = actualSpentGroup[budget.category] ?: 0.0
                val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
                val isOverBudget = spent > budget.limitAmount

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (isOverBudget) BorderStroke(1.5.dp, Color.Red) else null
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(getIconForName(budget.category), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            IconButton(onClick = { viewModel.deleteBudgetByCategory(budget.category) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Anggaran", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = progress.coerceAtMost(1f),
                            color = if (isOverBudget) Color.Red else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Terpakai: $currency ${String.format("%,.0f", spent)}",
                                fontSize = 11.sp,
                                color = if (isOverBudget) Color.Red else Color.Gray,
                                fontWeight = if (isOverBudget) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                "Batas: $currency ${String.format("%,.0f", budget.limitAmount)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        if (isOverBudget) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "⚠️ ANGGARAN MELEBIHI BATAS! Mohon kurangi belanja kotor Anda.",
                                color = Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Add Budget Dialog
        if (showAddBudgetDialog) {
            AddBudgetDialog(
                categories = categories.filter { it.type == "PENGELUARAN" },
                onDismiss = { showAddBudgetDialog = false },
                onSave = { category, limit ->
                    viewModel.saveBudget(category, limit)
                    showAddBudgetDialog = false
                    Toast.makeText(context, "Batas anggaran disimpan!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ==========================================
// --- SAVINGS SCREEN ---
// ==========================================
@Composable
fun SavingsScreen(
    goals: List<SavingGoalEntity>,
    currency: String,
    viewModel: FinanceViewModel
) {
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showTopUpDialog by remember { mutableStateOf<SavingGoalEntity?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Target Tabungan (Goals)", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddGoalDialog = true }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah Target", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada target tabungan dibuat.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            goals.forEach { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                val isCompleted = goal.currentAmount >= goal.targetAmount

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                goal.note?.let {
                                    Text(it, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Row {
                                IconButton(onClick = { showTopUpDialog = goal }) {
                                    Icon(Icons.Default.Upload, contentDescription = "Topup Tabungan", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteSavingGoal(goal) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = progress.coerceAtMost(1f),
                            color = if (isCompleted) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                        .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Terkumpul: $currency ${String.format("%,.0f", goal.currentAmount)} (${String.format("%.1f", progress * 100)}%)",
                                fontSize = 11.sp,
                                color = if (isCompleted) Color(0xFF059669) else Color.Gray,
                                fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                "Target: $currency ${String.format("%,.0f", goal.targetAmount)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Add Goal Dialog
        if (showAddGoalDialog) {
            AddSavingGoalDialog(
                onDismiss = { showAddGoalDialog = false },
                onSave = { name, target, current, note ->
                    viewModel.addSavingGoal(name, target, current, note)
                    showAddGoalDialog = false
                    Toast.makeText(context, "Target Tabungan baru ditambahkan!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Topup dialog
        showTopUpDialog?.let { goal ->
            TopUpSavingGoalDialog(
                goal = goal,
                onDismiss = { showTopUpDialog = null },
                onSave = { amount ->
                    viewModel.updateSavingGoal(goal.copy(currentAmount = goal.currentAmount + amount))
                    showTopUpDialog = null
                    Toast.makeText(context, "Tabungan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ==========================================
// --- DEBTS & CREDITS SCREEN ---
// ==========================================
@Composable
fun DebtsScreen(
    debts: List<DebtCreditEntity>,
    currency: String,
    viewModel: FinanceViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("HUTANG") } // or "PIUTANG"
    val context = LocalContext.current

    val filteredList = debts.filter { it.type == selectedTab }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hutang Piutang", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah Catatan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab switcher
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { selectedTab = "HUTANG" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "HUTANG") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Hutang Saya", color = if (selectedTab == "HUTANG") Color.White else MaterialTheme.colorScheme.onSurface)
            }

            Button(
                onClick = { selectedTab = "PIUTANG" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "PIUTANG") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Piutang Saya", color = if (selectedTab == "PIUTANG") Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada catatan ditemukan.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            filteredList.forEach { item ->
                val isPaid = item.status == "LUNAS"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.personName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(item.description, fontSize = 11.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isPaid,
                                    onCheckedChange = { checked ->
                                        viewModel.updateDebtCredit(item.copy(status = if (checked) "LUNAS" else "BELUM_LUNAS"))
                                    }
                                )
                                Text(if (isPaid) "Lunas" else "Belum", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { viewModel.deleteDebtCredit(item) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Nominal: $currency ${String.format("%,.0f", item.amount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == "HUTANG") Color(0xFFEF4444) else Color(0xFF059669)
                            )
                            Text(
                                "Jatuh Tempo: ${item.dueDate}",
                                fontSize = 11.sp,
                                color = if (isPaid) Color.Gray else Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Add Debt/Credit dialog
        if (showAddDialog) {
            AddDebtCreditDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, amount, type, dueDate, desc, isReminder ->
                    viewModel.addDebtCredit(name, amount, type, dueDate, desc, isReminder)
                    showAddDialog = false
                    Toast.makeText(context, "Catatan hutang-piutang disimpan!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ==========================================
// --- CALENDAR SCREEN ---
// ==========================================
@Composable
fun CalendarScreen(
    transactions: List<TransactionEntity>,
    currency: String,
    viewModel: FinanceViewModel
) {
    val selectedDate by viewModel.calendarSelectedDate.collectAsState()
    
    // Simplistic visual calendar mock (Let's draw a nice standard 30 day grid starting with Monday)
    val calendarDays = remember {
        val days = mutableListOf<String>()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startMonthOffset = cal.get(Calendar.DAY_OF_WEEK) - 2 // Offset for monday
        
        // Populate previous month offset padding
        cal.add(Calendar.DAY_OF_MONTH, -startMonthOffset)
        for (i in 0..34) {
            days.add(df.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        days
    }

    val selectedDayTransactions = remember(transactions, selectedDate) {
        transactions.filter { it.date == selectedDate }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Kalender Transaksi", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Month indicator
        val currentMonthTitle = remember {
            SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
        }
        Text(currentMonthTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))

        // Grid weekdays
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val weekDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Grid Calendar days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(240.dp)
        ) {
            items(calendarDays.size) { index ->
                val dateStr = calendarDays[index]
                val dayNum = dateStr.split("-").last()
                val isSelected = dateStr == selectedDate
                val hasTransactions = transactions.any { it.date == dateStr }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .border(
                            if (hasTransactions && !isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else BorderStroke(0.dp, Color.Transparent),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectCalendarDate(dateStr) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayNum,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (hasTransactions) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List Transactions of Selected day
        Text("Transaksi pada $selectedDate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDayTransactions.isEmpty()) {
            Text("Tidak ada transaksi pada tanggal ini.", color = Color.Gray, fontSize = 12.sp)
        } else {
            selectedDayTransactions.forEach { tx ->
                TransactionRow(
                    tx = tx,
                    currency = currency,
                    onFavorite = { viewModel.toggleFavorite(tx) },
                    onDuplicate = { viewModel.duplicateTransaction(tx) },
                    onDelete = { viewModel.deleteTransaction(tx) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

// ==========================================
// --- CATEGORIES LIST SCREEN ---
// ==========================================
@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    viewModel: FinanceViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daftar Kategori", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah Kategori", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories.size) { index ->
                val cat = categories[index]
                val catColor = parseColorHex(cat.colorHex)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(catColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(getIconForName(cat.iconName), contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(cat.type, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteCategoryByName(cat.name) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCategoryDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, type, hex, iconName ->
                    viewModel.addCategory(name, type, hex, iconName)
                    showAddDialog = false
                    Toast.makeText(context, "Kategori baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ==========================================
// --- SETTINGS SCREEN ---
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    isDark: Boolean,
    onToggleDark: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val currentCurrency by viewModel.currency.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()
    var backupCode by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Pengaturan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sistem Keuangan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                // Currency select
                Text("Mata Uang Acuan", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Rp", "$", "€", "¥").forEach { curr ->
                        Box(
                            modifier = Modifier
                                .clickable { viewModel.setCurrency(curr) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .background(
                                    if (currentCurrency == curr) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(curr, color = if (currentCurrency == curr) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language Select
                Text("Bahasa Aplikasi", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.setLanguage("id") }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .background(
                                if (currentLanguage == "id") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text("Bahasa Indonesia", color = if (currentLanguage == "id") Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.setLanguage("en") }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .background(
                                if (currentLanguage == "en") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text("English", color = if (currentLanguage == "en") Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backup Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cadangkan & Pemulihan (Backup)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        backupCode = viewModel.backupData()
                        Toast.makeText(context, "Kode Cadangan disalin: $backupCode", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buat Cadangan Baru")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = backupCode,
                    onValueChange = { backupCode = it },
                    placeholder = { Text("Tempel Kode Pemulihan di Sini") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (viewModel.restoreData(backupCode)) {
                            Toast.makeText(context, "Pemulihan data berhasil!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Kode Cadangan Tidak Valid!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pulihkan Data")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Akun & Sinkronisasi Card
        val userName by viewModel.currentUserName.collectAsState()
        val userEmail by viewModel.currentUserEmail.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profil Akun Pengguna", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Nama Pengguna", fontSize = 11.sp, color = Color.Gray)
                Text(userName.ifBlank { "Tidak Ada Nama" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Alamat Email", fontSize = 11.sp, color = Color.Gray)
                Text(userEmail.ifBlank { "Tidak Ada Email" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.logoutUser()
                        Toast.makeText(context, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar (Logout)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ Penyimpanan Lokal (Lokal Storage) Aktif",
                    fontSize = 11.sp,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// --- DIALOGS FOR ADDS ---
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAddTransaction: (Double, String, String, String, String, String, String, String, String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PENGELUARAN") } // "PEMASUKAN" or "PENGELUARAN"
    var selectedCategory by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var status by remember { mutableStateOf("BERHASIL") }
    var ocrCheckMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(type, categories) {
        val typeFiltered = categories.filter { it.type == type }
        if (typeFiltered.isNotEmpty()) {
            selectedCategory = typeFiltered.first().name
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Tambah Transaksi Cepat", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(14.dp))

                // Type select
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { type = "PENGELUARAN" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PENGELUARAN") Color(0xFFEF4444) else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Keluar (Expense)", color = if (type == "PENGELUARAN") Color.White else Color.Black)
                    }

                    Button(
                        onClick = { type = "PEMASUKAN" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PEMASUKAN") Color(0xFF059669) else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Masuk (Income)", color = if (type == "PEMASUKAN") Color.White else Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Nominal Transaksi") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("amount_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / Keterangan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category dropdown representation (clickable horizontal row for high premium simplicity)
                Text("Pilih Kategori", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.filter { it.type == type }.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clickable { selectedCategory = cat.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .background(
                                    if (selectedCategory == cat.name) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(cat.name, color = if (selectedCategory == cat.name) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Method selection row
                Text("Metode Pembayaran", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val listMethods = listOf("Cash", "Transfer", "QRIS", "Debit", "Kredit", "E-Wallet")
                    listMethods.forEach { method ->
                        Box(
                            modifier = Modifier
                                .clickable { paymentMethod = method }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .background(
                                    if (paymentMethod == method) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(method, color = if (paymentMethod == method) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status
                Text("Status Transaksi", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("BERHASIL", "PENDING", "DIBATALKAN").forEach { stat ->
                        Box(
                            modifier = Modifier
                                .clickable { status = stat }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .background(
                                    if (status == stat) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(stat, color = if (status == stat) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mock OCR Nota structure
                Button(
                    onClick = {
                        ocrCheckMessage = "Membaca Nota QRIS... Ditemukan nominal Rp 120.000 kategori Makan. Data diisikan otomatis!"
                        amount = "120000"
                        note = "Makan Siang Sederhana (OCR)"
                        type = "PENGELUARAN"
                        paymentMethod = "QRIS"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulasi Unggah & Baca Nota (OCR)", fontSize = 12.sp)
                }

                ocrCheckMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color(0xFF059669), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons row
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val doubleAmt = amount.toDoubleOrNull() ?: 0.0
                            if (doubleAmt > 0 && selectedCategory.isNotEmpty()) {
                                onAddTransaction(doubleAmt, type, selectedCategory, note, paymentMethod, status, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), null)
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_transaction_button")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty()) {
            selectedCategory = categories.first().name
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tambah Batas Anggaran", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                // Category List horizontal row
                Text("Kategori Belanja", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clickable { selectedCategory = cat.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .background(
                                    if (selectedCategory == cat.name) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(cat.name, color = if (selectedCategory == cat.name) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Batas Anggaran Bulanan") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val limitDouble = limit.toDoubleOrNull() ?: 0.0
                            if (limitDouble > 0 && selectedCategory.isNotEmpty()) {
                                onSave(selectedCategory, limitDouble)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddSavingGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tambah Target Tabungan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Target (cth: Laptop Baru)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Nominal Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Saldo Terkumpul Saat Ini") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan Tambahan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val targetAmt = target.toDoubleOrNull() ?: 0.0
                            val currentAmt = current.toDoubleOrNull() ?: 0.0
                            if (name.isNotEmpty() && targetAmt > 0) {
                                onSave(name, targetAmt, currentAmt, note.ifEmpty { null })
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun TopUpSavingGoalDialog(
    goal: SavingGoalEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Isi Tabungan: ${goal.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Masukkan Nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val doubleAmt = amount.toDoubleOrNull() ?: 0.0
                            if (doubleAmt > 0) {
                                onSave(doubleAmt)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tambahkan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddDebtCreditDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("HUTANG") }
    var desc by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var isReminder by remember { mutableStateOf(true) }

    LaunchedEffect(dueDate) {
        if (dueDate.isEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 7) // Default 7 days
            dueDate = sdf.format(cal.time)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tambah Hutang Piutang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                // Type select
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { type = "HUTANG" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "HUTANG") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hutang Saya", color = if (type == "HUTANG") Color.White else Color.Black)
                    }

                    Button(
                        onClick = { type = "PIUTANG" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PIUTANG") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Piutang Saya", color = if (type == "PIUTANG") Color.White else Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Orang") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Nominal Uang") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Jatuh Tempo (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Keterangan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val doubleAmt = amount.toDoubleOrNull() ?: 0.0
                            if (name.isNotEmpty() && doubleAmt > 0) {
                                onSave(name, doubleAmt, type, dueDate, desc, isReminder)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PENGELUARAN") }
    var colorHex by remember { mutableStateOf("#10B981") }
    var iconName by remember { mutableStateOf("restaurant") }

    val presetColors = listOf("#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#06B6D4", "#6B7280")
    val presetIcons = listOf("restaurant", "directions_car", "shopping_cart", "receipt", "wifi", "phone_android", "school", "local_hospital", "sports_esports")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tambah Kategori Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                // Type select
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { type = "PENGELUARAN" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PENGELUARAN") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pengeluaran", color = if (type == "PENGELUARAN") Color.White else Color.Black)
                    }

                    Button(
                        onClick = { type = "PEMASUKAN" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "PEMASUKAN") MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pemasukan", color = if (type == "PEMASUKAN") Color.White else Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preset Colors Selection
                Text("Pilih Warna", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { colStr ->
                        val col = parseColorHex(colStr)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    if (colorHex == colStr) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent),
                                    CircleShape
                                )
                                .clickable { colorHex = colStr }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preset Icons Selection
                Text("Pilih Simbol", fontSize = 11.sp, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetIcons.forEach { iconKey ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (iconName == iconKey) MaterialTheme.colorScheme.primaryContainer else Color.LightGray.copy(alpha = 0.2f)
                                )
                                .clickable { iconName = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(getIconForName(iconKey), contentDescription = null, modifier = Modifier.size(20.dp), tint = if (iconName == iconKey) MaterialTheme.colorScheme.primary else Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                onSave(name, type, colorHex, iconName)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
