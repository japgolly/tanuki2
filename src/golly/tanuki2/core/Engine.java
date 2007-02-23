package golly.tanuki2.core;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.res.TanukiImage;
import golly.tanuki2.support.Helpers;

import java.io.File;
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
	private final FilenameParser filenameParser= new FilenameParser();

	/**
	 * Recursively adds the contents of a folder.
	 */
	public void addFolder(String sourceFolderName) {
		addFolder(new File(sourceFolderName));
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
				break;
			}

		// Guess values
		if (dd.hasAudioContent()) {
			final Map<String, FileData> ddFiles= dd.files;
			final Set<AlbumData> allAlbumProperties= new HashSet<AlbumData>();
			
			// Read values
			final Map<String, List<TrackProperties>> trackPropertyMap= filenameParser.readMultipleTrackProperties(dd);
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackProperties> trackPropertyCollection= trackPropertyMap.get(filename);
				final FileData fd= ddFiles.get(filename);
				
				// Set track filenames
				if (!trackPropertyCollection.isEmpty()) {
					// TODO temporarily just using allAlbumProperties.iterator().next()
					TrackProperties tp= trackPropertyCollection.iterator().next();
					fd.setTn(tp.get(TrackPropertyType.TN));
					fd.setTrack(tp.get(TrackPropertyType.TRACK));
				}
				
				// Save album values
				for (TrackProperties tp : trackPropertyCollection)
					allAlbumProperties.add(tp.toAlbumData());
			}

			// Create album data
			// TODO temporarily just using allAlbumProperties.iterator().next()
			AlbumData ad= null;
			if (!allAlbumProperties.isEmpty())
				ad= allAlbumProperties.iterator().next();
			for (String s : Helpers.sort(ddFiles.keySet())) {
				final FileData fd= ddFiles.get(s);
				if (fd.isAudio())
					fd.setAlbumData(ad);
			}
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
}
