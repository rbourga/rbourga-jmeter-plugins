package com.github.rbourga.jmeter.multimodalitycov.maths;

import java.util.List;

import org.apache.commons.csv.CSVRecord;

import com.github.rbourga.jmeter.common.MathMoments;

public class MValueCalculator {

	// Constructor
	private String sBinRuleName;
	private int iBinSize;
	private double dMvalue;
	private int[] iBinsArray; // Array to store the histogram bins

	// Constants
	/**
	 * Scott's normal reference rule uses a constant factor (3.5) in the bin width
	 * calculation: binWidth = 3.5 * sigma / n^(1/3).
	 */
	private static final double SCOTT_FACTOR = 3.5;

	/**
	 * Freedman–Diaconis rule uses a factor of 2 in bin width calculation:
	 * binWidth = 2 * IQR / n^(1/3).
	 */
	private static final double FREEDMAN_DIACONIS_FACTOR = 2.0;

	public MValueCalculator(String sBinRule, int iBinSize, double dMvalue, int[] iBinsArray) {
		this.sBinRuleName = sBinRule;
		this.iBinSize = iBinSize;	// we use an int as we are counting discrete values (i.e., response times in ms)
		this.dMvalue = dMvalue;
		this.iBinsArray = iBinsArray;
	}

	public String getBinRule() {
		return sBinRuleName;
	}

	public int getBinSize() {
		return iBinSize;
	}

	public int[] getiBinsArray() {
		return iBinsArray;
	}

	public double getMvalue() {
		return dMvalue;
	}

	/*
	 * Public methods
	 */
	public static MValueCalculator calculate(List<CSVRecord> listRcd, MathMoments mathMo) {

		double dMvalue = 0;
		int iBinSize = 0;
		int[] histogram = null;
		String sBinRule = null;

		/*
		 * We calculate bin sizes using 2 bin rules and use the one that produces
		 * the largest bin size. This reduces sensitivity to small variations and false
		 * positives. The MIN_BIN_SIZE filter in MultimodalityCoVLogic will mark as "na"
		 * any results with insufficient bin size.
		 * See https://en.wikipedia.org/wiki/Histogram
		 */
		if (mathMo.getStdDev() != 0) {
			int iRcdNbr = listRcd.size();
			int iMaxBinSize = 0;
			String sMaxBinRule = null;

			for (int i = 0; i < 2; i++) {
				int iCurrBinSize;
				String sCurrRule;
				if (i == 0) {
					// 1st try: use of Scott's formula (uses SCOTT_FACTOR constant)
					sCurrRule = "Scott";
					iCurrBinSize = (int) Math.ceil(SCOTT_FACTOR * mathMo.getStdDev() / Math.cbrt(iRcdNbr));
				} else {
					// 2nd try: use of Freedman–Diaconis rule (uses FREEDMAN_DIACONIS_FACTOR constant)
					sCurrRule = "Freedman-Diaconis";
					double dIQR = mathMo.getQ3() - mathMo.getQ1();
					iCurrBinSize = (int) Math.ceil(FREEDMAN_DIACONIS_FACTOR * dIQR / Math.cbrt(iRcdNbr));
				}

				// Keep track of the formula that produces the largest bin size
				if (iCurrBinSize > iMaxBinSize) {
					iMaxBinSize = iCurrBinSize;
					sMaxBinRule = sCurrRule;
				}
			}

			// Use the bin size from the formula that produced the largest bin size
			if (iMaxBinSize != 0) {
				iBinSize = iMaxBinSize;
				sBinRule = sMaxBinRule;
				histogram = buildHistogram(listRcd, iBinSize, mathMo);

				// Now calculate the mvalue using this bin size
				// See formula at https://www.brendangregg.com/FrequencyTrails/modes.html
				// 1. Find the maximum frequency
				int iMaxFrequency = 0;
				for (int iBin : histogram) {
					if (iBin > iMaxFrequency) {
						iMaxFrequency = iBin;
					}
				}
				// 2. Get mvalue
				double dSumOfAbsoluteDifferences = 0;
				for (int iH = 1; iH < histogram.length; iH++) {
					dSumOfAbsoluteDifferences += Math.abs(histogram[iH] - histogram[iH - 1]);
				}
				dMvalue = iMaxFrequency == 0 ? 0 : dSumOfAbsoluteDifferences * (1.0 / iMaxFrequency);
			}
		}
		return new MValueCalculator(sBinRule, iBinSize, dMvalue, histogram);

	}

	/*
	 * Private methods
	 */
	private static int[] buildHistogram(List<CSVRecord> listRcd, int iBinSize, MathMoments mathMo) {
		double dMin = mathMo.getMin();
		double dMax = mathMo.getMax();

		// Get the number of bins
		int iBinCount = (int) Math.ceil((dMax - dMin) / iBinSize);

		// Build the array of bins with the count of items in each corresponding bin,
		// including zero bin terminators
		int[] iBins = new int[iBinCount + 2]; // Add 2 for zero terminators at the beginning and end (initialized at 0)
		for (CSVRecord rcd : listRcd) {
			double dElapsed = Long.parseLong(rcd.get("elapsed"));
			int iBinIndex = (int) ((dElapsed - dMin) / iBinSize) + 1; // Offset by 1 to account for zero terminator at
																		// the start
			// Any data point higher than max bin is counted in the max bin.
			if (iBinIndex >= iBinCount + 1) {
				iBinIndex = iBinCount;
			}
			iBins[iBinIndex]++;
		}
		return iBins;
	}
}
