/**
 *
 */
package com.github.rbourga.jmeter.tukeyoutlierdetector.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;

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
import com.github.rbourga.jmeter.tukeyoutlierdetector.logic.TukeyOutlierDetectorLogic;

import kg.apc.jmeter.JMeterPluginsUtils;

/**
 *
 */
public class TukeyOutlierDetectorGui extends AbstractVisualizer implements ActionListener, Clearable {
	/**
	 * This extends the AbstractVisualizer class because it provides the easiest
	 * means to handle SampleResults.
	 */
	private static final long serialVersionUID = 1L;
	private static final String WIKIPAGE = "https://github.com/rbourga/rbourga-jmeter-plugins/wiki/Right-Tail-Outlier-Detection";
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };
	private static final String ACTION_DETECT = "detect";
	private static final String ACTION_SAVE = "save";

	// Objects for the UI Panels
	// Buttons for Tukey's Control Panel
	private JRadioButton jRadioBtn_1_5 = new JRadioButton("1.5 (remove all upper outliers)", false);
	private JRadioButton jRadioBtn_3 = new JRadioButton("3 (remove only extreme upper outliers)", false);
	private JRadioButton jRadioBtn_carling = new JRadioButton("Carling (dynamic k)", true);
	private final JLabel jLblRemAL = new JLabel("Removal Acceptable Limit (%) ");
	private JFormattedTextField jFTxtFldRemAL;
	private FilePanel filePnl;

	// GUI constructor
	public TukeyOutlierDetectorGui() {
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

		// Panel for Tukey's option
		JPanel jPnlTukey = new JPanel(new BorderLayout());
		ButtonGroup btnGrp = new ButtonGroup();
		btnGrp.add(jRadioBtn_1_5);
		btnGrp.add(jRadioBtn_3);
		btnGrp.add(jRadioBtn_carling);
		jPnlTukey.setLayout(new FlowLayout());
		jPnlTukey.add(jRadioBtn_1_5);
		jPnlTukey.add(jRadioBtn_3);
		jPnlTukey.add(jRadioBtn_carling);
		jPnlTukey.setBorder(BorderFactory.createTitledBorder("Tukey's constant k"));

		// Panel for Failure criteria option
		JPanel jPnlFail = new JPanel(new BorderLayout());
		NumberFormat pctInst = NumberFormat.getPercentInstance();
		pctInst.setMaximumFractionDigits(2);
		jPnlFail.add(jLblRemAL, BorderLayout.WEST);
		jFTxtFldRemAL = new JFormattedTextField(pctInst);
		jFTxtFldRemAL.setValue(0.2); // by default, 20% max trimmed
		jFTxtFldRemAL.setColumns(4);
		jPnlFail.add(jFTxtFldRemAL);
		jPnlFail.setBorder(BorderFactory.createTitledBorder("Removal Failure Criteria Specification"));

		// Panel for selection of file
		filePnl = new FilePanel("Read results from file and Detect Upper Outliers", EXTS);

		// Detect button
		JPanel jPnlDetn = new JPanel();
		JButton jBtnDetn = new JButton("Detect");
		jBtnDetn.addActionListener(this);
		jBtnDetn.setActionCommand(ACTION_DETECT);
		jPnlDetn.add(jBtnDetn);

		// Grid to display trimming of samplers
		JTable jTblStats = new JTable(TukeyOutlierDetectorLogic.getPwrTblMdelStats());
		JMeterUtils.applyHiDPI(jTblStats);
		jTblStats.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(jTblStats, new TableCellRenderer[] { null, // Label
				null, // # Samples
				new MinMaxLongRenderer("#0"), // Average
				null, // Upper Fence
				null, // # Removed
				new NumberRenderer("#0.00%"), // Removed %
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
		vrtPnl.add(jPnlTukey);
		vrtPnl.add(jPnlFail);
		vrtPnl.add(filePnl);
		vrtPnl.add(jPnlDetn);
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
		return "Upper Outlier Removal";
	}

	@Override
	public void clearData() {
		/*
		 * Called when user clicks on "Clear" or "Clear All" buttons. Clears data
		 * specific to this plugin
		 */
		TukeyOutlierDetectorLogic.getPwrTblMdelStats().clearData();
		TukeyOutlierDetectorLogic.getPwrTblMdelStats().fireTableDataChanged(); // Repaint the table
	}

	@Override
	public void actionPerformed(ActionEvent actionEvnt) {
		String sActionCmd = actionEvnt.getActionCommand();

		switch (sActionCmd) {
		case ACTION_DETECT:
			actionDetect();
			break;

		case ACTION_SAVE:
			if (TukeyOutlierDetectorLogic.getPwrTblMdelStats().getRowCount() == 0) {
				GuiPackage.showErrorMessage("Data table empty - please perform Detect before.",
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
		String sInFile = filePnl.getFilename();
		return TukeyOutlierDetectorLogic.saveTableStatsAsCsv(sInFile);
	}

	private void actionDetect() {
		// Parse trim % value
		double fRemALPct = ((Number) jFTxtFldRemAL.getValue()).doubleValue();
		if (TukeyOutlierDetectorLogic.isTrimPctOutOfRange(fRemALPct)) {
			GuiPackage.showErrorMessage("Please enter a maximum removal percentage value >= 0.",
					"Max Removal Setting error");
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
			GuiPackage.showErrorMessage(
					"Input file is empty or contains invalid data - please enter a valid results file.",
					"Input file error");
			return;
		}

		// Set Tukey's option that was selected
		double fTukeyK = 0.0; // We use 0 to indicate Carling's option was selected
		if (jRadioBtn_1_5.isSelected()) {
			fTukeyK = 1.5;
		} else if (jRadioBtn_3.isSelected()) {
			fTukeyK = 3.0;
		}

		// Now, process the data
		int iResult = TukeyOutlierDetectorLogic.RemoveUpper(sInFile, fTukeyK, fRemALPct);
		if (iResult == -1) {
			GuiPackage.showErrorMessage("No samplers found in results file - please check your file.",
					"Input file error");
		}
		// Repaint the table
		TukeyOutlierDetectorLogic.getPwrTblMdelStats().fireTableDataChanged();
	}

	@Override
	public void add(SampleResult sample) {
		// Unused Auto-generated method stub
	}
}
