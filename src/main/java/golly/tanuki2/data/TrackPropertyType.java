package golly.tanuki2.data;

import golly.tanuki2.core.ITextProcessor;

/**
 * @author Golly
 * @since 23/02/2007
 */
@SuppressWarnings("nls")
public enum TrackPropertyType {
	ARTIST("artist", true) {
		@Override
		public String getValue(FileData fd) {
			final AlbumData ad= fd.getAlbumData();
			return (ad == null) ? null : ad.getArtist();
		}

		@Override
		public void setValue(FileData fd, String value, ITextProcessor textProcessor) {
			getWritableAlbumData(fd).setArtist(textProcessor.processText(value));
		}
	},

	YEAR("year", true) {
		@Override
		public String getValue(FileData fd) {
			final AlbumData ad= fd.getAlbumData();
			if (ad == null) {
				return null;
			}
			final Integer i= ad.getYear();
			return (i == null) ? null : i.toString();
		}

		@Override
		public void setValue(FileData fd, String value, ITextProcessor textProcessor) {
			getWritableAlbumData(fd).setYear(value);
		}
	},

	ALBUM("album", true) {
		@Override
		public String getValue(FileData fd) {
			final AlbumData ad= fd.getAlbumData();
			return (ad == null) ? null : ad.getAlbum();
		}

		@Override
		public void setValue(FileData fd, String value, ITextProcessor textProcessor) {
			getWritableAlbumData(fd).setAlbum(textProcessor.processText(value));
		}
	},

	TN("tn", false) {
		@Override
		public String getValue(FileData fd) {
			final Integer i= fd.getTn();
			return (i == null) ? null : i.toString();
		}

		@Override
		public void setValue(FileData fd, String value, ITextProcessor textProcessor) {
			fd.setTn(value);
		}
	},

	TRACK("track", false) {
		@Override
		public String getValue(FileData fd) {
			return fd.getTrack();
		}

		@Override
		public void setValue(FileData fd, String value, ITextProcessor textProcessor) {
			fd.setTrack(textProcessor.processText(value));
		}
	},
	;

	// ==============================================================================================

	public static TrackPropertyType[] albumTypes= new TrackPropertyType[] {ARTIST, YEAR, ALBUM};

	public final String name;
	public final boolean albumProperty;

	private TrackPropertyType(String name, boolean albumProperty) {
		this.name= name;
		this.albumProperty= albumProperty;
	}

	public abstract String getValue(FileData fd);

	public abstract void setValue(FileData fd, String value, ITextProcessor textProcessor);

	private static AlbumData getWritableAlbumData(FileData fd) {
		AlbumData ad= fd.getAlbumData();
		if (ad == null) {
			fd.setAlbumData(ad= new AlbumData());
		}
		return ad;
	}
}