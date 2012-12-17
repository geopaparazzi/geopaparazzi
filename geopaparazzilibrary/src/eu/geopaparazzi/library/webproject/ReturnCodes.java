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
package eu.geopaparazzi.library.webproject;
/**
 * Codes that the sync can return.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public enum ReturnCodes {
    OK(0, "ok"), //
    PROBLEM_LOGIN(1, "An error occurred during the login process. Check your account settings."), //
    ERROR(2, "An error occurred during the sync."), //
    NETWORKMISSING(3, "This operation is possible only with an active network connection."), //
    FILEEXISTS(4, "The file exists. Won't overwrite.");

    private final String msgString;
    private final int msgCode;

    ReturnCodes( int msgCode, String msgString ) {
        this.msgCode = msgCode;
        this.msgString = msgString;
    }

    public static ReturnCodes get4Code( int i ) {
        ReturnCodes[] values = ReturnCodes.values();
        for( ReturnCodes returncode : values ) {
            if (returncode.getMsgCode() == i) {
                return returncode;
            }
        }
        return null;
    }

    public int getMsgCode() {
        return msgCode;
    }

    public String getMsgString() {
        return msgString;
    }

    public static String getMsgStringForCode( int i ) {
        ReturnCodes[] values = ReturnCodes.values();
        for( ReturnCodes returncode : values ) {
            if (returncode.getMsgCode() == i) {
                return returncode.msgString;
            }
        }
        return "-";
    }
}