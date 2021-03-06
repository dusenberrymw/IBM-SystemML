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

package com.ibm.bi.dml.runtime.instructions.mr;

import java.util.ArrayList;

import com.ibm.bi.dml.lops.Lop;
import com.ibm.bi.dml.parser.Expression.DataType;
import com.ibm.bi.dml.runtime.DMLRuntimeException;
import com.ibm.bi.dml.runtime.DMLUnsupportedOperationException;
import com.ibm.bi.dml.runtime.functionobjects.Builtin;
import com.ibm.bi.dml.runtime.functionobjects.Divide;
import com.ibm.bi.dml.runtime.functionobjects.Equals;
import com.ibm.bi.dml.runtime.functionobjects.GreaterThan;
import com.ibm.bi.dml.runtime.functionobjects.GreaterThanEquals;
import com.ibm.bi.dml.runtime.functionobjects.IntegerDivide;
import com.ibm.bi.dml.runtime.functionobjects.LessThan;
import com.ibm.bi.dml.runtime.functionobjects.LessThanEquals;
import com.ibm.bi.dml.runtime.functionobjects.Minus;
import com.ibm.bi.dml.runtime.functionobjects.MinusNz;
import com.ibm.bi.dml.runtime.functionobjects.Modulus;
import com.ibm.bi.dml.runtime.functionobjects.Multiply;
import com.ibm.bi.dml.runtime.functionobjects.Multiply2;
import com.ibm.bi.dml.runtime.functionobjects.NotEquals;
import com.ibm.bi.dml.runtime.functionobjects.Plus;
import com.ibm.bi.dml.runtime.functionobjects.Power;
import com.ibm.bi.dml.runtime.functionobjects.Power2;
import com.ibm.bi.dml.runtime.instructions.Instruction;
import com.ibm.bi.dml.runtime.instructions.InstructionUtils;
import com.ibm.bi.dml.runtime.matrix.data.MatrixValue;
import com.ibm.bi.dml.runtime.matrix.data.OperationsOnMatrixValues;
import com.ibm.bi.dml.runtime.matrix.mapred.CachedValueMap;
import com.ibm.bi.dml.runtime.matrix.mapred.IndexedMatrixValue;
import com.ibm.bi.dml.runtime.matrix.operators.LeftScalarOperator;
import com.ibm.bi.dml.runtime.matrix.operators.RightScalarOperator;
import com.ibm.bi.dml.runtime.matrix.operators.ScalarOperator;


public class ScalarInstruction extends UnaryMRInstructionBase 
{
	
	public ScalarInstruction(ScalarOperator op, byte in, byte out, String istr)
	{
		super(op, in, out);
		mrtype = MRINSTRUCTION_TYPE.ArithmeticBinary;
		instString = istr;
		
		//value dependent safe-safeness (trigger re-evaluation sparse-safe)
		op.setConstant(op.getConstant());
	}
	
	public static Instruction parseInstruction ( String str ) throws DMLRuntimeException {
		
		InstructionUtils.checkNumFields ( str, 3 );
		
		String[] parts = InstructionUtils.getInstructionParts ( str );
		byte in, out;
		double cst;
		boolean firstArgScalar = isFirstArgumentScalar(str);
		String opcode = parts[0];
		if( firstArgScalar ) {
			cst = Double.parseDouble(parts[1]);
			in = Byte.parseByte(parts[2]);
		}
		else {
			in = Byte.parseByte(parts[1]);
			cst = Double.parseDouble(parts[2]);
		}
		out = Byte.parseByte(parts[3]);
		
		if ( opcode.equalsIgnoreCase("+") ) {
			return new ScalarInstruction(new RightScalarOperator(Plus.getPlusFnObject(), cst), in, out, str);
		} 
		else if ( opcode.equalsIgnoreCase("-") ) {
			return new ScalarInstruction(new RightScalarOperator(Minus.getMinusFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("-nz") ) {
			return new ScalarInstruction(new RightScalarOperator(MinusNz.getMinusNzFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("s-r") ) {
			return new ScalarInstruction(new LeftScalarOperator(Minus.getMinusFnObject(), cst), in, out, str);
		} 
		else if ( opcode.equalsIgnoreCase("*") ) {
			return new ScalarInstruction(new RightScalarOperator(Multiply.getMultiplyFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("*2") ) {
			return new ScalarInstruction(new RightScalarOperator(Multiply2.getMultiply2FnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("/") ) {
			return new ScalarInstruction(new RightScalarOperator(Divide.getDivideFnObject(), cst), in, out, str);
		} 
		else if ( opcode.equalsIgnoreCase("%%") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(Modulus.getModulusFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(Modulus.getModulusFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("%/%") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(IntegerDivide.getIntegerDivideFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(IntegerDivide.getIntegerDivideFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("so") ) {
			return new ScalarInstruction(new LeftScalarOperator(Divide.getDivideFnObject(), cst), in, out, str);
		} 
		else if ( opcode.equalsIgnoreCase("^") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(Power.getPowerFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(Power.getPowerFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("^2") ) {
			return new ScalarInstruction(new RightScalarOperator(Power2.getPower2FnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("max") ) {
			return new ScalarInstruction(new RightScalarOperator(Builtin.getBuiltinFnObject("max"), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("min") ) {
			return new ScalarInstruction(new RightScalarOperator(Builtin.getBuiltinFnObject("min"), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase(">") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(GreaterThan.getGreaterThanFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(GreaterThan.getGreaterThanFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase(">=") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(GreaterThanEquals.getGreaterThanEqualsFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(GreaterThanEquals.getGreaterThanEqualsFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("<") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(LessThan.getLessThanFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(LessThan.getLessThanFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("<=") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(LessThanEquals.getLessThanEqualsFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(LessThanEquals.getLessThanEqualsFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("==") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(Equals.getEqualsFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(Equals.getEqualsFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("!=") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(NotEquals.getNotEqualsFnObject(), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(NotEquals.getNotEqualsFnObject(), cst), in, out, str);
		}
		else if ( opcode.equalsIgnoreCase("log") || opcode.equalsIgnoreCase("log_nz") ) {
			if( firstArgScalar )
				return new ScalarInstruction(new LeftScalarOperator(Builtin.getBuiltinFnObject(opcode), cst), in, out, str);
			return new ScalarInstruction(new RightScalarOperator(Builtin.getBuiltinFnObject(opcode), cst), in, out, str);
		}
		
		
		return null;
	}
	
	public void processInstruction(Class<? extends MatrixValue> valueClass, CachedValueMap cachedValues, 
			IndexedMatrixValue tempValue, IndexedMatrixValue zeroInput, int blockRowFactor, int blockColFactor)
		throws DMLUnsupportedOperationException, DMLRuntimeException
	{
		ArrayList<IndexedMatrixValue> blkList = cachedValues.get(input);
		if( blkList != null )
			for( IndexedMatrixValue in : blkList )
			{
				if(in==null)
					continue;
			
				//allocate space for the output value
				IndexedMatrixValue out;
				if(input==output)
					out=tempValue;
				else
					out=cachedValues.holdPlace(output, valueClass);
				
				//process instruction
				out.getIndexes().setIndexes(in.getIndexes());
				OperationsOnMatrixValues.performScalarIgnoreIndexes(in.getValue(), out.getValue(), ((ScalarOperator)this.optr));
				
				//put the output value in the cache
				if(out==tempValue)
					cachedValues.add(output, out);
			}
	}
	
	/**
	 * 
	 * @param inst
	 * @return
	 */
	private static boolean isFirstArgumentScalar(String inst)
	{
		//get first argument
		String[] parts = InstructionUtils.getInstructionPartsWithValueType(inst);
		String arg1 = parts[1];
		
		//get data type of first argument
		String[] subparts = arg1.split(Lop.VALUETYPE_PREFIX);
		DataType dt = DataType.valueOf(subparts[1]);
		
		return (dt == DataType.SCALAR);
	}
}
