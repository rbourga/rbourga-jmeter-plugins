package kg.apc.cmdtools;
/*
 * Please keep this package name for calling this tool with the the command line cmdrunner tool
 */

import java.io.PrintStream;
import java.util.ListIterator;

import org.apache.commons.lang3.math.NumberUtils;

import com.github.rbourga.jmeter.apdex.logic.ApdexLogic;
import com.github.rbourga.jmeter.common.FileServices;

import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.logging.LoggingUtils;

public class ApdexTool extends AbstractCMDTool{

	public ApdexTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
	}

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool Apdex [--help]"
		 */
		String sInFile = null;
		String sApdexTgtTholdSec = "4";	// 4s by default
		String sApdexAQL = "0.85";	// good by default

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
			} else if (arg.equalsIgnoreCase("--apdex-tgt-thold-secs")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Apdex satisfied threshold value missing.");
				}
				sApdexTgtTholdSec = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--apdex-aql")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Apdex Acceptable Quality Level value missing.");
				}
				sApdexAQL = ((String) args.next());
			}
		}

		// Check input-file parameter
		if (!(FileServices.isFileExist(sInFile))) {
			throw new IllegalArgumentException("Input file not found.");
		}
		if (!(FileServices.isFileValid(sInFile))) {
			throw new IllegalArgumentException("Input file invalid (could not find results).");
		}
		// Check apdex-tgt-thold parameter
		if (!(NumberUtils.isCreatable(sApdexTgtTholdSec))) {
			throw new IllegalArgumentException("Apdex satisfied threshold value invalid.");
		}
		double fApdexTgtTholdSec = Double.parseDouble(sApdexTgtTholdSec);
		if (ApdexLogic.isTgtTHoldOutOfRange(fApdexTgtTholdSec)) {
			throw new IllegalArgumentException("Apdex satisfied threshold value T needs to be greater or equal to 0.1.");			
		}
		// Check apdex-min-score parameter
		if (!(NumberUtils.isCreatable(sApdexAQL))) {
			throw new IllegalArgumentException("Apdex Acceptable Quality Level value invalid.");
		}
		double fApdexAQL = Double.parseDouble(sApdexAQL);
		if (ApdexLogic.isApdexMinScoreOutOfRange(fApdexAQL)) {
			throw new IllegalArgumentException("Apdex Acceptable Quality Level value needs to be between 0 and 1.");			
		}

		// Do the job
		int iResult = ApdexLogic.computeApdexScore(sInFile, fApdexTgtTholdSec, fApdexAQL);
		if (iResult == -1) {
			System.out.println("No samplers found in input file - please check your file.");
		} else {
			// Save Apdex results in an HTML file for import in DevOps tool later on
			String htmlFilename = ApdexLogic.saveTableStatsAsHtml(sInFile, sApdexAQL);
			System.out.println("Results saved in " + htmlFilename);
		}
		return iResult;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'Apdex': --input-file <filenameIn> "
				+ "["
				+ "--apdex-tgt-thold-secs <satisified treshold value in secs (greater than 0.1)> "
				+ "--apdex-aql <min Apdex score to pass (between 0 and 1)> "
				+ "]");				
	}
}
