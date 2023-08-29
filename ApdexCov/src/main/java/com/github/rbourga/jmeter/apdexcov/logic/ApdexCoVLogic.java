/**
 * 
 */
package com.github.rbourga.jmeter.apdexcov.logic;

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

public final class ApdexCoVLogic {

	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					JMeterUtils.getResString("aggregate_report_count"), // # Samples
					JMeterUtils.getResString("average"), // Average
					"CoV %", // Coefficent of Variation
					"CoV Rating",
					JMeterUtils.getResString("aggregate_report_error%"), // # Error %
					"Apdex Value",
					"Apdex Target", // Target threshold
					"Apdex Rating",
					"Small Group", // shows a tick if number of samples < 100
					"Failed" // shows a tick if value less than the specified threshold
			}, new Class[] {
					String.class,	// Label
					Integer.class,	// # Samples
					Double.class,	// Average
					Double.class,	// Coefficient of Variation %
					String.class,	// Coefficient of Variation Rating
					Double.class,	// # Error %
					Double.class,	// Apdex Value
					Double.class,	// Apdex Target
					String.class,	// Apdex Rating
					Boolean.class,	// Small Group
					Boolean.class	// Failed
					});

	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	public static int computeApdexCoV(String sFilepath, double dApdexTgtTholdSec, double dApdexAQL, double dCoVALPct) {
		// Load the data after getting the delimiter separator from current JMeter
		// properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMap = FileServices.loadSamplesIntoHashMap(sFilepath, cDelim);
		if (rcdHashMap.isEmpty()) {
			return -1; // Nothing loaded, so abort...
		}

		// Format the threshold as per Apdex specs
		dApdexTgtTholdSec = ApdexCoVLogic.formatTgtTHold(dApdexTgtTholdSec);

		// Now process the data points
		long lApdexTgtTholdMS = (long) (dApdexTgtTholdSec * 1000); // Convert to ms as JMeter times are stored in ms
		long lApdexTolTholdMS = 4 * lApdexTgtTholdMS; // Tolerate = 4xTarget, as per Apdex specs
		int iTotNbrOfFailedRcd = 0;
		// Loop through the Labels in the dataset
		for (String sLbl : rcdHashMap.keySet()) {
			List<CSVRecord> aRcd = rcdHashMap.get(sLbl);
			int iTotRcd = aRcd.size();

			// Get some stats for this set of samples
			MathMoments mathMoments = MathMoments.crteMomentsFromRecordsList(aRcd);
			/*
			 * As per Apdex specs, all server failures must be counted as frustrated
			 * regardless of their time. So we must calculate Apdex only on the successful
			 * samples.
			 */
			Stream<CSVRecord> oPassedSamples = aRcd.stream().filter(rcd -> rcd.get("success").equals("true"));

			// Satisfied list of samples
			Stream<CSVRecord> oSatisfiedSamples = oPassedSamples.filter(rcd -> Long.parseLong(rcd.get("elapsed")) <= lApdexTgtTholdMS); // 0 to T
			long lSatisfiedCount  = oSatisfiedSamples.count();

			// Tolerating list of samples: reset the successful stream as a stream can only be used once
			oPassedSamples = aRcd.stream().filter(rcd -> rcd.get("success").equals("true"));			
			Stream<CSVRecord> oToleratingSamples = oPassedSamples.filter(rcd -> ((Long.parseLong(rcd.get("elapsed")) > lApdexTgtTholdMS) && ((Long.parseLong(rcd.get("elapsed")) < lApdexTolTholdMS)))); // T to F
			long lToleratingCount = oToleratingSamples.count();

			// Now compute the Apdex value
			BigDecimal bdApdexScore = new BigDecimal(0.00);
			bdApdexScore = new BigDecimal((double) (lSatisfiedCount + (lToleratingCount / 2.0)) / iTotRcd);
			// Round to 2 decimal places as per Apdex specs
			BigDecimal bdApdexScoreRnd = new BigDecimal(0.00);
			bdApdexScoreRnd = bdApdexScore.setScale(2, RoundingMode.HALF_UP);
			// Set rating as per Apdex specs
			String sApdexRating = setApdexRating(bdApdexScoreRnd);

			// Coeff Var processing
			BigDecimal dCoVScore = new BigDecimal(0.00);
			dCoVScore = new BigDecimal(mathMoments.getCoV());
			// Similar to error rate, round to 4 decimal places
			BigDecimal bdCoVScoreRnd = new BigDecimal(0.00);
			bdCoVScoreRnd = dCoVScore.setScale(4, RoundingMode.HALF_UP);
			String sCoVRating = setCoVRating(bdCoVScoreRnd);

			// Finally update the statistics table
			Boolean bSmallGroup = false;
			if (iTotRcd < 100) {
				bSmallGroup = true;
			}
			Boolean bFailed = false;
			BigDecimal bdApdexAQL = new BigDecimal(dApdexAQL);
			BigDecimal bdCoVALPct = new BigDecimal(dCoVALPct);
			if ((bdApdexScoreRnd.compareTo(bdApdexAQL) == -1) ||
				(bdCoVScoreRnd.compareTo(bdCoVALPct) != -1)) {
				bFailed = true;
				iTotNbrOfFailedRcd++;
			}

			Object[] oArrayRowData = {
					sLbl,		// Label
					iTotRcd,	// # Samples
					Long.valueOf((long) mathMoments.getMean()),	// Average
					bdCoVScoreRnd.doubleValue(),	//Cof of Var %
					sCoVRating,	// Cof of Var Rating
                    Double.valueOf(mathMoments.getErrorPercentage()),	//# Error %
                    bdApdexScoreRnd.doubleValue(),	//Apdex Value
                    dApdexTgtTholdSec,	// Apdex Target
                    sApdexRating,    // Apdex Rating
                    bSmallGroup,    // shows a tick if number of samples < 100
					bFailed };	// shows a tick if value less than the specified threshold

			pwrTblMdlStats.addRow(oArrayRowData);
		}
		return iTotNbrOfFailedRcd;
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

	private static String setCoVRating(BigDecimal bdScore) {
		String sRating = "Low";
		if (bdScore.doubleValue() >= 0.30)
			sRating = "High";	// high if > 30%
		else if (bdScore.doubleValue() >= 0.10)
			sRating = "Moderate";	// moderate if > 10%
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

	public static String saveApdexStatsAsCsv(String sFilePath) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ApdexCoVScores.csv";
		FileServices.saveTableAsCsv(sOutputFile, pwrTblMdlStats);
		return sOutputFile;
	}

	public static String saveApdexStatsAsHtml(String sFilePath, String sApdexAQL, String sCoVALPct) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ApdexCoVScores.html";
		String sTableTitle = "Apdex & Coefficient of Variation Score Results (Apdex Acceptable Quality Level = " + sApdexAQL + ", CoV Acceptable Limit = " + sCoVALPct + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, 10);
		return sOutputFile;
	}
}
