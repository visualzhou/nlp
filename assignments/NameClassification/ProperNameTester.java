package nlp.assignments.NameClassification;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.sun.tools.javac.util.Pair;

import nlp.assignments.MaximumEntropyClassifier;
import nlp.assignments.MostFrequentLabelClassifier;
import nlp.classify.*;
import nlp.util.BoundedList;
import nlp.util.CommandLineUtils;
import nlp.util.Counter;
import nlp.util.CounterMap;

/**
 * This is the main harness for assignment 2. To run this harness, use
 * <p/>
 * java edu.berkeley.nlp.assignments.ProperNameTester -path ASSIGNMENT_DATA_PATH
 * -model MODEL_DESCRIPTOR_STRING
 * <p/>
 * First verify that the data can be read on your system using the baseline
 * model. Second, find the point in the main method (near the bottom) where a
 * MostFrequentLabelClassifier is constructed. You will be writing new
 * implementations of the ProbabilisticClassifer interface and constructing them
 * there.
 */
public class ProperNameTester {

	public static class ProperNameFeatureExtractor implements
			FeatureExtractor<String, String> {

		Pattern specialChar = Pattern.compile("[\\W&&\\S]+");
		Pattern digitalSuffixChar = Pattern.compile("\\d+$");
		Pattern digitalPrefixChar = Pattern.compile("^\\d+");

		/**
		 * This method takes the list of characters representing the proper name
		 * description, and produces a list of features which represent that
		 * description. The basic implementation is that only character-unigram
		 * features are extracted. An easy extension would be to also throw
		 * character bigrams into the feature list, but better features are also
		 * possible.
		 */
		public Counter<String> extractFeatures(String name) {
			char[] characters = name.toCharArray();
			Counter<String> features = new Counter<String>();
			// add character unigram features
			// for (int i = 0; i < characters.length; i++) {
			// char character = characters[i];
			// features.incrementCount("UNI-" + character, 1.0);
			// }
			// extract better features!

			List<String> charList = new ArrayList<String>();
			for (int i = 0; i < characters.length; i++) {
				charList.add(String.valueOf(characters[i]));
			}
			BoundedList<String> boundedString = new BoundedList<String>(
					charList, "<S>");

			// // Bi-gram
			// for (int i = 0; i <= characters.length; i++) {
			// features.incrementCount("BI-" + boundedString.get(i - 1)
			// + boundedString.get(i), 1.0);
			// }
			// 3-gram
			for (int i = 0; i < characters.length + 2; i++) {
				features.incrementCount("TRI-" + boundedString.get(i - 2)
						+ boundedString.get(i - 1) + boundedString.get(i), 1.0);
			}
			// 4-gram
			for (int i = 0; i < characters.length + 3; i++) {
				features.incrementCount("QUA-" + boundedString.get(i - 3)
						+ boundedString.get(i - 2) + boundedString.get(i - 1)
						+ boundedString.get(i), 1.0);
			}
			// Suffix 1-5
//			for (int i = 3; i < 6; i++) {
//				String str = "SUFFIX-";
//				for (int j = characters.length - i; j < characters.length; j++) {
//					str += boundedString.get(j);
//				}
//				features.incrementCount(str, 1.0);
//			}

			// Prefix
			for (int i = 3; i < 6; i++) {
				String str = "PREFIX-";
				for (int j = 0; j < i; j++) {
					str += boundedString.get(j);
				}
				features.incrementCount(str, 1.0);
			}
			// Whole word
			features.incrementCount("ALL-" + name, 1.0);

			// words
			int wordlength = 0;
			// String[] words = name.split("[\\s,-]+");
			String[] words = name.split("[\\W]+");
			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				features.incrementCount("WORD-" + word, 1.0);
				wordlength += word.length();
				// Suffix 1-5
				for (int k = 3; k < 7; k++) {
					if (word.length() - k >= 0) {
						features.incrementCount(
								"WORDSUFFIX-" + word.substring(word.length() - k),
								1.0);
					}
				}
				// prefix
				for (int k = 2; k < 6; k++) {
					if (word.length() > k) {
						features.incrementCount(
								"WORDPREFIX-" + word.substring(0, k),
								1.0);
					}
				}
			}
			// Arg length of words
			if (words.length > 0) {
				wordlength /= words.length;
			}
			features.incrementCount("ARG-LENGHT-" + wordlength, 1.0);
			// Number of words
			features.incrementCount("COUNT-WORD-" + words.length, 1.0);
			// length
			features.incrementCount("LENGTH-" + name.length(), 1.0);

			// special character
			Matcher m = specialChar.matcher(name);
			if (m.find()) {
				features.incrementCount("*SPECIAL-" + m.group(), 1.0);
			}
			getPrefixSuffix(name, features);
			return features;
		}

		private void getPrefixSuffix(String word, Counter<String> features) {
			char[] characters = word.toCharArray();
			// Prefix
			String prefixPattern = "";
			for (int i = 0; i < 6; i++) {
				if (i < characters.length) {
					char ch = characters[i];
					if (Character.isLetter(ch)) {
						prefixPattern += Character.isUpperCase(ch) ? 'X' : 'x';
					} else if (Character.isDigit(ch)) {
						prefixPattern += 'D';
					} else {
						prefixPattern += ch;
					}
				} else {
					prefixPattern += 'E';
				}
			}
			for (int i = 1; i < prefixPattern.length(); i++) {
				features.incrementCount(
						"PREFIX-PATTERN-" + prefixPattern.substring(0, i), 1.0);
			}

			// Suffix
			String suffixPattern = "";
			for (int i = characters.length - 1; i > characters.length - 5; i--) {
				if (i > 0) {
					char ch = characters[i];
					if (Character.isLetter(ch)) {
						suffixPattern += Character.isUpperCase(ch) ? 'X' : 'x';
					} else if (Character.isDigit(ch)) {
						prefixPattern += 'D';
					} else {
						suffixPattern += ch;
					}
				} else {
					suffixPattern += 'E';
				}
			}
			for (int i = 1; i < suffixPattern.length(); i++) {
				features.incrementCount(
						"SUFFIX-PATTERN-" + suffixPattern.substring(0, i), 1.0);
			}
		}
	}

	private static List<LabeledInstance<String, String>> loadData(
			String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		List<LabeledInstance<String, String>> labeledInstances = new ArrayList<LabeledInstance<String, String>>();
		while (reader.ready()) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			String label = parts[0];
			String name = parts[1];
			LabeledInstance<String, String> labeledInstance = new LabeledInstance<String, String>(
					label, name);
			labeledInstances.add(labeledInstance);
		}
		return labeledInstances;
	}

	private static void testClassifier(
			ProbabilisticClassifier<String, String> classifier,
			List<LabeledInstance<String, String>> testData, boolean verbose) {
		double numCorrect = 0.0;
		double numTotal = 0.0;
		Counter<Pair<String, String>> confusedPairCounter = new Counter<Pair<String, String>>();
		CounterMap<Long, String> confidenceCounter = new CounterMap<Long, String>();

		for (LabeledInstance<String, String> testDatum : testData) {
			String name = testDatum.getInput();
			String label = classifier.getLabel(name);
			double confidence = classifier.getProbabilities(name).getCount(
					label);
			if (label.equals(testDatum.getLabel())) {
				numCorrect += 1.0;
				confidenceCounter.incrementCount(Math.round(confidence * 10),
						"Y", 1.0);
			} else {
				confidenceCounter.incrementCount(Math.round(confidence * 10),
						"N", 1.0);
				if (verbose) {
					confusedPairCounter.incrementCount(
							new Pair<String, String>(testDatum.getLabel(),
									label), 1.0);
					// display an error
					System.err.println("Error: " + name + " guess=" + label
							+ " gold=" + testDatum.getLabel() + " confidence="
							+ confidence);
				}
			}
			numTotal += 1.0;
		}
		double accuracy = numCorrect / numTotal;
		System.out.println(String.format("#Error: %f\t#Total: %f", numTotal
				- numCorrect, numTotal));
		System.out.println("Accuracy: " + accuracy);
		if (verbose) {
			System.out.println(confusedPairCounter.toString(5));
			System.out.println(confidenceCounter.toString());
			confidenceCounter.normalize();
			for (Long key : confidenceCounter.keySet()) {
				System.out.println(String.format("%d -> %f", key,
						confidenceCounter.getCount(key, "Y")));
			}
		}
	}

	public static void main(String[] args) throws IOException {
		// Parse command line flags and arguments
		Map<String, String> argMap = CommandLineUtils
				.simpleCommandLineParser(args);

		// Set up default parameters and settings
		String basePath = ".";
		String model = "baseline";
		boolean verbose = false;
		boolean useValidation = true;

		// Update defaults using command line specifications

		// The path to the assignment data
		if (argMap.containsKey("-path")) {
			basePath = argMap.get("-path");
		}
		System.out.println("Using base path: " + basePath);

		// A string descriptor of the model to use
		if (argMap.containsKey("-model")) {
			model = argMap.get("-model");
		}
		System.out.println("Using model: " + model);

		// A string descriptor of the model to use
		if (argMap.containsKey("-test")) {
			String testString = argMap.get("-test");
			if (testString.equalsIgnoreCase("test"))
				useValidation = false;
		}
		System.out.println("Testing on: "
				+ (useValidation ? "validation" : "test"));

		// Whether or not to print the individual speech errors.
		if (argMap.containsKey("-verbose")) {
			verbose = true;
		}

		// Load training, validation, and test data
		List<LabeledInstance<String, String>> trainingData = loadData(basePath
				+ "/pnp-train.txt");
		List<LabeledInstance<String, String>> validationData = loadData(basePath
				+ "/pnp-validate.txt");
		List<LabeledInstance<String, String>> testData = loadData(basePath
				+ "/pnp-test.txt");

		// Learn a classifier
		ProbabilisticClassifier<String, String> classifier = null;
		if (model.equalsIgnoreCase("baseline")) {
			classifier = new MostFrequentLabelClassifier.Factory<String, String>()
					.trainClassifier(trainingData);
		} else if (model.equalsIgnoreCase("unigram")) {
			// construct your n-gram model here
			ProbabilisticClassifierFactory<String, String> factory = new ClassConditionClassifier.Factory(
					CharUnigramLanguageModel.class);
			classifier = factory.trainClassifier(trainingData);
		} else if (model.equalsIgnoreCase("bigram")) {
			// construct your n-gram model here
			ProbabilisticClassifierFactory<String, String> factory = new ClassConditionClassifier.Factory(
					CharBigramLanguageModel.class);
			classifier = factory.trainClassifier(trainingData);
		} else if (model.equalsIgnoreCase("maxent")) {
			// construct your maxent model here
			ProbabilisticClassifierFactory<String, String> factory = new MaximumEntropyClassifier.Factory<String, String, String>(
					1.0, 40, new ProperNameFeatureExtractor());
			classifier = factory.trainClassifier(trainingData);
		} else {
			throw new RuntimeException("Unknown model descriptor: " + model);
		}

		// Test classifier
		testClassifier(classifier, (useValidation ? validationData : testData),
				verbose);
	}
}
