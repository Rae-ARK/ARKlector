# Design

The rules the code is held to, and the decisions that are settled. Each entry is a *why*; the *what* is in [`architecture.md`](architecture.md).

## Platform constraints

These are not negotiable and every other decision below is read in light of them:

* **Java 21**, min SDK 34. No Kotlin, anywhere, for any reason.
* **No Jetpack Compose.** UI is built with the classic `View`/`ViewGroup` toolkit.
* **No XML layouts.** Views are constructed and configured in Java code (`new LinearLayout(context)`, `setLayoutParams`, etc.), not inflated from `res/layout/*.xml`. Styling (colors, dimensions, typography) is likewise expressed in Java constants/builders, not `res/values/styles.xml`.
* **Material 3, View-based.** The app theme is the library's `Theme.Material3.DayNight.NoActionBar` (dynamic color on Android 12+, applied in `ArklectorApplication`), and widgets come from Material Components (`MaterialSwitch`, `Slider`, `MaterialCardView`, `FloatingActionButton`, `MaterialDivider`). Colors are always resolved from theme attributes (color roles such as `colorSurface`, `colorOnSurface`, `colorPrimary`) and type from `TextAppearance.Material3.*`, via `AppTheme` - never hard-coded hex. This is still Java-built Views, not Compose.
* **Resources that do exist.** Launcher icon layers, the in-app logo and small vector icons live in `res/drawable*` and `res/mipmap-anydpi` - they are assets, not layouts or styles. Unicode glyphs are no longer used as icons.
* **No pattern-matching `switch`.** `android.jar` lacks `java.lang.runtime.SwitchBootstraps`, so javac crashes on it; use `instanceof` patterns (also fine on sealed types).

## Code philosophy

Service-oriented, separated by concern, minimal boilerplate, functional by default. Four rules:

1. **Services own resources.** Anything holding an external resource or a unit of business capability (clipboard access, the TTS engine, the reading queue, preferences) is a small service with a narrow API, its own file, and constructor-injected dependencies. Add an interface only when a second implementation is real or concretely planned (e.g. `TtsEngine`, once a cloud-voice backend is more than a proposal).
2. **Layers depend downward.** UI renders state and forwards intent; `ViewModel`s (AndroidX `androidx.lifecycle.ViewModel`, plain Java — this is a platform library, not Compose) hold UI state; services hold logic and I/O; DAOs hold persistence. No sideways reach into another feature's state, no upward reach into UI types from a service.
3. **Functional by default.** Java has no top-level functions, so the equivalent is: logic that transforms data lives as a `static` method on a small, `final` utility class — not as instance state on something stateful. Use an actual class only when there's state to hold or a resource with a lifecycle to manage. Templates: `TextNormalizer.resolveVoice(Preferences)`, `QueueMerge.mergeOnRescan(List<Segment>, List<Segment>)`.
4. **Minimal boilerplate.** Smallest construct that says it: a `static` method over a one-method class, a `record` or a `sealed interface` (Java 17+/21, not Kotlin's) over a hand-rolled model class, no one-`ViewModel`-per-screen by default, and never one giant `MainViewModel`.

## Testing strategy

* **Plain logic:** JVM unit tests in `app/src/test`, no device, no Android framework classes. Majority tier — this is where `static` utility methods and pure data transforms are proven.
* **Services with Android dependencies** (`ContentResolver` for clipboard, `TextToSpeech`, `SharedPreferences`): Robolectric-backed JVM tests in `app/src/test`, with the real Android dependency replaced at the constructor boundary by a fake or mock. No emulator needed for this tier either.
* **Cross-component and lifecycle flows** (foreground service survives a configuration change, playback notification stays in sync with `ViewModel` state): instrumented tests in `app/src/androidTest`, run on-device or on an emulator, using Espresso — never a Compose testing API. Smallest tier by design; reserved for behavior that only exists once real Android lifecycle callbacks are involved.

---

*Adapted from an earlier draft written for a sibling project; the interface/example names above (`TtsEngine`, `QueueMerge`) and the Kotlin-specific phrasing (`fun`, `data class`) have been reworked to fit ARKlector's domain and the Java-only constraint. Flag it if any of the renamed examples don't match how the code actually ends up being organized.*
