package kg.apc.cmdtools;
/*
 * Please keep this package name for calling this tool with the the command line cmdrunner tool
 */

import java.io.PrintStream;
import java.util.ListIterator;

import com.github.rbourga.jmeter.common.FileServices;
import com.github.rbourga.jmeter.jmx2tc.logic.Jmx2TcLogic;

import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.logging.LoggingUtils;

public class Jmx2TcTool extends AbstractCMDTool{

	public Jmx2TcTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
	}

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool Jmx2Tc"
		 */
		String sInFile = null;

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
			}
		}

		// Check input-file parameter
		if (!(FileServices.isFileExist(sInFile))) {
			throw new IllegalArgumentException("Input file not found.");
		}
		if (!(FileServices.isFileValid(sInFile))) {
			throw new IllegalArgumentException("Input file invalid (could not find results).");
		}

		// Do the job
		int iResult = Jmx2TcLogic.SelectAndRenameColumns(sInFile);
		if (iResult == -1) {
			System.out.println("No samplers found in input file - please check your file.");
		}
		return iResult;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'Jmx2TcTool': --input-file <filenameIn>");
	}
}
