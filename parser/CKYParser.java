package nlp.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nlp.ling.Tree;
import nlp.parser.Grammar.GrammarBuilder;

public class CKYParser implements Parser {

	Lexicon lexicon;
	Grammar grammar;
	UnaryClosure uc;

	public CKYParser(List<Tree<String>> trainTrees) {

		System.out.print("Annotating / binarizing training trees ... ");
		List<Tree<String>> annotatedTrainTrees = annotateTrees(trainTrees);
		System.out.println("done.");
		System.out.print("Building grammar ... ");
		buildGrammar(annotatedTrainTrees);
		System.out.println("done. ("
				+ grammar.getStates().size()
				+ " states, "
				+ (grammar.getBinaryRules().size() + grammar.getUnaryRules()
						.size()) + " rules)");
		uc = new UnaryClosure(grammar);
		// System.out.println(uc);
		buildLexicon(annotatedTrainTrees);
	}

	protected void buildGrammar(List<Tree<String>> annotatedTrainTrees) {
		GrammarBuilder grammarBuilder = new Grammar.DefaultGrammarBuilder(
				annotatedTrainTrees);
		grammar = grammarBuilder.buildGrammar();
	}

	protected void buildLexicon(List<Tree<String>> annotatedTrainTrees) {
		System.out.print("Setting up a CKY parser ... ");
		lexicon = Lexicon.createLexicon(annotatedTrainTrees);
		System.out.println("done.");
	}

	protected List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
		List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			annotatedTrees.add(TreeAnnotations.annotateTree(tree));
		}
		return annotatedTrees;
	}

	@Override
	public Tree<String> getBestParse(List<String> sentence) {
		Tree<String> annotatedBestParse = getBestAnnotatedParse(sentence);
		System.out.println(nlp.ling.Trees.PennTreeRenderer
				.render(annotatedBestParse));
		return TreeAnnotations.unAnnotateTree(annotatedBestParse);
	}

	protected Tree<String> getBestAnnotatedParse(List<String> sentence) {
		int len = sentence.size();
		@SuppressWarnings("unchecked")
		Map<String, Double>[][] table = (HashMap<String, Double>[][]) new HashMap[len + 1][len + 1];
		@SuppressWarnings("unchecked")
		Map<String, BackTrace>[][] back = (HashMap<String, BackTrace>[][]) new HashMap[len + 1][len + 1];
		for (int j = 1; j <= len; j++) { // loop the right end
			// init the word->tag
			table[j - 1][j] = new HashMap<String, Double>();
			back[j - 1][j] = new HashMap<String, BackTrace>();
			for (String tag : lexicon.getAllTags()) {
				double score = lexicon.scoreTagging(sentence.get(j - 1), tag);
				if (score == 0) // reduce states
					continue;
				table[j - 1][j].put(tag, score);
				// unary rules
				List<UnaryRule> urules = uc.getClosedUnaryRulesByChild(tag);
				for (UnaryRule unaryRule : urules) {
					String parent = unaryRule.getParent();
					if (parent.equals(tag)) {
						continue;
					}
					Double oldp = table[j - 1][j].get(parent);
					double p = score * unaryRule.getScore();
					if (oldp == null || oldp < p) {
						table[j - 1][j].put(parent, p);
						back[j - 1][j].put(parent,
								BackTrace.creatUnaryBack(unaryRule));
					}
				}
			}
			for (int i = j - 2; i >= 0; i--) { // loop the left end
				table[i][j] = new HashMap<String, Double>();
				back[i][j] = new HashMap<String, BackTrace>();
				for (int k = i + 1; k < j; k++) { // loop the separate position
					Map<String, Double> CMap = table[k][j];
					for (String B : table[i][k].keySet()) {
						// loop possible left child
						List<BinaryRule> bRules = grammar
								.getBinaryRulesByLeftChild(B);
						for (BinaryRule binaryRule : bRules) {
							String C = binaryRule.getRightChild();
							if (CMap.containsKey(C)) {
								String A = binaryRule.getParent();
								double score = table[i][k].get(B) * CMap.get(C)
										* binaryRule.getScore();
								Double oldScore = table[i][j].get(A);
								if (oldScore == null || oldScore < score) {
									table[i][j].put(A, score);
									back[i][j].put(A,
											BackTrace.creatBinaryBack(k, B, C));
								}
							}
						}
					}
				}
				// apply unary rules to (i, j)
				for (String A : table[i][j].keySet().toArray(new String[0])) {
					List<UnaryRule> unaryRules = uc
							.getClosedUnaryRulesByChild(A);
					double scoreA = table[i][j].get(A);
					for (UnaryRule unaryRule : unaryRules) {
						String P = unaryRule.getParent();
						if (P.equals(A)) {
							continue;
						}
						double score = scoreA * unaryRule.score;
						Double oldScoreP = table[i][j].get(P);
						if (oldScoreP == null || oldScoreP < score) {
							table[i][j].put(P, score);
							back[i][j].put(P,
									BackTrace.creatUnaryBack(unaryRule));
						}
					}
				}

			}
		}

		return buildTree(sentence, 0, len, getRoot(), back);
	}

	protected String getRoot() {
		return "S";
	}

	protected Tree<String> buildTree(List<String> sentence, int i, int j,
			String label, Map<String, BackTrace>[][] back) {
		// for a single word
		if (i == j - 1) {
			BackTrace trace = back[i][j].get(label);
			if (trace == null) {
				return new Tree<String>(label,
						Collections.singletonList(new Tree<String>(sentence
								.get(i))));
			}
			List<String> path = uc.getPath(trace.unaryRule);
			List<Tree<String>> emptyList = Collections.emptyList();
			Tree<String> leaf = new Tree<String>(sentence.get(i), emptyList);
			return buildUnaryTree(path, Collections.singletonList(leaf));
		}
		// for more than one words
		BackTrace trace = back[i][j].get(label);
		// a unary rule
		if (trace.isUnary()) {
			UnaryRule unaryRule = trace.unaryRule;
			Tree<String> leafChildrenTree = buildTree(sentence, i, j,
					unaryRule.getChild(), back);
			List<String> path = uc.getPath(unaryRule);
			Tree<String> unaryTree = buildUnaryTree(
					path.subList(0, path.size() - 1),
					Collections.singletonList(leafChildrenTree));
			return unaryTree;
		}
		// a binary rule
		Tree<String> leftTree = buildTree(sentence, i, trace.k, trace.B, back);
		Tree<String> rightTree = buildTree(sentence, trace.k, j, trace.C, back);
		ArrayList<Tree<String>> childrenList = new ArrayList<Tree<String>>(2);
		childrenList.add(leftTree);
		childrenList.add(rightTree);
		return new Tree<String>(label, childrenList);
	}

	protected <L> Tree<L> buildUnaryTree(List<L> path,
			List<Tree<L>> leafChildren) {
		List<Tree<L>> trees = leafChildren;
		for (int k = path.size() - 1; k >= 0; k--) {
			trees = Collections.singletonList(new Tree<L>(path.get(k), trees));
		}
		return trees.get(0);
	}

	protected static class BackTrace {
		public int k;
		public String B, C;
		private UnaryRule unaryRule;

		public static BackTrace creatBinaryBack(int k, String b, String c) {
			return new BackTrace(k, b, c);
		}

		public static BackTrace creatUnaryBack(UnaryRule unaryRule) {
			return new BackTrace(unaryRule);
		}

		private BackTrace(int k, String b, String c) {
			this.k = k;
			this.B = b;
			this.C = c;
		}

		private BackTrace(UnaryRule unaryRule) {
			this.unaryRule = unaryRule;
		}

		public boolean isUnary() {
			return unaryRule != null;
		}
	}
}
