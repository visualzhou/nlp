package nlp.assignments.tagger;

import java.util.List;

/**
 * Tagged sentences are a bundling of a list of words and a list of their
 * tags.
 */
public class TaggedSentence {
	List<String> words;
	List<String> tags;

	public int size() {
		return words.size();
	}

	public List<String> getWords() {
		return words;
	}

	public List<String> getTags() {
		return tags;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int position = 0; position < words.size(); position++) {
			String word = words.get(position);
			String tag = tags.get(position);
			sb.append(word);
			sb.append("_");
			sb.append(tag);
		}
		return sb.toString();
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TaggedSentence))
			return false;

		final TaggedSentence taggedSentence = (TaggedSentence) o;

		if (tags != null ? !tags.equals(taggedSentence.tags)
				: taggedSentence.tags != null)
			return false;
		if (words != null ? !words.equals(taggedSentence.words)
				: taggedSentence.words != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (words != null ? words.hashCode() : 0);
		result = 29 * result + (tags != null ? tags.hashCode() : 0);
		return result;
	}

	public TaggedSentence(List<String> words, List<String> tags) {
		this.words = words;
		this.tags = tags;
	}
}