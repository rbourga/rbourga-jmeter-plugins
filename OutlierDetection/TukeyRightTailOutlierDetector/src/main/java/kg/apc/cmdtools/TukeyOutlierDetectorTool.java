package kg.apc.cmdtools;
/*
 * Please keep this package name for calling this tool with the the command line cmdrunner tool
 */

import java.io.PrintStream;
import java.util.ListIterator;

import org.apache.commons.lang3.math.NumberUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.tukeyoutlierdetector.logic.TukeyOutlierDetectorLogic;

import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.logging.LoggingUtils;

public class TukeyOutlierDetectorTool extends AbstractCMDTool{

	public TukeyOutlierDetectorTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
	}

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool TukeyOutlierDetector"
		 */
		String sInFile = null;
		String sTukeyK = "0";	// Carling's value by default
		String sRemALPct = "0.20";	// 20% max by default

		if (!args.hasNext()) {
			showHelp(System.out);
			return 0;
		}

		// Process params
		while (args.hasNext()) {
			String arg = (String) args.next();
			if (arg.equalsIgnoreCase("--input-file")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Input file name missing.");
				}
				sInFile = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--tukey-k")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Tukey K value missing.");
				}
				sTukeyK = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--rem-alim-pct")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Upper outlier removal acceptable limit value missing.");
				}
				sRemALPct = ((String) args.next());
			}
		}

		// Check input-file parameter
		if (!(FileServices.isFileExist(sInFile))) {
			throw new IllegalArgumentException("Input file not found.");
		}
		if (!(FileServices.isFileValid(sInFile))) {
			throw new IllegalArgumentException("Input file invalid (could not find results).");
		}
		// Check tukey-k parameter
		if (!(NumberUtils.isCreatable(sTukeyK))) {
			throw new IllegalArgumentException("Tukey k value invalid.");
		}
		if (!(sTukeyK.equals("0")) &&
			!(sTukeyK.equals("0.0")) &&
			!(sTukeyK.equals("1.5")) &&
			!(sTukeyK.equals("3")) &&
			!(sTukeyK.equals("3.0"))) {
			throw new IllegalArgumentException("Unacceptable Tukey k value (only 0, 1.5 or 3 accepted).");
		}
		// Check rem-alim-pct parameter
		if (!(NumberUtils.isCreatable(sRemALPct))) {
			throw new IllegalArgumentException("Upper outlier removal percentage acceptable limit value invalid.");
		}
		double fRemALPct = Double.parseDouble(sRemALPct);
		if (TukeyOutlierDetectorLogic.isTrimPctOutOfRange(fRemALPct)) {
			throw new IllegalArgumentException("Upper outlier removal acceptable limit value needs to be greater or equal to 0.");			
		}

		// Do the job
		int iResult = TukeyOutlierDetectorLogic.RemoveUpper(sInFile, Double.parseDouble(sTukeyK), fRemALPct);
		if (iResult == -1) {
			System.out.println("No samplers found in input file - please check your file.");
		} else {
			// Save Removal results in an HTML file for import in DevOps tool later on
			String htmlFilename = TukeyOutlierDetectorLogic.saveTableStatsAsHtml(sInFile, sRemALPct);
			System.out.println("Results saved in " + htmlFilename);
		}
		return iResult;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'TukeyOutlierDetector': --input-file <filenameIn> "
				+ "["
				+ "--tukey-k <k (0 (Carling), 1.5 or 3, default = 0)> "
				+ "--rem-alim-pct <Removal acceptable limit percentage value to pass (default = 20%)>"
				+ "]");				
	}
}
