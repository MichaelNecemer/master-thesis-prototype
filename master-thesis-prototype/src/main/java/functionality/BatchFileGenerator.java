package functionality;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	static String pathForAddingRandomModels = "C:\\Users\\Micha\\OneDrive\\Desktop\\randomProcessModels";
	
	//bounds for the random numbers	for ProcessGenerator
	static int randomAmountParticipantsUpperBound = 10;
	static int randomAmountMaxTasksUpperBound = 30;
	static int randomAmountMaxXorSplitsUpperBound = 10;
	static int randomAmountMaxParallelSplitsUpperBound = 10;
	
	//bounds for ProcessModellAnnotater
	static LinkedList<Integer> dataObjectBounds = new LinkedList<Integer>(Arrays.asList(1,2));
	static LinkedList<String> defaultSpheres = new LinkedList<String>(Arrays.asList("Public","Global","Static","Weak-Dynamic","Strong-Dynamic"));
	static int sphereProb = 50;
	static int readerProb = 30;
	static int writerProb = 20;
	static int probPublicSphere = 30;
	
	//bounds for Algorithm that runs on annotated models
	static ArrayList<Double> cost = new ArrayList<>(Arrays.asList(10.0, 5.0, 3.0, 2.0));
	static double costForAddingReaderAfterBrt = 1.0;
	
	public static void main(String[]args) {
		
		
		
		
		//generate random process models
		//BatchFileGenerator.generateRandomProcesses(10);
		
		//annotate models
		//BatchFileGenerator.annotateModels();
		
		//perform the algorithms and generate csv file
		LinkedList<API> apiList = BatchFileGenerator.runAlgorithmsOnAnnotatedProcesses();
		if(!apiList.isEmpty()) {
		BatchFileGenerator.addInformationToCSVFile(apiList);
		} else {
			System.out.println("No Algorithms have been run successfully on annotated models");
		}
		
	}
	
	public static void annotateModels() {
		//iterate through all files in the directory and annotate them
		File dir = new File(pathForAddingRandomModels);
		  File[] directoryListing = dir.listFiles();
		
		  LinkedList<String>paths = new LinkedList<String>();
		  if (directoryListing != null) {
		    for (File model : directoryListing) {
		    	if(model.getName().contains(".bpmn")&&!model.getName().contains("_annotated")) {
		    		paths.add(model.getAbsolutePath());
		    	}
		    }
		  } else {
			  System.out.println("No process models found in "+dir.getAbsolutePath());
			  
		  }
		  
		  for(String pathToFile: paths) {
			  ProcessModellAnnotater.annotateModel(pathToFile, dataObjectBounds, defaultSpheres, sphereProb, readerProb, writerProb, probPublicSphere);
		    	System.out.println(pathToFile +" was annotated");  
		  }
		  
	}
	
	
	
	public static void generateRandomProcesses(int amountProcesses) {
		
		for(int i = 1; i <= amountProcesses; i++) {			
			
			int randomAmountParticipants = ThreadLocalRandom.current().nextInt(1, randomAmountParticipantsUpperBound+1);
			int randomAmountMaxTasks = ThreadLocalRandom.current().nextInt(1, randomAmountMaxTasksUpperBound+1);
			int randomAmountMaxXorSplits = ThreadLocalRandom.current().nextInt(1, randomAmountMaxXorSplitsUpperBound+1);
			int randomAmountMaxParallelSplits = ThreadLocalRandom.current().nextInt(1, randomAmountMaxParallelSplitsUpperBound+ 1);
			
			//will be written to the pathForAddingRandomModels
			new ProcessGenerator(pathForAddingRandomModels, randomAmountParticipants, randomAmountMaxTasks, randomAmountMaxXorSplits, randomAmountMaxParallelSplits,50,30,20,20,10);
			System.out.println("randomProcess"+i+" deployed");
		}
		
	}
	
	public static LinkedList<API> runAlgorithmsOnAnnotatedProcesses() {
		//iterate through all files in the directory and annotate them
		LinkedList<API> apiList = new LinkedList<API>();
				File dir = new File(pathForAddingRandomModels);
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
