/**
 * 
 */
package com.github.rbourga.jmeter.multimodalitycov.gui;

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

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.multimodalitycov.logic.MultimodalityCoVLogic;

import kg.apc.jmeter.JMeterPluginsUtils;

/**
 * 
 */
public class MultimodalityCoVGui extends AbstractVisualizer implements ActionListener, Clearable {
	/**
	 * This extends the AbstractVisualizer class because it provides the easiest means to handle SampleResults.
	 */
	private static final long serialVersionUID = 1L;
	// TODO:
	private static final String WIKIPAGE = "https://github.com/rbourga/rbourga-jmeter-plugins/wiki/APDEX-Score-and-Coefficient-of-Variation-Calculator";
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };
	private static final String ACTION_CALCULATE = "calculate";
	private static final String ACTION_SAVE = "save";

	// Objects for the UI Panels
	private JLabel jLblMvalueThold = new JLabel("MValue Threshold ");
	private JLabel jLblCoVAL = new JLabel("Coefficient of Variation Acceptable Limit (%) ");
	private JFormattedTextField jFtxtFldMvalueThold;
	private JFormattedTextField jFTxtFldCoVAL;
	private FilePanel filePnl;

	// GUI constructor
	public MultimodalityCoVGui() {
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

		// Panel for MValue option
		JPanel jPnlMvalue = new JPanel(new BorderLayout());
		NumberFormat nbrF1digit = NumberFormat.getNumberInstance();
		nbrF1digit.setMaximumFractionDigits(1);
		// Create MValue threshold text field and setup format
		jPnlMvalue.add(jLblMvalueThold, BorderLayout.WEST);
		jFtxtFldMvalueThold = new JFormattedTextField(nbrF1digit);
		jFtxtFldMvalueThold.setValue(2.4); // by default, 2.4 as per Brendan Gregg
		jFtxtFldMvalueThold.setColumns(3);
		jPnlMvalue.add(jFtxtFldMvalueThold);
		jPnlMvalue.setBorder(BorderFactory.createTitledBorder("Modal Test Sensitivity"));

		// Panel for CoV Pass/Fail text field and setup format
		JPanel jPnlCoVFail = new JPanel(new BorderLayout());
		NumberFormat pctInst = NumberFormat.getPercentInstance();
		pctInst.setMaximumFractionDigits(2);
		jPnlCoVFail.add(jLblCoVAL, BorderLayout.WEST);
		jFTxtFldCoVAL = new JFormattedTextField(pctInst);
		jFTxtFldCoVAL.setValue(0.3); // 30% and above considered high variation
		jFTxtFldCoVAL.setColumns(4);
		jPnlCoVFail.add(jFTxtFldCoVAL);
		jPnlCoVFail.setBorder(BorderFactory.createTitledBorder("Coefficient of Variation Failure Criteria Specification"));

		// Panel for selection of file
		filePnl = new FilePanel("Read results from file and calculate Modality & Coeff Var scores", EXTS);

		// Calculate button
		JPanel jPnlCalc = new JPanel();
		JButton jBtnCalc = new JButton("Calculate");
		jBtnCalc.addActionListener(this);
		jBtnCalc.setActionCommand(ACTION_CALCULATE);
		jPnlCalc.add(jBtnCalc);

		// Grid to display modality & cov of samplers
		JTable jTblStats = new JTable(MultimodalityCoVLogic.getPwrTblMdelStats());
		JMeterUtils.applyHiDPI(jTblStats);
		jTblStats.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(jTblStats, new TableCellRenderer[] {
				null, // Label
				null, // # Samples
				new MinMaxLongRenderer("#0"), // Average
				new MinMaxLongRenderer("#0"), // Min
				new MinMaxLongRenderer("#0"), // Max
				new NumberRenderer("#0.00%"), // Cov
				null, // CoV Rating
				new MinMaxLongRenderer("#0"), // Bin size
				null, // Multimodal
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
		vrtPnl.add(jPnlMvalue);
		vrtPnl.add(jPnlCoVFail);
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
		return "Modality & Coefficient of Variation";
	}

	@Override
	public void clearData() {
		/* Called when user clicks on "Clear" or "Clear All" buttons.
		 * Clears data specific to this plugin
		 */
		MultimodalityCoVLogic.getPwrTblMdelStats().clearData();
		MultimodalityCoVLogic.getPwrTblMdelStats().fireTableDataChanged(); // Repaint the table
	}

	@Override
	public void actionPerformed(ActionEvent actionEvnt) {
		String sActionCmd = actionEvnt.getActionCommand();
		
		switch (sActionCmd) {
		case ACTION_CALCULATE:
			actionCalc();
			break;

		case ACTION_SAVE:
			if (MultimodalityCoVLogic.getPwrTblMdelStats().getRowCount() == 0) {
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
		return MultimodalityCoVLogic.saveTableStatsAsCsv(sInFile);
	}

	private void actionCalc() {
		// Parse mValue threshold
		double fMvalueThold = ((Number) jFtxtFldMvalueThold.getValue()).doubleValue();
		if (MultimodalityCoVLogic.isMvaleTHoldOutOfRange(fMvalueThold)) {
			GuiPackage.showErrorMessage("Please enter an MValue threshold equal to or greater than 0.1.", "MValue Threshold Setting error");
			return;
		}

		// Parse Cov Acceptable limit
		double fCoVALPct = ((Number) jFTxtFldCoVAL.getValue()).doubleValue();
		if (MultimodalityCoVLogic.isCoVPctOutOfRange(fCoVALPct)) {
			GuiPackage.showErrorMessage("Please enter a maximum Coefficent of Variation value >= 0.", "Coefficient of Variation Setting error");
			return;
		}

		// Parse filename
		String sInFile = filePnl.getFilename();
		if (FileServices.isFilenameEmpty(sInFile)) {
			GuiPackage.showErrorMessage("File name missing - please enter a filename.", "Input file error");
			return;
		}
		if (!(FileServices.isFileExist(sInFile))) {
			GuiPackage.showErrorMessage("Cannot find input file - please enter a valid filename.", "Input file error");
			return;
		}
		if (!(FileServices.isFileValid(sInFile))) {
			GuiPackage.showErrorMessage("Input file is empty or contains invalid data - please enter a valid results file.", "Input file error");
			return;
		}

		// Now, process the data
		int iResult = MultimodalityCoVLogic.computeMvalueCoV(sInFile, fMvalueThold, fCoVALPct);
		if (iResult == -1) {
			GuiPackage.showErrorMessage("No samplers found in results file - please check your file.", "Input file error");
		}
		// Repaint the table
		MultimodalityCoVLogic.getPwrTblMdelStats().fireTableDataChanged();
	}

	@Override
	public void add(SampleResult sample) {
		// Unused Auto-generated method stub
	}
}
