package edu.lu.uni.serval.FixPatternParser.violations;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import edu.lu.uni.serval.MultipleThreadsParser.MessageFile;
import edu.lu.uni.serval.MultipleThreadsParser.ParseFixPatternActor;
import edu.lu.uni.serval.MultipleThreadsParser.WorkMessage;
import edu.lu.uni.serval.config.Configuration;
import edu.lu.uni.serval.utils.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestHunkParserSingleFile {

	private static Logger log = LoggerFactory.getLogger(TestHunkParserSingleFile.class);
	public static void main(String[] args) {
		// input data

//			String rootPath = "/Users/anilkoyuncu/bugStudy";
        String inputPath;
        String outputPath;
        if(args.length > 0){
            inputPath = args[1];
            outputPath = args[0];
        }else{
//            inputPath = "/Users/anilkoyuncu/bugStudy/dataset/GumTreeInputBug4";
			inputPath = "/Users/anilkoyuncu/bugStudy/dataset/allDataset";
//            outputPath = "/Users/anilkoyuncu/bugStudy/code/python/GumTreeOutput2/";
			outputPath = "/Users/anilkoyuncu/bugStudy/dataset/GumTreeOutputSingle";
        }

		//5d9d60_76f5be_components#camel-jaxb#src#test#java#org#apache#camel#jaxb#FallbackTypeConverterShouldNotThrowExceptionTest.txt_1_CAMEL

		File folder = new File(inputPath);
		File[] listOfFiles = folder.listFiles();
        Stream<File> stream = Arrays.stream(listOfFiles);
        List<File> folders = stream
				.filter(x -> !x.getName().startsWith("."))

				.collect(Collectors.toList());

//		List<File> targetList = new ArrayList<File>();
//		for (File f:folders){
//            for(File f1 :f.listFiles()){
//                if  (!f1.getName().startsWith(".")){
//                targetList.add(f1);
//                }
//            }
//        }

//        List<String> pjList = Arrays.asList("DATAJPA","ZXing","PDE","SWS","SWT", "SWF", "COLLECTIONS", "JDT");
		List<String> files = new ArrayList<String>();
//		files.add("5d9d60_76f5be_components#camel-jaxb#src#test#java#org#apache#camel#jaxb#FallbackTypeConverterShouldNotThrowExceptionTest.java");
		files.add("d6c5e5_9f96d9_hbase-server#src#main#java#org#apache#hadoop#hbase#master#RegionStates.java");
		for(String f : files){
			String pjName = "HBASE";
//        for (File target : folders) {
//            String pjName = target.getName();
//            if (!pjList.contains(pjName)){
//                continue;
//            }

//            final List<MessageFile> msgFiles = getMessageFiles(target.toString() + "/"); //"/Users/anilkoyuncu/bugStudy/code/python/GumTreeInput/Apache/CAMEL/"
			MessageFile messageFile = getMessageFile(inputPath + "/" + pjName +"/", f);

			List<MessageFile> msgFiles = new ArrayList<>();
			msgFiles.add(messageFile);
            String GUM_TREE_OUTPUT = outputPath + "/"+  pjName + "/";
            final String editScriptsFilePath = GUM_TREE_OUTPUT + "editScripts.list";
            final String patchesSourceCodeFilePath =GUM_TREE_OUTPUT + "patchSourceCode.list";
            final String buggyTokensFilePath = GUM_TREE_OUTPUT + "tokens.list";
            final String editScriptSizesFilePath = GUM_TREE_OUTPUT + "editScriptSizes.csv";
            final String alarmTypesFilePath = GUM_TREE_OUTPUT + "alarmTypes.list";


			FileHelper.createDirectory(GUM_TREE_OUTPUT + "/ActionSetDumps");
            FileHelper.deleteDirectory(editScriptsFilePath);
            FileHelper.deleteDirectory(patchesSourceCodeFilePath);
            FileHelper.deleteDirectory(buggyTokensFilePath);
            FileHelper.deleteDirectory(editScriptSizesFilePath);
            FileHelper.deleteDirectory(alarmTypesFilePath);

            StringBuilder astEditScripts = new StringBuilder();
            StringBuilder tokens = new StringBuilder();
            StringBuilder sizes = new StringBuilder();
            StringBuilder patches = new StringBuilder();
            StringBuilder alarmTypes = new StringBuilder();

//            int a = 0;

			ActorSystem system = null;
			ActorRef parsingActor = null;
			final WorkMessage msg = new WorkMessage(0, msgFiles);
			try {
				log.info("Akka begins...");
				system = ActorSystem.create("Mining-FixPattern-System");
				parsingActor = system.actorOf(ParseFixPatternActor.props(1, editScriptsFilePath,
						patchesSourceCodeFilePath, buggyTokensFilePath, editScriptSizesFilePath), "mine-fix-pattern-actor");
				parsingActor.tell(msg, ActorRef.noSender());
			} catch (Exception e) {
				system.shutdown();
				e.printStackTrace();
			}

//		int counter = 0;
//            for (MessageFile msgFile : msgFiles) {
//                FixedViolationHunkParser parser = new FixedViolationHunkParser();
//
//                final ExecutorService executor = Executors.newSingleThreadExecutor();
//                // schedule the work
//                final Future<?> future = executor.submit(new RunnableParser(msgFile.getPrevFile(),
//                        msgFile.getRevFile(), msgFile.getDiffEntryFile(), parser));
//                try {
//                    // where we wait for task to complete
//                    future.get(Configuration.SECONDS_TO_WAIT, TimeUnit.SECONDS);
//                    String editScripts = parser.getAstEditScripts();
//                    if (!editScripts.equals("")) {
//                        astEditScripts.append(editScripts);
//                        tokens.append(parser.getTokensOfSourceCode());
//                        sizes.append(parser.getSizes());
//                        patches.append(parser.getPatchesSourceCode());
//                        alarmTypes.append(parser.getAlarmTypes());
//
//                        a++;
//                        if (a % 100 == 0) {
//                            FileHelper.outputToFile(editScriptsFilePath, astEditScripts, true);
//                            FileHelper.outputToFile(buggyTokensFilePath, tokens, true);
//                            FileHelper.outputToFile(editScriptSizesFilePath, sizes, true);
//                            FileHelper.outputToFile(patchesSourceCodeFilePath, patches, true);
//                            FileHelper.outputToFile(alarmTypesFilePath, alarmTypes, true);
//                            astEditScripts.setLength(0);
//                            tokens.setLength(0);
//                            sizes.setLength(0);
//                            patches.setLength(0);
//                            alarmTypes.setLength(0);
//                            System.out.println("Finish of parsing " + a + " files......");
//                        }
//                    }
//                } catch (TimeoutException e) {
//                    err.println("task timed out");
//                    future.cancel(true /* mayInterruptIfRunning */);
//                } catch (InterruptedException e) {
//                    err.println("task interrupted");
//                } catch (ExecutionException e) {
//                    err.println("task aborted");
//                } finally {
//                    executor.shutdownNow();
//                }
//            }

            FileHelper.outputToFile(editScriptsFilePath, astEditScripts, true);
            FileHelper.outputToFile(buggyTokensFilePath, tokens, true);
            FileHelper.outputToFile(editScriptSizesFilePath, sizes, true);
            FileHelper.outputToFile(patchesSourceCodeFilePath, patches, true);
            FileHelper.outputToFile(alarmTypesFilePath, alarmTypes, true);
            astEditScripts.setLength(0);
            tokens.setLength(0);
            sizes.setLength(0);
            patches.setLength(0);
            alarmTypes.setLength(0);


//		classifyByAlarmTypes();
        }
	}
	

	private static List<MessageFile> getMessageFiles(String gumTreeInput) {
		String inputPath = gumTreeInput; // prevFiles  revFiles diffentryFile positionsFile
		File revFilesPath = new File(inputPath + "revFiles/");
		File[] revFiles = revFilesPath.listFiles();   // project folders
		List<MessageFile> msgFiles = new ArrayList<>();

		// gumTreeInput = /Volumes/data/bugStudy_backup/dataset/GumTreeInputBug4/AMQP/
		// fileName = 01534a_df5570_spring-rabbit#src#test#java#org#springframework#amqp#rabbit#listener#LocallyTransactedTests.java
        if (revFiles.length >= 0) {
            for (File revFile : revFiles) {
//			if (revFile.getName().endsWith(".java")) {
                String fileName = revFile.getName();
                File prevFile = new File(gumTreeInput + "prevFiles/prev_" + fileName);// previous file
                fileName = fileName.replace(".java", ".txt");
                File diffentryFile = new File(gumTreeInput + "DiffEntries/" + fileName); // DiffEntry file
                File positionFile = new File(gumTreeInput + "positions/" + fileName); // position file
                MessageFile msgFile = new MessageFile(revFile, prevFile, diffentryFile);
                msgFile.setPositionFile(positionFile);
                msgFiles.add(msgFile);
//			}
            }

            return msgFiles;
        }
        else{
            return null;
        }
	}

	private static MessageFile getMessageFile(String gumTreeInput, String fileName) {
//		String inputPath = gumTreeInput; // prevFiles  revFiles diffentryFile positionsFile
//		File revFilesPath = new File(inputPath + "revFiles/");
//		File[] revFiles = revFilesPath.listFiles();   // project folders
//		List<MessageFile> msgFiles = new ArrayList<>();

		// gumTreeInput = /Volumes/data/bugStudy_backup/dataset/GumTreeInputBug4/AMQP/
		// fileName = 01534a_df5570_spring-rabbit#src#test#java#org#springframework#amqp#rabbit#listener#LocallyTransactedTests.java
//		if (revFiles.length >= 0) {
//			for (File revFile : revFiles) {
//			if (revFile.getName().endsWith(".java")) {
//				String fileName = revFile.getName();
				File revFile = new File(gumTreeInput + "revFiles/"+fileName);
				File prevFile = new File(gumTreeInput + "prevFiles/prev_" + fileName);// previous file
				fileName = fileName.replace(".java", ".txt");
				File diffentryFile = new File(gumTreeInput + "DiffEntries/" + fileName); // DiffEntry file
//				File positionFile = new File(gumTreeInput + "positions/" + fileName); // position file
				MessageFile msgFile = new MessageFile(revFile, prevFile, diffentryFile);
				return msgFile;
//				msgFile.setPositionFile(positionFile);
//				msgFiles.add(msgFile);
//			}
//			}
//
//			return msgFiles;
//		}
//		else{
//			return null;
//		}
	}
	
	public static void classifyByAlarmTypes() {

		final String alarmTypesFilePath = Configuration.ALARM_TYPES_FILE;
		List<String> alarmTypes = readStringList(alarmTypesFilePath);
		//edit scripts, sizes of edit scripts, buggy tokens, patches.
		classifyByAlarmTypes(alarmTypes, Configuration.EDITSCRIPT_SIZES_FILE);
		classifyByAlarmTypes(alarmTypes, Configuration.EDITSCRIPTS_FILE);
		classifyByAlarmTypes(alarmTypes, Configuration.BUGGY_CODE_TOKENS_FILE);
		classifyByAlarmTypes2(alarmTypes, Configuration.PATCH_SOURCECODE_FILE);
	}

	private static void classifyByAlarmTypes(List<String> alarmTypes, String file) {
		Map<String, StringBuilder> buildersMap = new HashMap<>();
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(file);
			scanner = new Scanner(fis);
			int counter = 0;
			while (scanner.hasNextLine()) {
				String alarmType = alarmTypes.get(counter);
				StringBuilder builder = getBuilder(buildersMap, alarmType);
				builder.append(scanner.nextLine() + "\n");
				counter ++;
				if (counter % 1000 == 0) {
					outputBuilders(buildersMap, file);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		outputBuilders(buildersMap, file);
	}
	
	private static void classifyByAlarmTypes2(List<String> alarmTypes, String patchSourcecodeFile) {
		Map<String, StringBuilder> buildersMap = new HashMap<>();
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(patchSourcecodeFile);
			scanner = new Scanner(fis);
			int counter = 0;
			String singlePatch = "";
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (Configuration.PATCH_SIGNAL.equals(line)) {
					if (!"".equals(singlePatch)) {
						String alarmType = alarmTypes.get(counter);
						StringBuilder builder = getBuilder(buildersMap, alarmType);
						builder.append(scanner.nextLine() + "\n");
						counter ++;
						if (counter % 2000 == 0) {
							outputBuilders(buildersMap, patchSourcecodeFile);
						}
					}
					singlePatch = line + "\n";
				}
				singlePatch += line + "\n";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		outputBuilders(buildersMap, patchSourcecodeFile);
	}

	private static void outputBuilders(Map<String, StringBuilder> map, String fileNameStr) {
		File file = new File(fileNameStr);
		String fileName = file.getName();
		String parentPath = file.getParent();
		for (Map.Entry<String, StringBuilder> entry : map.entrySet()) {
			String alarmType = entry.getKey();
			StringBuilder builder = entry.getValue();
			
			FileHelper.outputToFile(parentPath + "/" + alarmType + "/" + fileName, builder, true);
			
			builder.setLength(0);
			entry.setValue(builder);
		}
	}

	public static List<String> readStringList(String inputFile) {
		List<String> list = new ArrayList<>();
		FileInputStream fis = null;
		Scanner scanner = null;
		try {
			fis = new FileInputStream(inputFile);
			scanner = new Scanner(fis);
			while(scanner.hasNextLine()) {
				list.add(scanner.nextLine());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				scanner.close();
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	private static StringBuilder getBuilder(Map<String, StringBuilder> buildersMap, String alarmType) {
		if (buildersMap.containsKey(alarmType)) {
			return buildersMap.get(alarmType);
		} else {
			StringBuilder builder = new StringBuilder();
			buildersMap.put(alarmType, builder);
			return builder;
		}
	}
}
