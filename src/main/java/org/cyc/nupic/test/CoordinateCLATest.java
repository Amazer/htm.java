package org.cyc.nupic.test;

import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.encoders.CoordinateEncoder;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;

public class CoordinateCLATest {
	public static void main(String[] args) {
    	Parameters params = getParameters();
    	System.out.println(params);
    	
    	//Layer components
    	CoordinateEncoder.Builder dayBuilder =
    			CoordinateEncoder.builder()
				.n(2048)
				.w(51)
				.radius(1.0)
				.minVal(1.0)
				.maxVal(8)
				.periodic(true)
				.forced(true)
				.resolution(1);
    	CoordinateEncoder encoder = dayBuilder.build();
    	SpatialPooler sp = new SpatialPooler();
    	TemporalMemory tm = new TemporalMemory();
    	CLAClassifier classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.3, 0);
    	
    	Layer<int[]> layer = getLayer(params, encoder, sp, tm, classifier);
    	
    	for(double i = 1, x = 0;x < 10000;i = (i == 7 ? 1 : i + 1), x++) {  // USE "X" here to control run length
    		if (i == 1) tm.reset(layer.getMemory());
//    		runThroughLayer(layer, i, (int)i, (int)x);
    	}
    	System.out.println("输入1-7的数字，输入-1结束");
    	int screenInput=0;
		Scanner s=new Scanner(System.in);
		screenInput = s.nextInt();
		while(screenInput!=-1)
		{
			System.out.println("input value:"+screenInput);
			screenInput = s.nextInt();
		}
    	s.close();
    	System.out.println("end-------");
    }
    
    public static Parameters getParameters() {
    	Parameters parameters = Parameters.getAllDefaultParameters();
    	parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 0 });
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        
        //SpatialPooler specific
        parameters.setParameterByKey(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.8);//0.5
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITIONS, false);
        parameters.setParameterByKey(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40);
        parameters.setParameterByKey(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        parameters.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.0001);
        parameters.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.setParameterByKey(KEY.MAX_BOOST, 10.0);
        parameters.setParameterByKey(KEY.SEED, 1956);
        parameters.setParameterByKey(KEY.SP_VERBOSITY, 0);
        
        //Temporal Memory specific
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.21);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.5);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 3);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.1);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.1);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 6);
        
        return parameters;
    }

	public static <T> void runThroughLayer(Layer<T> l, T input, int recordNum, int sequenceNum) {
    	l.input(input, recordNum, sequenceNum);
    }
    
	public static Layer<int[]> getLayer(Parameters p, CoordinateEncoder e, SpatialPooler s, TemporalMemory t, CLAClassifier c) {
    	Layer<int[]> l = new LayerImpl(p, e, s, t, c);
    	return l;
    }
    
    ////////////////// Preliminary Network API Toy ///////////////////
    
    interface Layer<T> {
    	public void input(T value, int recordNum, int iteration);
    	public int[] getPredicted();
    	public Connections getMemory();
    	public int[] getActual();
    }
    
    /**
     * I'm going to make an actual Layer, this is just temporary so I can
     * work out the details while I'm completing this for Peter
     * 
     * @author David Ray
     *
     */
    static class LayerImpl implements Layer<int[]> {
    	private Parameters params;
    	
    	private Connections memory = new Connections();
    	
    	private CoordinateEncoder encoder;
    	private SpatialPooler spatialPooler;
    	private TemporalMemory temporalMemory;
    	private CLAClassifier classifier;
    	private Map<String, Object> classification = new LinkedHashMap<String, Object>();
    	
    	private int columnCount;
    	private int cellsPerColumn;
    	private int theNum;
    	
    	private int[] predictedColumns;
    	private int[] actual;
    	private int[] lastPredicted;
    	
    	public LayerImpl(Parameters p, CoordinateEncoder e, SpatialPooler s, TemporalMemory t, CLAClassifier c) {
    		this.params = p;
    		this.encoder = e;
    		this.spatialPooler = s;
    		this.temporalMemory = t;
    		this.classifier = c;
    		
    		params.apply(memory);
    		spatialPooler.init(memory);
    		temporalMemory.init(memory);
    		
    		columnCount = memory.getPotentialPools().getMaxIndex() + 1; //If necessary, flatten multi-dimensional index 
    		cellsPerColumn = memory.getCellsPerColumn();
    	}
    	
    	@Override
    	public void input(int[] value, int recordNum, int sequenceNum) {
    		String recordOut = "";
    		switch(recordNum) {
    			case 1: recordOut = "Monday (1)";break; 
    			case 2: recordOut = "Tuesday (2)";break;
    			case 3: recordOut = "Wednesday (3)";break;
    			case 4: recordOut = "Thursday (4)";break;
    			case 5: recordOut = "Friday (5)";break;
    			case 6: recordOut = "Saturday (6)";break;
    			case 7: recordOut = "Sunday (7)";break;
    		}
    		
    		if(recordNum == 1) {
    			theNum++;
    			System.out.println("--------------------------------------------------------");
    			System.out.println("Iteration: " + theNum);
    		}
    		System.out.println("===== " + recordOut + "  - Sequence Num: " + sequenceNum + " =====");
    		
    		int[] output = new int[columnCount];
    		
    		//Input through encoder
    		System.out.println("ScalarEncoder Input = " + value);
    		int[] encoding = encoder.encode(new Tuple(value));
    		System.out.println("ScalarEncoder Output = " + Arrays.toString(encoding));
//    		int bucketIdx = encoder.getbu(value)[0];
    		
    		//Input through spatial pooler
    		spatialPooler.compute(memory, encoding, output, true, true);
    		System.out.println("SpatialPooler Output = " + Arrays.toString(output));
    		
    		//Input through temporal memory
    		int[] input = actual = ArrayUtils.where(output, ArrayUtils.WHERE_1);
    		ComputeCycle cc = temporalMemory.compute(memory, input, true);
    		lastPredicted = predictedColumns;
    		predictedColumns = getSDR(cc.predictiveCells()); //Get the active column indexes
    		System.out.println("TemporalMemory Input = " + Arrays.toString(input));
    		System.out.println("TemporalMemory Prediction = " + Arrays.toString(predictedColumns));
    		if(lastPredicted!=null)
    		{
    			System.out.println("!!!!!!!!!!!!!!!!! lastPredicted:");
    			for(int ii=0;ii<lastPredicted.length;++ii)
    			{
    				System.out.format("           %d", lastPredicted[ii]);
    			}
    			System.out.println("!!!!!!!!!!!");
    			Object preObj= classification.get(lastPredicted);
    			if(preObj!=null)
    			{
    				System.out.format("!!!!!!!!!!!!!!!!! prediction:%s", preObj);
    			}
    		}
//    		classification.put("bucketIdx", bucketIdx);
    		classification.put("actValue", value);
    		ClassifierResult<Double> result = classifier.compute(recordNum, classification, predictedColumns, true, true);
    		
    		System.out.println("  |  CLAClassifier 1 step prob = " + Arrays.toString(result.getStats(1)) + "\n");
    		if(sequenceNum>5000)
    		{
//    			System.out.println("pre actualValue---------"+result.getActualValue(bucketIdx));
    			System.out.println("stepcount:"+result.getStepCount());
    			for(int preStep=1;preStep<=result.getStepCount();++preStep)
    			{
    				int bIndex=result.getMostProbableBucketIndex(preStep);
    				System.out.println("step:"+preStep);
    				System.out.format("pre step %d: value %f \n",preStep,result.getActualValue(bIndex));
    			}
    		}
    		System.out.println("");
    	}
    	
    	public int[] inflateSDR(int[] SDR, int len) {
    		int[] retVal = new int[len];
    		for(int i : SDR) {
    			retVal[i] = 1;
    		}
    		return retVal;
    	}
    	
    	public int[] getSDR(Set<Cell> cells) {
    		int[] retVal = new int[cells.size()];
    		int i = 0;
    		for(Iterator<Cell> it = cells.iterator();i < retVal.length;i++) {
    			retVal[i] = it.next().getIndex();
    			retVal[i] /= cellsPerColumn; // Get the column index
    		}
    		Arrays.sort(retVal);
    		retVal = ArrayUtils.unique(retVal);
    		
    		return retVal;
    	}
    	
    	/**
         * Returns the next predicted value.
         * 
         * @return the SDR representing the prediction
         */
    	@Override
        public int[] getPredicted() {
        	return lastPredicted;
        }
        
        /**
         * Returns the actual columns in time t + 1 to compare
         * with {@link #getPrediction()} which returns the prediction
         * at time t for time t + 1.
         * @return
         */
    	@Override
        public int[] getActual() {
            return actual;
        }
        
        /**
         * Simple getter for external reset
         * @return
         */
        public Connections getMemory() {
        	return memory;
        }

    }
    
}
