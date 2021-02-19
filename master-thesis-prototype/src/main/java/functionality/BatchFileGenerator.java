package functionality;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

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
	static LinkedList<String> defaultSpheres = new LinkedList<String>(Arrays.asList("Global","Static","Weak-Dynamic","Strong-Dynamic"));
	static int sphereProb = 50;
	static int readerProb = 30;
	static int writerProb = 20;
	
	public static void main(String[]args) {
		
		
		
		
		//generate random process models
		//BatchFileGenerator.generateRandomProcesses(10);
		
		//annotate models
		BatchFileGenerator.annotateModels();
	}
	
	public static void annotateModels() {
		//iterate through all files in the directory and annotate them
		File dir = new File(pathForAddingRandomModels);
		  File[] directoryListing = dir.listFiles();
		
		  
		  if (directoryListing != null) {
		    for (File model : directoryListing) {
		    	if(model.getName().contains(".bpmn")) {
		    	ProcessModellAnnotater.annotateModel(model.getAbsolutePath(), dataObjectBounds, defaultSpheres, sphereProb, readerProb, writerProb);
		    	System.out.println(model.getName()+" was annotated");  
		    	}
		    }
		  } else {
			  System.out.println("No process models found in "+dir.getAbsolutePath());
			  
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

}
