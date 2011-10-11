package nlp.classify;

import nlp.util.Counter;

/**
 * Probabilistic classifiers assign distributions over labels to instances.
 */
public interface ProbabilisticClassifier<I,L> extends Classifier<I,L> {
  Counter<L> getProbabilities(I instance);
}
