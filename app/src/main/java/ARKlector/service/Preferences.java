package ARKlector.service;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * The only thing in the app allowed to touch {@link SharedPreferences} directly.
 * See docs/implementation/settings.md.
 */
public final class Preferences {

    private static final String KEY_AUTO_READ = "autoReadOnCopy";
    private static final String KEY_VOICE_ID = "voiceId";
    private static final String KEY_SPEECH_RATE = "speechRate";
    private static final String KEY_PITCH = "pitch";

    private final SharedPreferences prefs;
    private final MutableLiveData<Voice> voiceLiveData;

    public Preferences(SharedPreferences prefs) {
        this.prefs = prefs;
        this.voiceLiveData = new MutableLiveData<>(readVoice());
    }

    public boolean autoReadOnCopy() {
        return prefs.getBoolean(KEY_AUTO_READ, true);
    }

    public void setAutoReadOnCopy(boolean value) {
        prefs.edit().putBoolean(KEY_AUTO_READ, value).apply();
    }

    public LiveData<Voice> observeVoice() {
        return voiceLiveData;
    }

    public void setVoice(Voice voice) {
        prefs.edit()
                .putString(KEY_VOICE_ID, voice.voiceId())
                .putFloat(KEY_SPEECH_RATE, voice.speechRate())
                .putFloat(KEY_PITCH, voice.pitch())
                .apply();
        voiceLiveData.setValue(voice);
    }

    private Voice readVoice() {
        return new Voice(
                prefs.getString(KEY_VOICE_ID, null),
                prefs.getFloat(KEY_SPEECH_RATE, 1.0f),
                prefs.getFloat(KEY_PITCH, 1.0f));
    }

    public record Voice(String voiceId, float speechRate, float pitch) {
    }
}
