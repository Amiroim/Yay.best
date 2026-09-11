package best.yay.loginplugin.api;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles all HTTP communication with the external account/login database API.
 *
 * Every public method here is asynchronous (returns a CompletableFuture) and must
 * never be blocked on (via .get()/.join()) from the main server thread, since that
 * would freeze the whole server while waiting on the network. The one exception is
 * inside AsyncPlayerPreLoginEvent, which Bukkit already fires off the main thread.
 *
 * JSON is built/parsed with regex + manual escaping instead of a library like Gson,
 * so this plugin has zero external dependencies to shade/relocate. It only ever
 * needs a couple of flat fields, which is all this API contract requires.
 *
 * Every request also sends an "X-API-Key" header - the backend rejects anything
 * without it, so random callers who find the API's URL can't hit it directly and
 * bypass this plugin's own brute-force guard. Set the matching value in api.key
 * in config.yml, equal to the API_KEY the backend is configured with.
 */
public class DatabaseAPI {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String checkUserPath;
    private final String loginPath;
    private final String logoutPath;
    private final String apiKey;
    private final Duration requestTimeout;
    private final Logger logger;

    public DatabaseAPI(FileConfiguration config, Logger logger) {
        this.logger = logger;
        this.baseUrl = stripTrailingSlash(config.getString("api.base-url", ""));
        this.checkUserPath = config.getString("api.check-user-path", "/api/checkUser");
        this.loginPath = config.getString("api.login-path", "/api/login");
        this.logoutPath = config.getString("api.logout-path", "/api/logout");
        this.apiKey = config.getString("api.key", "");

        int connectTimeout = config.getInt("api.connect-timeout-seconds", 5);
        this.requestTimeout = Duration.ofSeconds(config.getInt("api.request-timeout-seconds", 5));

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        if (apiKey.isBlank()) {
            logger.warning("api.key is empty in config.yml - the backend will reject every request "
                    + "until you set it to match its API_KEY.");
        }
    }

    /**
     * Checks whether a username exists and, if it does, whether the account is
     * currently allowed to play (not banned/restricted/unauthorised).
     */
    public CompletableFuture<WhitelistCheckResult> checkUser(String username) {
        String url = baseUrl + checkUserPath + "?username=" + encode(username);

        HttpRequest request = baseRequestBuilder(url)
                .timeout(requestTimeout)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("checkUser API returned HTTP " + response.statusCode() + " for '" + username + "'");
                        return WhitelistCheckResult.ERROR;
                    }
                    String body = response.body();
                    boolean exists = parseBooleanField(body, "exists").orElse(false);
                    // Default to true when the field is absent, so this still works
                    // against a backend that only returns {"exists": ...}.
                    boolean allowed = parseBooleanField(body, "allowed").orElse(true);
                    String reason = parseStringField(body, "reason");
                    return new WhitelistCheckResult(exists, allowed, reason);
                })
                .exceptionally(ex -> {
                    logFailure("checkUser", username, ex);
                    return WhitelistCheckResult.ERROR;
                });
    }

    /**
     * Verifies a username/password/UUID triple against the database. The UUID is
     * bound to the account on the first successful login and checked against on
     * every login after that, so reason can come back "uuid_mismatch" even with
     * a correct password, if this Minecraft account isn't the one linked to it.
     *
     * `ip` must be the player's real address (from Player#getAddress()), not
     * anything derived on the backend - every request to this API comes from
     * the Minecraft server's own network, so the backend can never see the
     * player's actual IP unless the plugin sends it explicitly.
     */
    public CompletableFuture<LoginResult> login(String username, String password, UUID uuid, String ip) {
        String url = baseUrl + loginPath;
        String jsonBody = "{\"username\":\"" + escapeJson(username) + "\","
                + "\"password\":\"" + escapeJson(password) + "\","
                + "\"uuid\":\"" + escapeJson(uuid.toString()) + "\","
                + "\"ip\":\"" + escapeJson(ip) + "\"}";

        HttpRequest request = baseRequestBuilder(url)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    int status = response.statusCode();
                    // 401/403/404/409 are expected "not valid" outcomes with a JSON
                    // reason body, not transport failures - only bail out on the rest.
                    if (status != 200 && status != 401 && status != 403 && status != 404 && status != 409) {
                        logger.warning("login API returned HTTP " + status + " for '" + username + "'");
                        return LoginResult.ERROR;
                    }
                    boolean valid = parseBooleanField(body, "valid").orElse(false);
                    String reason = parseStringField(body, "reason");
                    return new LoginResult(valid, reason);
                })
                .exceptionally(ex -> {
                    logFailure("login", username, ex);
                    return LoginResult.ERROR;
                });
    }

    /**
     * Fire-and-forget logout notification, only meaningful for a session that
     * actually authenticated. Failures are logged but never surfaced to the
     * player - a missed logout row just means one playtime session is
     * incomplete, it shouldn't affect the player leaving the server.
     */
    public CompletableFuture<Boolean> logout(String username, UUID uuid) {
        String url = baseUrl + logoutPath;
        String jsonBody = "{\"username\":\"" + escapeJson(username) + "\","
                + "\"uuid\":\"" + escapeJson(uuid.toString()) + "\"}";

        HttpRequest request = baseRequestBuilder(url)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        logger.warning("logout API returned HTTP " + response.statusCode() + " for '" + username + "'");
                        return false;
                    }
                    return parseBooleanField(response.body(), "ok").orElse(false);
                })
                .exceptionally(ex -> {
                    logFailure("logout", username, ex);
                    return false;
                });
    }

    private HttpRequest.Builder baseRequestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", apiKey);
    }

    private Optional<Boolean> parseBooleanField(String body, String field) {
        if (body == null || body.isEmpty()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*(true|false)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return Optional.of(Boolean.parseBoolean(matcher.group(1)));
        }
        return Optional.empty();
    }

    private String parseStringField(String body, String field) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void logFailure(String operation, String username, Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof HttpTimeoutException) {
            logger.warning(operation + " timed out for '" + username + "'");
        } else {
            logger.log(Level.WARNING, operation + " failed for '" + username + "'", cause);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String stripTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url == null ? "" : url;
    }
}
