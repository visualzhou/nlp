package nlp.assignments.NameClassification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;
import nlp.util.Counters;

/**
 * A language model -- uses bigram counts, Good-Turning smoothing for unknown
 * words.
 */
class CharBigramLanguageModel implements GradualLanguageModel {

	CharUnigramLanguageModel unigramLanguageModel = new CharUnigramLanguageModel();
	protected CounterMap<String, String> ngramCounter = new CounterMap<String, String>();

	public double getWordProbability(List<String> sentence, int index) {
		String word = sentence.get(index);
		String preWord;
		if (index == 0) {
			preWord = START;
		} else {
			preWord = sentence.get(index - 1);
		}
		return getWordProbability(preWord, word);
	}

	public double getWordProbability(String preWord, String word) {
		double count;
		double ans;

		if (!ngramCounter.containsKey(preWord)) {
			return unigramLanguageModel.getWordProbobility(word)
					/ ngramCounter.size();
		}
		Counter<String> vCounter = ngramCounter.getCounter(preWord);
		if (vCounter.containsKey(word)) {
			count = vCounter.getCount(word);
			ans = count / vCounter.totalCount();
		} else {
			return unigramLanguageModel.getWordProbobility(word)
					/ ngramCounter.size();
		}
		return ans;
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

	public String Show(String sentence) {
		String[] list = sentence.split("\\s*,\\s*");
		StringBuilder sb = new StringBuilder();
		double total = 1.0;
		for (int i = 1; i < list.length; i++) {
			double p = getWordProbability(list[i - 1], list[i]);
			total *= p;
			sb.append(String.format("%s\t%s\t%f\n", list[i - 1], list[i], p));
		}
		sb.append(String.format("Total:\t%f\n", total));
		return sb.toString();
	}

	public List<String> generateSentence() {
		throw new NotImplementedException();
	}

	public CharBigramLanguageModel() {
	}

	public void Train(List<String> sentence) {
		// train unigram
		unigramLanguageModel.Train(sentence);

		List<String> modifiedSentence = new ArrayList<String>();
		modifiedSentence.add(START);
		modifiedSentence.addAll(sentence);
		modifiedSentence.add(STOP);
		for (int i = 0; i < modifiedSentence.size() - 1; i++) {
			ngramCounter.incrementCount(modifiedSentence.get(i),
					modifiedSentence.get(i + 1), 1.0);
		}
	}

	public CharBigramLanguageModel(Collection<List<String>> sentenceCollection) {
		this();
		for (List<String> sentence : sentenceCollection) {
			// List<String> stoppedSentence = new ArrayList<String>(sentence);
			List<String> modifiedSentence = new ArrayList<String>();
			modifiedSentence.add(START);
			modifiedSentence.addAll(sentence);
			modifiedSentence.add(STOP);
			for (int i = 0; i < modifiedSentence.size() - 1; i++) {
				ngramCounter.incrementCount(modifiedSentence.get(i),
						modifiedSentence.get(i + 1), 1.0);
			}
		}
	}
}
