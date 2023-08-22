package com.github.rbourga.jmeter.apdexcov.test;

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

import com.github.rbourga.jmeter.common.FileServices;

public class ApdexCovTest {

	private static String sJMeterTempDir;

	@BeforeClass
	public static void setUpClass() throws Exception {	
		TestJMeterUtils.createJmeterEnv();
		sJMeterTempDir = TestJMeterUtils.getTempDir();
	}

	public ApdexCovTest() {
	}
	
	@Test
	public void crteDummyTestResults() {
		/*
		 * This method creates bogus test results for manual testing of the ApdexCov plugin afterwards.
		 */
		// Setup the results file writer
		String sFilePath = sJMeterTempDir + "/ApdexCoVresults.csv";
		PrintWriter oPrintWriter = FileServices.initCSVtestResultsFile(sFilePath);

		// Setup the config to save the results
		SampleSaveConfiguration oSampleSaveConfig = new SampleSaveConfiguration();
        JMeterPluginsUtils.doBestCSVSetup(oSampleSaveConfig);

        // Now create the results, transform them into events and save them via the CSVservice
		long lEnd;
		long lInitial = System.currentTimeMillis();
		SampleEvent oSampleEvent = null;
		SampleResult oSampleResult;
		Random oRandom = new Random();

		// Only 1 successful event between 100 to 300ms to test that the CoV is 0
		long lStart = lInitial;
		for (int i = 0; i < 1; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Only1Sample_0Var");
			oSampleEvent = FileServices.resultToEvent(oSampleSaveConfig, oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// Creation of 30 failed results with random times between 2 and 3s to test Small Group
		lStart = lInitial;		
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(1000) + 2000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("SmallGroup_Failed");
			oSampleEvent = FileServices.resultToEvent(oSampleSaveConfig, oSampleResult, false);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// Creation of 100 successful results with random times between 3 and 5s to test Apdex score
		lStart = lInitial;		
		for (int i = 0; i < 100; i++) {
			lEnd = lStart + oRandom.nextInt(2000) + 3000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Successful_Result");
			oSampleEvent = FileServices.resultToEvent(oSampleSaveConfig, oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		oPrintWriter.close();
		System.out.println("*****");
		System.out.println("***** ApdexCovTest: for unit tests, get a test file at " + sJMeterTempDir);        
		System.out.println("*****");
    }

}
