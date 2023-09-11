/**
 *
 */
package com.github.rbourga.jmeter.tukeyoutlierdetector.logic;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.save.CSVSaveService;
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
					"Removed %",  // percentage of samples that have been discarded
					"Small Group", // shows a tick if remaining number of samples < 100
					"Failed" // shows a tick if value greater than the specified threshold
			}, new Class[] {
					String.class,	// Label
					Integer.class,	// # Samples
					Double.class,	// Average
					Integer.class,	// Upper Fence
					Integer.class,	// # Removed
					Double.class,	// # Removed %
					Boolean.class,	// Small Group
					Boolean.class	// Failed
					});

	public static PowerTableModel getPwrTblMdelStats() {
		return pwrTblMdlStats;
	}

	public static int RemoveUpper(String sFilepath, double fTukeyK, double fMaxRemPct) {
		/*
		 * Will remove only upper outliers (which are bigger than the upper boundary).
		 */
		boolean bUpperRemoved;
		int iInitCnt;
		double fUpperFence, fUpperFenceMin;
		List<CSVRecord> aRcd;

		BigDecimal _bdMaxTrimPct = new BigDecimal(fMaxRemPct);
		int _iNbOfFailedElements = 0;

		// Load the data after getting the delimiter separator from current JMeter properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		Map<String, List<CSVRecord>> rcdHashMap = FileServices.loadSamplesIntoHashMap(sFilepath, cDelim);
		if (rcdHashMap.isEmpty()) {
			return -1; // Nothing loaded, so abort...
		}

		// Clear any statistics from a previous operation
		pwrTblMdlStats.clearData();

		// 2. Now, process the data points...
		// Loop through the Labels in the dataset
		for (String sLbl : rcdHashMap.keySet()) {
			aRcd = rcdHashMap.get(sLbl);
			iInitCnt = aRcd.size();
			fUpperFence = 0.0;
			fUpperFenceMin = Double.MAX_VALUE;

			// Only look for outliers if there are at least four items to compare
			if (iInitCnt > 3) {
				/*
				 * An outlier can hide another outlier...so when removing extreme values, we
				 * have to iterate until no extreme values are left.
				 */
				bUpperRemoved = true;
				do {
					// Get the upper fence
					fUpperFence = getUpperFence(aRcd, fTukeyK);
					// Save the most severe limit for the report
					fUpperFenceMin = Math.min(fUpperFence, fUpperFenceMin); 
					// Now remove all samples that are higher than the upper fence using Java's Stream API
					// Ici
					int _iSampleIndex = _aLabelSamples.size() - 1; // for performance reasons, start by the end
					_bDataTrimmed = false;
					boolean _bSampleTrimmed = true;
					do {
						if (_aLabelSamples.get(_iSampleIndex).getTime() > _fUpperFence) {
							/*
							 * Outlier detected: save the outlier in a new list before removing it from
							 * current list
							 */
							aOutlierList.add(_aLabelSamples.get(_iSampleIndex));
							_aLabelSamples.remove(_iSampleIndex);
							_iSampleIndex--;
							_bDataTrimmed = true;
						} else {
							/*
							 * No more samples to trim: stop parsing the list
							 */
							_bSampleTrimmed = false;
						}
					} while (_bSampleTrimmed);
				} while (_bDataTrimmed);
			}

			// Report the results
			int _iNumberOfObjectsTrimmed = _iNumberOfObjectsBefore - _aLabelSamples.size();
			BigDecimal _bDnumberOfObjectsTrimmedPerCent = new BigDecimal(
					(double) _iNumberOfObjectsTrimmed / (double) _iNumberOfObjectsBefore);
			// Round % to 4 decimal places
			BigDecimal _bDnumberOfObjectsTrimmedPerCentRounded = _bDnumberOfObjectsTrimmedPerCent.setScale(4,
					RoundingMode.HALF_UP);
			iNumberOfTotalObjectsTrimmed = iNumberOfTotalObjectsTrimmed + _iNumberOfObjectsTrimmed;
			Boolean _bSmallGroup = false;
			if (_aLabelSamples.size() < 100) {
				_bSmallGroup = true;
			}
			Boolean _bFailed = false;
			if (_bDnumberOfObjectsTrimmedPerCentRounded.compareTo(_bdMaxTrimPct) >= 0) {
				_bFailed = true;
				_iNbOfFailedElements++;
			}

			// Update the statistics table
			Object[] _oArrayRowData = { _sLabelId, _iNumberOfObjectsBefore, _fUpperFenceMin, _iNumberOfObjectsTrimmed,
					_bDnumberOfObjectsTrimmedPerCentRounded.doubleValue(), _bSmallGroup, _bFailed};
			oPowerTableModel.addRow(_oArrayRowData);
		}

		// Save the non-trimmed results in a file for post stats
		sInputFileDirectoryName = FilenameUtils.getFullPath(sInputFile);
		sInputFileBaseName = FilenameUtils.getBaseName(sInputFile);
		String _sInputFileExtension = FilenameUtils.getExtension(sInputFile);
		// Filename for the "good" samplers only
		String _sOutputFile = sInputFileDirectoryName + sInputFileBaseName + "_Trimmed." + _sInputFileExtension;
		PrintWriter _oPrintWriter = initializeFileOutput(_sOutputFile);
		SampleEvent _oSampleEvent = null;
		for (String _sLabelId : mSampleList.keySet()) {
			List<SampleResult> _aSampleResult = mSampleList.get(_sLabelId);
			for (SampleResult _oSampleResult : _aSampleResult) {
				_oSampleEvent = new SampleEvent(_oSampleResult, null);
				_oPrintWriter.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
			}
		}
		// Close the file
		_oPrintWriter.close();

		// Now save the outliers in a separate file for post analysis
		if (aOutlierList.isEmpty() == false) {
			// Filename containing the excluded samplers only
			_sOutputFile = sInputFileDirectoryName + sInputFileBaseName + "_Outliers." + _sInputFileExtension;
			_oPrintWriter = initializeFileOutput(_sOutputFile);
			for (SampleResult _oSampleResult : aOutlierList) {
				_oSampleEvent = new SampleEvent(_oSampleResult, null);
				_oPrintWriter.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
			}
			// Close the file
			_oPrintWriter.close();
		}
		return _iNbOfFailedElements;
	}

	private static double getUpperFence(List<CSVRecord> aRcd, double fTukeyK) {
		/* We use Apache Commons Math library to find the 25th and 75th percentiles which
		 * will give us the UpperFence.
		 */
		// 1. Extract the "elapsed time" values into a list
		List<Double> elapsedValues = new ArrayList<Double>();
		for (CSVRecord rcd : aRcd) {
			double elapsedTime = Double.parseDouble(rcd.get("elapsed"));
			elapsedValues.add(elapsedTime);
		}
		// 2. Convert the list of elapsedTime to a double array
		double[] dElapsedArray = elapsedValues.stream().mapToDouble(Double::doubleValue).toArray();

		//3. Use DescriptiveStatistics to get Q1 and Q3
		DescriptiveStatistics descrStats = new DescriptiveStatistics(dElapsedArray);
		double dQ1 = descrStats.getPercentile(25);	// first quartile
		double dQ3 = descrStats.getPercentile(75);	// third quartile

		// Return the upper fence value
		double fInterQuartileRange = dQ3 - dQ1;
		double fUpperFence = dQ3 + (fTukeyK * fInterQuartileRange);
		return fUpperFence;
	}


	public static int computeApdexCoV(String sFilepath, double dApdexTgtTholdSec, double dApdexAQL, double dCoVALPct) {
		int iTotRcd;
		long lSatisfiedCount, lToleratingCount;
		BigDecimal bdApdexScore, bdApdexScoreRnd, bdCoVScore, bdCoVScoreRnd, bdErrPct , bdErrPctRnd;
		BigDecimal bdApdexAQL = new BigDecimal(dApdexAQL);
		BigDecimal bdCoVALPct = new BigDecimal(dCoVALPct);
		Boolean bSmallGroup, bFailed;
		String sApdexRating, sCoVRating;
		List<CSVRecord> aRcd;
		MathMoments mathMoments;
		Stream<CSVRecord> oPassedSamples, oSatisfiedSamples, oToleratingSamples;

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
		dApdexTgtTholdSec = ApdexCoVLogic.formatTgtTHold(dApdexTgtTholdSec);

		// Now process the data points
		long lApdexTgtTholdMS = (long) (dApdexTgtTholdSec * 1000); // Convert to ms as JMeter times are stored in ms
		long lApdexTolTholdMS = 4 * lApdexTgtTholdMS; // Tolerate = 4xTarget, as per Apdex specs
		int iTotNbrOfFailedRcd = 0;
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

			// Coeff Var processing
			bdCoVScore = new BigDecimal(mathMoments.getCoV());
			// Similar to error rate, round to 4 decimal places
			bdCoVScoreRnd = bdCoVScore.setScale(4, RoundingMode.HALF_UP);
			sCoVRating = setCoVRating(bdCoVScoreRnd);

			// ErrPct formatting: round to 4 decimal places
			bdErrPct = new BigDecimal(mathMoments.getErrorPercentage());
			bdErrPctRnd = bdErrPct.setScale(4, RoundingMode.HALF_UP);

			// Finally update the statistics table
			bSmallGroup = false;
			if (iTotRcd < 100) {
				bSmallGroup = true;
			}
			bFailed = false;
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
					bdErrPctRnd.doubleValue(),	//# Error %
                    bdApdexScoreRnd.doubleValue(),	//Apdex Value
                    dApdexTgtTholdSec,	// Apdex Target
                    sApdexRating,    // Apdex Rating
                    bSmallGroup,    // shows a tick if number of samples < 100
					bFailed };	// shows a tick if value less than the specified threshold

			pwrTblMdlStats.addRow(oArrayRowData);
		}
		return iTotNbrOfFailedRcd;
	}

	private static String setCoVRating(BigDecimal bdScore) {
		String sRating = "Low";
		if (bdScore.doubleValue() >= 0.30)
			sRating = "High";	// high if > 30%
		else if (bdScore.doubleValue() >= 0.10)
			sRating = "Moderate";	// moderate if > 10%
		return sRating;
	}

	public static boolean isTrimPctOutOfRange(double fPct) {
		return fPct < 0;
	}

	public static String saveTableStatsAsCsv(String sFilePath) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ApdexCoVScores.csv";
		FileServices.saveTableAsCsv(sOutputFile, pwrTblMdlStats);
		return sOutputFile;
	}

	public static String saveTableStatsAsHtml(String sFilePath, String sApdexAQL, String sCoVALPct) {
		String sFileDirectoryName = FilenameUtils.getFullPath(sFilePath);
		String sFileBaseName = FilenameUtils.getBaseName(sFilePath);
		String sOutputFile = sFileDirectoryName + sFileBaseName + "_ApdexCoVScores.html";
		String sTableTitle = "Apdex & Coefficient of Variation Score Results (Apdex Acceptable Quality Level = " + sApdexAQL + ", CoV Acceptable Limit = " + sCoVALPct + ")";
		FileServices.saveTableAsHTML(sOutputFile, sTableTitle, pwrTblMdlStats, 10);
		return sOutputFile;
	}
}
