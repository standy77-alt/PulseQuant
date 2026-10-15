# PulseQuant — Android market radar & paper-trading lab

PulseQuant is an experimental Android market-monitoring and **paper-trading** app. It is not a broker and the project does not claim to place live exchange orders. Market data is fetched from the public BingX swap API; availability and API response formats can change.

## What changed in this revision

- Renamed the app to **PulseQuant** (`com.pulsequant.mobile`).
- Added `StrategyCore`, a deterministic signal rules module shared by the live paper-trading path and historical backtest. Strategy conditions and the Z-score trigger are now defined in one place.
- Shared stop-loss / take-profit bracket calculations and H2 risk-score calculation between the live paper-trading path and backtest. The backtest's parameter sweep still deliberately tests alternative stop multipliers.
- Removed a backtest inconsistency where strategies 2–4 used different entry rules than the live engine.
- Fixed a same-candle look-ahead issue in the trailing-stop simulation: a stop moved after favorable movement is applied only to subsequent candles. If stop and target are both touched in one candle, the simulator uses a conservative stop-first assumption.
- Added unit tests for core signal conditions and bracket calculations.
- GitHub Actions now runs unit tests before building and uploads a debug APK artifact.

## Important limitations

- This is still an experimental research tool. A shared signal function does not make tick-derived and candle-derived features identical; OHLC candles cannot reproduce trade-level order flow exactly.
- Backtest results are sensitive to candle granularity, fees, slippage, funding, and the stop/target ordering assumption. The current backtest is not evidence of future profitability.
- The app currently uses paper trading. Do not add exchange trading keys or assume live execution is supported.
- The public market-data endpoint can fail or change. Validate results and error handling before relying on the app.

## Build on GitHub (recommended)

1. Create a new empty repository on GitHub named `PulseQuant`.
2. Extract this ZIP on your phone or computer. Upload the **contents** of the `RhythmTrader-main` folder to the repository root (so `settings.gradle`, `build.gradle`, `app/`, `.github/`, and this README are at the root).
3. Commit the files to the `main` branch.
4. Open **Actions** in GitHub. The `Build PulseQuant APK` workflow should run automatically after the push. If Actions are disabled, enable them in the repository settings.
5. Open the completed workflow run, scroll to **Artifacts**, and download `PulseQuant-debug`. Extract the downloaded artifact ZIP to get `app-debug.apk`.
6. Transfer the APK to the Android device and install it. You may need to allow installation from the file manager/browser used to open it. This is a debug build for testing, not a Play Store release.

## Upload using Termux

Install Termux from a trusted source and run:

```sh
pkg update && pkg upgrade
pkg install git unzip
```

Extract the project ZIP to a directory, then:

```sh
cd ~/storage/downloads
unzip 'RhythmTrader-main (1).zip'
cd RhythmTrader-main
```

If the ZIP is elsewhere, adjust the path. To grant Termux access to shared storage first run `termux-setup-storage`.

Create a GitHub repository named `PulseQuant` in the browser. Then push the project (replace the placeholder with your repository URL):

```sh
git init
git add .
git commit -m "Initial PulseQuant revision"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/PulseQuant.git
git push -u origin main
```

GitHub may ask you to authenticate. Use a GitHub token or GitHub's supported authentication flow; your account password is not accepted for Git HTTPS pushes. Never commit tokens or API secrets into the repository.

## Build locally

The most reliable route on a phone is GitHub Actions. Building the Android app directly inside Termux can be difficult because the Android SDK and compatible build tools must also be installed. On a computer with Android SDK/JDK 17 and Gradle 8.4, run:

```sh
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
