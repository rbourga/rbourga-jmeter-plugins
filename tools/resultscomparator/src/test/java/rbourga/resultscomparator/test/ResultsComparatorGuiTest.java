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

package rbourga.resultscomparator.test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.save.CSVSaveService;
import org.junit.BeforeClass;
import org.junit.Test;

import kg.apc.emulators.TestJMeterUtils;
import kg.apc.jmeter.JMeterPluginsUtils;

public class ResultsComparatorGuiTest {

	private static String DATA_DIR;
	private static SampleSaveConfiguration oSampleSaveConfig = null;

	@BeforeClass
	public static void setUpClass() throws Exception {	
		TestJMeterUtils.createJmeterEnv();
		DATA_DIR = TestJMeterUtils.getTempDir();
		System.out.println("ResultsComparatorGuiTest: get test files at " + DATA_DIR);
	}

	public ResultsComparatorGuiTest() {
	}
	
	private SampleEvent transformResultIntoEvent(SampleResult oSampleResult) {
		oSampleResult.setAllThreads(0);
		oSampleResult.setConnectTime(0);
		oSampleResult.setIdleTime(0);
		oSampleResult.setLatency(0);
		oSampleResult.setSaveConfig(oSampleSaveConfig);
		oSampleResult.setSuccessful(true);
		SampleEvent _oSampleEvent = new SampleEvent(oSampleResult, null);
		return _oSampleEvent;
	}
	
	private PrintWriter initializeFileOutput(String sFilename) {
		PrintWriter _oPrintWriter = null;
		try {
			_oPrintWriter = new PrintWriter(sFilename);
			// Save the header line
			_oPrintWriter.println(CSVSaveService.printableFieldNamesToString());
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		return _oPrintWriter;
	}

	/**
	 * Test of actionPerformed method, of class ResultsComparatorGui
	 */
	@Test
	public void testCreateFiles() {
		// Creates Control and Variation files for UI testing
		long _lStart, _lEnd;
		SampleEvent _oSampleEvent = null;
		SampleResult _oSampleResult;
		
        oSampleSaveConfig = new SampleSaveConfiguration();
        JMeterPluginsUtils.doBestCSVSetup(oSampleSaveConfig);

		String _sFilePathA = DATA_DIR + "/Control.csv";
		String _sFilePathB = DATA_DIR + "/Variation.csv";
		PrintWriter _oPrintWriterA = initializeFileOutput(_sFilePathA);
		PrintWriter _oPrintWriterB = initializeFileOutput(_sFilePathB);

		_lStart = System.currentTimeMillis();
		Random _oRandom = new Random();

		// Creation of 30 test results with random times between 100 and 300ms
		// Samplers only in A
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(200) + 100;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("OnlyInA");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterA.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}
		// Samplers only in B
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(200) + 100;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("OnlyInB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterB.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}

		// Only 1 in A
		for (int i = 0; i < 1; i++) {
			_lEnd = _lStart + _oRandom.nextInt(200) + 100;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("Only1InA");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterA.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(200) + 100;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("Only1InA");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterB.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}

		// Only 1 in B
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(200) + 100;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("Only1InB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterA.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}
		for (int i = 0; i < 1; i++) {
			_lEnd = _lStart + _oRandom.nextInt(200) + 100;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("Only1InB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterB.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}

		// A and B equal
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(300) + 150;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("AequalToB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterA.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
			_oPrintWriterB.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}
		
		// A faster than B
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(200) + 100;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("AfasterThanB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterA.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(400) + 200;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("AfasterThanB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterB.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}

		// A slower than B
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(600) + 300;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("AslowerThanB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterA.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(400) + 200;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("AslowerThanB");
			_oSampleEvent = transformResultIntoEvent(_oSampleResult);
			_oPrintWriterB.println(CSVSaveService.resultToDelimitedString(_oSampleEvent));
	        _lStart = _lEnd;
		}

		// Close the files
		_oPrintWriterA.close();
		_oPrintWriterB.close();
		
	}
}
