package dev.abdullahrathore.sitecheck;

/** One check and how the page did on it. */
public record Finding(String id, Status status, String message) {

    public enum Status { PASS, WARN, FAIL }

    static Finding pass(String id, String message) {
        return new Finding(id, Status.PASS, message);
    }

    static Finding warn(String id, String message) {
        return new Finding(id, Status.WARN, message);
    }

    static Finding fail(String id, String message) {
        return new Finding(id, Status.FAIL, message);
    }
}
