package nlp.assignments.NameClassification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;

class CharUnigramLanguageModel implements GradualLanguageModel {

	static final String STOP = "</S>";

	double total = 0.0;
	Counter<String> wordCounter = new Counter<String>();

	public double getWordProbability(List<String> sentence, int index) {
		String word = sentence.get(index);
		return getWordProbobility(word);
	}

	public double getWordProbobility(String word) {
		double c = wordCounter.getCount(word);
		if (c == 0) {
			c = 1;
		}
		return c / (total + 1);
	}

	public double getSentenceProbability(String sentence) {
		List<String> list = new ArrayList<String>();

		for (int i = 0; i < sentence.length(); i++) {
			list.add(String.valueOf(sentence.charAt(i)));
		}
		return getSentenceProbability(list);
	}

	public double getSentenceProbability(List<String> sentence) {
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(STOP);
		double probability = 1.0;
		for (int index = 0; index < stoppedSentence.size(); index++) {
			probability *= getWordProbability(stoppedSentence, index);
		}
		return probability;
	}

	String generateWord() {
		double sample = Math.random();
		double sum = 0.0;
		for (String word : wordCounter.keySet()) {
			sum += wordCounter.getCount(word) / total;
			if (sum > sample) {
				return word;
			}
		}
		return "*UNKNOWN*";
	}

	public List<String> generateSentence() {
		List<String> sentence = new ArrayList<String>();
		String word = generateWord();
		while (!word.equals(STOP)) {
			sentence.add(word);
			word = generateWord();
		}
		return sentence;
	}

	public CharUnigramLanguageModel() {
	}

	public void Train(List<String> sentence) {
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(STOP);
		for (String word : stoppedSentence) {
			wordCounter.incrementCount(word, 1.0);
		}
		total = wordCounter.totalCount();
	}

	public CharUnigramLanguageModel(Collection<List<String>> sentenceCollection) {
		for (List<String> sentence : sentenceCollection) {
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			stoppedSentence.add(STOP);
			for (String word : stoppedSentence) {
				wordCounter.incrementCount(word, 1.0);
			}
		}
		total = wordCounter.totalCount();
	}
}
