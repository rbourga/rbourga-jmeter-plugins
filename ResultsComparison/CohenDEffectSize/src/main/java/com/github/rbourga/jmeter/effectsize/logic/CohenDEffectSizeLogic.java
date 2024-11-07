/**
 *
 */
package com.github.rbourga.jmeter.effectsize.logic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.util.JMeterUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.common.MathMoments;

public final class CohenDEffectSizeLogic {

	public static final String AVERAGE_OF_AVERAGES = "OVERALL AVERAGE";
	private static String HTML_STATS_TITLE = "Results Comparison";
	private static final String RATING_NOTAPPLICABLE = "Not Applicable";
	private static String SUFFIX_STATS = "_CompareStats.";

	// Variables to store all the moments
	private static ArrayList<Double> alAveragesA = new ArrayList<>(); // Used to store all averages of A
	private static ArrayList<Double> alAveragesB = new ArrayList<>(); // Used to store all averages of B
	private static HashMap<String, CohenDEffectSizeLogic> hmCohendResults = new HashMap<>();

	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					"# Samples A",
					"# Samples B",
					"Average A", // Averages
					"Average B",
					"Cohen's d", // d
					"Diff Rating", // Descriptor
					"Failed" // true if value more than the specified threshold
			}, new Class[] {
					String.class, // Label
					Integer.class, // # Samples A
					Integer.class, // # Samples B
					Double.class, // Average A
					Double.class, // Average B
					Double.class, // Cohen's d
					String.class, // Diff Rating
					String.class // Failed
			});
	private static int PASSFAIL_TEST_COLNBR = 7; // Position of Failed column in the table

	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	/*
	 * Constructor
	 */
	private int iCountA;
	private int iCountB;
	private double fMeanA;
	private double fMeanB;
	private double fVarianceA;
	private double fVarianceB;
	private BigDecimal bdCohenD;
	private String sDiffRating;

	private CohenDEffectSizeLogic(String sLbl) {
		iCountA = 0;
		iCountB = 0;
		fMeanA = 0;
		fMeanB = 0;
		fVarianceA = 0;
		fVarianceB = 0;
		bdCohenD = new BigDecimal(0.00);
		sDiffRating = "";
	}

	private int getCountA() {
		return iCountA;
	}

	private int getCountB() {
		return iCountB;
	}

	private double getMeanA() {
		return fMeanA;
	}

	private double getMeanB() {
		return fMeanB;
	}

	private double getVarianceA() {
		return fVarianceA;
	}

	private double getVarianceB() {
		return fVarianceB;
	}

	private BigDecimal getCohenD() {
		return bdCohenD;
	}

	private String getDiffRating() {
		return sDiffRating;
	}

	private void setCountB(int iCount) {
		this.iCountB = iCount;
	}

	private void setMeanB(double dMean) {
		this.fMeanB = dMean;
	}

	private void setVarianceB(double dVariance) {
		this.fVarianceB = dVariance;
	}

	private void setCohenD(double dValue) {
		// BigDecimal bdCohenDNotRounded = new BigDecimal((double) Math.abs(fCohenD));
		BigDecimal bdCohenDNotRnded = new BigDecimal(dValue);
		// Round to 2 decimal places as per specs
		this.bdCohenD = bdCohenDNotRnded.setScale(2, RoundingMode.HALF_UP);
	}

	/*
	 * Public methods
	 */
	public static boolean isCohendALOutOfRange(double fCohendAL) {
		return fCohendAL < 0;
	}

	public static int calcCohenDEffectSize(String sFilepathA, String sFilepathB, double dCohendAL) {
		// Load the data after getting the delimiter separator from current JMeter
		// properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMapA = FileServices.loadSamplesIntoHashMap(sFilepathA, cDelim);
		if (rcdHashMapA.isEmpty()) {
			return -1; // Nothing in Control, so abort...
		}
		Map<String, List<CSVRecord>> rcdHashMapB = FileServices.loadSamplesIntoHashMap(sFilepathB, cDelim);
		if (rcdHashMapB.isEmpty()) {
			return -2; // Nothing in Control, so abort...
		}

		// Clear any comparisons from a previous analysis
		alAveragesA.clear();
		alAveragesB.clear();
		hmCohendResults.clear();
		pwrTblMdlStats.clearData();

		/*
		 * Processing of the data is done in multiple steps: 1. Calculate the moments
		 * for the Control samplers A. 2. Calculate the moments for the Variation
		 * samplers B. 3. Calculate Cohen's d between A and B
		 */
		// 1. Loop through the Labels in the dataset A
		for (String sLbl : rcdHashMapA.keySet()) {
			List<CSVRecord> aRcd = rcdHashMapA.get(sLbl);
			int iTotRcd = aRcd.size();

			// Get some stats for this set of samples
			MathMoments mathMoments = MathMoments.crteMomentsFromRecordsList(aRcd);
			// Save some values for later analysis
			CohenDEffectSizeLogic oCohenDEffectSizeLogic = new CohenDEffectSizeLogic(sLbl);
			oCohenDEffectSizeLogic.iCountA = iTotRcd;
			oCohenDEffectSizeLogic.fMeanA = mathMoments.getMean();
			oCohenDEffectSizeLogic.fVarianceA = mathMoments.getVariance();

			// Save this average for later processing of all the averages
			alAveragesA.add(mathMoments.getMean());
			// Add the results of analysis to hashmap for later reference
			hmCohendResults.put(sLbl, oCohenDEffectSizeLogic);
		}

		// 2. Repeat for dataset B
		for (String sLbl : rcdHashMapB.keySet()) {
			List<CSVRecord> aRcd = rcdHashMapB.get(sLbl);
			int iTotRcd = aRcd.size();

			// Get some stats for this set of samples
			MathMoments mathMoments = MathMoments.crteMomentsFromRecordsList(aRcd);
			// Save this mean for later processing
			alAveragesB.add(mathMoments.getMean());

			// Save some values for later analysis
			// Is this Sample in the Control file?
			if (hmCohendResults.containsKey(sLbl)) {
				// Yes: update the Stats with B values
				hmCohendResults.get(sLbl).setCountB(iTotRcd);
				hmCohendResults.get(sLbl).setMeanB(mathMoments.getMean());
				hmCohendResults.get(sLbl).setVarianceB(mathMoments.getVariance());
			} else {
				// No: add a new result to the list
				CohenDEffectSizeLogic oCohenDEffectSizeLogic = new CohenDEffectSizeLogic(sLbl);
				oCohenDEffectSizeLogic.iCountB = iTotRcd;
				oCohenDEffectSizeLogic.fMeanB = mathMoments.getMean();
				oCohenDEffectSizeLogic.fVarianceB = mathMoments.getVariance();
				// Add the results of analysis to hashmap for later reference
				hmCohendResults.put(sLbl, oCohenDEffectSizeLogic);
			}
		}

		// 3. Now calculate Cohen's d values for all keys and set the difference between
		// the means
		for (String sLbl : hmCohendResults.keySet()) {
			// Only calculate if more than 2 samplers on each side
			int iCntA = hmCohendResults.get(sLbl).getCountA();
			int iCntB = hmCohendResults.get(sLbl).getCountB();
			if ((iCntA > 2) && (iCntB > 2)) {
				double dVarA = hmCohendResults.get(sLbl).getVarianceA();
				double dVarB = hmCohendResults.get(sLbl).getVarianceB();
				double dMeanA = hmCohendResults.get(sLbl).getMeanA();
				double dMeanB = hmCohendResults.get(sLbl).getMeanB();
				double dPooledSD = calcPooledSD(iCntA, dVarA, iCntB, dVarB);
				double dCohend = (dPooledSD == 0) ? 0 : calcCohensd(dMeanA, dMeanB, dPooledSD);

				// Update the stats
				hmCohendResults.get(sLbl).setCohenD(dCohend);
				hmCohendResults.get(sLbl).setDiffRating(dCohend);
			} else {
				hmCohendResults.get(sLbl).sDiffRating = RATING_NOTAPPLICABLE;
			}
		}

		// 4. Update the statistics table for the UI in a natural order
		TreeMap<String, CohenDEffectSizeLogic> tmResultsSorted = new TreeMap<>(hmCohendResults);
		int iFailedLblCnt = 0;
		for (String sLbl : tmResultsSorted.keySet()) {
			String sIsFailed = "false";
			if (tmResultsSorted.get(sLbl).getDiffRating() == RATING_NOTAPPLICABLE) {
				sIsFailed = "na";
			} else {
				double dCohend = tmResultsSorted.get(sLbl).getCohenD().doubleValue();
				if (dCohend >= dCohendAL) {
					sIsFailed = "true";
					iFailedLblCnt++;
				}
			}

			Object[] oArrayRowData = {
					sLbl,
					tmResultsSorted.get(sLbl).getCountA(),
					tmResultsSorted.get(sLbl).getCountB(),
					Long.valueOf((long) tmResultsSorted.get(sLbl).getMeanA()),
					Long.valueOf((long) tmResultsSorted.get(sLbl).getMeanB()),
					Math.abs(tmResultsSorted.get(sLbl).getCohenD().doubleValue()),
					tmResultsSorted.get(sLbl).getDiffRating(),
					sIsFailed };
			pwrTblMdlStats.addRow(oArrayRowData);
		}

		// 6. Add last line for a global comparison of averages between A and B
		// Same calculations on the averages for a global comparison
		CohenDEffectSizeLogic oCohenDEffectSizeLogic = new CohenDEffectSizeLogic(AVERAGE_OF_AVERAGES);
		int iCntA = alAveragesA.size();
		int iCntB = alAveragesB.size();

		MathMoments mathMoments = MathMoments.crteMomentsFromMeansList(alAveragesA);
		oCohenDEffectSizeLogic.iCountA = iCntA;
		oCohenDEffectSizeLogic.fMeanA = mathMoments.getMean();
		oCohenDEffectSizeLogic.fVarianceA = mathMoments.getVariance();
		mathMoments = MathMoments.crteMomentsFromMeansList(alAveragesB);
		oCohenDEffectSizeLogic.iCountB = iCntB;
		oCohenDEffectSizeLogic.fMeanB = mathMoments.getMean();
		oCohenDEffectSizeLogic.fVarianceB = mathMoments.getVariance();

		if ((iCntA > 2) && (iCntB > 2)) {
			double dVarA = oCohenDEffectSizeLogic.getVarianceA();
			double dVarB = oCohenDEffectSizeLogic.getVarianceB();
			double dMeanA = oCohenDEffectSizeLogic.getMeanA();
			double dMeanB = oCohenDEffectSizeLogic.getMeanB();
			double dPooledSD = calcPooledSD(iCntA, dVarA, iCntB, dVarB);
			double dCohend = (dPooledSD == 0) ? 0 : calcCohensd(dMeanA, dMeanB, dPooledSD);

			// Update the stats
			oCohenDEffectSizeLogic.setCohenD(dCohend);
			oCohenDEffectSizeLogic.setDiffRating(dCohend);
		} else {
			oCohenDEffectSizeLogic.sDiffRating = RATING_NOTAPPLICABLE;
		}

		// Add the result to statistics table
		double dCohend = oCohenDEffectSizeLogic.getCohenD().doubleValue();
		String sIsFailed = "false";
		if (dCohend >= dCohendAL) {
			sIsFailed = "true";
		}
		Object[] oArrayRowData = { AVERAGE_OF_AVERAGES, iCntA, iCntB,
				Long.valueOf((long) oCohenDEffectSizeLogic.getMeanA()),
				Long.valueOf((long) oCohenDEffectSizeLogic.getMeanB()),
				Math.abs(oCohenDEffectSizeLogic.getCohenD().doubleValue()), oCohenDEffectSizeLogic.getDiffRating(),
				sIsFailed };
		pwrTblMdlStats.addRow(oArrayRowData);

		return iFailedLblCnt;
	}

	public static String saveTableStatsAsCsv(String sFilePath) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + SUFFIX_STATS + "csv";
		FileServices.saveTableAsCsv(sOutputFile, pwrTblMdlStats);
		return sOutputFile;
	}

	public static String saveTableStatsAsHtml(String sFilePath, String sCohensdAL) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + SUFFIX_STATS + "html";
		String sTableTitle = HTML_STATS_TITLE + " (Cohen's d Acceptable Limit = " + sCohensdAL + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, PASSFAIL_TEST_COLNBR);
		return sOutputFile;
	}

	/*
	 * Private methods
	 */
	private static double calcCohensd(double dMean1, double dMean2, double dPooledSD) {
		// returns Cohen's d, as per specs
		return (dMean2 - dMean1) / dPooledSD;
	}

	private static double calcPooledSD(int iN1, double dVariance1, int iN2, double dVariance2) {
		// returns Pooled standard deviation, as per specs
		return Math.sqrt(((iN1 - 1) * dVariance1 + (iN2 - 1) * dVariance2) / (iN1 + iN2 - 2));
	}

	private void setDiffRating(double dValue) {
		// 1. Get direction of movement
		String sDir = "";
		if (dValue < 0) {
			sDir = "decrease";
		} else if (dValue > 0) {
			sDir = "increase";
		}

		// 2. Get magnitude of movement according to Sawilowsky's rule of thumb
		double fAbsValue = Math.abs(dValue);
		String sMag = "Similar";
		if (fAbsValue >= 2.0) {
			sMag = "Huge";
		} else if (fAbsValue >= 1.20) {
			sMag = "Very large";
		} else if (fAbsValue >= 0.80) {
			sMag = "Large";
		} else if (fAbsValue >= 0.50) {
			sMag = "Medium";
		} else if (fAbsValue >= 0.02) {
			sMag = "Small";
		} else if (fAbsValue >= 0.01) {
			sMag = "Very small";
		} else if (fAbsValue > 0.0) {
			sMag = "Negligeable";
		}

		this.sDiffRating = sMag + " " + sDir;
	}

}
