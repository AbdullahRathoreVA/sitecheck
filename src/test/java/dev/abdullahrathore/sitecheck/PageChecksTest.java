package dev.abdullahrathore.sitecheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abdullahrathore.sitecheck.Finding.Status;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageChecksTest {

    private static final String GOOD = """
            <!doctype html>
            <html lang="en"><head>
              <title>Bright Smile Dental | Family dentist</title>
              <meta name="description" content="Family dentist with evening and Saturday appointments. New patients welcome, most insurance accepted.">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <link rel="canonical" href="https://brightsmile.example/">
            </head><body>
              <h1>Gentle dental care for the whole family</h1>
              <img src="team.jpg" alt="Our dental team">
              <a href="tel:+15125550100">Call (512) 555-0100</a>
            </body></html>
            """;

    @Test
    void aWellBuiltPagePassesEveryCheck() {
        List<Finding> findings = PageChecks.run(GOOD);
        assertTrue(findings.stream().allMatch(f -> f.status() == Status.PASS), findings::toString);
    }

    @Test
    void missingTitleAndDescriptionFail() {
        String html = "<html><head></head><body><h1>Hi</h1></body></html>";
        assertEquals(Status.FAIL, PageChecks.title(html).status());
        assertEquals(Status.FAIL, PageChecks.description(html).status());
    }

    @Test
    void titleLengthOutsideTheVisibleRangeIsAWarning() {
        assertEquals(Status.WARN, PageChecks.title("<title>Home</title>").status());
        assertEquals(Status.WARN, PageChecks.title("<title>" + "x".repeat(70) + "</title>").status());
    }

    @Test
    void moreThanOneH1IsAWarningAndNoneIsAFailure() {
        assertEquals(Status.WARN, PageChecks.h1("<h1>a</h1><h1 class='x'>b</h1>").status());
        assertEquals(Status.FAIL, PageChecks.h1("<h2>a</h2>").status());
    }

    @Test
    void phoneShownAsTextIsAWarningButATelLinkPasses() {
        assertEquals(Status.WARN, PageChecks.phone("<p>Call us: 0300 1234567</p>").status());
        assertEquals(Status.PASS, PageChecks.phone("<a href='tel:03001234567'>Call</a>").status());
        assertEquals(Status.FAIL, PageChecks.phone("<p>Founded in 2019</p>").status());
    }

    @Test
    void numbersInsideScriptsDoNotCountAsAPhone() {
        String html = "<script>var id = 1234567890123;</script><p>Welcome</p>";
        assertEquals(Status.FAIL, PageChecks.phone(html).status());
    }

    @Test
    void imagesWithoutAltAreCounted() {
        Finding f = PageChecks.imageAlt("<img src=a.png><img src=b.png alt=''><img src=c.png>");
        assertEquals(Status.WARN, f.status());
        assertTrue(f.message().startsWith("2 of 3"), f.message());
    }

    @Test
    void attributesAreReadQuotedOrUnquoted() {
        assertEquals("canonical", PageChecks.attr("<link rel=canonical href=/>", "rel"));
        assertEquals("a b", PageChecks.attr("<meta content='a b'>", "content"));
        assertEquals(null, PageChecks.attr("<meta name=x>", "content"));
    }
}
