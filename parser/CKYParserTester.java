package nlp.parser;

import java.util.ArrayList;
import java.util.List;

import nlp.ling.Tree;
import nlp.parser.Grammar.GrammarBuilder;

public class CKYParserTester extends CKYParserMarkov {

	public CKYParserTester(List<Tree<String>> trainTrees) {
		super(trainTrees);
	}

	@Override
	protected void buildGrammar(List<Tree<String>> annotatedTrainTrees) {
		EMGrammarTrainer trainer = new EMGrammarTrainer(annotatedTrainTrees);
		trainer.train();
		grammar = trainer.buildGrammar();
		lexicon = trainer.getLexicon();
	}

	@Override
	protected void buildLexicon(List<Tree<String>> annotatedTrainTrees) {
		System.out.println("build lexicon ... done.");
	}

	@Override
	protected List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
		List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
		TreeAnnotations.horizontal = 0;
		TreeAnnotations.useparent = false;
		for (Tree<String> tree : trees) {
			annotatedTrees.add(TreeAnnotations.annotateTreeMarkov(tree));
		}
		return annotatedTrees;
	}

	@Override
	protected String getRoot() {
		return "ROOT";
	}

}
