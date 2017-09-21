package edu.lu.uni.serval.gumtree.regroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.ITree;

import edu.lu.uni.serval.FixPatternParser.CUCreator;
import edu.lu.uni.serval.FixPatternParser.violations.Violation;
import edu.lu.uni.serval.diffentry.DiffEntryHunk;

public class HunkActionFilter {
	
	/**
	 * Filter out the modify actions, which are not in the DiffEntry hunks, without considering the same parent node.
	 * 
	 * @param hunks
	 * @param actionSets
	 * @return
	 */
	public List<HierarchicalActionSet> filterActionsByDiffEntryHunk(List<DiffEntryHunk> hunks, 
			List<HierarchicalActionSet> actionSets, File revFile, File prevFile) {
		List<HierarchicalActionSet> uselessActions = new ArrayList<>();
		
		CUCreator cuCreator = new CUCreator();
		CompilationUnit prevUnit = cuCreator.createCompilationUnit(prevFile);
		CompilationUnit revUnit = cuCreator.createCompilationUnit(revFile);
		if (prevUnit == null || revUnit == null) {
			return uselessActions;
		}
		
		for (HierarchicalActionSet actionSet : actionSets) {
			// position of buggy statements
			int startPosition = 0;
			int endPosition = 0;
			int startLine = 0;
			int endLine = 0;
			// position of fixed statements
			int startPosition2 = 0;
			int endPosition2 = 0;
			int startLine2 = 0;
			int endLine2 = 0;
			
			String actionStr = actionSet.getActionString();
			if (actionStr.startsWith("INS")) {
				startPosition2 = actionSet.getStartPosition();
				endPosition2 = startPosition2 + actionSet.getLength();

				List<Move> firstAndLastMov = getFirstAndLastMoveAction(actionSet);
				if (firstAndLastMov != null) {
					startPosition = firstAndLastMov.get(0).getNode().getPos();
					ITree lastTree = firstAndLastMov.get(1).getNode();
					endPosition = lastTree.getPos() + lastTree.getLength();
				}
			} else {
				startPosition = actionSet.getStartPosition(); // range of actions
				endPosition = startPosition + actionSet.getLength();
				if (actionStr.startsWith("UPD")) {
					Update update = (Update) actionSet.getAction();
					ITree newNode = update.getNewNode();
					startPosition2 = newNode.getPos();
					endPosition2 = startPosition2 + newNode.getLength();
				}
			}
			startLine = startPosition == 0 ? 0 : prevUnit.getLineNumber(startPosition);
			endLine =  endPosition == 0 ? 0 : prevUnit.getLineNumber(endPosition);
			startLine2 =  startPosition2 == 0 ? 0 : revUnit.getLineNumber(startPosition2);
			endLine2 =  endPosition2 == 0 ? 0 : revUnit.getLineNumber(endPosition2);
			
			for (DiffEntryHunk hunk : hunks) {
				int bugStartLine = hunk.getBugLineStartNum();
				int bugRange = hunk.getBugRange();
				int fixStartLine = hunk.getFixLineStartNum();
				int fixRange = hunk.getFixRange();
				
				if (actionStr.startsWith("INS")) {
					if (fixStartLine + fixRange < startLine2)  {
						continue;
					} 
					if (endLine2 < fixStartLine ) {
						uselessActions.add(actionSet);
					} 
				} else {
					if (bugStartLine + bugRange < startLine)  {
						continue;
					} 
					if (endLine < bugStartLine ) {
						uselessActions.add(actionSet);
					} 
					break;
				}
			}
			actionSet.setBugStartLineNum(startLine);
			actionSet.setBugEndLineNum(endLine);
			actionSet.setFixStartLineNum(startLine2);
			actionSet.setFixEndLineNum(endLine2);
		}
		
		actionSets.removeAll(uselessActions);
		uselessActions.clear();
		return actionSets;
	}
	
	/**
	 * Filter out the modify actions, which are not in the DiffEntry hunks, with considering the same parent node.
	 * 
	 * @param hunks
	 * @param actionSets
	 * @return
	 */
	public List<HunkFixPattern> filterActionsByDiffEntryHunk2(List<DiffEntryHunk> hunks, 
			List<HierarchicalActionSet> actionSets, File revFile, File prevFile) {
		List<HunkFixPattern> allHunkFixPatterns = new ArrayList<>();
		
		CUCreator cuCreator = new CUCreator();
		CompilationUnit prevUnit = cuCreator.createCompilationUnit(prevFile);
		CompilationUnit revUnit = cuCreator.createCompilationUnit(revFile);
		if (prevUnit == null || revUnit == null) {
			return allHunkFixPatterns;
		}
		
		int i = 0;
		int size = actionSets.size();
		for (DiffEntryHunk hunk : hunks) {
			int hunkBugStartLine = hunk.getBugLineStartNum();
			int hunkBugRange = hunk.getBugRange();
			int hunkFixStartLine = hunk.getFixLineStartNum();
			int hunkFixRange = hunk.getFixRange();
			
			for (; i < size; i ++) {
				HierarchicalActionSet actionSet = actionSets.get(i);
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				int actionBugEndLine = actionSet.getBugEndLineNum();
				int actionFixStartLine = actionSet.getFixStartLineNum();
				int actionFixEndLine = actionSet.getFixEndLineNum();
				
				String actionStr = actionSet.getActionString();
				ITree previousParent = null;
				List<HierarchicalActionSet> hunkActionSets = new ArrayList<>();
				
				if (actionStr.startsWith("INS")) {
					if (hunkFixStartLine + hunkFixRange < actionFixStartLine)  {
						addHunkActionSets(hunkActionSets, allHunkFixPatterns, hunk);// save the previous non-null hunkFixPattern.
						break;
					} 
					if (actionFixEndLine >= hunkFixStartLine ) {
						ITree parent = addToHunkActionSets(actionSet, hunkActionSets, allHunkFixPatterns, previousParent, hunk);
						if (parent != null) {
							if (parent != previousParent) {
								hunkActionSets = new ArrayList<>();
							}
							hunkActionSets.add(actionSet);
						} else if (hunkActionSets.size() > 0) {
							hunkActionSets = new ArrayList<>();
						}
						previousParent = parent;
					} 
				} else { // UPD, DEL, MOV
					if (hunkBugStartLine + hunkBugRange < actionBugStartLine)  { 
						addHunkActionSets(hunkActionSets, allHunkFixPatterns, hunk);// save the previous non-null hunkFixPattern.
						break;
					} 
					if (actionBugEndLine >= hunkBugStartLine ) {
						ITree parent = addToHunkActionSets(actionSet, hunkActionSets, allHunkFixPatterns, previousParent, hunk);
						if (parent != null) { // same parent
							if (parent != previousParent) {
								hunkActionSets = new ArrayList<>();
							}
							hunkActionSets.add(actionSet);
						} else if (hunkActionSets.size() > 0) {
							hunkActionSets = new ArrayList<>();
						}
						previousParent = parent;
					} 
				}
				addHunkActionSets(hunkActionSets, allHunkFixPatterns, hunk);
			}
		}
		
		return allHunkFixPatterns;
	}
	
	private int getEndPosition(List<ITree> children) {
		int endPosition = 0;
		for (int i = 0, size = children.size(); i < size; i ++) {
			ITree child = children.get(i);
			int type = child.getType();
			if (type == 6 || type == 10 || type == 12 || type == 17 || type == 18 || type == 19 || type == 21 || type == 8// Block, EmptyStatement 
					|| type == 24 || type == 25 || type == 30 || type == 41 || type == 46 || type == 49 || type == 50 
					|| type == 51 || type == 53 || type == 54 || type == 56 || type == 60 || type == 61 || type == 70) {
				//AssertStatement, BreakStatement, CatchClause, ConstructorInvocation, ContinueStatement, DoStatement
				// ExpressionStatement, ForStatement, IfStatement, LabeledStatement, ReturnStatement, SuperConstructorInvocation
				// SwitchCase, SwitchStatement, SynchronizedStatement, ThrowStatement, TryStatement
				// TypeDeclarationStatement, VariableDeclarationStatement, WhileStatement, EnhancedForStatement
				if ( i > 0) {
					child = children.get(i - 1);
					endPosition = child.getPos() + child.getLength();
				} else {
					endPosition = child.getPos() - 1;
				}
				break;
			}
		}
		return endPosition;
	}
	
	private void addHunkActionSets(List<HierarchicalActionSet> hunkActionSets, List<HunkFixPattern> allHunkFixPatterns, DiffEntryHunk hunk) {
		if (hunkActionSets.size() > 0) {
			HunkFixPattern hunkFixPattern = new HunkFixPattern(hunk, hunkActionSets);
			allHunkFixPatterns.add(hunkFixPattern);
		}
	}

	private ITree addToHunkActionSets(HierarchicalActionSet actionSet, List<HierarchicalActionSet> hunkActionSets, 
			List<HunkFixPattern> allHunkFixPatterns, ITree previousParent, DiffEntryHunk hunk) {
		String astNodeType = actionSet.getAstNodeType();
		if ("FieldDeclaration".equals(astNodeType)) {
			addHunkActionSets(hunkActionSets, allHunkFixPatterns, hunk);
			hunkActionSets = new ArrayList<>();
			hunkActionSets.add(actionSet);
			HunkFixPattern hunkFixPattern = new HunkFixPattern(hunk, hunkActionSets);
			allHunkFixPatterns.add(hunkFixPattern);
			return null;
		} else {
			ITree currentParent = actionSet.getNode().getParent();
			if (previousParent == null) {
				previousParent = currentParent;
			} else {
				if (!previousParent.equals(currentParent)) {
					HunkFixPattern hunkFixPattern = new HunkFixPattern(hunk, hunkActionSets);
					allHunkFixPatterns.add(hunkFixPattern);
					previousParent = currentParent;
				} 
			}
			return previousParent;
		}
	}

	private List<Move> getFirstAndLastMoveAction(HierarchicalActionSet gumTreeResult) {
		List<Move> firstAndLastMoveActions = new ArrayList<>();
		List<HierarchicalActionSet> actions = new ArrayList<>();
		actions.addAll(gumTreeResult.getSubActions());
		if (actions.size() == 0) {
			return null;
		}
		Move firstMoveAction = null;
		Move lastMoveAction = null;
		while (actions.size() > 0) {
			List<HierarchicalActionSet> subActions = new ArrayList<>();
			for (HierarchicalActionSet action : actions) {
				subActions.addAll(action.getSubActions());
				if (action.toString().startsWith("MOV")) {
					if (firstMoveAction == null) {
						firstMoveAction = (Move) action.getAction();
						lastMoveAction = (Move) action.getAction();
					} else {
						int startPosition = action.getStartPosition();
						int length = action.getLength();
						int startPositionFirst = firstMoveAction.getPosition();
						int startPositionLast = lastMoveAction.getPosition();
						int lengthLast = lastMoveAction.getNode().getLength();
						if (startPosition < startPositionFirst || (startPosition == startPositionFirst && length > firstMoveAction.getLength())) {
							firstMoveAction = (Move) action.getAction();
						}
						if ((startPosition + length) > (startPositionLast + lengthLast)) {
							lastMoveAction = (Move) action.getAction();
						} 
					}
				}
			}
			
			actions.clear();
			actions.addAll(subActions);
		}
		if (firstMoveAction == null) {
			return null;
		}
		firstAndLastMoveActions.add(firstMoveAction);
		firstAndLastMoveActions.add(lastMoveAction);
		return firstAndLastMoveActions;
	}

	/**
	 * Filter out the modify actions, which are not in the DiffEntry hunks, without considering the same parent node.
	 * 
	 * @param violations
	 * @param actionSets
	 * @param revFile
	 * @param prevFile
	 * @return
	 */
	public List<Violation> filterActionsByModifiedRange(List<Violation> violations,
			List<HierarchicalActionSet> actionSets, File revFile, File prevFile) {
		
		List<Violation> selectedViolations = new ArrayList<>();
		
		CUCreator cuCreator = new CUCreator();
		CompilationUnit prevUnit = cuCreator.createCompilationUnit(prevFile);
		CompilationUnit revUnit = cuCreator.createCompilationUnit(revFile);
		if (prevUnit == null || revUnit == null) {
			return selectedViolations;
		}
		
		for (Violation violation : violations) {
			int startLine = violation.getStartLineNum();
			int endLine = violation.getEndLineNum();
			int bugStartLine = violation.getBugStartLineNum();
			int bugEndLine = violation.getBugEndLineNum();
			int fixStartLine = violation.getFixStartLineNum();
			int fixEndLine = violation.getFixEndLineNum();
			
			for (HierarchicalActionSet actionSet : actionSets) {
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				int actionBugEndLine = actionSet.getBugEndLineNum();
				int actionFixStartLine = actionSet.getFixStartLineNum();
				int actionFixEndLine = actionSet.getFixEndLineNum();

				String actionStr = actionSet.getActionString();
				if (actionStr.startsWith("INS")) {
					if (fixStartLine <= actionFixStartLine && actionFixEndLine <= fixEndLine) {
						if (actionBugStartLine != 0) {
							if (startLine <= actionBugEndLine && endLine >= actionBugStartLine) {
								violation.getActionSets().add(actionSet);
							}
						} else {
							
							violation.getActionSets().add(actionSet);
						}
					}
				} else {
//					if (bugEndLine < actionBugStartLine)  {
//						break;
//					}
					if (bugStartLine <= actionBugStartLine && actionBugEndLine <= bugEndLine) {
						if (startLine <= actionBugEndLine && endLine >= actionBugStartLine) {
							violation.getActionSets().add(actionSet);
						}
					}
				}
			}
			
			if (violation.getActionSets().size() > 0) {
				selectedViolations.add(violation);
			}
		}
		return selectedViolations;
	}
	
	/**
	 * Filter out the modify actions, which are not in the DiffEntry hunks, with considering the same parent node.
	 * 
	 * @param violations
	 * @param actionSets
	 * @param revFile
	 * @param prevFile
	 * @return
	 */
	public List<Violation> filterActionsByModifiedRange2(List<Violation> violations,
			List<HierarchicalActionSet> actionSets, File revFile, File prevFile) {
		
		List<Violation> selectedViolations = new ArrayList<>();
		
		CUCreator cuCreator = new CUCreator();
		CompilationUnit prevUnit = cuCreator.createCompilationUnit(prevFile);
		CompilationUnit revUnit = cuCreator.createCompilationUnit(revFile);
		if (prevUnit == null || revUnit == null) {
			for (Violation violation : violations) {
				this.unfixedViolations += "#NullMatchedGumTreeResult:"  + revFile.getName() + ":" +violation.getStartLineNum() + ":" + 
						violation.getEndLineNum() + ":" + violation.getViolationType() + "\n";
				System.err.println("#NullMatchedGumTreeResult:"  + revFile.getName() + ":" +violation.getStartLineNum() + ":" + 
						violation.getEndLineNum() + ":" + violation.getViolationType());
			}
			return selectedViolations;
		}
		
		for (Violation violation : violations) {
			int violationStartLine = violation.getStartLineNum();
			int violationEndLine = violation.getEndLineNum();
			int bugHunkStartLine = violation.getBugStartLineNum();
			if (bugHunkStartLine == 0) {// Null source code matched for this violation.
//				String type = getType(violation);
//				continue;
			} else if (bugHunkStartLine == -1) {//
				specialVioaltionTypes(violation, actionSets, prevUnit, revUnit);
//				continue;
			} else {
				int bugHunkEndLine = violation.getBugEndLineNum();
				int fixHunkStartLine = violation.getFixStartLineNum();
				int fixHunkEndLine = violation.getFixEndLineNum();
				
				for (HierarchicalActionSet actionSet : actionSets) {
					int actionBugStartLine = actionSet.getBugStartLineNum();
					if (actionBugStartLine == 0) {
						actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
					} 
					int actionBugEndLine = actionSet.getBugEndLineNum();
					int actionFixStartLine = actionSet.getFixStartLineNum();
					int actionFixEndLine = actionSet.getFixEndLineNum();
					
					String actionStr = actionSet.getActionString();
					if (actionStr.startsWith("INS")) {
						if (fixHunkStartLine <= actionFixStartLine && actionFixEndLine <= fixHunkEndLine) {
							if (actionBugStartLine != 0) {
								if (violationStartLine <= actionBugEndLine && violationEndLine >= actionBugStartLine) {
									violation.getActionSets().add(actionSet);
									continue;
								}
							}
							
							// INS with MOV actions that are not identified in previous IF predicate, and pure INS actions
							if (violation.getBugFixStartLineNum() >= actionFixEndLine && actionFixStartLine <= violation.getBugFixEndLineNum()) {
								violation.getActionSets().add(actionSet);
							}
						}
					} else {
						if (bugHunkStartLine <= actionBugStartLine && violationEndLine <= bugHunkEndLine) {
							if (violationStartLine <= actionBugEndLine && violationEndLine >= actionBugStartLine) {
								violation.getActionSets().add(actionSet);
							}
						}
					}
				}
			}
			
			if (violation.getActionSets().size() > 0) {
				// FieldDeclaration: single insert, move, delete or update.
//				List<HierarchicalActionSet> matchedActionSets = filterActionSets(violation.getActionSets());
//				violation.getActionSets().clear();
//				violation.getActionSets().addAll(matchedActionSets);
				selectedViolations.add(violation);
			} else {
				this.unfixedViolations += "#NullMatchedGumTreeResult:"  + revFile.getName() + ":" +violation.getStartLineNum() + ":" + 
						violation.getEndLineNum() + ":" + violation.getViolationType() + "\n";
				System.err.println("#NullMatchedGumTreeResult:"  + revFile.getName() + ":" +violation.getStartLineNum() + ":" + 
						violation.getEndLineNum() + ":" + violation.getViolationType());
			}
		}
		return selectedViolations;
	}
	public String unfixedViolations = "";
	
	private void specialVioaltionTypes(Violation violation, List<HierarchicalActionSet> actionSets, CompilationUnit prevUnit, CompilationUnit revUnit) {
		String type = violation.getViolationType();
		if ("SE_NO_SUITABLE_CONSTRUCTOR".equals(type)) {// 158, 47
			for (HierarchicalActionSet actionSet : actionSets) {
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				if (actionSet.getActionString().startsWith("UPD TypeDeclaration@@")) {
					violation.getActionSets().add(actionSet);
					break;
				}
			}
		} else if ("CN_IDIOM".equals(type)) { // 202 23
			//add clone method. or update clone method
			for (HierarchicalActionSet actionSet : actionSets) {
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				if (actionSet.getActionString().startsWith("INS MethodDeclaration@@clone")) {
//						|| actionSet.getActionString().startsWith("UPD MethodDeclaration@@clone")) {
					violation.getActionSets().add(actionSet);
					break;
				}
			}
		} else if ("SE_NO_SERIALVERSIONID".equals(type)) {// 12 1960
			// change superclass or interface, add field or remove @SuppressWarnings("serial"),   some are inner class
			for (HierarchicalActionSet actionSet : actionSets) {
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				if (actionSet.getActionString().startsWith("UPD TypeDeclaration@")) {
					violation.getActionSets().add(actionSet);
				}
				if (actionSet.getActionString().startsWith("INS FieldDeclaration@") && actionSet.getNode().getLabel().contains("serialVersionUID")) {
					violation.getActionSets().add(actionSet);
					break;
				}
			}
		} else if ("SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION".equals(type)) { // 175 34
			// constructor
			for (HierarchicalActionSet actionSet : actionSets) { 
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				if (actionSet.getActionString().startsWith("UPD MethodDeclaration@@")) {
					violation.getActionSets().add(actionSet);
					break;
				}
			}
		} else if ("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE".equals(type)) { // 89 148
			//class, and add a field se...?
			for (HierarchicalActionSet actionSet : actionSets) {
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				if (actionSet.getActionString().startsWith("UPD TypeDeclaration@")) {
					violation.getActionSets().add(actionSet);
				}
				if (actionSet.getActionString().startsWith("INS FieldDeclaration@") && actionSet.getNode().getLabel().contains("serialVersionUID")) {
					violation.getActionSets().add(actionSet);
					break;
				}
			}
		}
	}

	private List<HierarchicalActionSet> filterActionSets(List<HierarchicalActionSet> actionSets) {
		List<HierarchicalActionSet> matchedActionSets = new ArrayList<>();
		// find the update
		HierarchicalActionSet updateActionSet = null;
		for (HierarchicalActionSet actionSet : actionSets) {
			if (actionSet.getAction() instanceof Update) {
				updateActionSet = actionSet;
			}
		}

		if (updateActionSet != null) {
			if (updateActionSet.getAstNodeType().equals("FieldDeclaration")) {
				matchedActionSets.clear();
				matchedActionSets.add(updateActionSet);
				return matchedActionSets;
			}
		}

		return actionSets;
	}
	private String getType(Violation violation) {
		String type = violation.getViolationType();
		switch (type) {
		case "CI_CONFUSED_INHERITANCE":// field
			// update fieldDeclaration
			break;
		case "CO_ABSTRACT_SELF":
			// java file is an interface, and delete compareTo().
			break;
		case "EQ_ABSTRACT_SELF":
			// java file is an interface, and delete equals().
			break;
		case "SE_NO_SERIALVERSIONID":
			// add a field: serialVersionUID
			break;
		case "EQ_COMPARETO_USE_OBJECT_EQUALS":
			//Update or Delete compareTo(), Add equals()
			break;
		case "EQ_DOESNT_OVERRIDE_EQUALS":
			//override equals()
			break;
		case "HE_SIGNATURE_DECLARES_HASHING_OF_UNHASHABLE_CLASS":
			//remove equals()
			break;
		case "ME_MUTABLE_ENUM_FIELD":
			// under enum, field add final keyword
			break;
		case "MF_CLASS_MASKS_FIELD":
			// change super class or delete the field with a same name in super class.
			break;
		case "MS_SHOULD_BE_FINAL":
			// add final to field
			break;
		case "STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE":
			//remove public static final DateFormat DATE_FORMAT....  or SimpleDateFormat
			break;
		case "UUF_UNUSED_FIELD":
			//remove unused fields.  not sure
			break;
		case "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD":
			//remove unused fields.  not sure
		case "UWF_NULL_FIELD":
			//update field, remove field
			break;
		case "UWF_UNWRITTEN_FIELD":
			//field
			break;
		case "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD":
			//remove field
			break;
		case "VO_VOLATILE_REFERENCE_TO_ARRAY":
			//field
			break;
		default:
			break;
		}
		return null;
	}

	/**
	 * Filter out the modify actions, which are not in the DiffEntry hunks, without considering DiffEntry hunks.
	 * 
	 * @param violations
	 * @param actionSets
	 * @param revFile
	 * @param prevFile
	 * @return
	 */
	public List<Violation> filterActionsByModifiedRange3(List<Violation> violations,
			List<HierarchicalActionSet> actionSets, File revFile, File prevFile) {
		
		List<Violation> selectedViolations = new ArrayList<>();
		
		CUCreator cuCreator = new CUCreator();
		CompilationUnit prevUnit = cuCreator.createCompilationUnit(prevFile);
		CompilationUnit revUnit = cuCreator.createCompilationUnit(revFile);
		if (prevUnit == null || revUnit == null) {
			return selectedViolations;
		}
		
		for (Violation violation : violations) {
			int startLine = violation.getStartLineNum();
			int endLine = violation.getEndLineNum();
			
//			ITree parent = null;
//			List<HierarchicalActionSet> actionSetsWithSameParent = new ArrayList<>(); //TODO
			for (HierarchicalActionSet actionSet : actionSets) {
				int actionBugStartLine = actionSet.getBugStartLineNum();
				if (actionBugStartLine == 0) {
					actionBugStartLine = setLineNumbers(actionSet, prevUnit, revUnit);
				} 
				int actionBugEndLine = actionSet.getBugEndLineNum();
				int actionFixStartLine = actionSet.getFixStartLineNum();
				int actionFixEndLine = actionSet.getFixEndLineNum();

				String actionStr = actionSet.getActionString();
				if (actionStr.startsWith("INS")) { // FIXME It is impossible to locate the INS action by the buggy line range.
					if (startLine <= actionFixStartLine && actionFixEndLine <= endLine) {
						if (actionBugStartLine != 0) {
							if (startLine <= actionBugEndLine && endLine >= actionBugStartLine) {
								violation.getActionSets().add(actionSet);
							}
						} else {
							violation.getActionSets().add(actionSet);
						}
					}
				} else {
//					if (endLine < actionBugStartLine)  {
//						break;
//					}
					if (startLine <= actionBugStartLine && actionBugEndLine <= endLine) {
						if (startLine <= actionBugEndLine && endLine >= actionBugStartLine) {
							violation.getActionSets().add(actionSet);
						}
					}
				}
			}
			
			if (violation.getActionSets().size() > 0) {
				selectedViolations.add(violation);
			}
		}
		return selectedViolations;
	}

	private int setLineNumbers(HierarchicalActionSet actionSet, CompilationUnit prevUnit, CompilationUnit revUnit) {
		int actionBugStartLine;
		int actionBugEndLine;
		int actionFixStartLine;
		int actionFixEndLine;
		
		// position of buggy statements
		int bugStartPosition = 0;
		int bugEndPosition = 0;
		// position of fixed statements
		int fixStartPosition = 0;
		int fixEndPosition = 0;
		
		String actionStr = actionSet.getActionString();
		if (actionStr.startsWith("INS")) {
			fixStartPosition = actionSet.getStartPosition();
			fixEndPosition = fixStartPosition + actionSet.getLength();

			List<Move> firstAndLastMov = getFirstAndLastMoveAction(actionSet);
			if (firstAndLastMov != null) {
				bugStartPosition = firstAndLastMov.get(0).getNode().getPos();
				ITree lastTree = firstAndLastMov.get(1).getNode();
				bugEndPosition = lastTree.getPos() + lastTree.getLength();
			}
		} else {
			bugStartPosition = actionSet.getStartPosition(); // range of actions
			bugEndPosition = bugStartPosition + actionSet.getLength();
			if (actionStr.startsWith("UPD")) {
				Update update = (Update) actionSet.getAction();
				ITree newNode = update.getNewNode();
				fixStartPosition = newNode.getPos();
				fixEndPosition = fixStartPosition + newNode.getLength();
				
				String astNodeType = actionSet.getAstNodeType();
				if ("EnhancedForStatement".equals(astNodeType) || "ForStatement".equals(astNodeType) 
						|| "DoStatement".equals(astNodeType) || "WhileStatement".equals(astNodeType)
						|| "LabeledStatement".equals(astNodeType) || "SynchronizedStatement".equals(astNodeType)
						|| "IfStatement".equals(astNodeType) || "TryStatement".equals(astNodeType)
						|| "MethodDeclaration".equals(astNodeType)) {
					List<ITree> children = update.getNode().getChildren();
					bugEndPosition = getEndPosition(children);
					List<ITree> newChildren = newNode.getChildren();
					fixEndPosition = getEndPosition(newChildren);
					
					if (bugEndPosition == 0) {
						bugEndPosition = bugStartPosition + actionSet.getLength();
					}
					if (fixEndPosition == 0) {
						fixEndPosition = fixStartPosition + newNode.getLength();
					}
				} else if ("TypeDeclaration".equals(astNodeType)) {
					bugEndPosition = getClassBodyStartPosition(update.getNode());
					fixEndPosition = getClassBodyStartPosition(newNode);
					
					if (bugEndPosition == 0) {
						bugEndPosition = bugStartPosition + actionSet.getLength();
					}
					if (fixEndPosition == 0) {
						fixEndPosition = fixStartPosition + newNode.getLength();
					}
				}
			}
		}
		actionBugStartLine = bugStartPosition == 0 ? 0 : prevUnit.getLineNumber(bugStartPosition);
		actionBugEndLine = bugEndPosition == 0 ? 0 : prevUnit.getLineNumber(bugEndPosition);
		actionFixStartLine = fixStartPosition == 0 ? 0 : revUnit.getLineNumber(fixStartPosition);
		actionFixEndLine = fixEndPosition == 0 ? 0 : revUnit.getLineNumber(fixEndPosition);
		actionSet.setBugStartLineNum(actionBugStartLine);
		actionSet.setBugEndLineNum(actionBugEndLine);
		actionSet.setFixStartLineNum(actionFixStartLine);
		actionSet.setFixEndLineNum(actionFixEndLine);
		actionSet.setBugEndPosition(bugEndPosition);
		actionSet.setFixEndPosition(fixEndPosition);
		
		return actionBugStartLine;
	}
	
	private int getClassBodyStartPosition(ITree tree) {
		List<ITree> children = tree.getChildren();
		for (int i = 0, size = children.size(); i < size; i ++) {
			ITree child = children.get(i);
			int type = child.getType();
			// Modifier, NormalAnnotation, MarkerAnnotation, SingleMemberAnnotation
			if (type != 83 && type != 77 && type != 78 && type != 79
				&& type != 5 && type != 39 && type != 43 && type != 74 && type != 75
				&& type != 76 && type != 84 && type != 87 && type != 88 && type != 42) {
				// ArrayType, PrimitiveType, SimpleType, ParameterizedType, 
				// QualifiedType, WildcardType, UnionType, IntersectionType, NameQualifiedType, SimpleName
				if (i > 0) {
					child = children.get(i - 1);
					return child.getPos() + child.getLength() + 1;
				} else {
					return child.getPos() - 1;
				}
			}
		}
		return 0;
	}

}
