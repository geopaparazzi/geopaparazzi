package eu.geopaparazzi.library.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKBWriter;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

import eu.geopaparazzi.library.util.ResourcesManager;

import android.content.Context;

/**
 * An utility class to handle the spatial database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DatabaseHandler {
    // 3857
    private GeometryFactory gf = new GeometryFactory();
    private WKBWriter wr = new WKBWriter();

    private static final double TEST_LAT = 46.0;
    private static final double TEST_LON = 11.0;
    private final String SEP = "********************************************\n";
    private static final String ERROR = "\tERROR: ";
    private Database db;
    private Context context;

    private final String COMUNITABLE = "Comuni_11";
    private final String NOME = "NOME";
    private final String AS_TEXT_GEOMETRY = "AsText(Geometry)";
    private StringBuilder sb;

    private void error( Exception e ) {
        sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    }

    public DatabaseHandler( Context context, StringBuilder sb ) {
        this.context = context;
        try {
            this.sb = sb;

            File sdcardDir = ResourcesManager.getInstance(context).getSdcardDir();
            File spatialDbFile = new File(sdcardDir, "shp/italy.sqlite");

            if (!spatialDbFile.getParentFile().exists()) {
                throw new RuntimeException();
            }
            db = new jsqlite.Database();
            db.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String queryVersions() throws Exception {
        sb.append(SEP);
        sb.append("Check versions...\n");

        Stmt stmt01 = db.prepare("SELECT spatialite_version();");
        if (stmt01.step()) {
            sb.append("\t").append("SPATIALITE_VERSION: " + stmt01.column_string(0));
            sb.append("\n");
        }

        stmt01 = db.prepare("SELECT proj4_version();");
        if (stmt01.step()) {
            sb.append("\t").append("PROJ4_VERSION: " + stmt01.column_string(0));
            sb.append("\n");
        }

        stmt01 = db.prepare("SELECT geos_version();");
        if (stmt01.step()) {
            sb.append("\t").append("GEOS_VERSION: " + stmt01.column_string(0));
            sb.append("\n");
        }
        stmt01.close();

        sb.append("Done...\n");
        return sb.toString();
    }

    public String queryComuni() {
        sb.append(SEP);
        sb.append("Query Comuni...\n");

        String query = "SELECT " + NOME + //
                " from " + COMUNITABLE + //
                " order by " + NOME + ";";
        sb.append("Execute query: ").append(query).append("\n");
        try {
            Stmt stmt = db.prepare(query);
            int index = 0;
            while( stmt.step() ) {
                String nomeStr = stmt.column_string(0);
                sb.append("\t").append(nomeStr).append("\n");
                if (index++ > 5) {
                    break;
                }
            }
            sb.append("\t...");
            stmt.close();
        } catch (Exception e) {
            error(e);
        }

        sb.append("Done...\n");

        return sb.toString();
    }

    public String queryComuniWithGeom() {
        sb.append(SEP);
        sb.append("Query Comuni with AsText(Geometry)...\n");

        String query = "SELECT " + NOME + //
                " , " + AS_TEXT_GEOMETRY + //
                " as geom from " + COMUNITABLE + //
                " where geom not null;";
        sb.append("Execute query: ").append(query).append("\n");
        try {
            Stmt stmt = db.prepare(query);
            while( stmt.step() ) {
                String nomeStr = stmt.column_string(0);
                String geomStr = stmt.column_string(1);
                String substring = geomStr;
                if (substring.length() > 40)
                    substring = geomStr.substring(0, 40);
                sb.append("\t").append(nomeStr).append(" - ").append(substring).append("...\n");
                break;
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
        }
        sb.append("Done...\n");

        return sb.toString();
    }

    public String queryGeomTypeAndSrid() {
        sb.append(SEP);
        sb.append("Query Comuni geom type and srid...\n");

        String query = "SELECT " + NOME + //
                " , " + AS_TEXT_GEOMETRY + //
                " as geom from " + COMUNITABLE + //
                " where geom not null;";
        sb.append("Execute query: ").append(query).append("\n");
        try {
            Stmt stmt = db.prepare(query);
            while( stmt.step() ) {
                String nomeStr = stmt.column_string(0);
                String geomStr = stmt.column_string(1);
                String substring = geomStr;
                if (substring.length() > 40)
                    substring = geomStr.substring(0, 40);
                sb.append("\t").append(nomeStr).append(" - ").append(substring).append("...\n");
                break;
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
        }
        sb.append("Done...\n");

        return sb.toString();
    }

    public String queryComuniArea() {
        sb.append(SEP);
        sb.append("Query Comuni area sum...\n");

        String query = "SELECT ST_Area(Geometry) / 1000000.0 from " + COMUNITABLE + //
                ";";
        sb.append("Execute query: ").append(query).append("\n");
        try {
            Stmt stmt = db.prepare(query);
            double totalArea = 0;
            while( stmt.step() ) {
                double area = stmt.column_double(0);
                totalArea = totalArea + area;
            }
            sb.append("\tTotal area by summing each area: ").append(totalArea).append("Km2\n");
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
        }
        query = "SELECT sum(ST_Area(Geometry) / 1000000.0) from " + COMUNITABLE + //
                ";";
        sb.append("Execute query: ").append(query).append("\n");
        try {
            Stmt stmt = db.prepare(query);
            double totalArea = 0;
            if (stmt.step()) {
                double area = stmt.column_double(0);
                totalArea = totalArea + area;
            }
            sb.append("\tTotal area by summing in query: ").append(totalArea).append("Km2\n");
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
        }
        sb.append("Done...\n");

        return sb.toString();
    }

    public String queryComuniNearby() {
        sb.append(SEP);
        sb.append("Query Comuni nearby...\n");

        String query = "SELECT Hex(ST_AsBinary(ST_Buffer(Geometry, 1.0))), ST_Srid(Geometry), ST_GeometryType(Geometry) from "
                + COMUNITABLE + //
                " where " + NOME + "= 'Bolzano';";
        sb.append("Execute query: ").append(query).append("\n");
        String bufferGeom = "";
        String bufferGeomShort = "";
        try {
            Stmt stmt = db.prepare(query);
            if (stmt.step()) {
                bufferGeom = stmt.column_string(0);
                String geomSrid = stmt.column_string(1);
                String geomType = stmt.column_string(2);
                sb.append("\tThe selected geometry is of type: ").append(geomType).append(" and of SRID: ").append(geomSrid)
                        .append("\n");
            }
            bufferGeomShort = bufferGeom;
            if (bufferGeom.length() > 10)
                bufferGeomShort = bufferGeom.substring(0, 10) + "...";
            sb.append("\tBolzano polygon buffer geometry in HEX: ").append(bufferGeomShort).append("\n");
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
        }

        query = "SELECT " + NOME + ", AsText(ST_centroid(Geometry)) from " + COMUNITABLE + //
                " where ST_Intersects( ST_GeomFromWKB(x'" + bufferGeom + "') , Geometry );";
        // just for print
        String tmpQuery = "SELECT " + NOME + " from " + COMUNITABLE + //
                " where ST_Intersects( ST_GeomFromWKB(x'" + bufferGeomShort + "') , Geometry );";
        sb.append("Execute query: ").append(tmpQuery).append("\n");
        try {
            sb.append("\tComuni nearby Bolzano: \n");
            Stmt stmt = db.prepare(query);
            while( stmt.step() ) {
                String name = stmt.column_string(0);
                String wkt = stmt.column_string(1);
                sb.append("\t\t").append(name).append(" - with centroid in ").append(wkt).append("\n");
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
        }
        sb.append("Done...\n");

        return sb.toString();
    }

    public byte[] getBolzanoWKB() {
        String query = "SELECT ST_AsBinary(ST_Transform(Geometry, 4326)) from " + COMUNITABLE + //
                " where " + NOME + "= 'Bolzano';";
        try {
            Stmt stmt = db.prepare(query);
            byte[] theGeom = null;
            if (stmt.step()) {
                theGeom = stmt.column_bytes(0);
            }
            stmt.close();
            return theGeom;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<byte[]> getIntersectingWKB( double n, double s, double e, double w ) {
        List<byte[]> list = new ArrayList<byte[]>();
        Coordinate ll = new Coordinate(w, s);
        Coordinate ul = new Coordinate(w, n);
        Coordinate ur = new Coordinate(e, n);
        Coordinate lr = new Coordinate(e, s);
        Polygon bboxPolygon = gf.createPolygon(new Coordinate[]{ll, ul, ur, lr, ll});

        byte[] bbox = wr.write(bboxPolygon);
        String query = "SELECT ST_AsBinary(ST_Transform(Geometry, 4326)) from " + COMUNITABLE + //
                " where ST_Intersects(ST_Transform(Geometry, 4326), ST_GeomFromWKB(?));";
        try {
            Stmt stmt = db.prepare(query);
            stmt.bind(1, bbox);
            while (stmt.step()) {
                list.add(stmt.column_bytes(0));
            }
            stmt.close();
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String doSimpleTransform() {

        sb.append(SEP);
        sb.append("Coordinate transformation...\n");

        String query = "SELECT AsText(Transform(MakePoint(" + TEST_LON + ", " + TEST_LAT + ", 4326), 32632));";
        sb.append("Execute query: ").append(query).append("\n");
        try {
            Stmt stmt = db.prepare(query);
            if (stmt.step()) {
                String pointStr = stmt.column_string(0);
                sb.append("\t").append(TEST_LON + "/" + TEST_LAT + "/EPSG:4326").append(" = ")//
                        .append(pointStr + "/EPSG:32632").append("...\n");
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
        }
        sb.append("Done...\n");

        return sb.toString();

    }

}
