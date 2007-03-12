package golly.tanuki2.qa;

import static golly.tanuki2.data.TrackPropertyType.ALBUM;
import static golly.tanuki2.data.TrackPropertyType.ARTIST;
import static golly.tanuki2.data.TrackPropertyType.TN;
import static golly.tanuki2.data.TrackPropertyType.TRACK;
import static golly.tanuki2.data.TrackPropertyType.YEAR;
import static golly.tanuki2.support.Helpers.addPathElements;
import static golly.tanuki2.support.Helpers.ensureCorrectDirSeperators;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import golly.tanuki2.core.Engine;
import golly.tanuki2.core.ITrackProprtyReader;
import golly.tanuki2.core.IVoodooProgressMonitor;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackProperties;

import org.eclipse.swt.widgets.Shell;
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

	protected FileData makeFileData(DirData dd, String filename, boolean isAudio) {
		FileData fd= makeFileData(dd, isAudio);
		dd.files.put(filename, fd);
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
}

/**
 * @author Golly
 * @since 24/02/2007
 */
class Engine2 extends Engine {
	@SuppressWarnings("nls")
	public Engine2(ITrackProprtyReader tr) {
		super();
		trackProprtyReaders.clear();
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
 * @since 24/02/2007
 */
class MockTrackProprtyReader extends TestHelper implements ITrackProprtyReader {

	private final Map<String, List<TrackProperties>> mockResults= new HashMap<String, List<TrackProperties>>();

	public void addMockResult(String filename, TrackProperties tp) {
		filename= ensureCorrectDirSeperators(filename) + ".mp3"; //$NON-NLS-1$
		List<TrackProperties> l= mockResults.get(filename);
		if (l == null)
			mockResults.put(filename, l= new ArrayList<TrackProperties>());
		l.add(tp);
	}

	public Map<String, List<TrackProperties>> readMultipleTrackProperties(DirData dd) {
		final Map<String, List<TrackProperties>> r= new HashMap<String, List<TrackProperties>>();
		for (String f : dd.files.keySet())
			r.put(f, readTrackProperties(addPathElements(dd.dir, f)));
		return r;
	}

	public List<TrackProperties> readTrackProperties(String filename) {
		List<TrackProperties> r= new ArrayList<TrackProperties>();
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

	public void finished() {
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
		System.out.println("  Deleting " + file.getName());
	}

	public void moving(File source, File target) {
		System.out.println("  Moving " + source.getName() + " to " + target.getName());
	}

	public void rmdirs(List<File> removedDirs) {
		for (File f : removedDirs)
			System.out.println("  rmdir " + f.toString());
	}

	public void finished() {
		System.out.println("Finished");
	}
}
