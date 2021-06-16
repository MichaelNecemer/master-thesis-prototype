package functionality;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

	static int timeOutForProcessGeneratorInMin = 2;
	static int timeOutForApiInMin = 1;
	static int timeOutForProcessModelAnnotaterInMin = 1;

	// bounds for ProcessModelGenerator
	static int probTask = 50;
	static int probXorGtw = 30;
	static int probParallelGtw = 20;
	static int probJoinGtw = 20;
	static int nestingDepthFactor = 5;

	// bounds for ProcessModelAnnotater
	static LinkedList<Integer> dataObjectBoundsSmallProcesses = new LinkedList<Integer>(Arrays.asList(1, 3));
	static LinkedList<Integer> dataObjectBoundsMediumProcesses = new LinkedList<Integer>(Arrays.asList(2, 5));
	static LinkedList<Integer> dataObjectBoundsLargeProcesses = new LinkedList<Integer>(Arrays.asList(1, 5));

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

		// Test 1 - Boundary Test
		// The amount solutions generated will be the binomial coefficient for each
		// decision multiplied
		// e.g. 2 decisions - 1. needs 2 out of 4 voters = 6 possible combinations, 2.
		// needs 3 out of 5 = 10 possible combinations -> 5*10 = 50 possible
		// combinations of participants
		// so the boundary will be heavily influenced by the combination of voters per
		// decision as well as the amount of decisions in the process

		// the boundary test will set the max size which will be taken for further tests

		// generate 10 Processes - annotate them - try performing algorithms with a time
		// limit
		// count amount of timeouts
		// do that while there are not timeouts on all processes for both algorithms

		String pathToFolderForModelsForBoundaryTest = CommonFunctionality
				.fileWithDirectoryAssurance(rootFolder, "BoundaryTest").getAbsolutePath();

		int upperBoundSolutionsForLocalMinWithBound = 1;
		int tasksFactor = 3; // for each new xor there will be tasksFactor more tasks in the model
		int votersPerDecision = 3;
		int globalSphere = 5;
		int amountDecisions = -1; // will be increased to 0 on first iteration
		int amountProcesses = 1;
		int amountTasks = 6;
		int lowerBoundAmountParallelGtws = 0;
		int upperBoundAmountParallelGtws = 0;
		int amountParallelGtws = 0;
		int amountWritersInPercent = percentageOfWritersClasses.get(1); // medium amount of writers
		int amountReadersInPercent = percentageOfWritersClasses.get(1); // medium amount of readers
		int minDataObjectsPerDecision = 1;
		int maxDataObjectsPerDecision = 1;
		LinkedList<String>sphereList = new LinkedList<String>();
		sphereList.add("Global");
		// for each decision annotate a unique dataObject

		int amountThreads = 1;

		File csvFile = BatchFileGenerator.createNewCSVFile(pathToFolderForModelsForBoundaryTest,
				"boundaryTestResults");
		
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		HashMap<String, Integer>timeOutMap = new HashMap<String, Integer>();


		ExecutorService executor = Executors.newFixedThreadPool(amountThreads);
		do {
			timeOutMap.put("bruteForce", 0);
			timeOutMap.put("localMin",0);
			amountDecisions++;
			int amountDataObjectsToCreate = amountDecisions;
			System.out.println("Generate models with " + amountDecisions + " decisions!");

			String folderName = "BoundaryTest_decision-" + amountDecisions;
			String pathToFolderForModelsWithDecisions = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsForBoundaryTest, folderName).getAbsolutePath();

			int amountTasksWithFactor = amountTasks + (amountDecisions * tasksFactor);
			amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws,
					upperBoundAmountParallelGtws + 1);
			int amountWriters = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasksWithFactor,
					amountWritersInPercent, amountDataObjectsToCreate);
			int amountReaders = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasksWithFactor,
					amountReadersInPercent, amountDataObjectsToCreate);

			// generate processes
			BatchFileGenerator.generateRandomProcessesWithFixedValues(pathToFolderForModelsWithDecisions, globalSphere,
					amountTasksWithFactor, amountDecisions, amountParallelGtws, amountProcesses, executor);

			// annotate the processes
			File folder = new File(pathToFolderForModelsWithDecisions);
			LinkedList<File> listOfFiles = new LinkedList<File>();
			listOfFiles.addAll(Arrays.asList(folder.listFiles()));

			String pathToFolderForModelsWithDecisionsAnnotated = CommonFunctionality
					.fileWithDirectoryAssurance(pathToFolderForModelsWithDecisions, "annotated").getAbsolutePath();
			Iterator<File> modelIter = listOfFiles.iterator();
			while (modelIter.hasNext()) { // annotate the model
				File model = modelIter.next();
				ProcessModelAnnotater p = new ProcessModelAnnotater(model.getAbsolutePath(),
						pathToFolderForModelsWithDecisionsAnnotated, "_annotated");
				p.addNamesForOutgoingFlowsOfXorSplits(defaultNamesSeqFlowsXorSplits);
				p.generateDataObjects(amountDataObjectsToCreate, sphereList);
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
			}

			// map annotated models
			LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderForModelsWithDecisionsAnnotated);

			// perform both algorithms and count the timeouts

			for (API api : apiList) {			
			timeOutMap = BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api, upperBoundSolutionsForLocalMinWithBound, timeOutMap, writer, executor);
			}
			
			writer.addEmptyRow();
			
			System.out.println("Iteration" + amountDecisions + " end - timeOutsBruteForce: " + timeOutMap.get("bruteForce")
					+ ", timeOutsLocalMin: " + timeOutMap.get("localMin"));
			
		} while (timeOutMap.get("bruteForce") < amountProcesses || timeOutMap.get("localMin") < amountProcesses);

		executor.shutdownNow();

		writer.writeRowsToCSVAndcloseWriter();
		
		System.out.println("FINISHED WRITING!");
		

		// generate 3 Classes -> small, medium, large processes (without annotation)
		// put them into a new folder into the root
		String pathToFolderForModelsWithoutAnnotation = CommonFunctionality
				.fileWithDirectoryAssurance(rootFolder, "ProcessesWithoutAnnotation").getAbsolutePath();

		String pathToSmallProcessesFolder = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "SmallProcessesFolder")
				.getAbsolutePath();
		String pathToMediumProcessesFolder = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "MediumProcessesFolder")
				.getAbsolutePath();
		String pathToLargeProcessesFolder = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "LargeProcessesFolder")
				.getAbsolutePath();

		/*
		 * // mixed processes with decisions for adnan and nada List<Integer>
		 * dataObjectBoundsMixed = Arrays.asList(1, 4); int dynamicWriterProbMixed = 30;
		 * int upperBoundParticipants = 6; String pathToMixedProcessesFolder =
		 * CommonFunctionality
		 * .fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation,
		 * "MixedProcessesFolder") .getAbsolutePath(); String
		 * pathToAnnotatedProcessesFolder = CommonFunctionality
		 * .fileWithDirectoryAssurance(pathToMixedProcessesFolder,
		 * "AnnotatedModels").getAbsolutePath();
		 * //BatchFileGenerator.generateRandomProcessesWithinGivenRanges(
		 * pathToMixedProcessesFolder, 2, // upperBoundParticipants, 10, 30, 1, 6, 0, 4,
		 * 30); List<Integer> writersOfProcessInPercent = Arrays.asList(10, 20, 30);
		 * List<Integer> readersOfProcessInPercent = Arrays.asList(10, 20, 30); int
		 * publicDecisionProb = 0; int minDataObjectsPerDecision = 1; int
		 * maxDataObjectsPerDecision = -1;
		 * BatchFileGenerator.annotateModels(pathToMixedProcessesFolder,
		 * pathToAnnotatedProcessesFolder, dataObjectBoundsMixed, defaultSpheres,
		 * dynamicWriterProbMixed, writersOfProcessInPercent, readersOfProcessInPercent,
		 * 2, upperBoundParticipants, minDataObjectsPerDecision,
		 * maxDataObjectsPerDecision, publicDecisionProb);
		 * 
		 * BatchFileGenerator.runLocalMinAndGenerateNewFilesWithAnnotatedSolutions(
		 * pathToAnnotatedProcessesFolder, 1);
		 */

		// for each processes folder -> generate 100 random processes
		// small processes: 2-5 participants, 7-15 tasks, 1-4 xors, 0-2 parallels, 100
		// processes
		// BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToSmallProcessesFolder,
		// 2, 5, 7, 15, 1, 4, 0, 2, 100);

		// medium processes: 6-10 participants, 16-30 tasks, 5-10 xors, 0-6 parallels,
		// 100 processes
		// BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToMediumProcessesFolder,
		// 6, 10, 16, 30, 5, 10, 0, 6, 100);

		// large processes: 11-15 participants, 31-60 tasks, 11-15 xors, 0-9, parallels,
		// 100 processes
		// BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolder,
		// 11, 15, 31, 60, 11, 15, 0, 9, 100);

		// each test will be based on the same randomly chosen models
		LinkedList<File> randomSmallProcesses = BatchFileGenerator.getRandomModelsFromSourceFolder(5,
				pathToSmallProcessesFolder);
		// LinkedList<File> randomMediumProcesses =
		// BatchFileGenerator.getRandomModelsFromSourceFolder(5,
		// pathToMediumProcessesFolder);
		// LinkedList<File> randomLargeProcesses =
		// BatchFileGenerator.getRandomModelsFromSourceFolder(20,
		// pathToLargeProcessesFolder);

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

		String pathToSmallProcessesForTest2 = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "SmallProcessesAnnotatedFolder")
				.getAbsolutePath();
		String pathToMediumProcessesForTest2 = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "MediumProcessesAnnotatedFolder")
				.getAbsolutePath();
		String pathToLargeProcessesForTest2 = CommonFunctionality
				.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "LargeProcessesAnnotatedFolder")
				.getAbsolutePath();

		//BatchFileGenerator.performTest2(randomSmallProcesses,
		 //pathToSmallProcessesForTest2, dataObjectBoundsSmallProcesses, 1);
		// BatchFileGenerator.performTest2(randomMediumProcesses,
		// pathToMediumProcessesForTest2, dataObjectBoundsMediumProcesses );

		// Test 3 - Measure impact of dataObjects
		// annotate different dataObject for each decision e.g. 5 decisions - 5
		// different dataObjects - each decision has 1 different dataObject connected
		// next step - choose one dataObject that will be annotated for a different
		// decision
		// this results in having 5 decisions where 1 of them has 2 dataObjects
		// connected
		// so 1 decision will be based on 2 dataObjects - 4 decisions with 1 different
		// dataObject connected

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
						publicDecisionProb, amountParticipantsPerDecisionLowerBound, amountDataObjectsToCreate,
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
						maxAmountMaxXorSplits, maxAmountMaxParallelSplits, probTask, readerProb, writerProb,
						probPublicSphere, nestingDepthFactor);

				Future<File> future = executor.submit(pGen);
				try {
					File f = future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
					System.out.println(f.getName() + " deployed successfully");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
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
						probXorGtw, probParallelGtw, probJoinGtw, nestingDepthFactor);
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

	private static File createNewCSVFile(String directoryForFile, String nameOfCSVFile) {
		String fileNameForResults = nameOfCSVFile + idCSVFile + ".csv";
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

		
	public static HashMap<String, Integer> runAlgorithmsAndWriteResultsToCSV(API api, int limitForLocalMin,HashMap<String, Integer>timeOutMap,
			ResultsToCSVWriter writer, ExecutorService service) {
		// write the result of an algorithm to an existing csv file
		//clone api for algorithms		
		API bruteForceApi = (API) CommonFunctionality.deepCopy(api);
		bruteForceApi.setAlgorithmToPerform("bruteForce");
		
		API localMinApi = (API) CommonFunctionality.deepCopy(api);
		localMinApi.setAlgorithmToPerform("localMin");
		
		
		String localMinWithMaxSolutions = "localMinWithLimit"+limitForLocalMin;
		API localMinWithMaxSolutionsApi = (API)CommonFunctionality.deepCopy(api);
		localMinWithMaxSolutionsApi.setAlgorithmToPerform(localMinWithMaxSolutions);
		
		
		HashMap<String, Exception> exceptionPerAlgorithm = new HashMap<String, Exception>();
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap = new HashMap<String, LinkedList<ProcessInstanceWithVoters>>();
		
		Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureBruteForce = service.submit(bruteForceApi);
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesBruteForce = null;
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
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			exceptionBruteForce = (ExecutionException)e;
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			exceptionBruteForce = (TimeoutException)e;
			timeOutMap.put("bruteForce", timeOutMap.getOrDefault("bruteForce", 0)+1);
			System.err.println("Timeout for bruteForce!");
		} finally {
			exceptionPerAlgorithm.putIfAbsent("bruteForce", exceptionBruteForce);
			futureBruteForce.cancel(true);
		}
		
		Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureLocalMin = service.submit(localMinApi);
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesLocalMin = null;
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
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			exceptionLocalMin = (ExecutionException)e;
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			exceptionLocalMin = (TimeoutException)e;
			timeOutMap.put("localMin", timeOutMap.getOrDefault("localMin", 0)+1);
			System.err.println("Timeout for localMin!");
		} finally {
			exceptionPerAlgorithm.putIfAbsent("localMin", exceptionLocalMin);
			futureLocalMin.cancel(true);
		}
		
		
		
		Future<HashMap<String, LinkedList<ProcessInstanceWithVoters>>> futureLocalMinWithMaxSolutions = service.submit(localMinWithMaxSolutionsApi);
		HashMap<String, LinkedList<ProcessInstanceWithVoters>> pInstancesLocalMinWithMaxSolutions = null;
		Exception exceptionLocalMinWithMaxSolutions = null;
		
		try {
			pInstancesLocalMinWithMaxSolutions= futureLocalMinWithMaxSolutions.get(timeOutForApiInMin, TimeUnit.MINUTES);
			algorithmMap.putAll(pInstancesLocalMinWithMaxSolutions);
			System.out.println("LocalMinInstances: "+pInstancesLocalMinWithMaxSolutions.get(localMinWithMaxSolutions).size());
			futureLocalMinWithMaxSolutions.cancel(true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			exceptionLocalMinWithMaxSolutions = (InterruptedException)e;
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			exceptionLocalMinWithMaxSolutions = (ExecutionException)e;
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			exceptionLocalMinWithMaxSolutions = (TimeoutException)e;
			timeOutMap.put(localMinWithMaxSolutions, timeOutMap.getOrDefault(localMinWithMaxSolutions, 0)+1);
			System.err.println("Timeout for "+localMinWithMaxSolutions
					+ "!");
		} finally {
			exceptionPerAlgorithm.putIfAbsent(localMinWithMaxSolutions, exceptionLocalMinWithMaxSolutions);
			futureLocalMinWithMaxSolutions.cancel(true);
		}
		
		
		
		
		
		boolean isCheapestSolutionInBruteForce = CommonFunctionality.compareResultsOfAlgorithmsForDifferentAPIs(pInstancesLocalMin.get("localMin"), pInstancesBruteForce.get("bruteForce"));
		writer.writeResultsOfBothAlgorithmsToCSVFile(bruteForceApi, localMinApi, algorithmMap, exceptionPerAlgorithm, isCheapestSolutionInBruteForce);
		
		
		return timeOutMap;
	}

	


	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels, int upperBoundSolutionsForLocalMin, ResultsToCSVWriter writer, ExecutorService service,
			int amountThreadPools) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithAnnotatedModels);
		if (!apiList.isEmpty()) {
			for(API api: apiList) {
				BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(api,upperBoundSolutionsForLocalMin, new HashMap<String, Integer>(), writer, service);
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

	public static void performTest3(LinkedList<File> randomProcesses, String pathToDestinationFolderForStoringModels,
			int amountUniqueDataObjectsPerDecision, int amountThreadPools) {
		// each decision has unique dataObject sets of same size
		// e.g. model has 4 decisions -> and each decision has 3 DataObjects -> 12
		// unique dataObjects need to be connected

		List<String> sphere = Arrays.asList("Global");
		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		for (int i = 0; i < randomProcesses.size(); i++) {
			String pathToRandomProcess = randomProcesses.get(i).getAbsolutePath();
			BpmnModelInstance processModel = Bpmn.readModelFromFile(randomProcesses.get(i));
			int amountTasks = processModel.getModelElementsByType(Task.class).size();
			int amountDecisions = CommonFunctionality.getAmountExclusiveGtwSplits(processModel);
			File newModel = null;
			boolean modelIsValid = false;
			int amountRandomCountDataObjectsToCreate = 0;
			while (modelIsValid == false) {
				try {
					ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess,
							pathToDestinationFolderForStoringModels, "");

					amountRandomCountDataObjectsToCreate = amountUniqueDataObjectsPerDecision * amountDecisions;

					pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, sphere);
					pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(amountUniqueDataObjectsPerDecision,
							amountUniqueDataObjectsPerDecision, 0, 0, 0, true);
					newModel = pModel.writeChangesToFileWithoutCorrectnessCheck();
					modelIsValid = true;
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}

			}
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

	public static void performTest2(LinkedList<File> randomProcesses, String pathToDestinationFolderForStoringModels,
			List<Integer> dataObjectBounds, int upperBoundSolutionsForLocalMinWithBound,int amountThreadPools) {

		LinkedList<String> emptySphere = new LinkedList<String>();
		emptySphere.add("");

		ExecutorService executor = Executors.newFixedThreadPool(amountThreadPools);

		for (int i = 0; i < randomProcesses.size(); i++) {
			String pathToRandomProcess = randomProcesses.get(i).getAbsolutePath();
			BpmnModelInstance processModel = Bpmn.readModelFromFile(randomProcesses.get(i));
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
		executor.shutdown();
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
		File file = CommonFunctionality.createFileWithinDirectory(pathToFolderWithModelsForEvaluation, "resultsOfTest2");
		ResultsToCSVWriter writer = new ResultsToCSVWriter(file);
		BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(pathToFolderWithModelsForEvaluation, upperBoundSolutionsForLocalMinWithBound, writer, service, amountThreadPools);

	}

}
