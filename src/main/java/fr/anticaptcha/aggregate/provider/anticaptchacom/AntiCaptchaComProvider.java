package fr.anticaptcha.aggregate.provider.anticaptchacom;

import fr.anticaptcha.aggregate.provider.AntiCaptchaProvider;
import fr.anticaptcha.aggregate.enums.CaptchaType;
import net.httpclient.wrapper.exception.HttpClientException;
import net.httpclient.wrapper.exception.HttpServerException;
import net.httpclient.wrapper.ratelimiter.RateLimiter;
import net.httpclient.wrapper.response.RequestResponse;
import net.httpclient.wrapper.session.HttpClientSession;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;

import static fr.anticaptcha.aggregate.AntiCaptchaAggregate.logger;

/**
 * This provider implement the <a href="https://anti-captcha.com/">anti-captcha.com</a> API.
 */
public class AntiCaptchaComProvider implements AntiCaptchaProvider {

    private final int MAX_ATTEMPTS = 100;

    @NotNull
    private final HttpClientSession httpClientSession;

    @NotNull
    private final RateLimiter rateLimiter;

    public AntiCaptchaComProvider() {
        httpClientSession = new HttpClientSession();
        rateLimiter = new RateLimiter(Duration.ofSeconds(2));
    }

    @Override
    public @Nullable String solve(@NotNull String captchaKey,
                                  @NotNull String captchaUrl,
                                  @NotNull CaptchaType captchaType) {
        logger.debug("Start solving captcha with anti-captcha.com");
        String apiKey = getApiKey();
        if (apiKey == null) {
            logger.warn("No API key found for anti-captcha.com");
            return (null);
        }
        logger.trace("API key found for anti-captcha.com");
        if (captchaType != CaptchaType.RECAPTCHA_V2) {
            logger.warn("Captcha type not supported by anti-captcha.com");
            return (null);
        }
        Integer taskId = startTask(captchaKey, captchaUrl);
        if (taskId == null) {
            logger.warn("Impossible to start task on anti-captcha.com");
            return (null);
        }
        logger.debug("Task started on anti-captcha.com with id {}", taskId);
        String solution = getCaptchaSolution(taskId, 1);
        if (solution == null) {
            logger.warn("Impossible to get captcha solution from anti-captcha.com");
            return (solve(captchaKey, captchaUrl, captchaType));
        }
        logger.debug("Captcha solution found {} on anti-captcha.com with task id {}", solution, taskId);
        return (solution);
    }

    /*
     $      Private methods
     */

    /**
     * This method start the task on the anti-captcha.com API only
     * for the captcha type RECAPTCHA_V2. (Proxyless)
     * @param captchaKey The captcha key of the website.
     * @param captchaUrl The captcha url of the website.
     * @return The task id.
     */
    private @Nullable Integer startTask(@NotNull String captchaKey,
                                        @NotNull String captchaUrl) {
        logger.debug("Start task on anti-captcha.com");
        JSONObject request = new JSONObject();
        request.put("clientKey", getApiKey());
        request.put("task", new JSONObject()
                .put("type", "RecaptchaV2TaskProxyless")
                .put("websiteURL", captchaUrl)
                .put("websiteKey", captchaKey));
        request.put("softId", 0);
        try {
            RequestResponse response = httpClientSession.sendPost("https://api.anti-captcha.com/createTask",
                    request, ContentType.APPLICATION_JSON);
            JSONObject responseJson = response.toJSONObject();
            logger.trace("Request response start task on anti-captcha.com: {}", responseJson);
            if (responseJson.getInt("errorId") != 0) {
                logger.warn("Error while starting task on anti-captcha.com: {}", responseJson.getString(
                        "errorDescription"));
                return (null);
            }
            return responseJson.getInt("taskId");
        } catch (IOException | HttpServerException e) {
            httpClientSession.resetHttpClient();
            logger.error("Error while starting task on anti-captcha.com {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return (startTask(captchaKey, captchaUrl));
        } catch (HttpClientException e) {
            logger.error("Error while starting task on anti-captcha.com {}", e.getMessage(), e);
            return (null);
        }
    }

    /**
     * This method request the captcha solution on the anti-captcha.com API
     * for the task id given in parameter.
     * @param taskId The task id.
     * @return The captcha solution.
     */
    private @Nullable String getCaptchaSolution(@NotNull Integer taskId, int attempt) {
        if (attempt > MAX_ATTEMPTS) {
            logger.warn("Max attempts reached for anti-captcha.com for task id {}", taskId);
            return (null);
        }
        JSONObject request = new JSONObject();
        request.put("clientKey", getApiKey());
        request.put("taskId", taskId);
        rateLimiter.acquire();
        try {
            RequestResponse response = httpClientSession.sendPost("https://api.anti-captcha.com/getTaskResult",
                    request, ContentType.APPLICATION_JSON);
            JSONObject responseJson = response.toJSONObject();
            logger.trace("Request response get captcha solution on anti-captcha.com: {}", responseJson);
            if (responseJson.getInt("errorId") != 0) {
                logger.warn("Error while getting captcha solution from anti-captcha.com: {}", responseJson.getString(
                        "errorDescription"));
                return (null);
            }
            if (responseJson.getString("status").equals("processing")) {
                logger.trace("Attempt {}: captcha solution not ready yet on anti-captcha.com with task id {}," +
                                "waiting {} seconds before retrying...",
                        attempt, taskId, rateLimiter.getDuration().toSeconds());
                return (getCaptchaSolution(taskId, attempt + 1));
            }
            return (responseJson.getJSONObject("solution").getString("gRecaptchaResponse"));
        } catch (IOException | HttpServerException e) {
            httpClientSession.resetHttpClient();
            logger.error("Error while getting captcha solution from anti-captcha.com {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return (getCaptchaSolution(taskId, attempt + 1));
        } catch (HttpClientException e) {
            logger.error("Error while getting captcha solution from anti-captcha.com {}", e.getMessage(), e);
            return (null);
        }
    }

    @Override
    public @Nullable Float getBalance(@NotNull HttpClientSession httpClientSession) {
        try {
            RequestResponse response = httpClientSession.sendPost("https://api.anti-captcha.com/getBalance",
                    new JSONObject().put("clientKey", getApiKey()),
                    ContentType.APPLICATION_JSON);
            JSONObject responseJson = response.toJSONObject();
            logger.trace("Request response get balance on anti-captcha.com: {}", responseJson);
            if (responseJson.getInt("errorId") != 0) {
                logger.warn("Error while getting balance from anti-captcha.com: {}", responseJson.getString(
                        "errorDescription"));
                return (null);
            }
            return (responseJson.getFloat("balance"));
        } catch (IOException | HttpServerException e) {
            httpClientSession.resetHttpClient();
            logger.error("Error while getting balance from anti-captcha.com {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return (getBalance(httpClientSession));
        } catch (HttpClientException e) {
            logger.error("Error while getting balance from anti-captcha.com {}", e.getMessage(), e);
            return (null);
        }
    }

    @Override
    public @NotNull String getProviderName() {
        return ("anti-captcha.com");
    }

    /**
     * This method return the API key of the provider from
     * the property <b>anticaptcha.aggregate.provider.anticaptchacom.apikey</b>/
     * @return The API key of anti-captcha.com.
     */
    public @Nullable String getApiKey() {
        return (System.getProperty("anticaptcha.aggregate.provider.anticaptchacom.apikey"));
    }

}
