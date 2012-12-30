package nlp.assignments.tagger;

import java.util.ArrayList;
import java.util.List;

import nlp.assignments.MaximumEntropyClassifier;
import nlp.assignments.LanguageModel.GoodTurningSmoothing;
import nlp.classify.LabeledInstance;
import nlp.classify.ProbabilisticClassifier;
import nlp.classify.ProbabilisticClassifierFactory;
import nlp.langmodel.LanguageModel;
import nlp.math.DoubleArrays;
import nlp.util.Counter;
import nlp.util.CounterMap;

/**
 * P(t_i | t_(i-1), t_(i-2)) * P(W|t_i);
 * 
 * @author syzhou
 * 
 */
public class TrigramTagScorer implements LocalTrigramScorer {

	public CounterMap<String, String> trigram = new CounterMap<String, String>();
	public CounterMap<String, String> bigram = new CounterMap<String, String>();
	Counter<String> unigram = new Counter<String>();

	CounterMap<String, String> tagtowords = new CounterMap<String, String>();
	CounterMap<String, String> wordtotags = new CounterMap<String, String>();
	Counter<String> unknownWordTags = new Counter<String>();
	double[] lambda = new double[3];
	ProbabilisticClassifier<String, String> unknownClassifier = null;
	public static final String UNKWORD = LanguageModel.UNKNOWN;
	Counter<String> tagsProbabilityCounter = new Counter<String>();

	@Override
	public Counter<String> getLogScoreCounter(
			LocalTrigramContext localTrigramContext) {
		String preTags = makePrefixString(
				localTrigramContext.previousPreviousTag,
				localTrigramContext.previousTag);
		Counter<String> allowedTags = trigram.getCounter(preTags);

		String word = localTrigramContext.getCurrentWord();
		Counter<String> scoreCounter = new Counter<String>();
		// unknown word
		if (!wordtotags.containsKey(word)) {
			Counter<String> estimatedTags = unknownClassifier
					.getProbabilities(MaxentClassifier4POSTagger
							.addProperties(word,
									localTrigramContext.getPosition(),
									localTrigramContext.getPreviousTag()));
			// only loop for seen tags
			for (String tag : allowedTags.keySet()) {
				// unknown word
				if (!unknownWordTags.containsKey(tag)) {
					continue;
				}
				double p = estimatedTags.getCount(tag) // emission
						* tagtowords.getCount(tag, UNKWORD)
						* 1E-10
						/ tagsProbabilityCounter.getCount(tag)
						// times P(t_i|t_(i-1), t_(i-2)) transition
						* getProbability(localTrigramContext, tag);
				scoreCounter.setCount(tag, Math.log(p));
			}
			return scoreCounter;
		}
		for (String tag : wordtotags.getCounter(word).keySet()) {
			// only when w | t_i occurs
			double score = tagtowords.getCount(tag, word)
					* getProbability(localTrigramContext, tag);
			score = Math.log(score);
			scoreCounter.setCount(tag, score);
		}
		return scoreCounter;
	}

	private double getProbability(LocalTrigramContext localTrigramContext,
			String tag) {
		LocalTrigramContext local = localTrigramContext;
		double p = trigram.getCount(
				makePrefixString(local.previousPreviousTag,
						local.previousTag), tag)
				* lambda[0];
		p += bigram.getCount(local.previousTag, tag) * lambda[1];
		p += unigram.getCount(tag) * lambda[2];
		return p;
	}

	private String makePrefixString(String previousPreviousTag,
			String previousTag) {
		return previousPreviousTag + " " + previousTag;
	}

	@Override
	public void train(
			List<LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
		for (LabeledLocalTrigramContext labeledLocalTrigramContext : labeledLocalTrigramContexts) {
			String word = labeledLocalTrigramContext.getCurrentWord();
			String tag = labeledLocalTrigramContext.getCurrentTag();

			tagtowords.incrementCount(tag, word, 1.0);
			wordtotags.incrementCount(word, tag, 1.0);

			String preTags = makePrefixString(
					labeledLocalTrigramContext.previousPreviousTag,
					labeledLocalTrigramContext.previousTag);
			trigram.incrementCount(preTags, tag, 1.0);
			bigram.incrementCount(labeledLocalTrigramContext.previousTag,
					tag, 1.0);
			unigram.incrementCount(tag, 1.0);
		}

		// unknown word

		List<LabeledInstance<String, String>> labeledUNKInstances = MaxentClassifier4POSTagger
				.buildTrainingData(wordtotags, labeledLocalTrigramContexts);
		ProbabilisticClassifierFactory<String, String> factory = new MaximumEntropyClassifier.Factory<String, String, String>(
				1.0, 20,
				new MaxentClassifier4POSTagger.PosTaggerFeatureExtractor());
		unknownClassifier = factory.trainClassifier(labeledUNKInstances);
		List<String> removedWords = new ArrayList<String>();
		for (String word : wordtotags.keySet()) {
			Counter<String> tagCounter = wordtotags.getCounter(word);
			if (tagCounter.totalCount() <= 1.0) {
				for (String tag : tagCounter.keySet()) {
					unknownWordTags.incrementCount(tag, 1.0);
				}
				removedWords.add(word);
			}
		}
		unknownWordTags.normalize();
		for (String word : removedWords) {
			wordtotags.remove(word);
		}
		// train UNK
		trainUNK(labeledUNKInstances);
	}

	private void trainUNK(
			List<LabeledInstance<String, String>> labeledUNKInstances) {
		for (LabeledInstance<String, String> labeledInstance : labeledUNKInstances) {
			String tag = labeledInstance.getLabel();
			tagsProbabilityCounter.incrementCount(tag, 1.0);
		}
		// P(tag) used for UNK
		tagsProbabilityCounter.normalize();
	}

	@Override
	public void validate(
			List<LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
		double[] l = new double[3];
		for (LabeledLocalTrigramContext labeledLocalTrigramContext : labeledLocalTrigramContexts) {
			String tag = labeledLocalTrigramContext.getCurrentTag();
			String preTags = makePrefixString(
					labeledLocalTrigramContext.getPreviousPreviousTag(),
					labeledLocalTrigramContext.getPreviousTag());
			double[] temp = new double[3];
			temp[0] = (trigram.getCount(preTags, tag) - 1)
					/ (trigram.getCounter(preTags).totalCount() - 1);
			temp[1] = (bigram.getCount(
					labeledLocalTrigramContext.getPreviousTag(), tag) - 1)
					/ (bigram.getCounter(
							labeledLocalTrigramContext.getPreviousTag())
							.totalCount() - 1);
			temp[2] = (unigram.getCount(tag) - 1)
					/ (unigram.totalCount() - 1);
			int maxIndex = DoubleArrays.argMax(temp);
			l[maxIndex] += trigram.getCount(preTags, tag);

		}
		DoubleArrays.scale(l, 1.0 / DoubleArrays.add(l));
		lambda = l;

		// probability fo UNK
		GoodTurningSmoothing.Smooth(tagtowords, UNKWORD);

		// normalize
		trigram.normalize();
		bigram.normalize();
		unigram.normalize();
	}
}