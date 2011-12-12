package nlp.assignments;

import java.util.*;

import nlp.io.PennTreebankReader;
import nlp.ling.Tree;
import nlp.ling.Trees;
import nlp.parser.*;
import nlp.util.*;

/**
 * Harness for PCFG Parser project.
 */
public class PCFGParserTester {

	public static void main(String[] args) {
		// Parse command line flags and arguments
		Map<String, String> argMap = CommandLineUtils
				.simpleCommandLineParser(args);

		// Set up default parameters and settings
		String basePath = ".";
		boolean verbose = false;
		String testMode = "validate";
		int maxTrainLength = 1000;
		int maxTestLength = 40;

		// Update defaults using command line specifications
		if (argMap.containsKey("-path")) {
			basePath = argMap.get("-path");
			System.out.println("Using base path: " + basePath);
		}
		if (argMap.containsKey("-test")) {
			testMode = "test";
			System.out.println("Testing on final test data.");
		} else {
			System.out.println("Testing on validation data.");
		}
		if (argMap.containsKey("-maxTrainLength")) {
			maxTrainLength = Integer.parseInt(argMap.get("-maxTrainLength"));
		}
		System.out.println("Maximum length for training sentences: "
				+ maxTrainLength);
		if (argMap.containsKey("-maxTestLength")) {
			maxTestLength = Integer.parseInt(argMap.get("-maxTestLength"));
		}
		System.out.println("Maximum length for test sentences: "
				+ maxTestLength);
		if (argMap.containsKey("-verbose")) {
			verbose = true;
		}
		if (argMap.containsKey("-quiet")) {
			verbose = false;
		}

		System.out.print("Loading training trees (sections 2-21) ... ");
		List<Tree<String>> trainTrees = readTrees(basePath, 200, 2199,
				maxTrainLength);
		System.out.println("done. (" + trainTrees.size() + " trees)");
		List<Tree<String>> testTrees = null;
		if (testMode.equalsIgnoreCase("validate")) {
			System.out.print("Loading validation trees (section 22) ... ");
			testTrees = readTrees(basePath, 2200, 2299, maxTestLength);
		} else {
			System.out.print("Loading test trees (section 23) ... ");
			testTrees = readTrees(basePath, 2300, 2319, maxTestLength);
		}
		System.out.println("done. (" + testTrees.size() + " trees)");

		// Build a better parser!

		Parser parser;
		String model = argMap.get("-model");
		if (model.equalsIgnoreCase("CKY") || model.equalsIgnoreCase("CYK")) {
			parser = new nlp.parser.CKYParser(trainTrees);
		} else if (model.equalsIgnoreCase("markov")) {
			parser = new nlp.parser.CKYParserMarkov(trainTrees);

		} else {
			parser = new nlp.parser.BaselineParser(trainTrees);
		}
		testParser(parser, testTrees, verbose);
	}

	private static void testParser(Parser parser, List<Tree<String>> testTrees,
			boolean verbose) {
		EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String> eval = new EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String>(
				Collections.singleton("ROOT"),
				new HashSet<String>(Arrays.asList(new String[] { "''", "``",
						".", ":", "," })));
		for (Tree<String> testTree : testTrees) {
			List<String> testSentence = testTree.getYield();
			Tree<String> guessedTree = parser.getBestParse(testSentence);
			if (verbose) {
				System.out.println("Guess:\n"
						+ Trees.PennTreeRenderer.render(guessedTree));
				System.out.println("Gold:\n"
						+ Trees.PennTreeRenderer.render(testTree));
			}
			eval.evaluate(guessedTree, testTree);
		}
		eval.display(true);
	}

	private static List<Tree<String>> readTrees(String basePath, int low,
			int high, int maxLength) {
		Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath,
				low, high);
		// normalize trees
		Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
		List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			Tree<String> normalizedTree = treeTransformer.transformTree(tree);
			if (normalizedTree.getYield().size() > maxLength)
				continue;
			// System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
			normalizedTreeList.add(normalizedTree);
		}
		return normalizedTreeList;
	}
}
