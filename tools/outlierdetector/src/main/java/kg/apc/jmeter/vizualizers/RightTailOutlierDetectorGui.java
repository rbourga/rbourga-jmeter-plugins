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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jorphan.gui.NumberRenderer;
import org.apache.jorphan.gui.RendererUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kg.apc.jmeter.JMeterPluginsUtils;
import rbourga.common.HTMLSaveService;
import rbourga.maths.outlierdetection.UpperFenceOutlierDetector;

/**
 * Considers all values greater than (Q3 + k * IQR) as outliers.
 */
public class RightTailOutlierDetectorGui extends AbstractVisualizer implements ActionListener, Clearable {
	// Extends the AbstractVisualizer class because it provides the easiest means to handle SampleResults.

	private static final long serialVersionUID = 1L;
	private static final Logger oLogger = LoggerFactory.getLogger(RightTailOutlierDetectorGui.class);

	// URL for project Wiki page
	public static final String WIKIPAGE = "https://github.com/rbourga/jmeter-plugins-2/blob/main/tools/outlierdetector/src/site/dat/wiki/RightTailOutlierDetection.wiki";
	public String getWikiPage() {
		return WIKIPAGE;
	}
	
	// Buttons for Tukey's Control Panel
	private final JRadioButton oJRadioButton_1_5 = new JRadioButton("1.5 (detect all outliers)", false);
	private final JRadioButton oJRadioButton_3 = new JRadioButton("3 (detect only extreme values)", true);
	private JFormattedTextField oJFormattedTextField_MaxTrimPct;
	private final JLabel oJLabel_MaxTrimPct = new JLabel("Max. Trim. Percentage [0..1] ");

	// Objects for the File Panel selector
	private FilePanel oFilePanel;
	public FilePanel getInputFilePanel() {
		return oFilePanel;
	}
	// File extensions that are authorized in the File Panel filter
	private static String sInputFileDirectoryName; 
	private static String sInputFileBaseName;
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };

	// Strings associated with the actions of the control buttons in the UI
	public static final String ACTION_DETECT = "detect";
	public static final String ACTION_SAVE = "save";

	// We store the samples in a list and group them by their labels
	private static List<SampleResult> aOutlierList = new ArrayList<SampleResult>();
	private static HashMap<String, List<SampleResult>> mSampleList = new HashMap<String, List<SampleResult>>();

	// Table to save some stats results of the trimming process
	private static PowerTableModel oPowerTableModel = null;
	public void createDataModelTable() {
		// TODO add the new column labels to core/org/apache/jmeter/resources/messages.properties files.
		oPowerTableModel = new PowerTableModel(new String[] { JMeterUtils.getResString("sampler label"), // Label
				JMeterUtils.getResString("aggregate_report_count"), // # Samples
				"Upper Fence", "# Trimmed", // number of samples that have been discarded
				"Trimmed %", // number of samples that have been discarded as a percentage
				"Small Group", // shows a tick if remaining number of samples < 100
				"Failed" // shows a tick if pct trimmed more than the specified threshold
		}, new Class[] { String.class, Integer.class, Integer.class, Integer.class, Double.class, Boolean.class, Boolean.class });
	}
	private int iNumberOfTotalObjectsTrimmed = 0;
	private int iFailedClnNbr = 6;
    public void saveDataModelTableAsCsv() {
		// By default, data saved with comma separated values
		FileWriter _oFileWriter = null;
    	String _sOutputFile = sInputFileDirectoryName + sInputFileBaseName + "_TrimSummary.csv";
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
    public void saveDataModelTableAsHtml(String sMaxTrimPct) {
		// Saves trimming results in an HTML file for import in DevOps tool later on
		FileWriter _oFileWriter = null;
		String _sOutputFile = sInputFileDirectoryName + sInputFileBaseName + "_TrimSummary.html";
		try {
			_oFileWriter = new FileWriter(_sOutputFile);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			HTMLSaveService.saveHTMLtable("Right Tail Outliers Trimming Results (accepted max trim pct: " + sMaxTrimPct + ")", oPowerTableModel, _oFileWriter, iFailedClnNbr);
			_oFileWriter.close();
		} catch (IOException ioE) {
			ioE.printStackTrace();
		}
	}

	// Table for displaying the results of trimming
	private JTable oJTable = null;

	private PrintWriter initializeFileOutput(String sFilename) {
		PrintWriter _oPrintWriter = null;
		try {
			_oPrintWriter = new PrintWriter(sFilename);
			// Save the header line
			_oPrintWriter.println(CSVSaveService.printableFieldNamesToString());
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		return _oPrintWriter;
	}

	// Used only in unit tests
	private Boolean bUnitTests = false;
	public void enableUnitTests() {
		bUnitTests = true;
	}

	@Override
	public void add(SampleResult sample) {
		/*
		 * Called by JMeter's engine when we load the data file (but not when from TestJMeterUtils).
		 * We add the samples to mSampleList with label as key.
		 */
		String _sLabelId = sample.getSampleLabel();
		if (!mSampleList.containsKey(_sLabelId)) {
			// New label sample
			mSampleList.put(_sLabelId, new ArrayList<SampleResult>());
		}
		// Use the default config of this.collector as save properties for the sample
		sample.setSaveConfig(collector.getSaveConfig());
		mSampleList.get(_sLabelId).add(sample);		
	}

	@Override
	public void clearData() {
		/* Called when user clicks on "Clear" or "Clear All" buttons.
		 * Clears data specific to this plugin
		 */
		iNumberOfTotalObjectsTrimmed = 0;
		aOutlierList.clear();
		collector.clearData();
		mSampleList.clear();
		oPowerTableModel.clearData();		
	}

	public void clearGui() {
		/*
		 * Called when user selects the plugin in the tree Call default clear method
		 */
		super.clearGui();
	}

	@Override
	public String getLabelResource() {
		/*
		 * TODO get the title name (an possibly translations) from the
		 * message.properties file. The files are located in
		 * core/org/apache/jmeter/resources.
		 */
		return this.getClass().getSimpleName();
	}
	
	 @Override
	public String getStaticLabel() {
		return JMeterPluginsUtils.prefixLabel("Right Tail Outlier Detection");
	}
	
	public Collection<String> getMenuCategories() {
		/*
		 * Adds this visualizer to the Non-Test Elements of the JMeter GUI
		 */
		return Arrays.asList(MenuFactory.NON_TEST_ELEMENTS);
	}

	// Create the GUI
	public RightTailOutlierDetectorGui() {
		super();

		// Use standard JMeter border
		this.setLayout(new BorderLayout());
		this.setBorder(makeBorder());

		// Add title
		//this.add(makeTitlePanel(), BorderLayout.NORTH);
		// Add title and help link
		this.add(JMeterPluginsUtils.addHelpLinkToPanel(makeTitlePanel(), WIKIPAGE), BorderLayout.NORTH);

		// Create a vertical panel layout scheme to hold the different panels of the UI
		JPanel _oVerticalPanel = new VerticalPanel();

		// Panel for Tukey's options
		JPanel _oJPanelTukey = new JPanel(new BorderLayout());
		ButtonGroup _oButtonGroup = new ButtonGroup();
		_oButtonGroup.add(oJRadioButton_1_5);
		_oButtonGroup.add(oJRadioButton_3);
		_oJPanelTukey.setLayout(new FlowLayout());
		_oJPanelTukey.add(oJRadioButton_1_5);
		_oJPanelTukey.add(oJRadioButton_3);
		_oJPanelTukey.setBorder(BorderFactory.createTitledBorder("Tukey's constant k"));

		// Panel for Failure criteria option
		JPanel _oJPanelFail = new JPanel(new BorderLayout());
		NumberFormat oNumberFormat_MaxPct = NumberFormat.getNumberInstance();
		oNumberFormat_MaxPct.setMaximumFractionDigits(2);
		// Create Trim Pass/Fail text field and setup format
		_oJPanelFail.add(oJLabel_MaxTrimPct, BorderLayout.WEST);
		oJFormattedTextField_MaxTrimPct = new JFormattedTextField(oNumberFormat_MaxPct);
		oJFormattedTextField_MaxTrimPct.setValue(0.10); // by default, 10% max trimmed
		oJFormattedTextField_MaxTrimPct.setColumns(4);
		_oJPanelFail.add(oJFormattedTextField_MaxTrimPct);
		_oJPanelFail.setBorder(BorderFactory.createTitledBorder("Failure Criteria Specification"));

		// Panel for selection of file
		oFilePanel = new FilePanel("Read results from file and Detect outliers in right tail", EXTS);

		// Detect button
		JPanel _oJPanelDetect = new JPanel();
		JButton _oJButtonDetect = new JButton("Detect");
		_oJButtonDetect.addActionListener(this);
		_oJButtonDetect.setActionCommand(ACTION_DETECT);
		_oJPanelDetect.add(_oJButtonDetect);

		// Grid for some statistics results after the trimming of samplers
		createDataModelTable();
		oJTable = new JTable(oPowerTableModel);
		JMeterUtils.applyHiDPI(oJTable);
		oJTable.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(oJTable, new TableCellRenderer[] { null, // Label
				null, // Count
				null, // Upper Fence
				null, // Trim count
				new NumberRenderer("#0.00%"), // Trim %
				null }); // Low Sample indicator
		// Create the scroll pane and add the table to it
		JScrollPane _oJScrollPane = new JScrollPane(oJTable);

		// Save Table button
		JPanel _oJPanelSave = new JPanel();
		JButton _oJButtonSave = new JButton(JMeterUtils.getResString("aggregate_graph_save_table"));
		_oJButtonSave.addActionListener(this);
		_oJButtonSave.setActionCommand(ACTION_SAVE);
		_oJPanelSave.add(_oJButtonSave, BorderLayout.CENTER);

		// Finally, assemble all panels
		_oVerticalPanel.add(_oJPanelTukey);
		_oVerticalPanel.add(_oJPanelFail);
		_oVerticalPanel.add(oFilePanel);
		_oVerticalPanel.add(_oJPanelDetect);
		_oVerticalPanel.add(_oJScrollPane);
		this.add(_oVerticalPanel, BorderLayout.CENTER);
		this.add(_oJPanelSave, BorderLayout.SOUTH);
		// Hide the default file panel of this class as we are using another file panel
		this.getFilePanel().setVisible(false);
	}
	
	public void actionPerformed(ActionEvent actionEvt) {
		double _fMaxTrimPct;
		String _sActionCommand = actionEvt.getActionCommand();
		switch (_sActionCommand) {
		case ACTION_DETECT:
			double _kValue;

			// Parse Pct value
			_fMaxTrimPct = ((Number) oJFormattedTextField_MaxTrimPct.getValue()).doubleValue();
			if ((_fMaxTrimPct < 0) || (_fMaxTrimPct > 1)) {
				if (bUnitTests) {
					System.out.println("RightTailOutlierDetectorGUI_ERROR: Please enter a percentage between 0 and 1.");
				} else {
					GuiPackage.showErrorMessage("Please enter a percentage value between 0 and 1.", "Max Percentage Trimming Setting error");
				}
				return;
			}

			// Parse filename
			String _sInputFile = oFilePanel.getFilename();
			if (_sInputFile.isEmpty()) {
				if (bUnitTests) {
					System.out.println("RightTailOutlierDetectorGUI_ERROR: file name empty - Please set a filename.");
				} else {
					GuiPackage.showErrorMessage("File name empty - please enter a filename.", "Input file error");
				}
				return;
			}
			if (!bUnitTests) {
				if (!(new File(_sInputFile).exists())) {
					GuiPackage.showErrorMessage("Cannot find specified file - please enter a valid filename.", "Input file error");
					return;
				}
			}

			// Get Tukey's option that was selected
			if (oJRadioButton_1_5.isSelected()) {
				_kValue = 1.5;
			} else {
				_kValue = 3.0;
			}

			// Clear any statistics from a previous analysis
			// Not called in unit tests so that test data remains for the testing
			if (!bUnitTests) {
				clearData();
			}

			// Now, process the data
			int _iTrimResult = outlierDetection(_sInputFile, _kValue, _fMaxTrimPct);
			switch (_iTrimResult) {
			case -1:
				if (bUnitTests) {
					System.out.println(
							"RightTailOutlierDetectorGUI_ERROR: No samplers found - please give some samplers!");
				} else {
					GuiPackage.showErrorMessage("No samplers found in results file - please check your file.",
							"Input file error");
				}
				break;
			case 0:
				switch (iNumberOfTotalObjectsTrimmed) {
				case 0:
					if (bUnitTests) {
						System.out.println("RightTailOutlierDetectorGUI_INFO: Done, no outliers found.");
					} else {
						GuiPackage.showInfoMessage("No outliers found in the right tail.", "Right Tail Outlier Detection");
					}
					break;
				default:
					if (bUnitTests) {
						System.out.println("RightTailOutlierDetectorGUI_INFO: Done, " + iNumberOfTotalObjectsTrimmed
								+ " outliers found in the right tail.\n" + "Refer to " + sInputFileBaseName	+ " _Outliers and _Trimmed files.");
					} else {
						GuiPackage.showInfoMessage(iNumberOfTotalObjectsTrimmed + " outliers found in the right tail.\n" + "Refer to " + sInputFileBaseName	+" _Outliers and _Trimmed files.", "Right Tail Outlier Detection");
					}
					break;
				 }
				break;
			default:
				if (bUnitTests) {
					System.out.println("RightTailOutlierDetectorGUI_WARN: Done, " + _iTrimResult
							+ " elements exceed trimming pct threshold.\n" + "Refer to " + sInputFileBaseName	+ " _Outliers and _Trimmed files.");
				} else {
					GuiPackage.showWarningMessage(_iTrimResult + " elements exceed trimming threshold.\n" + "Refer to " + sInputFileBaseName	+" _Outliers and _Trimmed files.", "Right Tail Outlier Detection");
				}
			}
			break;
		case ACTION_SAVE:
			if (oPowerTableModel.getRowCount() == 0) {
				if (bUnitTests) {
					System.out.println("RightTailOutlierDetectorGUI_ERROR: Data table empty!");
				} else {
					GuiPackage.showErrorMessage("Data table empty - please perform Detect before.",
							"Save Table Data error");
				}
				return;
			}
			saveDataModelTableAsCsv();
			if (bUnitTests) {
				System.out.println("RightTailOutlierDetectorGUI_INFO: Stats saved to " + sInputFileBaseName + "_TrimSummary.csv.");
			} else {
				GuiPackage.showInfoMessage("Data saved to " + sInputFileBaseName + "_TrimSummary.csv.", "Save Table Data");
			}
			break;
		default:
			if (bUnitTests) {
				System.out.println("RightTailOutlierDetectorGUI_ERROR: unknown action " + _sActionCommand + ".");
			} else {
				oLogger.warn("RightTailOutlierDetectorGUI: unknown action " + _sActionCommand + ".");
			}
		}
	}
	
	public int outlierDetection(String sInputFile, double fTukey_K, double fMaxTrimPct) {
		BigDecimal _bdMaxTrimPct = new BigDecimal(fMaxTrimPct);
		int _iNbOfFailedElements = 0;

		UpperFenceOutlierDetector _oUpperFenceOutlierDetector = new UpperFenceOutlierDetector();

		// 1. Load the data file using the default collector provided by the AbstractVisualizer class
		collector.setFilename(sInputFile);
		// Set the listener for when called from RightTailOutlierDetectorTool
		collector.setListener(this);
		collector.loadExistingFile();
		if (mSampleList.isEmpty()) {
			return -1; // Nothing to load, so abort...
		}

		// 2. Now, process the data points...
		for (String _sLabelId : mSampleList.keySet()) {
			List<SampleResult> _aLabelSamples = mSampleList.get(_sLabelId);
			int _iNumberOfObjectsBefore = _aLabelSamples.size();
			double _fUpperFence = 0.0, _fUpperFenceMin = Double.MAX_VALUE;

			// Only look for outliers if there are at least four items to compare
			if (_aLabelSamples.size() > 3) {
				// First, sort the samples by their elapsed time
				_aLabelSamples.sort(Comparator.comparingLong(SampleResult::getTime));
				/*
				 * An outlier can hide another outlier...so when removing extreme values, we
				 * have to iterate until no extreme values are left.
				 */
				boolean _bDataTrimmed = true;
				do {
					// Get the upper fence
					_fUpperFence = _oUpperFenceOutlierDetector.getUpperFence(_aLabelSamples, fTukey_K);
					// Save the most severe limit for the report
					_fUpperFenceMin = Math.min(_fUpperFence, _fUpperFenceMin); 
					// Now remove all samples that are higher than the upper fence
					int _iSampleIndex = _aLabelSamples.size() - 1; // for performance reasons, start by the end
					_bDataTrimmed = false;
					boolean _bSampleTrimmed = true;
					do {
						if (_aLabelSamples.get(_iSampleIndex).getTime() > _fUpperFence) {
							/*
							 * Outlier detected: save the outlier in a new list before removing it from
							 * current list
							 */
							aOutlierList.add(_aLabelSamples.get(_iSampleIndex));
							_aLabelSamples.remove(_iSampleIndex);
							_iSampleIndex--;
							_bDataTrimmed = true;
						} else {
							/*
							 * No more samples to trim: stop parsing the list
							 */
							_bSampleTrimmed = false;
						}
					} while (_bSampleTrimmed);
				} while (_bDataTrimmed);
			}

			// Report the results
			int _iNumberOfObjectsTrimmed = _iNumberOfObjectsBefore - _aLabelSamples.size();
			BigDecimal _bDnumberOfObjectsTrimmedPerCent = new BigDecimal(
					(double) _iNumberOfObjectsTrimmed / (double) _iNumberOfObjectsBefore);
			// Round % to 4 decimal places
			BigDecimal _bDnumberOfObjectsTrimmedPerCentRounded = _bDnumberOfObjectsTrimmedPerCent.setScale(4,
					RoundingMode.HALF_UP);
			iNumberOfTotalObjectsTrimmed = iNumberOfTotalObjectsTrimmed + _iNumberOfObjectsTrimmed;
			Boolean _bSmallGroup = false;
			if (_aLabelSamples.size() < 100) {
				_bSmallGroup = true;
			}
			Boolean _bFailed = false;
			if (_bDnumberOfObjectsTrimmedPerCentRounded.compareTo(_bdMaxTrimPct) >= 0) {
				_bFailed = true;
				_iNbOfFailedElements++;
			}

			// Update the statistics table
			Object[] _oArrayRowData = { _sLabelId, _iNumberOfObjectsBefore, _fUpperFenceMin, _iNumberOfObjectsTrimmed,
					_bDnumberOfObjectsTrimmedPerCentRounded.doubleValue(), _bSmallGroup, _bFailed};
			oPowerTableModel.addRow(_oArrayRowData);
		}

		// Repaint the table
		oPowerTableModel.fireTableDataChanged();

		// Save the non-trimmed results in a file for post stats
		sInputFileDirectoryName = FilenameUtils.getFullPath(sInputFile);
		sInputFileBaseName = FilenameUtils.getBaseName(sInputFile);
		String _sInputFileExtension = FilenameUtils.getExtension(sInputFile);
		// Filename for the "good" samplers only
		String _sOutputFile = sInputFileDirectoryName + sInputFileBaseName + "_Trimmed." + _sInputFileExtension;
		PrintWriter _oPrintWriter = initializeFileOutput(_sOutputFile);
		SampleEvent _oSampleEvent = null;
		for (String _sLabelId : mSampleList.keySet()) {
			List<SampleResult> _aSampleResult = mSampleList.get(_sLabelId);
			for (SampleResult _oSampleResult : _aSampleResult) {
				_oSampleEvent = new SampleEvent(_oSampleResult, null);
				_oPrintWriter.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
			}
		}
		// Close the file
		_oPrintWriter.close();

		// Now save the outliers in a separate file for post analysis
		if (aOutlierList.isEmpty() == false) {
			// Filename containing the excluded samplers only
			_sOutputFile = sInputFileDirectoryName + sInputFileBaseName + "_Outliers." + _sInputFileExtension;
			_oPrintWriter = initializeFileOutput(_sOutputFile);
			for (SampleResult _oSampleResult : aOutlierList) {
				_oSampleEvent = new SampleEvent(_oSampleResult, null);
				_oPrintWriter.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
			}
			// Close the file
			_oPrintWriter.close();
		}
		return _iNbOfFailedElements;
	}
}
