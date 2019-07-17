package eu.geopaparazzi.map;

public enum GPMapThemes {
    DEFAULT("Default"),
    MAPZEN("Mapzen"),
    NEWTRON("Newtron"),
    OPENMAPTILES("Openmaptiles"),
    OSMAGRAY("Osmgray"),
    OSMARENDER("Osmarender"),
    TRONRENDER("Tubes");

    private String themeLabel;

    GPMapThemes(String themeLabel) {
        this.themeLabel = themeLabel;
    }

    public String getThemeLabel() {
        return themeLabel;
    }

    public static GPMapThemes fromLabel(String label) {
        for (GPMapThemes theme : values()) {
            if (theme.themeLabel.equals(label))
                return theme;
        }
        return DEFAULT;
    }
}
