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
		 * We calculate the mvalue using 2 bin rules and keep the largest mvalue found.
		 * See https://en.wikipedia.org/wiki/Histogram
		 */
		if (mathMo.getStdDev() != 0) {
			int iRcdNbr = listRcd.size();
			for (int i = 0; i < 2; i++) {
				int iCurrBinSize;
				String sCurrRule;
				if (i == 0) {
					// 1st try: use of Scott's formula
					sCurrRule = "Scott";
					iCurrBinSize = (int) Math.ceil(3.5 * mathMo.getStdDev() / Math.cbrt(iRcdNbr));
				} else {
					// 2nd try: use of Freedman–Diaconis rule
					sCurrRule = "Freedman-Diaconis";
					double dIQR = mathMo.getQ3() - mathMo.getQ1();
					iCurrBinSize = (int) Math.ceil(2 * dIQR / Math.cbrt(iRcdNbr));
				}

				double dCurrM = 0;
				int[] currHistogram = null;
				if (iCurrBinSize != 0) {
					// Build the histogram
					currHistogram = buildHistogram(listRcd, iCurrBinSize, mathMo);

					// Now calculate the mvalue
					// See formula at https://www.brendangregg.com/FrequencyTrails/modes.html
					// 1. Find the maximum frequency
					int iMaxFrequency = 0;
					for (int iBin : currHistogram) {
						if (iBin > iMaxFrequency) {
							iMaxFrequency = iBin;
						}
					}
					// 2. Get mvalue
					double dSumOfAbsoluteDifferences = 0;
					for (int iH = 1; iH < currHistogram.length; iH++) {
						dSumOfAbsoluteDifferences += Math.abs(currHistogram[iH] - currHistogram[iH - 1]);
					}
					dCurrM = iMaxFrequency == 0 ? 0 : dSumOfAbsoluteDifferences * (1.0 / iMaxFrequency);
				}

				// If mValue is larger, then save this try for the reporting
				if (dCurrM > dMvalue) {
					dMvalue = dCurrM;
					sBinRule = sCurrRule;
					iBinSize = iCurrBinSize;
					histogram = currHistogram;
				}
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
