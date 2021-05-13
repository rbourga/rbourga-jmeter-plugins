/*
 * Copyright 2021 Robert Bourgault du Coudray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package rbourga.apdex.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JButton;

import org.apache.jmeter.gui.util.FilePanel;
import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.BeforeClass;
import org.junit.Test;

import kg.apc.emulators.TestJMeterUtils;
import kg.apc.jmeter.vizualisers.ApdexScoreCalculatorGui;

public class ApdexScoreCalculatorGuiTest {

	private static String DATA_DIR;

	@BeforeClass
	public static void setUpClass() throws Exception {	
		TestJMeterUtils.createJmeterEnv();
		DATA_DIR = TestJMeterUtils.getTempDir();
		System.out.println("ApdexScoreCalculatorGuiTest: check files at " + DATA_DIR);
	}

	public ApdexScoreCalculatorGuiTest() {
	}

	/**
	 * Test of actionPerformed method, of class ApdexScoreCalculatorGui
	 */
	@Test
	public void testActionPerformed() {
		long _lStart, _lEnd;
		SampleResult _oSampleResult;
		
		ApdexScoreCalculatorGui _oApdexScoreCalculatorGui = new ApdexScoreCalculatorGui();
		// Set the instance into Unit Test mode
		_oApdexScoreCalculatorGui.enableUnitTests();
		// Set the file to process in the file panel UI
		FilePanel _oFilePanel = _oApdexScoreCalculatorGui.getInputFilePanel();
		String _sFilePath = DATA_DIR + "/BogusUnitTests.csv";
		_oFilePanel.setFilename(_sFilePath);

		/*
		 * Test of add method, of class ApdexScoreCalculatorGui
		 */
		System.out.println("ApdexScoreCalculatorGuiTest: add");
		// Creation of 30 test results with random times between 3 and 5s
		_lStart = System.currentTimeMillis();
		Random _oRandom = new Random();
		for (int i = 0; i < 30; i++) {
			_lEnd = _lStart + _oRandom.nextInt(2000) + 3000;
			_oSampleResult = SampleResult.createTestSample(_lStart,_lEnd);
			_oSampleResult.setSampleLabel("UnitTest");
			_oApdexScoreCalculatorGui.add(_oSampleResult);
	        _lStart = _lEnd;
		}
		
		// Calculate
		System.out.println("ApdexScoreCalculatorGuiTest: ACTION_CALCULATE");
		ActionEvent _oActionEvent_Calculate = new ActionEvent(new JButton(), 1, "calculate");
		_oApdexScoreCalculatorGui.actionPerformed(_oActionEvent_Calculate);
		
		// Save
		System.out.println("ApdexScoreCalculatorGuiTest: ACTION_SAVE");
		ActionEvent _oActionEvent_Save = new ActionEvent(new JButton(), 2, "save");
		_oApdexScoreCalculatorGui.actionPerformed(_oActionEvent_Save);
		// Check for existence of file
		_sFilePath = DATA_DIR + "/BogusUnitTests_ApdexScore.csv";
		File _oFile = new File(_sFilePath);
		assertTrue(_oFile.exists());
	}
	
	/**
	 * Test of clearData method, of class ApdexScoreCalculatorGui
	 */
	@Test
	public void testClearData() {
		System.out.println("ApdexScoreCalculatorGuiTest: clearData");
		ApdexScoreCalculatorGui _oApdexScoreCalculatorGui = new ApdexScoreCalculatorGui();
		_oApdexScoreCalculatorGui.clearData();
	}
	
	/**
	 * Test of clearGui method, of class ApdexScoreCalculatorGui
	 */
	@Test
	public void testClearGui() {
		System.out.println("ApdexScoreCalculatorGuiTest: clearGui");
		ApdexScoreCalculatorGui _oApdexScoreCalculatorGui = new ApdexScoreCalculatorGui();
		_oApdexScoreCalculatorGui.clearGui();
	}

	/**
	 * Test of getLabelResource method, of class ApdexScoreCalculatorGui
	 */
	@Test
	public void testGetLabelResource() {
		System.out.println("ApdexScoreCalculatorGuiTest: getLabelResource");
		ApdexScoreCalculatorGui _oApdexScoreCalculatorGui = new ApdexScoreCalculatorGui();
		String _sLabelResource = _oApdexScoreCalculatorGui.getLabelResource();
		String _sExpectedResult = "ApdexScoreCalculatorGui";
		assertEquals(_sLabelResource, _sExpectedResult);
	}

	/**
	 * Test of getStaticLabel method, of class ApdexScoreCalculatorGui
	 */
	@Test
	public void testGetStaticLabel() {
		System.out.println("ApdexScoreCalculatorGuiTest: getStaticLabel");
		ApdexScoreCalculatorGui _oApdexScoreCalculatorGui = new ApdexScoreCalculatorGui();
		String _sStaticLabel = _oApdexScoreCalculatorGui.getStaticLabel();
		String _sExpectedResult = "jp@gc - Apdex Score Calculator";
		assertEquals(_sStaticLabel, _sExpectedResult);
	}

	/**
	 * Test of getMenuCategories method, of class ApdexScoreCalculatorGui
	 */
	@Test
	public void testGetMenuCategories() {
		System.out.println("ApdexScoreCalculatorGuiTest: getMenuCategories");
		ApdexScoreCalculatorGui _oApdexScoreCalculatorGui = new ApdexScoreCalculatorGui();
		Collection<String> _oCollection = _oApdexScoreCalculatorGui.getMenuCategories();
		Iterator<String> _oIterator = _oCollection.iterator();
		String _sExpectedResult = MenuFactory.NON_TEST_ELEMENTS;
		assertEquals(_sExpectedResult, _oIterator.next());
	}
}
