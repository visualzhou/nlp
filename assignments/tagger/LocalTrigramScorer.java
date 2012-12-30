package nlp.assignments.tagger;

import java.util.List;

import nlp.util.Counter;

/**
 * LocalTrigramScorers assign scores to tags occuring in specific
 * LocalTrigramContexts.
 */
public interface LocalTrigramScorer {
	/**
	 * The Counter returned should contain log probabilities, meaning if all
	 * values are exponentiated and summed, they should sum to one. For
	 * efficiency, the Counter can contain only the tags which occur in the
	 * given context with non-zero model probability.
	 */
	Counter<String> getLogScoreCounter(
			LocalTrigramContext localTrigramContext);

	void train(List<LabeledLocalTrigramContext> localTrigramContexts);

	void validate(List<LabeledLocalTrigramContext> localTrigramContexts);
}