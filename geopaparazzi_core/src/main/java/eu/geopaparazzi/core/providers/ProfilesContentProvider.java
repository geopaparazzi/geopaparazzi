
package eu.geopaparazzi.core.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.profiles.Profile;
import eu.geopaparazzi.library.profiles.ProfilesHandler;

public class ProfilesContentProvider extends ContentProvider {
    // UriMatcher helps ContentProvider determine operation to perform
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // constants used with UriMatcher to determine operation to perform
    private static final int ONE_PROFILE = 1; // manipulate one profile
    private static final int PROFILES = 2; // manipulate all profiles

    // static block to configure this ContentProvider's UriMatcher
    static {
        uriMatcher.addURI(ProfilesHandler.AUTHORITY, ProfilesHandler.PROFILE + "/#", ONE_PROFILE);
        uriMatcher.addURI(ProfilesHandler.AUTHORITY, ProfilesHandler.PROFILE, PROFILES);
    }


    @Override
    public boolean onCreate() {
        return true; // ContentProvider successfully created
    }

    // required method: Not used in this app, so we return null
    @Override
    public String getType(Uri uri) {
        return null;
    }

    // query the database
    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection, String[] selectionArgs, String sortOrder) {
        SharedPreferences mPeferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        List<Profile> mAvailableProfiles = new ArrayList<>();
        try {
            mAvailableProfiles = ProfilesHandler.INSTANCE.getProfilesFromPreferences(mPeferences);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MatrixCursor mc = new MatrixCursor(ProfilesHandler.CONTENT_PROVIDER_FIELDS);

        switch (uriMatcher.match(uri)) {
            case ONE_PROFILE: // profile with specified id will be selected
                String id = uri.getLastPathSegment();
                try {
                    int idInt = Integer.parseInt(id);
                    Profile profile = mAvailableProfiles.get(idInt);
                    mc.addRow(new Object[]{profile.name, profile.active ? 1 : 0, ProfilesHandler.INSTANCE.getJsonFromProfile(profile).toString(2)});
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case PROFILES: // all profiles will be selected
                try {
                    for (Profile profile : mAvailableProfiles) {
                        mc.addRow(new Object[]{profile.name, profile.active ? 1 : 0, ProfilesHandler.INSTANCE.getJsonFromProfile(profile).toString(2)});
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid query uri: " + uri);
        }

        // configure to watch for content changes
        mc.setNotificationUri(getContext().getContentResolver(), uri);
        return mc;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new RuntimeException("Not allowed");
    }

    @Override
    public int update(Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {
        throw new RuntimeException("Not allowed");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new RuntimeException("Not allowed");
    }
}

