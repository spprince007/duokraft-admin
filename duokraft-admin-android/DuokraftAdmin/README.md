# Duokraft Admin (Android)

Kotlin + Jetpack Compose app for managing the Duokraft WordPress site.

## Phase 1 features
- **Login** through WordPress' own "Authorize application" page. The app receives a revocable
  application password, stored encrypted (Android Keystore). Logout revokes it on the website.
- **Dashboard**: counts, drafts, scheduled items, recent pages/posts/uploads and quick actions.
- **Pages & Posts** (and any custom post type the site adds later): search, status filters,
  paging, pull-to-refresh, create, edit, HTML editor with preview, featured image, categories/tags,
  draft / publish / update / schedule, move to trash, unsaved-changes warning.
  Elementor-built pages show a notice instead of a content editor (their layout lives in Elementor).
- **Media Library**: grid, search, multi-upload with automatic resizing (max 2048 px, JPEG 85 %),
  details (title, alt text, caption), copy URL, open, delete.

## Build
1. Open this folder in Android Studio (Ladybug or newer). It will offer to create the Gradle
   wrapper; accept, or run `gradle wrapper --gradle-version 8.11.1` once.
2. The website address is set in `app/build.gradle.kts` (`SITE_URL`). It must be HTTPS.
3. Run on a device or emulator (Android 8.0+).

## Website requirements
- The **Duokraft App API** plugin is installed and active on the site.
- Application Passwords are enabled (default in WordPress on HTTPS sites). Security plugins or
  hosts sometimes disable them, and some staging servers add their own password prompt; both block
  the app's login.

## Architecture
UI (Compose screens) → ViewModel → Repository → `WpApi` (Retrofit) → WordPress REST API
(`wp/v2`) + Duokraft App API plugin (`duokraft-app/v1`).
`data/AppContainer.kt` wires everything together.
