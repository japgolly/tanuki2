package golly.tanuki2.qa;

import static golly.tanuki2.data.TrackPropertyType.ALBUM;
import static golly.tanuki2.data.TrackPropertyType.ARTIST;
import static golly.tanuki2.data.TrackPropertyType.TN;
import static golly.tanuki2.data.TrackPropertyType.TRACK;
import static golly.tanuki2.data.TrackPropertyType.YEAR;
import static golly.tanuki2.support.Helpers.addPathElements;
import static golly.tanuki2.support.Helpers.ensureCorrectDirSeperators;
import golly.tanuki2.core.Engine;
import golly.tanuki2.core.ITrackPropertyReader;
import golly.tanuki2.core.IVoodooProgressMonitor;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackPropertyMap;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
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
		for (TrackPropertyMap tp : test)
			if (tp.equals(expected)) {
				found= true;
				break;
			}
		if (!found) {
			System.err.println("Expected TrackProperties not found.");
			System.err.println("  Filename: " + filename);
			System.err.println("  Expected: " + expected);
			for (TrackPropertyMap tp : test)
				System.err.println("  Found:    " + tp);
			System.err.println();
			fail("Expected TrackProperties not found.");
		}
	}

	static String getQAPath(String path) {
		try {
			return new File(ID3V2TagReaderTest.class.getResource(path).toURI()).getAbsolutePath();
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

/**
 * @author Golly
 * @since 24/02/2007
 */
class Engine2 extends Engine {
	public Engine2(ITrackPropertyReader... trackReaders) {
		super();
		trackProprtyReaders.clear();
		add(trackReaders);
	}

	public void add(ITrackPropertyReader... trackReaders) {
		for (ITrackPropertyReader tr : trackReaders)
			trackProprtyReaders.add(tr);
	}

	public void addFakeDir(String path, String... filenames) {
		DirData dd= new DirData(path);
		for (String f : filenames) {
			FileData fd= new FileData(dd);
			fd.setAudio(true);
			files.put(addPathElements(path, f), fd);
			dd.files.put(f, fd);
		}
		dd.setHasAudioContent(true);
		dirs.put(path, dd);
		dirsNeedingTrackProprties.add(dd);
	}

	public void readTrackProprties2() {
		readAndAssignTrackProprties();
	}
}

/**
 * @author Golly
 * @since 13/03/2007
 */
class Engine3 extends Engine {
	public Engine3() {
	}

	public void addDirNeedingTrackProprties(DirData dd) {
		dirsNeedingTrackProprties.add(dd);
	}

	public void readAndAssignTrackProprties2() {
		readAndAssignTrackProprties();
	}
}

/**
 * @author Golly
 * @since 24/02/2007
 */
class MockTrackProprtyReader extends TestHelper implements ITrackPropertyReader {

	private final Map<String, List<TrackPropertyMap>> mockResults= new HashMap<String, List<TrackPropertyMap>>();

	public void addMockResult(String filename, TrackPropertyMap tp) {
		filename= ensureCorrectDirSeperators(filename) + ".mp3"; //$NON-NLS-1$
		List<TrackPropertyMap> l= mockResults.get(filename);
		if (l == null)
			mockResults.put(filename, l= new ArrayList<TrackPropertyMap>());
		l.add(tp);
	}

	public Map<String, List<TrackPropertyMap>> readMultipleTrackProperties(DirData dd) {
		final Map<String, List<TrackPropertyMap>> r= new HashMap<String, List<TrackPropertyMap>>();
		for (String f : dd.files.keySet())
			r.put(f, readTrackProperties(addPathElements(dd.dir, f)));
		return r;
	}

	public List<TrackPropertyMap> readTrackProperties(String filename) {
		List<TrackPropertyMap> r= new ArrayList<TrackPropertyMap>();
		if (mockResults.containsKey(filename))
			r.addAll(mockResults.get(filename));
		return r;
	}

}

/**
 * @author Golly
 * @since 07/03/2007
 */
class MockVoodooProgressMonitor implements IVoodooProgressMonitor {
	public void deleting(File file) {
	}

	public void finished(boolean aborted) {
	}

	public Shell getShell() {
		return null;
	}

	public void moving(File source, File target) {
	}

	public void nextDir(String srcDir, String targetDir, int fileCount) {
	}

	public void nextFile() {
	}

	public void rmdirs(List<File> removedDirs) {
	}

	public void starting(int dirCount, int totalFiles) {
	}

	public boolean isCancelled() {
		return false;
	}

	public void fileOperationComplete(int status) {
	}
}

/**
 * @author Golly
 * @since 08/03/2007
 */
@SuppressWarnings("nls")
class NoisyMockVoodooProgressMonitor implements IVoodooProgressMonitor {

	public Shell getShell() {
		return null;
	}

	public void starting(int dirCount, int totalFiles) {
		System.out.println("\nStarting. (" + dirCount + " dirs, " + totalFiles + " files)");
	}

	public void nextDir(String srcDir, String targetDir, int fileCount) {
		System.out.println("Next dir: " + srcDir);
		if (targetDir != null)
			System.out.println("Target dir: " + targetDir);
	}

	public void nextFile() {
	}

	public void deleting(File file) {
		System.out.print("  Deleting " + file.getName() + "...");
	}

	public void moving(File source, File target) {
		System.out.print("  Moving " + source.getName() + " to " + target.getName() + "...");
	}

	public void rmdirs(List<File> removedDirs) {
		for (File f : removedDirs)
			System.out.println("  rmdir " + f.toString());
	}

	public void finished(boolean aborted) {
		System.out.println(aborted ? "Aborted" : "Finished");
	}

	public boolean isCancelled() {
		return false;
	}

	public void fileOperationComplete(int status) {
		switch (status) {
		case SUCCEEDED:
			System.out.println("ok");
			break;
		case FAILED:
			System.out.println("FAILED");
			break;
		case SKIPPED:
			System.out.println("skipped");
			break;
		}
	}
}
