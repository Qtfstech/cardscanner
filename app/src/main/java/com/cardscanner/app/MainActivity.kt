package com.cardscanner.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cardscanner.app.ui.detail.CardDetailScreen
import com.cardscanner.app.ui.edit.EditCardScreen
import com.cardscanner.app.ui.list.ProjectScreen
import com.cardscanner.app.ui.projects.ProjectsScreen
import com.cardscanner.app.ui.scan.ScanScreen
import com.cardscanner.app.ui.theme.CardScannerTheme

private const val PROJECT_ROUTE = "project/{projectId}"
private const val SCAN_ROUTE = "scan/{projectId}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CardScannerTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "projects") {
                    composable("projects") {
                        ProjectsScreen(onOpen = { id -> nav.navigate("project/$id") })
                    }
                    composable(
                        PROJECT_ROUTE,
                        arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                    ) { entry ->
                        val projectId = entry.arguments?.getLong("projectId") ?: 0L
                        ProjectScreen(
                            projectId = projectId,
                            onBack = { nav.popBackStack("projects", inclusive = false) },
                            onScan = { nav.navigate("scan/$projectId") },
                            onOpenCard = { id -> nav.navigate("card/$id") },
                            // A newly started project replaces the current one, so Back returns to all projects.
                            onOpenProject = { id -> nav.navigate("project/$id") { popUpTo("projects") } },
                        )
                    }
                    composable(
                        SCAN_ROUTE,
                        arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                    ) { entry ->
                        val projectId = entry.arguments?.getLong("projectId") ?: 0L
                        ScanScreen(
                            onBack = { nav.popBackStack() },
                            onCaptured = { path ->
                                nav.navigate("edit?image=${Uri.encode(path)}&project=$projectId") {
                                    popUpTo(SCAN_ROUTE) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(
                        "edit?image={image}&id={id}&project={project}",
                        arguments = listOf(
                            navArgument("image") { type = NavType.StringType; nullable = true },
                            navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                            navArgument("project") { type = NavType.LongType; defaultValue = 0L },
                        ),
                    ) { entry ->
                        EditCardScreen(
                            imagePath = entry.arguments?.getString("image"),
                            cardId = entry.arguments?.getLong("id") ?: 0L,
                            projectId = entry.arguments?.getLong("project") ?: 0L,
                            onBack = { nav.popBackStack() },
                            onSaved = { id ->
                                nav.navigate("card/$id") { popUpTo(PROJECT_ROUTE) }
                            },
                        )
                    }
                    composable(
                        "card/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        CardDetailScreen(
                            cardId = entry.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                            onEdit = { id -> nav.navigate("edit?id=$id") },
                        )
                    }
                }
            }
        }
    }
}
