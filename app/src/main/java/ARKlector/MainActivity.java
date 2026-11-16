package ARKlector;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ARKlector.service.ClipboardSource;
import ARKlector.service.PlaybackService;
import ARKlector.service.Preferences;
import ARKlector.service.ReadingQueueService;
import ARKlector.service.TtsEngine;
import ARKlector.ui.HomeScreen;
import ARKlector.ui.SettingsScreen;
import ARKlector.ui.TtsSettingsScreen;
import ARKlector.ui.TtsVoicePickerScreen;
import ARKlector.viewmodel.PlaybackState;
import ARKlector.viewmodel.ReaderViewModel;

/**
 * Entirely programmatic UI — no {@code setContentView(int)} from an XML layout
 * anywhere in this class, per docs/foundational/design.md platform constraints.
 * MainActivity is just an orchestrator: all the UI logic lives in the ui/ package
 * ({@link HomeScreen}, {@link SettingsScreen}, {@link TtsSettingsScreen},
 * {@link TtsVoicePickerScreen}); this class only owns the container that swaps
 * screens in and out, and forwards their callbacks down to the ViewModel/service
 * layer.
 */
public final class MainActivity extends AppCompatActivity {

    private ReaderViewModel viewModel;
    private Preferences preferences;
    private FrameLayout container;
    private HomeScreen homeScreen;
    private boolean playbackBound;

    private final ServiceConnection playbackConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            PlaybackService service = ((PlaybackService.LocalBinder) binder).getService();
            service.setQueueService(viewModel.queueService());
            service.state().observe(MainActivity.this, viewModel::updatePlaybackState);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    /**
     * A second {@link TtsEngine} — separate from the one {@code PlaybackService}
     * speaks through — purely so the settings screens can enumerate installed voices
     * without binding to playback. One extra native TTS engine instance for the
     * Activity's lifetime is the trade-off accepted here for not having to plumb a
     * service binding into every settings screen; revisit if that turns out to be a
     * real resource cost in practice.
     */
    private TtsEngine voiceBrowserEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArklectorApplication app = (ArklectorApplication) getApplication();
        preferences = app.preferences();
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        voiceBrowserEngine = new TtsEngine(this);

        // Built inside the factory so they're created once per ViewModel, not once per
        // Activity instance: constructing them here on every onCreate (e.g. rotation)
        // registered a new clipboard listener each time that nothing ever removed.
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                ClipboardSource clipboardSource = new ClipboardSource(clipboardManager, preferences);
                ReadingQueueService queueService = new ReadingQueueService(app.database().queueDao());
                return (T) new ReaderViewModel(clipboardSource, queueService);
            }
        }).get(ReaderViewModel.class);
        viewModel.setOnSegmentsQueued(this::startPlayback);

        container = new FrameLayout(this);
        setContentView(container);
        showHome();

        viewModel.playbackState().observe(this, this::onPlaybackStateChanged);
        viewModel.lastClipboardText().observe(this, this::onClipboardContentChanged);

        // Without the runtime grant the foreground-service notification is hidden
        // (playback itself still works).
        if (savedInstanceState == null) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 0);
        }

        // savedInstanceState == null: on rotation getIntent() is still the original
        // share intent and would otherwise be re-submitted.
        if (savedInstanceState == null && Intent.ACTION_SEND.equals(getIntent().getAction())) {
            CharSequence shared = getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) {
                viewModel.submitSharedText(shared.toString());
            }
        }
    }

    /**
     * Nothing started PlaybackService before, and nothing ever gave it the queue, so
     * queued segments were never spoken.
     */
    private void startPlayback() {
        Intent intent = new Intent(this, PlaybackService.class);
        startForegroundService(intent);
        if (!playbackBound) {
            playbackBound = bindService(intent, playbackConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void showHome() {
        homeScreen = new HomeScreen(this, new HomeScreen.Listener() {
            @Override
            public void onReadClipboardNow() {
                viewModel.readClipboardNow();
            }

            @Override
            public void onOpenSettings() {
                showSettings();
            }
        });
        // A fresh HomeScreen starts from its own placeholder defaults; push the
        // ViewModel's current values in immediately so returning here (e.g. from
        // Settings) doesn't flash "Idle" / the empty-clipboard placeholder before the
        // next state change happens to arrive.
        homeScreen.setPlaybackState(viewModel.playbackState().getValue());
        homeScreen.setContent(viewModel.lastClipboardText().getValue());
        container.removeAllViews();
        container.addView(homeScreen);
    }

    private void showSettings() {
        SettingsScreen settingsScreen = new SettingsScreen(this, preferences, new SettingsScreen.Listener() {
            @Override
            public void onBack() {
                showHome();
            }

            @Override
            public void onOpenTtsSettings() {
                showTtsSettings();
            }
        });
        container.removeAllViews();
        container.addView(settingsScreen);
    }

    private void showTtsSettings() {
        TtsSettingsScreen ttsSettingsScreen = new TtsSettingsScreen(
                this, preferences, voiceBrowserEngine, new TtsSettingsScreen.Listener() {
                    @Override
                    public void onBack() {
                        showSettings();
                    }

                    @Override
                    public void onOpenVoicePicker() {
                        showVoicePicker();
                    }
                });
        container.removeAllViews();
        container.addView(ttsSettingsScreen);
    }

    private void showVoicePicker() {
        TtsVoicePickerScreen voicePickerScreen =
                new TtsVoicePickerScreen(this, preferences, voiceBrowserEngine, this::showTtsSettings);
        container.removeAllViews();
        container.addView(voicePickerScreen);
    }

    private void onPlaybackStateChanged(PlaybackState state) {
        if (homeScreen != null) {
            homeScreen.setPlaybackState(state);
        }
    }

    private void onClipboardContentChanged(String text) {
        if (homeScreen != null) {
            homeScreen.setContent(text);
        }
    }

    @Override
    protected void onDestroy() {
        viewModel.setOnSegmentsQueued(null);
        if (playbackBound) {
            unbindService(playbackConnection);
            playbackBound = false;
        }
        voiceBrowserEngine.shutdown();
        super.onDestroy();
    }
}
