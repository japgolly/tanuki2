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
		final Set<String> successfulFiles= new HashSet<String>();

		// ====================================================================
		// FIRST PASS
		for (DirData dd : dirsNeedingTrackProprties) {
			final Map<String, FileData> ddFiles= dd.files;

			// Read values
			final Map<String, List<TrackProperties>> trackPropertyMap= new HashMap<String, List<TrackProperties>>();
			for (ITrackProprtyReader reader : trackProprtyReaders)
				Helpers.mergeListMap(trackPropertyMap, reader.readMultipleTrackProperties(dd));

			// TODO: Merge TPs
			// TODO: Remove duplicate TPs

			// ATTEMPT: assign if only one result
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackProperties> resultArray= trackPropertyMap.get(filename);
				if (resultArray.size() == 1) {
					TrackProperties tp= resultArray.iterator().next();
					assignTrackPropertiesToFile(ddFiles.get(filename), tp, sharedAlbumData);
					successfulFiles.add(filename);
				}
			}

			// ATTEMPT: if more than one result, check AlbumData of others in this dir
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackProperties> resultArray= trackPropertyMap.get(filename);
				if (resultArray.size() > 1)
					second_pass: for (RankedObject<AlbumData> adr : sharedAlbumData)
						for (TrackProperties tp : resultArray)
							if (adr.data.equals(tp.toAlbumData())) {
								assignTrackPropertiesToFile(ddFiles.get(filename), tp, sharedAlbumData);
								successfulFiles.add(filename);
								break second_pass;
							}
			}

			// Save any still unassigned
			removeSuccessfulTrackProperties(trackPropertyMap, successfulFiles);
			if (!trackPropertyMap.isEmpty())
				unassignedData.put(dd, trackPropertyMap);

		} // end outer pass 1
		removeEmptyValues(unassignedData);

		// ====================================================================
		// SECOND PASS
		final RankedObjectCollection<String> rankedArtists= getRankedArtists(true);
		for (DirData dd : unassignedData.keySet()) {
			final Map<String, List<TrackProperties>> trackPropertyMap= unassignedData.get(dd);
			final Map<String, FileData> ddFiles= dd.files;

			// ATTEMPT: if there is one result whose artist has a higher rank than any other results, then just use it
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
						successfulFiles.add(filename);
					}
				}
			}
			removeSuccessfulTrackProperties(trackPropertyMap, successfulFiles);

		} // end outer pass 2
		removeEmptyValues(unassignedData);

		// ====================================================================
		// LAST PASS: guessing time
		for (DirData dd : unassignedData.keySet()) {
			final Map<String, List<TrackProperties>> trackPropertyMap= unassignedData.get(dd);
			final Map<String, FileData> ddFiles= dd.files;

			// ATTEMPT: rank each album property individually, then use that to rank each tp
			// STEP 1: Create individual, ranked album properties
			final Map<TrackPropertyType, RankedObjectCollection<String>> rankedIndividualAlbumProperties= new HashMap<TrackPropertyType, RankedObjectCollection<String>>();
			for (TrackPropertyType propType : TrackPropertyType.albumTypes) {
				final RankedObjectCollection<String> roc= new RankedObjectCollection<String>();
				rankedIndividualAlbumProperties.put(propType, roc);
				for (String filename : trackPropertyMap.keySet())
					for (TrackProperties tp : trackPropertyMap.get(filename)) {
						String x= tp.get(propType);
						if (x != null) {
							x= Helpers.normalizeText(x);
							roc.increaseRank(x, 1);
						}
					}
			}
			// STEP 2: Rank each tp
			for (String filename : trackPropertyMap.keySet()) {
				final RankedObjectCollection<TrackProperties> rankedTPs= new RankedObjectCollection<TrackProperties>();
				for (TrackProperties tp : trackPropertyMap.get(filename)) {
					double rank= 0;
					for (TrackPropertyType propType : TrackPropertyType.albumTypes) {
						final RankedObjectCollection<String> roc= rankedIndividualAlbumProperties.get(propType);
						String x= tp.get(propType);
						if (x != null)
							rank+= roc.getRank(Helpers.normalizeText(x));
					}
					rankedTPs.add(tp, rank);
				}
				// choose
				if (rankedTPs.hasSingleWinner()) {
					assignTrackPropertiesToFile(ddFiles.get(filename), rankedTPs.getWinner(), sharedAlbumData);
					successfulFiles.add(filename);
				}
			}
			removeSuccessfulTrackProperties(trackPropertyMap, successfulFiles);

			//			// ATTEMPT: just use whatever
			//			for (String filename : trackPropertyMap.keySet()) {
			//				final List<TrackProperties> resultArray= trackPropertyMap.get(filename);
			//				// TODO
			//			}

		} // end last pass

		// Finished. Clean up.
		dirsNeedingTrackProprties.clear();
	}

	private void assignTrackPropertiesToFile(final FileData fd, TrackProperties tp, final RankedObjectCollection<AlbumData> sharedAlbumData) {
		fd.setTn(tp.get(TrackPropertyType.TN));
		fd.setTrack(tp.get(TrackPropertyType.TRACK));
		final AlbumData ad= sharedAlbumData.increaseRank(tp.toAlbumData(), 1).data;
		fd.setAlbumData(ad);
	}

	private void removeEmptyValues(final Map<DirData, Map<String, List<TrackProperties>>> unassignedData) {
		for (Map<String, List<TrackProperties>> tpm : unassignedData.values())
			Helpers.removeEmptyCollections(tpm);
		Helpers.removeEmptyMaps(unassignedData);
	}

	private void removeSuccessfulTrackProperties(final Map<String, List<TrackProperties>> trackPropertyMap, final Set<String> successfulFiles) {
		for (String filename : successfulFiles)
			trackPropertyMap.remove(filename);
		successfulFiles.clear();
	}
}
