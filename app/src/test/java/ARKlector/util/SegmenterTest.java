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
}
