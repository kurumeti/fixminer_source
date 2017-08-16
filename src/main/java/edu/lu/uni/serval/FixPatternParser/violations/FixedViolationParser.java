package edu.lu.uni.serval.FixPatternParser.violations;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.actions.model.Action;

import edu.lu.uni.serval.FixPatternParser.Parser;
import edu.lu.uni.serval.gumtree.GumTreeComparer;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalActionSet;
import edu.lu.uni.serval.gumtree.regroup.HierarchicalRegrouper;
import edu.lu.uni.serval.utils.FileHelper;

/**
 * Parse fix patterns with GumTree.
 * 
 * @author kui.liu
 *
 */
public class FixedViolationParser extends Parser {
	
	private File positionFile = null;
	protected String alarmTypes = "";
	
	public void setPositionFile(File positionFile) {
		this.positionFile = positionFile;
	}
	
	@Override
	public void parseFixPatterns(File prevFile, File revFile, File diffentryFile) {
	}
	
	/**
	 * Regroup GumTree results without remove the modification of variable names.
	 * 
	 * @param prevFile
	 * @param revFile
	 * @return
	 */
	protected List<HierarchicalActionSet> parseChangedSourceCodeWithGumTree2(File prevFile, File revFile) {
		List<HierarchicalActionSet> actionSets = new ArrayList<>();
		// GumTree results
		List<Action> gumTreeResults = new GumTreeComparer().compareTwoFilesWithGumTree(prevFile, revFile);
		if (gumTreeResults == null) {
			return null;
		} else if (gumTreeResults.size() == 0){
			return actionSets;
		} else {
			// Regroup GumTre results.
			List<HierarchicalActionSet> allActionSets = new HierarchicalRegrouper().regroupGumTreeResults(gumTreeResults);
			for (HierarchicalActionSet actionSet : allActionSets) {
				String astNodeType = actionSet.getAstNodeType();
				if (astNodeType.endsWith("Statement") || "FieldDeclaration".equals(astNodeType)) {
					actionSets.add(actionSet);
				}
			}
			
			return actionSets;
		}
	}

	protected List<Violation> readPositionsAndAlarmTypes() {
		List<Violation> violations = new ArrayList<>();
		String fileContent = FileHelper.readFile(positionFile);
		BufferedReader reader = null;
		reader = new BufferedReader(new StringReader(fileContent));
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				String[] positionStr = line.split(":");
				int startLine = Integer.parseInt(positionStr[0]);
				int endLine = Integer.parseInt(positionStr[1]);
				String alarmType = positionStr[2];
				
				Violation violation = new Violation(startLine, endLine, alarmType);
				violations.add(violation);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return violations;
	}

	protected String getPatchSourceCode(File prevFile, File revFile, int startLineNum, int endLineNum, int startLineNum2, int endLineNum2) {
		String buggyStatements = readSourceCode(prevFile, startLineNum, endLineNum, "-");
		String fixedStatements = readSourceCode(revFile, startLineNum2, endLineNum2, "+");
		return buggyStatements + fixedStatements;
	}
	
	protected String getPatchSourceCode(File prevFile, File revFile, int startLineNum, int endLineNum, int startLineNum2, int endLineNum2, boolean isInsert) {
		String buggyStatements = "";
		if (isInsert) {
			buggyStatements = readSourceCode(prevFile, startLineNum, endLineNum, "");
		} else {
			buggyStatements = readSourceCode(prevFile, startLineNum, endLineNum, "-");
		}
		String fixedStatements = readSourceCode(revFile, startLineNum2, endLineNum2, "+");
		return buggyStatements + fixedStatements;
	}

	protected String readSourceCode(File file, int startLineNum, int endLineNum, String type) {
		String sourceCode = "";
		String fileContent = FileHelper.readFile(file);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new StringReader(fileContent));
			String line = null;
			int lineIndex = 0;
			while ((line = reader.readLine()) != null) {
				lineIndex ++;
				if (lineIndex >= startLineNum && lineIndex <= endLineNum) {
					sourceCode += type + line + "\n";
				}
				if (lineIndex == endLineNum) break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sourceCode;
	}

	public String getAlarmTypes() {
		return alarmTypes;
	}

}
