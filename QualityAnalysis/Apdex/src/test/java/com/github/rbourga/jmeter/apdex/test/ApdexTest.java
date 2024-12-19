package com.github.rbourga.jmeter.apdex.test;

import java.io.PrintWriter;
import java.util.Random;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rbourga.jmeter.common.TestResultsServices;

import kg.apc.emulators.TestJMeterUtils;

public class ApdexTest {

	private static String sJMeterTempDir;

	@BeforeClass
	public static void setUpClass() throws Exception {
		TestJMeterUtils.createJmeterEnv();
		sJMeterTempDir = TestJMeterUtils.getTempDir();
	}

	public ApdexTest() {
	}

	@Test
	public void crteDummyTestResults() {
		/*
		 * This method creates bogus test results for manual testing of the ApdexCov
		 * plugin afterwards.
		 */
		// Setup the results file writer
		String sFilePath = sJMeterTempDir + "/ApdexResults.csv";
		PrintWriter oPrintWriter = TestResultsServices.initCSVtestResultsFile(sFilePath);

		// Now create the results, transform them into events and save them via the
		// CSVservice
		long lEnd;
		long lInitial = System.currentTimeMillis();
		SampleEvent oSampleEvent = null;
		SampleResult oSampleResult;
		Random oRandom = new Random();

		// Creation of 30 results with random times between 2 and 3s to test Small Group
		// failed if > 2.5s
		long lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(1000) + 2000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("SmallGroup_WithErrors");
			if ((lEnd - lStart) > 2500) {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, false);
			} else {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			}
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}

		// Creation of 100 results with random times between 3 and 5s to test Apdex
		// score, failed if > 4.5s
		lStart = lInitial;
		for (int i = 0; i < 100; i++) {
			lEnd = lStart + oRandom.nextInt(2000) + 3000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("NotSmallGroup_WithErrors");
			if ((lEnd - lStart) > 4500) {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, false);
			} else {
				oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			}
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}

		// Creation of 100 results with random times between 1 and 3s to test Apdex
		// score
		lStart = lInitial;
		for (int i = 0; i < 100; i++) {
			lEnd = lStart + oRandom.nextInt(1000) + 2000;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("NotSmallGroup_WithOutErrors");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}

		oPrintWriter.close();
		System.out.println("*****");
		System.out.println("");
		System.out.println("***** ApdexTest: for unit tests, get a test file at " + sJMeterTempDir);
		System.out.println("");
		System.out.println("*****");
	}

}
