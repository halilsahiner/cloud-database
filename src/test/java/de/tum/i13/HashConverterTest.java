package de.tum.i13;

import de.tum.i13.shared.HashConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HashConverterTest {

    @Test
    public void convertSimpleMessage() {
        String hashed = HashConverter.getMd5("sdadasdasdasd");

        assertEquals("3dc319bdd9f5b0ecf202f367268a39e4", hashed);
    }

    @Test
    public void convertWithDots() {
        String hashed = HashConverter.getMd5("192.168.1.1:5553");

        assertEquals("da594b63360ede9a6dddacb89c4f0741", hashed);
    }

    @Test
    public void convertWithSymbols() {
        String hashed = HashConverter.getMd5("192.168.1.1:5553?__/%$??");

        assertEquals("fb18face6dd247ad83c1ef0cfb7df5e1", hashed);
    }
}
