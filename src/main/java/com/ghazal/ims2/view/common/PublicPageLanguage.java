package com.ghazal.ims2.view.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.LocaleResolver;
import jakarta.servlet.http.Cookie;

import java.util.Locale;

/**
 * Resolves and applies the language of the public pages (landing page and privacy policy), keeping it in sync with the
 * Jmix UI language so that other pages, such as the login page, use the same language.
 */
public final class PublicPageLanguage {

    private static final String LOCALE_COOKIE = "LAST_LOCALE";
    private static final int COOKIE_MAX_AGE = 31536000;
    private static final Locale ARABIC = new Locale("ar");
    private static final Locale ENGLISH = Locale.ENGLISH;

    private PublicPageLanguage() {
    }

    /**
     * @return {@code true} if the URL itself selects a language ("en" or "ar" path segment)
     */
    public static boolean isExplicit(String path) {
        return matches(path, "en") || matches(path, "ar");
    }

    /**
     * English URLs are always English, Arabic URLs always Arabic. Any other URL follows the language the visitor
     * selected before; Arabic is the default.
     */
    public static boolean isEnglish(String path) {
        if (matches(path, "en")) {
            return true;
        }
        if (matches(path, "ar")) {
            return false;
        }
        return "en".equalsIgnoreCase(rememberedLanguage());
    }

    /**
     * Makes the current session use the given language, so the login page and other views follow it. An explicit
     * language in the URL is also remembered for later visits.
     */
    public static void apply(boolean english, boolean remember) {
        Locale locale = english ? ENGLISH : ARABIC;

        if (UI.getCurrent() != null) {
            UI.getCurrent().setLocale(locale);
        }
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setLocale(locale);
        }
        if (remember) {
            writeLocaleCookie(locale);
        }
    }

    private static boolean matches(String path, String language) {
        return path.equals(language) || path.startsWith(language + "/");
    }

    /**
     * Stores an explicitly chosen language so that pages without a language in their URL follow it.
     */
    public static void remember(Locale locale) {
        writeLocaleCookie(locale);
    }

    private static String rememberedLanguage() {
        String value = readLocaleCookie();
        if (value == null || value.isBlank()) {
            return "ar";
        }
        return LocaleResolver.resolve(value).getLanguage();
    }

    private static String readLocaleCookie() {
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request instanceof VaadinServletRequest servletRequest) {
            Cookie[] cookies = servletRequest.getHttpServletRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (LOCALE_COOKIE.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }
        return null;
    }

    private static void writeLocaleCookie(Locale locale) {
        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response instanceof VaadinServletResponse servletResponse) {
            Cookie cookie = new Cookie(LOCALE_COOKIE, LocaleResolver.localeToString(locale));
            cookie.setPath("/");
            cookie.setMaxAge(COOKIE_MAX_AGE);
            servletResponse.getHttpServletResponse().addCookie(cookie);
        }
    }
}
