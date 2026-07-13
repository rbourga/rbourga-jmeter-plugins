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

public class ModalityCoVTool extends AbstractCMDTool {

    private static final String DEFAULT_MVALUE_THRESHOLD = "2.4";   // 2.4 by default
    private static final String DEFAULT_COV_ALIM_PCT = "0.30";      // 30% (as decimal) by default
    private static final String DEFAULT_MIN_BIN_SIZE = "100";       // 100 by default

    public ModalityCoVTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
    }

    @Override
    protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
        /**
         * Called by the Universal Command Line Tool runner as in "cmdrunner --tool ModalityCoV"
         */
        String sInFile = null;
        String sMvalueThreshold = DEFAULT_MVALUE_THRESHOLD;
        String sCoVALPct = DEFAULT_COV_ALIM_PCT;
        String sMinBinSize = DEFAULT_MIN_BIN_SIZE;

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
                sInFile = (String) args.next();
            } else if (arg.equalsIgnoreCase("--mvalue-thold")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("MValue threshold value missing.");
                }
                sMvalueThreshold = (String) args.next();
            } else if (arg.equalsIgnoreCase("--cov-alim-pct")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Coefficient of Variation acceptable limit value missing.");
                }
                sCoVALPct = (String) args.next();
            } else if (arg.equalsIgnoreCase("--min-bin-size")) {
                if (!args.hasNext()) {
                    throw new IllegalArgumentException("Min Bin Size value missing.");
                }
                sMinBinSize = (String) args.next();
            }
        }

        // Required parameter: input-file
        if (sInFile == null) {
            throw new IllegalArgumentException("Required parameter --input-file is missing.");
        }

        // Check input-file parameter
        if (!FileServices.isFileExist(sInFile)) {
            throw new IllegalArgumentException("Input file not found.");
        }
        if (!FileServices.isFileValid(sInFile)) {
            throw new IllegalArgumentException("Input file invalid (could not find results).");
        }

        // Check mvalue-thold parameter
        if (!NumberUtils.isCreatable(sMvalueThreshold)) {
            throw new IllegalArgumentException("MValue threshold value invalid.");
        }
        double fMvalueThold = Double.parseDouble(sMvalueThreshold);
        if (MultimodalityCoVLogic.isMvalueTHoldOutOfRange(fMvalueThold)) {
            throw new IllegalArgumentException("MValue threshold value needs to be greater or equal to 0.1.");
        }

        // Check cov-alim-pct parameter
        // Accepts decimal (e.g. 0.30) or percent (e.g. 30 or 30%)
        String covInput = sCoVALPct == null ? "" : sCoVALPct.trim();
        boolean hadPercentSign = false;
        if (covInput.endsWith("%")) {
            hadPercentSign = true;
            covInput = covInput.substring(0, covInput.length() - 1).trim();
        }
        if (!NumberUtils.isCreatable(covInput)) {
            throw new IllegalArgumentException("Coefficient of Variation acceptable limit value invalid.");
        }
        double fCoVALPct = Double.parseDouble(covInput);
        // If user provided a percent-like value (e.g., 30 or "30%") or a number > 1, treat as percentage and convert to decimal
        if (hadPercentSign || fCoVALPct > 1.0) {
            fCoVALPct = fCoVALPct / 100.0;
        }
        if (MultimodalityCoVLogic.isCoVPctOutOfRange(fCoVALPct)) {
            throw new IllegalArgumentException("Coefficient of Variation acceptable limit value needs to be greater or equal to 0.");
        }

        // Check min-bin-size parameter - strict integer parse
        int iMinBinSize;
        try {
            iMinBinSize = Integer.parseInt(sMinBinSize);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Min Bin Size value invalid.");
        }
        if (MultimodalityCoVLogic.isMinBinSizeOutOfRange(iMinBinSize)) {
            throw new IllegalArgumentException("Minimum Bin Size needs to be greater than 0.");
        }

        // Do the job
        int iResult = MultimodalityCoVLogic.computeMvalueCoV(sInFile, fMvalueThold, fCoVALPct, iMinBinSize);
        if (iResult == -1) {
            System.out.println("No samplers found in input file - please check your file.");
        } else {
            // Save Modality & CoV results in an HTML file for import in DevOps tool later on
            // Pass original strings for saved report so it reflects user-provided values
            String htmlFilename = MultimodalityCoVLogic.saveTableStatsAsHtml(sInFile, sMvalueThreshold, sCoVALPct);
            System.out.println("Results saved in " + htmlFilename);
        }
        return iResult;
    }

    @Override
    protected void showHelp(PrintStream os) {
        os.println("Options for tool 'ModalityCoV':");
        os.println("  --input-file <filenameIn>              Input JMeter results file (required)");
        os.println("  --min-bin-size <minimum bin size>      Minimum bin size for modality test (default: " + DEFAULT_MIN_BIN_SIZE + ")");
        os.println("  --mvalue-thold <modality threshold>    Modality detection threshold (>= 0.1) (default: " + DEFAULT_MVALUE_THRESHOLD + ")");
        os.println("  --cov-alim-pct <decimal|percent>       Coefficient of Variation acceptable limit (decimal e.g. 0.30,");
        os.println("                                         or percent e.g. 30 or 30%). Default: " + DEFAULT_COV_ALIM_PCT);
    }
}
