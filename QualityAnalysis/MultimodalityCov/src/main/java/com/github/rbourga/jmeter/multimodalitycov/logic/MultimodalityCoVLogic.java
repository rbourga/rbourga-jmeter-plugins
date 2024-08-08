/**
 * 
 */
package com.github.rbourga.jmeter.multimodalitycov.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.util.JMeterUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.common.MathMoments;
import com.github.rbourga.jmeter.multimodalitycov.maths.MValueCalculator;

public final class MultimodalityCoVLogic {

	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					JMeterUtils.getResString("aggregate_report_count"), // # Samples
					JMeterUtils.getResString("average"), // Average
					JMeterUtils.getResString("aggregate_report_min"), // Min
					JMeterUtils.getResString("aggregate_report_max"), // Max
					"CoV %", // Coefficent of Variation
					"CoV Rating",
					"mValue",
					"Multimodal", // true if multimodal, false otherwise
					"Failed" // shows a tick if excessive CoV or multimodal
			}, new Class[] {
					String.class,	// Label
					Integer.class,	// # Samples
					Double.class,	// Average
					Double.class,	// Min
					Double.class,	// Max
					Double.class,	// Coefficient of Variation %
					String.class,	// Coefficient of Variation Rating
					Double.class,	// mValue
					Boolean.class,	// Multimodal
					Boolean.class	// Failed
					});
	private static int PASSFAIL_TEST_COLNBR = 9;	// Position of Failed column in the table

	private static PowerTableModel pwrTblMdlRows = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					JMeterUtils.getResString("aggregate_report_min"), // Min
					"Bin Size",
					"Multimodal", // true if multimodal, false otherwise
					"Check" // user to tick for generating the bar chart
			}, new Class[] {
					String.class,	// Label
					Double.class,	// Min
					Double.class,	// Bin size
					Boolean.class,	// Multimodal
					Boolean.class	// Check
					});

	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}
	public static PowerTableModel getPwrTblMdelRows() {
		return pwrTblMdlRows;
	}
	
	// Hashmap that contains the list of bins to draw the bar charts
	private static Map<String, int[]> binsMap = new HashMap<>();
	public static Map<String, int[]> getBinsMap() {
		return binsMap;
	}

	public static int computeMvalueCoV(String sFilepath, double dMvalueThold, double dCoVALPct) {
		int iTotRcd;
		BigDecimal bdMvalue, bdMvalueRnd, bdCoVScore, bdCoVScoreRnd;
		BigDecimal bdMvalueThold = new BigDecimal(dMvalueThold);
		BigDecimal bdCoVALPct = new BigDecimal(dCoVALPct);
		Boolean bIsMultimodal, bIsFailed;
		String sCoVRating;
		List<CSVRecord> aRcd;
		MathMoments mathMoments;
		MValueCalculator mValueCalculator;

		// Load the data after getting the delimiter separator from current JMeter properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMap = FileServices.loadSamplesIntoHashMap(sFilepath, cDelim);
		if (rcdHashMap.isEmpty()) {
			return -1; // Nothing loaded, so abort...
		}

		// Clear any statistics from a previous analysis
		pwrTblMdlStats.clearData();
		pwrTblMdlRows.clearData();
		binsMap.clear();

		// Now process the data points
		int iFailedLblCnt = 0;

		// Loop through the Labels in the dataset
		for (String sLbl : rcdHashMap.keySet()) {
			aRcd = rcdHashMap.get(sLbl);
			iTotRcd = aRcd.size();

			// Get some stats for this set of samples
			mathMoments = MathMoments.crteMomentsFromRecordsList(aRcd);
			
			// Calculate the mValue
			mValueCalculator = MValueCalculator.calculate(aRcd, mathMoments);
			bdMvalue = new BigDecimal(mValueCalculator.getMvalue());
			// Round to 2 decimal places
			bdMvalueRnd = bdMvalue.setScale(1, RoundingMode.HALF_UP);
			bIsMultimodal = false;
			if (bdMvalueRnd.compareTo(bdMvalueThold) != -1) {
				bIsMultimodal = true;
			}
			// Add the bins to the corresponding label
			binsMap.put(sLbl, mValueCalculator.getiBinsArray());

			// Calculate the Coefficient of Variation
			bdCoVScore = new BigDecimal(mathMoments.getCoV());
			// Similar to error rate, round to 4 decimal places
			bdCoVScoreRnd = bdCoVScore.setScale(4, RoundingMode.HALF_UP);
			sCoVRating = setCoVRating(bdCoVScoreRnd);

			// Update the statistics table
			bIsFailed = false;
			if (bIsMultimodal ||
				(bdCoVScoreRnd.compareTo(bdCoVALPct) != -1)) {
				bIsFailed = true;
				iFailedLblCnt++;
			}
			Object[] oArrayRowDataStat = {
					sLbl,		// Label
					iTotRcd,	// # Samples
					Long.valueOf((long) mathMoments.getMean()),	// Average
					Long.valueOf((long) mathMoments.getMin()),	// Min
					Long.valueOf((long) mathMoments.getMax()),	// Max
					bdCoVScoreRnd.doubleValue(),	//Cof of Var %
					sCoVRating,	// Cof of Var Rating
					bdMvalueRnd.doubleValue(),	// mValue
					bIsMultimodal,	// Multimodal
					bIsFailed };	// shows a tick if values more than the specified thresholds
			pwrTblMdlStats.addRow(oArrayRowDataStat);
			
			// Update the rows table
			Object[] oArrayRowDataRow = {
					sLbl,		// Label
					Long.valueOf((long) mathMoments.getMin()),	// Min
					Long.valueOf((long) mValueCalculator.getBinSize()),	// Bin Size
					bIsMultimodal,	// Multimodal
					false };	// nothing selected by default
			pwrTblMdlRows.addRow(oArrayRowDataRow);

		}
		return iFailedLblCnt;
	}

	private static String setCoVRating(BigDecimal bdScore) {
		String sRating = "Low";
		if (bdScore.doubleValue() >= 0.30)
			sRating = "High";	// high if > 30%
		else if (bdScore.doubleValue() >= 0.10)
			sRating = "Moderate";	// moderate if > 10%
		return sRating;
	}

	public static boolean isCoVPctOutOfRange(double fCoVALPct) {
		return fCoVALPct < 0;
	}

	public static boolean isMvaleTHoldOutOfRange(double dValue) {
		return dValue < 0.1;
	}

	public static String saveTableStatsAsCsv(String sFilePath) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ModalityCoV.csv";
		FileServices.saveTableAsCsv(sOutputFile, pwrTblMdlStats);
		return sOutputFile;
	}

	public static String saveTableStatsAsHtml(String sFilePath, String sMvalueThold, String sCoVALPct) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ModalityCoV.html";
		String sTableTitle = "Modality & Coefficient of Variation Test Results (Modality threshold = " + sMvalueThold + ", CoV Acceptable Limit = " + sCoVALPct + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, PASSFAIL_TEST_COLNBR);
		return sOutputFile;
	}
}
