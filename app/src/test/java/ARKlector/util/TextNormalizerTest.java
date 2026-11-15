package ARKlector.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextNormalizerTest {

    @Test
    public void collapsesWhitespaceAndTrims() {
        assertEquals("hello world", TextNormalizer.clean("  hello \n\n world  "));
    }

    @Test
    public void nullBecomesEmptyString() {
        assertEquals("", TextNormalizer.clean(null));
    }
}
