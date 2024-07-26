package com.github.rbourga.jmeter.common;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVRecord;

public class MathMoments {

	// Variables
	private double dCoV;	// Coefficient of Variation
	private double dErrPct;
	private double dMax;
	private double dMean;
	private double dMin;
	private double dStdDev;
	private double dVariance;

	// Constructor
	public MathMoments(double dErrPct, double dMax, double dMean, double dMin, double dVariance) {
		this.dErrPct = dErrPct;
		this.dMax = dMax;
		this.dMean = dMean;
		this.dMin = dMin;
		this.dVariance = dVariance;
		this.dStdDev = Math.sqrt(dVariance);
		this.dCoV = (dMean == 0) ? 0 : dStdDev / dMean;
	}

	public double getCoV() {
		return dCoV;
	}
	public double getErrorPercentage() {
		return dErrPct;
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
	public double getStdDev() {
		return dStdDev;
	}
	public double getVariance() {
		return dVariance;
	}

	public static MathMoments crteMomentsFromRecordsList(List<CSVRecord> listRcd) {
		// Perform calculations after casting the lists into streams to take advantage of streams
		double dVariance = 0;
		int iRcdNbr = listRcd.size();
		double dMean = listRcd.stream().mapToLong(rcd -> Long.parseLong(rcd.get("elapsed"))).average().orElse(0);
		double dMin = listRcd.stream().mapToLong(rcd -> Long.parseLong(rcd.get("elapsed"))).min().orElse(0);
		double dMax = listRcd.stream().mapToLong(rcd -> Long.parseLong(rcd.get("elapsed"))).max().orElse(0);

		if (iRcdNbr > 1) {
			// variance = sum((x_i - mean)^2) / (n - 1)
			dVariance = listRcd.stream().mapToDouble(rcd -> Long.parseLong(rcd.get("elapsed"))).map(t -> Math.pow(t - dMean, 2)).sum() / (iRcdNbr - 1);
		}
		// Filter the failed samples and count them
		Stream<CSVRecord> oFailedSamples = listRcd.stream().filter(rcd -> rcd.get("success").equals("false"));
		double dErrPct = (double) oFailedSamples.count() / (double) iRcdNbr;;

		return new MathMoments(dErrPct, dMax, dMean, dMin, dVariance);
	}

	public static MathMoments crteMomentsFromMeansList(ArrayList<Double> alMeans) {
		// Perform calculations after casting the list into streams to take advantage of streams
		double dVariance = 0;
		int iEnbr = alMeans.size();
		double dMean = alMeans.stream().mapToDouble(m -> m).average().orElse(0.0);
		double dMin = alMeans.stream().mapToDouble(m -> m).min().orElse(0.0);
		double dMax = alMeans.stream().mapToDouble(m -> m).max().orElse(0.0);
		if (iEnbr > 1) {
			// variance = sum((x_i - mean)^2) / (n - 1)
			dVariance = alMeans.stream().mapToDouble(m -> m).map(m -> Math.pow(m - dMean, 2)).sum() / (iEnbr - 1);
		}
		return new MathMoments(0, dMax, dMean, dMin, dVariance);
	}

}
