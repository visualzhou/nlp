package nlp.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nlp.ling.Tree;
import nlp.parser.Grammar.GrammarBuilder;
import nlp.util.CounterMap;

/**
 * Baseline parser (though not a baseline I've ever seen before). Tags the
 * sentence using the baseline tagging method, then either retrieves a known
 * parse of that tag sequence, or builds a right-branching parse for unknown tag
 * sequences.
 */
public class BaselineParser implements Parser {
	CounterMap<List<String>, Tree<String>> knownParses;
	CounterMap<Integer, String> spanToCategories;
	Lexicon lexicon;

	public Tree<String> getBestParse(List<String> sentence) {
		List<String> tags = getBaselineTagging(sentence);
		Tree<String> annotatedBestParse = null;
		if (knownParses.keySet().contains(tags)) {
			annotatedBestParse = getBestKnownParse(tags);
		} else {
			annotatedBestParse = buildRightBranchParse(sentence, tags);
		}
		return TreeAnnotations.unAnnotateTree(annotatedBestParse);
	}

	private Tree<String> buildRightBranchParse(List<String> words,
			List<String> tags) {
		int currentPosition = words.size() - 1;
		Tree<String> rightBranchTree = buildTagTree(words, tags,
				currentPosition);
		while (currentPosition > 0) {
			currentPosition--;
			rightBranchTree = merge(buildTagTree(words, tags, currentPosition),
					rightBranchTree);
		}
		rightBranchTree = addRoot(rightBranchTree);
		return rightBranchTree;
	}

	private Tree<String> merge(Tree<String> leftTree, Tree<String> rightTree) {
		int span = leftTree.getYield().size() + rightTree.getYield().size();
		String mostFrequentLabel = spanToCategories.getCounter(span).argMax();
		List<Tree<String>> children = new ArrayList<Tree<String>>();
		children.add(leftTree);
		children.add(rightTree);
		return new Tree<String>(mostFrequentLabel, children);
	}

	private Tree<String> addRoot(Tree<String> tree) {
		return new Tree<String>("ROOT", Collections.singletonList(tree));
	}

	private Tree<String> buildTagTree(List<String> words, List<String> tags,
			int currentPosition) {
		Tree<String> leafTree = new Tree<String>(words.get(currentPosition));
		Tree<String> tagTree = new Tree<String>(tags.get(currentPosition),
				Collections.singletonList(leafTree));
		return tagTree;
	}

	private Tree<String> getBestKnownParse(List<String> tags) {
		return knownParses.getCounter(tags).argMax();
	}

	private List<String> getBaselineTagging(List<String> sentence) {
		List<String> tags = new ArrayList<String>();
		for (String word : sentence) {
			String tag = getBestTag(word);
			tags.add(tag);
		}
		return tags;
	}

	private String getBestTag(String word) {
		double bestScore = Double.NEGATIVE_INFINITY;
		String bestTag = null;
		for (String tag : lexicon.getAllTags()) {
			double score = lexicon.scoreTagging(word, tag);
			if (bestTag == null || score > bestScore) {
				bestScore = score;
				bestTag = tag;
			}
		}
		return bestTag;
	}

	public BaselineParser(List<Tree<String>> trainTrees) {

		System.out.print("Annotating / binarizing training trees ... ");
		List<Tree<String>> annotatedTrainTrees = annotateTrees(trainTrees);
		System.out.println("done.");

		System.out.print("Building grammar ... ");
		GrammarBuilder grammarBuilder = new Grammar.DefaultGrammarBuilder(annotatedTrainTrees);
		Grammar grammar = grammarBuilder.buildGrammar();
		System.out.println("done. (" + grammar.getStates().size() + " states)");
		UnaryClosure uc = new UnaryClosure(grammar);
		System.out.println(uc);

		System.out
				.print("Discarding grammar and setting up a baseline parser ... ");
		lexicon = Lexicon.createLexicon(annotatedTrainTrees);
		knownParses = new CounterMap<List<String>, Tree<String>>();
		spanToCategories = new CounterMap<Integer, String>();
		for (Tree<String> trainTree : annotatedTrainTrees) {
			List<String> tags = trainTree.getPreTerminalYield();
			knownParses.incrementCount(tags, trainTree, 1.0);
			tallySpans(trainTree, 0);
		}
		System.out.println("done.");
	}

	private List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
		List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			annotatedTrees.add(TreeAnnotations.annotateTree(tree));
		}
		return annotatedTrees;
	}

	private int tallySpans(Tree<String> tree, int start) {
		if (tree.isLeaf() || tree.isPreTerminal())
			return 1;
		int end = start;
		for (Tree<String> child : tree.getChildren()) {
			int childSpan = tallySpans(child, end);
			end += childSpan;
		}
		String category = tree.getLabel();
		if (!category.equals("ROOT"))
			spanToCategories.incrementCount(end - start, category, 1.0);
		return end - start;
	}
}
