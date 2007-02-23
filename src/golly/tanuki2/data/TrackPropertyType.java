package golly.tanuki2.data;

/**
 * @author Golly
 * @since 23/02/2007
 */
@SuppressWarnings("nls")
public enum TrackPropertyType {
	ARTIST("artist", true), //
	YEAR("year", true), //
	ALBUM("album", true), //
	TN("tn", false), //
	TRACK("track", false), //
	;

	public static TrackPropertyType[] albumTypes= new TrackPropertyType[] {ARTIST, YEAR, ALBUM};

	public final String name;
	public final boolean albumProperty;

	private TrackPropertyType(String name, boolean albumProperty) {
		this.name= name;
		this.albumProperty= albumProperty;
	}
}