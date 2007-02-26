package golly.tanuki2.data;

import golly.tanuki2.support.Helpers;

/**
 * This is class holds an object and an integer that represents its rank.
 * <p>
 * <code>equals()</code> and <code>hashCode()</code> both delegate to the object so this class can be used in place
 * of the object in <code>Set</code>s etc.
 * </p>
 * <p>
 * This is also implements <code>Comparable</code> and sorts by rank. If the rank of both classes are equal then
 * <code>super.hashCode()</code> will be compared instead. <code>compareTo()</code> will <b>never</b> return
 * <code>0</code> and indicate that two ranked objects are the same. The behaviour is required so that ranked objects
 * may be used correctly in <code>SortedSet</code>s.
 * </p>
 * 
 * @author Golly
 * @since 24/02/2007
 */
public class RankedObject<T> extends AbstractDataObject implements Comparable<RankedObject<T>> {
	public final T data;
	private double rank;

	public RankedObject(T rankedObject, double rank) {
		this.data= rankedObject;
		this.rank= rank;
	}

	public int compareTo(RankedObject<T> b) {
		if (rank != b.rank)
			return rank > b.rank ? -1 : 1;
		return hashCodeThis() > b.hashCodeThis() ? -1 : 1;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (obj instanceof RankedObject)
			return data.equals(((RankedObject) obj).data);
		return data.equals(obj);
	}

	@Override
	@SuppressWarnings("nls")
	protected String generateToString() {
		return Helpers.inspect(this, false, "data", "rank");
	}

	public double getRank() {
		return rank;
	}

	@Override
	public int hashCode() {
		return data.hashCode();
	}

	private int hashCodeThis() {
		return super.hashCode();
	}

	public void increaseRank(double incRank) {
		setRank(rank + incRank);
	}

	public void setRank(double rank) {
		this.rank= rank;
		dataUpdated();
	}
}
