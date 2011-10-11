package nlp.classify;

import nlp.util.Counter;

/**
 * Feature extractors process input instances into feature counters.
 */
public interface FeatureExtractor<I,O> {
  Counter<O> extractFeatures(I instance);
}
