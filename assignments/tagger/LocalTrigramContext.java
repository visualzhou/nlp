package nlp.assignments.tagger;

import java.util.List;

/**
 * A LocalTrigramContext is a position in a sentence, along with the
 * previous two tags -- basically a FeatureVector.
 */
public class LocalTrigramContext {
	List<String> words;
	int position;
	String previousTag;
	String previousPreviousTag;

	public List<String> getWords() {
		return words;
	}

	public String getCurrentWord() {
		return words.get(position);
	}

	public int getPosition() {
		return position;
	}

	public String getPreviousTag() {
		return previousTag;
	}

	public String getPreviousPreviousTag() {
		return previousPreviousTag;
	}

	public String toString() {
		return "[" + getPreviousPreviousTag() + ", " + getPreviousTag()
				+ ", " + getCurrentWord() + "]";
	}

	public LocalTrigramContext(List<String> words, int position,
			String previousPreviousTag, String previousTag) {
		this.words = words;
		this.position = position;
		this.previousTag = previousTag;
		this.previousPreviousTag = previousPreviousTag;
	}
}