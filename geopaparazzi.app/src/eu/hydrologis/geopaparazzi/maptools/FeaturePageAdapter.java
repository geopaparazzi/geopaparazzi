/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maptools;

import java.util.List;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import eu.geopaparazzi.library.R;

/**
 * Page adapter for features.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeaturePageAdapter extends PagerAdapter {
    private Context context;
    private List<Feature> featuresList;
    private boolean isReadOnly;

    /**
     * Constructor.
     * 
     * @param context the {@link Context} to use.
     * @param featuresList the list of features to show.
     * @param isReadOnly if <code>true</code>, the adapter will be initially readonly.
     */
    public FeaturePageAdapter( Context context, List<Feature> featuresList, boolean isReadOnly ) {
        super();
        this.context = context;
        this.featuresList = featuresList;
        this.isReadOnly = isReadOnly;
    }

    /**
     * Mark the adapter as readonly.
     * 
     * @param isReadOnly if <code>true</code>, then editing will be disabled.
     */
    public void setReadOnly( boolean isReadOnly ) {
        this.isReadOnly = isReadOnly;
    }

    @Override
    public int getCount() {
        return featuresList.size();
    }

    @Override
    public Object instantiateItem( ViewGroup container, int position ) {
        Feature feature = featuresList.get(position);

        int bgColor = context.getResources().getColor(R.color.formbgcolor);
        int textColor = context.getResources().getColor(R.color.formcolor);

        ScrollView scrollView = new ScrollView(context);
        ScrollView.LayoutParams scrollLayoutParams = new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        scrollLayoutParams.setMargins(10, 10, 10, 10);
        scrollView.setLayoutParams(scrollLayoutParams);
        scrollView.setBackgroundColor(bgColor);

        LinearLayout linearLayoutView = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        int margin = 10;
        layoutParams.setMargins(margin, margin, margin, margin);
        linearLayoutView.setLayoutParams(layoutParams);
        linearLayoutView.setOrientation(LinearLayout.VERTICAL);
        int padding = 10;
        linearLayoutView.setPadding(padding, padding, padding, padding);
        scrollView.addView(linearLayoutView);

        List<String> attributeNames = feature.getAttributeNames();
        List<String> attributeValues = feature.getAttributeValuesStrings();
        for( int i = 0; i < attributeNames.size(); i++ ) {
            String name = attributeNames.get(i);
            String value = attributeValues.get(i);

            TextView textView = new TextView(context);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            textView.setPadding(padding, padding, padding, padding);
            textView.setText(name);
            textView.setTextColor(textColor);
            // textView.setTextAppearance(context, android.R.style.TextAppearance_Medium);

            linearLayoutView.addView(textView);

            EditText editView = new EditText(context);
            LinearLayout.LayoutParams editViewParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            editViewParams.setMargins(margin, 0, margin, 0);
            editView.setLayoutParams(editViewParams);
            editView.setPadding(padding * 2, padding, padding * 2, padding);
            editView.setText(value);
            editView.setEnabled(!isReadOnly);
            // editView.setTextAppearance(context, android.R.style.TextAppearance_Medium);

            switch( 0 ) {
            case 1:
                editView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case 2:
                editView.setInputType(InputType.TYPE_CLASS_PHONE);
                break;
            case 3:
                editView.setInputType(InputType.TYPE_CLASS_DATETIME);
                break;
            case 4:
                editView.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            default:
                break;
            }

            linearLayoutView.addView(editView);
        }
        container.addView(scrollView);

        return scrollView;
    }

    @Override
    public void destroyItem( ViewGroup container, int position, Object object ) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject( View view, Object object ) {
        return view == object;
    }

}
