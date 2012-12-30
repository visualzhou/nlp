package nlp.assignments.tagger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nlp.util.Counter;
import nlp.util.CounterMap;

/**
 * The MostFrequentTagScorer gives each test word the tag it was seen with
 * most often in training (or the tag with the most seen word types if the
 * test word is unseen in training. This scorer actually does a little more
 * than its name claims -- if constructed with restrictTrigrams = true, it
 * will forbid illegal tag trigrams, otherwise it makes no use of tag
 * history information whatsoever.
 */
public class MostFrequentTagScorer implements LocalTrigramScorer {

	boolean restrictTrigrams; // if true, assign log score of
								// Double.NEGATIVE_INFINITY to illegal tag
								// trigrams.

	CounterMap<String, String> wordsToTags = new CounterMap<String, String>();
	Counter<String> unknownWordTags = new Counter<String>();
	Set<String> seenTagTrigrams = new HashSet<String>();

	public int getHistorySize() {
		return 2;
	}

	public Counter<String> getLogScoreCounter(
			LocalTrigramContext localTrigramContext) {
		int position = localTrigramContext.getPosition();
		String word = localTrigramContext.getWords().get(position);
		Counter<String> tagCounter = unknownWordTags;
		if (wordsToTags.keySet().contains(word)) {
			tagCounter = wordsToTags.getCounter(word);
		}
		Set<String> allowedFollowingTags = allowedFollowingTags(
				tagCounter.keySet(),
				localTrigramContext.getPreviousPreviousTag(),
				localTrigramContext.getPreviousTag());
		Counter<String> logScoreCounter = new Counter<String>();
		for (String tag : tagCounter.keySet()) {
			double logScore = Math.log(tagCounter.getCount(tag));
			if (!restrictTrigrams || allowedFollowingTags.isEmpty()
					|| allowedFollowingTags.contains(tag))
				logScoreCounter.setCount(tag, logScore);
		}
		return logScoreCounter;
	}

	private Set<String> allowedFollowingTags(Set<String> tags,
			String previousPreviousTag, String previousTag) {
		Set<String> allowedTags = new HashSet<String>();
		for (String tag : tags) {
			String trigramString = makeTrigramString(previousPreviousTag,
					previousTag, tag);
			if (seenTagTrigrams.contains((trigramString))) {
				allowedTags.add(tag);
			}
		}
		return allowedTags;
	}

	private String makeTrigramString(String previousPreviousTag,
			String previousTag, String currentTag) {
		return previousPreviousTag + " " + previousTag + " " + currentTag;
	}

	public void train(
			List<LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
		// collect word-tag counts
		for (LabeledLocalTrigramContext labeledLocalTrigramContext : labeledLocalTrigramContexts) {
			String word = labeledLocalTrigramContext.getCurrentWord();
			String tag = labeledLocalTrigramContext.getCurrentTag();
			if (!wordsToTags.keySet().contains(word)) {
				// word is currently unknown, so tally its tag in the
				// unknown tag counter
				unknownWordTags.incrementCount(tag, 1.0);
			}
			wordsToTags.incrementCount(word, tag, 1.0);
			seenTagTrigrams.add(makeTrigramString(
					labeledLocalTrigramContext.getPreviousPreviousTag(),
					labeledLocalTrigramContext.getPreviousTag(),
					labeledLocalTrigramContext.getCurrentTag()));
		}
		wordsToTags.normalize();
		unknownWordTags.normalize();
	}

	public void validate(
			List<LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
		// no tuning for this dummy model!
	}

	public MostFrequentTagScorer(boolean restrictTrigrams) {
		this.restrictTrigrams = restrictTrigrams;
	}
}