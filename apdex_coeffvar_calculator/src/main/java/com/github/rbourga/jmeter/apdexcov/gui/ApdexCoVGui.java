/**
 * 
 */
package com.github.rbourga.jmeter.apdexcov.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.util.FilePanel;
import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.Clearable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jorphan.gui.MinMaxLongRenderer;
import org.apache.jorphan.gui.NumberRenderer;
import org.apache.jorphan.gui.RendererUtils;

import com.github.rbourga.jmeter.apdexcov.logic.ApdexCoVLogic;

import kg.apc.jmeter.JMeterPluginsUtils;

/**
 * 
 */
public class ApdexCoVGui extends AbstractVisualizer implements ActionListener, Clearable {
	/**
	 * This extends the AbstractVisualizer class because it provides the easiest means to handle SampleResults.
	 */
	private static final long serialVersionUID = 1L;
	// TODO: check url
	private static final String WIKIPAGE = "https://github.com/rbourga/rbourga-jmeter-plugins/blob/main/tools/apdexcalculator/src/site/dat/wiki/ApdexScoreCalculator.wiki";
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };
	private static final String ACTION_CALCULATE = "calculate";
	private static final String ACTION_SAVE = "save";

	// Objects for the UI Panels
	private JLabel jLblApdexTgtTholdSec = new JLabel("Apdex Target Threshold T (in seconds) ");
	private JLabel jLblApdexMinScore = new JLabel("Minimum Apdex Score [0..1] ");
	private JLabel jLblCofVarMax = new JLabel("Maximum Coeff Variation (%) ");
	private JFormattedTextField jFtxtFldApdexTgtTholdSec;
	private JFormattedTextField jFTxtFldApdexMinScore;
	private JFormattedTextField jFTxtFldCofVarMax;
	private FilePanel filePnl;

	// GUI constructor
	public ApdexCoVGui() {
		super();

		// Use standard JMeter border
		setLayout(new BorderLayout());
		setBorder(makeBorder());

		// Add title
		// this.add(makeTitlePanel(), BorderLayout.NORTH);
		// Add title and help link
		add(JMeterPluginsUtils.addHelpLinkToPanel(makeTitlePanel(), WIKIPAGE), BorderLayout.NORTH);

		// Create a vertical panel layout scheme to hold the different panels of the UI
		JPanel vrtPnl = new VerticalPanel();

		// Panel for Apdex option
		JPanel jPnlApdex = new JPanel(new BorderLayout());
		NumberFormat nbrF1digit = NumberFormat.getNumberInstance();
		nbrF1digit.setMaximumFractionDigits(1);
		// Create Apdex Target text field and setup format
		jPnlApdex.add(jLblApdexTgtTholdSec, BorderLayout.WEST);
		jFtxtFldApdexTgtTholdSec = new JFormattedTextField(nbrF1digit);
		jFtxtFldApdexTgtTholdSec.setValue(4); // by default, 4s as per Apdex specs
		jFtxtFldApdexTgtTholdSec.setColumns(4);
		jPnlApdex.add(jFtxtFldApdexTgtTholdSec);
		jPnlApdex.setBorder(BorderFactory.createTitledBorder("Apdex Calculation Inputs"));

		// Panel for Failure criteria option
		JPanel jPnlApdexFail = new JPanel(new BorderLayout());
		NumberFormat nbrF2digit = NumberFormat.getNumberInstance();
		nbrF2digit.setMaximumFractionDigits(2);
		// Create Apdex Pass/Fail text field and setup format
		jPnlApdexFail.add(jLblApdexMinScore, BorderLayout.WEST);
		jFTxtFldApdexMinScore = new JFormattedTextField(nbrF2digit);
		jFTxtFldApdexMinScore.setValue(0.85); // by default, "good" rating
		jFTxtFldApdexMinScore.setColumns(4);
		jPnlApdexFail.add(jFTxtFldApdexMinScore);
		jPnlApdexFail.setBorder(BorderFactory.createTitledBorder("Apdex Failure Criteria Specification"));
		// Create CoV Pass/Fail text field and setup format
		JPanel jPnlCoeffVarFail = new JPanel(new BorderLayout());
		NumberFormat pctInst = NumberFormat.getPercentInstance();
		pctInst.setMaximumFractionDigits(2);
		jPnlCoeffVarFail.add(jLblCofVarMax, BorderLayout.WEST);
		jFTxtFldCofVarMax = new JFormattedTextField(pctInst);
		jFTxtFldCofVarMax.setValue(0.3); // 30% and above considered high variation
		jFTxtFldCofVarMax.setColumns(4);
		jPnlCoeffVarFail.add(jFTxtFldCofVarMax);
		jPnlCoeffVarFail.setBorder(BorderFactory.createTitledBorder("Coefficient of Variation Failure Criteria Specification"));

		// Panel for selection of file
		filePnl = new FilePanel("Read results from file and calculate Apdex & Coeff Var scores", EXTS);

		// Calculate button
		JPanel jPnlCalc = new JPanel();
		JButton jBtnCalc = new JButton("Calculate");
		jBtnCalc.addActionListener(this);
		jBtnCalc.setActionCommand(ACTION_CALCULATE);
		jPnlCalc.add(jBtnCalc);

		// Grid to display Apdex score of samplers
		JTable jTblStats = new JTable(ApdexCoVLogic.getPwrTblMdelStats());
		JMeterUtils.applyHiDPI(jTblStats);
		jTblStats.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(jTblStats, new TableCellRenderer[] {
				null, // Label
				null, // # Samples
				new MinMaxLongRenderer("#0"), // Average
				new NumberRenderer("#0.00%"), // Cov
				null, // CoV Rating
				new NumberRenderer("#0.00%"), // Error %
				new NumberRenderer("0.00"), // Apdex score
				new NumberRenderer("0.00"), // Target threshold
				null, // Apdex Rating
				null, // Low Sample indicator
				null }); // Pass/Failed indicator
		// Create the scroll pane and add the table to it
		JScrollPane jScrollPane = new JScrollPane(jTblStats);

		// Save Table button
		JPanel jPnlSave = new JPanel();
		JButton jBtnSave = new JButton(JMeterUtils.getResString("aggregate_graph_save_table"));
		jBtnSave.addActionListener(this);
		jBtnSave.setActionCommand(ACTION_SAVE);
		jPnlSave.add(jBtnSave, BorderLayout.CENTER);

		// Finally, assemble all panels
		vrtPnl.add(jPnlApdex);
		vrtPnl.add(jPnlApdexFail);
		vrtPnl.add(jPnlCoeffVarFail);
		vrtPnl.add(filePnl);
		vrtPnl.add(jPnlCalc);
		vrtPnl.add(jScrollPane);
		add(vrtPnl, BorderLayout.CENTER);
		add(jPnlSave, BorderLayout.SOUTH);
		// Hide the default file panel of this class as we are using another file panel
		getFilePanel().setVisible(false);
	}

	/**
	 * Section to override AbstractVisualizer's methods 
	 */
	public Collection<String> getMenuCategories() {
		//  Add this visualizer to the Non-Test Elements menu of the JMeter GUI
		return Arrays.asList(MenuFactory.NON_TEST_ELEMENTS);
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
		// return JMeterPluginsUtils.prefixLabel("Apdex Score Calculator");
		return "Apdex & Coefficient of Variation";
	}

	@Override
	public void clearData() {
		/* Called when user clicks on "Clear" or "Clear All" buttons.
		 * Clears data specific to this plugin
		 */
		ApdexCoVLogic.getPwrTblMdelStats().clearData();
		ApdexCoVLogic.getPwrTblMdelStats().fireTableDataChanged(); // Repaint the table
	}

	@Override
	public void actionPerformed(ActionEvent actionEvnt) {
		String sActionCmd = actionEvnt.getActionCommand();
		
		switch (sActionCmd) {
		case ACTION_CALCULATE:
			actionCalc();
			break;

		case ACTION_SAVE:
			if (ApdexCoVLogic.getPwrTblMdelStats().getRowCount() == 0) {
				GuiPackage.showErrorMessage("Data table empty - please perform Calculate before.", "Save Table Data error");
				return;
			}
			String csvFilename = saveDataModTblAsCsv();
			GuiPackage.showInfoMessage("Data saved to " + csvFilename, "Save Table Data");
			break;
		default:
		}
	}
	
	private String saveDataModTblAsCsv() {
		String sInFile = filePnl.getFilename();
		return ApdexCoVLogic.saveApdexStatsAsCsv(sInFile);
	}

	private void actionCalc() {
		// Parse target threshold
		double fApdexTgtTholdSec = ((Number) jFtxtFldApdexTgtTholdSec.getValue()).doubleValue();
		if (ApdexCoVLogic.isTgtTHoldOutOfRange(fApdexTgtTholdSec)) {
			GuiPackage.showErrorMessage("Please enter an Apdex target threshold equal to or greater than 0.1.", "Apdex Threshold Setting error");
			return;
		}

		// Parse Apdex score
		double fApdexMinScore = ((Number) jFTxtFldApdexMinScore.getValue()).doubleValue();
		if (ApdexCoVLogic.isApdexMinScoreOutOfRange(fApdexMinScore)) {
			GuiPackage.showErrorMessage("Please enter a minimum Apdex score between 0 and 1.", "Apdex Score Setting error");
			return;
		}

		// Parse Cov value
		double fCofVarMaxPct = ((Number) jFTxtFldCofVarMax.getValue()).doubleValue();
		if (ApdexCoVLogic.isCofVarPctOutOfRange(fCofVarMaxPct)) {
			GuiPackage.showErrorMessage("Please enter a maximum Coefficent of Variation value >= 0.", "Coefficient of Variation Setting error");
			return;
		}

		// Parse filename
		String sInFile = filePnl.getFilename();
		if (ApdexCoVLogic.isFilenameMissing(sInFile)) {
			GuiPackage.showErrorMessage("File name missing - please enter a filename.", "Input file error");
			return;
		}
		if (ApdexCoVLogic.isFileNotFound(sInFile)) {
			GuiPackage.showErrorMessage("Cannot find input file - please enter a valid filename.", "Input file error");
			return;
		}
		if (ApdexCoVLogic.isFileNotValid(sInFile)) {
			GuiPackage.showErrorMessage("Input file is empty or contains invalid data - please enter a valid results file.", "Input file error");
			return;
		}

		// Now that preliminary checks have been done, let's do the job...
		// Clear any statistics from a previous analysis
		ApdexCoVLogic.getPwrTblMdelStats().clearData();

		// Now, process the data
		int iResult = ApdexCoVLogic.computeApdexCofVar(sInFile, fApdexTgtTholdSec, fApdexMinScore, fCofVarMaxPct);
		if (iResult == -1) {
			GuiPackage.showErrorMessage("No samplers found in results file - please check your file.", "Input file error");
		}
		// Repaint the table
		ApdexCoVLogic.getPwrTblMdelStats().fireTableDataChanged();
	}

	@Override
	public void add(SampleResult sample) {
		// Unused Auto-generated method stub
	}
}
