/**
 * 
 */
package com.github.rbourga.jmeter.apdex.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.util.JMeterUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.common.MathMoments;

public final class ApdexLogic {

	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					JMeterUtils.getResString("aggregate_report_count"), // # Samples
					JMeterUtils.getResString("average"), // Average
					JMeterUtils.getResString("aggregate_report_error%"), // # Error %
					"Apdex Value",
					"Apdex Target (s)", // Target threshold
					"Apdex Rating",
					"Small Group", // shows a tick if number of samples < 100
					"Failed" // shows a tick if value less than the specified threshold
			}, new Class[] {
					String.class,	// Label
					Integer.class,	// # Samples
					Double.class,	// Average
					Double.class,	// # Error %
					Double.class,	// Apdex Value
					Double.class,	// Apdex Target
					String.class,	// Apdex Rating
					Boolean.class,	// Small Group
					Boolean.class	// Failed
					});
	private static int PASSFAIL_TEST_COLNBR = 8;	// Position of Failed column in the table


	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	public static int computeApdexScore(String sFilepath, double dApdexTgtTholdSec, double dApdexAQL) {
		int iTotRcd;
		long lSatisfiedCount, lToleratingCount;
		BigDecimal bdApdexScore, bdApdexScoreRnd, bdErrPct , bdErrPctRnd;
		BigDecimal bdApdexAQL = new BigDecimal(dApdexAQL);
		Boolean bIsSmallGroup, bIsFailed;
		String sApdexRating;
		List<CSVRecord> aRcd;
		MathMoments mathMoments;
		Stream<CSVRecord> oPassedSamples, oSatisfiedSamples, oToleratingSamples;

		// Load the data after getting the delimiter separator from current JMeter properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMap = FileServices.loadSamplesIntoHashMap(sFilepath, cDelim);
		if (rcdHashMap.isEmpty()) {
			return -1; // Nothing loaded, so abort...
		}

		// Clear any statistics from a previous analysis
		pwrTblMdlStats.clearData();

		// Format the threshold as per Apdex specs
		dApdexTgtTholdSec = ApdexLogic.formatTgtTHold(dApdexTgtTholdSec);

		// Now process the data points
		long lApdexTgtTholdMS = (long) (dApdexTgtTholdSec * 1000); // Convert to ms as JMeter times are stored in ms
		long lApdexTolTholdMS = 4 * lApdexTgtTholdMS; // Tolerate = 4xTarget, as per Apdex specs
		int iFailedLblCnt = 0;
		// Loop through the Labels in the dataset
		for (String sLbl : rcdHashMap.keySet()) {
			aRcd = rcdHashMap.get(sLbl);
			iTotRcd = aRcd.size();

			// Get some stats for this set of samples
			mathMoments = MathMoments.crteMomentsFromRecordsList(aRcd);
			/*
			 * As per Apdex specs, all server failures must be counted as frustrated
			 * regardless of their time. So we must calculate Apdex only on the successful
			 * samples.
			 */
			oPassedSamples = aRcd.stream().filter(rcd -> rcd.get("success").equals("true"));
			// Satisfied list of samples
			oSatisfiedSamples = oPassedSamples.filter(rcd -> Long.parseLong(rcd.get("elapsed")) <= lApdexTgtTholdMS); // 0 to T
			lSatisfiedCount  = oSatisfiedSamples.count();
			// Tolerating list of samples: reset the successful stream as a stream can only be used once
			oPassedSamples = aRcd.stream().filter(rcd -> rcd.get("success").equals("true"));			
			oToleratingSamples = oPassedSamples.filter(rcd -> ((Long.parseLong(rcd.get("elapsed")) > lApdexTgtTholdMS) && ((Long.parseLong(rcd.get("elapsed")) < lApdexTolTholdMS)))); // T to F
			lToleratingCount = oToleratingSamples.count();

			// Now compute the Apdex value
			bdApdexScore = new BigDecimal((double) (lSatisfiedCount + (lToleratingCount / 2.0)) / iTotRcd);
			// Round to 2 decimal places as per Apdex specs
			bdApdexScoreRnd = bdApdexScore.setScale(2, RoundingMode.HALF_UP);
			// Set rating as per Apdex specs
			sApdexRating = setApdexRating(bdApdexScoreRnd);

			// ErrPct formatting: round to 4 decimal places
			bdErrPct = new BigDecimal(mathMoments.getErrorPercentage());
			bdErrPctRnd = bdErrPct.setScale(4, RoundingMode.HALF_UP);

			// Finally update the statistics table
			bIsSmallGroup = false;
			if (iTotRcd < 100) {
				bIsSmallGroup = true;
			}
			bIsFailed = false;
			if (bdApdexScoreRnd.compareTo(bdApdexAQL) == -1) {
				bIsFailed = true;
				iFailedLblCnt++;
			}
			Object[] oArrayRowData = {
					sLbl,		// Label
					iTotRcd,	// # Samples
					Long.valueOf((long) mathMoments.getMean()),	// Average
					bdErrPctRnd.doubleValue(),	//# Error %
                    bdApdexScoreRnd.doubleValue(),	//Apdex Value
                    dApdexTgtTholdSec,	// Apdex Target
                    sApdexRating,    // Apdex Rating
                    bIsSmallGroup,    // shows a tick if number of samples < 100
					bIsFailed };	// shows a tick if value less than the specified threshold

			pwrTblMdlStats.addRow(oArrayRowData);
		}
		return iFailedLblCnt;
	}

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
		if (bdScore.doubleValue() >= 0.94)
			sRating = "Excellent"; // blue
		else if (bdScore.doubleValue() >= 0.85)
			sRating = "Good"; // green
		else if (bdScore.doubleValue() >= 0.70)
			sRating = "Fair"; // yellow
		else if (bdScore.doubleValue() >= 0.50)
			sRating = "Poor"; // red
		return sRating;
	}

	public static boolean isApdexMinScoreOutOfRange(double dValue) {
		return dValue < 0 || dValue > 1;
	}

	public static boolean isCoVPctOutOfRange(double fCoVALPct) {
		return fCoVALPct < 0;
	}

	public static boolean isTgtTHoldOutOfRange(double dValue) {
		return dValue < 0.1;
	}

	public static String saveTableStatsAsCsv(String sFilePath) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ApdexScores.csv";
		FileServices.saveTableAsCsv(sOutputFile, pwrTblMdlStats);
		return sOutputFile;
	}

	public static String saveTableStatsAsHtml(String sFilePath, String sApdexAQL) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ApdexScores.html";
		String sTableTitle = "Apdex Score Results (Apdex Acceptable Quality Level = " + sApdexAQL + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, PASSFAIL_TEST_COLNBR);
		return sOutputFile;
	}
}
