# FriendTok

FriendTok is a native Android short-video social app built with Kotlin and Jetpack Compose.

## Online backend

The FriendTok Supabase project is configured at `https://drtyxivcbuuswbapgsmu.supabase.co`.

- Email/password account creation and sign-in via Supabase Auth
- Public feed backed by the `posts` table
- Profile records in `profiles`
- Likes in `likes` and comments in `comments`
- Public video bucket named `friendtok-videos`
- Row Level Security enabled on all four public tables
- Users can edit their own profile and posts; authenticated users can add/remove their own likes and add their own comments
- Public video reading; uploads and deletes restricted to the signed-in user's own storage folder
- Android uses the project's publishable key only. Never put a Supabase service-role or secret key in the app or this public repository.

## Implemented app features

- Home feed loads public post records from Supabase
- Email/password account UI
- Profile creation/editing
- Record video through the phone's camera app (60-second capture limit requested)
- Pick a video from the gallery and upload it to Supabase Storage when signed in (100 MB maximum)
- Public video playback through an installed video player
- Caption search, likes, and comments connected to Supabase endpoints
- Dark/light theme switch
- Android secure-window flag to help block screenshots and supported screen recordings
- GitHub Actions workflow intended to build a debug APK

## Current limitations

- This is an early prototype and has not yet been verified by a successful Android build or end-to-end device test.
- The feed currently displays a shortened user ID rather than resolving every username from the profiles table.
- Some camera apps do not return a video URI; if that happens, choose the saved video from the gallery to upload it.
- Likes/comments are connected to database writes, but the full social UX (counts, persistent liked-state display, threaded/realtime updates, and polished error states) still needs testing and refinement.
- No reporting, blocking, moderation, or other safety/moderation tools are included, as requested.
- Public videos can be viewed by anyone with access to the public feed. People may copy or record public content.
- Account email confirmation behavior depends on the Supabase Auth settings.

## Build locally

Requirements: JDK 17 and Gradle 8.9 or compatible Android Studio.

1. Clone this repository.
2. Open it in Android Studio, or run from the project root:

   ```bash
   gradle assembleDebug
   ```

3. The debug APK should be at `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow uploads a debug APK as a workflow artifact if the build succeeds. The APK is not a signed release APK.

## Privacy notes

Android's secure-window flag can help block screenshots and supported screen recordings while the protected app window is visible. It cannot stop someone filming the display with another device, and it is not a guarantee across every device or OS version.

Never commit signing keys, passwords, Supabase service-role keys, or other secrets.

## License

No license has been selected yet. Until one is added, do not assume others have permission to reuse this code.
