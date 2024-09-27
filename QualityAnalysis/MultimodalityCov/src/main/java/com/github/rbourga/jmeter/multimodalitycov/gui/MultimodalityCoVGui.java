/**
 *
 */
package com.github.rbourga.jmeter.multimodalitycov.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.multimodalitycov.logic.MultimodalityCoVLogic;

import kg.apc.jmeter.JMeterPluginsUtils;

/**
 *
 */
public class MultimodalityCoVGui extends AbstractVisualizer implements ActionListener, Clearable {
	/**
	 * This extends the AbstractVisualizer class because it provides the easiest
	 * means to handle SampleResults.
	 */
	private static final long serialVersionUID = 1L;
	private static final String PLUGIN_LABEL = "Modality & Coefficient of Variation";
	private static final String WIKIPAGE = "https://github.com/rbourga/rbourga-jmeter-plugins/wiki/Modality-and-Coefficient-of-Variation";
	private static final String[] EXTS = { ".jtl", ".csv", ".tsv" };
	private static final String ACTION_CALCULATE = "calculate";
	private static final String ACTION_SAVE = "save";

	// Objects for the UI Panels
	private JLabel jLblMvalueThold = new JLabel("MValue Threshold ");
	private JLabel jLblCoVAL = new JLabel("Coefficient of Variation Acceptable Limit (%) ");
	private JFormattedTextField jFtxtFldMvalueThold;
	private JFormattedTextField jFTxtFldCoVAL;
	private FilePanel filePnl;

	// Objects for graph
	private JTable jTblRows;
	private int iColBin, iColCheck, iColLabel, iColMin;

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

		// Top panel layout
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
		jPnlCoVFail
				.setBorder(BorderFactory.createTitledBorder("Coefficient of Variation Failure Criteria Specification"));

		// Panel for selection of file
		filePnl = new FilePanel("Read results from file and calculate Modality & Coeff Var scores", EXTS);

		// Calculate button
		JPanel jPnlCalc = new JPanel();
		JButton jBtnCalc = new JButton("Calculate");
		jBtnCalc.addActionListener(this);
		jBtnCalc.setActionCommand(ACTION_CALCULATE);
		jPnlCalc.add(jBtnCalc);

		// Create a tabbed pane with the different tabs
		JTabbedPane jTabbedPane = new JTabbedPane();
		jTabbedPane.addTab("Results", null, addResultsTab(), "View Results");
		jTabbedPane.addTab("Rows", null, addRowsTab(), "Select rows to display");

		// Create a placeholder for the Chart tab
		JPanel jPnlChart = new JPanel(new BorderLayout());
		ChartPanel jChartPnl = new ChartPanel(null);
//		jChartPnl.setFillZoomRectangle(true);
//		jChartPnl.setMouseWheelEnabled(true); // Enable/Disable mouse wheel zooming
//		jChartPnl.setPreferredSize(new Dimension(600, 400));
		jPnlChart.add(jChartPnl, BorderLayout.CENTER);
		jTabbedPane.addTab("Chart", null, jPnlChart, "View chart");

		// Add a ChangeListener to the tabbedPane to update the bar chart when the user
		// selects it
		jTabbedPane.addChangeListener(e -> {
			if (jTabbedPane.getSelectedIndex() == 2) { // Index of Chart tab
				JFreeChart barChart = crteBarChart();
				jChartPnl.setChart(barChart);
			}
		});

		// Finally, assemble all panels
		vrtPnl.add(jPnlMvalue);
		vrtPnl.add(jPnlCoVFail);
		vrtPnl.add(filePnl);
		vrtPnl.add(jPnlCalc);
		vrtPnl.add(jTabbedPane);
		add(vrtPnl, BorderLayout.CENTER);

		// Hide the default file panel of this class as we are using another file panel
		getFilePanel().setVisible(false);

	}

	/**
	 * Section to override AbstractVisualizer's methods
	 */
	@Override
	public void actionPerformed(ActionEvent actionEvnt) {
		String sActionCmd = actionEvnt.getActionCommand();

		switch (sActionCmd) {
		case ACTION_CALCULATE:
			actionCalc();
			break;

		case ACTION_SAVE:
			if (MultimodalityCoVLogic.getPwrTblMdelStats().getRowCount() == 0) {
				GuiPackage.showErrorMessage("Data table empty - please perform Calculate before.",
						"Save Table Data error");
				return;
			}
			String sInFile = filePnl.getFilename();
			String csvFilename = MultimodalityCoVLogic.saveTableStatsAsCsv(sInFile);
			GuiPackage.showInfoMessage("Data saved to " + csvFilename, "Save Table Data");
			break;

		default:
		}
	}

	@Override
	public void add(SampleResult sample) {
		// Unused Auto-generated method stub
	}

	@Override
	public void clearData() {
		/*
		 * Called when user clicks on "Clear" or "Clear All" buttons. Clears data
		 * specific to this plugin
		 */
		MultimodalityCoVLogic.getBinsMap().clear();
		MultimodalityCoVLogic.getPwrTblMdelStats().clearData();
		MultimodalityCoVLogic.getPwrTblMdelRows().clearData();

		// Repaint the tables
		MultimodalityCoVLogic.getPwrTblMdelStats().fireTableDataChanged();
		MultimodalityCoVLogic.getPwrTblMdelRows().fireTableDataChanged();
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
	public Collection<String> getMenuCategories() {
		// Add this visualizer to the Non-Test Elements menu of the JMeter GUI
		return Arrays.asList(MenuFactory.NON_TEST_ELEMENTS);
	}

	@Override
	public String getStaticLabel() {
		return PLUGIN_LABEL;
	}

	/*
	 * Private methods
	 */
	private void actionCalc() {
		// Parse mValue threshold
		double fMvalueThold = ((Number) jFtxtFldMvalueThold.getValue()).doubleValue();
		if (MultimodalityCoVLogic.isMvaleTHoldOutOfRange(fMvalueThold)) {
			GuiPackage.showErrorMessage("Please enter an MValue threshold equal to or greater than 0.1.",
					"MValue Threshold Setting error");
			return;
		}

		// Parse Cov Acceptable limit
		double fCoVALPct = ((Number) jFTxtFldCoVAL.getValue()).doubleValue();
		if (MultimodalityCoVLogic.isCoVPctOutOfRange(fCoVALPct)) {
			GuiPackage.showErrorMessage("Please enter a maximum Coefficent of Variation value >= 0.",
					"Coefficient of Variation Setting error");
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

		// Now, process the data
		int iResult = MultimodalityCoVLogic.computeMvalueCoV(sInFile, fMvalueThold, fCoVALPct);
		if (iResult == -1) {
			GuiPackage.showErrorMessage("No samplers found in results file - please check your file.",
					"Input file error");
		}
		// Repaint the tables
		MultimodalityCoVLogic.getPwrTblMdelStats().fireTableDataChanged();
		MultimodalityCoVLogic.getPwrTblMdelRows().fireTableDataChanged();
	}

	// Results tab
	private JPanel addResultsTab() {
		// To display the Results grid and Save button
		JPanel vrtPnlResults = new VerticalPanel();

		// The table
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
				null, // Bin Size
				new NumberRenderer("#0.0"), // mValue
				null, // Multimodal
				null }); // Pass/Failed indicator
		// Create the scroll pane and add the table to it
		JScrollPane jScrollPane = new JScrollPane(jTblStats);

		// The Save Table button
		JPanel jPnlSave = new JPanel();
		JButton jBtnSave = new JButton(JMeterUtils.getResString("aggregate_graph_save_table"));
		jBtnSave.addActionListener(this);
		jBtnSave.setActionCommand(ACTION_SAVE);
		jPnlSave.add(jBtnSave, BorderLayout.CENTER);

		// Add the grid & save button to the Results tab
		vrtPnlResults.add(jScrollPane);
		vrtPnlResults.add(jPnlSave);

		return vrtPnlResults;

	}

	// Rows tab
	private Component addRowsTab() {
		// To display the Rows grid and Display button
		JPanel vrtPnlRows = new VerticalPanel();

		// The table
		jTblRows = new JTable(MultimodalityCoVLogic.getPwrTblMdelRows());
		JMeterUtils.applyHiDPI(jTblRows);
		jTblRows.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(jTblRows, new TableCellRenderer[] {
				null, // Label
				new MinMaxLongRenderer("#0"), // Min
				null, // Bin rule
				null, // Bin Size
				null, // Multimodal
				null }); // Check/uncheck indicator
		// Create the scroll pane and add the table to it
		JScrollPane jScrollPane = new JScrollPane(jTblRows);
		iColLabel = 0;
		iColMin = 1;
		iColBin = 3;
		iColCheck = 5;

		// Add the grid to the Results tab
		vrtPnlRows.add(jScrollPane);
		return vrtPnlRows;
	}

	private JFreeChart crteBarChart() {
		/*
		 * Creates a Bar Chart using JFreeChart. The bins will be displayed according to
		 * their actual range on the X-Axis . The chart will be updated when the user
		 * selects the Chart tab thanks to the tabbed ChangeListener.
		 */
		// Check if the Rows table is empty
		if (MultimodalityCoVLogic.getPwrTblMdelRows().getRowCount() == 0) {
			return null;
		}

		// Parse the Rows table and save the number of the selected rows in an array
		int iRowCnt = jTblRows.getRowCount();
		ArrayList<Integer> aliCheckedRow = new ArrayList<>();
		for (int i = 0; i < iRowCnt; i++) {
			if ((boolean) jTblRows.getValueAt(i, iColCheck)) {
				aliCheckedRow.add(i);
			}
		}

		// Check if some rows are selected and that no bin size is equal to 0
		if (aliCheckedRow.isEmpty()) {
			return null;
		} else {
			int iNbrOfChecks = aliCheckedRow.size();
			for (int i = 0; i < iNbrOfChecks; i++) {			
				int iBinSizei = (int) jTblRows.getValueAt(aliCheckedRow.get(i), iColBin);
				if (iBinSizei == 0) {
					GuiPackage.showErrorMessage(
							"Bin size cannot be 0 - please select rows with bin size different from 0.",
							"Chart Data error");
					return null;
				}
			}
		}

		// Create a DefaultCategoryDataset which includes the bin ranges as categories.
		Map<String, int[]> binsMap = MultimodalityCoVLogic.getBinsMap();
		DefaultCategoryDataset catDataset = new DefaultCategoryDataset();
		for (Integer iRow : aliCheckedRow) {
			String sLabel = (String) jTblRows.getValueAt(iRow, iColLabel);
			long lMin = (long) jTblRows.getValueAt(iRow, iColMin);
			int iBinSize = (int) jTblRows.getValueAt(iRow, iColBin);
			int[] aiBins = binsMap.get(sLabel);
			// Note: the list of bins contain the zero bin terminators at the beginning and
			// the end.
			// So we need to skip them in the graph.
			for (int iBin = 1; iBin < (aiBins.length - 1); iBin++) {
				// Tag the bins with their actual range on the X-axis
				String binLabel = getBinLabel(iBin - 1, lMin, iBinSize);
				catDataset.addValue(aiBins[iBin], sLabel, binLabel);
			}
		}

		// Create the bar chart with the dataset and title
		JFreeChart barChart = ChartFactory.createBarChart("Response Times Distribution", // Title
				"Response times in ms", // X Axis label
				"Number of responses", // Y Axis label
				catDataset, PlotOrientation.VERTICAL, true, // Legend
				true, // Tooltips
				false); // URLs

		// Some rendering functions...
		barChart.setBackgroundPaint(Color.WHITE);

		// Customize the X-axis to show the bin ranges rotated
		CategoryPlot plot = (CategoryPlot) barChart.getPlot();
		CategoryAxis xAxis = plot.getDomainAxis();
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// Set legend at the top
		LegendTitle legend = barChart.getLegend();
		legend.setPosition(RectangleEdge.TOP);

		return barChart;
	}

	private String getBinLabel(int iBinIndex, long lMin, int iBinSize) {
		int binStart = (int) ((iBinIndex * iBinSize) + lMin);
		int binEnd = binStart + (int) iBinSize;
		return binStart + "-" + binEnd + " ms";
	}

}
