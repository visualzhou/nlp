package nlp.parser;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import nlp.ling.Tree;
import nlp.parser.Grammar.GrammarBuilder;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class EMGrammarTrainer implements GrammarBuilder {

	Grammar finalGrammar;

	public void trainGrammar(List<Tree<String>> trainTrees) {
		// 1.1 Get the default grammar
		GrammarBuilder gb = new Grammar.DefaultGrammarBuilder(trainTrees);
		Grammar grammar = gb.buildGrammar();
		SimpleLexicon lexicon = SimpleLexicon.createSimpleLexicon(trainTrees);
		// 1.2 Split the initial training grammar by half all its probabilities
		GrammarSpliter spliter = new GrammarSpliter(grammar, lexicon);
		grammar = spliter.getNewGrammar();
		lexicon = spliter.getNewLexicon();

		// 2. EM training
		for (int emtrainingtimes = 0; emtrainingtimes < 10; emtrainingtimes++) {
			GrammarTrainingHelper helper = new GrammarTrainingHelper(grammar,
					lexicon, spliter);
			// loop for all trees
			for (Tree<String> tree : trainTrees) {
				// count posterior probability
				helper.tallyTree(tree);
			}
		}
	}

	static class GrammarTrainingHelper {
		Grammar grammar;
		SimpleLexicon lexicon;
		GrammarSpliter spliter;
		Counter<UnaryRule> unaryRuleCounter;
		Counter<BinaryRule> binaryRuleCounter;
		CounterMap<String, String> wordToTagCounters;
		Counter<String> tagCounter;

		public GrammarTrainingHelper(Grammar grammar, SimpleLexicon lexicon,
				GrammarSpliter spliter) {
			this.grammar = grammar;
			this.lexicon = lexicon;
			this.spliter = spliter;
			// 1. set data structure
			// for new grammar
			unaryRuleCounter = new Counter<UnaryRule>();
			binaryRuleCounter = new Counter<BinaryRule>();
			// for lexicon
			wordToTagCounters = new CounterMap<String, String>();
			tagCounter = new Counter<String>();
		}

		public void tallyTree(Tree<String> tree) {
			// in out probabilities
			// always use the same tree
			IdentityHashMap<Tree<String>, List<InOutProbability>> identityInOutMap = new IdentityHashMap<Tree<String>, List<InOutProbability>>();
			computeIn(tree, identityInOutMap);
		}

		void computeIn(
				Tree<String> tree,
				IdentityHashMap<Tree<String>, List<InOutProbability>> identityInOutMap) {
			if (tree.isLeaf()) {
				return;
			}
			String label = tree.getLabel();

			List<String> possibleLabels = spliter.getVariance(label);
			List<InOutProbability> currentInOutPairs = new ArrayList<InOutProbability>(
					possibleLabels.size());
			// a tag, loop all possible tags
			if (tree.isPreTerminal()) {
				String word = tree.getChildren().get(0).getLabel();
				for (String tag : possibleLabels) {
					InOutProbability pair = new InOutProbability(tag);
					pair.inProbability = lexicon.scoreTagging(word, tag);
					currentInOutPairs.add(pair);
				}
				identityInOutMap.put(tree, currentInOutPairs);
				return;
			}
			// a non-terminal
			// compute children first
			for (Tree<String> child : tree.getChildren()) {
				computeIn(child, identityInOutMap);
			}
			// compute itself
			// unary rule
			if (tree.getChildren().size() == 1) {
				// children
				Tree<String> child = tree.getChildren().get(0);
				List<InOutProbability> childrenInOuts = identityInOutMap
						.get(child);
				for (int i = 0; i < possibleLabels.size(); i++) {
					String refinedLabel = possibleLabels.get(i);
					double sum = 0;
					// when split a very large grammar, the following quadratic
					// method will be slow
					// TODO: search a grammar by parent and child
					// loop children labels
					for (InOutProbability inout : childrenInOuts) {
						UnaryRule uRule = grammar.getUnaryRule(new UnaryRule(
								refinedLabel, inout.label));
						sum += uRule.score * inout.inProbability;
					}
					InOutProbability pair = new InOutProbability(refinedLabel);
					pair.inProbability = sum;
					currentInOutPairs.add(pair);
				}
				identityInOutMap.put(tree, currentInOutPairs);
			} else if (tree.getChildren().size() == 2) {
				// left
				Tree<String> left = tree.getChildren().get(0);
				List<InOutProbability> leftInOuts = identityInOutMap.get(left);
				// right
				Tree<String> right = tree.getChildren().get(1);
				List<InOutProbability> rightInOuts = identityInOutMap
						.get(right);
				for (int i = 0; i < possibleLabels.size(); i++) {
					String newLabel = possibleLabels.get(i);
					double sum = 0;
					// search a grammar by parent and children
					for (InOutProbability leftinout : leftInOuts) {
						for (InOutProbability rightinout : rightInOuts) {
							BinaryRule bRule = grammar
									.getBinaryRule(new BinaryRule(newLabel,
											leftinout.label, rightinout.label));
							sum += bRule.getScore() * leftinout.inProbability
									* rightinout.inProbability;
						}
					}
					InOutProbability pair = new InOutProbability(newLabel);
					pair.inProbability = sum;
					currentInOutPairs.add(pair);
				}
				identityInOutMap.put(tree, currentInOutPairs);
			}
		}

		void computeChildrenOut(
				Tree<String> parentTree,
				IdentityHashMap<Tree<String>, List<InOutProbability>> identityInOutMap) {
			// no need for left child
			if (parentTree.isLeaf() || parentTree.isPreTerminal()) {
				return;
			}

			String parentLabel = parentTree.getLabel();
			// unary rule
			if (parentTree.getChildren().size() == 1) {
				String childlabel = parentTree.getChildren().get(0).getLabel();
				List<InOutProbability> currentInOutList;
				for (String newLabel : spliter.getVariance(childlabel)) {
					double sum = 0;
					for (InOutProbability inout : identityInOutMap
							.get(parentTree)) {
						sum += inout.outProbability
								* grammar.getUnaryRule(
										new UnaryRule(parentLabel, childlabel))
										.getScore();
					}
					//InOutProbability = identityInOutMap.get(key)
				}
			}
		}
	}

	@Override
	public Grammar buildGrammar() {
		// TODO Auto-generated method stub
		return null;
	}

	static class InOutProbability {
		public double inProbability, outProbability;
		public final String label;

		public InOutProbability(String label) {
			this.label = label;
		}
	}

}
