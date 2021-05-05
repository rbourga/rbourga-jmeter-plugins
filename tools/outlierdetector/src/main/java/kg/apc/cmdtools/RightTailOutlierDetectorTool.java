/*
 * Copyright 2021 Robert Bourgault du Coudray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package kg.apc.cmdtools;

import java.io.File;
import java.io.PrintStream;
import java.util.ListIterator;

import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.vizualizers.RightTailOutlierDetectorGui;
import kg.apc.logging.LoggingUtils;

public class RightTailOutlierDetectorTool extends AbstractCMDTool {
	
	public RightTailOutlierDetectorTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
	}
	
	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'RightTailOutlierDetector': --input-file <filenameIn>"
				+ "--tukey-k <K value (1.5 or 3)>");
	}	

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool RightTailOutlierDetector"
		 */
		String _sInputFile = null;
		String _sTukeyK = null;

		if (!args.hasNext()) {
			showHelp(System.out);
			return 0;
		}

		// Process params
		while (args.hasNext()) {
			String arg = (String) args.next();
			if (arg.equalsIgnoreCase("--input-file")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Missing JTL (or CSV) input file name.");
				}
				_sInputFile = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--tukey-k")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Missing constant K value.");
				}
				_sTukeyK = ((String) args.next());
			}
		}

		// Check params
		if (_sInputFile == null) {
			throw new IllegalArgumentException("Missing input file specification.");
		}
		if (!(new File(_sInputFile).exists())) {
			throw new IllegalArgumentException("Cannot find specified file: " + _sInputFile);
		}
		if (_sTukeyK == null) {
			throw new IllegalArgumentException("Missing Tukey constant K.");
		}
		if (!(_sTukeyK.equals("1.5")) && !(_sTukeyK.equals("3")) && !(_sTukeyK.equals("3.0"))) {
			throw new IllegalArgumentException("Invalid K value (only 1.5 or 3 accepted)");
		}

		// Do the job:
		RightTailOutlierDetectorGui _oRightTailOutlierDetectorGui = new RightTailOutlierDetectorGui();
		_oRightTailOutlierDetectorGui.createDataModelTable();
		int _iTrimResult = _oRightTailOutlierDetectorGui.outlierDetection(_sInputFile, Double.parseDouble(_sTukeyK));

		// Process the results
		System.out.println("RightTailOutlierDetectorTool: " + _iTrimResult);
		switch (_iTrimResult) {
		case -1:
			System.out.println("No samplers found in input file - please check your file.");
			break;
		case 0:
			System.out.println("No outliers found in the right tail.");
			break;
		default:
			_oRightTailOutlierDetectorGui.saveDataModelTable();
			System.out.println(_iTrimResult + " outliers found in the right tail.\n" + "Refer to the _Outliers and _Trimmed files.");
			System.out.println("Trimming stats saved to _TrimSummary.csv file.");
		}
		return 0;
	}
}
