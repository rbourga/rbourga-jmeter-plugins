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

package rbourga.maths.outlierdetection;

import java.util.List;

import org.apache.jmeter.samplers.SampleResult;

public class UpperFenceOutlierDetector {

	public double getUpperFence(List<SampleResult> dataList, double k) {
		/*
		 * @dataList is a list of sample results that must have been sorted by increasing
		 * values of elapsed time.
		 * @k Tukey K parameter
		 * This function returns the upper fence of the datalist per Tukey's criteria:
		 * upperFence = Q3 + k*(Q3-Q1) where:
		 * Q1 = median of the lower half of the data
		 * Q3 = median of the upper half of the data
		 * To find Q1 and Q3 hinges:
		 * - if there is an odd number of data points in the datalist, we include the median (i.e., the central
		 * value in the ordered list) in both halves;
		 * - if there is an even number of data points, we split this data set exactly in half;
		 * Also please note that, when using indexes, we substract 1 because arrays start at 0!
		 */
		int _iMidHinge, _iLowerHinge, _iUpperHinge;
		double _fQ1Elapsed, _fQ3Elapsed;

		if ((dataList.size() % 2) != 0) {
			// Odd set of numbers in the dataList
			_iMidHinge = (dataList.size() + 1) / 2;
			if ((_iMidHinge % 2) != 0) {
				// Odd set of numbers in each half
				_iLowerHinge = (_iMidHinge + 1) / 2;
				_fQ1Elapsed = dataList.get(_iLowerHinge - 1).getTime();
				_iUpperHinge = _iLowerHinge + _iMidHinge - 1;
				_fQ3Elapsed = dataList.get(_iUpperHinge - 1).getTime();
			} else {
				// Even size in each half: return the average of the 2 middle values
				_iLowerHinge = _iMidHinge / 2;
				_fQ1Elapsed = (dataList.get(_iLowerHinge - 1).getTime() + dataList.get(_iLowerHinge).getTime()) / 2.0;
				_iUpperHinge = _iLowerHinge + _iMidHinge;
				_fQ3Elapsed = (dataList.get(_iUpperHinge - 2).getTime() + dataList.get(_iUpperHinge - 1).getTime())
						/ 2.0;
			}
		} else {
			// Even set of numbers in the dataList
			_iMidHinge = dataList.size() / 2;
			if ((_iMidHinge % 2) != 0) {
				// Odd set of numbers in each half
				_iLowerHinge = (_iMidHinge + 1) / 2;
				_fQ1Elapsed = dataList.get(_iLowerHinge - 1).getTime();
				_iUpperHinge = _iLowerHinge + _iMidHinge;
				_fQ3Elapsed = dataList.get(_iUpperHinge - 1).getTime();
			} else {
				// Even size in each half: return the average of the 2 middle values
				_iLowerHinge = _iMidHinge / 2;
				_fQ1Elapsed = (dataList.get(_iLowerHinge - 1).getTime() + dataList.get(_iLowerHinge).getTime()) / 2.0;
				_iUpperHinge = _iLowerHinge + _iMidHinge;
				_fQ3Elapsed = (dataList.get(_iUpperHinge - 1).getTime() + dataList.get(_iUpperHinge).getTime()) / 2.0;
			}
		}

		// Return the upper fence value
		double _fIqr = _fQ3Elapsed - _fQ1Elapsed; // inter-quartile range
		double _fUpLimit = _fQ3Elapsed + (k * _fIqr);
		return _fUpLimit;
	}
}
