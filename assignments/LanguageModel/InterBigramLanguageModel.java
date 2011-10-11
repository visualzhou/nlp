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
class InterBigramLanguageModel extends SimpleBigramLanguageModel {
	double lambda; // interpolation

	public double getWordProbability(List<String> sentence, int index, double l) {
		String word = sentence.get(index);
		String preWord;
		if (index == 0) {
			preWord = START;
		} else {
			preWord = sentence.get(index - 1);
		}
		return getWordProbability(preWord, word, l);
	}

	public double getSentenceProbability(List<String> sentence, double l) {
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(STOP);
		double probability = 1.0;
		for (int index = 0; index < stoppedSentence.size(); index++) {
			probability *= getWordProbability(stoppedSentence, index, l);
		}
		return probability;
	}

	public double getWordProbability(String preWord, String word, double l) {
		// double count;
		double ans;
		ans = super.getWordProbability(preWord, word);
		// if (!ngramCounter.containsKey(preWord)) {
		// preWord = UNKNOWN;
		// }
		// Counter<String> vCounter = ngramCounter.getCounter(preWord);
		// if (vCounter.containsKey(word)) {
		// count = vCounter.getCount(word);
		// ans = count / vCounter.totalCount();
		// } else {
		// ans = vCounter.getCount(UNKNOWN)
		// / (VocabularySize + 1 - (vCounter.size() - 1));
		// }
		return l * ans + (1 - l) * unigramLM.getWordProbability(word);
	}

	@Override
	public double getWordProbability(String preWord, String word) {
		return getWordProbability(preWord, word, lambda);
	}

	public InterBigramLanguageModel(
			Collection<List<String>> sentenceCollection,
			Collection<List<String>> validationCollection) {
		super(sentenceCollection);
		TrainLambda(validationCollection); // load held-out data
		checkProbability();
	}

	private double getSentenceDerivative(List<String> sentence, double l) {
		List<String> stoppedSentence = new ArrayList<String>();
		stoppedSentence.add(START);
		stoppedSentence.addAll(sentence);
		stoppedSentence.add(STOP);
		double sumDerivative = 0.0;
		for (int index = 1; index < stoppedSentence.size(); index++) {
			String preWord = stoppedSentence.get(index - 1);
			String word = stoppedSentence.get(index);
			double p2 = super.getWordProbability(preWord, word);
			double p1 = unigramLM.getWordProbability(word);
			sumDerivative += (p2 - p1) / (l * p2 + (1 - l) * p1);
		}
		return sumDerivative;
	}

	private void TrainLambda(final Collection<List<String>> validationCollection) {
		// calculate lambda
		LBFGSMinimizer minimizer = new LBFGSMinimizer();

		double[] ans = minimizer.minimize(new DifferentiableFunction() {

			@Override
			public double valueAt(double[] x) {
				double logProbability = 0.0;
				for (List<String> sentence : validationCollection) {
					logProbability += Math.log(getSentenceProbability(sentence,
							x[0]));
				}
				return -logProbability;
			}

			@Override
			public int dimension() {
				return 1;
			}

			@Override
			public double[] derivativeAt(double[] x) {
				double sumLogDerivative = 0.0;
				for (List<String> sentence : validationCollection) {
					sumLogDerivative += getSentenceDerivative(sentence, x[0]);
				}
				return new double[] { -sumLogDerivative };
			}
		}, new double[] { 0.5 }, 1e-4);
		lambda = ans[0];

		// lambda = 0.88;
		System.out.println(String.format("lambda:\t%f", lambda));
	}
}
