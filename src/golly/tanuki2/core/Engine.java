package golly.tanuki2.core;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObject;
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
		dd.setHasAudioContent(false);
		for (FileData fd : dd.files.values())
			if (fd.isAudio()) {
				dd.setHasAudioContent(true);
				dirsNeedingTrackProprties.add(dd);
				break;
			}
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
		final Map<DirData, Map<String, List<TrackProperties>>> unassignedData= new HashMap<DirData, Map<String, List<TrackProperties>>>();
		final RankedObjectCollection<AlbumData> sharedAlbumData= new RankedObjectCollection<AlbumData>();

		// ====================================================================
		// Outer pass 1
		for (DirData dd : dirsNeedingTrackProprties) {
			final Map<String, FileData> ddFiles= dd.files;
			final Set<String> assignedOk= new HashSet<String>();

			// Read values
			final Map<String, List<TrackProperties>> trackPropertyMap= new HashMap<String, List<TrackProperties>>();
			for (ITrackProprtyReader reader : trackProprtyReaders)
				Helpers.mergeListMap(trackPropertyMap, reader.readMultipleTrackProperties(dd));

			// TODO: Merge TPs
			// TODO: Remove duplicate TPs

			// Assign (first pass: assign if only one result)
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackProperties> resultArray= trackPropertyMap.get(filename);
				if (resultArray.size() == 1) {
					TrackProperties tp= resultArray.iterator().next();
					assignTrackPropertiesToFile(ddFiles.get(filename), tp, sharedAlbumData);
					assignedOk.add(filename);
				}
			}

			// Assign (second pass: if more than one result, check AlbumData of others in this dir)
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackProperties> resultArray= trackPropertyMap.get(filename);
				if (resultArray.size() > 1)
					second_pass: for (RankedObject<AlbumData> adr : sharedAlbumData)
						for (TrackProperties tp : resultArray)
							if (adr.data.equals(tp.toAlbumData())) {
								assignTrackPropertiesToFile(ddFiles.get(filename), tp, sharedAlbumData);
								assignedOk.add(filename);
								break second_pass;
							}
			}

			// Save any still unassigned
			for (String filename : assignedOk)
				trackPropertyMap.remove(filename);
			if (!trackPropertyMap.isEmpty())
				unassignedData.put(dd, trackPropertyMap);
		} // end outer pass 1

		// ====================================================================
		// Outer pass 2
		final RankedObjectCollection<String> rankedArtists= getRankedArtists(true);
		for (DirData dd : unassignedData.keySet()) {
			final Map<String, List<TrackProperties>> trackPropertyMap= unassignedData.get(dd);
			final Map<String, FileData> ddFiles= dd.files;
			final Set<String> assignedOk= new HashSet<String>();

			// Assign: if there is one result whose artist has a higher rank than any other results, then just use it
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackProperties> resultArray= trackPropertyMap.get(filename);
				if (resultArray.size() > 1) {
					final RankedObjectCollection<TrackProperties> rankedTPs= new RankedObjectCollection<TrackProperties>();
					for (TrackProperties tp : resultArray) {
						double rank= 0;
						String artist= tp.get(TrackPropertyType.ARTIST);
						if (artist != null) {
							artist= Helpers.normalizeText(artist);
							if (rankedArtists.contains(artist))
								rank= rankedArtists.getRank(artist);
						}
						rankedTPs.add(tp, rank);
					}
					if (rankedTPs.hasSingleWinner()) {
						assignTrackPropertiesToFile(ddFiles.get(filename), rankedTPs.getWinner(), sharedAlbumData);
						assignedOk.add(filename);
					}
				}
			}

			// Remove successful
			for (String filename : assignedOk)
				trackPropertyMap.remove(filename);

		} // end outer pass 2

		dirsNeedingTrackProprties.clear();
	}

	private void assignTrackPropertiesToFile(final FileData fd, TrackProperties tp, final RankedObjectCollection<AlbumData> sharedAlbumData) {
		fd.setTn(tp.get(TrackPropertyType.TN));
		fd.setTrack(tp.get(TrackPropertyType.TRACK));
		final AlbumData ad= sharedAlbumData.increaseRank(tp.toAlbumData(), 1).data;
		fd.setAlbumData(ad);
	}
}
