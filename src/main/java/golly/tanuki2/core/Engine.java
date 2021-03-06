package golly.tanuki2.core;

import static golly.tanuki2.support.Helpers.addPathElements;
import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.data.TrackPropertyMap;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.modules.FilenameParser;
import golly.tanuki2.modules.ID3V1TagReader;
import golly.tanuki2.modules.ID3V2TagReader;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.I18n;
import golly.tanuki2.support.OSSpecific;
import golly.tanuki2.support.RichRandomAccessFileCache;
import golly.tanuki2.support.RuntimeConfig;
import golly.tanuki2.support.TanukiException;
import golly.tanuki2.support.TanukiImage;
import golly.tanuki2.support.UIHelpers;
import golly.tanuki2.ui.YesNoToAllBox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;

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

	@SuppressWarnings("serial")
	private static class VoodooAborted extends Exception {
	}

	public static boolean PRETEND_MODE= false;

	private final static Pattern patAudio= Pattern.compile("^.+\\.(?:mp3|flac|ape|mp4|m4a|ogg|aac|wmv|wav)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private final static Pattern patImage= Pattern.compile("^.+\\.(?:jpe?g|gif|png|bmp|tiff?|tga)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
	private final static Pattern patText= Pattern.compile("^.+\\.(?:txt|html?|diz|nfo)$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	public final HashMap<String, DirData> dirs= new HashMap<String, DirData>();
	public final HashMap<String, FileData> files= new HashMap<String, FileData>();

	protected final List<ITrackPropertyReader> trackProprtyReaders= new ArrayList<ITrackPropertyReader>();
	protected final Set<DirData> dirsNeedingTrackProprties= new HashSet<DirData>();
	private Boolean overwriteAll= null;

	public Engine() {
		trackProprtyReaders.add(new FilenameParser());
		trackProprtyReaders.add(new ID3V1TagReader());
		trackProprtyReaders.add(new ID3V2TagReader());
	}

	/**
	 * Add files, and recursively add contents of folders to the processing list.
	 */
	public void add(String... filesAndDirs) {
		for (String filename : filesAndDirs) {
			File f= new File(filename);
			if (f.isDirectory()) {
				addFolder(f);
			} else {
				addFile(f);
			}
		}
		readAndAssignTrackProprties();
		removeEmptyDirs();
	}

	public Map<String, ProcessingCommands> createProcessingList(String targetBaseDir) {
		if (targetBaseDir != null) {
			targetBaseDir= Helpers.ensureCorrectDirSeperators(targetBaseDir);
		}
		final String targetDirFormat= Helpers.ensureCorrectDirSeperators(RuntimeConfig.getInstance().targetDirFormat);
		final String targetAudioFileFormat= Helpers.ensureCorrectDirSeperators(RuntimeConfig.getInstance().targetAudioFileFormat);

		final Map<String, ProcessingCommands> processingList= new HashMap<String, ProcessingCommands>();
		for (final String srcDir : dirs.keySet()) {
			final DirData dd= dirs.get(srcDir);
			final Map<String, FileData> ddFiles= dd.files;
			ProcessingCommands pc= new ProcessingCommands();

			// If the dir has audio content...
			if (dd.hasAudioContent(false)) {
				AlbumData ad= null;
				boolean processThisDir= true;
				for (FileData fd : ddFiles.values()) {
					if (fd.isAudio() && !fd.isMarkedForDeletion()) {
						// make sure all audio files are complete
						if (!fd.isComplete(false) || fd.getAlbumData() == null) {
							processThisDir= false;
							break;
						}
						// make sure all album data in sync
						if (ad == null) {
							ad= fd.getAlbumData();
						} else if (!ad.equals(fd.getAlbumData())) {
							processThisDir= false;
							break;
						}
					}
				}
				if (ad == null || !ad.isComplete()) {
					processThisDir= false;
				}

				// Process this dir
				if (processThisDir) {
					pc.targetDirectory= addPathElements(targetBaseDir, formatFilename(targetDirFormat, ad));
					for (String f : ddFiles.keySet()) {
						final FileData fd= ddFiles.get(f);
						// delete files marked for deletion
						if (fd.isMarkedForDeletion()) {
							pc.deletions.add(f);
						} else {
							final String targetFilename;
							if (fd.isAudio()) {
								targetFilename= formatFilename(targetAudioFileFormat, fd) + Helpers.getFileExtention(f, true).toLowerCase(Locale.ENGLISH);
							} else {
								targetFilename= f;
							}
							pc.moves.put(f, targetFilename);
						}
					}
				}
			} else {
				for (String f : ddFiles.keySet()) {
					final FileData fd= ddFiles.get(f);
					// delete files marked for deletion
					if (fd.isMarkedForDeletion()) {
						pc.deletions.add(f);
					}
				}
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
	public void doYaVoodoo(final String targetBaseDir, final IVoodooProgressMonitor progressDlg, Boolean overwriteAll) {
		this.overwriteAll= overwriteAll;

		// TODO: Check for missing files (ie files in memory but no longer on hd)

		// Make processing list
		final Map<String, ProcessingCommands> processingList= createProcessingList(targetBaseDir);
		int totalCommands= 0;
		for (ProcessingCommands pc : processingList.values()) {
			totalCommands+= pc.getCommandCount();
		}

		progressDlg.starting(processingList.size(), totalCommands);
		boolean aborted= false;
		if (!progressDlg.isCancelled()) {
			try {

				// Make target base dir
				if (!PRETEND_MODE) {
					Helpers.mkdir_p(targetBaseDir);
				}

				// Start processing
				final Set<String> removeList= new HashSet<String>();
				try {
					for (final String srcDir : Helpers.sort(processingList.keySet())) {
						final ProcessingCommands pc= processingList.get(srcDir);

						progressDlg.nextDir(srcDir, pc.targetDirectory, pc.getCommandCount());

						// Move files
						if (!pc.moves.isEmpty()) {
							if (!PRETEND_MODE) {
								Helpers.mkdir_p(pc.targetDirectory);
							}
							for (String sourceFilename : Helpers.sort(pc.moves.keySet())) {
								final String sourceFullFilename= addPathElements(srcDir, sourceFilename);
								progressDlg.nextFile();
								if (moveFile(progressDlg, sourceFullFilename, addPathElements(pc.targetDirectory, pc.moves.get(sourceFilename)))) {
									removeList.add(sourceFullFilename);
								}
							}
						}

						// Delete files
						for (String f : Helpers.sort(pc.deletions)) {
							final String sourceFullFilename= addPathElements(srcDir, f);
							progressDlg.nextFile();
							if (deleteFile(progressDlg, sourceFullFilename)) {
								removeList.add(sourceFullFilename);
							}
						}

						// remove empty dirs from HD
						List<File> removedDirs= new ArrayList<File>();
						if (!PRETEND_MODE) {
							Helpers.rmdirPath(new File(srcDir), removedDirs);
						}
						progressDlg.rmdirs(removedDirs);
					}
				} finally {
					// Remove processed files
					for (String f : removeList) {
						remove(f);
					}
					removeEmptyDirs();
				}

			} catch (Throwable t) {
				if (t instanceof VoodooAborted) {
					aborted= true;
				} else {
					TanukiException.showErrorDialog(t);
				}
			}
		} // end if (!progressDlg.isCancelled())

		System.gc();
		progressDlg.finished(aborted);
	}

	/**
	 * Returns a map of all artists, and thier rank (occurance count).
	 */
	public RankedObjectCollection<String> getRankedArtists(boolean normalizeArtistNames) {
		final RankedObjectCollection<String> rankedArtists= new RankedObjectCollection<String>();
		for (FileData fd : this.files.values()) {
			if (fd.getAlbumData() != null) {
				String artist= fd.getAlbumData().getArtist();
				if (artist != null) {
					if (normalizeArtistNames) {
						artist= Helpers.normalizeText(artist);
					}
					rankedArtists.increaseRank(artist, 1);
				}
			}
		}
		return rankedArtists;
	}

	public String processText(String txt) {
		if (txt != null) {
			if (RuntimeConfig.getInstance().autoTitleCase) {
				txt= Helpers.makeTitleCase(txt, RuntimeConfig.getInstance().intelligentTitleCase);
			}
		}
		return txt;
	}

	/**
	 * Reads potential values for a directory of tracks.
	 * 
	 * @return a map of filenames to list-of-potential-properties.
	 */
	public Map<String, List<TrackPropertyMap>> readTrackProprties(DirData dd) {
		Set<DirData> dirset= new HashSet<DirData>();
		dirset.add(dd);
		return readTrackProprties(dirset).get(dd);
	}

	/**
	 * Reads potential values for a number of directories of tracks.
	 * 
	 * @return a map of filenames to list-of-potential-properties for each directory.
	 */
	public Map<DirData, Map<String, List<TrackPropertyMap>>> readTrackProprties(Set<DirData> dirsToProcess) {
		final Map<DirData, Map<String, List<TrackPropertyMap>>> unassignedData= new HashMap<DirData, Map<String, List<TrackPropertyMap>>>();
		for (DirData dd : dirsToProcess) {
			final Map<String, List<TrackPropertyMap>> trackPropertyMap= new HashMap<String, List<TrackPropertyMap>>();
			for (ITrackPropertyReader reader : trackProprtyReaders) {
				Helpers.mergeListMap(trackPropertyMap, reader.readMultipleTrackProperties(dd));
			}
			unassignedData.put(dd, trackPropertyMap);
		}
		postTrackPropertyReading(unassignedData);
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
			if (dd.files.isEmpty()) {
				removeDir(dd.dir);
			} else {
				dd.autoSetHasAudioContent();
			}
		} else {
			// Remove dir
			if (dirs.containsKey(item)) {
				removeDir(item);
			}
			final Pattern p= Pattern.compile("^" + Pattern.quote(item) + "[\\\\/].+$"); //$NON-NLS-1$ //$NON-NLS-2$
			final Set<String> matchingDirs= new HashSet<String>();
			for (String dir : dirs.keySet()) {
				if (p.matcher(dir).matches()) {
					matchingDirs.add(dir);
				}
			}
			for (String dir : matchingDirs) {
				removeDir(dir);
			}
		}
	}

	/**
	 * Removes entries from <code>dirs</code> if their <code>DirData.files</code> collection is empty.
	 */
	public void removeEmptyDirs() {
		final Set<String> emptyDirs= new HashSet<String>();
		for (String dir : dirs.keySet()) {
			if (dirs.get(dir).files.isEmpty()) {
				emptyDirs.add(dir);
			}
		}
		for (String dir : emptyDirs) {
			dirs.remove(dir);
		}
	}

	// =============================================================================================== //
	// = Internal
	// =============================================================================================== //

	private void addFolder(File sourceFolder) {
		final DirData dd= getOrCreateDirData(sourceFolder.getAbsolutePath());
		for (File f : sourceFolder.listFiles()) {
			if (f.isDirectory()) {
				addFolder(f);
			} else {
				addFile(dd, f);
			}
		}
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
		if (fd == null) {
			files.put(fullFilename, fd= new FileData(dd));
		}
		dd.files.put(f.getName(), fd);

		// Check file extension
		fd.setAudio(false);
		if (patAudio.matcher(f.getName()).matches()) {
			fd.setMimeImage(TanukiImage.MIME_AUDIO);
			fd.setAudio(true);
		} else if (patImage.matcher(f.getName()).matches()) {
			fd.setMimeImage(TanukiImage.MIME_IMAGE);
		} else if (patText.matcher(f.getName()).matches()) {
			fd.setMimeImage(TanukiImage.MIME_TEXT);
		}

		// Get file size
		fd.setSize(f.length());
	}

	private void afterAddingFilesToDir(final DirData dd) {
		// Update dd
		dd.autoSetHasAudioContent();

		// Place in dirsNeedingTrackProprties
		if (dd.hasAudioContent(true)) {
			dirsNeedingTrackProprties.add(dd);
		}
	}

	private static Map<DirData, Map<String, List<TrackPropertyMap>>> copy(Map<DirData, Map<String, List<TrackPropertyMap>>> src) {
		final Map<DirData, Map<String, List<TrackPropertyMap>>> copy= new HashMap<DirData, Map<String, List<TrackPropertyMap>>>(src.size());
		for (Map.Entry<DirData, Map<String, List<TrackPropertyMap>>> e : src.entrySet()) {
			copy.put(e.getKey(), copy2(e.getValue()));
		}
		return copy;
	}

	private static Map<String, List<TrackPropertyMap>> copy2(Map<String, List<TrackPropertyMap>> src) {
		final Map<String, List<TrackPropertyMap>> copy= new HashMap<String, List<TrackPropertyMap>>(src.size());
		for (Map.Entry<String, List<TrackPropertyMap>> e : src.entrySet()) {
			copy.put(e.getKey(), copy(e.getValue()));
		}
		return copy;
	}

	private static List<TrackPropertyMap> copy(List<TrackPropertyMap> src) {
		final List<TrackPropertyMap> copy= new ArrayList<TrackPropertyMap>(src.size());
		copy.addAll(src);
		return copy;
	}

	private boolean deleteFile(IVoodooProgressMonitor progressDlg, final String sourceFilename) throws VoodooAborted {
		// TODO Move to recycling bin
		int completionStatus= IVoodooProgressMonitor.SUCCEEDED;
		try {
			final File f= new File(sourceFilename);
			progressDlg.deleting(f);
			if (PRETEND_MODE) {
				return true;
			}
			while (true) {
				try {
					if (f.delete()) {
						return true;
					}
				} catch (Throwable t) {
				}
				switch (UIHelpers.showAbortIgnoreRetryBox(progressDlg.getShell(), I18n.l("general_error_title"), I18n.l("voodoo_err_deleteFailedPrompt", sourceFilename))) {//$NON-NLS-1$ //$NON-NLS-2$
				case SWT.IGNORE:
					completionStatus= IVoodooProgressMonitor.FAILED;
					return false;
				case SWT.ABORT:
					completionStatus= IVoodooProgressMonitor.FAILED;
					throw new VoodooAborted();
				}
			}
		} finally {
			progressDlg.fileOperationComplete(completionStatus);
		}
	}

	private DirData getOrCreateDirData(String dir) {
		DirData dd= dirs.get(dir);
		if (dd == null) {
			dirs.put(dir, dd= new DirData(dir));
		}
		return dd;
	}

	private String formatFilename(String fmt, AlbumData ad) {
		fmt= fmt.replace("[:artist:]", OSSpecific.makeFilenameSafe(ad.getArtist())); //$NON-NLS-1$
		fmt= fmt.replace("[:year:]", ad.getYear().toString()); //$NON-NLS-1$
		fmt= fmt.replace("[:album:]", OSSpecific.makeFilenameSafe(ad.getAlbum())); //$NON-NLS-1$
		return fmt;
	}

	private String formatFilename(String fmt, FileData fd) {
		fmt= formatFilename(fmt, fd.getAlbumData());
		fmt= fmt.replace("[:tn:]", String.format("%02d", fd.getTn())); //$NON-NLS-1$ //$NON-NLS-2$
		fmt= fmt.replace("[:track:]", OSSpecific.makeFilenameSafe(fd.getTrack())); //$NON-NLS-1$
		return fmt;
	}

	private boolean moveFile(final IVoodooProgressMonitor progressDlg, final String sourceFilename, final String targetFilename) throws VoodooAborted {
		int completionStatus= IVoodooProgressMonitor.SUCCEEDED;
		try {
			File source= new File(sourceFilename);
			File target= new File(targetFilename);

			if (source.equals(target)) {
				return true;
			}

			// Target file already exists
			if (target.isFile()) {
				final boolean overwrite;
				if (overwriteAll == null) {
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
				} else {
					overwrite= overwriteAll;
				}
				if (!overwrite) {
					completionStatus= IVoodooProgressMonitor.SKIPPED;
					return false;
				}
			}

			// Move file (and overwrite if neccessary)
			progressDlg.moving(source, target);
			if (PRETEND_MODE) {
				return true;
			}
			while (true) {
				try {
					Helpers.mv(source, target);
					return true;
				} catch (IOException e) {
					switch (UIHelpers.showAbortIgnoreRetryBox(progressDlg.getShell(), I18n.l("general_error_title"), I18n.l("voodoo_err_movedFailedPrompt", source, target))) {//$NON-NLS-1$ //$NON-NLS-2$
					case SWT.IGNORE:
						completionStatus= IVoodooProgressMonitor.FAILED;
						return false;
					case SWT.ABORT:
						completionStatus= IVoodooProgressMonitor.FAILED;
						throw new VoodooAborted();
					}
				}
			} // end while
		} finally {
			progressDlg.fileOperationComplete(completionStatus);
		}
	}

	/**
	 * This should be called after properties are read from a batch of files.
	 */
	private void postTrackPropertyReading(Map<DirData, Map<String, List<TrackPropertyMap>>> data) {
		// Close open files
		RichRandomAccessFileCache.getInstance().clear();

		// Trim all values and remove if empty
		for (final Map<String, List<TrackPropertyMap>> tpm : data.values()) {
			for (final List<TrackPropertyMap> rows : tpm.values()) {
				for (final TrackPropertyMap row : rows) {
					for (final TrackPropertyType field : new HashSet<TrackPropertyType>(row.keySet())) {
						final String value= row.get(field);
						if (value != null) {
							final String newValue= Helpers.unicodeTrim(value);
							if (newValue.length() == 0) {
								row.remove(field);
							} else {
								row.put(field, newValue);
							}
						}
					}
				}
			}
		}

		// Remove empty containers
		removeEmptyContainers(data);
	}

	protected void readAndAssignTrackProprties() {
		// Read properties
		final Map<DirData, Map<String, List<TrackPropertyMap>>> allData= readTrackProprties(dirsNeedingTrackProprties);

		// Remove blacklisted artists
		removeBlacklistedArtists(allData);

		// Create a separate copy of unassigned data
		final Map<DirData, Map<String, List<TrackPropertyMap>>> unassignedData= copy(allData);

		// Remove for files that already have values
		final Set<String> removeList= new HashSet<String>();
		for (DirData dd : unassignedData.keySet()) {
			final Map<String, List<TrackPropertyMap>> trackPropertyMap= unassignedData.get(dd);
			// Find out which to delete
			for (String f : trackPropertyMap.keySet()) {
				FileData fd= dd.files.get(f);
				if (fd.isMarkedForDeletion() || !fd.isEmpty()) {
					removeList.add(f);
				}
			}
			// Delete them
			for (String f : removeList) {
				trackPropertyMap.remove(f);
			}
			removeList.clear();
		}

		// Select and assign
		TrackPropertySelectors.Runner trackPropertySelector= new TrackPropertySelectors.Runner(unassignedData, this);
		trackPropertySelector.run(new TrackPropertySelectors.AssignSingleRows());
		trackPropertySelector.run(new TrackPropertySelectors.RankEachAlbumPropertyThenRankResults(getRankedArtists(true), rankUnconfirmedArtists(unassignedData), true));
		trackPropertySelector.run(new TrackPropertySelectors.RankEachAlbumPropertyThenRankResults(getRankedArtists(true), rankUnconfirmedArtists(unassignedData), false));
		new TrackPropertySelectors.PopulateEmptyFields(allData, this).run();

		// Finished. Clean up.
		dirsNeedingTrackProprties.clear();
	}

	/**
	 * Removes any {@link TrackPropertyMap}s that contain an artist that matches the artist blacklist pattern.
	 */
	private void removeBlacklistedArtists(final Map<DirData, Map<String, List<TrackPropertyMap>>> data) {
		if (RuntimeConfig.getInstance().artistBlacklist != null) {
			final Pattern artistBlacklist= Pattern.compile(RuntimeConfig.getInstance().artistBlacklist, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			for (Map<String, List<TrackPropertyMap>> map : data.values()) {
				for (List<TrackPropertyMap> list : map.values()) {
					Iterator<TrackPropertyMap> it= list.iterator();
					while (it.hasNext()) {
						TrackPropertyMap tpm= it.next();
						if (tpm.containsKey(TrackPropertyType.ARTIST) && artistBlacklist.matcher(tpm.get(TrackPropertyType.ARTIST)).matches()) {
							it.remove();
						}
					}
				}
			}
		}
	}

	private static RankedObjectCollection<String> rankUnconfirmedArtists(Map<DirData, Map<String, List<TrackPropertyMap>>> unassignedData) {
		final RankedObjectCollection<String> x= new RankedObjectCollection<String>();
		for (Map<String, List<TrackPropertyMap>> map : unassignedData.values()) {
			for (List<TrackPropertyMap> props : map.values()) {
				for (TrackPropertyMap tp : props) {
					if (tp.get(TrackPropertyType.ARTIST) != null) {
						x.increaseRank(Helpers.normalizeText(tp.get(TrackPropertyType.ARTIST)), 1);
					}
				}
			}
		}
		return x;
	}

	private void removeDir(String dir) {
		DirData dd= dirs.get(dir);
		for (String file : dd.files.keySet()) {
			files.remove(addPathElements(dir, file));
		}
		dirs.remove(dir);
	}

	static void removeEmptyContainers(final Map<DirData, Map<String, List<TrackPropertyMap>>> unassignedData) {
		for (Map<String, List<TrackPropertyMap>> tpm : unassignedData.values()) {
			Helpers.removeEmptyCollections(tpm);
		}
		Helpers.removeEmptyMaps(unassignedData);
	}
}
