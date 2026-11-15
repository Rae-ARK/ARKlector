package ARKlector.ui;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ARKlector.ArklectorApplication;
import ARKlector.service.ClipboardSource;
import ARKlector.service.ReadingQueueService;
import ARKlector.viewmodel.PlaybackState;
import ARKlector.viewmodel.ReaderViewModel;

/**
 * Entirely programmatic UI — no {@code setContentView(int)} from an XML layout
 * anywhere in this class, per docs/foundational/design.md platform constraints.
 * MainActivity is just an orcastrator, all the UI logic lives in the ui/
 */
public final class MainActivity extends AppCompatActivity {

    private ReaderViewModel viewModel;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArklectorApplication app = (ArklectorApplication) getApplication();
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipboardSource clipboardSource = new ClipboardSource(clipboardManager, app.preferences());
        ReadingQueueService queueService = new ReadingQueueService(app.database().queueDao());

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new ReaderViewModel(clipboardSource, queueService);
            }
        }).get(ReaderViewModel.class);

        setContentView(buildRootView());

        viewModel.playbackState().observe(this, this::onPlaybackStateChanged);

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            CharSequence shared = getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (shared != null) {
                clipboardSource.submit(shared.toString());
            }
        }
    }

    private android.view.View buildRootView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        statusText = new TextView(this);
        statusText.setText("Idle");
        root.addView(statusText);

        Button readNowButton = new Button(this);
        readNowButton.setText("Read clipboard now");
        readNowButton.setOnClickListener(v -> viewModel.readClipboardNow());
        root.addView(readNowButton);

        return root;
    }

    private void onPlaybackStateChanged(PlaybackState state) {
        String label = switch (state) {
            case PlaybackState.Idle ignored -> "Idle";
            case PlaybackState.Playing playing -> "Playing: " + playing.current().text();
            case PlaybackState.Paused paused -> "Paused: " + paused.current().text();
        };
        statusText.setText(label);
    }
}
