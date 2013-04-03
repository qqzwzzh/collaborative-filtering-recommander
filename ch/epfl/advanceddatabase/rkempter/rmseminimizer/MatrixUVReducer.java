package ch.epfl.advanceddatabase.rkempter.rmseminimizer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ch.epfl.advanceddatabase.rkempter.UVDecomposer;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;


public class MatrixUVReducer extends MapReduceBase implements 
	Reducer<IntWritable, MatrixUVValueWritable, IntWritable, MatrixInputValueWritable> {
	
	private int xPos;
	
	public void configure(JobConf conf) {
		xPos = conf.getInt(UVDecomposer.MATRIX_X_POSITION, 1);
	}

	public void reduce(IntWritable key, Iterator<MatrixUVValueWritable> values, OutputCollector<IntWritable, MatrixInputValueWritable> output, Reporter reporter) 
			throws IOException {
		
		// Store all elements
		Map<Integer, Float> xVal = new HashMap<Integer, Float>();
		Map<Integer, Float> kVal = new HashMap<Integer, Float>();
		Map<Integer, Float> mVal = new HashMap<Integer, Float>();
		
		// Sum of the elements
		float xSum = 0;
		float kSum = 0;
		float mSum = 0;
		
		char matrixType = 'U';
		
		// Iterate over all values, add the values to the maps and sum them up
		while(values.hasNext()) {
			MatrixUVValueWritable value = (MatrixUVValueWritable) values.next();
			int type = value.getType();
			if(type == UVDecomposer.MATRIX_M) {
				mVal.put(value.getColumn(), value.getValue_k());
				mSum += value.getValue_k();
			} else {
				if(type == 'V') {
					matrixType = 'V';
				}
				xVal.put(value.getColumn(), value.getValue_x());
				xSum += value.getValue_x();
				kVal.put(value.getColumn(), value.getValue_k());
				kSum += value.getValue_k();
			}
		}
		
		// Compute X
		float x = (mSum - kSum) / xSum;
		
		// Compute the RMSE
		float rmse = 0;
		for(Map.Entry<Integer, Float> entry : mVal.entrySet()) {
			int index = entry.getKey();
			float val = entry.getValue() - xVal.get(index) * x - kVal.get(index);
			rmse += (float) Math.pow(val, 2);
		}
		
		// Write RMSE to counter
		Counters.Counter rmseCounter = reporter.getCounter(UVDecomposer.OVERALL_COUNTERS.RMSE_COUNTER);
		rmseCounter.increment((long) rmse * UVDecomposer.COUNTER_MULTIPLICATOR);
		
		// Output X
		MatrixInputValueWritable outputValue = new MatrixInputValueWritable(matrixType, key.get(), xPos, x);
		output.collect(key, outputValue);
		
	}

}