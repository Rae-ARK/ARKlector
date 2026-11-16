package ARKlector.service;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import ARKlector.service.TtsVoiceModel.TtsVoiceInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Coverage for {@link TtsVoiceModel}, ported alongside it from ARKarium's
 * {@code TtsVoiceModelTest.kt}: languages present, rows for one language,
 * installed-vs-not, the description string, favorites, and voiceNameToApply.
 */
public class TtsVoiceModelTest {

    private static TtsVoiceInfo voice(String name, String language) {
        return voice(name, language, "", 300, false, true);
    }

    private static TtsVoiceInfo voice(String name, String language, String region) {
        return voice(name, language, region, 300, false, true);
    }

    private static TtsVoiceInfo voice(String name, String language, String region, boolean online) {
        return voice(name, language, region, 300, online, true);
    }

    private static TtsVoiceInfo voice(
            String name, String language, String region, int quality, boolean online, boolean installed) {
        return new TtsVoiceInfo(name, language, region, quality, online, installed);
    }

    @Test
    public void languagesOf_putsDeviceLanguageFirst_thenRestAlphabetical() {
        List<TtsVoiceInfo> voices = List.of(
                voice("en-in", "en", "India"),
                voice("ja-jp", "ja", "Japan"),
                voice("ta-in", "ta", "India"),
                voice("en-us", "en", "United States"));
        assertEquals(List.of("ta", "en", "ja"), TtsVoiceModel.languagesOf(voices, "ta"));
    }

    @Test
    public void languagesOf_deviceLanguageAbsent_stillSortsTheRest() {
        List<TtsVoiceInfo> voices = List.of(voice("en-us", "en"), voice("ja-jp", "ja"));
        assertEquals(List.of("en", "ja"), TtsVoiceModel.languagesOf(voices, "fr"));
    }

    @Test
    public void languagesOf_distinctOnly() {
        List<TtsVoiceInfo> voices = List.of(voice("en-in-a", "en"), voice("en-in-b", "en"));
        assertEquals(List.of("en"), TtsVoiceModel.languagesOf(voices, "en"));
    }

    @Test
    public void rowsForLanguage_filtersToThatLanguage() {
        List<TtsVoiceInfo> voices = List.of(voice("en-us", "en"), voice("ja-jp", "ja"));
        assertEquals(List.of("en-us"), names(TtsVoiceModel.rowsForLanguage(voices, "en")));
    }

    @Test
    public void rowsForLanguage_localBeforeOnline() {
        List<TtsVoiceInfo> voices = List.of(
                voice("en-online", "en", "", true),
                voice("en-local", "en", "", false));
        assertEquals(List.of("en-local", "en-online"), names(TtsVoiceModel.rowsForLanguage(voices, "en")));
    }

    @Test
    public void rowsForLanguage_thenByRegionThenName() {
        List<TtsVoiceInfo> voices = List.of(
                voice("en-us-b", "en", "United States"),
                voice("en-in-a", "en", "India"),
                voice("en-in-z", "en", "India"));
        assertEquals(
                List.of("en-in-a", "en-in-z", "en-us-b"),
                names(TtsVoiceModel.rowsForLanguage(voices, "en")));
    }

    @Test
    public void descriptionOf_statesOnlineAndOfflineTheSameWay() {
        TtsVoiceInfo online = voice("v1", "en", "India", 400, true, true);
        TtsVoiceInfo offline = voice("v2", "en", "India", 400, false, true);
        assertEquals("India · online · high quality", TtsVoiceModel.descriptionOf(online));
        assertEquals("India · offline · high quality", TtsVoiceModel.descriptionOf(offline));
    }

    @Test
    public void descriptionOf_omitsBlankRegion() {
        TtsVoiceInfo noRegion = voice("v1", "en", "", 100, false, true);
        assertEquals("offline · very low quality", TtsVoiceModel.descriptionOf(noRegion));
    }

    @Test
    public void qualityLabel_fiveTierLadder() {
        assertEquals("very low quality", TtsVoiceModel.qualityLabel(50));
        assertEquals("low quality", TtsVoiceModel.qualityLabel(200));
        assertEquals("standard quality", TtsVoiceModel.qualityLabel(300));
        assertEquals("high quality", TtsVoiceModel.qualityLabel(400));
        assertEquals("very high quality", TtsVoiceModel.qualityLabel(500));
    }

    @Test
    public void installedOnly_dropsNotInstalled() {
        List<TtsVoiceInfo> voices = List.of(
                voice("installed", "en", "", 300, false, true),
                voice("missing", "en", "", 300, false, false));
        assertEquals(List.of("installed"), names(TtsVoiceModel.installedOnly(voices)));
    }

    @Test
    public void withFavoritesFirst_movesFavoritesToFront_keepingRelativeOrder() {
        List<TtsVoiceInfo> voices = List.of(
                voice("a", "en"), voice("b", "en"), voice("c", "en"), voice("d", "en"));
        Set<String> favorites = Set.of("c", "a");
        assertEquals(List.of("a", "c", "b", "d"), names(TtsVoiceModel.withFavoritesFirst(voices, favorites)));
    }

    @Test
    public void voiceNameToApply_returnsStoredNameWhenAvailable() {
        assertEquals("v1", TtsVoiceModel.voiceNameToApply("v1", List.of("v0", "v1")));
    }

    @Test
    public void voiceNameToApply_nullWhenNotAvailableOrNotStored() {
        assertNull(TtsVoiceModel.voiceNameToApply("v1", List.of("v0")));
        assertNull(TtsVoiceModel.voiceNameToApply(null, List.of("v0")));
    }

    private static List<String> names(List<TtsVoiceInfo> voices) {
        return voices.stream().map(TtsVoiceInfo::name).toList();
    }
}
