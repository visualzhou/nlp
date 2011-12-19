package nlp.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nlp.util.CollectionUtils;
import nlp.util.Counter;
import nlp.util.CounterMap;

public class GrammarSpliter {
	Map<String, List<String>> stateVariance;
	Grammar newGrammar;
	SimpleLexicon newLexicon;
	final static char LableMark = '^';
	Map<String, List<String>> splitMap;

	public GrammarSpliter(Grammar grammar, SimpleLexicon lexicon) {
		splitMap = splitStates(grammar.getStates());
		splitOldGrammar(grammar);
		buildGrammarVariance();
		buildSimpleLexicon(lexicon, splitMap);
	}

	public Grammar getNewGrammar() {
		return newGrammar;
	}

	public SimpleLexicon getNewLexicon() {
		return newLexicon;
	}

	public List<String> getVariance(String state) {
		return stateVariance.get(state);
	}

	public List<String> getSplitResult(String state) {
		return splitMap.get(state);
	}

	private void buildGrammarVariance() {
		stateVariance = new HashMap<String, List<String>>(98);
		for (String state : newGrammar.getStates()) {
			CollectionUtils.addToValueList(stateVariance, getBaseState(state),
					state);
		}
	}

	private void buildSimpleLexicon(SimpleLexicon lexicon,
			Map<String, List<String>> statesMap) {
		CounterMap<String, String> wordToTagCounters = new CounterMap<String, String>();
		CounterMap<String, String> oldWordToTagCounters = lexicon
				.getWordToTagCounters();

		for (String word : oldWordToTagCounters.keySet()) {
			Counter<String> vCounter = oldWordToTagCounters.getCounter(word);
			for (String oldTag : vCounter.keySet()) {
				List<String> newTagList = statesMap.get(oldTag);
				double score = vCounter.getCount(oldTag) / 2.0 * getRandom();
				for (String newTag : newTagList) {
					wordToTagCounters.incrementCount(word, newTag, score);
				}
			}
		}
		newLexicon = new SimpleLexicon(wordToTagCounters);
	}

	public static String getBaseState(String state) {
		int markIndex = state.indexOf(LableMark);
		return markIndex >= 0 ? state.substring(0, markIndex) : state;
	}

	private List<String> splitState(String state) {
		int markIndex = state.indexOf(LableMark);
		int oldLabel = 0;
		String baseState;
		if (markIndex >= 0) {
			// has been split
			oldLabel = Integer.parseInt(state.substring(markIndex + 1));
			baseState = state.substring(0, markIndex);
		} else {
			baseState = state;
		}
		if (state == "ROOT") {
			return Collections.singletonList(state);
		}
		// split two new states and add to map
		List<String> list = new ArrayList<String>(2);
		String newState = String.format("%s%c%d", baseState, LableMark,
				oldLabel * 2);
		list.add(newState);
		newState = String.format("%s%c%d", baseState, LableMark,
				oldLabel * 2 + 1);
		list.add(newState);
		return list;
	}

	/**
	 * From states before split to that after split
	 * 
	 * @param statesSet
	 * @return
	 */
	private Map<String, List<String>> splitStates(Set<String> statesSet) {
		Map<String, List<String>> statesMap = new HashMap<String, List<String>>(
				statesSet.size());
		for (String state : statesSet) {
			List<String> list = splitState(state);
			statesMap.put(state, list);
		}
		return statesMap;
	}

	private void splitOldGrammar(Grammar oldGrammar) {
		Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
		Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
		List<BinaryRule> oldBinaryRules = oldGrammar.getBinaryRules();
		List<UnaryRule> oldUnaryRules = oldGrammar.getUnaryRules();
		for (BinaryRule binaryRule : oldBinaryRules) {
			for (String newparent : splitMap.get(binaryRule.getParent())) {
				for (String newleft : splitMap.get(binaryRule.getLeftChild())) {
					for (String newright : splitMap.get(binaryRule
							.getRightChild())) {
						binaryRuleCounter.incrementCount(new BinaryRule(
								newparent, newleft, newright),
								binaryRule.getScore() / 8.0 * getRandom());
					}
				}
			}
		}
		for (UnaryRule unaryRule : oldUnaryRules) {
			for (String newparent : splitMap.get(unaryRule.getParent())) {
				for (String newchild : splitMap.get(unaryRule.getChild())) {
					// split the score
					unaryRuleCounter
							.incrementCount(new UnaryRule(newparent, newchild),
									unaryRule.getScore() / 4.0 * getRandom());
				}
			}
		}
		newGrammar = new Grammar(unaryRuleCounter, binaryRuleCounter, false);
	}

	static Random random = new Random(1);

	private double getRandom() {
		return 1.0 + (random.nextDouble() - 0.5) / 50;
	}
	// public static void main(String[] args) {
	// Counter<BinaryRule> bCounter = new Counter<BinaryRule>();
	// Counter<UnaryRule> uCounter = new Counter<UnaryRule>();
	// bCounter.incrementCount(new BinaryRule("NP^0", "VB^1", "NP^1"), 1.0);
	// uCounter.incrementCount(new UnaryRule("NP", "NPP"), 1.0);
	// Grammar grammar = new Grammar(uCounter, bCounter);
	// GrammarSpliter gs = new GrammarSpliter(grammar, lexicon);
	// System.out.println(gs.getNewGrammar());
	// System.out.println(gs.getVariance("NP"));
	// }
}
