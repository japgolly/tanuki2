package golly.tanuki2;

import static golly.tanuki2.data.TrackPropertyType.ALBUM;
import static golly.tanuki2.data.TrackPropertyType.ARTIST;
import static golly.tanuki2.data.TrackPropertyType.TN;
import static golly.tanuki2.data.TrackPropertyType.TRACK;
import static golly.tanuki2.data.TrackPropertyType.YEAR;
import golly.tanuki2.core.ITrackPropertyReader;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackPropertyMap;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

/**
 * @author Golly
 * @since 24/02/2007
 */
@SuppressWarnings("nls")
public abstract class TestHelper extends Assert {

	protected void assertAlbumData(AlbumData test, String artist, Integer year, String album) {
		AlbumData ad= new AlbumData();
		ad.setArtist(artist);
		ad.setYear(year);
		ad.setAlbum(album);
		assertEquals(ad, test);
	}

	protected void assertTrackPropertiesFound(String filename, TrackPropertyMap expected, final Collection<TrackPropertyMap> test) {
		boolean found= false;
		for (TrackPropertyMap tp : test) {
			if (tp.equals(expected)) {
				found= true;
				break;
			}
		}
		if (!found) {
			System.err.println("Expected TrackProperties not found.");
			System.err.println("  Filename: " + filename);
			System.err.println("  Expected: " + expected);
			for (TrackPropertyMap tp : test) {
				System.err.println("  Found:    " + tp);
			}
			System.err.println();
			fail("Expected TrackProperties not found.");
		}
	}

	public static URL getTestResource(String name) {
		return Thread.currentThread().getContextClassLoader().getResource(name);
	}

	public static String getTestResourcePath(String path) {
		try {
			return new File(getTestResource(path).toURI()).getAbsolutePath();
		} catch (URISyntaxException e) {
			throw new RuntimeException();
		}
	}

	protected FileData makeFileData(DirData dd, boolean isAudio) {
		FileData fd= new FileData(dd);
		fd.setAudio(isAudio);
		return fd;
	}

	protected FileData makeFileData(DirData dd, String filename, boolean isAudio) {
		FileData fd= makeFileData(dd, isAudio);
		dd.files.put(filename, fd);
		return fd;
	}

	protected TrackPropertyMap makeTrackProperties(String artist, Integer year, String album, String tn, String track) {
		TrackPropertyMap expected= new TrackPropertyMap();
		expected.put(ARTIST, artist);
		expected.put(YEAR, year == null ? null : year.toString());
		expected.put(ALBUM, album);
		expected.put(TN, tn == null ? null : tn.toString());
		expected.put(TRACK, track);
		return expected;
	}

	protected void subtestParse(ITrackPropertyReader tpr, String filename, String artist, Integer year, String album, String tn, String track) {
		final TrackPropertyMap expected= makeTrackProperties(artist, year, album, tn, track);

		final Collection<TrackPropertyMap> r= tpr.readTrackProperties(filename);
		//		if (r.size() > 1) {
		//			System.out.println("=================================================================================");
		//			System.out.println(filename);
		//			for (TrackProperties tp : r)
		//				System.out.println("  " + tp.size() + " : " + tp);
		//			System.out.println();
		//		}
		assertTrackPropertiesFound(filename, expected, r);

		File f= new File(filename);
		DirData dd= new DirData(f.getParent());
		dd.files.put(f.getName(), makeFileData(dd, true));
		Map<String, List<TrackPropertyMap>> r2= tpr.readMultipleTrackProperties(dd);
		assertEquals(1, r2.size());
		assertEquals(f.getName(), r2.keySet().iterator().next());
		assertTrackPropertiesFound(filename, expected, r2.get(f.getName()));
	}
}
