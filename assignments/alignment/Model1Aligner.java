/**
 * 
 */
package nlp.assignments.alignment;

import java.util.List;

import nlp.assignments.alignment.WordAlignmentTester.*;
import nlp.math.DoubleArrays;
import nlp.util.CounterMap;

public class Model1Aligner implements WordAligner {

	CounterMap<String, String> e2f;
	double NullProbabiliy = 0.2;
	final String nullString = "*NULL*";

	@Override
	public Alignment alignSentencePair(SentencePair sentencePair) {
		Alignment alignment = new Alignment();
		for (int i = 0; i < sentencePair.getFrenchWords().size(); i++) {
			String fr = sentencePair.getFrenchWords().get(i);
			double[] enProbobality = new double[sentencePair.getEnglishWords()
					.size()];
			for (int j = 0; j < sentencePair.getEnglishWords().size(); j++) {
				String en = sentencePair.getEnglishWords().get(j);
				enProbobality[j] = e2f.getCount(en, fr);
			}
			int englishPosition = DoubleArrays.argMax(enProbobality);
			// if (enProbobality[englishPosition] * (1 - NullProbabiliy) <
			// NullProbabiliy) {
			// englishPosition = -1;
			// }
			if (enProbobality[englishPosition] < e2f.getCount(nullString, fr)) {
				englishPosition = -1;
			}
			alignment.addAlignment(englishPosition, i, true);
		}
		return alignment;
	}

	public void train(List<SentencePair> trainingSentencePairs) {
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
				double[] sourceProbabilities = new double[sentencePair
						.getEnglishWords().size() + 1]; // +1 for null
				for (String fr : sentencePair.getFrenchWords()) {
					// give null string a count
					sourceProbabilities[0] = e2f.getCount(nullString, fr);
					// the probability distribution of the generating source of a
					// given french
					for (int i = 0, stop = sentencePair.getEnglishWords()
							.size(); i < stop; i++) {
						String en = sentencePair.getEnglishWords().get(i);
						sourceProbabilities[i + 1] = e2f.getCount(en, fr);
					}
					// normalize
					double sum = DoubleArrays.add(sourceProbabilities);
					// add the fractional number to counter
					for (int i = 0; i < sourceProbabilities.length-1; i++) {
						String en = sentencePair.getEnglishWords().get(i);
						e2f_new.incrementCount(en, fr,
								sourceProbabilities[i+1] / sum); // fractional number
					}
					// add null string
					e2f_new.incrementCount(nullString, fr,
							sourceProbabilities[0] / sum);
				}
			}
			e2f_new.normalize();
			e2f = null; // for garbage collection
			e2f = e2f_new;
		}
	}
}
