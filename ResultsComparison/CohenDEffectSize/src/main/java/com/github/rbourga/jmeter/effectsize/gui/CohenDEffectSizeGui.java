/**
 *
 */
package com.github.rbourga.jmeter.effectsize.gui;

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
import com.github.rbourga.jmeter.effectsize.logic.CohenDEffectSizeLogic;

import kg.apc.jmeter.JMeterPluginsUtils;

/**
 *
 */
public class CohenDEffectSizeGui extends AbstractVisualizer implements ActionListener, Clearable {
	/**
	 * This extends the AbstractVisualizer class because it provides the easiest
	 * means to handle SampleResults.
	 */
	private static final long serialVersionUID = 1L;
	private static final String WIKIPAGE = "https://github.com/rbourga/rbourga-jmeter-plugins/wiki/Results-Comparison";
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };
	private static final String ACTION_COMPARE = "compare";
	private static final String ACTION_SAVE = "save";

	// Objects for the UI Panels
	private JLabel jLblCohendAL = new JLabel("Cohen's d Acceptable Limit ");
	private JFormattedTextField jFTxtFldCohendAL;
	private FilePanel filePnlA;
	private FilePanel filePnlB;

	// GUI constructor
	public CohenDEffectSizeGui() {
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

		// Panel for Failure criteria option
		JPanel jPnlDiffFail = new JPanel(new BorderLayout());
		NumberFormat nbrF2digit = NumberFormat.getNumberInstance();
		nbrF2digit.setMaximumFractionDigits(2);
		// Create Diff Pass/Fail text field and setup format
		jPnlDiffFail.add(jLblCohendAL, BorderLayout.WEST);
		jFTxtFldCohendAL = new JFormattedTextField(nbrF2digit);
		jFTxtFldCohendAL.setValue(1.20); // by default, "Very large" or more will be marked as failed
		jFTxtFldCohendAL.setColumns(4);
		jPnlDiffFail.add(jFTxtFldCohendAL);
		jPnlDiffFail.setBorder(BorderFactory.createTitledBorder("Failure Criteria Specification"));

		// Panel for selection of files
		filePnlA = new FilePanel("Control File (A)", EXTS);
		filePnlB = new FilePanel("Variation File (B)", EXTS);

		// Compare button
		JPanel jPnlCompare = new JPanel();
		JButton jBtnCompare = new JButton("Compare");
		jBtnCompare.addActionListener(this);
		jBtnCompare.setActionCommand(ACTION_COMPARE);
		jPnlCompare.add(jBtnCompare);

		// Grid to display the results of the difference
		JTable jTblStats = new JTable(CohenDEffectSizeLogic.getPwrTblMdelStats());
		JMeterUtils.applyHiDPI(jTblStats);
		jTblStats.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(jTblStats, new TableCellRenderer[] { null, // Label
				null, // # Samples A
				null, // # Samples B
				new MinMaxLongRenderer("#0"), // Average A
				new MinMaxLongRenderer("#0"), // Average B
				new NumberRenderer("0.00"), // Cohen's d
				null, // Diff Rating
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
		vrtPnl.add(jPnlDiffFail);
		vrtPnl.add(filePnlA);
		vrtPnl.add(filePnlB);
		vrtPnl.add(jPnlCompare);
		vrtPnl.add(jScrollPane);
		add(vrtPnl, BorderLayout.CENTER);
		add(jPnlSave, BorderLayout.SOUTH);
		// Hide the default file panel of this class as we are using another file panel
		getFilePanel().setVisible(false);
	}

	/**
	 * Section to override AbstractVisualizer's methods
	 */
	@Override
	public Collection<String> getMenuCategories() {
		// Add this visualizer to the Non-Test Elements menu of the JMeter GUI
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
		return "Results Comparator";
	}

	@Override
	public void clearData() {
		/*
		 * Called when user clicks on "Clear" or "Clear All" buttons. Clears data
		 * specific to this plugin
		 */
		CohenDEffectSizeLogic.getPwrTblMdelStats().clearData();
		CohenDEffectSizeLogic.getPwrTblMdelStats().fireTableDataChanged(); // Repaint the table
	}

	@Override
	public void actionPerformed(ActionEvent actionEvnt) {
		String sActionCmd = actionEvnt.getActionCommand();

		switch (sActionCmd) {
		case ACTION_COMPARE:
			actionCompare();
			break;

		case ACTION_SAVE:
			if (CohenDEffectSizeLogic.getPwrTblMdelStats().getRowCount() == 0) {
				GuiPackage.showErrorMessage("Data table empty - please perform Compare before.",
						"Save Table Data error");
				return;
			}
			String csvFilename = saveDataModTblAsCsv();
			GuiPackage.showInfoMessage("Data saved to " + csvFilename, "Save Table Data");
			break;
		default:
		}
	}

	private String saveDataModTblAsCsv() {
		String sInFileB = filePnlB.getFilename();
		return CohenDEffectSizeLogic.saveTableStatsAsCsv(sInFileB);
	}

	private void actionCompare() {
		// Parse Cohen's d threshold
		double fCohendAL = ((Number) jFTxtFldCohendAL.getValue()).doubleValue();
		if (CohenDEffectSizeLogic.isCohendALOutOfRange(fCohendAL)) {
			GuiPackage.showErrorMessage("Please enter an acceptable limit value equal to or greater than 0.",
					"Cohen's d Threshold Setting error");
			return;
		}

		// Parse filenames
		String sInFileA = filePnlA.getFilename();
		if (FileServices.isFilenameEmpty(sInFileA)) {
			GuiPackage.showErrorMessage("Control file name missing - please enter a filename.",
					"Input Control file error");
			return;
		}
		if (!(FileServices.isFileExist(sInFileA))) {
			GuiPackage.showErrorMessage("Cannot find Control file - please enter a valid filename.",
					"Input Control file error");
			return;
		}
		if (!(FileServices.isFileValid(sInFileA))) {
			GuiPackage.showErrorMessage(
					"Control file is empty or contains invalid data - please enter a valid results file.",
					"Input Control file error");
			return;
		}
		String sInFileB = filePnlB.getFilename();
		if (FileServices.isFilenameEmpty(sInFileB)) {
			GuiPackage.showErrorMessage("Variation file name missing - please enter a filename.",
					"Input Variation file error");
			return;
		}
		if (!(FileServices.isFileExist(sInFileB))) {
			GuiPackage.showErrorMessage("Cannot find Variation file - please enter a valid filename.",
					"Input Variation file error");
			return;
		}
		if (!(FileServices.isFileValid(sInFileB))) {
			GuiPackage.showErrorMessage(
					"Variation file is empty or contains invalid data - please enter a valid results file.",
					"Input Variation file error");
			return;
		}

		// Now, process the data
		int iResult = CohenDEffectSizeLogic.calcCohenDEffectSize(sInFileA, sInFileB, fCohendAL);
		switch (iResult) {
		case -1:
			GuiPackage.showErrorMessage("No samplers found in Control file - please check your file.",
					"Input Control file error");
			break;
		case -2:
			GuiPackage.showErrorMessage("No samplers found in Variation file - please check your file.",
					"Input Variation file error");
			break;
		default:
		}

		// Repaint the table
		CohenDEffectSizeLogic.getPwrTblMdelStats().fireTableDataChanged();
	}

	@Override
	public void add(SampleResult sample) {
		// Unused Auto-generated method stub
	}
}
