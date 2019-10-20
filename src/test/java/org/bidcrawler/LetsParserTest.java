package org.bidcrawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;

public class LetsParserTest {
    private LetsParser letsParser;
    private void init() throws SQLException, ClassNotFoundException {
        letsParser = new LetsParser(null, null, null, null, null);
    }

    @Test
    void assertion() {
        try {
            init();
            assertEquals(letsParser.getClass().toString(), "class org.bidcrawler.LetsParser");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Failed to init");
        }
    }
}
