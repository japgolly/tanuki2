package golly.tanuki2.data;

import golly.tanuki2.support.Helpers;

/**
 * @author Golly
 * @since 19/02/2007
 */
abstract class AbstractDataObject {
	private String toStringCache= null;
	private int hashcode= 0;

	protected void dataUpdated() {
		toStringCache= null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AbstractDataObject)) {
			return false;
		}
		AbstractDataObject o2= (AbstractDataObject) obj;
		return toString().equals(o2.toString());
	}

	@Override
	public int hashCode() {
		if (toStringCache == null) {
			toString();
		}
		return hashcode;
	}

	@Override
	public String toString() {
		if (toStringCache == null) {
			toStringCache= generateToString();
			hashcode= toString().intern().hashCode();
		}
		return toStringCache;
	}

	protected String generateToString() {
		return Helpers.inspect(this, false);
	}
}
