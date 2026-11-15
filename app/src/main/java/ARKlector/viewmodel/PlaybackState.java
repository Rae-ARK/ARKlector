package ARKlector.viewmodel;

import ARKlector.model.Segment;

/**
 * Java 21 sealed interface — the project's answer to a Kotlin sealed class, per
 * docs/foundational/design.md rule 4.
 */
public sealed interface PlaybackState {

    record Idle() implements PlaybackState {
    }

    record Playing(Segment current) implements PlaybackState {
    }

    record Paused(Segment current) implements PlaybackState {
    }
}
