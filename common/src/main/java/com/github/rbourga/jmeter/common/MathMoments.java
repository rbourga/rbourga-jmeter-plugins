package com.github.rbourga.jmeter.common;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVRecord;

public class MathMoments {

	// Variables
	private double dCoV;	// Coefficient of Variation
	private double dErrPct;
	private double dMean;
	private double dStdDev;
	private double dVariance;

	// Constructor
	public MathMoments(double dErrPct, double dMean, double dVariance) {
		this.dErrPct = dErrPct;
		this.dMean = dMean;
		this.dVariance = dVariance;
		this.dStdDev = Math.sqrt(dVariance);
		this.dCoV = dStdDev / dMean;
	}

	public double getCoV() {
		return dCoV;
	}

	public double getErrorPercentage() {
		return dErrPct;
	}

	public double getMean() {
		return dMean;
	}

	public double getVariance() {
		return dVariance;
	}

	public static MathMoments crteMomentsFromRecordsList(List<CSVRecord> listRcd) {
		// Perform calculations after casting the lists into streams to take advantage
		// of streams
		double dVariance = 0;
		int iRcdNbr = listRcd.size();

		double dMean = listRcd.stream().mapToLong(rcd -> Long.parseLong(rcd.get("elapsed"))).average().orElse(0);

		if (iRcdNbr > 1) {
			// variance = sum((x_i - mean)^2) / (n - 1)
			dVariance = listRcd.stream().mapToDouble(rcd -> Long.parseLong(rcd.get("elapsed"))).map(t -> Math.pow(t - dMean, 2)).sum() / (iRcdNbr - 1);
		}
		/*
		 * To delete: long lMinTime = aLabelSamples.stream().mapToLong(record ->
		 * Long.parseLong(record.get("elapsed"))).min() .orElse(0); long lMaxTime =
		 * aLabelSamples.stream().mapToLong(record ->
		 * Long.parseLong(record.get("elapsed"))).max() .orElse(0);
		 */
		// Filter the failed samples and count them
		Stream<CSVRecord> oFailedSamples = listRcd.stream().filter(rcd -> rcd.get("success").equals("false"));
		double dErrPct = (double) oFailedSamples.count() / (double) iRcdNbr;;

		return new MathMoments(dErrPct, dMean, dVariance);
	}
}
