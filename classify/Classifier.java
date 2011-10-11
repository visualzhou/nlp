package nlp.classify;

/**
 * Classifiers assign labels to instances.
 */
public interface Classifier<I,L> {
  L getLabel(I instance);
}
