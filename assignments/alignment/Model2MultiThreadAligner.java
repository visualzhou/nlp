/**
 * 
 */
package nlp.assignments.alignment;

import java.util.List;

import nlp.assignments.alignment.WordAlignmentTester.*;
import nlp.math.DoubleArrays;
import nlp.math.SloppyMath;
import nlp.util.CounterMap;

public class Model2MultiThreadAligner implements WordAligner {

	protected CounterMap<String, String> e2f;
	protected double NullPositionProbabiliy = 0.2;
	protected final String nullString = "*NULL*";

	@Override
	public Alignment alignSentencePair(SentencePair sentencePair) {
		Alignment alignment = new Alignment();
		int enLength = sentencePair.getEnglishWords().size();
		int frLenght = sentencePair.getFrenchWords().size();
		double[] enProbobality = new double[enLength];
		for (int i = 0; i < frLenght; i++) {
			String fr = sentencePair.getFrenchWords().get(i);
			for (int j = 0; j < enLength; j++) {
				String en = sentencePair.getEnglishWords().get(j);
				enProbobality[j] = e2f.getCount(en, fr)
						* getPositionProbability(j, i, enLength, frLenght);
			}
			int englishPosition = DoubleArrays.argMax(enProbobality);
			if (enProbobality[englishPosition] < e2f.getCount(nullString, fr)
					* NullPositionProbabiliy) {
				englishPosition = -1;
			}
			alignment.addAlignment(englishPosition, i, true);
		}
		return alignment;
	}

	public void train(List<SentencePair> trainingSentencePairs) {
		Thread threade2f = new Traine2f(this, trainingSentencePairs);
		threade2f.start();
		try {
			threade2f.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	double alpha = 0.5;

	protected double getPositionProbability(int enPosition, int frPositioin,
			int enLenght, int frLenght) {
		double key = -alpha
				* Math.abs(enPosition - 1.0 * frPositioin / frLenght * enLenght);
		double exp = SloppyMath.exp(key);
		return exp;
	}
	
	class Traine2f extends Thread {
		Model2MultiThreadAligner model;
		List<SentencePair> trainingSentencePairs;

		public Traine2f(Model2MultiThreadAligner model, List<SentencePair> trainingSentencePairs) {
			this.model = model;
			this.trainingSentencePairs = trainingSentencePairs;
		}
		
		@Override
		public void run() {
			traine2f();
		}

		void traine2f() {
			// EM Algorithm
			// init e2f;
			e2f = new CounterMap<String, String>();
			for (SentencePair sentencePair : trainingSentencePairs) {
				for (String fr : sentencePair.getFrenchWords()) {
					for (String en : sentencePair.getEnglishWords()) {
						if (!e2f.containsKeyValue(en, fr)) {
							e2f.incrementCount(en, fr, 1.0);
						}
					}
					e2f.incrementCount(nullString, fr, 1.0);
				}
			}
			// uniform e2f
			e2f.normalize();

			// until not converged
			int iterationTimes = 20;
			while (iterationTimes-- > 0) {
				// init another e2f
				CounterMap<String, String> e2f_new = new CounterMap<String, String>();

				for (SentencePair sentencePair : trainingSentencePairs) {
					int enLenght = sentencePair.getEnglishWords().size();
					int frLenght = sentencePair.getFrenchWords().size();
					double[] enSourceProbabilities = new double[enLenght + 1];

					// +1 for null
					for (int frPositioin = 0; frPositioin < frLenght; frPositioin++) {
						String fr = sentencePair.getFrenchWords().get(
								frPositioin);
						// give null string a count
						enSourceProbabilities[0] = e2f.getCount(nullString, fr);
						// the probability distribution of the generating source
						// of
						// a given french
						for (int i = 0; i < enLenght; i++) {
							String en = sentencePair.getEnglishWords().get(i);
							enSourceProbabilities[i + 1] = e2f.getCount(en, fr);
						}
						// normalize
						double sum = DoubleArrays.add(enSourceProbabilities);
						// add the fractional number to counter
						for (int i = 0; i < enSourceProbabilities.length - 1; i++) {
							String en = sentencePair.getEnglishWords().get(i);
							double p = enSourceProbabilities[i + 1]
									/ sum
									* getPositionProbability(i, frPositioin,
											enLenght, frLenght);
							e2f_new.incrementCount(en, fr, p);
							// fractional number
						}
						// add null string
						e2f_new.incrementCount(nullString, fr,
								NullPositionProbabiliy
										* enSourceProbabilities[0] / sum);
					}
				}
				e2f_new.normalize();
				e2f = null; // for garbage collection
				e2f = e2f_new;
			}
		}
	}

}
