package nlp.assignments.tagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nlp.assignments.tagger.Trellis.TrellisDecoder;
import nlp.util.BoundedList;
import nlp.util.Counter;
import nlp.util.Interner;

public class POSTagger {

	public static final String START_WORD = "<S>";
    public static final String STOP_WORD = "</S>";
    public static final String START_TAG = "<S>";
    public static final String STOP_TAG = "</S>";

    LocalTrigramScorer localTrigramScorer;
    TrellisDecoder<State> trellisDecoder;

    /**
     * States are pairs of tags along with a position index, representing the
     * two tags preceding that position. So, the START state, which can be
     * gotten by State.getStartState() is [START, START, 0]. To build an
     * arbitrary state, for example [DT, NN, 2], use the static factory method
     * State.buildState("DT", "NN", 2). There isnt' a single final state, since
     * sentences lengths vary, so State.getEndState(i) takes a parameter for the
     * length of the sentence.
     */
    public static class State {
    
    	int position;
        String previousTag;
        String previousPreviousTag;

        private static transient Interner<State> stateInterner = new Interner<State>(
    			new Interner.CanonicalFactory<State>() {
    				public State build(State state) {
    					return new State(state);
    				}
    			});
    
    	private static transient State tempState = new State();
    
    	public static State getStartState() {
    		return buildState(POSTagger.START_TAG, POSTagger.START_TAG, 0);
    	}
    
    	public static State getStopState(int position) {
    		return buildState(POSTagger.STOP_TAG, POSTagger.STOP_TAG, position);
    	}
    
    	public static State buildState(String previousPreviousTag,
    			String previousTag, int position) {
    		tempState.setState(previousPreviousTag, previousTag, position);
    		return stateInterner.intern(tempState);
    	}
    
    	public static List<String> toTagList(List<State> states) {
    		List<String> tags = new ArrayList<String>();
    		if (states.size() > 0) {
    			tags.add(states.get(0).getPreviousPreviousTag());
    			for (State state : states) {
    				tags.add(state.getPreviousTag());
    			}
    		}
    		return tags;
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
    
    	public State getNextState(String tag) {
    		return State.buildState(getPreviousTag(), tag, getPosition() + 1);
    	}
    
    	public State getPreviousState(String tag) {
    		return State.buildState(tag, getPreviousPreviousTag(),
    				getPosition() - 1);
    	}
    
    	public boolean equals(Object o) {
    		if (this == o)
    			return true;
    		if (!(o instanceof State))
    			return false;
    
    		final State state = (State) o;
    
    		if (position != state.position)
    			return false;
    		if (previousPreviousTag != null ? !previousPreviousTag
    				.equals(state.previousPreviousTag)
    				: state.previousPreviousTag != null)
    			return false;
    		if (previousTag != null ? !previousTag.equals(state.previousTag)
    				: state.previousTag != null)
    			return false;
    
    		return true;
    	}
    
    	public int hashCode() {
    		int result;
    		result = position;
    		result = 29 * result
    				+ (previousTag != null ? previousTag.hashCode() : 0);
    		result = 29
    				* result
    				+ (previousPreviousTag != null ? previousPreviousTag
    						.hashCode() : 0);
    		return result;
    	}
    
    	public String toString() {
    		return "[" + getPreviousPreviousTag() + ", " + getPreviousTag()
    				+ ", " + getPosition() + "]";
    	}
    
    	private void setState(String previousPreviousTag, String previousTag,
    			int position) {
    		this.previousPreviousTag = previousPreviousTag;
    		this.previousTag = previousTag;
    		this.position = position;
    	}
    
    	private State() {
    	}
    
    	private State(State state) {
    		setState(state.getPreviousPreviousTag(), state.getPreviousTag(),
    				state.getPosition());
    	}
    }

    // chop up the training instances into local contexts and pass them on
	// to the local scorer.
	public void train(List<TaggedSentence> taggedSentences) {
		localTrigramScorer
				.train(extractLabeledLocalTrigramContexts(taggedSentences));
	}

	// chop up the validation instances into local contexts and pass them on
	// to the local scorer.
	public void validate(List<TaggedSentence> taggedSentences) {
		localTrigramScorer
				.validate(extractLabeledLocalTrigramContexts(taggedSentences));
	}

	private List<LabeledLocalTrigramContext> extractLabeledLocalTrigramContexts(
			List<TaggedSentence> taggedSentences) {
		List<LabeledLocalTrigramContext> localTrigramContexts = new ArrayList<LabeledLocalTrigramContext>();
		for (TaggedSentence taggedSentence : taggedSentences) {
			localTrigramContexts
					.addAll(extractLabeledLocalTrigramContexts(taggedSentence));
		}
		return localTrigramContexts;
	}

	private List<LabeledLocalTrigramContext> extractLabeledLocalTrigramContexts(
			TaggedSentence taggedSentence) {
		List<LabeledLocalTrigramContext> labeledLocalTrigramContexts = new ArrayList<LabeledLocalTrigramContext>();
		List<String> words = new BoundedList<String>(
				taggedSentence.getWords(), POSTagger.START_WORD, POSTagger.STOP_WORD);
		List<String> tags = new BoundedList<String>(
				taggedSentence.getTags(), POSTagger.START_TAG, POSTagger.STOP_TAG);
		for (int position = 0; position <= taggedSentence.size() + 1; position++) {
			labeledLocalTrigramContexts.add(new LabeledLocalTrigramContext(
					words, position, tags.get(position - 2), tags
							.get(position - 1), tags.get(position)));
		}
		return labeledLocalTrigramContexts;
	}

	/**
	 * Builds a Trellis over a sentence, by starting at the state State, and
	 * advancing through all legal extensions of each state already in the
	 * trellis. You should not have to modify this code (or even read it,
	 * really).
	 */
	private Trellis<State> buildTrellis(List<String> sentence) {
		Trellis<State> trellis = new Trellis<State>();
		trellis.setStartState(State.getStartState());
		State stopState = State.getStopState(sentence.size() + 2);
		trellis.setStopState(stopState);
		Set<State> states = Collections.singleton(State.getStartState());
		for (int position = 0; position <= sentence.size() + 1; position++) {
			Set<State> nextStates = new HashSet<State>();
			for (State state : states) {
				if (state.equals(stopState))
					continue;
				LocalTrigramContext localTrigramContext = new LocalTrigramContext(
						sentence, position, state.getPreviousPreviousTag(),
						state.getPreviousTag());
				Counter<String> tagScores = localTrigramScorer
						.getLogScoreCounter(localTrigramContext);
				for (String tag : tagScores.keySet()) {
					double score = tagScores.getCount(tag);
					State nextState = state.getNextState(tag);
					trellis.setTransitionCount(state, nextState, score);
					nextStates.add(nextState);
				}
			}
			// System.out.println("States: "+nextStates);
			states = nextStates;
		}
		return trellis;
	}

	// to tag a sentence: build its trellis and find a path through that
	// trellis
	public List<String> tag(List<String> sentence) {
		Trellis<State> trellis = buildTrellis(sentence);
		List<State> states = trellisDecoder.getBestPath(trellis);
		List<String> tags = State.toTagList(states);
		tags = stripBoundaryTags(tags);
		return tags;
	}

	/**
	 * Scores a tagging for a sentence. Note that a tag sequence not
	 * accepted by the markov process should receive a log score of
	 * Double.NEGATIVE_INFINITY.
	 */
	public double scoreTagging(TaggedSentence taggedSentence) {
		double logScore = 0.0;
		List<LabeledLocalTrigramContext> labeledLocalTrigramContexts = extractLabeledLocalTrigramContexts(taggedSentence);
		for (LabeledLocalTrigramContext labeledLocalTrigramContext : labeledLocalTrigramContexts) {
			Counter<String> logScoreCounter = localTrigramScorer
					.getLogScoreCounter(labeledLocalTrigramContext);
			String currentTag = labeledLocalTrigramContext.getCurrentTag();
			if (logScoreCounter.containsKey(currentTag)) {
				logScore += logScoreCounter.getCount(currentTag);
			} else {
				logScore += Double.NEGATIVE_INFINITY;
			}
		}
		return logScore;
	}

	private List<String> stripBoundaryTags(List<String> tags) {
		return tags.subList(2, tags.size() - 2);
	}

	public POSTagger(LocalTrigramScorer localTrigramScorer,
			TrellisDecoder<State> trellisDecoder) {
		this.localTrigramScorer = localTrigramScorer;
		this.trellisDecoder = trellisDecoder;
	}
}