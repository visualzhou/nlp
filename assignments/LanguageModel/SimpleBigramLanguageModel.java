package nlp.assignments.LanguageModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;
import nlp.util.Counters;

/**
 * A language model -- uses bigram counts, Good-Turning smoothing for unknown
 * words.
 */
class SimpleBigramLanguageModel implements LanguageModel {

	protected double total = 0.0; // include UNKNOWN
	protected CounterMap<String, String> ngramCounter = new CounterMap<String, String>();
	protected double VocabularySize;
	protected GTUnigramLanguageModel unigramLM;

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
//		if (preWord.equals("a") && word.equals("sale")) {
//			preWord = "a";
//		}
		if (!ngramCounter.containsKey(preWord)) {
			preWord = UNSEEN;
		}
		Counter<String> vCounter = ngramCounter.getCounter(preWord);
		if (vCounter.containsKey(word)) {
			count = vCounter.getCount(word);
			ans = count / vCounter.totalCount();
		} else {
			ans = vCounter.getCount(UNSEEN)
					/ (VocabularySize + 1 - vCounter.size() + 1);
			ans /= vCounter.totalCount();
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

	protected String generateWord(String preWord) {
		if (ngramCounter.containsKey(preWord)) {
			String word = Counters.sample(ngramCounter.getCounter(preWord));
			if (word.equals(UNSEEN)) {
				return unigramLM.generateWord();
			}
			return word;
		} else {
			System.out.println("Generate OOV word!");
			return unigramLM.generateWord();
		}
	}

	public List<String> generateSentence() {
		List<String> sentence = new ArrayList<String>();
		String word = generateWord(START);
		while (!word.equals(STOP)) {
			sentence.add(word);
			word = generateWord(word);
		}
		return sentence;
	}

	public void checkProbability() {
		// int i = 0;
		// System.out.println("TotoalSize: " + ngramCounter.totalSize());
		// for (String key : ngramCounter.keySet()) {
		// double p = 0.0;
		// for (String v : unigramLM.wordCounter.keySet()) {
		// if (v.equalsIgnoreCase(UNSEEN)) {
		// continue;
		// }
		// p += getWordProbability(key, v);
		// }
		// // p += getWordProbability(key, UNKNOWN);
		// Counter<String> vCounter = ngramCounter.getCounter(key);
		// System.out.println(String.format(
		// "Probalility sum: %s\t%f\tSumcount:\t%f", key, p,
		// vCounter.totalCount()));
		//
		// i++;
		// if (i > 10) {
		// break;
		// }
		// }
	}

	protected SimpleBigramLanguageModel() {
	}

	public SimpleBigramLanguageModel(Collection<List<String>> sentenceCollection) {
		this();
		// unigram
		unigramLM = new GTUnigramLanguageModel(sentenceCollection);
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
		// smoothing
		smooth();
		checkProbability();
	}

	protected void smooth() {
		double preTotal = ngramCounter.totalCount();
		double unseenTotalCount = GoodTurningSmoothing.Smooth(ngramCounter);
		// Add UNSEEN to each sub-counter, e.g. (w, UNSEEN) is defined.
		VocabularySize = ngramCounter.size();
		// including UNKNOWN, excluding START

		/*
		 * (START|[V], [V]|STOP) (|V|+1)(|V|+1)
		 */
		double totalPairCount = VocabularySize * (VocabularySize + 2) + 1;
		double unseenArgCount = unseenTotalCount
				/ (totalPairCount - ngramCounter.totalSize());

		for (String key : ngramCounter.keySet()) {
			Counter<String> vCounter = ngramCounter.getCounter(key);
			double unseenCount = (VocabularySize + 1 - vCounter.size())
					* unseenArgCount;
			// |V| times |V|+1
			ngramCounter.incrementCount(key, UNSEEN, unseenCount);
		}
		ngramCounter.incrementCount(UNSEEN, UNSEEN, (VocabularySize + 1)
				* unseenArgCount);
		// |V| + 1
		total = ngramCounter.totalCount();

		System.out.println(String.format(
				"PreTotal bigram: %f\tAfter smoothing: %f", preTotal, total));
	}
}
