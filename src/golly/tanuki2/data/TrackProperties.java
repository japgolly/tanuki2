package golly.tanuki2.data;

import static golly.tanuki2.data.TrackPropertyType.ALBUM;
import static golly.tanuki2.data.TrackPropertyType.ARTIST;
import static golly.tanuki2.data.TrackPropertyType.YEAR;

import java.util.HashMap;

/**
 * @author Golly
 * @since 23/02/2007
 */
public class TrackProperties extends HashMap<TrackPropertyType, String> {
	private static final long serialVersionUID= -1436707286917587072L;

	@Override
	public String put(TrackPropertyType key, String value) {
		if (value == null)
			return remove(key);
		else
			return super.put(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof TrackProperties))
			return false;
		TrackProperties o2= (TrackProperties) obj;
		return toString().equals(o2.toString());
	}

	public AlbumData toAlbumData() {
		AlbumData ad= new AlbumData();
		ad.setArtist(get(ARTIST));
		ad.setYear(get(YEAR));
		ad.setAlbum(get(ALBUM));
		return ad;
	}
}
