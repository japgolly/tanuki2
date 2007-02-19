package golly.tanuki2.data;

/**
 * @author Golly
 * @since 19/02/2007
 */
public class AlbumData {
	private String album= null;
	private String artist= null;
	private Integer year= null;

	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}

	public Integer getYear() {
		return year;
	}

	public boolean isComplete() {
		if (year == null || album == null)
			return false;
		return true;
	}

	public void setAlbum(String album) {
		this.album= album;
	}

	public void setArtist(String artist) {
		this.artist= artist;
	}

	public void setYear(Integer year) {
		this.year= year;
	}
}