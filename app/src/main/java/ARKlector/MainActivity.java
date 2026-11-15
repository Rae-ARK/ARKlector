package ARKlector;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ARKlector.service.ClipboardSource;
import ARKlector.service.Preferences;
import ARKlector.service.ReadingQueueService;
import ARKlector.ui.HomeScreen;
import ARKlector.ui.SettingsScreen;
import ARKlector.viewmodel.PlaybackState;
import ARKlector.viewmodel.ReaderViewModel;

/**
 * Entirely programmatic UI — no {@code setContentView(int)} from an XML layout
 * anywhere in this class, per docs/foundational/design.md platform constraints.
 * MainActivity is just an orchestrator: all the UI logic lives in the ui/
 * package ({@link HomeScreen}, {@link SettingsScreen}); this class only owns the
 * container that swaps the two in and out, and forwards their callbacks down to
 * the ViewModel/service layer.
 */
public final class MainActivity extends AppCompatActivity {

    private ReaderViewModel viewModel;
    private Preferences preferences;
    private FrameLayout container;
    private HomeScreen homeScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArklectorApplication app = (ArklectorApplication) getApplication();
        preferences = app.preferences();
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipboardSource clipboardSource = new ClipboardSource(clipboardManager, preferences);
        ReadingQueueService queueService = new ReadingQueueService(app.database().queueDao());

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new ReaderViewModel(clipboardSource, queueService);
            }
        }).get(ReaderViewModel.class);

        container = new FrameLayout(this);
        setContentView(container);
        showHome();

        viewModel.playbackState().observe(this, this::onPlaybackStateChanged);

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            CharSequence shared = getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) {
                clipboardSource.submit(shared.toString());
            }
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
        container.removeAllViews();
        container.addView(homeScreen);
    }

    private void showSettings() {
        SettingsScreen settingsScreen = new SettingsScreen(this, preferences, this::showHome);
        container.removeAllViews();
        container.addView(settingsScreen);
    }

    private void onPlaybackStateChanged(PlaybackState state) {
        if (homeScreen != null) {
            homeScreen.setPlaybackState(state);
        }
    }
}
