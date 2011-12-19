package nlp.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nlp.ling.Tree;
import nlp.util.CollectionUtils;
import nlp.util.Counter;

/**
 * Simple implementation of a PCFG grammar, offering the ability to look up
 * rules by their child symbols. Rule probability estimates are just relative
 * frequency estimates off of training trees.
 */
public class Grammar {
	// flag for a full grammar which supports all lookup
	boolean full;
	// rules, which is a grammar all about
	List<BinaryRule> binaryRules = new ArrayList<BinaryRule>();
	List<UnaryRule> unaryRules = new ArrayList<UnaryRule>();
	// lookup tables
	Map<String, List<BinaryRule>> binaryRulesByLeftChild = new HashMap<String, List<BinaryRule>>();
	Map<String, List<BinaryRule>> binaryRulesByParent = new HashMap<String, List<BinaryRule>>();
	Map<String, List<UnaryRule>> unaryRulesByParent = new HashMap<String, List<UnaryRule>>();
	Map<BinaryRule, BinaryRule> binaryRulesMap = new HashMap<BinaryRule, BinaryRule>();
	Map<UnaryRule, UnaryRule> unaryRulesMap = new HashMap<UnaryRule, UnaryRule>();
	// states for statistic
	Set<String> states = new HashSet<String>();

	// lookup methods
	public List<BinaryRule> getBinaryRulesByLeftChild(String leftChild) {
		return CollectionUtils.getValueList(binaryRulesByLeftChild, leftChild);
	}

	public List<BinaryRule> getBinaryRulesByParent(String parent) {
		return CollectionUtils.getValueList(binaryRulesByParent, parent);
	}

	public List<BinaryRule> getBinaryRules() {
		return binaryRules;
	}

	public List<UnaryRule> getUnaryRulesByParent(String parent) {
		return CollectionUtils.getValueList(unaryRulesByParent, parent);
	}

	public List<UnaryRule> getUnaryRules() {
		return unaryRules;
	}

	public BinaryRule getBinaryRule(BinaryRule binaryRule) {
		return binaryRulesMap.get(binaryRule);
	}

	public double getBinaryScore(String parent, String left, String right) {
		BinaryRule binaryRule = binaryRulesMap.get(new BinaryRule(parent, left,
				right));
		return binaryRule == null ? 0.0 : binaryRule.getScore();
	}

	public UnaryRule getUnaryRule(UnaryRule unaryRule) {
		return unaryRulesMap.get(unaryRule);
	}

	public double getUnaryScore(String parent, String child) {
		UnaryRule unaryRule = unaryRulesMap.get(new UnaryRule(parent, child));
		return unaryRule == null ? 0.0 : unaryRule.getScore();
	}

	public Set<String> getStates() {
		return states;
	}

	// lookup methods end

	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<String> ruleStrings = new ArrayList<String>();
		for (BinaryRule binaryRule : binaryRules)
			ruleStrings.add(binaryRule.toString());
		for (UnaryRule unaryRule : unaryRules)
			ruleStrings.add(unaryRule.toString());
		for (String ruleString : CollectionUtils.sort(ruleStrings)) {
			sb.append(ruleString);
			sb.append("\n");
		}
		return sb.toString();
	}

	// constructor helper
	private void addBinary(BinaryRule binaryRule) {
		states.add(binaryRule.getParent());
		states.add(binaryRule.getLeftChild());
		states.add(binaryRule.getRightChild());
		binaryRules.add(binaryRule);
		// optional
		if (full) {
			addBinaryLookup(binaryRule);
		}
		binaryRulesMap.put(binaryRule, binaryRule);
	}

	private void addBinaryLookup(BinaryRule binaryRule) {
		CollectionUtils.addToValueList(binaryRulesByParent,
				binaryRule.getParent(), binaryRule);
		CollectionUtils.addToValueList(binaryRulesByLeftChild,
				binaryRule.getLeftChild(), binaryRule);
	}

	private void addUnary(UnaryRule unaryRule) {
		states.add(unaryRule.getParent());
		states.add(unaryRule.getChild());
		unaryRules.add(unaryRule);
		if (full) {
			addUnaryLookup(unaryRule);
		}
		unaryRulesMap.put(unaryRule, unaryRule);
	}

	private void addUnaryLookup(UnaryRule unaryRule) {
		CollectionUtils.addToValueList(unaryRulesByParent,
				unaryRule.getParent(), unaryRule);
	}

	/**
	 * Construct a grammar with binary, unary rules. Set the probability as
	 * relative frequency.
	 * 
	 * @param unaryRuleCounter
	 *            Counter for all unary rules
	 * @param binaryRuleCounter
	 *            Counter for all binary rules
	 */
	public Grammar(Counter<UnaryRule> unaryRuleCounter,
			Counter<BinaryRule> binaryRuleCounter) {
		this(unaryRuleCounter, binaryRuleCounter, true);
	}

	public Grammar(Counter<UnaryRule> unaryRuleCounter,
			Counter<BinaryRule> binaryRuleCounter, boolean isFull) {
		full = isFull;
		// construct symbolCounter
		Counter<String> symbolCounter = new Counter<String>();
		for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
			symbolCounter.incrementCount(unaryRule.getParent(),
					unaryRuleCounter.getCount(unaryRule));
		}
		for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
			symbolCounter.incrementCount(binaryRule.getParent(),
					binaryRuleCounter.getCount(binaryRule));
		}

		// set score and build lookup table
		for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
			double unaryProbability = unaryRuleCounter.getCount(unaryRule)
					/ symbolCounter.getCount(unaryRule.getParent());
			unaryRule.setScore(unaryProbability);
			addUnary(unaryRule);
		}
		for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
			double binaryProbability = binaryRuleCounter.getCount(binaryRule)
					/ symbolCounter.getCount(binaryRule.getParent());
			binaryRule.setScore(binaryProbability);
			addBinary(binaryRule);
		}
	}

	public void becomeFull() {
		// set score and build lookup table
		for (UnaryRule unaryRule : unaryRules) {
			addUnaryLookup(unaryRule);
		}
		for (BinaryRule binaryRule : binaryRules) {
			addBinaryLookup(binaryRule);
		}

	}

	/**
	 * Use intern String in the rule
	 */
	public static UnaryRule makeUnaryRule(Tree<String> tree) {
		// return new UnaryRule(tree.getLabel().intern(), tree.getChildren()
		// .get(0).getLabel().intern());
		return new UnaryRule(tree.getLabel(), tree.getChildren().get(0)
				.getLabel());
	}

	/**
	 * Use intern String in the rule
	 */
	public static BinaryRule makeBinaryRule(Tree<String> tree) {
		// return new BinaryRule(tree.getLabel().intern(), tree.getChildren()
		// .get(0).getLabel().intern(), tree.getChildren().get(1)
		// .getLabel().intern());
		return new BinaryRule(tree.getLabel(), tree.getChildren().get(0)
				.getLabel(), tree.getChildren().get(1).getLabel());
	}

	public static interface GrammarBuilder {
		public Grammar buildGrammar();
	}

	public static class DefaultGrammarBuilder implements GrammarBuilder {
		List<Tree<String>> trainTrees;
		boolean isFull;

		public DefaultGrammarBuilder(List<Tree<String>> trainTrees,
				boolean isFull) {
			this.trainTrees = trainTrees;
			this.isFull = isFull;
		}

		public DefaultGrammarBuilder(List<Tree<String>> trainTrees) {
			this(trainTrees, true);
		}

		@Override
		public Grammar buildGrammar() {
			Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
			Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
			for (Tree<String> trainTree : trainTrees) {
				tallyTree(trainTree, unaryRuleCounter, binaryRuleCounter);
			}
			return new Grammar(unaryRuleCounter, binaryRuleCounter, isFull);
		}

		private void tallyTree(Tree<String> tree,
				Counter<UnaryRule> unaryRuleCounter,
				Counter<BinaryRule> binaryRuleCounter) {
			if (tree.isLeaf())
				return;
			if (tree.isPreTerminal())
				return;
			if (tree.getChildren().size() == 1) {
				UnaryRule unaryRule = makeUnaryRule(tree);
				unaryRuleCounter.incrementCount(unaryRule, 1.0);
			}
			if (tree.getChildren().size() == 2) {
				BinaryRule binaryRule = makeBinaryRule(tree);
				binaryRuleCounter.incrementCount(binaryRule, 1.0);
			}
			if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) {
				throw new RuntimeException(
						"Attempted to construct a Grammar with an illegal tree (unbinarized?): "
								+ tree);
			}
			for (Tree<String> child : tree.getChildren()) {
				tallyTree(child, unaryRuleCounter, binaryRuleCounter);
			}
		}
	}
}