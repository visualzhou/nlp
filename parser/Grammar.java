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
	Map<String, List<BinaryRule>> binaryRulesByLeftChild = new HashMap<String, List<BinaryRule>>();
	Map<String, List<BinaryRule>> binaryRulesByRightChild = new HashMap<String, List<BinaryRule>>();
	Map<String, List<BinaryRule>> binaryRulesByParent = new HashMap<String, List<BinaryRule>>();
	List<BinaryRule> binaryRules = new ArrayList<BinaryRule>();
	Map<String, List<UnaryRule>> unaryRulesByChild = new HashMap<String, List<UnaryRule>>();
	Map<String, List<UnaryRule>> unaryRulesByParent = new HashMap<String, List<UnaryRule>>();
	List<UnaryRule> unaryRules = new ArrayList<UnaryRule>();
	Set<String> states = new HashSet<String>();

	public List<BinaryRule> getBinaryRulesByLeftChild(String leftChild) {
		return CollectionUtils.getValueList(binaryRulesByLeftChild, leftChild);
	}

	public List<BinaryRule> getBinaryRulesByRightChild(String rightChild) {
		return CollectionUtils
				.getValueList(binaryRulesByRightChild, rightChild);
	}

	public List<BinaryRule> getBinaryRulesByParent(String parent) {
		return CollectionUtils.getValueList(binaryRulesByParent, parent);
	}

	public List<BinaryRule> getBinaryRules() {
		return binaryRules;
	}

	public List<UnaryRule> getUnaryRulesByChild(String child) {
		return CollectionUtils.getValueList(unaryRulesByChild, child);
	}

	public List<UnaryRule> getUnaryRulesByParent(String parent) {
		return CollectionUtils.getValueList(unaryRulesByParent, parent);
	}

	public List<UnaryRule> getUnaryRules() {
		return unaryRules;
	}

	public Set<String> getStates() {
		return states;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		List<String> ruleStrings = new ArrayList<String>();
		for (String parent : binaryRulesByParent.keySet()) {
			for (BinaryRule binaryRule : getBinaryRulesByParent(parent)) {
				ruleStrings.add(binaryRule.toString());
			}
		}
		for (String parent : unaryRulesByParent.keySet()) {
			for (UnaryRule unaryRule : getUnaryRulesByParent(parent)) {
				ruleStrings.add(unaryRule.toString());
			}
		}
		for (String ruleString : CollectionUtils.sort(ruleStrings)) {
			sb.append(ruleString);
			sb.append("\n");
		}
		return sb.toString();
	}

	private void addBinary(BinaryRule binaryRule) {
		states.add(binaryRule.getParent());
		states.add(binaryRule.getLeftChild());
		states.add(binaryRule.getRightChild());
		binaryRules.add(binaryRule);
		CollectionUtils.addToValueList(binaryRulesByParent,
				binaryRule.getParent(), binaryRule);
		CollectionUtils.addToValueList(binaryRulesByLeftChild,
				binaryRule.getLeftChild(), binaryRule);
		CollectionUtils.addToValueList(binaryRulesByRightChild,
				binaryRule.getRightChild(), binaryRule);
	}

	private void addUnary(UnaryRule unaryRule) {
		states.add(unaryRule.getParent());
		states.add(unaryRule.getChild());
		unaryRules.add(unaryRule);
		CollectionUtils.addToValueList(unaryRulesByParent,
				unaryRule.getParent(), unaryRule);
		CollectionUtils.addToValueList(unaryRulesByChild, unaryRule.getChild(),
				unaryRule);
	}

	public Grammar(List<Tree<String>> trainTrees) {
		Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
		Counter<String> symbolCounter = new Counter<String>();
		for (Tree<String> trainTree : trainTrees) {
			tallyTree(trainTree, symbolCounter, unaryRuleCounter,
					binaryRuleCounter);
		}
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

	private void tallyTree(Tree<String> tree, Counter<String> symbolCounter,
			Counter<UnaryRule> unaryRuleCounter,
			Counter<BinaryRule> binaryRuleCounter) {
		if (tree.isLeaf())
			return;
		if (tree.isPreTerminal())
			return;
		if (tree.getChildren().size() == 1) {
			UnaryRule unaryRule = makeUnaryRule(tree);
			symbolCounter.incrementCount(tree.getLabel(), 1.0);
			unaryRuleCounter.incrementCount(unaryRule, 1.0);
		}
		if (tree.getChildren().size() == 2) {
			BinaryRule binaryRule = makeBinaryRule(tree);
			symbolCounter.incrementCount(tree.getLabel(), 1.0);
			binaryRuleCounter.incrementCount(binaryRule, 1.0);
		}
		if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) {
			throw new RuntimeException(
					"Attempted to construct a Grammar with an illegal tree (unbinarized?): "
							+ tree);
		}
		for (Tree<String> child : tree.getChildren()) {
			tallyTree(child, symbolCounter, unaryRuleCounter, binaryRuleCounter);
		}
	}

	private UnaryRule makeUnaryRule(Tree<String> tree) {
		return new UnaryRule(tree.getLabel(), tree.getChildren().get(0)
				.getLabel());
	}

	private BinaryRule makeBinaryRule(Tree<String> tree) {
		return new BinaryRule(tree.getLabel(), tree.getChildren().get(0)
				.getLabel(), tree.getChildren().get(1).getLabel());
	}
}