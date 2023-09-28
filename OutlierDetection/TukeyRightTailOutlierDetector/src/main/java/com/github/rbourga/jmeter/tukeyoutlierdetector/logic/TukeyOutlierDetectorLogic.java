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
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.util.JMeterUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.common.MathMoments;

public final class TukeyOutlierDetectorLogic {

	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					JMeterUtils.getResString("aggregate_report_count"), // # Samples
					JMeterUtils.getResString("average"), // Average
					"Upper Fence",
					"# Removed", // number of samples that have been discarded
					"Removed %", // percentage of samples that have been discarded
					"Small Group", // shows a tick if remaining number of samples < 100
					"Failed" // shows a tick if value greater than the specified threshold
			}, new Class[] {
					String.class, // Label
					Integer.class, // # Samples
					Double.class, // Average
					Integer.class, // Upper Fence
					Integer.class, // # Removed
					Double.class, // # Removed %
					Boolean.class, // Small Group
					Boolean.class // Failed
			});
	private static int PASSFAIL_TEST_COLNBR = 7;	// Position of Failed column in the table

	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	public static int RemoveUpper(String sFilepath, double fTukeyK, double fMaxRemPct) {
		/*
		 * Will remove only upper outliers (which are bigger than the upper boundary).
		 */
		boolean bIsUpperRemoved, bIsSmallGroup, bIsFailed;
		int iInitLblCnt, iUpprOutlierCnt;
		double fUpFence, fUpFenceMin;
		String sKrounded, sOutputFile;
		List<CSVRecord> aLblRcd, mergedOutliers = null, mergedWithoutOutliers = null;
		MathMoments mathMoments;

		double fK = fTukeyK;
		DecimalFormat df2Decimals = new DecimalFormat("0.00");
		BigDecimal bdMaxRemPct = new BigDecimal(fMaxRemPct);

		// Load the data after getting the delimiter separator from current JMeter properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMap = FileServices.loadSamplesIntoHashMap(sFilepath, cDelim);
		if (rcdHashMap.isEmpty()) {
			return -1; // Nothing loaded, so abort...
		}

		// Clear any statistics from a previous operation
		pwrTblMdlStats.clearData();

		// 2. Now, process the data points...
		int iFailedLblCnt = 0;
		// Loop through the Labels in the dataset
		for (String sLbl : rcdHashMap.keySet()) {
			aLblRcd = rcdHashMap.get(sLbl);
			iInitLblCnt = aLblRcd.size();
			fUpFence = 0.0;
			fUpFenceMin = Double.MAX_VALUE;

			// Get some stats for this set of samples
			mathMoments = MathMoments.crteMomentsFromRecordsList(aLblRcd);

			// Only look for outliers if there are at least four items to compare
			if (iInitLblCnt > 3) {
				// Initialize k
				if (fTukeyK == 0) {
					// Use Carling's formulae and round to 2 decimal places
					fK = ((17.63 * iInitLblCnt) - 23.64) / ((7.74 * iInitLblCnt) - 3.71);
					sKrounded = df2Decimals.format(fK);
					// Convert value back to double
					fK = Double.parseDouble(sKrounded);
				}
				/*
				 * An outlier can hide another outlier...so when removing extreme values, we
				 * have to iterate until no extreme values are left.
				 */
				do {
					// Get the upper fence
					fUpFence = getUpprFence(aLblRcd, fK);
					// Save the most severe limit for the report
					fUpFenceMin = Math.min(fUpFence, fUpFenceMin);
					// Now remove all samples that are higher than the upper fence using Java's
					// Stream API
					final double fUpFenceFinal = fUpFence;
					List<CSVRecord> upprOutliers = aLblRcd.stream()
							.filter(rcd -> Double.parseDouble(rcd.get("elapsed")) > fUpFenceFinal)
							.collect(Collectors.toList());
					if (upprOutliers.size() == 0) {
						bIsUpperRemoved = false;
					} else {
						// Outliers detected: save them in a separate list and repeat
						mergedOutliers = addCSVList2To1(mergedOutliers, upprOutliers);
						// Remove these outliers from the current list
						aLblRcd = aLblRcd.stream()
								.filter(rcd -> Double.parseDouble(rcd.get("elapsed")) <= fUpFenceFinal)
								.collect(Collectors.toList());
						bIsUpperRemoved = true;
					}
				} while (bIsUpperRemoved);

				// Outliers removed, save the new cleansed list
				mergedWithoutOutliers = addCSVList2To1(mergedWithoutOutliers, aLblRcd);
			} else {
				// No outliers removed because not enough samples: just save those samples
				mergedWithoutOutliers = addCSVList2To1(mergedWithoutOutliers, aLblRcd);
			}

			// Save the results in the table
			bIsSmallGroup = false;
			if (aLblRcd.size() < 100) {
				bIsSmallGroup = true;
			}
			iUpprOutlierCnt = iInitLblCnt - aLblRcd.size();
			BigDecimal bdUpprOutlierPct = new BigDecimal((double) iUpprOutlierCnt / (double) iInitLblCnt);
			// Round % to 4 decimal places
			BigDecimal bdUpprOutlierPctRnd = bdUpprOutlierPct.setScale(4, RoundingMode.HALF_UP);
			bIsFailed = false;
			if (bdUpprOutlierPctRnd.compareTo(bdMaxRemPct) != -1) {
				bIsFailed = true;
				iFailedLblCnt++;
			}
			// Update the statistics table
			Object[] oArrayRowData = {
					sLbl, // Label
					iInitLblCnt, // # Samples
					Long.valueOf((long) mathMoments.getMean()), // Average
					fUpFenceMin, // Upper Fence
					iUpprOutlierCnt, // # Removed
					bdUpprOutlierPctRnd.doubleValue(), // Removed %
					bIsSmallGroup, // Small Group
					bIsFailed };
			pwrTblMdlStats.addRow(oArrayRowData);
		}

		String sFileDirectoryName = FilenameUtils.getFullPath(sFilepath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilepath);
		String sFileExtension = FilenameUtils.getExtension(sFilepath);
		// Save cleansed results in a file for post statistics
		sOutputFile = sFileDirectoryName + sFileBaseName + "_WithoutUpperOutliers." + sFileExtension;
		FileServices.saveCSVRecsToFile(sOutputFile, mergedWithoutOutliers, cDelim);
		// Save the outliers in a separate file for post analysis
		if (mergedOutliers != null) {
			sOutputFile = sFileDirectoryName + sFileBaseName + "_UpperOutliers." + sFileExtension;
			FileServices.saveCSVRecsToFile(sOutputFile, mergedOutliers, cDelim);
		}

		return iFailedLblCnt;
	}

	private static double getUpprFence(List<CSVRecord> aRcd, double fK) {
		// Uses StatUtils to get the percentiles which will give us the UpperFence.

		// Extract the elapsed values into a list
		List<Double> listElapsed = new ArrayList<Double>();
		for (CSVRecord rcd : aRcd) {
			double dElapsed = Double.parseDouble(rcd.get("elapsed"));
			listElapsed.add(dElapsed);
		}
		// Convert the list of elapsed to a double array
		double[] aElapsed = listElapsed.stream().mapToDouble(Double::doubleValue).toArray();

		// Get Q1 and Q3
		double dQ1 = StatUtils.percentile(aElapsed, 25); // first quartile
		double dQ3 = StatUtils.percentile(aElapsed, 75); // third quartile

		// Return the upper fence value
		double fInterQuartileRange = dQ3 - dQ1;
		double fUpperFence = dQ3 + (fK * fInterQuartileRange);
		return fUpperFence;
	}

	private static List<CSVRecord> addCSVList2To1(List<CSVRecord> listCSV1, List<CSVRecord> listCSV2) {
		if (listCSV1 == null) {
			listCSV1 = new ArrayList<>(listCSV2);
		} else {
			listCSV1.addAll(listCSV2);
		}
		return listCSV1;
	}

	public static boolean isTrimPctOutOfRange(double fPct) {
		return fPct < 0;
	}

	public static String saveTableStatsAsCsv(String sFilePath) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_UpprOutlierRemStats.csv";
		FileServices.saveTableAsCsv(sOutputFile, pwrTblMdlStats);
		return sOutputFile;
	}

	public static String saveTableStatsAsHtml(String sFilePath, String sRemALPct) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_UpTrimSum.html";
		String sTableTitle = "Upper Outliers Removal Summary (Removal Acceptable Limit = " + sRemALPct + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, PASSFAIL_TEST_COLNBR);
		return sOutputFile;
	}
}
