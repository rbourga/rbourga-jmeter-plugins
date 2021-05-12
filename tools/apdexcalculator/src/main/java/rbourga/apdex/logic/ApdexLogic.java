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

package rbourga.apdex.logic;

import java.math.BigDecimal;

public class ApdexLogic {
	
	public double formatThreshold(double thrhld) {
		// Format the threshold as per Apdex specs
		// For values greater than 10s, define the value to one second
		if (thrhld >= 10.0) {
			thrhld = Math.rint(thrhld);
		}
		// For values greater than 100s, define the value to 10 seconds
		if (thrhld >= 100.0) {
			thrhld = 10 * (Math.rint(thrhld / 10));
		}
		// For values greater than 1000s, follow the same two significant digits restriction
		if (thrhld >= 1000.0) {
			thrhld = 100 * (Math.rint(thrhld / 100));
		}
		return thrhld;
	}
	
	public String setRating(BigDecimal bdScore) {
		// Sets the rating as per Apdex specs
		String _sRating = "Unacceptable";	// grey
		if (bdScore.doubleValue() >= 0.94) _sRating = "Excellent";	// blue
		else if (bdScore.doubleValue() >= 0.85) _sRating = "Good";	// green
		else if (bdScore.doubleValue() >= 0.70) _sRating = "Fair";	// yellow
		else if (bdScore.doubleValue() >= 0.50) _sRating = "Poor";	// red
		return _sRating;
	}
}
