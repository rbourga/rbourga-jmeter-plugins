/**
 *
 */
package com.github.rbourga.jmeter.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.jmeter.save.CSVSaveService;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Various methods for handling files
 */
public final class FileServices {
	
	/**
	 * Enum for row colors based on test result
	 */
	public enum RowColor {
	    DEFAULT(""),
	    FAIL("tomato"),
	    PASS("mediumseagreen"),
	    NA("orange"),
	    YELLOW("yellow");

	    private final String cssColor;

	    RowColor(String cssColor) {
	        this.cssColor = cssColor;
	    }

	    public String getCssColor() {
	        return cssColor;
	    }

	    public static RowColor fromValue(String value) {
	        if (value == null) {
	            return DEFAULT;
	        }

	        String s = value.trim();

	        if ("true".equalsIgnoreCase(s)) {
	            return FAIL;
	        }
	        if ("false".equalsIgnoreCase(s)) {
	            return PASS;
	        }
	        if ("na".equalsIgnoreCase(s)) {
	            return NA;
	        }
	        return DEFAULT;
	    }
	}

	/*
	 * Validation methods
	 */
	public static boolean isFileExist(String sFilePath) {
		return (new File(sFilePath).exists());
	}

	public static boolean isFilenameEmpty(String sFilePath) {
		return sFilePath.isEmpty();
	}

	public static boolean isFileValid(String sFilePath) {
		try (BufferedReader brRdr = new BufferedReader(new FileReader(sFilePath))) {
			String sLine = brRdr.readLine();
			return sLine != null && !sLine.isEmpty(); // Check if the first line is not empty
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * Consolidation methods
	 */
	public static Map<String, List<CSVRecord>> loadSamplesIntoHashMap(String sFilePath, char cDelim) {
		// Stores the samples and group them by their labels in separate lists

		CSVFormat csvFmt;
		if (cDelim == ',') {
			// Comma separated format
			csvFmt = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).build();
		} else {
			// Tab-delimited format
			csvFmt = CSVFormat.TDF.builder().setHeader().setSkipHeaderRecord(true).build();
		}

		HashMap<String, List<CSVRecord>> rcdHashMap = new HashMap<>();
		try (FileReader fileRdr = new FileReader(sFilePath); CSVParser csvParser = new CSVParser(fileRdr, csvFmt)) {
			for (CSVRecord rcd : csvParser) {
				String sLabel = rcd.get("label");
				if (!rcdHashMap.containsKey(sLabel)) {
					// New label
					rcdHashMap.put(sLabel, new ArrayList<>());
				}
				rcdHashMap.get(sLabel).add(rcd);
			}
			csvParser.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rcdHashMap;
	}

	public static void saveCSVRecsToFile(String sFilePath, List<CSVRecord> listCSVRecs, char cDelim) {

		CSVFormat csvFmt;
		if (cDelim == ',') {
			// Comma separated format
			csvFmt = CSVFormat.RFC4180;
		} else {
			// Tab-delimited format
			csvFmt = CSVFormat.TDF;
		}

		try (FileWriter fileWriter = new FileWriter(sFilePath);
				CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFmt)) {

			// Write the header to the file
			csvPrinter.printRecord(listCSVRecs.get(0).getParser().getHeaderNames());

			// Write the records to the file
			for (CSVRecord rcd : listCSVRecs) {
				csvPrinter.printRecord(rcd);
			}

			csvPrinter.flush();
			csvPrinter.close();

		} catch (IOException ioE) {
			// TODO Auto-generated catch block
			ioE.printStackTrace();
		}
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

	public static void saveTableAsHTML(String sFilePath, String sTblCaption, DefaultTableModel tblMdl,
			String highlightColumnName) {
		/**
		 * This method saves the data from a table model into an HTML file. Background
		 * color of the rows depend on highlightColumnName and will be set as follow:
		 * 	null -> default color
		 *  "true" -> red 
		 *  "false" -> green
		 *  "na" -> orange
		 */
		DecimalFormat df4digits = new DecimalFormat("#.####");
		
	    Integer boolColIdx = null;
	    if (highlightColumnName != null && !highlightColumnName.isEmpty()) {
	        boolColIdx = findColumnIndex(tblMdl, highlightColumnName);
	    }
	    Integer smallGroupColIdx = findColumnIndex(tblMdl, "Small Group");

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
		int rowCnt = tblMdl.getRowCount();
		for (int i = 0; i < rowCnt; i++) {
		    Element eTrData = docHtml.createElement("tr");

	        if (boolColIdx != null) {
	            Object value = tblMdl.getValueAt(i, boolColIdx);
	            String sValue = value == null ? "" : value.toString().trim();

	            RowColor rowColor = RowColor.fromValue(sValue);

	            if (rowColor == RowColor.PASS && smallGroupColIdx != null) {
	                Object smallGroupValue = tblMdl.getValueAt(i, smallGroupColIdx);
	                String sSmallGroup = smallGroupValue == null ? "" : smallGroupValue.toString().trim();
	                if ("true".equalsIgnoreCase(sSmallGroup)) {
	                    Integer sampleCountColIdx = findColumnIndex(tblMdl, "# Samples");

	                    if (sampleCountColIdx != null) {
	                        Object sampleCountValue = tblMdl.getValueAt(i, sampleCountColIdx);

	                        try {
	                            double sampleCount = sampleCountValue == null
	                                ? 0
	                                : Double.parseDouble(sampleCountValue.toString().trim());

	                            rowColor = sampleCount < 30 ? RowColor.NA : RowColor.YELLOW;
	                        } catch (NumberFormatException e) {
	                            rowColor = RowColor.NA; // safe fallback
	                        }
	                    } else {
	                        rowColor = RowColor.NA; // no "# Samples" column found
	                    }
	                }
	            }

	            if (rowColor != RowColor.DEFAULT) {
	                eTrData = docHtml.createElement("tr style=\"background-color: " + rowColor.getCssColor() + "\"");
	            }
	        }
				
			for (int j = 0; j < colCnt; j++) {
				Element eTd = docHtml.createElement("td");
				
	            Object cellValue = tblMdl.getValueAt(i, j);
	            String sHtmlOut = cellValue == null ? "" : cellValue.toString();

				// Convert to percentage if needed
				String sColName = tblMdl.getColumnName(j);
	            if (sColName.contains("%") && cellValue instanceof Number) {
	                double dPctValue = ((Number) cellValue).doubleValue() * 100;
					// Only keep 4 digits
	                sHtmlOut = df4digits.format(dPctValue) + "%";
				}
				eTd.text(sHtmlOut);
				eTrData.appendChild(eTd);
			}
			eTbl.appendChild(eTrData);
		}

		// Save the HTML to the file
	    try (FileWriter fileWrtr = new FileWriter(sFilePath)) {
	        fileWrtr.write(docHtml.toString());
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private static Integer findColumnIndex(DefaultTableModel tblMdl, String expectedColumnName) {
	    if (expectedColumnName == null || expectedColumnName.trim().isEmpty()) {
	        return null;
	    }

	    String target = expectedColumnName.trim();

	    for (int col = 0; col < tblMdl.getColumnCount(); col++) {
	        String colName = tblMdl.getColumnName(col);
	        if (colName != null && colName.trim().equalsIgnoreCase(target)) {
	            return col;
	        }
	    }
	    return null;
	}
}
