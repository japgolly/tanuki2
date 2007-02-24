package golly.tanuki2.qa;

import golly.tanuki2.core.Engine;
import golly.tanuki2.core.ITrackProprtyReader;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.support.Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public void testSimple() {
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
	public void testMultipleResultsForSingleTrack_selectsByUsingOthersInSameDir() {
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
	public void testMultipleResultsForWholeDir_selectsByCheckingArtistOfOtherDirs() {
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

	private void assertEngineTrackProperties(String filename, TrackProperties expected) {
		filename= osCompatFilename(filename) + ".mp3"; //$NON-NLS-1$
		FileData fd= engine.files.get(filename);
		if (fd == null) {
			System.err.println("assertEngineTrackProperties failed: key not found: " + filename);
			System.err.println("  keys: " + Helpers.sort(engine.files.keySet()));
			fail("assertEngineTrackProperties failed: key not found: " + filename);
		}
		TrackProperties test= TrackProperties.fromFileData(fd);
		if (expected.equals(test))
			return;

		System.err.println("assertEngineTrackProperties failed.");
		System.err.println("  expected: " + expected);
		System.err.println("  found   : " + test);
		fail("assertEngineTrackProperties failed.");
	}
}
