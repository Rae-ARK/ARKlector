# Open questions

Not settled. Each becomes a `foundational/` entry once decided — until then, implementation docs should not assume an answer here.

1. **Cloud/neural TTS fallback.** Device `TextToSpeech` quality varies a lot by OEM. Worth a second `TtsEngine`-compatible implementation behind a paid/opt-in cloud voice? If yes, this is the "second real implementation" that promotes `TtsEngine` from concrete class to interface per [design.md rule 1](../foundational/design.md#code-philosophy).
2. **Background queue survival across app updates/reinstalls.** Current plan persists the queue in Room ([persistence.md](../implementation/persistence.md)), which survives process death but not an uninstall. Is that enough, or does this need export/import?
3. **Multi-paragraph / very long clipboard pastes.** [Segmenter](../implementation/parsing.md) chunks by character limit today; unclear whether a cap on total queued segments per paste is needed to keep the queue usable.
4. **Notification actions beyond play/pause/skip.** E.g. speed-up/slow-down from the notification itself, vs. only from in-app settings.
5. **Language auto-switch mid-queue.** [Parsing](../implementation/parsing.md) guesses a locale per segment; not yet decided whether `TtsEngine` should switch voices automatically per segment or stay pinned to the user's chosen voice regardless of detected language.
