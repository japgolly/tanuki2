package golly.tanuki2.data;

/**
 * This is basicaly the same as <code>AlbumData</code> except it has an integer represents its rank.
 * <code>equals()</code> and <code>hashCode()</code> both delegate to <code>AlbumData</code> so this class can be
 * used in <code>Set</code>s just as <code>AlbumData</code> can. This is also implements <code>Comparable</code>
 * and sorts by rank.
 * 
 * @author Golly
 * @since 21/02/2007
 */
public class AlbumDataAndRank extends AbstractDataObject implements Comparable<AlbumDataAndRank> {
	public final AlbumData ad;
	public int rank;

	public AlbumDataAndRank(final AlbumData ad, int rank) {
		this.ad= ad;
		this.rank= rank;
	}

	public int compareTo(AlbumDataAndRank b) {
		return rank == b.rank ? 0 : (rank > b.rank ? -1 : 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AlbumDataAndRank)
			return ad.equals(((AlbumDataAndRank) obj).ad);
		return ad.equals(obj);
	}

	@Override
	public int hashCode() {
		return ad.hashCode();
	}
}
