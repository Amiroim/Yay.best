package best.yay.loginplugin.api;

/**
 * Result of a checkUser call. `allowed` defaults to true when the API
 * doesn't send that field, so this stays compatible with a backend that
 * only ever returns {"exists": ...}.
 */
public final class WhitelistCheckResult {

    public static final WhitelistCheckResult ERROR = new WhitelistCheckResult(false, false, null);

    private final boolean exists;
    private final boolean allowed;
    private final String reason;

    public WhitelistCheckResult(boolean exists, boolean allowed, String reason) {
        this.exists = exists;
        this.allowed = allowed;
        this.reason = reason;
    }

    public boolean exists() {
        return exists;
    }

    /** Only meaningful when exists() is true. */
    public boolean allowed() {
        return allowed;
    }

    /** e.g. "banned", "restricted", "unauthorised" - null if not blocked. */
    public String reason() {
        return reason;
    }
}
