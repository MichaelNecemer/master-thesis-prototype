package functionality;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
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
	static long timeOutForProcessGeneratorInMin = 2; //1 min for generating random processes
	static int timeOutForApiInMin = 3; //3 min time limit for Api to finish all calculations
	static int timeOutForProcessModelAnnotaterInMin = 3;
	
	
	//bounds for ProcessModelGenerator
	static int probTask = 50;
	static int probXorGtw = 30;
	static int probParallelGtw = 20;
	static int probJoinGtw = 20;
	static int nestingDepthFactor = 5;
	
	
	//bounds for ProcessModelAnnotater
	static LinkedList<Integer> dataObjectBoundsSmallProcesses = new LinkedList<Integer>(Arrays.asList(1,3));
	static LinkedList<Integer> dataObjectBoundsMediumProcesses = new LinkedList<Integer>(Arrays.asList(2,5));
	static LinkedList<Integer> dataObjectBoundsLargeProcesses = new LinkedList<Integer>(Arrays.asList(1,5));

	static LinkedList<String> defaultSpheres = new LinkedList<String>(Arrays.asList("Global","Static","Weak-Dynamic","Strong-Dynamic"));
	static LinkedList<String> defaultNamesSeqFlowsXorSplits = new LinkedList<String>(Arrays.asList("true", "false"));
	static int dynamicWriterProb = 0; 
	static int readerProb = 30;
	static int writerProb = 20;
	static int probPublicSphere = 0;
	
	
	//bounds for "small", "medium", "large" amountOfWriters classes in percentage
	// e.g. 10 means, there will be 10% writers of the tasks in the process
	static LinkedList<Integer>percentageOfWritersClasses = new LinkedList<Integer>(Arrays.asList(10, 20,30));
	
	//bounds for "small", "medium", "large" amountOfReaders
	static LinkedList<Integer>percentageOfReadersClasses = new LinkedList<Integer>(Arrays.asList(10,20,30));
	
	//bounds for Algorithm that runs on annotated models
	static ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	static double costForAddingReaderAfterBrt = 0.0;
	
	public static void main(String[]args) throws Exception {
		
		//Test 1 - Boundary Test
		//The amount solutions generated will be the binomial coefficient for each decision multiplied 
		//e.g. 2 decisions - 1. needs 2 out of 4 voters = 6 possible combinations, 2. needs 3 out of 5 = 10 possible combinations -> 5*10 = 50 possible combinations of participants
		//so the boundary will be heavily influenced by the combination of voters per decision as well as the amount of decisions in the process 
		
		//the boundary test will set the max size which will be taken for further tests
		
		//generate 10 Processes - annotate them - try performing algorithms with a time limit
		//count amount of timeout
		//while there are timeouts -> reduce amount of decisions or global sphere
		String pathToFolderForModelsForBoundaryTest = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "BoundaryTest").getAbsolutePath();
		
		int tasksFactor = 3; //for each new xor there will be tasksFactor more tasks in the model
		int votersPerDecision = 3;
		int globalSphere = 5;
		int amountDecisions = 0; 
		int amountProcesses = 10;
		int amountTasks = 6;
		int lowerBoundAmountParallelGtws = 0;
		int upperBoundAmountParallelGtws = 0;
		int amountParallelGtws = 0; 
		int amountTimeOutsBruteForce = 0;
		int amountTimeOutsLocalMin = 0; 
		
		do {
			amountTimeOutsBruteForce=0;
			amountTimeOutsLocalMin = 0; 
			amountDecisions++;

			String folderName = "BoundaryTest_decision-"+amountDecisions;
			String pathToFolderForModelsWithDecisions = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForBoundaryTest, folderName).getAbsolutePath();
			
			int amountTasksWithFactor = amountTasks+(amountDecisions*tasksFactor); 
			//int amountParallelGtws = ThreadLocalRandom.current().nextInt(lowerBoundAmountParallelGtws, upperBoundAmountParallelGtws+1);
					
			//generate processes 
			BatchFileGenerator.generateRandomProcessesWithFixedValues(pathToFolderForModelsWithDecisions, amountProcesses, globalSphere, amountTasks, amountDecisions, amountParallelGtws);
			
			//annotate the processes
			File folder = new File(pathToFolderForModelsWithDecisions);
			LinkedList<File>listOfFiles = new LinkedList<File>();
			listOfFiles.addAll(Arrays.asList(folder.listFiles()));
			
			
			String pathToFolderForModelsWithDecisionsAnnotated = CommonFunctionality.fileWithDirectoryAssurance(folderName, "annotated").getAbsolutePath();
			Iterator<File>modelIter = listOfFiles.iterator();
			while(modelIter.hasNext()) {
				//annotate the model 
				File model = modelIter.next();
				ProcessModelAnnotater p = new ProcessModelAnnotater(model.getAbsolutePath(), pathToFolderForModelsWithDecisionsAnnotated, "_annotated");
				ExecutorService executor = Executors.newSingleThreadExecutor();
				
				Future future = executor.submit(p);
				try {
				future.get(timeOutForProcessModelAnnotaterInMin, TimeUnit.MINUTES);
				} catch (Exception e){
			        e.printStackTrace();
			        future.cancel(true); 
				}
				
			}
			
			//map annotated models
			LinkedList<API>apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderForModelsWithDecisionsAnnotated);
			
			
			amountProcesses = apiList.size();
			
			//perform both algorithms and count the timeouts
			for(API api: apiList) {
			boolean timeOutBruteForce = BatchFileGenerator.performBoundaryTest(api, "bruteForce", timeOutForApiInMin);
				if(timeOutBruteForce) {
					amountTimeOutsBruteForce++;
				}
			boolean timeOutLocalMin = BatchFileGenerator.performBoundaryTest(api, "localMin", timeOutForApiInMin);
			
				if(timeOutLocalMin) {
					amountTimeOutsLocalMin++;
				}
			}
			
			
			
		} while(amountTimeOutsBruteForce<amountProcesses||amountTimeOutsLocalMin<amountProcesses);	
		
			
		
		
		
		
		//generate 3 Classes -> small, medium, large processes (without annotation)
		//put them into a new folder into the root
		String pathToFolderForModelsWithoutAnnotation = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "ProcessesWithoutAnnotation").getAbsolutePath();
		
		String pathToSmallProcessesFolder = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "SmallProcessesFolder").getAbsolutePath();
		String pathToMediumProcessesFolder = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "MediumProcessesFolder").getAbsolutePath();
		String pathToLargeProcessesFolder = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsWithoutAnnotation, "LargeProcessesFolder").getAbsolutePath();
		
		//for each processes folder -> generate 100 random processes
		//small processes: 2-5 participants, 7-15 tasks, 1-4 xors, 0-2 parallels, 100 processes
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToSmallProcessesFolder, 2, 5, 7, 15, 1, 4, 0, 2, 100);

		//medium processes: 6-10 participants, 16-30 tasks, 5-10 xors, 0-6 parallels, 100 processes
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToMediumProcessesFolder, 6, 10, 16, 30, 5, 10, 0, 6, 100);
		BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToMediumProcessesFolder, 5, 7, 16, 30, 5, 6, 0, 6, 10);

		
		
		//large processes: 11-15 participants, 31-60 tasks, 11-15 xors, 0-9, parallels, 100 processes
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolder, 11, 15, 31, 60, 11, 15, 0, 9, 100);
		
		
		//each test will be based on the same randomly chosen models 
		LinkedList<File> randomSmallProcesses = BatchFileGenerator.getRandomModelsFromSourceFolder(20, pathToSmallProcessesFolder);
		LinkedList<File> randomMediumProcesses = BatchFileGenerator.getRandomModelsFromSourceFolder(5, pathToMediumProcessesFolder);
		//LinkedList<File> randomLargeProcesses = BatchFileGenerator.getRandomModelsFromSourceFolder(20, pathToLargeProcessesFolder);

		
		String pathToFolderForModelsForTest2 = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "Test2").getAbsolutePath();
		//Test 2 - Measure impact of enforceability on privity and vice versa
		//take x random models from small, medium and large processes
		//add dataObjects with global sphere and 1 voter per decision
		//add readers/writers combinations - generate new models (9 new models for each)
		
		// increase the amount of voters needed for decisions till the global sphere of that process is reached on all xors
		// increase privity requirements to next stricter sphere for all dataObjects
		
		String pathToSmallProcessesForTest2= CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "SmallProcessesAnnotatedFolder").getAbsolutePath();
		String pathToMediumProcessesForTest2 = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "MediumProcessesAnnotatedFolder").getAbsolutePath();
		String pathToLargeProcessesForTest2 = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "LargeProcessesAnnotatedFolder").getAbsolutePath();
	
		
		
		//BatchFileGenerator.performTest1(randomSmallProcesses, pathToSmallProcessesForTest2, dataObjectBoundsSmallProcesses );
		BatchFileGenerator.performTest2(randomMediumProcesses, pathToMediumProcessesForTest2, dataObjectBoundsMediumProcesses );

		//BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV("C:\\Users\\Micha\\OneDrive\\Desktop\\TestModels");
		//BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV("C:\\Users\\Micha\\OneDrive\\Desktop\\EvaluationSetup\\Test1\\MediumProcessesAnnotatedFolder\\ModelsForEvaluation");

		
		//Test 3 - Measure impact of dataObjects 
		// annotate different dataObject for each decision e.g. 5 decisions - 5 different dataObjects - each decision has 1 different dataObject connected
		// next step - choose one dataObject that will be annotated for a different decision 
		// this results in having 5 decisions where 1 of them has 2 dataObjects connected 
		// so 1 decision will be based on 2 dataObjects - 4 decisions with 1 different dataObject connected
		
		
	
		
	}
	
	
	/*
	public static void annotateModels(String rootFolder, String pathToFiles, String nameOfAnnotatedModelsFolder) {
		//iterate through all files in the directory and annotate them
		
		String newDirectory = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, nameOfAnnotatedModelsFolder).getAbsolutePath();
		
		File dir = new File(pathToFiles);
		  File[] directoryListing = dir.listFiles();
		
		  LinkedList<String>paths = new LinkedList<String>();
		  LinkedList<String>alreadyAnnotated = new LinkedList<String>();
		  if (directoryListing != null) {
		    for (File model : directoryListing) {
		    	if(model.getName().contains(".bpmn")) {
		    		if(!model.getName().contains("_annotated")) {
		  			
		    		paths.add(model.getAbsolutePath());
		    		} else if(model.getName().contains("_annotated")) {
		    			String name = model.getAbsolutePath();		    			
		    			String filter = name.substring(0, name.indexOf('_'));
		    			filter+=".bpmn";
		    			System.out.println("Already annotated: "+filter);
		    			alreadyAnnotated.add(filter);
		    		}
		    		
		    	}
		    	
		    }
		    
		    paths.removeAll(alreadyAnnotated);
		    
		    
		  } else {
			  System.out.println("No process models found in "+dir.getAbsolutePath());
			  
		  }
		  
		  Iterator<String>fileIter = paths.iterator();
		  
		  while(fileIter.hasNext()) {
			  String pathToFile = fileIter.next();
			  System.out.println(pathToFile);
			ProcessModelAnnotater.annotateModelWithReaderAndWriterProbabilities(pathToFile, newDirectory, dataObjectBounds, defaultSpheres, dynamicWriterProb, readerProb, writerProb, probPublicSphere, defaultNamesSeqFlowsXorSplits);
		    System.out.println(pathToFile +" was annotated");  
		  }
		  
	}*/
	
	public static void generateRandomProcessesWithFixedValues(String pathForAddingRandomModels, int maxAmountParticipants, int maxAmountMaxTasks, int maxAmountMaxXorSplits, int maxAmountMaxParallelSplits, int amountProcesses) {
		for(int i = 1; i <= amountProcesses; i++) {			
			
			//will be written to the pathForAddingRandomModels
			try {
				try {
					
					ProcessGenerator pGen = new ProcessGenerator(pathForAddingRandomModels, maxAmountParticipants, maxAmountMaxTasks, maxAmountMaxXorSplits, maxAmountMaxParallelSplits,dynamicWriterProb,readerProb,writerProb,probPublicSphere,nestingDepthFactor, true);

					ExecutorService executor = Executors.newSingleThreadExecutor();				
					Future future = executor.submit(pGen);
					try {
					future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
					} catch (Exception e){
				        e.printStackTrace();
				        future.cancel(true); 
					}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("randomProcess"+i+" deployed");
		}
		
	}
	
	
	public static void generateRandomProcessesWithinGivenRanges(String pathToFiles, int lowerBoundParticipants, int upperBoundParticipants, int lowerBoundTasks, int upperBoundTasks, int lowerBoundXorGtws, int upperBoundXorGtws, int lowerBoundParallelGtws, int upperBoundParallelGtws, int amountProcesses) {
		
		
		for(int i = 1; i <= amountProcesses; i++) {			
			
			int randomAmountParticipants = ThreadLocalRandom.current().nextInt(lowerBoundParticipants, upperBoundParticipants+1);
			int randomAmountMaxTasks = ThreadLocalRandom.current().nextInt(lowerBoundTasks, upperBoundTasks+1);
			int randomAmountMaxXorSplits = ThreadLocalRandom.current().nextInt(lowerBoundXorGtws, upperBoundXorGtws+1);
			int randomAmountMaxParallelSplits = ThreadLocalRandom.current().nextInt(lowerBoundParallelGtws, upperBoundParallelGtws+ 1);
			
			try {
				
			ProcessGenerator pGen = new ProcessGenerator(pathToFiles, randomAmountParticipants, randomAmountMaxTasks, randomAmountMaxXorSplits, randomAmountMaxParallelSplits, probTask,probXorGtw,probParallelGtw,probJoinGtw,nestingDepthFactor, true);
			ExecutorService executor = Executors.newSingleThreadExecutor();				
			Future future = executor.submit(pGen);
			try {
			future.get(timeOutForProcessGeneratorInMin, TimeUnit.MINUTES);
			} catch (Exception e){
		        e.printStackTrace();
		        future.cancel(true); 
			}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	System.out.println("randomProcess"+i+" deployed in "+pathToFiles);
		}
		
	}
	
	public static LinkedList<API> mapFilesToAPI(String pathToFiles) {
		//iterate through all files in the directory and run algorithms on annotated models
		LinkedList<API> apiList = new LinkedList<API>();
				File dir = new File(pathToFiles);
				  File[] directoryListing = dir.listFiles();
				
				  LinkedList<String>paths = new LinkedList<String>();
				  if (directoryListing != null) {
				    for (File model : directoryListing) {
				    	if(model.getName().contains(".bpmn")&&model.getName().contains("_annotated")) {
				    		paths.add(model.getAbsolutePath());
				    	}
				    }
				  } else {
					  System.out.println("No annotated process models found in "+dir.getAbsolutePath());
					  
				  }
				  
				  for(String pathToFile: paths) {
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
	

	private static void addInformationToCSVFile(LinkedList<API>apiList) {
		//write the results to a csv file
		
		String fileNameForResults = "resultsOfAlgorithm"+idCSVFile+".csv";
		File csvFile = new File(apiList.get(0).getProcessFile().getParent(), fileNameForResults);
		try {
			while(csvFile.createNewFile()==false) {
				System.out.println("CSV File"+fileNameForResults+" already exists!");
				idCSVFile++;
				fileNameForResults = "resultsOfAlgorithm"+idCSVFile+".csv";
				csvFile = new File(apiList.get(0).getProcessFile().getParent(), fileNameForResults);
				if(csvFile.createNewFile()) {
					System.out.println("CSV File"+fileNameForResults+" has been created at: "+csvFile.getAbsolutePath());
					break;
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		csvFile.getParentFile().mkdirs();
		ResultsToCSVWriter writer = new ResultsToCSVWriter(csvFile);
		for(API api: apiList) {
		
		try {
			LinkedList<ProcessInstanceWithVoters> localMin = api.localMinimumAlgorithm();
			LinkedList<ProcessInstanceWithVoters>bruteForce = api.bruteForceAlgorithm();
			writer.writeResultsOfBothAlgorithmsToCSVFile(api, localMin, bruteForce, api.compareResultsOfAlgorithms(localMin, bruteForce));
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		}
		writer.writeRowsToCSVAndcloseWriter();
		
	}
	
	public static void performTest2(int amountRandomProcessesToTakeFromFolder, String pathToSourceFolder, String pathToDestinationFolderForStoringModels, List<Integer>dataObjectBounds ) throws Exception {
		
		File folder = new File(pathToSourceFolder);
		LinkedList<File>listOfFiles = new LinkedList<File>();
		listOfFiles.addAll(Arrays.asList(folder.listFiles()));
		LinkedList<File>filesToBeChosenOf = new LinkedList<File>();
		filesToBeChosenOf.addAll(listOfFiles);			
		
		for(int i = 0; i < amountRandomProcessesToTakeFromFolder; i++) {
		//get a random model from the folder			
			File randomProcessModelFile = CommonFunctionality.getRandomItem(filesToBeChosenOf);
			BpmnModelInstance processModel = Bpmn.readModelFromFile(randomProcessModelFile);
			int amountTasks = processModel.getModelElementsByType(Task.class).size();	
			int lowerBoundAmountParticipants = 1;
			int upperBoundAmountParticipants = CommonFunctionality.getGlobalSphere(processModel, false);		
			File newModel = null;
			boolean modelIsValid = false;
			int amountRandomCountDataObjectsToCreate = 0;
			LinkedList<String>globalSphere = new LinkedList<String>();
			globalSphere.add("Global");
			while(modelIsValid==false) {
			ProcessModelAnnotater pModel = new ProcessModelAnnotater(randomProcessModelFile.getAbsolutePath(), pathToDestinationFolderForStoringModels, "");
			// randomly generate dataObjects in the range [DataObjects, maxCountDataObjects]
			amountRandomCountDataObjectsToCreate = ThreadLocalRandom.current().nextInt(dataObjectBounds.get(0),
					dataObjectBounds.get(1) + 1);
			try {
				//create model with dataObjects with global sphere annotated and random amount participants needed vor voting
				pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, globalSphere);			
				pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(amountRandomCountDataObjectsToCreate, lowerBoundAmountParticipants, upperBoundAmountParticipants, 0);
				//write this model to the folder
				newModel = pModel.writeChangesToFileWithoutCorrectnessCheck();
				modelIsValid=true;
			} catch(Exception e) {
				System.err.println(e.getMessage());
			}
			
				
			}		
			
			for(int writerClass = 0; writerClass < percentageOfWritersClasses.size(); writerClass++) {
				//for each model -> annotate it with small, medium, large amount of writers 
				BpmnModelInstance newModelInstance = Bpmn.readModelFromFile(newModel);
				int minAmountWriters = newModelInstance.getModelElementsByType(DataObjectReference.class).size();
				
				int amountWriterTasksInModel = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasks, percentageOfWritersClasses.get(writerClass), minAmountWriters);
			
				
				for(int readerClass = 0; readerClass < percentageOfReadersClasses.size(); readerClass++) {
					//for each model -> annotate it with small, medium, large amount of readers
					int amountReaderTasksInModel = CommonFunctionality.getAmountFromPercentage(amountTasks, percentageOfReadersClasses.get(readerClass));
					StringBuilder suffixBuilder = new StringBuilder();
					
					if(writerClass==0) {
						suffixBuilder.append("sW");
					} else if (writerClass==1) {
						suffixBuilder.append("mW");
					} else if(writerClass==2) {
						suffixBuilder.append("lW");
					}
					
					if(readerClass==0) {
						suffixBuilder.append("sR");
					} else if (readerClass==1) {
						suffixBuilder.append("mR");
					} else if(readerClass==2) {
						suffixBuilder.append("lR");
					}
					
					boolean correctModel = false;
					while(correctModel==false) {	
						ProcessModelAnnotater modelWithReadersAndWriters = new ProcessModelAnnotater(newModel.getAbsolutePath(), pathToDestinationFolderForStoringModels, suffixBuilder.toString(), globalSphere,
								0, amountWriterTasksInModel, amountReaderTasksInModel,0, 1, amountRandomCountDataObjectsToCreate, defaultNamesSeqFlowsXorSplits);
						
						try {							
						modelWithReadersAndWriters.checkCorrectnessAndWriteChangesToFile();
						correctModel=true;
						} catch(Exception e) {
							System.err.println(e.getMessage());
							
							//try annotating model again with same values
						}			
					}
					
					
				
					
				}
			}
			
			filesToBeChosenOf.remove(randomProcessModelFile);
		}
		
		
		//for each model that has been generated with random amount participants needed per decision
		// -> generate new ones where spheres of dataObjects are increased to the next stricter one till Strong-Dynamic is reached
		
		String pathToFolderWithSpheres = CommonFunctionality.fileWithDirectoryAssurance(pathToDestinationFolderForStoringModels, "Spheres").getAbsolutePath();
		File directory = new File(pathToDestinationFolderForStoringModels);
		File[]directoryList = directory.listFiles();
		for(int i = 0; i < directoryList.length; i++) {	
			File annotatedModel = directoryList[i];
			try {
			BpmnModelInstance currModel = Bpmn.readModelFromFile(annotatedModel);

			
			if(annotatedModel.getName().contains("sWsR")||annotatedModel.getName().contains("sWmR")||annotatedModel.getName().contains("sWlR")||annotatedModel.getName().contains("mWsR")||annotatedModel.getName().contains("mWmR")||annotatedModel.getName().contains("mWlR")||annotatedModel.getName().contains("lWsR")||annotatedModel.getName().contains("lWmR")||annotatedModel.getName().contains("lWlR")) {
			for(int j = 0; j < defaultSpheres.size(); j++) {
				String currentSphere = defaultSpheres.get(j);
				String suffix = currentSphere;
				CommonFunctionality.increaseSpherePerDataObject(annotatedModel, pathToFolderWithSpheres, suffix, currentSphere);
			}
			
			}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}

		//perform the algorithms and generate csv file 
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithSpheres);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
		
		
	
		
	}
	
	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels) {
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithAnnotatedModels);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
			
	}
	
	public static LinkedList<File> getRandomModelsFromSourceFolder(int amountRandomModels, String pathToSourceFolder){
		File folder = new File(pathToSourceFolder);
		LinkedList<File>listOfFiles = new LinkedList<File>();
		listOfFiles.addAll(Arrays.asList(folder.listFiles()));
	
		LinkedList<File> randomModelsFromSourceFolder = new LinkedList<File>();
		
		for(int i = 0; i < amountRandomModels; i++) {	
		File randomProcessModelFile = CommonFunctionality.getRandomItem(listOfFiles);
		randomModelsFromSourceFolder.add(randomProcessModelFile);
		listOfFiles.remove(randomProcessModelFile);
		}
		
		return randomModelsFromSourceFolder;
	}
	
	
	
	public static void performTest2(LinkedList<File>randomProcesses, String pathToDestinationFolderForStoringModels, List<Integer>dataObjectBounds) throws Exception {
			
			LinkedList<String>emptySphere = new LinkedList<String>();
			emptySphere.add("");
			
			for(int i = 0; i < randomProcesses.size(); i++) {
			String pathToRandomProcess = randomProcesses.get(i).getAbsolutePath();
			BpmnModelInstance processModel = Bpmn.readModelFromFile(randomProcesses.get(i));
			int amountTasks = processModel.getModelElementsByType(Task.class).size();			
			File newModel = null;
			boolean modelIsValid = false;
			int amountRandomCountDataObjectsToCreate = 0;
			while(modelIsValid==false) {
			ProcessModelAnnotater pModel = new ProcessModelAnnotater(pathToRandomProcess, pathToDestinationFolderForStoringModels, "");
					
			// randomly generate dataObjects in the range [DataObjects, maxCountDataObjects]
			amountRandomCountDataObjectsToCreate = ThreadLocalRandom.current().nextInt(dataObjectBounds.get(0),
					dataObjectBounds.get(1) + 1);
			try {
				//create model with amountRandomCountDataObjects dataObjects, 0 participant needed for voting and empty string as starting sphere
				pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, emptySphere);			
				pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(amountRandomCountDataObjectsToCreate, 0, 0, 0);
				newModel = pModel.writeChangesToFileWithoutCorrectnessCheck();
				modelIsValid=true;
			} catch(Exception e) {
				System.err.println(e.getMessage());
			}
			
				
			}		
			
			for(int writerClass = 0; writerClass < percentageOfWritersClasses.size(); writerClass++) {
				//for each model -> annotate it with small, medium, large amount of writers 
				BpmnModelInstance newModelInstance = Bpmn.readModelFromFile(newModel);
				int minAmountWriters = newModelInstance.getModelElementsByType(DataObjectReference.class).size();
				
				int amountWriterTasksInModel = CommonFunctionality.getAmountFromPercentageWithMinimum(amountTasks, percentageOfWritersClasses.get(writerClass), minAmountWriters);
			
				
				for(int readerClass = 0; readerClass < percentageOfReadersClasses.size(); readerClass++) {
					//for each model -> annotate it with small, medium, large amount of readers
					int amountReaderTasksInModel = CommonFunctionality.getAmountFromPercentage(amountTasks, percentageOfReadersClasses.get(readerClass));
					StringBuilder suffixBuilder = new StringBuilder();
					
					if(writerClass==0) {
						suffixBuilder.append("sW");
					} else if (writerClass==1) {
						suffixBuilder.append("mW");
					} else if(writerClass==2) {
						suffixBuilder.append("lW");
					}
					
					if(readerClass==0) {
						suffixBuilder.append("sR");
					} else if (readerClass==1) {
						suffixBuilder.append("mR");
					} else if(readerClass==2) {
						suffixBuilder.append("lR");
					}
					
					boolean correctModel = false;
					ExecutorService executor = Executors.newSingleThreadExecutor();
					while(correctModel==false) {	
						ProcessModelAnnotater modelWithReadersAndWriters = new ProcessModelAnnotater(newModel.getAbsolutePath(), pathToDestinationFolderForStoringModels, suffixBuilder.toString(), emptySphere,
								0, amountWriterTasksInModel, amountReaderTasksInModel,0, 1, amountRandomCountDataObjectsToCreate, defaultNamesSeqFlowsXorSplits);
						
						Future future = executor.submit(modelWithReadersAndWriters);
						future.get(20, TimeUnit.SECONDS);
						try {			
						
						//modelWithReadersAndWriters.checkCorrectnessAndWriteChangesToFile();
											
						correctModel=true;
						
						} catch(Exception e) {
							
							e.printStackTrace();
							System.err.println(e.getMessage());
							future.cancel(true);
							executor.shutdown();

							
							//try annotating model again with same values
						}	
						
					}
								
					
				}
			}
			
		}
			
			
		
		
		//for each model that has been generated with 0 participants per decision and empty sphere annotated for each dataObject
		// -> generate new ones where voters is increased by 1 on each model till globalSphereSize is reached
		// -> generate new ones where privity requirement is increased to next stricter sphere till Strong-Dynamic is reached on all dataObjects
			
		String pathToFolderWithModelsForEvaluation = CommonFunctionality.fileWithDirectoryAssurance(pathToDestinationFolderForStoringModels, "ModelsForEvaluation").getAbsolutePath();
		File directory = new File(pathToDestinationFolderForStoringModels);
		File[]directoryList = directory.listFiles();
		
		
		for(int modelIndex = 0; modelIndex < directoryList.length; modelIndex++) {	
			File annotatedModel = directoryList[modelIndex];
			try {
			BpmnModelInstance currModel = Bpmn.readModelFromFile(annotatedModel);
			
			int globalSphereSize = CommonFunctionality.getGlobalSphere(currModel,false);
						
			if(annotatedModel.getName().contains("sWsR")||annotatedModel.getName().contains("sWmR")||annotatedModel.getName().contains("sWlR")||annotatedModel.getName().contains("mWsR")||annotatedModel.getName().contains("mWmR")||annotatedModel.getName().contains("mWlR")||annotatedModel.getName().contains("lWsR")||annotatedModel.getName().contains("lWmR")||annotatedModel.getName().contains("lWlR")) {
						
				for(int spheresIndex = 0; spheresIndex < defaultSpheres.size(); spheresIndex++) {
					String currentSphere = defaultSpheres.get(spheresIndex);
					CommonFunctionality.increaseSpherePerDataObject(currModel, currentSphere);
				
					for(int votersAmountIndex= 1; votersAmountIndex <= globalSphereSize; votersAmountIndex++) {
					String suffix = "_"+currentSphere + "_voters"+votersAmountIndex;
					CommonFunctionality.increaseVotersPerDataObject(currModel, votersAmountIndex);
					String modelName = annotatedModel.getName().substring(0, annotatedModel.getName().indexOf(".bpmn"));
					CommonFunctionality.writeChangesToFile(currModel, modelName, pathToFolderWithModelsForEvaluation, suffix);
					
					}
					
				
				}
				
								
			
			}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}

		//perform the algorithms and generate csv file 
		LinkedList<API> apiList = BatchFileGenerator.mapFilesToAPI(pathToFolderWithModelsForEvaluation);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
		
		
	}
	
	
	public static boolean performBoundaryTest(API api, String algorithmToPerform, int timeOutInMinutes) {
		//return true if there was a timeout performing the algorithm
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		
		if(algorithmToPerform.contentEquals("localMin")) {
			try {
				api.setAlgorithmToPerform("localMin");
				Future future = executor.submit(api);
				future.get(timeOutInMinutes, TimeUnit.MINUTES);
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(algorithmToPerform.contentEquals("bruteForce")) {
			try {
				api.setAlgorithmToPerform("bruteForce");
				Future future = executor.submit(api);
				future.get(timeOutInMinutes, TimeUnit.MINUTES);	
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return false;
	}
	

}
