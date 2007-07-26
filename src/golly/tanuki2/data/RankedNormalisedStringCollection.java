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
	final Map<String, RankedObjectCollection<String>> unnormalised= new HashMap<String, RankedObjectCollection<String>>();
	final RankedObjectCollection<String> rankedCollection= new RankedObjectCollection<String>();

	private static String normalise(String data) {
		return Helpers.normalizeText(data);
	}

	private String regStr(String data, double incRank) {
		final String n= normalise(data);
		RankedObjectCollection<String> u= unnormalised.get(n);
		if (u == null) {
			u= new RankedObjectCollection<String>();
			unnormalised.put(n, u);
		}
		u.increaseRank(data, incRank);
		return n;
	}

	//	private void unregStr(String data) {
	//		final String n= normalise(data);
	//		RankedObjectCollection<String> u= unnormalised.get(n);
	//		if (u != null) {
	//			if (u.getRank(n) == 1)
	//				u.remove(n);
	//			else
	//			u.increaseRank(n, -1);
	//		}
	//	}

	public void add(String data, double rank) {
		rankedCollection.add(regStr(data, rank), rank);
	}

	public void clear() {
		rankedCollection.clear();
		unnormalised.clear();
	}

	public boolean contains(String o) {
		return rankedCollection.contains(normalise(o));
	}

	public double getRank(String o) {
		return rankedCollection.getRank(normalise(o));
	}

	public String getWinner() {
		final String n= rankedCollection.getWinner();
		if (n == null || !unnormalised.containsKey(n))
			return null;
		return unnormalised.get(n).getWinner();
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

	public void increaseRank(String data, double incRank) {
		rankedCollection.increaseRank(regStr(data, incRank), incRank);
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

	public void sort() {
		rankedCollection.sort();
	}
}
