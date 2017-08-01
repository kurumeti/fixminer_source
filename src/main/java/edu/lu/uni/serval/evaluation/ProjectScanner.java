package edu.lu.uni.serval.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.github.gumtreediff.tree.ITree;

import edu.lu.uni.serval.FixPattern.utils.ASTNodeMap;
import edu.lu.uni.serval.FixPattern.utils.Checker;
import edu.lu.uni.serval.FixPatternParser.CUCreator;
import edu.lu.uni.serval.FixPatternParser.Tokenizer;
import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.gumtree.GumTreeGenerator;
import edu.lu.uni.serval.gumtree.GumTreeGenerator.GumTreeType;
import edu.lu.uni.serval.gumtree.regroup.SimpleTree;
import edu.lu.uni.serval.gumtree.regroup.SimplifyTree;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Scan the whole java project.
 * Get the source code tokens of all statements.
 * 
 * @author kui.liu
 *
 */
public class ProjectScanner {
	
	public static void main(String[] args) {
		String inputPath = Configuration.TEST_INPUT; //test java projects
		File inputFileDirector = new File(inputPath);
		File[] projects = inputFileDirector.listFiles(); // project folders
		
		String outputLocalizeFile = Configuration.TEST_LOCALIZATION_FILE;
		String outputTokensFile = Configuration.TEST_DATA_FILE;
		
		for (File project : projects) {
			ProjectScanner scanner = new ProjectScanner();
			scanner.scanJavaProject(project.getPath(), outputLocalizeFile, outputTokensFile);
		}
	}

	List<SimpleTree> allSimpleTrees = new ArrayList<>();
	
	public void scanJavaProject(String javaProject, String outputLocalizeFile, String outputTokensFile) {
		List<File> files = new ArrayList<>();
		files.addAll(FileHelper.getAllFiles(javaProject, ".java"));
		
		StringBuilder tokensBuilder = new StringBuilder();
		StringBuilder localizationsBuilder = new StringBuilder();
		int counter = 0;
		for (File file : files) {
			if (file.getPath().toLowerCase().contains("test")) {
				continue; // ignore all test-related java files.
			}
			ITree tree = new GumTreeGenerator().generateITreeForJavaFile(file, GumTreeType.EXP_JDT);
			
			CUCreator cuCreator = new CUCreator();
			CompilationUnit cUnit = cuCreator.createCompilationUnit(file);
			getTokenVectorOfAllStatements(tree, cUnit, tokensBuilder, localizationsBuilder, javaProject, file.getPath());
		
			if (++ counter % 1000 == 0) {
				FileHelper.outputToFile(outputLocalizeFile, localizationsBuilder, true);
				FileHelper.outputToFile(outputTokensFile, tokensBuilder, true);
				localizationsBuilder.setLength(0);
				tokensBuilder.setLength(0);
			}
		}
		
		FileHelper.outputToFile(outputLocalizeFile, localizationsBuilder, true);
		FileHelper.outputToFile(outputTokensFile, tokensBuilder, true);
		localizationsBuilder.setLength(0);
		tokensBuilder.setLength(0);
	}
	
	public void getTokenVectorOfAllStatements(ITree tree, CompilationUnit unit, StringBuilder tokensBuilder, StringBuilder localizationsBuilder, String projectName, String filePath) {
		String astNodeType = ASTNodeMap.map.get(tree.getType()); //ignore: SwitchCase, SuperConstructorInvocation, ConstructorInvocation
		if ((astNodeType.endsWith("Statement") && !astNodeType.equals("TypeDeclarationStatement"))
				|| astNodeType.equals("FieldDeclaration")) {
			List<ITree> children = new ArrayList<>();
			if (Checker.containsBodyBlock(astNodeType)) {
				List<ITree> childrenList = tree.getChildren();
				for (ITree child : childrenList) {
					if (!child.getLabel().endsWith("Body")) {
						children.add(child);
					}
				}
				tree.setChildren(children);
			} else {
				children.addAll(tree.getChildren());
			}
			
			if (children.size() > 0) {
				SimplifyTree simplifier = new SimplifyTree();
				SimpleTree simpleTree = simplifier.canonicalizeSourceCodeTree(tree, null);
				// project name: file name: line number
				String tokens = Tokenizer.getTokensDeepFirst(simpleTree).trim();
				String[] tokensArray = tokens.split(" ");
				if (tokensArray.length <= Configuration.TOKEN_VECTOR_SIZE) {
					int position = tree.getPos();
					int lineNum = unit.getLineNumber(position);
					tokensBuilder.append(tokens).append("\n");
					localizationsBuilder.append(projectName + ":" + filePath + "LineNum:" + lineNum + "\n"); //project name: file name: line number
				}
			}
		} else {
			List<ITree> children = tree.getChildren();
			for (ITree child : children) {
				if (astNodeType.endsWith("Name")) continue;
				if (Checker.isExpressionType(astNodeType) && !"LambdaExpression".equals(astNodeType)) continue;
				
				getSimpleTreesOfAllStatements(child);
			}
			
		}
	}
	
	public void getSimpleTreesOfAllStatements(ITree tree) {
		String astNodeType = ASTNodeMap.map.get(tree.getType()); //ignore: SwitchCase, SuperConstructorInvocation, ConstructorInvocation
		if ((astNodeType.endsWith("Statement") && !astNodeType.equals("TypeDeclarationStatement"))
				|| astNodeType.equals("FieldDeclaration")) {
			SimpleTree simpleTree = new SimpleTree();
			List<SimpleTree> children = getChildren(tree, astNodeType, simpleTree);
			if (children != null) { // Ignore LabeledStatements and TryStatements
				simpleTree.setNodeType(astNodeType);
				simpleTree.setLabel(astNodeType);
				simpleTree.setParent(null);
				simpleTree.setChildren(children);
				allSimpleTrees.add(simpleTree);
				
			}
		} else {
			List<ITree> children = tree.getChildren();
			for (ITree child : children) {
				getSimpleTreesOfAllStatements(child);
			}
		}
	}

	private List<SimpleTree> getChildren(ITree tree, String astNodeType, SimpleTree parent) {
		List<ITree> children = new ArrayList<>();
		if (Checker.containsBodyBlock(astNodeType)) {
			List<ITree> childrenList = tree.getChildren();
			for (ITree child : childrenList) {
				if (!child.getLabel().endsWith("Body")) {
					children.add(child);
				}
			}
		} else {
			children.addAll(tree.getChildren());
		}
		
		if (children.size() == 0) {
			return null;
		}
		
		List<SimpleTree> childrenSimpleTrees = new ArrayList<>();
		for (ITree child : children) {
			childrenSimpleTrees.add(getSimpleTree(child, parent));
		}
		return childrenSimpleTrees;
	}

	private SimpleTree getSimpleTree(ITree tree, SimpleTree parent) {
		String astNodeType = ASTNodeMap.map.get(tree.getType());
		SimpleTree simpleTree = new SimpleTree();
		simpleTree.setNodeType(astNodeType);
		
		List<ITree> children = tree.getChildren();
		if (children.size() > 0) {
			List<SimpleTree> subTrees = new ArrayList<>();
			for (ITree child : children) {
				subTrees.add(getSimpleTree(child, simpleTree));
			}
			simpleTree.setChildren(subTrees);
			simpleTree.setLabel(astNodeType);
		} else {
			simpleTree.setLabel(tree.getLabel());
		}
		
		simpleTree.setParent(parent);
		return simpleTree;
	}
}
