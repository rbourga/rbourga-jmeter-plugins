package rbourga.maths.moments;

import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.samplers.SampleResult;

public class ResultsComparatorMoments {
	
	// Variables
	private double Mean;
	private double Variance;
	
	// Constructor
	public ResultsComparatorMoments (double fMean, double fVariance) {
		this.setMean(fMean);
		this.setVariance(fVariance);
	}

	public double getMean() {
		return Mean;
	}
	public void setMean(double mean) {
		Mean = mean;
	}
	
	public double getVariance() {
		return Variance;
	}
	public void setVariance(double variance) {
		Variance = variance;
	}

    public static ResultsComparatorMoments createMomentsFromSamplesList(List<SampleResult> listSamples) {
    	//  variance = sum((x_i - mean)^2) / (n - 1)
		double _dMean = listSamples.stream().mapToLong(SampleResult::getTime).average().orElse(0.0);
		double _dVariance = listSamples.stream().mapToDouble(SampleResult::getTime).map(t -> Math.pow(t - _dMean, 2)).sum() / listSamples.size();
        return new ResultsComparatorMoments(_dMean, _dVariance);
    }

    public static ResultsComparatorMoments createMomentsFromMeansList(ArrayList<Double> listMeans) {
    	//  variance = sum((x_i - mean)^2) / (n - 1)
		double _dMean = listMeans.stream().mapToDouble(m -> m).average().orElse(0.0);
		double _dVariance = listMeans.stream().mapToDouble(m -> m).map(m -> Math.pow(m - _dMean, 2)).sum() / listMeans.size();
        return new ResultsComparatorMoments(_dMean, _dVariance);
    }

}
