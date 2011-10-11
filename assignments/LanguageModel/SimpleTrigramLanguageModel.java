package nlp.assignments.LanguageModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nlp.util.Counter;
import nlp.util.Counters;

public class SimpleTrigramLanguageModel extends SimpleBigramLanguageModel {

	public static final String WORDSEPERATOR = " ";
	protected SimpleBigramLanguageModel bigramLanguageModel;
	protected double unseenTrigramCount;

	protected String CombineWord(String[] words) {
		if (words.length == 1) {
			return words[0];
		} else {
			return words[0] + WORDSEPERATOR + words[1];
		}
	}

	protected String[] getWordPair(List<String> sentence, int index) {
		if (index < 0) {
			return new String[] { START, START };
		} else if (index == 0) {
			return new String[] { START, sentence.get(index) };
		}
		return new String[] { sentence.get(index - 1), sentence.get(index) };
	}

	@Override
	public double getWordProbability(List<String> sentence, int index) {
		return getWordProbability(getWordPair(sentence, index - 1),
				sentence.get(index));
	}

	@Override
	public double getWordProbability(String preWord, String word) {
		double count;
		double ans;
		if (!ngramCounter.containsKey(preWord)) {
			double unseen = ngramCounter.getCount(UNSEEN, UNSEEN);
			ans = unseen / unseenTrigramCount;
		} else {
			Counter<String> vCounter = ngramCounter.getCounter(preWord);
			if (vCounter.containsKey(word)) {
				count = vCounter.getCount(word);
				ans = count / vCounter.totalCount();
			} else {
				ans = vCounter.getCount(UNSEEN)
						/ (VocabularySize + 1 - (vCounter.size() - 1))
						/ vCounter.totalCount();
			}
		}
		return ans;
	}

	public double getWordProbability(String[] preWords, String word) {
		return getWordProbability(CombineWord(preWords), word);
		// double count;
		// double ans;
		// if (!ngramCounter.containsKey(CombineWord(preWords))) {
		// double unseen = ngramCounter.getCount(UNSEEN, UNSEEN);
		// ans = unseen / unseenTrigramCount;
		// } else {
		// Counter<String> vCounter = ngramCounter
		// .getCounter(CombineWord(preWords));
		// if (vCounter.containsKey(word)) {
		// count = vCounter.getCount(word);
		// ans = count / vCounter.totalCount();
		// } else {
		// ans = vCounter.getCount(UNSEEN)
		// / (VocabularySize + 1 - (vCounter.size() - 1))
		// / vCounter.totalCount();
		// }
		// }
		// return ans;
	}

	@Override
	public List<String> generateSentence() {
		List<String> sentence = new ArrayList<String>();
		String word = generateWord(getWordPair(sentence, -1));
		while (!word.equals(STOP)) {
			sentence.add(word);
			word = generateWord(getWordPair(sentence, sentence.size() - 1));
		}
		return sentence;
	}

	protected String generateWord(String[] preWords) {
		if (ngramCounter.containsKey(CombineWord(preWords))) {
			String word = Counters.sample(ngramCounter
					.getCounter(CombineWord(preWords)));
			if (word.equals(UNSEEN)) {
				return bigramLanguageModel
						.generateWord(preWords[preWords.length - 1]);
			}
			return word;
		} else {
			System.out.println("Generate OOV word!");
			return bigramLanguageModel
					.generateWord(preWords[preWords.length - 1]);
		}
	}

	public SimpleTrigramLanguageModel(
			Collection<List<String>> sentenceCollection) {
		super();
		// bigram
		bigramLanguageModel = new SimpleBigramLanguageModel(sentenceCollection);
		unigramLM = bigramLanguageModel.unigramLM;
		for (List<String> sentence : sentenceCollection) {
			// List<String> stoppedSentence = new ArrayList<String>(sentence);
			List<String> modifiedSentence = new ArrayList<String>();
			modifiedSentence.add(START);
			modifiedSentence.add(START);
			modifiedSentence.addAll(sentence);
			modifiedSentence.add(STOP);
			for (int i = 2; i < modifiedSentence.size(); i++) {
				ngramCounter.incrementCount(
						CombineWord(getWordPair(modifiedSentence, i - 1)),
						modifiedSentence.get(i), 1.0);
			}
		}
		// smoothing
		smooth();
		// smoothAdd1();

		checkProbability();
	}

	protected void smooth() {
		double preTotal = ngramCounter.totalCount();
		double unseenTotalCount = GoodTurningSmoothing.Smooth(ngramCounter);

		VocabularySize = bigramLanguageModel.ngramCounter.size();
		// including UNKNOWN, excluding START

		double totalPairCount = 1.0
				* (VocabularySize * (VocabularySize + 1) + 1)
				* (VocabularySize + 1);
		double unseenArgCount = unseenTotalCount
				/ (totalPairCount - ngramCounter.totalSize());
		unseenTrigramCount = totalPairCount - (VocabularySize + 1)
				* ngramCounter.size();

		for (String key : ngramCounter.keySet()) {
			Counter<String> vCounter = ngramCounter.getCounter(key);
			double unseenCount = (VocabularySize + 1 - vCounter.size())
					* unseenArgCount;
			// |V| * (|V|-1) + 1 times |V|+1
			// ((START|W) W) | (START, START)
			ngramCounter.incrementCount(key, UNSEEN, unseenCount);
		}
		ngramCounter.incrementCount(UNSEEN, UNSEEN, unseenTrigramCount
				* unseenArgCount);
		// |V| + 1
		// (S|W), Unknown |V|
		// Unknown, W |V|-1
		// Unknown, Unknown 1
		total = ngramCounter.totalCount();

		System.out.println(String.format(
				"PreTotal trigram: %f\tAfter smoothing: %f", preTotal, total));
	}

	protected void smoothAdd1() {
		double preTotal = ngramCounter.totalCount();
		double unseenTotalCount = GoodTurningSmoothing.Smooth(ngramCounter);

		VocabularySize = bigramLanguageModel.ngramCounter.size();
		// including UNKNOWN, excluding START

		double totalPairCount = 1.0 * (ngramCounter.size() + 1)
				* (VocabularySize + 1);
		double unseenArgCount = unseenTotalCount
				/ (totalPairCount - ngramCounter.totalSize());
		double v = VocabularySize;

		for (String key : ngramCounter.keySet()) {
			Counter<String> vCounter = ngramCounter.getCounter(key);
			double unseenCount = (VocabularySize + 1 - vCounter.size())
					* unseenArgCount;
			// |V| * (|V|-1) + 1 times |V|+1
			// ((START|W) W) | (START, START)
			ngramCounter.incrementCount(key, UNSEEN, unseenCount);
		}
		unseenTrigramCount = totalPairCount - ngramCounter.size() * (v + 1);
		ngramCounter.incrementCount(UNSEEN, UNSEEN, unseenTrigramCount
				* unseenArgCount);
		// |V| + 1
		// (S|W), Unknown |V|
		// Unknown, W |V|-1
		// Unknown, Unknown 1
		total = ngramCounter.totalCount();

		System.out.println(String.format(
				"PreTotal trigram: %f\tAfter smoothing: %f", preTotal, total));
	}

}
