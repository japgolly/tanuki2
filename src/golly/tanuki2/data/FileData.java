package golly.tanuki2.data;

import golly.tanuki2.res.TanukiImage;

import org.eclipse.swt.graphics.Image;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class FileData {
	private AlbumData albumData= null;
	private final DirData dirData;
	private boolean isAudio= false;
	private TanukiImage mimeImage= null;
	private Integer tn= null;
	private String track= null;

	public FileData(final DirData dirData) {
		this.dirData= dirData;
	}
	
	public String getAlbum() {
		return albumData == null ? null : albumData.getAlbum();
	}

	public AlbumData getAlbumData() {
		return albumData;
	}

	public DirData getDirData() {
		return dirData;
	}

	public Image getImage() {
		return mimeImage == null ? null : mimeImage.get();
	}

	public TanukiImage getMimeImage() {
		return mimeImage;
	}

	public Integer getTn() {
		return tn;
	}

	public String getTrack() {
		return track;
	}

	public Integer getYear() {
		return albumData == null ? null : albumData.getYear();
	}

	public boolean isAudio() {
		return isAudio;
	}

	public boolean isComplete() {
		if (albumData == null || tn == null || track == null)
			return false;
		return albumData.isComplete();
	}

	public void setAlbumData(AlbumData albumData) {
		this.albumData= albumData;
	}

	public void setAudio(boolean isAudio) {
		this.isAudio= isAudio;
	}
	
	public void setMimeImage(TanukiImage mimeImage) {
		this.mimeImage= mimeImage;
	}

	public void setTn(Integer tn) {
		this.tn= tn;
	}

	public void setTrack(String track) {
		this.track= track;
	}
}