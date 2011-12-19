package nlp.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nlp.ling.Tree;
import nlp.parser.BinaryTree.TraverseAction;
import nlp.parser.Grammar.GrammarBuilder;
import nlp.util.Counter;
import nlp.util.CounterMap;
import nlp.util.Pair;

public class EMGrammarTrainer implements GrammarBuilder {

	Grammar finalGrammar;
	Lexicon finalLexicon;
	List<Tree<String>> trainTrees;
	Grammar originalGrammar;

	public EMGrammarTrainer(List<Tree<String>> trainTrees) {
		this.trainTrees = trainTrees;
	}

	public void train() {
		// 1.1 Get the default grammar
		GrammarBuilder gb = new Grammar.DefaultGrammarBuilder(trainTrees, false);
		Grammar grammar = gb.buildGrammar();
		grammar.becomeFull();
		originalGrammar = grammar;
		SimpleLexicon lexicon = SimpleLexicon.createSimpleLexicon(trainTrees);
		Pair<Grammar, SimpleLexicon> pair = trainGrammar(grammar, lexicon);
		// pair = trainGrammar(pair.getFirst(), pair.getSecond());
		// only once
		finalGrammar = pair.getFirst();
		finalGrammar.becomeFull();
		// System.out.println(finalGrammar);
		finalLexicon = pair.getSecond().buildLexicon();
	}

	private Pair<Grammar, SimpleLexicon> trainGrammar(Grammar grammar,
			SimpleLexicon lexicon) {
		// 1.2 Split the initial training grammar by half all its probabilities
		// build initial even grammar and lexicon
		// System.out.println(grammar.toString());
		GrammarSpliter spliter = new GrammarSpliter(grammar, lexicon);
		grammar = spliter.getNewGrammar();
		lexicon = spliter.getNewLexicon();
		// System.out.println(lexicon.toString());

		// 1.3 Build binary tree
		List<BinaryTree<String>> binaryTrees = buildBinaryTree(trainTrees,
				spliter);

		// 2. EM training
		for (int emtrainingtimes = 0; emtrainingtimes < 12; emtrainingtimes++) {
			System.out.println("EM iteration: " + emtrainingtimes);
			GrammarTrainingHelper helper = new GrammarTrainingHelper(grammar,
					lexicon, spliter, originalGrammar);
			// loop for all trees
			helper.trainOnce(binaryTrees);
			grammar = helper.getNewGrammar();
			lexicon = helper.getNewLexicon();
		}
		return new Pair<Grammar, SimpleLexicon>(grammar, lexicon);
	}

	static class GrammarTrainingHelper {
		Grammar grammar;
		SimpleLexicon lexicon;
		GrammarSpliter spliter;
		Grammar newGrammar;
		SimpleLexicon newLexicon;
		Grammar originalGrammar;

		public GrammarTrainingHelper(Grammar grammar, SimpleLexicon lexicon,
				GrammarSpliter spliter, Grammar originalGrammar) {
			this.grammar = grammar;
			this.lexicon = lexicon;
			this.spliter = spliter;
			this.originalGrammar = originalGrammar;
		}

		public Grammar getNewGrammar() {
			return newGrammar;
		}

		public SimpleLexicon getNewLexicon() {
			return newLexicon;
		}

		public void trainOnce(List<BinaryTree<String>> binaryTrees) {
			InProbabilityComputer inComputer = new InProbabilityComputer();
			OutProbabilityComputer outComputer = new OutProbabilityComputer();
			PosteriorProbabilityCounter posteriorCounter = new PosteriorProbabilityCounter();
			// compute in probability
			for (BinaryTree<String> binaryTree : binaryTrees) {
				binaryTree.postOrdertraverse(inComputer);
			}
			// compute out probability
			for (BinaryTree<String> binaryTree : binaryTrees) {
				// binaryTree.outProbabilities
				Collections.fill(binaryTree.outProbabilities,
						1.0 / binaryTree.lableSize());
				binaryTree.preOrdertraverse(outComputer);
			}
			// compute post probability
			for (BinaryTree<String> binaryTree : binaryTrees) {
				binaryTree.preOrdertraverse(posteriorCounter);
			}
			buildNewGrammar(posteriorCounter);
			newLexicon = new SimpleLexicon(posteriorCounter.wordToTagCounters);
		}

		private void buildNewGrammar(PosteriorProbabilityCounter postCounter) {
			Counter<UnaryRule> baseUnaryCounter = new Counter<UnaryRule>();
			Counter<BinaryRule> baseBinaryCounter = new Counter<BinaryRule>();
			for (UnaryRule unaryRule : postCounter.unaryRuleCounter.keySet()) {
				baseUnaryCounter.incrementCount(getBaseRule(unaryRule),
						postCounter.unaryRuleCounter.getCount(unaryRule));
			}
			for (BinaryRule binaryRule : postCounter.binaryRuleCounter.keySet()) {
				baseBinaryCounter.incrementCount(getBaseRule(binaryRule),
						postCounter.binaryRuleCounter.getCount(binaryRule));
			}
			for (UnaryRule unaryRule : postCounter.unaryRuleCounter.keySet()) {
				UnaryRule baseRule = getBaseRule(unaryRule);
				postCounter.unaryRuleCounter.setCount(unaryRule,
						postCounter.unaryRuleCounter.getCount(unaryRule)
								/ baseUnaryCounter.getCount(baseRule)
								* originalGrammar.selfUnaryMap.get(baseRule)
										.getScore());

			}
			for (BinaryRule binaryRule : postCounter.binaryRuleCounter.keySet()) {
				BinaryRule baseRule = getBaseRule(binaryRule);
				postCounter.binaryRuleCounter.setCount(binaryRule,
						postCounter.binaryRuleCounter.getCount(binaryRule)
								/ baseBinaryCounter.getCount(baseRule)
								* originalGrammar.selfBinaryMap.get(baseRule)
										.getScore());
			}
			newGrammar = new Grammar(postCounter.unaryRuleCounter,
					postCounter.binaryRuleCounter, false);
		}

		private BinaryRule getBaseRule(BinaryRule binaryRule) {
			return new BinaryRule(GrammarSpliter.getBaseState(binaryRule
					.getParent()), GrammarSpliter.getBaseState(binaryRule
					.getLeftChild()), GrammarSpliter.getBaseState(binaryRule
					.getRightChild()));
		}

		private UnaryRule getBaseRule(UnaryRule unaryRule) {
			return new UnaryRule(GrammarSpliter.getBaseState(unaryRule
					.getParent()), GrammarSpliter.getBaseState(unaryRule
					.getChild()));
		}

		class InProbabilityComputer implements TraverseAction<String> {
			@Override
			public void act(BinaryTree<String> tree) {
				if (tree.isLeaf()) {
					return;
				}
				if (tree.isPreTerminal()) { // tag
					String word = tree.left.getBaseLabel();
					for (int i = 0; i < tree.lableSize(); i++) {
						String tag = tree.getLabel(i);
						tree.SetIn(i, lexicon.scoreTagging(word, tag));
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

		// TODO: compute smaller and smaller number
		static double factor = 1E4, lowThreshold = 1E-20;

		class PosteriorProbabilityCounter implements TraverseAction<String> {
			public Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
			public Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
			public CounterMap<String, String> wordToTagCounters = new CounterMap<String, String>();

			@Override
			public void act(BinaryTree<String> tree) {
				if (tree.isLeaf()) {
					return;
				}
				if (tree.isPreTerminal()) { // tag
					String word = tree.left.getBaseLabel();
					for (int i = 0; i < tree.lableSize(); i++) {
						String tag = tree.getLabel(i);
						double score = tree.getOut(i) * factor
								* lexicon.scoreTagging(word, tag);
						wordToTagCounters.incrementCount(word, tag, score);
					}
				} else if (tree.isUnary()) { // unary
					for (int i = 0; i < tree.lableSize(); i++) {
						BinaryTree<String> child = tree.left;
						for (int j = 0; j < child.lableSize(); j++) {
							double score = tree.getOut(i)
									* factor
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
								double score = tree.getOut(i)
										* factor
										* grammar.getBinaryScore(
												tree.getLabel(i),
												left.getLabel(j),
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
		return finalGrammar;
	}

	public Lexicon getLexicon() {
		return finalLexicon;
	}
}
