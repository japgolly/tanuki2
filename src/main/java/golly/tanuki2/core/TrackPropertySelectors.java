package golly.tanuki2.core;

import golly.tanuki2.data.AlbumData;
import golly.tanuki2.data.DirData;
import golly.tanuki2.data.FileData;
import golly.tanuki2.data.RankedNormalisedStringCollection;
import golly.tanuki2.data.RankedObject;
import golly.tanuki2.data.RankedObjectCollection;
import golly.tanuki2.data.TrackPropertyMap;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.support.Helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		public void run(Map<String, FileData> ddFiles, Map<String, List<TrackPropertyMap>> trackPropertyMap, RankedObjectCollection<AlbumData> sharedAlbumData, Set<String> successfulFiles) {
			for (String filename : trackPropertyMap.keySet()) {
				final List<TrackPropertyMap> resultArray= trackPropertyMap.get(filename);
				if (resultArray.size() == 1) {
					TrackPropertyMap tp= resultArray.iterator().next();
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
		private static final Pattern pUnlikely= Pattern.compile(" - |128|160|192|224|256|320|vbr|kbps|www\\.|\\.(?:com|org|net)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

		private final RankedObjectCollection<String> rankedConfirmedArtists;
		private final RankedObjectCollection<String> rankedUnconfirmedArtists;
		private final boolean firstPass;

		public RankEachAlbumPropertyThenRankResults(RankedObjectCollection<String> rankedConfirmedArtists, RankedObjectCollection<String> rankedUnconfirmedArtists, boolean firstPass) {
			this.rankedConfirmedArtists= rankedConfirmedArtists;
			this.rankedUnconfirmedArtists= rankedUnconfirmedArtists;
			this.firstPass= firstPass;
		}

		public void run(Map<String, FileData> ddFiles, Map<String, List<TrackPropertyMap>> trackPropertyMap, RankedObjectCollection<AlbumData> sharedAlbumData, Set<String> successfulFiles) {
			final Map<TrackPropertyType, RankedObjectCollection<String>> rankedIndividualAlbumProperties= new HashMap<TrackPropertyType, RankedObjectCollection<String>>();
			// STEP 1: Create individual, ranked album properties
			for (TrackPropertyType propType : TrackPropertyType.albumTypes) {
				final RankedObjectCollection<String> roc= new RankedObjectCollection<String>();
				rankedIndividualAlbumProperties.put(propType, roc);
				for (String filename : trackPropertyMap.keySet())
					for (TrackPropertyMap tp : trackPropertyMap.get(filename)) {
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
				final RankedObjectCollection<TrackPropertyMap> rankedTPs= new RankedObjectCollection<TrackPropertyMap>();
				for (TrackPropertyMap tp : trackPropertyMap.get(filename)) {
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
					// Try to change ranks of of the winners
					for (RankedObject<TrackPropertyMap> i : rankedTPs.getWinners()) {
						// Penalise the rank of strings with unlikely substrings in them
						penaliseUnlikelySubstrings(i, i.data.get(TrackPropertyType.ARTIST));
						penaliseUnlikelySubstrings(i, i.data.get(TrackPropertyType.ALBUM));
						penaliseUnlikelySubstrings(i, i.data.get(TrackPropertyType.TRACK));
						// Increase rank of those with TN set
						if (i.data.get(TrackPropertyType.TN) != null)
							i.increaseRank(0.001);
					}
					rankedTPs.sort();
					// select one of the winners
					TrackPropertyMap winner= rankedTPs.iterator().next().data;
					assignTrackPropertiesToFile(ddFiles.get(filename), winner, sharedAlbumData);
					successfulFiles.add(filename);
					// and increase the rank of all album properties found in the winner
					for (TrackPropertyType propType : TrackPropertyType.albumTypes)
						if (winner.get(propType) != null) {
							final String x= Helpers.normalizeText(winner.get(propType));
							rankedIndividualAlbumProperties.get(propType).increaseRank(x, 0.0001);
						}
				}
			}
		}

		private void penaliseUnlikelySubstrings(RankedObject<TrackPropertyMap> i, String a) {
			if (a != null) {
				final Matcher m= pUnlikely.matcher(a);
				while (m.find())
					i.increaseRank(-0.0001);
			}
		}
	}

	// =============================================================================================== //
	// = AbstractTrackPropertySelector
	// =============================================================================================== //

	private static abstract class AbstractTrackPropertySelector {
		public abstract void run(Map<String, FileData> ddFiles, Map<String, List<TrackPropertyMap>> trackPropertyMap, RankedObjectCollection<AlbumData> sharedAlbumData, Set<String> successfulFiles);

		private ITextProcessor textProcessor= null;

		public void init(ITextProcessor textProcessor) {
			this.textProcessor= textProcessor;
		}

		protected void assignTrackPropertiesToFile(final FileData fd, TrackPropertyMap tp, final RankedObjectCollection<AlbumData> sharedAlbumData) {
			TrackPropertyType.TRACK.setValue(fd, tp.get(TrackPropertyType.TRACK), textProcessor);
			TrackPropertyType.TN.setValue(fd, tp.get(TrackPropertyType.TN), textProcessor);
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
		private final Map<DirData, Map<String, List<TrackPropertyMap>>> unassignedData;
		private final RankedObjectCollection<AlbumData> sharedAlbumData;
		private final ITextProcessor textProcessor;

		public Runner(final Map<DirData, Map<String, List<TrackPropertyMap>>> unassignedData, ITextProcessor textProcessor) {
			this.unassignedData= unassignedData;
			this.sharedAlbumData= new RankedObjectCollection<AlbumData>();
			this.textProcessor= textProcessor;
		}

		public void run(AbstractTrackPropertySelector selector) {
			selector.init(textProcessor);
			final Set<String> successfulFiles= new HashSet<String>();
			for (Map.Entry<DirData, Map<String, List<TrackPropertyMap>>> entry : unassignedData.entrySet()) {
				final DirData dd= entry.getKey();
				final Map<String, List<TrackPropertyMap>> trackPropertyMap= entry.getValue();
				final Map<String, FileData> ddFiles= dd.files;
				selector.run(ddFiles, trackPropertyMap, sharedAlbumData, successfulFiles);
				removeSuccessfulTrackProperties(trackPropertyMap, successfulFiles);
			}
			Engine.removeEmptyContainers(unassignedData);
		}

		private static void removeSuccessfulTrackProperties(final Map<String, List<TrackPropertyMap>> trackPropertyMap, final Set<String> successfulFiles) {
			for (String filename : successfulFiles)
				trackPropertyMap.remove(filename);
			successfulFiles.clear();
		}
	}

	// =============================================================================================== //
	// = PopulateEmptyFields
	// =============================================================================================== //
	/**
	 * Attempts to populate any empty fields without any regard for which row each value is in, etc.
	 */
	public static class PopulateEmptyFields {
		private final Map<DirData, Map<String, List<TrackPropertyMap>>> allData;
		private final ITextProcessor textProcessor;
		private RankedObjectCollection<TrackPropertyMap> rankedTrackPropertyMaps= null;

		public PopulateEmptyFields(Map<DirData, Map<String, List<TrackPropertyMap>>> allData, ITextProcessor textProcessor) {
			this.allData= allData;
			this.textProcessor= textProcessor;
		}

		public void run() {
			for (final Map.Entry<DirData, Map<String, List<TrackPropertyMap>>> e1 : allData.entrySet()) {
				final DirData dd= e1.getKey();
				final Map<String, List<TrackPropertyMap>> trackPropertyMap= e1.getValue();
				final Map<String, FileData> ddFiles= dd.files;
				for (final Map.Entry<String, List<TrackPropertyMap>> e2 : trackPropertyMap.entrySet()) {
					final String filename= e2.getKey();
					final List<TrackPropertyMap> rows= trackPropertyMap.get(filename);
					if (rows.size() > 1) {
						final FileData fd= ddFiles.get(filename);
						for (TrackPropertyType field : TrackPropertyType.values())
							if (field.getValue(fd) == null) {

								// Found an empty field
								if (rankedTrackPropertyMaps == null)
									rankEachRow(fd, rows);
								populateEmptyProperty(fd, field, rows);
							}
					}
					rankedTrackPropertyMaps= null;
				}
			}
		}

		/**
		 * Ranks each row. These values are used when we have multiple possible values for a currently empty field. It
		 * helps to improve the general accuracy of the chosen value.
		 */
		private void rankEachRow(final FileData fd, final List<TrackPropertyMap> rows) {
			rankedTrackPropertyMaps= new RankedObjectCollection<TrackPropertyMap>();

			// Assign every row an initial score of 1
			for (TrackPropertyMap tpm : rows)
				rankedTrackPropertyMaps.add(tpm, 1);

			// Compare the values in the rows to the values in the FileData
			for (TrackPropertyType field : TrackPropertyType.values()) {
				final String v= Helpers.normalizeText(field.getValue(fd));
				if (v != null)
					for (TrackPropertyMap row : rows)
						if (v.equals(Helpers.normalizeText(row.get(field))))
							rankedTrackPropertyMaps.increaseRank(row, 1);
						else if (row.get(field) != null)
							rankedTrackPropertyMaps.increaseRank(row, 0.0001);
			}

//			// Compare the values in each row to the values in each row of lower rank
//			// NOTE: After writing this and then realising I had a mistake in my test it turns out
//			// that this might not be required after all.
//			for (TrackPropertyType field : TrackPropertyType.values())
//				for (RankedObject<TrackPropertyMap> outerRow : rankedTrackPropertyMaps) {
//					final String v= Helpers.normalizeText(outerRow.data.get(field));
//					if (v != null) {
//						boolean found= false;
//						for (RankedObject<TrackPropertyMap> innerRow : rankedTrackPropertyMaps) {
//							if (!found) {
//								if (innerRow == outerRow)
//									found= true;
//							} else {
//								if (v.equals(Helpers.normalizeText(innerRow.data.get(field))))
//									innerRow.increaseRank(outerRow.getRank() * 0.08);
//							}
//						}
//					}
//				}
//			rankedTrackPropertyMaps.sort();
		}

		private void populateEmptyProperty(final FileData fd, TrackPropertyType prop, final List<TrackPropertyMap> rows) {
			// Rank each row
			final RankedNormalisedStringCollection rankedValues= new RankedNormalisedStringCollection();
			for (TrackPropertyMap row : rows)
				if (row.containsKey(prop))
					rankedValues.increaseRank(row.get(prop), rankedTrackPropertyMaps.getRank(row) + 1);

			// Populate
			prop.setValue(fd, rankedValues.getWinner(), textProcessor);
		}
	}
}
