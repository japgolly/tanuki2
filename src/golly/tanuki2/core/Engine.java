package golly.tanuki2.core;

import static golly.tanuki2.support.Helpers.addPathElements;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;

import java.io.File;
import java.io.IOException;
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
	 * Performs the crazy-ass voodoo magic that this whole app is about, mon.
	 * <ul>
	 * <li>Renames files with complete file and album data</li>
	 * <li>Deletes files marks for deletion</li>
	 * <li>Removes empty directories</li>
	 * </ul>
	 */
	public void doYaVoodoo(final String targetBaseDir) throws IOException {
		final String targetDirFormat= "[:artist:]\\[:year:] - [:album:]"; //$NON-NLS-1$
		final String targetAudioFileFormat= "[:tn:] - [:track:]"; //$NON-NLS-1$

		// TODO: Check for missing files (ie files in memory but no longer on hd)

		// make target dir
		Helpers.mkdir_p(targetBaseDir);

		final Set<String> removeList= new HashSet<String>();
		try {

			for (final String srcDir : Helpers.sort(dirs.keySet())) {
				final DirData dd= dirs.get(srcDir);
				final Map<String, FileData> ddFiles= dd.files;

				AlbumData ad= null;
				boolean processThisDir= true;
				for (FileData fd : ddFiles.values())
					if (fd.isAudio() && !fd.isMarkedForDeletion()) {
						// make sure all audio files are complete
						if (!fd.isComplete(false) || fd.getAlbumData() == null) {
							processThisDir= false;
							break;
						}
						// make sure all album data in sync
						if (ad == null)
							ad= fd.getAlbumData();
						else if (!ad.equals(fd.getAlbumData())) {
							processThisDir= false;
							break;
						}
					}
				if (ad == null)
					processThisDir= false;

				if (processThisDir) {
					// create target dir
					final String targetDir= addPathElements(targetBaseDir, formatFilename(targetDirFormat, ad));
					Helpers.mkdir_p(targetDir);

					for (String f : Helpers.sort(ddFiles.keySet())) {
						final FileData fd= ddFiles.get(f);
						final String sourceFullFilename= addPathElements(srcDir, f);
						// delete files marked for deletion
						if (fd.isMarkedForDeletion()) {
							deleteFile(sourceFullFilename);
							removeList.add(sourceFullFilename);
						}
						// move all files not marked for deletion
						else {
							final String targetFilename;
							if (fd.isAudio())
								targetFilename= formatFilename(targetAudioFileFormat, fd) + Helpers.getFileExtention(f, true);
							else
								targetFilename= f;
							moveFile(sourceFullFilename, addPathElements(targetDir, targetFilename));
							removeList.add(sourceFullFilename);
						}
					}

					// remove empty dirs from HD
					Helpers.rmdirPath(srcDir);

				} // if (processThisDir)
			} // for dir

		} finally {
			// Remove processed files
			for (String f : removeList)
				remove(f);
			removeEmptyDirs();
		}
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

	/**
	 * Removes entries from <code>dirs</code> if their <code>DirData.files</code> collection is empty.
	 */
	public void removeEmptyDirs() {
		final Set<String> emptyDirs= new HashSet<String>();
		for (String dir : dirs.keySet())
			if (dirs.get(dir).files.isEmpty())
				emptyDirs.add(dir);
		for (String dir : emptyDirs)
			dirs.remove(dir);
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

	private void deleteFile(final String sourceFilename) throws IOException {
		// TODO Move to recycling bin
		if (!new File(sourceFilename).delete())
			throw new IOException("Delete failed. (\"" + sourceFilename + "\")"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String formatFilename(String fmt, AlbumData ad) {
		fmt= fmt.replace("[:artist:]", Helpers.makeFilenameSafe(ad.getArtist())); //$NON-NLS-1$
		fmt= fmt.replace("[:year:]", ad.getYear().toString()); //$NON-NLS-1$
		fmt= fmt.replace("[:album:]", Helpers.makeFilenameSafe(ad.getAlbum())); //$NON-NLS-1$
		return fmt;
	}

	private String formatFilename(String fmt, FileData fd) {
		fmt= formatFilename(fmt, fd.getAlbumData());
		fmt= fmt.replace("[:tn:]", String.format("%02d", fd.getTn())); //$NON-NLS-1$ //$NON-NLS-2$
		fmt= fmt.replace("[:track:]", Helpers.makeFilenameSafe(fd.getTrack())); //$NON-NLS-1$
		return fmt;
	}

	private void moveFile(final String sourceFilename, final String targetFilename) throws IOException {
		File source= new File(sourceFilename);
		File target= new File(targetFilename);
		if (target.isFile()) {
			// TODO Prompt use to overwrite or not 
		}
		if (!source.renameTo(target))
			throw new IOException("Move failed. (\"" + sourceFilename + "\" --> \"" + targetFilename + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			files.remove(addPathElements(dir, file));
		dirs.remove(dir);
	}
}
