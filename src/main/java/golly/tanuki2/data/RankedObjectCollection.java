package golly.tanuki2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is a collection of objects and their respective rank values.
 * <p>
 * The collection will constantly be sorted by rank (highest to lowest) however if you change the rank of a
 * <code>RankedObject</code> directly using its {@link RankedObject#increaseRank(double)} method instead of using this
 * class's {@link #increaseRank(Object, double)} method, then you will need to call {@link #sort()} to resort the
 * collection.
 * <p>
 * 
 * @author Golly
 * @since 24/02/2007
 */
public class RankedObjectCollection<T> implements Iterable<RankedObject<T>> {
	private final List<RankedObject<T>> collection= new ArrayList<RankedObject<T>>();

	/**
	 * Adds a new object to the collection.
	 * <p>
	 * <u><strong>WARNING:</strong></u> This is not a set, this will always <em>add</em> objects. It is your
	 * responsibility to check whether or not the object already exists in the collection if you do not want duplicates.
	 * Alternatively call {@link #increaseRank(Object, double)}.
	 */
	public RankedObject<T> add(T data, double rank) {
		RankedObject<T> r= new RankedObject<T>(data, rank);
		collection.add(r);
		sort();
		return r;
	}

	public void clear() {
		collection.clear();
	}

	public boolean contains(RankedObject<T> o) {
		return collection.contains(o);
	}

	public boolean contains(T o) {
		return get(o) != null;
	}

	public RankedObject<T> get(T o) {
		if (o == null) {
			for (RankedObject<T> i : this) {
				if (i.data == null) {
					return i;
				}
			}
		} else {
			for (RankedObject<T> i : this) {
				if (o.equals(i.data)) {
					return i;
				}
			}
		}
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
		return collection.isEmpty() ? null : iterator().next().data;
	}

	/**
	 * Determines the highest rank and returns the number of objects that have that rank.
	 */
	public int getWinnerCount() {
		if (collection.isEmpty()) {
			return 0;
		}

		if (collection.size() == 1) {
			return 1;
		}

		boolean first= true;
		double highest= 0;
		int count= 1;
		for (RankedObject<T> ro : collection) {
			if (first) {
				first= false;
				highest= ro.getRank();
			} else {
				if (highest != ro.getRank()) {
					break;
				}
				count++;
			}
		}
		return count;
	}

	@SuppressWarnings("unchecked")
	public RankedObject<T>[] getWinners() {
		int i= getWinnerCount();
		RankedObject<T>[] winners= new RankedObject[i];
		for (RankedObject<T> ro : collection) {
			winners[--i]= ro;
			if (i == 0) {
				break;
			}
		}
		return winners;
	}

	public Double getWinningRank() {
		return collection.isEmpty() ? null : iterator().next().getRank();
	}

	/**
	 * Indicates whether or not there is one, single winner.
	 */
	public boolean hasSingleWinner() {
		return getWinnerCount() == 1;
	}

	/**
	 * Increases the rank of an object in the collection by a given amount. If the collection doesn't already contain
	 * the object, then it is added and given the specified rank.
	 */
	public RankedObject<T> increaseRank(final T data, double incRank) {
		final RankedObject<T> ro= get(data);
		if (ro == null) {
			return add(data, incRank);
		} else {
			ro.increaseRank(incRank);
			sort();
			return ro;
		}
	}

	public boolean isEmpty() {
		return collection.isEmpty();
	}

	public Iterator<RankedObject<T>> iterator() {
		return collection.iterator();
	}

	public boolean remove(T data) {
		RankedObject<T> ro= get(data);
		if (ro == null) {
			return false;
		} else {
			collection.remove(ro);
			sort();
			return true;
		}
	}

	public int size() {
		return collection.size();
	}

	public void sort() {
		Collections.sort(collection);
	}

	@Override
	public String toString() {
		return collection.toString();
	}
}
