package fr.anticaptcha.aggregate;

import fr.anticaptcha.aggregate.enums.CaptchaType;
import fr.anticaptcha.aggregate.provider.AntiCaptchaProvider;
import fr.anticaptcha.aggregate.provider.anticaptchacom.AntiCaptchaComProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    /*
     $      Private methods
     */

    private @NotNull List<AntiCaptchaProvider> getAllProviders() {
        List<AntiCaptchaProvider> providers = new ArrayList<>();
        providers.add(new AntiCaptchaComProvider());
        return (providers);
    }

}
