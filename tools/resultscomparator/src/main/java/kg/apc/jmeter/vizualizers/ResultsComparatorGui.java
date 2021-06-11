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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.DoubleStream;

import javax.swing.JButton;
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
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jorphan.gui.NumberRenderer;
import org.apache.jorphan.gui.RendererUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kg.apc.jmeter.JMeterPluginsUtils;
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
	
	public static final String WIKIPAGE = "https://github.com/rbourga/jmeter-plugins-2/blob/main/tools/resultscomparator/src/site/dat/wiki/ApdexScoreCalculator.wiki";
	
	// Objects for the File Panels selector
	private FilePanel oFilePanelA;
	private FilePanel oFilePanelB;
	private String fileSet;
	public FilePanel getInputFilePanelA() {
		return oFilePanelA;
	}
	public FilePanel getInputFilePanelB() {
		return oFilePanelB;
	}
	// File extensions that are authorized in the File Panel filter
	private static String sInputFileDirectoryNameA; 
	private static String sInputFileBaseNameA;
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };
	
	// Strings associated with the actions of the control buttons in the UI
	public static final String ACTION_COMPARE = "compare";
	public static final String ACTION_SAVE = "save";

	// We store the samples in lists and group them by their labels
	private static HashMap<String, List<SampleResult>> mSampleListA = new HashMap<String, List<SampleResult>>();
	private static HashMap<String, List<SampleResult>> mSampleListB = new HashMap<String, List<SampleResult>>();
	private static HashMap<String, ResultsComparatorData> mComparisonResults = new HashMap<String, ResultsComparatorData>();

	// Table to save some Cohen's d values
	private static PowerTableModel oPowerTableModel = null;
	public void createDataModelTable() {
		// TODO add the new column labels to core/org/apache/jmeter/resources/messages.properties files.
		oPowerTableModel = new PowerTableModel(new String[] { JMeterUtils.getResString("sampler label"), // Label
				"# Samples A", // # Samples
				"# Samples B",
				"Cohen's d", // d
				"Magnitude" // Descriptor
		}, new Class[] { String.class, Integer.class, Integer.class, Double.class, String.class });
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
			
			sInputFileDirectoryNameA = FilenameUtils.getFullPath(_sInputFileA);
			sInputFileBaseNameA = FilenameUtils.getBaseName(_sInputFileA);

			// Clear any comparisons from a previous analysis
			// Not called in unit tests so that test data remains for the testing
			if (!bUnitTests) {
				clearData();
			}
			
			// Now, compare the data
			int _iCompareResult = resultsCompare(_sInputFileA, _sInputFileB);
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
			default:
			}
		case ACTION_SAVE:
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
				new NumberRenderer("0.00"), // Cohen's d
				null }); // Magnitude descriptor
		// Create the scroll pane and add the table to it
		JScrollPane _oJScrollPane = new JScrollPane(oJTable);

		// Save Table button
		JPanel _oJPanelSave = new JPanel();
		JButton _oJButtonSave = new JButton(JMeterUtils.getResString("aggregate_graph_save_table"));
		_oJButtonSave.addActionListener(this);
		_oJButtonSave.setActionCommand(ACTION_SAVE);
		_oJPanelSave.add(_oJButtonSave, BorderLayout.CENTER);

		// Finally, assemble all panels
		_oVerticalPanel.add(oFilePanelA);
		_oVerticalPanel.add(oFilePanelB);
		_oVerticalPanel.add(_oJPanelCompare);
		_oVerticalPanel.add(_oJScrollPane);
		this.add(_oVerticalPanel, BorderLayout.CENTER);
		this.add(_oJPanelSave, BorderLayout.SOUTH);
		// Hide the default file panel of this class as we are using another file panel
		this.getFilePanel().setVisible(false);
	}

	public int resultsCompare(String sInputFileA, String sInputFileB) {		
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
				ResultsComparatorMoments _oResultsComparatorMoments = ResultsComparatorMoments.createMoments(_aLabelSamples);
				_oResultsComparatorData.setMeanA(_oResultsComparatorMoments.getMean());
				_oResultsComparatorData.setVarianceA(_oResultsComparatorMoments.getVariance());
			}
			// Add the results of analysis to hashmap for later reference
			mComparisonResults.put(_sLabelId, _oResultsComparatorData);
		};

		// 3. Then process the Variation data points
		for (String _sLabelId : mSampleListB.keySet()) {
			List<SampleResult> _aLabelSamples = mSampleListB.get(_sLabelId);
			int _iNumberOfSamples = _aLabelSamples.size();
			// Save stats of these samplers for later comparison
			// Is this Sample in the Control file?
			if (mComparisonResults.containsKey(_sLabelId)) {
				// Yes: update the Stats				
				mComparisonResults.get(_sLabelId).setCountB(_iNumberOfSamples);
				if (_iNumberOfSamples >= 2) {
					// Calculate if at least 2 elements
					ResultsComparatorMoments _oResultsComparatorMoments = ResultsComparatorMoments.createMoments(_aLabelSamples);
					mComparisonResults.get(_sLabelId).setMeanB(_oResultsComparatorMoments.getMean());
					mComparisonResults.get(_sLabelId).setVarianceB(_oResultsComparatorMoments.getVariance());
				}
			} else {
				// No: add a new result to the list
				ResultsComparatorData _oResultsComparatorData = new ResultsComparatorData(_sLabelId);
				_oResultsComparatorData.setCountB(_iNumberOfSamples);
				if (_iNumberOfSamples >= 2) {
					// Calculate if at least 2 elements
					ResultsComparatorMoments _oResultsComparatorMoments = ResultsComparatorMoments.createMoments(_aLabelSamples);
					_oResultsComparatorData.setMeanB(_oResultsComparatorMoments.getMean());
					_oResultsComparatorData.setVarianceB(_oResultsComparatorMoments.getVariance());
				}
				// Add the results of analysis to hashmap for later reference
				mComparisonResults.put(_sLabelId, _oResultsComparatorData);
			}
		};
		
		// 4. Finally calculate Cohen's d values for all keys
		for (String _sLabelId : mComparisonResults.keySet() ) {
			// Only calculate if more than 2 samplers on each side
			int _iN1 = mComparisonResults.get(_sLabelId).getCountA();
			int _iN2 = mComparisonResults.get(_sLabelId).getCountB();
			if ((_iN1 >= 2) && (_iN2 >= 2)) {
				double _dS1 = mComparisonResults.get(_sLabelId).getVarianceA();
				double _dS2 = mComparisonResults.get(_sLabelId).getVarianceB();

				// Pooled standard deviation, as per specs
				double _s = Math.sqrt(((_iN1 - 1) * _dS1 + (_iN2 - 1) * _dS2) / (_iN1 + _iN2 - 2));
				// Cohen's d, as per specs
				double _dX1 = mComparisonResults.get(_sLabelId).getMeanA();
				double _dX2 = mComparisonResults.get(_sLabelId).getMeanB();
				double _dcD = (_dX2 - _dX1) / _s;
				mComparisonResults.get(_sLabelId).setCohenD(_dcD);
				
				//Ici: a faire test de magnitude
			}
		}
		
		return 0;
	}

}
