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
import golly.tanuki2.support.I18n;
import golly.tanuki2.ui.YesNoToAllBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 16/02/2007
 */
public class Engine implements ITextProcessor {
	public static class ProcessingCommands {
		public String sourceDirectory= null;
		public String targetDirectory= null;
		public Set<String> deletions= new HashSet<String>();
		public Map<String, String> moves= new HashMap<String, String>();

		public int getCommandCount() {
			return deletions.size() + moves.size();
		}
	}

	public static boolean PRETEND_MODE= false;

	private final static Pattern patAudio= Pattern.compile("^.+\\.(?:mp3|flac|ape|mp4|m4a|ogg|aac|wmv|wav)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private final static Pattern patImage= Pattern.compile("^.+\\.(?:jpe?g|gif|png|bmp|tiff?|tga)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private final static Pattern patText= Pattern.compile("^.+\\.(?:txt|html?|diz|nfo)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	public final HashMap<String, DirData> dirs= new HashMap<String, DirData>();
	public final HashMap<String, FileData> files= new HashMap<String, FileData>();

	protected final List<ITrackProprtyReader> trackProprtyReaders= new ArrayList<ITrackProprtyReader>();
	protected final Set<DirData> dirsNeedingTrackProprties= new HashSet<DirData>();
	private Boolean overwriteAll= null;
	protected boolean autoTitleCase= true;

	public Engine() {
		trackProprtyReaders.add(new FilenameParser());
	}

	/**
	 * Add files, and recursively add contents of folders to the processing list.
	 */
	public void add(boolean autoTitleCase, String... filesAndDirs) {
		this.autoTitleCase= autoTitleCase;
		for (String filename : filesAndDirs) {
			File f= new File(filename);
			if (f.isDirectory())
				addFolder(f);
			else
				addFile(f);
		}
		readAndAssignTrackProprties();
		removeEmptyDirs();
	}

	public Map<String, ProcessingCommands> createProcessingList(final String targetBaseDir) {
		// TODO output formats shouldn't be hard-coded
		final String targetDirFormat= "[:artist:]\\[:year:] - [:album:]"; //$NON-NLS-1$
		final String targetAudioFileFormat= "[:tn:] - [:track:]"; //$NON-NLS-1$

		final Map<String, ProcessingCommands> processingList= new HashMap<String, ProcessingCommands>();
		for (final String srcDir : dirs.keySet()) {
			final DirData dd= dirs.get(srcDir);
			final Map<String, FileData> ddFiles= dd.files;
			ProcessingCommands pc= new ProcessingCommands();

			// If the dir has audio content...
			if (dd.hasAudioContent(false)) {
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
				if (ad == null || !ad.isComplete())
					processThisDir= false;

				// Process this dir
				if (processThisDir) {
					pc.targetDirectory= addPathElements(targetBaseDir, formatFilename(targetDirFormat, ad));
					for (String f : ddFiles.keySet()) {
						final FileData fd= ddFiles.get(f);
						// delete files marked for deletion
						if (fd.isMarkedForDeletion())
							pc.deletions.add(f);
						// move all files not marked for deletion
						else {
							final String targetFilename;
							if (fd.isAudio())
								targetFilename= formatFilename(targetAudioFileFormat, fd) + Helpers.getFileExtention(f, true).toLowerCase(Locale.ENGLISH);
							else
								targetFilename= f;
							pc.moves.put(f, targetFilename);
						}
					}
				}
			}

			// If the dir doesn't have any audio content
			else
				for (String f : ddFiles.keySet()) {
					final FileData fd= ddFiles.get(f);
					// delete files marked for deletion
					if (fd.isMarkedForDeletion())
						pc.deletions.add(f);
				}

			// Add to processing list
			if (pc.getCommandCount() != 0) {
				pc.sourceDirectory= srcDir;
				processingList.put(srcDir, pc);
			}
		}
		return processingList;
	}

	/**
	 * Performs the crazy-ass voodoo magic that this whole app is about, mon.
	 * <ul>
	 * <li>Renames files with complete file and album data</li>
	 * <li>Deletes files marks for deletion</li>
	 * <li>Removes empty directories</li>
	 * </ul>
	 */
	public void doYaVoodoo(final String targetBaseDir, final IVoodooProgressMonitor progressDlg, Boolean overwriteAll) throws IOException {
		this.overwriteAll= overwriteAll;

		// TODO: Check for missing files (ie files in memory but no longer on hd)

		// Make processing list
		final Map<String, ProcessingCommands> processingList= createProcessingList(targetBaseDir);
		int totalCommands= 0;
		for (ProcessingCommands pc : processingList.values())
			totalCommands+= pc.getCommandCount();

		progressDlg.starting(processingList.size(), totalCommands);
		if (!progressDlg.isCancelled()) {

			// Make target base dir
			if (!PRETEND_MODE)
				Helpers.mkdir_p(targetBaseDir);

			// Start processing
			final Set<String> removeList= new HashSet<String>();
			try {
				for (final String srcDir : Helpers.sort(processingList.keySet())) {
					final ProcessingCommands pc= processingList.get(srcDir);

					progressDlg.nextDir(srcDir, pc.targetDirectory, pc.getCommandCount());

					// Move files
					if (!pc.moves.isEmpty()) {
						if (!PRETEND_MODE)
							Helpers.mkdir_p(pc.targetDirectory);
						for (String sourceFilename : Helpers.sort(pc.moves.keySet())) {
							final String sourceFullFilename= addPathElements(srcDir, sourceFilename);
							progressDlg.nextFile();
							if (moveFile(progressDlg, sourceFullFilename, addPathElements(pc.targetDirectory, pc.moves.get(sourceFilename))))
								removeList.add(sourceFullFilename);
						}
					}

					// Delete files
					for (String f : Helpers.sort(pc.deletions)) {
						final String sourceFullFilename= addPathElements(srcDir, f);
						progressDlg.nextFile();
						deleteFile(progressDlg, sourceFullFilename);
						removeList.add(sourceFullFilename);
					}

					// remove empty dirs from HD
					List<File> removedDirs= new ArrayList<File>();
					if (!PRETEND_MODE)
						Helpers.rmdirPath(new File(srcDir), removedDirs);
					progressDlg.rmdirs(removedDirs);
				}
			} finally {
				// Remove processed files
				for (String f : removeList)
					remove(f);
				removeEmptyDirs();
			}

		} // end if (!progressDlg.isCancelled())

		System.gc();
		progressDlg.finished();
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

	public String processText(String txt) {
		if (txt != null) {
			if (autoTitleCase)
				txt= Helpers.makeTitleCase(txt);
		}
		return txt;
	}

	/**
	 * Reads potential values for a directory of tracks.
	 * 
	 * @return a map of filenames to list-of-potential-properties.
	 */
	public Map<String, List<TrackProperties>> readTrackProprties(DirData dd) {
		Set<DirData> dirset= new HashSet<DirData>();
		dirset.add(dd);
		return readTrackProprties(dirset).get(dd);
	}

	/**
	 * Reads potential values for a number of directories of tracks.
	 * 
	 * @return a map of filenames to list-of-potential-properties for each directory.
	 */
	public Map<DirData, Map<String, List<TrackProperties>>> readTrackProprties(Set<DirData> dirsToProcess) {
		final Map<DirData, Map<String, List<TrackProperties>>> unassignedData= new HashMap<DirData, Map<String, List<TrackProperties>>>();
		for (DirData dd : dirsToProcess) {
			final Map<String, List<TrackProperties>> trackPropertyMap= new HashMap<String, List<TrackProperties>>();
			for (ITrackProprtyReader reader : trackProprtyReaders)
				Helpers.mergeListMap(trackPropertyMap, reader.readMultipleTrackProperties(dd));
			unassignedData.put(dd, trackPropertyMap);
		}
		return unassignedData;
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

	private void addFolder(File sourceFolder) {
		final DirData dd= getOrCreateDirData(sourceFolder.getAbsolutePath());
		for (File f : sourceFolder.listFiles())
			if (f.isDirectory())
				addFolder(f);
			else
				addFile(dd, f);
		afterAddingFilesToDir(dd);
	}

	private void addFile(File f) {
		final DirData dd= getOrCreateDirData(f.getParentFile().getAbsolutePath());
		addFile(dd, f);
		afterAddingFilesToDir(dd);
	}

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

		// Get file size
		fd.setSize(f.length());
	}

	private void afterAddingFilesToDir(final DirData dd) {
		// Update dd
		dd.autoSetHasAudioContent();

		// Place in dirsNeedingTrackProprties
		if (dd.hasAudioContent(true))
			dirsNeedingTrackProprties.add(dd);
	}

	private void deleteFile(IVoodooProgressMonitor progressDlg, final String sourceFilename) throws IOException {
		// TODO Move to recycling bin
		final File f= new File(sourceFilename);
		progressDlg.deleting(f);
		if (!PRETEND_MODE)
			if (!f.delete())
				throw new IOException("Delete failed. (\"" + sourceFilename + "\")"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private DirData getOrCreateDirData(String dir) {
		DirData dd= dirs.get(dir);
		if (dd == null)
			dirs.put(dir, dd= new DirData(dir));
		return dd;
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

	private boolean moveFile(final IVoodooProgressMonitor progressDlg, final String sourceFilename, final String targetFilename) throws IOException {
		File source= new File(sourceFilename);
		File target= new File(targetFilename);

		if (source.equals(target))
			return true;

		// Target file already exists
		if (target.isFile()) {
			final boolean overwrite;
			if (overwriteAll == null)
				switch (YesNoToAllBox.show(progressDlg.getShell(), I18n.l("voodoo_txt_overwritePrompt", targetFilename), YesNoToAllBox.Value.NO)) { //$NON-NLS-1$
				case NO:
					overwrite= false;
					break;
				case YES:
					overwrite= true;
					break;
				case NO_TO_ALL:
					overwriteAll= overwrite= false;
					break;
				case YES_TO_ALL:
					overwriteAll= overwrite= true;
					break;
				default:
					throw new RuntimeException(); // This is just to shut up the compiler.
				}
			else
				overwrite= overwriteAll;
			if (!overwrite)
				return false;
		}

		// Move file (and overwrite if neccessary)
		progressDlg.moving(source, target);
		if (!PRETEND_MODE)
			Helpers.mv(source, target);
		return true;
	}

	protected void readAndAssignTrackProprties() {
		// Read properties
		final Map<DirData, Map<String, List<TrackProperties>>> unassignedData= readTrackProprties(dirsNeedingTrackProprties);

		// Remove for files that already have values
		final Set<String> removeList= new HashSet<String>();
		for (DirData dd : unassignedData.keySet()) {
			final Map<String, List<TrackProperties>> trackPropertyMap= unassignedData.get(dd);
			// Find out which to delete
			for (String f : trackPropertyMap.keySet()) {
				FileData fd= dd.files.get(f);
				if (fd.isMarkedForDeletion() || !fd.isEmpty())
					removeList.add(f);
			}
			// Delete them
			for (String f : removeList)
				trackPropertyMap.remove(f);
			removeList.clear();
		}

		// Select and assign
		TrackPropertySelectors.Runner trackPropertySelector= new TrackPropertySelectors.Runner(unassignedData, this);
		trackPropertySelector.run(new TrackPropertySelectors.AssignSingleRows());
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
