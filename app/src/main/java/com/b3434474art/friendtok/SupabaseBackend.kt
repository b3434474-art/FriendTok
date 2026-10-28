package com.b3434474art.friendtok

import android.content.Context
import android.net.Uri
import com.b3434474art.friendtok.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Small REST client for FriendTok's Supabase project.
 * Only the publishable key is embedded in the Android app. Never put a service-role key here.
 */
object SupabaseBackend {
    private val http = OkHttpClient()
    @Volatile private var accessToken: String? = null
    @Volatile private var currentUserId: String? = null

    data class FeedPost(
        val id: String,
        val userId: String,
        val videoPath: String,
        val caption: String,
        val createdAt: String
    )

    val userId: String? get() = currentUserId
    val isSignedIn: Boolean get() = !accessToken.isNullOrBlank() && !currentUserId.isNullOrBlank()

    private fun request(path: String, method: String = "GET", body: String? = null, extraHeaders: Map<String, String> = emptyMap()): String {
        val builder = Request.Builder()
            .url(BuildConfig.SUPABASE_URL.trimEnd('/') + path)
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Authorization", "Bearer " + (accessToken ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY))
        extraHeaders.forEach { (key, value) -> builder.header(key, value) }
        val requestBody = body?.toRequestBody("application/json; charset=utf-8".toMediaType())
        when (method) {
            "POST" -> builder.post(requestBody ?: ByteArray(0).toRequestBody(null))
            "PATCH" -> builder.patch(requestBody ?: ByteArray(0).toRequestBody(null))
            "DELETE" -> builder.delete(requestBody)
            else -> builder.get()
        }
        http.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val error = runCatching { JSONObject(text).optString("msg").ifBlank { JSONObject(text).optString("message") } }.getOrNull()
                throw IllegalStateException(error?.takeIf { it.isNotBlank() } ?: "Supabase request failed (${response.code}).")
            }
            return text
        }
    }

    suspend fun signUp(email: String, password: String, username: String, displayName: String): String = withContext(Dispatchers.IO) {
        require(email.contains("@")) { "Enter a valid email address." }
        require(password.length >= 8) { "Password must be at least 8 characters." }
        val auth = JSONObject().put("email", email.trim()).put("password", password)
        val result = JSONObject(request("/auth/v1/signup", "POST", auth.toString()))
        acceptSession(result)
        val user = result.optJSONObject("user") ?: result
        val id = user.optString("id").ifBlank { currentUserId ?: "" }
        if (id.isNotBlank() && isSignedIn) {
            val profile = JSONObject().put("id", id)
                .put("username", username.trim().lowercase())
                .put("display_name", displayName.trim())
            request("/rest/v1/profiles", "POST", profile.toString(), mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
        }
        if (isSignedIn) "Account created. You are signed in."
        else "Account request sent. Check your email to confirm, then sign in."
    }

    suspend fun signIn(email: String, password: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val result = JSONObject(request("/auth/v1/token?grant_type=password", "POST", body.toString()))
        acceptSession(result)
        if (isSignedIn) "Signed in successfully." else "Sign-in response received, but no session was returned."
    }

    private fun acceptSession(result: JSONObject) {
        accessToken = result.optString("access_token").takeIf { it.isNotBlank() }
        currentUserId = result.optJSONObject("user")?.optString("id")?.takeIf { it.isNotBlank() }
    }

    fun signOut() {
        val token = accessToken ?: return
        runCatching {
            val request = Request.Builder()
                .url(BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/logout")
                .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + token)
                .post(ByteArray(0).toRequestBody(null)).build()
            http.newCall(request).execute().close()
        }
        accessToken = null
        currentUserId = null
    }

    suspend fun loadPublicFeed(): List<FeedPost> = withContext(Dispatchers.IO) {
        val json = JSONArray(request("/rest/v1/posts?select=id,user_id,video_path,caption,created_at&order=created_at.desc&limit=100"))
        (0 until json.length()).map { i ->
            val item = json.getJSONObject(i)
            FeedPost(item.getString("id"), item.getString("user_id"), item.getString("video_path"),
                item.optString("caption"), item.optString("created_at"))
        }
    }

    suspend fun saveProfile(username: String, displayName: String, bio: String) = withContext(Dispatchers.IO) {
        val id = currentUserId ?: error("Sign in before editing your online profile.")
        val body = JSONObject().put("id", id).put("username", username.trim().lowercase())
            .put("display_name", displayName.trim()).put("bio", bio.take(160))
        request("/rest/v1/profiles", "POST", body.toString(), mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
    }

    suspend fun uploadVideo(context: Context, uri: Uri, caption: String): FeedPost = withContext(Dispatchers.IO) {
        val id = currentUserId ?: error("Sign in before uploading a video.")
        val mime = context.contentResolver.getType(uri) ?: "video/mp4"
        require(mime.startsWith("video/")) { "Choose a video file." }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the selected video.")
        require(bytes.size <= 100 * 1024 * 1024) { "Videos must be 100 MB or smaller." }
        val ext = when (mime) {
            "video/webm" -> "webm"
            "video/quicktime" -> "mov"
            "video/3gpp" -> "3gp"
            else -> "mp4"
        }
        val objectPath = id + "/" + UUID.randomUUID().toString() + "." + ext
        val uploadRequest = Request.Builder()
            .url(BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/friendtok-videos/" + objectPath)
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Authorization", "Bearer " + (accessToken ?: error("Sign in before uploading.")))
            .header("Content-Type", mime)
            .post(bytes.toRequestBody(mime.toMediaType()))
            .build()
        http.newCall(uploadRequest).execute().use { response ->
            if (!response.isSuccessful) error("Video upload failed (${response.code}).")
        }
        val post = JSONObject().put("user_id", id).put("video_path", objectPath).put("caption", caption.take(500))
        val created = JSONArray(request("/rest/v1/posts?select=id,user_id,video_path,caption,created_at", "POST", post.toString(),
            mapOf("Prefer" to "return=representation"))).getJSONObject(0)
        FeedPost(created.getString("id"), created.getString("user_id"), created.getString("video_path"),
            created.optString("caption"), created.optString("created_at"))
    }

    fun publicVideoUrl(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/friendtok-videos/" + path
    }

    suspend fun setLike(postId: String, liked: Boolean) = withContext(Dispatchers.IO) {
        val id = currentUserId ?: error("Sign in to like videos.")
        if (liked) {
            val body = JSONObject().put("post_id", postId).put("user_id", id)
            request("/rest/v1/likes", "POST", body.toString(), mapOf("Prefer" to "resolution=ignore-duplicates,return=minimal"))
        } else {
            request("/rest/v1/likes?post_id=eq.$postId&user_id=eq.$id", "DELETE")
        }
    }

    suspend fun addComment(postId: String, body: String) = withContext(Dispatchers.IO) {
        val id = currentUserId ?: error("Sign in to comment.")
        require(body.isNotBlank() && body.length <= 500) { "Comments must be 1–500 characters." }
        val payload = JSONObject().put("post_id", postId).put("user_id", id).put("body", body.trim())
        request("/rest/v1/comments", "POST", payload.toString(), mapOf("Prefer" to "return=minimal"))
    }
}
