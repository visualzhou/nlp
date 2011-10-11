package nlp.classify;

/**
 * LabeledDatums add a label to the basic FeatureVector interface.
 */
public interface LabeledFeatureVector<F,L> extends FeatureVector<F> {
  L getLabel();
}
