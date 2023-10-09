/**
 *
 */
package com.github.rbourga.jmeter.jmx2tc.logic;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.samplers.SampleSaveConfiguration;

public final class Jmx2TcLogic {

	public static int SelectAndRenameColumns(String sInFilepath) {
		CSVFormat csvFmtIn, csvFmtOut;

		// Initialize output file name
		String sFileDirectoryName = FilenameUtils.getFullPath(sInFilepath);
		String sFileBaseName = FilenameUtils.getBaseName(sInFilepath);
		String sFileExtension = FilenameUtils.getExtension(sInFilepath);
		String sOutFile = sFileDirectoryName + sFileBaseName + "_TC." + sFileExtension;

		// Get the delimiter separator of the input file from current JMeter properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		if (cDelim == ',') {
			// Comma separated format
			csvFmtIn = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build();
		} else {
			// Tab-delimited format
			csvFmtIn = CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build();
		}

		// Read the input CSV file
		FileReader fileRdr = null;
		try {
			fileRdr = new FileReader(sInFilepath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Iterable<CSVRecord> inRcds = null;
		try {
			inRcds = csvFmtIn.parse(fileRdr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!inRcds.iterator().hasNext()) {
			try {
				fileRdr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return -1; // Nothing loaded, so abort...
		}

		// Create a list to hold the data for the selected columns
		List<String[]> outDataList = new ArrayList<>();

		// Extract and store the data for selected columns
		for (CSVRecord rcd : inRcds) {
			String sStartTime = rcd.get("timeStamp");
			String sSpendTime = rcd.get("elapsed");
			String sLabel = rcd.get("label");
			String sIsSuccessful = rcd.get("success");
			String[] outRow = {sStartTime, sSpendTime, sLabel, sIsSuccessful};
			outDataList.add(outRow);
		}

		// CSVParser parses the CSV data lazily; so only close the reader when done.
		try {
			fileRdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Define the header for the new Teamcity file & write the data to the output Teamcity file
		String[] tcHeader = {"startTime", "spendTime", "label", "isSuccessful"};

		FileWriter fileWrtr = null;
		try {
			fileWrtr = new FileWriter(sOutFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		csvFmtOut = CSVFormat.TDF.builder().setHeader(tcHeader).build();
		try (CSVPrinter csvPrinter = new CSVPrinter(fileWrtr, csvFmtOut)) {
			try {
				csvPrinter.printRecords(outDataList);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			fileWrtr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
