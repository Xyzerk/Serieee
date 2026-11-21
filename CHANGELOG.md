# Changelog

All notable changes to this project are documented here. The in-app "What's New"
dialog is generated from the `changelog_*` strings in
`app/src/main/res/values/strings.xml`.

## [Unreleased]

### Added

- **Theme Studio**, a full-screen editor opened from the Custom theme: live
  preview, per-token colours (text, secondary text, cards, panels, borders,
  success, warning, danger, info, reminder, stars), a background photo with
  overlay, blur (Android 12+), and card-opacity controls, artwork-sampled
  accents, and shareable theme codes.
- Six selectable **Neo-Brutalism tones** (yellow, orange, pink, purple, blue,
  mint). The choice is persisted and applied to the home-screen widget too.
- First-run **welcome slides** (skippable, shown once), with notification
  permission prompts deferred until onboarding finishes.
- **Report Bug** in the overflow menu, which zips the in-app logs and device
  details and opens the share sheet without including library data.
- Add/edit form: episode lists start collapsed per season, and pressing back
  twice within two seconds warns before discarding a new title.
- Hidden alternate titles (romaji, native, synonyms, original title) are stored
  when metadata is fetched from AniList/TMDB and used to recognise episode files
  whose name carries a translated title. Release-group names such as
  `CHFY--01.mkv` or `[Group] Show - 07 (1080p)` now resolve by their last safe
  number, and unnumbered files fall back to natural-sorted order.

### Changed

- The theme entry moved from the overflow menu to a palette button in the top
  bar; **Custom** now opens Theme Studio instead of an in-menu colour page.
- Theme codes now also carry per-token colours, image tuning, and the Neo tone.
  The background image file itself stays device-local and is never shared.
- The top-bar fade over a custom background image uses the readability scrim
  instead of a solid band, and the palette honours the new colour overrides.

### Fixed

- Newly fetched or manually added series start at episode 0, so episode 1 is no
  longer counted in Insights before it is watched and the first `+` records
  episode 1 in history instead of jumping to episode 2.
- The episode list's **Missing only** chip stays available after the last gap is
  filled (or the episode count shrinks below the assigned episodes). It used to
  hide itself while the filter was still on, leaving an empty list with no way
  to switch back to all episodes.
- The six Neo-Brutalism tone swatches now have TalkBack names; unselected tones
  are no longer announced as unnamed radio buttons.
- Theme Studio's accent and gradient swatches are named the same way, so TalkBack
  announces "Blue", "Pink to violet", and so on instead of an unnamed radio button.
- Editing any appearance setting now refreshes the home-screen widget right away
  instead of waiting for the next library change or the midnight alarm.
- Importing a MyAnimeList export with a malformed numeric entity no longer
  crashes with an invalid code point; the entity is left as written instead.
- Posters picked from a document provider get a validated image extension, so an
  unexpected MIME type or file name can no longer shape the stored file name.
- Imported entries without a source id get a stable positive id even when the
  title hash is `Int.MIN_VALUE`.

## [0.5.0] — 2026-11-16

### Added

- **Insights**, a third pager tab (Progress → Library → Insights) with a
  dedicated nav item, swipe support, and TalkBack next/previous clamping.
- Hero summary: episodes watched, estimated watch time (24 minutes per
  episode), completion rate, and completed-title progress.
- Series/movie/rating quick tiles, a stacked status breakdown with legend,
  5-star distribution bars, and a top-genres chip cloud.
- Accessible 12-week activity heatmap (date + count per cell, today outline)
  and a recent watch timeline with posters, relative time, and detail-sheet
  navigation.
- `WatchEvent` history model with a 500-event cap, 365-day retention, exact
  de-duplication, and a clear-history action that leaves library, progress,
  and streak untouched.
- Backup format **version 4** with watch history: replace restores the file's
  history (or clears it), merge unions and de-duplicates, and v1–v3 backups
  still import.
- Pure-JVM tests for history trimming and every Insights metric.

### Changed

- Episode `+` and manual episode jumps now log the watched season/episode;
  manual edits, collection imports, and backup-merge backfills do not.
- Library and Progress filters are compact dropdowns backed by one shared
  status list, with counts kept in the menu and long labels ellipsized. The
  top-bar search shortcut was removed, and the calendar quick action now
  selects **Coming Soon** directly.
- `versionCode` is **15** (14 was already used by 0.4.2).

### Fixed

- History survives renaming or deleting a title through a stored title/type
  snapshot, so the timeline never develops holes.

## [0.4.2] — 2026-11-15

### Added

- **Material You** style that follows the wallpaper palette on Android 12 and
  newer, with Classic colours as the fallback on older devices.
- Custom theme upgrades: two-stop gradient presets, an accent sampled from a
  poster's artwork, and copy/paste **theme codes** for sharing a setup.
- **Reduce transparency** switch (collapses glass surfaces to solid ones) and an
  optional **AMOLED in battery saver** mode.
- Appearance × style theme menu with live swatches, a WCAG contrast warning for
  custom accents, and a full in-menu preview.
- Vibrant status tokens (`info`, `reminder`, `star`) and a complete Material 3
  colour scheme so stock dialogs, fields, and switches never fall back to
  purple.

### Changed

- Themes are now two independent axes: appearance (Follow system / Light / Dark)
  and style (Classic / AMOLED / Frost Glass / Liquid Glass / Material You /
  Custom). Every style ships both a light and a dark variant; the previous flat
  theme list is migrated automatically on upgrade.
- Palette colours cross-fade when the theme changes, and Liquid Glass drifts its
  background gradient.
- Glass surfaces use specular gradient borders and translucent chrome instead of
  flat outlines.
- Built-in styles use the same colour for accent and primary, so the wordmark,
  badges, and selection tints stay in sync.
- Screens, dialogs, chips, and buttons now draw from palette tokens instead of
  hardcoded brand colours.

### Fixed

- Light themes no longer flash the dark window background during startup.
- Text and icons on filled buttons, chips, badges, and switches stay readable on
  light accents.

## [0.4.0] — 2026-11-12

### Added

- Seven themes: Follow system, Dark, AMOLED, Frost Glass, Liquid Glass, Light,
  and Custom (eight accent presets plus a solid or gradient background).
- Two-level theme menu with live previews, descriptions, radio semantics, and
  back-navigation through the page stack.
- **On Hold** and **Dropped** as real statuses, with colours, filters, and
  import mapping (`PAUSED` / `On-Hold` and `DROPPED`).
- Horizontal swipe between Progress and Library, with a floating navigation pill
  that follows the drag, haptic settle, and TalkBack next/previous actions.
- Collection import from a public **AniList** username or a **MyAnimeList**
  `.xml` / `.xml.gz` export, with a preview and a merge/replace choice.
- Non-destructive merge: progress and totals only grow, statuses only upgrade,
  and local-only fields survive. Replace requires explicit confirmation.
- Pure-JVM tests for import mapping, MAL parsing (including gzip), merge
  behaviour, and enum/status persistence.
- System default as a third language option.

### Changed

- Detail, add/edit, backup/restore, and import screens are now bottom sheets
  with drag handles, IME padding, and independently scrolling content.
- Redesigned header with the launcher mark, gradient wordmark, and search /
  schedule quick actions.
- Filled search pill, gradient progress bars, status-coloured badges, richer
  empty states, and a press-scale FAB that hides while scrolling.
- Filter chips wrap with `FlowRow`, so horizontal pager swipes are no longer
  swallowed by horizontal chip rows.
- Metadata and import errors, empty states, and theme descriptions are localised
  in English and Indonesian.

### Fixed

- Imported entries are matched by source ID first, then by normalized title and
  type, so re-importing the same list is idempotent.
- Movies imported from either source are normalised to one watched/unwatched
  unit instead of an episode count.

## [0.3.0] — 2026-11-11

### Added

- Apache-2.0 license, `NOTICE`, and contributor/security/privacy documentation.
- In-app **About** screen with version information, data-source credits, privacy
  summary, and links to the license and third-party notices.
- TMDB, JustWatch, AniList, TVmaze, and OMDb attribution.
- Unit tests for season parsing and episode filename parsing.
- CI workflows for lint, unit tests, dependency review, and CodeQL.
- Dependabot configuration and issue/PR templates.

### Changed

- Application ID is now `com.seriee.app` (previously the AI Studio default
  `com.aistudio.seriee.twvpmz`). **Android treats this as a new app** — export a
  JSON backup before installing.
- `compileSdk` / `targetSdk` 36 (Android 16); AGP 8.13.2.
- Release builds are minified and resource-shrunk with R8.
- Releases now publish a `SHA256SUMS.txt` asset and the signing certificate
  fingerprint.
- API keys moved to an isolated preferences file and excluded from Android
  backup and device transfer.
- Cleartext HTTP is blocked app-wide.

### Removed

- The public `debug-latest` APK channel. Debug builds are CI artifacts for
  contributors only; end users should install signed releases.

### Fixed

- `gradlew` is now executable in the repository.
- README build instructions match the Gradle wrapper and CI.

## [0.2.7] — 2026-11-06

- OMDb as an optional metadata source.
- README refresh, overflow-menu cleanup.

## [0.2.6] — 2026-11-05

- Daily streak tracking.
- TVmaze next-episode schedule fallback.

## [0.2.5] — 2026-11-04

- Library UI polish, movie labels, duplicate guard.

## [0.2.4] and earlier

- Initial progress tracking, multi-season support, backups, reminders, themes,
  localization, and metadata search (AniList, TVmaze, TMDB).
