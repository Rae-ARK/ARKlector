package ARKlector.service;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    private static final String KEY_FAVORITE_VOICE_NAMES = "favoriteVoiceNames";

    private final SharedPreferences prefs;
    private final MutableLiveData<Voice> voiceLiveData;
    private final MutableLiveData<Set<String>> favoriteVoiceNamesLiveData;

    public Preferences(SharedPreferences prefs) {
        this.prefs = prefs;
        this.voiceLiveData = new MutableLiveData<>(readVoice());
        this.favoriteVoiceNamesLiveData = new MutableLiveData<>(readFavoriteVoiceNames());
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

    /**
     * Voice names the voice picker should float to the top of each language group, via
     * {@link TtsVoiceModel#withFavoritesFirst}. Ported from ARKarium's voice picker, which
     * kept the same set per device rather than per voice-language.
     */
    public LiveData<Set<String>> observeFavoriteVoiceNames() {
        return favoriteVoiceNamesLiveData;
    }

    public void setVoiceFavorite(String voiceName, boolean favorite) {
        Set<String> updated = new HashSet<>(favoriteVoiceNamesLiveData.getValue());
        if (favorite) {
            updated.add(voiceName);
        } else {
            updated.remove(voiceName);
        }
        prefs.edit().putStringSet(KEY_FAVORITE_VOICE_NAMES, updated).apply();
        favoriteVoiceNamesLiveData.setValue(Collections.unmodifiableSet(updated));
    }

    private Set<String> readFavoriteVoiceNames() {
        Set<String> stored = prefs.getStringSet(KEY_FAVORITE_VOICE_NAMES, Collections.emptySet());
        return Collections.unmodifiableSet(new HashSet<>(stored));
    }

    public record Voice(String voiceId, float speechRate, float pitch) {
    }
}
