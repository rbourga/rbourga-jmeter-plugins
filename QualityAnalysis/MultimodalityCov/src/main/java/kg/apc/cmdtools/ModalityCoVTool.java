package kg.apc.cmdtools;
/*
 * Please keep this package name for calling this tool with the the command line cmdrunner tool
 */

import java.io.PrintStream;
import java.util.ListIterator;

import org.apache.commons.lang3.math.NumberUtils;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.multimodalitycov.logic.MultimodalityCoVLogic;

import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.logging.LoggingUtils;

public class ModalityCoVTool extends AbstractCMDTool{

	public ModalityCoVTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
	}

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool ModalityCoV [--help]"
		 */
		String sInFile = null;
		String sMvalueThold = "2.4";	// 2.4 by default
		String sCoVALPct = "0.30";	// 30% max by default

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
			} else if (arg.equalsIgnoreCase("--mvalue-thold")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("MValue threshold value missing.");
				}
				sMvalueThold = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--cov-alim-pct")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Coefficient of Variation acceptable limit value missing.");
				}
				sCoVALPct = ((String) args.next());
			}
		}

		// Check input-file parameter
		if (!(FileServices.isFileExist(sInFile))) {
			throw new IllegalArgumentException("Input file not found.");
		}
		if (!(FileServices.isFileValid(sInFile))) {
			throw new IllegalArgumentException("Input file invalid (could not find results).");
		}
		// Check mvalue-thold parameter
		if (!(NumberUtils.isCreatable(sMvalueThold))) {
			throw new IllegalArgumentException("MValue threshold value invalid.");
		}
		double fMvalueThold = Double.parseDouble(sMvalueThold);
		if (MultimodalityCoVLogic.isMvaleTHoldOutOfRange(fMvalueThold)) {
			throw new IllegalArgumentException("MValue threshold value needs to be greater or equal to 0.1.");			
		}
		// Check cov-max-pct parameter
		if (!(NumberUtils.isCreatable(sCoVALPct))) {
			throw new IllegalArgumentException("Coefficient of Variation acceptable limit value invalid.");
		}
		double fCoVALPct = Double.parseDouble(sCoVALPct);
		if (MultimodalityCoVLogic.isCoVPctOutOfRange(fCoVALPct)) {
			throw new IllegalArgumentException("Coefficient of Variation acceptable limit value needs to be greater or equal to 0.");			
		}

		// Do the job
		int iResult = MultimodalityCoVLogic.computeMvalueCoV(sInFile, fMvalueThold, fCoVALPct);
		if (iResult == -1) {
			System.out.println("No samplers found in input file - please check your file.");
		} else {
			// Save Modality & CoV results in an HTML file for import in DevOps tool later on
			String htmlFilename = MultimodalityCoVLogic.saveTableStatsAsHtml(sInFile, sMvalueThold, sCoVALPct);
			System.out.println("Results saved in " + htmlFilename);
		}
		return iResult;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'ModalityCoV': --input-file <filenameIn> "
				+ "["
				+ "--mvalue-thold <modality detection treshold (greater than 0.1)> "
				+ "--cov-alim-pct <Coefficient of Variation acceptable limit percentage value to pass>"
				+ "]");				
	}
}
