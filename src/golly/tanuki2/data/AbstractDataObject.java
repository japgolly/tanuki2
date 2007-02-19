package golly.tanuki2.data;

import golly.tanuki2.support.Helpers;

/**
 * @author Golly
 * @since 19/02/2007
 */
abstract class AbstractDataObject {
	private String toStringCache_noId= null;
	private String toStringCache_withId= null;

	protected void dataUpdated() {
		toStringCache_noId= toStringCache_withId= null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AbstractDataObject))
			return false;
		AbstractDataObject o2= (AbstractDataObject) obj;
		return toStringForEquals().equals(o2.toStringForEquals());
	}

	@Override
	public String toString() {
		if (toStringCache_withId == null)
			toStringCache_withId= Helpers.inspect(this, true);
		return toStringCache_withId;
	}

	private String toStringForEquals() {
		if (toStringCache_noId == null)
			toStringCache_noId= Helpers.inspect(this, false);
		return toStringCache_noId;
	}
}
