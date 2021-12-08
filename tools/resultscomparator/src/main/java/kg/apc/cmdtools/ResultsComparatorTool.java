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
import kg.apc.jmeter.vizualizers.ResultsComparatorGui;
import kg.apc.logging.LoggingUtils;

public class ResultsComparatorTool extends AbstractCMDTool {

	public ResultsComparatorTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'ResultsComparatorTool': --thld <Cohen's d max effect size> "
				+ "--ctrl-file <Control file "
				+ "--var-file <Variation file");		
	}

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool ResultsComparatorTool"
		 */
		String _sCtrlFile = null;
		String _sVarFile = null;
		String _sDThld = null;

		if (!args.hasNext()) {
			showHelp(System.out);
			return 0;
		}

		// Process params
		while (args.hasNext()) {
			String arg = (String) args.next();
			if (arg.equalsIgnoreCase("--ctrl-file")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Missing JTL (or CSV) control file name.");
				}
				_sCtrlFile = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--var-file")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Missing JTL (or CSV) variation file name.");
				}
				_sVarFile = ((String) args.next());
			} else if (arg.equalsIgnoreCase("--thld")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Missing Cohen's d max threshold value.");
				}
				_sDThld = ((String) args.next());
			}
		}

		// Check params
		if (_sCtrlFile == null) {
			throw new IllegalArgumentException("Missing control file specification.");
		}
		if (!(new File(_sCtrlFile).exists())) {
			throw new IllegalArgumentException("Cannot find control file: " + _sCtrlFile);
		}
		if (_sVarFile == null) {
			throw new IllegalArgumentException("Missing variation file specification.");
		}
		if (!(new File(_sVarFile).exists())) {
			throw new IllegalArgumentException("Cannot find variation file: " + _sVarFile);
		}
		if (_sDThld == null) {
			throw new IllegalArgumentException("Missing Cohen's d max threshold.");
		}
		//double _fThld = Double.valueOf(_sDThld);
		if (Double.parseDouble(_sDThld) < 0) {
			throw new IllegalArgumentException("Invalid thld value (only equal or greater than 0 accepted).");
		}

		// Do the job:
		ResultsComparatorGui _oResultsComparatorGui = new ResultsComparatorGui();
		_oResultsComparatorGui.createDataModelTable();
		int _iCompareResult = _oResultsComparatorGui.resultsCompare(_sCtrlFile, _sVarFile, Double.parseDouble(_sDThld));
		// Process the results
		switch (_iCompareResult) {
		case -1:
			System.out.println("No samplers found in Control file - please check your file.");
			break;
		case -2:
			System.out.println("No samplers found in Variation file - please check your file.");
			break;
		default:
			// Export results to html
			_oResultsComparatorGui.saveDataModelTableAsHtml();
			System.out.println(_iCompareResult + " elements found to be worsened.");
		}
		return _iCompareResult;
	}

}
