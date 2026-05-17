package com.chrionline.server.services;

/**
 * Interface commune pour les services de validation CAPTCHA.
 *
 * Permet d'injecter différentes implémentations (reCAPTCHA Google,
 * hCaptcha, mode test désactivé, etc.) sans modifier le code appelant.
 *
 * Usage dans les contrôleurs :
 * <pre>
 *   CaptchaValidator captchaValidator = new CaptchaService();
 *   if (!captchaValidator.validate(token)) {
 *       return AppResponse.error("CAPTCHA invalide.");
 *   }
 * </pre>
 */
public interface CaptchaValidator {

    /**
     * Valide un token CAPTCHA soumis par le client.
     *
     * @param token le token CAPTCHA à vérifier (non nul, non vide)
     * @return {@code true} si le token est valide, {@code false} sinon
     */
    boolean validate(String token);
}
