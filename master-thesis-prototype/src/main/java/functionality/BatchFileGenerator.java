package functionality;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

import Mapping.ProcessInstanceWithVoters;
import ProcessModelGeneratorAndAnnotater.ProcessGenerator;
import ProcessModelGeneratorAndAnnotater.ProcessModellAnnotater;

public class BatchFileGenerator {
	
	//generate random Processes
	//annotate the processes
	//Run the Algorithm -> generate possible solutions for the annotated processes
	//export the data as CSV file
	
	//path for adding the random models
	//static String pathToFiles = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModels";
	static String pathToFiles = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessesMax15Gtws";

	
	//bounds for the random numbers	for ProcessGenerator
	static int randomAmountParticipantsUpperBound = 6;
	static int randomAmountMaxTasksUpperBound = 20;
	static int randomAmountMaxXorSplitsUpperBound = 5;
	static int randomAmountMaxParallelSplitsUpperBound = 10;
	
	//bounds for ProcessModellAnnotater
	static LinkedList<Integer> dataObjectBounds = new LinkedList<Integer>(Arrays.asList(1,3));
	static LinkedList<String> defaultSpheres = new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic"));
	static int sphereProb = 50;
	static int readerProb = 30;
	static int writerProb = 20;
	static int probPublicSphere = 30;
	
	//bounds for Algorithm that runs on annotated models
	static ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	static double costForAddingReaderAfterBrt = 1.0;
	
	public static void main(String[]args) {
				
		//generate random process models within the specified ranges
		//e.g. max 20 tasks -> process that will be generated will have between 1 and 20 tasks
		//BatchFileGenerator.generateRandomProcessesWithinGivenRanges(10);
		
		//generate random process models using parameter values 
		//(NOTE: the values are maxValues -> if maxAmountXorSplits == 5 -> the ProcessGenerator will try to build a process with 5 xor-splits, but will not have 5 in all instances e.g. when there are no more tasks left to be inserte, there will not be a xor-split generated anymore!)
		int maxParticipants = 5;
		int maxAmountTasks = 9;
		int maxAmountXorSplits = 3;
		int maxAmountParallelSplits = 2;
		int amountProcessesToGenerate = 10;
		//BatchFileGenerator.generateRandomProcessesWithFixedValues(pathToFiles, amountProcessesToGenerate, maxParticipants, maxAmountTasks, maxAmountXorSplits, maxAmountParallelSplits);
		
		//annotate models
		BatchFileGenerator.annotateModels(pathToFiles);
		
		/*
		//perform the algorithms and generate csv file
		LinkedList<API> apiList = BatchFileGenerator.runAlgorithmsOnAnnotatedProcesses(pathToFiles);
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
		*/
	}
	
	public static void annotateModels(String pathToFiles) {
		//iterate through all files in the directory and annotate them
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
			ProcessModellAnnotater.annotateModel(pathToFile, dataObjectBounds, defaultSpheres, sphereProb, readerProb, writerProb, probPublicSphere);
		    System.out.println(pathToFile +" was annotated");  
		  }
		  
	}
	
	public static void generateRandomProcessesWithFixedValues(String pathForAddingRandomModels, int amountProcesses, int maxAmountParticipants, int maxAmountMaxTasks, int maxAmountMaxXorSplits, int maxAmountMaxParallelSplits) {
		for(int i = 1; i <= amountProcesses; i++) {			
			
			//will be written to the pathForAddingRandomModels
			new ProcessGenerator(pathForAddingRandomModels, maxAmountParticipants, maxAmountMaxTasks, maxAmountMaxXorSplits, maxAmountMaxParallelSplits,50,30,20,20,10);
			System.out.println("randomProcess"+i+" deployed");
		}
		
	}
	
	
	public static void generateRandomProcessesWithinGivenRanges(String pathToFiles, int amountProcesses) {
		
		for(int i = 1; i <= amountProcesses; i++) {			
			
			int randomAmountParticipants = ThreadLocalRandom.current().nextInt(1, randomAmountParticipantsUpperBound+1);
			int randomAmountMaxTasks = ThreadLocalRandom.current().nextInt(1, randomAmountMaxTasksUpperBound+1);
			int randomAmountMaxXorSplits = ThreadLocalRandom.current().nextInt(1, randomAmountMaxXorSplitsUpperBound+1);
			int randomAmountMaxParallelSplits = ThreadLocalRandom.current().nextInt(1, randomAmountMaxParallelSplitsUpperBound+ 1);
			
			//will be written to the pathForAddingRandomModels
			new ProcessGenerator(pathToFiles, randomAmountParticipants, randomAmountMaxTasks, randomAmountMaxXorSplits, randomAmountMaxParallelSplits,50,30,20,20,10);
			System.out.println("randomProcess"+i+" deployed");
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
		LinkedList<ProcessInstanceWithVoters>localMin = api.localMinimumAlgorithm();
		LinkedList<ProcessInstanceWithVoters>bruteForce = api.bruteForceAlgorithm();
		writer.writeResultsOfBothAlgorithmsToCSVFile(api, api.localMinimumAlgorithm(), api.compareResultsOfAlgorithms(localMin, bruteForce));
		}
		writer.writeRowsToCSVAndcloseWriter();
		
		
		
		
	}

}
