package nlp.parser;

import java.util.Arrays;
import java.util.List;

import nlp.ling.Tree;

public class BinaryTree<L> {
	L baseLabel;
	BinaryTree<L> left, right;
	public List<Double> inProbabilities, outProbabilities;
	public List<String> labelVariance;

	public BinaryTree(L label) {
		this.baseLabel = label;
	}

	public L getBaseLabel() {
		return baseLabel;
	}

	public void setBaseLabel(L label) {
		this.baseLabel = label;
	}

	public boolean isLeaf() {
		return left == null && right == null;
	}

	public boolean isUnary() {
		return left != null && right == null;
	}

	public boolean isPreTerminal() {
		return isUnary() && left.isLeaf();
	}

	public boolean isPhrasal() {
		return !(isLeaf() || isPreTerminal());
	}

	public Double getIn(int index) {
		return inProbabilities.get(index);
	}

	public Double getOut(int index) {
		return outProbabilities.get(index);
	}

	public void SetIn(int index, double d) {
		inProbabilities.set(index, d);
	}

	public void SetOut(int index, double d) {
		outProbabilities.set(index, d);
	}

	public int lableSize() {
		return labelVariance.size();
	}

	public String getLabel(int index) {
		return labelVariance.get(index);
	}

	// public static <L> BinaryTree<L> buildBinaryTree(Tree<L> tree) {
	// if (tree == null) {
	// return null;
	// }
	// BinaryTree<L> bTree = new BinaryTree<L>(tree.getLabel());
	// if (tree.getChildren().size() > 0) {
	// bTree.left = buildBinaryTree(tree.getChildren().get(0));
	// }
	// if (tree.getChildren().size() > 1) {
	// bTree.right = buildBinaryTree(tree.getChildren().get(1));
	// }
	// if (tree.getChildren().size() > 2) {
	// System.err.println(String.format("%s: More than two children.",
	// bTree.getLabel().toString()));
	// }
	// return bTree;
	// }

	public static BinaryTree<String> buildBinaryTree(Tree<String> tree,
			GrammarSpliter spliter) {
		if (tree == null) {
			return null;
		}
		BinaryTree<String> bTree = new BinaryTree<String>(tree.getLabel());
		if (tree.getChildren().size() > 0) {
			bTree.left = buildBinaryTree(tree.getChildren().get(0), spliter);
		}
		if (tree.getChildren().size() > 1) {
			bTree.right = buildBinaryTree(tree.getChildren().get(1), spliter);
		}
		if (tree.getChildren().size() > 2) {
			System.err.println(String.format("%s: More than two children.",
					bTree.getBaseLabel()));
		}
		if (!bTree.isLeaf()) {
			bTree.labelVariance = spliter.getVariance(bTree.getBaseLabel());
			bTree.inProbabilities = Arrays
					.asList(new Double[bTree.lableSize()]);
			bTree.outProbabilities = Arrays
					.asList(new Double[bTree.lableSize()]);
		}
		return bTree;
	}

	public static interface TraverseAction<L> {
		public void act(BinaryTree<L> tree);
	}

	public void preOrdertraverse(TraverseAction<L> action) {
		action.act(this);
		if (left != null)
			left.preOrdertraverse(action);
		if (right != null)
			right.preOrdertraverse(action);
	}

	public void postOrdertraverse(TraverseAction<L> action) {
		if (left != null)
			left.postOrdertraverse(action);
		if (right != null)
			right.postOrdertraverse(action);
		action.act(this);
	}

	/**
	 * Make Rule
	 * 
	 * @param i
	 *            index for parent label
	 * @param j
	 *            index for child label
	 * @return
	 */
	public UnaryRule makeUnaryRule(int i, int j) {
		return new UnaryRule(this.getLabel(i), left.getLabel(j));
	}

	public BinaryRule makeBinaryRule(int i, int j, int k) {
		return new BinaryRule(getLabel(i), left.getLabel(j), right.getLabel(k));
	}
}
