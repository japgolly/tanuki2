package golly.tanuki2.core;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.TrackPropertyMap;

import java.util.List;
import java.util.Map;

/**
 * @author Golly
 * @since 24/02/2007
 */
public interface ITrackPropertyReader {

	/**
	 * Returns a map of possible {@link TrackPropertyMap}s read for each audio file contained in the given
	 * {@link DirData}.
	 * 
	 * @param dd A directory worth of files.
	 * @return A map. This cannot be <code>null</code> nor can it contain any <code>null</code> keys or values.
	 */
	public abstract Map<String, List<TrackPropertyMap>> readMultipleTrackProperties(final DirData dd);

	/**
	 * Reads and returns a list of possible {@link TrackPropertyMap}s for a given file.
	 * 
	 * @param filename The filename of the target file.
	 * @return A list. This cannot be <code>null</code> nor can it contain any <code>null</code>s.
	 */
	public abstract List<TrackPropertyMap> readTrackProperties(final String filename);

}