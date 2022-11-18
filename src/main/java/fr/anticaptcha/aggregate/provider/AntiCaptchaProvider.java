package fr.anticaptcha.aggregate.provider;

import fr.anticaptcha.aggregate.enums.CaptchaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AntiCaptchaProvider {

    @Nullable String solve(@NotNull final String captchaKey,
                           @NotNull final String captchaUrl,
                           @NotNull final CaptchaType captchaType);

}
