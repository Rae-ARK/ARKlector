package ARKlector.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ARKlector.ArklectorApplication;
import org.arklector.app.R;
import ARKlector.model.Segment;
import ARKlector.viewmodel.PlaybackState;

/**
 * Foreground service keeping playback alive once the app leaves the foreground.
 * See docs/implementation/synthesis-playback.md. Notification is built in Java,
 * not inflated from any XML layout.
 */
public final class PlaybackService extends Service {

    private static final String CHANNEL_ID = "playback";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();
    private final MutableLiveData<PlaybackState> state = new MutableLiveData<>(new PlaybackState.Idle());

    private TtsEngine ttsEngine;
    private volatile ReadingQueueService queueService;
    private Preferences preferences;
    // True from the moment a segment is taken off the queue until its utterance ends.
    // Guards against onStartCommand (one per startForegroundService call) pulling a
    // second segment while one is still speaking.
    private boolean speaking;
    private boolean paused;
    private Segment currentSegment;

    @Override
    public void onCreate() {
        super.onCreate();
        ArklectorApplication app = (ArklectorApplication) getApplication();
        preferences = app.preferences();
        ttsEngine = new TtsEngine(this);
        ttsEngine.setProgressListeners(this::onSegmentDone, this::onSegmentDone);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        advanceQueue();
        // Nothing to resume after a process kill (no queue reference until the Activity
        // rebinds), so don't let the system restart an empty foreground service.
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public LiveData<PlaybackState> state() {
        return state;
    }

    /**
     * Called by the bound Activity. The service is usually started before the binding
     * completes, so pick up anything already queued rather than waiting for the next
     * start command.
     */
    public void setQueueService(ReadingQueueService queueService) {
        this.queueService = queueService;
        if (!queueService.isEmpty()) {
            advanceQueue();
        }
    }

    private void advanceQueue() {
        ReadingQueueService queue = queueService;
        synchronized (this) {
            if (queue == null || speaking) {
                return;
            }
            speaking = true;
        }
        queue.next().ifPresentOrElse(
                segment -> {
                    synchronized (this) {
                        currentSegment = segment;
                    }
                    state.postValue(new PlaybackState.Playing(segment));
                    ttsEngine.speak(segment, preferences.observeVoice().getValue());
                },
                () -> {
                    synchronized (this) {
                        speaking = false;
                        currentSegment = null;
                    }
                    finishPlayback();
                });
    }

    private void finishPlayback() {
        state.postValue(new PlaybackState.Idle());
        // A bound Activity keeps the service alive past stopSelf(), which would leave
        // the foreground notification up; remove it explicitly.
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void onSegmentDone(Segment ignored) {
        synchronized (this) {
            if (paused) {
                return;
            }
            speaking = false;
        }
        advanceQueue();
    }

    // ---- Playback controls (called from the bound Activity, main thread) ---------

    /**
     * TextToSpeech has no real pause, so pausing stops the utterance and resuming
     * re-speaks the current segment from its start.
     */
    public void pause() {
        Segment segment;
        synchronized (this) {
            if (!speaking || paused || currentSegment == null) {
                return;
            }
            paused = true;
            segment = currentSegment;
        }
        ttsEngine.stop();
        state.postValue(new PlaybackState.Paused(segment));
    }

    public void resume() {
        Segment segment;
        synchronized (this) {
            if (!paused) {
                return;
            }
            paused = false;
            segment = currentSegment;
        }
        state.postValue(new PlaybackState.Playing(segment));
        ttsEngine.speak(segment, preferences.observeVoice().getValue());
    }

    public void skipNext() {
        synchronized (this) {
            paused = false;
            speaking = false;
        }
        ttsEngine.stop();
        advanceQueue();
    }

    /** Stops speaking and discards whatever is still queued. */
    public void stopPlayback() {
        ReadingQueueService queue = queueService;
        if (queue != null) {
            queue.clear();
        }
        synchronized (this) {
            paused = false;
            speaking = false;
            currentSegment = null;
        }
        ttsEngine.stop();
        finishPlayback();
    }

    private Notification buildNotification() {
        // Built entirely in code — no res/layout XML. A custom RemoteViews layout for
        // play/pause/skip actions is still open, see docs/proposal/open-questions.md item 4.
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_play_arrow)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        ttsEngine.shutdown();
        super.onDestroy();
    }

    public final class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }
}
