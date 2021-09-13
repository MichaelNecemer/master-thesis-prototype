package functionality;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Task;

import Mapping.ProcessInstanceWithVoters;
import ProcessModelGeneratorAndAnnotater.ProcessGenerator;
import ProcessModelGeneratorAndAnnotater.ProcessModelAnnotater;

public class BatchFileGenerator {

	static int idCSVFile = 1;
	static String rootFolder = "C:\\Users\\Micha\\OneDrive\\Desktop\\EvaluationSetup";

	static int timeOutForProcessGeneratorInMin = 1;
	static int timeOutForApiInMin = 1;
	static int timeOutForProcessModelAnnotaterInMin = 1;
	
	//set the upper bound where outcomes of algorithms will be compared
	//else it will take a lot of time to compute
	static int boundForComparisons = 10000000;

	// bounds for ProcessModelGenerator
	static int probTask = 50;
	static int probXorGtw = 30;
	static int probParallelGtw = 20;
	static int probJoinGtw = 20;
	static int nestingDepthFactor = 10;

	// bounds for ProcessModelAnnotater
	static LinkedList<Integer> dataObjectBoundsSmallProcesses = new LinkedList<Integer>(Arrays.asList(1, 2));
	static LinkedList<Integer> dataObjectBoundsMediumProcesses = new LinkedList<Integer>(Arrays.asList(1, 3));
	static LinkedList<Integer> dataObjectBoundsLargeProcesses = new LinkedList<Integer>(Arrays.asList(1, 4));

	static LinkedList<String> defaultSpheres = new LinkedList<String>(
			Arrays.asList("Global", "Static", "Weak-Dynamic", "Strong-Dynamic"));
	static LinkedList<String> defaultNamesSeqFlowsXorSplits = new LinkedList<String>(Arrays.asList("true", "false"));
	static int dynamicWriterProb = 0;
	static int readerProb = 30;
	static int writerProb = 20;
	static int probPublicSphere = 0;

	// bounds for "small", "medium", "large" amountOfWriters classes in percentage
	// e.g. 10 means, there will be 10% writers of the tasks in the process
	static LinkedList<Integer> percentageOfWritersClasses = new LinkedList<Integer>(Arrays.asList(10, 20, 30));

	// bounds for "small", "medium", "large" amountOfReaders
	static LinkedList<Integer> percentageOfReadersClasses = new LinkedList<Integer>(Arrays.asList(10, 20, 30));

	// bounds for Algorithm that runs on annotated models
	static ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	static double costForAddingReaderAfterBrt = 0.0;

	public static void main(String[] args) throws Exception {

		//Test 1 - Boundary Test 1
		// The amount solutions generated will be the binomial coefficient for each
		// decision multiplied
		// e.g. 2 decisions - 1. needs 2 out of 4 voters = 6 possible combinations, 2.
		// needs 3 out of 5 = 10 possible combinations -> 6*10 = 60 possible
		// combinations of participants
		// so the boundary will be heavily influenced by the combination of voters per
		// decision as well as the amount of decisions in the process

		// the boundary test will set the max size which will be taken for further tests

		// generate 10 Processes - annotate them - try performing algorithms with a time
		// limit
		// count amount of timeouts
		// do that while there are not timeouts on all processes for both algorithms
		
		//Test with 1 unique dataObject per decision
		int minDataObjectsPerDecision = 1;
		int maxDataObjectsPerDecision = 1;
		String pathToFolderForModelsForBoundaryTest1AllSpheres = CommonFunctionality
				.fileWithDirectoryAssurance(rootFolder, "BoundaryTest1AllSpheres").getAbsolutePath();
		int votersPerDecision = 3;
		//BatchFileGenerator.performBoundaryTest1(10, 0, votersPerDecision, 5, 1, 6, 4, 0, 0, percentageOfWritersClasses.get(1), percentageOfReadersClasses.get(1), minDataObjectsPerDecision, maxDataObjectsPerDecision, defaultSpheres, 1, pathToFolderForModelsForBoundaryTest1AllSpheres);
						
		//Test 1 - Boundary Test 2
		//choose a model from boundary test 1 that had no exceptions for all algorithms
		//create x new models on each iteration till every task of the model has a different participant connected
		//start with the globalSphereLowerBound e.g. 2 -> x models where each task has one of the 2 participants connected
		//end point -> x models where each task has a different participant connected
		
		String pathToFolderForModelsForBoundaryTest2 = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "BoundaryTest2").getAbsolutePath();
	
		//choose a model
		File directoryOfFiles = new File(pathToFolderForModelsForBoundaryTest1AllSpheres+"\\BoundaryTest_decision-3\\annotated\\");
		//List<File> listOfFiles = Arrays.asList(directoryOfFiles.listFiles());
		//File model = CommonFunctionality.getRandomItem(listOfFiles);
		int newModelsPerIteration = 5;
		//BatchFileGenerator.performBoundaryTest2(model, votersPerDecision, newModelsPerIteration, 1, boundForComparisons, 1, pathToFolderForModelsForBoundaryTest2);
				
				// generate 3 Classes -> small, medium, large processes (without annotation)
		// put them into a new folder into the root
		String pathToFolderForModelsWithoutAnnotation = CommonFunctionality
				.fileWithDirectoryAssurance(rootFolder, "ProcessesWithoutAnnotation").getAbsolutePath();

		String pathToSmallProcessesFolderWithoutAnnotation = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "SmallProcessesFolder")
				.getAbsolutePath();
		String pathToMediumProcessesFolderWithoutAnnotation = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "MediumProcessesFolder")
				.getAbsolutePath();
		String pathToLargeProcessesFolderWithoutAnnotation = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "LargeProcessesFolder")
				.getAbsolutePath();

	

		// for each processes folder -> generate 100 random processes
		//small processes: 2-3 participants, 6-15 tasks, 1-2 xor, 0-2 parallels, 100 processes
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToSmallProcessesFolderWithoutAnnotation,
		//2, 3, 6, 15, 1, 2, 0, 2, 100);

		// medium processes: 3-4 participants, 16-30 tasks, 3-4 xors, 0-3 parallels, 100 processes
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToMediumProcessesFolderWithoutAnnotation,
		//3, 4, 16, 30, 3, 4, 0, 3, 100);

		//large processes: 4-5 participants, 31-60 tasks, 5-6 xors, 0-4, parallels, 100 processes
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolderWithoutAnnotation,
		//4, 5, 31, 60, 5, 6, 0, 4, 100);

		
		String pathToFolderForModelsForTest2 = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "Test2")
				.getAbsolutePath();
		// Test 2 - Measure impact of enforceability on privity and vice versa
		// take x random models from small, medium and large processes
		// add dataObjects with global sphere and 1 voter per decision
		// add readers/writers combinations - generate new models (9 new models for
		// each)
		// increase the amount of voters needed for decisions till the global sphere of
		// that process is reached on all xors
		// increase privity requirements to next stricter sphere for all dataObjects

		String pathToSmallProcessesForTest2WithAnnotation = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "SmallProcessesAnnotatedFolder")
				.getAbsolutePath();
	/*	String pathToMediumProcessesForTest2WithAnnotation  = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "MediumProcessesAnnotatedFolder")
				.getAbsolutePath();
		String pathToLargeProcessesForTest2WithAnnotation  = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "LargeProcessesAnnotatedFolder")
				.getAbsolutePath();
		*/
		LinkedList<File> smallProcessesWithoutAnnotation = BatchFileGenerator.getModelsInOrderFromSourceFolder(1, pathToSmallProcessesFolderWithoutAnnotation);
		LinkedList<File> mediumProcessesWithoutAnnotation = BatchFileGenerator.getModelsInOrderFromSourceFolder(0, pathToMediumProcessesFolderWithoutAnnotation);
		LinkedList<File> largeProcessesWithoutAnnotation = BatchFileGenerator.getModelsInOrderFromSourceFolder(0, pathToLargeProcessesFolderWithoutAnnotation);

		BatchFileGenerator.performTradeOffTest("small",smallProcessesWithoutAnnotation,
			pathToSmallProcessesForTest2WithAnnotation, dataObjectBoundsSmallProcesses, 1, boundForComparisons, 1);
		//BatchFileGenerator.performTradeOffTest("medium",mediumProcessesWithoutAnnotation,
		//pathToMediumProcessesForTest2WithAnnotation, dataObjectBoundsMediumProcesses, 1, boundForComparisons, 1);
		//BatchFileGenerator.performTradeOffTest("large",largeProcessesWithoutAnnotation,
		//pathToLargeProcessesForTest2WithAnnotation, dataObjectBoundsLargeProcesses, 1, boundForComparisons, 1);
		
		
		
		// Test 3 - Measure impact of dataObjects
		//annotate unique gateways per decision e.g. 5 unique data objects for 5 decisions -> 25 unique data objects
		//take one dataObject, loop through the others and replace one object with that dataObject in each iteration
		//on last step each decision will only have that dataObject connected
		String pathToFolderForModelsForDataObjectTest = CommonFunctionality
				.fileWithDirectoryAssurance(rootFolder, "DataObjectTest").getAbsolutePath();
		
		//take models from randomMixedModels
		
		int amountSmallProcesses = 1;
		int amountMediumProcesses = 0; 
		int amountLargeProcesses = 0;
		List<Integer>amountModelsPerFolder = new LinkedList<Integer>();
		amountModelsPerFolder.add(amountSmallProcesses);
		amountModelsPerFolder.add(amountMediumProcesses);
		amountModelsPerFolder.add(amountLargeProcesses);
		LinkedList<File>randomMixedModels = BatchFileGenerator.getFilesFromFolders(smallProcessesWithoutAnnotation, mediumProcessesWithoutAnnotation, largeProcessesWithoutAnnotation, amountModelsPerFolder);	
	
		
		//annotate models with same amount of unique data objects per decision
		int amountUniqueDataObjectsPerDecision = 4;
				
		//BatchFileGenerator.performDataObjectTest(randomMixedModels, pathToFolderForModelsForDataObjectTest, amountUniqueDataObjectsPerDecision, 1);
		
		
		
		// Test 4 - 1 "killer constraints"
		
		// add killer constraints
		//take models from small and medium processes
		//after that compare it to test 2 where those models have been run without constraints
		
		String pathToSmallProcessesWithAnnotation=pathToSmallProcessesForTest2WithAnnotation+"\\ModelsForEvaluation";
		LinkedList<File> smallProcessesFromTradeOffTest = BatchFileGenerator.getAllModelsFromFolder(pathToSmallProcessesWithAnnotation);
	//	String pathToMediumProcessesWithAnnotation=pathToMediumProcessesForTest2WithAnnotation+"\\ModelsForEvaluation";
		//LinkedList<File> mediumProcessesFromTradeOffTest = BatchFileGenerator.getModelsInOrderFromSourceFolder(3,pathToMediumProcessesWithAnnotation);
	//	LinkedList<File> mediumProcessesFromTradeOffTest = BatchFileGenerator.getAllModelsFromFolder(pathToMediumProcessesWithAnnotation);

		String pathToFolderForModelsForTest4= CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "KillerConstraintTest")
				.getAbsolutePath();
		//String pathToFolderForSmallModelsForTest4= CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest4, "SmallModels").getAbsolutePath();
		//BatchFileGenerator.performTestWithMaxPossibleConstraints(smallProcessesFromTradeOffTest, false, pathToFolderForSmallModelsForTest4, 1);
		
		/*
		File file = new File("C:\\Users\\Micha\\OneDrive\\Desktop\\EvaluationSetup\\ConstraintTest\\MediumModels\\randomProcessModel108_annotated1_annotated112mWsR_Strong-Dynamic_voters3_alwMaxConstrained.bpmn");
		File file2 = new File("C:\\Users\\Micha\\OneDrive\\Desktop\\EvaluationSetup\\ConstraintTest\\MediumModels\\randomProcessModel108_annotated1_annotated112mWsR_Strong-Dynamic_voters2_alwMaxConstrained.bpmn");	
		LinkedList<File>list = new LinkedList<File>();
		list.add(file);
		list.add(file2);
		
		
		File csv = BatchFileGenerator.createNewCSVFile(rootFolder, "test");
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csv);
		ExecutorService service = Executors.newFixedThreadPool(1);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(list, 1, amountUniqueDataObjectsPerDecision, writer, service);
		writer.writeRowsToCSVAndcloseWriter();
		service.shutdownNow();
		*/
		
		//String pathToFolderForMediumModelsForTest4= CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest4, "MediumModels").getAbsolutePath();
		//BatchFileGenerator.performTestWithMaxPossibleConstraints(mediumProcessesFromTradeOffTest, false, pathToFolderForMediumModelsForTest4, 1);
		
		
		//Test 4 - 2 -> Processes with probability to have constraints
		
		String pathToFolderForModelsForTest4_2= CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "SomeConstraintTest")
				.getAbsolutePath();
		String pathToFolderForSmallModelsForTest4_2= CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "SmallModels").getAbsolutePath();
		int probabilityForGatewayToHaveConstraint = 30;
		//BatchFileGenerator.performTestWithConstraintsProbability(smallProcessesFromTradeOffTest, false, probabilityForGatewayToHaveConstraint, pathToFolderForSmallModelsForTest4_2, 1);
		
		
				
		String pathToFolderForMediumModelsForTest4_2= CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "MediumModels").getAbsolutePath();
		
		//BatchFileGenerator.performTestWithConstraintsProbability(mediumProcessesFromTradeOffTest, false, probabilityForGatewayToHaveConstraint, pathToFolderForMediumModelsForTest4_2, 1);

		
		
		// Test 5 - "real world processes"		
		
		 List<Integer> dataObjectBoundsRealWorld = Arrays.asList(1, 6); 
		 int dynamicWriterProb = 30;
		  int upperBoundParticipants = 8; 
		  int lowerBoundTasks = 8; 
		  int upperBoundTasks = 80;
		  int upperBoundXorGtws = 10;
		  int upperBoundParallelGtws = 6;
		  int amountProcesses = 30;
		  int minDataObjectsPerDecisionTest5 = 1;
		  int  maxDataObjectsPerDecisionTest5 = 4;
		  List<Integer> writersOfProcessInPercent = Arrays.asList(10, 20, 30);
		  List<Integer> readersOfProcessInPercent = Arrays.asList(10, 20, 30); 
		  int upperBoundlocalMinWithBound = 1;
		  int amountThreads = 1;
		  
		  
		  String pathToRealWorldProcesses =  CommonFunctionality.fileWithDirectoryAssurance(rootFolder,
		 "RealWorldProcesses") .getAbsolutePath(); 
		  String pathToAnnotatedProcessesFolder = CommonFunctionality
		  .fileWithDirectoryAssurance(pathToRealWorldProcesses,
		  "AnnotatedModels").getAbsolutePath();
		  
		//BatchFileGenerator.performTestWithRealWorldProcesses(pathToRealWorldProcesses, pathToAnnotatedProcessesFolder, dynamicWriterProb, upperBoundParticipants, lowerBoundTasks, upperBoundTasks, upperBoundXorGtws, upperBoundParallelGtws, amountProcesses, minDataObjectsPerDecisionTest5, maxDataObjectsPerDecisionTest5, dataObjectBoundsRealWorld, writersOfProcessInPercent, readersOfProcessInPercent, upperBoundlocalMinWithBound, amountThreads);
		
		
		System.out.println("Everything finished!");
	}

	
	public static void performTestWithRealWorldProcesses(String pathWhereToCreateProcessesWithoutAnnotation, String pathWhereToCreateAnnotatedProcesses,  int dynamicWriterProb, int upperBoundParticipants, int lowerBoundTasks, int upperBoundTasks, int upperBoundXorGtws, int upperBoundParallelGtws,
	  int amountProcesses, int minDataObjectsPerDecision, int  maxDataObjectsPerDecision, List<Integer> dataObjectBoundsRealWorld, List<Integer> writersOfProcessInPercent, List<Integer> readersOfProcessInPercent, int upperBoundlocalMinWithBound, int amountThreads) {
		  BatchFileGenerator.generateRandomProcessesWithinGivenRanges(
				  pathWhereToCreateProcessesWithoutAnnotation, 2,  upperBoundParticipants, lowerBoundTasks, upperBoundTasks, 1, upperBoundXorGtws, 0, upperBoundParallelGtws,
		 amountProcesses); 
		  
		  
		  int publicDecisionProb = 0; 
		  
		  BatchFileGenerator.annotateModels(pathWhereToCreateProcessesWithoutAnnotation,
				  pathWhereToCreateAnnotatedProcesses, dataObjectBoundsRealWorld, defaultSpheres,
		  dynamicWriterProb, writersOfProcessInPercent, readersOfProcessInPercent,
		  2, upperBoundParticipants, minDataObjectsPerDecision,
		 maxDataObjectsPerDecision, publicDecisionProb);
		 
		  ExecutorService service = Executors.newFixedThreadPool(amountThreads);
		  File csv = BatchFileGenerator.createNewCSVFile(pathWhereToCreateAnnotatedProcesses, "real-world-processes");
		  ResultsToCSVWriter writer = new ResultsToCSVWriter(csv);
		 BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathWhereToCreateAnnotatedProcesses, upperBoundlocalMinWithBound, publicDecisionProb, writer, service);
		  writer.writeRowsToCSVAndcloseWriter();
		  service.shutdownNow();
		  
	}
	
	
	public static void annotateModels(String pathToFolderWithFilesWithoutAnnotation,
			String pathToFolderForStoringAnnotatedModelsFolder, List<Integer> dataObjectBoundsMixed,
			LinkedList<String> defaultSpheres, int dynamicWriterProb, List<Integer> writersOfProcessInPercent,
			List<Integer> readersOfProcessInPercent, int amountParticipantsPerDecisionLowerBound,
			int amountParticipantsPerDecisionUpperBound, int minDataObjectsPerDecision, int maxDataObjectsPerDecision,
			int publicDecisionProb) {
		// iterate through all files in the directory and annotate them

		File dir = new File(pathToFolderWithFilesWithoutAnnotation);
		File[] directoryListing = dir.listFiles();

		LinkedList<String> paths = new LinkedList<String>();
		LinkedList<String> alreadyAnnotated = new LinkedList<String>();
		if (directoryListing != null) {
			for (File model : directoryListing) {
				if (model.getName().contains(".bpmn")) {
					if (!model.getName().contains("_annotated")) {

						paths.add(model.getAbsolutePath());
					} else if (model.getName().contains("_annotated")) {
						String name = model.getAbsolutePath();
						String filter = name.substring(0, name.indexOf('_'));
						filter += ".bpmn";
						System.out.println("Already annotated: " + filter);
						alreadyAnnotated.add(filter);
					}

				}

			}

			paths.removeAll(alreadyAnnotated);

		} else {
			System.out.println("No process models found in " + dir.getAbsolutePath());

		}
		int coreCount = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(coreCount);

		LinkedList<Future<File>> futures = new LinkedList<Future<File>>();

		for (int i = 0; i < paths.size(); i++) {
			String pathToFile = paths.get(i);
			File currFile = new File(pathToFile);
			BpmnModelInstance modelInstance = Bpmn.readModelFromFile(currFile);
			int amountTasks = modelInstance.getModelElementsByType(Task.class).size();
			int writersPercentage = CommonFunctionality.getRandomItem(writersOfProcessInPercent);
			int readersPercentage = CommonFunctionality.getRandomItem(writersOfProcessInPercent);
			int amountWritersOfProcess = CommonFunctionality.getAmountFromPercentage(amountTasks, writersPercentage);
			int amountReadersOfProcess = CommonFunctionality.getAmountFromPercentage(amountTasks, readersPercentage);
			int globalSphere = CommonFunctionality.getGlobalSphere(modelInstance, false);
			if (amountParticipantsPerDecisionUpperBound <= 0
					|| amountParticipantsPerDecisionUpperBound > globalSphere) {
				if (amountParticipantsPerDecisionLowerBound == globalSphere) {
					amountParticipantsPerDecisionUpperBound = globalSphere;
				} else if (amountParticipantsPerDecisionLowerBound < globalSphere) {
					amountParticipantsPerDecisionUpperBound = ThreadLocalRandom.current()
							.nextInt(amountParticipantsPerDecisionLowerBound, globalSphere + 1);

				}

			}

			int amountDataObjectsToCreate = ThreadLocalRandom.current().nextInt(dataObjectBoundsMixed.get(0),
					dataObjectBoundsMixed.get(1) + 1);
			ProcessModelAnnotater p = null;

			try {
				p = new ProcessModelAnnotater(pathToFile, pathToFolderForStoringAnnotatedModelsFolder, "_annotated",
						defaultSpheres, dynamicWriterProb, amountWritersOfProcess, amountReadersOfProcess,
						publicDecisionProb,  amountDataObjectsToCreate,
						minDataObjectsPerDecision, maxDataObjectsPerDecision, amountParticipantsPerDecisionLowerBound,
						amountParticipantsPerDecisionUpperBound, defaultNamesSeqFlowsXorSplits, false);

				Future<File> future = executor.submit(p);
				futures.add(future);
				try {
					future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					future.cancel(true);
					i--;
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					System.err.println(e.getMessage());
					future.cancel(true);
					i--;
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					future.cancel(true);
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// Model not valid
				i--;
			}

		}

		executor.shutdown();
	}

	public static void generateRandomProcessesWithFixedValues(String pathForAddingRandomModels,
			int maxAmountParticipants, int maxAmountMaxTasks, int maxAmountMaxXorSplits, int maxAmountMaxParallelSplits,
			int amountProcesses, ExecutorService executor) {

		for (int i = 1; i <= amountProcesses; i++) {

			// will be written to the pathForAddingRandomModels

			ProcessGenerator pGen;
			try {
				
				pGen = new ProcessGenerator(pathForAddingRandomModels, maxAmountParticipants, maxAmountMaxTasks,
						maxAmountMaxXorSplits, maxAmountMaxParallelSplits,probTask, probXorGtw,probParallelGtw, probJoinGtw,nestingDepthFactor);

				Future<File> future = executor.submit(pGen);
				try {
					File f = future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
					System.out.println(f.getName() + " deployed successfully");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					future.cancel(true);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					future.cancel(true);
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					System.out.println("ProcessGenerator timed out!");
					e.printStackTrace();
					future.cancel(true);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				i--;
			}

		}

	}

	public static void generateRandomProcessesWithinGivenRanges(String pathToFiles, int lowerBoundParticipants,
			int upperBoundParticipants, int lowerBoundTasks, int upperBoundTasks, int lowerBoundXorGtws,
			int upperBoundXorGtws, int lowerBoundParallelGtws, int upperBoundParallelGtws, int amountProcesses) {

		for (int i = 1; i <= amountProcesses; i++) {

			int randomAmountParticipants = ThreadLocalRandom.current().nextInt(lowerBoundParticipants,
					upperBoundParticipants + 1);
			int randomAmountMaxTasks = ThreadLocalRandom.current().nextInt(lowerBoundTasks, upperBoundTasks + 1);
			int randomAmountMaxXorSplits = ThreadLocalRandom.current().nextInt(lowerBoundXorGtws,
					upperBoundXorGtws + 1);
			int randomAmountMaxParallelSplits = ThreadLocalRandom.current().nextInt(lowerBoundParallelGtws,
					upperBoundParallelGtws + 1);

			try {

				ProcessGenerator pGen = new ProcessGenerator(pathToFiles, randomAmountParticipants,
						randomAmountMaxTasks, randomAmountMaxXorSplits, randomAmountMaxParallelSplits, probTask,
						probXorGtw, probParallelGtw, probJoinGtw, nestingDepthFactor, lowerBoundXorGtws);
				ExecutorService executor = Executors.newSingleThreadExecutor();
				Future future = executor.submit(pGen);
				try {
					future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
				} catch (Exception e) {
					e.printStackTrace();
					future.cancel(true);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("randomProcess" + i + " deployed in " + pathToFiles);
		}

	}

	public static LinkedList<API> mapFilesToAPI(String pathToFiles) {
		// iterate through all files in the directory and run algorithms on annotated
		// models
		LinkedList<API> apiList = new LinkedList<API>();
		File dir = new File(pathToFiles);
		File[] directoryListing = dir.listFiles();
		Arrays.sort(directoryListing, Comparator.comparingLong(File::lastModified));
		
		
				
		LinkedList<String> paths = new LinkedList<String>();
		if (directoryListing != null) {
			for (File model : directoryListing) {
				if (model.getName().contains(".bpmn") && model.getName().contains("_annotated")) {
					paths.add(model.getAbsolutePath());
				}
			}
		} else {
			System.out.println("No annotated process models found in " + dir.getAbsolutePath());

		}

		for (String pathToFile : paths) {
			try {
				API api = new API(pathToFile, cost, costForAddingReaderAfterBrt);
				apiList.add(api);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return apiList;
	}

	public static LinkedList<API> mapFilesToAPI(LinkedList<File> files) {
		// iterate through all files in the directory and run algorithms on annotated
		// models
		LinkedList<API> apiList = new LinkedList<API>();
	
		LinkedList<String> paths = new LinkedList<String>();
		if (files != null) {
			for (File model : files) {
				if (model.getName().contains(".bpmn") && model.getName().contains("_annotated")) {
					paths.add(model.getAbsolutePath());
				}
			}
		} 

		for (String pathToFile : paths) {
			try {
				API api = new API(pathToFile, cost, costForAddingReaderAfterBrt);
				apiList.add(api);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return apiList;
	}

	
	
	
	private static File createNewCSVFile(String directoryForFile, String nameOfCSVFile) {
		String fileNameForResults = nameOfCSVFile +"_"+ idCSVFile + ".csv";
		File csvFile = new File(directoryForFile, fileNameForResults);
		try {
			while (csvFile.createNewFile() == false) {
				System.out.println("CSV File" + fileNameForResults + " already exists!");
				idCSVFile++;
				fileNameForResults = nameOfCSVFile + idCSVFile + ".csv";
				csvFile = new File(directoryForFile, fileNameForResults);
				if (csvFile.createNewFile()) {
					System.out.println(
							"CSV File" + fileNameForResults + " has been created at: " + csvFile.getAbsolutePath());
					break;
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		csvFile.getParentFile().mkdirs();
		return csvFile;
	}

		
	public static HashMap<String, Integer> runAlgorithmsAndWriteResultsToCSV(API api, int limitForLocalMin, int boundForComparisons, HashMap<String, Integer>timeOutMap,
			ResultsToCSVWriter writer, ExecutorService service) {
		// write the result of an algorithm to an existing csv file
		//clone api for algorithms
		
		API bruteForceApi = (API) CommonFunctionality.deepCopy(api);
		bruteForceApi.setAlgorithmToPerform("bruteForce");
		
		API localMinApi = (API) CommonFunctionality.deepCopy(api);
		localMinApi.setAlgorithmToPerform("localMin");
		
		
		String localMinWithMaxSolutions = "localMinWithBound"+limitForLocalMin;
		API localMinWithMaxSolutionsApi = (API)CommonFunctionality.deepCopy(api);
		localMinWithMaxSolutionsApi.setAlgorithmToPerform(localMinWithMaxSolutions);
		
		HashMap<String, Exception> exceptionPerAlgorithm = new HashMap<String, Exception>();
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap = new HashMap<String, LinkedList<ProcessInstanceWithVoters>>();
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesBruteForce = null;
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesLocalMin = null;
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesLocalMinWithMaxSolutions = null;

		try {
		
		Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureBruteForce = service.submit(bruteForceApi);
		Exception exceptionBruteForce = null;
		try {
			pInstancesBruteForce = futureBruteForce.get(timeOutForApiInMin, TimeUnit.MINUTES);
			algorithmMap.putAll(pInstancesBruteForce);
			System.out.println("BruteForceSolutions: " +pInstancesBruteForce.get("bruteForce").size());
			futureBruteForce.cancel(true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			exceptionBruteForce = (InterruptedException)e;	
			e.printStackTrace();
			futureBruteForce.cancel(true);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			exceptionBruteForce = (ExecutionException)e;
			e.printStackTrace();
			futureBruteForce.cancel(true);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			exceptionBruteForce = (TimeoutException)e;
			timeOutMap.put("bruteForce", timeOutMap.getOrDefault("bruteForce", 0)+1);
			System.err.println("Timeout for bruteForce!");
			futureBruteForce.cancel(true);
		} finally {
			exceptionPerAlgorithm.putIfAbsent("bruteForce", exceptionBruteForce);
			futureBruteForce.cancel(true);
		}
		
		Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureLocalMin = service.submit(localMinApi);
		Exception exceptionLocalMin = null;
		
		try {
			pInstancesLocalMin= futureLocalMin.get(timeOutForApiInMin, TimeUnit.MINUTES);
			algorithmMap.putAll(pInstancesLocalMin);
			System.out.println("LocalMinInstances: "+pInstancesLocalMin.get("localMin").size());
			futureLocalMin.cancel(true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
				exceptionLocalMin = (InterruptedException)e;
			e.printStackTrace();
			futureLocalMin.cancel(true);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			exceptionLocalMin = (ExecutionException)e;
			e.printStackTrace();
			futureLocalMin.cancel(true);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			exceptionLocalMin = (TimeoutException)e;
			timeOutMap.put("localMin", timeOutMap.getOrDefault("localMin", 0)+1);
			System.err.println("Timeout for localMin!");
			futureLocalMin.cancel(true);
		} finally {
			exceptionPerAlgorithm.putIfAbsent("localMin", exceptionLocalMin);
			futureLocalMin.cancel(true);
		}
		
		
		
		Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureLocalMinWithMaxSolutions = service.submit(localMinWithMaxSolutionsApi);
		Exception exceptionLocalMinWithMaxSolutions = null;
		
		try {
			pInstancesLocalMinWithMaxSolutions= futureLocalMinWithMaxSolutions.get(timeOutForApiInMin, TimeUnit.MINUTES);
			algorithmMap.putAll(pInstancesLocalMinWithMaxSolutions);
			System.out.println("LocalMinInstances with bound: "+pInstancesLocalMinWithMaxSolutions.get(localMinWithMaxSolutions).size());
			futureLocalMinWithMaxSolutions.cancel(true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			exceptionLocalMinWithMaxSolutions = (InterruptedException)e;
			e.printStackTrace();
			futureLocalMinWithMaxSolutions.cancel(true);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			exceptionLocalMinWithMaxSolutions = (ExecutionException)e;
			e.printStackTrace();
			futureLocalMinWithMaxSolutions.cancel(true);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			exceptionLocalMinWithMaxSolutions = (TimeoutException)e;
			timeOutMap.put(localMinWithMaxSolutions, timeOutMap.getOrDefault(localMinWithMaxSolutions, 0)+1);
			System.err.println("Timeout for "+localMinWithMaxSolutions
					+ "!");
			futureLocalMinWithMaxSolutions.cancel(true);
		} finally {
			exceptionPerAlgorithm.putIfAbsent(localMinWithMaxSolutions, exceptionLocalMinWithMaxSolutions);
			futureLocalMinWithMaxSolutions.cancel(true);
		}
				
		
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			String isCheapestSolutionOfLocalMinInBruteForce ="null";
			if(pInstancesLocalMin!=null&&pInstancesBruteForce!=null) {
				isCheapestSolutionOfLocalMinInBruteForce = CommonFunctionality.compareResultsOfAlgorithmsForDifferentAPIs(pInstancesLocalMin.get("localMin"), pInstancesBruteForce.get("bruteForce"), boundForComparisons);

			}
		String isCheapestSolutionOflocalMinWithBoundInBruteForce = "null";
		 if(pInstancesLocalMinWithMaxSolutions!=null&&pInstancesBruteForce!=null) {
			 isCheapestSolutionOflocalMinWithBoundInBruteForce =	CommonFunctionality.compareResultsOfAlgorithmsForDifferentAPIs(pInstancesLocalMinWithMaxSolutions.get(localMinWithMaxSolutions), pInstancesBruteForce.get("bruteForce"), boundForComparisons);

		 }
				

		writer.writeResultsOfAlgorithmsToCSVFile(bruteForceApi, localMinApi, localMinWithMaxSolutionsApi, algorithmMap, exceptionPerAlgorithm, isCheapestSolutionOfLocalMinInBruteForce, isCheapestSolutionOflocalMinWithBoundInBruteForce);
		
		}
		
		return timeOutMap;
		
	}

	

	public static void runAlgorithmsAndWriteResultsToCSV(LinkedList<File> files, int upperBoundSolutionsForLocalMin, int boundForComparisons, ResultsToCSVWriter writer, ExecutorService service
			) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(files);
		if (!apiList.isEmpty()) {
			for(API api: apiList) {
				BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api,upperBoundSolutionsForLocalMin, boundForComparisons, new HashMap<String, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}
	
	
	
	

	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels, int upperBoundSolutionsForLocalMin, int boundForComparisons, ResultsToCSVWriter writer, ExecutorService service
			) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithAnnotatedModels);
		if (!apiList.isEmpty()) {
			for(API api: apiList) {
				BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api,upperBoundSolutionsForLocalMin, boundForComparisons, new HashMap<String, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void runLocalMinAndGenerateNewFilesWithAnnotatedSolutions(String pathToFolderWithAnnotatedModels,
			int upperBoundNewModels) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithAnnotatedModels);
		if (!apiList.isEmpty()) {
			int coreCount = Runtime.getRuntime().availableProcessors();
			ExecutorService service = Executors.newFixedThreadPool(coreCount);
			HashMap<API, Future<LinkedList<ProcessInstanceWithVoters>>> allFuturesMap = new HashMap<API, Future<LinkedList<ProcessInstanceWithVoters>>>();
			for (API api : apiList) {
				api.setAlgorithmToPerform("localMin");
				// Future<LinkedList<ProcessInstanceWithVoters>> future = service.submit(api);
				// allFuturesMap.putIfAbsent(api, future);
			}

			for (Entry<API, Future<LinkedList<ProcessInstanceWithVoters>>> entry : allFuturesMap.entrySet()) {
				Future<LinkedList<ProcessInstanceWithVoters>> future = entry.getValue();
				API api = entry.getKey();
				LinkedList<ProcessInstanceWithVoters> localMinInst = null;
				try {
					localMinInst = future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
					CommonFunctionality.generateNewModelsWithAnnotatedChosenParticipants(api, localMinInst,
							upperBoundNewModels, "");

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					future.cancel(true);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					future.cancel(true);
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					future.cancel(true);
				}
			}
			service.shutdown();

		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void performDataObjectTest(LinkedList<File> processes, String pathToDestinationFolderForStoringModels,
			int amountUniqueDataObjectsPerDecision, int amountThreadPools) {
		// each decision has unique dataObject sets of same size
		// e.g. model has 4 decisions -> and each decision has 3 DataObjects -> 12
		// unique dataObjects need to be connected
		
		//amount voters per gtw will be between 2 and globalSphere
		

		//Test with medium amount of writers and readers
		int amountWritersInPercent = percentageOfWritersClasses.get(1);
		int amountReadersInPercent = percentageOfReadersClasses.get(1);
		
				
		//Start test with static sphere
		// then take same models and take strong dynamic sphere
		List<String> sphere = Arrays.asList("Static");
		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		for (int i = 0; i < processes.size(); i++) {
			String pathToRandomProcess = processes.get(i).getAbsolutePath();
			BpmnModelInstance processModel = Bpmn.readModelFromFile(processes.get(i));
			int amountTasks = processModel.getModelElementsByType(Task.class).size();
			int amountDecisions = CommonFunctionality.getAmountExclusiveGtwSplits(processModel);
			int globalSphere = CommonFunctionality.getGlobalSphere(processModel, false);
			int amountVotersUpperBound = ThreadLocalRandom.current().nextInt(2, globalSphere+1);
			
			File newModel = null;
			boolean modelIsValid = false;
			int sumDataObjectsToCreate = 0;
			boolean skipModel = false;
			while (modelIsValid == false && skipModel == false) {
				try {
					ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess,
							pathToDestinationFolderForStoringModels, "");

					sumDataObjectsToCreate = amountUniqueDataObjectsPerDecision * amountDecisions;
					if(amountTasks>sumDataObjectsToCreate) {
					pModel.generateDataObjects(sumDataObjectsToCreate, sphere);
					pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(amountUniqueDataObjectsPerDecision,
						amountUniqueDataObjectsPerDecision, 2, amountVotersUpperBound, 0, true);
					int amountWriters = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasks, amountWritersInPercent, sumDataObjectsToCreate);
					int amountReaders = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasks, amountReadersInPercent, sumDataObjectsToCreate);
					
					pModel.annotateModelWithFixedAmountOfReadersAndWriters(amountWriters, amountReaders, 0, null);
					//newModel = pModel.writeChangesToFileWithoutCorrectnessCheck();
					Future f = executor.submit(pModel);
					try {
					f.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
					f.cancel(true);
					modelIsValid = true;

					} catch(Exception e) {
						f.cancel(true);
					}
					
					} else {
						skipModel = true;
					}
					
					
				} catch (Exception e) {
					System.err.println(e.getMessage());
					
				}

			}
		}
		
		File directory = new File(pathToDestinationFolderForStoringModels);
		File[] files = directory.listFiles();
		
		for(File f:files) {
			BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality.deepCopy(Bpmn.readModelFromFile(f));
			String fileName = f.getName();
			BatchFileGenerator.substituteDataObjectAndWriteNewModels(modelInstance, fileName, directory.getAbsolutePath());

		}
		

	}

	public static LinkedList<File> getRandomModelsFromSourceFolder(int amountRandomModels, String pathToSourceFolder) {
		File folder = new File(pathToSourceFolder);
		LinkedList<File> listOfFiles = new LinkedList<File>();
		listOfFiles.addAll(Arrays.asList(folder.listFiles()));

		LinkedList<File> randomModelsFromSourceFolder = new LinkedList<File>();
		for (int i = 0; i < amountRandomModels; i++) {
			File randomProcessModelFile = CommonFunctionality.getRandomItem(listOfFiles);
			randomModelsFromSourceFolder.add(randomProcessModelFile);
			listOfFiles.remove(randomProcessModelFile);
		}

		return randomModelsFromSourceFolder;
	}
	
	
	public static LinkedList<File> getModelsInOrderFromSourceFolder(int amountModels, String pathToSourceFolder) {
		File folder = new File(pathToSourceFolder);
		LinkedList<File> listOfFiles = new LinkedList<File>();
		listOfFiles.addAll(Arrays.asList(folder.listFiles()));

		LinkedList<File> modelsFromSourceFolder = new LinkedList<File>();
		for (int i = 0; i < amountModels; i++) {
			File model = listOfFiles.get(i);
			if(!model.getName().contains(".csv")) {
			modelsFromSourceFolder.add(model);
			}
		}

		return modelsFromSourceFolder;
	}

	public static LinkedList<File> getAllModelsFromFolder(String pathToSourceFolder) {
		File folder = new File(pathToSourceFolder);
		LinkedList<File> listOfFiles = new LinkedList<File>();
		listOfFiles.addAll(Arrays.asList(folder.listFiles()));

		LinkedList<File> modelsFromSourceFolder = new LinkedList<File>();
		for (int i = 0; i < listOfFiles.size(); i++) {
			File model = listOfFiles.get(i);
			if(!model.getName().contains(".csv")) {
			modelsFromSourceFolder.add(model);
			}
		}

		return modelsFromSourceFolder;
	}

	
	

	public static void performTradeOffTest(String processType, LinkedList<File> processes, String pathToDestinationFolderForStoringModels,
			List<Integer> dataObjectBounds, int upperBoundSolutionsForLocalMinWithBound, int boundForComparisons, int amountThreadPools) {

		LinkedList<String> emptySphere = new LinkedList<String>();
		emptySphere.add("");

		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		for (int i = 0; i < processes.size(); i++) {
			String pathToRandomProcess = processes.get(i).getAbsolutePath();
			BpmnModelInstance processModel = Bpmn.readModelFromFile(processes.get(i));
			int amountTasks = processModel.getModelElementsByType(Task.class).size();
			File newModel = null;
			boolean modelIsValid = false;
			int amountRandomCountDataObjectsToCreate = 0;
			while (modelIsValid == false) {
				try {
					ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess,
							pathToDestinationFolderForStoringModels, "");

					// randomly generate dataObjects in the range [DataObjects, maxCountDataObjects]
					amountRandomCountDataObjectsToCreate = ThreadLocalRandom.current().nextInt(dataObjectBounds.get(0),
							dataObjectBounds.get(1) + 1);

					// create model with amountRandomCountDataObjects dataObjects, 0 participant
					// needed for voting and empty string as starting sphere
					pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, emptySphere);
					pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(amountRandomCountDataObjectsToCreate,
							amountRandomCountDataObjectsToCreate, 0, 0, 0, false);
					newModel = pModel.writeChangesToFileWithoutCorrectnessCheck();
					modelIsValid = true;
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}

			}

			for (int writerClass = 0; writerClass < percentageOfWritersClasses.size(); writerClass++) {
				// for each model -> annotate it with small, medium, large amount of writers
				BpmnModelInstance newModelInstance = Bpmn.readModelFromFile(newModel);

				int minAmountWriters = newModelInstance.getModelElementsByType(DataObjectReference.class).size();

				int amountWriterTasksInModel = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasks,
						percentageOfWritersClasses.get(writerClass), minAmountWriters);

				for (int readerClass = 0; readerClass < percentageOfReadersClasses.size(); readerClass++) {
					// for each model -> annotate it with small, medium, large amount of readers
					int amountReaderTasksInModel = CommonFunctionality.getAmountFromPercentage(amountTasks,
							percentageOfReadersClasses.get(readerClass));
					StringBuilder suffixBuilder = new StringBuilder();

					if (writerClass == 0) {
						suffixBuilder.append("sW");
					} else if (writerClass == 1) {
						suffixBuilder.append("mW");
					} else if (writerClass == 2) {
						suffixBuilder.append("lW");
					}

					if (readerClass == 0) {
						suffixBuilder.append("sR");
					} else if (readerClass == 1) {
						suffixBuilder.append("mR");
					} else if (readerClass == 2) {
						suffixBuilder.append("lR");
					}

					ProcessModelAnnotater modelWithReadersAndWriters;
					try {
						modelWithReadersAndWriters = new ProcessModelAnnotater(newModel.getAbsolutePath(),
								pathToDestinationFolderForStoringModels, suffixBuilder.toString(), emptySphere, 0,
								amountWriterTasksInModel, amountReaderTasksInModel, 0, 1,
								amountRandomCountDataObjectsToCreate, defaultNamesSeqFlowsXorSplits);
						Future<File> future = executor.submit(modelWithReadersAndWriters);

						try {
							future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							future.cancel(true);
							e.printStackTrace();
							readerClass--;
						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							System.err.println(e.getMessage());
							future.cancel(true);
							readerClass--;
						} catch (TimeoutException e) {
							// TODO Auto-generated catch block
							future.cancel(true);
							e.printStackTrace();
						}

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						readerClass--;
					}

				}
			}

		}
		executor.shutdownNow();
		// for each model that has been generated with 0 participants per decision and
		// empty sphere annotated for each dataObject
		// -> generate new ones where voters is increased by 1 on each model till
		// globalSphereSize is reached
		// -> generate new ones where privity requirement is increased to next stricter
		// sphere till Strong-Dynamic is reached on all dataObjects

		String pathToFolderWithModelsForEvaluation = CommonFunctionality
				.fileWithDirectoryAssurance(pathToDestinationFolderForStoringModels, "ModelsForEvaluation")
				.getAbsolutePath();
		File directory = new File(pathToDestinationFolderForStoringModels);
		File[] directoryList = directory.listFiles();

		for (int modelIndex = 0; modelIndex < directoryList.length; modelIndex++) {
			File annotatedModel = directoryList[modelIndex];
			try {
				BpmnModelInstance currModel = Bpmn.readModelFromFile(annotatedModel);

				int globalSphereSize = CommonFunctionality.getGlobalSphere(currModel, false);

				if (annotatedModel.getName().contains("sWsR") || annotatedModel.getName().contains("sWmR")
						|| annotatedModel.getName().contains("sWlR") || annotatedModel.getName().contains("mWsR")
						|| annotatedModel.getName().contains("mWmR") || annotatedModel.getName().contains("mWlR")
						|| annotatedModel.getName().contains("lWsR") || annotatedModel.getName().contains("lWmR")
						|| annotatedModel.getName().contains("lWlR")) {

					for (int spheresIndex = 0; spheresIndex < defaultSpheres.size(); spheresIndex++) {
						String currentSphere = defaultSpheres.get(spheresIndex);
						CommonFunctionality.increaseSpherePerDataObject(currModel, currentSphere);

						for (int votersAmountIndex = 1; votersAmountIndex <= globalSphereSize; votersAmountIndex++) {
							String suffix = "_" + currentSphere + "_voters" + votersAmountIndex;
							CommonFunctionality.increaseVotersPerDataObject(currModel, votersAmountIndex);
							String modelName = annotatedModel.getName().substring(0,
									annotatedModel.getName().indexOf(".bpmn"));
							if(!CommonFunctionality.isCorrectModel(currModel)) {
								votersAmountIndex--;
							}
							
							CommonFunctionality.writeChangesToFile(currModel, modelName,
									pathToFolderWithModelsForEvaluation, suffix);

						}

					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		// perform the algorithms and generate csv file
		ExecutorService service = Executors.newFixedThreadPool(amountThreadPools);
		File file = BatchFileGenerator.createNewCSVFile(pathToFolderWithModelsForEvaluation, "resultsOfTest2"+processType);
		ResultsToCSVWriter writer = new ResultsToCSVWriter(file);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathToFolderWithModelsForEvaluation, upperBoundSolutionsForLocalMinWithBound, boundForComparisons, writer, service);
		service.shutdownNow();
		writer.writeRowsToCSVAndcloseWriter();

	}
	
	public static void performBoundaryTest1(int amountProcessesPerIteration, int amountDecisionsToStart, int votersPerDecision, int globalSphere,int upperBoundSolutionsForLocalMinWithBound, int amountTasksToStartWith, int tasksFactor, int lowerBoundAmountParallelGtws, int upperBoundAmountParallelGtws, int amountWritersInPercent, int amountReadersInPercent, int minDataObjectsPerDecision, int maxDataObjectsPerDecision, LinkedList<String>sphereList, int amountThreads, String pathToFolderForModelsForBoundaryTest) {

		int amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws, upperBoundAmountParallelGtws+1);
	
		File csvFile = BatchFileGenerator.createNewCSVFile(pathToFolderForModelsForBoundaryTest,
				"boundaryTestResults");
		
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		HashMap<String, Integer>timeOutMap = new HashMap<String, Integer>();
		
		String localMinWithBound = "localMinWithBound"+upperBoundSolutionsForLocalMinWithBound;
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		do {
			timeOutMap.put("bruteForce", 0);
			timeOutMap.put("localMin",0);
			timeOutMap.put(localMinWithBound,0);
			
			int amountDataObjectsToCreate = amountDecisionsToStart;
			System.out.println("Generate models with " + amountDecisionsToStart + " decisions!");

			String folderName = "BoundaryTest_decision-" + amountDecisionsToStart;
			String pathToFolderForModelsWithDecisions = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsForBoundaryTest, folderName).getAbsolutePath();

			int amountTasksWithFactor = amountTasksToStartWith + (amountDecisionsToStart * tasksFactor);
			amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
					upperBoundAmountParallelGtws + 1);
			int amountWriters = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasksWithFactor,
					amountWritersInPercent, amountDataObjectsToCreate);
			int amountReaders = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasksWithFactor,
					amountReadersInPercent, amountDataObjectsToCreate);

			// generate processes
			BatchFileGenerator.generateRandomProcessesWithFixedValues(pathToFolderForModelsWithDecisions, globalSphere,
					amountTasksWithFactor, amountDecisionsToStart, amountParallelGtws, amountProcessesPerIteration, executor);

			// annotate the processes
			File folder = new File(pathToFolderForModelsWithDecisions);
			LinkedList<File> listOfFiles = new LinkedList<File>();
			listOfFiles.addAll(Arrays.asList(folder.listFiles()));
			
			amountProcessesPerIteration = listOfFiles.size();

			String pathToFolderForModelsWithDecisionsAnnotated = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsWithDecisions, "annotated").getAbsolutePath();
			Iterator<File> modelIter = listOfFiles.iterator();
			while (modelIter.hasNext()) { // annotate the model
				File model = modelIter.next();
				ProcessModelAnnotater p;
				try {
					p = new ProcessModelAnnotater(model.getAbsolutePath(),
							pathToFolderForModelsWithDecisionsAnnotated, "_annotated");
					p.addNamesForOutgoingFlowsOfXorSplits(defaultNamesSeqFlowsXorSplits);
					p.generateDataObjects(amountDataObjectsToCreate, defaultSpheres);
					p.annotateModelWithFixedAmountOfReadersAndWriters(amountWriters, amountReaders, 0, defaultSpheres);
					p.connectDataObjectsToBrtsAndTuplesForXorSplits(minDataObjectsPerDecision, maxDataObjectsPerDecision,
							votersPerDecision, votersPerDecision, 0, true);

					Future<File> future = executor.submit(p);
					try {
						future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);

					} catch (Exception e) {
						System.err.println(e.getMessage());
						future.cancel(true);
						modelIter.remove();
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				
			}

			// map annotated models
			LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderForModelsWithDecisionsAnnotated);

			// perform all algorithms and count the timeouts

			for (API api : apiList) {			
			timeOutMap = BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api, upperBoundSolutionsForLocalMinWithBound, boundForComparisons, timeOutMap, writer, executor);
			}
						
			System.out.println("Iteration" + amountDecisionsToStart + " end - timeOutsBruteForce: " + timeOutMap.get("bruteForce")
					+ ", timeOutsLocalMin: " + timeOutMap.get("localMin") );
			amountDecisionsToStart++;			
			
		} while (timeOutMap.get("bruteForce") < amountProcessesPerIteration || timeOutMap.get("localMin") < amountProcessesPerIteration ||timeOutMap.get(localMinWithBound)<amountProcessesPerIteration);

		executor.shutdownNow();

		writer.writeRowsToCSVAndcloseWriter();
		
		System.out.println("FINISHED BOUNDARY TEST 1!");
	
	}
	
	
	public static void performBoundaryTest2(File file,int globalSphereLowerBound, int amountNewProcessesToCreatePerIteration, int upperBoundSolutionsForLocalMin, int boundForComparisons, int amountThreadPools, String directoryToStore) {
		
		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStore,
				"boundaryTest2Results");
		
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		
		
		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		CommonFunctionality.generateNewModelsUntilGlobalSphereReached(file, globalSphereLowerBound, amountNewProcessesToCreatePerIteration, directoryToStore);
		
		
		
		
		String localMinWithBound = "localMinWithBound"+upperBoundSolutionsForLocalMin;
		// map annotated models
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(directoryToStore);

		// perform all algorithms and count the timeouts

		for (API api : apiList) {	
			HashMap<String, Integer>timeOutMap = new HashMap<String, Integer>();

			timeOutMap.put("bruteForce", 0);
			timeOutMap.put("localMin",0);
			timeOutMap.put(localMinWithBound,0);
					
		timeOutMap = BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api, upperBoundSolutionsForLocalMin, boundForComparisons, timeOutMap, writer, executor);
			
		
	
		System.out.println("timeOutsBruteForce: " + timeOutMap.get("bruteForce")
				+ ", timeOutsLocalMin: " + timeOutMap.get("localMin") +", timeOuts"+localMinWithBound+": "+timeOutMap.get(localMinWithBound) );
		
		if(timeOutMap.get("bruteForce") == 1 && timeOutMap.get("localMin") ==1 && timeOutMap.get(localMinWithBound)==1) {
			break;
		}
		}
	executor.shutdownNow();

	writer.writeRowsToCSVAndcloseWriter();
	
	System.out.println("FINISHED BOUNDARY TEST 2!");

	}
	
	public static void substituteDataObjectAndWriteNewModels(BpmnModelInstance modelInstance, String modelName, String directoryToStore) {
		
		LinkedList<DataObjectReference>daoList = new LinkedList<DataObjectReference>();
		daoList.addAll(modelInstance.getModelElementsByType(DataObjectReference.class));
		DataObjectReference substitute = CommonFunctionality.getRandomItem(daoList);
	
		
		CommonFunctionality.substituteOneDataObjectPerIterationAndWriteNewModels(modelInstance, substitute, modelName, directoryToStore);
		
		
	}
	
	
	public static void performTestWithMaxPossibleConstraints(LinkedList<File>models,boolean modelWithLanes, String directoryToStoreNewModels, int amountThreads) {
		int probabilityForGatewayToHaveConstraint = 100;
		
		for(File file: models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);
			
			try {
				//will always be max. constrained
				CommonFunctionality.addExcludeParticipantConstraintsOnModel(clone, fileName, probabilityForGatewayToHaveConstraint, 1, true, modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels,
				"alwaysMaxConstrainedResults");
		
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File>modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, 1, probabilityForGatewayToHaveConstraint, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();
		
	}
	
	public static void performTestWithConstraintsProbability(LinkedList<File>models,boolean modelWithLanes, int probabilityForGatewayToHaveConstraint, String directoryToStoreNewModels, int amountThreads) {
		
		for(File file: models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);
	
			try {
				CommonFunctionality.addExcludeParticipantConstraintsOnModel(clone, fileName, probabilityForGatewayToHaveConstraint, 1,  false, modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels,
				"constraintProb"+probabilityForGatewayToHaveConstraint);
		
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File>modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, 1, probabilityForGatewayToHaveConstraint, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();
		
	}
	
	
	
	public static LinkedList<File> getFilesFromFolders(LinkedList<File>smallProcesses, LinkedList<File>mediumProcesses, LinkedList<File>largeProcesses, List<Integer>amountModels){
		LinkedList<File> filesList = new LinkedList<File>();
		try {
			
			for(int i = 0; i < amountModels.get(0); i++) {
				filesList.add(smallProcesses.get(i));				
			}
			
			for(int j = 0; j < amountModels.get(1); j++) {
				filesList.add(mediumProcesses.get(j));				
			}
			
			for(int k = 0; k < amountModels.get(2); k++) {
				filesList.add(largeProcesses.get(k));
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return filesList;
	}

}
