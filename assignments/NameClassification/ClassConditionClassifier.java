package nlp.assignments.NameClassification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nlp.classify.LabeledInstance;
import nlp.classify.ProbabilisticClassifier;
import nlp.classify.ProbabilisticClassifierFactory;
import nlp.langmodel.LanguageModel;
import nlp.util.Counter;

public class ClassConditionClassifier implements
		ProbabilisticClassifier<String, String> {

	Map<String, GradualLanguageModel> models;
	Counter<String> labelCounter;

	public ClassConditionClassifier(Map<String, GradualLanguageModel> models,
			Counter<String> labelCounter) {
		this.models = models;
		this.labelCounter = labelCounter;
		labelCounter.normalize();
	}

	public static class Factory implements
			ProbabilisticClassifierFactory<String, String> {
		Class<GradualLanguageModel> lmClass;

		public Factory(Class lmClass) {
			this.lmClass = lmClass;
		}

		@Override
		public ProbabilisticClassifier<String, String> trainClassifier(
				List<LabeledInstance<String, String>> trainingData) {
			Map<String, GradualLanguageModel> models = new HashMap<String, GradualLanguageModel>();
			Counter<String> labelCounter = new Counter<String>();
			for (LabeledInstance<String, String> datum : trainingData) {
				// P(label)
				labelCounter.incrementCount(datum.getLabel(), 1.0);
				if (!models.containsKey(datum.getLabel())) {
					try {
						models.put(datum.getLabel(), lmClass.newInstance());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				GradualLanguageModel model = models.get(datum.getLabel());
				model.Train(convertToList(datum.getInput()));
			}
			ProbabilisticClassifier<String, String> classifier = new ClassConditionClassifier(
					models, labelCounter);
			return classifier;
		}

	}

	private static List<String> convertToList(String sentence) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < sentence.length(); i++) {
			list.add(String.valueOf(sentence.charAt(i)));
		}
		return list;
	}

	@Override
	public String getLabel(String instance) {
		Counter<String> counter = getProbabilities(instance);
		return counter.argMax();
	}

	@Override
	public Counter<String> getProbabilities(String instance) {
		Counter<String> probabilities = new Counter<String>();
		for (String label : models.keySet()) {
			LanguageModel model = models.get(label);
			double p = model.getSentenceProbability(convertToList(instance))
					* labelCounter.getCount(label);
			probabilities.incrementCount(label, p);
		}
		probabilities.normalize();
		return probabilities;
	}
}
