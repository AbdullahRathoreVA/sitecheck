package dev.abdullahrathore.sitecheck;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Fetches a site and runs every check: the network ones here, the HTML ones in {@link PageChecks}. */
public final class SiteChecks {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36 sitecheck/0.1";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final long SLOW_MS = 3000;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    public Report check(String address) throws IOException, InterruptedException {
        URI uri = normalise(address);
        List<Finding> findings = new ArrayList<>();

        long start = System.nanoTime();
        HttpResponse<String> page = get(uri);
        long ms = (System.nanoTime() - start) / 1_000_000;

        URI finalUri = page.uri();
        findings.add("https".equalsIgnoreCase(finalUri.getScheme())
                ? Finding.pass("https", "Served over HTTPS.")
                : Finding.fail("https", "Not served over HTTPS; browsers mark the page as not secure."));
        findings.add(page.statusCode() == 200
                ? Finding.pass("status", "Home page returns 200.")
                : Finding.fail("status", "Home page returns HTTP " + page.statusCode() + "."));
        findings.add(ms <= SLOW_MS
                ? Finding.pass("speed", "First response in " + ms + " ms.")
                : Finding.warn("speed", "First response took " + ms + " ms; visitors start leaving after about 3 seconds."));

        findings.addAll(PageChecks.run(page.body()));

        URI root = URI.create(finalUri.getScheme() + "://" + finalUri.getAuthority() + "/");
        HttpResponse<String> robots = getQuietly(root.resolve("robots.txt"));
        boolean robotsOk = robots != null && robots.statusCode() == 200;
        findings.add(robotsOk
                ? Finding.pass("robots", "robots.txt is present.")
                : Finding.warn("robots", "No robots.txt."));

        boolean sitemapListed = robotsOk && robots.body().toLowerCase().contains("sitemap:");
        HttpResponse<String> sitemap = sitemapListed ? null : getQuietly(root.resolve("sitemap.xml"));
        findings.add(sitemapListed || (sitemap != null && sitemap.statusCode() == 200)
                ? Finding.pass("sitemap", "A sitemap is available.")
                : Finding.warn("sitemap", "No sitemap.xml and none listed in robots.txt, so new pages are found more slowly."));

        return new Report(finalUri.toString(), findings);
    }

    static URI normalise(String address) {
        String a = address.trim();
        if (!a.startsWith("http://") && !a.startsWith("https://")) {
            a = "https://" + a;
        }
        URI uri = URI.create(a);
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("Not a website address: " + address);
        }
        return uri.getPath() == null || uri.getPath().isEmpty() ? uri.resolve("/") : uri;
    }

    private HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,*/*;q=0.8")
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getQuietly(URI uri) {
        try {
            return get(uri);
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
