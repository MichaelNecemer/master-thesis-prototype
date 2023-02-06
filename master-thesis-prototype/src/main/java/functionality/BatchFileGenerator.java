package functionality;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.camunda.bpm.model.bpmn.instance.BusinessRuleTask;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;

import processModelGeneratorAndAnnotater.ProcessGenerator;
import processModelGeneratorAndAnnotater.ProcessModelAnnotater;

public class BatchFileGenerator {

	static int idCSVFile = 1;
	// static String root = System.getProperty("user.home") + "/Desktop";
	static String root = System.getProperty("user.home") + "/Onedrive/Desktop";

	static int timeOutForProcessGeneratorInMin = 5;
	static int timeOutForProcessModelAnnotaterInMin = 7;
	// API is the class where the computations will be done
	static int timeOutForApiInMin = 3;

	// how often will the modelAnnotater try to annotate the model if it fails
	static int triesForModelAnnotater = 50;

	// amount of solutions to be generated with naive and heuristic approaches
	static int amountSolutionsToBeGenerated = 1;

	static int amountThreads = 1;

	// bounds for ProcessModelGenerator
	static int probTask = 50;
	static int probXorGtw = 30;
	static int probParallelGtw = 20;
	static int maxTriesForGeneratingProcess = 100;

	// bounds for ProcessModelAnnotater
	static LinkedList<Integer> dataObjectBoundsSmallProcesses = new LinkedList<Integer>(Arrays.asList(1, 2));
	static LinkedList<Integer> dataObjectBoundsMediumProcesses = new LinkedList<Integer>(Arrays.asList(1, 4));
	static LinkedList<Integer> dataObjectBoundsLargeProcesses = new LinkedList<Integer>(Arrays.asList(1, 6));

	static LinkedList<String> defaultSpheres = new LinkedList<String>(
			Arrays.asList("Private", "Static", "Weak-Dynamic", "Strong-Dynamic"));
	static LinkedList<String> defaultNamesSeqFlowsXorSplits = new LinkedList<String>(Arrays.asList("true", "false"));
	static int dynamicWriterProb = 0;
	static int probPublicSphere = 0;

	// bounds for "small", "medium", "large" amountOfWriters classes in percentage
	// e.g. 10 means, there will be 10% writers of the tasks in the process
	static LinkedList<Integer> percentageOfWritersClasses = new LinkedList<Integer>(Arrays.asList(10, 20, 30));

	// bounds for "small", "medium", "large" amountOfReaders
	static LinkedList<Integer> percentageOfReadersClasses = new LinkedList<Integer>(Arrays.asList(10, 20, 30));

	// alpha, beta and gamma cost parameters
	static LinkedList<Double> costParameters = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

	// amount of xor-splits per class
	// lower bound, upper bound
	static ArrayList<Integer> amountXorsSmallProcessesBounds = new ArrayList<>(Arrays.asList(1, 2));
	static ArrayList<Integer> amountXorsMediumProcessesBounds = new ArrayList<>(Arrays.asList(3, 4));
	static ArrayList<Integer> amountXorsLargeProcessesBounds = new ArrayList<>(Arrays.asList(5, 6));
	static ArrayList<Integer> amountXorsSuperLargeProcessesBounds = new ArrayList<>(Arrays.asList(7, 8));

	// bounds for nestingDepthFactor and probJoinGtw
	static ArrayList<Integer> nestingDepthFactorBounds = new ArrayList<>(Arrays.asList(1, 25));
	static ArrayList<Integer> probJoinGtwBounds = new ArrayList<>(Arrays.asList(1, 25));

	// boundary tests shared bounds
	static int boundaryTest1_1_privateSphere = 6;
	static int boundaryTest1_1_addActorsPerDecision = 3;

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
		// methodsToRun.add(test1_2ToRun);
		// methodsToRun.add(createRandomProcesses);
		// methodsToRun.add(test2ToRun);		
		// methodsToRun.add(test3ToRun);
		// methodsToRun.add(test4_1ToRun);
		// methodsToRun.add(test4_2ToRun); 
		// methodsToRun.add(test5ToRun);		 
		// methodsToRun.add(test6ToRun);

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
			// e.g. 2 decisions - 1. needs 2 out of 4 additional actors = 6 possible
			// combinations,
			// 2.
			// needs 3 out of 5 = 10 possible combinations -> 6*10 = 60 possible
			// combinations of participants
			// so the boundary will be heavily influenced by the combination of additional
			// actors
			// per decision as well as the amount of decisions in the process

			// the boundary test will set the max process size which will be taken for
			// further tests

			// generate 10 Processes - annotate them - try performing algorithms with a time
			// limit
			// count amount of timeouts
			// do that while there are not timeouts on all processes for the algorithms

			int minDataObjectsPerDecision = 1;
			int maxDataObjectsPerDecision = 1;

			int amountProcessesPerIteration = 10;

			pathToFolderForModelsForTest1_1 = CommonFunctionality
					.fileWithDirectoryAssurance(pathToRootFolder, "Test1_1-BoundaryTest1").getAbsolutePath();

			LinkedList<String> sphere = new LinkedList<String>();
			sphere.add("Strong-Dynamic");

			int stopAfterDecisions = 50;

			// the amount of possible combinations of additional actors for the process will
			// be increased by
			// binom(5,3) -> 10 additional combs per decision
			// the actor of the brt itself can not be an additional actor to the brt and is
			// therefore excluded
			// e.g. with 0 decisions = 0, 1 decision = 10, 2 decisions = 100, ...
			// since the cost of the models will have to be calculated for all the possible
			// combinations
			// this can be used to estimate the feasibility for the following experiments

			// tasksFactor is the number of additional tasks to be inserted per decision
			int tasksFactor = 4;

			BatchFileGenerator.performBoundaryTest1_1(amountProcessesPerIteration, 0,
					boundaryTest1_1_addActorsPerDecision, boundaryTest1_1_privateSphere, amountSolutionsToBeGenerated,
					6, tasksFactor, 0, 0, percentageOfWritersClasses.get(1), percentageOfReadersClasses.get(1),
					minDataObjectsPerDecision, maxDataObjectsPerDecision, sphere, amountThreads, stopAfterDecisions,
					pathToFolderForModelsForTest1_1);

			System.out.println("BoundartyTest1_1 finished!");
		}

		if (methodsToRun.contains(test1_2ToRun)) {
			// Test 1.2 - Boundary Test 2
			// choose a model from boundary test 1 that had no exceptions for all algorithms
			// create x new models on each iteration till every task of the model has a
			// different participant
			// start with the SphereLowerBound e.g. 3 -> x models where each task has
			// one of the 3 participants connected
			// end point -> x models where each task has a different participant connected

			if (!pathToFolderForModelsForTest1_1.isEmpty()) {

				String pathToFolderForModelsForTest1_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test1_2-BoundaryTest2").getAbsolutePath();

				// choose a model
				File directoryOfFiles = new File(pathToFolderForModelsForTest1_1 + File.separatorChar
						+ "BoundaryTest_decision-4" + File.separatorChar + "annotated");
				List<File> listOfFiles = Arrays.asList(directoryOfFiles.listFiles());
				File model = CommonFunctionality.getRandomItem(listOfFiles);
				int newModelsPerIteration = 10;

				// the private sphere lower bound for this test is influenced by the boundary
				// test 1_1
				// i.e. the numbers of additional actors we are looking for in 1_1
				// it must be at least 1 bigger than the amount of additional actors
				// else the models are not valid
				int privateSphereLowerBound = boundaryTest1_1_addActorsPerDecision + 1;

				BatchFileGenerator.performBoundaryTest1_2(model, privateSphereLowerBound, newModelsPerIteration,
						amountSolutionsToBeGenerated, amountThreads, pathToFolderForModelsForTest1_2);
				System.out.println("BoundartyTest1_2 finished!");
			} else {
				System.out.println(test1_2ToRun + " not performed! Run Test1_1 first!");
			}
		}

		if (methodsToRun.contains(createRandomProcesses)) {

			// generate 3 classes of processes -> small, medium, large processes (without
			// annotation)
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

			int amountProcessesToCreatePerDecision = 50;

			ExecutorService randomProcessGeneratorService = Executors.newFixedThreadPool(amountThreads);

			// small processes: 5 participants, 6-15 tasks, 1-2 xors, 0-2 parallels
			for (int i = amountXorsSmallProcessesBounds.get(0); i <= amountXorsSmallProcessesBounds.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToSmallProcessesFolderWithoutAnnotation,
						5, 5, 6, 15, i, i, 0, 2, amountProcessesToCreatePerDecision, randomProcessGeneratorService,
						true);
			}

			// medium processes: 5 participants, 16-30 tasks, 3-4 xors, 0-3 parallels
			for (int i = amountXorsMediumProcessesBounds.get(0); i <= amountXorsMediumProcessesBounds.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(
						pathToMediumProcessesFolderWithoutAnnotation, 5, 5, 16, 30, i, i, 0, 3,
						amountProcessesToCreatePerDecision, randomProcessGeneratorService, true);
			}

			// large processes: 5 participants, 31-45 tasks, 5-6 xors, 0-4, parallels
			for (int i = amountXorsLargeProcessesBounds.get(0); i <= amountXorsLargeProcessesBounds.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolderWithoutAnnotation,
						5, 5, 31, 45, i, i, 0, 4, amountProcessesToCreatePerDecision, randomProcessGeneratorService,
						true);
			}

			// xl processes
			for (int i = amountXorsSuperLargeProcessesBounds.get(0); i <= amountXorsLargeProcessesBounds.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolderWithoutAnnotation,
						5, 5, 31, 45, i, i, 0, 4, amountProcessesToCreatePerDecision, randomProcessGeneratorService,
						true);
			}

			randomProcessGeneratorService.shutdownNow();
			System.out.println("All random processes generated!");
		}

		if (methodsToRun.contains(test2ToRun)) {
			// Test 2 - Measure impact of enforceability on privity and vice versa
			// take x random models from small, medium and large processes
			// add dataObjects with private sphere and 1 voter per decision
			// add readers/writers combinations - generate new models (9 new models for //
			// each)
			// increase the amount of additional actors needed for decisions till the
			// private
			// sphere-1 (because the actor of the brt itself is excluded)
			// of that process is reached on all xors
			// increase privity requirements to next stricter sphere for all dataObjects
			int modelsToTakePerDecision = 5;

			if (pathToSmallProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToMediumProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToLargeProcessesFolderWithoutAnnotation.isEmpty()) {
				System.out.println(test2ToRun + " not performed! Generate random processes first!");
			} else {
				String pathToFolderForModelsForTest2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test2-TradeOff").getAbsolutePath();
				pathToSmallProcessesForTest2WithAnnotation = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "SmallProcessesAnnotatedFolder")
						.getAbsolutePath();
				pathToMediumProcessesForTest2WithAnnotation = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "MediumProcessesAnnotatedFolder")
						.getAbsolutePath();
				pathToLargeProcessesForTest2WithAnnotation = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "LargeProcessesAnnotatedFolder")
						.getAbsolutePath();

				LinkedList<File> smallProcessesWithoutAnnotation = new LinkedList<File>();
				for (int i = amountXorsSmallProcessesBounds.get(0); i <= amountXorsSmallProcessesBounds.get(1); i++) {
					LinkedList<File> smallProcessesToAdd = BatchFileGenerator
							.getModelsInOrderFromSourceFolderWithExactAmountDecision(modelsToTakePerDecision, i,
									pathToSmallProcessesFolderWithoutAnnotation);
					smallProcessesWithoutAnnotation.addAll(smallProcessesToAdd);
					System.out
							.println(smallProcessesToAdd.size() + " small processes with: " + i + " decisions added!");
				}

				LinkedList<File> mediumProcessesWithoutAnnotation = new LinkedList<File>();
				for (int i = amountXorsMediumProcessesBounds.get(0); i <= amountXorsMediumProcessesBounds.get(1); i++) {
					LinkedList<File> mediumProcessestoAdd = BatchFileGenerator
							.getModelsInOrderFromSourceFolderWithExactAmountDecision(modelsToTakePerDecision, i,
									pathToMediumProcessesFolderWithoutAnnotation);
					mediumProcessesWithoutAnnotation.addAll(mediumProcessestoAdd);
					System.out.println(
							mediumProcessestoAdd.size() + " medium processes with: " + i + " decisions added!");
				}

				LinkedList<File> largeProcessesWithoutAnnotation = new LinkedList<File>();
				for (int i = amountXorsLargeProcessesBounds.get(0); i <= amountXorsLargeProcessesBounds.get(1); i++) {
					LinkedList<File> largeProcessesToAdd = BatchFileGenerator
							.getModelsInOrderFromSourceFolderWithExactAmountDecision(modelsToTakePerDecision, i,
									pathToLargeProcessesFolderWithoutAnnotation);
					largeProcessesWithoutAnnotation.addAll(largeProcessesToAdd);
					System.out
							.println(largeProcessesToAdd.size() + " large processes with: " + i + " decisions added!");
				}

				BatchFileGenerator.performTradeOffTest("small", smallProcessesWithoutAnnotation,
						pathToSmallProcessesForTest2WithAnnotation, dataObjectBoundsSmallProcesses,
						amountSolutionsToBeGenerated, amountThreads);
				BatchFileGenerator.performTradeOffTest("medium", mediumProcessesWithoutAnnotation,
						pathToMediumProcessesForTest2WithAnnotation, dataObjectBoundsMediumProcesses,
						amountSolutionsToBeGenerated, amountThreads);
				BatchFileGenerator.performTradeOffTest("large", largeProcessesWithoutAnnotation,
						pathToLargeProcessesForTest2WithAnnotation, dataObjectBoundsLargeProcesses,
						amountSolutionsToBeGenerated, amountThreads);

				System.out.println("Test 2 finished!");
			}
		}

		if (methodsToRun.contains(test3ToRun)) {
			// Test 3 - Measure impact of dataObjects
			// annotate unique data objects for each decision
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

				boolean enoughModelsFound = true;
				int modelsToTakePerDecision = 10;
				LinkedList<Integer> allDecisions = new LinkedList<Integer>();
				// annotate models with max amount of unique dataObjects
				int amountUniqueDataObjectsPerDecision = 3;
				int maxAmountUniqueDataObjects = amountXorsMediumProcessesBounds.get(1)
						* amountUniqueDataObjectsPerDecision;

				LinkedList<File> smallProcessesWithoutAnnotationTest3 = new LinkedList<File>();
				for (int i = amountXorsSmallProcessesBounds.get(0); i <= amountXorsSmallProcessesBounds.get(1); i++) {
					if (enoughModelsFound) {
						LinkedList<File> smallProcessesFound = BatchFileGenerator
								.getModelsInOrderFromSourceFolderWithExactAmountDecisionsAndMinTasks(
										modelsToTakePerDecision, i, maxAmountUniqueDataObjects,
										pathToSmallProcessesFolderWithoutAnnotation);
						if (smallProcessesFound.size() < modelsToTakePerDecision) {
							System.err.println("Only " + smallProcessesFound.size() + " smallProcesses with " + i
									+ " decisions found instead of " + modelsToTakePerDecision);
							enoughModelsFound = false;
						} else {
							smallProcessesWithoutAnnotationTest3.addAll(smallProcessesFound);

							if (!allDecisions.contains(i)) {
								allDecisions.add(i);
							}
						}
					}
				}

				LinkedList<File> mediumProcessesWithoutAnnotationTest3 = new LinkedList<File>();
				for (int i = amountXorsMediumProcessesBounds.get(0); i <= amountXorsMediumProcessesBounds.get(1); i++) {
					if (enoughModelsFound) {
						LinkedList<File> mediumProcessesFound = BatchFileGenerator
								.getModelsInOrderFromSourceFolderWithExactAmountDecisionsAndMinTasks(
										modelsToTakePerDecision, i, maxAmountUniqueDataObjects,
										pathToMediumProcessesFolderWithoutAnnotation);
						if (mediumProcessesFound.size() < modelsToTakePerDecision) {
							System.err.println("Only " + mediumProcessesFound.size() + " mediumProcesses with " + i
									+ " decisions found instead of " + modelsToTakePerDecision);
							enoughModelsFound = false;
						} else {
							mediumProcessesWithoutAnnotationTest3.addAll(mediumProcessesFound);
							if (!allDecisions.contains(i)) {
								allDecisions.add(i);
							}
						}
					}
				}

				if (enoughModelsFound == false) {
					LinkedList<File> allProcessesWithoutAnnotationTest3 = new LinkedList<File>();
					allProcessesWithoutAnnotationTest3.addAll(smallProcessesWithoutAnnotationTest3);
					allProcessesWithoutAnnotationTest3.addAll(mediumProcessesWithoutAnnotationTest3);

					boolean performTest = true;

					for (int i = 1; i <= allDecisions.getLast(); i++) {
						int modulo = maxAmountUniqueDataObjects % i;
						if (modulo != 0) {
							System.err.println(maxAmountUniqueDataObjects + "%" + i + "=" + modulo + "! Should be 0!");
							performTest = false;
						}
					}

					if (performTest) {
						BatchFileGenerator.performDataObjectTest(allProcessesWithoutAnnotationTest3,
								pathToFolderForModelsForDataObjectTest, maxAmountUniqueDataObjects,
								amountSolutionsToBeGenerated, amountThreads);
						System.out.println("Test 3 finished!");
					}
				}
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
						decisionTakerExcludeable, pathToFolderForSmallModelsForTest4_1, amountSolutionsToBeGenerated,
						"small", amountThreads);

				String pathToFolderForMediumModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(mediumProcessesFromTradeOffTest,
						decisionTakerExcludeable, pathToFolderForMediumModelsForTest4_1, amountSolutionsToBeGenerated,
						"medium", amountThreads);

				String pathToFolderForLargeModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(largeProcessesFromTradeOffTest,
						decisionTakerExcludeable, pathToFolderForLargeModelsForTest4_1, amountSolutionsToBeGenerated,
						"large", amountThreads);

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
						false, probabilityForGatewayToHaveConstraint,
						pathToFolderForSmallModelsForTest4_2, amountSolutionsToBeGenerated, "small", amountThreads);

				String pathToFolderForMediumModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(mediumProcessesFromTradeOffTest,
						false, probabilityForGatewayToHaveConstraint,
						pathToFolderForMediumModelsForTest4_2, amountSolutionsToBeGenerated, "medium", amountThreads);

				String pathToFolderForLargeModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(largeProcessesFromTradeOffTest,
						false, probabilityForGatewayToHaveConstraint,
						pathToFolderForLargeModelsForTest4_2, amountSolutionsToBeGenerated, "large", amountThreads);

				System.out.println("Test 4_2 finished!");
			}
		}

		if (methodsToRun.contains(test5ToRun)) {
			// Test 5 - "mandatory participants"
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
						.fileWithDirectoryAssurance(pathToRootFolder, "Test5_MandadtoryConstraints").getAbsolutePath();

				String pathToFolderForSmallModelsForTest5 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "SmallModels").getAbsolutePath();

				String pathToFolderForMediumModelsForTest5 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "MediumModels").getAbsolutePath();

				String pathToFolderForLargeModelsForTest5 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "LargeModels").getAbsolutePath();

				BatchFileGenerator.performTestWithMandatoryConstraints(smallProcessesFromTradeOffTest, false,
						pathToFolderForSmallModelsForTest5, amountSolutionsToBeGenerated, "small", amountThreads);

				BatchFileGenerator.performTestWithMandatoryConstraints(mediumProcessesFromTradeOffTest, false,
						pathToFolderForMediumModelsForTest5, amountSolutionsToBeGenerated, "medium", amountThreads);

				BatchFileGenerator.performTestWithMandatoryConstraints(largeProcessesFromTradeOffTest, false,
						pathToFolderForLargeModelsForTest5, amountSolutionsToBeGenerated, "large", amountThreads);

				System.out.println("Test 5 finished!");

			}
		}

		if (methodsToRun.contains(test6ToRun)) {
			// Test 6 - "real world processes"
			// may contain exclude and mandatory participant constraint
			// may contain dynamic writers
			// has wider ranges for variables

			List<Integer> dataObjectBoundsRealWorld = Arrays.asList(1, 4);
			// int dynamicWriterProb = 30;
			int upperBoundParticipants = 6;
			int lowerBoundTasks = 8;
			int upperBoundTasks = 40;
			int upperBoundXorGtws = 10;
			int upperBoundParallelGtws = 6;
			int amountProcesses = 20;
			int minDataObjectsPerDecisionTest6 = dataObjectBoundsRealWorld.get(0);
			int maxDataObjectsPerDecisionTest6 = dataObjectBoundsRealWorld.get(1);
			int probabilityForGatewayToHaveExclConstraint = 30;
			int lowerBoundAmountParticipantsToExclude = 0;
			// upperBoundAmountParticipantsToExclude will be set inside the method
			// accordingly
			int upperBoundAmountParticipantsToExclude = -1;
			boolean alwaysMaxExclConstrained = false;
			int probabilityForGatewayToHaveMandConstraint = 30;
			int lowerBoundAmountParticipantsToBeMandatory = 0;
			// upperBoundAmountParticipantsToBeMandatory will be set inside the method
			// accordingly
			int upperBoundAmountParticipantsToBeMandatory = -1;
			boolean alwaysMaxMandConstrained = false;
			List<Integer> writersOfProcessInPercent = Arrays.asList(10, 20, 30);
			List<Integer> readersOfProcessInPercent = Arrays.asList(10, 20, 30);
			int amountSolutionsToBeGenerated = 1;

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
					alwaysMaxExclConstrained, probabilityForGatewayToHaveMandConstraint,
					lowerBoundAmountParticipantsToBeMandatory, upperBoundAmountParticipantsToBeMandatory,
					alwaysMaxMandConstrained, amountSolutionsToBeGenerated, amountThreads);
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
			boolean alwaysMaxExclConstrained, int probabilityForGatewayToHaveMandConstraint,
			int lowerBoundAmountParticipantsToBeMandatory, int upperBoundAmountParticipantsToBeMandatory,
			boolean alwaysMaxMandConstrained, int boundForSolutionsToBeGenerated, int amountThreads) {

		ExecutorService randomProcessGeneratorService = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathWhereToCreateProcessesWithoutAnnotation, 2,
				upperBoundParticipants, lowerBoundTasks, upperBoundTasks, 1, upperBoundXorGtws, 0,
				upperBoundParallelGtws, amountProcesses, randomProcessGeneratorService, false);
		randomProcessGeneratorService.shutdownNow();

		int publicDecisionProb = 0;

		BatchFileGenerator.annotateModels(pathWhereToCreateProcessesWithoutAnnotation,
				pathWhereToCreateAnnotatedProcesses, dataObjectBoundsRealWorld, defaultSpheres, dynamicWriterProb,
				writersOfProcessInPercent, readersOfProcessInPercent, 2, upperBoundParticipants,
				minDataObjectsPerDecision, maxDataObjectsPerDecision, publicDecisionProb,
				probabilityForGatewayToHaveExclConstraint, lowerBoundAmountParticipantsToExclude,
				lowerBoundAmountParticipantsToExclude, alwaysMaxExclConstrained,
				probabilityForGatewayToHaveMandConstraint, lowerBoundAmountParticipantsToBeMandatory,
				upperBoundAmountParticipantsToBeMandatory, alwaysMaxMandConstrained);

		ExecutorService service = Executors.newFixedThreadPool(amountThreads);
		File csv = BatchFileGenerator.createNewCSVFile(pathWhereToCreateAnnotatedProcesses, "test6_results");
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csv);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathWhereToCreateAnnotatedProcesses, true, true, true,
				true, boundForSolutionsToBeGenerated, writer, service);
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
			boolean alwaysMaxExclConstrained, int probabilityForGatewayToHaveMandConstraint,
			int lowerBoundAmountParticipantsToBeMandatory, int upperBoundAmountParticipantsToBeMandatory,
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
			int privateSphere = CommonFunctionality.getPrivateSphere(modelInstance, false);
			if (amountParticipantsPerDecisionUpperBound <= 0
					|| amountParticipantsPerDecisionUpperBound > privateSphere) {
				if (amountParticipantsPerDecisionLowerBound == privateSphere) {
					amountParticipantsPerDecisionUpperBound = privateSphere;
				} else if (amountParticipantsPerDecisionLowerBound < privateSphere) {
					amountParticipantsPerDecisionUpperBound = ThreadLocalRandom.current()
							.nextInt(amountParticipantsPerDecisionLowerBound, privateSphere + 1);

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

					// set upperBoundAmountParticipantsToExclude to be max privateSphereSize-1
					if (upperBoundAmountParticipantsToExclude <= 0) {
						upperBoundAmountParticipantsToExclude = ThreadLocalRandom.current()
								.nextInt(lowerBoundAmountParticipantsToExclude, privateSphere);
					}

					Object[] argumentsForFifthMethod = new Object[4];
					argumentsForFifthMethod[0] = probabilityForGatewayToHaveExcludeConstraint;
					argumentsForFifthMethod[1] = lowerBoundAmountParticipantsToExclude;
					argumentsForFifthMethod[2] = upperBoundAmountParticipantsToExclude;
					argumentsForFifthMethod[3] = alwaysMaxExclConstrained;
					methodsToBeCalledMap.putIfAbsent(fifthMethodToBeCalledName, argumentsForFifthMethod);
				}

				if (probabilityForGatewayToHaveMandConstraint > 0) {
					String sixthMethodToBeCalledName = "addMandatoryParticipantConstraintsOnModel";

					// set upperBoundAmountParticipantsToExclude to be max privateSphereSize-1
					if (upperBoundAmountParticipantsToBeMandatory <= 0) {
						upperBoundAmountParticipantsToBeMandatory = ThreadLocalRandom.current()
								.nextInt(lowerBoundAmountParticipantsToBeMandatory, privateSphere);
					}

					Object[] argumentsForSixthMethod = new Object[4];
					argumentsForSixthMethod[0] = probabilityForGatewayToHaveMandConstraint;
					argumentsForSixthMethod[1] = lowerBoundAmountParticipantsToBeMandatory;
					argumentsForSixthMethod[2] = upperBoundAmountParticipantsToBeMandatory;
					argumentsForSixthMethod[3] = alwaysMaxMandConstrained;
					methodsToBeCalledMap.putIfAbsent(sixthMethodToBeCalledName, argumentsForSixthMethod);
				}
				// set the methods that will be run within the call method
				p.setMethodsToRunWithinCall(methodsToBeCalledMap);

				Future<File> future = executor.submit(p);
				futures.add(future);
				try {
					future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
					future.cancel(true);
					CommonFunctionality.removeDataObjectsWithoutConnectionsToDecisionsFromModel(p.getModelInstance());

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

	public static void generateRandomProcessesWithinGivenRanges(String pathToFiles, int lowerBoundParticipants,
			int upperBoundParticipants, int lowerBoundTasks, int upperBoundTasks, int lowerBoundXorGtws,
			int upperBoundXorGtws, int lowerBoundParallelGtws, int upperBoundParallelGtws, int amountProcesses,
			ExecutorService executor, boolean testForNodesBeforeGtws) {

		for (int i = 1; i <= amountProcesses; i++) {

			int randomAmountParticipantsWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundParticipants,
					upperBoundParticipants + 1);
			int randomAmountTasksWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundTasks,
					upperBoundTasks + 1);
			int randomAmountXorSplitsWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundXorGtws,
					upperBoundXorGtws + 1);
			int randomAmountParallelSplitsWithinBounds = ThreadLocalRandom.current().nextInt(lowerBoundParallelGtws,
					upperBoundParallelGtws + 1);

			// substract the amount of max xors that will be inserted
			// since brts will have to be inserted afterwards
			// also substract 1, if a tasks needs to be inserted after start node
			if (randomAmountXorSplitsWithinBounds > 0) {
				randomAmountTasksWithinBounds = randomAmountTasksWithinBounds - randomAmountXorSplitsWithinBounds - 1;
			}

			int processId = 0;
			try {
				int nestingDepthFactor = ThreadLocalRandom.current().nextInt(nestingDepthFactorBounds.get(0),
						nestingDepthFactorBounds.get(1) + 1);
				int probJoinGtw = ThreadLocalRandom.current().nextInt(probJoinGtwBounds.get(0),
						probJoinGtwBounds.get(1) + 1);
				ProcessGenerator pGen = new ProcessGenerator(pathToFiles, randomAmountParticipantsWithinBounds,
						randomAmountTasksWithinBounds, randomAmountXorSplitsWithinBounds,
						randomAmountParallelSplitsWithinBounds, probTask, probXorGtw, probParallelGtw, probJoinGtw,
						nestingDepthFactor, maxTriesForGeneratingProcess);

				processId = pGen.getProcessId();
				Future<File> future = executor.submit(pGen);
				try {
					File f = future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
					future.cancel(true);
					// all variables of the generated file must be within the bounds
					if (f != null && f.isFile()) {
						InputStream inputStream = new FileInputStream(f);
						BpmnModelInstance createdModel = Bpmn.readModelFromStream(inputStream);
						inputStream.close();
						boolean deleteModel = false;
						int tasksOfCreatedModel = createdModel.getModelElementsByType(Task.class).size();

						if (testForNodesBeforeGtws) {
							int amountNodesToBeInserted = 0;
							FlowNode nodeAfterStartEvent = createdModel.getModelElementsByType(StartEvent.class)
									.iterator().next().getOutgoing().iterator().next().getTarget();
							if (nodeAfterStartEvent instanceof ParallelGateway
									|| nodeAfterStartEvent instanceof ExclusiveGateway
									|| nodeAfterStartEvent instanceof BusinessRuleTask) {
								// writer task will be inserted in front
								amountNodesToBeInserted++;
							}

							for (ExclusiveGateway gtw : createdModel.getModelElementsByType(ExclusiveGateway.class)) {
								if (gtw.getOutgoing().size() >= 2) {
									// if xor is a split
									// there will be a brt inserted before
									amountNodesToBeInserted++;
								}

							}

							int amountTasksOfModelAfterNodesToBeInserted = tasksOfCreatedModel
									+ amountNodesToBeInserted;
							if (amountTasksOfModelAfterNodesToBeInserted > upperBoundTasks) {
								deleteModel = true;
								System.out.println("Tasks of model after nodes to be inserted: "
										+ amountTasksOfModelAfterNodesToBeInserted + " > upperBoundTasks "
										+ upperBoundTasks);
							} else if (amountTasksOfModelAfterNodesToBeInserted < lowerBoundTasks) {
								deleteModel = true;
								System.out.println("Tasks of model after nodes to be inserted: "
										+ amountTasksOfModelAfterNodesToBeInserted + " < lowerBoundTasks "
										+ lowerBoundTasks);
							}
						}

						if (deleteModel) {
							System.out.println(f.getName() + " deleted: " + f.delete() + "!");
							i--;
							ProcessGenerator.decreaseProcessGeneratorId();
						}

					} else if (f == null) {
						i--;
						ProcessGenerator.decreaseProcessGeneratorId();
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

			if (processId != 0) {
				System.out.println("randomProcess" + processId + " deployed in " + pathToFiles);
			}
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
			File f = new File(pathToFile);
			try {
				API api = new API(pathToFile, costParameters);
				apiList.add(api);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				writer.addNullValueRowForModel(f.getName(), f.getAbsolutePath(), e.getMessage());
				System.err.println(f.getAbsolutePath() + " could not be mapped to api: " + e.getMessage());
			}

		}

		return apiList;
	}

	public static LinkedList<API> mapFilesToAPI(LinkedList<File> files, ResultsToCSVWriter writer) {
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
				API api = new API(pathToFile, costParameters);
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

	static File createNewCSVFile(String directoryForFile, String nameOfCSVFile) {
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

	public static HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> runAlgsAndWriteResults(API api,
			boolean runExhaustiveSearch, boolean runHeuristicSearch, boolean runNaiveSearch,
			boolean runIncrementalNaiveSearch, int amountSolutionsToGenerateWithNaiveAndHeuristic,
			HashMap<Enums.AlgorithmToPerform, Integer> timeOutOrHeapSpaceExceptionMap, ResultsToCSVWriter writer,
			ExecutorService service) {

		// only hold the cost of the cheapest solutions found with the algorithms in
		// memory
		// storing all possible solutions in memory will influence execution time of
		// other algorithms drastically with increasing amount of combinations!!!

		HashMap<Enums.AlgorithmToPerform, Exception> exceptionPerAlgorithm = new HashMap<Enums.AlgorithmToPerform, Exception>();
		HashMap<Enums.AlgorithmToPerform, Integer> cheapestSolutionsMap = new HashMap<Enums.AlgorithmToPerform, Integer>();
		HashMap<Enums.AlgorithmToPerform, Integer> totalAmountSolutionsMap = new HashMap<Enums.AlgorithmToPerform, Integer>();
		HashMap<Enums.AlgorithmToPerform, Double> cheapestSolutionCostMap = new HashMap<Enums.AlgorithmToPerform, Double>();
		HashMap<Enums.AlgorithmToPerform, String> loggingMap = new HashMap<Enums.AlgorithmToPerform, String>();

		boolean heapSpaceError = false;
		try {
			if (runExhaustiveSearch) {
				heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.EXHAUSTIVE, 0, exceptionPerAlgorithm,
						cheapestSolutionsMap, totalAmountSolutionsMap, cheapestSolutionCostMap, loggingMap,
						timeOutOrHeapSpaceExceptionMap, service);
			}

			if (runHeuristicSearch) {
				heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.HEURISTIC,
						amountSolutionsToGenerateWithNaiveAndHeuristic, exceptionPerAlgorithm, cheapestSolutionsMap,
						totalAmountSolutionsMap, cheapestSolutionCostMap, loggingMap, timeOutOrHeapSpaceExceptionMap,
						service);
			}

			if (runNaiveSearch) {
				heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.NAIVE,
						amountSolutionsToGenerateWithNaiveAndHeuristic, exceptionPerAlgorithm, cheapestSolutionsMap,
						totalAmountSolutionsMap, cheapestSolutionCostMap, loggingMap, timeOutOrHeapSpaceExceptionMap,
						service);
			}

			if (runIncrementalNaiveSearch) {
				heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.INCREMENTALNAIVE,
						amountSolutionsToGenerateWithNaiveAndHeuristic, exceptionPerAlgorithm, cheapestSolutionsMap,
						totalAmountSolutionsMap, cheapestSolutionCostMap, loggingMap, timeOutOrHeapSpaceExceptionMap,
						service);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.writeResultsOfAlgorithmsToCSVFile(api, cheapestSolutionCostMap, totalAmountSolutionsMap,
					cheapestSolutionsMap, loggingMap, exceptionPerAlgorithm);
		}

		HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = new HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>>();
		returnMap.putIfAbsent(heapSpaceError, timeOutOrHeapSpaceExceptionMap);
		return returnMap;
	}

	public static synchronized boolean runAlgorithm(API api, Enums.AlgorithmToPerform algToPerform,
			int amountSolutionsToGenerate, HashMap<Enums.AlgorithmToPerform, Exception> exceptionPerAlgorithm,
			HashMap<Enums.AlgorithmToPerform, Integer> cheapestSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, Integer> totalAmountSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, Double> cheapestSolutionCostMap,
			HashMap<Enums.AlgorithmToPerform, String> loggingMap,
			HashMap<Enums.AlgorithmToPerform, Integer> timeOutOrHeapSpaceExceptionMap, ExecutorService service) {

		HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>> currentSolutionsMap = null;

		boolean heapSpaceError = false;
		String logging = "null";

		api.setAlgorithmToPerform(algToPerform, amountSolutionsToGenerate);

		Future<HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>>> futureAlgSearch = service
				.submit(api);
		Exception exception = null;
		try {
			currentSolutionsMap = futureAlgSearch.get(timeOutForApiInMin, TimeUnit.MINUTES);
			LinkedList<PModelWithAdditionalActors> solutions = currentSolutionsMap.get(algToPerform);
			// get the total amount of solutions
			totalAmountSolutionsMap.putIfAbsent(algToPerform, solutions.size());
			// get the amount of cheapest solutions
			LinkedList<PModelWithAdditionalActors> cheapestModels = CommonFunctionality
					.getCheapestPModelsWithAdditionalActors(solutions);
			cheapestSolutionsMap.putIfAbsent(algToPerform, cheapestModels.size());

			if (!cheapestModels.isEmpty()) {
				// get the cost of the cheapest solutions
				cheapestSolutionCostMap.putIfAbsent(algToPerform, cheapestModels.get(0).getSumMeasure());
			}
			futureAlgSearch.cancel(true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			exception = (InterruptedException) e;
			e.printStackTrace();
			futureAlgSearch.cancel(true);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			if (e.getCause() instanceof NotEnoughAddActorsException) {
				exception = new NotEnoughAddActorsException(e.getMessage(), e.getCause());
				logging = e.getMessage();
			} else if (e.getCause() instanceof OutOfMemoryError) {
				exception = new HeapSpaceException(e.getMessage(), e.getCause());
				timeOutOrHeapSpaceExceptionMap.put(algToPerform,
						timeOutOrHeapSpaceExceptionMap.getOrDefault(algToPerform, 0) + 1);
				heapSpaceError = true;
			} else {
				exception = (ExecutionException) e;
			}
			futureAlgSearch.cancel(true);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			exception = (TimeoutException) e;
			timeOutOrHeapSpaceExceptionMap.put(algToPerform,
					timeOutOrHeapSpaceExceptionMap.getOrDefault(algToPerform, 0) + 1);
			System.err.println("Timeout for " + algToPerform.toString() + " search!");
			futureAlgSearch.cancel(true);
		} catch (Error e) {
			System.gc();
			if (e instanceof OutOfMemoryError) {
				exception = new HeapSpaceException(e.getMessage(), e.getCause());
				timeOutOrHeapSpaceExceptionMap.put(algToPerform,
						timeOutOrHeapSpaceExceptionMap.getOrDefault(algToPerform, 0) + 1);
				System.err.println(algToPerform.toString() + " search ran out of memory");
				heapSpaceError = true;
			}
			futureAlgSearch.cancel(true);
		} finally {
			futureAlgSearch.cancel(true);
			exceptionPerAlgorithm.putIfAbsent(algToPerform, exception);
			loggingMap.putIfAbsent(algToPerform, logging);
		}

		return heapSpaceError;

	}

	public static void runAlgorithmsAndWriteResultsToCSV(LinkedList<File> files, boolean runExhaustiveSearch,
			boolean runHeuristicSearch, boolean runNaiveSearch, boolean runIncrementalNaiveSearch,
			int boundForSolutions, ResultsToCSVWriter writer, ExecutorService service) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(files, writer);
		if (!apiList.isEmpty()) {
			for (API api : apiList) {
				BatchFileGenerator.runAlgsAndWriteResults(api, runExhaustiveSearch, runHeuristicSearch,
						runIncrementalNaiveSearch, runIncrementalNaiveSearch, boundForSolutions,
						new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels,
			boolean runExhaustiveSearch, boolean runHeuristicSearch, boolean runHeuristicSearchWithBound,
			boolean runNaiveSearch, int boundForHeuristicSearchWithBound, ResultsToCSVWriter writer,
			ExecutorService service) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithAnnotatedModels, writer);
		if (!apiList.isEmpty()) {
			for (API api : apiList) {
				BatchFileGenerator.runAlgsAndWriteResults(api, runExhaustiveSearch, runHeuristicSearch,
						runHeuristicSearchWithBound, runNaiveSearch, boundForHeuristicSearchWithBound,
						new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void performDataObjectTest(LinkedList<File> processes, String pathToDestinationFolderForStoringModels,
			int maxAmountUniqueDataObjects, int upperBoundLocalMinWithBound, int amountThreadPools) throws IOException {
		// each decision has unique dataObject sets of same size
		// e.g. model has 3 decisions -> and maxAmountUniqueDataObjects = 12 -> each
		// decision will have 4 unique dataObjects
		// unique dataObjects need to be connected
		// -> at least 12 readers and 12 writers needed, since every data object will
		// have to be first written and then get read by some brt

		// amount additional actors per gtw will be between 2 and privateSphere

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
					int privateSphere = CommonFunctionality.getPrivateSphere(processModel, false);
					int amountAddActorsUpperBound = ThreadLocalRandom.current().nextInt(2, privateSphere + 1);

					boolean modelIsValid = false;
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
							amountTasks = CommonFunctionality.getAmountTasks(pModel.getModelInstance());

							if (amountTasks > maxAmountUniqueDataObjects) {
								int minDataObjectsPerDecision = maxAmountUniqueDataObjects / amountDecisions;
								pModel.generateDataObjects(maxAmountUniqueDataObjects, sphere);
								pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(minDataObjectsPerDecision,
										minDataObjectsPerDecision, 2, amountAddActorsUpperBound, 0, true);
								int amountWritersNeededForDataObjects = maxAmountUniqueDataObjects;
								int amountReadersNeededForDataObjects = maxAmountUniqueDataObjects;

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
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true, true,
				upperBoundLocalMinWithBound, writer, executor);
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

	public static LinkedList<File> getModelsInOrderFromSourceFolderWithExactAmountDecision(int amountModelsPerDecision,
			int amountDecisions, String pathToSourceFolder) {
		File folder = new File(pathToSourceFolder);
		LinkedList<File> listOfFiles = new LinkedList<File>();
		listOfFiles.addAll(Arrays.asList(folder.listFiles()));

		LinkedList<File> modelsFromSourceFolder = new LinkedList<File>();
		for (int listOfFilesIndex = 0; listOfFilesIndex < listOfFiles.size(); listOfFilesIndex++) {
			if (modelsFromSourceFolder.size() == amountModelsPerDecision) {
				break;
			}
			File model = listOfFiles.get(listOfFilesIndex);
			if (!model.getName().contains(".csv")) {
				BpmnModelInstance modelInst = Bpmn.readModelFromFile(model);
				int modelInstDecisions = CommonFunctionality.getAmountExclusiveGtwSplits(modelInst);
				if (modelInstDecisions == amountDecisions) {
					modelsFromSourceFolder.add(model);
				}
			}
		}

		return modelsFromSourceFolder;
	}

	public static LinkedList<File> getModelsInOrderFromSourceFolderWithExactAmountDecisionsAndMinTasks(
			int amountModelsPerDecision, int amountDecisions, int minAmountTasks, String pathToSourceFolder) {
		File folder = new File(pathToSourceFolder);
		LinkedList<File> listOfFiles = new LinkedList<File>();
		listOfFiles.addAll(Arrays.asList(folder.listFiles()));

		LinkedList<File> modelsFromSourceFolder = new LinkedList<File>();
		for (int listOfFilesIndex = 0; listOfFilesIndex < listOfFiles.size(); listOfFilesIndex++) {
			if (modelsFromSourceFolder.size() == amountModelsPerDecision) {
				break;
			}
			File model = listOfFiles.get(listOfFilesIndex);
			if (!model.getName().contains(".csv")) {
				BpmnModelInstance modelInst = Bpmn.readModelFromFile(model);
				int modelInstDecisions = CommonFunctionality.getAmountExclusiveGtwSplits(modelInst);
				int amountTasks = CommonFunctionality.getAmountTasks(modelInst);
				if (modelInstDecisions == amountDecisions && amountTasks >= minAmountTasks) {
					modelsFromSourceFolder.add(model);
				}
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
			int upperBoundSolutionsForLocalMinWithBound, int amountThreadPools) {

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
			int tries = 0;
			while (modelIsValid == false && tries < triesForModelAnnotater) {
				try {
					ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess,
							pathToDestinationFolderForStoringModels, "");

					// randomly generate dataObjects in the range [DataObjects, maxCountDataObjects]
					amountRandomCountDataObjectsToCreate = ThreadLocalRandom.current().nextInt(dataObjectBounds.get(0),
							dataObjectBounds.get(1) + 1);

					// create model with amountRandomCountDataObjects dataObjects, 0 additional
					// actors and
					// empty string as starting sphere
					pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, emptySphere);
					// connect between 1 and amountRandomCountDataObjectsToCreate per decision
					pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(1, amountRandomCountDataObjectsToCreate, 0, 0,
							0, false);

					CommonFunctionality
							.removeDataObjectsWithoutConnectionsToDecisionsFromModel(pModel.getModelInstance());

					newModel = pModel.writeChangesToFileWithoutCorrectnessCheck();
					modelIsValid = true;
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
				tries++;
			}

			if (newModel != null) {
				for (int writerClass = 0; writerClass < percentageOfWritersClasses.size(); writerClass++) {
					// for each model -> annotate it with small, medium, large amount of writers
					BpmnModelInstance newModelInstance = Bpmn.readModelFromFile(newModel);

					int minAmountWriters = newModelInstance.getModelElementsByType(DataObjectReference.class).size();

					int amountWriterTasksToBeInserted = CommonFunctionality.getAmountFromPercentageWithMinimum(
							amountTasks, percentageOfWritersClasses.get(writerClass), minAmountWriters);

					for (int readerClass = 0; readerClass < percentageOfReadersClasses.size(); readerClass++) {
						// for each model -> annotate it with small, medium, large amount of readers
						int triesForInsertingReadersAndWriters = 50;
						boolean modelAnnotatedCorrectly = false;
						Exception ex = null;
						int amountReaderTasksToBeInserted = CommonFunctionality.getAmountFromPercentage(amountTasks,
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

						String suffix = suffixBuilder.toString();

						int currAmountReaderTasksInModel = newModelInstance
								.getModelElementsByType(DataInputAssociation.class).size();
						boolean insertWriters = true;
						if (currAmountReaderTasksInModel > amountReaderTasksToBeInserted) {
							// model out of bound
							insertWriters = false;
						}

						int amountWriterTasksForPercentage = CommonFunctionality.getAmountFromPercentage(amountTasks,
								percentageOfWritersClasses.get(writerClass));
						if (amountWriterTasksToBeInserted > amountWriterTasksForPercentage) {
							// model out of bound
							insertWriters = false;
						}

						if (insertWriters) {
							while (triesForInsertingReadersAndWriters > 0 && modelAnnotatedCorrectly == false) {
								ProcessModelAnnotater modelWithReadersAndWriters;
								try {
									modelWithReadersAndWriters = new ProcessModelAnnotater(newModel.getAbsolutePath(),
											pathToDestinationFolderForStoringModels, suffix);
									modelWithReadersAndWriters.setDataObjectsConnectedToBrts(true);

									// add the methods to be called
									LinkedHashMap<String, Object[]> methodsToBeCalledMap = new LinkedHashMap<String, Object[]>();

									String firstMethodToBeCalledName = "addNamesForOutgoingFlowsOfXorSplits";
									Object[] argumentsForFirstMethod = new Object[1];
									argumentsForFirstMethod[0] = defaultNamesSeqFlowsXorSplits;
									methodsToBeCalledMap.putIfAbsent(firstMethodToBeCalledName,
											argumentsForFirstMethod);

									String secondMethodToBeCalledName = "annotateModelWithFixedAmountOfReadersAndWriters";
									Object[] argumentsForSecondMethod = new Object[4];
									argumentsForSecondMethod[0] = amountWriterTasksToBeInserted;
									argumentsForSecondMethod[1] = amountReaderTasksToBeInserted;
									argumentsForSecondMethod[2] = 0;
									argumentsForSecondMethod[3] = emptySphere;
									methodsToBeCalledMap.putIfAbsent(secondMethodToBeCalledName,
											argumentsForSecondMethod);

									modelWithReadersAndWriters.setMethodsToRunWithinCall(methodsToBeCalledMap);

									Future<File> future = executor.submit(modelWithReadersAndWriters);
									try {
										future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
										future.cancel(true);
										modelAnnotatedCorrectly = true;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										future.cancel(true);
										e.printStackTrace();
										ex = e;
									} catch (ExecutionException e) {
										// TODO Auto-generated catch block
										System.err.println(e.getMessage());
										future.cancel(true);
										ex = e;
									} catch (TimeoutException e) {
										// TODO Auto-generated catch block
										future.cancel(true);
										e.printStackTrace();
										ex = e;
									}

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
									ex = e;
								}
								if (ex != null) {
									triesForInsertingReadersAndWriters--;
									System.out.println(
											"Tries left for annotating model: " + triesForInsertingReadersAndWriters);
								}

							}
						} else {
							// model can not be annotated with the current readers/writers combination
							// skip this combination
							triesForInsertingReadersAndWriters = 0;
						}
					}
				}
			}
		}
		executor.shutdownNow();
		// for each model that has been generated with 0 additional actors per decision
		// and
		// empty sphere annotated for each dataObject
		// -> generate new ones where the amount of additional actors is increased by 1
		// on each
		// step till the models
		// privateSphereSize-1 is reached
		// -> generate new ones where privity requirement is increased to next stricter
		// sphere till Strong-Dynamic is reached on all dataObjects

		String pathToFolderWithModelsForEvaluation = CommonFunctionality
				.fileWithDirectoryAssurance(pathToDestinationFolderForStoringModels, "ModelsForEvaluation")
				.getAbsolutePath();
		File directory = new File(pathToDestinationFolderForStoringModels);
		File[] directoryList = directory.listFiles();
		LinkedList<File> modelsToEvaluate = new LinkedList<File>();
		for (int modelIndex = 0; modelIndex < directoryList.length; modelIndex++) {
			File annotatedModel = directoryList[modelIndex];
			if (annotatedModel.isFile() && annotatedModel.getName().contains(".bpmn")) {
				try {
					BpmnModelInstance currModel = Bpmn.readModelFromFile(annotatedModel);

					int privateSphereSize = CommonFunctionality.getPrivateSphere(currModel, false);

					if (annotatedModel.getName().contains("sWsR") || annotatedModel.getName().contains("sWmR")
							|| annotatedModel.getName().contains("sWlR") || annotatedModel.getName().contains("mWsR")
							|| annotatedModel.getName().contains("mWmR") || annotatedModel.getName().contains("mWlR")
							|| annotatedModel.getName().contains("lWsR") || annotatedModel.getName().contains("lWmR")
							|| annotatedModel.getName().contains("lWlR")) {

						for (int spheresIndex = 0; spheresIndex < defaultSpheres.size(); spheresIndex++) {
							String currentSphere = defaultSpheres.get(spheresIndex);
							CommonFunctionality.increaseSpherePerDataObject(currModel, currentSphere);

							for (int addActorsAmountIndex = 1; addActorsAmountIndex <= privateSphereSize
									- 1; addActorsAmountIndex++) {
								String suffix = "_" + currentSphere + "_addActors" + addActorsAmountIndex;
								CommonFunctionality.increaseAdditionalActorsPerDataObject(currModel,
										addActorsAmountIndex);
								String modelName = annotatedModel.getName().substring(0,
										annotatedModel.getName().indexOf(".bpmn"));
								try {

									File file = CommonFunctionality.writeChangesToFile(currModel, modelName,
											pathToFolderWithModelsForEvaluation, suffix);
									modelsToEvaluate.add(file);
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
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true, true,
				upperBoundSolutionsForLocalMinWithBound, writer, service);
		service.shutdownNow();
		writer.writeRowsToCSVAndcloseWriter();

	}

	public static void performBoundaryTest1_1(int amountProcessesPerIteration, int amountDecisionsToStart,
			int addActorsPerDecision, int privateSphere, int upperBoundSolutionsForLocalMinWithBound,
			int amountTasksToStartWith, int tasksFactor, int lowerBoundAmountParallelGtws,
			int upperBoundAmountParallelGtws, int amountWritersInPercent, int amountReadersInPercent,
			int minDataObjectsPerDecision, int maxDataObjectsPerDecision, LinkedList<String> sphereList,
			int amountThreads, int stopAfterDecisions, String pathToFolderForModelsForBoundaryTest) {

		int amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
				upperBoundAmountParallelGtws + 1);

		File csvFile = BatchFileGenerator.createNewCSVFile(pathToFolderForModelsForBoundaryTest, "test1_1-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		HashMap<Enums.AlgorithmToPerform, Integer> timeOutOrHeapSpaceExceptionMap = new HashMap<Enums.AlgorithmToPerform, Integer>();

		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		boolean outOfMemoryError = false;
		boolean runExhaustiveSearch = true;
		boolean runHeuristicSearch = true;
		boolean runNaiveSearch = true;
		boolean runIncrementalNaiveSearch = true;
		boolean finishTest = false;
		try {
		int i = 0;
		do {
			timeOutOrHeapSpaceExceptionMap.clear();
			timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
			timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTIC, 0);
			timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.NAIVE, 0);
			timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.INCREMENTALNAIVE, 0);

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
			BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToFolderForModelsWithDecisions,
					privateSphere, privateSphere, amountTasksWithFactor, amountTasksWithFactor, amountDecisionsToStart,
					amountDecisionsToStart, amountParallelGtws, amountParallelGtws, amountProcessesPerIteration,
					executor, false);

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
						argumentsForSecondMethod[1] = sphereList;
						methodsToBeCalledMap.putIfAbsent(secondMethodToBeCalledName, argumentsForSecondMethod);

						String thirdMethodToBeCalledName = "connectDataObjectsToBrtsAndTuplesForXorSplits";
						Object[] argumentsForThirdMethod = new Object[6];
						argumentsForThirdMethod[0] = minDataObjectsPerDecision;
						argumentsForThirdMethod[1] = maxDataObjectsPerDecision;
						argumentsForThirdMethod[2] = addActorsPerDecision;
						argumentsForThirdMethod[3] = addActorsPerDecision;
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

			// perform all algorithms and count the timeouts
			for (API api : apiList) {
				HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = BatchFileGenerator
						.runAlgsAndWriteResults(api, runExhaustiveSearch, runHeuristicSearch, runNaiveSearch,
								runIncrementalNaiveSearch, upperBoundSolutionsForLocalMinWithBound,
								timeOutOrHeapSpaceExceptionMap, writer, executor);
				if (returnMap.get(true) != null) {
					outOfMemoryError = true;
					timeOutOrHeapSpaceExceptionMap = returnMap.get(true);
				} else {
					timeOutOrHeapSpaceExceptionMap = returnMap.get(false);
				}

			}

			if (!apiList.isEmpty()) {
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) == apiList.size()) {
					runExhaustiveSearch = false;
				}
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.HEURISTIC) == apiList.size()) {
					runHeuristicSearch = false;
				}
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.NAIVE) == apiList.size()) {
					runNaiveSearch = false;
				}
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) == apiList.size()) {
					runIncrementalNaiveSearch = false;
				}
			}

			System.out.println("Iteration" + amountDecisionsToStart + " end - timeOutsExhaustiveSearch: "
					+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE)
					+ ", timeOutsHeuristicSearch: "
					+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.HEURISTIC) + ", timeOutsNaiveSearch: "
					+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.NAIVE)
					+ ", timeOutsIncrementalNaiveSearch: "
					+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE));

			amountDecisionsToStart++;

			if (runExhaustiveSearch == false && runHeuristicSearch == false && runNaiveSearch == false
					&& runIncrementalNaiveSearch == false) {
				finishTest = true;
			}

			i++;
			if (i == stopAfterDecisions) {
				finishTest = true;
			}
		} while ((!finishTest) && (!outOfMemoryError));

		executor.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		writer.writeRowsToCSVAndcloseWriter();
		}

	}

	public static void performBoundaryTest1_2(File file, int privateSphereLowerBound,
			int amountNewProcessesToCreatePerIteration, int boundForAlgorithms, int amountThreadPools,
			String directoryToStore) {

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStore, "test1_2-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		try {

		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		CommonFunctionality.generateNewModelsUntilPrivateSphereReached(file, privateSphereLowerBound,
				amountNewProcessesToCreatePerIteration, directoryToStore);

		// map annotated models
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(directoryToStore, writer);
		boolean outOfMemoryException = false;
		// perform all algorithms and count the timeouts
		for (API api : apiList) {
			HashMap<Enums.AlgorithmToPerform, Integer> timeOutMap = new HashMap<Enums.AlgorithmToPerform, Integer>();

			timeOutMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
			timeOutMap.put(Enums.AlgorithmToPerform.HEURISTIC, 0);
			timeOutMap.put(Enums.AlgorithmToPerform.NAIVE, 0);
			timeOutMap.put(Enums.AlgorithmToPerform.INCREMENTALNAIVE, 0);

			HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = BatchFileGenerator
					.runAlgsAndWriteResults(api, true, true, true, true, boundForAlgorithms, timeOutMap, writer,
							executor);
			if (returnMap.get(true) != null) {
				outOfMemoryException = true;
				timeOutMap = returnMap.get(true);
			} else {
				timeOutMap = returnMap.get(false);
			}

			if ((timeOutMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) == 1
					&& timeOutMap.get(Enums.AlgorithmToPerform.HEURISTIC) == 1
					&& timeOutMap.get(Enums.AlgorithmToPerform.NAIVE) == 1
					&& timeOutMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) == 1)
					|| outOfMemoryException == true) {
				break;
			}
		}
		executor.shutdownNow();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		writer.writeRowsToCSVAndcloseWriter();
		}
	}

	public static void substituteDataObjectAndWriteNewModels(BpmnModelInstance modelInstance, String modelName,
			String directoryToStore) {

		LinkedList<DataObjectReference> daoList = new LinkedList<DataObjectReference>();
		daoList.addAll(modelInstance.getModelElementsByType(DataObjectReference.class));
		DataObjectReference substitute = CommonFunctionality.getRandomItem(daoList);

		CommonFunctionality.substituteOneDataObjectPerIterationAndWriteNewModels(modelInstance, substitute, modelName,
				directoryToStore);

	}

	public static void performTestWithMandatoryConstraints(LinkedList<File> models, boolean modelWithLanes,
			String directoryToStoreNewModels, int boundSolutions, String suffixCSV,
			int amountThreads) {
		
		// each brt will have mandatory participants
		int probabilityForGatwayToHaveMandPartConst = 100;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				CommonFunctionality.addMandatoryParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatwayToHaveMandPartConst, 1, false, modelWithLanes,
						directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels, "test5_results_" + suffixCSV);

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true, true,
				boundSolutions, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();

	}

	public static void performTestWithMaxPossibleExcludeConstraints(LinkedList<File> models, boolean modelWithLanes,
			String directoryToStoreNewModels, int upperBoundHeuristicSearchWithBound, String suffixCSV,
			int amountThreads) {
		int probabilityForGatewayToHaveConstraint = 100;
		int lowerBoundAmountParticipantsToExcludePerGtw = 1;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				// will always be max. constrained
				CommonFunctionality.addExcludeParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatewayToHaveConstraint, lowerBoundAmountParticipantsToExcludePerGtw, true,
						modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels, "test4_1-results_" + suffixCSV);
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true, true,
				upperBoundHeuristicSearchWithBound, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();

	}

	public static void performTestWithExcludeConstraintsProbability(LinkedList<File> models,
			 boolean modelWithLanes, int probabilityForGatewayToHaveConstraint,
			String directoryToStoreNewModels, int upperBoundHeuristicSearchWithBound, String suffixCSV,
			int amountThreads) {
		int lowerBoundAmountParticipantsToExcludePerGtw = 1;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				CommonFunctionality.addExcludeParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatewayToHaveConstraint, lowerBoundAmountParticipantsToExcludePerGtw,
						false, modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels,
				"test4_2-prob" + probabilityForGatewayToHaveConstraint + "_results_" + suffixCSV);

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true, true,
				upperBoundHeuristicSearchWithBound, writer, executor);
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
