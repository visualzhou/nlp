package nlp.assignments.tagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import nlp.util.Counter;
import nlp.util.CounterMap;

/**
 * A Trellis is a graph with a start state an an end state, along with
 * successor and predecessor functions.
 */
public class Trellis<S> {
	S startState;
	S endState;
	CounterMap<S, S> forwardTransitions;
	CounterMap<S, S> backwardTransitions;

	/**
	 * Get the unique start state for this trellis.
	 */
	public S getStartState() {
		return startState;
	}

	public void setStartState(S startState) {
		this.startState = startState;
	}

	/**
	 * Get the unique end state for this trellis.
	 */
	public S getEndState() {
		return endState;
	}

	public void setStopState(S endState) {
		this.endState = endState;
	}

	/**
	 * For a given state, returns a counter over what states can be next in
	 * the markov process, along with the cost of that transition. Caution:
	 * a state not in the counter is illegal, and should be considered to
	 * have cost Double.NEGATIVE_INFINITY, but Counters score items they
	 * don't contain as 0.
	 */
	public Counter<S> getForwardTransitions(S state) {
		return forwardTransitions.getCounter(state);

	}

	/**
	 * For a given state, returns a counter over what states can precede it
	 * in the markov process, along with the cost of that transition.
	 */
	public Counter<S> getBackwardTransitions(S state) {
		return backwardTransitions.getCounter(state);
	}

	public void setTransitionCount(S start, S end, double count) {
		forwardTransitions.setCount(start, end, count);
		backwardTransitions.setCount(end, start, count);
	}

	public Trellis() {
		forwardTransitions = new CounterMap<S, S>();
		backwardTransitions = new CounterMap<S, S>();
	}
	
	  /**
     * A TrellisDecoder takes a Trellis and returns a path through that trellis
     * in which the first item is trellis.getStartState(), the last is
     * trellis.getEndState(), and each pair of states is conntected in the
     * trellis.
     */
    public static interface TrellisDecoder<S> {
        List<S> getBestPath(Trellis<S> trellis);
    }

    public static class GreedyDecoder<S> implements Trellis.TrellisDecoder<S> {
        public List<S> getBestPath(Trellis<S> trellis) {
            List<S> states = new ArrayList<S>();
            S currentState = trellis.getStartState();
            states.add(currentState);
            while (!currentState.equals(trellis.getEndState())) {
                Counter<S> transitions = trellis
                        .getForwardTransitions(currentState);
                S nextState = transitions.argMax();
                states.add(nextState);
                currentState = nextState;
            }
            return states;
        }
    }

    public static class ViterbiDecoder<S> implements Trellis.TrellisDecoder<S> {
        public List<S> getBestPath(Trellis<S> trellis) {
            // initialize
            Map<S, S> brackTrace = new HashMap<S, S>();
            Counter<S> optimal = new Counter<S>();
            optimal.setCount(trellis.getStartState(), 0);
            // the first log probability
            Queue<S> queue = new LinkedList<S>();
            Set<S> visitedStates = new HashSet<S>();

            // loop queue
            S currentState = trellis.getStartState();
            while (!currentState.equals(trellis.getEndState())) {
                Counter<S> transitions = trellis
                        .getForwardTransitions(currentState);
                for (S next : transitions.keySet()) {
                    double p = optimal.getCount(currentState)
                            + transitions.getCount(next);
                    if (!optimal.containsKey(next)
                            || optimal.getCount(next) < p) {
                        optimal.setCount(next, p);
                        brackTrace.put(next, currentState);
                    }
                    if (!visitedStates.contains(next)) {
                        visitedStates.add(next);
                        queue.add(next);
                    }
                }
                currentState = queue.remove();
            }
            // construct the back path
            List<S> path = new ArrayList<S>();
            while (!currentState.equals(trellis.getStartState())) {
                path.add(currentState);
                currentState = brackTrace.get(currentState);
            }
            path.add(trellis.getStartState());
            Collections.reverse(path);
            return path;
        }
    }
}