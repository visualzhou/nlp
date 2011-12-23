package nlp.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import nlp.ling.Tree;
import nlp.parser.BinaryTree.TraverseAction;
import nlp.parser.Grammar.GrammarBuilder;
import nlp.util.Counter;
import nlp.util.CounterMap;
import nlp.util.Counters;
import nlp.util.Pair;

public class EMGrammarTrainer implements GrammarBuilder {

	Grammar finalGrammar;
	Lexicon finalLexicon;
	Grammar baseGrammar;
	SimpleLexicon baseLexicon;
	List<Tree<String>> trainTrees;
	public static long[] randomSeeds = new long[] { 3, 8, 1, 1 };
	static int[] EMTrainingTimes = new int[] { 11, 11, 1, 1 };

	public EMGrammarTrainer(List<Tree<String>> trainTrees) {
		this.trainTrees = trainTrees;
	}

	public void train() {
		// 1.1 Get the default grammar
		GrammarBuilder gb = new Grammar.DefaultGrammarBuilder(trainTrees, false);
		Grammar grammar = gb.buildGrammar();
		grammar.becomeFull();
		baseGrammar = grammar;
		SimpleLexicon lexicon = SimpleLexicon.createSimpleLexicon(trainTrees);
		baseLexicon = lexicon;
		Pair<Grammar, SimpleLexicon> pair = new Pair<Grammar, SimpleLexicon>(
				grammar, lexicon);
		for (int smcycle = 0; smcycle < 2; smcycle++) {
			System.out.println("SM cycle " + smcycle);
			// if (smcycle > 0 && randomSeeds[smcycle] != randomSeeds[smcycle -
			// 1])
			GrammarSpliter.random = new Random(randomSeeds[smcycle]);
			pair = trainGrammar(smcycle, pair.getFirst(), pair.getSecond());
			System.out.println("SM cycle " + smcycle + " done.\n");
		}
		// only once
		finalGrammar = pair.getFirst();
		finalGrammar.becomeFull();
		// System.out.println(finalGrammar);
		finalLexicon = pair.getSecond().buildLexicon();
	}

	private Pair<Grammar, SimpleLexicon> trainGrammar(int cycle,
			Grammar grammar, SimpleLexicon lexicon) {
		// 1.2 Split the initial training grammar by half all its probabilities
		// build initial even grammar and lexicon
		// System.out.println(grammar.toString());
		GrammarSpliter spliter = new GrammarSpliter(grammar, lexicon);
		Grammar splitGrammar = spliter.getNewGrammar();
		SimpleLexicon splitlexicon = spliter.getNewLexicon();
		// System.out.println(lexicon.toString());

		// 1.3 Build binary tree
		List<BinaryTree<String>> binaryTrees = buildBinaryTree(trainTrees,
				spliter);

		// 2. EM training
		for (int emtrainingtimes = 0; emtrainingtimes < EMTrainingTimes[cycle]; emtrainingtimes++) {
			System.out.println("EM iteration: " + emtrainingtimes);
			GrammarTrainingHelper helper = new GrammarTrainingHelper(
					splitGrammar, splitlexicon, spliter, baseGrammar,
					baseLexicon);
			// loop for all trees
			helper.trainOnce(binaryTrees);
			splitGrammar = helper.getNewGrammar();
			splitlexicon = helper.getNewLexicon();
		}

		if (EMTrainingTimes[cycle] > 0) {
			// 3. Merge
			// 3.1 measure merge loss
			System.out.print("Merging ... ");
			RelativeProbabilityComputer relativeProbabilityComputer = new RelativeProbabilityComputer();
			for (BinaryTree<String> binaryTree : binaryTrees) {
				binaryTree.preOrdertraverse(relativeProbabilityComputer);
			}
			MergeLossMeasurer mergeLossMeasurer = new MergeLossMeasurer(
					relativeProbabilityComputer.getObservationCounter());
			for (BinaryTree<String> binaryTree : binaryTrees) {
				binaryTree.preOrdertraverse(mergeLossMeasurer);
			}
			Set<String> mergeSet = mergeLossMeasurer.getMergeSet();
			splitGrammar = mergeGrammar(splitGrammar, mergeSet);
			splitlexicon = mergeLexicon(splitlexicon, mergeSet);
			System.out.println(" done.");
		}

		// smooth
		splitGrammar = smothGrammar(splitGrammar, spliter);
		return new Pair<Grammar, SimpleLexicon>(splitGrammar, splitlexicon);
	}

	private SimpleLexicon mergeLexicon(SimpleLexicon oldLexicon,
			Set<String> mergeSet) {
		CounterMap<String, String> wordToTagCounters = new CounterMap<String, String>();
		for (String word : oldLexicon.wordToTagCounters.keySet()) {
			Counter<String> vCounter = oldLexicon.wordToTagCounters
					.getCounter(word);
			for (String tag : vCounter.keySet()) {
				String original = GrammarSpliter.getOriginalState(tag);
				if (mergeSet.contains(original)) {
					wordToTagCounters.incrementCount(word, original,
							vCounter.getCount(tag));
				} else {
					wordToTagCounters.incrementCount(word, tag,
							vCounter.getCount(tag));
				}
			}
		}
		return new SimpleLexicon(wordToTagCounters);
	}

	private Grammar mergeGrammar(Grammar oldGrammar, Set<String> mergeSet) {
		Counter<UnaryRule> unaryCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryCounter = new Counter<BinaryRule>();
		for (UnaryRule unaryRule : oldGrammar.unaryRuleCounter.keySet()) {
			UnaryRule newRule = getOriginalRule(unaryRule, mergeSet);
			unaryCounter.incrementCount(newRule,
					oldGrammar.unaryRuleCounter.getCount(unaryRule));
		}
		for (BinaryRule binaryRule : oldGrammar.binaryRuleCounter.keySet()) {
			BinaryRule newRule = getOriginalRule(binaryRule, mergeSet);
			binaryCounter.incrementCount(newRule,
					oldGrammar.binaryRuleCounter.getCount(binaryRule));
		}
		return new Grammar(unaryCounter, binaryCounter, false);
	}

	static double smoothfactor = 0.01;

	private Grammar smothGrammar(Grammar grammar, GrammarSpliter spliter) {
		Counter<UnaryRule> unaryCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryCounter = new Counter<BinaryRule>();
		Counter<UnaryRule> oldUnaryRuleCounter = grammar.unaryRuleCounter;
		Counter<BinaryRule> oldBinaryRuleCounter = grammar.binaryRuleCounter;
		for (UnaryRule unaryRule : oldUnaryRuleCounter.keySet()) {
			UnaryRule original = GrammarTrainingHelper.getBaseRule(unaryRule);
			double d = (1 - smoothfactor)
					* oldUnaryRuleCounter.getCount(unaryRule) + smoothfactor
					* baseGrammar.unaryRuleCounter.getCount(original)
					/ spliter.getVariance(original.getParent()).size()
					/ spliter.getVariance(original.getChild()).size();
			unaryCounter.setCount(unaryRule, d);
		}
		for (BinaryRule binaryRule : oldBinaryRuleCounter.keySet()) {
			BinaryRule original = GrammarTrainingHelper.getBaseRule(binaryRule);
			double d = (1 - smoothfactor)
					* oldBinaryRuleCounter.getCount(binaryRule) + smoothfactor
					* baseGrammar.binaryRuleCounter.getCount(original)
					/ spliter.getVariance(original.getParent()).size()
					/ spliter.getVariance(original.getLeftChild()).size()
					/ spliter.getVariance(original.getRightChild()).size();
			binaryCounter.setCount(binaryRule, d);
		}
		return new Grammar(unaryCounter, binaryCounter, false);
	}

	private static UnaryRule getOriginalRule(UnaryRule unaryRule,
			Set<String> mergeSet) {
		String oParent = GrammarSpliter.getOriginalState(unaryRule.getParent());
		String oChild = GrammarSpliter.getOriginalState(unaryRule.getChild());
		String p, c;
		p = mergeSet.contains(oParent) ? oParent : unaryRule.getParent();
		c = mergeSet.contains(oChild) ? oChild : unaryRule.getChild();
		return new UnaryRule(p, c);
	}

	static BinaryRule getOriginalRule(BinaryRule binaryRule,
			Set<String> mergeSet) {
		String oParent = GrammarSpliter
				.getOriginalState(binaryRule.getParent());
		String oleft = GrammarSpliter.getOriginalState(binaryRule
				.getLeftChild());
		String oright = GrammarSpliter.getOriginalState(binaryRule
				.getRightChild());
		String p, l, r;
		p = mergeSet.contains(oParent) ? oParent : binaryRule.getParent();
		l = mergeSet.contains(oleft) ? oleft : binaryRule.getLeftChild();
		r = mergeSet.contains(oright) ? oright : binaryRule.getRightChild();
		return new BinaryRule(p, l, r);
	}

	static class RelativeProbabilityComputer implements TraverseAction<String> {
		private Counter<String> observationCounter = new Counter<String>();

		@Override
		public void act(BinaryTree<String> tree) {
			if (tree.isLeaf()) {
				return;
			}
			for (int i = 0; i < tree.lableSize(); i++) {
				double observation = tree.getIn(i) * tree.getOut(i);
				observationCounter
						.incrementCount(tree.getLabel(i), observation);
			}
		}

		public Counter<String> getObservationCounter() {
			return observationCounter;
		}

		public void setObservationCounter(Counter<String> observationCounter) {
			this.observationCounter = observationCounter;
		}
	}

	static class MergeLossMeasurer implements TraverseAction<String> {
		Counter<String> observationCounter;
		private Map<String, Double> lossMap = new HashMap<String, Double>();

		public MergeLossMeasurer(Counter<String> observationCounter) {
			this.observationCounter = observationCounter;
		}

		@Override
		public void act(BinaryTree<String> tree) {
			if (tree.isLeaf()) {
				return;
			}
			if (tree.getBaseLabel().equals("ROOT")) {
				return;
			}
			double sum = 0;
			for (int i = 0; i < tree.lableSize(); i++) {
				double observation = tree.getIn(i) * tree.getOut(i);
				sum += observation;
			}
			boolean[] mark = new boolean[tree.lableSize()];
			for (int i = 0; i < tree.lableSize(); i++) {
				if (mark[i]) {
					continue;
				}
				String lable1 = tree.getLabel(i), label2 = GrammarSpliter
						.getOtherLabel(lable1);
				// find label2
				int j = -1;
				for (int k = i + 1; k < tree.lableSize(); k++) {
					if (tree.getLabel(k).equals(label2)) {
						j = k;
						break;
					}
				}
				assert j >= 0;
				double newsum = sum, unsplitIn, unsplitOut, c1, c2;
				c1 = observationCounter.getCount(tree.getLabel(i));
				c2 = observationCounter.getCount(tree.getLabel(j));
				unsplitIn = (c1 * tree.getIn(i) + c2 * tree.getIn(j))
						/ (c1 + c2);
				unsplitOut = tree.getOut(i) + tree.getOut(j);
				newsum -= tree.getIn(i) * tree.getOut(i);
				newsum -= tree.getIn(j) * tree.getOut(j);
				newsum += unsplitIn * unsplitOut;
				String originalLabel = GrammarSpliter.getOriginalState(lable1);
				Double d = lossMap.get(originalLabel);
				if (newsum / sum > 2.0) {
					int p = 1;
					p = 2;
				}
				if (d == null) {
					lossMap.put(originalLabel, newsum / sum);
				} else {
					d *= newsum / sum;
					if (d.isNaN())
						d = 0.0;
					lossMap.put(originalLabel, d);
				}
				// newsum / sum < 1
				mark[j] = true;
			}
		}

		public Map<String, Double> getLossMap() {
			return lossMap;
		}

		public Set<String> getMergeSet() {
			List<Entry<String, Double>> entryList = new ArrayList<Map.Entry<String, Double>>(
					lossMap.entrySet());
			Collections.sort(entryList,
					new Comparator<Entry<String, Double>>() {
						@Override
						public int compare(Entry<String, Double> o1,
								Entry<String, Double> o2) {
							if (o1.getValue() > o2.getValue()) {
								return -1;
							} else if (o1.getValue() < o2.getValue()) {
								return 1;
							} else
								return 0;
						}
					});
			int size = entryList.size() / 2;
			Set<String> mergeSet = new HashSet<String>(size * 2);
			for (int i = 0; i < size; i++) {
				mergeSet.add(entryList.get(i).getKey());
			}
			return mergeSet;
		}
	}

	static class GrammarTrainingHelper {
		Grammar grammar;
		SimpleLexicon lexicon;
		GrammarSpliter spliter;
		Grammar newGrammar;
		SimpleLexicon newLexicon;
		Grammar unsplitGrammar;
		SimpleLexicon unsplitLexicon;

		public GrammarTrainingHelper(Grammar grammar, SimpleLexicon lexicon,
				GrammarSpliter spliter, Grammar originalGrammar,
				SimpleLexicon unsplitLexicon) {
			this.grammar = grammar;
			this.lexicon = lexicon;
			this.spliter = spliter;
			this.unsplitGrammar = originalGrammar;
			this.unsplitLexicon = unsplitLexicon;
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
			normalizeGrammar(posteriorCounter);
			posteriorCounter.binaryRuleCounter = Counters
					.cleanCounter(posteriorCounter.binaryRuleCounter);
			System.out.println("After clean binaryRuleCounter: "
					+ posteriorCounter.binaryRuleCounter.size());
			posteriorCounter.unaryRuleCounter = Counters
					.cleanCounter(posteriorCounter.unaryRuleCounter);
			System.out.println("After clean unaryRuleCounter: "
					+ posteriorCounter.unaryRuleCounter.size());
			normalizeGrammar(posteriorCounter);
			newGrammar = new Grammar(posteriorCounter.unaryRuleCounter,
					posteriorCounter.binaryRuleCounter, false);
			// checkGrammarConsistency(newGrammar);

			int beforelexiconsize = posteriorCounter.wordToTagCounters
					.totalSize();
			normalizeLexicon(posteriorCounter.wordToTagCounters);
			posteriorCounter.wordToTagCounters = Counters
					.cleanCounter(posteriorCounter.wordToTagCounters);
			System.out.println("Before clean wordToTagCounters: "
					+ beforelexiconsize + "  After: "
					+ posteriorCounter.wordToTagCounters.totalSize());
			normalizeLexicon(posteriorCounter.wordToTagCounters);
			newLexicon = new SimpleLexicon(posteriorCounter.wordToTagCounters);
		}

		private void normalizeGrammar(PosteriorProbabilityCounter postCounter) {
			Counter<UnaryRule> originalUnaryCounter = new Counter<UnaryRule>();
			Counter<BinaryRule> originalBinaryCounter = new Counter<BinaryRule>();
			for (UnaryRule unaryRule : postCounter.unaryRuleCounter.keySet()) {
				originalUnaryCounter.incrementCount(getBaseRule(unaryRule),
						postCounter.unaryRuleCounter.getCount(unaryRule));
			}
			for (BinaryRule binaryRule : postCounter.binaryRuleCounter.keySet()) {
				originalBinaryCounter.incrementCount(getBaseRule(binaryRule),
						postCounter.binaryRuleCounter.getCount(binaryRule));
			}
			for (UnaryRule unaryRule : postCounter.unaryRuleCounter.keySet()) {
				UnaryRule original = getBaseRule(unaryRule);
				postCounter.unaryRuleCounter.setCount(
						unaryRule,
						postCounter.unaryRuleCounter.getCount(unaryRule)
								* unsplitGrammar.unaryRuleCounter
										.getCount(original)
								/ originalUnaryCounter.getCount(original));

			}
			for (BinaryRule binaryRule : postCounter.binaryRuleCounter.keySet()) {
				BinaryRule originalRule = getBaseRule(binaryRule);
				postCounter.binaryRuleCounter.setCount(
						binaryRule,
						postCounter.binaryRuleCounter.getCount(binaryRule)
								* unsplitGrammar.binaryRuleCounter
										.getCount(originalRule)
								/ originalBinaryCounter.getCount(originalRule));
			}
		}

		private void normalizeLexicon(
				CounterMap<String, String> wordToTagCounters) {
			CounterMap<String, String> originalw2tCounters = new CounterMap<String, String>();
			for (String word : wordToTagCounters.keySet()) {
				Counter<String> vCounter = wordToTagCounters.getCounter(word);
				for (String tag : vCounter.keySet()) {
					originalw2tCounters.incrementCount(word,
							GrammarSpliter.getBaseState(tag),
							vCounter.getCount(tag));
				}
			}
			for (String word : wordToTagCounters.keySet()) {
				Counter<String> vCounter = wordToTagCounters.getCounter(word);
				for (String tag : vCounter.keySet()) {
					String originalTag = GrammarSpliter.getBaseState(tag);
					double score = vCounter.getCount(tag)
							* unsplitLexicon.wordToTagCounters.getCount(word,
									originalTag)
							/ originalw2tCounters.getCount(word, originalTag);
					wordToTagCounters.setCount(word, tag, score);
				}
			}
			// checkLexiconConsistency(wordToTagCounters);
		}

		private void checkLexiconConsistency(
				CounterMap<String, String> wordToTagCounters) {
			CounterMap<String, String> originalw2tCounters = new CounterMap<String, String>();
			for (String word : wordToTagCounters.keySet()) {
				Counter<String> vCounter = wordToTagCounters.getCounter(word);
				for (String tag : vCounter.keySet()) {
					originalw2tCounters.incrementCount(word,
							GrammarSpliter.getBaseState(tag),
							vCounter.getCount(tag));
				}
			}
			int match = 0, all = 0;
			for (String word : wordToTagCounters.keySet()) {
				Counter<String> vCounter = wordToTagCounters.getCounter(word);
				for (String tag : vCounter.keySet()) {
					String originalTag = GrammarSpliter.getBaseState(tag);
					double newscore = originalw2tCounters.getCount(word,
							originalTag);
					double oldscore = unsplitLexicon.wordToTagCounters
							.getCount(word, originalTag);
					if (Math.abs(newscore - oldscore) < 1E-6)
						match++;
					all++;
				}
			}
			System.out.println("New old lexcion match " + 1.0 * match / all);
		}

		private void checkGrammarConsistency(Grammar newGrammar) {
			Counter<UnaryRule> originalUnaryCounter = new Counter<UnaryRule>();
			Counter<BinaryRule> originalBinaryCounter = new Counter<BinaryRule>();
			for (UnaryRule unaryRule : newGrammar.getUnaryRules()) {
				originalUnaryCounter.incrementCount(getBaseRule(unaryRule),
						newGrammar.unaryRuleCounter.getCount(unaryRule));
			}
			for (BinaryRule binaryRule : newGrammar.getBinaryRules()) {
				originalBinaryCounter.incrementCount(getBaseRule(binaryRule),
						newGrammar.binaryRuleCounter.getCount(binaryRule));
			}
			int matchCount = 0, all = 0;
			for (UnaryRule unaryRule : unsplitGrammar.getUnaryRules()) {
				double oldscore = unsplitGrammar.unaryRuleCounter
						.getCount(unaryRule);
				double newscore = originalUnaryCounter.getCount(unaryRule);
				if (Math.abs(oldscore - newscore) < 1E-6) {
					matchCount++;
				} else {
					all = all + 1 - 1;
				}
				all++;
			}
			for (BinaryRule binaryRule : unsplitGrammar.getBinaryRules()) {
				double oldscore = unsplitGrammar.binaryRuleCounter
						.getCount(binaryRule);
				double newscore = originalBinaryCounter.getCount(binaryRule);
				if (Math.abs(oldscore - newscore) < 1E-6) {
					matchCount++;
				} else {
					all = all + 1 - 1;
				}
				all++;
			}
			System.out.println("New old grammar match " + 1.0 * matchCount
					/ all);
		}

		public static BinaryRule getBaseRule(BinaryRule binaryRule) {
			return new BinaryRule(GrammarSpliter.getBaseState(binaryRule
					.getParent()), GrammarSpliter.getBaseState(binaryRule
					.getLeftChild()), GrammarSpliter.getBaseState(binaryRule
					.getRightChild()));
		}

		public static UnaryRule getBaseRule(UnaryRule unaryRule) {
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
						tree.SetIn(i, lexicon.relativeScore(word, tag));
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
								* lexicon.relativeScore(word, tag);
						if (score == Double.NaN)
							score = 0.0;
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
							if (score == Double.NaN)
								score = 0.0;
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
								if (score == Double.NaN)
									score = 0.0;
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
