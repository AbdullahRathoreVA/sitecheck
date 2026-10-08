package dev.abdullahrathore.sitecheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Command line entry point.
 *
 * <pre>
 *   java -jar sitecheck.jar example.com
 *   java -jar sitecheck.jar --json example.com another.com
 * </pre>
 *
 * Exits with 1 when any site has a failed check, so it can run in CI or a cron job.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        boolean json = false;
        List<String> sites = new ArrayList<>();
        for (String a : args) {
            if (a.equals("--json")) {
                json = true;
            } else if (a.equals("-h") || a.equals("--help")) {
                usage();
                return;
            } else {
                sites.add(a);
            }
        }
        if (sites.isEmpty()) {
            usage();
            System.exit(2);
        }

        SiteChecks checks = new SiteChecks();
        boolean anyFailed = false;
        for (String site : sites) {
            try {
                Report r = checks.check(site);
                System.out.println(json ? r.toJson() : r.toText());
                anyFailed |= r.count(Finding.Status.FAIL) > 0;
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                anyFailed = true;
            } catch (java.io.IOException e) {
                System.err.println(site + ": could not be reached (" + e.getMessage() + ")");
                anyFailed = true;
            }
        }
        System.exit(anyFailed ? 1 : 0);
    }

    private static void usage() {
        System.out.println("usage: java -jar sitecheck.jar [--json] <site> [<site> ...]");
    }
}
