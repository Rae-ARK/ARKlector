package ARKlector.util;

import org.junit.Test;

import java.util.List;
import java.util.Locale;

import ARKlector.model.Segment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SegmenterTest {

    @Test
    public void shortTextIsOneSegment() {
        List<Segment> segments = Segmenter.split("Hello there.", Locale.US, 300);
        assertEquals(1, segments.size());
    }

    @Test
    public void longTextIsSplitUnderLimit() {
        String longText = "Sentence one. ".repeat(50);
        List<Segment> segments = Segmenter.split(longText, Locale.US, 50);
        assertTrue(segments.size() > 1);
        for (Segment s : segments) {
            assertTrue(s.text().length() <= 50);
        }
    }

    @Test
    public void sentenceExactlyAtLimitProducesNoEmptySegment() {
        String exact = "a".repeat(49) + ".";
        List<Segment> segments = Segmenter.split(exact, Locale.US, 50);
        assertEquals(1, segments.size());
        assertEquals(exact, segments.get(0).text());
    }

    @Test
    public void neverProducesBlankSegments() {
        for (int max = 5; max < 60; max++) {
            for (Segment s : Segmenter.split("One two three. Four five six seven eight! Nine?", Locale.US, max)) {
                assertTrue(max + ": '" + s.text() + "'", !s.text().isBlank());
                assertTrue(s.text().length() <= max);
            }
        }
    }

    @Test
    public void hardWrapPrefersWordBoundaries() {
        List<Segment> segments = Segmenter.split("alpha beta gamma delta epsilon", Locale.US, 12);
        assertEquals(List.of("alpha beta", "gamma delta", "epsilon"),
                segments.stream().map(Segment::text).toList());
    }
}
