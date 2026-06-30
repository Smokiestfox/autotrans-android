# AutoTrans Android — Sequence Diagrams

> **Version**: 1.0 | **Last updated**: 2026-06-29
> **Prerequisite**: Read [ARCHITECTURE.md](ARCHITECTURE.md) and [PIPELINE.md](PIPELINE.md) first.
> All actors below map directly to classes described in ARCHITECTURE.md §7.

---

## Table of Contents

1. [App Startup Flow](#1-app-startup-flow)
2. [Permission Request Flow](#2-permission-request-flow)
3. [Single-Shot Translation Flow](#3-single-shot-translation-flow)
4. [Continuous Translation Flow (Overlay Mode)](#4-continuous-translation-flow-overlay-mode)
5. [Overlay Update Flow](#5-overlay-update-flow)
6. [Settings Update Flow](#6-settings-update-flow)
7. [Translation Engine Switch Flow](#7-translation-engine-switch-flow)
8. [Language Model Download Flow](#8-language-model-download-flow)
9. [Error Recovery Flow](#9-error-recovery-flow)
10. [Service Destruction Flow](#10-service-destruction-flow)

---

## 1. App Startup Flow

Cold start from launcher icon through to the main UI being ready.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant OS as Android OS
    participant App as Application\n(HiltAndroidApp)
    participant MA as MainActivity
    participant VM as MainViewModel
    participant GSU as GetSettingsUseCase
    participant SR as SettingsRepository\n(DataStore)

    User->>OS: tap launcher icon
    OS->>App: onCreate()
    Note over App: Hilt DI graph initialized<br/>All singletons constructed

    App->>MA: startActivity()
    MA->>VM: created by Hilt ViewModelFactory

    VM->>GSU: invoke()
    GSU->>SR: settings (Flow)
    SR-->>GSU: AppSettings (first emission)
    GSU-->>VM: Flow<AppSettings>

    VM->>VM: _uiState = UiState.Ready(settings)
    VM-->>MA: uiState (StateFlow)
    MA-->>User: Main screen rendered

    alt Overlay permission already granted
        MA->>MA: show "Start Overlay" button enabled
    else Overlay permission not granted
        MA->>MA: show "Grant Permission" CTA
    end
```

---

## 2. Permission Request Flow

Two permissions are required before the pipeline can run:

- `SYSTEM_ALERT_WINDOW` — draw overlay over other apps
- `MediaProjection` consent — screen capture

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant MA as MainActivity
    participant VM as MainViewModel
    participant PM as PermissionManager
    participant OS as Android OS
    participant MPM as MediaProjection\nManager

    User->>MA: tap "Start Overlay"
    MA->>VM: onStartOverlayClicked()
    VM->>PM: checkAllPermissions()

    PM->>OS: Settings.canDrawOverlays(context)

    alt SYSTEM_ALERT_WINDOW not granted
        PM-->>VM: MissingPermission.Overlay
        VM-->>MA: UiEvent.RequestOverlayPermission
        MA->>OS: Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        User->>OS: grants permission in Settings
        OS->>MA: onActivityResult()
        MA->>VM: onOverlayPermissionResult(granted=true)
    end

    PM->>OS: mediaProjectionManager.createScreenCaptureIntent()
    MA->>OS: startActivityForResult(captureIntent)
    User->>OS: taps "Start now" in system dialog

    OS-->>MA: onActivityResult(resultCode, data)
    MA->>VM: onMediaProjectionResult(resultCode, data)
    VM->>PM: storeMediaProjectionToken(resultCode, data)

    VM-->>MA: UiEvent.AllPermissionsGranted
    MA->>MA: start OverlayForegroundService
```

---

## 3. Single-Shot Translation Flow

User manually triggers a one-time translation without enabling the overlay.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant MA as MainActivity
    participant VM as MainViewModel
    participant TSU as TranslateScreenUseCase
    participant CR as CaptureRepository
    participant IS as ImageStore
    participant OR as OcrRepository
    participant OE as MlKitOcrEngine
    participant TR as TranslationRepository
    participant TC as TranslationCache
    participant TE as MlKitTranslationEngine

    User->>MA: tap "Translate Now"
    MA->>VM: onTranslateClicked()
    VM->>VM: _uiState = UiState.Loading

    VM->>TSU: invoke(languagePair)

    TSU->>CR: captureScreen()
    CR->>IS: registerBitmap(uuid, bitmap)
    IS-->>CR: stored
    CR-->>TSU: Result.success(ImageData(uuid))

    TSU->>OR: recognizeText(ImageData(uuid))
    OR->>IS: resolve(uuid) → Bitmap
    OR->>OE: recognize(bitmap)
    OE-->>OR: TextRecognitionResult
    OR->>OR: map Rect→BoundingBox, build OcrResult
    OR-->>TSU: Result.success(OcrResult)

    TSU->>TSU: postProcess(ocrResult) → filtered

    TSU->>TR: translate(text, from, to)
    TR->>TC: get(text, from, to)

    alt Cache HIT
        TC-->>TR: TranslationResult
        TR-->>TSU: Result.success(cached)
    else Cache MISS
        TC-->>TR: null
        TR->>TE: translate(TranslationRequest)
        TE-->>TR: TranslationResult
        TR->>TC: put(text, from, to, result)
        TR-->>TSU: Result.success(result)
    end

    TSU-->>VM: Result.success(TranslationResult)
    VM->>VM: _uiState = UiState.Success(result)
    VM-->>MA: uiState updated
    MA-->>User: result shown in UI panel
```

---

## 4. Continuous Translation Flow (Overlay Mode)

User enables auto-translate. The service starts and the pipeline runs until stopped.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant MA as MainActivity
    participant OFS as OverlayForeground\nService
    participant PP as TranslationPipeline\nImpl
    participant CR as CaptureRepository
    participant OR as OcrRepository
    participant TR as TranslationRepository
    participant OWM as OverlayWindow\nManager

    User->>MA: toggle "Auto Translate" ON
    MA->>OFS: startForegroundService(Intent)

    OFS->>OFS: onCreate()<br/>create serviceScope<br/>show notification

    OFS->>PP: start(serviceScope)
    PP->>CR: startContinuousCapture(intervalMs)

    loop Every intervalMs (e.g. 1000ms)
        CR-->>PP: emit Result<ImageData>

        Note over PP: .conflate() — drop if busy
        Note over PP: .mapLatest — cancel prev if new frame

        PP->>OR: recognizeText(imageData)
        OR-->>PP: Result<OcrResult>

        PP->>PP: postProcess() → filtered

        alt Text unchanged (distinctUntilChanged)
            PP->>PP: skip — no overlay update
        else Text changed
            PP->>TR: translate(text, from, to)
            TR-->>PP: Result<TranslationResult>
            PP->>PP: _state = PipelineState.Success
            PP-->>OWM: state collected via StateFlow
            OWM->>OWM: update ComposeView content
            OWM-->>User: overlay updated
        end
    end

    User->>MA: toggle "Auto Translate" OFF
    MA->>OFS: stopService(Intent)
    OFS->>PP: stop()
    OFS->>OFS: onDestroy()<br/>serviceScope.cancel()<br/>remove overlay window
```

---

## 5. Overlay Update Flow

Zooms in on how a `TranslationResult` becomes visible pixels on screen.

```mermaid
sequenceDiagram
    autonumber
    participant PP as TranslationPipeline
    participant OFS as OverlayForeground\nService
    participant OWM as OverlayWindow\nManager
    participant CV as ComposeView\n(in WindowManager)
    participant OS as WindowManager\n(Android OS)
    participant Screen as User Screen

    PP->>PP: _state.value = PipelineState.Success(result)

    OFS->>OWM: collects pipelineState (StateFlow)
    OWM->>OWM: buildOverlayContent(result, screenSize)
    Note over OWM: BoundingBox (0..1) →<br/>pixel Rect for each block

    OWM->>OWM: _overlayContent.value = OverlayContent(blocks)

    CV->>CV: recompose (observes overlayContent StateFlow)
    Note over CV: key(block.id) ensures only<br/>changed blocks recompose

    CV->>OS: requestLayout() / invalidate()
    OS->>Screen: composite overlay window on top
    Screen-->>User: translated text visible over original
```

---

## 6. Settings Update Flow

User changes a setting (e.g., target language from English to Japanese).

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SUI as SettingsScreen\n(Compose)
    participant SVM as SettingsViewModel
    participant USU as UpdateSettingsUseCase
    participant SR as SettingsRepository\n(DataStore)
    participant PP as TranslationPipeline
    participant TC as TranslationCache

    User->>SUI: select "Japanese" as target language
    SUI->>SVM: onTargetLanguageChanged(Language.Specific("ja"))

    SVM->>USU: invoke(updatedSettings)
    USU->>SR: updateSettings(AppSettings(...targetLanguage = ja))
    SR->>SR: DataStore.updateData { ... }
    SR-->>USU: Result.success(Unit)
    USU-->>SVM: Result.success(Unit)

    SR-->>PP: settings Flow emits new AppSettings
    Note over PP: pipeline picks up new<br/>targetLanguage on next frame

    PP->>TC: evict()
    Note over TC: old translations (en target)<br/>are no longer valid

    SVM-->>SUI: UiState updated
    SUI-->>User: language chip shows "JA"
```

---

## 7. Translation Engine Switch Flow

User switches from ML Kit to Google Cloud Translate in Settings.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SVM as SettingsViewModel
    participant USU as UpdateSettingsUseCase
    participant SR as SettingsRepository
    participant TEP as TranslationEngine\nProvider
    participant MLE as MlKitTranslation\nEngine
    participant GCE as GoogleCloudTranslation\nEngine

    User->>SVM: select "Google Cloud" engine
    SVM->>USU: invoke(settings.copy(translationEngine = GOOGLE_CLOUD))
    USU->>SR: updateSettings(...)

    SR-->>TEP: settings Flow emits new engine type

    TEP->>MLE: release()
    Note over MLE: cleanup ML Kit resources

    TEP->>GCE: initialize(EngineConfig(apiKey = storedKey))
    GCE-->>TEP: Result.success(Unit)

    TEP->>TEP: activeEngine = GCE

    Note over TEP: Next pipeline frame will<br/>use GoogleCloudTranslationEngine
```

---

## 8. Language Model Download Flow

ML Kit requires downloading language models before offline translation is available.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant SUI as SettingsScreen
    participant SVM as SettingsViewModel
    participant DLU as DownloadLanguage\nModelUseCase
    participant LR as LanguageRepository
    participant MLE as MlKitTranslation\nEngine
    participant OS as ML Kit / Network

    User->>SUI: tap "Download Japanese model"
    SUI->>SVM: onDownloadModel(Language.Specific("ja"))

    SVM->>DLU: invoke(language)
    DLU->>LR: downloadModel(language)
    LR->>MLE: downloadModelIfNeeded(language)

    MLE->>OS: TranslateRemoteModel download request
    OS-->>MLE: progress callbacks (0..100%)

    loop While downloading
        MLE-->>LR: emit DownloadProgress(percent)
        LR-->>DLU: Flow<DownloadProgress>
        DLU-->>SVM: Flow<DownloadProgress>
        SVM-->>SUI: UiState.Downloading(percent)
        SUI-->>User: progress bar updates
    end

    OS-->>MLE: download complete
    MLE-->>LR: emit DownloadProgress(100, done=true)
    LR-->>SVM: complete
    SVM-->>SUI: UiState.DownloadComplete
    SUI-->>User: "Japanese model ready ✓"
```

---

## 9. Error Recovery Flow

Illustrates how a translation timeout is handled and retried.

```mermaid
sequenceDiagram
    autonumber
    participant PP as TranslationPipeline
    participant TR as TranslationRepository
    participant TE as TranslationEngine
    participant OWM as OverlayWindow\nManager

    PP->>TR: translate(text, from, to)
    TR->>TE: translate(request) [attempt 1]

    TE-->>TR: timeout after 10s

    Note over TR: withRetry(times=3,<br/>initialDelay=1s, factor=2.0)

    TR->>TE: translate(request) [attempt 2, delay 1s]
    TE-->>TR: timeout after 10s

    TR->>TE: translate(request) [attempt 3, delay 2s]
    TE-->>TR: timeout after 10s

    TR-->>PP: Result.failure(TimeoutException)
    PP->>PP: _state = PipelineState.Error(TimeoutException)

    OWM->>OWM: collect PipelineState.Error
    OWM->>OWM: show error indicator on overlay
    OWM-->>User: overlay shows "⚠ Translation failed"

    Note over PP: Next frame captured after<br/>intervalMs — pipeline continues<br/>Error state is not terminal
```

---

## 10. Service Destruction Flow

Covers both user-initiated stop and system kill (low memory).

```mermaid
sequenceDiagram
    autonumber
    participant OS as Android OS
    participant OFS as OverlayForeground\nService
    participant PP as TranslationPipeline
    participant CR as CaptureRepository
    participant IS as ImageStore
    participant OWM as OverlayWindow\nManager
    participant WM as WindowManager\n(Android OS)

    alt User stops overlay
        OS->>OFS: stopService() from MainActivity
    else System kills service (low memory)
        OS->>OFS: onTaskRemoved() / LMK
    end

    OFS->>OFS: onDestroy()
    OFS->>PP: stop()
    PP->>PP: pipelineJob.cancel()

    PP->>CR: stopCapture() [triggered by scope cancel]
    CR->>CR: finally block in flow builder
    CR->>CR: virtualDisplay.release()
    CR->>CR: imageReader.close()
    CR->>IS: clear() — release all Bitmap references

    OFS->>OWM: destroy()
    OWM->>WM: removeView(composeView)
    OWM->>OWM: composeView.disposeComposition()

    OFS->>OFS: serviceScope.cancel("Service destroyed")
    Note over OFS: All remaining coroutines<br/>receive CancellationException<br/>and terminate

    OFS->>OS: stopForeground(STOP_FOREGROUND_REMOVE)
    Note over OS: Notification dismissed<br/>Service fully stopped
```

---

*For full state definitions and transition rules, see [STATE_MACHINE.md](STATE_MACHINE.md).*
*For error details and recovery strategies per failure mode, see [ERROR_HANDLING.md](../ERROR_HANDLING.md).*
