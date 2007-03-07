package golly.tanuki2.qa;

import static golly.tanuki2.support.Helpers.addPathElements;
import golly.tanuki2.core.Engine;
import golly.tanuki2.core.ITrackProprtyReader;
import golly.tanuki2.core.IVoodooProgressMonitor;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;

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
		readTrackProprties();
	}
}

/**
 * @author Golly
 * @since 24/02/2007
 */
class MockTrackProprtyReader extends TestHelper implements ITrackProprtyReader {

	private final Map<String, List<TrackProperties>> mockResults= new HashMap<String, List<TrackProperties>>();

	public void addMockResult(String filename, TrackProperties tp) {
		filename= osCompatFilename(filename) + ".mp3"; //$NON-NLS-1$
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
 * @since 24/02/2007
 */
@SuppressWarnings("nls")
public class EngineTest extends TestHelper {
	private Engine2 engine;
	private MockTrackProprtyReader mtpr;
	private TrackProperties noprop= makeTrackProperties(null, null, null, null, null);

	@Before
	public void setup() {
		mtpr= new MockTrackProprtyReader();
		engine= new Engine2(mtpr);
	}

	@Test
	public void makeSureRemoveUpdatesTheHasAudioProperty() {
		engine.addFakeDir("A", "a1.mp3", "a2.mp3", "as.txt");
		engine.files.get(addPathElements("A", "as.txt")).setAudio(false);
		engine.files.get(addPathElements("A", "as.txt")).setMimeImage(TanukiImage.MIME_TEXT);
		final DirData dd= engine.dirs.get("A");
		assertTrue(dd.hasAudioContent());
		engine.remove(addPathElements("A", "a2.mp3"));
		assertTrue(dd.hasAudioContent());
		engine.remove(addPathElements("A", "a1.mp3"));
		assertFalse(dd.hasAudioContent());
	}

	// =============================================================================================== //
	// = TP selection tests
	// =============================================================================================== //

	@Test
	public void eachTrackHasOneResult() {
		addFakeDirsToEngine();
		TrackProperties a1, a2, a3;
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("Metallica", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("Metallica", 2006, "A", "2", "A Two"));
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("Metallica", 2006, "A", "3", "A Three"));
		engine.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1);
		assertEngineTrackProperties("A/a2", a2);
		assertEngineTrackProperties("A/a3", a3);
		assertEngineTrackProperties("B/b1", noprop);
		assertEngineTrackProperties("B/b2", noprop);
		assertEngineTrackProperties("B/b3", noprop);
	}

	@Test
	public void oneTrackHasMultipleResults_checksAlbumDataOfOtherTracks() {
		addFakeDirsToEngine();
		TrackProperties a1, a2, a3;
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("Metallica", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", makeTrackProperties("!etallica", 2006, "A", "2", "A Two @bad")); // should lose
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("Metallica", 2006, "A", "2", "A Two")); // should win - same album info as other tracks
		mtpr.addMockResult("A/a2", makeTrackProperties("Zetallica", 2006, "A", "2", "A Two @bad")); // should lose
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("Metallica", 2006, "A", "3", "A Three"));
		engine.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1);
		assertEngineTrackProperties("A/a2", a2);
		assertEngineTrackProperties("A/a3", a3);
		assertEngineTrackProperties("B/b1", noprop);
		assertEngineTrackProperties("B/b2", noprop);
		assertEngineTrackProperties("B/b3", noprop);
	}

	@Test
	public void wholeDirHasMultipleResults_checksArtistAgainstSuccessfulDirs() {
		addFakeDirsToEngine();
		TrackProperties a1, a2, a3, b1, b2, b3;
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("METALLICA", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("METALLICA", 2006, "A", "2", "A Two"));
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("METALLICA", 2006, "A", "3", "A Three"));
		mtpr.addMockResult("B/b1", makeTrackProperties("Bullshit", 1997, "B", "1", "B One @bad"));
		mtpr.addMockResult("B/b2", makeTrackProperties("Bullshit", 1997, "B", "2", "B Two @bad"));
		mtpr.addMockResult("B/b3", makeTrackProperties("Bullshit", 1997, "B", "3", "B Three @bad"));
		mtpr.addMockResult("B/b1", b1= makeTrackProperties("Metallica", 1997, "B", "1", "B One @good"));
		mtpr.addMockResult("B/b2", b2= makeTrackProperties("Metallica", 1997, "B", "2", "B Two @good"));
		mtpr.addMockResult("B/b3", b3= makeTrackProperties("Metallica", 1997, "B", "3", "B Three @good"));
		engine.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1);
		assertEngineTrackProperties("A/a2", a2);
		assertEngineTrackProperties("A/a3", a3);
		assertEngineTrackProperties("B/b1", b1);
		assertEngineTrackProperties("B/b2", b2);
		assertEngineTrackProperties("B/b3", b3);
		assertEngineTrackProperties("C/c1", noprop);
		assertEngineTrackProperties("C/c2", noprop);
		assertEngineTrackProperties("C/c3", noprop);
	}

	@Test
	public void wholeDirHasMultipleResults_checksArtistAgainstOtherPendingDirs() {
		addFakeDirsToEngine();
		TrackProperties a1, a2, a3, b1, b2, b3;
		mtpr.addMockResult("A/a1", makeTrackProperties("Bullshit", 2005, "B", "1", "B One @bad"));
		mtpr.addMockResult("A/a2", makeTrackProperties("Bullshit", 2005, "B", "2", "B Two @bad"));
		mtpr.addMockResult("A/a3", makeTrackProperties("Bullshit", 2005, "B", "3", "B Three @bad"));
		mtpr.addMockResult("B/b1", makeTrackProperties("Crap", 1997, "B", "1", "B One @bad"));
		mtpr.addMockResult("B/b2", makeTrackProperties("Crap", 1997, "B", "2", "B Two @bad"));
		mtpr.addMockResult("B/b3", makeTrackProperties("Crap", 1997, "B", "3", "B Three @bad"));
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("METALLICA", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("METALLICA", 2006, "A", "2", "A Two"));
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("METALLICA", 2006, "A", "3", "A Three"));
		mtpr.addMockResult("B/b1", b1= makeTrackProperties("Metallica", 1997, "B", "1", "B One @good"));
		mtpr.addMockResult("B/b2", b2= makeTrackProperties("Metallica", 1997, "B", "2", "B Two @good"));
		mtpr.addMockResult("B/b3", b3= makeTrackProperties("Metallica", 1997, "B", "3", "B Three @good"));
		engine.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1);
		assertEngineTrackProperties("A/a2", a2);
		assertEngineTrackProperties("A/a3", a3);
		assertEngineTrackProperties("B/b1", b1);
		assertEngineTrackProperties("B/b2", b2);
		assertEngineTrackProperties("B/b3", b3);
		assertEngineTrackProperties("C/c1", noprop);
		assertEngineTrackProperties("C/c2", noprop);
		assertEngineTrackProperties("C/c3", noprop);
	}

	@Test
	public void wholeDirHasMultipleResults_checksAlbumDataOfOtherTracks() {
		// || ARTIST       | RANK || YEAR | RANK || ALBUM     | RANK ||
		// ||--------------|------||------|------||-----------|------||
		// || Bullshit     | 1    || 2006 | 5    || A         | 4    ||
		// || Metallica    | 2    || 1980 | 1    || FakeAlbum | 1    ||
		// || Crap         | 1    ||
		// || WhatCanYouDo | 1    ||
		// || WayOff       | 1    ||
		//
		// A/a1
		//   11 (2+5+4) - "Metallica", 2006, "A"
		//   10 (1+5+4) - "Bullshit", 2006, "A"
		// A/a2
		//   11 (2+5+4) - "Metallica", 2006, "A"
		//   10 (1+5+4) - "Crap", 2006, "A"
		// A/a3
		//   7 (1+5+1) - "WhatCanYouDo", 2006, "FakeAlbum"
		//   2 (1+1+0) - "WayOff", 1980, null
		addFakeDirsToEngine();
		TrackProperties a1, a2, a3;
		mtpr.addMockResult("A/a1", makeTrackProperties("Bullshit", 2006, "A", "2", "A Two @bad"));
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("Metallica", 2006, "A", "1", "A One @good"));
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("Metallica", 2006, "A", "2", "A Two @good"));
		mtpr.addMockResult("A/a2", makeTrackProperties("Crap", 2006, "A", "2", "A Two @bad"));
		mtpr.addMockResult("A/a3", makeTrackProperties("WayOff", 1980, null, "3", "A Three"));
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("WhatCanYouDo", 2006, "FakeAlbum", "3", "A Three"));
		engine.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1);
		assertEngineTrackProperties("A/a2", a2);
		assertEngineTrackProperties("A/a3", a3);
		assertEngineTrackProperties("B/b1", noprop);
		assertEngineTrackProperties("B/b2", noprop);
		assertEngineTrackProperties("B/b3", noprop);
	}

	@Test
	public void wholeDirHasMultipleResults_noWayOfKnowingWhichIsCorrect() {
		addFakeDirsToEngine();
		TrackProperties a1a, a2a, a3a, a1b, a2b, a3b;
		mtpr.addMockResult("A/a1", a1a= makeTrackProperties("METALLICA", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", a2a= makeTrackProperties("METALLICA", 2006, "A", "2", "A Two"));
		mtpr.addMockResult("A/a1", a1b= makeTrackProperties("Nirvana", 1994, "B", "7", "B One"));
		mtpr.addMockResult("A/a2", a2b= makeTrackProperties("Nirvana", 1994, "B", "8", "B Two"));
		mtpr.addMockResult("A/a3", a3b= makeTrackProperties("Nirvana", 1994, "B", "9", "B Three"));
		mtpr.addMockResult("A/a3", a3a= makeTrackProperties("METALLICA", 2006, "A", "3", "A Three"));
		engine.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1a, a1b);
		assertEngineTrackProperties("A/a2", a2a, a2b);
		assertEngineTrackProperties("A/a3", a3a, a3b);
		assertEngineTrackProperties("C/c1", noprop);
		assertEngineTrackProperties("C/c2", noprop);
		assertEngineTrackProperties("C/c3", noprop);
		AlbumData ad1= engine.files.get("A\\a1.mp3").getAlbumData();
		AlbumData ad2= engine.files.get("A\\a2.mp3").getAlbumData();
		AlbumData ad3= engine.files.get("A\\a3.mp3").getAlbumData();
		assertEquals(ad1, ad2);
		assertEquals(ad1, ad3);
	}

	// TODO Add more TrackProperty selection tests: check for matches from different sources
	// TODO Add more TrackProperty selection tests: choose by number of complete fields

	// =============================================================================================== //
	// = Public
	// =============================================================================================== //

	@Test
	public void testDaVoodoo_overwriteNull() throws IOException, URISyntaxException {
		testDaVoodoo(null);
	}

	@Test
	public void testDaVoodoo_overwriteTrue() throws IOException, URISyntaxException {
		testDaVoodoo(true);
	}

	@Test
	public void testDaVoodoo_overwriteFalse() throws IOException, URISyntaxException {
		testDaVoodoo(false);
	}

	private void createFile(String filename, String content) throws IOException {
		File f= new File(filename);
		Helpers.mkdir_p(f.getParentFile());
		if (f.isFile())
			f.delete();
		BufferedWriter out= new BufferedWriter(new FileWriter(f));
		out.write(content);
		out.close();
	}

	public void testDaVoodoo(final Boolean overwriteAll) throws IOException, URISyntaxException {
		final boolean oldFilesRemain= (overwriteAll != null && !overwriteAll);
		String sourceDir= prepareVoodooTestSourceDir("sample_data");
		String targetDir= prepareVoodooTestTargetDir();
		if (overwriteAll != null) {
			createFile(addPathElements(targetDir, "Children Of Bodom", "2005 - Are You Dead Yet_", "01 - Living Dead Beat.mp3"), "overwrite1");
			createFile(addPathElements(targetDir, "Children Of Bodom", "2005 - Are You Dead Yet_", "autotag.txt"), "o2!");
		}
		mtpr.addMockResult(addPathElements(sourceDir, "complete", "blah", "01"), makeTrackProperties("Children Of Bodom", 2005, "Are You Dead Yet?", "1", "Living Dead Beat"));
		mtpr.addMockResult(addPathElements(sourceDir, "complete", "blah", "02"), makeTrackProperties("Children Of Bodom", 2005, "Are You Dead Yet?", "2", "Are You Dead Yet?"));
		mtpr.addMockResult(addPathElements(sourceDir, "complete", "blah", "14"), makeTrackProperties("Children Of Bodom", 2005, "Are You Dead Yet?", "14", "Needled 24/7"));
		mtpr.addMockResult(addPathElements(sourceDir, "incomplete", "02"), makeTrackProperties("Children Of Bodom", 2003, "Hate Crew Deathroll", "02", "incomplete dir"));
		mtpr.addMockResult(addPathElements(sourceDir, "other", "remain", "asd"), makeTrackProperties("Children Of Bodom", 2003, "Hate Crew Deathroll", "01", "Angels Don't Kill"));
		engine.addFolder(sourceDir);
		int fileCount= 1 + 7 + 1 + 3 + 5;
		assertEquals(fileCount, engine.files.size());
		engine.files.get(addPathElements(sourceDir, "complete", "blah", "www.heavytorrents.org.txt")).setMarkedForDeletion(true);
		engine.files.get(addPathElements(sourceDir, "complete", "blah", "VICL-35940.jpg")).setMarkedForDeletion(true);
		engine.files.get(addPathElements(sourceDir, "incomplete", "crap.txt")).setMarkedForDeletion(true);
		engine.files.get(addPathElements(sourceDir, "other", "del_all", "byebye.jpg")).setMarkedForDeletion(true);
		engine.files.get(addPathElements(sourceDir, "other", "remain", "delme.txt")).setMarkedForDeletion(true);
		engine.remove(addPathElements(sourceDir, "other", "remain", "remain.mp3"));
		fileCount--;
		assertEquals(fileCount, engine.files.size());
		assertFalse(engine.files.get(addPathElements(sourceDir, "incomplete", "01.mp3")).isComplete(true));
		assertTrue(engine.files.get(addPathElements(sourceDir, "incomplete", "02.mp3")).isComplete(true));

		engine.doYaVoodoo(targetDir, new MockVoodooProgressMonitor(), overwriteAll);

		// Test target dir
		assertDirContents(targetDir, "Children Of Bodom");
		String tdir= addPathElements(targetDir, "Children Of Bodom");
		assertDirContents(tdir, "2003 - Hate Crew Deathroll", "2005 - Are You Dead Yet_");
		// Children Of Bodom/2003 - Hate Crew Deathroll
		assertDirContents(addPathElements(tdir, "2003 - Hate Crew Deathroll"), "01 - Angels Don't Kill.mp3");
		// Children Of Bodom/2005 - Are You Dead Yet_
		tdir= addPathElements(tdir, "2005 - Are You Dead Yet_");
		assertDirContents(tdir, "01 - Living Dead Beat.mp3", "02 - Are You Dead Yet_.mp3", "14 - Needled 24_7.mp3", "autotag.txt", "cover.jpg");
		assertEquals(oldFilesRemain ? 10L : 1187L, new File(addPathElements(tdir, "01 - Living Dead Beat.mp3")).length());
		assertEquals(2L, new File(addPathElements(tdir, "02 - Are You Dead Yet_.mp3")).length());
		assertEquals(6L, new File(addPathElements(tdir, "14 - Needled 24_7.mp3")).length());
		assertEquals(oldFilesRemain ? 3L : 81L, new File(addPathElements(tdir, "autotag.txt")).length());
		assertEquals("14ness", new BufferedReader(new InputStreamReader(new FileInputStream(new File(addPathElements(tdir, "14 - Needled 24_7.mp3"))), "ASCII")).readLine());
		assertEquals(oldFilesRemain ? "o2!" : "ARTIST:  AC-DC", new BufferedReader(new InputStreamReader(new FileInputStream(new File(addPathElements(tdir, "autotag.txt"))), "UTF-8")).readLine());

		// Test engine.dirs + engine.files
		assertEquals(1 + 5 + (oldFilesRemain ? 2 : 0), engine.files.size());
		assertEquals(2 + (oldFilesRemain ? 1 : 0), engine.dirs.size());
		assertTrue(engine.dirs.containsKey(sourceDir));
		assertTrue(engine.dirs.containsKey(addPathElements(sourceDir, "incomplete")));
		assertTrue(engine.files.containsKey(addPathElements(sourceDir, "asd.txt")));
		assertEquals(oldFilesRemain, engine.dirs.containsKey(addPathElements(sourceDir, "complete", "blah")));

		// Test source dir
		assertDirContents(sourceDir, "asd.txt", "incomplete", "other", oldFilesRemain ? "complete" : null);
		// complete/blah
		if (oldFilesRemain)
			assertDirContents(addPathElements(sourceDir, "complete", "blah"), "01.mp3", "autotag.txt");
		// incomplete
		assertDirContents(addPathElements(sourceDir, "incomplete"), "01.mp3", "02.mp3", "autotag.txt", "cover.jpg", "crap.txt");
		// other
		tdir= addPathElements(sourceDir, "other");
		assertDirContents(tdir, "empty", "remain");
		// other/empty
		assertDirContents(addPathElements(tdir, "empty"));
		// other/remain
		assertDirContents(addPathElements(tdir, "remain"), "remain.mp3");
	}

	// =============================================================================================== //
	// = Private
	// =============================================================================================== //

	private void addFakeDirsToEngine() {
		engine.addFakeDir("A", "a1.mp3", "a2.mp3", "a3.mp3");
		engine.addFakeDir("B", "b1.mp3", "b2.mp3", "b3.mp3");
		engine.addFakeDir("C", "c1.mp3", "c2.mp3", "c3.mp3");
	}

	private void assertDirContents(String dir, String... expectedFiles) {
		File d= new File(dir);
		if (!d.isDirectory())
			fail("Directory " + dir + " expected but not found.");

		Set<String> expectedFileSet= Helpers.arrayToSet(expectedFiles);
		expectedFileSet.remove(null);
		String[] expectedFiles2= expectedFileSet.toArray(new String[expectedFileSet.size()]);
		String[] actual= d.list();
		Arrays.sort(expectedFiles2);
		Arrays.sort(actual);
		//		assertEquals(expectedFiles2, actual);
		assertEquals(Arrays.deepToString(expectedFiles2), Arrays.deepToString(actual));
	}

	private void assertEngineTrackProperties(String filename, TrackProperties expected) {
		assertEngineTrackProperties(filename, expected, null);
	}

	private void assertEngineTrackProperties(String filename, TrackProperties expected1, TrackProperties expected2) {
		filename= osCompatFilename(filename) + ".mp3"; //$NON-NLS-1$
		FileData fd= engine.files.get(filename);
		if (fd == null) {
			System.err.println("assertEngineTrackProperties failed: key not found: " + filename);
			System.err.println("  keys: " + Helpers.sort(engine.files.keySet()));
			fail("assertEngineTrackProperties failed: key not found: " + filename);
		}
		TrackProperties test= TrackProperties.fromFileData(fd);
		if (expected1.equals(test))
			return;
		if (expected2 != null && expected2.equals(test))
			return;

		System.err.println("assertEngineTrackProperties failed.");
		System.err.println("  expected: " + expected1);
		if (expected2 != null)
			System.err.println("  expected: " + expected2);
		System.err.println("  found   : " + test);
		fail("assertEngineTrackProperties failed.");
	}

	private String prepareVoodooTestSourceDir(String sampleDataDir) throws IOException, URISyntaxException {
		File targetDir= new File(addPathElements(Helpers.getSystemTempDir(), "tanuki_test_da_voodoo", "source"));
		if (targetDir.exists())
			Helpers.rm_rf(targetDir);
		Helpers.cp_r(new File(EngineTest.class.getResource(sampleDataDir).toURI()), targetDir, true, ".svn");
		return targetDir.toString();
	}

	private String prepareVoodooTestTargetDir() throws IOException {
		File targetDir= new File(addPathElements(Helpers.getSystemTempDir(), "tanuki_test_da_voodoo", "target"));
		if (targetDir.exists())
			Helpers.rm_rf(targetDir);
		return targetDir.toString();
	}
}
