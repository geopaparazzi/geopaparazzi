package eu.geopaparazzi.spatialite.database.spatial.core.mbtiles;

/** @author Simon Thépot aka djcoin <simon.thepot@gmail.com, simon.thepot@makina-corpus.com> */

import java.io.File;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata.MetadataParseException;
import eu.geopaparazzi.spatialite.database.spatial.core.mbtiles.MbTilesMetadata.MetadataValidator;

public class MBTilesDroidSpitter {
    // private static final int VERSION_BDD = 1;
    // private MbTilesSQLite mbtilesdb;

    private SQLiteDatabase db;
    private File dbpath;
    private MbTilesMetadata metadata;

    public MBTilesDroidSpitter( File dbpath ) {
        // mbtilesdb = new MbTilesSQLite(ctx, dbpath.getName(), null, VERSION_BDD);
        this.dbpath = dbpath;
    }

    public void open( boolean fetchMetadata, String metadataVersion ) {
        // db = mbtilesdb.getReadableDatabase();
        db = SQLiteDatabase.openDatabase(dbpath.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

        if (!fetchMetadata)
            return;

        try {
            fetchMetadata(metadataVersion);
        } catch (MetadataParseException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public void close() {
        db.close();
    }

    public SQLiteDatabase getBDD() {
        return db;
    }

    public Drawable getTileAsDrawable( int x, int y, int z ) {
        return new BitmapDrawable(getTileAsBitmap(x, y, z));
    }

    public Drawable getTileAsDrawable( String x, String y, String z ) {
        return new BitmapDrawable(getTileAsBitmap(x, y, z));
    }

    public Bitmap getTileAsBitmap( int x, int y, int z ) {
        return this.getTileAsBitmap(Integer.toString(x), Integer.toString(y), Integer.toString(z));
    }
    public byte[] getTileAsBytes( String x, String y, String z ) {
        final Cursor c = db.rawQuery("select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[]{x,
                y, z});
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }
        byte[] bb = c.getBlob(c.getColumnIndex("tile_data"));
        c.close();
        return bb;
    }

    // Warning: you should have checked that those x y z are real integers
    /**
     * @return the bitmap of the tile or null if no tile matched the given parameters
     */
    public Bitmap getTileAsBitmap( String x, String y, String z ) {
        // TODO: Optimize this if we have metadata with bound or min/max zoomlevels
        // Do not make any request and return null if we know it won't match any tile
        byte[] bb = getTileAsBytes(x, y, z);
        return BitmapFactory.decodeByteArray(bb, 0, bb.length);
    }
    
    

    public MbTilesMetadata fetchMetadata( String metadataVersion ) throws MetadataParseException {
        Cursor c = db.query(MbTilesSQLite.TABLE_METADATA, new String[]{MbTilesSQLite.COL_METADATA_NAME,
                MbTilesSQLite.COL_METADATA_VALUE}, null, null, null, null, null);
        MetadataValidator validator = MbTilesMetadata.MetadataValidatorFactory.getMetadataValidatorFromVersion(metadataVersion);
        if (validator == null)
            return null;

        this.metadata = MbTilesMetadata.createFromCursor(c, c.getColumnIndex(MbTilesSQLite.COL_METADATA_NAME),
                c.getColumnIndex(MbTilesSQLite.COL_METADATA_VALUE), validator);
        return this.metadata;
    }

    public MbTilesMetadata getMetadata() {
        return this.metadata;
    }
}
