# AnimeX TV

Antarmuka Android TV 10-foot untuk AnimeX. Shell aplikasi langsung membuka `TvApp()`;
tampilan ponsel tidak dipakai, tetapi APK tetap bisa diinstal di ponsel untuk uji coba.

## Build

GitHub Actions (`build-debug.yml`) otomatis menjalankan `assembleDebug` pada push ke
`main` dan mengunggah APK debug sebagai artifact `animex-debug-apk`.

Build lokal membutuhkan Android SDK dengan:

- Platform Android 37
- Build Tools 36.0.0
- NDK 28.2.13676358
- CMake 3.22.1
- JDK 17 dan Gradle 9.6.0

```bash
gradle assembleDebug
```

Karena `gradle-wrapper.jar` tidak disertakan, workflow memakai Gradle yang dipasang
oleh `gradle/actions/setup-gradle`.

## Tidak ada animasi

Seluruh transisi Compose dinonaktifkan lewat `AppMotion` dan `MotionScheme.standard()`.
Latar juga statis (`BackgroundType.NONE`, `backgroundParallax = false`).
