package dev.abdullahrathore.sitecheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that only need the page's HTML. Kept free of any network code so every
 * rule can be tested against a string.
 *
 * Parsing is done with regular expressions on purpose: these checks look for a
 * handful of well-known tags, and a full HTML parser would be the project's
 * only dependency.
 */
public final class PageChecks {

    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK = Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern H1 = Pattern.compile("<h1\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_LANG = Pattern.compile("<html\\b[^>]*\\blang\\s*=\\s*[\"']?[a-z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEL_LINK = Pattern.compile("href\\s*=\\s*[\"']tel:", Pattern.CASE_INSENSITIVE);
    // 7 to 15 digits with the usual separators; bounded so a year or a long hash doesn't count.
    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s().-]{5,}\\d");
    private static final Pattern SCRIPT_STYLE = Pattern.compile("<(script|style)\\b.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private PageChecks() {
    }

    public static List<Finding> run(String html) {
        List<Finding> out = new ArrayList<>();
        out.add(title(html));
        out.add(description(html));
        out.add(h1(html));
        out.add(viewport(html));
        out.add(canonical(html));
        out.add(language(html));
        out.add(phone(html));
        out.add(imageAlt(html));
        return out;
    }

    static Finding title(String html) {
        Matcher m = TITLE.matcher(html);
        if (!m.find()) {
            return Finding.fail("title", "No <title>. It is the headline people see in search results.");
        }
        String t = m.group(1).trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) {
            return Finding.fail("title", "The <title> is empty.");
        }
        if (t.length() < 10 || t.length() > 60) {
            return Finding.warn("title", "Title is " + t.length() + " characters; 10 to 60 shows in full in search results.");
        }
        return Finding.pass("title", "Title: \"" + t + "\"");
    }

    static Finding description(String html) {
        String d = metaContent(html, "name", "description");
        if (d == null || d.isBlank()) {
            return Finding.fail("description", "No meta description, so search engines will pick a snippet themselves.");
        }
        int n = d.trim().length();
        if (n < 50 || n > 160) {
            return Finding.warn("description", "Meta description is " + n + " characters; 50 to 160 is the useful range.");
        }
        return Finding.pass("description", "Meta description is " + n + " characters.");
    }

    static Finding h1(String html) {
        int count = count(H1, html);
        if (count == 0) {
            return Finding.fail("h1", "No <h1>. The page has no main heading.");
        }
        if (count > 1) {
            return Finding.warn("h1", count + " <h1> headings; one clear main heading is easier to read and to rank.");
        }
        return Finding.pass("h1", "One <h1>.");
    }

    static Finding viewport(String html) {
        String v = metaContent(html, "name", "viewport");
        if (v == null || !v.toLowerCase(Locale.ROOT).contains("width=device-width")) {
            return Finding.fail("viewport", "No mobile viewport tag, so phones show a zoomed-out desktop page.");
        }
        return Finding.pass("viewport", "Mobile viewport is set.");
    }

    static Finding canonical(String html) {
        Matcher m = LINK.matcher(html);
        while (m.find()) {
            String tag = m.group();
            if (attr(tag, "rel") != null && attr(tag, "rel").equalsIgnoreCase("canonical") && attr(tag, "href") != null) {
                return Finding.pass("canonical", "Canonical URL: " + attr(tag, "href"));
            }
        }
        return Finding.warn("canonical", "No canonical link. Duplicate URLs (with and without www, tracking parameters) may split ranking.");
    }

    static Finding language(String html) {
        return HTML_LANG.matcher(html).find()
                ? Finding.pass("lang", "The page declares its language.")
                : Finding.warn("lang", "No lang attribute on <html>; screen readers and translators have to guess.");
    }

    static Finding phone(String html) {
        if (TEL_LINK.matcher(html).find()) {
            return Finding.pass("phone", "Has a tap-to-call link.");
        }
        Matcher m = PHONE.matcher(visibleText(html));
        while (m.find()) {
            long digits = m.group().chars().filter(Character::isDigit).count();
            if (digits >= 7 && digits <= 15) {
                return Finding.warn("phone", "A phone number is shown but not as a tap-to-call (tel:) link.");
            }
        }
        return Finding.fail("phone", "No phone number found. For a local business that is the main way customers get in touch.");
    }

    static Finding imageAlt(String html) {
        Matcher m = IMG.matcher(html);
        int total = 0;
        int missing = 0;
        while (m.find()) {
            total++;
            String alt = attr(m.group(), "alt");
            if (alt == null) {
                missing++;
            }
        }
        if (total == 0) {
            return Finding.pass("img-alt", "No images on the page.");
        }
        if (missing > 0) {
            return Finding.warn("img-alt", missing + " of " + total + " images have no alt text.");
        }
        return Finding.pass("img-alt", "All " + total + " images have alt text.");
    }

    static String visibleText(String html) {
        String noCode = SCRIPT_STYLE.matcher(html).replaceAll(" ");
        return TAG.matcher(noCode).replaceAll(" ").replaceAll("\\s+", " ");
    }

    static String metaContent(String html, String keyAttr, String keyValue) {
        Matcher m = META.matcher(html);
        while (m.find()) {
            String tag = m.group();
            String key = attr(tag, keyAttr);
            if (key != null && key.equalsIgnoreCase(keyValue)) {
                return attr(tag, "content");
            }
        }
        return null;
    }

    /** Value of one attribute inside a single tag, quoted or not; null when absent. */
    static String attr(String tag, String name) {
        Matcher m = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
                Pattern.CASE_INSENSITIVE).matcher(tag);
        if (!m.find()) {
            return null;
        }
        for (int g = 2; g <= 4; g++) {
            if (m.group(g) != null) {
                return m.group(g);
            }
        }
        return null;
    }

    private static int count(Pattern p, String s) {
        Matcher m = p.matcher(s);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }
}
