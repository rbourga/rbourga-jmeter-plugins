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

import org.apache.commons.lang3.math.NumberUtils;
import kg.apc.cmd.UniversalRunner;
import kg.apc.jmeter.JMeterPluginsUtils;
import kg.apc.jmeter.vizualisers.ApdexScoreCalculatorGui;
import kg.apc.logging.LoggingUtils;

public class ApdexScoreCalculatorTool extends AbstractCMDTool {

	public ApdexScoreCalculatorTool() {
        super();
        JMeterPluginsUtils.prepareJMeterEnv(UniversalRunner.getJARLocation());
        LoggingUtils.addLoggingConfig();
	}

	@Override
	protected int processParams(ListIterator args) throws UnsupportedOperationException, IllegalArgumentException {
		/**
		 * Called by the Universal Command Line Tool runner as in "cmdrunner --tool ApdexScoreCalculator"
		 */
		String _sInputFile = null;
		String _sTargetThreshold_T = null;

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
			} else if (arg.equalsIgnoreCase("--trhld")) {
				if (!args.hasNext()) {
					throw new IllegalArgumentException("Missing satisfied threshold value.");
				}
				_sTargetThreshold_T = ((String) args.next());
			}
		}

		// Check params
		if (_sInputFile == null) {
			throw new IllegalArgumentException("Missing input file specification.");
		}
		if (!(new File(_sInputFile).exists())) {
			throw new IllegalArgumentException("Cannot find specified file: " + _sInputFile);
		}
		if (_sTargetThreshold_T == null) {
			throw new IllegalArgumentException("Missing satisfied threshold value T.");
		}
		if (!(NumberUtils.isCreatable(_sTargetThreshold_T))) {
			throw new IllegalArgumentException("Invalid satisfied threshold value T.");
		}
		if (Double.parseDouble(_sTargetThreshold_T) < 0.1) {
			throw new IllegalArgumentException("Satisfied threshold value T needs to be >= 0.1.");			
		}

		// Do the job:
		ApdexScoreCalculatorGui _oApdexScoreCalculatorGui = new ApdexScoreCalculatorGui();
		_oApdexScoreCalculatorGui.createDataModelTable();
		boolean _bApdexResult = _oApdexScoreCalculatorGui.computeApdex(_sInputFile, Double.parseDouble(_sTargetThreshold_T));

		// Process the results
		if (!_bApdexResult) {
			System.out.println("No samplers found in input file - please check your file.");
		} else {
			_oApdexScoreCalculatorGui.saveDataModelTable();
			System.out.println("Apdex scores saved to _ApdexScore.csv file.");	
		}
		return 0;
	}

	@Override
	protected void showHelp(PrintStream os) {
		os.println("Options for tool 'ApdexScoreCalculator': --input-file <filenameIn>"
				+ "--trhld <satisified treshold value in secs (greater than 0.1)>");		
	}

}
