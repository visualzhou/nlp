package nlp.parser;

public class BinaryRule {
	String parent;
	String leftChild;
	String rightChild;
	double score;

	public String getParent() {
		return parent;
	}

	public String getLeftChild() {
		return leftChild;
	}

	public String getRightChild() {
		return rightChild;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof BinaryRule))
			return false;

		final BinaryRule binaryRule = (BinaryRule) o;

		if (leftChild != null ? !leftChild.equals(binaryRule.leftChild)
				: binaryRule.leftChild != null)
			return false;
		if (parent != null ? !parent.equals(binaryRule.parent)
				: binaryRule.parent != null)
			return false;
		if (rightChild != null ? !rightChild.equals(binaryRule.rightChild)
				: binaryRule.rightChild != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (parent != null ? parent.hashCode() : 0);
		result = 29 * result + (leftChild != null ? leftChild.hashCode() : 0);
		result = 29 * result + (rightChild != null ? rightChild.hashCode() : 0);
		return result;
	}

	public String toString() {
		return parent + " -> " + leftChild + " " + rightChild + " %% " + score;
	}

	public BinaryRule(String parent, String leftChild, String rightChild) {
		this.parent = parent;
		this.leftChild = leftChild;
		this.rightChild = rightChild;
	}
}
