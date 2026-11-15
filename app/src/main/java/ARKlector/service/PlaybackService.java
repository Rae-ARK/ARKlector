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
import ARKlector.R;
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
    private ReadingQueueService queueService;
    private Preferences preferences;

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
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public LiveData<PlaybackState> state() {
        return state;
    }

    public void setQueueService(ReadingQueueService queueService) {
        this.queueService = queueService;
    }

    private void advanceQueue() {
        if (queueService == null) {
            return;
        }
        queueService.next().ifPresentOrElse(
                segment -> {
                    state.postValue(new PlaybackState.Playing(segment));
                    ttsEngine.speak(segment, preferences.observeVoice().getValue());
                },
                () -> {
                    state.postValue(new PlaybackState.Idle());
                    stopSelf();
                });
    }

    private void onSegmentDone(Segment ignored) {
        advanceQueue();
    }

    private Notification buildNotification() {
        // Built entirely in code — no res/layout XML. A custom RemoteViews layout for
        // play/pause/skip actions is still open, see docs/proposal/open-questions.md item 4.
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(android.R.drawable.ic_media_play)
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
