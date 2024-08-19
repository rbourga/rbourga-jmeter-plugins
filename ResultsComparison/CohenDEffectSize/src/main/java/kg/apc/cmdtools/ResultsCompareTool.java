package kg.apc.cmdtools;
/*
 * Please keep this package name for calling this tool with the the command line cmdrunner tool
 */

import java.io.PrintStream;
import java.util.ListIterator;

import org.apache.commons.lang3.math.NumberUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.effectsize.logic.CohenDEffectSizeLogic;

import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.logging.LoggingUtils;

public class ResultsCompareTool extends AbstractCMDTool {

	public ResultsCompareTool() {
		super();
		JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
		LoggingUtils.addLoggingConfig();
	}

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool
		 * ResultsCompare"
		 */
		String sCtrlFile = null;
		String sVarFile = null;
		String sCohensdAL = "1.2"; // 1.2 max acceptable limit by default

		if (!args.hasNext()) {
			showHelp(System.out);
			return 0;
		}

		// Process params
		while (args.hasNext()) {
			String arg = (String) args.next();
			if (arg.equalsIgnoreCase("--ctrl-file")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Control file name missing.");
				}
				sCtrlFile = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--var-file")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Variation file name missing.");
				}
				sVarFile = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--cohensd-alim")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Cohen's d acceptable limit value missing.");
				}
				sCohensdAL = ((String) args.next());
			}
		}

		// Check file parameters
		if (!(FileServices.isFileExist(sCtrlFile))) {
			throw new IllegalArgumentException("Control file not found.");
		}
		if (!(FileServices.isFileValid(sCtrlFile))) {
			throw new IllegalArgumentException("Control file invalid (could not find results).");
		}
		if (!(FileServices.isFileExist(sVarFile))) {
			throw new IllegalArgumentException("Variation file not found.");
		}
		if (!(FileServices.isFileValid(sVarFile))) {
			throw new IllegalArgumentException("Variation file invalid (could not find results).");
		}
		// Check sCohensdAL parameter
		if (!(NumberUtils.isCreatable(sCohensdAL))) {
			throw new IllegalArgumentException("Cohen's d acceptable limit value invalid.");
		}
		double fCohensdAL = Double.parseDouble(sCohensdAL);
		if (CohenDEffectSizeLogic.isCohendALOutOfRange(fCohensdAL)) {
			throw new IllegalArgumentException("Cohen's d acceptable limit value needs to be greater or equal to 0.");
		}

		// Do the job
		int iResult = CohenDEffectSizeLogic.calcCohenDEffectSize(sCtrlFile, sVarFile, fCohensdAL);
		switch (iResult) {
		case -1:
			System.out.println("No samplers found in control file - please check your file.");
			break;
		case -2:
			System.out.println("No samplers found in variation file - please check your file.");
			break;
		default:
			// Save Apdex results in an HTML file for import in DevOps tool later on
			String htmlFilename = CohenDEffectSizeLogic.saveTableStatsAsHtml(sVarFile, sCohensdAL);
			System.out.println("Results saved in " + htmlFilename);
			System.out.println("Performance worsened for " + iResult + " elements.");
		}
		return iResult;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'ResultsCompare':" + "--ctrl-file <controlFilename> "
				+ "--var-file <variationFilename> " + "[" + "--cohensd-alim <Cohen's d acceptable limit value to pass>"
				+ "]");
	}
}
