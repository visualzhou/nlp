package nlp.parser;

import nlp.util.Counter;
import nlp.util.CounterMap;

public class WordTagDump {
	CounterMap<String, String> wordToTagCounters;
	CounterMap<String, String> tagToWordCounterMap;

	public WordTagDump(CounterMap<String, String> wordToTagCounters) {
		this.wordToTagCounters = wordToTagCounters;
		buildTagToWordCounterMap();
	}

	private void buildTagToWordCounterMap() {
		tagToWordCounterMap = new CounterMap<String, String>();
		for (String word : wordToTagCounters.keySet()) {
			Counter<String> vCounter = wordToTagCounters.getCounter(word);
			for (String tag : vCounter.keySet()) {
				tagToWordCounterMap.incrementCount(tag, word,
						vCounter.getCount(tag));
			}
		}
		tagToWordCounterMap.normalize();
	}

	public Counter<String> getCounter(String tag) {
		return tagToWordCounterMap.getCounter(tag);
	}

	public String getPopularWords(String tag) {
		return getCounter(tag).toString(10);
	}
}
