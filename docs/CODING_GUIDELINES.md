# AutoTrans Android — Coding Guidelines

> **Version**: 1.0 | **Last updated**: 2026-06-29
> These rules apply to all code in every module of the project.
> Automated enforcement: **ktlint** (formatting) + **detekt** (static analysis).
> When a rule conflicts with a library's convention, the library's convention wins.

---

## Table of Contents

1. [Package Structure](#1-package-structure)
2. [Naming Conventions](#2-naming-conventions)
3. [Kotlin Style](#3-kotlin-style)
4. [Coroutine & Flow Rules](#4-coroutine--flow-rules)
5. [Dependency Injection Rules](#5-dependency-injection-rules)
6. [Compose Conventions](#6-compose-conventions)
7. [Error Handling Rules](#7-error-handling-rules)
8. [Logging Rules](#8-logging-rules)
9. [Architecture Rules](#9-architecture-rules)
10. [Branch Naming](#10-branch-naming)
11. [Commit Message](#11-commit-message)
12. [Pull Request Checklist](#12-pull-request-checklist)

---

## 1. Package Structure

Every module follows the same internal package layout. The root package is `com.autotrans.android`.

```
com.autotrans.android.
│
├── [module]/               ← module name (capture, ocr, translator, overlay, …)
│   ├── di/                 ← Hilt modules (@Module, @InstallIn)
│   ├── data/               ← Repository implementations, DTOs, mappers
│   │   ├── local/          ← Room entities, DAOs
│   │   └── remote/         ← API services, DTOs
│   ├── engine/             ← OcrEngine / TranslationEngine implementations
│   ├── service/            ← Android Services
│   ├── ui/                 ← Compose screens, ViewModels
│   │   ├── screen/         ← Top-level screen composables
│   │   ├── component/      ← Reusable composables within this feature
│   │   └── viewmodel/      ← ViewModels
│   └── util/               ← Feature-local helpers (not for export)
│
└── core/
    ├── common/             ← Extensions, constants, RetryPolicy
    ├── ui/                 ← Theme, shared components, icons
    └── testing/            ← Fakes, builders, test rules
```

### Rules

- **One class per file** — no exceptions. File name must match class name exactly.
- **No `util/` package at project root** — utilities belong to `:core:common` or the feature module.
- **`di/` package** — only Hilt `@Module` classes live here. No business logic.
- **`data/` package** — no ViewModel, no UseCase. Only data access objects.

---

## 2. Naming Conventions

### Files & Classes

| Element | Convention | Example |
|---------|-----------|---------|
| Class | `PascalCase` | `TranslationRepositoryImpl` |
| Interface | `PascalCase` (no `I` prefix) | `TranslationRepository` |
| Sealed class/interface | `PascalCase` | `PipelineState` |
| Enum | `PascalCase`, entries `SCREAMING_SNAKE` | `enum class OcrEngineType { ML_KIT }` |
| Data class | `PascalCase` | `TranslationResult` |
| Object | `PascalCase` | `TranslationResultBuilder` |
| Extension file | `[ReceiverType]Extensions.kt` | `BitmapExtensions.kt` |
| Fake class | `Fake[InterfaceName]` | `FakeCaptureRepository` |
| Test class | `[SubjectClass]Test` | `TranslationPipelineImplTest` |
| Hilt module | `[Feature]Module` | `TranslatorModule` |

### Functions

| Element | Convention | Example |
|---------|-----------|---------|
| Regular function | `camelCase` | `recognizeText()` |
| Suspend function | `camelCase` (no suffix) | `translate()` — not `translateAsync()` |
| Extension function | `camelCase` | `Bitmap.downsample()` |
| Use Case `invoke` | operator fun `invoke` | `operator fun invoke(…)` |
| Composable | `PascalCase` | `TranslationBlock()` |

### Properties & Variables

| Element | Convention | Example |
|---------|-----------|---------|
| `val` / `var` | `camelCase` | `translatedText` |
| `StateFlow` backing prop | `_camelCase` (private mutable) | `_uiState` |
| `StateFlow` public prop | `camelCase` | `uiState` |
| Constant (`const val`) | `SCREAMING_SNAKE_CASE` | `MAX_CACHE_ENTRIES` |
| Top-level constant | `SCREAMING_SNAKE_CASE` in `object Constants` | — |
| Boolean property | starts with `is`, `has`, `should`, `can` | `isCapturing`, `hasError` |

```kotlin
// ✅ Correct
private val _uiState = MutableStateFlow<AppUiState>(AppUiState.Loading)
val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

// ❌ Wrong
private var uiState = MutableStateFlow<AppUiState>(AppUiState.Loading)
val publicUiState: StateFlow<AppUiState> get() = uiState
```

### Android-specific

| Element | Convention | Example |
|---------|-----------|---------|
| Resource IDs | `snake_case` | `@string/translate_button_label` |
| Compose `testTag` | `snake_case` | `"translate_button"` |
| ViewModel | `[Feature]ViewModel` | `SettingsViewModel` |
| Activity | `[Feature]Activity` | `MainActivity` |
| Service | `[Feature]Service` | `OverlayForegroundService` |
| Hilt entry point | `[Class]EntryPoint` | `OverlayServiceEntryPoint` |

---

## 3. Kotlin Style

### Prefer expressions over statements

```kotlin
// ✅ Expression — concise and readable
val result = if (cache.has(key)) cache.get(key) else engine.translate(request)

// ❌ Statement — unnecessary verbosity
val result: TranslationResult?
if (cache.has(key)) {
    result = cache.get(key)
} else {
    result = engine.translate(request)
}
```

### Use `when` for sealed types — always exhaustive

```kotlin
// ✅ Exhaustive — compiler enforces all branches
fun handleState(state: PipelineState) = when (state) {
    is PipelineState.Idle       -> hideOverlay()
    is PipelineState.Capturing  -> showLoadingIndicator()
    is PipelineState.Processing -> showStageIndicator(state.stage)
    is PipelineState.Success    -> showTranslation(state.result)
    is PipelineState.Error      -> showError(state.cause)
}

// ❌ Incomplete — misses future states silently
when (state) {
    is PipelineState.Success -> showTranslation(state.result)
    else -> { }    // dangerous — hides unhandled new states
}
```

### Avoid nullable types when unnecessary

```kotlin
// ✅ Use Result<T> instead of T?
suspend fun translate(text: String): Result<TranslationResult>

// ❌ Null has no failure context
suspend fun translate(text: String): TranslationResult?
```

### Prefer `data class` for value types, `value class` for wrappers

```kotlin
// ✅ Inline value class — zero allocation
@JvmInline value class ImageData(val id: String)

// ✅ Data class for multi-field model
data class TranslationResult(val originalText: String, val translatedText: String, ...)
```

### Limit function length

- **Max 30 lines** per function. If longer → extract private helper.
- **Max 5 parameters** per function. If more → use a data class parameter.

```kotlin
// ❌ Too many parameters
fun render(text: String, x: Int, y: Int, width: Int, height: Int, alpha: Float, size: Float)

// ✅ Use a model
fun render(block: OverlayBlock)
```

### Prefer `val` everywhere

Declare `var` only when mutation is genuinely required (e.g., local loop variable). All class properties that hold state must use `StateFlow` — not a mutable `var`.

### `apply`, `also`, `let`, `run` — use deliberately

```kotlin
// ✅ apply — configure an object
val params = WindowManager.LayoutParams().apply {
    type   = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    format = PixelFormat.TRANSLUCENT
    flags  = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
}

// ✅ let — null-safe operation
cachedResult?.let { return Result.success(it) }

// ✅ also — side effect without changing the object
return Result.success(result).also { Timber.d("Translation complete: $result") }

// ❌ Chained scope functions — kills readability
obj.let { it.also { it.apply { ... } } }
```

---

## 4. Coroutine & Flow Rules

### Never block the main thread

```kotlin
// ❌ Blocks main thread
val result = runBlocking { repository.translate(text) }

// ✅ Suspend inside a coroutine
viewModelScope.launch {
    val result = repository.translate(text)
}
```

### Always use injectable `AppDispatchers`

```kotlin
// ❌ Hardcoded dispatcher — untestable
withContext(Dispatchers.IO) { ... }

// ✅ Injected dispatcher — swappable in tests
withContext(dispatchers.io) { ... }
```

### `CancellationException` must always propagate

```kotlin
// ❌ Swallows cancellation — coroutine never stops
try {
    engine.recognize(bitmap)
} catch (e: Exception) {
    Result.failure(e)
}

// ✅ Re-throw CancellationException
try {
    engine.recognize(bitmap)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

// ✅ Even simpler — runCatching excludes CancellationException in Kotlin 1.9+
runCatching { engine.recognize(bitmap) }
```

### Expose `StateFlow`, collect internally

```kotlin
// ✅ ViewModel exposes immutable StateFlow
val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

// ❌ Expose MutableStateFlow — external mutation is uncontrolled
val uiState: MutableStateFlow<AppUiState> = MutableStateFlow(AppUiState.Loading)
```

### Use `SharedFlow` for one-shot events

```kotlin
// ✅ SharedFlow for navigation events, snackbar triggers
private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
val events: SharedFlow<UiEvent> = _events.asSharedFlow()
```

### Prefer `Flow` builders for repositories, not callbacks

```kotlin
// ✅ callbackFlow — bridge callback APIs to Flow
override fun startContinuousCapture(intervalMs: Long): Flow<Result<ImageData>> =
    callbackFlow {
        val listener = ImageReader.OnImageAvailableListener { reader ->
            trySend(captureFromReader(reader))
        }
        imageReader.setOnImageAvailableListener(listener, null)
        awaitClose { imageReader.setOnImageAvailableListener(null, null) }
    }
```

### `viewModelScope` vs `serviceScope` vs `lifecycleScope`

| Scope | Use when |
|-------|---------|
| `viewModelScope` | ViewModel-tied work — cancelled on ViewModel clear |
| `lifecycleScope` | Activity/Fragment work tied to UI lifecycle |
| `serviceScope` | Work that must survive app being backgrounded |
| `CoroutineScope(SupervisorJob())` | Custom lifetime — manually cancelled |

**Never** create `GlobalScope` coroutines.

---

## 5. Dependency Injection Rules

### One `@Module` per feature

```
feature/ocr/di/OcrModule.kt          ← binds OcrEngine, OcrRepository
feature/translator/di/TranslatorModule.kt
feature/overlay/di/OverlayModule.kt
data/di/DataModule.kt                 ← binds Room, DataStore repos
```

### Bind interfaces, not implementations

```kotlin
// ✅ Bind interface → implementation
@Binds
@Singleton
abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository

// ❌ Expose implementation — breaks plugin pattern
@Provides
@Singleton
fun provideOcrRepository(): OcrRepositoryImpl = OcrRepositoryImpl()
```

### `@Singleton` only when truly needed

```kotlin
// ✅ Singleton — shared state (cache, store)
@Singleton class TranslationCache @Inject constructor()
@Singleton class ImageStore @Inject constructor()

// ❌ Singleton — stateless utility (wasteful)
@Singleton class BitmapExtensions @Inject constructor()  // should be top-level functions
```

### No `@Inject` in `:domain`

Domain classes (Use Cases, models) are constructed manually or with assisted inject — they must not depend on Hilt annotations to keep them framework-free.

```kotlin
// ✅ Use case — plain constructor, no @Inject in domain
class TranslateScreenUseCase(
    private val captureRepo: CaptureRepository,
    private val ocrRepo: OcrRepository,
    private val translationRepo: TranslationRepository
)

// ❌ Hilt annotation in domain layer
class TranslateScreenUseCase @Inject constructor(...)  // imports hilt — breaks purity
```

Wiring happens in the Hilt module inside `:app` or `:feature:*`:

```kotlin
// app/di/UseCaseModule.kt
@Provides
fun provideTranslateScreenUseCase(
    captureRepo: CaptureRepository,
    ocrRepo: OcrRepository,
    translationRepo: TranslationRepository
): TranslateScreenUseCase = TranslateScreenUseCase(captureRepo, ocrRepo, translationRepo)
```

### Engine maps for plugin pattern

```kotlin
// feature/ocr/di/OcrModule.kt
@Module @InstallIn(SingletonComponent::class)
abstract class OcrModule {

    @Binds @IntoMap
    @OcrEngineKey(OcrEngineType.ML_KIT)
    abstract fun bindMlKitEngine(impl: MlKitOcrEngine): OcrEngine

    // future: Tesseract, PaddleOCR added here — zero changes elsewhere
}
```

---

## 6. Compose Conventions

### Composable function rules

```kotlin
// ✅ Composable — PascalCase, no side effects in body
@Composable
fun TranslationBlock(
    block: OverlayBlock,
    modifier: Modifier = Modifier   // always include modifier as last param before lambdas
) {
    Text(
        text     = block.translatedText,
        modifier = modifier.padding(8.dp)
    )
}

// ❌ Triggering effects directly in Composable body
@Composable
fun BadExample() {
    viewModel.loadData()   // side effect — use LaunchedEffect instead
}
```

### State hoisting

```kotlin
// ✅ State hoisted to caller — composable is stateless and testable
@Composable
fun LanguagePicker(
    selected: Language,
    onLanguageSelected: (Language) -> Unit,
    modifier: Modifier = Modifier
)

// ❌ State inside composable — untestable and hard to reuse
@Composable
fun LanguagePicker() {
    var selected by remember { mutableStateOf(Language.Auto) }
}
```

### `key()` for list items

```kotlin
// ✅ key() prevents unnecessary recomposition
content.blocks.forEach { block ->
    key(block.id) {
        TranslationBlock(block = block)
    }
}

// ❌ No key — every block recomposes on any list change
content.blocks.forEach { block ->
    TranslationBlock(block = block)
}
```

### Collect Flow in Composable — use `collectAsStateWithLifecycle`

```kotlin
// ✅ Lifecycle-aware collection
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// ❌ Not lifecycle-aware — collects even when app is backgrounded
val uiState by viewModel.uiState.collectAsState()
```

### Preview for every screen composable

```kotlin
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    AutoTransTheme {
        SettingsScreen(uiState = SettingsUiState.default())
    }
}
```

### No business logic in Composables

```kotlin
// ❌ Business logic in Composable
@Composable
fun TranslateButton(text: String, from: Language, to: Language) {
    val result = runBlocking { engine.translate(text, from, to) }   // ❌
}

// ✅ All logic in ViewModel
@Composable
fun TranslateButton(onTranslateClick: () -> Unit) {
    Button(onClick = onTranslateClick) { Text("Translate") }
}
```

---

## 7. Error Handling Rules

Full error handling specification: [ERROR_HANDLING.md](ERROR_HANDLING.md).

### Quick rules

```kotlin
// Rule 1: Repository functions return Result<T> — never throw
override suspend fun translate(request: TranslationRequest): Result<TranslationResult> =
    runCatching { engine.translate(request) }

// Rule 2: CancellationException always propagates
runCatching { ... }.onFailure { if (it is CancellationException) throw it }

// Rule 3: ViewModel maps Result to UiState — never exposes raw exceptions to UI
val result = translateUseCase(pair)
_uiState.value = result.fold(
    onSuccess = { AppUiState.Ready(settings) },
    onFailure = { AppUiState.Error(it, retryable = true) }
)

// Rule 4: Never show raw exception message to user
// ❌
Text("Error: ${exception.message}")
// ✅
Text(stringResource(R.string.error_translation_failed))
```

---

## 8. Logging Rules

Full logging specification: [ERROR_HANDLING.md §6](ERROR_HANDLING.md#6-logging-strategy).

### Quick rules

```kotlin
// ✅ Always use Timber — never android.util.Log
Timber.d("Cache hit for key=$key")
Timber.e(exception, "E09: Translation timeout (engine=${engine.engineType})")

// ❌ Direct Log usage
Log.d("MyTag", "Cache hit")
Log.e("MyTag", "Error", exception)

// ✅ Include error code and context in error logs
Timber.w("E04: captureFrame null — imageId=${imageData.id}")

// ❌ No context — useless for debugging
Timber.e(e, "Error occurred")

// ✅ Verbose logs wrapped in DEBUG guard for performance
if (BuildConfig.DEBUG) Timber.v("Frame processed in ${elapsed}ms")
```

---

## 9. Architecture Rules

These are hard rules enforced by **module dependencies** (Gradle won't compile if violated) and **detekt custom rules** where possible.

### The dependency direction rule

```
:app → :feature:* → :domain ← (nothing)
:app → :data → :domain
:app → :core:ui, :core:common
:feature:* → :core:common
```

**Never**:
- `:domain` imports anything from `android.*`
- `:feature:capture` imports `:feature:ocr`
- `:data` imports `:feature:*`
- `:core:common` imports `:domain`

### Use Case rule

```kotlin
// ✅ One Use Case = one class = one public function
class GetTranslationHistoryUseCase @Inject constructor(...) {
    operator fun invoke(limit: Int): Flow<List<TranslationHistoryItem>>
}

// ❌ Use Case doing multiple unrelated things
class TranslationUseCase {
    fun getHistory(): Flow<...>
    fun translate(): Result<...>
    fun clearHistory()
}
```

### Repository rule

```kotlin
// ✅ Repository = data access only, no business logic
class TranslationRepositoryImpl : TranslationRepository {
    override suspend fun translate(...) = engineProvider.getActiveEngine().translate(...)
}

// ❌ Business logic in repository
class TranslationRepositoryImpl : TranslationRepository {
    override suspend fun translate(...) {
        if (from == to) return Result.success(...)   // business rule — belongs in UseCase
        // ...
    }
}
```

### ViewModel rule

```kotlin
// ✅ ViewModel calls Use Cases only — not Repositories directly
class MainViewModel @Inject constructor(
    private val translateUseCase: TranslateScreenUseCase,
    private val getSettingsUseCase: GetSettingsUseCase
)

// ❌ ViewModel bypasses domain — accesses repository directly
class MainViewModel @Inject constructor(
    private val translationRepo: TranslationRepository   // should use UseCase instead
)
```

---

## 10. Branch Naming

```
<type>/<short-description>
```

| Type | Use for |
|------|---------|
| `feat/` | New feature |
| `fix/` | Bug fix |
| `refactor/` | Code improvement without behavior change |
| `test/` | Adding or fixing tests |
| `docs/` | Documentation only |
| `ci/` | CI/CD changes |
| `chore/` | Dependency updates, build config |
| `perf/` | Performance improvement |

### Examples

```bash
feat/mlkit-ocr-engine
feat/overlay-foreground-service
fix/virtual-display-memory-leak
refactor/capture-repository-cleanup
test/pipeline-state-transitions
docs/add-adr-001-multi-module
ci/add-ktlint-detekt-workflow
chore/update-compose-bom-2025
perf/bitmap-downsampling-720p
```

### Rules

- Use lowercase and hyphens — no underscores, no slashes inside the description
- Keep the description ≤ 5 words
- Feature branches off `develop`, not `main`
- Hotfix branches off `main`
- **Never commit directly to `main`** or `develop`

---

## 11. Commit Message

Full specification: [Implementation Plan — Commit Strategy](../implementation_plan.md).

Format: **Conventional Commits**

```
<type>(<scope>): <subject>

[optional body — explain WHY, not WHAT]

[optional footer: Closes #123, Breaking change: ...]
```

### Type reference

| Type | When |
|------|------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | No behavior change |
| `perf` | Performance improvement |
| `test` | Tests only |
| `docs` | Documentation only |
| `ci` | CI/CD workflow |
| `chore` | Build, deps, config |
| `style` | Formatting (auto-generated by ktlint) |
| `revert` | Revert a previous commit |

### Scope reference

| Scope | Module/area |
|-------|------------|
| `capture` | `:feature:capture` |
| `ocr` | `:feature:ocr` |
| `translator` | `:feature:translator` |
| `overlay` | `:feature:overlay` |
| `settings` | `:feature:settings` |
| `domain` | `:domain` |
| `data` | `:data` |
| `pipeline` | Pipeline orchestration |
| `di` | Hilt / DI modules |
| `ui` | `:core:ui` |
| `common` | `:core:common` |
| `arch` | Architecture docs |

### Subject rules

- Lowercase first letter
- No period at end
- Imperative mood: "add", "fix", "remove" — not "added", "fixes", "removing"
- ≤ 72 characters total (type + scope + subject)

### Quality examples

```bash
feat(capture): implement MediaProjection screen capture with ImageStore

fix(overlay): prevent WindowManager leak when service destroyed unexpectedly

refactor(domain): replace Bitmap with ImageData to remove Android dependency

perf(ocr): add LRU cache for repeated OCR results using content hash

test(pipeline): add state transition tests for non-fatal OCR failure

docs(arch): add ADR-001 for multi-module architecture decision

ci: add Gradle build cache and detekt to CI pipeline

chore(deps): update ML Kit text recognition to 16.0.2
```

### Anti-patterns to avoid

```bash
# ❌ Too vague
fix: bug fix
feat: stuff
update: things

# ❌ Past tense
feat(ocr): added ML Kit engine

# ❌ No scope on feature work
feat: implement translation cache

# ❌ Too long subject
feat(capture): implement the screen capture module using MediaProjection API with VirtualDisplay and ImageReader and ImageStore
```

---

## 12. Pull Request Checklist

Every PR must pass this checklist before requesting review.

### Author checklist (before opening PR)

```markdown
## Self-review
- [ ] I have read the diff top-to-bottom at least once
- [ ] No debug code, TODOs, or commented-out code left behind
- [ ] No hardcoded strings (use `@string` resources or constants)
- [ ] No hardcoded colors (use `AutoTransTheme` tokens)

## Architecture
- [ ] `:domain` has no `android.*` imports
- [ ] No `:feature:*` module depends on another `:feature:*`
- [ ] New repository? → Interface added to `:domain`, implementation in `:feature:*` or `:data`
- [ ] New engine? → Implements `OcrEngine` or `TranslationEngine` interface
- [ ] No business logic in Repository or ViewModel bypasses Use Cases

## Code quality
- [ ] `./gradlew ktlintCheck` passes (zero violations)
- [ ] `./gradlew detekt` passes (zero new issues)
- [ ] `./gradlew test` passes (all unit tests green)
- [ ] New public functions have KDoc comments
- [ ] New error paths have Timber log with error code

## Tests
- [ ] New feature has unit tests covering happy path
- [ ] New feature has unit tests covering at least one failure path
- [ ] Fake updated if new method added to Repository interface
- [ ] No `Thread.sleep()` in tests

## Compose (if UI changed)
- [ ] New composables have `@Preview`
- [ ] All interactive elements have `Modifier.testTag(...)`
- [ ] State is hoisted — composable is stateless
- [ ] `collectAsStateWithLifecycle()` used (not `collectAsState()`)

## Performance (if feature touches pipeline)
- [ ] No new allocation inside `processFrame()` tight loop
- [ ] No blocking call on `Dispatchers.Main`
- [ ] Bitmap released after use (`recycle()` or via `ImageStore`)

## Commits
- [ ] All commits follow Conventional Commits format
- [ ] No "WIP", "fix fix", "temp" commit messages
- [ ] Commits are atomic — each can be reverted independently

## Documentation
- [ ] CHANGELOG.md updated (for user-visible changes)
- [ ] Inline comments explain WHY, not WHAT
- [ ] If new ADR needed → created in `docs/architecture/decisions/`
```

### Reviewer guidelines

```markdown
## What reviewers check
- Architecture rule compliance (dependency direction)
- Test coverage of error paths
- `CancellationException` handling
- No Android types in `:domain`
- Commit message quality
- Performance implications of hot-path changes

## Approval criteria
- At least 1 approving review required to merge
- CI must be green (ktlint + detekt + tests)
- No unresolved review comments

## Review turnaround
- Aim to review within 24 hours
- If blocked, leave a comment explaining what's blocking
```

---

*Automated enforcement: `.editorconfig` + `ktlint.gradle.kts` + `detekt.yml`.*
*For setup instructions, see [CONTRIBUTOR_GUIDE.md](CONTRIBUTOR_GUIDE.md).*
*For commit message examples by milestone, see [ROADMAP.md](ROADMAP.md).*
