package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CrmViewModel
import com.example.ui.screens.*
import kotlinx.coroutines.launch

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val screenIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: CrmViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(0) }
    var selectedProfileClientId by remember { mutableStateOf<Int?>(null) }

    val navItems = listOf(
        NavigationItem("Dashboard", Icons.Default.Dashboard, 0),
        NavigationItem("Clients", Icons.Default.People, 1),
        NavigationItem("Packages", Icons.Default.BookmarkBorder, 2),
        NavigationItem("Tasks", Icons.Default.Task, 3),
        NavigationItem("Bills", Icons.Default.ReceiptLong, 4),
        NavigationItem("Payments", Icons.Default.Payment, 5),
        NavigationItem("Expenses", Icons.Default.MoneyOff, 6),
        NavigationItem("Activity Logs", Icons.Default.History, 8),
        NavigationItem("Settings", Icons.Default.Settings, 7)
    )

    // Helper back navigation
    val onBackToClients = {
        selectedProfileClientId = null
    }

    // Modal Drawer content for mobile
    val drawerContent = @Composable {
        ModalDrawerSheet(
            drawerContainerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.width(280.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            // AB Graphics Brand Banner in Drawer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "AB GRAPHICS",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Personal CRM Console",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            navItems.forEach { item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.title, fontWeight = FontWeight.Bold) },
                    selected = currentScreen == item.screenIndex,
                    onClick = {
                        currentScreen = item.screenIndex
                        selectedProfileClientId = null // reset profile state
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedTextColor = MaterialTheme.colorScheme.onBackground,
                        unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }

    if (isTablet) {
        // --- DESKTOP/TABLET SIDEBAR VIEW LAYOUT ---
        Row(modifier = Modifier.fillMaxSize()) {
            // Static responsive sidebar on left side
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Brand Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "AB GRAPHICS",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "Control Station",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Sidebar items
                navItems.forEach { item ->
                    val isSelected = currentScreen == item.screenIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                currentScreen = item.screenIndex
                                selectedProfileClientId = null
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Content Panel on the right
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AppContent(
                    screenIndex = currentScreen,
                    selectedProfileClientId = selectedProfileClientId,
                    viewModel = viewModel,
                    onNavigateToClients = {
                        currentScreen = 1
                        selectedProfileClientId = null
                    },
                    onNavigateToTasks = {
                        currentScreen = 3
                    },
                    onNavigateToExpenses = {
                        currentScreen = 6
                    },
                    onNavigateToClientProfile = { id ->
                        currentScreen = 1
                        selectedProfileClientId = id
                    },
                    onBackToClients = onBackToClients
                )
            }
        }
    } else {
        // --- MOBILE DRAWER VIEW LAYOUT ---
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = drawerContent
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentScreen) {
                                    0 -> "Dashboard"
                                    1 -> if (selectedProfileClientId != null) "Portfolio Profile" else "Clients"
                                    2 -> "Packages"
                                    3 -> "Tasks"
                                    4 -> "Bills"
                                    5 -> "Payments"
                                    6 -> "Expenses"
                                    7 -> "Settings"
                                    8 -> "Activity Logs"
                                    else -> "AB CRM"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (currentScreen == 1 && selectedProfileClientId != null) {
                                IconButton(onClick = onBackToClients) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AppContent(
                        screenIndex = currentScreen,
                        selectedProfileClientId = selectedProfileClientId,
                        viewModel = viewModel,
                        onNavigateToClients = {
                            currentScreen = 1
                            selectedProfileClientId = null
                        },
                        onNavigateToTasks = {
                            currentScreen = 3
                        },
                        onNavigateToExpenses = {
                            currentScreen = 6
                        },
                        onNavigateToClientProfile = { id ->
                            currentScreen = 1
                            selectedProfileClientId = id
                        },
                        onBackToClients = onBackToClients
                    )
                }
            }
        }
    }
}

@Composable
fun AppContent(
    screenIndex: Int,
    selectedProfileClientId: Int?,
    viewModel: CrmViewModel,
    onNavigateToClients: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToClientProfile: (Int) -> Unit,
    onBackToClients: () -> Unit
) {
    when (screenIndex) {
        0 -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToClients = onNavigateToClients,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToExpenses = onNavigateToExpenses,
            onNavigateToClientProfile = onNavigateToClientProfile
        )
        1 -> {
            if (selectedProfileClientId != null) {
                ClientProfileScreen(
                    clientId = selectedProfileClientId,
                    viewModel = viewModel,
                    onBack = onBackToClients
                )
            } else {
                ClientsScreen(
                    viewModel = viewModel,
                    onNavigateToClientProfile = onNavigateToClientProfile
                )
            }
        }
        2 -> ServicesPackagesScreen(viewModel = viewModel)
        3 -> TasksScreen(
            viewModel = viewModel,
            onNavigateToClientProfile = onNavigateToClientProfile
        )
        4 -> BillsScreen(
            viewModel = viewModel,
            onNavigateToClientProfile = onNavigateToClientProfile
        )
        5 -> PaymentsScreen(viewModel = viewModel)
        6 -> ExpensesScreen(viewModel = viewModel)
        7 -> SettingsScreen(viewModel = viewModel)
        8 -> ActivityLogsScreen(
            viewModel = viewModel,
            onNavigateToClientProfile = onNavigateToClientProfile
        )
    }
}
