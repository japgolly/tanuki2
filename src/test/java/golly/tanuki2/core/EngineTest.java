package golly.tanuki2.core;

import static golly.tanuki2.support.Helpers.addPathElements;
import static golly.tanuki2.support.Helpers.ensureCorrectDirSeperators;
import golly.tanuki2.TestHelper;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackPropertyMap;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.RuntimeConfig;
import golly.tanuki2.support.TanukiImage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Golly
 * @since 24/02/2007
 */
@SuppressWarnings("nls")
public class EngineTest extends TestHelper {
	private Engine engine;
	private Engine2 engine2;
	private MockTrackProprtyReader mtpr;
	private MockTrackProprtyReader mtpr1, mtpr2;
	private TrackPropertyMap noprop= makeTrackProperties(null, null, null, null, null);

	@Before
	public void setup() {
		RuntimeConfig.getInstance().autoTitleCase= false;
		mtpr= mtpr1= new MockTrackProprtyReader();
		mtpr2= null;
		engine= engine2= new Engine2(mtpr);
	}

	@After
	public void teardown() throws IOException {
		System.gc(); // Or else certain File objects can still be in memory causing deletes to fail because handles are still held :S 
		removeDirectoryIfExists(getTestSourceDir());
		removeDirectoryIfExists(getTestTargetDir());
	}

	@Test
	public void makeSureRemoveUpdatesTheHasAudioProperty() {
		engine2.addFakeDir("A", "a1.mp3", "a2.mp3", "as.txt");
		engine2.files.get(addPathElements("A", "as.txt")).setAudio(false);
		engine2.files.get(addPathElements("A", "as.txt")).setMimeImage(TanukiImage.MIME_TEXT);
		final DirData dd= engine2.dirs.get("A");
		assertTrue(dd.hasAudioContent(true));
		assertTrue(dd.hasAudioContent(false));
		engine2.remove(addPathElements("A", "a2.mp3"));
		assertTrue(dd.hasAudioContent(true));
		assertTrue(dd.hasAudioContent(false));
		engine2.remove(addPathElements("A", "a1.mp3"));
		assertFalse(dd.hasAudioContent(true));
		assertFalse(dd.hasAudioContent(false));
	}

	@Test
	public void testAddingSingleFilesAfterThatDirAlreadyExistsInMemory() throws IOException, URISyntaxException {
		Engine engine= new Engine();
		final String sourceDir= prepareVoodooTestSourceDir("sample_data2");
		final String octo= addPathElements(sourceDir, "Dream Theater", "2005 - Octavarium");
		engine.add(octo);
		assertEquals(4, engine.files.size());
		assertTrue(engine.files.get(addPathElements(octo, "08. Octavarium.mp3")).isComplete(true));

		engine.remove(addPathElements(octo, "08. Octavarium.mp3"));
		engine.remove(addPathElements(octo, "del_me.txt"));
		assertEquals(2, engine.files.size());

		FileData fd5= engine.files.get(addPathElements(octo, "05 - Panic Attack.mp3"));
		fd5.getAlbumData().setAlbum("xxx");
		fd5.getAlbumData().setYear((String) null);
		fd5.setTrack("mah mah");
		final String fd5str= fd5.toString();

		engine.add(addPathElements(octo, "08. Octavarium.mp3"), addPathElements(octo, "del_me.txt"));
		assertEquals(4, engine.files.size());
		assertEquals(fd5str, engine.files.get(addPathElements(octo, "05 - Panic Attack.mp3")).toString());
		FileData fd8= new FileData(engine.dirs.get(octo));
		fd8.setAudio(true);
		fd8.setMarkedForDeletion(false);
		fd8.setMimeImage(TanukiImage.MIME_AUDIO);
		fd8.setTn(8);
		fd8.setTrack("Octavarium");
		fd8.setSize(6);
		AlbumData ad8= new AlbumData();
		ad8.setArtist("Dream Theater");
		ad8.setAlbum("Octavarium");
		ad8.setYear(2005);
		fd8.setAlbumData(ad8);
		assertEquals(fd8.toString(), engine.files.get(addPathElements(octo, "08. Octavarium.mp3")).toString());
	}

	// =============================================================================================== //
	// = TP selection tests
	// =============================================================================================== //

	@Test
	public void eachTrackHasOneResult() {
		addFakeDirsToEngine();
		TrackPropertyMap a1, a2, a3;
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("Metallica", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("Metallica", 2006, "A", "2", "A Two"));
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("Metallica", 2006, "A", "3", "A Three"));
		engine2.readTrackProprties2();
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
		TrackPropertyMap a1, a2, a3;
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("Metallica", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", makeTrackProperties("!etallica", 2006, "A", "2", "A Two @bad")); // should lose
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("Metallica", 2006, "A", "2", "A Two")); // should win - same
		// album info as
		// other tracks
		mtpr.addMockResult("A/a2", makeTrackProperties("Zetallica", 2006, "A", "2", "A Two @bad")); // should lose
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("Metallica", 2006, "A", "3", "A Three"));
		engine2.readTrackProprties2();
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
		TrackPropertyMap a1, a2, a3, b1, b2, b3;
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("METALLICA", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("METALLICA", 2006, "A", "2", "A Two"));
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("METALLICA", 2006, "A", "3", "A Three"));
		mtpr.addMockResult("B/b1", makeTrackProperties("Bullshit", 1997, "B", "1", "B One @bad"));
		mtpr.addMockResult("B/b2", makeTrackProperties("Bullshit", 1997, "B", "2", "B Two @bad"));
		mtpr.addMockResult("B/b3", makeTrackProperties("Bullshit", 1997, "B", "3", "B Three @bad"));
		mtpr.addMockResult("B/b1", b1= makeTrackProperties("Metallica", 1997, "B", "1", "B One @good"));
		mtpr.addMockResult("B/b2", b2= makeTrackProperties("Metallica", 1997, "B", "2", "B Two @good"));
		mtpr.addMockResult("B/b3", b3= makeTrackProperties("Metallica", 1997, "B", "3", "B Three @good"));
		engine2.readTrackProprties2();
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
		TrackPropertyMap a1, a2, a3, b1, b2, b3;
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
		engine2.readTrackProprties2();
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
		// || ARTIST | RANK || YEAR | RANK || ALBUM | RANK ||
		// ||--------------|------||------|------||-----------|------||
		// || Bullshit | 1 || 2006 | 5 || A | 4 ||
		// || Metallica | 2 || 1980 | 1 || FakeAlbum | 1 ||
		// || Crap | 1 ||
		// || WhatCanYouDo | 1 ||
		// || WayOff | 1 ||
		//
		// A/a1
		// 11 (2+5+4) - "Metallica", 2006, "A"
		// 10 (1+5+4) - "Bullshit", 2006, "A"
		// A/a2
		// 11 (2+5+4) - "Metallica", 2006, "A"
		// 10 (1+5+4) - "Crap", 2006, "A"
		// A/a3
		// 7 (1+5+1) - "WhatCanYouDo", 2006, "FakeAlbum"
		// 2 (1+1+0) - "WayOff", 1980, null
		addFakeDirsToEngine();
		TrackPropertyMap a1, a2, a3;
		mtpr.addMockResult("A/a1", makeTrackProperties("Bullshit", 2006, "A", "2", "A Two @bad"));
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("Metallica", 2006, "A", "1", "A One @good"));
		mtpr.addMockResult("A/a2", a2= makeTrackProperties("Metallica", 2006, "A", "2", "A Two @good"));
		mtpr.addMockResult("A/a2", makeTrackProperties("Crap", 2006, "A", "2", "A Two @bad"));
		mtpr.addMockResult("A/a3", makeTrackProperties("WayOff", 1980, null, "3", "A Three"));
		mtpr.addMockResult("A/a3", a3= makeTrackProperties("WhatCanYouDo", 2006, "FakeAlbum", "3", "A Three"));
		engine2.readTrackProprties2();
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
		TrackPropertyMap a1a, a2a, a3a, a1b, a2b, a3b;
		mtpr.addMockResult("A/a1", a1a= makeTrackProperties("METALLICA", 2006, "A", "1", "A One"));
		mtpr.addMockResult("A/a2", a2a= makeTrackProperties("METALLICA", 2006, "A", "2", "A Two"));
		mtpr.addMockResult("A/a1", a1b= makeTrackProperties("Nirvana", 1994, "B", "7", "B One"));
		mtpr.addMockResult("A/a2", a2b= makeTrackProperties("Nirvana", 1994, "B", "8", "B Two"));
		mtpr.addMockResult("A/a3", a3b= makeTrackProperties("Nirvana", 1994, "B", "9", "B Three"));
		mtpr.addMockResult("A/a3", a3a= makeTrackProperties("METALLICA", 2006, "A", "3", "A Three"));
		engine2.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1a, a1b);
		assertEngineTrackProperties("A/a2", a2a, a2b);
		assertEngineTrackProperties("A/a3", a3a, a3b);
		assertEngineTrackProperties("C/c1", noprop);
		assertEngineTrackProperties("C/c2", noprop);
		assertEngineTrackProperties("C/c3", noprop);
		final String aDir= "A" + File.separator;
		AlbumData ad1= engine2.files.get(aDir + "a1.mp3").getAlbumData();
		AlbumData ad2= engine2.files.get(aDir + "a2.mp3").getAlbumData();
		AlbumData ad3= engine2.files.get(aDir + "a3.mp3").getAlbumData();
		assertEquals(ad1, ad2);
		assertEquals(ad1, ad3);
	}

	@Test
	public void oneTrack_mostCompleteFields() {
		addFakeDirsToEngine();
		TrackPropertyMap a1;
		mtpr.addMockResult("A/a1", makeTrackProperties("Bullshit", null, "No year", "2", "A Two @bad"));
		mtpr.addMockResult("A/a1", a1= makeTrackProperties("asd", 2006, "qwe", "2", "A Two @bad"));
		engine2.readTrackProprties2();
		assertEngineTrackProperties("A/a1", a1);
	}

	@Test
	public void multipleSources_simple() {
		useTwoTrackReaders();
		addFakeDirsToEngine();
		mtpr1.addMockResult("A/a1", makeTrackProperties("some bullshit", null, null, null, null));
		mtpr1.addMockResult("A/a1", makeTrackProperties("asd", 2006, "qwe", "2", null));
		mtpr2.addMockResult("A/a1", makeTrackProperties("asd", null, "qwe", "2", "aahh"));
		engine2.readTrackProprties2();
		assertEngineTrackProperties("A/a1", makeTrackProperties("asd", 2006, "qwe", "2", "aahh"));
	}

	@Test
	public void multipleSources_prefersHighlyRankedRows_singleTrack() {
		useTwoTrackReaders();
		addFakeDirsToEngine();
		mtpr1.addMockResult("A/a1", makeTrackProperties("some bullshit", 1995, null, null, "rubbish"));
		mtpr1.addMockResult("A/a1", makeTrackProperties("asd", 2006, "qwe", "2", null));
		mtpr2.addMockResult("A/a1", makeTrackProperties("ASD", null, "QWE", null, "AAHH"));
		mtpr2.addMockResult("A/a1", makeTrackProperties("not gonna happen", 1995, "hehe", null, "more rubbish"));
		engine2.readTrackProprties2();
		assertEngineTrackProperties("A/a1", makeTrackProperties("asd", 2006, "qwe", "2", "AAHH"));
	}

	@Test
	public void multipleSources_prefersHighlyRankedRows_multipleTracks() {
		useTwoTrackReaders();
		addFakeDirsToEngine();

		mtpr1.addMockResult("A/a1", makeTrackProperties("machine head", null, "the blackening", null, "dissent"));
		mtpr1.addMockResult("A/a1", makeTrackProperties("INCORRECT", 1995, "INCORRECT", "8", "INCORRECT"));
		mtpr2.addMockResult("A/a1", makeTrackProperties("Machine Head", 2007, null, null, null));
		mtpr2.addMockResult("A/a1", makeTrackProperties(null, null, null, "1", "DISSENT"));

		mtpr1.addMockResult("A/a2", makeTrackProperties("machine head", null, "the blackening", null, "mourning"));
		mtpr1.addMockResult("A/a2", makeTrackProperties("INCORRECT", 1995, "INCORRECT", "8", "INCORRECT"));
		mtpr2.addMockResult("A/a2", makeTrackProperties("Machine Head", 2007, null, null, null));
		mtpr2.addMockResult("A/a2", makeTrackProperties(null, null, null, "2", "MOURNING"));

		RuntimeConfig.getInstance().autoTitleCase= true;
		engine2.readTrackProprties2();
		assertEngineTrackProperties("A/a1", makeTrackProperties("Machine Head", 2007, "The Blackening", "1", "Dissent"));
		assertEngineTrackProperties("A/a2", makeTrackProperties("Machine Head", 2007, "The Blackening", "2", "Mourning"));
	}

	@Test
	public void shouldTrimAndIgnoreEmptyStrings() {
		addFakeDirsToEngine();
		mtpr1.addMockResult("A/a1", makeTrackProperties("\0asd hehe　 ", 2006, "", " 2", "　 \0"));
		engine2.readTrackProprties2();
		assertEngineTrackProperties("A/a1", makeTrackProperties("asd hehe", 2006, null, "2", null));
	}

	@Test
	public void testTPSelectionMisc1() {
		DirData dd= new DirData(ensureCorrectDirSeperators("X:\\music\\1. Fresh\\Hawaii - The Natives Are Restless (1985) 320 Kbps"));
		final String fn1= "Hawaii - 01 - Call Of The Wild - The Natives Are Restless.mp3";
		final String fn2= "Hawaii - 02 - Turn It Louder - The Natives Are Restless.mp3";
		final String fn3= "Hawaii - 03 - V.P.H.B. - The Natives Are Restless.mp3";
		final String fn4= "Hawaii - 04 - Beg For Mercy - The Natives Are Restless.mp3";
		final String fn5= "Hawaii - 05 - Unfinished Business - The Natives Are Restless.mp3";
		final String fn6= "Hawaii - 06 - Proud To Be Loud - The Natives Are Restless.mp3";
		final String fn7= "Hawaii - 07 - Lies - The Natives Are Restless.mp3";
		final String fn8= "Hawaii - 08 - Omichan No Uta - The Natives Are Restless.mp3";
		final String fn9= "Hawaii - 09 - Dynamite - The Natives Are Restless.mp3";
		final String[] fnAll= new String[] {fn1, fn2, fn3, fn4, fn5, fn6, fn7, fn8, fn9};
		for (String f : fnAll) {
			makeFileData(dd, f, true);
		}
		dd.files.put("Hawaii - 00 - The Natives Are Restless.nfo", makeFileData(dd, false));
		dd.files.put("Hawaii - The Natives Are Restless.jpg", makeFileData(dd, false));

		Engine3 engine3= new Engine3();
		engine= engine3;
		engine3.dirs.put(dd.dir, dd);
		for (String f : fnAll) {
			engine3.files.put(addPathElements(dd.dir, f), dd.files.get(f));
		}
		engine3.addDirNeedingTrackProprties(dd);
		engine3.readAndAssignTrackProprties2();
		System.out.flush();

		assertEngineTrackProperties(addPathElements(dd.dir, fn1), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "1", "Call Of The Wild"));
		assertEngineTrackProperties(addPathElements(dd.dir, fn2), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "2", "Turn It Louder"));
		assertEngineTrackProperties(addPathElements(dd.dir, fn3), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "3", "V.P.H.B."));
		assertEngineTrackProperties(addPathElements(dd.dir, fn4), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "4", "Beg For Mercy"));
		assertEngineTrackProperties(addPathElements(dd.dir, fn5), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "5", "Unfinished Business"));
		assertEngineTrackProperties(addPathElements(dd.dir, fn6), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "6", "Proud To Be Loud"));
		assertEngineTrackProperties(addPathElements(dd.dir, fn7), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "7", "Lies"));
		assertEngineTrackProperties(addPathElements(dd.dir, fn8), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "8", "Omichan No Uta"));
		assertEngineTrackProperties(addPathElements(dd.dir, fn9), makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "9", "Dynamite"));
	}

	// =============================================================================================== //
	// = Voodoo tests
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

	public void testDaVoodoo(final Boolean overwriteAll) throws IOException, URISyntaxException {
		final boolean oldFilesRemain= (overwriteAll != null && !overwriteAll);
		String sourceDir= prepareVoodooTestSourceDir("sample_data");
		String targetDir= prepareVoodooTestTargetDir();
		if (overwriteAll != null) {
			createFile(addPathElements(targetDir, "Children Of Bodom", "2005 - Are You Dead Yet_", "01. Living Dead Beat.mp3"), "overwrite1");
			createFile(addPathElements(targetDir, "Children Of Bodom", "2005 - Are You Dead Yet_", "autotag.txt"), "o2!");
		}
		mtpr.addMockResult(addPathElements(sourceDir, "complete", "blah", "01"), makeTrackProperties("Children Of Bodom", 2005, "Are You Dead Yet?", "1", "Living Dead Beat"));
		mtpr.addMockResult(addPathElements(sourceDir, "complete", "blah", "02"), makeTrackProperties("Children Of Bodom", 2005, "Are You Dead Yet?", "2", "Are You Dead Yet?"));
		mtpr.addMockResult(addPathElements(sourceDir, "complete", "blah", "14"), makeTrackProperties("Children Of Bodom", 2005, "Are You Dead Yet?", "14", "Needled 24/7"));
		mtpr.addMockResult(addPathElements(sourceDir, "incomplete", "1", "02"), makeTrackProperties("Children Of Bodom", 2003, "Hate Crew Deathroll", "02", "incomplete dir"));
		mtpr.addMockResult(addPathElements(sourceDir, "incomplete", "2", "201"), makeTrackProperties("Incomplete 2", null, "blah", "01", "incomplete dir"));
		mtpr.addMockResult(addPathElements(sourceDir, "incomplete", "2", "202"), makeTrackProperties("Incomplete 2", null, "blah", "02", "incomplete dir"));
		mtpr.addMockResult(addPathElements(sourceDir, "other", "remain", "asd"), makeTrackProperties("Children Of Bodom", 2003, "Hate Crew Deathroll", "01", "Angels Don't Kill"));
		engine2.add(sourceDir);
		int fileCount= 1 + 7 + 1 + 3 + 5 + 4;
		assertEquals(fileCount, engine2.files.size());
		engine2.files.get(addPathElements(sourceDir, "complete", "blah", "www.heavytorrents.org.txt")).setMarkedForDeletion(true);
		engine2.files.get(addPathElements(sourceDir, "complete", "blah", "VICL-35940.jpg")).setMarkedForDeletion(true);
		engine2.files.get(addPathElements(sourceDir, "incomplete", "1", "crap.txt")).setMarkedForDeletion(true);
		engine2.files.get(addPathElements(sourceDir, "incomplete", "2", "crap.txt")).setMarkedForDeletion(true);
		engine2.files.get(addPathElements(sourceDir, "other", "del_all", "byebye.jpg")).setMarkedForDeletion(true);
		engine2.files.get(addPathElements(sourceDir, "other", "remain", "delme.txt")).setMarkedForDeletion(true);
		engine2.remove(addPathElements(sourceDir, "other", "remain", "remain.mp3"));
		fileCount--;
		assertEquals(fileCount, engine2.files.size());
		assertFalse(engine2.files.get(addPathElements(sourceDir, "incomplete", "1", "01.mp3")).isComplete(true));
		assertTrue(engine2.files.get(addPathElements(sourceDir, "incomplete", "1", "02.mp3")).isComplete(true));
		assertFalse(engine2.files.get(addPathElements(sourceDir, "incomplete", "2", "201.mp3")).isComplete(true));

		engine2.doYaVoodoo(targetDir, new MockVoodooProgressMonitor(), overwriteAll);

		// Test target dir
		assertDirContents(targetDir, "Children Of Bodom");
		String tdir= addPathElements(targetDir, "Children Of Bodom");
		assertDirContents(tdir, "2003 - Hate Crew Deathroll", "2005 - Are You Dead Yet_");
		// Children Of Bodom/2003 - Hate Crew Deathroll
		assertDirContents(addPathElements(tdir, "2003 - Hate Crew Deathroll"), "01. Angels Don't Kill.mp3");
		// Children Of Bodom/2005 - Are You Dead Yet_
		tdir= addPathElements(tdir, "2005 - Are You Dead Yet_");
		assertDirContents(tdir, "01. Living Dead Beat.mp3", "02. Are You Dead Yet_.mp3", "14. Needled 24_7.mp3", "autotag.txt", "cover.jpg");
		assertEquals(oldFilesRemain ? 10L : 1187L, new File(addPathElements(tdir, "01. Living Dead Beat.mp3")).length());
		assertEquals(2L, new File(addPathElements(tdir, "02. Are You Dead Yet_.mp3")).length());
		assertEquals(6L, new File(addPathElements(tdir, "14. Needled 24_7.mp3")).length());
		assertEquals(oldFilesRemain ? 3L : 81L, new File(addPathElements(tdir, "autotag.txt")).length());
		assertEquals("14ness", new BufferedReader(new InputStreamReader(new FileInputStream(new File(addPathElements(tdir, "14. Needled 24_7.mp3"))), "ASCII")).readLine());
		assertEquals(oldFilesRemain ? "o2!" : "ARTIST:  AC-DC", new BufferedReader(new InputStreamReader(new FileInputStream(new File(addPathElements(tdir, "autotag.txt"))), "UTF-8")).readLine());

		// Test engine.dirs + engine.files
		assertEquals(1 + 5 + 4 + (oldFilesRemain ? 2 : 0), engine2.files.size());
		assertEquals(3 + (oldFilesRemain ? 1 : 0), engine2.dirs.size());
		assertTrue(engine2.dirs.containsKey(sourceDir));
		assertTrue(engine2.dirs.containsKey(addPathElements(sourceDir, "incomplete", "1")));
		assertTrue(engine2.files.containsKey(addPathElements(sourceDir, "asd.txt")));
		assertEquals(oldFilesRemain, engine2.dirs.containsKey(addPathElements(sourceDir, "complete", "blah")));

		// Test source dir
		assertDirContents(sourceDir, "asd.txt", "incomplete", "other", oldFilesRemain ? "complete" : null);
		// complete/blah
		if (oldFilesRemain) {
			assertDirContents(addPathElements(sourceDir, "complete", "blah"), "01.mp3", "autotag.txt");
		}
		// incomplete
		tdir= addPathElements(sourceDir, "incomplete");
		assertDirContents(tdir, "1", "2");
		assertDirContents(addPathElements(tdir, "1"), "01.mp3", "02.mp3", "autotag.txt", "cover.jpg", "crap.txt");
		assertDirContents(addPathElements(tdir, "2"), "201.mp3", "202.mp3", "autotag.txt", "crap.txt");
		// other
		tdir= addPathElements(sourceDir, "other");
		assertDirContents(tdir, "empty", "remain");
		// other/empty
		assertDirContents(addPathElements(tdir, "empty"));
		// other/remain
		assertDirContents(addPathElements(tdir, "remain"), "remain.mp3");
	}

	@Test
	public void testDaVoodoo_sameSourceAndTargetDir() throws IOException, URISyntaxException {
		String sourceDir= prepareVoodooTestSourceDir("sample_data2");
		final String octavarium= addPathElements(sourceDir, "Dream Theater", "2005 - Octavarium");
		mtpr.addMockResult(addPathElements(octavarium, "05 - Panic Attack"), makeTrackProperties("Dream Theater", 2005, "Octavarium", "5", "Panic Attack"));
		mtpr.addMockResult(addPathElements(octavarium, "08. Octavarium"), makeTrackProperties("Dream Theater", 2005, "Octavarium", "8", "Octavarium"));
		engine2.add(sourceDir);
		engine2.files.get(addPathElements(octavarium, "del_me.txt")).setMarkedForDeletion(true);
		assertEquals(4, engine2.files.size());

		engine2.doYaVoodoo(sourceDir, new MockVoodooProgressMonitor(), null);

		assertDirContents(sourceDir, "Dream Theater");
		assertDirContents(addPathElements(sourceDir, "Dream Theater"), "2005 - Octavarium");
		assertDirContents(octavarium, "05. Panic Attack.mp3", "08. Octavarium.mp3", "00-dream_theater-octavarium-2005.nfo");
		assertEquals("5 monkeys", new BufferedReader(new InputStreamReader(new FileInputStream(new File(addPathElements(octavarium, "05. Panic Attack.mp3"))), "ASCII")).readLine());
		assertEquals("8 octo", new BufferedReader(new InputStreamReader(new FileInputStream(new File(addPathElements(octavarium, "08. Octavarium.mp3"))), "ASCII")).readLine());
		assertEquals(0, engine2.files.size());
		assertEquals(0, engine2.dirs.size());
	}

	// =============================================================================================== //
	// = Private
	// =============================================================================================== //

	private void addFakeDirsToEngine() {
		engine2.addFakeDir("A", "a1.mp3", "a2.mp3", "a3.mp3");
		engine2.addFakeDir("B", "b1.mp3", "b2.mp3", "b3.mp3");
		engine2.addFakeDir("C", "c1.mp3", "c2.mp3", "c3.mp3");
	}

	private void assertDirContents(String dir, String... expectedFiles) {
		File d= new File(dir);
		if (!d.isDirectory()) {
			fail("Directory " + dir + " expected but not found.");
		}

		Set<String> expectedFileSet= Helpers.arrayToSet(expectedFiles);
		expectedFileSet.remove(null);
		String[] expectedFiles2= expectedFileSet.toArray(new String[expectedFileSet.size()]);
		String[] actual= d.list();
		Arrays.sort(expectedFiles2);
		Arrays.sort(actual);
		// assertEquals(expectedFiles2, actual);
		assertEquals(Arrays.deepToString(expectedFiles2), Arrays.deepToString(actual));
	}

	private void assertEngineTrackProperties(String filename, TrackPropertyMap expected) {
		assertEngineTrackProperties(filename, expected, null);
	}

	private void assertEngineTrackProperties(String filename, TrackPropertyMap expected1, TrackPropertyMap expected2) {
		filename= ensureCorrectDirSeperators(Helpers.removeFilenameExtension(filename)) + ".mp3"; //$NON-NLS-1$
		FileData fd= engine.files.get(filename);
		if (fd == null) {
			System.err.println("assertEngineTrackProperties failed: key not found: " + filename);
			System.err.println("  keys: " + Helpers.sort(engine.files.keySet()));
			fail("assertEngineTrackProperties failed: key not found: " + filename);
		}
		TrackPropertyMap test= TrackPropertyMap.fromFileData(fd);
		if (expected1.equals(test)) {
			return;
		}
		if (expected2 != null && expected2.equals(test)) {
			return;
		}

		System.err.println("assertEngineTrackProperties failed.");
		System.err.println("  expected: " + expected1);
		if (expected2 != null) {
			System.err.println("  expected: " + expected2);
		}
		System.err.println("  found   : " + test);
		fail("assertEngineTrackProperties failed.");
	}

	private void createFile(String filename, String content) throws IOException {
		File f= new File(filename);
		Helpers.mkdir_p(f.getParentFile());
		if (f.isFile()) {
			f.delete();
		}
		BufferedWriter out= new BufferedWriter(new FileWriter(f));
		out.write(content);
		out.close();
	}

	private void removeDirectoryIfExists(File dir) throws IOException {
		if (dir.exists()) {
			Helpers.rm_rf(dir);
		}
	}

	private String prepareVoodooTestSourceDir(String sampleDataDir) throws IOException, URISyntaxException {
		File dir= getTestSourceDir();
		removeDirectoryIfExists(dir);
		Helpers.cp_r(new File(getTestResource(sampleDataDir).toURI()), dir, true, ".svn");
		return dir.toString();
	}

	private File getTestSourceDir() {
		return new File(addPathElements(Helpers.getSystemTempDir(), "tanuki_test_da_voodoo", "source"));
	}

	private String prepareVoodooTestTargetDir() throws IOException {
		File targetDir= getTestTargetDir();
		removeDirectoryIfExists(targetDir);
		return targetDir.toString();
	}

	private File getTestTargetDir() {
		return new File(addPathElements(Helpers.getSystemTempDir(), "tanuki_test_da_voodoo", "target"));
	}

	private void useTwoTrackReaders() {
		mtpr2= new MockTrackProprtyReader();
		engine2.add(mtpr2);
	}
}
