package golly.tanuki2.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Golly
 * @since 15/04/2009
 */
@SuppressWarnings("nls")
public class RankedNormalisedStringCollectionTest {
	private static final double DBL_DELTA= 0.000001;

	private RankedNormalisedStringCollection x;

	@Before
	public void setup() {
		x= new RankedNormalisedStringCollection();
		x.add("two words", 1.5);
		x.add("ffs", 4);
		x.add("TWO  WORDS", 3);
	}

	@Test
	public void testSizeRelatedAndClear() {
		assertEquals(2, x.size());
		assertFalse(x.isEmpty());
		x.clear();
		assertEquals(0, x.size());
		assertTrue(x.isEmpty());
		assertNull(x.getWinner());
	}

	@Test
	public void testContains() {
		assertTrue(x.contains("Two Words"));
		assertFalse(x.contains("que?"));
	}

	@Test
	public void testGetRank() {
		assertEquals(4.5, x.getRank("two words"), DBL_DELTA);
		assertEquals(4, x.getRank("FFS"), DBL_DELTA);
	}

	@Test
	public void testSingleWinner() {
		assertEquals("TWO  WORDS", x.getWinner());
		assertEquals(1, x.getWinnerCount());
		assertTrue(x.hasSingleWinner());
	}

	@Test
	public void testMultipleWinners() {
		x.add("omfg!", 4.5);
		assertEquals(2, x.getWinnerCount());
		assertFalse(x.hasSingleWinner());
		String winner= x.getWinner();
		assertTrue("TWO  WORDS".equals(winner) || "omfg!".equals(winner));
	}

	@Test
	public void testGetWinningRank() {
		assertEquals(4.5, x.getWinningRank(), DBL_DELTA);
	}

	@Test
	public void testIncreaseRank() {
		x.increaseRank("FFs", 3);
		assertEquals(7, x.getRank("ffs"), DBL_DELTA);
	}
}
