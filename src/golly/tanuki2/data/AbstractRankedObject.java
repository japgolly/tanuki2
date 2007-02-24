package golly.tanuki2.data;

import golly.tanuki2.support.Helpers;

/**
 * This is class holds an object and an integer that represents its rank.<br>
 * <code>equals()</code> and <code>hashCode()</code> both delegate to the object so this class can be used in place
 * of the object in <code>Set</code>s etc. This is also implements <code>Comparable</code> and sorts by rank.
 * 
 * @author Golly
 * @since 24/02/2007
 */
public abstract class AbstractRankedObject<T> extends AbstractDataObject implements Comparable<AbstractRankedObject<T>> {
	public final T data;
	public double rank;

	public AbstractRankedObject(T rankedObject, double rank) {
		this.data= rankedObject;
		this.rank= rank;
	}

	public int compareTo(AbstractRankedObject<T> b) {
		return rank == b.rank ? 0 : (rank > b.rank ? -1 : 1);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object obj) {
		if (obj instanceof AbstractRankedObject)
			return data.equals(((AbstractRankedObject) obj).data);
		return data.equals(obj);
	}

	@Override
	public int hashCode() {
		return data.hashCode();
	}

	@Override
	@SuppressWarnings("nls")
	protected String generateToString() {
		return Helpers.inspect(this, false, "data", "rank");
	}
}
