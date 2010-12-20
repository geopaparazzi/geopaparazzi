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
package eu.hydrologis.geopaparazzi.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import eu.hydrologis.geopaparazzi.R;

/**
 * The Dashboard view
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * 
 */
public class Dashboard extends ViewGroup {
    private static final int DEFAULT_COLS = 5;

    private int cols = DEFAULT_COLS;
    /**
     * max width of the view
     */
    private int maxWidth;
    /**
     * max height of the view
     */
    private int maxHeight;

    private int defaultWidth = 100;
    private int defaultHeight = 100;

    /**
     * @param context
     */
    public Dashboard( Context context ) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public Dashboard( Context context, AttributeSet attrs ) {
        super(context, attrs);

        TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.Dashboard);
        cols = styledAttributes.getInt(R.styleable.Dashboard_cols, DEFAULT_COLS);
        styledAttributes.recycle();

    }

    @Override
    protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
        // dimensioni preferite del dashboard
        int preferredDashboardWidth = 0;
        int preferredDashboardHeight = 0;

        // vincoli del container
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
        int availableWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        // int availableHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        // prendiamo come preferita quella checi viene passata
        preferredDashboardWidth = availableWidth;

        // calculate the space related to width for every button
        int buttonWidthAvailable = 0;
        switch( availableWidthMode ) {
        case MeasureSpec.AT_MOST:
        case MeasureSpec.EXACTLY:
            buttonWidthAvailable = availableWidth / cols;
            break;
        case MeasureSpec.UNSPECIFIED:
            buttonWidthAvailable = defaultWidth / cols;
            break;
        }

        int maxChildHeight = 0;
        for( int i = 0; i < getChildCount(); i++ ) {
            View child = getChildAt(i);
            // LayoutParams viewLayoutParams = (LayoutParams) child
            // .getLayoutParams();
            int childWidthSpec = MeasureSpec.makeMeasureSpec(buttonWidthAvailable, MeasureSpec.AT_MOST);
            // in height delegate height decision to button (do as you wish)
            int childHeightSpec = MeasureSpec.makeMeasureSpec(buttonWidthAvailable, MeasureSpec.UNSPECIFIED);

            child.measure(childWidthSpec, childHeightSpec);

            // dimensione che la view mi consiglia in seguito alle dimensioni
            // passate
            int tmpHeight = child.getMeasuredHeight();
            if (tmpHeight > maxChildHeight) {
                maxChildHeight = tmpHeight;
            }

        }
        // preferredDashboardHeight = availableHeight;
        if (getChildCount() <= cols) {
            preferredDashboardHeight = maxChildHeight;
        } else if (getChildCount() % cols == 0) {
            preferredDashboardHeight = maxChildHeight * getChildCount() / cols;
        } else {
            preferredDashboardHeight = maxChildHeight * (getChildCount() / cols + 1);
        }

        // comunico al container della dashboard le dimensioni preferite
        setMeasuredDimension(preferredDashboardWidth, preferredDashboardHeight);

    }
    protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
        // dobbiamo dire ad ogni figlio dove mettersi in termini di altezza,
        // larghezza, etc
        for( int i = 0; i < getChildCount(); i++ ) {
            View child = getChildAt(i);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            int tmpWidth = getMeasuredWidth() / cols;
            child.layout((i % cols) * tmpWidth, (i / cols) * childHeight, (i % cols) * tmpWidth + tmpWidth, (i / cols)
                    * childHeight + childHeight);

        }

    }

    public class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * custom attribute colnum
         */
        private int cols;

        public LayoutParams( Context context, AttributeSet atts ) {
            super(context, atts);

            TypedArray styledAttributes = context.obtainStyledAttributes(atts, R.styleable.Dashboard);
            cols = styledAttributes.getInt(R.styleable.Dashboard_cols, DEFAULT_COLS);
            styledAttributes.recycle();
        }

        public LayoutParams( int width, int height ) {
            super(width, height);
            cols = DEFAULT_COLS;
        }

        public LayoutParams( android.view.ViewGroup.LayoutParams layoutParams ) {
            super(layoutParams);
            if (layoutParams instanceof Dashboard.LayoutParams) {
                Dashboard.LayoutParams dashLayoutParams = (Dashboard.LayoutParams) layoutParams;
                cols = dashLayoutParams.cols;
            } else {
                cols = DEFAULT_COLS;
            }
        }

    }
}
