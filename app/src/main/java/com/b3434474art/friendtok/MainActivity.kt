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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            var dark by remember { mutableStateOf(true) }
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFFFF4F9A)) else lightColorScheme(primary = Color(0xFFD81B70))) {
                FriendTok(dark, { dark = it }, { uri ->
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(uri), "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }) }
                })
            }
        }
    }
}
private enum class Tab { Home, Discover, Create, Likes, Profile }
private data class Post(
    val id: String, val uri: String, val caption: String, val user: String,
    val liked: Boolean = false, val comments: List<String> = emptyList()
)

@Composable
private fun FriendTok(dark: Boolean, setDark: (Boolean) -> Unit, play: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.Home) }
    var name by remember { mutableStateOf("FriendTok user") }
    var user by remember { mutableStateOf("newfriend") }
    var bio by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }
    var commentFor by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Connecting to FriendTok…") }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val posts = remember { mutableStateListOf<Post>() }
    val isSignedIn = SupabaseBackend.isSignedIn

    fun runTask(task: suspend () -> String) {
        scope.launch {
            busy = true
            message = "Please wait…"
            try { message = task() }
            catch (e: Exception) { message = e.message ?: "Something went wrong." }
            finally { busy = false }
        }
    }

    LaunchedEffect(refresh) {
        try {
            val remote = SupabaseBackend.loadPublicFeed()
            posts.clear()
            posts.addAll(remote.map { Post(it.id, SupabaseBackend.publicVideoUrl(it.videoPath), it.caption, it.userId.take(8)) })
            message = "Online feed loaded: " + posts.size + " videos."
        } catch (e: Exception) {
            message = "Could not load online feed: " + (e.message ?: "check your connection")
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (!SupabaseBackend.isSignedIn) message = "Open Profile and sign in before uploading."
            else runTask {
                val post = SupabaseBackend.uploadVideo(context, uri, caption)
                caption = ""
                refresh++
                "Video uploaded!"
            }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            if (!SupabaseBackend.isSignedIn) message = "Recording received. Sign in before uploading it."
            else runTask {
                SupabaseBackend.uploadVideo(context, uri, caption)
                caption = ""
                refresh++
                "Recording uploaded!"
            }
        } else if (result.resultCode == android.app.Activity.RESULT_OK) {
            message = "Camera did not return a video link. Try choosing the saved video from Gallery."
        }
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
                    Text("Community feed", style = MaterialTheme.typography.headlineSmall)
                    if (posts.isEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("No public videos yet. Sign in and upload the first one!")
                        Button(onClick = { tab = Tab.Create }) { Icon(Icons.Default.Add, null); Text("Create video") }
                    } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(posts, key = { it.id }) { p ->
                            PostView(p, play, {
                                if (!SupabaseBackend.isSignedIn) message = "Sign in to like videos."
                                else runTask { SupabaseBackend.setLike(p.id, !p.liked); refresh++; "Like updated." }
                            }, { commentFor = p.id })
                        }
                    }
                }
                Tab.Discover -> {
                    Text("Discover", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search public captions/users") })
                    val found = posts.filter { it.caption.contains(search, true) || it.user.contains(search, true) }
                    if (found.isEmpty()) Text("No matching public videos.")
                    LazyColumn { items(found, key = { it.id }) { p -> PostView(p, play, {
                        if (!SupabaseBackend.isSignedIn) message = "Sign in to like videos."
                        else runTask { SupabaseBackend.setLike(p.id, !p.liked); refresh++; "Like updated." }
                    }, { commentFor = p.id }) } }
                }
                Tab.Create -> {
                    Text("Create a video", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(caption, { caption = it }, Modifier.fillMaxWidth(), label = { Text("Caption") })
                    Text("Sign in to upload videos to the public feed. Maximum upload size: 100 MB.", style = MaterialTheme.typography.bodySmall)
                    Button(enabled = !busy, onClick = { camera.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply { putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60) }) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Videocam, null); Text("Record video (60 sec max)")
                    }
                    Button(enabled = !busy, onClick = { gallery.launch("video/*") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Text("Choose and upload from gallery")
                    }
                }
                Tab.Likes -> {
                    Text("Liked videos", style = MaterialTheme.typography.headlineSmall)
                    Text("Likes are saved to your online account. The list refreshes when you return to the feed.")
                    val liked = posts.filter { it.liked }
                    if (liked.isEmpty()) Text("Like a video to see it here.")
                    LazyColumn { items(liked, key = { it.id }) { p -> PostView(p, play, {
                        if (SupabaseBackend.isSignedIn) runTask { SupabaseBackend.setLike(p.id, false); refresh++; "Like removed." }
                    }, { commentFor = p.id }) } }
                }
                Tab.Profile -> {
                    Text("Account & profile", style = MaterialTheme.typography.headlineSmall)
                    Text(if (isSignedIn) "Signed in to FriendTok" else "Sign in or create an account")
                    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password (8+ characters)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    if (!isSignedIn) {
                        OutlinedTextField(user, { user = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(24) }, Modifier.fillMaxWidth(), label = { Text("Username (for sign up)") }, singleLine = true)
                        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                        Button(enabled = !busy, onClick = { runTask { SupabaseBackend.signUp(email, password, user, name) } }, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
                        Button(enabled = !busy, onClick = { runTask { SupabaseBackend.signIn(email, password) } }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
                    } else {
                        OutlinedTextField(user, { user = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(24) }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
                        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                        OutlinedTextField(bio, { bio = it.take(160) }, Modifier.fillMaxWidth(), label = { Text("Bio") }, maxLines = 3)
                        Button(enabled = !busy, onClick = { runTask { SupabaseBackend.saveProfile(user, name, bio); "Profile saved online." } }, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
                        Button(onClick = { SupabaseBackend.signOut(); message = "Signed out."; refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark mode"); Switch(dark, setDark)
                    }
                }
            }
        }
    }
    val id = commentFor
    if (id != null) AlertDialog(
        onDismissRequest = { commentFor = null; commentText = "" },
        title = { Text("Add comment") },
        text = { OutlinedTextField(commentText, { commentText = it.take(500) }, label = { Text("Comment") }) },
        confirmButton = { TextButton(onClick = {
            if (!SupabaseBackend.isSignedIn) message = "Sign in to comment."
            else runTask { SupabaseBackend.addComment(id, commentText); commentText = ""; commentFor = null; "Comment posted online." }
        }) { Text("Post") } },
        dismissButton = { TextButton(onClick = { commentFor = null; commentText = "" }) { Text("Cancel") } }
    )
}

@Composable
private fun PostView(post: Post, play: (String) -> Unit, like: () -> Unit, comment: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("@" + post.user, fontWeight = FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(210.dp).background(
                Brush.linearGradient(listOf(Color(0xFF37214D), Color(0xFF111827), Color(0xFF7A2456))), RoundedCornerShape(14.dp)
            ).clickable { play(post.uri) }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(52.dp))
                    Text("Tap to play video", color = Color.White)
                }
            }
            Text(post.caption)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = like) { Icon(if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Text(if (post.liked) " Liked" else " Like") }
                TextButton(onClick = comment) { Text("Add comment") }
            }
        }
    }
}
