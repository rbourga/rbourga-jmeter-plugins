package com.github.rbourga.jmeter.common;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.StatUtils;

public class MathMoments {

	// Constructor
	private double dCoV; // Coefficient of Variation
	private double dMax;
	private double dMean;
	private double dMin;
	private double dQ1; // 1st quartile
	private double dQ3; // 3rd quartile
	private double dStdDev;
	private double dVariance;
	public MathMoments(double dMax, double dMean, double dMin, double dQ1, double dQ3, double dVariance) {
		this.dMax = dMax;
		this.dMean = dMean;
		this.dMin = dMin;
		this.dQ1 = dQ1;
		this.dQ3 = dQ3;
		this.dStdDev = Math.sqrt(dVariance);
		this.dCoV = (dMean == 0) ? 0 : dStdDev / dMean;
		this.dVariance = dVariance;
	}

	public double getCoV() {
		return dCoV;
	}

	public double getMax() {
		return dMax;
	}

	public double getMean() {
		return dMean;
	}

	public double getMin() {
		return dMin;
	}

	public double getQ1() {
		return dQ1;
	}

	public double getQ3() {
		return dQ3;
	}

	public double getStdDev() {
		return dStdDev;
	}

	public double getVariance() {
		return dVariance;
	}

	public static MathMoments crteMomentsFromRecordsList(List<CSVRecord> listRcd) {
		/*
		 * Previously, we were using streams for few statistics. Now we use Apache
		 * Commons Maths that provides more statistics methods.
		 */
		/*
		 * Old code:
		 * int iRcdNbr = listRcd.size();
		 * // Filter the failed samples and count them
		 * Stream<CSVRecord> oFailedSamples = listRcd.stream().filter(rcd -> rcd.get("success").equals("false"));
		 * double dErrPct = (double) oFailedSamples.count() / (double) iRcdNbr;
		 * // Perform calculations after casting the lists into streams to take advantage of streams
		 * double dVariance = 0;
		 * double dMean = listRcd.stream().mapToLong(rcd -> Long.parseLong(rcd.get("elapsed"))).average().orElse(0);
		 * double dMin = listRcd.stream().mapToLong(rcd -> Long.parseLong(rcd.get("elapsed"))).min().orElse(0); 
		 * double dMax = listRcd.stream().mapToLong(rcd -> Long.parseLong(rcd.get("elapsed"))).max().orElse(0);
		 * if (iRcdNbr > 1) {
		 *  // variance = sum((x_i - mean)^2) / (n - 1)
		 *  dVariance = listRcd.stream().mapToDouble(rcd -> Long.parseLong(rcd.get("elapsed"))).map(t -> Math.pow(t - dMean, 2)).sum() / (iRcdNbr - 1);
		 *  }
		 */

		// Extract the elapsed values into a list and then convert this list to a double
		// array for StatsUtils
		List<Double> listElapsed = new ArrayList<>();
		for (CSVRecord rcd : listRcd) {
			double dElapsed = Double.parseDouble(rcd.get("elapsed"));
			listElapsed.add(dElapsed);
		}
		double[] aElapsed = listElapsed.stream().mapToDouble(Double::doubleValue).toArray();

		// Get our statistics
		return calculateStats(aElapsed);
	}

	public static MathMoments crteMomentsFromMeansList(ArrayList<Double> alMeans) {
		/*
		 * Switch to StatsUtils class to get the Moments
		 */
		/*
		 * Old code:
		 * // Perform calculations after casting the list into streams to take advantage of streams
		 * double dVariance = 0;
		 * int iEnbr = alMeans.size(); 
		 * double dMean = alMeans.stream().mapToDouble(m -> m).average().orElse(0.0);
		 * double dMin = alMeans.stream().mapToDouble(m -> m).min().orElse(0.0); 
		 * double dMax = alMeans.stream().mapToDouble(m -> m).max().orElse(0.0); 
		 * if (iEnbr > 1) { 
		 * // variance = sum((x_i - mean)^2) / (n - 1) 
		 *  dVariance = alMeans.stream().mapToDouble(m -> m).map(m -> Math.pow(m - dMean, 2)).sum() / (iEnbr - 1); 
		 *  }
		 */
		double[] aElapsed = alMeans.stream().mapToDouble(Double::doubleValue).toArray();
		return calculateStats(aElapsed);
	}

	private static MathMoments calculateStats(double[] dValues) {
		// We use StatUtils to get the different stats of interest.
		double dMax = StatUtils.max(dValues);
		double dMean = StatUtils.mean(dValues);
		double dMin = StatUtils.min(dValues);
		double dQ1 = StatUtils.percentile(dValues, 25);
		double dQ3 = StatUtils.percentile(dValues, 75);
		double dVariance = StatUtils.variance(dValues);

		return new MathMoments(dMax, dMean, dMin, dQ1, dQ3, dVariance);
	}

}
