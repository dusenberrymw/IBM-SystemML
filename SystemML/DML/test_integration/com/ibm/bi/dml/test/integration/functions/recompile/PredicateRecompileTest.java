/**
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2013
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office.
 */

package com.ibm.bi.dml.test.integration.functions.recompile;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.bi.dml.hops.OptimizerUtils;
import com.ibm.bi.dml.runtime.matrix.io.MatrixValue.CellIndex;
import com.ibm.bi.dml.test.integration.AutomatedTestBase;
import com.ibm.bi.dml.test.integration.TestConfiguration;
import com.ibm.bi.dml.utils.Statistics;

public class PredicateRecompileTest extends AutomatedTestBase 
{
	@SuppressWarnings("unused")
	private static final String _COPYRIGHT = "Licensed Materials - Property of IBM\n(C) Copyright IBM Corp. 2010, 2013\n" +
                                             "US Government Users Restricted Rights - Use, duplication  disclosure restricted by GSA ADP Schedule Contract with IBM Corp.";
	
	private final static String TEST_NAME1 = "while_recompile";
	private final static String TEST_NAME2 = "if_recompile";
	private final static String TEST_NAME3 = "for_recompile";
	private final static String TEST_NAME4 = "parfor_recompile";
	private final static String TEST_DIR = "functions/recompile/";
	
	private final static int rows = 10;
	private final static int cols = 15;    
	private final static int val = 7;    
	
	
	@Override
	public void setUp() 
	{
		addTestConfiguration(
				TEST_NAME1, 
				new TestConfiguration(TEST_DIR, TEST_NAME1, 
				new String[] { "Rout" })   );
		addTestConfiguration(
				TEST_NAME2, 
				new TestConfiguration(TEST_DIR, TEST_NAME2, 
				new String[] { "Rout" })   );
		addTestConfiguration(
				TEST_NAME3, 
				new TestConfiguration(TEST_DIR, TEST_NAME3, 
				new String[] { "Rout" })   );
		addTestConfiguration(
				TEST_NAME4, 
				new TestConfiguration(TEST_DIR, TEST_NAME4, 
				new String[] { "Rout" })   );
	}

	@Test
	public void testWhileRecompile() 
	{
		runRecompileTest(TEST_NAME1, true, false);
	}
	
	@Test
	public void testWhileNoRecompile() 
	{
		runRecompileTest(TEST_NAME1, false, false);
	}
	
	@Test
	public void testIfRecompile() 
	{
		runRecompileTest(TEST_NAME2, true, false);
	}
	
	@Test
	public void testIfNoRecompile() 
	{
		runRecompileTest(TEST_NAME2, false, false);
	}
	
	@Test
	public void testForRecompile() 
	{
		runRecompileTest(TEST_NAME3, true, false);
	}
	
	@Test
	public void testForNoRecompile() 
	{
		runRecompileTest(TEST_NAME3, false, false);
	}
	
	@Test
	public void testParForRecompile() 
	{
		runRecompileTest(TEST_NAME4, true, false);
	}
	
	@Test
	public void testParForNoRecompile() 
	{
		runRecompileTest(TEST_NAME4, false, false);
	}

	@Test
	public void testWhileRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME1, true, true);
	}
	
	@Test
	public void testWhileNoRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME1, false, true);
	}
	
	@Test
	public void testIfRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME2, true, true);
	}
	
	@Test
	public void testIfNoRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME2, false, true);
	}
	
	@Test
	public void testForRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME3, true, true);
	}
	
	@Test
	public void testForNoRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME3, false, true);
	}
	
	@Test
	public void testParForRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME4, true, true);
	}
	
	@Test
	public void testParForNoRecompileExprEval() 
	{
		runRecompileTest(TEST_NAME4, false, true);
	}
	
	
	private void runRecompileTest( String testname, boolean recompile, boolean evalExpr )
	{	
		boolean oldFlagRecompile = OptimizerUtils.ALLOW_DYN_RECOMPILATION;
		boolean oldFlagEval = OptimizerUtils.ALLOW_SIZE_EXPRESSION_EVALUATION;
		
		try
		{
			TestConfiguration config = getTestConfiguration(testname);
			
			/* This is for running the junit test the new way, i.e., construct the arguments directly */
			String HOME = SCRIPT_DIR + TEST_DIR;
			fullDMLScriptName = HOME + testname + ".dml";
			programArgs = new String[]{"-args",Integer.toString(rows),
					                           Integer.toString(cols),
					                           Integer.toString(val),
					                           HOME + OUTPUT_DIR + "R" };
			fullRScriptName = HOME + testname + ".R";
			rCmd = "Rscript" + " " + fullRScriptName + " " + 
			       HOME + INPUT_DIR + " " + HOME + EXPECTED_DIR;			
			loadTestConfiguration(config);

			OptimizerUtils.ALLOW_DYN_RECOMPILATION = recompile;
			OptimizerUtils.ALLOW_SIZE_EXPRESSION_EVALUATION = evalExpr;
			
			boolean exceptionExpected = false;
			runTest(true, exceptionExpected, null, -1); 
			
			//check expected number of compiled and executed MR jobs
			if( recompile )
			{
				Assert.assertEquals("Unexpected number of executed MR jobs.", 
						  1 - ((evalExpr)?1:0), Statistics.getNoOfExecutedMRJobs()); //rand	
			}
			else
			{
				if( testname.equals(TEST_NAME1) )
					Assert.assertEquals("Unexpected number of executed MR jobs.", 
				            4 - ((evalExpr)?1:0), Statistics.getNoOfExecutedMRJobs()); //rand, 2xgmr while pred, 1x gmr while body				
				else //if( testname.equals(TEST_NAME2) )
					Assert.assertEquals("Unexpected number of executed MR jobs.", 
				            3 - ((evalExpr)?1:0), Statistics.getNoOfExecutedMRJobs()); //rand, 1xgmr if pred, 1x gmr if body
			}
			
			//compare matrices
			HashMap<CellIndex, Double> dmlfile = readDMLMatrixFromHDFS("R");
			Assert.assertEquals((double)val, dmlfile.get(new CellIndex(1,1)));
		}
		finally
		{
			OptimizerUtils.ALLOW_DYN_RECOMPILATION = oldFlagRecompile;
			OptimizerUtils.ALLOW_SIZE_EXPRESSION_EVALUATION = oldFlagEval;
		}
	}
	
}