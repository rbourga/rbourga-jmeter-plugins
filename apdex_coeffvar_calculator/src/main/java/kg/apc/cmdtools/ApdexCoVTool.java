package kg.apc.cmdtools;
/*
 * Please keep this package name for calling this tool with the the command line cmdrunner tool
 */

import java.io.PrintStream;
import java.util.ListIterator;

import org.apache.commons.lang3.math.NumberUtils;

import com.github.rbourga.jmeter.apdexcov.logic.ApdexCoVLogic;

import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.logging.LoggingUtils;

public class ApdexCoVTool extends AbstractCMDTool{

	public ApdexCoVTool() {
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
		String sApdexTgtTholdSec = "4";	// 4s by default
		String sApdexMinScore = "0.85";	// good by default
		String sCofVarMaxPct = "0.30";	// 30% max by default

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
			} else if (arg.equalsIgnoreCase("--apdex-min-score")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Apdex minimun score value missing.");
				}
				sApdexMinScore = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--cov-max-pct")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Coefficient of variation maximum value missing.");
				}
				sCofVarMaxPct = ((String) args.next());
			}
		}

		// Check input-file parameter	
		if (ApdexCoVLogic.isFileNotFound(sInFile)) {
			throw new IllegalArgumentException("Input file not found.");
		}
		if (ApdexCoVLogic.isFileNotValid(sInFile)) {
			throw new IllegalArgumentException("Input file invalid (could not find results).");
		}
		// Check apdex-tgt-thold parameter
		if (!(NumberUtils.isCreatable(sApdexTgtTholdSec))) {
			throw new IllegalArgumentException("Apdex satisfied threshold value invalid.");
		}
		double fApdexTgtTholdSec = Double.parseDouble(sApdexTgtTholdSec);
		if (ApdexCoVLogic.isTgtTHoldOutOfRange(fApdexTgtTholdSec)) {
			throw new IllegalArgumentException("Apdex satisfied threshold value T needs to be greater or equal to 0.1.");			
		}
		// Check apdex-min-score parameter
		if (!(NumberUtils.isCreatable(sApdexMinScore))) {
			throw new IllegalArgumentException("Apdex min score value invalid.");
		}
		double fApdexMinScore = Double.parseDouble(sApdexMinScore);
		if (ApdexCoVLogic.isApdexMinScoreOutOfRange(fApdexMinScore)) {
			throw new IllegalArgumentException("Apdex min score value needs to be between 0 and 1.");			
		}
		// Check cov-max-pct parameter
		if (!(NumberUtils.isCreatable(sCofVarMaxPct))) {
			throw new IllegalArgumentException("Coefficient of Variation max value invalid.");
		}
		double fCofVarMaxPct = Double.parseDouble(sCofVarMaxPct);
		if (ApdexCoVLogic.isCofVarPctOutOfRange(fCofVarMaxPct)) {
			throw new IllegalArgumentException("Coefficient of Variation max value needs to be greater or equal to 0.");			
		}

		// Do the job
		int iResult = ApdexCoVLogic.computeApdexCofVar(sInFile, fApdexTgtTholdSec, fApdexMinScore, fCofVarMaxPct);
		if (iResult == -1) {
			System.out.println("No samplers found in input file - please check your file.");
		} else {
			// Save Apdex results in an HTML file for import in DevOps tool later on
			String htmlFilename = ApdexCoVLogic.saveApdexStatsAsHtml(sInFile, sApdexMinScore, sCofVarMaxPct);
			System.out.println("Results saved in " + htmlFilename);
		}
		return iResult;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'ApdexCoV': --input-file <filenameIn>"
				+ "["
				+ "--apdex-tgt-thold-secs <satisified treshold value in secs (greater than 0.1)>"
				+ "--apdex-min-score <min Apdex score to pass (between 0 and 1)>]"
				+ "--cov-max-pct <max Coefficient of Variation percentage value to pass>"
				+ "]");				
	}
}
