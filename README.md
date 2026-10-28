# FriendTok

FriendTok is a native Android short-video app starter built with Kotlin and Jetpack Compose.

## Implemented in this starter

- Home feed for videos selected or recorded on this device
- Record video using the phone's camera app (60-second capture limit requested)
- Pick a video from the device gallery
- Open/play a selected video using an installed video player
- Local caption text, likes, comments, and caption/creator search
- Editable local display name, username, and bio
- Dark/light theme switch
- GitHub Actions workflow intended to build a debug APK
- Android secure-window flag to help block screenshots and supported screen recordings

## Important limits

You asked to leave out **the online backend and safety/moderation features**. This means this version is a local prototype, not a real multi-user social network:

- There is no sign-up/sign-in authentication.
- Profiles, videos, likes, and comments are held in app memory and are not synced to other phones. Current prototype data may reset when the app restarts.
- Videos are selected locally; there is no upload/storage service or public online feed.
- Discover searches only the local videos currently loaded in the app.
- No reporting, blocking, moderation, or other safety tools are included.
- The app uses Android's external camera app. Some camera apps do not return a video URI, in which case FriendTok cannot add that recording to its feed.
- Video previews are a placeholder; tapping them opens the selected clip in another video player.

## Build locally

Requirements: JDK 17 and Gradle 8.9 or compatible Android Studio.

1. Clone this repository.
2. Open it in Android Studio, or run from the project root:

   ```bash
   gradle assembleDebug
   ```

3. The debug APK should be at `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow uploads a debug APK as a workflow artifact when its build succeeds. The APK has not been verified as successfully built yet. It is not a signed release APK.

## Privacy notes

Android's secure-window flag can help block screenshots and supported screen recordings while the protected app window is visible. It cannot stop someone filming the display with another device, and it is not a guarantee across every device or OS version.

Never commit signing keys, passwords, or other secrets to this public repository.

## License

No license has been selected yet. Until one is added, do not assume others have permission to reuse this code.
