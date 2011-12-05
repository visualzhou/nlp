package nlp.parser;

public class UnaryRule {
	String parent;
	String child;
	double score;

	public String getParent() {
		return parent;
	}

	public String getChild() {
		return child;
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
		if (!(o instanceof UnaryRule))
			return false;

		final UnaryRule unaryRule = (UnaryRule) o;

		if (child != null ? !child.equals(unaryRule.child)
				: unaryRule.child != null)
			return false;
		if (parent != null ? !parent.equals(unaryRule.parent)
				: unaryRule.parent != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (parent != null ? parent.hashCode() : 0);
		result = 29 * result + (child != null ? child.hashCode() : 0);
		return result;
	}

	public String toString() {
		return parent + " -> " + child + " %% " + score;
	}

	public UnaryRule(String parent, String child) {
		this.parent = parent;
		this.child = child;
	}
}
