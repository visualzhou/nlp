package nlp.classify;


import java.util.List;

import nlp.classify.LabeledFeatureVector;
import nlp.util.Counter;

/**
 * A minimal implementation of a labeled datum, wrapping a list of features and
 * a label.
 *
 */
public class BasicLabeledFeatureVector <F,L> implements LabeledFeatureVector<F, L> {
  L label;
  Counter<F> features;

  public L getLabel() {
    return label;
  }

  public Counter<F> getFeatures() {
    return features;
  }

  public String toString() {
    return "<" + getLabel() + " : " + getFeatures().toString() + ">";
  }

  public BasicLabeledFeatureVector(L label, Counter<F> features) {
    this.label = label;
    this.features = features;
  }
}
