# Streamflix Changes

## Carousel playlist view
Playlist movies now display as a horizontal carousel row (instead of 3-column grid).

### Files changed:
- `res/layout/item_library_carousel.xml` — **new**: poster fills height (2:3 ratio), title overlaid at bottom
- `adapters/LibraryAdapter.kt` — added `carouselMode` flag + `CarouselViewHolder` view type
- `fragments/library/LibraryMobileFragment.kt` — `updateLayoutForSelection()` switches between horizontal carousel (`LinearSnapHelper`, peek padding, vertical shrink) and 3-column grid; drag-and-drop disabled in carousel mode
- `res/layout/fragment_library_mobile.xml` — removed catalog toggle button (top-right grid icon)

### Removed:
- Catalog mode (`PlaylistCatalogAdapter`, `catalogMode`, `catalogAdapter`, `toggleCatalogMode()`, `setupCatalog()`, `updateCatalogMode()`)

## Download progress bar in Library
Downloading items in the Downloads carousel now show a horizontal progress bar.

### Files changed:
- `res/layout/item_library_carousel.xml` — added `<ProgressBar style="?horizontal" />` pinned above the title, tinted hotpink, visibility toggled by the adapter
- `adapters/LibraryAdapter.kt` — `CarouselViewHolder.bind()` queries `DownloadUtil.downloadManager.currentDownloads` and shows the progress bar with `percentDownloaded` when state is `DOWNLOADING`

## Download auto-selects best quality
Download now picks the first (best) server automatically instead of showing a quality picker dialog.

### Files changed:
- `adapters/viewholders/MovieViewHolder.kt` — removed `AlertDialog` quality picker, auto-selects `servers.first()`
- `ui/ShowOptionsMobileDialog.kt` — same auto-select logic for the long-press menu download path

## Download notification
Foreground notification now shows download title and progress bar.

### Files changed:
- `app/build.gradle` — added `androidx.media3:media3-database:1.8.0` for `StandaloneDatabaseProvider`
- `utils/DownloadUtil.kt` — added `downloadTitles` (ConcurrentHashMap<String, String>) mapping contentId → movie title, populated in `startDownload()`; `buildForegroundNotification()` now passes the movie title as `contentTitle`; fixed `SimpleCache` + `DefaultDownloadIndex` to use `StandaloneDatabaseProvider`; fixed method name `addDownload()`; `startDownload()` now takes `Context` and starts `VideoDownloadService` via `startForegroundService()` so the download actually runs
- `service/VideoDownloadService.kt` — changed from `FOREGROUND_NOTIFICATION_ID_NONE` to `DownloadUtil.NOTIFICATION_ID` so `startForeground()` is called with the progress notification
- `activities/main/MainMobileActivity.kt` — requests `POST_NOTIFICATIONS` permission on Android 13+ at startup (one-time) via `ActivityResultContracts.RequestPermission`
- `utils/DownloadUtil.kt` — notification channel importance raised `LOW`→`DEFAULT`, lock screen visibility set to `PUBLIC`, added `openNotificationSettings()` helper to open system notification settings for the app

## Download headers (Referer/Origin) + MIME type fallback
Download pipeline now passes `video.headers` (Referer, Origin, etc.) to HTTP requests, and defaults to `application/x-mpegURL` when `video.type` is null but URL ends in `.m3u8`.

### Files changed:
- `utils/DownloadUtil.kt` — added `downloadUrls` and `downloadHeaders` maps; `HeaderInjectingDataSourceFactory` wraps `OkHttpDataSource.Factory` and returns `HeaderInjectingHttpDataSource` (Kotlin `by` delegation) that injects headers before `open()`; `startDownload()` now accepts `headers` param and stores it; mime type fallback for `.m3u8` → `application/x-mpegURL`; cleanup in `removeDownload()` and `release()`
- `adapters/viewholders/MovieViewHolder.kt` — passes `video.headers` to `DownloadUtil.startDownload()`
- `ui/ShowOptionsMobileDialog.kt` — passes `video.headers` to `DownloadUtil.startDownload()`
