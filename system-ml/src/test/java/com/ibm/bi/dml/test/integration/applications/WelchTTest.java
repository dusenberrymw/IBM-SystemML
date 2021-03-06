/**
 * (C) Copyright IBM Corp. 2010, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.ibm.bi.dml.test.integration.applications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.bi.dml.runtime.matrix.MatrixCharacteristics;
import com.ibm.bi.dml.runtime.matrix.data.MatrixValue.CellIndex;
import com.ibm.bi.dml.test.integration.AutomatedTestBase;
import com.ibm.bi.dml.test.integration.TestConfiguration;
import com.ibm.bi.dml.test.utils.TestUtils;

@RunWith(value = Parameterized.class)
public abstract class WelchTTest extends AutomatedTestBase {
	
	protected final static String TEST_DIR = "applications/welchTTest/";
	protected final static String TEST_WELCHTTEST = "welchTTest";

	protected int numAttr, numPosSamples, numNegSamples;
	
	public WelchTTest(int numAttr, int numPosSamples, int numNegSamples){
		this.numAttr = numAttr;
		this.numPosSamples = numPosSamples;
		this.numNegSamples = numNegSamples;
	}
	
	@Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] { { 5, 100, 150}, { 50, 2000, 1500}, { 50, 7000, 1500}};
		return Arrays.asList(data);
	}
	 
	@Override
	public void setUp() {
		setUpBase();
    	addTestConfiguration(TEST_WELCHTTEST, 
    						 new TestConfiguration(TEST_DIR, 
    								 TEST_WELCHTTEST, 
    								 			   new String[] { "t_statistics", 
    								 							  "degrees_of_freedom" }));
	}
	
	protected void testWelchTTest(ScriptType scriptType) {
		System.out.println("------------ BEGIN " + TEST_WELCHTTEST + " " + scriptType + " TEST {" + numAttr + ", " + numPosSamples + ", " + numNegSamples + "} ------------");
		this.scriptType = scriptType;
		
		TestConfiguration config = getTestConfiguration(TEST_WELCHTTEST);
		loadTestConfiguration(config);
		
		List<String> proArgs = new ArrayList<String>();
		if (scriptType == ScriptType.PYDML) {
			proArgs.add("-python");
		}
		proArgs.add("-args");
		proArgs.add(input("posSamples"));
		proArgs.add(input("negSamples"));
		proArgs.add(output("t_statistics"));
		proArgs.add(output("degrees_of_freedom"));
		programArgs = proArgs.toArray(new String[proArgs.size()]);
		System.out.println("arguments from test case: " + Arrays.toString(programArgs));
		
		fullDMLScriptName = getScript();
		
		rCmd = getRCmd(inputDir(), expectedDir());
		
		double[][] posSamples = getRandomMatrix(numPosSamples, numAttr, 1, 5, 0.2, System.currentTimeMillis());
		double[][] negSamples = getRandomMatrix(numNegSamples, numAttr, 1, 5, 0.2, System.currentTimeMillis());
		
		MatrixCharacteristics mc1 = new MatrixCharacteristics(numPosSamples,numAttr,-1,-1);
		writeInputMatrixWithMTD("posSamples", posSamples, true, mc1);
		MatrixCharacteristics mc2 = new MatrixCharacteristics(numNegSamples,numAttr,-1,-1);
		writeInputMatrixWithMTD("negSamples", negSamples, true, mc2);
		
		int expectedNumberOfJobs = 1;
		
		runTest(true, EXCEPTION_NOT_EXPECTED, null, expectedNumberOfJobs); 
		
		runRScript(true);
		disableOutAndExpectedDeletion();

		double tol = Math.pow(10, -13);
		HashMap<CellIndex, Double> t_statistics_R = readRMatrixFromFS("t_statistics");
        HashMap<CellIndex, Double> t_statistics_systemml= readDMLMatrixFromHDFS("t_statistics");
        TestUtils.compareMatrices(t_statistics_R, t_statistics_systemml, tol, "t_statistics_R", "t_statistics_systemml");
        
        HashMap<CellIndex, Double> degrees_of_freedom_R = readRMatrixFromFS("degrees_of_freedom");
        HashMap<CellIndex, Double> degrees_of_freedom_systemml= readDMLMatrixFromHDFS("degrees_of_freedom");
        TestUtils.compareMatrices(degrees_of_freedom_R, degrees_of_freedom_systemml, tol, "degrees_of_freedom_R", "degrees_of_freedom_systemml");		
	}
}
