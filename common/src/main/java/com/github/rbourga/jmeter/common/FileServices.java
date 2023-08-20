/**
 *
 */
package com.github.rbourga.jmeter.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jmeter.save.CSVSaveService;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Various methods for handling files
 */
public final class FileServices {

	public static boolean isValidFile(String sFilePath) {
		try (BufferedReader brRdr = new BufferedReader(new FileReader(sFilePath))) {
			String sLine = brRdr.readLine();
			brRdr.close();
			return sLine != null && !sLine.isEmpty(); // Check if the first line is not empty
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Map<String, List<CSVRecord>> loadSamplesIntoHashMap(String sFilePath, char cDelim) {
		// Store the samples and group them by their labels in separate lists
		HashMap<String, List<CSVRecord>> rcdHashMap = new HashMap<String, List<CSVRecord>>();
		CSVFormat csvFmt;

		if (cDelim == ',') {
			// Comma separated format
			csvFmt = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build();
		} else {
			// Tab-delimited format
			csvFmt = CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build();
		}

		try (FileReader fileRdr = new FileReader(sFilePath);
				CSVParser csvParser = new CSVParser(fileRdr, csvFmt)) {
			for (CSVRecord rcd : csvParser) {
				String sLabel = rcd.get("label");
				if (!rcdHashMap.containsKey(sLabel)) {
					// New label
					rcdHashMap.put(sLabel, new ArrayList<CSVRecord>());
				}
				rcdHashMap.get(sLabel).add(rcd);
			}
			csvParser.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rcdHashMap;
	}

	public static void saveTableAsCsv(String sFilePath, DefaultTableModel tblMdl) {
		// By default, data saved with comma separated values
		FileWriter fileWrtr = null;
		try {
			fileWrtr = new FileWriter(sFilePath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			CSVSaveService.saveCSVStats(tblMdl, fileWrtr);
			fileWrtr.close();
		} catch (IOException ioE) {
			ioE.printStackTrace();
		}
	}

	public static void saveTableAsHTML(String sFilePath, String sTblCaption, DefaultTableModel tblMdl, int iBoolColNbr) {
		/**
		 * This method saves the data from a table model into an HTML file. Background
		 * color of the rows depend on iBoolClnNbr and will be set as follow: -1:
		 * default color, any positive number: will check for value in the column number and
		 * if: true: orange false: green
		 */
		// Create empty HTML shell
		Document docHtml = Document.createShell("");
		Element eBdy = docHtml.body();
		// Add table with border & caption
		Element eTbl = eBdy.appendElement("table border");
		eTbl.append("<caption>" + sTblCaption + "</caption>");
		// Write the table headers
		Element eTrHdr = docHtml.createElement("tr");
		int colCnt = tblMdl.getColumnCount();
		for (int i = 0; i < colCnt; i++) {
			Element eTh = docHtml.createElement("th");
			eTh.text(tblMdl.getColumnName(i));
			eTrHdr.appendChild(eTh);
		}
		eTbl.appendChild(eTrHdr);
		// Write the data rows
		Element eTrData = null;
		int rowCnt = tblMdl.getRowCount();
		for (int i = 0; i < rowCnt; i++) {
			// Highlight the row if failed
			if (iBoolColNbr == -1) {
				eTrData = docHtml.createElement("tr");
			} else if (tblMdl.getValueAt(i, iBoolColNbr).toString().equalsIgnoreCase("true")) {
				eTrData = docHtml.createElement("tr style=\"background-color: orange\"");
			} else {
				eTrData = docHtml.createElement("tr style=\"background-color: lawngreen\"");
			}
			for (int j = 0; j < colCnt; j++) {
				Element eTd = docHtml.createElement("td");
				eTd.text(tblMdl.getValueAt(i, j).toString());
				eTrData.appendChild(eTd);
			}
			eTbl.appendChild(eTrData);
		}

		// Save the HTML to the file
		FileWriter fileWrtr = null;
		try {
			fileWrtr = new FileWriter(sFilePath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			fileWrtr.write(docHtml.toString());
			fileWrtr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
