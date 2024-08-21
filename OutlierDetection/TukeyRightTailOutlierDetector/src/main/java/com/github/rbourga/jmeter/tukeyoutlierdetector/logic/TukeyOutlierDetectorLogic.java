/**
 *
 */
package com.github.rbourga.jmeter.tukeyoutlierdetector.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.util.JMeterUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.common.MathMoments;

public final class TukeyOutlierDetectorLogic {

	private static DecimalFormat df2Decimals = new DecimalFormat("0.00");

	private static String HTML_STATS_TITLE = "Summary of Upper Outliers Removal";
	private static String SUFFIX_OUTLIERS_ONLY = "_True_UpperOutliers.";
	private static String SUFFIX_WITHOUT_OUTLIERS = "_True_WOupperOutliers.";
	private static String SUFFIX_STATS = "_UpperTrimStats.";

	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					JMeterUtils.getResString("aggregate_report_count"), // # Samples
					JMeterUtils.getResString("average"), // Average
					"Upper Fence", "# Removed", // number of samples that have been discarded
					"Removed %", // percentage of samples that have been discarded
					"Small Group", // true if remaining number of samples < 100
					"Failed" // true if value greater than the specified threshold
			}, new Class[] {
					String.class, // Label
					Integer.class, // # Samples
					Double.class, // Average
					Double.class, // Upper Fence
					Integer.class, // # Removed
					Double.class, // # Removed %
					String.class, // Small Group
					String.class // Failed
			});
	private static int PASSFAIL_TEST_COLNBR = 7; // Position of Failed column in the table

	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	/*
	 * Public methods
	 */
	public static boolean isTrimPctOutOfRange(double fPct) {
		return fPct < 0;
	}

	public static int RemoveUpper(String sFilepath, double fTukeyK, double fMaxRemPct) {
		/*
		 * Will remove only upper outliers (which are bigger than the upper boundary).
		 */
		int iInitPassedLblCnt;
		double fUpFenceMin;
		List<CSVRecord> mergedOutliers = null, mergedWithoutOutliers = null, aLblRcdPassed = null;

		double fK = fTukeyK;
		BigDecimal bdMaxRemPct = new BigDecimal(fMaxRemPct);

		// Load the data after getting the delimiter separator from current JMeter
		// properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMap = FileServices.loadSamplesIntoHashMap(sFilepath, cDelim);
		if (rcdHashMap.isEmpty()) {
			return -1; // Nothing loaded, so abort...
		}

		// Clear any statistics from a previous operation
		pwrTblMdlStats.clearData();

		// Now, process the data points in natural order...
		int iFailedLblCnt = 0;
		TreeMap<String, List<CSVRecord>> tmSortedRcd = new TreeMap<>(rcdHashMap);
		// Loop through the Labels in the dataset
		for (String sLbl : tmSortedRcd.keySet()) {
			List<CSVRecord> aLblRcd = tmSortedRcd.get(sLbl);
			double fUpFence = 0.0;
			fUpFenceMin = Double.MAX_VALUE;

			// We focus on successful samplers as they are the main interest
			aLblRcdPassed = aLblRcd.stream().filter(rcd -> rcd.get("success").equals("true"))
					.collect(Collectors.toList());
			iInitPassedLblCnt = aLblRcdPassed.size();

			// Get initial average value
			double dAvg = MathMoments.crteMomentsFromRecordsList(aLblRcdPassed).getMean();

			// Only look for outliers if there are at least four items to compare
			if (iInitPassedLblCnt > 3) {
				// Initialize k
				if (fTukeyK == 0) {
					// Use Carling's formulae and round to 2 decimal places
					fK = ((17.63 * iInitPassedLblCnt) - 23.64) / ((7.74 * iInitPassedLblCnt) - 3.71);
					String sKrounded = df2Decimals.format(fK);
					// Convert value back to double
					fK = Double.parseDouble(sKrounded);
				}
				/*
				 * An outlier can hide another outlier...so when removing extreme values, we
				 * have to iterate until no extreme values are left.
				 */
				boolean bIsUpperRemoved;
				do {
					// Update the stats on the series and get the new upper fence
					MathMoments mathMoments = MathMoments.crteMomentsFromRecordsList(aLblRcdPassed);
					fUpFence = getUpprFence(mathMoments, fK);
					// Save the most severe limit for the report
					fUpFenceMin = Math.min(fUpFence, fUpFenceMin);
					// Now remove all samples that are higher than the upper fence using Java's
					// Stream API
					final double fUpFenceFinal = fUpFence;
					List<CSVRecord> upprOutliers = aLblRcdPassed.stream()
							.filter(rcd -> Double.parseDouble(rcd.get("elapsed")) > fUpFenceFinal)
							.collect(Collectors.toList());
					if (upprOutliers.size() == 0) {
						bIsUpperRemoved = false;
					} else {
						// Outliers detected: save them in a separate list and repeat
						mergedOutliers = addCSVList2To1(mergedOutliers, upprOutliers);
						// Remove these outliers from the current list
						aLblRcdPassed = aLblRcdPassed.stream()
								.filter(rcd -> Double.parseDouble(rcd.get("elapsed")) <= fUpFenceFinal)
								.collect(Collectors.toList());
						bIsUpperRemoved = true;
					}
				} while (bIsUpperRemoved);

				// Outliers removed, save the new cleansed list
				mergedWithoutOutliers = addCSVList2To1(mergedWithoutOutliers, aLblRcdPassed);
			} else {
				// No outliers removed because not enough samples: just save those samples
				mergedWithoutOutliers = addCSVList2To1(mergedWithoutOutliers, aLblRcdPassed);
			}

			// Save the results in the table
			String sIsSmallGroup = "false";
			if (aLblRcdPassed.size() < 100) {
				sIsSmallGroup = "true";
			}
			int iUpprOutlierCnt = iInitPassedLblCnt - aLblRcdPassed.size();
			BigDecimal bdUpprOutlierPct = (iInitPassedLblCnt == 0) ? new BigDecimal(0)
					: new BigDecimal((double) iUpprOutlierCnt / (double) iInitPassedLblCnt);
			// Round % to 4 decimal places
			BigDecimal bdUpprOutlierPctRnd = bdUpprOutlierPct.setScale(4, RoundingMode.HALF_UP);
			String sIsFailed = "false";
			if (bdUpprOutlierPctRnd.compareTo(bdMaxRemPct) != -1) {
				sIsFailed = "true";
				iFailedLblCnt++;
			}
			// Update the statistics table
			Object[] oArrayRowData = {
					sLbl, // Label
					iInitPassedLblCnt, // # Samples
					Long.valueOf((long) dAvg), // Average
					fUpFenceMin, // Upper Fence
					iUpprOutlierCnt, // # Removed
					bdUpprOutlierPctRnd.doubleValue(), // Removed %
					sIsSmallGroup, // Small Group
					sIsFailed };
			pwrTblMdlStats.addRow(oArrayRowData);
		}

		String sFileDirectoryName = FilenameUtils.getFullPath(sFilepath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilepath);
		String sFileExtension = FilenameUtils.getExtension(sFilepath);
		// Save cleansed results in a file for post statistics
		String sOutputFile = sFileDirectoryName + sFileBaseName + SUFFIX_WITHOUT_OUTLIERS + sFileExtension;
		FileServices.saveCSVRecsToFile(sOutputFile, mergedWithoutOutliers, cDelim);
		// Save the outliers in a separate file for post analysis
		if (mergedOutliers != null) {
			sOutputFile = sFileDirectoryName + sFileBaseName + SUFFIX_OUTLIERS_ONLY + sFileExtension;
			FileServices.saveCSVRecsToFile(sOutputFile, mergedOutliers, cDelim);
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

	public static String saveTableStatsAsHtml(String sFilePath, String sRemALPct) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + SUFFIX_STATS + "html";
		String sTableTitle = HTML_STATS_TITLE + " (Removal Acceptable Limit = " + sRemALPct + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, PASSFAIL_TEST_COLNBR);
		return sOutputFile;
	}

	/*
	 * Private methods
	 */
	private static List<CSVRecord> addCSVList2To1(List<CSVRecord> listCSV1, List<CSVRecord> listCSV2) {
		if (listCSV1 == null) {
			listCSV1 = new ArrayList<>(listCSV2);
		} else {
			listCSV1.addAll(listCSV2);
		}
		return listCSV1;
	}

	private static double getUpprFence(MathMoments mathMoments, double fK) {
		// Return the upper fence value to 2 decimal places
		double fInterQuartileRange = mathMoments.getQ3() - mathMoments.getQ1();
		double fUpperFence = mathMoments.getQ3() + (fK * fInterQuartileRange);
		String sUpperFencerounded = df2Decimals.format(fUpperFence);
		// Convert value back to double
		fUpperFence = Double.parseDouble(sUpperFencerounded);
		return fUpperFence;
	}

}
