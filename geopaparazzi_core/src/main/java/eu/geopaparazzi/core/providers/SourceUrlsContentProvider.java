// AddressBookContentProvider.java
// ContentProvider subclass for manipulating the app's database
package eu.geopaparazzi.core.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SourceUrlsContentProvider extends ContentProvider {
    // used to access the database
    private SourceUrlsDatabaseHelper dbHelper;

    // UriMatcher helps ContentProvider determine operation to perform
    private static final UriMatcher uriMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);

    // constants used with UriMatcher to determine operation to perform
    private static final int ONE_SOURCEURL = 1; // manipulate one contact
    private static final int SOURCEURLS = 2; // manipulate contacts table

    // static block to configure this ContentProvider's UriMatcher
    static {
        // Uri for Contact with the specified id (#)
        uriMatcher.addURI(DatabaseDescription.AUTHORITY,
                DatabaseDescription.SourceUrl.TABLE_NAME + "/#", ONE_SOURCEURL);

        // Uri for Contacts table
        uriMatcher.addURI(DatabaseDescription.AUTHORITY,
                DatabaseDescription.SourceUrl.TABLE_NAME, SOURCEURLS);
    }

    // called when the AddressBookContentProvider is created
    @Override
    public boolean onCreate() {
        // create the AddressBookDatabaseHelper
        dbHelper = new SourceUrlsDatabaseHelper(getContext());
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

        // create SQLiteQueryBuilder for querying contacts table
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(DatabaseDescription.SourceUrl.TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case ONE_SOURCEURL: // contact with specified id will be selected
                queryBuilder.appendWhere(
                        DatabaseDescription.SourceUrl._ID + "=" + uri.getLastPathSegment());
                break;
            case SOURCEURLS: // all contacts will be selected
                break;
            default:
                throw new UnsupportedOperationException("Invalid query uri: " + uri);
        }

        // execute the query to select one or all contacts
        Cursor cursor = queryBuilder.query(dbHelper.getReadableDatabase(),
                projection, selection, selectionArgs, null, null, sortOrder);

        // configure to watch for content changes
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    // insert a new contact in the database
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri newSourceUrlUri = null;

        switch (uriMatcher.match(uri)) {
            case SOURCEURLS:
                // insert the new contact--success yields new contact's row id
                long rowId = dbHelper.getWritableDatabase().insert(
                        DatabaseDescription.SourceUrl.TABLE_NAME, null, values);

                // if the contact was inserted, create an appropriate Uri;
                // otherwise, throw an exception
                if (rowId > 0) { // SQLite row IDs start at 1
                    newSourceUrlUri = DatabaseDescription.SourceUrl.buildContactUri(rowId);

                    // notify observers that the database changed
                    getContext().getContentResolver().notifyChange(uri, null);
                } else
                    throw new SQLException("Insertion failed: " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Invalid uri: " + uri);
        }

        return newSourceUrlUri;
    }

    // update an existing contact in the database
    @Override
    public int update(Uri uri, ContentValues values,
                      String selection, String[] selectionArgs) {
        int numberOfRowsUpdated; // 1 if update successful; 0 otherwise

        switch (uriMatcher.match(uri)) {
            case ONE_SOURCEURL:
                // get from the uri the id of contact to update
                String id = uri.getLastPathSegment();

                // update the contact
                numberOfRowsUpdated = dbHelper.getWritableDatabase().update(
                        DatabaseDescription.SourceUrl.TABLE_NAME, values, DatabaseDescription.SourceUrl._ID + "=" + id,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Invalid update uri: " + uri);
        }

        // if changes were made, notify observers that the database changed
        if (numberOfRowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numberOfRowsUpdated;
    }

    // delete an existing contact from the database
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int numberOfRowsDeleted;

        switch (uriMatcher.match(uri)) {
            case ONE_SOURCEURL:
                // get from the uri the id of contact to update
                String id = uri.getLastPathSegment();

                // delete the contact
                numberOfRowsDeleted = dbHelper.getWritableDatabase().delete(
                        DatabaseDescription.SourceUrl.TABLE_NAME, DatabaseDescription.SourceUrl._ID + "=" + id, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Invalid delete uri: " + uri);
        }

        // notify observers that the database changed
        if (numberOfRowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return numberOfRowsDeleted;
    }
}

