package nlp.assignments.tagger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.assignments.tagger.Trellis.TrellisDecoder;
import nlp.io.PennTreebankReader;
import nlp.ling.Tree;
import nlp.ling.Trees;
import nlp.util.BoundedList;
import nlp.util.CommandLineUtils;
import nlp.util.Counter;
import nlp.util.CounterMap;
import nlp.util.Pair;

/**
 * Harness for POS Tagger project.
 */
public class POSTaggerTester {

    private static List<TaggedSentence> readTaggedSentences(String path,
			int low, int high) {
		Collection<Tree<String>> trees = PennTreebankReader.readTrees(path,
				low, high);
		List<TaggedSentence> taggedSentences = new ArrayList<TaggedSentence>();
		Trees.TreeTransformer<String> treeTransformer = new Trees.EmptyNodeStripper();
		for (Tree<String> tree : trees) {
			tree = treeTransformer.transformTree(tree);
			List<String> words = new BoundedList<String>(new ArrayList<String>(
					tree.getYield()), POSTagger.START_WORD, POSTagger.STOP_WORD);
			List<String> tags = new BoundedList<String>(new ArrayList<String>(
					tree.getPreTerminalYield()), POSTagger.START_TAG, POSTagger.STOP_TAG);
			taggedSentences.add(new TaggedSentence(words, tags));
		}
		return taggedSentences;
	}

	private static void evaluateTagger(POSTagger posTagger,
			List<TaggedSentence> taggedSentences,
			Set<String> trainingVocabulary, boolean verbose) {
		double numTags = 0.0;
		double numTagsCorrect = 0.0;
		double numUnknownWords = 0.0;
		double numUnknownWordsCorrect = 0.0;
		int numDecodingInversions = 0;
		/**
		 * @author syzhou
		 */
		Counter<Pair<String, String>> confusionMatrix = new Counter<Pair<String, String>>();
		CounterMap<String, String> errorCause = new CounterMap<String, String>();
		for (TaggedSentence taggedSentence : taggedSentences) {
			List<String> words = taggedSentence.getWords();
			List<String> goldTags = taggedSentence.getTags();
			List<String> guessedTags = posTagger.tag(words);
			boolean accurate = true;
			for (int position = 0; position < words.size() - 1; position++) {
				String word = words.get(position);
				String goldTag = goldTags.get(position);
				String guessedTag = guessedTags.get(position);
				String isKNOWN = trainingVocabulary.contains(word) ? "KNOWN"
						: "UNKNOWN";
				String guessResult = null;
				if (guessedTag.equals(goldTag)) {
					numTagsCorrect += 1.0;
					guessResult = "Right";
				} else {
					// tag error
					accurate = false;
					confusionMatrix.incrementCount(new Pair<String, String>(
							goldTag, guessedTag), 1.0);
					guessResult = "Wrong";
				}
				errorCause.incrementCount(isKNOWN, guessResult, 1.0);
				numTags += 1.0;
				if (!trainingVocabulary.contains(word)) {
					if (guessedTag.equals(goldTag))
						numUnknownWordsCorrect += 1.0;
					numUnknownWords += 1.0;
				}
			}
			double scoreOfGoldTagging = posTagger.scoreTagging(taggedSentence);
			double scoreOfGuessedTagging = posTagger
					.scoreTagging(new TaggedSentence(words, guessedTags));
			if (scoreOfGoldTagging > scoreOfGuessedTagging) {
				numDecodingInversions++;
				if (verbose)
					System.out
							.println("WARNING: Decoder suboptimality detected.  Gold tagging has higher score than guessed tagging.");
			}
			if (verbose && !accurate)
				System.out.println(alignedTaggings(words, goldTags,
						guessedTags, true) + "\n");
		}
		System.out.println("Tag Accuracy: " + (numTagsCorrect / numTags)
				+ " (Unknown Accuracy: "
				+ (numUnknownWordsCorrect / numUnknownWords)
				+ ")  Decoder Suboptimalities Detected: "
				+ numDecodingInversions);
		confusionMatrix.normalize();
		System.out.println(confusionMatrix.toString(10));
		System.out.println(errorCause.toString());
		errorCause.normalize();
		System.out.println(errorCause.toString());
	}

	// pretty-print a pair of taggings for a sentence, possibly suppressing the
	// tags which correctly match
	private static String alignedTaggings(List<String> words,
			List<String> goldTags, List<String> guessedTags,
			boolean suppressCorrectTags) {
		StringBuilder goldSB = new StringBuilder("Gold Tags: ");
		StringBuilder guessedSB = new StringBuilder("Guessed Tags: ");
		StringBuilder wordSB = new StringBuilder("Words: ");
		for (int position = 0; position < words.size(); position++) {
			equalizeLengths(wordSB, goldSB, guessedSB);
			String word = words.get(position);
			String gold = goldTags.get(position);
			String guessed = guessedTags.get(position);
			wordSB.append(word);
			if (position < words.size() - 1)
				wordSB.append(' ');
			boolean correct = (gold.equals(guessed));
			if (correct && suppressCorrectTags)
				continue;
			guessedSB.append(guessed);
			goldSB.append(gold);
		}
		return goldSB + "\n" + guessedSB + "\n" + wordSB;
	}

	private static void equalizeLengths(StringBuilder sb1, StringBuilder sb2,
			StringBuilder sb3) {
		int maxLength = sb1.length();
		maxLength = Math.max(maxLength, sb2.length());
		maxLength = Math.max(maxLength, sb3.length());
		ensureLength(sb1, maxLength);
		ensureLength(sb2, maxLength);
		ensureLength(sb3, maxLength);
	}

	private static void ensureLength(StringBuilder sb, int length) {
		while (sb.length() < length) {
			sb.append(' ');
		}
	}

	private static Set<String> extractVocabulary(
			List<TaggedSentence> taggedSentences) {
		Set<String> vocabulary = new HashSet<String>();
		for (TaggedSentence taggedSentence : taggedSentences) {
			List<String> words = taggedSentence.getWords();
			vocabulary.addAll(words);
		}
		return vocabulary;
	}

	public static void main(String[] args) {
		// Parse command line flags and arguments
		Map<String, String> argMap = CommandLineUtils
				.simpleCommandLineParser(args);

		// Set up default parameters and settings
		String basePath = ".";
		boolean verbose = false;
		boolean useValidation = true;

		// Update defaults using command line specifications

		// The path to the assignment data
		if (argMap.containsKey("-path")) {
			basePath = argMap.get("-path");
		}
		System.out.println("Using base path: " + basePath);

		// Whether to use the validation or test set
		if (argMap.containsKey("-test")) {
			String testString = argMap.get("-test");
			if (testString.equalsIgnoreCase("test"))
				useValidation = false;
		}
		System.out.println("Testing on: "
				+ (useValidation ? "validation" : "test"));

		// Whether or not to print the individual errors.
		if (argMap.containsKey("-verbose")) {
			verbose = true;
		}

		// Read in data
		System.out.print("Loading training sentences...");
		List<TaggedSentence> trainTaggedSentences = readTaggedSentences(
				basePath, 200, 2199);
		Set<String> trainingVocabulary = extractVocabulary(trainTaggedSentences);
		System.out.println("done.");
		System.out.print("Loading validation sentences...");
		List<TaggedSentence> validationTaggedSentences = readTaggedSentences(
				basePath, 2200, 2299);
		System.out.println("done.");
		System.out.print("Loading test sentences...");
		List<TaggedSentence> testTaggedSentences = readTaggedSentences(
				basePath, 2300, 2399);
		System.out.println("done.");

		// Construct tagger components
		// TODO : improve on the MostFrequentTagScorer
		LocalTrigramScorer localTrigramScorer = null;
		if (argMap.containsKey("-model")) {
			String modelString = argMap.get("-model");
			if (modelString.equalsIgnoreCase("base")) {
				localTrigramScorer = new MostFrequentTagScorer(false);
			}
			if (modelString.equalsIgnoreCase("trigram")) {
				localTrigramScorer = new TrigramTagScorer();
			}
		} else {
			localTrigramScorer = new MostFrequentTagScorer(false);
		}
		// TODO : improve on the GreedyDecoder
		// TrellisDecoder<State> trellisDecoder = new GreedyDecoder<State>();
		TrellisDecoder<POSTagger.State> trellisDecoder = new Trellis.ViterbiDecoder<POSTagger.State>();

		// Train tagger
		POSTagger posTagger = new POSTagger(localTrigramScorer, trellisDecoder);
		posTagger.train(trainTaggedSentences);
		posTagger.validate(validationTaggedSentences);

		// Test tagger
		evaluateTagger(posTagger, testTaggedSentences, trainingVocabulary,
				verbose);
	}
}
