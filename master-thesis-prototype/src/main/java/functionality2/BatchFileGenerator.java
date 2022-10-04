package functionality2;

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
	static int triesForModelAnnotater = 50;

	// bound for the bounded localMinAlgorithm
	static int boundForHeuristicSearch = 1;

	static int amountThreads = 1;

	// bounds for ProcessModelGenerator
	static int probTask = 50;
	static int probXorGtw = 30;
	static int probParallelGtw = 20;

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

	// bounds for nestingDepthFactor and probJoinGtw
	static ArrayList<Integer> nestingDepthFactorBounds = new ArrayList<>(Arrays.asList(1, 25));
	static ArrayList<Integer> probJoinGtwBounds = new ArrayList<>(Arrays.asList(1, 25));

	public static void main(String[] args) throws Exception {
		String pathToRootFolder = CommonFunctionality2.fileWithDirectoryAssurance(root, "EvaluationSetup")
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
		/*methodsToRun.add(test1_2ToRun);
		methodsToRun.add(createRandomProcesses);
		methodsToRun.add(test2ToRun);
		methodsToRun.add(test3ToRun);
		methodsToRun.add(test4_1ToRun);
		methodsToRun.add(test4_2ToRun);
		methodsToRun.add(test5ToRun);*/
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
			// e.g. 2 decisions - 1. needs 2 out of 4 verifiers = 6 possible combinations, 2.
			// needs 3 out of 5 = 10 possible combinations -> 6*10 = 60 possible
			// combinations of participants
			// so the boundary will be heavily influenced by the combination of verifiers per
			// decision as well as the amount of decisions in the process
			
			// the boundary test will set the max process size which will be taken for
			// further tests

			// generate 10 Processes - annotate them - try performing algorithms with a time
			// limit
			// count amount of timeouts
			// do that while there are not timeouts on all processes for the algorithms

			int minDataObjectsPerDecision = 1;
			int maxDataObjectsPerDecision = 1;
			pathToFolderForModelsForTest1_1 = CommonFunctionality2
					.fileWithDirectoryAssurance(pathToRootFolder, "Test1_1-BoundaryTest1").getAbsolutePath();
			int verifiersPerDecision = 3;
			int privateSphere = 6;
			// the amount of possible combinations of verifiers for the process will be
			// increased by
			// binom(5,3) -> 10 per decision
			// the actor of the brt itself can not be an additional actor to the brt and is therefore excluded
			// e.g. with 0 decisions = 0, 1 decision = 10, 2 decisions = 100, ...
			// since the cost of the models will have to be calculated for all the possible
			// combinations
			// this can be used to estimate the feasibility for other models
			int tasksFactor = 4;
			BatchFileGenerator.performBoundaryTest1_1(1, 0, verifiersPerDecision, privateSphere,
					boundForHeuristicSearch, 6, tasksFactor, 0, 0, percentageOfWritersClasses.get(1),
					percentageOfReadersClasses.get(1), minDataObjectsPerDecision, maxDataObjectsPerDecision,
					defaultSpheres, amountThreads, pathToFolderForModelsForTest1_1);

			System.out.println("BoundartyTest1_1 finished!");
		}

		if (methodsToRun.contains(test1_2ToRun)) {
			// Test 1.2 - Boundary Test 2
			// choose a model from boundary test 1 that had no exceptions for all algorithms
			// create x new models on each iteration till every task of the model has a
			// different participant
			// start with the SphereLowerBound e.g. 2 -> x models where each task has
			// one of the 2 participants connected
			// end point -> x models where each task has a different participant connected

			if (!pathToFolderForModelsForTest1_1.isEmpty()) {

				String pathToFolderForModelsForTest1_2 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToRootFolder, "Test1_2-BoundaryTest2").getAbsolutePath();

				// choose a model
				File directoryOfFiles = new File(pathToFolderForModelsForTest1_1 + File.separatorChar
						+ "BoundaryTest_decision-4" + File.separatorChar + "annotated");
				List<File> listOfFiles = Arrays.asList(directoryOfFiles.listFiles());
				File model = CommonFunctionality2.getRandomItem(listOfFiles);
				int newModelsPerIteration = 10;
				int verifiersPerDecision = 3;
				BatchFileGenerator.performBoundaryTest1_2(model, verifiersPerDecision, newModelsPerIteration,
						boundForHeuristicSearch, amountThreads,
						pathToFolderForModelsForTest1_2);
				System.out.println("BoundartyTest1_2 finished!");
			} else {
				System.out.println(test1_2ToRun + " not performed! Run Test1_1 first!");
			}
		}

		if (methodsToRun.contains(createRandomProcesses)) {

			// generate 3 Classes -> small, medium, large processes (without annotation)
			// put them into a new folder into the root
			String pathToFolderForModelsWithoutAnnotation = CommonFunctionality2
					.fileWithDirectoryAssurance(pathToRootFolder, "ProcessesWithoutAnnotation").getAbsolutePath();

			pathToSmallProcessesFolderWithoutAnnotation = CommonFunctionality2
					.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "SmallProcessesFolder")
					.getAbsolutePath();
			pathToMediumProcessesFolderWithoutAnnotation = CommonFunctionality2
					.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "MediumProcessesFolder")
					.getAbsolutePath();
			pathToLargeProcessesFolderWithoutAnnotation = CommonFunctionality2
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

			randomProcessGeneratorService.shutdownNow();
			System.out.println("All random processes generated!");
		}

		if (methodsToRun.contains(test2ToRun)) {
			// Test 2 - Measure impact of enforceability on privity and vice versa
			// take x random models from small, medium and large processes
			// add dataObjects with private sphere and 1 voter per decision
			// add readers/writers combinations - generate new models (9 new models for //
			// each)
			// increase the amount of verifiers needed for decisions till the private sphere
			// of that process is reached on all xors
			// increase privity requirements to next stricter sphere for all dataObjects

			if (pathToSmallProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToMediumProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToLargeProcessesFolderWithoutAnnotation.isEmpty()) {
				System.out.println(test2ToRun + " not performed! Generate random processes first!");
			} else {
				String pathToFolderForModelsForTest2 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToRootFolder, "Test2-TradeOff").getAbsolutePath();
				int modelsToTakePerDecision = 5;
				pathToSmallProcessesForTest2WithAnnotation = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "SmallProcessesAnnotatedFolder")
						.getAbsolutePath();
				pathToMediumProcessesForTest2WithAnnotation = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "MediumProcessesAnnotatedFolder")
						.getAbsolutePath();
				pathToLargeProcessesForTest2WithAnnotation = CommonFunctionality2
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
						boundForHeuristicSearch,  amountThreads);
				BatchFileGenerator.performTradeOffTest("medium", mediumProcessesWithoutAnnotation,
						pathToMediumProcessesForTest2WithAnnotation, dataObjectBoundsMediumProcesses,
						boundForHeuristicSearch,  amountThreads);
				BatchFileGenerator.performTradeOffTest("large", largeProcessesWithoutAnnotation,
						pathToLargeProcessesForTest2WithAnnotation, dataObjectBoundsLargeProcesses,
						boundForHeuristicSearch,  amountThreads);

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
				String pathToFolderForModelsForDataObjectTest = CommonFunctionality2
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
								boundForHeuristicSearch, amountThreads);
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

				String pathToFolderForModelsForTest4_1 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToRootFolder, "Test4_1-KillerConstraints").getAbsolutePath();
				String pathToFolderForSmallModelsForTest4_1 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "SmallModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(smallProcessesFromTradeOffTest,
						decisionTakerExcludeable, pathToFolderForSmallModelsForTest4_1,
						boundForHeuristicSearch, "small", amountThreads);

				String pathToFolderForMediumModelsForTest4_1 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(mediumProcessesFromTradeOffTest,
						decisionTakerExcludeable, pathToFolderForMediumModelsForTest4_1,
						boundForHeuristicSearch, "medium", amountThreads);

				String pathToFolderForLargeModelsForTest4_1 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(largeProcessesFromTradeOffTest,
						decisionTakerExcludeable, pathToFolderForLargeModelsForTest4_1,
						boundForHeuristicSearch, "large", amountThreads);

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

				String pathToFolderForModelsForTest4_2 = CommonFunctionality2
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

				String pathToFolderForSmallModelsForTest4_2 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "SmallModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(smallProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, probabilityForGatewayToHaveConstraint,
						pathToFolderForSmallModelsForTest4_2, boundForHeuristicSearch, "small", amountThreads);

				String pathToFolderForMediumModelsForTest4_2 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(mediumProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, probabilityForGatewayToHaveConstraint,
						pathToFolderForMediumModelsForTest4_2, boundForHeuristicSearch, "medium", amountThreads);

				String pathToFolderForLargeModelsForTest4_2 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(largeProcessesFromTradeOffTest,
						decisionTakerExcludeable, false, probabilityForGatewayToHaveConstraint,
						pathToFolderForLargeModelsForTest4_2, boundForHeuristicSearch, "large", amountThreads);

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

				String pathToFolderForModelsForTest5 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToRootFolder, "Test5_SearchForBestVerifiers").getAbsolutePath();

				String pathToFolderForSmallModelsForTest5 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "SmallModels").getAbsolutePath();

				String pathToFolderForMediumModelsForTest5 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "MediumModels").getAbsolutePath();

				String pathToFolderForLargeModelsForTest5 = CommonFunctionality2
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest5, "LargeModels").getAbsolutePath();

				BatchFileGenerator.performTestWithSearchForSetOfBestVerifiers(smallProcessesFromTradeOffTest, false,
						pathToFolderForSmallModelsForTest5, boundForHeuristicSearch, "small", amountThreads);

				BatchFileGenerator.performTestWithSearchForSetOfBestVerifiers(mediumProcessesFromTradeOffTest, false,
						pathToFolderForMediumModelsForTest5, boundForHeuristicSearch, "medium", amountThreads);

				BatchFileGenerator.performTestWithSearchForSetOfBestVerifiers(largeProcessesFromTradeOffTest, false,
						pathToFolderForLargeModelsForTest5, boundForHeuristicSearch, "large", amountThreads);

				System.out.println("Test 5 finished!");

			}
		}

		if (methodsToRun.contains(test6ToRun)) {
			// Test 6 - "real world processes"
			// may contain exclude and mandatory participant constraint
			// may contain dynamic writers
			// has wider ranges for variables

			List<Integer> dataObjectBoundsRealWorld = Arrays.asList(1, 6);
			// int dynamicWriterProb = 30;
			int upperBoundParticipants = 8;
			int lowerBoundTasks = 8;
			int upperBoundTasks = 80;
			int upperBoundXorGtws = 10;
			int upperBoundParallelGtws = 6;
			int amountProcesses = 500;
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

			String pathToRealWorldProcesses = CommonFunctionality2
					.fileWithDirectoryAssurance(pathToRootFolder, "Test6-RealWorldProcesses").getAbsolutePath();
			String pathToAnnotatedProcessesFolder = CommonFunctionality2
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
				upperBoundParallelGtws, amountProcesses, randomProcessGeneratorService, false);
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
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathWhereToCreateAnnotatedProcesses, true, true, true,
				upperBoundlocalMinWithBound, writer, service);
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
			BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality2
					.deepCopy(modelInstanceNotToChange);

			int amountTasks = modelInstance.getModelElementsByType(Task.class).size();
			int writersPercentage = CommonFunctionality2.getRandomItem(writersOfProcessInPercent);
			int readersPercentage = CommonFunctionality2.getRandomItem(writersOfProcessInPercent);
			int amountWritersOfProcess = CommonFunctionality2.getAmountFromPercentage(amountTasks, writersPercentage);
			int amountReadersOfProcess = CommonFunctionality2.getAmountFromPercentage(amountTasks, readersPercentage);
			int privateSphere = CommonFunctionality2.getPrivateSphere(modelInstance, false);
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

					// set upperBoundAmountParticipantsToExclude to be max privateSphereSize-1
					if (upperBoundAmountParticipantsToBeMandatory <= 0) {
						upperBoundAmountParticipantsToBeMandatory = ThreadLocalRandom.current()
								.nextInt(lowerBoundAmountParticipantsToBeMandatory, privateSphere);
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
			if(randomAmountXorSplitsWithinBounds > 0) {
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
						nestingDepthFactor);
				processId = pGen.getProcessId();
				Future<File> future = executor.submit(pGen);
				try {
					File f = future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
					System.out.println(f.getName() + " deployed successfully");
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

			System.out.println("randomProcess" + processId + " deployed in " + pathToFiles);
		}

	}

	public static LinkedList<API2> mapFilesToAPI(String pathToFiles, ResultsToCSVWriter writer) {
		// iterate through all files in the directory and run algorithms on annotated
		// models
		LinkedList<API2> apiList = new LinkedList<API2>();
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
				API2 api = new API2(pathToFile, costParameters);
				apiList.add(api);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				writer.addNullValueRowForModel(f.getName(), f.getAbsolutePath(), e.getMessage());
				System.err.println(f.getAbsolutePath() + " could not be mapped to api: " + e.getMessage());
			}

		}

		return apiList;
	}

	public static LinkedList<API2> mapFilesToAPI(LinkedList<File> files, ResultsToCSVWriter writer) {
		// iterate through all files in the directory and run algorithms on annotated
		// models
		LinkedList<API2> apiList = new LinkedList<API2>();

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
				API2 api = new API2(pathToFile, costParameters);
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

	public static synchronized HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> runAlgorithmsAndWriteResultsToCSV(API2 api,
			boolean runExhaustiveSearch, boolean runHeuristicSearch, boolean runHeuristicSearchWithBound,
			int boundForHeuristicSearchWithBound, HashMap<Enums.AlgorithmToPerform, Integer> timeOutOrHeapSpaceExceptionMap, ResultsToCSVWriter writer,
			ExecutorService service) {

		HashMap<Enums.AlgorithmToPerform, Exception> exceptionPerAlgorithm = new HashMap<Enums.AlgorithmToPerform, Exception>();
		HashMap<Enums.AlgorithmToPerform, Integer> cheapestSolutionsMap = new HashMap<Enums.AlgorithmToPerform, Integer>();
		HashMap<Enums.AlgorithmToPerform, Integer> totalAmountSolutionsMap = new HashMap<Enums.AlgorithmToPerform, Integer>();
		HashMap<Enums.AlgorithmToPerform, Double> cheapestSolutionCostMap = new HashMap<Enums.AlgorithmToPerform, Double>();
		HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>> currentSolutionsMap = null;
		HashMap<Enums.AlgorithmToPerform, String> loggingMap = new HashMap<Enums.AlgorithmToPerform, String>();
		boolean exhaustiveRunSuccessfully = false;
		boolean heuristicRunSuccessfully = false;
		boolean heuristicWithBoundRunSuccessfully = false;
		
		boolean heapSpaceError = false;

		// only hold the cost of the cheapest solutions found with the algorithms in
		// memory
		// storing all possible solutions in memory will influence execution time of other algorithms drastically with increasing amount of combinations!!!

		try {
			if (runExhaustiveSearch) {
				api.setAlgorithmToPerform(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
				Future<HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>>> futureExhaustiveSearch = service
						.submit(api);
				Exception exceptionExhaustiveSearch = null;
				try {
					currentSolutionsMap = futureExhaustiveSearch.get(timeOutForApiInMin, TimeUnit.MINUTES);
					LinkedList<PModelWithAdditionalActors> solutionsExhaustive = currentSolutionsMap
							.get(Enums.AlgorithmToPerform.EXHAUSTIVE);
					// get the total amount of solutions
					totalAmountSolutionsMap.putIfAbsent(Enums.AlgorithmToPerform.EXHAUSTIVE,
							solutionsExhaustive.size());
					// get the amount of cheapest solutions
					LinkedList<PModelWithAdditionalActors> cheapestModels = CommonFunctionality2
							.getCheapestPModelsWithAdditionalActors(solutionsExhaustive);
					cheapestSolutionsMap.putIfAbsent(Enums.AlgorithmToPerform.EXHAUSTIVE, cheapestModels.size());
					
					if(!cheapestModels.isEmpty()) {
						// get the cost of the cheapest solutions
						cheapestSolutionCostMap.putIfAbsent(Enums.AlgorithmToPerform.EXHAUSTIVE, cheapestModels.get(0).getSumMeasure());
					}
										
					// get the logging
					String logging = CommonFunctionality2.getLoggingForModelsWithAdditionalActors(Enums.AlgorithmToPerform.EXHAUSTIVE, api.getBusinessRuleTasks(), solutionsExhaustive);
					loggingMap.putIfAbsent(Enums.AlgorithmToPerform.EXHAUSTIVE, logging);
					
					System.out.println("Exhaustive search solutions: " + solutionsExhaustive.size());
					exhaustiveRunSuccessfully = true;
					futureExhaustiveSearch.cancel(true);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					exceptionExhaustiveSearch = (InterruptedException) e;
					e.printStackTrace();
					futureExhaustiveSearch.cancel(true);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					if (e.getCause() instanceof OutOfMemoryError) {
						exceptionExhaustiveSearch = new HeapSpaceException(e.getMessage(), e.getCause());
						timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE,
								timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.EXHAUSTIVE, 0)
										+ 1);
						System.err.println("Exhaustive search ran out of memory");
						heapSpaceError = true;
					} else {
						exceptionExhaustiveSearch = (ExecutionException) e;
						e.printStackTrace();
					}
					futureExhaustiveSearch.cancel(true);

				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					exceptionExhaustiveSearch = (TimeoutException) e;
					timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE,
							timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.EXHAUSTIVE, 0) + 1);
					System.err.println("Timeout for exhaustive search!");
					futureExhaustiveSearch.cancel(true);
				} catch (Error e) {
					System.gc();
					if (e instanceof OutOfMemoryError) {
						exceptionExhaustiveSearch = new HeapSpaceException(e.getMessage(), e.getCause());
						timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE,
								timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.EXHAUSTIVE, 0)
										+ 1);
						System.err.println("Exhaustive search ran out of memory");
						heapSpaceError = true;
					}
					futureExhaustiveSearch.cancel(true);
				}

				finally {
					exceptionPerAlgorithm.putIfAbsent(Enums.AlgorithmToPerform.EXHAUSTIVE, exceptionExhaustiveSearch);
					futureExhaustiveSearch.cancel(true);
					
					// set the current solutions to null to garbage collect
					currentSolutionsMap = null;
				}
			}

			if (runHeuristicSearch) {
				api.setAlgorithmToPerform(Enums.AlgorithmToPerform.HEURISTIC, 0);
				Future<HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>>> futureHeuristicSearch = service
						.submit(api);
				Exception exceptionHeuristicSearch = null;
				try {
					currentSolutionsMap = futureHeuristicSearch.get(timeOutForApiInMin, TimeUnit.MINUTES);
					LinkedList<PModelWithAdditionalActors> solutionsHeuristic = currentSolutionsMap
							.get(Enums.AlgorithmToPerform.HEURISTIC);
					// get the total amount of solutions
					totalAmountSolutionsMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTIC, solutionsHeuristic.size());
					// get the amount of cheapest solutions
					LinkedList<PModelWithAdditionalActors> cheapestModels = CommonFunctionality2
							.getCheapestPModelsWithAdditionalActors(solutionsHeuristic);
					cheapestSolutionsMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTIC, cheapestModels.size());
					
					if(!cheapestModels.isEmpty()) {
					// get the cost of the cheapest solutions
					cheapestSolutionCostMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTIC, cheapestModels.get(0).getSumMeasure());
					}
					
					// get the logging
					String logging = CommonFunctionality2.getLoggingForModelsWithAdditionalActors(Enums.AlgorithmToPerform.HEURISTIC, api.getBusinessRuleTasks(), solutionsHeuristic);
					loggingMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTIC, logging);
					
					System.out.println("Heuristic search solutions: " + solutionsHeuristic.size());
					heuristicRunSuccessfully = true;
					futureHeuristicSearch.cancel(true);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					exceptionHeuristicSearch = (InterruptedException) e;
					e.printStackTrace();
					futureHeuristicSearch.cancel(true);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					if (e.getCause() instanceof OutOfMemoryError) {
						exceptionHeuristicSearch = new HeapSpaceException(e.getMessage(), e.getCause());
						timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTIC,
								timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.HEURISTIC, 0) + 1);
						System.err.println("Heuristic ran out of memory");
						heapSpaceError = true;
					} else {
						exceptionHeuristicSearch = (ExecutionException) e;
						e.printStackTrace();
					}
					futureHeuristicSearch.cancel(true);

				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					exceptionHeuristicSearch = (TimeoutException) e;
					timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTIC,
							timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.HEURISTIC, 0) + 1);
					System.err.println("Timeout for heuristic search!");
					futureHeuristicSearch.cancel(true);
				} catch (Error e) {
					System.gc();
					if (e instanceof OutOfMemoryError) {
						exceptionHeuristicSearch = new HeapSpaceException(e.getMessage(), e.getCause());
						timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTIC,
								timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.HEURISTIC, 0) + 1);
						System.err.println("Heuristic search ran out of memory");
						heapSpaceError = true;
					}
					futureHeuristicSearch.cancel(true);
				}

				finally {
					exceptionPerAlgorithm.putIfAbsent(Enums.AlgorithmToPerform.HEURISTIC, exceptionHeuristicSearch);
					futureHeuristicSearch.cancel(true);

					// set the current solutions to null to garbage collect
					currentSolutionsMap = null;
				}
			}

			if (runHeuristicSearchWithBound) {
				api.setAlgorithmToPerform(Enums.AlgorithmToPerform.HEURISTICWITHBOUND, boundForHeuristicSearchWithBound);
				Future<HashMap<Enums.AlgorithmToPerform, LinkedList<PModelWithAdditionalActors>>> futureHeuristicSearchWithBound = service
						.submit(api);
				Exception exceptionHeuristicSearch = null;
				try {
					currentSolutionsMap = futureHeuristicSearchWithBound.get(timeOutForApiInMin, TimeUnit.MINUTES);
					LinkedList<PModelWithAdditionalActors> solutionsHeuristicWithBound = currentSolutionsMap
							.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND);
					// get the total amount of solutions
					totalAmountSolutionsMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
							solutionsHeuristicWithBound.size());
					// get the amount of cheapest solutions
					LinkedList<PModelWithAdditionalActors> cheapestModels = CommonFunctionality2
							.getCheapestPModelsWithAdditionalActors(solutionsHeuristicWithBound);
					cheapestSolutionsMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
							cheapestModels.size());
					if(!cheapestModels.isEmpty()) {
					// get the cost of the cheapest solutions
					cheapestSolutionCostMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
							cheapestModels.get(0).getSumMeasure());
					}
					// get the logging
					String logging = CommonFunctionality2.getLoggingForModelsWithAdditionalActors(Enums.AlgorithmToPerform.HEURISTICWITHBOUND, api.getBusinessRuleTasks(), solutionsHeuristicWithBound);
					loggingMap.putIfAbsent(Enums.AlgorithmToPerform.HEURISTICWITHBOUND, logging);

					System.out.println("Heuristic search with bound solutions: " + solutionsHeuristicWithBound.size());
					heuristicWithBoundRunSuccessfully = true;
					futureHeuristicSearchWithBound.cancel(true);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					exceptionHeuristicSearch = (InterruptedException) e;
					e.printStackTrace();
					futureHeuristicSearchWithBound.cancel(true);
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					if (e.getCause() instanceof OutOfMemoryError) {
						exceptionHeuristicSearch = new HeapSpaceException(e.getMessage(), e.getCause());
						timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
								timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
										0) + 1);
						System.err.println("Heuristic with bound ran out of memory");
						heapSpaceError = true;
					} else {
						exceptionHeuristicSearch = (ExecutionException) e;
						e.printStackTrace();
					}
					futureHeuristicSearchWithBound.cancel(true);

				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					exceptionHeuristicSearch = (TimeoutException) e;
					timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
							timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.HEURISTICWITHBOUND, 0)
									+ 1);
					System.err.println("Timeout for heuristic search with bound!");
					futureHeuristicSearchWithBound.cancel(true);
				} catch (Error e) {
					System.gc();
					if (e instanceof OutOfMemoryError) {
						exceptionHeuristicSearch = new HeapSpaceException(e.getMessage(), e.getCause());
						timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
								timeOutOrHeapSpaceExceptionMap.getOrDefault(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
										0) + 1);
						System.err.println("Heuristic search with bound ran out of memory");
						heapSpaceError = true;
					}
					futureHeuristicSearchWithBound.cancel(true);
				}

				finally {
					exceptionPerAlgorithm.putIfAbsent(Enums.AlgorithmToPerform.HEURISTICWITHBOUND,
							exceptionHeuristicSearch);
					futureHeuristicSearchWithBound.cancel(true);

					// set the current solutions to null to garbage collect
					currentSolutionsMap = null;
				}
			}

		} catch (Exception e) {
			System.err.println("Some other exception has happened!");
			e.printStackTrace();
		} finally {
			String isCheapestSolutionOfHeuristicInExhaustiveSearch = "null";
			if (exhaustiveRunSuccessfully && heuristicRunSuccessfully) {
				if (cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.HEURISTIC) == cheapestSolutionCostMap
						.get(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
					isCheapestSolutionOfHeuristicInExhaustiveSearch = "true";
				} else {
					isCheapestSolutionOfHeuristicInExhaustiveSearch = "false";
				}

			}

			String isCheapestSolutionOflocalMinWithBoundInExhaustiveSearch = "null";
			if (exhaustiveRunSuccessfully && heuristicWithBoundRunSuccessfully) {
				if (cheapestSolutionCostMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) == cheapestSolutionCostMap
						.get(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
					isCheapestSolutionOflocalMinWithBoundInExhaustiveSearch = "true";
				} else {
					isCheapestSolutionOflocalMinWithBoundInExhaustiveSearch = "false";
				}

			}

			writer.writeResultsOfAlgorithmsToCSVFile(api, cheapestSolutionCostMap, totalAmountSolutionsMap, cheapestSolutionsMap, loggingMap, exceptionPerAlgorithm,
					isCheapestSolutionOfHeuristicInExhaustiveSearch, isCheapestSolutionOflocalMinWithBoundInExhaustiveSearch);

		}

		HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = new HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>>();
		returnMap.putIfAbsent(heapSpaceError, timeOutOrHeapSpaceExceptionMap);
		return returnMap;

	}

	public static void runAlgorithmsAndWriteResultsToCSV(LinkedList<File> files, boolean runExhaustiveSearch,
			boolean runHeuristicSearch, boolean runHeuristicSearchWithBound, int boundForHeuristicSearchWithBound,
			ResultsToCSVWriter writer, ExecutorService service) {
		LinkedList<API2> apiList = BatchFileGenerator.mapFilesToAPI(files, writer);
		if (!apiList.isEmpty()) {
			for (API2 api : apiList) {
				BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api, runExhaustiveSearch, runHeuristicSearch,
						runHeuristicSearchWithBound, boundForHeuristicSearchWithBound, 
						new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels, boolean runExhaustiveSearch,
			boolean runHeuristicSearch, boolean runHeuristicSearchWithBound, int boundForHeuristicSearchWithBound,
			ResultsToCSVWriter writer, ExecutorService service) {
		LinkedList<API2> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithAnnotatedModels, writer);
		if (!apiList.isEmpty()) {
			for (API2 api : apiList) {
				BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api, runExhaustiveSearch, runHeuristicSearch,
						runHeuristicSearchWithBound, boundForHeuristicSearchWithBound, 
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

		// amount verifiers per gtw will be between 2 and privateSphere

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
					int amountDecisions = CommonFunctionality2.getAmountExclusiveGtwSplits(processModel);
					int privateSphere = CommonFunctionality2.getPrivateSphere(processModel, false);
					int amountVerifiersUpperBound = ThreadLocalRandom.current().nextInt(2, privateSphere + 1);

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
							amountTasks = CommonFunctionality2.getAmountTasks(pModel.getModelInstance());

							if (amountTasks > maxAmountUniqueDataObjects) {
								int minDataObjectsPerDecision = maxAmountUniqueDataObjects / amountDecisions;
								pModel.generateDataObjects(maxAmountUniqueDataObjects, sphere);
								pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(minDataObjectsPerDecision,
										minDataObjectsPerDecision, 2, amountVerifiersUpperBound, 0, true);
								int amountWritersNeededForDataObjects = maxAmountUniqueDataObjects;
								int amountReadersNeededForDataObjects = maxAmountUniqueDataObjects;

								// additional readers and writers to be inserted
								int amountAdditionalWriters = CommonFunctionality2.getAmountFromPercentage(amountTasks,
										percentageWriters);
								int amountAdditionalReaders = CommonFunctionality2.getAmountFromPercentage(amountTasks,
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
				BpmnModelInstance modelInstance = (BpmnModelInstance) CommonFunctionality2
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
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true,
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
			File randomProcessModelFile = CommonFunctionality2.getRandomItem(listOfFiles);
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
				int modelInstDecisions = CommonFunctionality2.getAmountExclusiveGtwSplits(modelInst);
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
				int modelInstDecisions = CommonFunctionality2.getAmountExclusiveGtwSplits(modelInst);
				int amountTasks = CommonFunctionality2.getAmountTasks(modelInst);
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

					// create model with amountRandomCountDataObjects dataObjects, 0 participant
					// needed for voting and empty string as starting sphere
					pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, emptySphere);
					// connect between 1 and amountRandomCountDataObjectsToCreate per decision
					pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(1, amountRandomCountDataObjectsToCreate, 0, 0,
							0, false);

					CommonFunctionality2
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

					int amountWriterTasksToBeInserted = CommonFunctionality2.getAmountFromPercentageWithMinimum(
							amountTasks, percentageOfWritersClasses.get(writerClass), minAmountWriters);

					for (int readerClass = 0; readerClass < percentageOfReadersClasses.size(); readerClass++) {
						// for each model -> annotate it with small, medium, large amount of readers
						int triesForInsertingReadersAndWriters = 50;
						boolean modelAnnotatedCorrectly = false;
						Exception ex = null;
						int amountReaderTasksToBeInserted = CommonFunctionality2.getAmountFromPercentage(amountTasks,
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

						int amountWriterTasksForPercentage = CommonFunctionality2.getAmountFromPercentage(amountTasks,
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
		// for each model that has been generated with 0 participants per decision and
		// empty sphere annotated for each dataObject
		// -> generate new ones where verifiers is increased by 1 on each model till
		// privateSphereSize is reached
		// -> generate new ones where privity requirement is increased to next stricter
		// sphere till Strong-Dynamic is reached on all dataObjects

		String pathToFolderWithModelsForEvaluation = CommonFunctionality2
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

					int privateSphereSize = CommonFunctionality2.getPrivateSphere(currModel, false);

					if (annotatedModel.getName().contains("sWsR") || annotatedModel.getName().contains("sWmR")
							|| annotatedModel.getName().contains("sWlR") || annotatedModel.getName().contains("mWsR")
							|| annotatedModel.getName().contains("mWmR") || annotatedModel.getName().contains("mWlR")
							|| annotatedModel.getName().contains("lWsR") || annotatedModel.getName().contains("lWmR")
							|| annotatedModel.getName().contains("lWlR")) {

						for (int spheresIndex = 0; spheresIndex < defaultSpheres.size(); spheresIndex++) {
							String currentSphere = defaultSpheres.get(spheresIndex);
							CommonFunctionality2.increaseSpherePerDataObject(currModel, currentSphere);

							for (int verifiersAmountIndex = 1; verifiersAmountIndex <= privateSphereSize; verifiersAmountIndex++) {
								String suffix = "_" + currentSphere + "_verifiers" + verifiersAmountIndex;
								CommonFunctionality2.increaseVerifiersPerDataObject(currModel, verifiersAmountIndex);
								String modelName = annotatedModel.getName().substring(0,
										annotatedModel.getName().indexOf(".bpmn"));
								try {

									File file = CommonFunctionality2.writeChangesToFile(currModel, modelName,
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
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true,
				upperBoundSolutionsForLocalMinWithBound, writer, service);
		service.shutdownNow();
		writer.writeRowsToCSVAndcloseWriter();

	}

	public static void performBoundaryTest1_1(int amountProcessesPerIteration, int amountDecisionsToStart,
			int verifiersPerDecision, int privateSphere, int upperBoundSolutionsForLocalMinWithBound,
			int amountTasksToStartWith, int tasksFactor, int lowerBoundAmountParallelGtws,
			int upperBoundAmountParallelGtws, int amountWritersInPercent, int amountReadersInPercent,
			int minDataObjectsPerDecision, int maxDataObjectsPerDecision, LinkedList<String> sphereList,
			int amountThreads, String pathToFolderForModelsForBoundaryTest) {

		int amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
				upperBoundAmountParallelGtws + 1);

		File csvFile = BatchFileGenerator.createNewCSVFile(pathToFolderForModelsForBoundaryTest, "test1_1-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		HashMap<Enums.AlgorithmToPerform, Integer> timeOutOrHeapSpaceExceptionMap = new HashMap<Enums.AlgorithmToPerform, Integer>();

		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		boolean outOfMemoryError = false;
		boolean runExhaustiveSearch = true;
		boolean runHeuristicSearch = true;
		boolean runHeuristicSearchWithBound = true;
		boolean finishTest = false;

		do {
			timeOutOrHeapSpaceExceptionMap.clear();
			timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
			timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTIC, 0);
			timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.HEURISTICWITHBOUND, 0);

			int amountDataObjectsToCreate = amountDecisionsToStart;
			System.out.println("Generate models with " + amountDecisionsToStart + " decisions!");

			String folderName = "BoundaryTest_decision-" + amountDecisionsToStart;
			String pathToFolderForModelsWithDecisions = CommonFunctionality2
					.fileWithDirectoryAssurance(pathToFolderForModelsForBoundaryTest, folderName).getAbsolutePath();

			int amountTasksWithFactor = amountTasksToStartWith + (amountDecisionsToStart * tasksFactor);
			amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
					upperBoundAmountParallelGtws + 1);
			int amountWriters = CommonFunctionality2.getAmountFromPercentageWithMinimum(amountTasksWithFactor,
					amountWritersInPercent, amountDataObjectsToCreate);
			int amountReaders = CommonFunctionality2.getAmountFromPercentageWithMinimum(amountTasksWithFactor,
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

			String pathToFolderForModelsWithDecisionsAnnotated = CommonFunctionality2
					.fileWithDirectoryAssurance(pathToFolderForModelsWithDecisions, "annotated").getAbsolutePath();
			Iterator<File> modelIter = listOfFiles.iterator();

			while (modelIter.hasNext()) { // annotate the model
				File model = modelIter.next();
				boolean correctModel = false;
				int tries = 0;
				while (correctModel == false && tries < triesForModelAnnotater) {
					File modelCopy = (File) CommonFunctionality2.deepCopy(model);
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
						argumentsForThirdMethod[2] = verifiersPerDecision;
						argumentsForThirdMethod[3] = verifiersPerDecision;
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
			LinkedList<API2> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderForModelsWithDecisionsAnnotated,
					writer);

			// perform all algorithms and count the timeouts
			for (API2 api : apiList) {
				HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = BatchFileGenerator
						.runAlgorithmsAndWriteResultsToCSV(api, runExhaustiveSearch, runHeuristicSearch, runHeuristicSearchWithBound,
								upperBoundSolutionsForLocalMinWithBound, timeOutOrHeapSpaceExceptionMap, writer, executor);
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
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) == apiList.size()) {
					runHeuristicSearchWithBound = false;
				}
			}

			System.out.println("Iteration" + amountDecisionsToStart + " end - timeOutsExhaustiveSearch: "
					+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + ", timeOutsHeuristicSearch: "
					+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.HEURISTIC) + ", timeOutsHeuristicSearchWithBound"
					+ upperBoundSolutionsForLocalMinWithBound + ": "
					+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND));

			amountDecisionsToStart++;

			if (runExhaustiveSearch == false && runHeuristicSearch == false && runHeuristicSearchWithBound == false) {
				finishTest = true;
			}

		} while ((!finishTest) && (!outOfMemoryError));

		executor.shutdownNow();

		writer.writeRowsToCSVAndcloseWriter();

	}

	public static void performBoundaryTest1_2(File file, int privateSphereLowerBound,
			int amountNewProcessesToCreatePerIteration, int upperBoundSolutionsForHeuristicSearch, 
			int amountThreadPools, String directoryToStore) {

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStore, "test1_2-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);

		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		CommonFunctionality2.generateNewModelsUntilPrivateSphereReached(file, privateSphereLowerBound,
				amountNewProcessesToCreatePerIteration, directoryToStore);

		// map annotated models
		LinkedList<API2> apiList = BatchFileGenerator.mapFilesToAPI(directoryToStore, writer);
		boolean outOfMemoryException = false;
		// perform all algorithms and count the timeouts
		for (API2 api : apiList) {
			HashMap<Enums.AlgorithmToPerform, Integer> timeOutMap = new HashMap<Enums.AlgorithmToPerform, Integer>();

			timeOutMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
			timeOutMap.put(Enums.AlgorithmToPerform.HEURISTIC, 0);
			timeOutMap.put(Enums.AlgorithmToPerform.HEURISTICWITHBOUND, 0);

			HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(
					api, true, true, true, upperBoundSolutionsForHeuristicSearch, timeOutMap, writer,
					executor);
			if (returnMap.get(true) != null) {
				outOfMemoryException = true;
				timeOutMap = returnMap.get(true);
			} else {
				timeOutMap = returnMap.get(false);
			}

			System.out.println("timeOutsExhaustiveSearch: " + timeOutMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) + ", timeOutsHeuristicSearch: "
					+ timeOutMap.get(Enums.AlgorithmToPerform.HEURISTIC) + ", timeOutsHeuristicSearchWithBound: " + timeOutMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND));

			if ((timeOutMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) == 1 && timeOutMap.get(Enums.AlgorithmToPerform.HEURISTIC) == 1
					&& timeOutMap.get(Enums.AlgorithmToPerform.HEURISTICWITHBOUND) == 1) || outOfMemoryException == true) {
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
		DataObjectReference substitute = CommonFunctionality2.getRandomItem(daoList);

		CommonFunctionality2.substituteOneDataObjectPerIterationAndWriteNewModels(modelInstance, substitute, modelName,
				directoryToStore);

	}

	public static void performTestWithSearchForSetOfBestVerifiers(LinkedList<File> models, boolean modelWithLanes,
			String directoryToStoreNewModels, int upperBoundHeuristicSearchWithBound, String suffixCSV, int amountThreads) {
		// the decision taker must always be part of the verifiers
		int probabilityForGatwayToHaveMandPartConst = 100;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality2.deepCopy(currModel);

			try {
				CommonFunctionality2.addMandatoryParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatwayToHaveMandPartConst, 1, 1, false, modelWithLanes,
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
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true,
				upperBoundHeuristicSearchWithBound, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();

	}

	public static void performTestWithMaxPossibleExcludeConstraints(LinkedList<File> models,
			boolean modelWithLanes, String directoryToStoreNewModels,
			int upperBoundHeuristicSearchWithBound, String suffixCSV, int amountThreads) {
		int probabilityForGatewayToHaveConstraint = 100;
		int lowerBoundAmountParticipantsToExcludePerGtw = 1;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality2.deepCopy(currModel);

			try {
				// will always be max. constrained
				CommonFunctionality2.addExcludeParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatewayToHaveConstraint, lowerBoundAmountParticipantsToExcludePerGtw,
						true, modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels, "test4_1-results_" + suffixCSV);

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true,
				upperBoundHeuristicSearchWithBound, writer, executor);
		writer.writeRowsToCSVAndcloseWriter();
		executor.shutdownNow();

	}

	public static void performTestWithExcludeConstraintsProbability(LinkedList<File> models,
			boolean decisionTakerExcludeable, boolean modelWithLanes, int probabilityForGatewayToHaveConstraint,
			String directoryToStoreNewModels, int upperBoundHeuristicSearchWithBound, String suffixCSV, int amountThreads) {
		int lowerBoundAmountParticipantsToExcludePerGtw = 1;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality2.deepCopy(currModel);

			try {
				CommonFunctionality2.addExcludeParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatewayToHaveConstraint, lowerBoundAmountParticipantsToExcludePerGtw,
						decisionTakerExcludeable, modelWithLanes, directoryToStoreNewModels);
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
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(modelsToEvaluate, true, true, true,
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
