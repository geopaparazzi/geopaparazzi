package eu.hydrologis.geopaparazzi.camera;
//package eu.hydrologis.geopaparazi.camera;
//
//import java.io.BufferedOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//import org.apache.sanselan.ImageReadException;
//import org.apache.sanselan.ImageWriteException;
//import org.apache.sanselan.Sanselan;
//import org.apache.sanselan.common.IImageMetadata;
//import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
//import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
//import org.apache.sanselan.formats.tiff.TiffImageMetadata;
//import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
//
//public class ExifHandler {
//
//	private static SimpleDateFormat timeStampFormat = new SimpleDateFormat(
//			"yyyyMMdd_HHmmss");
//
//	public static void setGPSLatLonTag(File jpegImageFile, double lat,
//			double lon) throws IOException, ImageReadException,
//			ImageWriteException {
//		File tempFile = null;
//		OutputStream os = null;
//		try {
//			TiffOutputSet outputSet = null;
//
//			// note that metadata might be null if no metadata is found.
//			IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
//			JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
//			if (null != jpegMetadata) {
//				// note that exif might be null if no Exif metadata is found.
//				TiffImageMetadata exif = jpegMetadata.getExif();
//				if (null != exif) {
//					outputSet = exif.getOutputSet();
//				}
//			}
//			if (null == outputSet)
//				outputSet = new TiffOutputSet();
//
//			outputSet.setGPSInDegrees(lon, lat);
//
//			tempFile = File.createTempFile("temp-", timeStampFormat
//					.format(new Date()));
//			os = new FileOutputStream(tempFile);
//			os = new BufferedOutputStream(os);
//
//			new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os,
//					outputSet);
//
//			os.close();
//			os = null;
//		} finally {
//			if (os != null)
//				try {
//					os.close();
//				} catch (IOException e) {
//
//				}
//		}
//		
//		if (tempFile!=null && tempFile.exists()) {
//			copyFile(tempFile, jpegImageFile);
//			tempFile.delete();
//		}
//	}
//
//	public static void copyFile(File in, File out) throws IOException {
//		FileInputStream fis = new FileInputStream(in);
//		FileOutputStream fos = new FileOutputStream(out);
//		byte[] buf = new byte[1024];
//		int i = 0;
//		while ((i = fis.read(buf)) != -1) {
//			fos.write(buf, 0, i);
//		}
//		fis.close();
//		fos.close();
//	}
//}
