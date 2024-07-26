package com.github.rbourga.jmeter.modalitycov.test;

import java.io.PrintWriter;
import java.util.Random;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.CSVSaveService;
import org.junit.BeforeClass;
import org.junit.Test;

import kg.apc.emulators.TestJMeterUtils;
import com.github.rbourga.jmeter.common.TestResultsServices;

public class ModalityCovTest {

	private static String sJMeterTempDir;

	@BeforeClass
	public static void setUpClass() throws Exception {	
		TestJMeterUtils.createJmeterEnv();
		sJMeterTempDir = TestJMeterUtils.getTempDir();
	}

	public ModalityCovTest() {
	}
	
	@Test
	public void crteDummyTestResults() {
		/*
		 * This method creates bogus test results for manual testing of the ModalityCov plugin afterwards.
		 */
		// Setup the results file writer
		String sFilePath = sJMeterTempDir + "/ModalityCoVresults.csv";
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
			oSampleResult.setSampleLabel("Only1Sample_CoV0");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// Creation of 100 results following a Gaussian distribution centered on 1s
		lStart = lInitial;
		double dMean = 1000.0;
		double dStdDev = 100.0;
		for (int i = 0; i < 100; i++) {
			lEnd = (long) (lStart + dMean + dStdDev * oRandom.nextGaussian());
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Unimodal");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		// Creation of 100 results following a Gaussian distribution
		// with one centered on 0.5s and the other centered on 1.5s
		lStart = lInitial;
		dMean = 500.0;
		dStdDev = 100.0;
		for (int i = 0; i < 50; i++) {
			lEnd = (long) (lStart + dMean + dStdDev * oRandom.nextGaussian());
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Bimodal");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}
		dMean = 1500.0;
		dStdDev = 100.0;
		for (int i = 0; i < 50; i++) {
			lEnd = (long) (lStart + dMean + dStdDev * oRandom.nextGaussian());
			oSampleResult = SampleResult.createTestSample(lStart, lEnd);
			oSampleResult.setSampleLabel("Bimodal");
			oSampleEvent = TestResultsServices.resultToEvent(oSampleResult, true);
			oPrintWriter.println(CSVSaveService.resultToDelimitedString(oSampleEvent));
			// Move the time for the next result 
			lStart = lEnd;
		}

		oPrintWriter.close();
		System.out.println("*****");
		System.out.println("");
		System.out.println("***** ModalityCovTest: for unit tests, get a test file at " + sJMeterTempDir); 
		System.out.println("");
		System.out.println("*****");
    }

}
