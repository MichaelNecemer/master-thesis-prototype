package functionality;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Task;

import Mapping.ProcessInstanceWithVoters;
import ProcessModelGeneratorAndAnnotater.ProcessGenerator;
import ProcessModelGeneratorAndAnnotater.ProcessModelAnnotater;

public class BatchFileGenerator {
	
	static String rootFolder = "C:\\Users\\Micha\\OneDrive\\Desktop\\EvaluationSetup";
	static long timeOutForProcessGenerator = 60000; //1 min for generating random processes
	static long timeOutForApi = 180000; //3 min time limit for Api to finish all calculations

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
		
		//large processes: 11-15 participants, 31-60 tasks, 11-15 xors, 0-9, parallels, 100 processes
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(pathToLargeProcessesFolder, 11, 15, 31, 60, 11, 15, 0, 9, 100);
		
		String pathToFolderForModelsForTest1 = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "Test1").getAbsolutePath();
		//Test 1 - Measure impact of enforceability on privity
		// increase the amount of voters needed for decisions till the max amount voters is reached on all xors
		// for n models of a class create m new ones 
		
		
		
		String pathToSmallProcessesForTest1= CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest1, "SmallProcessesAnnotatedFolder").getAbsolutePath();
		String pathToMediumProcessesForTest1 = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest1, "MediumProcessesAnnotatedFolder").getAbsolutePath();
		String pathToLargeProcessesForTest1 = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest1, "LargeProcessesAnnotatedFolder").getAbsolutePath();
	
		
		//run through the small processes without annotation and annotate them -> increase the amount of voters for each xor on every run
		//on first run there should be 0 voter on each xor split - on last run, there should be the amount of participants in the global sphere 
		//generate models with 1 participant per decision
		
		
		//BatchFileGenerator.performTest1(10, pathToSmallProcessesFolder, pathToSmallProcessesForTest1, dataObjectBoundsSmallProcesses );
		//BatchFileGenerator.performTest1(10, pathToMediumProcessesFolder, pathToMediumProcessesForTest1, dataObjectBoundsMediumProcesses);
		//BatchFileGenerator.performTest1(1, pathToLargeProcessesFolder, pathToLargeProcessesForTest1, dataObjectBoundsLargeProcesses);

		/*
		String path = pathToSmallProcessesForTest1+"//Voter";
		LinkedList<API> apiList = BatchFileGenerator.runAlgorithmsOnAnnotatedProcesses(path);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
		*/
		
		//Test 2 - Measure impact of privity on enforceability
		//increase the sphere of dataObjects to the next stricter one till "Strong-Dynamic" is reached on all dataObjects
		String pathToFolderForModelsForTest2 = CommonFunctionality.fileWithDirectoryAssurance(rootFolder, "Test2").getAbsolutePath();

		String pathToSmallProcessesForTest2= CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "SmallProcessesAnnotatedFolder").getAbsolutePath();
		String pathToMediumProcessesForTest2 = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "MediumProcessesAnnotatedFolder").getAbsolutePath();
		String pathToLargeProcessesForTest2 = CommonFunctionality.fileWithDirectoryAssurance(pathToFolderForModelsForTest2, "LargeProcessesAnnotatedFolder").getAbsolutePath();
	
		
		BatchFileGenerator.performTest2(10, pathToSmallProcessesFolder, pathToSmallProcessesForTest2, dataObjectBoundsSmallProcesses );
				//BatchFileGenerator.performTest1(10, pathToMediumProcessesFolder, pathToMediumProcessesForTest1, dataObjectBoundsMediumProcesses);
				//BatchFileGenerator.performTest1(1, pathToLargeProcessesFolder, pathToLargeProcessesForTest1, dataObjectBoundsLargeProcesses);

		//String path = pathToSmallProcessesForTest2+"//Spheres";
		//BatchFileGenerator.runAlgorithmsAndWriteResultsToCSV(path);
		
				
		//annotate models
		//BatchFileGenerator.annotateModels(rootFolder, pathToSmallProcessesFolder, "smallProcessesAnnotated");
		
		
		
		
		
		
		
		//Test 3 - Measure impact of dataobjects 
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	
		
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
	
	public static void generateRandomProcessesWithFixedValues(String pathForAddingRandomModels, int amountProcesses, int maxAmountParticipants, int maxAmountMaxTasks, int maxAmountMaxXorSplits, int maxAmountMaxParallelSplits) {
		for(int i = 1; i <= amountProcesses; i++) {			
			
			//will be written to the pathForAddingRandomModels
			try {
				
				new ProcessGenerator(pathForAddingRandomModels, timeOutForProcessGenerator, maxAmountParticipants, maxAmountMaxTasks, maxAmountMaxXorSplits, maxAmountMaxParallelSplits,dynamicWriterProb,readerProb,writerProb,probPublicSphere,nestingDepthFactor, true);
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
			
			new ProcessGenerator(pathToFiles,timeOutForProcessGenerator, randomAmountParticipants, randomAmountMaxTasks, randomAmountMaxXorSplits, randomAmountMaxParallelSplits, probTask,probXorGtw,probParallelGtw,probJoinGtw,nestingDepthFactor, true);
	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	System.out.println("randomProcess"+i+" deployed in "+pathToFiles);
		}
		
	}
	
	public static LinkedList<API> runAlgorithmsOnAnnotatedProcesses(String pathToFiles) {
		//iterate through all files in the directory and annotate them
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
		int i = 1; 
		String fileNameForResults = "resultsOfAlgorithm"+i+".csv";
		File csvFile = new File(apiList.get(0).getProcessFile().getParent(), fileNameForResults);
		try {
			while(csvFile.createNewFile()==false) {
				System.out.println("CSV File"+fileNameForResults+" already exists!");
				i++;
				fileNameForResults = "resultsOfAlgorithm"+i+".csv";
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
		LinkedList<API> apiList = BatchFileGenerator.runAlgorithmsOnAnnotatedProcesses(pathToFolderWithSpheres);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
		
		
	
		
	}
	
	public static void runAlgorithmsAndWriteResultsToCSV(String pathToFolderWithAnnotatedModels) {
		LinkedList<API> apiList = BatchFileGenerator.runAlgorithmsOnAnnotatedProcesses(pathToFolderWithAnnotatedModels);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
			
	}
	
	
	
	public static void performTest1(int amountRandomProcessesToTakeFromFolder, String pathToSourceFolder, String pathToDestinationFolderForStoringModels, List<Integer>dataObjectBounds ) throws Exception {
		
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
			File newModel = null;
			boolean modelIsValid = false;
			int amountRandomCountDataObjectsToCreate = 0;
			while(modelIsValid==false) {
			ProcessModelAnnotater pModel = new ProcessModelAnnotater(randomProcessModelFile.getAbsolutePath(), pathToDestinationFolderForStoringModels, "");
					
			// randomly generate dataObjects in the range [DataObjects, maxCountDataObjects]
			amountRandomCountDataObjectsToCreate = ThreadLocalRandom.current().nextInt(dataObjectBounds.get(0),
					dataObjectBounds.get(1) + 1);
			try {
				//create model with dataObjects and exactly 0 participant needed for voting
				pModel.generateDataObjects(amountRandomCountDataObjectsToCreate, defaultSpheres);			
				pModel.connectDataObjectsToBrtsAndTuplesForXorSplits(amountRandomCountDataObjectsToCreate, 0, 0, 0);
				//write this model (only the dataObjects annotated and connected to the brts so far) to the folder
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
						ProcessModelAnnotater modelWithReadersAndWriters = new ProcessModelAnnotater(newModel.getAbsolutePath(), pathToDestinationFolderForStoringModels, suffixBuilder.toString(), defaultSpheres,
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
		
		
		//for each model that has been generated with 0 participants per decision
		// -> generate new ones where voters is increased by 1 on each model till globalSphereSize is reached
		
		String pathToFolderWithVoters = CommonFunctionality.fileWithDirectoryAssurance(pathToDestinationFolderForStoringModels, "Voter").getAbsolutePath();
		File directory = new File(pathToDestinationFolderForStoringModels);
		File[]directoryList = directory.listFiles();
		for(int i = 0; i < directoryList.length; i++) {	
			File annotatedModel = directoryList[i];
			try {
			BpmnModelInstance currModel = Bpmn.readModelFromFile(annotatedModel);
			int globalSphereSize = CommonFunctionality.getGlobalSphere(currModel,false);
			
			if(annotatedModel.getName().contains("sWsR")||annotatedModel.getName().contains("sWmR")||annotatedModel.getName().contains("sWlR")||annotatedModel.getName().contains("mWsR")||annotatedModel.getName().contains("mWmR")||annotatedModel.getName().contains("mWlR")||annotatedModel.getName().contains("lWsR")||annotatedModel.getName().contains("lWmR")||annotatedModel.getName().contains("lWlR")) {
			for(int j = 1; j <= globalSphereSize; j++) {
				String suffix = "voters"+j;
				CommonFunctionality.increaseVotersPerDataObject(annotatedModel, pathToFolderWithVoters, suffix, j);
				}
			
			}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}

		//perform the algorithms and generate csv file 
		LinkedList<API> apiList = BatchFileGenerator.runAlgorithmsOnAnnotatedProcesses(pathToFolderWithVoters);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
		
		
	}
	
	

}
