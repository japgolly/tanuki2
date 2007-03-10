package golly.tanuki2.qa;

import golly.tanuki2.core.ClipboardParser;
import golly.tanuki2.support.Helpers;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Golly
 * @since 10/03/2007
 */
@SuppressWarnings("nls")
public class ClipboardParserTest extends TestHelper {

	@Test
	public void testGood1() {
		final String clipboardTxt= "" //
				+ "01. Splintered Visions\n" //
				+ "02. Embraced By Desolation\n" //
				+ "03. 3 Dimensional Aperture\n" //
				+ "04. Beginning Of The End\n" //
				+ "05. Point Of Uncertainty\n" //
				+ "06. Spiraling Into Depression\n" //
				+ "07. Isolation\n" //
				+ "08. Buried In Oblivion\n" //
				+ "09. Black Sea Of Agony\n" //
				+ "10. Morose Seclusion\n" //				;
		;
		subtest(clipboardTxt);
	}

	@Test
	public void testGood2() {
		final String clipboardTxt= "" //
				+ "1. Splintered Visions \tListen \tListen\n" //
				+ "2. Embraced By Desolation \tListen \tListen\n" //
				+ "3. 3 Dimensional Aperture \tListen \tListen\n" //
				+ "4. Beginning Of The End \tListen \tListen\n" //
				+ "5. Point Of Uncertainty \tListen \tListen\n" //
				+ "6. Spiraling Into Depression \tListen \tListen\n" //
				+ "7. Isolation \tListen \tListen\n" //
				+ "8. Buried In Oblivion \tListen \tListen\n" //
				+ "9. Black Sea Of Agony \tListen \tListen\n" //
				+ "10. Morose Seclusion \tListen \tListen\n" //
		;
		subtest(clipboardTxt);
	}

	@Test
	public void testGood3() {
		final String clipboardTxt= "" //
				+ "1 \tAdd this track to your playlist Splintered Visions \t4:56 \t3,388\n" //
				+ "2 \tAdd this track to your playlist Embraced by Desolation \t4:08 \t2,814\n" //
				+ "3 \tAdd this track to your playlist 3 Dimensional Aperture \t4:47 \t727\n" //
				+ "4 \tAdd this track to your playlist Beginning of the End \t4:39 \t3,128\n" //
				+ "5 \tAdd this track to your playlist Point of Uncertainty \t3:45 \t2,443\n" //
				+ "6 \tAdd this track to your playlist Spiraling Into Depression \t3:36 \t1,307\n" //
				+ "7 \tAdd this track to your playlist Isolation \t4:59 \t2,578\n" //
				+ "8 \tAdd this track to your playlist Buried in Oblivion \t4:00 \t2,616\n" //
				+ "9 \tAdd this track to your playlist Black Sea of Agony \t6:31 \t2,349\n" //
				+ "10 \tAdd this track to your playlist Morose Seclusion \t3:21 \t2,095\n" //
		;
		subtest(clipboardTxt);
	}

	@Test
	public void testGood4() {
		final String clipboardTxt= "" //
				+ "1  \tSplintered Visions  \t(4:56)  \t   \t\n" //
				+ "2 \tEmbraced by Desolation \t(4:08) \t\t\n" //
				+ "3 \t3 Dimensional Aperture \t(4:47) \t\t\n" //
				+ "4 \tBeginning of the End \t(4:39) \t\t\n" //
				+ "5 \tPoint of Uncertainty \t(3:45) \t\t\n" //
				+ "6 \tSpiraling into Depression \t(3:36) \t\t\n" //
				+ "7 \tIsolation \t(4:59) \t\t\n" //
				+ "8 \tBuried in Oblivion \t(4:00) \t\t\n" //
				+ "9 \tBlack Sea of Agony \t(6:31) \t\t\n" //
				+ "10 \tMorose Seclusion \t(3:21) \t\t\n" //
		;
		subtest(clipboardTxt);
	}

	@Test
	public void testGood5() {
		final String clipboardTxt= "" //
				+ "1  \tListen it  \t Splintered Visions     \t04:57  \t6.79  \t $0.15  \t\n" //
				+ "2 \tListen it \tEmbraced By Desolation \t04:09 \t5.70 \t$0.15 \t\n" //
				+ "3 \tListen it \t3 Dimensional Aperture \t04:48 \t6.58 \t$0.15 \t\n" //
				+ "4 \tListen it \tBeginning Of The End \t04:40 \t6.40 \t$0.15 \t\n" //
				+ "5 \tListen it \tPoint Of Uncertainty \t03:46 \t5.17 \t$0.15 \t\n" //
				+ "6 \tListen it \tSpiraling Into Depression \t03:37 \t4.96 \t$0.15 \t\n" //
				+ "7 \tListen it \tIsolation \t04:59 \t6.85 \t$0.15 \t\n" //
				+ "8 \tListen it \tBuried In Oblivion \t04:00 \t5.50 \t$0.15 \t\n" //
				+ "9 \tListen it \tBlack Sea Of Agony \t06:32 \t8.97 \t$0.15 \t\n" //
				+ "10 \tListen it \tMorose Seclusion \t03:22 \t4.62 \t$0.15 \t\n" //
		;
		subtest(clipboardTxt);
	}

	@Test
	public void testGood6() {
		final String clipboardTxt= "" //
				+ "   1. \"Splintered Visions\" ? 4:56\n" //
				+ "   2. \"Embraced by Desolation\" ? 4:08\n" //
				+ "   3. \"3 Dimensional Aperture\" ? 4:47\n" //
				+ "   4. \"Beginning of the End\" ? 4:39\n" //
				+ "   5. \"Point of Uncertainty\" ? 3:45\n" //
				+ "   6. \"Spiraling into Depression\" ? 3:36\n" //
				+ "   7. \"Isolation\" ? 4:59\n" //
				+ "   8. \"Buried in Oblivion\" ? 4:00\n" //
				+ "   9. \"Black Sea of Agony\" ? 6:31\n" //
				+ "  10. \"Morose Seclusion\" ? 3:21\n" //
		;
		subtest(clipboardTxt);
	}

	@Test
	public void testGood7() {
		final String clipboardTxt= "" //
				+ "Windows Media\tReal Audio\t1.\tSplintered Visions\n" //
				+ "Windows Media\tReal Audio\t2.\tEmbraced By Desolation\n" //
				+ "Windows Media\tReal Audio\t3.\t3 Dimensional Aperture\n" //
				+ "Windows Media\tReal Audio\t4.\tBeginning Of The End\n" //
				+ "Windows Media\tReal Audio\t5.\tPoint Of Uncertainty\n" //
				+ "Windows Media\tReal Audio\t6.\tSpiraling Into Depression\n" //
				+ "Windows Media\tReal Audio\t7.\tIsolation\n" //
				+ "Windows Media\tReal Audio\t8.\tBuried In Oblivion\n" //
				+ "Windows Media\tReal Audio\t9.\tBlack Sea Of Agony\n" //
				+ "Windows Media\tReal Audio\t10.\tMorose Seclusion\n" //
		;
		subtest(clipboardTxt);
	}

	@Test
	@Ignore
	public void testGood8() {
		final String clipboardTxt= "Track list: Splintered Visions / Embraced By Desolation / 3 Dimensional Aperture / Beginning Of The End / Point Of Uncertainty / Spiraling Into Depression / Isolation / Buried In Oblivion / Black Sea Of Agony / Morose Seclusion\n";
		subtest(clipboardTxt);
	}

	@Test
	@Ignore
	public void testGood9() {
		final String clipboardTxt= "Track Listing: Splintered Visions (4:56) / Embraced By Desolation (4:08) / 3 Dimensional Aperture (4:47) / Beginning Of The End (4:39) / Point Of Uncertainty (3:45) / Spiraling Into Depression (3:36) / Isolation (4:59) / Buried In Oblivion (4:00) / Black Sea Of Agony (6:31) / Morose Seclusion (3:21)\n";
		subtest(clipboardTxt);
	}

	private void subtest(String clipboardTxt) {
		ClipboardParser cp= new ClipboardParser();
		//		Map<Integer, String> r= cp.readTracks("and there were 5 more\n\n\r\npeople on the 10 number "+clipboardTxt+"\n10 prople died.");
		Map<Integer, String> r= cp.readTracks(clipboardTxt);
		assertEquals(mapToStringLC(getExpected()), mapToStringLC(r));
	}

	private Map<Integer, String> getExpected() {
		Map<Integer, String> m= new HashMap<Integer, String>();
		m.put(1, "Splintered Visions");
		m.put(2, "Embraced By Desolation");
		m.put(3, "3 Dimensional Aperture");
		m.put(4, "Beginning Of The End");
		m.put(5, "Point Of Uncertainty");
		m.put(6, "Spiraling Into Depression");
		m.put(7, "Isolation");
		m.put(8, "Buried In Oblivion");
		m.put(9, "Black Sea Of Agony");
		m.put(10, "Morose Seclusion");
		return m;
	}

	private static String mapToStringLC(Map<Integer, String> m) {
		StringBuilder sb= new StringBuilder();
		for (Integer i : Helpers.sort(m.keySet())) {
			sb.append("\n");
			sb.append(i);
			sb.append(": ");
			sb.append(m.get(i).toLowerCase());
		}
		return sb.toString();
	}
}
