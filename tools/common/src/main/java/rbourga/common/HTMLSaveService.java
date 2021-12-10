package rbourga.common;

import java.io.FileWriter;
import java.io.IOException;

import javax.swing.table.DefaultTableModel;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class HTMLSaveService {

	/**
	 * Method saves data from a table model into an HTML file. Background color of
	 * the rows depend on iFailedClnNbr and will be set as follow:
	 * -1: default
	 * any positive number: will check for value in the column number and if:
	 *  true: orange
	 *  false: green
	 */
	public static void saveHTMLtable(String sTblCaption, DefaultTableModel tblModel, FileWriter fWriter,
			int iBoolClnNbr) {
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

}
