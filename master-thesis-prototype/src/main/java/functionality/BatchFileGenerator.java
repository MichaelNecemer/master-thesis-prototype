package functionality;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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
import org.camunda.bpm.model.bpmn.instance.Task;

import Mapping.ProcessInstanceWithVoters;
import ProcessModelGeneratorAndAnnotater.ProcessGenerator;
import ProcessModelGeneratorAndAnnotater.ProcessModelAnnotater;

public class BatchFileGenerator {

	static int idCSVFile = 1;
	// static String root = System.getProperty("user.home") + "/Desktop";
	static String root = System.getProperty("user.home") + "/Onedrive/Desktop";

	static int timeOutForProcessGeneratorInMin = 5;
	static int timeOutForProcessModelAnnotaterInMin = 10;
	// API is the class where the computations will be done
	static int timeOutForApiInMin = 3;

	// how often will the modelAnnotater try to annotate the model if it fails
	static int triesForModelAnnotater = 5;

	// bound for the bounded localMinAlgorithm
	static int upperBoundLocalMinWithBound = 1;

	static int amountThreads = 1;

	// set the upper bound where outcomes of algorithms will be compared one by one
	// if the amount solutions is > boundForComparisons -> only the cost of the
	// cheapest solutions of the algorithms will be compared
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

	public static void main(String[] args) throws Exception {
		String pathToRootFolder = CommonFunctionality.fileWithDirectoryAssurance(root, "EvaluationSetup")
				.getAbsolutePath();

		LinkedList<String> methodsToRun = new LinkedList<String>();
		String test1_1ToRun = "Test1_1ToRun";
		String test1_2ToRun = "Test1_2ToRun";
		String createRandomProcesses = "createRandomProcesses";
		String test2ToRun = "Test2ToRun";
		String test3ToRun = "Test3ToRun";
		String test4_1ToRun = "Test4_1ToRun";
		String test4_2ToRun = "Test4_2ToRun";
		String test5ToRun = "Test5ToRun";
		String test6ToRun = "Test6ToRun";

		methodsToRun.add(test1_1ToRun);
		methodsToRun.add(test1_2ToRun);
		methodsToRun.add(createRandomProcesses);
		methodsToRun.add(test2ToRun);
		methodsToRun.add(test3ToRun);
		methodsToRun.add(test4_1ToRun);
		methodsToRun.add(test4_2ToRun);
		methodsToRun.add(test5ToRun);
		methodsToRun.add(test6ToRun);

		String pathToFolderForModelsForTest1_1 = "";
		String pathToSmallProcessesFolderWithoutAnnotation = "";
		String pathToMediumProcessesFolderWithoutAnnotation = "";
		String pathToLargeProcessesFolderWithoutAnnotation = "";
		String pathToSmallProcessesForTest2WithAnnotation = "";
		String pathToMediumProcessesForTest2WithAnnotation = "";
		String pathToLargeProcessesForTest2WithAnnotation = "";

		File rootFolderDirectory = new File(pathToRootFolder);
		for (File subFile : rootFolderDirectory.listFiles(File::isDirectory)) {
			String subFileName = subFile.getName();
			if (subFileName.contentEquals("Test1_1-BoundaryTest1")) {
				pathToFolderForModelsForTest1_1 = subFile.getAbsolutePath();
			} else if (subFileName.contentEquals("ProcessesWithoutAnnotation")) {
				for (File processesWithoutAnnotation : subFile.listFiles(File::isDirectory)) {
					String processesWithoutAnnotationFolderName = processesWithoutAnnotation.getName();
					if (processesWithoutAnnotationFolderName.contentEquals("SmallProcessesFolder")) {
						pathToSmallProcessesFolderWithoutAnnotation = processesWithoutAnnotation.getAbsolutePath();
					} else if (processesWithoutAnnotationFolderName.contentEquals("MediumProcessesFolder")) {
						pathToMediumProcessesFolderWithoutAnnotation = processesWithoutAnnotation.getAbsolutePath();
					} else if (processesWithoutAnnotationFolderName.contentEquals("LargeProcessesFolder")) {
						pathToLargeProcessesFolderWithoutAnnotation = processesWithoutAnnotation.getAbsolutePath();
					}
				}

			} else if (subFileName.contentEquals("Test2-TradeOff")) {
				for (File processesWithAnnotation : subFile.listFiles(File::isDirectory)) {
					String processesWithAnnotationFolderName = processesWithAnnotation.getName();
					if (processesWithAnnotationFolderName.contentEquals("SmallProcessesAnnotatedFolder")) {
						pathToSmallProcessesForTest2WithAnnotation = processesWithAnnotation.getAbsolutePath();
					} else if (processesWithAnnotationFolderName.contentEquals("MediumProcessesAnnotatedFolder")) {
						pathToMediumProcessesForTest2WithAnnotation = processesWithAnnotation.getAbsolutePath();
					} else if (processesWithAnnotationFolderName.contentEquals("LargeProcessesAnnotatedFolder")) {
						pathToLargeProcessesForTest2WithAnnotation = processesWithAnnotation.getAbsolutePath();
					}
				}
			}
		}

		if (methodsToRun.contains(test1_1ToRun)) {
			// Test 1 - Boundary Test 1
			// Test with 1.1 - 1 unique dataObject per decision
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
			// do that while there are not timeouts on all processes for the algorithms

			int minDataObjectsPerDecision = 1;
			int maxDataObjectsPerDecision = 1;
			pathToFolderForModelsForTest1_1 = CommonFunctionality
					.fileWithDirectoryAssurance(pathToRootFolder, "Test1_1-BoundaryTest1").getAbsolutePath();
			int votersPerDecision = 3;
			int globalSphere = 5;
			// the amount of possible combinations of voters for the process will be
			// increased by
			// binom(5,3) -> 10 per decision
			// e.g. with 0 decisions = 0, 1 decision = 10, 2 decisions = 100, ...
			// since the cost of the models will have to be calculated for all the possible
			// combinations
			// this can be used to estimate the feasibility for other models

			BatchFileGenerator.performBoundaryTest1_1(10, 0, votersPerDecision, globalSphere,
					upperBoundLocalMinWithBound, 6, 4, 0, 0, percentageOfWritersClasses.get(1),
					percentageOfReadersClasses.get(1), minDataObjectsPerDecision, maxDataObjectsPerDecision,
					defaultSpheres, amountThreads, pathToFolderForModelsForTest1_1);

			System.out.println("BoundartyTest1_1 finished!");
		}

		if (methodsToRun.contains(test1_2ToRun)) {
			// Test 1.2 - Boundary Test 2
			// choose a model from boundary test 1 that had no exceptions for all algorithms
			// create x new models on each iteration till every task of the model has a
			// different participant connected
			// start with the globalSphereLowerBound e.g. 2 -> x models where each task has
			// one of the 2 participants connected
			// end point -> x models where each task has a different participant connected

			if (!pathToFolderForModelsForTest1_1.isEmpty()) {

				String pathToFolderForModelsForTest1_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test1_2-BoundaryTest2").getAbsolutePath();

				// choose a model
				File directoryOfFiles = new File(pathToFolderForModelsForTest1_1 + File.separatorChar
						+ "BoundaryTest_decision-4" + File.separatorChar + "annotated");
				List<File> listOfFiles = Arrays.asList(directoryOfFiles.listFiles());
				File model = CommonFunctionality.getRandomItem(listOfFiles);
				int newModelsPerIteration = 5;
				int votersPerDecision = 3;
				BatchFileGenerator.performBoundaryTest1_2(model, votersPerDecision, newModelsPerIteration,
						upperBoundLocalMinWithBound, boundForComparisons, amountThreads,
						pathToFolderForModelsForTest1_2);
				System.out.println("BoundartyTest1_2 finished!");
			} else {
				System.out.println(test1_2ToRun + " not performed! Run Test1_1 first!");
			}
		}

		if (methodsToRun.contains(createRandomProcesses)) {

			// generate 3 Classes -> small, medium, large processes (without annotation)
			// put them into a new folder into the root
			String pathToFolderForModelsWithoutAnnotation = CommonFunctionality
					.fileWithDirectoryAssurance(pathToRootFolder, "ProcessesWithoutAnnotation").getAbsolutePath();

			pathToSmallProcessesFolderWithoutAnnotation = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "SmallProcessesFolder")
					.getAbsolutePath();
			pathToMediumProcessesFolderWithoutAnnotation = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "MediumProcessesFolder")
					.getAbsolutePath();
			pathToLargeProcessesFolderWithoutAnnotation = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "LargeProcessesFolder")
					.getAbsolutePath();

			// for each processes folder -> generate 100 random processes
			// small processes: 2-3 participants, 6-15 tasks, 1-2 xors, 0-2 parallels, 100
			// processes

			ExecutorService randomProcessGeneratorService = Executors.newFixedThreadPool(amountThreads);

			BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToSmallProcessesFolderWithoutAnnotation, 2,
					3, 6, 15, 1, 2, 0, 2, 100, randomProcessGeneratorService);

			// medium processes: 3-4 participants, 16-30 tasks, 3-4 xors, 0-3 parallels, 100
			// processes
			BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToMediumProcessesFolderWithoutAnnotation, 3,
					4, 16, 30, 3, 4, 0, 3, 100, randomProcessGeneratorService);

			// large processes: 4-5 participants, 31-60 tasks, 5-6 xors, 0-4, parallels, 100
			// processes
			BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolderWithoutAnnotation, 4,
					5, 31, 60, 5, 6, 0, 4, 100, randomProcessGeneratorService);
			randomProcessGeneratorService.shutdownNow();
			System.out.println("All random processes generated!");
		}

		if (methodsToRun.contains(test2ToRun)) {
			// Test 2 - Measure impact of enforceability on privity and vice versa
			// take x random models from small, medium and large processes
			// add dataObjects with global sphere and 1 voter per decision
			// add readers/writers combinations - generate new models (9 new models for //
			// each)
			// increase the amount of voters needed for decisions till the global sphere
			// of that process is reached on all xors
			// increase privity requirements to next stricter sphere for all dataObjects

			if (pathToSmallProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToMediumProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToLargeProcessesFolderWithoutAnnotation.isEmpty()) {
				System.out.println(test2ToRun + " not performed! Generate random processes first!");
			} else {
				String pathToFolderForModelsForTest2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test2-TradeOff").getAbsolutePath();
				int modelsToTakeFromEachFolder = 10;
				pathToSmallProcessesForTest2WithAnnotation = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "SmallProcessesAnnotatedFolder")
						.getAbsolutePath();
				pathToMediumProcessesForTest2WithAnnotation = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "MediumProcessesAnnotatedFolder")
						.getAbsolutePath();
				pathToLargeProcessesForTest2WithAnnotation = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "LargeProcessesAnnotatedFolder")
						.getAbsolutePath();

				LinkedList<File> smallProcessesWithoutAnnotation = BatchFileGenerator.getModelsInOrderFromSourceFolder(
						modelsToTakeFromEachFolder, pathToSmallProcessesFolderWithoutAnnotation);
				LinkedList<File> mediumProcessesWithoutAnnotation = BatchFileGenerator.getModelsInOrderFromSourceFolder(
						modelsToTakeFromEachFolder, pathToMediumProcessesFolderWithoutAnnotation);
				LinkedList<File> largeProcessesWithoutAnnotation = BatchFileGenerator.getModelsInOrderFromSourceFolder(
						modelsToTakeFromEachFolder, pathToLargeProcessesFolderWithoutAnnotation);

				BatchFileGenerator.performTradeOffTest("small", smallProcessesWithoutAnnotation,
						pathToSmallProcessesForTest2WithAnnotation, dataObjectBoundsSmallProcesses,
						upperBoundLocalMinWithBound, boundForComparisons, amountThreads);
				BatchFileGenerator.performTradeOffTest("medium", mediumProcessesWithoutAnnotation,
						pathToMediumProcessesForTest2WithAnnotation, dataObjectBoundsMediumProcesses,
						upperBoundLocalMinWithBound, boundForComparisons, amountThreads);
				BatchFileGenerator.performTradeOffTest("large", largeProcessesWithoutAnnotation,
						pathToLargeProcessesForTest2WithAnnotation, dataObjectBoundsLargeProcesses,
						upperBoundLocalMinWithBound, boundForComparisons, amountThreads);
				System.out.println("Test 2 finished!");
			}
		}

		if (methodsToRun.contains(test3ToRun)) {
			// Test 3 - Measure impact of dataObjects
			// annotate unique gateways per decision e.g. 5 unique data objects and 5
			// decisions in the model
			// -> 25 unique data objects
			// take one dataObject, loop through the others and replace one object with that
			// dataObject in each iteration
			// on last step each decision will only have that dataObject connected
			// performed on small and medium processes

			if (pathToSmallProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToMediumProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToLargeProcessesFolderWithoutAnnotation.isEmpty()) {
				System.out.println(test3ToRun + " not performed! Generate random processes first!");
			} else {
				String pathToFolderForModelsForDataObjectTest = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test3-ImpactOfDataObjects").getAbsolutePath();

				int modelsToTakeFromEachFolderForTest3 = 15;

				LinkedList<File> smallProcessesWithoutAnnotationTest3 = BatchFileGenerator
						.getModelsInOrderFromSourceFolder(modelsToTakeFromEachFolderForTest3,
								pathToSmallProcessesFolderWithoutAnnotation);
				LinkedList<File> mediumProcessesWithoutAnnotationTest3 = BatchFileGenerator
						.getModelsInOrderFromSourceFolder(modelsToTakeFromEachFolderForTest3,
								pathToMediumProcessesFolderWithoutAnnotation);
				// LinkedList<File> largeProcessesWithoutAnnotationTest3 = BatchFileGenerator
				// .getModelsInOrderFromSourceFolder(modelsToTakeFromEachFolderForTest3,
				// pathToLargeProcessesFolderWithoutAnnotation);
				LinkedList<File> allProcessesWithoutAnnotationTest3 = new LinkedList<File>();
				allProcessesWithoutAnnotationTest3.addAll(smallProcessesWithoutAnnotationTest3);
				allProcessesWithoutAnnotationTest3.addAll(mediumProcessesWithoutAnnotationTest3);
				// allProcessesWithoutAnnotationTest3.addAll(largeProcessesWithoutAnnotationTest3);

				// annotate models with same amount of unique data objects per decision
				int amountUniqueDataObjectsPerDecision = 4;

				BatchFileGenerator.performDataObjectTest(allProcessesWithoutAnnotationTest3,
						pathToFolderForModelsForDataObjectTest, amountUniqueDataObjectsPerDecision,
						upperBoundLocalMinWithBound, amountThreads);
				System.out.println("Test 3 finished!");
			}
		}

		if (methodsToRun.contains(test4_1ToRun)) {
			// Test 4_1 "killer constraints"
			// after that compare it to test 2 where those models have been run without
			// constraints
			if (pathToSmallProcessesForTest2WithAnnotation.isEmpty()
					&& pathToMediumProcessesForTest2WithAnnotation.isEmpty()
					&& pathToLargeProcessesForTest2WithAnnotation.isEmpty()) {
				System.out.println(test4_1ToRun + " not performed! Run test 2 first!");
			} else {
				boolean decisionTakerExcludeable = false;
				String pathToSmallProcessesWithAnnotation = pathToSmallProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> smallProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToSmallProcessesWithAnnotation);
				String pathToMediumProcessesWithAnnotation = pathToMediumProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> mediumProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToMediumProcessesWithAnnotation);
				String pathToLargeProcessesWithAnnotation = pathToLargeProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> largeProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToLargeProcessesWithAnnotation);

				String pathToFolderForModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test4_1-KillerConstraints").getAbsolutePath();
				String pathToFolderForSmallModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "SmallModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(smallProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, pathToFolderForSmallModelsForTest4_1,
						upperBoundLocalMinWithBound, amountThreads);

				String pathToFolderForMediumModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(mediumProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, pathToFolderForMediumModelsForTest4_1,
						upperBoundLocalMinWithBound, amountThreads);

				String pathToFolderForLargeModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(largeProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, pathToFolderForLargeModelsForTest4_1,
						upperBoundLocalMinWithBound, amountThreads);

				System.out.println("Test 4_1 finished!");
			}
		}

		if (methodsToRun.contains(test4_2ToRun)) {
			// Test 4_2 -> Processes with probability to have exclude constraints
			// performed on small and medium processes

			if (pathToSmallProcessesForTest2WithAnnotation.isEmpty()
					&& pathToMediumProcessesForTest2WithAnnotation.isEmpty()
					&& pathToLargeProcessesForTest2WithAnnotation.isEmpty()) {
				System.out.println(test4_2ToRun + " not performed! Run test 2 first!");
			} else {
				int probabilityForGatewayToHaveConstraint = 30;
				boolean decisionTakerExcludeable = false;

				String pathToFolderForModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder,
								"Test4_2-constraintProbability" + probabilityForGatewayToHaveConstraint)
						.getAbsolutePath();

				String pathToSmallProcessesWithAnnotation = pathToSmallProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> smallProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToSmallProcessesWithAnnotation);
				String pathToMediumProcessesWithAnnotation = pathToMediumProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> mediumProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToMediumProcessesWithAnnotation);
				String pathToLargeProcessesWithAnnotation = pathToLargeProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> largeProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToLargeProcessesWithAnnotation);

				String pathToFolderForSmallModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "SmallModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(smallProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, probabilityForGatewayToHaveConstraint,
						pathToFolderForSmallModelsForTest4_2, upperBoundLocalMinWithBound, amountThreads);

				String pathToFolderForMediumModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(mediumProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, probabilityForGatewayToHaveConstraint,
						pathToFolderForMediumModelsForTest4_2, upperBoundLocalMinWithBound, amountThreads);

				String pathToFolderForLargeModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(largeProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, probabilityForGatewayToHaveConstraint,
						pathToFolderForLargeModelsForTest4_2, upperBoundLocalMinWithBound, amountThreads);

				System.out.println("Test 4_2 finished!");
			}
		}

		if (methodsToRun.contains(test5ToRun)) {
			// Test 5 - "mandatory participants"
			// Search for the best set of verifiers
			// take all models from trade off test

			if (pathToSmallProcessesForTest2WithAnnotation.isEmpty()
					&& pathToMediumProcessesForTest2WithAnnotation.isEmpty()
					&& pathToLargeProcessesForTest2WithAnnotation.isEmpty()) {
				System.out.println(test5ToRun + " not performed! Run test 2 first!");

			} else {
				String pathToSmallProcessesWithAnnotation = pathToSmallProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> smallProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToSmallProcessesWithAnnotation);
				String pathToMediumProcessesWithAnnotation = pathToMediumProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";
				LinkedList<File> mediumProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToMediumProcessesWithAnnotation);
				String pathToLargeProcessesWithAnnotation = pathToLargeProcessesForTest2WithAnnotation
						+ File.separatorChar + "ModelsForEvaluation";	
				LinkedList<File> largeProcessesFromTradeOffTest = BatchFileGenerator
						.getAllModelsFromFolder(pathToLargeProcessesWithAnnotation);
				
				String pathToFolderForModelsForTest5 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test5_SearchForBestVerifiers").getAbsolutePath();

				String pathToFolderForSmallModelsForTest5= CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "SmallModels").getAbsolutePath();
				
				String pathToFolderForMediumModelsForTest5= CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "MediumModels").getAbsolutePath();
				
				String pathToFolderForLargeModelsForTest5= CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "LargeModels").getAbsolutePath();
												
				BatchFileGenerator.performTestWithSearchForSetOfBestVerifiers(smallProcessesFromTradeOffTest, false,
						pathToFolderForSmallModelsForTest5, upperBoundLocalMinWithBound, amountThreads);
				
				BatchFileGenerator.performTestWithSearchForSetOfBestVerifiers(mediumProcessesFromTradeOffTest, false,
						pathToFolderForMediumModelsForTest5, upperBoundLocalMinWithBound, amountThreads);
				
				BatchFileGenerator.performTestWithSearchForSetOfBestVerifiers(largeProcessesFromTradeOffTest, false,
						pathToFolderForLargeModelsForTest5, upperBoundLocalMinWithBound, amountThreads);
				
				
				System.out.println("Test 5 finished!");

			}
		}

		if (methodsToRun.contains(test6ToRun)) {
			// Test 6 - "real world processes"
			// may contain exclude and mandatory participant constraint
			// may contain dynamic writers
			// has wider ranges for variables

			List<Integer> dataObjectBoundsRealWorld = Arrays.asList(1, 6);
			int dynamicWriterProb = 30;
			int upperBoundParticipants = 8;
			int lowerBoundTasks = 8;
			int upperBoundTasks = 80;
			int upperBoundXorGtws = 10;
			int upperBoundParallelGtws = 6;
			int amountProcesses = 3;
			int minDataObjectsPerDecisionTest6 = dataObjectBoundsRealWorld.get(0);
			int maxDataObjectsPerDecisionTest6 = dataObjectBoundsRealWorld.get(1);
			int probabilityForGatewayToHaveExclConstraint = 30;
			int lowerBoundAmountParticipantsToExclude = 0;
			// upperBoundAmountParticipantsToExclude will be set inside the method
			// accordingly
			int upperBoundAmountParticipantsToExclude = -1;
			boolean decisionTakerExcludeableTest6 = true;
			boolean alwaysMaxExclConstrained = false;
			int probabilityForGatewayToHaveMandConstraint = 30;
			int lowerBoundAmountParticipantsToBeMandatory = 0;
			// upperBoundAmountParticipantsToBeMandatory will be set inside the method
			// accordingly
			int upperBoundAmountParticipantsToBeMandatory = -1;
			boolean decisionTakerMandatory = false;
			boolean alwaysMaxMandConstrained = false;
			List<Integer> writersOfProcessInPercent = Arrays.asList(10, 20, 30);
			List<Integer> readersOfProcessInPercent = Arrays.asList(10, 20, 30);
			int upperBoundLocalMinWithBound = 1;

			String pathToRealWorldProcesses = CommonFunctionality
					.fileWithDirectoryAssurance(pathToRootFolder, "Test6-RealWorldProcesses").getAbsolutePath();
			String pathToAnnotatedProcessesFolder = CommonFunctionality
					.fileWithDirectoryAssurance(pathToRealWorldProcesses, "AnnotatedModels").getAbsolutePath();

			BatchFileGenerator.performTestWithRealWorldProcesses(pathToRealWorldProcesses,
					pathToAnnotatedProcessesFolder, dynamicWriterProb, upperBoundParticipants, lowerBoundTasks,
					upperBoundTasks, upperBoundXorGtws, upperBoundParallelGtws, amountProcesses,
					minDataObjectsPerDecisionTest6, maxDataObjectsPerDecisionTest6, dataObjectBoundsRealWorld,
					writersOfProcessInPercent, readersOfProcessInPercent, probabilityForGatewayToHaveExclConstraint,
					lowerBoundAmountParticipantsToExclude, upperBoundAmountParticipantsToExclude,
					decisionTakerExcludeableTest6, alwaysMaxExclConstrained, probabilityForGatewayToHaveMandConstraint,
					lowerBoundAmountParticipantsToBeMandatory, upperBoundAmountParticipantsToBeMandatory,
					decisionTakerMandatory, alwaysMaxMandConstrained, upperBoundLocalMinWithBound, amountThreads);
			System.out.println("Test 6 finished!");
		}

		System.out.println("Everything finished!");

	}

	public static void performTestWithRealWorldProcesses(String pathWhereToCreateProcessesWithoutAnnotation,
			String pathWhereToCreateAnnotatedProcesses, int dynamicWriterProb, int upperBoundParticipants,
			int lowerBoundTasks, int upperBoundTasks, int upperBoundXorGtws, int upperBoundParallelGtws,
			int amountProcesses, int minDataObjectsPerDecision, int maxDataObjectsPerDecision,
			List<Integer> dataObjectBoundsRealWorld, List<Integer> writersOfProcessInPercent,
			List<Integer> readersOfProcessInPercent, int probabilityForGatewayToHaveExclConstraint,
			int lowerBoundAmountParticipantsToExclude, int upperBoundAmountParticipantsToExclude,
			boolean decisionTakerExcludeable, boolean alwaysMaxExclConstrained,
			int probabilityForGatewayToHaveMandConstraint, int lowerBoundAmountParticipantsToBeMandatory,
			int upperBoundAmountParticipantsToBeMandatory, boolean decisionTakerMandatory,
			boolean alwaysMaxMandConstrained, int upperBoundlocalMinWithBound, int amountThreads) {

		ExecutorService randomProcessGeneratorService = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathWhereToCreateProcessesWithoutAnnotation, 2,
				upperBoundParticipants, lowerBoundTasks, upperBoundTasks, 1, upperBoundXorGtws, 0,
				upperBoundParallelGtws, amountProcesses, randomProcessGeneratorService);
		randomProcessGeneratorService.shutdownNow();

		int publicDecisionProb = 0;

		BatchFileGenerator.annotateModels(pathWhereToCreateProcessesWithoutAnnotation,
				pathWhereToCreateAnnotatedProcesses, dataObjectBoundsRealWorld, defaultSpheres, dynamicWriterProb,
				writersOfProcessInPercent, readersOfProcessInPercent, 2, upperBoundParticipants,
				minDataObjectsPerDecision, maxDataObjectsPerDecision, publicDecisionProb,
				probabilityForGatewayToHaveExclConstraint, lowerBoundAmountParticipantsToExclude,
				lowerBoundAmountParticipantsToExclude, decisionTakerExcludeable, alwaysMaxExclConstrained,
				probabilityForGatewayToHaveMandConstraint, lowerBoundAmountParticipantsToBeMandatory,
				upperBoundAmountParticipantsToBeMandatory, decisionTakerMandatory, alwaysMaxMandConstrained);

		ExecutorService service = Executors.newFixedThreadPool(amountThreads);
		File csv = BatchFileGenerator.createNewCSVFile(pathWhereToCreateAnnotatedProcesses, "test6_results");
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csv);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathWhereToCreateAnnotatedProcesses,
				upperBoundlocalMinWithBound, publicDecisionProb, writer, service);
		writer.writeRowsToCSVAndcloseWriter();
		service.shutdownNow();

	}

	public static void annotateModels(String pathToFolderWithFilesWithoutAnnotation,
			String pathToFolderForStoringAnnotatedModelsFolder, List<Integer> dataObjectBoundsMixed,
			LinkedList<String> defaultSpheres, int dynamicWriterProb, List<Integer> writersOfProcessInPercent,
			List<Integer> readersOfProcessInPercent, int amountParticipantsPerDecisionLowerBound,
			int amountParticipantsPerDecisionUpperBound, int minDataObjectsPerDecision, int maxDataObjectsPerDecision,
			int publicDecisionProb, int probabilityForGatewayToHaveExcludeConstraint,
			int lowerBoundAmountParticipantsToExclude, int upperBoundAmountParticipantsToExclude,
			boolean decisionTakerExcludeable, boolean alwaysMaxExclConstrained,
			int probabilityForGatewayToHaveMandConstraint, int lowerBoundAmountParticipantsToBeMandatory,
			int upperBoundAmountParticipantsToBeMandatory, boolean decisionTakerMandatory,
			boolean alwaysMaxMandConstrained) {
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
			BpmnModelInstance modelInstanceNotToChange = Bpmn.readModelFromFile(currFile);
			BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality
					.deepCopy(modelInstanceNotToChange);

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
				p = new ProcessModelAnnotater(pathToFile, pathToFolderForStoringAnnotatedModelsFolder, "_annotated");

				// add the methods to be called
				LinkedHashMap<String, Object[]> methodsToBeCalledMap = new LinkedHashMap<String, Object[]>();

				String firstMethodToBeCalledName = "generateDataObjects";
				Object[] argumentsForFirstMethod = new Object[2];
				argumentsForFirstMethod[0] = amountDataObjectsToCreate;
				argumentsForFirstMethod[1] = defaultSpheres;
				methodsToBeCalledMap.putIfAbsent(firstMethodToBeCalledName, argumentsForFirstMethod);

				String secondMethodToBeCalledName = "connectDataObjectsToBrtsAndTuplesForXorSplits";
				Object[] argumentsForSecondMethod = new Object[6];
				argumentsForSecondMethod[0] = minDataObjectsPerDecision;
				argumentsForSecondMethod[1] = maxDataObjectsPerDecision;
				argumentsForSecondMethod[2] = amountParticipantsPerDecisionLowerBound;
				argumentsForSecondMethod[3] = amountParticipantsPerDecisionUpperBound;
				argumentsForSecondMethod[4] = publicDecisionProb;
				argumentsForSecondMethod[5] = false;
				methodsToBeCalledMap.putIfAbsent(secondMethodToBeCalledName, argumentsForSecondMethod);

				String thirdMethodToBeCalledName = "addNamesForOutgoingFlowsOfXorSplits";
				Object[] argumentsForThirdMethod = new Object[1];
				argumentsForThirdMethod[0] = defaultNamesSeqFlowsXorSplits;
				methodsToBeCalledMap.putIfAbsent(thirdMethodToBeCalledName, argumentsForThirdMethod);

				String fourthMethodToBeCalledName = "annotateModelWithFixedAmountOfReadersAndWriters";
				Object[] argumentsForFourthMethod = new Object[4];
				argumentsForFourthMethod[0] = amountWritersOfProcess;
				argumentsForFourthMethod[1] = amountReadersOfProcess;
				argumentsForFourthMethod[2] = dynamicWriterProb;
				argumentsForFourthMethod[3] = defaultSpheres;
				methodsToBeCalledMap.putIfAbsent(fourthMethodToBeCalledName, argumentsForFourthMethod);

				if (probabilityForGatewayToHaveExcludeConstraint > 0) {
					String fifthMethodToBeCalledName = "addExcludeParticipantConstraintsOnModel";

					// set upperBoundAmountParticipantsToExclude to be max globalSphereSize-1
					if (upperBoundAmountParticipantsToExclude <= 0) {
						upperBoundAmountParticipantsToExclude = ThreadLocalRandom.current()
								.nextInt(lowerBoundAmountParticipantsToExclude, globalSphere);
					}

					Object[] argumentsForFifthMethod = new Object[5];
					argumentsForFifthMethod[0] = probabilityForGatewayToHaveExcludeConstraint;
					argumentsForFifthMethod[1] = lowerBoundAmountParticipantsToExclude;
					argumentsForFifthMethod[2] = upperBoundAmountParticipantsToExclude;
					argumentsForFifthMethod[3] = decisionTakerExcludeable;
					argumentsForFifthMethod[4] = alwaysMaxExclConstrained;
					methodsToBeCalledMap.putIfAbsent(fifthMethodToBeCalledName, argumentsForFifthMethod);
				}

				if (probabilityForGatewayToHaveMandConstraint > 0) {
					String sixthMethodToBeCalledName = "addMandatoryParticipantConstraintsOnModel";

					// set upperBoundAmountParticipantsToExclude to be max globalSphereSize-1
					if (upperBoundAmountParticipantsToBeMandatory <= 0) {
						upperBoundAmountParticipantsToBeMandatory = ThreadLocalRandom.current()
								.nextInt(lowerBoundAmountParticipantsToBeMandatory, globalSphere);
					}

					Object[] argumentsForSixthMethod = new Object[5];
					argumentsForSixthMethod[0] = probabilityForGatewayToHaveMandConstraint;
					argumentsForSixthMethod[1] = lowerBoundAmountParticipantsToBeMandatory;
					argumentsForSixthMethod[2] = upperBoundAmountParticipantsToBeMandatory;
					argumentsForSixthMethod[3] = decisionTakerMandatory;
					argumentsForSixthMethod[4] = alwaysMaxMandConstrained;
					methodsToBeCalledMap.putIfAbsent(sixthMethodToBeCalledName, argumentsForSixthMethod);
				}
				// set the methods that will be run within the call method
				p.setMethodsToRunWithinCall(methodsToBeCalledMap);

				Future<File> future = executor.submit(p);
				futures.add(future);
				try {
					future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
					future.cancel(true);
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

	public static void generateRandomProcessesWithFixedValuesAndExactlyMaxDecisionsAndMaxAmountParticipants(
			String pathForAddingRandomModels, int maxAmountParticipants, int maxAmountTasks, int maxAmountXorSplits,
			int maxAmountParallelSplits, int amountProcesses, ExecutorService executor) {

		for (int i = 1; i <= amountProcesses; i++) {

			// will be written to the pathForAddingRandomModels
			ProcessGenerator pGen;

			try {

				pGen = new ProcessGenerator(pathForAddingRandomModels, maxAmountParticipants, maxAmountTasks,
						maxAmountXorSplits, maxAmountParallelSplits, probTask, probXorGtw, probParallelGtw, probJoinGtw,
						nestingDepthFactor);

				Future<File> future = executor.submit(pGen);
				try {
					File f = future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
					System.out.println(f.getName() + " deployed successfully");
					future.cancel(true);
					// there need to be exactly as many xor splits as maxAmountXorSplits
					// there need to be exactly as many participants as maxAmountParticipants
					if (f != null && f.isFile()) {
						BpmnModelInstance createdModel = Bpmn.readModelFromFile(f);

						if (CommonFunctionality.getAmountExclusiveGtwSplits(createdModel) != maxAmountXorSplits) {
							System.out.println(f.getName() + " deleted: " + f.delete() + "! Not exactly "
									+ maxAmountXorSplits + " xor-splits!");
							i--;
							ProcessGenerator.decreaseProcessGeneratorId();

						}
						if (CommonFunctionality.getGlobalSphere(createdModel, false) != maxAmountParticipants) {
							System.out.println(f.getName() + " deleted: " + f.delete() + "! Not exactly "
									+ maxAmountParticipants + " participants!");
							i--;
							ProcessGenerator.decreaseProcessGeneratorId();
						}
					}
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
			int upperBoundXorGtws, int lowerBoundParallelGtws, int upperBoundParallelGtws, int amountProcesses,
			ExecutorService executor) {

		for (int i = 1; i <= amountProcesses; i++) {

			int randomAmountParticipantsWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundParticipants,
					upperBoundParticipants + 1);
			int randomAmountMaxTasksWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundTasks,
					upperBoundTasks + 1);
			int randomAmountMaxXorSplitsWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundXorGtws,
					upperBoundXorGtws + 1);
			int randomAmountMaxParallelSplitsWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundParallelGtws,
					upperBoundParallelGtws + 1);

			try {

				ProcessGenerator pGen = new ProcessGenerator(pathToFiles, randomAmountParticipantsWithinBounds,
						randomAmountMaxTasksWithinBounds, randomAmountMaxXorSplitsWithinBounds,
						randomAmountMaxParallelSplitsWithinBounds, probTask, probXorGtw, probParallelGtw, probJoinGtw,
						nestingDepthFactor, lowerBoundXorGtws);
				Future<File> future = executor.submit(pGen);
				try {
					File f = future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
					System.out.println(f.getName() + " deployed successfully");
					future.cancel(true);
					// all variables of the generated file must be within the bounds

					if (f != null && f.isFile()) {
						BpmnModelInstance createdModel = Bpmn.readModelFromFile(f);

						int decisionsOfCreatedModel = CommonFunctionality.getAmountExclusiveGtwSplits(createdModel);
						if (decisionsOfCreatedModel < lowerBoundXorGtws
								|| decisionsOfCreatedModel > upperBoundXorGtws) {
							System.out.println(f.getName() + " deleted: " + f.delete() + "! Xors of model: "
									+ decisionsOfCreatedModel + " out of range [" + lowerBoundXorGtws + ","
									+ upperBoundXorGtws + "]!");
							i--;
							ProcessGenerator.decreaseProcessGeneratorId();
						}
						int globalSphereOfCreatedModel = CommonFunctionality.getGlobalSphere(createdModel, false);
						if (globalSphereOfCreatedModel < lowerBoundParticipants
								|| globalSphereOfCreatedModel > upperBoundParticipants) {
							System.out.println(f.getName() + " deleted: " + f.delete() + "! Participants of model: "
									+ globalSphereOfCreatedModel + " out of range [" + lowerBoundParticipants + ","
									+ upperBoundParticipants + "]!");
							i--;
							ProcessGenerator.decreaseProcessGeneratorId();
						}

						int parallelGtwsOfCreatedModel = CommonFunctionality.getAmountParallelGtwSplits(createdModel);
						if (parallelGtwsOfCreatedModel < lowerBoundParallelGtws
								|| parallelGtwsOfCreatedModel > upperBoundParallelGtws) {
							System.out.println(f.getName() + " deleted: " + f.delete() + "! Parallel gtws of model: "
									+ parallelGtwsOfCreatedModel + " out of range [" + lowerBoundParallelGtws + ","
									+ upperBoundParallelGtws + "]!");
							i--;
							ProcessGenerator.decreaseProcessGeneratorId();
						}

						int tasksOfCreatedModel = createdModel.getModelElementsByType(Task.class).size();
						if (tasksOfCreatedModel < lowerBoundTasks || tasksOfCreatedModel > upperBoundTasks) {
							System.out.println(
									f.getName() + " deleted: " + f.delete() + "! Tasks of model: " + tasksOfCreatedModel
											+ " out of range [" + lowerBoundTasks + "," + upperBoundTasks + "]!");
							i--;
							ProcessGenerator.decreaseProcessGeneratorId();
						}

					}
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
			}

			System.out.println("randomProcess" + i + " deployed in " + pathToFiles);
		}

	}

	public static LinkedList<API> mapFilesToAPI(String pathToFiles, ResultsToCSVWriter writer) {
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
				API api = new API(pathToFile, cost);
				apiList.add(api);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				File f = new File(pathToFile);
				writer.addNullValueRowForModel(f.getName(), f.getAbsolutePath(), e.getMessage());
				System.err.println(f.getAbsolutePath() + " could not be mapped to api: " + e.getMessage());
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
				API api = new API(pathToFile, cost);
				apiList.add(api);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return apiList;
	}

	private static File createNewCSVFile(String directoryForFile, String nameOfCSVFile) {
		String fileNameForResults = nameOfCSVFile + "_" + idCSVFile + ".csv";
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

	public static HashMap<Boolean, HashMap<String, Integer>> runAlgorithmsAndWriteResultsToCSV(API api,
			int limitForLocalMin, int boundForComparisons, HashMap<String, Integer> timeOutOrHeapSpaceExceptionMap,
			ResultsToCSVWriter writer, ExecutorService service) {
		// write the result of an algorithm to an existing csv file
		// clone api for algorithms

		API bruteForceApi = (API) CommonFunctionality.deepCopy(api);
		bruteForceApi.setAlgorithmToPerform("bruteForce");

		API localMinApi = (API) CommonFunctionality.deepCopy(api);
		localMinApi.setAlgorithmToPerform("localMin");

		String localMinWithMaxSolutions = "localMinWithBound" + limitForLocalMin;
		API localMinWithMaxSolutionsApi = (API) CommonFunctionality.deepCopy(api);
		localMinWithMaxSolutionsApi.setAlgorithmToPerform(localMinWithMaxSolutions);

		HashMap<String, Exception> exceptionPerAlgorithm = new HashMap<String, Exception>();
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap = new HashMap<String, LinkedList<ProcessInstanceWithVoters>>();
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesBruteForce = null;
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesLocalMin = null;
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesLocalMinWithMaxSolutions = null;
		boolean heapSpaceError = false;

		try {

			Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureBruteForce = service
					.submit(bruteForceApi);
			Exception exceptionBruteForce = null;
			try {
				pInstancesBruteForce = futureBruteForce.get(timeOutForApiInMin, TimeUnit.MINUTES);
				algorithmMap.putAll(pInstancesBruteForce);
				System.out.println("BruteForceSolutions: " + pInstancesBruteForce.get("bruteForce").size());
				futureBruteForce.cancel(true);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				exceptionBruteForce = (InterruptedException) e;
				e.printStackTrace();
				futureBruteForce.cancel(true);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				exceptionBruteForce = (ExecutionException) e;
				e.printStackTrace();
				futureBruteForce.cancel(true);
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				exceptionBruteForce = (TimeoutException) e;
				timeOutOrHeapSpaceExceptionMap.put("bruteForce",
						timeOutOrHeapSpaceExceptionMap.getOrDefault("bruteForce", 0) + 1);
				System.err.println("Timeout for bruteForce!");
				futureBruteForce.cancel(true);
			} catch (Error e) {
				System.gc();
				if (e instanceof OutOfMemoryError) {
					exceptionBruteForce = new HeapSpaceException(e.getMessage(), e.getCause());
					timeOutOrHeapSpaceExceptionMap.put("bruteForce",
							timeOutOrHeapSpaceExceptionMap.getOrDefault("bruteForce", 0) + 1);
					System.err.println("BruteForce ran out of memory");
				}
				futureBruteForce.cancel(true);
			}

			finally {
				exceptionPerAlgorithm.putIfAbsent("bruteForce", exceptionBruteForce);
				futureBruteForce.cancel(true);
			}

			Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureLocalMin = service.submit(localMinApi);
			Exception exceptionLocalMin = null;

			try {
				pInstancesLocalMin = futureLocalMin.get(timeOutForApiInMin, TimeUnit.MINUTES);
				algorithmMap.putAll(pInstancesLocalMin);
				System.out.println("LocalMinInstances: " + pInstancesLocalMin.get("localMin").size());
				futureLocalMin.cancel(true);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				exceptionLocalMin = (InterruptedException) e;
				e.printStackTrace();
				futureLocalMin.cancel(true);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				exceptionLocalMin = (ExecutionException) e;
				e.printStackTrace();
				futureLocalMin.cancel(true);
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				exceptionLocalMin = (TimeoutException) e;
				timeOutOrHeapSpaceExceptionMap.put("localMin",
						timeOutOrHeapSpaceExceptionMap.getOrDefault("localMin", 0) + 1);
				System.err.println("Timeout for localMin!");
				futureLocalMin.cancel(true);
			} catch (Error e) {
				System.gc();
				if (e instanceof OutOfMemoryError) {
					exceptionLocalMin = new HeapSpaceException(e.getMessage(), e.getCause());
					timeOutOrHeapSpaceExceptionMap.put("localMin",
							timeOutOrHeapSpaceExceptionMap.getOrDefault("localMin", 0) + 1);
					System.err.println("LocalMin ran out of memory");
				}
				futureLocalMin.cancel(true);
			} finally {
				exceptionPerAlgorithm.putIfAbsent("localMin", exceptionLocalMin);
				futureLocalMin.cancel(true);
			}

			Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureLocalMinWithMaxSolutions = service
					.submit(localMinWithMaxSolutionsApi);
			Exception exceptionLocalMinWithMaxSolutions = null;

			try {
				pInstancesLocalMinWithMaxSolutions = futureLocalMinWithMaxSolutions.get(timeOutForApiInMin,
						TimeUnit.MINUTES);
				algorithmMap.putAll(pInstancesLocalMinWithMaxSolutions);
				System.out.println("LocalMinInstances with bound: "
						+ pInstancesLocalMinWithMaxSolutions.get(localMinWithMaxSolutions).size());
				futureLocalMinWithMaxSolutions.cancel(true);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				exceptionLocalMinWithMaxSolutions = (InterruptedException) e;
				e.printStackTrace();
				futureLocalMinWithMaxSolutions.cancel(true);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				exceptionLocalMinWithMaxSolutions = (ExecutionException) e;
				e.printStackTrace();
				futureLocalMinWithMaxSolutions.cancel(true);
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				exceptionLocalMinWithMaxSolutions = (TimeoutException) e;
				timeOutOrHeapSpaceExceptionMap.put(localMinWithMaxSolutions,
						timeOutOrHeapSpaceExceptionMap.getOrDefault(localMinWithMaxSolutions, 0) + 1);
				System.err.println("Timeout for " + localMinWithMaxSolutions + "!");
				futureLocalMinWithMaxSolutions.cancel(true);
			} catch (Error e) {
				System.gc();
				if (e instanceof OutOfMemoryError) {
					exceptionLocalMinWithMaxSolutions = new HeapSpaceException(e.getMessage(), e.getCause());
					timeOutOrHeapSpaceExceptionMap.put(localMinWithMaxSolutions,
							timeOutOrHeapSpaceExceptionMap.getOrDefault(localMinWithMaxSolutions, 0) + 1);
					System.err.println(localMinWithMaxSolutions + " ran out of memory");
				}
				futureLocalMinWithMaxSolutions.cancel(true);
			} finally {
				exceptionPerAlgorithm.putIfAbsent(localMinWithMaxSolutions, exceptionLocalMinWithMaxSolutions);
				futureLocalMinWithMaxSolutions.cancel(true);
			}

		} catch (Exception e) {
			System.err.println("Some other exception has happened!");
			e.printStackTrace();
		} finally {
			String isCheapestSolutionOfLocalMinInBruteForce = "null";
			if (pInstancesLocalMin != null && pInstancesBruteForce != null) {
				isCheapestSolutionOfLocalMinInBruteForce = CommonFunctionality
						.compareResultsOfAlgorithmsForDifferentAPIs(pInstancesLocalMin.get("localMin"),
								pInstancesBruteForce.get("bruteForce"), boundForComparisons);

			}
			String isCheapestSolutionOflocalMinWithBoundInBruteForce = "null";
			if (pInstancesLocalMinWithMaxSolutions != null && pInstancesBruteForce != null) {
				isCheapestSolutionOflocalMinWithBoundInBruteForce = CommonFunctionality
						.compareResultsOfAlgorithmsForDifferentAPIs(
								pInstancesLocalMinWithMaxSolutions.get(localMinWithMaxSolutions),
								pInstancesBruteForce.get("bruteForce"), boundForComparisons);

			}

			writer.writeResultsOfAlgorithmsToCSVFile(bruteForceApi, localMinApi, localMinWithMaxSolutionsApi,
					algorithmMap, exceptionPerAlgorithm, isCheapestSolutionOfLocalMinInBruteForce,
					isCheapestSolutionOflocalMinWithBoundInBruteForce);

		}

		HashMap<Boolean, HashMap<String, Integer>> returnMap = new HashMap<Boolean, HashMap<String, Integer>>();
		returnMap.putIfAbsent(heapSpaceError, timeOutOrHeapSpaceExceptionMap);
		return returnMap;

	}

	public static void runAlgorithmsAndWriteResultsToCSV(LinkedList<File> files, int upperBoundSolutionsForLocalMin,
			int boundForComparisons, ResultsToCSVWriter writer, ExecutorService service) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(files);
		if (!apiList.isEmpty()) {
			for (API api : apiList) {
				BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api, upperBoundSolutionsForLocalMin,
						boundForComparisons, new HashMap<String, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels,
			int upperBoundSolutionsForLocalMin, int boundForComparisons, ResultsToCSVWriter writer,
			ExecutorService service) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithAnnotatedModels, writer);
		if (!apiList.isEmpty()) {
			for (API api : apiList) {
				BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api, upperBoundSolutionsForLocalMin,
						boundForComparisons, new HashMap<String, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void performDataObjectTest(LinkedList<File> processes, String pathToDestinationFolderForStoringModels,
			int amountUniqueDataObjectsPerDecision, int upperBoundLocalMinWithBound, int amountThreadPools) {
		// each decision has unique dataObject sets of same size
		// e.g. model has 4 decisions -> and each decision has 3 DataObjects -> 12
		// unique dataObjects need to be connected
		// -> at least 12 readers and 12 writers needed, since every data object will
		// have to be first written and then get read by some brt

		// amount voters per gtw will be between 2 and globalSphere

		// Test each model with small, medium and large amount of writers and readers
		// in this case, small, medium and large will be added on top of the already
		// existing readers/writers

		// Start test with static sphere
		// then take same models and take strong dynamic sphere
		LinkedList<String> sphere = new LinkedList<String>();
		sphere.add("Static");
		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		for (int i = 0; i < processes.size(); i++) {
			for (int indexWriters = 0; indexWriters < percentageOfWritersClasses.size(); indexWriters++) {
				for (int indexReaders = 0; indexReaders < percentageOfReadersClasses.size(); indexReaders++) {
					String pathToRandomProcess = processes.get(i).getAbsolutePath();
					BpmnModelInstance processModel = Bpmn.readModelFromFile(processes.get(i));
					int amountTasks = processModel.getModelElementsByType(Task.class).size();
					int amountDecisions = CommonFunctionality.getAmountExclusiveGtwSplits(processModel);
					int globalSphere = CommonFunctionality.getGlobalSphere(processModel, false);
					int amountVotersUpperBound = ThreadLocalRandom.current().nextInt(2, globalSphere + 1);

					boolean modelIsValid = false;
					int sumDataObjectsToCreate = 0;
					int triesPerModel = 100;
					boolean skipModel = false;
					while (modelIsValid == false && skipModel == false && triesPerModel > 0) {
						try {
							int percentageReaders = percentageOfReadersClasses.get(indexReaders);
							int percentageWriters = percentageOfWritersClasses.get(indexWriters);
							String suffix = "";
							// additional readers and writers suffix
							if (indexReaders == 0) {
								suffix += "asR";
							} else if (indexReaders == 1) {
								suffix += "amR";
							} else if (indexReaders == 2) {
								suffix += "alR";
							}

							if (indexWriters == 0) {
								suffix += "asW";
							} else if (indexWriters == 1) {
								suffix += "amW";
							} else if (indexWriters == 2) {
								suffix += "alW";
							}
							ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess,
									pathToDestinationFolderForStoringModels, suffix);

							sumDataObjectsToCreate = amountUniqueDataObjectsPerDecision * amountDecisions;
							if (amountTasks > sumDataObjectsToCreate) {
								pModel.generateDataObjects(sumDataObjectsToCreate, sphere);
								pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(amountUniqueDataObjectsPerDecision,
										amountUniqueDataObjectsPerDecision, 2, amountVotersUpperBound, 0, true);
								int amountWritersNeededForDataObjects = sumDataObjectsToCreate;
								int amountReadersNeededForDataObjects = sumDataObjectsToCreate;

								// additional readers and writers to be inserted
								int amountAdditionalWriters = CommonFunctionality.getAmountFromPercentage(amountTasks,
										percentageWriters);
								int amountAdditionalReaders = CommonFunctionality.getAmountFromPercentage(amountTasks,
										percentageReaders);

								int amountWritersToBeInserted = amountWritersNeededForDataObjects
										+ amountAdditionalWriters;
								int amountReadersToBeInserted = amountReadersNeededForDataObjects
										+ amountAdditionalReaders;

								if (amountWritersToBeInserted <= amountTasks
										&& amountReadersToBeInserted <= amountTasks) {
									// add the methods to be called
									LinkedHashMap<String, Object[]> methodsToBeCalledMap = new LinkedHashMap<String, Object[]>();

									String firstMethodToBeCalledName = "annotateModelWithFixedAmountOfReadersAndWriters";
									Object[] argumentsForFirstMethod = new Object[4];
									argumentsForFirstMethod[0] = amountWritersToBeInserted;
									argumentsForFirstMethod[1] = amountReadersToBeInserted;
									argumentsForFirstMethod[2] = 0;
									argumentsForFirstMethod[3] = defaultSpheres;
									methodsToBeCalledMap.putIfAbsent(firstMethodToBeCalledName,
											argumentsForFirstMethod);
									pModel.setMethodsToRunWithinCall(methodsToBeCalledMap);

									Future<File> f = executor.submit(pModel);
									try {
										f.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
										f.cancel(true);
										modelIsValid = true;

									} catch (Exception e) {
										f.cancel(true);
									}
								} else {
									skipModel = true;
								}

							} else {
								skipModel = true;
							}

						} catch (Exception e) {
							System.err.println(e.getMessage());

						}
						triesPerModel--;
					}
				}
			}

		}
		executor.shutdownNow();
		File directory = new File(pathToDestinationFolderForStoringModels);
		File[] files = directory.listFiles();

		for (File f : files) {
			if (f.getName().contains("bpmn")) {
				BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality
						.deepCopy(Bpmn.readModelFromFile(f));
				String fileName = f.getName();
				BatchFileGenerator.substituteDataObjectAndWriteNewModels(modelInstance, fileName,
						pathToDestinationFolderForStoringModels);
			}
		}

		File csvFile = BatchFileGenerator.createNewCSVFile(pathToDestinationFolderForStoringModels, "test3_results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator
				.getAllModelsFromFolder(pathToDestinationFolderForStoringModels);
		executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, upperBoundLocalMinWithBound,
				boundForComparisons, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();
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
			if (!model.getName().contains(".csv")) {
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
			if (model.getName().contains(".bpmn")) {
				modelsFromSourceFolder.add(model);
			}
		}

		return modelsFromSourceFolder;
	}

	public static void performTradeOffTest(String processType, LinkedList<File> processes,
			String pathToDestinationFolderForStoringModels, List<Integer> dataObjectBounds,
			int upperBoundSolutionsForLocalMinWithBound, int boundForComparisons, int amountThreadPools) {

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
								pathToDestinationFolderForStoringModels, suffixBuilder.toString());
						modelWithReadersAndWriters.setDataObjectsConnectedToBrts(true);

						// add the methods to be called
						LinkedHashMap<String, Object[]> methodsToBeCalledMap = new LinkedHashMap<String, Object[]>();

						String firstMethodToBeCalledName = "addNamesForOutgoingFlowsOfXorSplits";
						Object[] argumentsForFirstMethod = new Object[1];
						argumentsForFirstMethod[0] = defaultNamesSeqFlowsXorSplits;
						methodsToBeCalledMap.putIfAbsent(firstMethodToBeCalledName, argumentsForFirstMethod);

						String secondMethodToBeCalledName = "annotateModelWithFixedAmountOfReadersAndWriters";
						Object[] argumentsForSecondMethod = new Object[4];
						argumentsForSecondMethod[0] = amountWriterTasksInModel;
						argumentsForSecondMethod[1] = amountReaderTasksInModel;
						argumentsForSecondMethod[2] = 0;
						argumentsForSecondMethod[3] = emptySphere;
						methodsToBeCalledMap.putIfAbsent(secondMethodToBeCalledName, argumentsForSecondMethod);

						modelWithReadersAndWriters.setMethodsToRunWithinCall(methodsToBeCalledMap);

						Future<File> future = executor.submit(modelWithReadersAndWriters);

						try {
							future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
							future.cancel(true);
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
			if (annotatedModel.isFile() && annotatedModel.getName().contains(".bpmn")) {
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
								try {

									CommonFunctionality.writeChangesToFile(currModel, modelName,
											pathToFolderWithModelsForEvaluation, suffix);

								} catch (Exception e) {
									e.printStackTrace();
								}

							}

						}

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

		// perform the algorithms and generate csv file
		ExecutorService service = Executors.newFixedThreadPool(amountThreadPools);
		File file = BatchFileGenerator.createNewCSVFile(pathToFolderWithModelsForEvaluation,
				"test2_processType" + processType + "_results");
		ResultsToCSVWriter writer = new ResultsToCSVWriter(file);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathToFolderWithModelsForEvaluation,
				upperBoundSolutionsForLocalMinWithBound, boundForComparisons, writer, service);
		service.shutdownNow();
		writer.writeRowsToCSVAndcloseWriter();

	}

	public static void performBoundaryTest1_1(int amountProcessesPerIteration, int amountDecisionsToStart,
			int votersPerDecision, int globalSphere, int upperBoundSolutionsForLocalMinWithBound,
			int amountTasksToStartWith, int tasksFactor, int lowerBoundAmountParallelGtws,
			int upperBoundAmountParallelGtws, int amountWritersInPercent, int amountReadersInPercent,
			int minDataObjectsPerDecision, int maxDataObjectsPerDecision, LinkedList<String> sphereList,
			int amountThreads, String pathToFolderForModelsForBoundaryTest) {

		int amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
				upperBoundAmountParallelGtws + 1);

		File csvFile = BatchFileGenerator.createNewCSVFile(pathToFolderForModelsForBoundaryTest, "test1_1-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		HashMap<String, Integer> timeOutOrHeapSpaceExceptionMap = new HashMap<String, Integer>();

		String localMinWithBound = "localMinWithBound" + upperBoundSolutionsForLocalMinWithBound;
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		boolean outOfMemoryError = false;
		int apiListSize = amountProcessesPerIteration;

		do {
			timeOutOrHeapSpaceExceptionMap.clear();
			timeOutOrHeapSpaceExceptionMap.put("bruteForce", 0);
			timeOutOrHeapSpaceExceptionMap.put("localMin", 0);
			timeOutOrHeapSpaceExceptionMap.put(localMinWithBound, 0);

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
			BatchFileGenerator.generateRandomProcessesWithFixedValuesAndExactlyMaxDecisionsAndMaxAmountParticipants(
					pathToFolderForModelsWithDecisions, globalSphere, amountTasksWithFactor, amountDecisionsToStart,
					amountParallelGtws, amountProcessesPerIteration, executor);

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
				boolean correctModel = false;
				int tries = 0;
				while (correctModel == false && tries < triesForModelAnnotater) {
					File modelCopy = (File) CommonFunctionality.deepCopy(model);
					ProcessModelAnnotater p;
					System.out.println(model.getName() + ", tries: " + tries);
					try {
						p = new ProcessModelAnnotater(modelCopy.getAbsolutePath(),
								pathToFolderForModelsWithDecisionsAnnotated, "_annotated");
						// add the methods to be called
						LinkedHashMap<String, Object[]> methodsToBeCalledMap = new LinkedHashMap<String, Object[]>();

						String firstMethodToBeCalledName = "addNamesForOutgoingFlowsOfXorSplits";
						Object[] argumentsForFirstMethod = new Object[1];
						argumentsForFirstMethod[0] = defaultNamesSeqFlowsXorSplits;
						methodsToBeCalledMap.putIfAbsent(firstMethodToBeCalledName, argumentsForFirstMethod);

						String secondMethodToBeCalledName = "generateDataObjects";
						Object[] argumentsForSecondMethod = new Object[2];
						argumentsForSecondMethod[0] = amountDataObjectsToCreate;
						argumentsForSecondMethod[1] = defaultSpheres;
						methodsToBeCalledMap.putIfAbsent(secondMethodToBeCalledName, argumentsForSecondMethod);

						String thirdMethodToBeCalledName = "connectDataObjectsToBrtsAndTuplesForXorSplits";
						Object[] argumentsForThirdMethod = new Object[6];
						argumentsForThirdMethod[0] = minDataObjectsPerDecision;
						argumentsForThirdMethod[1] = maxDataObjectsPerDecision;
						argumentsForThirdMethod[2] = votersPerDecision;
						argumentsForThirdMethod[3] = votersPerDecision;
						argumentsForThirdMethod[4] = 0;
						argumentsForThirdMethod[5] = true;
						methodsToBeCalledMap.putIfAbsent(thirdMethodToBeCalledName, argumentsForThirdMethod);

						String fourthMethodToBeCalledName = "annotateModelWithFixedAmountOfReadersAndWriters";
						Object[] argumentsForFourthMethod = new Object[4];
						argumentsForFourthMethod[0] = amountWriters;
						argumentsForFourthMethod[1] = amountReaders;
						argumentsForFourthMethod[2] = 0;
						argumentsForFourthMethod[3] = defaultSpheres;
						methodsToBeCalledMap.putIfAbsent(fourthMethodToBeCalledName, argumentsForFourthMethod);

						// set the methods that will be run within the call method
						p.setMethodsToRunWithinCall(methodsToBeCalledMap);

						Future<File> future = executor.submit(p);
						try {
							future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
							System.out.println("Model annotated correctly!");
							correctModel = true;
							future.cancel(true);
						} catch (Exception e) {
							correctModel = false;
							System.err.println("Exception in call method of ProcessModelAnnotater: ");
							e.printStackTrace();
							future.cancel(true);
							// delete already generated file
							File alreadyGenerated = new File(p.getDirectoryForNewFile());
							if (alreadyGenerated != null && alreadyGenerated.isFile()) {
								System.out.println(alreadyGenerated.getAbsolutePath() + " deleted - "
										+ alreadyGenerated.delete() + "!");
							}
						}
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						System.err.println("Exception in ProcessModelAnnotater");
						e1.printStackTrace();
					}
					tries++;
				}
			}
			// map annotated models
			LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderForModelsWithDecisionsAnnotated,
					writer);

			apiListSize = apiList.size();

			// perform all algorithms and count the timeouts
			for (API api : apiList) {
				HashMap<Boolean, HashMap<String, Integer>> returnMap = BatchFileGenerator
						.runAlgorithmsAndWriteResultsToCSV(api, upperBoundSolutionsForLocalMinWithBound,
								boundForComparisons, timeOutOrHeapSpaceExceptionMap, writer, executor);
				if (returnMap.get(false) != null) {
					outOfMemoryError = true;
					timeOutOrHeapSpaceExceptionMap = returnMap.get(false);
				} else {
					timeOutOrHeapSpaceExceptionMap = returnMap.get(true);
				}

			}

			System.out.println("Iteration" + amountDecisionsToStart + " end - timeOutsBruteForce: "
					+ timeOutOrHeapSpaceExceptionMap.get("bruteForce") + ", timeOutsLocalMin: "
					+ timeOutOrHeapSpaceExceptionMap.get("localMin") + ", timeOutsLocalMinWithBound"
					+ upperBoundSolutionsForLocalMinWithBound + ": "
					+ timeOutOrHeapSpaceExceptionMap.get(localMinWithBound));

			amountDecisionsToStart++;

		} while (timeOutOrHeapSpaceExceptionMap.get("bruteForce") < apiListSize
				|| timeOutOrHeapSpaceExceptionMap.get("localMin") < apiListSize
				|| timeOutOrHeapSpaceExceptionMap.get(localMinWithBound) < apiListSize || outOfMemoryError == false);

		executor.shutdownNow();

		writer.writeRowsToCSVAndcloseWriter();

	}

	public static void performBoundaryTest1_2(File file, int globalSphereLowerBound,
			int amountNewProcessesToCreatePerIteration, int upperBoundSolutionsForLocalMin, int boundForComparisons,
			int amountThreadPools, String directoryToStore) {

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStore, "test1_2-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);

		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		CommonFunctionality.generateNewModelsUntilGlobalSphereReached(file, globalSphereLowerBound,
				amountNewProcessesToCreatePerIteration, directoryToStore);

		String localMinWithBound = "localMinWithBound" + upperBoundSolutionsForLocalMin;
		// map annotated models
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(directoryToStore, writer);

		// perform all algorithms and count the timeouts
		for (API api : apiList) {
			HashMap<String, Integer> timeOutMap = new HashMap<String, Integer>();

			timeOutMap.put("bruteForce", 0);
			timeOutMap.put("localMin", 0);
			timeOutMap.put(localMinWithBound, 0);

			HashMap<Boolean, HashMap<String, Integer>> returnMap = BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(
					api, upperBoundSolutionsForLocalMin, boundForComparisons, timeOutMap, writer, executor);
			boolean outOfMemoryException = false;
			if (returnMap.get(false) != null) {
				outOfMemoryException = true;
				timeOutMap = returnMap.get(false);
			} else {
				timeOutMap = returnMap.get(true);
			}

			System.out.println("timeOutsBruteForce: " + timeOutMap.get("bruteForce") + ", timeOutsLocalMin: "
					+ timeOutMap.get("localMin") + ", timeOuts" + localMinWithBound + ": "
					+ timeOutMap.get(localMinWithBound));

			if ((timeOutMap.get("bruteForce") == 1 && timeOutMap.get("localMin") == 1
					&& timeOutMap.get(localMinWithBound) == 1) || outOfMemoryException == true) {
				break;
			}
		}
		executor.shutdownNow();

		writer.writeRowsToCSVAndcloseWriter();

	}

	public static void substituteDataObjectAndWriteNewModels(BpmnModelInstance modelInstance, String modelName,
			String directoryToStore) {

		LinkedList<DataObjectReference> daoList = new LinkedList<DataObjectReference>();
		daoList.addAll(modelInstance.getModelElementsByType(DataObjectReference.class));
		DataObjectReference substitute = CommonFunctionality.getRandomItem(daoList);

		CommonFunctionality.substituteOneDataObjectPerIterationAndWriteNewModels(modelInstance, substitute, modelName,
				directoryToStore);

	}

	public static void performTestWithSearchForSetOfBestVerifiers(LinkedList<File> models, boolean modelWithLanes,
			String directoryToStoreNewModels, int upperBoundLocalMinWithBound, int amountThreads) {
		// the decision taker must always be part of the verifiers
		int probabilityForGatwayToHaveMandPartConst = 100;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				CommonFunctionality.addMandatoryParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatwayToHaveMandPartConst, 1, 1, true, false, modelWithLanes,
						directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels, "test5_results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, upperBoundLocalMinWithBound,
				boundForComparisons, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();

	}

	public static void performTestWithMaxPossibleExcludeConstraints(LinkedList<File> models,
			boolean decisionTakerExcludeable, boolean modelWithLanes, String directoryToStoreNewModels,
			int upperBoundLocalMinWithBound, int amountThreads) {
		int probabilityForGatewayToHaveConstraint = 100;
		int lowerBoundAmountParticipantsToExcludePerGtw = 1;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				// will always be max. constrained
				CommonFunctionality.addExcludeParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatewayToHaveConstraint, lowerBoundAmountParticipantsToExcludePerGtw,
						decisionTakerExcludeable, true, modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels, "test4_1-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, upperBoundLocalMinWithBound,
				boundForComparisons, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();

	}

	public static void performTestWithExcludeConstraintsProbability(LinkedList<File> models,
			boolean decisionTakerExcludeable, boolean modelWithLanes, int probabilityForGatewayToHaveConstraint,
			String directoryToStoreNewModels, int upperBoundLocalMinWithBound, int amountThreads) {
		int lowerBoundAmountParticipantsToExcludePerGtw = 1;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				CommonFunctionality.addExcludeParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatewayToHaveConstraint, lowerBoundAmountParticipantsToExcludePerGtw,
						decisionTakerExcludeable, false, modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels,
				"test4_2-prob" + probabilityForGatewayToHaveConstraint + "_results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, upperBoundLocalMinWithBound,
				boundForComparisons, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();

	}

	public static LinkedList<File> getFilesFromFolders(LinkedList<File> smallProcesses,
			LinkedList<File> mediumProcesses, LinkedList<File> largeProcesses, List<Integer> amountModels) {
		LinkedList<File> filesList = new LinkedList<File>();
		try {

			for (int i = 0; i < amountModels.get(0); i++) {
				filesList.add(smallProcesses.get(i));
			}

			for (int j = 0; j < amountModels.get(1); j++) {
				filesList.add(mediumProcesses.get(j));
			}

			for (int k = 0; k < amountModels.get(2); k++) {
				filesList.add(largeProcesses.get(k));
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return filesList;
	}

}
