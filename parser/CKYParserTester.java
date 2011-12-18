package nlp.parser;

import java.util.ArrayList;
import java.util.List;

import nlp.ling.Tree;

public class CKYParserTester extends CKYParser {

	public CKYParserTester(List<Tree<String>> trainTrees) {
		super(trainTrees);
		System.out.print("Annotating / binarizing training trees ... ");
		List<Tree<String>> annotatedTrainTrees = annotateTrees(trainTrees);
		System.out.println("done.");
//		EMGrammarTrainer trainer = new EMGrammarTrainer();
//		trainer.trainGrammar(annotatedTrainTrees, null, null);
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
		if (TreeAnnotations.useparent) {
			return "S^ROOT";
		} else {
			return "ROOT";
		}
	}

}
