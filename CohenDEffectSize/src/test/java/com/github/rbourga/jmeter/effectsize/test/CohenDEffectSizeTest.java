package com.github.rbourga.jmeter.effectsize.test;

import java.io.PrintWriter;
import java.util.Random;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.junit.BeforeClass;
import org.junit.Test;

import kg.apc.emulators.TestJMeterUtils;
import com.github.rbourga.jmeter.common.TestResultsServices;

public class CohenDEffectSizeTest {

	private static String sJMeterTempDir;

	@BeforeClass
	public static void setUpClass() throws Exception {
		TestJMeterUtils.createJmeterEnv();
		sJMeterTempDir = TestJMeterUtils.getTempDir();
	}

	public CohenDEffectSizeTest() {
	}

	@Test
	public void crteDummyTestResults() {
		/*
		 * This method creates bogus test results for manual testing of the ApdexCov plugin afterwards.
		 */
		// Setup the results file writer
		String sFilePathA = sJMeterTempDir + "/Control.csv";
		String sFilePathB = sJMeterTempDir + "/Variation.csv";
		PrintWriter oPrintWriterA = TestResultsServices.initCSVtestResultsFile(sFilePathA);
		PrintWriter oPrintWriterB = TestResultsServices.initCSVtestResultsFile(sFilePathB);

        // Now create the results, transform them into events and save them via the CSVservice
		long lEnd;
		long lInitial = System.currentTimeMillis();
		SampleEvent oSampleEvent = null;
		SampleResult oSampleResult;
		Random oRandom = new Random();

		// Creation of test results with random times between 100 and 300ms
		// 30 Samplers only in A
		long lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Only_InA");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterA.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		// 30 Samplers only in B
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Only_InB");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterB.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}

		// 1 failed Sampler in A and 30 successful in B
		lStart = lInitial;
		for (int i = 0; i < 1; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Only_1_InA");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, false);
			oPrintWriterA.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Only_1_InA");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterB.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		// 30 successful Samplers in A and 1 failed in B
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Only_1_InB");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterA.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		lStart = lInitial;
		for (int i = 0; i < 1; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Only_1_InB");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, false);
			oPrintWriterB.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		// 30 Samplers in A and B
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("B_equalTo_A");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterA.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			oPrintWriterB.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		// 30 failed Samplers in A and B
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("B_equalTo_A_Failed");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, false);
			oPrintWriterA.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			oPrintWriterB.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		// A faster than B
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(200) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("B_SlowerThan_A");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterA.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(400) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("B_SlowerThan_A");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterB.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		// A slower than B
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(600) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("B_FasterThan_A");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterA.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}
		lStart = lInitial;
		for (int i = 0; i < 30; i++) {
			lEnd = lStart + oRandom.nextInt(400) + 100;
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("B_FasterThan_A");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriterB.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result
			lStart = lEnd;
		}

		oPrintWriterA.close();
		oPrintWriterB.close();
		System.out.println("*****");
		System.out.println("");
		System.out.println("***** CohenDEffectSizeTest: for unit tests, get test files at " + sJMeterTempDir);
		System.out.println("");
		System.out.println("*****");
    }

}
