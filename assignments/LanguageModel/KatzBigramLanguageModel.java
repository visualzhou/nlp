package nlp.assignments.LanguageModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.omg.CORBA.PUBLIC_MEMBER;

import nlp.langmodel.LanguageModel;
import nlp.math.DifferentiableFunction;
import nlp.math.LBFGSMinimizer;
import nlp.util.Counter;
import nlp.util.CounterMap;
import nlp.util.Counters;

/**
 * A language model -- uses bigram counts, Good-Turning smoothing for unknown
 * words. Interpolation.
 */
class KatzBigramLanguageModel extends SimpleBigramLanguageModel {

	Counter<String> unseenAvgCounter;

	@Override
	public double getWordProbability(String preWord, String word) {
		if (!unigramLM.wordCounter.containsKey(word)) {
			word = UNKNOWN;
		}

		if (ngramCounter.containsKey(preWord)) {
			double c = ngramCounter.getCount(preWord, word);
			if (c == 0.0) {
				c = unseenAvgCounter.getCount(preWord)
						* unigramLM.wordCounter.getCount(word);
			}
			return c / ngramCounter.getCounter(preWord).totalCount();
		}
		return unigramLM.getWordProbability(word);
	}

	public KatzBigramLanguageModel(Collection<List<String>> sentenceCollection) {
		super(sentenceCollection);
	}

	@Override
	protected void smooth() {
		unseenAvgCounter = new Counter<String>();
		double preTotal = ngramCounter.totalCount();
		double unseenTotalCount = GoodTurningSmoothing.Smooth(ngramCounter);
		// Add UNSEEN to each sub-counter, e.g. (w, UNSEEN) is defined.
		VocabularySize = ngramCounter.size();
		// including UNKNOWN, excluding START

		/*
		 * (START|[V], [V]|STOP) (|V|+1)(|V|+1)
		 */
		double totalPairCount = VocabularySize * (VocabularySize + 1);
		double unseenArgCount = unseenTotalCount
				/ (totalPairCount - ngramCounter.totalSize());

		for (String key : ngramCounter.keySet()) {
			Counter<String> vCounter = ngramCounter.getCounter(key);
			double unseenCount = (VocabularySize + 1 - vCounter.size())
					* unseenArgCount;
			// |V| times |V|+1
			double unseenLastWordCount = 0.0;

			for (String word : vCounter.keySet()) {
				unseenLastWordCount += unigramLM.wordCounter.getCount(word);
			}
			unseenLastWordCount = unigramLM.wordCounter.totalCount()
					- unseenLastWordCount;

			unseenAvgCounter.setCount(key, unseenCount / unseenLastWordCount);
			ngramCounter.incrementCount(key, UNSEEN, unseenCount);
		}
		// ngramCounter.incrementCount(UNKNOWN, UNKNOWN, (VocabularySize + 1) *
		// unseenArgCount);
		// no need anymore
		// |V| + 1
		total = ngramCounter.totalCount();

		System.out.println(String.format(
				"PreTotal bigram: %f\tAfter smoothing: %f", preTotal, total));
	}
}
