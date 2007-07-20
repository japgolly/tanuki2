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

	public abstract Map<String, List<TrackPropertyMap>> readMultipleTrackProperties(final DirData dd);

	public abstract List<TrackPropertyMap> readTrackProperties(final String filename);

}