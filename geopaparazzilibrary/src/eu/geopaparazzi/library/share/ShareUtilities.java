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
package eu.geopaparazzi.library.share;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Utilities to help sharing of data through the android intents. 
 * 
 * <p>Adapted from http://writecodeeasy.blogspot.it/2012/09/androidtutorial-shareintents.html</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ShareUtilities {

    /**
     * Share text. 
     * 
     * @param context  the context to use.
     * @param titleMessage title.
     * @param textToShare text.
     */
    public static void shareText( Context context, String titleMessage, String textToShare ) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, textToShare);
        context.startActivity(Intent.createChooser(intent, titleMessage));
    }

    /**
     * Share text by email. 
     * 
     * @param context  the context to use.
     * @param titleMessage title.
     * @param emailSubject email subject.
     * @param textToShare text.
     * @param email email address.
     */
    public static void shareTextByEmail( Context context, String titleMessage, String emailSubject, String textToShare,
            String email ) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc882");
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        intent.putExtra(Intent.EXTRA_TEXT, textToShare);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        context.startActivity(Intent.createChooser(intent, titleMessage));
    }

    /**
     * Share image. 
     * 
     * @param context  the context to use.
     * @param titleMessage title.
     * @param imageFile the image file. 
     */
    public static void shareImage( Context context, String titleMessage, File imageFile ) {
        String mimeType = "image/png";
        if (imageFile.getName().toLowerCase().endsWith("jpg")) {
            mimeType = "image/jpg";
        }
        shareFile(context, titleMessage, imageFile, mimeType);
    }

    /**
     * Share text and image. 
     * 
     * @param context  the context to use.
     * @param titleMessage title.
     * @param textToShare text.
     * @param imageFile the image file.
     */
    public static void shareTextAndImage( Context context, String titleMessage, String textToShare, File imageFile ) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, textToShare);
        Uri uri = Uri.fromFile(imageFile);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(intent, titleMessage));
    }

    /**
     * Share audio. 
     * 
     * @param context  the context to use.
     * @param titleMessage title.
     * @param audioFile the audio file. 
     */
    public static void shareAudio( Context context, String titleMessage, File audioFile ) {
        String mimeType = "audio/amr";
        shareFile(context, titleMessage, audioFile, mimeType);
    }

    /**
     * Share video. 
     * 
     * @param context  the context to use.
     * @param titleMessage title.
     * @param videoFile the video file. 
     */
    public static void shareVideo( Context context, String titleMessage, File videoFile ) {
        String mimeType = "video/mp4";
        shareFile(context, titleMessage, videoFile, mimeType);
    }

    private static void shareFile( Context context, String titleMessage, File file, String mimeType ) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        Uri uri = Uri.fromFile(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(intent, titleMessage));
    }

}
