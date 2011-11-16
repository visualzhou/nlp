/**
 * 
 */
package nlp.assignments.alignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nlp.assignments.alignment.WordAlignmentTester.*;
import nlp.math.DoubleArrays;
import nlp.math.SloppyMath;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class Model2Aligner extends Model1Aligner {

	double alpha = 0.5;

	@Override
	protected double getPositionProbability(int enPosition, int frPositioin,
			int enLenght, int frLenght) {
		double key = -alpha
				* Math.abs(enPosition - 1.0 * frPositioin / frLenght * enLenght);
		double exp = SloppyMath.exp(key);
		return exp;
	}
}
