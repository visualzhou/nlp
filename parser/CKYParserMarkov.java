package nlp.parser;

import java.util.ArrayList;
import java.util.List;

import nlp.ling.Tree;

public class CKYParserMarkov extends CKYParser {

	public CKYParserMarkov(List<Tree<String>> trainTrees) {
		super(trainTrees);
	}
	
	@Override
	protected List<Tree<String>> annotateTrees(List<Tree<String>> trees) {
		List<Tree<String>> annotatedTrees = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			annotatedTrees.add(TreeAnnotations.annotateTreeMarkov(tree));
		}
		return annotatedTrees;
	}
	
	@Override
	protected String getRoot()
	{
		if (TreeAnnotations.useparent) {			
			return "S^ROOT";
		}
		else {
			return "ROOT";
		}
	}


}
