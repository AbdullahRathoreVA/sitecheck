package dev.abdullahrathore.sitecheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReportTest {

    @Test
    void jsonEscapesQuotesAndNewlines() {
        Report r = new Report("https://a.example/", List.of(Finding.warn("title", "Say \"hi\"\nnow")));
        assertEquals("{\"url\":\"https://a.example/\",\"findings\":[{\"id\":\"title\",\"status\":\"warn\","
                + "\"message\":\"Say \\\"hi\\\"\\nnow\"}]}", r.toJson());
    }

    @Test
    void textReportEndsWithASummary() {
        Report r = new Report("https://a.example/", List.of(
                Finding.pass("https", "ok"), Finding.fail("phone", "none"), Finding.warn("lang", "missing")));
        assertTrue(r.toText().contains("1 passed, 1 warnings, 1 failed"), r.toText());
    }

    @Test
    void bareDomainsAreTreatedAsHttps() {
        assertEquals("https://example.com/", SiteChecks.normalise("example.com").toString());
        assertEquals("http://example.com/a", SiteChecks.normalise("http://example.com/a").toString());
        assertThrows(IllegalArgumentException.class, () -> SiteChecks.normalise("https://"));
    }
}
