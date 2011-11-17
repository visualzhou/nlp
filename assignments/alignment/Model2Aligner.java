/**
 * 
 */
package nlp.assignments.alignment;

import nlp.math.SloppyMath;

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
