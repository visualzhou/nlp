package nlp.assignments.NameClassification;

import java.util.List;

import nlp.langmodel.LanguageModel;

public interface GradualLanguageModel extends LanguageModel {
	public void Train(List<String> sentence);
}
