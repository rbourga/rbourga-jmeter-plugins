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
	private String _sMagnitude;
	
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
		_sMagnitude = "";
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

	public void setMagnitude(String sMagnitude) {
		_sMagnitude = sMagnitude;
	}

}
