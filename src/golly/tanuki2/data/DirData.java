package golly.tanuki2.data;

import golly.tanuki2.support.Helpers;

import java.util.HashMap;

/**
 * @author Golly
 * @since 16/02/2007
 */
public final class DirData extends AbstractDataObject {
	public final String dir;
	public final HashMap<String, FileData> files;
	private boolean hasAudioContent= false;

	public DirData(final String dir) {
		this.dir= dir;
		this.files= new HashMap<String, FileData>();
	}

	@Override
	protected String generateToString() {
		return Helpers.inspectExcept(this, false, "files"); //$NON-NLS-1$
	}

	public boolean getHasAudioContent() {
		return hasAudioContent;
	}

	public void setHasAudioContent(boolean hasAudioContent) {
		dataUpdated();
		this.hasAudioContent= hasAudioContent;
	}
}
