package org.example.client.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class LocaleManager {
    private static final String BUNDLE_BASE_NAME = "messages";
    private static ResourceBundle bundle;

    public static final Locale RU = new Locale("ru", "RU");
    public static final Locale MK = new Locale("mk", "MK");
    public static final Locale DA = new Locale("da", "DK");
    public static final Locale ES_EC = new Locale("es", "EC");

    static {
        setLocale(RU);
    }

    public static void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
    }

    public static String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public static String getFormattedString(String key, Object... args) {
        String pattern = getString(key);
        return MessageFormat.format(pattern, args);
    }

    public static Locale getCurrentLocale() {
        return bundle.getLocale();
    }
}