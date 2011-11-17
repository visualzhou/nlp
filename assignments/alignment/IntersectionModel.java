package nlp.assignments.alignment;

import java.util.List;

import nlp.assignments.alignment.WordAlignmentTester.Alignment;
import nlp.assignments.alignment.WordAlignmentTester.ProbobalityWordAligner;
import nlp.assignments.alignment.WordAlignmentTester.SentencePair;
import nlp.assignments.alignment.WordAlignmentTester.Tester;
import nlp.math.DoubleArrays;
import nlp.math.SloppyMath;
import nlp.util.CounterMap;

public class IntersectionModel implements ProbobalityWordAligner {

	protected CounterMap<String, String> e2f;
	protected CounterMap<String, String> f2e;

	protected double NullPositionProbabiliy = 0.2;
	protected final String nullString = "*NULL*";
	double alpha = 0.5;
	protected final int totalIterationTimes = 12;

	@Override
	public Alignment alignSentencePair(SentencePair sentencePair) {
		Alignment alignment = new Alignment();
		int enLength = sentencePair.getEnglishWords().size();
		int frLength = sentencePair.getFrenchWords().size();
		double[] enProbobality = new double[enLength];
		double[] frProbobality = new double[frLength];
		int[] best_e2f_index = new int[enLength];
		// e2f alignment
		for (int i = 0; i < enLength; i++) {
			String en = sentencePair.getEnglishWords().get(i);
			for (int j = 0; j < frLength; j++) {
				String fr = sentencePair.getFrenchWords().get(j);
				frProbobality[j] = f2e.getCount(fr, en)
						* getPositionProbability(i, j, enLength, frLength);
			}
			int frPosition = DoubleArrays.argMax(frProbobality);
			if (frProbobality[frPosition] < f2e.getCount(nullString, en)
					* NullPositionProbabiliy) {
				frPosition = -1;
			}
			best_e2f_index[i] = frPosition;
		}

		for (int i = 0; i < frLength; i++) {
			String fr = sentencePair.getFrenchWords().get(i);
			for (int j = 0; j < enLength; j++) {
				String en = sentencePair.getEnglishWords().get(j);
				enProbobality[j] = e2f.getCount(en, fr)
						* getPositionProbability(j, i, enLength, frLength);
			}
			int englishPosition = DoubleArrays.argMax(enProbobality);
			if (enProbobality[englishPosition] < e2f.getCount(nullString, fr)
					* NullPositionProbabiliy) {
				englishPosition = -1;
			} else if (best_e2f_index[englishPosition] != i) { // intersection
				englishPosition = -1;
			}
			alignment.addAlignment(englishPosition, i, true);
		}
		return alignment;
	}

	public void train(List<SentencePair> trainingSentencePairs) {
		TrainProgress(null, trainingSentencePairs);
	}

	@Override
	public void TrainProgress(Tester tester,
			List<SentencePair> trainingSentencePairs) {
		// EM Algorithm
		// init e2f;
		e2f = new CounterMap<String, String>();
		f2e = new CounterMap<String, String>();
		for (SentencePair sentencePair : trainingSentencePairs) {
			for (String fr : sentencePair.getFrenchWords()) {
				for (String en : sentencePair.getEnglishWords()) {
					if (!e2f.containsKeyValue(en, fr)) {
						e2f.incrementCount(en, fr, 1.0);
						f2e.incrementCount(fr, en, 1.0);
					}
				}
				e2f.incrementCount(nullString, fr, 1.0);
			}
			for (String en : sentencePair.getEnglishWords()) {
				f2e.incrementCount(nullString, en, 1.0);
			}
		}
		// uniform e2f
		e2f.normalize();
		f2e.normalize();

		// until not converged
		for (int iterationTimes = 0; iterationTimes < totalIterationTimes; iterationTimes++) {
			// init another e2f
			CounterMap<String, String> e2f_new = new CounterMap<String, String>();
			CounterMap<String, String> f2e_new = new CounterMap<String, String>();

			for (SentencePair sentencePair : trainingSentencePairs) {
				int enLenght = sentencePair.getEnglishWords().size();
				int frLenght = sentencePair.getFrenchWords().size();
				double[] enSourceProbabilities = new double[enLenght + 1];
				double[] frSourceProbabilities = new double[frLenght + 1];

				// +1 for null
				for (int frPositioin = 0; frPositioin < frLenght; frPositioin++) {
					String fr = sentencePair.getFrenchWords().get(frPositioin);
					// give null string a count
					enSourceProbabilities[0] = e2f.getCount(nullString, fr);
					// the probability distribution of the generating source of
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
							NullPositionProbabiliy * enSourceProbabilities[0]
									/ sum);
				}

				for (int enPositioin = 0; enPositioin < enLenght; enPositioin++) {
					String en = sentencePair.getEnglishWords().get(enPositioin);
					// give null string a count
					frSourceProbabilities[0] = f2e.getCount(nullString, en);
					// the probability distribution of the generating source of
					// a given french
					for (int i = 0; i < frLenght; i++) {
						String fr = sentencePair.getFrenchWords().get(i);
						frSourceProbabilities[i + 1] = f2e.getCount(fr, en);
					}
					// normalize
					double sum = DoubleArrays.add(frSourceProbabilities);
					// add the fractional number to counter
					for (int i = 0; i < frSourceProbabilities.length - 1; i++) {
						String fr = sentencePair.getFrenchWords().get(i);
						double p = frSourceProbabilities[i + 1]
								/ sum
								* getPositionProbability(enPositioin, i,
										enLenght, frLenght);
						f2e_new.incrementCount(fr, en, p);
						// fractional number
					}
					// add null string
					f2e_new.incrementCount(nullString, en,
							NullPositionProbabiliy * frSourceProbabilities[0]
									/ sum);
				}

			}
			e2f_new.normalize();
			f2e_new.normalize();
			e2f = null; // for garbage collection
			e2f = e2f_new;
			f2e = f2e_new;
			System.out.println("training iteration " + (iterationTimes + 1)
					+ " done.");
			if (tester != null) {
				tester.runTest(this);
			}
		}
	}

	protected double getPositionProbability(int enPosition, int frPositioin,
			int enLenght, int frLenght) {
		double key = -alpha
				* Math.abs(enPosition - 1.0 * frPositioin / frLenght * enLenght);
		double exp = SloppyMath.exp(key);
		return exp;
	}

}
