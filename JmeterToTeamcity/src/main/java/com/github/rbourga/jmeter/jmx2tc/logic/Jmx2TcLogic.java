/**
 *
 */
package com.github.rbourga.jmeter.jmx2tc.logic;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.jmeter.samplers.SampleSaveConfiguration;

public final class Jmx2TcLogic {

	public static int SelectAndRenameColumns(String sInFilepath) {
		// Initialize output file name
		String sFileDirectoryName = FilenameUtils.getFullPath(sInFilepath);
		String sFileBaseName = FilenameUtils.getBaseName(sInFilepath);
		String sFileExtension = FilenameUtils.getExtension(sInFilepath);
		String sOutFile = sFileDirectoryName + sFileBaseName + "_TC." + sFileExtension;

		// Let's define the columns we want to select and their new names
		List<String> inCol = Arrays.asList("timeStamp", "elapsed", "label", "success");
		List<String> outCol = Arrays.asList("startTime", "spendTime", "label", "isSuccessful");

		// Get the delimiter separator of the input file from current JMeter properties
		char cDelim = SampleSaveConfiguration.staticConfig().getDelimiter().charAt(0);
		CSVParser csvParser = new CSVParserBuilder().withSeparator(cDelim).build();

		// Read the input CSV file
		try {
			CSVReader csvRdr = new CSVReaderBuilder(new FileReader(sInFilepath))
					.withCSVParser(csvParser)
					.build();
			List<String[]> inRows = null;
			try {
				inRows = csvRdr.readAll();
			} catch (CsvException e) {
				e.printStackTrace();
			}
			csvRdr.close();

			if (inRows.isEmpty()) {
				return -1; // Nothing loaded, so abort...
			}

			// Find the indices of the selected columns in the header row
			String[] jmxHeaders = inRows.get(0);
			List<Integer> selectedIndices = new ArrayList<>();
			for (String sColName : inCol) {
				int iColIndex = Arrays.asList(jmxHeaders).indexOf(sColName);
				selectedIndices.add(iColIndex);
			}

			// Rename the selected columns in the header row
			for (int i = 0; i < selectedIndices.size(); i++) {
				int iColIndex = selectedIndices.get(i);
				if (iColIndex != -1) {
					jmxHeaders[iColIndex] = outCol.get(i);
				}
			}

			// Create a list to hold the data for the selected columns
			List<String[]> outSelectedData = new ArrayList<>();

			// Extract and store the data for the selected columns
			for (String[] row : inRows) {
				String[] outRow = new String[inCol.size()];
				for (int i = 0; i < inCol.size(); i++) {
					int iColIndex = selectedIndices.get(i);
					if (iColIndex != -1) {
						outRow[i] = row[iColIndex];
					}
				}
				outSelectedData.add(outRow);
			}

			// Write the updated header row and data rows to the output tabbed file
			CSVWriter csvWrtr = (CSVWriter) new CSVWriterBuilder(new FileWriter(sOutFile))
					.withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER)
					.withSeparator('\t')
					.build();
			csvWrtr.writeAll(outSelectedData);
			csvWrtr.close();

		}  catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
