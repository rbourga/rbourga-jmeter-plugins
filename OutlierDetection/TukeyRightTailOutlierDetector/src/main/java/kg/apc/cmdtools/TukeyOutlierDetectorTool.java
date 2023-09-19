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
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool ApdexCoV [--help]"
		 */
		String sInFile = null;
		String sTukeyK = "3";	// 3 by default
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
			throw new IllegalArgumentException("Tukey K value invalid.");
		}
		if (!(sTukeyK.equals("1.5")) && !(sTukeyK.equals("3")) && !(sTukeyK.equals("3.0"))) {
			throw new IllegalArgumentException("Unacceptable Tuke K value (only 1.5 or 3 accepted).");
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
			String htmlFilename = TukeyOutlierDetectorLogic.saveTableStatsAsHtml(sInFile, sTukeyK, sRemALPct);
			System.out.println("Results saved in " + htmlFilename);
		}
		return iResult;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'ApdexCoV': --input-file <filenameIn> "
				+ "["
				+ "--apdex-tgt-thold-secs <satisified treshold value in secs (greater than 0.1)> "
				+ "--apdex-aql <min Apdex score to pass (between 0 and 1)> "
				+ "--cov-alim-pct <Coefficient of Variation acceptable limit percentage value to pass>"
				+ "]");				
	}
}