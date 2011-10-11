package nlp.assignments.LanguageModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;

/**
 * A dummy language model -- uses empirical unigram counts, Good-Turning
 * smoothing for unknown words.
 */
class GTUnigramLanguageModel implements LanguageModel {

	double total = 0.0; // include UNKNOWN
	Counter<String> wordCounter = new Counter<String>();

	public double getWordProbability(List<String> sentence, int index) {
		String word = sentence.get(index);
		return getWordProbability(word);
	}

	public double getWordProbability(String word) {
		if (!wordCounter.containsKey(word)) {
			word = LanguageModel.UNKNOWN;
		}
		double count = wordCounter.getCount(word);
		// if (count == 0) {
		// // System.out.println("UNKNOWN WORD: "+sentence.get(index));
		// count = wordCounter.getCount(LanguageModel.UNKNOWN);
		// }
		return count / (total);
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

	public String generateWord() {
		double sample = Math.random();
		double sum = 0.0;
		for (String word : wordCounter.keySet()) {
			sum += wordCounter.getCount(word) / total;
			if (sum > sample) {
				return word;
			}
		}
		return LanguageModel.UNKNOWN;
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

	public String Show(String sentence) {
		String[] list = sentence.split("\\s*,\\s*");
		StringBuilder sb = new StringBuilder();
		double total = 1.0;
		for (int i = 0; i < list.length; i++) {
			double p = getWordProbability(list[i]);
			total *= p;
			sb.append(String.format("%s\t%f\n", list[i], p));
		}
		sb.append(String.format("Total:\t%f\n", total));
		return sb.toString();
	}

	public GTUnigramLanguageModel(Collection<List<String>> sentenceCollection) {
		for (List<String> sentence : sentenceCollection) {
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			stoppedSentence.add(STOP);
			for (String word : stoppedSentence) {
				wordCounter.incrementCount(word, 1.0);
			}
		}

		double preTotal = wordCounter.totalCount();
		double n0 = GoodTurningSmoothing.Smooth(wordCounter);
		wordCounter.incrementCount(LanguageModel.UNKNOWN, n0);

		total = wordCounter.totalCount();

		System.out.println(String.format(
				"PreTotal unigram: %f\tAfter smoothing: %f", preTotal, total));
	}
}
