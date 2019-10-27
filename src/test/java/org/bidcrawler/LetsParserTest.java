package org.bidcrawler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import java.sql.SQLException;

public class LetsParserTest {
    private static LetsParser letsParser;
    private static void initParser() throws SQLException, ClassNotFoundException {
        letsParser = new LetsParser("2019-10-25", "2019-10-25", null, null, null);
    }

    @BeforeAll
    static void init() {
        try {
            initParser();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Failed to init");
        }
    }

    @Test
    void testAnnListPageFormat() {
        try {
            letsParser.setOption("공고");
            Document doc = letsParser.getListPage(1);
            System.out.println(doc.html());
            assert(doc != null);
            assert(doc.html().length() > 0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Ann list page test failed");
        }
    }

    @Test
    void testResListPageFormat() {
        try {
            letsParser.setOption("결과");
            Document doc = letsParser.getListPage(1);
            System.out.println(doc.html());
            assert(doc != null);
            assert(doc.html().length() > 0);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Res list page test failed");
        }
    }
}
