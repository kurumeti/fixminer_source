package edu.lu.uni.serval.MultipleThreadsParser;

import static java.lang.System.err;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import edu.lu.uni.serval.FixPatternParser.Parser;
import edu.lu.uni.serval.FixPatternParser.RunnableParser;
import edu.lu.uni.serval.FixPatternParser.patch.CommitPatchSingleStatementParser;
import edu.lu.uni.serval.FixPatternParser.violations.FixedViolationHunkParser;
import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.utils.FileHelper;

public class ParseFixPatternWorker extends UntypedActor {
	private static Logger log = LoggerFactory.getLogger(ParseFixPatternActor.class);
	
	private String editScriptsFilePath;
	private String patchesSourceCodeFilePath;
	private String editScriptSizesFilePath;
	private String buggyTokensFilePath;
	private String alarmTypesFilePath;
	
	public ParseFixPatternWorker(String editScriptsFilePath, String patchesSourceCodeFilePath, 
			String buggyTokensFilePath, String editScriptSizesFilePath, String alarmTypesFilePath) {
		this.editScriptsFilePath = editScriptsFilePath;
		this.patchesSourceCodeFilePath = patchesSourceCodeFilePath;
		this.editScriptSizesFilePath = editScriptSizesFilePath;
		this.buggyTokensFilePath = buggyTokensFilePath;
		this.alarmTypesFilePath = alarmTypesFilePath;
	}

	public static Props props(final String editScriptsFile, final String patchesSourceCodeFile, final String buggyTokensFilePath, 
			final String editScriptSizesFilePath, final String alarmTypesFilePath) {
		return Props.create(new Creator<ParseFixPatternWorker>() {

			private static final long serialVersionUID = -7615153844097275009L;

			@Override
			public ParseFixPatternWorker create() throws Exception {
				return new ParseFixPatternWorker(editScriptsFile, patchesSourceCodeFile, 
						buggyTokensFilePath, editScriptSizesFilePath, alarmTypesFilePath);
			}
			
		});
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof WorkMessage) {
			WorkMessage msg = (WorkMessage) message;
			List<MessageFile> files = msg.getMsgFiles();
			StringBuilder editScripts = new StringBuilder();
			StringBuilder patchesSourceCode = new StringBuilder();
			StringBuilder sizes = new StringBuilder();
			StringBuilder tokens = new StringBuilder();
			StringBuilder alarmTypes = new StringBuilder();

			int id = msg.getId();
			int counter = 0;
			boolean containsAlarmTypes = false;
			for (MessageFile msgFile : files) {
				File revFile = msgFile.getRevFile();
				File prevFile = msgFile.getPrevFile();
				File diffentryFile = msgFile.getDiffEntryFile();
				File positionFile = msgFile.getPositionFile();
				if (revFile.getName().toLowerCase().contains("test")) {
					continue;
				}
				Parser parser = null;
				if (containsAlarmTypes || positionFile != null) {
					parser = new FixedViolationHunkParser(positionFile);
					containsAlarmTypes = true;
				} else {
					parser = new CommitPatchSingleStatementParser();
				}
				
				final ExecutorService executor = Executors.newSingleThreadExecutor();
				// schedule the work
				final Future<?> future = executor.submit(new RunnableParser(prevFile, revFile, diffentryFile, parser));
				try {
					// wait for task to complete
					future.get(Configuration.SECONDS_TO_WAIT, TimeUnit.SECONDS);
					
					String editScript = parser.getAstEditScripts();
					if (!"".equals(editScript)) {
						editScripts.append(editScript);
						patchesSourceCode.append(parser.getPatchesSourceCode());
						sizes.append(parser.getSizes());
						tokens.append(parser.getTokensOfSourceCode());
						if (containsAlarmTypes) {
							alarmTypes.append(((FixedViolationHunkParser) parser).getAlarmTypes());
						}
						counter ++;
						if (counter % 100 == 0) {
							FileHelper.outputToFile(editScriptsFilePath + "edistScripts_" + id + ".list", editScripts, true);
							FileHelper.outputToFile(patchesSourceCodeFilePath + "patches_" + id + ".list", patchesSourceCode, true);
							FileHelper.outputToFile(editScriptSizesFilePath + "sizes_" + id + ".list", sizes, true);
							FileHelper.outputToFile(buggyTokensFilePath + "tokens_" + id + ".list", tokens, true);
							editScripts.setLength(0);
							patchesSourceCode.setLength(0);
							sizes.setLength(0);
							tokens.setLength(0);
							if (containsAlarmTypes) {
								FileHelper.outputToFile(alarmTypesFilePath + "alarmTypes_" + id + ".list", alarmTypes, true);
								alarmTypes.setLength(0);
							}
							log.info("Worker #" + id +"Finish of parsing " + counter + " files...");
						}
					}
				} catch (TimeoutException e) {
					err.println("task timed out");
					future.cancel(true);
				} catch (InterruptedException e) {
					err.println("task interrupted");
				} catch (ExecutionException e) {
					err.println("task aborted");
				} finally {
					executor.shutdownNow();
				}
			}
			
			if (sizes.length() > 0) {
				FileHelper.outputToFile(editScriptsFilePath + "edistScripts_" + id + ".list", editScripts, true);
				FileHelper.outputToFile(patchesSourceCodeFilePath + "patches_" + id + ".list", patchesSourceCode, true);
				FileHelper.outputToFile(editScriptSizesFilePath + "sizes_" + id + ".list", sizes, true);
				FileHelper.outputToFile(buggyTokensFilePath + "tokens_" + id + ".list", tokens, true);
				editScripts.setLength(0);
				patchesSourceCode.setLength(0);
				sizes.setLength(0);
				tokens.setLength(0);
				if (containsAlarmTypes) {
					FileHelper.outputToFile(alarmTypesFilePath + "alarmTypes_" + id + ".list", alarmTypes, true);
					alarmTypes.setLength(0);
				}
			}

			log.info("Worker #" + id +"Finish of parsing " + counter + " files...");
			log.info("Worker #" + id + " finished the work...");
			this.getSender().tell("STOP", getSelf());
		} else {
			unhandled(message);
		}
	}
}
