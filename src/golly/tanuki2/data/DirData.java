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

	public void autoSetHasAudioContent() {
		for (FileData fd : files.values())
			if (fd.isAudio()) {
				setHasAudioContent(true);
				return;
			}
		setHasAudioContent(false);
	}

	@Override
	protected String generateToString() {
		return Helpers.inspectExcept(this, false, "files"); //$NON-NLS-1$
	}

	public boolean hasAudioContent(boolean andNotMarkedForDeletion) {
		if (andNotMarkedForDeletion) {
			if (hasAudioContent)
				for (FileData fd : files.values())
					if (fd.isAudio() && !fd.isMarkedForDeletion())
						return true;
			return false;
		} else
			return hasAudioContent;
	}

	public void setHasAudioContent(boolean hasAudioContent) {
		dataUpdated();
		this.hasAudioContent= hasAudioContent;
	}
}
