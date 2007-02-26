package golly.tanuki2.data;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This is a collection of objects and their respective rank values.
 * 
 * @author Golly
 * @since 24/02/2007
 */
public class RankedObjectCollection<T> implements Iterable<RankedObject<T>> {
	private final SortedSet<RankedObject<T>> set= new TreeSet<RankedObject<T>>();

	/**
	 * Adds a new object to the collection. It is your responsibility to check whether or not the object already exists
	 * in the collection.
	 */
	public RankedObject<T> add(T data, double rank) {
		RankedObject<T> r= new RankedObject<T>(data, rank);
		set.add(r);
		return r;
	}

	public void clear() {
		set.clear();
	}

	public boolean contains(RankedObject<T> o) {
		return set.contains(o);
	}

	public boolean contains(T o) {
		for (RankedObject<T> i : this)
			if (i.data.equals(o))
				return true;
		return false;
	}

	public RankedObject<T> get(T o) {
		for (RankedObject<T> i : this)
			if (i.data.equals(o))
				return i;
		return null;
	}

	public double getRank(T o) {
		return get(o).getRank();
	}

	/**
	 * Returns the object with the highest rank. If there are multiple objects that all have the highest rank, then
	 * there is no way of determining which one of them will be returned. If there are no objects in the collection then
	 * <code>null</code> will be returned.
	 */
	public T getWinner() {
		return set.isEmpty() ? null : set.iterator().next().data;
	}

	/**
	 * Determines the highest rank and returns the number of objects that have that rank.
	 */
	public int getWinnerCount() {
		if (set.isEmpty())
			return 0;

		if (set.size() == 1)
			return 1;

		boolean first= true;
		double highest= 0;
		int count= 1;
		for (RankedObject<T> ro : set)
			if (first) {
				first= false;
				highest= ro.getRank();
			} else {
				if (highest != ro.getRank())
					break;
				count++;
			}
		return count;
	}

	/**
	 * Indicates whether or not there is one, single winner.
	 */
	public boolean hasSingleWinner() {
		return getWinnerCount() == 1;
	}

	/**
	 * Increases the rank of an object in the collection by a given amount. If the collection doesn't already contain
	 * the opbject, then it is added and given the specified rank.
	 */
	public RankedObject<T> increaseRank(final T data, double incRank) {
		for (RankedObject<T> i : this)
			if (i.data.equals(data)) {
				i.increaseRank(incRank);
				return i;
			}
		return add(data, incRank);
	}

	public boolean isEmpty() {
		return set.isEmpty();
	}

	public Iterator<RankedObject<T>> iterator() {
		return set.iterator();
	}

	public int size() {
		return set.size();
	}
	
	@Override
	public String toString() {
		return set.toString();
	}
}
