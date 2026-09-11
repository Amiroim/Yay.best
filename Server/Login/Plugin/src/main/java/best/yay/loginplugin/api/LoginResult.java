package best.yay.loginplugin.api;

/**
 * Result of a /login password-verification call. `reason` is null when
 * valid is true, and one of "invalid_credentials", "not_found", "banned",
 * "restricted", "unauthorised", "uuid_mismatch", "invalid_request", or
 * null (unknown/transport error) otherwise.
 */
public final class LoginResult {

    public static final LoginResult ERROR = new LoginResult(false, null);

    private final boolean valid;
    private final String reason;

    public LoginResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public boolean valid() {
        return valid;
    }

    public String reason() {
        return reason;
    }
}
