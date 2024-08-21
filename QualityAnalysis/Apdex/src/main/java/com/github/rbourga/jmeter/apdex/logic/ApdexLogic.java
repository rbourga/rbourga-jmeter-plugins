/**
 *
 */
package com.github.rbourga.jmeter.apdex.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.util.JMeterUtils;

import com.github.rbourga.jmeter.common.FileServices;

public final class ApdexLogic {

	private static String HTML_STATS_TITLE = "Apdex Score Results";
	private static String SUFFIX_STATS = "_ApdexScores.";

	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] { 
					JMeterUtils.getResString("sampler label"), // Label
					JMeterUtils.getResString("aggregate_report_count"), // # Samples
					/*
					 * Hiding Average as it should be calculated only on successful results and
					 * without outliers whereas Apdex requires the full set of results.
					 */
//					JMeterUtils.getResString("average"), // Average
					JMeterUtils.getResString("aggregate_report_error%"), // # Error %
					"Apdex Value", "Apdex Target (s)", // Target threshold
					"Apdex Rating", "Small Group", // true if number of samples < 100
					"Failed" // true if value less than the specified threshold
			}, new Class[] {
					String.class, // Label
					Integer.class, // # Samples
//					Double.class,	// Average
					Double.class, // # Error %
					Double.class, // Apdex Value
					Double.class, // Apdex Target
					String.class, // Apdex Rating
					String.class, // Small Group
					String.class // Failed
			});
	private static int PASSFAIL_TEST_COLNBR = 7; // Position of Failed column in the table

	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	/*
	 * Validation methods
	 */
	public static boolean isApdexMinScoreOutOfRange(double dValue) {
		return dValue < 0 || dValue > 1;
	}

	public static boolean isCoVPctOutOfRange(double fCoVALPct) {
		return fCoVALPct < 0;
	}

	public static boolean isTgtTHoldOutOfRange(double dValue) {
		return dValue < 0.1;
	}

	/*
	 * Computing method
	 */
	public static int computeApdexScore(String sFilepath, double dApdexTgtTholdSec, double dApdexAQL) {
		// Load the data after getting the delimiter separator from current JMeter
		// properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMap = FileServices.loadSamplesIntoHashMap(sFilepath, cDelim);
		if (rcdHashMap.isEmpty()) {
			return -1; // Nothing loaded, so abort...
		}

		// Clear any statistics from a previous analysis
		pwrTblMdlStats.clearData();

		// Format the threshold as per Apdex specs
		dApdexTgtTholdSec = ApdexLogic.formatTgtTHold(dApdexTgtTholdSec);
		BigDecimal bdApdexAQL = new BigDecimal(dApdexAQL);

		long lApdexTgtTholdMS = (long) (dApdexTgtTholdSec * 1000); // Convert to ms as JMeter times are stored in ms
		long lApdexTolTholdMS = 4 * lApdexTgtTholdMS; // Tolerate = 4xTarget, as per Apdex specs
		int iFailedLblCnt = 0;
		// Now, process the data points in natural order...
		TreeMap<String, List<CSVRecord>> tmSortedRcd = new TreeMap<>(rcdHashMap);
		for (String sLbl : tmSortedRcd.keySet()) {
			List<CSVRecord> aRcd = tmSortedRcd.get(sLbl);
			int iTotRcd = aRcd.size();

			/*
			 * As per Apdex specs, all server failures must be counted as frustrated
			 * regardless of their time. So we must calculate Apdex only on the successful
			 * samples.
			 */
			Stream<CSVRecord> oPassedSamples = aRcd.stream().filter(rcd -> rcd.get("success").equals("true"));
			// Satisfied list of samples: 0 to T
			Stream<CSVRecord> oSatisfiedSamples = oPassedSamples.filter(rcd -> Long.parseLong(rcd.get("elapsed")) <= lApdexTgtTholdMS);
			long lSatisfiedCount = oSatisfiedSamples.count();
			// Tolerating list of samples: T to F
			// Reset the successful stream as a stream can only be used once
			oPassedSamples = aRcd.stream().filter(rcd -> rcd.get("success").equals("true"));
			Stream<CSVRecord> oToleratingSamples = oPassedSamples.filter(rcd -> ((Long.parseLong(rcd.get("elapsed")) > lApdexTgtTholdMS)
					&& ((Long.parseLong(rcd.get("elapsed")) < lApdexTolTholdMS))));
			long lToleratingCount = oToleratingSamples.count();

			// Now compute the Apdex value
			BigDecimal bdApdexScore = new BigDecimal((lSatisfiedCount + (lToleratingCount / 2.0)) / iTotRcd);
			// Round to 2 decimal places as per Apdex specs
			BigDecimal bdApdexScoreRnd = bdApdexScore.setScale(2, RoundingMode.HALF_UP);
			// Set rating as per Apdex specs
			String sApdexRating = setApdexRating(bdApdexScoreRnd);

			// ErrPct formatting: round to 4 decimal places
			Stream<CSVRecord> oFailedSamples = aRcd.stream().filter(rcd -> rcd.get("success").equals("false"));
			long lFailedCount = oFailedSamples.count();
			BigDecimal bdErrPct = new BigDecimal((double) lFailedCount / iTotRcd);
			BigDecimal bdErrPctRnd = bdErrPct.setScale(4, RoundingMode.HALF_UP);

			// Finally update the statistics table
			String sIsSmallGroup = "false";
			if (iTotRcd < 100) {
				sIsSmallGroup = "true";
			}
			String sIsFailed = "false";
			if (bdApdexScoreRnd.compareTo(bdApdexAQL) == -1) {
				sIsFailed = "true";
				iFailedLblCnt++;
			}
			Object[] oArrayRowData = {
					sLbl, // Label
					iTotRcd, // # Samples
//					Long.valueOf((long) mathMoments.getMean()),	// Average
					bdErrPctRnd.doubleValue(), // # Error %
					bdApdexScoreRnd.doubleValue(), // Apdex Value
					dApdexTgtTholdSec, // Apdex Target
					sApdexRating, // Apdex Rating
					sIsSmallGroup, // shows a tick if number of samples < 100
					sIsFailed }; // shows a tick if value less than the specified threshold

			pwrTblMdlStats.addRow(oArrayRowData);
		}
		return iFailedLblCnt;
	}
	
	public static String saveTableStatsAsCsv(String sFilePath) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + SUFFIX_STATS + "csv";
		FileServices.saveTableAsCsv(sOutputFile, pwrTblMdlStats);
		return sOutputFile;
	}

	public static String saveTableStatsAsHtml(String sFilePath, String sApdexAQL) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + SUFFIX_STATS + "html";
		String sTableTitle = HTML_STATS_TITLE + " (Apdex Acceptable Quality Level = " + sApdexAQL + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, PASSFAIL_TEST_COLNBR);
		return sOutputFile;
	}

	/*
	 * Private methods
	 */
	private static double formatTgtTHold(double dTgtThold) {
		// Format the threshold as per Apdex specs
		// For values greater than 10s, define the value to one second
		if (dTgtThold >= 10.0) {
			dTgtThold = Math.rint(dTgtThold);
		}
		// For values greater than 100s, define the value to 10 seconds
		if (dTgtThold >= 100.0) {
			dTgtThold = 10 * (Math.rint(dTgtThold / 10));
		}
		// For values greater than 1000s, follow the same two significant digits
		// restriction
		if (dTgtThold >= 1000.0) {
			dTgtThold = 100 * (Math.rint(dTgtThold / 100));
		}
		return dTgtThold;
	}

	private static String setApdexRating(BigDecimal bdScore) {
		// Sets the rating as per Apdex specs
		String sRating = "Unacceptable"; // grey
		if (bdScore.doubleValue() >= 0.94) {
			sRating = "Excellent"; // blue
		} else if (bdScore.doubleValue() >= 0.85) {
			sRating = "Good"; // green
		} else if (bdScore.doubleValue() >= 0.70) {
			sRating = "Fair"; // yellow
		} else if (bdScore.doubleValue() >= 0.50)
		 {
			sRating = "Poor"; // red
		}
		return sRating;
	}

}
