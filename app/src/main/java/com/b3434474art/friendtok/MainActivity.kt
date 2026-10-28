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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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
private enum class Tab { Home, Discover, Create, Likes, Profile, CreatorProfile }
private data class Post(
    val id: String, val uri: String, val caption: String, val user: String,
    val liked: Boolean = false, val comments: List<String> = emptyList(), val ownerId: String = ""
)

@Composable
private fun FriendTok(dark: Boolean, setDark: (Boolean) -> Unit, play: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.Home) }
    var name by remember { mutableStateOf("FriendTok user") }
    var user by remember { mutableStateOf("newfriend") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var followers by remember { mutableLongStateOf(0L) }
    var followingCount by remember { mutableLongStateOf(0L) }
    var profileViews by remember { mutableLongStateOf(0L) }
    var creatorProfileId by remember { mutableStateOf<String?>(null) }
    var creatorProfileName by remember { mutableStateOf("") }
    var creatorAvatar by remember { mutableStateOf<String?>(null) }
    var creatorBio by remember { mutableStateOf("") }
    var creatorFollowers by remember { mutableLongStateOf(0L) }
    var creatorFollowing by remember { mutableLongStateOf(false) }
    var draftCaptions by remember { mutableStateOf(context.getSharedPreferences("friendtok_drafts", 0).getStringSet("captions", emptySet())?.toList() ?: emptyList()) }
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
            posts.addAll(remote.map { Post(it.id, SupabaseBackend.publicVideoUrl(it.videoPath), it.caption, it.userId.take(8), ownerId = it.userId) })
            message = "Online feed loaded: " + posts.size + " videos."
        } catch (e: Exception) {
            message = "Could not load online feed: " + (e.message ?: "check your connection")
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runTask { avatarUrl = SupabaseBackend.uploadAvatar(context, uri); "Profile picture updated." }
    }

    LaunchedEffect(refresh, isSignedIn) {
        if (SupabaseBackend.isSignedIn) {
            runCatching {
                val profile = SupabaseBackend.loadProfile(SupabaseBackend.userId!!)
                user = profile.username
                name = profile.displayName
                bio = profile.bio
                avatarUrl = profile.avatarUrl
                val stats = SupabaseBackend.profileStats(profile.id)
                followers = stats.followers
                followingCount = stats.following
                profileViews = stats.views
            }
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
        NavigationBar { Tab.entries.filter { it != Tab.CreatorProfile }.forEach { t ->
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
                    if (posts.isEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("No public videos yet. Sign in and upload the first one!")
                        Button(onClick = { tab = Tab.Create }) { Icon(Icons.Default.Add, null); Text("Create video") }
                    } else {
                        val pagerState = rememberPagerState(pageCount = { posts.size })
                        VerticalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f), pageSpacing = 0.dp) { page ->
                            val p = posts[page]
                            SwipeFeedPost(
                                post = p,
                                active = pagerState.currentPage == page,
                                like = {
                                    if (!SupabaseBackend.isSignedIn) message = "Sign in to like videos."
                                    else runTask { SupabaseBackend.setLike(p.id, !p.liked); refresh++; "Like updated." }
                                },
                                comment = { commentFor = p.id },
                                openProfile = { creatorProfileId = p.ownerId; tab = Tab.CreatorProfile }
                            )
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
                    OutlinedButton(onClick = {
                        val saved = (draftCaptions + caption).distinct()
                        context.getSharedPreferences("friendtok_drafts", 0).edit().putStringSet("captions", saved.toSet()).apply()
                        draftCaptions = saved
                        message = "Draft saved on this device."
                    }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Save, null); Text("Save draft") }
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
                Tab.CreatorProfile -> {
                    val creatorId = creatorProfileId
                    LaunchedEffect(creatorId) {
                        if (!creatorId.isNullOrBlank()) runCatching {
                            val profile = SupabaseBackend.loadProfile(creatorId)
                            creatorProfileName = profile.displayName.ifBlank { profile.username }
                            creatorAvatar = profile.avatarUrl
                            creatorBio = profile.bio
                            creatorFollowers = SupabaseBackend.profileStats(creatorId).followers
                            creatorFollowing = SupabaseBackend.isFollowing(creatorId)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (creatorAvatar != null) AsyncImage(model = creatorAvatar, contentDescription = "Creator profile picture", modifier = Modifier.size(82.dp), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(82.dp), tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(creatorProfileName.ifBlank { "Creator" }, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("@${posts.firstOrNull { it.ownerId == creatorId }?.user ?: "creator"}")
                            Text("$creatorFollowers followers • ${posts.count { it.ownerId == creatorId }} videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (creatorBio.isNotBlank()) Text(creatorBio)
                    if (creatorId != SupabaseBackend.userId) Button(enabled = SupabaseBackend.isSignedIn, onClick = {
                        runTask { SupabaseBackend.setFollowing(creatorId ?: return@runTask "Creator not found.", !creatorFollowing); creatorFollowing = !creatorFollowing; "Follow status updated." }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (creatorFollowing) "Following" else "Follow") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Videos", "Shorts", "Posts").forEach { FilterChip(selected = profileSection == it, onClick = { profileSection = it }, label = { Text(it) }) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Latest", "Popular", "Oldest").forEach { FilterChip(selected = profileSort == it, onClick = { profileSort = it }, label = { Text(it) }) } }
                    val creatorPosts = posts.filter { it.ownerId == creatorId }
                    val sortedCreatorPosts = when (profileSort) { "Oldest" -> creatorPosts.reversed(); "Popular" -> creatorPosts.sortedByDescending { it.viewCount }; else -> creatorPosts }
                    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        gridItems(sortedCreatorPosts, key = { it.id }) { p -> Column(Modifier.fillMaxWidth().clickable { play(p.uri) }.padding(4.dp)) {
                            Box(Modifier.fillMaxWidth().aspectRatio(0.72f).background(Brush.linearGradient(listOf(Color(0xFF37214D), Color(0xFF111827), Color(0xFF7A2456))), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Color.White) }
                            Text(p.caption.ifBlank { "Video" }, maxLines = 2, style = MaterialTheme.typography.labelSmall)
                            Text("${p.viewCount} views", style = MaterialTheme.typography.labelSmall)
                        } }
                    }
                }
                Tab.Profile -> {
                    if (isSignedIn) {
                        val ownPosts = posts.filter { it.user == (SupabaseBackend.userId?.take(8) ?: "") }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(88.dp).clickable { avatarPicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                                if (avatarUrl != null) AsyncImage(model = avatarUrl, contentDescription = "Profile picture", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(Icons.Default.AccountCircle, null, modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text(name, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                Text("@$user", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$followers followers  ›  ${ownPosts.size} videos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("Tap profile picture to change it", style = MaterialTheme.typography.labelSmall)
                        if (bio.isNotBlank()) Text(bio, maxLines = 2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { message = "Analytics: $profileViews total video views • ${ownPosts.size} videos • $followers followers • $followingCount following" }) { Icon(Icons.Default.BarChart, null); Text("Analytics") }
                            OutlinedButton(onClick = { message = "Edit your display name and bio below." }) { Icon(Icons.Default.Edit, null); Text("Edit profile") }
                        }
                        Button(onClick = { message = "Community features are coming soon." }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Groups, null); Text("Community") }
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            listOf("Shorts", "Posts").forEach { section ->
                                Column(Modifier.clickable { profileSection = section }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(section, color = if (profileSection == section) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (profileSection == section) Box(Modifier.padding(top = 4.dp).width(34.dp).height(3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Latest", "Popular", "Oldest").forEach { sort -> FilterChip(selected = profileSort == sort, onClick = { profileSort = sort }, label = { Text(sort) }) }
                        }
                        val sortedOwnPosts = when(profileSort) { "Oldest" -> ownPosts.reversed(); "Popular" -> ownPosts.reversed(); else -> ownPosts }
                        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            item {
                                Column(Modifier.fillMaxWidth().clickable { message = "Draft saving is not available yet." }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.fillMaxWidth().aspectRatio(0.72f).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Default.InsertDriveFile, null, modifier = Modifier.size(28.dp)) }
                                    Text("Drafts (${draftCaptions.size})", style = MaterialTheme.typography.labelSmall)
                                    draftCaptions.take(2).forEach { draft -> Text(draft.ifBlank { "Untitled" }, maxLines = 1, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable { caption = draft; tab = Tab.Create }) }
                                }
                            }
                            gridItems(sortedOwnPosts, key = { it.id }) { p ->
                                Column(Modifier.fillMaxWidth().clickable { play(p.uri) }.padding(4.dp)) {
                                    Box(Modifier.fillMaxWidth().aspectRatio(0.72f).background(Brush.linearGradient(listOf(Color(0xFF37214D), Color(0xFF111827), Color(0xFF7A2456))), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
                                    Text(p.caption.ifBlank { "Video" }, maxLines = 2, style = MaterialTheme.typography.labelSmall)
                                    Text("0 views", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                    Text("Account & profile", style = MaterialTheme.typography.headlineSmall)
                    Text(if (isSignedIn) "Signed in to FriendTok" else "Sign in or create an account")
                    OutlinedTextField(user, { user = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(24) }, Modifier.fillMaxWidth(), label = { Text("Username (3–24 letters, numbers, or _)") }, singleLine = true)
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password (8+ characters)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    if (!isSignedIn) {
                        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                        Button(enabled = !busy, onClick = { runTask { SupabaseBackend.signUp(user, password, name) } }, modifier = Modifier.fillMaxWidth()) { Text("Create account") }
                        Button(enabled = !busy, onClick = { runTask { SupabaseBackend.signIn(user, password) } }, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
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
private fun SwipeFeedPost(post: Post, active: Boolean, like: () -> Unit, comment: () -> Unit, openProfile: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(post.id, active) { if (active) runCatching { SupabaseBackend.recordView(post.id) } }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    tag = ""
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        if (active) start() else pause()
                    }
                }
            },
            update = { video ->
                if (video.tag != post.uri) {
                    video.tag = post.uri
                    video.setVideoURI(Uri.parse(post.uri))
                    video.setOnPreparedListener { player ->
                        player.isLooping = true
                        if (active) video.start() else video.pause()
                    }
                } else if (active) {
                    if (!video.isPlaying) video.start()
                } else if (video.isPlaying) video.pause()
            }
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color.Transparent, Color(0xCC000000)))))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(0.78f).padding(start = 16.dp, end = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.clickable { openProfile() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(34.dp))
                Text("@${post.user}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            if (post.caption.isNotBlank()) Text(post.caption, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text("Swipe up for next video", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        }
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            IconButton(onClick = like) {
                Icon(if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like", tint = if (post.liked) Color(0xFFFF4F9A) else Color.White, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = comment) {
                Icon(Icons.Default.ChatBubbleOutline, "Comment", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Share, "Share", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun PostView(post: Post, play: (String) -> Unit, like: () -> Unit, comment: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("@"+post.user, fontWeight = FontWeight.Bold)
            Text(post.caption.ifBlank { "Video post" })
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { play(post.uri) }) { Icon(Icons.Default.PlayArrow, null); Text("Play video") }
                TextButton(onClick = like) { Icon(if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Text("Like") }
                TextButton(onClick = comment) { Text("Comment") }
            }
        }
    }
}
