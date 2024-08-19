/**
 *
 */
package com.github.rbourga.jmeter.common;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.save.CSVSaveService;

import kg.apc.jmeter.JMeterPluginsUtils;

/**
 * Various methods Used by Test methods
 */
public final class TestResultsServices {

	// Config to save the results
	private static SampleSaveConfiguration sampleSaveConfig = null; // singleton instance

	private static SampleSaveConfiguration getSampleSaveConfig() {
		// Lazy initialization: create the instance if it doesn't exist yet
		if (sampleSaveConfig == null) {
			sampleSaveConfig = new SampleSaveConfiguration();
			JMeterPluginsUtils.doBestCSVSetup(sampleSaveConfig);
		}
		return sampleSaveConfig;
	}

	public static PrintWriter initCSVtestResultsFile(String sFilePath) {
		PrintWriter oPrintWriter = null;
		try {
			oPrintWriter = new PrintWriter(sFilePath);
			SampleSaveConfiguration oSampleSaveConfig = getSampleSaveConfig();
			// Save the header line
			oPrintWriter.println(CSVSaveService.printableFieldNamesToString(oSampleSaveConfig));
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		return oPrintWriter;
	}

	public static SampleEvent resultToEvent(SampleResult oSampleResult, boolean bSuccess) {
		SampleSaveConfiguration oSampleSaveConfig = getSampleSaveConfig();
		oSampleResult.setAllThreads(0);
		oSampleResult.setConnectTime(0);
		oSampleResult.setIdleTime(0);
		oSampleResult.setLatency(0);
		oSampleResult.setSaveConfig(oSampleSaveConfig);
		oSampleResult.setSuccessful(bSuccess);
		SampleEvent oSampleEvent = new SampleEvent(oSampleResult, null);
		return oSampleEvent;
	}

}
