package golly.tanuki2.data;

import golly.tanuki2.support.Helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Similar to a {@link RankedObjectCollection} of strings except that the strings are normalised before being ranked.
 * Calling {@link #getWinner()} will always return the unnormalised version of the string.
 * 
 * @author Golly
 * @since 26/07/2007
 */
public class RankedNormalisedStringCollection {
	final Map<String, RankedObjectCollection<String>> normalisedToRankedOrig= new HashMap<String, RankedObjectCollection<String>>();
	final RankedObjectCollection<String> rankedCollection= new RankedObjectCollection<String>();

	private static String normalise(String data) {
		return Helpers.normalizeText(data);
	}

	/**
	 * Stores an unnormalised string in {@link #normalisedToRankedOrig} and returns a normalised version.
	 */
	private String registerUnnormalisedString(String orig, double rank) {
		final String n= normalise(orig);
		RankedObjectCollection<String> rankedOrig= normalisedToRankedOrig.get(n);
		if (rankedOrig == null) {
			rankedOrig= new RankedObjectCollection<String>();
			normalisedToRankedOrig.put(n, rankedOrig);
		}
		rankedOrig.increaseRank(orig, rank);
		return n;
	}

	/**
	 * Same as calling {@link #increaseRank(String, double)} as this is a set, not a list.
	 */
	public void add(String data, double rank) {
		increaseRank(data, rank);
	}

	public void clear() {
		rankedCollection.clear();
		normalisedToRankedOrig.clear();
	}

	public boolean contains(String o) {
		return rankedCollection.contains(normalise(o));
	}

	public double getRank(String o) {
		return rankedCollection.getRank(normalise(o));
	}

	public String getWinner() {
		final String n= rankedCollection.getWinner();
		if (n == null || !normalisedToRankedOrig.containsKey(n)) {
			return null;
		}
		return normalisedToRankedOrig.get(n).getWinner();
	}

	public int getWinnerCount() {
		return rankedCollection.getWinnerCount();
	}

	public Double getWinningRank() {
		return rankedCollection.getWinningRank();
	}

	public boolean hasSingleWinner() {
		return rankedCollection.hasSingleWinner();
	}

	public void increaseRank(String data, double rank) {
		rankedCollection.increaseRank(registerUnnormalisedString(data, rank), rank);
	}

	public boolean isEmpty() {
		return rankedCollection.isEmpty();
	}

	//	public boolean remove(String data) {
	//		return rankedCollection.remove(data);
	//	}

	public int size() {
		return rankedCollection.size();
	}
}
