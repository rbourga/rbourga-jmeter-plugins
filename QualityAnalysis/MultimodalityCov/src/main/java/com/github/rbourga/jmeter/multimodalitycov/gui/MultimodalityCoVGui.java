/**
 * 
 */
package com.github.rbourga.jmeter.multimodalitycov.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.category.DefaultCategoryDataset;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.multimodalitycov.logic.MultimodalityCoVLogic;

import kg.apc.charting.AbstractGraphRow;
import kg.apc.charting.ColorsDispatcher;
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
	private static final String ACTION_CHART = "chart";

	// Objects for the UI Panels
	private JLabel jLblMvalueThold = new JLabel("MValue Threshold ");
	private JLabel jLblCoVAL = new JLabel("Coefficient of Variation Acceptable Limit (%) ");
	private JFormattedTextField jFtxtFldMvalueThold;
	private JFormattedTextField jFTxtFldCoVAL;
	private FilePanel filePnl;
	
	// Objects for graph
	ArrayList<Integer> aliCheckedRow = new ArrayList<Integer>();
	private JTable jTblRows;
	private int iColBin, iColCheck, iColLabel;
    protected ConcurrentSkipListMap<String, AbstractGraphRow> model;
	protected ColorsDispatcher colors;
	JFreeChart barChart;

	// GUI constructor
	public MultimodalityCoVGui() {
		super();
		
		// For Graphing needs
        model = new ConcurrentSkipListMap<>();

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
		jPnlCoVFail.setBorder(BorderFactory.createTitledBorder("Coefficient of Variation Failure Criteria Specification"));

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
		jChartPnl.setFillZoomRectangle(true);
		jChartPnl.setMouseWheelEnabled(true); // Enable/Disable mouse wheel zooming
		jChartPnl.setPreferredSize(new Dimension(600, 400));
		jPnlChart.add(jChartPnl, BorderLayout.CENTER);
		jTabbedPane.addTab("Chart",null, jPnlChart, "View chart");

		// Add a ChangeListener to the tabbedPane to update the bar chart when the user selects it
		jTabbedPane.addChangeListener(e -> {
            if (jTabbedPane.getSelectedIndex() == 2) {	// Index of Chart tab
        		barChart = crteBarChart(aliCheckedRow, MultimodalityCoVLogic.getBinsMap());
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
				new MinMaxLongRenderer("#0"), // Bin size
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

	private Component addRowsTab() {
		// To display the Rows grid and Display button
		JPanel vrtPnlRows = new VerticalPanel();

		// The table
		jTblRows = new JTable(MultimodalityCoVLogic.getPwrTblMdelRows());
		JMeterUtils.applyHiDPI(jTblRows);
		jTblRows.setAutoCreateRowSorter(true);
		RendererUtils.applyRenderers(jTblRows, new TableCellRenderer[] {
				null, // Label
				new MinMaxLongRenderer("#0"), // Bin size
				null, // Multimodal
				null }); // Check/uncheck indicator
		// Create the scroll pane and add the table to it
		JScrollPane jScrollPane = new JScrollPane(jTblRows);
		iColLabel = 0;
		iColBin = 1;
		iColCheck = 3;

		// The Chart button
		JPanel jPnlChart = new JPanel();
		JButton jBtnChart = new JButton("Chart");
		jBtnChart.addActionListener(this);
		jBtnChart.setActionCommand(ACTION_CHART);
		jPnlChart.add(jBtnChart, BorderLayout.CENTER);
		
		// Add the grid & save button to the Results tab
		vrtPnlRows.add(jScrollPane);
		vrtPnlRows.add(jPnlChart);
		
		return vrtPnlRows;

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
		MultimodalityCoVLogic.getPwrTblMdelRows().clearData();
		MultimodalityCoVLogic.getBinsMap().clear();
		 // Repaint the tables
		MultimodalityCoVLogic.getPwrTblMdelStats().fireTableDataChanged();
		MultimodalityCoVLogic.getPwrTblMdelRows().fireTableDataChanged();
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

		case ACTION_CHART:
			actionChart();
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
		// Repaint the tables
		MultimodalityCoVLogic.getPwrTblMdelStats().fireTableDataChanged();
		MultimodalityCoVLogic.getPwrTblMdelRows().fireTableDataChanged();
	}

	private void actionChart() {
		if (MultimodalityCoVLogic.getPwrTblMdelRows().getRowCount() == 0) {
			GuiPackage.showErrorMessage("Rows table empty - please perform Calculate before.", "Chart Data error");
			return;
		}

		aliCheckedRow.clear();
		// Check if any rows are selected and save their number in an array
		int iRowCnt = jTblRows.getRowCount();
		for (int i = 0; i < iRowCnt; i++) {
			if ((boolean) jTblRows.getValueAt(i,iColCheck)) {
				aliCheckedRow.add(i);
			}
		}
		if (aliCheckedRow.size() == 0) {
			GuiPackage.showErrorMessage("No row selected - please tick a row.", "Chart Data error");
			return;
		}
		
		// Bin size cannot be 0
		long lBinSizeFirst = (long) jTblRows.getValueAt(aliCheckedRow.getFirst(), iColBin);
		if (lBinSizeFirst == 0) {
            GuiPackage.showErrorMessage("Bin size for selected rows cannot be 0 - please select rows with bin size different from 0.", "Chart Data error");
            return;
        }
		
		// Ensure the checked rows have same bin size
		if (aliCheckedRow.size() > 1) {
			int iNbrOfChecks = aliCheckedRow.size();
			for (int i = 1; i < iNbrOfChecks; i++) {
				long lBinSizei = (long) jTblRows.getValueAt(aliCheckedRow.get(i), iColBin);
                if (lBinSizeFirst != lBinSizei) {
                    GuiPackage.showErrorMessage("Bin sizes for selected rows are different - please select rows with same bin size.", "Chart Data error");
                    return;}
			}
		}
		
		// The chart will be updated when the user selects the Chart tab thanks to the ChangeListener
	}

	private JFreeChart crteBarChart(ArrayList<Integer> alistInt , Map<String, int[]> binsMap) {
		// Return null to clear the Chart if nothing selected
		if (alistInt.isEmpty()) {
			return null;
		}
		// Add the wanted series to a DefaultCategoryDataset used by JFreeChart
		DefaultCategoryDataset categoryDat = new DefaultCategoryDataset();
		for (Integer iRow : alistInt) {
			String sLabel = (String) jTblRows.getValueAt(iRow,iColLabel);
			int[] aiBins = binsMap.get(sLabel);
			// Note: the list of bins contain the zero bin terminators at the beginning and the end that we need to skip
			for (int iBin = 1; iBin < (aiBins.length - 1); iBin++) {
                categoryDat.addValue(aiBins[iBin], sLabel, "Bin " + iBin);
            }	
		}
		
		// Create the bar chart with the dataset and title
		JFreeChart barChart = ChartFactory.createBarChart(
                "Response Time Histogram",	// Title
                "Response times in ms",		// X Axis label
                "Number of responses",		// Y Axis label
                categoryDat,
                PlotOrientation.VERTICAL,
                true,						// Legend
                false,						// Tooltips
                false);						// URLs
		
		// Formatting the Chart...
		barChart.setBackgroundPaint(Color.WHITE);
		CategoryPlot plot = (CategoryPlot) barChart.getPlot();
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setDrawBarOutline(false);
		barChart.getLegend().setFrame(BlockBorder.NONE);
		
		// Set legend at the top
		LegendTitle legend = barChart.getLegend();
		legend.setPosition(RectangleEdge.TOP);

		return barChart;        
	}

	@Override
	public void add(SampleResult sample) {
		// Unused Auto-generated method stub
	}
}
