/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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

package eu.geopaparazzi.library.color;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import eu.geopaparazzi.library.R;

import static eu.geopaparazzi.library.forms.FormUtilities.COLON;
import static eu.geopaparazzi.library.forms.FormUtilities.UNDERSCORE;

// class for the Select Line Width dialog
public class ColorStrokeDialogFragment extends DialogFragment {
    private final static String PREFS_KEY_COLORPROPERTIES = "PREFS_KEY_COLORPROPERTIES";
    private final static String PREFS_KEY_COLORPROPERTIES_FLAGS = "PREFS_KEY_COLORPROPERTIES_FLAGS";

    private ImageView mWidthImageView;
    private ColorStrokeObject mCurrentColorStrokeObject;

    private TextView mWidthTextView;
    private IColorStrokePropertiesChangeListener colorStrokePropertiesChangeListener;
    private SeekBar mWidthSeekBar;
    private boolean handlingFillColor;
    private SeekBar mAlphaSeekBar;
    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private View mColorView;

    /**
     * Create a dialog instance.
     *
     * @param colorStrokeObject object holding color and stroke info.
     * @return the instance.
     */
    public static ColorStrokeDialogFragment newInstance(ColorStrokeObject colorStrokeObject) {
        ColorStrokeDialogFragment f = new ColorStrokeDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(PREFS_KEY_COLORPROPERTIES, colorStrokeObject);
        f.setArguments(args);
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ColorStrokeObject mInitialColorStrokeObject = (ColorStrokeObject) getArguments().getSerializable(PREFS_KEY_COLORPROPERTIES);
        if (mInitialColorStrokeObject != null) {
            mCurrentColorStrokeObject = mInitialColorStrokeObject.duplicate();
        }
    }

    // create an AlertDialog and return it
    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        // create the dialog
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View colorStrokeDialogView =
                getActivity().getLayoutInflater().inflate(
                        R.layout.fragment_dialog_color_stroke, null);
        builder.setView(colorStrokeDialogView); // add GUI to dialog

        if (!mCurrentColorStrokeObject.hasStrokeWidth) {
            View lineWidthView = colorStrokeDialogView.findViewById(R.id.lineWidthDialogGridLayout);
            lineWidthView.setVisibility(View.GONE);
        } else {
            mWidthImageView = (ImageView) colorStrokeDialogView.findViewById(R.id.widthImageView);
            mWidthTextView = (TextView) colorStrokeDialogView.findViewById(R.id.widthTextView);
            mWidthSeekBar = (SeekBar) colorStrokeDialogView.findViewById(R.id.widthSeekBar);

            mWidthSeekBar.setOnSeekBarChangeListener(lineWidthChanged);
            mWidthSeekBar.setProgress(mCurrentColorStrokeObject.strokeWidth);
            mWidthTextView.setText(String.valueOf(mCurrentColorStrokeObject.strokeWidth));
        }

        RadioButton fillRadioButton = (RadioButton) colorStrokeDialogView.findViewById(R.id.doFillRadioButton);
        RadioButton strokeRadioButton = (RadioButton) colorStrokeDialogView.findViewById(R.id.doStrokeRadioButton);
        TextView title2 = (TextView) colorStrokeDialogView.findViewById(R.id.title2);
        if (!mCurrentColorStrokeObject.hasFill && !mCurrentColorStrokeObject.hasStroke) {
            View colorView = colorStrokeDialogView.findViewById(R.id.colorDialogGridLayout);
            colorView.setVisibility(View.GONE);
        } else {
            if (!mCurrentColorStrokeObject.hasFill || !mCurrentColorStrokeObject.hasStroke) {
                View radioView = colorStrokeDialogView.findViewById(R.id.radioDoLayout);
                radioView.setVisibility(View.GONE);
            }
            handlingFillColor = mCurrentColorStrokeObject.hasFill;
            String title = "";
            if (mCurrentColorStrokeObject.hasFill && mCurrentColorStrokeObject.hasStroke) {
                title = "Fill and Stroke color";
            } else if (mCurrentColorStrokeObject.hasFill) {
                title = "Fill color";
            } else {
                title = "Stroke color";
            }
            title2.setText(title);

            // get the color SeekBars and set their onChange listeners
            mAlphaSeekBar = (SeekBar) colorStrokeDialogView.findViewById(R.id.alphaSeekBar);
            mRedSeekBar = (SeekBar) colorStrokeDialogView.findViewById(R.id.redSeekBar);
            mGreenSeekBar = (SeekBar) colorStrokeDialogView.findViewById(R.id.greenSeekBar);
            mBlueSeekBar = (SeekBar) colorStrokeDialogView.findViewById(R.id.blueSeekBar);
            mColorView = colorStrokeDialogView.findViewById(R.id.colorView);

            // register SeekBar event listeners
            mAlphaSeekBar.setOnSeekBarChangeListener(colorChangedListener);
            mRedSeekBar.setOnSeekBarChangeListener(colorChangedListener);
            mGreenSeekBar.setOnSeekBarChangeListener(colorChangedListener);
            mBlueSeekBar.setOnSeekBarChangeListener(colorChangedListener);

            int color = 0;
            int alpha = 0;
            if (handlingFillColor) {
                color = mCurrentColorStrokeObject.fillColor;
                alpha = mCurrentColorStrokeObject.fillAlpha;
            } else {
                color = mCurrentColorStrokeObject.strokeColor;
                alpha = mCurrentColorStrokeObject.strokeAlpha;
            }
            // use current drawing color to set SeekBar values
            mAlphaSeekBar.setProgress(alpha);
            mRedSeekBar.setProgress(Color.red(color));
            mGreenSeekBar.setProgress(Color.green(color));
            mBlueSeekBar.setProgress(Color.blue(color));


            ColorUtilities[] availableColors = ColorUtilities.values();
            GridLayout gridLayout = (GridLayout) colorStrokeDialogView.findViewById(R.id.availableColors);

            int count = 0;
            for (int col = 0; col < 7; col++) {
                for (int row = 0; row < 3; row++) {
                    if (count > 21) break;
                    GridLayout.Spec colSpec = GridLayout.spec(col, GridLayout.BASELINE);
                    GridLayout.Spec rowSpec = GridLayout.spec(row);

                    final Button button = new Button(getActivity());
                    button.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    button.setPadding(1, 1, 1, 1);

//                    int minTouchSize = 5;//(int) getResources().getDimension(R.dimen.min_touch_size);
//                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(minTouchSize,
//                            minTouchSize);
//                    button.setLayoutParams(layoutParams);

                    String hex = availableColors[count].getHex();
                    int c = ColorUtilities.toColor(hex);
                    button.setBackgroundColor(c);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int color = Color.TRANSPARENT;
                            Drawable background = button.getBackground();
                            if (background instanceof ColorDrawable)
                                color = ((ColorDrawable) background).getColor();

                            int red = Color.red(color);
                            int green = Color.green(color);
                            int blue = Color.blue(color);
                            int argb = Color.argb(red, green, blue, mAlphaSeekBar.getProgress());
                            mColorView.setBackgroundColor(argb);
                            mRedSeekBar.setProgress(red);
                            mGreenSeekBar.setProgress(green);
                            mBlueSeekBar.setProgress(blue);

                            if (handlingFillColor) {
                                mCurrentColorStrokeObject.fillColor = Color.rgb(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(),
                                        mBlueSeekBar.getProgress());
                                mCurrentColorStrokeObject.fillAlpha = mAlphaSeekBar.getProgress();
                            } else {
                                mCurrentColorStrokeObject.strokeColor = Color.rgb(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(),
                                        mBlueSeekBar.getProgress());
                                mCurrentColorStrokeObject.strokeAlpha = mAlphaSeekBar.getProgress();
                            }
                        }
                    });

                    gridLayout.addView(button, new GridLayout.LayoutParams(rowSpec, colSpec));
                    count++;
                }
            }
        }

        // add Set Line Width Button
        builder.setPositiveButton("Set properties",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (colorStrokePropertiesChangeListener != null) {
                            colorStrokePropertiesChangeListener.onPropertiesChanged(mCurrentColorStrokeObject);
                        }
                    }
                }
        );
        builder.setNegativeButton(getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }
        );

        return builder.create(); // return dialog
    }


    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        if (view.getId() == R.id.doFillRadioButton && checked) {
            handlingFillColor = true;
        }
        if (view.getId() == R.id.doStrokeRadioButton && checked) {
            handlingFillColor = false;
        }

        int fill;
        int alpha;
        if (handlingFillColor) {
            fill = mCurrentColorStrokeObject.fillColor;
            alpha = mCurrentColorStrokeObject.fillAlpha;
        } else {
            fill = mCurrentColorStrokeObject.strokeColor;
            alpha = mCurrentColorStrokeObject.strokeAlpha;
        }
        int red = Color.red(fill);
        int green = Color.green(fill);
        int blue = Color.blue(fill);
        int argb = Color.argb(red, green, blue, alpha);
        mColorView.setBackgroundColor(argb);
        mAlphaSeekBar.setProgress(alpha);
        mRedSeekBar.setProgress(red);
        mGreenSeekBar.setProgress(green);
        mBlueSeekBar.setProgress(blue);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof IColorStrokePropertiesChangeListener) {
            colorStrokePropertiesChangeListener = (IColorStrokePropertiesChangeListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        colorStrokePropertiesChangeListener = null;
    }

    private final OnSeekBarChangeListener lineWidthChanged =
            new OnSeekBarChangeListener() {
                final Bitmap bitmap = Bitmap.createBitmap(
                        400, 100, Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(bitmap); // draws into bitmap

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    mCurrentColorStrokeObject.strokeWidth = progress;

                    int tmpColor = Color.BLACK;
//                    if (mCurrentColorStrokeObject.hasStroke)
//                        tmpColor = mCurrentColorStrokeObject.strokeColor;
                    // configure a Paint object for the current SeekBar value
                    Paint p = new Paint();
                    p.setColor(tmpColor);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    p.setStrokeWidth(progress);

                    // erase the bitmap and redraw the line
                    bitmap.eraseColor(getResources().getColor(android.R.color.transparent, getContext().getTheme()));
                    canvas.drawLine(30, 50, 370, 50, p);
                    mWidthImageView.setImageBitmap(bitmap);
                    mWidthTextView.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                } // required

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                } // required
            };

    private final OnSeekBarChangeListener colorChangedListener =
            new OnSeekBarChangeListener() {
                // display the updated color
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {

//                    if (fromUser) {// user, not program, changed SeekBar progress
                    int color = Color.argb(mAlphaSeekBar.getProgress(),
                            mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(),
                            mBlueSeekBar.getProgress());
                    mColorView.setBackgroundColor(color);

                    if (handlingFillColor) {
                        mCurrentColorStrokeObject.fillColor = Color.rgb(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(),
                                mBlueSeekBar.getProgress());
                        mCurrentColorStrokeObject.fillAlpha = mAlphaSeekBar.getProgress();
                    } else {
                        mCurrentColorStrokeObject.strokeColor = Color.rgb(mRedSeekBar.getProgress(), mGreenSeekBar.getProgress(),
                                mBlueSeekBar.getProgress());
                        mCurrentColorStrokeObject.strokeAlpha = mAlphaSeekBar.getProgress();
                    }
//                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                } // required

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                } // required
            };

}
