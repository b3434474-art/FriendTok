package com.b3434474art.friendtok

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block supported screenshots and screen-recording captures in this app window.
        // This cannot prevent filming the display with a separate device.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            var darkMode by remember { mutableStateOf(true) }
            FriendTokTheme(darkMode = darkMode) {
                FriendTokApp(
                    darkMode = darkMode,
                    onThemeChange = { darkMode = it }
                )
            }
        }
    }
}

@Composable
private fun FriendTokTheme(darkMode: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkMode) {
        darkColorScheme(
            primary = Color(0xFFFF4F9A),
            secondary = Color(0xFFB9A2FF),
            background = Color(0xFF101014),
            surface = Color(0xFF1B1B22)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFD81B70),
            secondary = Color(0xFF6550B5),
            background = Color(0xFFFFF8FC),
            surface = Color(0xFFFFFFFF)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private enum class Tab(val label: String) {
    Home("Home"),
    Discover("Discover"),
    Create("Create"),
    Likes("Likes"),
    Profile("Profile")
}

@Composable
private fun FriendTokApp(darkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    val icon = when (tab) {
                        Tab.Home -> Icons.Default.Home
                        Tab.Discover -> Icons.Default.Explore
                        Tab.Create -> Icons.Default.Add
                        Tab.Likes -> Icons.Default.FavoriteBorder
                        Tab.Profile -> Icons.Default.Person
                    }
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FriendTok",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(20.dp))

            when (Tab.entries[selectedTab]) {
                Tab.Home -> HomeScreen()
                Tab.Discover -> PlaceholderScreen("Discover", "Find public videos from the FriendTok community.")
                Tab.Create -> PlaceholderScreen("Create a video", "Camera recording and gallery upload are coming next.")
                Tab.Likes -> PlaceholderScreen("Liked videos", "Videos you like will appear here after accounts are connected.")
                Tab.Profile -> SettingsProfileScreen(darkMode, onThemeChange)
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Text("Your feed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "Short videos. Good friends. Your corner of the internet.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
    )
    Spacer(Modifier.height(18.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF37214D), Color(0xFF111827), Color(0xFF7A2456))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color.White.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Your first video goes here", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("Welcome to FriendTok!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "The feed will show public videos after the backend is connected.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            Button(onClick = { /* Create flow is planned */ }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  Create a video")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Text(description, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
}

@Composable
private fun SettingsProfileScreen(darkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    Text("Profile & settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(18.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dark mode", fontWeight = FontWeight.SemiBold)
                Text(
                    if (darkMode) "On" else "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Switch(checked = darkMode, onCheckedChange = onThemeChange)
        }
    }
    Spacer(Modifier.height(14.dp))
    Text("Account sign-in, public profile editing, and account deletion will be added with the backend.")
}
