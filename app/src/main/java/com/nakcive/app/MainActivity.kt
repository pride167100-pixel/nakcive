package com.nakcive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nakcive.app.ui.screens.AddRecordScreen
import com.nakcive.app.ui.screens.HomeScreen
import com.nakcive.app.ui.screens.MapScreen
import com.nakcive.app.ui.screens.RecordDetailScreen
import com.nakcive.app.ui.screens.RecordScreen
import com.nakcive.app.ui.screens.SettingsScreen
import com.nakcive.app.ui.screens.SpeciesScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(onNavigate = { route -> navController.navigate(route) })
                        }
                        composable("map") {
                            MapScreen(onBack = { navController.popBackStack() })
                        }
                        composable("record") {
                            RecordScreen(
                                onBack = { navController.popBackStack() },
                                onRecordClick = { recordId ->
                                    navController.navigate("record_detail/$recordId")
                                },
                            )
                        }
                        composable(
                            "record_detail/{recordId}",
                            arguments = listOf(navArgument("recordId") { type = NavType.LongType }),
                        ) { backStackEntry ->
                            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
                            RecordDetailScreen(
                                recordId = recordId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable("add") {
                            AddRecordScreen(onBack = { navController.popBackStack() })
                        }
                        composable("species") {
                            SpeciesScreen(onBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
