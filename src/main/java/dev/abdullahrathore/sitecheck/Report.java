package dev.abdullahrathore.sitecheck;

import java.util.List;

/** Everything found for one site, plus the two ways of printing it. */
public record Report(String url, List<Finding> findings) {

    public long count(Finding.Status status) {
        return findings.stream().filter(f -> f.status() == status).count();
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append(url).append('\n');
        for (Finding f : findings) {
            String mark = switch (f.status()) {
                case PASS -> "  ok  ";
                case WARN -> "  warn";
                case FAIL -> "  FAIL";
            };
            sb.append(mark).append("  ").append(String.format("%-12s", f.id())).append(f.message()).append('\n');
        }
        sb.append(String.format("%n%d passed, %d warnings, %d failed%n",
                count(Finding.Status.PASS), count(Finding.Status.WARN), count(Finding.Status.FAIL)));
        return sb.toString();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"url\":").append(quote(url)).append(",\"findings\":[");
        for (int i = 0; i < findings.size(); i++) {
            Finding f = findings.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(quote(f.id()))
              .append(",\"status\":").append(quote(f.status().name().toLowerCase()))
              .append(",\"message\":").append(quote(f.message())).append('}');
        }
        return sb.append("]}").toString();
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
