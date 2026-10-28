# FriendTok

FriendTok is a native Android short-video social app built with Kotlin and Jetpack Compose.

## Current status

This repository starts with a buildable Android UI scaffold. It is **not yet a complete social network**: Supabase authentication, database policies, video storage/uploads, camera capture, likes, comments, reporting, and moderation still need implementation and testing.

## Planned features

- Android-only app; no website
- Email/password registration and login, with optional Google sign-in
- No Facebook sign-in
- Public profiles and publicly viewable videos
- Videos up to 60 seconds, selected from the gallery or recorded in-app
- Likes and comments; no sharing feature or share button
- Dark and light themes
- Android screen-capture protection using `FLAG_SECURE`
- Report/block controls and account/content deletion

## Build locally

Requirements: JDK 17 and Gradle 8.9 or compatible Android Studio.

1. Clone this repository.
2. Open it in Android Studio, or run from the project root:

   ```bash
   gradle assembleDebug
   ```

3. The debug APK should be at `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow builds the debug APK and uploads it as a workflow artifact. It does not publish a signed release APK.

## Backend setup (planned)

The app does not connect to a backend yet. Before public use, configure a Supabase project and implement:
- Auth providers (email/password and optional Google only)
- Database tables and Row Level Security policies for profiles, posts, likes, comments, reports, and blocks
- Storage rules that match the decision to make videos public
- Upload size/duration validation and moderation controls

Never commit Supabase service-role keys, Android signing keys, passwords, or other secrets. A public repository means anyone can read and copy the source.

## Privacy and capture limitations

Android's secure-window flag helps block screenshots and supported screen recordings while protected app screens are visible. It cannot prevent someone from filming the display with another device or guarantee protection on every device/OS version. Public videos may be copied by people who can view them, even without an in-app sharing button.

## License

No license has been selected yet. Until one is added, do not assume others have permission to reuse this code.
