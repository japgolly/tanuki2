package golly.tanuki2.qa;

import static golly.tanuki2.data.TrackPropertyType.ALBUM;
import static golly.tanuki2.data.TrackPropertyType.ARTIST;
import static golly.tanuki2.data.TrackPropertyType.TN;
import static golly.tanuki2.data.TrackPropertyType.TRACK;
import static golly.tanuki2.data.TrackPropertyType.YEAR;

import java.io.File;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackProperties;

import org.junit.Assert;

/**
 * @author Golly
 * @since 24/02/2007
 */
public abstract class TestHelper extends Assert {

	protected void assertAlbumData(AlbumData test, String artist, Integer year, String album) {
		AlbumData ad= new AlbumData();
		ad.setArtist(artist);
		ad.setYear(year);
		ad.setAlbum(album);
		assertEquals(ad, test);
	}
	
	protected FileData makeFileData(DirData dd, boolean isAudio) {
		FileData fd= new FileData(dd);
		fd.setAudio(isAudio);
		return fd;
	}

	protected TrackProperties makeTrackProperties(String artist, Integer year, String album, String tn, String track) {
		TrackProperties expected= new TrackProperties();
		expected.put(ARTIST, artist);
		expected.put(YEAR, year == null ? null : year.toString());
		expected.put(ALBUM, album);
		expected.put(TN, tn == null ? null : tn.toString());
		expected.put(TRACK, track);
		return expected;
	}

	protected String osCompatFilename(String filename) {
		if (File.separatorChar == '/')
			return filename.replace('\\', '/');
		else
			return filename.replace('/', '\\');

	}

}
