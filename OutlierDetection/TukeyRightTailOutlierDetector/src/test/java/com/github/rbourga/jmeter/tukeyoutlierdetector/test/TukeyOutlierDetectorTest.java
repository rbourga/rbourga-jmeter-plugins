package com.github.rbourga.jmeter.tukeyoutlierdetector.test;

import java.io.PrintWriter;
import java.util.Random;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.junit.BeforeClass;
import org.junit.Test;

import kg.apc.emulators.TestJMeterUtils;
import com.github.rbourga.jmeter.common.TestResultsServices;

public class TukeyOutlierDetectorTest {

	private static String sJMeterTempDir;

	@BeforeClass
	public static void setUpClass() throws Exception {	
		TestJMeterUtils.createJmeterEnv();
		sJMeterTempDir = TestJMeterUtils.getTempDir();
	}

	public TukeyOutlierDetectorTest() {
	}
	
	@Test
	public void crteDummyTestResults() {
		/*
		 * This method creates bogus test results for manual testing of the OutlierDetector plugin afterwards.
		 */
		// Setup the results file writer
		String sFilePath = sJMeterTempDir + "/TestResultsWithOutliers.csv";
		PrintWriter oPrintWriter = TestResultsServices.initCSVtestResultsFile(sFilePath);

        // Now create 100 results with outliers in the middle, transform them into events and save them via the CSVservice
		long lEnd;
		long lInitial = System.currentTimeMillis();
		SampleEvent oSampleEvent = null;
		SampleResult oSampleResult;
		Random oRandom = new Random();

		// We start with some results with random times from 100 to 299ms
		long lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("BogusUnitTest");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// Addition of 10 results with random times around 1s
		for (int i = 0; i < 10; i++) {
			lEnd = lStart + oRandom.nextInt(1000) + 1000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("BogusUnitTest");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// We finish with some results with normal random times
		for (int i = 0; i < 60; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("BogusUnitTest");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		oPrintWriter.close();
		System.out.println("*****");
		System.out.println("");
		System.out.println("***** TukeyOutlierDetectorTest: for unit tests, get a test file at " + sJMeterTempDir); 
		System.out.println("");
		System.out.println("*****");
    }

}
