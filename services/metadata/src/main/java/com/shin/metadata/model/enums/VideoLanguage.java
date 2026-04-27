package com.shin.metadata.model.enums;

public enum VideoLanguage {
    ENGLISH("English"),
    MANDARIN_CHINESE("Mandarin Chinese"),
    HINDI("Hindi"),
    SPANISH("Spanish"),
    FRENCH("French"),
    MODERN_STANDARD_ARABIC("Arabic"),
    BENGALI("Bengali"),
    PORTUGUESE("Portuguese"),
    RUSSIAN("Russian"),
    URDU("Urdu"),
    INDONESIAN("Indonesian"),
    GERMAN("German"),
    JAPANESE("Japanese"),
    SWAHILI("Swahili"),
    MARATHI("Marathi"),
    TELUGU("Telugu"),
    TURKISH("Turkish"),
    TAMIL("Tamil"),
    VIETNAMESE("Vietnamese"),
    KOREAN("Korean"),
    ITALIAN("Italian"),
    THAI("Thai"),
    PUNJABI("Punjabi"),
    JAVANESE("Javanese"),
    PERSIAN("Persian"),
    POLISH("Polish"),
    DUTCH("Dutch"),
    GREEK("Greek"),
    HEBREW("Hebrew"),
    CZECH("Czech"),
    ROMANIAN("Romanian"),
    HUNGARIAN("Hungarian"),
    SWEDISH("Swedish"),
    NORWEGIAN("Norwegian"),
    DANISH("Danish"),
    FINNISH("Finnish"),
    UKRAINIAN("Ukrainian"),
    MALAY("Malay"),
    FILIPINO("Filipino"),
    BURMESE("Burmese");

    private final String displayName;

    VideoLanguage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
