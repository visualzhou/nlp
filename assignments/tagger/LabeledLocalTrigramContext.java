package nlp.assignments.tagger;

import java.util.List;

/**
 * A LabeledLocalTrigramContext is a context plus the correct tag for that
 * position -- basically a LabeledFeatureVector
 */
public class LabeledLocalTrigramContext extends LocalTrigramContext {
	String currentTag;

	public String getCurrentTag() {
		return currentTag;
	}

	public String toString() {
		return "[" + getPreviousPreviousTag() + ", " + getPreviousTag()
				+ ", " + getCurrentWord() + "_" + getCurrentTag() + "]";
	}

	public LabeledLocalTrigramContext(List<String> words, int position,
			String previousPreviousTag, String previousTag,
			String currentTag) {
		super(words, position, previousPreviousTag, previousTag);
		this.currentTag = currentTag;
	}
}