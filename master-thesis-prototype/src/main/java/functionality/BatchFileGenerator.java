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
import java.util.Map.Entry;
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
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;

import functionality.Enums.AlgorithmToPerform;
import processModelGeneratorAndAnnotater.ProcessGenerator;
import processModelGeneratorAndAnnotater.ProcessModelAnnotater;

public class BatchFileGenerator {

	static int idCSVFile = 1;
	// static String root = System.getProperty("user.home") + "/Desktop";
	static String root = System.getProperty("user.home") + "/Onedrive/Desktop";

	static int timeOutForProcessGeneratorInMin = 5;
	static int timeOutForProcessModelAnnotaterInMin = 5;
	// API is the class where the computations will be done
	static int timeOutForApiInMin = 3;

	// for very large processes it is a good choice to set those parameters to false
	// in order to avoid very long execution time until the actual algorithms start
	static boolean testIfModelValid = true;
	static boolean calculateAmountOfPaths = true;

	// how many percent of the processes will have all xors as sequences instead of
	// nested
	static int percentageOfXorsAsSeq = 0;

	// amount of solutions to be generated with naive approaches
	static int amountSolutionsToBeGenerated = 1;

	static int amountThreads = 1;

	// bounds for ProcessModelGenerator
	static boolean firstElementAfterStartIsTask = true;
	static int probTask = 34;
	static int probXorGtw = 33;
	static int probParallelGtw = 33;

	// bounds for the data objects to be inserted
	static LinkedList<Integer> dataObjectBoundsSmallProcesses = new LinkedList<Integer>(Arrays.asList(1, 2));
	static LinkedList<Integer> dataObjectBoundsMediumProcesses = new LinkedList<Integer>(Arrays.asList(3, 4));
	static LinkedList<Integer> dataObjectBoundsLargeProcesses = new LinkedList<Integer>(Arrays.asList(5, 6));
	static LinkedList<Integer> dataObjectBoundsExtraLargeProcesses = new LinkedList<Integer>(Arrays.asList(10, 11));

	static LinkedList<String> defaultSpheres = new LinkedList<String>(
			Arrays.asList("Private", "Static", "Weak-Dynamic", "Strong-Dynamic"));
	static LinkedList<String> defaultNamesSeqFlowsXorSplits = new LinkedList<String>(Arrays.asList("true", "false"));
	static int dynamicWriterProb = 0;
	static int probPublicSphere = 0;

	// bounds for "small", "medium", "large" amountOfWriters classes in percentage
	// e.g. 10 means, there will be 10% writers of the tasks in the process
	static LinkedList<Double> percentageOfWritersClasses = new LinkedList<Double>(Arrays.asList(10.0, 20.0, 30.0));

	// bounds for "small", "medium", "large" amountOfReaders
	static LinkedList<Double> percentageOfReadersClasses = new LinkedList<Double>(Arrays.asList(10.0, 20.0, 30.0));

	// alpha, beta and gamma cost parameters
	static LinkedList<Double> costParameters = new LinkedList<>(Arrays.asList(1.0, 1.0, 1.0));

	// amount of xor-splits per class
	// lower bound, upper bound
	static ArrayList<Integer> amountXorsSmallProcessesBounds = new ArrayList<>(Arrays.asList(1, 2));
	static ArrayList<Integer> amountXorsMediumProcessesBounds = new ArrayList<>(Arrays.asList(3, 4));
	static ArrayList<Integer> amountXorsLargeProcessesBounds = new ArrayList<>(Arrays.asList(5, 6));
	static ArrayList<Integer> amountXorsExtraLargeProcessesBounds = new ArrayList<>(Arrays.asList(10, 11));

	// bounds for nestingDepthFactor and probJoinGtw
	static ArrayList<Integer> nestingDepthFactorBounds = new ArrayList<>(Arrays.asList(0, 50));
	static ArrayList<Integer> probJoinGtwBounds = new ArrayList<>(Arrays.asList(0, 50));

	// boundary tests shared bounds
	static int boundaryTest1_1_privateSphere = 6;
	static int boundaryTest1_1_addActorsPerDecision = 3;

	// create random processes
	// amountProcessesToCreatePerDecision should be a lot higher than
	// amountProcesesToTakePerDecision, since not every model will be valid
	static int amountProcessesToCreatePerDecision = 150;
	static int amountProcessesToTakePerDecision = 15;

	// to avoid heap space exceptions in further tests
	public static int maxXorsToRunExhaustiveOn = 5;

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

		// methodsToRun.add(test1_1ToRun);
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
		String pathToExtraLargeProcessesFolderWithoutAnnotation = "";

		String pathToSmallProcessesForTest2WithAnnotation = "";
		String pathToMediumProcessesForTest2WithAnnotation = "";
		String pathToLargeProcessesForTest2WithAnnotation = "";
		String pathToExtraLargeProcessesForTest2WithAnnotation = "";

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
					} else if (processesWithoutAnnotationFolderName.contentEquals("ExtraLargeProcessesFolder")) {
						pathToExtraLargeProcessesFolderWithoutAnnotation = processesWithoutAnnotation.getAbsolutePath();
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
					} else if (processesWithAnnotationFolderName.contentEquals("LargeProcessesAnnotatedFolder")) {
						pathToExtraLargeProcessesForTest2WithAnnotation = processesWithAnnotation.getAbsolutePath();
					}
				}
			}
		}

		if (methodsToRun.contains(test1_1ToRun)) {
			// Test 1 - Boundary Test 1
			// Test with 1.1 - 1 unique dataObject per decision
			// The amount solutions generated will be the binomial coefficient for each
			// decision multiplied
			// 3 out of 5 = 10 possible combinations for a gtw
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

			// set the percentage of processes that will have the xors always in sequence
			int percentageProcessesWithXorsAlwaysInSeq = 100;

			pathToFolderForModelsForTest1_1 = CommonFunctionality
					.fileWithDirectoryAssurance(pathToRootFolder, "Test1_1-BoundaryTest1").getAbsolutePath();

			LinkedList<String> sphere = new LinkedList<String>();
			sphere.add("Strong-Dynamic");

			int stopAfterDecisions = 4;

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
			int tasksToStartWith = 6;

			// we take the max amount of writers and readers
			double percentageOfWriters = percentageOfWritersClasses.get(2);
			double percentageOfReaders = percentageOfReadersClasses.get(2);

			BatchFileGenerator.performBoundaryTest1_1(amountProcessesPerIteration, 1,
					boundaryTest1_1_addActorsPerDecision, boundaryTest1_1_privateSphere, amountSolutionsToBeGenerated,
					tasksToStartWith, tasksFactor, 0, 0, percentageOfWriters, percentageOfReaders,
					minDataObjectsPerDecision, maxDataObjectsPerDecision, sphere, amountThreads, stopAfterDecisions,
					pathToFolderForModelsForTest1_1, firstElementAfterStartIsTask, timeOutForProcessGeneratorInMin,
					testIfModelValid, calculateAmountOfPaths, percentageProcessesWithXorsAlwaysInSeq);

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

				boolean testIfModelValid = false;
				boolean calculateAmountPaths = false;

				// choose a model
				File directoryOfFiles = new File(pathToFolderForModelsForTest1_1 + File.separatorChar
						+ "BoundaryTest_decision-12" + File.separatorChar + "annotated");
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
						amountSolutionsToBeGenerated, amountThreads, pathToFolderForModelsForTest1_2, testIfModelValid,
						calculateAmountPaths);
				System.out.println("BoundartyTest1_2 finished!");
			} else {
				System.out.println(test1_2ToRun + " not performed! Run Test1_1 first!");
			}
		}

		if (methodsToRun.contains(createRandomProcesses)) {
			// generate 4 classes of processes -> small, medium, large, extra large
			// processes (without
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
			pathToExtraLargeProcessesFolderWithoutAnnotation = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "ExtraLargeProcessesFolder")
					.getAbsolutePath();

			int tasksToAddToMinAmount = 10;
			int privateSphere = 6;

			int maxAmountDataObjectsSmallProcesses = dataObjectBoundsSmallProcesses.get(1);
			int minAmountTasksSmallProcesses = getMinAmountTasksForMaxAmountDataObjects(percentageOfReadersClasses,
					percentageOfWritersClasses, maxAmountDataObjectsSmallProcesses);
			int maxAmountTasksSmallProcesses = minAmountTasksSmallProcesses + tasksToAddToMinAmount;

			// small processes: 6 participants, 1-2 xors, 0-2 parallels
			for (int i = amountXorsSmallProcessesBounds.get(0); i <= amountXorsSmallProcessesBounds.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToSmallProcessesFolderWithoutAnnotation,
						privateSphere, privateSphere, minAmountTasksSmallProcesses, maxAmountTasksSmallProcesses, i, i,
						0, 2, amountProcessesToCreatePerDecision, firstElementAfterStartIsTask,
						timeOutForProcessGeneratorInMin, percentageOfXorsAsSeq);
			}

			// medium processes: 6 participants, 3-4 xors, 0-2 parallels
			int maxAmountDataObjectsMediumProcesses = dataObjectBoundsMediumProcesses.get(1);
			int minAmountTasksMediumProcesses = getMinAmountTasksForMaxAmountDataObjects(percentageOfReadersClasses,
					percentageOfWritersClasses, maxAmountDataObjectsMediumProcesses);
			int maxAmountTasksMediumProcesses = minAmountTasksMediumProcesses + tasksToAddToMinAmount;

			for (int i = amountXorsMediumProcessesBounds.get(0); i <= amountXorsMediumProcessesBounds.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(
						pathToMediumProcessesFolderWithoutAnnotation, privateSphere, privateSphere,
						minAmountTasksMediumProcesses, maxAmountTasksMediumProcesses, i, i, 0, 2,
						amountProcessesToCreatePerDecision, firstElementAfterStartIsTask,
						timeOutForProcessGeneratorInMin, percentageOfXorsAsSeq);
			}

			// large processes: 6 participants, 5-6 xors, 0-2, parallels
			int maxAmountDataObjectsLargeProcesses = dataObjectBoundsLargeProcesses.get(1);
			int minAmountTasksLargeProcesses = getMinAmountTasksForMaxAmountDataObjects(percentageOfReadersClasses,
					percentageOfWritersClasses, maxAmountDataObjectsLargeProcesses);
			int maxAmountTasksLargeProcesses = minAmountTasksLargeProcesses + tasksToAddToMinAmount;

			for (int i = amountXorsLargeProcessesBounds.get(0); i <= amountXorsLargeProcessesBounds.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolderWithoutAnnotation,
						privateSphere, privateSphere, minAmountTasksLargeProcesses, maxAmountTasksLargeProcesses, i, i,
						0, 2, amountProcessesToCreatePerDecision, firstElementAfterStartIsTask,
						timeOutForProcessGeneratorInMin, percentageOfXorsAsSeq);
			}

			// extra-large processes: 6 participants, 10-11 xors, 0-2 parallels
			int maxAmountDataObjectsExtraLargeProcesses = dataObjectBoundsExtraLargeProcesses.get(1);
			int minAmountTasksExtraLargeProcesses = getMinAmountTasksForMaxAmountDataObjects(percentageOfReadersClasses,
					percentageOfWritersClasses, maxAmountDataObjectsExtraLargeProcesses);
			int maxAmountTasksExtraLargeProcesses = minAmountTasksExtraLargeProcesses + tasksToAddToMinAmount;

			for (int i = amountXorsExtraLargeProcessesBounds.get(0); i <= amountXorsExtraLargeProcessesBounds
					.get(1); i++) {
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(
						pathToExtraLargeProcessesFolderWithoutAnnotation, privateSphere, privateSphere,
						minAmountTasksExtraLargeProcesses, maxAmountTasksExtraLargeProcesses, i, i, 0, 2,
						amountProcessesToCreatePerDecision, firstElementAfterStartIsTask,
						timeOutForProcessGeneratorInMin, percentageOfXorsAsSeq);
			}

			System.out.println("All random processes generated!");
		}

		if (methodsToRun.contains(test2ToRun)) {
			// Test 2 - Measure impact of additional actors on privity
			// take x random models from small, medium and large processes
			// add dataObjects with private sphere and 1 additional actor per decision
			// add readers/writers combinations - generate new models (9 new models for
			// each)
			// increase the amount of additional actors needed for decisions till the
			// private sphere-1 (because the actor of the brt itself is excluded)
			// of that process is reached on all xors
			// increase privity requirements to next stricter sphere for all dataObjects

			if (pathToSmallProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToMediumProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToLargeProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToExtraLargeProcessesFolderWithoutAnnotation.isEmpty()) {
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
				pathToExtraLargeProcessesForTest2WithAnnotation = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "ExtraLargeProcessesAnnotatedFolder")
						.getAbsolutePath();

				// extra large processes will run into timeout for exhaustive search
				// in most cases, therefore only run exhaustive search in some instances
				int percentageOfExtraLargeProcessesToRunExhaustiveOn = 0;

				BatchFileGenerator.performTradeOffTest("small", pathToSmallProcessesForTest2WithAnnotation,
						dataObjectBoundsSmallProcesses, amountSolutionsToBeGenerated,
						amountXorsSmallProcessesBounds.get(0), amountXorsSmallProcessesBounds.get(1),
						amountProcessesToTakePerDecision, pathToSmallProcessesFolderWithoutAnnotation, 100,
						maxXorsToRunExhaustiveOn, amountThreads, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.performTradeOffTest("medium", pathToMediumProcessesForTest2WithAnnotation,
						dataObjectBoundsMediumProcesses, amountSolutionsToBeGenerated,
						amountXorsMediumProcessesBounds.get(0), amountXorsMediumProcessesBounds.get(1),
						amountProcessesToTakePerDecision, pathToMediumProcessesFolderWithoutAnnotation, 100,
						maxXorsToRunExhaustiveOn, amountThreads, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.performTradeOffTest("large", pathToLargeProcessesForTest2WithAnnotation,
						dataObjectBoundsLargeProcesses, amountSolutionsToBeGenerated,
						amountXorsLargeProcessesBounds.get(0), amountXorsLargeProcessesBounds.get(1),
						amountProcessesToTakePerDecision, pathToLargeProcessesFolderWithoutAnnotation, 100,
						maxXorsToRunExhaustiveOn, amountThreads, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.performTradeOffTest("extraLarge", pathToExtraLargeProcessesForTest2WithAnnotation,
						dataObjectBoundsExtraLargeProcesses, amountSolutionsToBeGenerated,
						amountXorsExtraLargeProcessesBounds.get(0), amountXorsExtraLargeProcessesBounds.get(1),
						amountProcessesToTakePerDecision, pathToExtraLargeProcessesFolderWithoutAnnotation,
						percentageOfExtraLargeProcessesToRunExhaustiveOn, maxXorsToRunExhaustiveOn, amountThreads,
						false, false);

				System.out.println("Test 2 finished!");
			}
		}

		if (methodsToRun.contains(test3ToRun)) {
			// Test 3 - Measure impact of dataObjects
			// annotate unique data objects for each decision
			// take one dataObject, loop through the others and replace one object with that
			// dataObject in each iteration
			// on last step each decision will only have that dataObject connected
			// performed on small and medium processes with static and strong-dynamic
			// spheres
			// choose models without parallel branches

			if (pathToSmallProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToMediumProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToLargeProcessesFolderWithoutAnnotation.isEmpty()
					&& pathToExtraLargeProcessesForTest2WithAnnotation.isEmpty()) {
				System.out.println(test3ToRun + " not performed! Generate random processes first!");
			} else {
				String pathToFolderForModelsForDataObjectTestStatic = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test3-ImpactOfDataObjects_Static")
						.getAbsolutePath();

				String pathToFolderForModelsForDataObjectTestSD = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test3-ImpactOfDataObjects_SD").getAbsolutePath();

				int modelsToTakePerDecision = 10;

				// In this case test the models with a fixed amount of readers/writers instead
				// of percentage based on total tasks
				// will be added on top of the readers/writers needed for unique data objects
				LinkedList<Integer> amountReaders = new LinkedList<Integer>(Arrays.asList(5, 8, 10));
				LinkedList<Integer> amountWriters = new LinkedList<Integer>(Arrays.asList(5, 8, 10));

				boolean enoughModelsFoundOnEachIter = true;
				LinkedList<Integer> allDecisions = new LinkedList<Integer>();
				// annotate models with max amount of unique dataObjects
				int amountUniqueDataObjectsPerDecision = 3;
				int maxAmountUniqueDataObjects = amountXorsMediumProcessesBounds.get(1)
						* amountUniqueDataObjectsPerDecision;

				LinkedList<File> smallProcessesWithoutAnnotationTest3 = new LinkedList<File>();
				for (int i = amountXorsSmallProcessesBounds.get(0); i <= amountXorsSmallProcessesBounds.get(1); i++) {
					if (enoughModelsFoundOnEachIter) {
						LinkedList<File> smallProcessesFound = BatchFileGenerator
								.getModelsInOrderFromSourceFolderWithExactAmountDecisionsAndMinTasks(
										modelsToTakePerDecision, i, maxAmountUniqueDataObjects, false,
										pathToSmallProcessesFolderWithoutAnnotation);
						if (smallProcessesFound.size() < modelsToTakePerDecision) {
							System.err.println("Only " + smallProcessesFound.size() + " smallProcesses with " + i
									+ " decisions found instead of " + modelsToTakePerDecision);
							enoughModelsFoundOnEachIter = false;
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
					if (enoughModelsFoundOnEachIter) {
						LinkedList<File> mediumProcessesFound = BatchFileGenerator
								.getModelsInOrderFromSourceFolderWithExactAmountDecisionsAndMinTasks(
										modelsToTakePerDecision, i, maxAmountUniqueDataObjects, false,
										pathToMediumProcessesFolderWithoutAnnotation);
						if (mediumProcessesFound.size() < modelsToTakePerDecision) {
							System.err.println("Only " + mediumProcessesFound.size() + " mediumProcesses with " + i
									+ " decisions found instead of " + modelsToTakePerDecision);
							enoughModelsFoundOnEachIter = false;
						} else {
							mediumProcessesWithoutAnnotationTest3.addAll(mediumProcessesFound);
							if (!allDecisions.contains(i)) {
								allDecisions.add(i);
							}
						}
					}
				}

				if (enoughModelsFoundOnEachIter) {
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
						// Start test with static sphere
						LinkedList<String> sphereList = new LinkedList<String>();
						sphereList.add("Static");
						BatchFileGenerator.performDataObjectTest(allProcessesWithoutAnnotationTest3, amountReaders,
								amountWriters, pathToFolderForModelsForDataObjectTestStatic, maxAmountUniqueDataObjects,
								amountSolutionsToBeGenerated, amountThreads, testIfModelValid, sphereList);
						sphereList = new LinkedList<String>();
						sphereList.add("Strong-Dynamic");
						BatchFileGenerator.performDataObjectTest(allProcessesWithoutAnnotationTest3, amountReaders,
								amountWriters, pathToFolderForModelsForDataObjectTestSD, maxAmountUniqueDataObjects,
								amountSolutionsToBeGenerated, amountThreads, testIfModelValid, sphereList);
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
					&& pathToLargeProcessesForTest2WithAnnotation.isEmpty()
					&& pathToExtraLargeProcessesForTest2WithAnnotation.isEmpty()) {
				System.out.println(test4_1ToRun + " not performed! Run test 2 first!");
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

				String pathToFolderForModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToRootFolder, "Test4_1-KillerConstraints").getAbsolutePath();
				String pathToFolderForSmallModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "SmallModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(smallProcessesFromTradeOffTest, false,
						pathToFolderForSmallModelsForTest4_1, amountSolutionsToBeGenerated, "small", amountThreads,
						testIfModelValid, calculateAmountOfPaths);

				String pathToFolderForMediumModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(mediumProcessesFromTradeOffTest, false,
						pathToFolderForMediumModelsForTest4_1, amountSolutionsToBeGenerated, "medium", amountThreads,
						testIfModelValid, calculateAmountOfPaths);

				String pathToFolderForLargeModelsForTest4_1 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_1, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithMaxPossibleExcludeConstraints(largeProcessesFromTradeOffTest, false,
						pathToFolderForLargeModelsForTest4_1, amountSolutionsToBeGenerated, "large", amountThreads,
						testIfModelValid, calculateAmountOfPaths);

				System.out.println("Test 4_1 finished!");
			}
		}

		if (methodsToRun.contains(test4_2ToRun)) {
			// Test 4_2 -> Processes with probability to have exclude constraints
			if (pathToSmallProcessesForTest2WithAnnotation.isEmpty()
					&& pathToMediumProcessesForTest2WithAnnotation.isEmpty()
					&& pathToLargeProcessesForTest2WithAnnotation.isEmpty()
					&& pathToExtraLargeProcessesForTest2WithAnnotation.isEmpty()) {
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
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(smallProcessesFromTradeOffTest, false,
						probabilityForGatewayToHaveConstraint, pathToFolderForSmallModelsForTest4_2,
						amountSolutionsToBeGenerated, "small", amountThreads, testIfModelValid, calculateAmountOfPaths);

				String pathToFolderForMediumModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "MediumModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(mediumProcessesFromTradeOffTest, false,
						probabilityForGatewayToHaveConstraint, pathToFolderForMediumModelsForTest4_2,
						amountSolutionsToBeGenerated, "medium", amountThreads, testIfModelValid,
						calculateAmountOfPaths);

				String pathToFolderForLargeModelsForTest4_2 = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForTest4_2, "LargeModels").getAbsolutePath();
				BatchFileGenerator.performTestWithExcludeConstraintsProbability(largeProcessesFromTradeOffTest, false,
						probabilityForGatewayToHaveConstraint, pathToFolderForLargeModelsForTest4_2,
						amountSolutionsToBeGenerated, "large", amountThreads, testIfModelValid, calculateAmountOfPaths);

				System.out.println("Test 4_2 finished!");
			}
		}

		if (methodsToRun.contains(test5ToRun)) {
			// Test 5 - "mandatory participants"
			// take all models from trade off test

			if (pathToSmallProcessesForTest2WithAnnotation.isEmpty()
					&& pathToMediumProcessesForTest2WithAnnotation.isEmpty()
					&& pathToLargeProcessesForTest2WithAnnotation.isEmpty()
					&& pathToExtraLargeProcessesForTest2WithAnnotation.isEmpty()) {
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
						pathToFolderForSmallModelsForTest5, amountSolutionsToBeGenerated, "small", amountThreads,
						testIfModelValid, calculateAmountOfPaths);

				BatchFileGenerator.performTestWithMandatoryConstraints(mediumProcessesFromTradeOffTest, false,
						pathToFolderForMediumModelsForTest5, amountSolutionsToBeGenerated, "medium", amountThreads,
						testIfModelValid, calculateAmountOfPaths);

				BatchFileGenerator.performTestWithMandatoryConstraints(largeProcessesFromTradeOffTest, false,
						pathToFolderForLargeModelsForTest5, amountSolutionsToBeGenerated, "large", amountThreads,
						testIfModelValid, calculateAmountOfPaths);

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
					alwaysMaxMandConstrained, amountSolutionsToBeGenerated, amountThreads,
					timeOutForProcessGeneratorInMin);
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
			boolean alwaysMaxMandConstrained, int boundForSolutionsToBeGenerated, int amountThreads,
			int timeoutProcessGen) {

		ExecutorService randomProcessGeneratorService = Executors.newFixedThreadPool(amountThreads);
		BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathWhereToCreateProcessesWithoutAnnotation, 2,
				upperBoundParticipants, lowerBoundTasks, upperBoundTasks, 1, upperBoundXorGtws, 0,
				upperBoundParallelGtws, amountProcesses, false, timeOutForProcessGeneratorInMin, percentageOfXorsAsSeq);
		randomProcessGeneratorService.shutdownNow();

		int publicDecisionProb = 0;

		BatchFileGenerator.annotateModels(pathWhereToCreateProcessesWithoutAnnotation,
				pathWhereToCreateAnnotatedProcesses, dataObjectBoundsRealWorld, defaultSpheres, dynamicWriterProb,
				writersOfProcessInPercent, readersOfProcessInPercent, 2, upperBoundParticipants,
				minDataObjectsPerDecision, maxDataObjectsPerDecision, publicDecisionProb,
				probabilityForGatewayToHaveExclConstraint, lowerBoundAmountParticipantsToExclude,
				lowerBoundAmountParticipantsToExclude, alwaysMaxExclConstrained,
				probabilityForGatewayToHaveMandConstraint, lowerBoundAmountParticipantsToBeMandatory,
				upperBoundAmountParticipantsToBeMandatory, alwaysMaxMandConstrained, testIfModelValid);

		ExecutorService service = Executors.newFixedThreadPool(amountThreads);
		File csv = BatchFileGenerator.createNewCSVFile(pathWhereToCreateAnnotatedProcesses, "test6_results");
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csv);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathWhereToCreateAnnotatedProcesses, 100,
				maxXorsToRunExhaustiveOn, true, true, true, true, boundForSolutionsToBeGenerated, writer, service,
				testIfModelValid, calculateAmountOfPaths);
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
			boolean alwaysMaxMandConstrained, boolean testIfModelValid) {
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
				p = new ProcessModelAnnotater(pathToFile, pathToFolderForStoringAnnotatedModelsFolder, "_annotated",
						testIfModelValid);

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
			boolean firstElementAfterStartIsTask, int timeoutForProcessGenerator,
			int percentageProcessesWithXorsAlwaysInSeq) {

		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);

		int amountProcessesWithSeqXors = Math.round(amountProcesses * percentageProcessesWithXorsAlwaysInSeq / 100);

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
			if (randomAmountXorSplitsWithinBounds > 0) {
				randomAmountTasksWithinBounds = randomAmountTasksWithinBounds - randomAmountXorSplitsWithinBounds;
			}

			int processId = 0;
			try {
				boolean allowNestedXors = true;

				if (amountProcessesWithSeqXors > 0) {
					allowNestedXors = false;
					amountProcessesWithSeqXors--;
				}

				boolean found = false;
				while (!found) {
					int nestingDepthFactor = ThreadLocalRandom.current().nextInt(nestingDepthFactorBounds.get(0),
							nestingDepthFactorBounds.get(1) + 1);
					int probJoinGtw = ThreadLocalRandom.current().nextInt(probJoinGtwBounds.get(0),
							probJoinGtwBounds.get(1) + 1);

					ProcessGenerator pGen = new ProcessGenerator(pathToFiles, randomAmountParticipantsWithinBounds,
							randomAmountTasksWithinBounds, randomAmountXorSplitsWithinBounds,
							randomAmountParallelSplitsWithinBounds, probTask, probXorGtw, probParallelGtw, probJoinGtw,
							nestingDepthFactor, firstElementAfterStartIsTask, allowNestedXors);

					processId = pGen.getProcessId();
					Future<File> future = executor.submit(pGen);
					try {
						File f = future.get(timeoutForProcessGenerator, TimeUnit.MINUTES);
						future.cancel(true);
						// all variables of the generated file must be within the bounds
						if (f != null && f.isFile()) {
							System.out.println("randomProcess" + processId + " deployed in " + pathToFiles);
							found = true;
						} else if (f == null) {
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
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				i--;
			}

		}
		executor.shutdownNow();
	}

	public static LinkedList<String> pathsForFiles(String pathToFiles) {
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
		return paths;
	}

	public static API mapFileToAPI(String pathToFile, ResultsToCSVWriter writer, boolean testIfModelValid,
			boolean calculateAmountOfPaths) {
		API api = null;
		try {
			api = new API(pathToFile, costParameters, testIfModelValid, calculateAmountOfPaths);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			File f = new File(pathToFile);
			writer.addNullValueRowForModel(f.getName(), f.getAbsolutePath(), e.getMessage());
			System.err.println(f.getAbsolutePath() + " could not be mapped to api: " + e.getMessage());
		}
		return api;
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

	public static synchronized HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> runAlgsAndWriteResults(
			API api, int percentageOfProcessesToRunExhaustiveOn, int maxXorsToRunExhaustiveOn,
			boolean runExhaustiveSearch, boolean runNaiveSearch, boolean runIncrementalNaiveSearch,
			boolean runAdvancedNaiveSearch, int amountSolutionsToGeneratedWithNaive,
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
		HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = new HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>>();
		returnMap.put(true, new HashMap<Enums.AlgorithmToPerform, Integer>());
		returnMap.put(false, new HashMap<Enums.AlgorithmToPerform, Integer>());

		try {
			if (runExhaustiveSearch) {
				int randomInt = ThreadLocalRandom.current().nextInt(1, 101);
				if (randomInt <= percentageOfProcessesToRunExhaustiveOn) {
					// do not allow models with more than maxXorsToRunExhaustiveOn xors in order to avoid heap space exceptions
					if (CommonFunctionality
							.getAmountExclusiveGtwSplits(api.getModelInstance()) <= maxXorsToRunExhaustiveOn) {
						boolean heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.EXHAUSTIVE, 0,
								exceptionPerAlgorithm, cheapestSolutionsMap, totalAmountSolutionsMap,
								cheapestSolutionCostMap, loggingMap, timeOutOrHeapSpaceExceptionMap, service);
						if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) != null) {
							int res = timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE);
							returnMap.get(heapSpaceError).put(Enums.AlgorithmToPerform.EXHAUSTIVE, res);
						}
					}
				}
			}

			if (runNaiveSearch) {
				boolean heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.NAIVE,
						amountSolutionsToGeneratedWithNaive, exceptionPerAlgorithm, cheapestSolutionsMap,
						totalAmountSolutionsMap, cheapestSolutionCostMap, loggingMap, timeOutOrHeapSpaceExceptionMap,
						service);
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.NAIVE) != null) {
					int res = timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.NAIVE);
					returnMap.get(heapSpaceError).put(Enums.AlgorithmToPerform.NAIVE, res);
				}
			}

			if (runIncrementalNaiveSearch) {
				boolean heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.INCREMENTALNAIVE,
						amountSolutionsToGeneratedWithNaive, exceptionPerAlgorithm, cheapestSolutionsMap,
						totalAmountSolutionsMap, cheapestSolutionCostMap, loggingMap, timeOutOrHeapSpaceExceptionMap,
						service);
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) != null) {
					int res = timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE);
					returnMap.get(heapSpaceError).put(Enums.AlgorithmToPerform.INCREMENTALNAIVE, res);
				}
			}

			if (runAdvancedNaiveSearch) {
				boolean heapSpaceError = runAlgorithm(api, Enums.AlgorithmToPerform.ADVANCEDNAIVE,
						amountSolutionsToGeneratedWithNaive, exceptionPerAlgorithm, cheapestSolutionsMap,
						totalAmountSolutionsMap, cheapestSolutionCostMap, loggingMap, timeOutOrHeapSpaceExceptionMap,
						service);
				if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE) != null) {
					int res = timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE);
					returnMap.get(heapSpaceError).put(Enums.AlgorithmToPerform.ADVANCEDNAIVE, res);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.writeResultsOfAlgorithmsToCSVFile(api, cheapestSolutionCostMap, totalAmountSolutionsMap,
					cheapestSolutionsMap, loggingMap, exceptionPerAlgorithm);
		}
		return returnMap;
	}

	public static synchronized boolean runAlgorithm(API api, Enums.AlgorithmToPerform algToPerform,
			int amountSolutionsToGenerate, HashMap<Enums.AlgorithmToPerform, Exception> exceptionPerAlgorithm,
			HashMap<Enums.AlgorithmToPerform, Integer> cheapestSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, Integer> totalAmountSolutionsMap,
			HashMap<Enums.AlgorithmToPerform, Double> cheapestSolutionCostMap,
			HashMap<Enums.AlgorithmToPerform, String> loggingMap,
			HashMap<Enums.AlgorithmToPerform, Integer> timeOutOrHeapSpaceExceptionMap, ExecutorService service) {

		boolean heapSpaceError = false;
		String logging = "null";

		int bound = 1;
		if (algToPerform.equals(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
			bound = 0;
		}
		Future<LinkedList<PModelWithAdditionalActors>> futureAlgSearch = api.executeCallable(service, algToPerform,
				bound);

		Exception exception = null;
		try {
			LinkedList<PModelWithAdditionalActors> solutions = futureAlgSearch.get(timeOutForApiInMin,
					TimeUnit.MINUTES);
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
			System.err.println("Interrupted here!");
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
			// futureAlgSearch.cancel(true);
			exceptionPerAlgorithm.putIfAbsent(algToPerform, exception);
			loggingMap.putIfAbsent(algToPerform, logging);
		}

		return heapSpaceError;

	}

	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels,
			int percentageOfExtraLargeProcessesToRunExhaustiveOn, int maxXorsToRunExhaustiveOn,
			boolean runExhaustiveSearch, boolean runIncrementalNaiveSearch, boolean runNaiveSearch,
			boolean runAdvancedNaiveSearch, int boundForSearchWithBound, ResultsToCSVWriter writer,
			ExecutorService service, boolean testIfModelValid, boolean calculateAmountOfPaths) {
		LinkedList<String> pathsForFiles = pathsForFiles(pathToFolderWithAnnotatedModels);
		if (!pathsForFiles.isEmpty()) {
			for (String pathToFile : pathsForFiles) {
				API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.runAlgsAndWriteResults(api, percentageOfExtraLargeProcessesToRunExhaustiveOn,
						maxXorsToRunExhaustiveOn, runExhaustiveSearch, runNaiveSearch, runIncrementalNaiveSearch,
						runAdvancedNaiveSearch, boundForSearchWithBound,
						new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, service);
			}
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}

	}

	public static void performDataObjectTest(LinkedList<File> processes, LinkedList<Integer> fixedAmountReaders,
			LinkedList<Integer> fixedAmountWriters, String pathToDestinationFolderForStoringModels,
			int sumAmountUniqueDataObjects, int boundForGeneratingSolutions, int amountThreadPools,
			boolean testIfModelisValid, LinkedList<String> sphereToConnect) throws IOException {
		// each decision has unique dataObject sets of same size
		// e.g. model has 3 decisions -> and maxAmountUniqueDataObjects = 12 -> each
		// decision will have 4 unique dataObjects
		// unique dataObjects need to be connected
		// -> at least 12 readers and 12 writers needed, since every data object will
		// have to be first written and then get read by some brt

		// amount additional actors per gtw will be between 1 and privateSphere-1

		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		for (int i = 0; i < processes.size(); i++) {
			for (int indexWriters = 0; indexWriters < fixedAmountWriters.size(); indexWriters++) {
				for (int indexReaders = 0; indexReaders < fixedAmountReaders.size(); indexReaders++) {
					String pathToRandomProcess = processes.get(i).getAbsolutePath();
					BpmnModelInstance processModel = Bpmn.readModelFromFile(processes.get(i));
					int amountTasks = processModel.getModelElementsByType(Task.class).size();
					int amountDecisions = CommonFunctionality.getAmountExclusiveGtwSplits(processModel);
					int privateSphere = CommonFunctionality.getPrivateSphere(processModel, false);
					int upperBoundAddActors = privateSphere - 1;

					for (int addActors = 1; addActors <= upperBoundAddActors; addActors++) {

						try {

							StringBuilder suffixBuilder = new StringBuilder();
							// additional readers and writers suffix
							suffixBuilder.append("adR" + fixedAmountReaders.get(indexReaders));
							suffixBuilder.append("adW" + fixedAmountWriters.get(indexWriters));
							ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess,
									pathToDestinationFolderForStoringModels, suffixBuilder.toString(),
									testIfModelisValid);
							amountTasks = CommonFunctionality.getAmountTasks(pModel.getModelInstance());

							if (amountTasks > sumAmountUniqueDataObjects) {
								int amountWritersNeededForDataObjects = sumAmountUniqueDataObjects;
								int amountReadersNeededForDataObjects = sumAmountUniqueDataObjects;

								int amountWritersToBeInserted = amountWritersNeededForDataObjects
										+ fixedAmountWriters.get(indexWriters);
								int amountReadersToBeInserted = amountReadersNeededForDataObjects
										+ fixedAmountReaders.get(indexReaders);

								int minDataObjectsPerDecision = sumAmountUniqueDataObjects / amountDecisions;
								pModel.addNamesForOutgoingFlowsOfXorSplits(defaultNamesSeqFlowsXorSplits);
								pModel.generateDataObjects(sumAmountUniqueDataObjects, sphereToConnect);
								pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(minDataObjectsPerDecision,
										minDataObjectsPerDecision, amountReadersNeededForDataObjects, addActors,
										addActors, 0, true, true);

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
									boolean modelFound = false;
									while (!modelFound) {
										try {
											f.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
											f.cancel(true);
											modelFound = true;
										} catch (Exception e) {
											e.printStackTrace();
											f.cancel(true);
										}
									}
								}
							}
						} catch (Exception e) {
							System.err.println(e.getMessage());

						}
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

		for (File file : modelsToEvaluate) {
			String pathToFile = file.getAbsolutePath();
			API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
			BatchFileGenerator.runAlgsAndWriteResults(api, 100, maxXorsToRunExhaustiveOn, true, true, true, true,
					boundForGeneratingSolutions, new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, executor);

		}

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
			int amountModelsPerDecision, int amountDecisions, int minAmountTasks, boolean allowParallelBranches,
			String pathToSourceFolder) {
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
				boolean addModel = true;
				if (!allowParallelBranches) {
					// check if model contains parallels
					int amountParallels = CommonFunctionality.getAmountParallelGtwSplits(modelInst);
					if (amountParallels > 0) {
						addModel = false;
					}
				}

				if (modelInstDecisions == amountDecisions && amountTasks >= minAmountTasks && addModel) {
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

	public static void performTradeOffTest(String processType, String pathToDestinationFolderForStoringModels,
			List<Integer> dataObjectBounds, int upperBoundSolutionsForLocalMinWithBound, int amountXorsLowerBound,
			int amountXorsUpperBound, int modelsToTakePerDecision, String pathToProcessesFolderWithoutAnnotation,
			int percentageOfProcessesToRunExhaustiveOn, int maxXorsToRunExhaustiveOn, int amountThreadPools,
			boolean testIfModelValid, boolean calculateAmountOfPaths) {

		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);
		LinkedList<String> emptySphere = new LinkedList<String>();
		emptySphere.add("");

		for (int xorsIndex = amountXorsLowerBound; xorsIndex <= amountXorsUpperBound; xorsIndex++) {
			LinkedList<File> processesWithoutAnnotation = BatchFileGenerator
					.getModelsInOrderFromSourceFolderWithExactAmountDecision(Integer.MAX_VALUE, xorsIndex,
							pathToProcessesFolderWithoutAnnotation);

			int amountModelsWithAllValidCombs = 0;
			Iterator<File> processesWithoutAnnotationIter = processesWithoutAnnotation.iterator();

			while (processesWithoutAnnotationIter.hasNext()
					&& amountModelsWithAllValidCombs < modelsToTakePerDecision) {
				File processWithoutAnnotation = processesWithoutAnnotationIter.next();
				String pathToRandomProcess = processWithoutAnnotation.getAbsolutePath();
				File newModel = null;
				int amountRandomCountDataObjectsToCreate = 0;
				try {
					ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess,
							pathToDestinationFolderForStoringModels, "", testIfModelValid);

					// the model must fulfill the min and max amount of readers
					double minAmountReadersInPercent = percentageOfReadersClasses.get(0);

					int amountTasks = pModel.getModelInstance().getModelElementsByType(Task.class).size();

					int minAmountReaders = CommonFunctionality.getAmountFromPercentage(amountTasks,
							minAmountReadersInPercent);

					// randomly generate dataObjects in the range [DataObjects, maxCountDataObjects]
					amountRandomCountDataObjectsToCreate = ThreadLocalRandom.current().nextInt(dataObjectBounds.get(0),
							dataObjectBounds.get(1) + 1);

					// create model with amountRandomCountDataObjects dataObjects, 0 additional
					// actors and
					// empty string as starting sphere
					pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, emptySphere);

					// connect between 1 and amountRandomCountDataObjectsToCreate per decision
					pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(1, amountRandomCountDataObjectsToCreate,
							minAmountReaders, 0, 0, 0, false, true);

					CommonFunctionality
							.removeDataObjectsWithoutConnectionsToDecisionsFromModel(pModel.getModelInstance());

					newModel = pModel.writeChangesToFileWithoutCorrectnessCheck();
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}

				if (newModel != null) {

					// need to find valid annotations for a model on each iteration
					int maxCombs = percentageOfReadersClasses.size() * percentageOfWritersClasses.size();

					LinkedList<File> modelsCombsGenerated = new LinkedList<File>();

					BpmnModelInstance newModelInstance = Bpmn.readModelFromFile(newModel);
					int amountTasks = newModelInstance.getModelElementsByType(Task.class).size();
					int amountDataObjects = newModelInstance.getModelElementsByType(DataObjectReference.class).size();
					int currAmountReaderTasksInModel = newModelInstance
							.getModelElementsByType(DataInputAssociation.class).size();

					outerloop: for (int writerClass = 0; writerClass < percentageOfWritersClasses
							.size(); writerClass++) {
						// for each model -> annotate it with small, medium, large amount of writers
						int amountWriterTasksToBeInserted = CommonFunctionality.getAmountFromPercentageWithMinimum(
								amountTasks, percentageOfWritersClasses.get(writerClass), amountDataObjects);

						for (int readerClass = 0; readerClass < percentageOfReadersClasses.size(); readerClass++) {
							// for each model -> annotate it with small, medium, large amount of readers

							int amountReaderTasksToBeInsertedForPercentage = CommonFunctionality
									.getAmountFromPercentage(amountTasks, percentageOfReadersClasses.get(readerClass));

							int amountReaderTasksToInsertLeft = amountReaderTasksToBeInsertedForPercentage
									- currAmountReaderTasksInModel;

							StringBuilder suffixBuilder = new StringBuilder();
							suffixBuilder.append('_');

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

							boolean insertWriters = true;
							if (amountReaderTasksToInsertLeft < 0) {
								// model has already more reader than demanded by percentage of tasks
								insertWriters = false;
							}

							int amountWriterTasksForPercentage = CommonFunctionality
									.getAmountFromPercentage(amountTasks, percentageOfWritersClasses.get(writerClass));
							if (amountWriterTasksToBeInserted > amountWriterTasksForPercentage) {
								// model needs more writers than demanded by percentage of tasks
								insertWriters = false;
							}

							if (insertWriters) {
								ProcessModelAnnotater modelWithReadersAndWriters;
								try {
									modelWithReadersAndWriters = new ProcessModelAnnotater(newModel.getAbsolutePath(),
											pathToDestinationFolderForStoringModels, suffix, testIfModelValid);
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
									argumentsForSecondMethod[1] = amountReaderTasksToInsertLeft;
									argumentsForSecondMethod[2] = 0;
									argumentsForSecondMethod[3] = emptySphere;
									methodsToBeCalledMap.putIfAbsent(secondMethodToBeCalledName,
											argumentsForSecondMethod);

									modelWithReadersAndWriters.setMethodsToRunWithinCall(methodsToBeCalledMap);

									Future<File> future = executor.submit(modelWithReadersAndWriters);
									try {
										File generatedFile = future.get(timeOutForProcessModelAnnotaterInMin,
												TimeUnit.MINUTES);
										future.cancel(true);
										modelsCombsGenerated.add(generatedFile);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										future.cancel(true);
										e.printStackTrace();
									} catch (ExecutionException e) {
										// TODO Auto-generated catch block
										System.err.println(e.getMessage());
										future.cancel(true);
									} catch (TimeoutException e) {
										// TODO Auto-generated catch block
										future.cancel(true);
										e.printStackTrace();
									}

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							} else {
								// model can not be annotated with the current readers/writers combination
								// skip this combination
								System.out.println("Readers: " + amountReaderTasksToBeInsertedForPercentage
										+ " Writers: " + amountWriterTasksToBeInserted);
								break outerloop;
							}
						}
					}
					if (modelsCombsGenerated.size() != maxCombs) {
						// not for each combination of readers/writers there has been a valid model
						// remove all the models
						Iterator<File> generatedFileIter = modelsCombsGenerated.iterator();
						while (generatedFileIter.hasNext()) {
							File generatedFile = generatedFileIter.next();
							generatedFile.delete();
						}

						// delete the newModel generated
						System.out.println("Not all combs generated! Remove " + newModel.getName());
						newModel.delete();

					} else {
						amountModelsWithAllValidCombs++;
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
		try {
			for (File modelToEvaluate : modelsToEvaluate) {
				String pathToFile = modelToEvaluate.getAbsolutePath();
				API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.runAlgsAndWriteResults(api, percentageOfProcessesToRunExhaustiveOn,
						maxXorsToRunExhaustiveOn, true, true, true, true, upperBoundSolutionsForLocalMinWithBound,
						new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, service);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			service.shutdownNow();
			writer.writeRowsToCSVAndcloseWriter();
		}
	}

	public static void performBoundaryTest1_1(int amountProcessesPerIteration, int amountDecisionsToStart,
			int addActorsPerDecision, int privateSphere, int upperBoundSolutionsForLocalMinWithBound,
			int amountTasksToStartWith, int tasksFactor, int lowerBoundAmountParallelGtws,
			int upperBoundAmountParallelGtws, double amountWritersInPercent, double amountReadersInPercent,
			int minDataObjectsPerDecision, int maxDataObjectsPerDecision, LinkedList<String> sphereList,
			int amountThreads, int stopAfterDecisions, String pathToFolderForModelsForBoundaryTest,
			boolean firstElementAfterStartIsTask, int timeoutProcessGenInMin, boolean testIfModelValid,
			boolean calculateAmountOfPaths, int percentageProcessesWithXorsAlwaysInSeq) {

		int amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
				upperBoundAmountParallelGtws + 1);

		File csvFile = BatchFileGenerator.createNewCSVFile(pathToFolderForModelsForBoundaryTest, "test1_1-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		HashMap<Enums.AlgorithmToPerform, Integer> timeOutOrHeapSpaceExceptionMap = new HashMap<Enums.AlgorithmToPerform, Integer>();

		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		boolean runExhaustiveSearch = true;
		boolean runNaiveSearch = true;
		boolean runIncrementalNaiveSearch = true;
		boolean runAdvancedNaiveSearch = true;
		boolean finishTest = false;
		try {
			int i = amountDecisionsToStart;
			do {
				if (i > 15) {
					testIfModelValid = false;
				}
				timeOutOrHeapSpaceExceptionMap.clear();
				timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
				timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.NAIVE, 0);
				timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.INCREMENTALNAIVE, 0);
				timeOutOrHeapSpaceExceptionMap.put(Enums.AlgorithmToPerform.ADVANCEDNAIVE, 0);

				int amountDataObjectsToCreate = amountDecisionsToStart;
				System.out.println("Generate models with " + amountDecisionsToStart + " decisions!");

				String folderName = "BoundaryTest_decision-" + amountDecisionsToStart;
				String pathToFolderForModelsWithDecisions = CommonFunctionality
						.fileWithDirectoryAssurance(pathToFolderForModelsForBoundaryTest, folderName).getAbsolutePath();

				int amountTasksWithFactor = amountTasksToStartWith + (amountDecisionsToStart * tasksFactor);
				amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
						upperBoundAmountParallelGtws + 1);

				// generate processes
				BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToFolderForModelsWithDecisions,
						privateSphere, privateSphere, amountTasksWithFactor, amountTasksWithFactor,
						amountDecisionsToStart, amountDecisionsToStart, amountParallelGtws, amountParallelGtws,
						amountProcessesPerIteration, firstElementAfterStartIsTask, timeoutProcessGenInMin,
						percentageProcessesWithXorsAlwaysInSeq);

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
					File modelCopy = (File) CommonFunctionality.deepCopy(model);
					ProcessModelAnnotater p;
					System.out.println(model.getName());
					try {
						p = new ProcessModelAnnotater(modelCopy.getAbsolutePath(),
								pathToFolderForModelsWithDecisionsAnnotated, "_annotated", testIfModelValid);
						int tasks = CommonFunctionality.getAmountTasks(p.getModelInstance());

						int amountWriters = CommonFunctionality.getAmountFromPercentageWithMinimum(tasks,
								amountWritersInPercent, amountDataObjectsToCreate);
						int minAmountReaders = amountDataObjectsToCreate;
						int amountReadersPercentage = CommonFunctionality.getAmountFromPercentage(tasks,
								amountReadersInPercent);
						int amoundReadersLeft = amountReadersPercentage - minAmountReaders;
						if (amoundReadersLeft < 0) {
							amoundReadersLeft = 0;
						}

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
						Object[] argumentsForThirdMethod = new Object[8];
						argumentsForThirdMethod[0] = minDataObjectsPerDecision;
						argumentsForThirdMethod[1] = maxDataObjectsPerDecision;
						argumentsForThirdMethod[2] = minAmountReaders;
						argumentsForThirdMethod[3] = addActorsPerDecision;
						argumentsForThirdMethod[4] = addActorsPerDecision;
						argumentsForThirdMethod[5] = 0;
						argumentsForThirdMethod[6] = true;
						argumentsForThirdMethod[7] = true;
						methodsToBeCalledMap.putIfAbsent(thirdMethodToBeCalledName, argumentsForThirdMethod);

						String fourthMethodToBeCalledName = "annotateModelWithFixedAmountOfReadersAndWriters";
						Object[] argumentsForFourthMethod = new Object[4];
						argumentsForFourthMethod[0] = amountWriters;
						argumentsForFourthMethod[1] = amoundReadersLeft;
						argumentsForFourthMethod[2] = 0;
						argumentsForFourthMethod[3] = defaultSpheres;
						methodsToBeCalledMap.putIfAbsent(fourthMethodToBeCalledName, argumentsForFourthMethod);

						// set the methods that will be run within the call method
						p.setMethodsToRunWithinCall(methodsToBeCalledMap);

						Future<File> future = executor.submit(p);
						try {
							future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
							System.out.println("Model annotated correctly!");
							future.cancel(true);
						} catch (Exception e) {
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
				}

				LinkedList<String> pathForFiles = pathsForFiles(pathToFolderForModelsWithDecisionsAnnotated);

				// perform all algorithms and count the timeouts
				for (String pathToFile : pathForFiles) {
					API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
					HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = BatchFileGenerator
							.runAlgsAndWriteResults(api, 100, maxXorsToRunExhaustiveOn, runExhaustiveSearch,
									runNaiveSearch, runIncrementalNaiveSearch, runAdvancedNaiveSearch,
									upperBoundSolutionsForLocalMinWithBound, timeOutOrHeapSpaceExceptionMap, writer,
									executor);
					if (returnMap.get(true) != null) {
						// algorithms have led to heap space exception
						timeOutOrHeapSpaceExceptionMap = returnMap.get(true);
						for (Entry<AlgorithmToPerform, Integer> entr : timeOutOrHeapSpaceExceptionMap.entrySet()) {
							if (entr.getKey().equals(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
								runExhaustiveSearch = false;
							}
							if (entr.getKey().equals(Enums.AlgorithmToPerform.NAIVE)) {
								runNaiveSearch = false;
							}
							if (entr.getKey().equals(Enums.AlgorithmToPerform.INCREMENTALNAIVE)) {
								runIncrementalNaiveSearch = false;
							}
							if (entr.getKey().equals(Enums.AlgorithmToPerform.ADVANCEDNAIVE)) {
								runAdvancedNaiveSearch = false;
							}
						}

					}
					timeOutOrHeapSpaceExceptionMap = returnMap.get(false);
				}

				if (!pathForFiles.isEmpty()) {
					if (timeOutOrHeapSpaceExceptionMap.containsKey(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
						if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) == pathForFiles
								.size()) {
							runExhaustiveSearch = false;
						}
					}

					if (timeOutOrHeapSpaceExceptionMap.containsKey(Enums.AlgorithmToPerform.NAIVE)) {
						if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.NAIVE) == pathForFiles.size()) {
							runNaiveSearch = false;
						}
					}

					if (timeOutOrHeapSpaceExceptionMap.containsKey(Enums.AlgorithmToPerform.INCREMENTALNAIVE)) {
						if (timeOutOrHeapSpaceExceptionMap
								.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) == pathForFiles.size()) {
							runIncrementalNaiveSearch = false;
						}
					}

					if (timeOutOrHeapSpaceExceptionMap.containsKey(Enums.AlgorithmToPerform.ADVANCEDNAIVE)) {
						if (timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE) == pathForFiles
								.size()) {
							runAdvancedNaiveSearch = false;
						}
					}
				} else {
					finishTest = true;
				}

				System.out.println("Iteration" + amountDecisionsToStart + " end - timeOutsExhaustiveSearch: "
						+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE)
						+ ", timeOutsNaiveSearch: " + timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.NAIVE)
						+ ", timeOutsAdvancedNaiveSearch: "
						+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE)
						+ ", timeOutsIncrementalNaiveSearch: "
						+ timeOutOrHeapSpaceExceptionMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE));

				amountDecisionsToStart++;

				if (runExhaustiveSearch == false && runAdvancedNaiveSearch == false && runNaiveSearch == false
						&& runIncrementalNaiveSearch == false) {
					finishTest = true;
				}

				if (i == stopAfterDecisions) {
					finishTest = true;
				}
				i++;

			} while ((!finishTest));

			executor.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			executor.shutdownNow();
			writer.writeRowsToCSVAndcloseWriter();
		}

	}

	public static void performBoundaryTest1_2(File file, int privateSphereLowerBound,
			int amountNewProcessesToCreatePerIteration, int boundForAlgorithms, int amountThreadPools,
			String directoryToStore, boolean testIfModelValid, boolean calculateAmountOfPaths) {

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStore, "test1_2-results");

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);
		boolean runExhaustive = true;
		boolean runNaive = true;
		boolean runIncrementalNaive = true;
		boolean runAdvancedNaive = true;
		boolean finish = false;

		try {
			if (!finish) {
				CommonFunctionality.generateNewModelsUntilPrivateSphereReached(file, privateSphereLowerBound,
						amountNewProcessesToCreatePerIteration, directoryToStore);

				File[] directories = new File(directoryToStore).listFiles(File::isDirectory);
				Arrays.sort(directories, Comparator.comparingLong(File::lastModified));

				for (File directory : directories) {
					String directoryPath = directory.getAbsolutePath();
					// map annotated models

					LinkedList<String> pathForFiles = pathsForFiles(directoryPath);

					// perform all algorithms and count the timeouts
					HashMap<Enums.AlgorithmToPerform, Integer> timeOutMap = new HashMap<Enums.AlgorithmToPerform, Integer>();
					timeOutMap.put(Enums.AlgorithmToPerform.EXHAUSTIVE, 0);
					timeOutMap.put(Enums.AlgorithmToPerform.NAIVE, 0);
					timeOutMap.put(Enums.AlgorithmToPerform.INCREMENTALNAIVE, 0);
					timeOutMap.put(Enums.AlgorithmToPerform.ADVANCEDNAIVE, 0);

					for (String pathToFile : pathForFiles) {
						API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
						HashMap<Boolean, HashMap<Enums.AlgorithmToPerform, Integer>> returnMap = BatchFileGenerator
								.runAlgsAndWriteResults(api, 100, maxXorsToRunExhaustiveOn, runExhaustive, runNaive,
										runIncrementalNaive, runAdvancedNaive, boundForAlgorithms, timeOutMap, writer,
										executor);

						if (returnMap.get(true) != null) {
							// algorithms have led to heap space exception
							timeOutMap = returnMap.get(true);
							for (Entry<AlgorithmToPerform, Integer> entr : timeOutMap.entrySet()) {
								if (entr.getKey().equals(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
									runExhaustive = false;
								}
								if (entr.getKey().equals(Enums.AlgorithmToPerform.NAIVE)) {
									runNaive = false;
								}
								if (entr.getKey().equals(Enums.AlgorithmToPerform.INCREMENTALNAIVE)) {
									runIncrementalNaive = false;
								}
								if (entr.getKey().equals(Enums.AlgorithmToPerform.ADVANCEDNAIVE)) {
									runAdvancedNaive = false;
								}
							}
						}

						timeOutMap = returnMap.get(false);

						if (timeOutMap.containsKey(Enums.AlgorithmToPerform.EXHAUSTIVE)) {
							if (timeOutMap.get(Enums.AlgorithmToPerform.EXHAUSTIVE) == pathForFiles.size()) {
								runExhaustive = false;
							}
						}

						if (timeOutMap.containsKey(Enums.AlgorithmToPerform.NAIVE)) {
							if (timeOutMap.get(Enums.AlgorithmToPerform.NAIVE) == pathForFiles.size()) {
								runNaive = false;
							}
						}

						if (timeOutMap.containsKey(Enums.AlgorithmToPerform.INCREMENTALNAIVE)) {
							if (timeOutMap.get(Enums.AlgorithmToPerform.INCREMENTALNAIVE) == pathForFiles.size()) {
								runIncrementalNaive = false;
							}
						}

						if (timeOutMap.containsKey(Enums.AlgorithmToPerform.ADVANCEDNAIVE)) {
							if (timeOutMap.get(Enums.AlgorithmToPerform.ADVANCEDNAIVE) == pathForFiles.size()) {
								runAdvancedNaive = false;
							}
						}

						if (!runExhaustive && !runAdvancedNaive && !runNaive && !runIncrementalNaive) {
							finish = true;
						}
					}
				}
			}
			executor.shutdownNow();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			executor.shutdownNow();
			writer.writeRowsToCSVAndcloseWriter();
		}

	}

	public static void substituteDataObjectAndWriteNewModels(BpmnModelInstance modelInstance, String modelName,
			String directoryToStore) {

		LinkedList<DataObjectReference> daoList = new LinkedList<DataObjectReference>();
		daoList.addAll(modelInstance.getModelElementsByType(DataObjectReference.class));

		// get the one data object as substitute that has the first origin as writer
		// else there may be invalid models afterwards when replacing a data object that
		// is before the substitute when the writer is after the decision
		DataObjectReference substitute = null;

		StartEvent stEv = modelInstance.getModelElementsByType(StartEvent.class).iterator().next();

		String firstBrt = "Brt1";
		LinkedList<BusinessRuleTask> brts = new LinkedList<>(
				modelInstance.getModelElementsByType(BusinessRuleTask.class));
		outerloop: for (BusinessRuleTask brt : brts) {
			if (brt.getName().contains(firstBrt)) {
				try {
					LinkedList<LinkedList<FlowNode>> paths = CommonFunctionality.getAllPathsBetweenNodes(modelInstance,
							stEv.getId(), brt.getId());
					LinkedList<FlowNode> firstPath = paths.getFirst();

					for (FlowNode f : firstPath) {
						if (f instanceof Task) {
							Task task = (Task) f;
							for (DataOutputAssociation dao : task.getDataOutputAssociations()) {
								String ref = dao.getTarget().getId();
								for (DataObjectReference daoR : daoList) {
									if (daoR.getId().contentEquals(ref)) {
										substitute = daoR;
										break outerloop;
									}
								}
							}

						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		CommonFunctionality.substituteOneDataObjectPerIterationAndWriteNewModels(modelInstance, substitute, modelName,
				directoryToStore);

	}

	public static void performTestWithMandatoryConstraints(LinkedList<File> models, boolean modelWithLanes,
			String directoryToStoreNewModels, int boundSolutions, String suffixCSV, int amountThreads,
			boolean testIfModelValid, boolean calculateAmountOfPaths) {

		// each brt will have mandatory participants
		int probabilityForGatwayToHaveMandPartConst = 100;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				CommonFunctionality.addMandatoryParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatwayToHaveMandPartConst, 1, false, modelWithLanes, directoryToStoreNewModels);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		File csvFile = BatchFileGenerator.createNewCSVFile(directoryToStoreNewModels, "test5_results_" + suffixCSV);

		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		LinkedList<File> modelsToEvaluate = BatchFileGenerator.getAllModelsFromFolder(directoryToStoreNewModels);
		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);

		try {

			for (File modelToEvaluate : modelsToEvaluate) {
				String pathToFile = modelToEvaluate.getAbsolutePath();
				API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.runAlgsAndWriteResults(api, 100, maxXorsToRunExhaustiveOn, true, true, true, true,
						boundSolutions, new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, executor);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.writeRowsToCSVAndcloseWriter();
			executor.shutdownNow();
		}
	}

	public static void performTestWithMaxPossibleExcludeConstraints(LinkedList<File> models, boolean modelWithLanes,
			String directoryToStoreNewModels, int upperBoundSearchWithBound, String suffixCSV, int amountThreads,
			boolean testIfModelValid, boolean calculateAmountOfPaths) {
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
		try {
			for (File modelToEvaluate : modelsToEvaluate) {
				String pathToFile = modelToEvaluate.getAbsolutePath();
				API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.runAlgsAndWriteResults(api, 100, maxXorsToRunExhaustiveOn, true, true, true, true,
						upperBoundSearchWithBound, new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, executor);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.writeRowsToCSVAndcloseWriter();
			executor.shutdownNow();
		}
	}

	public static void performTestWithExcludeConstraintsProbability(LinkedList<File> models, boolean modelWithLanes,
			int probabilityForGatewayToHaveConstraint, String directoryToStoreNewModels, int upperBoundSearchWithBound,
			String suffixCSV, int amountThreads, boolean testIfModelValid, boolean calculateAmountOfPaths) {
		int lowerBoundAmountParticipantsToExcludePerGtw = 1;

		for (File file : models) {
			String fileName = file.getName();
			BpmnModelInstance currModel = Bpmn.readModelFromFile(file);
			BpmnModelInstance clone = (BpmnModelInstance) CommonFunctionality.deepCopy(currModel);

			try {
				CommonFunctionality.addExcludeParticipantConstraintsOnModel(clone, fileName,
						probabilityForGatewayToHaveConstraint, lowerBoundAmountParticipantsToExcludePerGtw, false,
						modelWithLanes, directoryToStoreNewModels);
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
		try {
			for (File modelToEvaluate : modelsToEvaluate) {
				String pathToFile = modelToEvaluate.getAbsolutePath();
				API api = mapFileToAPI(pathToFile, writer, testIfModelValid, calculateAmountOfPaths);
				BatchFileGenerator.runAlgsAndWriteResults(api, 100, maxXorsToRunExhaustiveOn, true, true, true, true,
						upperBoundSearchWithBound, new HashMap<Enums.AlgorithmToPerform, Integer>(), writer, executor);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			writer.writeRowsToCSVAndcloseWriter();
			executor.shutdownNow();
		}
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

	public static int getMinAmountTasksForMaxAmountDataObjects(List<Double> readersInPercentage,
			List<Double> writersInPercentage, int maxAmountDataObjects) {
		double minReadersInPercent = readersInPercentage.get(0);
		double minWritersInPercentage = writersInPercentage.get(0);
		// each data object will be connected to a brt, so at least 1 reader
		// and 1 writer -> since it needs to be written beforehand
		// minReadersInPercentage e.g. 10%
		// maxAmountDataObjects e.g. 6
		// -> 6 / (10/100) = minimum 60 tasks needed in the process that will be readers
		// there must be at least 60 writers to fulfill the min. amount of writers in
		// percentage
		// first origin of a data object can not be a reader

		double amountMaxTasksReaders = maxAmountDataObjects / (minReadersInPercent / 100);

		double amountMinTasksWriters = maxAmountDataObjects / (minWritersInPercentage / 100);

		double res = Math.max(amountMaxTasksReaders, amountMinTasksWriters);

		return (int) Math.round(res);

	}

}
