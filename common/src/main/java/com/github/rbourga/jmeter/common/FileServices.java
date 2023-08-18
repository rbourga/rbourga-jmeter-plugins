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

	public static boolean isValidFile(String sFilepath) {
		try (BufferedReader brReader = new BufferedReader(new FileReader(sFilepath))) {
			String sLine = brReader.readLine();
			brReader.close();
			return sLine != null && !sLine.isEmpty(); // Check if the first line is not empty
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Map<String, List<CSVRecord>> loadSamplesIntoHashMap(String sFilepath, char cDelim) {
		// Store the samples and group them by their labels in separate lists
		HashMap<String, List<CSVRecord>> hashMap = new HashMap<String, List<CSVRecord>>();
		CSVFormat csvFormat;

		if (cDelim == ',') {
			// Comma separated format
			csvFormat = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build();
		} else {
			// Tab-delimited format
			csvFormat = CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build();
		}

		try (FileReader fileReader = new FileReader(sFilepath);
				CSVParser csvParser = new CSVParser(fileReader, csvFormat)) {
			for (CSVRecord record : csvParser) {
				String sLabel = record.get("label");
				if (!hashMap.containsKey(sLabel)) {
					// New label
					hashMap.put(sLabel, new ArrayList<CSVRecord>());
				}
				hashMap.get(sLabel).add(record);
			}
			csvParser.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return hashMap;
	}

	public static void saveHTMLtable(String sTblCaption, DefaultTableModel tblModel, FileWriter fWriter,
			int iBoolClnNbr) {
		/**
		 * This method saves the data from a table model into an HTML file. Background
		 * color of the rows depend on iFailedClnNbr and will be set as follow: -1:
		 * default any positive number: will check for value in the column number and
		 * if: true: orange false: green
		 */
		// Create empty HTML shell
		Document _dHtmlOutput = Document.createShell("");
		Element _body = _dHtmlOutput.body();
		// Add table with border & caption
		Element _table = _body.appendElement("table border");
		_table.append("<caption>" + sTblCaption + "</caption>");
		// Write the table headers
		Element _trh = _dHtmlOutput.createElement("tr");
		int _colCnt = tblModel.getColumnCount();
		for (int i = 0; i < _colCnt; i++) {
			Element _th = _dHtmlOutput.createElement("th");
			_th.text(tblModel.getColumnName(i));
			_trh.appendChild(_th);
		}
		_table.appendChild(_trh);
		// Write the data rows
		int _rowCnt = tblModel.getRowCount();
		Element _trd = null;
		for (int i = 0; i < _rowCnt; i++) {
			// Highlight the row if failed
			if (iBoolClnNbr == -1) {
				_trd = _dHtmlOutput.createElement("tr");
			} else if (tblModel.getValueAt(i, iBoolClnNbr).toString().equalsIgnoreCase("true")) {
				_trd = _dHtmlOutput.createElement("tr style=\"background-color: orange\"");
			} else {
				_trd = _dHtmlOutput.createElement("tr style=\"background-color: lawngreen\"");
			}
			for (int j = 0; j < _colCnt; j++) {
				Element _td = _dHtmlOutput.createElement("td");
				_td.text(tblModel.getValueAt(i, j).toString());
				_trd.appendChild(_td);
			}
			_table.appendChild(_trd);
		}
		try {
			fWriter.write(_dHtmlOutput.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveTableAsCsv(String sFilePath, DefaultTableModel tblModel) {
		// By default, data saved with comma separated values
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(sFilePath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			CSVSaveService.saveCSVStats(tblModel, fileWriter);
			fileWriter.close();
		} catch (IOException ioE) {
			ioE.printStackTrace();
		}
	}
}
