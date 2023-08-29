/**
 * 
 */
package com.github.rbourga.jmeter.effectsize.logic;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
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

import rbourga.maths.resultscomparator.ResultsComparatorData;

public final class CohenDEffectSizeLogic {

	// Fields needed for Cohen's d analysis
	private String sLblName = "";
	private int iCountA;
	private int iCountB;
	private double fCoVA;
	private double fCoVB;
	private double dErrPctA;
	private double dErrPctB;
	private double fMeanA;
	private double fMeanB;
	private double fVarianceA;
	private double fVarianceB;
	private BigDecimal bdCohenD;
	private String sDiffRating;

	private CohenDEffectSizeLogic(String sLbl) {
		sLblName = sLbl;
		iCountA = 0;
		iCountB = 0;
		fCoVA = 0;
		fCoVB = 0;
		dErrPctA = 0;
		dErrPctB = 0;
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
	private void setCoVB(double dCov) {
		this.fCoVB = dCov;
	}
	private void setErrPctB(double dErrPct) {
		this.dErrPctB = dErrPct;
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
	private void setDiffRating(double dValue) {
		// 1. Get direction of movement
		String sDir = "";
		if (dValue < 0) sDir = "decrease";
		else if (dValue > 0) sDir = "increase";

		// 2. Get magnitude of movement according to Sawilowsky's rule of thumb
		double fAbsValue = Math.abs(dValue);
		String sMag = "Similar";
		if (fAbsValue >= 2.0) sMag = "Huge";
		else if (fAbsValue >= 1.20) sMag = "Very large";
		else if (fAbsValue >= 0.80) sMag = "Large";
		else if (fAbsValue >= 0.50) sMag = "Medium";
		else if (fAbsValue >= 0.02) sMag = "Small";
		else if (fAbsValue >= 0.01) sMag = "Very small";
		else if (fAbsValue > 0.0) sMag = "Negligeable";
		
		this.sDiffRating = sMag + " " + sDir;
	}

	// Variables to store all the moments
	private static ArrayList<Double> alAveragesA = new ArrayList<Double>(); // Used to store all averages of A
	private static ArrayList<Double> alAveragesB = new ArrayList<Double>(); // Used to store all averages of B
	private static HashMap<String, CohenDEffectSizeLogic> hmCohendResults = new HashMap<String, CohenDEffectSizeLogic>();

	private static ArrayList<Double> getAveragesA() {
		return alAveragesA;
	}
	private static ArrayList<Double> getAveragesB() {
		return alAveragesB;
	}
	private static HashMap<String, CohenDEffectSizeLogic> getCohendResults() {
		return hmCohendResults;
	}
	
	// TODO add the new column labels to
	// core/org/apache/jmeter/resources/messages.properties files.
	private static PowerTableModel pwrTblMdlStats = new PowerTableModel(
			new String[] {
					JMeterUtils.getResString("sampler label"), // Label
					"# Samples A",
					"# Samples B",
					"CoV A %", // Coefficents of Variation
					"CoV B %",
					"Error A %", // Errors
					"Error B %",
					"Average A", // Averages
					"Average B",
					"Cohen's d", // d
					"Diff Rating", // Descriptor
					"Failed" // shows a tick if value less than the specified threshold
			}, new Class[] {
					String.class, // Label
					Integer.class, // # Samples A
					Integer.class, // # Samples B
					Double.class, // CoV A
					Double.class, // CoV B
					Double.class, // Error A %
					Double.class, // Error B %
					Double.class, // Average A
					Double.class, // Average B
					Double.class, // Cohen's d
					String.class, // Diff Rating
					Boolean.class // Failed
			});
	
	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	public static int calcCohenDEffectSize(String sFilepathA, String sFilepathB, double dCohendAL) {
		// Load the data after getting the delimiter separator from current JMeter properties
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

		/*
		 * Processing of the data is done in multiple steps:
		 * 1. Calculate the moments for the Control samplers A.
		 * 2. Calculate the moments for the Variation samplers B.
		 * 3. Calculate Cohen's d between A and B
		 */
		// 1. Loop through the Labels in the dataset A
		MathMoments mathMoments = null;
		int iTotRcd;
		for (String sLbl : rcdHashMapA.keySet()) {
			List<CSVRecord> aRcd = rcdHashMapA.get(sLbl);
			iTotRcd = aRcd.size();

			// Get some stats for this set of samples
			mathMoments = MathMoments.crteMomentsFromRecordsList(aRcd);
			// Save some values for later analysis
			CohenDEffectSizeLogic oCohenDEffectSizeLogic = new CohenDEffectSizeLogic(sLbl);
			oCohenDEffectSizeLogic.iCountA = iTotRcd;
			oCohenDEffectSizeLogic.fCoVA = mathMoments.getCoV();
			oCohenDEffectSizeLogic.dErrPctA = mathMoments.getErrorPercentage();
			oCohenDEffectSizeLogic.fMeanA = mathMoments.getMean();
			oCohenDEffectSizeLogic.fVarianceA = mathMoments.getVariance();

			// Save this average for later processing oƒthe averages
			alAveragesA.add(mathMoments.getMean());
			// Add the results of analysis to hashmap for later reference
			hmCohendResults.put(sLbl, oCohenDEffectSizeLogic);
		}

		// 2. Repeat for dataset B
		for (String sLbl : rcdHashMapB.keySet()) {
			List<CSVRecord> aRcd = rcdHashMapB.get(sLbl);
			iTotRcd = aRcd.size();

			// Get some stats for this set of samples
			mathMoments = MathMoments.crteMomentsFromRecordsList(aRcd);
			// Save some values for later analysis
			// Is this Sample in the Control file?
			if (hmCohendResults.containsKey(sLbl)) {
				// Yes: update the Stats with B values			
				hmCohendResults.get(sLbl).setCountB(iTotRcd);
				hmCohendResults.get(sLbl).setCoVB(mathMoments.getCoV());
				hmCohendResults.get(sLbl).setErrPctB(mathMoments.getErrorPercentage());
				hmCohendResults.get(sLbl).setMeanB(mathMoments.getMean());
				hmCohendResults.get(sLbl).setVarianceB(mathMoments.getVariance());
			} else {
				// No: add a new result to the list
				CohenDEffectSizeLogic oCohenDEffectSizeLogic = new CohenDEffectSizeLogic(sLbl);
				oCohenDEffectSizeLogic.iCountB = iTotRcd;
				oCohenDEffectSizeLogic.fCoVB = mathMoments.getCoV();
				oCohenDEffectSizeLogic.dErrPctB = mathMoments.getErrorPercentage();
				oCohenDEffectSizeLogic.fMeanB = mathMoments.getMean();
				oCohenDEffectSizeLogic.fVarianceB = mathMoments.getVariance();
				// Add the results of analysis to hashmap for later reference
				hmCohendResults.put(sLbl, oCohenDEffectSizeLogic);
			}
		}
		
		// 3. Now calculate Cohen's d values for all keys and set the difference between the means
		int iCntA, iCntB;
		double dVarA, dVarB, dPooledSD, dMeanA, dMeanB, dCohenD;
		for (String sLbl : hmCohendResults.keySet() ) {
			// Only calculate if more than 2 samplers on each side
			iCntA = hmCohendResults.get(sLbl).getCountA();
			iCntB = hmCohendResults.get(sLbl).getCountB();
			if ((iCntA >= 2) && (iCntB >= 2)) {
				dVarA = hmCohendResults.get(sLbl).getVarianceA();
				dVarB = hmCohendResults.get(sLbl).getVarianceB();
				dMeanA = hmCohendResults.get(sLbl).getMeanA();
				dMeanB = hmCohendResults.get(sLbl).getMeanB();
				dPooledSD = calcPooledSD(iCntA, dVarA, iCntB, dVarB);
				dCohenD = calcCohensD(dMeanA, dMeanB, dPooledSD);
				// Update the stats
				hmCohendResults.get(sLbl).setCohenD(dCohenD);
				hmCohendResults.get(sLbl).setDiffRating(dCohenD);
			}
		}

// Ici

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
			if ((bdApdexScoreRnd.compareTo(bdApdexAQL) == -1) || (bdCoVScoreRnd.compareTo(bdCoVALPct) != -1)) {
				bFailed = true;
				iTotNbrOfFailedRcd++;
			}

			Object[] oArrayRowData = { sLbl, // Label
					iTotRcd, // # Samples
					Long.valueOf((long) mathMoments.getMean()), // Average
					bdCoVScoreRnd.doubleValue(), // Cof of Var %
					sCoVRating, // Cof of Var Rating
					Double.valueOf(mathMoments.getErrorPercentage()), // # Error %
					bdApdexScoreRnd.doubleValue(), // Apdex Value
					dApdexTgtTholdSec, // Apdex Target
					sApdexRating, // Apdex Rating
					bSmallGroup, // shows a tick if number of samples < 100
					bFailed }; // shows a tick if value less than the specified threshold

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

	public static boolean isCohendALOutOfRange(double fCohendAL) {
		return fCohendAL < 0;
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
		String sTableTitle = "Apdex & Coefficient of Variation Score Results (Apdex Acceptable Quality Level = "
				+ sApdexAQL + ", CoV Acceptable Limit = " + sCoVALPct + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, 10);
		return sOutputFile;
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
			sRating = "High"; // high if > 30%
		else if (bdScore.doubleValue() >= 0.10)
			sRating = "Moderate"; // moderate if > 10%
		return sRating;
	}

	private static void setHmCohendResults(HashMap<String, CohenDEffectSizeLogic> hmCohendResults) {
		CohenDEffectSizeLogic.hmCohendResults = hmCohendResults;
	}

	private static void setPwrTblMdlStats(PowerTableModel pwrTblMdlStats) {
		CohenDEffectSizeLogic.pwrTblMdlStats = pwrTblMdlStats;
	}

	private static double calcPooledSD(int iN1, double dVariance1, int iN2, double dVariance2) {
		// returns Pooled standard deviation, as per specs
		return Math.sqrt(((iN1 - 1) * dVariance1 + (iN2 - 1) * dVariance2) / (iN1 + iN2 - 2));
	}
	
	public static double calcCohensD(double iMean1, double dMean2, double dPooledSD) {
		// returns Cohen's d, as per specs
		return (dMean2 - iMean1) / dPooledSD;
	}


}
