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
package eu.hydrologis.geopaparazzi.osm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class TransparentPanel extends RelativeLayout {
    private Paint innerPaint, borderPaint;

    public TransparentPanel( Context context, AttributeSet attrs ) {
        super(context, attrs);
        init();
    }

    public TransparentPanel( Context context ) {
        super(context);
        init();
    }

    private void init() {
        innerPaint = new Paint();
        innerPaint.setARGB(0, 75, 75, 75); // gray
        // innerPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setARGB(0, 255, 255, 255);
        // borderPaint.setAntiAlias(true);
        // borderPaint.setStyle(Style.STROKE);
        // borderPaint.setStrokeWidth(2);
    }

    public void setInnerPaint( Paint innerPaint ) {
        this.innerPaint = innerPaint;
    }

    public void setBorderPaint( Paint borderPaint ) {
        this.borderPaint = borderPaint;
    }

    @Override
    protected void dispatchDraw( Canvas canvas ) {

        RectF drawRect = new RectF();
        drawRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());

        canvas.drawRoundRect(drawRect, 0, 0, innerPaint);
        canvas.drawRoundRect(drawRect, 0, 0, borderPaint);

        super.dispatchDraw(canvas);
    }
}