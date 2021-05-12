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

package kg.apc.jmeter.vizualisers;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

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
import rbourga.apdex.logic.ApdexLogic;

/**
 * Calculates the Apdex score of samplers
 */
public class ApdexScoreCalculatorGui extends AbstractVisualizer implements ActionListener, Clearable {
	// Extends the AbstractVisualizer class because it provides the easiest means to handle SampleResults.

	private static final long serialVersionUID = 1L;
	private static final Logger oLogger = LoggerFactory.getLogger(ApdexScoreCalculatorGui.class);
	ApdexLogic oApdexLogic = new ApdexLogic();

	public static final String WIKIPAGE = "https://github.com/rbourga/jmeter-plugins-2/blob/main/tools/apdexcalculator/src/site/dat/wiki/ApdexScoreCalculator.wiki";
	public String getWikiPage() {
		return WIKIPAGE;
	}
	
	// Objects for the Apdex panel
	private JFormattedTextField oJFormattedTextField_ApdexSatisfiedThreshold;
	private final JLabel oJLabel_ApdexThresholdSetting = new JLabel("Target Threshold T (in seconds):");

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
	public static final String ACTION_CALCULATE = "calculate";
	public static final String ACTION_SAVE = "save";

	// We store the samples in a list and group them by their labels
	private static HashMap<String, List<SampleResult>> mSampleList = new HashMap<String, List<SampleResult>>();

	// Table to save Apdex scoring results
	private JTable oJTable = null;
	private static PowerTableModel oPowerTableModel = null;
	public void createDataModelTable() {
		// TODO add the new column labels to core/org/apache/jmeter/resources/messages.properties files.
		oPowerTableModel = new PowerTableModel(new String[] { JMeterUtils.getResString("sampler label"), // Label
				JMeterUtils.getResString("aggregate_report_count"), // # Samples
				JMeterUtils.getResString("average"), // Average
				JMeterUtils.getResString("aggregate_report_min"), // # Min
				JMeterUtils.getResString("aggregate_report_max"), // # Max
				JMeterUtils.getResString("aggregate_report_error"), // # Error
				"Apdex Value",
				"T", // Target threshold
				"Rating",
				"Small Group" // shows a tick if number of samples < 100
		}, new Class[] { String.class,
				Integer.class,
				Double.class, 
				Double.class,
				Double.class,
				Integer.class,
				Double.class,
				Double.class,
				String.class,
				Boolean.class});
	}
    public void saveDataModelTable() {
		// By default, data saved with comma separated values
		FileWriter _oFileWriter = null;
    	String _sOutputFile = sInputFileDirectoryName + sInputFileBaseName + "_ApdexScore.csv";
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
		collector.clearData();
		mSampleList.clear();
		oPowerTableModel.clearData();		
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
		/*
		 * TODO get the title name (an possibly translations) from the
		 * message.properties file. The files are located in
		 * core/org/apache/jmeter/resources.
		 */
		return this.getClass().getSimpleName();
	}
	
	 @Override
	public String getStaticLabel() {
		return JMeterPluginsUtils.prefixLabel("Apdex Score Calculator");
	}
	
	public Collection<String> getMenuCategories() {
		/*
		 * Adds this visualizer to the Non-Test Elements of the JMeter GUI
		 */
		return Arrays.asList(MenuFactory.NON_TEST_ELEMENTS);
	}

	// Create the GUI
	public ApdexScoreCalculatorGui() {
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
		
		// Panel for Apdex options
		JPanel _oJPanelApdex = new JPanel(new BorderLayout());
		_oJPanelApdex.add(oJLabel_ApdexThresholdSetting, BorderLayout.WEST);
		// Create Apdex text field and setup format
		NumberFormat oNumberFormat_T = NumberFormat.getNumberInstance();
		oNumberFormat_T.setMaximumFractionDigits(1);
		oJFormattedTextField_ApdexSatisfiedThreshold = new JFormattedTextField(oNumberFormat_T);
		oJFormattedTextField_ApdexSatisfiedThreshold.setValue(4); // by default, 4s as per Apdex specs
		oJFormattedTextField_ApdexSatisfiedThreshold.setColumns(4);
		_oJPanelApdex.add(oJFormattedTextField_ApdexSatisfiedThreshold);
		_oJPanelApdex.setBorder(BorderFactory.createTitledBorder("Apdex Calculation Inputs"));
		
		// Panel for selection of file
		oFilePanel = new FilePanel("Read results from file and Calculate Apdex score", EXTS);
		
		// Calculate button
		JPanel _oJPanelCalculate = new JPanel();
		JButton _oJButtonCalculate = new JButton("Calculate");
		_oJButtonCalculate.addActionListener(this);
		_oJButtonCalculate.setActionCommand(ACTION_CALCULATE);
		_oJPanelCalculate.add(_oJButtonCalculate);

		// Grid to display Apdex score of samplers
		createDataModelTable();
		oJTable = new JTable(oPowerTableModel);
		JMeterUtils.applyHiDPI(oJTable);
		oJTable.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(oJTable, new TableCellRenderer[] { null, // Label
				null, // Count
				new MinMaxLongRenderer("#0"),	// Mean
				new MinMaxLongRenderer("#0"),	// Min
				new MinMaxLongRenderer("#0"),	// Max
				new MinMaxLongRenderer("#0"),	// Error
				new NumberRenderer("0.00"),		// Apdex score
				new NumberRenderer("0.00"),		// Threshold
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
		_oVerticalPanel.add(_oJPanelApdex);
		_oVerticalPanel.add(oFilePanel);
		_oVerticalPanel.add(_oJPanelCalculate);
		_oVerticalPanel.add(_oJScrollPane);
		this.add(_oVerticalPanel, BorderLayout.CENTER);
		this.add(_oJPanelSave, BorderLayout.SOUTH);
		// Hide the default file panel of this class as we are using another file panel
		this.getFilePanel().setVisible(false);
	}

	public void actionPerformed(ActionEvent actionEvt) {
		String _sActionCommand = actionEvt.getActionCommand();
		
		switch (_sActionCommand) {
		case ACTION_CALCULATE:			
			// Parse target threshold
			double _fTargetThreshold_T = ((Number) oJFormattedTextField_ApdexSatisfiedThreshold.getValue()).doubleValue();
			if (_fTargetThreshold_T < 0.1) {
				if (bUnitTests) {
					System.out.println("ApdexScoreCalculatorGui_ERROR: Please enter a threshold equal to or greater than 0.1.");
				} else {
					GuiPackage.showErrorMessage("Please enter a threshold equal to or greater than 0.1.", "Apdex Threshold Setting error");
				}
				return;
			}
			// Format the threshold as per Apdex specs
			_fTargetThreshold_T = oApdexLogic.formatThreshold(_fTargetThreshold_T);
						
			// Parse filename
			String _sInputFile = oFilePanel.getFilename();
			if (_sInputFile.isEmpty()) {
				if (bUnitTests) {
					System.out.println("ApdexScoreCalculatorGui_ERROR: file name empty - Please set a filename.");
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
			sInputFileDirectoryName = FilenameUtils.getFullPath(_sInputFile);
			sInputFileBaseName = FilenameUtils.getBaseName(_sInputFile);

			// Clear any statistics from a previous analysis
			// Not called in unit tests so that test data remains for the testing
			if (!bUnitTests) {
				clearData();
			}

			// Now, process the data
			boolean _bApdexResult = computeApdex(_sInputFile, _fTargetThreshold_T);
			if (!_bApdexResult) {
				if (bUnitTests) {
					System.out.println(
							"ApdexScoreCalculatorGui_ERROR: No samplers found - please give some samplers!");
				} else {
					GuiPackage.showErrorMessage("No samplers found in results file - please check your file.",
							"Input file error");
				}
			}
			break;
		case ACTION_SAVE:
			if (oPowerTableModel.getRowCount() == 0) {
				if (bUnitTests) {
					System.out.println("ApdexScoreCalculatorGui_ERROR: Data table empty!");
				} else {
					GuiPackage.showErrorMessage("Data table empty - please perform Calculate before.",
							"Save Table Data error");
				}
				return;
			}
			saveDataModelTable();
			if (bUnitTests) {
				System.out.println("ApdexScoreCalculatorGui_INFO: Stats saved to " + sInputFileBaseName + "_ApdexScore.csv.");
			} else {
				GuiPackage.showInfoMessage("Data saved to " + sInputFileBaseName + "_ApdexScore.csv.", "Save Table Data");
			}
			break;
		default:
			if (bUnitTests) {
				System.out.println("ApdexScoreCalculatorGui_ERROR: unknown action " + _sActionCommand + ".");
			} else {
				oLogger.warn("ApdexScoreCalculatorGui: unknown action " + _sActionCommand + ".");
			}
		}
	}
	
	public boolean computeApdex(String sInputFile, double fSatisfiedTargetSec) {
		// Load the data file using the default collector provided by the AbstractVisualizer class
		collector.setFilename(sInputFile);
		// Set the listener for when called from the DetectorTool
		collector.setListener(this);
		collector.loadExistingFile();
		if (mSampleList.isEmpty()) {
			return false; // Nothing to load, so abort...
		}
		
		// Now process the data points
		long _lSatisfiedThresholdMS = (long) (fSatisfiedTargetSec * 1000);	// Convert to ms as JMeter times are stored in ms
		long _lToleratedThresholdMS = 4 * _lSatisfiedThresholdMS;	// 4xTarget, as per Apdex specs
		for (String _sLabelId : mSampleList.keySet()) {
			List<SampleResult> _aLabelSamples = mSampleList.get(_sLabelId);
			int _iTotalSamples = _aLabelSamples.size();
			
			// Perform calculations
			double _dAverageTime = _aLabelSamples.stream().mapToLong(SampleResult::getTime).average().orElse(0);
			long _lMinTime = _aLabelSamples.stream().mapToLong(SampleResult::getTime).min().orElse(0);
			long _lMaxTime = _aLabelSamples.stream().mapToLong(SampleResult::getTime).max().orElse(0);
			Stream<SampleResult> _oErroredSamplers = _aLabelSamples.stream().filter(x -> x.getErrorCount() != 0);
			long _lErrorCount = _oErroredSamplers.count();
			/*
			 * As per Apdex specs, all server failures must be counted as frustrated regardless of their time.
			 * So we must calculate Apdex only on the successful samples.
			 */
			Stream<SampleResult> _oSuccessfulSamplers = _aLabelSamples.stream().filter(x -> x.getErrorCount() == 0);
			Stream<SampleResult> _oSatisfiedSamplers = _oSuccessfulSamplers.filter(x -> x.getTime() <= _lSatisfiedThresholdMS);	// 0 to T
			long _lSatisfiedCount = _oSatisfiedSamplers.count();
			// A stream can only be used once: reset the stream again
			_oSuccessfulSamplers = _aLabelSamples.stream().filter(x -> x.getErrorCount() == 0);
			Stream<SampleResult> _oToleratingSamplers = _oSuccessfulSamplers.filter(x -> (x.getTime() > _lSatisfiedThresholdMS)
					&& (x.getTime() < _lToleratedThresholdMS));	// T to F
			long _lToleratingCount = _oToleratingSamplers.count();
			BigDecimal _bdApdexScore = new BigDecimal(0.00);
			_bdApdexScore = new BigDecimal((double) (_lSatisfiedCount + (_lToleratingCount / 2.0)) / _iTotalSamples);
			// Round to 2 decimal places as per Apdex specs
			BigDecimal _bdApdexScoreRounded = new BigDecimal(0.00);
			_bdApdexScoreRounded = _bdApdexScore.setScale(2, RoundingMode.HALF_UP);
			
			// Set Rating as per Apdex sppecs
			String _sRating = oApdexLogic.setRating(_bdApdexScoreRounded);

			// Update the statistics table
			Boolean _bSmallGroup = false;
			if (_iTotalSamples < 100) {
				_bSmallGroup = true;
			}
			Object[] _oArrayRowData = { _sLabelId,
					_iTotalSamples,
					Long.valueOf((long) _dAverageTime),
					_lMinTime,
					_lMaxTime,
					_lErrorCount,
					_bdApdexScoreRounded.doubleValue(),
					fSatisfiedTargetSec,
					_sRating,
					_bSmallGroup };
			oPowerTableModel.addRow(_oArrayRowData);
		}
		
		// Repaint the table
		oPowerTableModel.fireTableDataChanged();
		return true;
	}

}
