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
 * Guarantee the non-terminal in rules are canonical representation,
 * so that the comparison with them can be faster
 *  
 * @author syzhou
 *
 */
public class SimpleGrammar {
	// rules, which is a grammar all about
	List<BinaryRule> binaryRules = new ArrayList<BinaryRule>();
	List<UnaryRule> unaryRules = new ArrayList<UnaryRule>();
	// lookup tables
	Map<BinaryRule, BinaryRule> binaryRulesMap = new HashMap<BinaryRule, BinaryRule>();
	Map<UnaryRule, UnaryRule> unaryRulesMap = new HashMap<UnaryRule, UnaryRule>();

	// states for statistic
	Set<String> states = new HashSet<String>();

	public List<BinaryRule> getBinaryRules() {
		return binaryRules;
	}

	public List<UnaryRule> getUnaryRules() {
		return unaryRules;
	}

	public Set<String> getStates() {
		return states;
	}

	public BinaryRule getBinaryRule(BinaryRule binaryRule) {
		return binaryRulesMap.get(binaryRule);
	}

	public UnaryRule getUnaryRule(UnaryRule unaryRule) {
		return unaryRulesMap.get(unaryRule);
	}

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
	}

	private void addUnary(UnaryRule unaryRule) {
		states.add(unaryRule.getParent());
		states.add(unaryRule.getChild());
		unaryRules.add(unaryRule);
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
	public SimpleGrammar(Counter<UnaryRule> unaryRuleCounter,
			Counter<BinaryRule> binaryRuleCounter) {
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

	/**
	 * Use intern String in the rule
	 */
	public static UnaryRule makeUnaryRule(Tree<String> tree) {
		return new UnaryRule(tree.getLabel().intern(), tree.getChildren()
				.get(0).getLabel().intern());
	}

	/**
	 * Use intern String in the rule
	 */
	public static BinaryRule makeBinaryRule(Tree<String> tree) {
		return new BinaryRule(tree.getLabel().intern(), tree.getChildren()
				.get(0).getLabel().intern(), tree.getChildren().get(1)
				.getLabel().intern());
	}

	public static SimpleGrammar createSimpleGrammar(
			List<Tree<String>> trainTrees) {
		Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
		for (Tree<String> trainTree : trainTrees) {
			tallyTree(trainTree, unaryRuleCounter, binaryRuleCounter);
		}
		return new SimpleGrammar(unaryRuleCounter, binaryRuleCounter);
	}

	private static void tallyTree(Tree<String> tree,
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
