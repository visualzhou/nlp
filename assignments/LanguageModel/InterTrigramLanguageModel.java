package nlp.assignments.LanguageModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nlp.math.DifferentiableFunction;
import nlp.math.LBFGSMinimizer;
import nlp.util.Counter;

public class InterTrigramLanguageModel extends SimpleTrigramLanguageModel {
	double[] lambda = new double[] { 0.3, 0.3 }; // interpolation

	@Override
	public double getWordProbability(String[] preWords, String word) {
		return super.getWordProbability(preWords, word)
				* lambda[0]
				+ (lambda[1])
				* bigramLanguageModel.getWordProbability(
						preWords[preWords.length - 1], word)
				+ (1 - lambda[0] - lambda[1])
				* unigramLM.getWordProbability(word);
	}

	public InterTrigramLanguageModel(
			Collection<List<String>> sentenceCollection,
			Collection<List<String>> validationCollection) {
		super(sentenceCollection);
		TrainLambda(validationCollection); // load held-out data
		checkProbability();
	}

	private double[] getSentenceDerivative(List<String> sentence) {
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(STOP);
		double[] sumDerivative = new double[2];
		for (int index = 1; index < stoppedSentence.size(); index++) {
			String[] preWords = getWordPair(stoppedSentence, (index - 1));
			String word = stoppedSentence.get(index);
			double p1 = super.getWordProbability(preWords, word);
			double p2 = bigramLanguageModel.getWordProbability(
					preWords[preWords.length - 1], word);
			double p3 = unigramLM.getWordProbability(word);
			double p = p1 * lambda[0] + lambda[1] * p2
					+ (1 - lambda[0] - lambda[1]) * p3;
			sumDerivative[0] += (p1 - p3) / p;
			sumDerivative[1] += (p2 - p3) / p;
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
				double[] temp = lambda.clone();
				lambda[0] = x[0];
				lambda[1] = x[1];
				for (List<String> sentence : validationCollection) {
					logProbability += Math
							.log(getSentenceProbability(sentence));
				}
				lambda = temp;
				return -logProbability;
			}

			@Override
			public int dimension() {
				return 2;
			}

			@Override
			public double[] derivativeAt(double[] x) {
				double[] sumLogDerivative = new double[2];
				double[] temp = lambda.clone();
				lambda[0] = x[0];
				lambda[1] = x[1];
				for (List<String> sentence : validationCollection) {
					double[] d = getSentenceDerivative(sentence);
					sumLogDerivative[0] -= d[0];
					sumLogDerivative[1] -= d[1];
				}
				lambda = temp;
				return sumLogDerivative;
			}
		}, new double[] { 0.3, 0.5 }, 1e-4);
		lambda = ans;

		// lambda = 0.88;
		System.out.println(String.format("lambda:\t%f\t%f", lambda[0],
				lambda[1]));
	}
}
