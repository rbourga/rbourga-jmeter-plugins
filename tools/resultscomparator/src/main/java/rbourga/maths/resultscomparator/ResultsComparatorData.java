package rbourga.maths.resultscomparator;

public class ResultsComparatorData {

	// Fields of this class
	private String _sLabel = "";
	private int _iCountA;
	private int _iCountB;
	private double _fMeanA;
	private double _fMeanB;
	private double _fVarianceA;
	private double _fVarianceB;
	private double _fCohenD;
	private String _sMeanDifference;
	
	// Constructor
	public ResultsComparatorData(String sLabel) {
		_sLabel = sLabel;
		_iCountA = 0;
		_iCountB = 0;
		_fMeanA = 0;
		_fMeanB = 0;
		_fVarianceA = 0;
		_fVarianceB = 0;
		_fCohenD = 0;
		_sMeanDifference = "";
	}
	
	// Methods
	public int getCountA() {
		return _iCountA;
	}
	public void setCountA(int iSize) {
		_iCountA = iSize;
	}

	public int getCountB() {
		return _iCountB;
	}
	public void setCountB(int iSize) {
		_iCountB = iSize;
	}

	public double getMeanA() {
		return _fMeanA;
	}
	public void setMeanA(double fMean) {
		_fMeanA = fMean;
	}

	public double getMeanB() {
		return _fMeanB;
	}
	public void setMeanB(double fMean) {
		_fMeanB = fMean;
	}

	public double getVarianceA() {
		return _fVarianceA;
	}
	public void setVarianceA(double fVariance) {
		_fVarianceA = fVariance;
	}

	public double getVarianceB() {
		return _fVarianceB;
	}
	public void setVarianceB(double fVariance) {
		_fVarianceB = fVariance;
	}

	public void setCohenD(double fCohenD) {
		_fCohenD = fCohenD;
	}

	public void setMeanDifference(double fCohenD) {
		// 1. Get direction of movement
		String _sDirection = "";
		if (fCohenD < 0) _sDirection = "decrease";
		else if (fCohenD > 0) _sDirection = "increase";

		// 2. Get magnitude of movement according to Sawilowsky's rule of thumb
		double _fAbsCohenD = Math.abs(fCohenD);
		String _sMagnitude = "Similar";
		if (_fAbsCohenD >= 2.0) _sMagnitude = "Huge";
		else if (_fAbsCohenD >= 1.20) _sMagnitude = "Very large";
		else if (_fAbsCohenD >= 0.80) _sMagnitude = "Large";
		else if (_fAbsCohenD >= 0.50) _sMagnitude = "Medium";
		else if (_fAbsCohenD >= 0.02) _sMagnitude = "Small";
		else if (_fAbsCohenD >= 0.01) _sMagnitude = "Very small";
		else if (_fAbsCohenD > 0.0) _sMagnitude = "Negligeable";
		
		_sMeanDifference = _sMagnitude + " " + _sDirection;
	}
	
	public static double calculatePooledSD(int n1, double variance1, int n2, double variance2) {
		// returns Pooled standard deviation, as per specs
		double _s = Math.sqrt(((n1 - 1) * variance1 + (n2 - 1) * variance2) / (n1 + n2 - 2));
		return _s;
	}
	
	public static double calculateCohensD(double mean1, double mean2, double pooledSD) {
		// returns Cohen's d, as per specs
		double _d = (mean2 - mean1) / pooledSD;
		return _d;
	}

}
