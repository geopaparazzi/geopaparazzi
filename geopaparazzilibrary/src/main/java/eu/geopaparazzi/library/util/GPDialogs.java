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

package eu.geopaparazzi.library.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Created by hydrologis on 15/01/16.
 */
public class GPDialogs {


    public static void infoDialog(final Context context, final String msg, final Runnable okRunnable) {
        messageDialog(context, msg, android.R.drawable.ic_dialog_info, okRunnable);
    }

    /**
     * Execute a message dialog in an {@link AsyncTask}.
     *
     * @param context      the {@link Context} to use.
     * @param msg          the message to show.
     * @param iconResource the icon resource to use or -1 fro default.
     * @param okRunnable   optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void messageDialog(final Context context, final String msg, final int iconResource, final Runnable okRunnable) {
        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute(String response) {
                try {
                    int icon = iconResource;
                    if (icon == -1) {
                        icon = android.R.drawable.ic_dialog_info;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder //
                            .setMessage(msg)
                            .setIcon(icon).setCancelable(true)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (okRunnable != null) {
                                        new Thread(okRunnable).start();
                                    }
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) {
                    GPLog.error("UTILITIES", "Error in messageDialog#inPostExecute -- " + msg, e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }.execute((String) null);
    }

    /**
     * Execute a message dialog with clickable links in an {@link AsyncTask}.
     *
     * @param context      the {@link Context} to use.
     * @param msg          the message to show.
     * @param iconResource the icon resource to use or -1 fro default.
     * @param okRunnable   optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void messageDialogWithLink(final Context context, final String msg, final int iconResource, final Runnable okRunnable) {

        final SpannableString s = new SpannableString(msg);
        Linkify.addLinks(s, Linkify.ALL);

        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute(String response) {
                try {
                    int icon = iconResource;
                    if (icon == -1) {
                        icon = android.R.drawable.ic_dialog_info;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder //
                            .setMessage(s)
                            .setIcon(icon).setCancelable(true)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (okRunnable != null) {
                                        new Thread(okRunnable).start();
                                    }
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                } catch (Exception e) {
                    GPLog.error("UTILITIES", "Error in messageDialog#inPostExecute -- " + msg, e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }.execute((String) null);
    }

    /**
     * Execute a generic error dialog in an {@link AsyncTask}.
     *
     * @param context    the {@link Context} to use.
     * @param t          the exception.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void errorDialog(final Context context, final Throwable t, final Runnable okRunnable) {

        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute(String response) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder //
                            .setTitle(t.getLocalizedMessage()).setMessage(Log.getStackTraceString(t))
                            .setIcon(android.R.drawable.ic_dialog_alert).setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (okRunnable != null) {
                                        new Thread(okRunnable).start();
                                    }
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) {
                    GPLog.error("UTILITIES", "Error in errorDialog#inPostExecute", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }.execute((String) null);
    }

    /**
     * Execute a message dialog in an {@link AsyncTask}.
     *
     * @param context     the {@link Context} to use.
     * @param msg         the message to show.
     * @param yesRunnable optional {@link Runnable} to trigger after yes was pressed.
     * @param noRunnable  optional {@link Runnable} to trigger after no was pressed.
     */
    public static void yesNoMessageDialog(final Context context, final String msg, final Runnable yesRunnable,
                                          final Runnable noRunnable) {
        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute(String response) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    try {
                        builder.setMessage(msg);
                        builder.setPositiveButton(context.getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        try {
                                            if (yesRunnable != null) {
                                                new Thread(yesRunnable).start();
                                            }
                                        } catch (Exception e) {
                                            GPLog.error(this, e.getLocalizedMessage(), e);
                                            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                        );

                        builder.setNegativeButton(context.getString(R.string.no),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        try {
                                            if (noRunnable != null) {
                                                new Thread(noRunnable).start();
                                            }
                                        } catch (Exception e) {
                                            GPLog.error(this, e.getLocalizedMessage(), e);
                                            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) {
                    GPLog.error("UTILITIES", "Error in yesNoMessageDialog#inPostExecute", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }.execute((String) null);
    }

    /**
     * A warning dialog.
     * <p>
     * <b>NOT IMPLEMENTED YET, FOR NOW JUST CALLS {@link #messageDialog}</b>
     *
     * @param context    the context to use.
     * @param msg        the message.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void warningDialog(final Context context, final String msg, final Runnable okRunnable) {
        messageDialog(context, msg, android.R.drawable.ic_dialog_alert, okRunnable);
    }

    /**
     * A warning dialog with link.
     * <p>
     * <b>NOT IMPLEMENTED YET, FOR NOW JUST CALLS {@link #messageDialog}</b>
     *
     * @param context    the context to use.
     * @param msg        the message.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void warningDialogWithLink(final Context context, final String msg, final Runnable okRunnable) {
        messageDialogWithLink(context, msg, android.R.drawable.ic_dialog_alert, okRunnable);
    }

    /**
     * Execute a toast in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param msg     the message to show.
     * @param length  toast length.
     */
    public static void toast(final Context context, final String msg, final int length) {
        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute(String response) {
                Toast.makeText(context, msg, length).show();
            }
        }.execute((String) null);
    }

    /**
     * Execute a toast in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param msgId   the id of the message to show.
     * @param length  toast length.
     */
    public static void toast(final Context context, final int msgId, final int length) {
        String msg = context.getString(msgId);
        toast(context, msg, length);
    }

    /**
     * Show a quick info in the current view.
     *
     * @param view the view to attach to.
     * @param msg  the message to show.
     */
    public static void quickInfo(final View view, final String msg) {
        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show();
    }


    /**
     * Execute a message dialog in an {@link AsyncTask}.
     *
     * @param activity     the {@link Activity} to use.
     * @param message      a message to show.
     * @param defaultText  a default text to fill in.
     * @param textRunnable optional {@link TextRunnable} to trigger after ok was pressed.
     */
    public static void inputMessageDialog(final Activity activity, final String message,
                                          final String defaultText, final TextRunnable textRunnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(
                eu.geopaparazzi.library.R.layout.inputdialog, null);
        builder.setView(view);

        final TextInputLayout textInputLayout = (TextInputLayout) view.findViewById(eu.geopaparazzi.library.R.id.dialogTextLayout);
        textInputLayout.setHint(message);

        final EditText editText = (EditText) view.findViewById(eu.geopaparazzi.library.R.id.dialogEdittext);
        if (defaultText != null)
            editText.setText(defaultText);
        final CheckBox checkBox = (CheckBox) view.findViewById(eu.geopaparazzi.library.R.id.dialogcheckBox);
        checkBox.setVisibility(View.GONE);

        try {
            builder.setPositiveButton(activity.getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Editable value = editText.getText();
                            String newText = value.toString();
                            if (newText.length() < 1) {
                                newText = defaultText;
                            }
                            dialog.dismiss();
                            if (textRunnable != null) {
                                textRunnable.setText(newText);
                                new Thread(textRunnable).start();
                            }
                        }
                    }
            );

            builder.setNegativeButton(activity.getString(android.R.string.cancel), null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Execute a message dialog with checkbox in an {@link AsyncTask}.
     *
     * @param activity                 the {@link Context} to use.
     * @param message                  a message to show.
     * @param defaultText              a default text to fill in.
     * @param checkBoxText             the text of the checkbox.
     * @param defaultCheckboxSelection default selection for checkbox.
     * @param textRunnable             optional {@link TextRunnable} to trigger after ok was pressed.
     */
    public static void inputMessageAndCheckboxDialog(final Activity activity, final String message,
                                                     final String defaultText, final String checkBoxText, final boolean defaultCheckboxSelection, final TextAndBooleanRunnable textRunnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(
                eu.geopaparazzi.library.R.layout.inputdialog, null);
        builder.setView(view); // add GUI to dialog
        final TextInputLayout textInputLayout = (TextInputLayout) view.findViewById(eu.geopaparazzi.library.R.id.dialogTextLayout);
        textInputLayout.setHint(message);

        final EditText editText = (EditText) view.findViewById(eu.geopaparazzi.library.R.id.dialogEdittext);
        if (defaultText != null)
            editText.setText(defaultText);

        final CheckBox checkBox = (CheckBox) view.findViewById(eu.geopaparazzi.library.R.id.dialogcheckBox);
        checkBox.setText(checkBoxText);
        checkBox.setChecked(defaultCheckboxSelection);

        try {
            builder.setPositiveButton(activity.getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Editable value = editText.getText();
                            String newText = value.toString();
                            if (newText.length() < 1) {
                                newText = defaultText;
                            }
                            dialog.dismiss();
                            if (textRunnable != null) {
                                textRunnable.setText(newText);
                                textRunnable.setTheBoolean(checkBox.isChecked());
                                new Thread(textRunnable).start();
                            }
                        }
                    }
            );

            builder.setNegativeButton(activity.getString(android.R.string.cancel), null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Dismiss {@link ProgressDialog} with check in one line.
     *
     * @param progressDialog the dialog to dismiss.
     */
    public static void dismissProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

}
