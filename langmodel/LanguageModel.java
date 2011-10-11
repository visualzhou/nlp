package nlp.langmodel;

import java.util.List;

/**
 * Language models assign probabilities to sentences and generate sentences.
 */
public interface LanguageModel {
	public static final String UNKNOWN = "*UNKNOWN*";
	public static final String UNSEEN = "*UNSEEN*";
	public static final String STOP = "</S>";
	public static final String START = "</S>";

	double getSentenceProbability(List<String> sentence);

	List<String> generateSentence();
}
