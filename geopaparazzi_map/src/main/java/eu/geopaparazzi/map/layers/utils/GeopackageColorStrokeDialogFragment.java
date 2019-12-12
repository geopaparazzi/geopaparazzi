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

package eu.geopaparazzi.map.layers.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.geopackage.android.GPGeopackageDb;
import org.hortonmachine.dbs.utils.BasicStyle;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.Compat;

/**
 * Class to set color and stroke for shapes.
 *
 * @author Andrea Antonello
 */
public class GeopackageColorStrokeDialogFragment extends DialogFragment {

    private final static String PREFS_KEY_COLORPROPERTIES = "PREFS_KEY_COLORPROPERTIES"; //NON-NLS

    private ImageView mWidthImageView;
    private ColorStrokeObject mCurrentColorStrokeObject;

    private TextView mWidthTextView;
    private SeekBar mWidthSeekBar;
    private boolean handlingFillColor;
    private SeekBar mAlphaSeekBar;
    private SeekBar mRedSeekBar;
    private SeekBar mGreenSeekBar;
    private SeekBar mBlueSeekBar;
    private View mColorView;
    //    private ImageView mShapeSizeImageView;
    private TextView mShapeSizeTextView;
    private SeekBar mShapeSizeSeekBar;
    private Spinner mShapeSpinner;

    /**
     * Create a dialog instance.
     *
     * @param colorStrokeObject object holding color and stroke info.
     * @return the instance.
     */
    public static GeopackageColorStrokeDialogFragment newInstance(ColorStrokeObject colorStrokeObject) {
        GeopackageColorStrokeDialogFragment f = new GeopackageColorStrokeDialogFragment();
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

        /*
         * the shape size image
         */
        if (!mCurrentColorStrokeObject.hasShape) {
            View shapeSizeView = colorStrokeDialogView.findViewById(R.id.shapeSizeDialogGridLayout);
            shapeSizeView.setVisibility(View.GONE);
        } else {
//            mShapeSizeImageView = (ImageView) colorStrokeDialogView.findViewById(R.id.shapeSizeImageView);
            mShapeSizeTextView = colorStrokeDialogView.findViewById(R.id.shapeSizeTextView);
            mShapeSizeSeekBar = colorStrokeDialogView.findViewById(R.id.shapeSizeSeekBar);

            mShapeSizeSeekBar.setOnSeekBarChangeListener(shapeSizeChanged);
            mShapeSizeSeekBar.setProgress(mCurrentColorStrokeObject.shapeSize);
            mShapeSizeTextView.setText(String.valueOf(mCurrentColorStrokeObject.shapeSize));


            mShapeSpinner = colorStrokeDialogView.findViewById(R.id.shape_spinner);
            int count = mShapeSpinner.getCount();
            for (int i = 0; i < count; i++) {
                if (mShapeSpinner.getItemAtPosition(i).equals(mCurrentColorStrokeObject.shapeWKT)) {
                    mShapeSpinner.setSelection(i);
                    break;
                }
            }
            mShapeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    Object selectedItem = mShapeSpinner.getSelectedItem();
                    String shapeStr = selectedItem.toString();
                    mCurrentColorStrokeObject.shapeWKT = shapeStr;
                }

                public void onNothingSelected(AdapterView<?> arg0) {
                    // ignore
                }
            });

        }

        /*
         * the stroke width image
         */
        if (!mCurrentColorStrokeObject.hasStrokeWidth) {
            View lineWidthView = colorStrokeDialogView.findViewById(R.id.lineWidthDialogGridLayout);
            lineWidthView.setVisibility(View.GONE);
        } else {
            mWidthImageView = colorStrokeDialogView.findViewById(R.id.widthImageView);
            mWidthTextView = colorStrokeDialogView.findViewById(R.id.widthTextView);
            mWidthSeekBar = colorStrokeDialogView.findViewById(R.id.widthSeekBar);

            mWidthSeekBar.setOnSeekBarChangeListener(lineWidthChanged);
            mWidthSeekBar.setProgress(mCurrentColorStrokeObject.strokeWidth);
            mWidthTextView.setText(String.valueOf(mCurrentColorStrokeObject.strokeWidth));
        }

        RadioButton fillRadioButton = colorStrokeDialogView.findViewById(R.id.doFillRadioButton);
        fillRadioButton.setChecked(true);
        RadioGroup radioGroup = colorStrokeDialogView.findViewById(R.id.radioDo);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                onRadioButtonClicked(checkedId);
            }
        });

        /*
         * The fill/stroke color picker part
         */
        TextView title2 = colorStrokeDialogView.findViewById(R.id.title2);
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
                title = getActivity().getString(R.string.fill_and_stroke_color_title);
            } else if (mCurrentColorStrokeObject.hasFill) {
                title = getActivity().getString(R.string.fill_color_title);
            } else {
                title = getActivity().getString(R.string.stroke_color_title);
            }
            title2.setText(title);

            // get the color SeekBars and set their onChange listeners
            mAlphaSeekBar = colorStrokeDialogView.findViewById(R.id.alphaSeekBar);
            mRedSeekBar = colorStrokeDialogView.findViewById(R.id.redSeekBar);
            mGreenSeekBar = colorStrokeDialogView.findViewById(R.id.greenSeekBar);
            mBlueSeekBar = colorStrokeDialogView.findViewById(R.id.blueSeekBar);
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


            final ColorUtilities[] availableColors = ColorUtilities.values();

            GridView gridview = colorStrokeDialogView.findViewById(R.id.availableColors);
            ArrayAdapter<ColorUtilities> colorsAdapter = new ArrayAdapter<ColorUtilities>(getActivity(), android.R.layout.simple_list_item_1, availableColors) {
                class ViewHolder {
                    Button button;
                }

                @Override
                public View getView(final int position, View cView, ViewGroup parent) {
                    ViewHolder holder;
                    View rowView = cView;
                    if (rowView == null) {
                        LayoutInflater inflater = getActivity().getLayoutInflater();
                        rowView = inflater.inflate(R.layout.fragment_dialog_color_stroke_row, parent, false);
                        holder = new ViewHolder();
                        holder.button = rowView.findViewById(R.id.button);
                        rowView.setTag(holder);
                    } else {
                        holder = (ViewHolder) rowView.getTag();
                    }
                    String hex = availableColors[position].getHex();
                    int color = ColorUtilities.toColor(hex);
                    holder.button.setBackgroundColor(color);
                    final Button b = holder.button;
                    holder.button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int color = Color.TRANSPARENT;
                            Drawable background = b.getBackground();
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

                    return rowView;
                }

            };
            gridview.setAdapter(colorsAdapter);

        }

        builder.setPositiveButton(R.string.set_properties,
                (dialog, id) -> {
                    try {
                        ASpatialDb db = GeopackageConnectionsHandler.INSTANCE.getDb(mCurrentColorStrokeObject.dbPath);
                        if (db instanceof GPGeopackageDb) {
                            GPGeopackageDb gpDb = (GPGeopackageDb) db;
                            BasicStyle style = gpDb.getBasicStyle(mCurrentColorStrokeObject.tableName);
                            style.fillcolor = ColorUtilities.getHex(mCurrentColorStrokeObject.fillColor);
                            style.fillalpha = mCurrentColorStrokeObject.fillAlpha / 255f;
                            style.strokecolor = ColorUtilities.getHex(mCurrentColorStrokeObject.strokeColor);
                            style.strokealpha = mCurrentColorStrokeObject.strokeAlpha / 255f;
                            style.width = mCurrentColorStrokeObject.strokeWidth;
                            style.shape = mCurrentColorStrokeObject.shapeWKT;
                            style.size = mCurrentColorStrokeObject.shapeSize;

                            gpDb.updateSimplifiedStyle(mCurrentColorStrokeObject.tableName, style.toString());
                        }
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                    }
                }
        );
        builder.setNegativeButton(getString(android.R.string.cancel),
                (dialog, id) -> {
                }
        );

        return builder.create(); // return dialog
    }


    public void onRadioButtonClicked(int id) {
        // Check which radio button was clicked
        handlingFillColor = id == R.id.doFillRadioButton;

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

    private final OnSeekBarChangeListener lineWidthChanged =
            new OnSeekBarChangeListener() {
                final Bitmap bitmap = Bitmap.createBitmap(
                        400, 100, Bitmap.Config.ARGB_8888);
                final Canvas canvas = new Canvas(bitmap); // draws into bitmap

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    if (progress == 0) progress = 1;
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
                    bitmap.eraseColor(Compat.getColor(getContext(), android.R.color.transparent));
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

    private final OnSeekBarChangeListener shapeSizeChanged =
            new OnSeekBarChangeListener() {

//                int centerX = 200;
//                int centerY = 50;
//                final Bitmap bitmap = Bitmap.createBitmap(centerX * 2, centerY * 2, Bitmap.Config.ARGB_8888);
//                final Canvas canvas = new Canvas(bitmap); // draws into bitmap

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    if (progress == 0) progress = 1;
                    mCurrentColorStrokeObject.shapeSize = progress;

//                    Paint p = new Paint();
//                    p.setColor(Color.BLACK);
//                    p.setStrokeCap(Paint.Cap.ROUND);
//                    p.setStrokeWidth(1);
//
//                    int delta = progress / 2;
//                    if (delta == 0) delta = 1;
//
//                    // erase the bitmap and redraw the line
//                    bitmap.eraseColor(Compat.getColor(getContext(),android.R.color.transparent, getContext().getTheme()));
//                    canvas.drawOval(centerX - delta, centerY + delta, centerX + delta, centerY - delta, p);
//                    mShapeSizeImageView.setImageBitmap(bitmap);
                    mShapeSizeTextView.setText(String.valueOf(progress));
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
