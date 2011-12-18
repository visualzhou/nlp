package nlp.parser;

import java.util.ArrayList;
import java.util.List;

import nlp.ling.Tree;
import nlp.parser.BinaryTree.TraverseAction;
import nlp.parser.Grammar.GrammarBuilder;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class EMGrammarTrainer implements GrammarBuilder {

	Grammar finalGrammar;

	public void trainGrammar(List<Tree<String>> trainTrees, Grammar grammar,
			SimpleLexicon lexicon) {
		// 1.1 Get the default grammar
		GrammarBuilder gb = new Grammar.DefaultGrammarBuilder(trainTrees);
		grammar = gb.buildGrammar();
		lexicon = SimpleLexicon.createSimpleLexicon(trainTrees);

		// 1.2 Split the initial training grammar by half all its probabilities
		// build initial even grammar and lexicon
		// System.out.println(grammar.toString());
		System.out.println(lexicon.toString());
		GrammarSpliter spliter = new GrammarSpliter(grammar, lexicon);
		grammar = spliter.getNewGrammar();
		lexicon = spliter.getNewLexicon();
		System.out.println(lexicon.toString());

		// 1.3 Build binary tree
		List<BinaryTree<String>> binaryTrees = buildBinaryTree(trainTrees,
				spliter);

		// 2. EM training
		for (int emtrainingtimes = 0; emtrainingtimes < 10; emtrainingtimes++) {
			GrammarTrainingHelper helper = new GrammarTrainingHelper(grammar,
					lexicon, spliter);
			// loop for all trees
			for (BinaryTree<String> tree : binaryTrees) {
				// count posterior probability
				// helper.tallyTree(tree);
			}
		}
	}

	static class GrammarTrainingHelper {
		Grammar grammar;
		SimpleLexicon lexicon;
		GrammarSpliter spliter;

		public GrammarTrainingHelper(Grammar grammar, SimpleLexicon lexicon,
				GrammarSpliter spliter) {
			this.grammar = grammar;
			this.lexicon = lexicon;
			this.spliter = spliter;
		}

		public void tallyTree(Tree<String> tree) {
			// in out probabilities
			// always use the same tree
		}

		class InProbabilityComputer implements TraverseAction<String> {
			@Override
			public void act(BinaryTree<String> tree) {
				if (tree.isPreTerminal()) { // tag
					for (int i = 0; i < tree.labelVariance.size(); i++) {
						String tag = tree.labelVariance.get(i);
						tree.inProbabilities.set(i, lexicon.scoreTagging(
								tree.left.getBaseLabel(), tag));
					}
					return;
				}
				if (tree.isUnary()) { // unary
					for (int i = 0; i < tree.lableSize(); i++) {
						String label = tree.getLabel(i);
						double sum = 0;
						for (int j = 0; j < tree.left.lableSize(); j++) {
							String childLabel = tree.left.getLabel(j);
							sum += grammar.getUnaryScore(label, childLabel)
									* tree.left.getIn(j);
						}
						tree.SetIn(i, sum);
					}
					return;
				}
				// binary
				for (int i = 0; i < tree.lableSize(); i++) {
					String label = tree.getLabel(i);
					double sum = 0;
					for (int j = 0; j < tree.left.lableSize(); j++) {
						String leftLabel = tree.left.getLabel(j);
						for (int k = 0; k < tree.right.lableSize(); k++) {
							String rightLabel = tree.right.getLabel(k);
							sum += grammar.getBinaryScore(label, leftLabel,
									rightLabel)
									* tree.left.getIn(j)
									* tree.right.getIn(k);
						}
					}
					tree.SetIn(i, sum);
				}
			}
		}

		class OutProbabilityComputer implements TraverseAction<String> {

			@Override
			public void act(BinaryTree<String> tree) {
				// set for children, leaf doesn't need
				if (tree.isLeaf() || tree.isPreTerminal()) {
					return;
				}
				if (tree.isUnary()) { // unary
					BinaryTree<String> child = tree.left;
					for (int j = 0; j < child.lableSize(); j++) {
						double sum = 0;
						for (int i = 0; i < tree.lableSize(); i++) {
							sum += grammar.getUnaryScore(tree.getLabel(i),
									child.getLabel(j)) * tree.getOut(i);
						}
						child.SetOut(j, sum);
					}
					return;
				}
				// binary
				BinaryTree<String> left = tree.left, right = tree.right;
				for (int j = 0; j < left.lableSize(); j++) {
					double sum = 0;
					for (int i = 0; i < tree.lableSize(); i++) {
						for (int k = 0; k < right.lableSize(); k++) {
							sum += grammar.getBinaryScore(tree.getLabel(i),
									left.getLabel(j), right.getLabel(k))
									* tree.getOut(i) * right.getIn(k);
						}
					}
					left.SetOut(j, sum);
				}
				for (int k = 0; k < right.lableSize(); k++) {
					double sum = 0;
					for (int i = 0; i < tree.lableSize(); i++) {
						for (int j = 0; j < left.lableSize(); j++) {
							sum += grammar.getBinaryScore(tree.getLabel(i),
									left.getLabel(j), right.getLabel(k))
									* tree.getOut(i) * left.getIn(j);
						}
					}
					right.SetOut(k, sum);
				}
			}
		}

		class PosteriorProbabilityCounter implements TraverseAction<String> {
			Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
			Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
			CounterMap<String, String> wordToTagCounters = new CounterMap<String, String>();

			@Override
			public void act(BinaryTree<String> tree) {
				if (tree.isLeaf()) {
					return;
				}
				if (tree.isPreTerminal()) { // tag
					String word = tree.left.getBaseLabel();
					for (int i = 0; i < tree.lableSize(); i++) {
						String tag = tree.getLabel(i);
						double score = tree.getOut(i)
								* lexicon.scoreTagging(word, tag);
						wordToTagCounters.incrementCount(word, tag, score);
					}
				} else if (tree.isUnary()) { // unary
					for (int i = 0; i < tree.lableSize(); i++) {
						BinaryTree<String> child = tree.left;
						for (int j = 0; j < child.lableSize(); j++) {
							double score = tree.getOut(i)
									* grammar.getUnaryScore(tree.getLabel(i),
											child.getLabel(j)) * child.getIn(j);
							unaryRuleCounter.incrementCount(
									tree.makeUnaryRule(i, j), score);
						}
					}
					return;
				} else { // binary
					for (int i = 0; i < tree.lableSize(); i++) {
						BinaryTree<String> left = tree.left, right = tree.right;
						for (int j = 0; j < left.lableSize(); j++) {
							for (int k = 0; k < right.lableSize(); k++) {
								double score = tree.getOut(i);
								score *= grammar.getBinaryScore(
										tree.getLabel(i), left.getLabel(j),
										right.getLabel(k));
								score *= left.getIn(j) * right.getIn(k);
								binaryRuleCounter.incrementCount(
										tree.makeBinaryRule(i, j, k), score);
							}
						}
					}
				}
			}
		}
	}

	private static List<BinaryTree<String>> buildBinaryTree(
			List<Tree<String>> trainningTrees, GrammarSpliter spliter) {
		List<BinaryTree<String>> bTrees = new ArrayList<BinaryTree<String>>(
				trainningTrees.size());
		for (Tree<String> tree : trainningTrees) {
			BinaryTree<String> binaryTree = BinaryTree.buildBinaryTree(tree,
					spliter);
			bTrees.add(binaryTree);
		}
		return bTrees;
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
