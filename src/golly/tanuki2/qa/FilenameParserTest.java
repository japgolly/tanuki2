package golly.tanuki2.qa;

import static golly.tanuki2.data.TrackPropertyType.ALBUM;
import static golly.tanuki2.data.TrackPropertyType.ARTIST;
import static golly.tanuki2.data.TrackPropertyType.TN;
import static golly.tanuki2.data.TrackPropertyType.TRACK;
import static golly.tanuki2.data.TrackPropertyType.YEAR;
import golly.tanuki2.core.FilenameParser;
import golly.tanuki2.core.ITrackProprtyReader;
import golly.tanuki2.core.FilenameParser.SmartPattern;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.TrackProperties;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Golly
 * @since 23/02/2007
 */
@SuppressWarnings("nls")
public class FilenameParserTest extends TestHelper {
	private ITrackProprtyReader fp= null;

	@Before
	public void setup() {
		fp= new FilenameParser();
	}

	@Test
	public void testSmartPattern() {
		SmartPattern sp= new FilenameParser.SmartPattern("[:artist:]", "[:year:] - [:album:]", "[:tn:] - [:track:]");
		assertEquals(sp.indexes.get(ARTIST), 1);
		assertEquals(sp.indexes.get(YEAR), 2);
		assertEquals(sp.indexes.get(ALBUM), 3);
		assertEquals(sp.indexes.get(TN), 4);
		assertEquals(sp.indexes.get(TRACK), 5);

		sp= new FilenameParser.SmartPattern("[:year:] [:artist:]", "[:year:] [:album:]", "[:track:]");
		assertEquals(sp.indexes.get(ARTIST), 2);
		assertEquals(sp.indexes.get(YEAR), 1);
		assertEquals(sp.indexes.get(ALBUM), 3);
		assertEquals(sp.indexes.get(TN), null);
		assertEquals(sp.indexes.get(TRACK), 4);
	}

	@Test
	public void testParsingSingle() {
		subtestParse(fp, "C:\\2\\Nevermore\\[2004] Enemies of Reality\\18 Who Decides.mp3", "Nevermore", 2004, "Enemies of Reality", "18", "Who Decides");
		subtestParse(fp, "/home/golly/music/4. Done/Unexpect/2006 - In a Flesh Aquarium/06 - Megalomaniac Trees.mp3", "Unexpect", 2006, "In a Flesh Aquarium", "06", "Megalomaniac Trees");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995. Destroy Erase Improve (320)\\07 - Inside What's Within Behind.mp3", "Meshuggah", 1995, "Destroy Erase Improve", "07", "Inside What's Within Behind");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995 - Destroy Erase Improve (320)\\07 - Inside What's Within Behind.mp3", "Meshuggah", 1995, "Destroy Erase Improve", "07", "Inside What's Within Behind");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995 - Destroy Erase Improve (192-320)\\07 - Inside What's Within Behind.mp3", "Meshuggah", 1995, "Destroy Erase Improve", "07", "Inside What's Within Behind");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995 - Destroy Erase Improve (320)\\8. asd.mp3", "Meshuggah", 1995, "Destroy Erase Improve", "8", "asd");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995 - Destroy Erase Improve (320)\\8. ...asd.mp3", "Meshuggah", 1995, "Destroy Erase Improve", "8", "...asd");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995 - Destroy Erase Improve (320)\\8. asd....mp3", "Meshuggah", 1995, "Destroy Erase Improve", "8", "asd...");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995 - Destroy Erase Improve (320)\\10 - Why... - Not_.mp3", "Meshuggah", 1995, "Destroy Erase Improve", "10", "Why... - Not_");
		subtestParse(fp, "X:\\music\\1. Fresh\\Altera Enigma - 2006 - Alteration [VBR HQ]\\06 - Skyward (Outer Atmosphere).mp3", "Altera Enigma", 2006, "Alteration", "06", "Skyward (Outer Atmosphere)");
		subtestParse(fp, "/Jordan Rudess - 4NYC  (www.heavytorrents.org)\\02 - If I Could.mp3", "Jordan Rudess", null, "4NYC", "02", "If I Could");
		subtestParse(fp, "/var/music/IN FLAMES Discografia (www.heavytorrents.org)/IN FLAMES Clayman/02 Pinball Map.mp3", "IN FLAMES", null, "Clayman", "02", "Pinball Map");
	}

	@Test
	public void testParsingMulti_commonSuffixInFilesAndDir() {
		// Test that readMultipleTrackProperties removes common suffix from filenames AND DIR
		DirData dd= new DirData("/var/music/Virgin Steele-Visions of Eden-2006-AMRC");
		final String fn4= "04 virgin steele-black light on black-amrc.mp3";
		final String fn5= "05 virgin steele-bonedust-amrc.mp3";
		dd.files.put(fn4, makeFileData(dd, true));
		dd.files.put(fn5, makeFileData(dd, true));
		dd.files.put("as.jpg", makeFileData(dd, false));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(2, r.size());
		assertTrue(r.containsKey(fn4));
		assertTrue(r.containsKey(fn5));
		assertTrackPropertiesFound(fn4, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "04", "black light on black"), r.get(fn4));
		assertTrackPropertiesFound(fn5, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "05", "bonedust"), r.get(fn5));
	}

	@Test
	public void testParsingMulti_commonSuffixInFilesOnly() {
		// Test that readMultipleTrackProperties removes common suffix from filenames ONLY
		DirData dd= new DirData("/var/music/Virgin Steele-Visions of Eden-2006");
		final String fn4= "04 virgin steele-black light on black-amrc.mp3";
		final String fn5= "05 virgin steele-bonedust-amrc.mp3";
		dd.files.put(fn4, makeFileData(dd, true));
		dd.files.put(fn5, makeFileData(dd, true));
		dd.files.put("as.jpg", makeFileData(dd, false));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(2, r.size());
		assertTrue(r.containsKey(fn4));
		assertTrue(r.containsKey(fn5));
		assertTrackPropertiesFound(fn4, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "04", "black light on black"), r.get(fn4));
		assertTrackPropertiesFound(fn5, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "05", "bonedust"), r.get(fn5));
	}

	@Test
	public void testParsingMulti_doesntRemoveCommonSuffix() {
		// Test that readMultipleTrackProperties doesnt remove text from filenames
		DirData dd= new DirData("/var/music/Virgin Steele-Visions of Eden-2006");
		final String fn4= "04 virgin steele-black light on blackamrc.mp3";
		final String fn5= "05 virgin steele-bonedustamrc.mp3";
		dd.files.put(fn4, makeFileData(dd, true));
		dd.files.put(fn5, makeFileData(dd, true));
		dd.files.put("as.jpg", makeFileData(dd, false));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(2, r.size());
		assertTrue(r.containsKey(fn4));
		assertTrue(r.containsKey(fn5));
		assertTrackPropertiesFound(fn4, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "04", "black light on blackamrc"), r.get(fn4));
		assertTrackPropertiesFound(fn5, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "05", "bonedustamrc"), r.get(fn5));
	}

	@Test
	public void testParsingMulti_removesArtistAndAlbumFromFilename() {
		// "X:\\music\\1. Fresh\\Black Label Society-Shot To Hell-2006[www.heavytorrents.org]\\01-black label society-concrete jungle.mp3"
		// "X:\\music\\1. Fresh\\Black Label Society-Shot To Hell-2006[www.heavytorrents.org]\\01-black label society-shot to hell-concrete jungle.mp3"
		// "X:\\music\\1. Fresh\\Black Label Society-Shot To Hell-2006[www.heavytorrents.org]\\01-shot to hell-black label society-concrete jungle.mp3"
		// "X:\\music\\1. Fresh\\Black Label Society-Shot To Hell-2006[www.heavytorrents.org]\\01-shot to hell-concrete jungle.mp3"
		// etc...
		String[] insertValues= new String[] {"", "black label society", "black label society-shot to hell",
				"shot to hell", "BLACK LABEL  SOCIETY", "Black-Label-Society SHOT-TO-HELL"};
		for (String pre : insertValues) {
			if (pre.length() > 0)
				pre= pre + "-";
			for (String mid : insertValues) {
				if (mid.length() > 0)
					mid= "-" + mid + "-";
				else
					mid= "-";
				for (String post : insertValues) {
					if (post.length() > 0)
						post= "-" + post;

					DirData dd= new DirData("X:\\music\\1. Fresh\\Black Label Society-Shot To Hell-2006[www.heavytorrents.org]");
					final String fn1= pre + "01" + mid + "concrete jungle" + post + ".mp3";
					final String fn2= pre + "02" + mid + "woteva biatch" + post + ".mp3";
					dd.files.put(fn1, makeFileData(dd, true));
					dd.files.put(fn2, makeFileData(dd, true));
					final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
					assertEquals(2, r.size());
					assertTrue(r.containsKey(fn1));
					assertTrue(r.containsKey(fn2));
					assertTrackPropertiesFound(fn1, makeTrackProperties("Black Label Society", 2006, "Shot To Hell", "01", "concrete jungle"), r.get(fn1));
					assertTrackPropertiesFound(fn2, makeTrackProperties("Black Label Society", 2006, "Shot To Hell", "02", "woteva biatch"), r.get(fn2));
				}
			}
		}
	}

	@Test
	public void testParsingMulti_doesntRemoveAlbumFromFilename() {
		DirData dd= new DirData("/var/music/IN FLAMES Discografia (www.heavytorrents.org)/IN FLAMES Trigger");
		final String fn1= "01 Trigger [Single Edit].mp3";
		final String fn2= "02 Watch Them Feed.mp3";
		dd.files.put(fn1, makeFileData(dd, true));
		dd.files.put(fn2, makeFileData(dd, true));
		dd.files.put("as.jpg", makeFileData(dd, false));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(2, r.size());
		assertTrue(r.containsKey(fn1));
		assertTrue(r.containsKey(fn2));
		assertTrackPropertiesFound(fn1, makeTrackProperties("IN FLAMES", null, "Trigger", "01", "Trigger [Single Edit]"), r.get(fn1));
		assertTrackPropertiesFound(fn2, makeTrackProperties("IN FLAMES", null, "Trigger", "02", "Watch Them Feed"), r.get(fn2));
	}

	@Test
	public void testParsingMulti_doesntRemoveWrongAlbumFromFilename() {
		DirData dd= new DirData("/var/music/IN FLAMES Discografia (www.heavytorrents.org)/IN FLAMES Trigger");
		final String fn1= "InFlames_Trigger_01 Trigger [Single Edit] - TRIGGER.mp3";
		final String fn2= "In Flames_Trigger_02 Watch Them Feed In Flames - TRIGGER.mp3";
		dd.files.put(fn1, makeFileData(dd, true));
		dd.files.put(fn2, makeFileData(dd, true));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(2, r.size());
		assertTrue(r.containsKey(fn1));
		assertTrue(r.containsKey(fn2));
		assertTrackPropertiesFound(fn1, makeTrackProperties("IN FLAMES", null, "Trigger", "01", "Trigger [Single Edit]"), r.get(fn1));
		assertTrackPropertiesFound(fn2, makeTrackProperties("IN FLAMES", null, "Trigger", "02", "Watch Them Feed In Flames"), r.get(fn2));
	}

	@Test
	public void testParsingMulti_replacesUnderscoresWithSpaces() {
		DirData dd= new DirData("/var/music/Virgin_Steele-Visions_of_Eden-2006");
		final String fn4= "04_virgin_steele-black_light_on_black.mp3";
		final String fn5= "05_virgin_steele-bonedust.mp3";
		dd.files.put(fn4, makeFileData(dd, true));
		dd.files.put(fn5, makeFileData(dd, true));
		dd.files.put("as.jpg", makeFileData(dd, false));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(2, r.size());
		assertTrue(r.containsKey(fn4));
		assertTrue(r.containsKey(fn5));
		assertTrackPropertiesFound(fn4, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "04", "black light on black"), r.get(fn4));
		assertTrackPropertiesFound(fn5, makeTrackProperties("Virgin Steele", 2006, "Visions of Eden", "05", "bonedust"), r.get(fn5));
	}

	@Test
	public void testParsingMulti_doesntAlwaysReplaceUnderscoresWithSpaces() {
		DirData dd= new DirData("/var/music/Unexpect/(2006) In_A_Flesh_Aquarium");
		final String fn4= "04.Summoning Scenes.mp3";
		final String fn5= "05.Silence_011010701.mp3";
		dd.files.put(fn4, makeFileData(dd, true));
		dd.files.put(fn5, makeFileData(dd, true));
		dd.files.put("as.jpg", makeFileData(dd, false));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(2, r.size());
		assertTrue(r.containsKey(fn4));
		assertTrue(r.containsKey(fn5));
		assertTrackPropertiesFound(fn4, makeTrackProperties("Unexpect", 2006, "In_A_Flesh_Aquarium", "04", "Summoning Scenes"), r.get(fn4));
		assertTrackPropertiesFound(fn5, makeTrackProperties("Unexpect", 2006, "In_A_Flesh_Aquarium", "05", "Silence_011010701"), r.get(fn5));
	}

	@Test
	public void testMisc1() {
		DirData dd= new DirData("X:\\music\\1. Fresh\\Lordi - Discografia [www.emwreloaded.com]\\Lordi - The Monster Show [2005]");
		final String fn1= "01-lordi_-_theatrical_trailer-qtxmp3.mp3";
		final String fn2= "02-lordi_-_bring_it_on-qtxmp3.mp3";
		final String fn3= "03-lordi_-_blood_red_sandman-qtxmp3.mp3";
		final String fn4= "04-lordi_-_my_heaven_is_your_hell-qtxmp3.mp3";
		dd.files.put(fn1, makeFileData(dd, true));
		dd.files.put(fn2, makeFileData(dd, true));
		dd.files.put(fn3, makeFileData(dd, true));
		dd.files.put(fn4, makeFileData(dd, true));
		dd.files.put("hi there.jpg", makeFileData(dd, false));
		final Map<String, List<TrackProperties>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(4, r.size());
		assertTrue(r.containsKey(fn1));
		assertTrue(r.containsKey(fn2));
		assertTrue(r.containsKey(fn3));
		assertTrue(r.containsKey(fn4));
		assertTrackPropertiesFound(fn1, makeTrackProperties("Lordi", 2005, "The Monster Show", "01", "theatrical trailer"), r.get(fn1));
		assertTrackPropertiesFound(fn2, makeTrackProperties("Lordi", 2005, "The Monster Show", "02", "bring it on"), r.get(fn2));
		assertTrackPropertiesFound(fn3, makeTrackProperties("Lordi", 2005, "The Monster Show", "03", "blood red sandman"), r.get(fn3));
		assertTrackPropertiesFound(fn4, makeTrackProperties("Lordi", 2005, "The Monster Show", "04", "my heaven is your hell"), r.get(fn4));
	}

	// =============================================================================================== //
	// = private
	// =============================================================================================== //

	private void assertTrackPropertiesFound(String filename, TrackProperties expected, final Collection<TrackProperties> test) {
		boolean found= false;
		for (TrackProperties tp : test)
			if (tp.equals(expected)) {
				found= true;
				break;
			}
		if (!found) {
			System.err.println("Expected TrackProperties not found.");
			System.err.println("  Filename: " + filename);
			System.err.println("  Expected: " + expected);
			for (TrackProperties tp : test)
				System.err.println("  Found:    " + tp);
			System.err.println();
			fail("Expected TrackProperties not found.");
		}
	}

	private void subtestParse(ITrackProprtyReader fp, String filename, String artist, Integer year, String album, String tn, String track) {
		final TrackProperties expected= makeTrackProperties(artist, year, album, tn, track);

		final Collection<TrackProperties> r= fp.readTrackProperties(filename);
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
		Map<String, List<TrackProperties>> r2= fp.readMultipleTrackProperties(dd);
		assertEquals(1, r2.size());
		assertEquals(f.getName(), r2.keySet().iterator().next());
		assertTrackPropertiesFound(filename, expected, r2.get(f.getName()));
	}
}
