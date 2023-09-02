package com.github.rbourga.jmeter.apdexcov.test;

import java.io.PrintWriter;
import java.util.Random;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.junit.BeforeClass;
import org.junit.Test;

import kg.apc.emulators.TestJMeterUtils;
import com.github.rbourga.jmeter.common.TestResultsServices;

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
		PrintWriter oPrintWriter = TestResultsServices.initCSVtestResultsFile(sFilePath);

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
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// Creation of 30 results with random times between 2 and 3s to test Small Group¿failed if > 2.5s
		lStart = lInitial;		
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(1000) + 2000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("SmallGroup_Failed");
			if ((lEnd - lStart) > 2500) {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, false);
			}
			else {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			}
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// Creation of 100 results with random times between 3 and 5s to test Apdex score, failed if > 4.5s
		lStart = lInitial;		
		for (int i = 0; i < 100; i++) {
			lEnd = lStart + oRandom.nextInt(2000) + 3000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Successful_Result");
			if ((lEnd - lStart) > 4500) {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, false);
			}
			else {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			}
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		oPrintWriter.close();
		System.out.println("*****");
		System.out.println("");
		System.out.println("***** ApdexCovTest: for unit tests, get a test file at " + sJMeterTempDir); 
		System.out.println("");
		System.out.println("*****");
    }

}
