package golly.tanuki2.modules;

import static golly.tanuki2.data.TrackPropertyType.ALBUM;
import static golly.tanuki2.data.TrackPropertyType.ARTIST;
import static golly.tanuki2.data.TrackPropertyType.TN;
import static golly.tanuki2.data.TrackPropertyType.TRACK;
import static golly.tanuki2.data.TrackPropertyType.YEAR;
import golly.tanuki2.TestHelper;
import golly.tanuki2.core.ITrackPropertyReader;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.TrackPropertyMap;
import golly.tanuki2.modules.FilenameParser.SmartPattern;

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
	private ITrackPropertyReader fp= null;

	@Before
	public void setup() {
		fp= new FilenameParser();
	}

	@Test
	public void testSmartPattern() {
		SmartPattern sp= new FilenameParser.SmartPattern("[:artist:]", "[:year:] - [:album:]", "[:tn:] - [:track:]");
		assertEquals(1, sp.indexes.get(ARTIST).intValue());
		assertEquals(2, sp.indexes.get(YEAR).intValue());
		assertEquals(3, sp.indexes.get(ALBUM).intValue());
		assertEquals(4, sp.indexes.get(TN).intValue());
		assertEquals(5, sp.indexes.get(TRACK).intValue());

		sp= new FilenameParser.SmartPattern("[:year:] [:artist:]", "[:year:] [:album:]", "[:track:]");
		assertEquals(1, sp.indexes.get(YEAR).intValue());
		assertEquals(2, sp.indexes.get(ARTIST).intValue());
		assertEquals(3, sp.indexes.get(ALBUM).intValue());
		assertEquals(4, sp.indexes.get(TRACK).intValue());
		assertNull(sp.indexes.get(TN));
	}

	@Test
	public void testParsingSingle_dirAndFilename() {
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
	public void testParsingSingle_filenameOnly() {
		subtestParse(fp, "/var/music/Dream Theater - Train Of Thought - 01 - As I Am.flac", "Dream Theater", null, "Train Of Thought", "01", "As I Am");
		subtestParse(fp, "/var/music/Dream Theater - 2003 - Train Of Thought - 01 - As I Am.flac", "Dream Theater", 2003, "Train Of Thought", "01", "As I Am");
		subtestParse(fp, "/var/music/Dream Theater [2003] Train Of Thought - 01 - As I Am.flac", "Dream Theater", 2003, "Train Of Thought", "01", "As I Am");
		subtestParse(fp, "/var/music/Dream Theater (2003) Train Of Thought - 01. As I Am.flac", "Dream Theater", 2003, "Train Of Thought", "01", "As I Am");
	}

	@Test
	public void testParsingSingle_dirOnly() {
		subtestParse(fp, "C:\\2\\Nevermore\\[2004] Enemies of Reality\\Who Decides.mp3", "Nevermore", 2004, "Enemies of Reality", null, "Who Decides");
		subtestParse(fp, "/home/golly/music/4. Done/Unexpect/2006 - In a Flesh Aquarium/Megalomaniac Trees.mp3", "Unexpect", 2006, "In a Flesh Aquarium", null, "Megalomaniac Trees");
		subtestParse(fp, "X:\\music\\1. Fresh\\Meshuggah - Discografia [heavytorrents.org]\\1995. Destroy Erase Improve (320)\\Inside What's Within Behind.mp3", "Meshuggah", 1995, "Destroy Erase Improve", null, "Inside What's Within Behind");
		subtestParse(fp, "X:\\music\\1. Fresh\\Altera Enigma - 2006 - Alteration [VBR HQ]\\Skyward (Outer Atmosphere).mp3", "Altera Enigma", 2006, "Alteration", null, "Skyward (Outer Atmosphere)");
		subtestParse(fp, "/Jordan Rudess - 4NYC  (www.heavytorrents.org)\\If I Could.mp3", "Jordan Rudess", null, "4NYC", null, "If I Could");
		subtestParse(fp, "/var/music/IN FLAMES Discografia (www.heavytorrents.org)/IN FLAMES Clayman/Pinball Map.mp3", "IN FLAMES", null, "Clayman", null, "Pinball Map");
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
				"shot TO   HELL", "Black-Label-Society   SHOT-TO-HELL"};
		String[] sepTypes= new String[] {"-", "- ", " -", "   -     "};
		for (String sep : sepTypes)
			for (String pre : insertValues) {
				if (pre.length() > 0)
					pre= pre + sep;
				for (String mid : insertValues) {
					if (mid.length() > 0)
						mid= sep + mid + sep;
					else
						mid= sep;
					for (String post : insertValues) {
						if (post.length() > 0)
							post= sep + post;

						DirData dd= new DirData("X:\\music\\1. Fresh\\Black Label Society-Shot To Hell-2006[www.heavytorrents.org]");
						final String fn1= pre + "01" + mid + "concrete jungle" + post + ".mp3";
						final String fn2= pre + "02" + mid + "woteva biatch" + post + ".mp3";
						dd.files.put(fn1, makeFileData(dd, true));
						dd.files.put(fn2, makeFileData(dd, true));
						final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
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

	@Test
	public void testMisc2() {
		DirData dd= new DirData("D:\\downloads\\bittorrent\\complete\\Into Eternity\\Into Eternity - Selftitled");
		final String fn1= "Into Eternity - Into Eternity - 01 - Torn.Mp3";
		final String fn2= "Into Eternity - Into Eternity - 02 - Sorrow.Mp3";
		final String fn3= "Into Eternity - Into Eternity - 03 - Left Behind.Mp3";
		dd.files.put(fn1, makeFileData(dd, true));
		dd.files.put(fn2, makeFileData(dd, true));
		dd.files.put(fn3, makeFileData(dd, true));
		dd.files.put("hi there.jpg", makeFileData(dd, false));
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(3, r.size());
		assertTrue(r.containsKey(fn1));
		assertTrue(r.containsKey(fn2));
		assertTrue(r.containsKey(fn3));
		assertTrackPropertiesFound(fn1, makeTrackProperties("Into Eternity", null, "Selftitled", "01", "Torn"), r.get(fn1));
		assertTrackPropertiesFound(fn2, makeTrackProperties("Into Eternity", null, "Selftitled", "02", "Sorrow"), r.get(fn2));
		assertTrackPropertiesFound(fn3, makeTrackProperties("Into Eternity", null, "Selftitled", "03", "Left Behind"), r.get(fn3));
	}

	@Test
	public void testMisc3() {
		DirData dd= new DirData("X:\\music\\1. Fresh\\Hawaii - The Natives Are Restless (1985) 320 Kbps");
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
		for (String f : fnAll)
			dd.files.put(f, makeFileData(dd, true));
		dd.files.put("Hawaii - 00 - The Natives Are Restless.nfo", makeFileData(dd, false));
		dd.files.put("Hawaii - The Natives Are Restless.jpg", makeFileData(dd, false));
		final Map<String, List<TrackPropertyMap>> r= fp.readMultipleTrackProperties(dd);
		assertEquals(9, r.size());
		for (String f : fnAll)
			assertTrue(r.containsKey(f));
		assertTrackPropertiesFound(fn1, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "01", "Call Of The Wild"), r.get(fn1));
		assertTrackPropertiesFound(fn2, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "02", "Turn It Louder"), r.get(fn2));
		assertTrackPropertiesFound(fn3, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "03", "V.P.H.B."), r.get(fn3));
		assertTrackPropertiesFound(fn4, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "04", "Beg For Mercy"), r.get(fn4));
		assertTrackPropertiesFound(fn5, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "05", "Unfinished Business"), r.get(fn5));
		assertTrackPropertiesFound(fn6, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "06", "Proud To Be Loud"), r.get(fn6));
		assertTrackPropertiesFound(fn7, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "07", "Lies"), r.get(fn7));
		assertTrackPropertiesFound(fn8, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "08", "Omichan No Uta"), r.get(fn8));
		assertTrackPropertiesFound(fn9, makeTrackProperties("Hawaii", 1985, "The Natives Are Restless", "09", "Dynamite"), r.get(fn9));
	}
}
