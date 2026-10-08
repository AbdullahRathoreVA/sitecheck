# sitecheck

A small Java command-line tool that checks a website for the basics that quietly cost
a small business customers: no HTTPS, slow first response, no phone number, no mobile
viewport, missing title or description, no sitemap.

```
$ java -jar target/sitecheck-0.1.0.jar brightsmile.example
https://brightsmile.example/
  ok    https       Served over HTTPS.
  ok    status      Home page returns 200.
  warn  speed       First response took 3412 ms; visitors start leaving after about 3 seconds.
  ok    title       Title: "Bright Smile Dental | Family dentist"
  FAIL  description No meta description, so search engines will pick a snippet themselves.
  ok    h1          One <h1>.
  ok    viewport    Mobile viewport is set.
  warn  canonical   No canonical link. Duplicate URLs (with and without www, tracking parameters) may split ranking.
  ok    lang        The page declares its language.
  warn  phone       A phone number is shown but not as a tap-to-call (tel:) link.
  ok    img-alt     All 6 images have alt text.
  ok    robots      robots.txt is present.
  warn  sitemap     No sitemap.xml and none listed in robots.txt, so new pages are found more slowly.

8 passed, 4 warnings, 1 failed
```

## Build and run

Needs Java 21 and Maven.

```bash
mvn -q package
java -jar target/sitecheck-0.1.0.jar example.com
java -jar target/sitecheck-0.1.0.jar --json example.com another-site.com
```

The exit status is 1 when any site has a failed check, so it can run from CI or a
nightly job and alert you when something breaks.

## Checks

| Check | Fails or warns when |
|---|---|
| `https` | the final URL after redirects is not HTTPS |
| `status` | the home page does not return 200 |
| `speed` | the first response takes longer than 3 seconds |
| `title` | missing, empty, or outside 10 to 60 characters |
| `description` | missing, or outside 50 to 160 characters |
| `h1` | none, or more than one |
| `viewport` | no `width=device-width` viewport meta tag |
| `canonical` | no `<link rel="canonical">` |
| `lang` | no `lang` attribute on `<html>` |
| `phone` | no phone number, or one without a `tel:` link |
| `img-alt` | images without an `alt` attribute |
| `robots`, `sitemap` | no robots.txt, or no sitemap found |

It reads the home page as served, without running JavaScript, the way most crawlers
first see it. A site that builds all its content in the browser will show up with a
missing title and heading, which is worth knowing too.

## Design

- No dependencies at runtime: `java.net.http.HttpClient` for fetching, regular
  expressions for the few tags it needs. JUnit 5 for tests only.
- `PageChecks` holds every rule that only needs HTML, with no network code, so each
  rule is unit tested against a string. `SiteChecks` does the fetching.

## Tests

```bash
mvn test
```

## License

MIT
