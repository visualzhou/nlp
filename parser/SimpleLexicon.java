package nlp.parser;

import java.util.List;

import nlp.ling.Tree;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class SimpleLexicon extends Lexicon {
	CounterMap<String, String> wordToTagRelativeCounters = new CounterMap<String, String>();

	public SimpleLexicon(CounterMap<String, String> wordToTagCounters) {
		super(wordToTagCounters);
		CounterMap<String, String> baseTagCounterMap = new CounterMap<String, String>();
		for (String word : wordToTagCounters.keySet()) {
			Counter<String> vCounter = wordToTagCounters.getCounter(word);
			for (String tag : vCounter.keySet()) {
				baseTagCounterMap.incrementCount(word,
						GrammarSpliter.getBaseState(tag),
						vCounter.getCount(tag));
			}
		}
		for (String word : wordToTagCounters.keySet()) {
			Counter<String> vCounter = wordToTagCounters.getCounter(word);
			for (String tag : vCounter.keySet()) {
				wordToTagRelativeCounters.setCount(
						word,
						tag,
						vCounter.getCount(tag)
								/ baseTagCounterMap.getCount(word,
										GrammarSpliter.getBaseState(tag)));
			}
		}
	}

	/**
	 * Assume all words are seen in the past Used in EM training
	 */
	public double relativeScore(String word, String tag) {
		double d = wordToTagRelativeCounters.getCount(word, tag);
		return d;

		// double p_tag = tagCounter.getCount(tag) / tagCounter.totalCount();
		// double c_word = wordToTagCounters.getCounter(word).totalCount();
		// double c_tag_and_word = wordToTagCounters.getCount(word, tag);
		// // if (c_word < 10) { // rare or unknown
		// // c_word += 1.0;
		// // c_tag_and_word += tagCounter.getCount(tag) /
		// tagCounter.totalCount();
		// // }
		// double p_tag_given_word = c_tag_and_word / c_word;
		// return p_tag_given_word / p_tag;
	}

	public CounterMap<String, String> getWordToTagCounters() {
		return wordToTagCounters;
	}

	public static SimpleLexicon createSimpleLexicon(
			List<Tree<String>> trainTrees) {
		CounterMap<String, String> wordToTagCounters = new CounterMap<String, String>();
		for (Tree<String> trainTree : trainTrees) {
			List<String> words = trainTree.getYield();
			List<String> tags = trainTree.getPreTerminalYield();
			for (int position = 0; position < words.size(); position++) {
				String word = words.get(position);
				String tag = tags.get(position);
				wordToTagCounters.incrementCount(word, tag, 1.0);
			}
		}
		return new SimpleLexicon(wordToTagCounters);
	}

	public Lexicon buildLexicon() {
		Lexicon lexicon = new Lexicon(wordToTagCounters, tagCounter);
		// Lexicon lexicon = new
		// Lexicon(Counters.cleanCounter(wordToTagCounters));
		return lexicon;
	}
}
