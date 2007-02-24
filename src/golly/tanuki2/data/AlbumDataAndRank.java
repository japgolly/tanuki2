package golly.tanuki2.data;

import java.util.Set;

/**
 * @author Golly
 * @since 21/02/2007
 */
public class AlbumDataAndRank extends AbstractRankedObject<AlbumData> {

	public AlbumDataAndRank(AlbumData rankedObject, double rank) {
		super(rankedObject, rank);
	}

	public static AlbumDataAndRank addOneToSet(Set<AlbumDataAndRank> set, final AlbumData ad) {
		for (AlbumDataAndRank i : set)
			if (i.data.equals(ad)) {
				i.rank+= 1;
				return i;
			}
		AlbumDataAndRank adr= new AlbumDataAndRank(ad, 1);
		set.add(adr);
		return adr;
	}
}
