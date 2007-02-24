package golly.tanuki2.qa;

import golly.tanuki2.core.Engine;
import golly.tanuki2.core.ITrackProprtyReader;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.support.Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
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
		addFakeDir("A", "a1.mp3", "a2.mp3", "a3.mp3");
		addFakeDir("B", "b1.mp3", "b2.mp3", "b3.mp3");
		addFakeDir("C", "c1.mp3", "c2.mp3", "c3.mp3");
	}

	private void addFakeDir(String path, String... filenames) {
		DirData dd= new DirData(path);
		for (String f : filenames) {
			FileData fd= new FileData(dd);
			fd.setAudio(true);
			files.put(Helpers.addPathElement(path, f), fd);
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
			r.put(f, readTrackProperties(Helpers.addPathElement(dd.dir, f)));
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
	public void eachTrackHasOneResult() {
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

	@Ignore
	@Test
	public void wholeDirHasMultipleResults_noWayOfKnowingWhichIsCorrect() {
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
}
