package golly.tanuki2.core;

import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class Engine {
	private final static Pattern patAudio= Pattern.compile("^.+\\.(?:mp3|flac|ape|mp4|m4a|ogg|aac|wmv|wav)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private final static Pattern patImage= Pattern.compile("^.+\\.(?:jpe?g|gif|png|bmp)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private final static Pattern patText= Pattern.compile("^.+\\.(?:txt|html?|diz|nfo)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	public final HashMap<String, DirData> dirs= new HashMap<String, DirData>();
	public final HashMap<String, FileData> files= new HashMap<String, FileData>();

	protected final List<ITrackProprtyReader> trackProprtyReaders= new ArrayList<ITrackProprtyReader>();
	protected final Set<DirData> dirsNeedingTrackProprties= new HashSet<DirData>();

	public Engine() {
		trackProprtyReaders.add(new FilenameParser());
	}

	/**
	 * Recursively adds the contents of a folder.
	 */
	public void addFolder(String sourceFolderName) {
		addFolder(new File(sourceFolderName));
		readTrackProprties();
	}

	/**
	 * Returns a map of all artists, and thier rank (occurance count).
	 */
	public RankedObjectCollection<String> getRankedArtists(boolean normalizeArtistNames) {
		final RankedObjectCollection<String> rankedArtists= new RankedObjectCollection<String>();
		for (FileData fd : this.files.values())
			if (fd.getAlbumData() != null) {
				String artist= fd.getAlbumData().getArtist();
				if (artist != null) {
					if (normalizeArtistNames)
						artist= Helpers.normalizeText(artist);
					rankedArtists.increaseRank(artist, 1);
				}
			}
		return rankedArtists;
	}

	/**
	 * Removes either a file or a directory (and its decendants).
	 */
	public void remove(String item) {
		if (files.containsKey(item)) {
			// Remove file
			DirData dd= files.get(item).getDirData();
			dd.files.remove(new File(item).getName());
			files.remove(item);
			if (dd.files.isEmpty())
				removeDir(dd.dir);
			else
				dd.autoSetHasAudioContent();
		} else {
			// Remove dir
			if (dirs.containsKey(item))
				removeDir(item);
			final Pattern p= Pattern.compile("^" + Pattern.quote(item) + "[\\\\/].+$"); //$NON-NLS-1$ //$NON-NLS-2$
			final Set<String> matchingDirs= new HashSet<String>();
			for (String dir : dirs.keySet())
				if (p.matcher(dir).matches())
					matchingDirs.add(dir);
			for (String dir : matchingDirs)
				removeDir(dir);
		}
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	/**
	 * Recursively adds the contents of a folder.
	 */
	private void addFolder(File sourceFolder) {
		// Create or get DirData
		final String sourceFolderName= sourceFolder.getAbsolutePath();
		DirData dd= dirs.get(sourceFolderName);
		if (dd == null)
			dirs.put(sourceFolderName, dd= new DirData(sourceFolderName));

		// Add dir contents
		for (File f : sourceFolder.listFiles())
			if (f.isDirectory())
				addFolder(f);
			else
				addFile(dd, f);

		// Set hasAudioContent
		dd.autoSetHasAudioContent();
		if (dd.hasAudioContent())
			dirsNeedingTrackProprties.add(dd);
	}

	/**
	 * Adds a file.
	 */
	private void addFile(DirData dd, File f) {
		// Create or get FileData
		final String fullFilename= f.getAbsolutePath();
		FileData fd= files.get(fullFilename);
		if (fd == null)
			files.put(fullFilename, fd= new FileData(dd));
		dd.files.put(f.getName(), fd);

		// Check file extension
		fd.setAudio(false);
		if (patAudio.matcher(f.getName()).matches()) {
			fd.setMimeImage(TanukiImage.MIME_AUDIO);
			fd.setAudio(true);
		} else if (patImage.matcher(f.getName()).matches())
			fd.setMimeImage(TanukiImage.MIME_IMAGE);
		else if (patText.matcher(f.getName()).matches())
			fd.setMimeImage(TanukiImage.MIME_TEXT);
	}

	protected void readTrackProprties() {
		// Read properties
		final Map<DirData, Map<String, List<TrackProperties>>> unassignedData= new HashMap<DirData, Map<String, List<TrackProperties>>>();
		for (DirData dd : dirsNeedingTrackProprties) {
			final Map<String, List<TrackProperties>> trackPropertyMap= new HashMap<String, List<TrackProperties>>();
			for (ITrackProprtyReader reader : trackProprtyReaders)
				Helpers.mergeListMap(trackPropertyMap, reader.readMultipleTrackProperties(dd));

			// TODO: Merge TPs
			// TODO: Remove duplicate TPs

			unassignedData.put(dd, trackPropertyMap);
		}

		// Select and assign
		TrackPropertySelectors.Runner trackPropertySelector= new TrackPropertySelectors.Runner(unassignedData);
		trackPropertySelector.run(new TrackPropertySelectors.AssignSingleRows());
		trackPropertySelector.run(new TrackPropertySelectors.CompareAlbumData());
		trackPropertySelector.run(new TrackPropertySelectors.RankEachAlbumPropertyThenRankResults(getRankedArtists(true), rankUnconfirmedArtists(unassignedData), true));
		trackPropertySelector.run(new TrackPropertySelectors.RankEachAlbumPropertyThenRankResults(getRankedArtists(true), rankUnconfirmedArtists(unassignedData), false));

		// Finished. Clean up.
		dirsNeedingTrackProprties.clear();
	}

	private static RankedObjectCollection<String> rankUnconfirmedArtists(Map<DirData, Map<String, List<TrackProperties>>> unassignedData) {
		final RankedObjectCollection<String> x= new RankedObjectCollection<String>();
		for (Map<String, List<TrackProperties>> map : unassignedData.values())
			for (List<TrackProperties> props : map.values())
				for (TrackProperties tp : props)
					if (tp.get(TrackPropertyType.ARTIST) != null)
						x.increaseRank(Helpers.normalizeText(tp.get(TrackPropertyType.ARTIST)), 1);
		return x;
	}

	private void removeDir(String dir) {
		DirData dd= dirs.get(dir);
		for (String file : dd.files.keySet())
			files.remove(Helpers.addPathElement(dir, file));
		dirs.remove(dir);
	}
}
