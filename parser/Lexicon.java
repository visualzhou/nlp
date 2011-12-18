package nlp.parser;

import java.util.List;
import java.util.Set;

import nlp.ling.Tree;
import nlp.util.Counter;
import nlp.util.CounterMap;

/**
 * Simple default implementation of a lexicon, which scores word, tag pairs with
 * a smoothed estimate of P(tag|word)/P(tag).
 */
public class Lexicon {
	CounterMap<String, String> wordToTagCounters;
	Counter<String> tagCounter;

	public Set<String> getAllTags() {
		return tagCounter.keySet();
	}

	public boolean isKnown(String word) {
		return wordToTagCounters.containsKey(word);
	}

	public double scoreTagging(String word, String tag) {
		double p_tag = tagCounter.getCount(tag) / tagCounter.totalCount();
		double c_word = wordToTagCounters.getCounter(word).totalCount();
		double c_tag_and_word = wordToTagCounters.getCount(word, tag);
		if (c_word < 10) { // rare or unknown
			c_word += 1.0;
			c_tag_and_word += tagCounter.getCount(tag)
					/ tagCounter.totalCount();
		}
		double p_tag_given_word = c_tag_and_word / c_word;
		return p_tag_given_word / p_tag;
	}

	public Lexicon(CounterMap<String, String> wordToTagCounters) {
		this.wordToTagCounters = wordToTagCounters;
		this.tagCounter = buildTagCounter(wordToTagCounters);
	}

	public static Lexicon createLexicon(List<Tree<String>> trainTrees) {
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
		return new Lexicon(wordToTagCounters);
	}

	private static Counter<String> buildTagCounter(
			CounterMap<String, String> wordToTagCounters) {
		Counter<String> tagCounter = new Counter<String>();
		for (String word : wordToTagCounters.keySet()) {
			Counter<String> vCounter = wordToTagCounters.getCounter(word);
			for (String tag : vCounter.keySet()) {
				tagCounter.incrementCount(tag, vCounter.getCount(tag));
			}
		}
		return tagCounter;
	}
}
