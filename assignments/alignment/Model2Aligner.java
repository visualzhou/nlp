/**
 * 
 */
package nlp.assignments.alignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nlp.assignments.alignment.WordAlignmentTester.*;
import nlp.math.DoubleArrays;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class Model2Aligner implements WordAligner {

	CounterMap<String, String> e2f;
	double NullProbabiliy = 0.2;
	final String nullString = "*NULL*";
	Map<Double, Double> expCache = new HashMap<Double, Double>();

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
//			if (enProbobality[englishPosition] * (1 - NullProbabiliy) < NullProbabiliy) {
//				englishPosition = -1;
//			}
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
				for (String fr : sentencePair.getFrenchWords()) {
					// the probablity distribution of the generating source of a
					// given franch
					Counter<String> sourceCounter = new Counter<String>();
					for (String en : sentencePair.getEnglishWords()) {
						sourceCounter.incrementCount(en, e2f.getCount(en, fr));
					}
					// give null string a count
					sourceCounter.incrementCount(nullString, e2f.getCount(nullString, fr));
					sourceCounter.normalize(); // fractional number
					// add the fractional number to counter
					for (String en : sentencePair.getEnglishWords()) {
						e2f_new.incrementCount(en, fr,
								sourceCounter.getCount(en));
					}
					// add null string
					e2f_new.incrementCount(nullString, fr, sourceCounter.getCount(nullString));
				}
			}
			e2f_new.normalize();
			e2f = null; // for garbage collection
			e2f = e2f_new;
		}
	}
}
