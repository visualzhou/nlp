package nlp.parser;

import java.util.List;

import nlp.ling.Tree;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class SimpleLexicon extends Lexicon {

	public SimpleLexicon(CounterMap<String, String> wordToTagCounters,
			Counter<String> tagCounter) {
		super(wordToTagCounters, tagCounter);
	}
	
	/**
	 * Assume all words are seen in the past
	 * Used in EM training
	 */
	public double scoreTagging(String word, String tag) {
		double p_tag = tagCounter.getCount(tag) / tagCounter.totalCount();
		double c_word = wordToTagCounters.getCounter(word).totalCount();
		double c_tag_and_word = wordToTagCounters.getCount(word, tag);
//		if (c_word < 10) { // rare or unknown
//			c_word += 1.0;
//			c_tag_and_word += tagCounter.getCount(tag) / tagCounter.totalCount();
//		}
		double p_tag_given_word = c_tag_and_word / c_word;
		return p_tag_given_word / p_tag;
	}

	public CounterMap<String, String> getWordToTagCounters() {
		return wordToTagCounters;
	}
	
	public static SimpleLexicon createSimpleLexicon(List<Tree<String>> trainTrees) {
		CounterMap<String, String> wordToTagCounters = new CounterMap<String, String>();
		Counter<String> tagCounter = new Counter<String>();
		for (Tree<String> trainTree : trainTrees) {
			List<String> words = trainTree.getYield();
			List<String> tags = trainTree.getPreTerminalYield();
			for (int position = 0; position < words.size(); position++) {
				String word = words.get(position);
				String tag = tags.get(position);
				tagCounter.incrementCount(tag, 1.0);
				wordToTagCounters.incrementCount(word, tag, 1.0);
			}
		}
		return new SimpleLexicon(wordToTagCounters, tagCounter);
	}
//	public Counter<String> getTagCounter()
//	{
//		return tagCounter;
//	}
}
