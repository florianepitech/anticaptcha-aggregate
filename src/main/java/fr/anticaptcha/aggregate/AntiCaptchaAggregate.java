package fr.anticaptcha.aggregate;

import fr.anticaptcha.aggregate.enums.CaptchaType;
import fr.anticaptcha.aggregate.provider.AntiCaptchaProvider;
import fr.anticaptcha.aggregate.provider.anticaptchacom.AntiCaptchaComProvider;
import net.httpclient.wrapper.session.HttpClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AntiCaptchaAggregate {

    public static final Logger logger = LogManager.getLogger(AntiCaptchaAggregate.class);

    public AntiCaptchaAggregate() {

    }

    /**
     * This method solve a captcha with all provider available.
     * @param captchaKey The captcha key of the website.
     * @param captchaUrl The captcha url of the website.
     * @param captchaType The captcha type of the website.
     * @return The captcha solution.
     */
    public @Nullable String solve(@NotNull final String captchaKey,
                                  @NotNull final String captchaUrl,
                                  @NotNull final CaptchaType captchaType) {
        List<AntiCaptchaProvider> providers = getAllProviders();
        for (AntiCaptchaProvider provider : providers) {
            String result = provider.solve(captchaKey, captchaUrl, captchaType);
            if (result != null)
                return (result);
        }
        return (null);
    }

    public @NotNull Map<String, Float> getBalances() {
        HttpClientSession httpClientSession = new HttpClientSession();
        List<AntiCaptchaProvider> providers = getAllProviders();
        Map<String, Float> balances = new HashMap<>();
        for (AntiCaptchaProvider provider : providers) {
            Float balance = provider.getBalance(httpClientSession);
            balances.put(provider.getProviderName(), balance);
            if (balance == null)
                logger.warn("Impossible to get balance for {}", provider.getProviderName());
        }
        return (balances);
    }

    /*
     $      Private methods
     */

    private @NotNull List<AntiCaptchaProvider> getAllProviders() {
        List<AntiCaptchaProvider> providers = new ArrayList<>();
        providers.add(new AntiCaptchaComProvider());
        return (providers);
    }

}
