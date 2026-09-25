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
import com.cardscanner.app.ui.list.CardListScreen
import com.cardscanner.app.ui.scan.ScanScreen
import com.cardscanner.app.ui.theme.CardScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CardScannerTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "cards") {
                    composable("cards") {
                        CardListScreen(
                            onScan = { nav.navigate("scan") },
                            onImagePicked = { path -> nav.navigate("edit?image=${Uri.encode(path)}") },
                            onOpen = { id -> nav.navigate("card/$id") },
                        )
                    }
                    composable("scan") {
                        ScanScreen(
                            onBack = { nav.popBackStack() },
                            onCaptured = { path ->
                                nav.navigate("edit?image=${Uri.encode(path)}") {
                                    popUpTo("scan") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(
                        "edit?image={image}&id={id}",
                        arguments = listOf(
                            navArgument("image") { type = NavType.StringType; nullable = true },
                            navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                        ),
                    ) { entry ->
                        EditCardScreen(
                            imagePath = entry.arguments?.getString("image"),
                            cardId = entry.arguments?.getLong("id") ?: 0L,
                            onBack = { nav.popBackStack() },
                            onSaved = { id ->
                                nav.navigate("card/$id") {
                                    popUpTo("cards")
                                }
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
