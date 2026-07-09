package br.com.sos.osmobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.sos.osmobile.core.di.AppContainer
import br.com.sos.osmobile.feature.audit.AuditScreen
import br.com.sos.osmobile.feature.backup.BackupScreen
import br.com.sos.osmobile.feature.backup.BackupViewModel
import br.com.sos.osmobile.feature.customers.CustomerScreen
import br.com.sos.osmobile.feature.customers.CustomerViewModel
import br.com.sos.osmobile.feature.dashboard.DashboardScreen
import br.com.sos.osmobile.feature.dashboard.DashboardViewModel
import br.com.sos.osmobile.feature.details.CustomerDetailScreen
import br.com.sos.osmobile.feature.details.CustomerDetailViewModel
import br.com.sos.osmobile.feature.details.QuoteDetailScreen
import br.com.sos.osmobile.feature.details.QuoteDetailViewModel
import br.com.sos.osmobile.feature.details.WorkOrderDetailScreen
import br.com.sos.osmobile.feature.details.WorkOrderDetailViewModel
import br.com.sos.osmobile.feature.quotes.QuoteScreen
import br.com.sos.osmobile.feature.quotes.QuoteViewModel
import br.com.sos.osmobile.feature.services.ServiceProductScreen
import br.com.sos.osmobile.feature.services.ServiceProductViewModel
import br.com.sos.osmobile.feature.settings.SettingsScreen
import br.com.sos.osmobile.feature.settings.SettingsViewModel
import br.com.sos.osmobile.feature.workorders.WorkOrderListScreen
import br.com.sos.osmobile.feature.workorders.WorkOrderScreen
import br.com.sos.osmobile.feature.workorders.WorkOrderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSMobileApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: AppRoute.Dashboard.route
    val currentLabel = AppRoute.entries.firstOrNull { it.route == currentRoute }?.label ?: "OS Mobile"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("OS Mobile", modifier = Modifier.padding(24.dp, 20.dp, 16.dp, 12.dp))
                AppRoute.entries.forEach { route ->
                    NavigationDrawerItem(
                        label = { Text(route.label) },
                        selected = currentRoute == route.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(route.route) {
                                launchSingleTop = true
                                popUpTo(AppRoute.Dashboard.route)
                            }
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentLabel) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Abrir menu")
                        }
                    },
                )
            },
        ) { contentPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.Dashboard.route,
                modifier = Modifier.padding(contentPadding),
            ) {
                composable(AppRoute.Dashboard.route) {
                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModel.factory(
                            workOrderRepository = appContainer.workOrderRepository,
                            quoteRepository = appContainer.quoteRepository,
                            customerRepository = appContainer.customerRepository,
                        ),
                    )
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onCustomerClick = { navController.navigate("customer/$it") },
                        onQuoteClick = { navController.navigate("quote/$it") },
                        onWorkOrderClick = { navController.navigate("work_orders/edit/$it") },
                    )
                }
                composable("customer/{id}") { backStack ->
                    val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    val vm: CustomerDetailViewModel = viewModel(
                        factory = CustomerDetailViewModel.factory(
                            id,
                            appContainer.customerRepository,
                            appContainer.quoteRepository,
                            appContainer.workOrderRepository,
                        ),
                    )
                    CustomerDetailScreen(
                        viewModel = vm,
                        onQuoteClick = { navController.navigate("quote/$it") },
                        onWorkOrderClick = { navController.navigate("work_orders/edit/$it") },
                    )
                }
                composable("quote/{id}") { backStack ->
                    val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    val vm: QuoteDetailViewModel = viewModel(
                        factory = QuoteDetailViewModel.factory(id, appContainer.quoteRepository, appContainer.auditRepository),
                    )
                    QuoteDetailScreen(vm)
                }
                composable("work_order/{id}") { backStack ->
                    val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: 0L
                    val vm: WorkOrderDetailViewModel = viewModel(
                        factory = WorkOrderDetailViewModel.factory(id, appContainer.workOrderRepository, appContainer.auditRepository),
                    )
                    WorkOrderDetailScreen(vm)
                }
                composable(AppRoute.Customers.route) {
                    val customerViewModel: CustomerViewModel = viewModel(
                        factory = CustomerViewModel.factory(
                            customerRepository = appContainer.customerRepository,
                            settingsRepository = appContainer.settingsRepository,
                            contactsRepository = appContainer.contactsRepository,
                        ),
                    )
                    CustomerScreen(viewModel = customerViewModel)
                }
                composable(AppRoute.Services.route) {
                    val serviceProductViewModel: ServiceProductViewModel = viewModel(
                        factory = ServiceProductViewModel.factory(appContainer.serviceProductRepository),
                    )
                    ServiceProductScreen(viewModel = serviceProductViewModel)
                }
                composable(AppRoute.Quotes.route) {
                    val quoteViewModel: QuoteViewModel = viewModel(
                        factory = QuoteViewModel.factory(
                            quoteRepository = appContainer.quoteRepository,
                            quoteConversionRepository = appContainer.quoteConversionRepository,
                            auditRepository = appContainer.auditRepository,
                            customerRepository = appContainer.customerRepository,
                            serviceProductRepository = appContainer.serviceProductRepository,
                        ),
                    )
                    QuoteScreen(viewModel = quoteViewModel)
                }
                composable(AppRoute.WorkOrders.route) {
                    val workOrderViewModel: WorkOrderViewModel = viewModel(
                        factory = WorkOrderViewModel.factory(
                            workOrderRepository = appContainer.workOrderRepository,
                            auditRepository = appContainer.auditRepository,
                            customerRepository = appContainer.customerRepository,
                            serviceProductRepository = appContainer.serviceProductRepository,
                        ),
                    )
                    WorkOrderScreen(viewModel = workOrderViewModel)
                }
                composable(AppRoute.WorkOrderList.route) {
                    val workOrderViewModel: WorkOrderViewModel = viewModel(
                        factory = WorkOrderViewModel.factory(
                            workOrderRepository = appContainer.workOrderRepository,
                            auditRepository = appContainer.auditRepository,
                            customerRepository = appContainer.customerRepository,
                            serviceProductRepository = appContainer.serviceProductRepository,
                        ),
                    )
                    WorkOrderListScreen(
                        viewModel = workOrderViewModel,
                        onEdit = { navController.navigate("work_orders/edit/$it") },
                    )
                }
                composable("work_orders/edit/{id}") { backStack ->
                    val id = backStack.arguments?.getString("id")?.toLongOrNull()
                    val workOrderViewModel: WorkOrderViewModel = viewModel(
                        factory = WorkOrderViewModel.factory(
                            workOrderRepository = appContainer.workOrderRepository,
                            auditRepository = appContainer.auditRepository,
                            customerRepository = appContainer.customerRepository,
                            serviceProductRepository = appContainer.serviceProductRepository,
                        ),
                    )
                    WorkOrderScreen(viewModel = workOrderViewModel, initialEditId = id)
                }
                composable(AppRoute.Backup.route) {
                    val backupViewModel: BackupViewModel = viewModel(
                        factory = BackupViewModel.factory(appContainer.backupRepository),
                    )
                    BackupScreen(viewModel = backupViewModel)
                }
                composable(AppRoute.Settings.route) {
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.factory(
                            repository = appContainer.settingsRepository,
                            contactsRepository = appContainer.contactsRepository,
                        ),
                    )
                    SettingsScreen(viewModel = settingsViewModel)
                }
                composable(AppRoute.Audit.route) {
                    AuditScreen(auditFlow = appContainer.auditRepository.observeRecent())
                }
            }
        }
    }
}
