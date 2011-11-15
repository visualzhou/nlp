package nlp.assignments.alignment;

import java.util.List;

import nlp.assignments.alignment.WordAlignmentTester.*;
import nlp.math.DoubleArrays;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class HeuristicAligner implements WordAligner {

	CounterMap<String, String> e2fCounterMap = new CounterMap<String, String>();
	Counter<String> fCounter = new Counter<String>();
	Counter<String> eCounter = new Counter<String>();

	@Override
	public Alignment alignSentencePair(SentencePair sentencePair) {
		Alignment alignment = new Alignment();
		for (int i = 0; i < sentencePair.getFrenchWords().size(); i++) {
			String frWord = sentencePair.getFrenchWords().get(i);
			double[] ratio = new double[sentencePair.getEnglishWords().size()];
			for (int j = 0; j < ratio.length; j++) {
				String enWord = sentencePair.getEnglishWords().get(j);
				ratio[j] = e2fCounterMap.getCount(enWord, frWord)
						/ eCounter.getCount(enWord);
				// c(f,e)/(c(e) á c(f))
				// c(f) is a constant
			}
			List<Integer> englishPositions = DoubleArrays.argMaxList(ratio);
			int englishPosition = -1;
			if (ratio[englishPositions.get(0)] == 0) {
				englishPosition = -1; // set to NULL
			} else {
				double distance = Double.MAX_VALUE;
				for (int p : englishPositions) {
					double d =Math.abs(p -  1.0 * i / sentencePair.getFrenchWords().size() * sentencePair.getEnglishWords().size());
					if (distance > d) {
						distance = d;
						englishPosition = p;
					}
				}
			}
			alignment.addAlignment(englishPosition, i, true);
		}
		return alignment;
	}

	public void train(List<SentencePair> trainingSentencePairs) {
		for (SentencePair sentencePair : trainingSentencePairs) {
			for (String englishWord : sentencePair.getEnglishWords()) {
				for (String franchWord : sentencePair.getFrenchWords()) {
					e2fCounterMap.incrementCount(englishWord, franchWord, 1);
				}
			}

			for (String english : sentencePair.getEnglishWords()) {
				eCounter.incrementCount(english, 1);
			}
			for (String fr : sentencePair.getFrenchWords()) {
				fCounter.incrementCount(fr, 1);
			}
		}
	}

}
