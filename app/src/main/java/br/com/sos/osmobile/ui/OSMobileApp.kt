package br.com.sos.osmobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.sos.osmobile.core.di.AppContainer
import br.com.sos.osmobile.feature.audit.AuditScreen
import br.com.sos.osmobile.feature.customers.CustomerScreen
import br.com.sos.osmobile.feature.customers.CustomerViewModel
import br.com.sos.osmobile.feature.quotes.QuoteScreen
import br.com.sos.osmobile.feature.quotes.QuoteViewModel
import br.com.sos.osmobile.feature.services.ServiceProductScreen
import br.com.sos.osmobile.feature.services.ServiceProductViewModel
import br.com.sos.osmobile.feature.settings.SettingsScreen
import br.com.sos.osmobile.feature.settings.SettingsViewModel
import br.com.sos.osmobile.feature.workorders.WorkOrderScreen
import br.com.sos.osmobile.feature.workorders.WorkOrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSMobileApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: AppRoute.Customers.route
    val currentLabel = AppRoute.entries.firstOrNull { it.route == currentRoute }?.label ?: "OS Mobile"

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(currentLabel) })
        },
        bottomBar = {
            NavigationBar {
                AppRoute.entries.forEach { route ->
                    NavigationBarItem(
                        selected = currentRoute == route.route,
                        onClick = {
                            navController.navigate(route.route) {
                                launchSingleTop = true
                                popUpTo(AppRoute.Customers.route)
                            }
                        },
                        label = { Text(route.label) },
                        icon = {},
                    )
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Customers.route,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(AppRoute.Customers.route) {
                val customerViewModel: CustomerViewModel = viewModel(
                    factory = CustomerViewModel.factory(
                        customerRepository = appContainer.customerRepository,
                        settingsRepository = appContainer.settingsRepository,
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
                        customerRepository = appContainer.customerRepository,
                        serviceProductRepository = appContainer.serviceProductRepository,
                    ),
                )
                WorkOrderScreen(viewModel = workOrderViewModel)
            }
            composable(AppRoute.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(appContainer.settingsRepository),
                )
                SettingsScreen(viewModel = settingsViewModel)
            }
            composable(AppRoute.Audit.route) {
                AuditScreen(auditFlow = appContainer.auditRepository.observeRecent())
            }
        }
    }
}
