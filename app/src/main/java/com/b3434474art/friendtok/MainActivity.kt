package com.b3434474art.friendtok

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            var dark by remember { mutableStateOf(true) }
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFFFF4F9A)) else lightColorScheme(primary = Color(0xFFD81B70))) {
                FriendTok(dark, { dark = it }, { uri ->
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(uri), "video/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }) }
                })
            }
        }
    }
}
private enum class Tab { Home, Discover, Create, Likes, Profile }
private data class Post(val id: Int, val uri: String, val caption: String, val user: String,
                        val liked: Boolean = false, val comments: List<String> = emptyList())

@Composable
private fun FriendTok(dark: Boolean, setDark: (Boolean) -> Unit, play: (String) -> Unit) {
    var tab by remember { mutableStateOf(Tab.Home) }
    var name by remember { mutableStateOf("FriendTok user") }
    var user by remember { mutableStateOf("newfriend") }
    var bio by remember { mutableStateOf("Hey! I'm on FriendTok.") }
    var caption by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var commentFor by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var nextId by remember { mutableIntStateOf(1) }
    val posts = remember { mutableStateListOf<Post>() }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { posts.add(0, Post(nextId++, uri.toString(), caption.ifBlank { "My video" }, "@$user")); caption = ""; message = "Added to local feed."; tab = Tab.Home }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) { posts.add(0, Post(nextId++, uri.toString(), caption.ifBlank { "My video" }, "@$user")); caption = ""; message = "Recording added."; tab = Tab.Home }
        else if (result.resultCode == android.app.Activity.RESULT_OK) message = "Camera did not return a video link."
    }
    Scaffold(bottomBar = {
        NavigationBar { Tab.entries.forEach { t ->
            val icon = when(t) { Tab.Home -> Icons.Default.Home; Tab.Discover -> Icons.Default.Search; Tab.Create -> Icons.Default.Add; Tab.Likes -> Icons.Default.Favorite; Tab.Profile -> Icons.Default.Person }
            NavigationBarItem(selected = tab == t, onClick = { tab = t }, icon = { Icon(icon, null) }, label = { Text(t.name) })
        } }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("FriendTok", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            when(tab) {
                Tab.Home -> {
                    Text("Your feed", style = MaterialTheme.typography.headlineSmall)
                    Text("Local videos on this device only.")
                    if (posts.isEmpty()) {
                        Spacer(Modifier.height(24.dp)); Text("Your feed is ready. Add a video to begin.")
                        Button(onClick = { tab = Tab.Create }) { Icon(Icons.Default.Add, null); Text("Create video") }
                    } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(posts, key = { it.id }) { p -> PostView(p, play, {
                        val i = posts.indexOfFirst { it.id == p.id }; if (i >= 0) posts[i] = p.copy(liked = !p.liked)
                    }, { commentFor = p.id }) } }
                }
                Tab.Discover -> {
                    Text("Discover", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search local captions/users") })
                    val found = posts.filter { it.caption.contains(search, true) || it.user.contains(search, true) }
                    LazyColumn { items(found, key = { it.id }) { p -> PostView(p, play, {
                        val i = posts.indexOfFirst { it.id == p.id }; if (i >= 0) posts[i] = p.copy(liked = !p.liked)
                    }, { commentFor = p.id }) } }
                }
                Tab.Create -> {
                    Text("Create a video", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(caption, { caption = it }, Modifier.fillMaxWidth(), label = { Text("Caption") })
                    Button(onClick = { camera.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply { putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) }) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Videocam, null); Text("Record video (60 sec max)") }
                    Button(onClick = { gallery.launch("video/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Choose from gallery") }
                    Text("These videos are not uploaded online; backend intentionally omitted.")
                }
                Tab.Likes -> {
                    Text("Liked videos", style = MaterialTheme.typography.headlineSmall)
                    val liked = posts.filter { it.liked }
                    if (liked.isEmpty()) Text("Like a video to see it here.")
                    LazyColumn { items(liked, key = { it.id }) { p -> PostView(p, play, {
                        val i = posts.indexOfFirst { it.id == p.id }; if (i >= 0) posts[i] = p.copy(liked = false)
                    }, { commentFor = p.id }) } }
                }
                Tab.Profile -> {
                    Text("Your profile", style = MaterialTheme.typography.headlineSmall)
                    Text("Local profile — no online account")
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") })
                    OutlinedTextField(user, { user = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(24) }, Modifier.fillMaxWidth(), label = { Text("Username") })
                    OutlinedTextField(bio, { bio = it.take(160) }, Modifier.fillMaxWidth(), label = { Text("Bio") })
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark mode"); Switch(dark, setDark)
                    }
                    Text("Profile edits only last for this session. Real sign-in and synced profiles need a backend.")
                }
            }
        }
    }
    val id = commentFor
    if (id != null) AlertDialog(
        onDismissRequest = { commentFor = null; commentText = "" },
        title = { Text("Comments") },
        text = {
            Column {
                val p = posts.firstOrNull { it.id == id }
                if (p == null || p.comments.isEmpty()) Text("No comments yet.")
                else p.comments.forEach { Text("• " + it) }
                OutlinedTextField(commentText, { commentText = it.take(200) }, label = { Text("Write a comment") })
            }
        },
        confirmButton = { TextButton(onClick = {
            val i = posts.indexOfFirst { it.id == id }
            if (i >= 0 && commentText.isNotBlank()) posts[i] = posts[i].copy(comments = posts[i].comments + commentText.trim())
            commentText = ""; commentFor = null
        }) { Text("Add comment") } },
        dismissButton = { TextButton(onClick = { commentFor = null }) { Text("Close") } }
    )
}

@Composable
private fun PostView(post: Post, play: (String) -> Unit, like: () -> Unit, comment: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(post.user, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(210.dp).background(Brush.linearGradient(listOf(Color(0xFF37214D), Color(0xFF111827), Color(0xFF7A2456))), RoundedCornerShape(14.dp)).clickable { play(post.uri) }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(52.dp)); Text("Tap to play video", color = Color.White) }
            }
            Text(post.caption)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = like) { Icon(if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Text(if (post.liked) " Liked" else " Like") }
                TextButton(onClick = comment) { Text("Comments (" + post.comments.size + ")") }
            }
        }
    }
}
