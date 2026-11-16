package ARKlector.viewmodel;

import ARKlector.model.Segment;

/**
 * Java 21 sealed interface — the project's answer to a Kotlin sealed class, per
 * docs/foundational/design.md rule 4.
 */
public sealed interface PlaybackState {
    // Consumers: use instanceof patterns, not `switch` patterns — the latter needs
    // java.lang.runtime.SwitchBootstraps, which isn't in android.jar (build fails).


    record Idle() implements PlaybackState {
    }

    record Playing(Segment current) implements PlaybackState {
    }

    record Paused(Segment current) implements PlaybackState {
    }
}
