/*
 * Copyright 2021 Robert Bourgault du Coudray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package kg.apc.jmeter.vizualizers;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.util.FilePanel;
import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.gui.util.PowerTableModel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.Clearable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jorphan.gui.MinMaxLongRenderer;
import org.apache.jorphan.gui.NumberRenderer;
import org.apache.jorphan.gui.RendererUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kg.apc.jmeter.JMeterPluginsUtils;
import rbourga.common.HTMLSaveService;
import rbourga.maths.moments.ResultsComparatorMoments;
import rbourga.maths.resultscomparator.ResultsComparatorData;


/**
 * Compares response times of samplers using Cohen's d effect size
 * @author Robert Bourgault du Coudray
 *
 */
public class ResultsComparatorGui extends AbstractVisualizer implements ActionListener, Clearable {
	// Extends the AbstractVisualizer class because it provides the easiest means to handle SampleResults.

	private static final long serialVersionUID = 1L;
	private static final Logger oLogger = LoggerFactory.getLogger(ResultsComparatorGui.class);
	
	public static final String WIKIPAGE = "https://github.com/rbourga/jmeter-plugins-2/blob/main/tools/resultscomparator/src/site/dat/wiki/ResultsComparator.wiki";
	public static final String AverageOfAverages = "OVERALL AVERAGE";
	
	// Objects for the Cohen Treshold panel
	private JFormattedTextField oJFormattedTextField_CohenThreshold;
	private final JLabel oJLabel_CohenThresholdSetting = new JLabel("Cohen's d Threshold to not exceed ");

	// Objects for the File Panels selector
	private FilePanel oFilePanelA;
	private FilePanel oFilePanelB;
	private String fileSet;
	public void setFileSet(String sSetName) {
		fileSet = sSetName;
	}
	public FilePanel getInputFilePanelA() {
		return oFilePanelA;
	}
	public FilePanel getInputFilePanelB() {
		return oFilePanelB;
	}
	// File extensions that are authorized in the File Panel filter
	private static String sInputFileDirectoryNameB; 
	private static String sInputFileBaseNameB;
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };
	
	// Strings associated with the actions of the control buttons in the UI
	public static final String ACTION_COMPARE = "compare";
	public static final String ACTION_SAVE = "save";

	// We store the samples in lists and group them by their labels
	private static HashMap<String, List<SampleResult>> mSampleListA = new HashMap<String, List<SampleResult>>();
	private static HashMap<String, List<SampleResult>> mSampleListB = new HashMap<String, List<SampleResult>>();
	private static HashMap<String, ResultsComparatorData> mComparisonResults = new HashMap<String, ResultsComparatorData>();
	// Variables to store all averages
	private static ArrayList<Double> mAveragesA = new ArrayList<Double>();
	private static ArrayList<Double> mAveragesB = new ArrayList<Double>();

	// Table to save some Cohen's d values
	private static PowerTableModel oPowerTableModel = null;
	public void createDataModelTable() {
		// TODO add the new column labels to core/org/apache/jmeter/resources/messages.properties files.
		oPowerTableModel = new PowerTableModel(new String[] { JMeterUtils.getResString("sampler label"), // Label
				"# Samples A", // # Samples
				"# Samples B",
				"Average A", // Averages
				"Average B",
				"Cohen's d", // d
				"Average Difference", // Descriptor
				"Failed" // shows a tick if Cohen's d exceeds the specified threshold value
		}, new Class[] { String.class,
				Integer.class,
				Integer.class,
				Double.class,
				Double.class,
				Double.class,
				String.class,
				Boolean.class});
	}
	private int iFailedClnNbr = 7;
    public void saveDataModelTableAsCsv() {
		// By default, data saved with comma separated values
		FileWriter _oFileWriter = null;
    	String _sOutputFile = sInputFileDirectoryNameB + sInputFileBaseNameB + "_CompareStats.csv";
		try {
			_oFileWriter = new FileWriter(_sOutputFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			CSVSaveService.saveCSVStats(oPowerTableModel, _oFileWriter);
			_oFileWriter.close();
		} catch (IOException ioE) {
			ioE.printStackTrace();
		}		
	}
    public void saveDataModelTableAsHtml() {
		// Saves comparison results in an HTML file for import in DevOps tool later on
		FileWriter _oFileWriter = null;
    	String _sOutputFile = sInputFileDirectoryNameB + sInputFileBaseNameB + "_CompareStats.html";
		try {
			_oFileWriter = new FileWriter(_sOutputFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			HTMLSaveService.saveHTMLtable("Baseline Comparison Results", oPowerTableModel, _oFileWriter, iFailedClnNbr);
			_oFileWriter.close();
		} catch (IOException ioE) {
			ioE.printStackTrace();
		}		
	}

	// Table for displaying the results of comparison
	private JTable oJTable = null;

	// Used only in unit tests
	private Boolean bUnitTests = false;
	public void enableUnitTests() {
		bUnitTests = true;
	}

	@Override
	public void actionPerformed(ActionEvent actionEvt) {
		String _sActionCommand = actionEvt.getActionCommand();

		switch (_sActionCommand) {
		case ACTION_COMPARE:
			double _fCohenThreshold;
			// Parse Cohen's d threshold
			_fCohenThreshold = ((Number) oJFormattedTextField_CohenThreshold.getValue()).doubleValue();
			if (_fCohenThreshold < 0) {
				if (bUnitTests) {
					System.out.println("ResultsComparatorGui_ERROR: Please enter a threshold equal to or greater than 0.");
				} else {
					GuiPackage.showErrorMessage("Please enter a threshold equal to or greater than 0.", "Cohen's d Threshold Setting error");
				}
				return;
			}

			// Parse filenames
			String _sInputFileA = oFilePanelA.getFilename();
			String _sInputFileB = oFilePanelB.getFilename();
			if (_sInputFileA.isEmpty()) {
				if (bUnitTests) {
					System.out.println("ResultsComparatorGui_ERROR: Control file name empty - Please set a filename.");
				} else {
					GuiPackage.showErrorMessage("Control file name empty - please enter a Control filename.", "Input file error");
				}
				return;
			}
			if (_sInputFileB.isEmpty()) {
				if (bUnitTests) {
					System.out.println("ResultsComparatorGui_ERROR: Variation file name empty - Please set a filename.");
				} else {
					GuiPackage.showErrorMessage("Variation file name empty - please enter a Variation filename.", "Input file error");
				}
				return;
			}
			if (!bUnitTests) {
				if (!(new File(_sInputFileA).exists())) {
					GuiPackage.showErrorMessage("Cannot find Control file - please enter a valid Control filename.", "Input file error");
					return;
				}
				if (!(new File(_sInputFileB).exists())) {
					GuiPackage.showErrorMessage("Cannot find Variation file - please enter a valid Variation filename.", "Input file error");
					return;
				}
			}

			// Clear any comparisons from a previous analysis
			// Not called in unit tests so that test data remains for the testing
			if (!bUnitTests) {
				clearData();
			}
			
			// Now, compare the data
			int _iCompareResult = resultsCompare(_sInputFileA, _sInputFileB, _fCohenThreshold);
			switch (_iCompareResult) {
			case -1:
				if (bUnitTests) {
					System.out.println(
						"ResultsComparatorGui_ERROR: No samplers found in Control file - please give some samplers!");
				} else {
					GuiPackage.showErrorMessage("No samplers found in Control file - please check your file.",
						"Input file error");
				}
				break;
			case -2:
				if (bUnitTests) {
					System.out.println(
						"ResultsComparatorGui_ERROR: No samplers found in Variation file - please give some samplers!");
				} else {
					GuiPackage.showErrorMessage("No samplers found in Variation file - please check your file.",
						"Input file error");
				}
				break;
			}
			break;
		case ACTION_SAVE:
			if (oPowerTableModel.getRowCount() == 0) {
				if (bUnitTests) {
					System.out.println("ResultsComparatorGui_ERROR: Data table empty!");
				} else {
					GuiPackage.showErrorMessage("Data table empty - please perform Compare before.",
							"Save Table Data error");
				}
				return;
			}
			saveDataModelTableAsCsv();
			if (bUnitTests) {
				System.out.println("ResultsComparatorGui_INFO: Stats saved to " + sInputFileBaseNameB + "_CompareStats.csv.");
			} else {
				GuiPackage.showInfoMessage("Data saved to " + sInputFileBaseNameB + "_CompareStats.csv.", "Save Table Data");
			}
			break;
		default:
			if (bUnitTests) {
				System.out.println("ResultsComparatorGui_ERROR: unknown action " + _sActionCommand + ".");
			} else {
				oLogger.warn("ResultsComparatorGui: unknown action " + _sActionCommand + ".");
			}
		}
	}
	
	@Override
	public void add(SampleResult sample) {
		/*
		 * Called by JMeter's engine when we load the data files (but not when from TestJMeterUtils).
		 * We add the samples to mSampleList with label as key.
		 */
		String _sLabelId = sample.getSampleLabel();
		// Use the default config of this.collector as save properties for the sample
		sample.setSaveConfig(collector.getSaveConfig());
		if (fileSet.equals("A")) {
			// Loading Control file...
			if (!mSampleListA.containsKey(_sLabelId)) {
				// New label sample
				mSampleListA.put(_sLabelId, new ArrayList<SampleResult>());
			}
			mSampleListA.get(_sLabelId).add(sample);		
		} else {
			// Loading Variation file
			if (!mSampleListB.containsKey(_sLabelId)) {
				// New label sample
				mSampleListB.put(_sLabelId, new ArrayList<SampleResult>());
			}
			mSampleListB.get(_sLabelId).add(sample);		
		}
	}
	
	@Override
	public void clearGui() {
		/*
		 * Called when user selects the plugin in the tree Call default clear method
		 */
		super.clearGui();
	}

	@Override
	public String getLabelResource() {
		return this.getClass().getSimpleName();
	}

	 @Override
	public String getStaticLabel() {
		return JMeterPluginsUtils.prefixLabel("Results Comparator");
	}
	 
	 public Collection<String> getMenuCategories() {
	/*
	 * Adds this visualizer to the Non-Test Elements of the JMeter GUI
	 */
		return Arrays.asList(MenuFactory.NON_TEST_ELEMENTS);
	}


	@Override
	public void clearData() {
		/* Called when user clicks on "Clear" or "Clear All" buttons.
		 * Clears data specific to this plugin
		 */
		collector.clearData();
		mSampleListA.clear();
		mSampleListB.clear();
		mAveragesA.clear();
		mAveragesB.clear();
		mComparisonResults.clear();
		oPowerTableModel.clearData();			
	}

	// Create the GUI
	public ResultsComparatorGui() {
		super();
		
		// Use standard JMeter border
		this.setLayout(new BorderLayout());
		this.setBorder(makeBorder());

		// Add title and help link
		this.add(JMeterPluginsUtils.addHelpLinkToPanel(makeTitlePanel(), WIKIPAGE), BorderLayout.NORTH);
		
		// Create a vertical panel layout scheme to hold the different panels of the UI
		JPanel _oVerticalPanel = new VerticalPanel();
		
		// Panel for Failure criteria option
		JPanel _oJPanelFail = new JPanel(new BorderLayout());
		_oJPanelFail.add(oJLabel_CohenThresholdSetting, BorderLayout.WEST);
		// Create Cohen text field and setup format
		NumberFormat oNumberFormat_T = NumberFormat.getNumberInstance();
		oNumberFormat_T.setMaximumFractionDigits(2);
		oJFormattedTextField_CohenThreshold = new JFormattedTextField(oNumberFormat_T);
		oJFormattedTextField_CohenThreshold.setValue(1.20); // by default, Very large or more will be marked as failed
		oJFormattedTextField_CohenThreshold.setColumns(4);
		_oJPanelFail.add(oJFormattedTextField_CohenThreshold);
		_oJPanelFail.setBorder(BorderFactory.createTitledBorder("Failure Criteria Specification"));
		
		// Panels for selection of files
		oFilePanelA = new FilePanel("Control File (A)", EXTS);
		oFilePanelB = new FilePanel("Variation File (B)", EXTS);

		// Compare button
		JPanel _oJPanelCompare = new JPanel();
		JButton _oJButtonCompare = new JButton("Compare");
		_oJButtonCompare.addActionListener(this);
		_oJButtonCompare.setActionCommand(ACTION_COMPARE);
		_oJPanelCompare.add(_oJButtonCompare);
		
		// Grid to display Effect size results
		createDataModelTable();
		oJTable = new JTable(oPowerTableModel);
		JMeterUtils.applyHiDPI(oJTable);
		oJTable.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(oJTable, new TableCellRenderer[] { null, // Label
				null, // Count A
				null, // Count B
				new MinMaxLongRenderer("#0"),	// Mean A
				new MinMaxLongRenderer("#0"),	// Mean B
				new NumberRenderer("0.00"), // Cohen's d
				null,                       // Magnitude descriptor
				null});                    // Pass/Failed indicator
		// Create the scroll pane and add the table to it
		JScrollPane _oJScrollPane = new JScrollPane(oJTable);

		// Save Table button
		JPanel _oJPanelSave = new JPanel();
		JButton _oJButtonSave = new JButton(JMeterUtils.getResString("aggregate_graph_save_table"));
		_oJButtonSave.addActionListener(this);
		_oJButtonSave.setActionCommand(ACTION_SAVE);
		_oJPanelSave.add(_oJButtonSave, BorderLayout.CENTER);

		// Finally, assemble all panels
		_oVerticalPanel.add(_oJPanelFail);
		_oVerticalPanel.add(oFilePanelA);
		_oVerticalPanel.add(oFilePanelB);
		_oVerticalPanel.add(_oJPanelCompare);
		_oVerticalPanel.add(_oJScrollPane);
		this.add(_oVerticalPanel, BorderLayout.CENTER);
		this.add(_oJPanelSave, BorderLayout.SOUTH);
		// Hide the default file panel of this class as we are using another file panel
		this.getFilePanel().setVisible(false);
	}

	public int resultsCompare(String sInputFileA, String sInputFileB, double fThld) {
		ResultsComparatorMoments _oResultsComparatorMoments = null;

		sInputFileDirectoryNameB = FilenameUtils.getFullPath(sInputFileB);
		sInputFileBaseNameB = FilenameUtils.getBaseName(sInputFileB);

		// Set the listener for when called from ResultsComparatorTool
		collector.setListener(this);
		
		// 1. Load the Control file using the default collector provided by the AbstractVisualizer class
		fileSet = "A";
		collector.setFilename(sInputFileA);
		collector.loadExistingFile();
		if (mSampleListA.isEmpty()) {
			return -1; // Nothing in Control file, so abort...
		}
		// 2. Load the Variation file
		fileSet = "B";
		collector.setFilename(sInputFileB);
		collector.loadExistingFile();
		if (mSampleListB.isEmpty()) {
			return -2; // Nothing in Variation file, so abort...
		}
		
		// 2. First, process the Control data points
		for (String _sLabelId : mSampleListA.keySet()) {
			List<SampleResult> _aLabelSamples = mSampleListA.get(_sLabelId);
			int _iNumberOfSamples = _aLabelSamples.size();
			// Save stats of these samplers for later comparison
			ResultsComparatorData _oResultsComparatorData = new ResultsComparatorData(_sLabelId);
			_oResultsComparatorData.setCountA(_iNumberOfSamples);
			if (_iNumberOfSamples >= 2) {
				// Calculate if at least 2 elements
				_oResultsComparatorMoments = ResultsComparatorMoments.createMomentsFromSamplesList(_aLabelSamples);
				_oResultsComparatorData.setMeanA(_oResultsComparatorMoments.getMean());
				_oResultsComparatorData.setVarianceA(_oResultsComparatorMoments.getVariance());
				// Save this mean for later processing
				mAveragesA.add(_oResultsComparatorMoments.getMean());
			}
			// Add the results of analysis to hashmap for later reference
			mComparisonResults.put(_sLabelId, _oResultsComparatorData);
		};

		// 3. Then process the Variation data points
		for (String _sLabelId : mSampleListB.keySet()) {
			List<SampleResult> _aLabelSamples = mSampleListB.get(_sLabelId);
			int _iNumberOfSamples = _aLabelSamples.size();
			// Save stats of these samplers for later comparison
			if (_iNumberOfSamples >= 2) {
				// Calculate if at least 2 elements
				_oResultsComparatorMoments = ResultsComparatorMoments.createMomentsFromSamplesList(_aLabelSamples);
				// Save this mean for later processing
				mAveragesB.add(_oResultsComparatorMoments.getMean());
			}
			// Is this Sample in the Control file?
			if (mComparisonResults.containsKey(_sLabelId)) {
				// Yes: update the Stats				
				mComparisonResults.get(_sLabelId).setCountB(_iNumberOfSamples);
				if (_iNumberOfSamples >= 2) {
					mComparisonResults.get(_sLabelId).setMeanB(_oResultsComparatorMoments.getMean());
					mComparisonResults.get(_sLabelId).setVarianceB(_oResultsComparatorMoments.getVariance());
				}
			} else {
				// No: add a new result to the list
				ResultsComparatorData _oResultsComparatorData = new ResultsComparatorData(_sLabelId);
				_oResultsComparatorData.setCountB(_iNumberOfSamples);
				if (_iNumberOfSamples >= 2) {
					_oResultsComparatorData.setMeanB(_oResultsComparatorMoments.getMean());
					_oResultsComparatorData.setVarianceB(_oResultsComparatorMoments.getVariance());
				}
				// Add the results of analysis to hashmap for later reference
				mComparisonResults.put(_sLabelId, _oResultsComparatorData);
			}
		};
		
		// 4. Calculate Cohen's d values for all keys and set the difference between means
		for (String _sLabelId : mComparisonResults.keySet() ) {
			// Only calculate if more than 2 samplers on each side
			int _iN1 = mComparisonResults.get(_sLabelId).getCountA();
			int _iN2 = mComparisonResults.get(_sLabelId).getCountB();
			if ((_iN1 >= 2) && (_iN2 >= 2)) {
				double _dS1 = mComparisonResults.get(_sLabelId).getVarianceA();
				double _dS2 = mComparisonResults.get(_sLabelId).getVarianceB();
				double _s = ResultsComparatorData.calculatePooledSD(_iN1, _dS1, _iN2, _dS2);
				double _dX1 = mComparisonResults.get(_sLabelId).getMeanA();
				double _dX2 = mComparisonResults.get(_sLabelId).getMeanB();
				double _dcD = ResultsComparatorData.calculateCohensD(_dX1, _dX2, _s);
				mComparisonResults.get(_sLabelId).setCohenD(_dcD);
				mComparisonResults.get(_sLabelId).setMeanDifference(_dcD);
			}
		}

		// 5. Update the statistics table for the UI in natural order
		TreeMap<String, ResultsComparatorData> mComparisonResultsSorted = new TreeMap<>(mComparisonResults);
		int _iNbOfFailedElements = 0;
		for (String _sLabelId : mComparisonResultsSorted.keySet() ) {
			Boolean _bFailed = false;
			double _dCd = mComparisonResultsSorted.get(_sLabelId).getCohenD().doubleValue();
			if (_dCd >=  fThld) {
				_bFailed = true;
				_iNbOfFailedElements++;
			}
			Object[] _oArrayRowData = { _sLabelId,
					mComparisonResultsSorted.get(_sLabelId).getCountA(),
					mComparisonResultsSorted.get(_sLabelId).getCountB(),
					Long.valueOf((long)mComparisonResultsSorted.get(_sLabelId).getMeanA()),
					Long.valueOf((long)mComparisonResultsSorted.get(_sLabelId).getMeanB()),
					Math.abs(mComparisonResultsSorted.get(_sLabelId).getCohenD().doubleValue()),
					mComparisonResultsSorted.get(_sLabelId).getMeanDifference(),
					_bFailed};
			oPowerTableModel.addRow(_oArrayRowData);
		}
		
		// 6. Add last line for a global comparison of averages between A and B
		ResultsComparatorData _oResultsComparatorData = new ResultsComparatorData(AverageOfAverages);
		int _iN1 = mAveragesA.size();
		int _iN2 = mAveragesB.size();
		_oResultsComparatorData.setCountA(_iN1);
		_oResultsComparatorData.setCountB(_iN2);
		if (_iN1 >= 2) {
			_oResultsComparatorMoments = ResultsComparatorMoments.createMomentsFromMeansList(mAveragesA);
			_oResultsComparatorData.setMeanA(_oResultsComparatorMoments.getMean());
			_oResultsComparatorData.setVarianceA(_oResultsComparatorMoments.getVariance());
		}
		if (_iN2 >= 2) {
			_oResultsComparatorMoments = ResultsComparatorMoments.createMomentsFromMeansList(mAveragesB);
			_oResultsComparatorData.setMeanB(_oResultsComparatorMoments.getMean());
			_oResultsComparatorData.setVarianceB(_oResultsComparatorMoments.getVariance());
		}
		if ((_iN1 >= 2) && (_iN2 >= 2)) {
			double _dS1 = _oResultsComparatorData.getVarianceA();
			double _dS2 = _oResultsComparatorData.getVarianceB();
			double _s = ResultsComparatorData.calculatePooledSD(_iN1, _dS1, _iN2, _dS2);
			double _dX1 = _oResultsComparatorData.getMeanA();
			double _dX2 = _oResultsComparatorData.getMeanB();
			double _dcD = ResultsComparatorData.calculateCohensD(_dX1, _dX2, _s);
			_oResultsComparatorData.setCohenD(_dcD);
			_oResultsComparatorData.setMeanDifference(_dcD);
		}
		// Add result to statistics table
		Boolean _bFailed = false;
		double _dCd = _oResultsComparatorData.getCohenD().doubleValue();
		if (_dCd >=  fThld) {
			_bFailed = true;
			_iNbOfFailedElements++;
		}
		Object[] _oArrayRowData = { AverageOfAverages,
				_iN1,
				_iN2,
				Long.valueOf((long)_oResultsComparatorData.getMeanA()),
				Long.valueOf((long)_oResultsComparatorData.getMeanB()),
				Math.abs(_oResultsComparatorData.getCohenD().doubleValue()),
				_oResultsComparatorData.getMeanDifference(),
				_bFailed};
		oPowerTableModel.addRow(_oArrayRowData);

		// Repaint the table
		oPowerTableModel.fireTableDataChanged();

		return _iNbOfFailedElements;
	}

}
