package golly.tanuki2.core;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedObject;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.data.TrackProperties;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.support.Helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Golly
 * @since 24/02/2007
 */
class TrackPropertySelectors {

	// =============================================================================================== //
	// = AssignSingleRows
	// =============================================================================================== //
	/**
	 * Assigns result if only one exists.
	 */
	public static class AssignSingleRows extends AbstractTrackPropertySelector {
		public void run(Map<String, FileData> ddFiles, Map<String, List<TrackProperties>> trackPropertyMap, RankedObjectCollection<AlbumData> sharedAlbumData, Set<String> successfulFiles) {
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackProperties> resultArray= trackPropertyMap.get(filename);
				if (resultArray.size() == 1) {
					TrackProperties tp= resultArray.iterator().next();
					assignTrackPropertiesToFile(ddFiles.get(filename), tp, sharedAlbumData);
					successfulFiles.add(filename);
				}
			}
		}
	}

	// =============================================================================================== //
	// = RankEachAlbumPropertyThenRankResults
	// =============================================================================================== //

	/**
	 * Rank each album property individually, then use that to rank each result.
	 */
	public static class RankEachAlbumPropertyThenRankResults extends AbstractTrackPropertySelector {

		private final RankedObjectCollection<String> rankedConfirmedArtists;
		private final RankedObjectCollection<String> rankedUnconfirmedArtists;
		private final boolean firstPass;

		public RankEachAlbumPropertyThenRankResults(RankedObjectCollection<String> rankedConfirmedArtists, RankedObjectCollection<String> rankedUnconfirmedArtists, boolean firstPass) {
			this.rankedConfirmedArtists= rankedConfirmedArtists;
			this.rankedUnconfirmedArtists= rankedUnconfirmedArtists;
			this.firstPass= firstPass;
		}

		public void run(Map<String, FileData> ddFiles, Map<String, List<TrackProperties>> trackPropertyMap, RankedObjectCollection<AlbumData> sharedAlbumData, Set<String> successfulFiles) {
			final Map<TrackPropertyType, RankedObjectCollection<String>> rankedIndividualAlbumProperties= new HashMap<TrackPropertyType, RankedObjectCollection<String>>();
			// STEP 1: Create individual, ranked album properties
			for (TrackPropertyType propType : TrackPropertyType.albumTypes) {
				final RankedObjectCollection<String> roc= new RankedObjectCollection<String>();
				rankedIndividualAlbumProperties.put(propType, roc);
				for (String filename : trackPropertyMap.keySet())
					for (TrackProperties tp : trackPropertyMap.get(filename)) {
						String x= tp.get(propType);
						if (x != null) {
							x= Helpers.normalizeText(x);
							roc.increaseRank(x, 0.01);
						}
					}
			}
			// STEP 2: Also check artists in other dirs (including "undecided"s)
			final RankedObjectCollection<String> artists= rankedIndividualAlbumProperties.get(TrackPropertyType.ARTIST);
			for (RankedObject<String> a : artists) {
				final String na= Helpers.normalizeText(a.data);
				if (rankedConfirmedArtists.contains(na))
					artists.increaseRank(na, 100 * rankedConfirmedArtists.getRank(na));
				if (rankedUnconfirmedArtists.contains(a.data))
					artists.increaseRank(na, rankedUnconfirmedArtists.getRank(na));
			}

			// STEP 3: Rank each tp
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
				// Check for single winner
				if (rankedTPs.hasSingleWinner()) {
					assignTrackPropertiesToFile(ddFiles.get(filename), rankedTPs.getWinner(), sharedAlbumData);
					successfulFiles.add(filename);
				}
				// If more than one winner and not on first pass..
				else if (!firstPass && rankedTPs.getWinnerCount() > 1) {
					// select any of the winners
					TrackProperties winner= rankedTPs.iterator().next().data;
					assignTrackPropertiesToFile(ddFiles.get(filename), winner, sharedAlbumData);
					successfulFiles.add(filename);
					// and increase the rank of all album properties found in the winner
					for (TrackPropertyType propType : TrackPropertyType.albumTypes)
						if (winner.get(propType) != null)
							rankedIndividualAlbumProperties.get(propType).increaseRank(winner.get(propType), 0.0001);
				}
			}
		}
	}

	// =============================================================================================== //
	// = AbstractTrackPropertySelector
	// =============================================================================================== //

	private static abstract class AbstractTrackPropertySelector {
		public abstract void run(Map<String, FileData> ddFiles, Map<String, List<TrackProperties>> trackPropertyMap, RankedObjectCollection<AlbumData> sharedAlbumData, Set<String> successfulFiles);

		private ITextProcessor textProcessor= null;

		public void init(ITextProcessor textProcessor) {
			this.textProcessor= textProcessor;
		}

		protected void assignTrackPropertiesToFile(final FileData fd, TrackProperties tp, final RankedObjectCollection<AlbumData> sharedAlbumData) {
			fd.setTn(tp.get(TrackPropertyType.TN));
			fd.setTrack(textProcessor.processText(tp.get(TrackPropertyType.TRACK)));
			final AlbumData ad= sharedAlbumData.increaseRank(tp.toAlbumData(), 1).data;
			ad.setArtist(textProcessor.processText(ad.getArtist()));
			ad.setAlbum(textProcessor.processText(ad.getAlbum()));
			fd.setAlbumData(ad);
		}
	}

	// =============================================================================================== //
	// = Runner
	// =============================================================================================== //

	public static class Runner {
		private final Map<DirData, Map<String, List<TrackProperties>>> unassignedData;
		private final RankedObjectCollection<AlbumData> sharedAlbumData;
		private final ITextProcessor textProcessor;

		public Runner(final Map<DirData, Map<String, List<TrackProperties>>> unassignedData, ITextProcessor textProcessor) {
			this.unassignedData= unassignedData;
			this.sharedAlbumData= new RankedObjectCollection<AlbumData>();
			this.textProcessor= textProcessor;
		}

		public void run(AbstractTrackPropertySelector selector) {
			selector.init(textProcessor);
			final Set<String> successfulFiles= new HashSet<String>();
			for (DirData dd : unassignedData.keySet()) {
				final Map<String, List<TrackProperties>> trackPropertyMap= unassignedData.get(dd);
				final Map<String, FileData> ddFiles= dd.files;
				selector.run(ddFiles, trackPropertyMap, sharedAlbumData, successfulFiles);
				removeSuccessfulTrackProperties(trackPropertyMap, successfulFiles);
			}
			removeEmptyValues(unassignedData);
		}

		private static void removeEmptyValues(final Map<DirData, Map<String, List<TrackProperties>>> unassignedData) {
			for (Map<String, List<TrackProperties>> tpm : unassignedData.values())
				Helpers.removeEmptyCollections(tpm);
			Helpers.removeEmptyMaps(unassignedData);
		}

		private static void removeSuccessfulTrackProperties(final Map<String, List<TrackProperties>> trackPropertyMap, final Set<String> successfulFiles) {
			for (String filename : successfulFiles)
				trackPropertyMap.remove(filename);
			successfulFiles.clear();
		}
	}
}
