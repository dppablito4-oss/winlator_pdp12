# Winlator Steam H200

Custom Winlator 11.2 build for the HONOR 200 with Snapdragon 7 Gen 3 and
Adreno 720.

## Steam launcher

The home screen is a lightweight Steam-only launcher. It creates a dedicated
`Steam H200` container with the device profile below, opens the bundled Steam
installer on first use, and launches `steam.exe` directly afterwards. The
regular containers, shortcuts, controls, and settings screens remain available
from the navigation drawer.

## Device profile

The profile is enabled when either an HONOR 200 model (`ELP-*`) or an Adreno
720 GPU is detected. Existing containers are not rewritten. New containers
start with:

- Turnip 26.2.0 and the hardware-buffer path.
- Adreno 720 GMEM rendering.
- DXVK 2.4.1 with a 30 FPS cap for sustained mobile play.
- 1280x720 output.
- 2 GB reported video memory on devices below 10 GB RAM, or 4 GB otherwise.
- CPU affinity `4,5,6,7`, targeting the four performance cores.
- `HONOR 200 Balanced`, a Box64 preset between Intermediate and Performance.
- Steam memory saving enabled by default.

All settings remain editable per container and per shortcut. If a game has
rendering errors, first change Box64 to Stability. If it is GPU-bound, try
960x544 before changing driver versions.

## Installation behavior

The application keeps the upstream `com.winlator` package ID because native
components and the bundled root filesystem use that path. Consequently, this
build can only replace an installed Winlator build signed with a compatible
key. Otherwise, Android requires uninstalling the existing package first.

Back up containers and save data before replacing another build.

## Build

Requirements: JDK 17 and an Android SDK with API 35, NDK 24.0.8215888, and
CMake 3.22.1.

On Windows:

```powershell
.\build-honor200.ps1
```

Or run Gradle directly:

```powershell
.\gradlew.bat clean assembleDebug
```

The APK is produced under `app/build/outputs/apk/debug/`.
