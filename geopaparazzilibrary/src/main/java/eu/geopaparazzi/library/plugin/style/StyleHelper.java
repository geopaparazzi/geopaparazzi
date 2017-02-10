package eu.geopaparazzi.library.plugin.style;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

/**
 * Created by hydrologis on 10/02/17.
 */
public class StyleHelper {


    public static LinearLayout.LayoutParams styleButton(Context context, Button button) {
        button.setTextColor(Compat.getColor(context, R.color.main_text_color));
        Compat.setButtonTextAppearance(context, button, android.R.attr.textAppearanceMedium);
        button.setBackground(Compat.getDrawable(context, R.drawable.button_background_drawable));
        int pad = (int) context.getResources().getDimension(R.dimen.button_indent);
        button.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(15, 15, 15, 15);
        return lp;
    }
}
