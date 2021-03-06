package golly.tanuki2.data;

import golly.tanuki2.support.TanukiImage;

import org.eclipse.swt.graphics.Image;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class FileData extends AbstractDataObject {
	private AlbumData albumData= null;
	private final DirData dirData;
	private boolean isAudio= false;
	private boolean isMarkedForDeletion= false;
	private TanukiImage mimeImage= null;
	private long size= 0;
	private Integer tn= null;
	private String track= null;

	public FileData(final DirData dirData) {
		this.dirData= dirData;
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

	public long getSize() {
		return size;
	}

	public Integer getTn() {
		return tn;
	}

	public String getTrack() {
		return track;
	}

	public boolean isAudio() {
		return isAudio;
	}

	public boolean isComplete(boolean checkAlbumDataToo) {
		if (tn == null || track == null) {
			return false;
		}
		return checkAlbumDataToo ? (albumData != null && albumData.isComplete()) : true;
	}

	public boolean isEmpty() {
		return (albumData == null || albumData.isEmpty()) && tn == null && track == null;
	}

	public boolean isMarkedForDeletion() {
		return isMarkedForDeletion;
	}

	public void setAlbumData(AlbumData albumData) {
		this.albumData= albumData;
		dataUpdated();
	}

	public void setAudio(boolean isAudio) {
		this.isAudio= isAudio;
		dataUpdated();
	}

	public void setMarkedForDeletion(boolean isMarkedForDeletion) {
		this.isMarkedForDeletion= isMarkedForDeletion;
	}

	public void setMimeImage(TanukiImage mimeImage) {
		this.mimeImage= mimeImage;
		dataUpdated();
	}

	public void setSize(long size) {
		this.size= size;
	}

	public void setTn(Integer tn) {
		this.tn= tn;
		dataUpdated();
	}

	public void setTn(String tn) {
		setTn((tn == null ? null : Integer.parseInt(tn)));
	}

	public void setTrack(String track) {
		this.track= track;
		dataUpdated();
	}
}