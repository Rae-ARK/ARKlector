# Implementation

The concrete how, one life cycle stage at a time. Each doc covers one stage's created → active → torn-down story and its Java API surface.

1. [Acquisition](acquisition.md) — clipboard + share-intent in
2. [Parsing](parsing.md) — normalize and segment
3. [Synthesis & Playback](synthesis-playback.md) — queue, TTS, foreground service
4. [Persistence](persistence.md) — Room: queue mirror + history
5. [Settings](settings.md) — voice, rate, pitch, auto-read

See [`../foundational/architecture.md`](../foundational/architecture.md) for how these fit together, and [`../foundational/design.md`](../foundational/design.md) for the rules every file above is held to.
