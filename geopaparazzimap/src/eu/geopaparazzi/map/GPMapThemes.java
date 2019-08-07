package eu.geopaparazzi.map;

@SuppressWarnings("ALL")
public enum GPMapThemes {
    DEFAULT("Default"),//NON-NLS
    MAPZEN("Mapzen"),//NON-NLS
    NEWTRON("Newtron"),//NON-NLS
    OPENMAPTILES("Openmaptiles"),//NON-NLS
    OSMAGRAY("Osmgray"),//NON-NLS
    OSMARENDER("Osmarender"),//NON-NLS
    TRONRENDER("Tubes");//NON-NLS

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
