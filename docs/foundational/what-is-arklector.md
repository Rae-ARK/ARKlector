# What is ARKlector?

ARKlector is an Android app that reads clipboard content aloud. Copy text anywhere on the device, open or switch to ARKlector (or let it pick up the copy automatically, per [settings](../implementation/settings.md)), and it is queued and read with text-to-speech.

It is not a document reader, not an ebook/novel app, and not a note-taking app — it has exactly one job: turn whatever landed on the clipboard into speech, with enough queue and playback control to make that pleasant to use for more than one clip at a time.

## Settled, non-negotiable constraints

* Java 21 only. No Kotlin.
* No Jetpack Compose. Classic Views.
* No XML layouts or styles — built in Java code.

See [`design.md`](design.md) for the code philosophy that follows from these, and [`architecture.md`](architecture.md) for how the app is actually structured.
