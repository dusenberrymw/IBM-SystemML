package com.ibm.bi.dml.runtime.instructions.spark.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.spark.api.java.JavaPairRDD;

import scala.Tuple2;

import com.ibm.bi.dml.runtime.DMLRuntimeException;
import com.ibm.bi.dml.runtime.controlprogram.context.SparkExecutionContext;
import com.ibm.bi.dml.runtime.matrix.data.MatrixBlock;
import com.ibm.bi.dml.runtime.matrix.data.MatrixIndexes;
import com.ibm.bi.dml.runtime.util.UtilFunctions;

public class SparkUtils {
	public static JavaPairRDD<MatrixIndexes, MatrixBlock> getRDDWithEmptyBlocks(SparkExecutionContext sec, 
			JavaPairRDD<MatrixIndexes, MatrixBlock> binaryBlocksWithoutEmptyBlocks,
			long numRows, long numColumns, int brlen, int bclen) throws DMLRuntimeException {
		JavaPairRDD<MatrixIndexes, MatrixBlock> binaryBlocksWithEmptyBlocks = null;
		// ----------------------------------------------------------------------------
		// Now take care of empty blocks
		// This is done as non-rdd operation due to complexity involved in "not in" operations
		// Since this deals only with keys and not blocks, it might not be that bad.
		List<MatrixIndexes> indexes = binaryBlocksWithoutEmptyBlocks.keys().collect();
		ArrayList<Tuple2<MatrixIndexes, MatrixBlock> > emptyBlocksList = getEmptyBlocks(indexes, numRows, numColumns, brlen, bclen);
		if(emptyBlocksList != null && emptyBlocksList.size() > 0) {
			// Empty blocks needs to be inserted
			binaryBlocksWithEmptyBlocks = JavaPairRDD.fromJavaRDD(sec.getSparkContext().parallelize(emptyBlocksList))
					.union(binaryBlocksWithoutEmptyBlocks);
		}
		else {
			binaryBlocksWithEmptyBlocks = binaryBlocksWithoutEmptyBlocks;
		}
		// ----------------------------------------------------------------------------
		return binaryBlocksWithEmptyBlocks;
	}
	
	private static ArrayList<Tuple2<MatrixIndexes, MatrixBlock>> getEmptyBlocks(List<MatrixIndexes> nonEmptyIndexes, long rlen, long clen, int brlen, int bclen) throws DMLRuntimeException {
		long numBlocksPerRow = (long) Math.ceil((double)rlen / brlen);
		long numBlocksPerCol = (long) Math.ceil((double)clen / bclen);
		long expectedNumBlocks = numBlocksPerRow*numBlocksPerCol;
		
		if(expectedNumBlocks == nonEmptyIndexes.size()) {
			return null; // no empty blocks required: sanity check
		}
		else if(expectedNumBlocks < nonEmptyIndexes.size()) {
			throw new DMLRuntimeException("Error: Incorrect number of indexes in ReblockSPInstruction:" + nonEmptyIndexes.size());
		}
		
		// ----------------------------------------------------------------------------
		// Add empty blocks: Performs a "not-in" operation
		Collections.sort(nonEmptyIndexes); // sort in ascending order first wrt rows and then wrt columns
		ArrayList<Tuple2<MatrixIndexes, MatrixBlock>> retVal = new ArrayList<Tuple2<MatrixIndexes,MatrixBlock>>();
		int index = 0;
		for(long row = 1; row <=  Math.ceil((double)rlen / brlen); row++) {
			for(long col = 1; col <=  Math.ceil((double)clen / bclen); col++) {
				boolean matrixBlockExists = false;
				if(nonEmptyIndexes.size() > index) {
					matrixBlockExists = (nonEmptyIndexes.get(index).getRowIndex() == row) && (nonEmptyIndexes.get(index).getColumnIndex() == col);
				}
				if(matrixBlockExists) {
					index++; // No need to add empty block
				}
				else {
					// ------------------------------------------------------------------
					//	Compute local block size: 
					// Example: For matrix: 1500 X 1100 with block length 1000 X 1000
					// We will have four local block sizes (1000X1000, 1000X100, 500X1000 and 500X1000)
					long blockRowIndex = row;
					long blockColIndex = col;
					int emptyBlk_lrlen = UtilFunctions.computeBlockSize(rlen, blockRowIndex, brlen);
					int emptyBlk_lclen = UtilFunctions.computeBlockSize(clen, blockColIndex, bclen);
					// ------------------------------------------------------------------
					
					MatrixBlock emptyBlk = new MatrixBlock(emptyBlk_lrlen, emptyBlk_lclen, true);
					retVal.add(new Tuple2<MatrixIndexes, MatrixBlock>(new MatrixIndexes(blockRowIndex, blockColIndex), emptyBlk));
				}
			}
		}
		// ----------------------------------------------------------------------------
		
		if(index != nonEmptyIndexes.size()) {
			throw new DMLRuntimeException("Unexpected error while adding empty blocks");
		}
		
		return retVal;
	}
}