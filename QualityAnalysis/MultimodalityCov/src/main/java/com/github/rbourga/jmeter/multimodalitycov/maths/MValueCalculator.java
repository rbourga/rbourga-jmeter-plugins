package com.github.rbourga.jmeter.multimodalitycov.maths;

import java.util.List;

import org.apache.commons.csv.CSVRecord;

import com.github.rbourga.jmeter.common.MathMoments;

public class MValueCalculator {

	// Variables
	private double dBinSize;
	private double dMvalue;
	
    // Constructor
	public MValueCalculator(double dBinSize, double dMvalue) {
        this.dBinSize = dBinSize;
        this.dMvalue = dMvalue;
    }

	public double getBinSize() {
		return dBinSize;
	}
	public double getMvalue() {
		return dMvalue;
	}

	public static MValueCalculator calculate(List<CSVRecord> listRcd, MathMoments mathMo) {

		double dMvalue = 0;
		int iRcdNbr = listRcd.size();

		// Calculate the bin size using Scott's method
		// See https://en.wikipedia.org/wiki/Histogram
		double dBinSize = ((3.5 * mathMo.getStdDev()) / Math.cbrt(iRcdNbr));
		if (dBinSize != 0) {
			// Build the histogram using the bin size
			int[] histogram = buildHistogram(listRcd, dBinSize, mathMo);

			// Now calculate the mvalue
			// See formula at https://www.brendangregg.com/FrequencyTrails/modes.html

			// Find the maximum frequency
			int iMaxFrequency = 0;
			for (int iBin : histogram) {
				if (iBin > iMaxFrequency) {
					iMaxFrequency = iBin;
				}
			}
			
			double dSumOfAbsoluteDifferences = 0;
			for (int i = 1; i < histogram.length; i++) {
				dSumOfAbsoluteDifferences += Math.abs(histogram[i] - histogram[i - 1]);
			}

			dMvalue = iMaxFrequency == 0 ? 0 : (1.0 /iMaxFrequency) * dSumOfAbsoluteDifferences;
		}		

		return new MValueCalculator(dBinSize, dMvalue);
	}

	private static int[] buildHistogram(List<CSVRecord> listRcd, double dBinSize, MathMoments mathMo) {
		double dMin = mathMo.getMin();
		double dMax = mathMo.getMax();
		
		// Get the number of bins
		int iBinCount = (int) Math.ceil((dMax - dMin) / dBinSize);
		
		// Build the array of bins with the count of items in each corresponding bin, including zero bin terminators
		int[] iBins = new int[iBinCount + 2];	// Add 2 for zero terminators at the beginning and end (initialized at 0)
		for (CSVRecord rcd : listRcd) {
			double dElapsed = Long.parseLong(rcd.get("elapsed"));
			int iBinIndex = (int) ((dElapsed - dMin) / dBinSize) + 1;	// Offset by 1 to account for zero terminator at the start
			if (iBinIndex >= iBinCount + 1) iBinIndex = iBinCount;
			iBins[iBinIndex]++;
		}

		return iBins;
	}

}
