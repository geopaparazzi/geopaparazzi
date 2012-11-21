package eu.geopaparazzi.library.database.spatial;

public class Style {
    public String name;
    public float size = 3;
    public String fillcolor = "red";
    public String strokecolor = "black";
    public float fillalpha = 0.5f;
    public float strokealpha = 1.0f;
    public String shape = "square";
    public float width = 2f;
    public float textsize = 5f;
    public String textfield = "";
    public int enabled = 0;

    public String insertValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        sb.append(name);
        sb.append("', ");
        sb.append(size);
        sb.append(", '");
        sb.append(fillcolor);
        sb.append("', '");
        sb.append(strokecolor);
        sb.append("', ");
        sb.append(fillalpha);
        sb.append(", ");
        sb.append(strokealpha);
        sb.append(", '");
        sb.append(shape);
        sb.append("', ");
        sb.append(width);
        sb.append(", ");
        sb.append(textsize);
        sb.append(", ");
        sb.append(textfield);
        sb.append(", ");
        sb.append(enabled);
        return sb.toString();
    }

}
