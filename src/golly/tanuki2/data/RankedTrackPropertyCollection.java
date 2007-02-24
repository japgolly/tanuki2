package golly.tanuki2.data;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Golly
 * @since 24/02/2007
 */
public class RankedTrackPropertyCollection {
	private static class RankedTrackProperty extends AbstractRankedObject<TrackProperties> {
		public RankedTrackProperty(TrackProperties rankedObject, double rank) {
			super(rankedObject, rank);
		}
	}

	private final SortedSet<RankedTrackProperty> set= new TreeSet<RankedTrackProperty>();

	public void add(TrackProperties tp, double rank) {
		set.add(new RankedTrackProperty(tp, rank));
	}

	public TrackProperties getWinner() {
		return set.isEmpty() ? null : set.iterator().next().data;
	}

	public boolean hasSingleWinner() {
		if (set.isEmpty())
			return false;
		if (set.size() == 1)
			return true;

		boolean first= true;
		double highest= 0;
		int count= 1;
		for (RankedTrackProperty rtp : set)
			if (first) {
				first= false;
				highest= rtp.rank;
			} else {
				if (highest != rtp.rank)
					break;
				count++;
			}
		return count == 1;
	}
}
