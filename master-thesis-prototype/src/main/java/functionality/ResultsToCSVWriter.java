package functionality;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import com.opencsv.CSVWriter;

import Mapping.BPMNParticipant;
import Mapping.ProcessInstanceWithVoters;
import Mapping.VoterForXorArc;

public class ResultsToCSVWriter {

	private CSVWriter writer;
	private List<String[]> rows;

	public ResultsToCSVWriter(File csvFile) {
		try {
			this.writer = new CSVWriter(new FileWriter(csvFile, true), ';', CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

			rows = new ArrayList<>();
			String[] header = new String[] { "fileName", "pathToFile", "exceptionLocalMin", "exceptionBruteForce", "totalAmountSolutions", "solution(s) bruteForce", "solution(s) localMinimumAlgorithm", "amount cheapest solution(s) bruteForce", "costCheapestSolution", "averageCostAllSolutions",
					"executionTimeLocalMinimumAlogrithm in sec", "executionTimeBruteForce in sec", "deltaExecutionTime in sec", "isCheapestSolutionInBruteForce", "amountPaths", "amountReadersPerDataObject", "amountWritersPerDataObject", "amountExclusiveGateways", "amountParallelGateways", "amountTasks", "amountElements", "amountSumVoters", "averageAmountVoters", "globalSphereSize", "averageSphereSum"};
			this.rows.add(header);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	public void writeResultsOfOneAlgorithmToCSVFile(API api, HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap, HashMap<String,Exception> exceptionPerAlgorithm) {
		String pathToFile = "null";
		String fileName = "null";
		LinkedList<ProcessInstanceWithVoters>pInstancesLocalMin = null;
		LinkedList<ProcessInstanceWithVoters>pInstancesBruteForce = null;
		String localMinAlgorithmTime = "null";
		String bruteForceAlgorithmTime = "null";
		String timeDifference = "null";
		
		if(api!=null&&pathToFile.contentEquals("null")&&fileName.contentEquals("null")) {
			pathToFile = api.getProcessFile().getAbsolutePath();
			fileName = api.getProcessFile().getName();
		}
		
				for(Entry<String, LinkedList<ProcessInstanceWithVoters>>algorithmEntry: algorithmMap.entrySet()) {
					String algorithm = algorithmEntry.getKey();					
					if(algorithm.contentEquals("bruteForce")) {
						pInstancesBruteForce = algorithmEntry.getValue();
						bruteForceAlgorithmTime = api.getExecutionTimeBruteForceAlgorithm()+"";
					} else if(algorithm.contentEquals("localMin")) {
						pInstancesLocalMin = algorithmEntry.getValue();
						localMinAlgorithmTime = api.getExecutionTimeLocalMinimumAlgorithm()+"";
					}
				
					
				}
							
			
			if((!localMinAlgorithmTime.contentEquals("null"))&&(!bruteForceAlgorithmTime.contentEquals("null"))) {
				timeDifference = (Double.parseDouble(localMinAlgorithmTime)-Double.parseDouble(bruteForceAlgorithmTime))+"";
			}

					// map the cheapest process instances to rows in the csv file
					
					String amountCheapestSolutionsLocalMin = "null";
					String costCheapestSolution = "null";
					if(pInstancesLocalMin!=null&&!pInstancesLocalMin.isEmpty()) {
					costCheapestSolution =pInstancesLocalMin.get(0).getCostForModelInstance()+"";					
					amountCheapestSolutionsLocalMin = pInstancesLocalMin.size()+"";
					}
					
					String amountCheapestSolutionsBruteForce = "null";
					String averageCostAllSolutions="null";
					String amountSolutionsBruteForce = "null";
					if(pInstancesBruteForce!=null&&!pInstancesBruteForce.isEmpty()) {
					LinkedList<ProcessInstanceWithVoters>cheapestBruteForceInst = 	CommonFunctionality.getCheapestProcessInstancesWithVoters(pInstancesBruteForce);
					amountCheapestSolutionsBruteForce = cheapestBruteForceInst.size()+"";
					amountSolutionsBruteForce = pInstancesBruteForce.size()+"";
					averageCostAllSolutions = CommonFunctionality.getAverageCostForAllModelInstances(pInstancesBruteForce)+"";
					if(costCheapestSolution.contentEquals("null")) {
						costCheapestSolution =cheapestBruteForceInst.get(0).getCostForModelInstance()+"";					

					}
					
					}
					
					
					StringBuilder readersPerDataObject = new StringBuilder();
					StringBuilder writersPerDataObject = new StringBuilder();
					String amountSolutions = "null";
					String allPathsThroughProcess = "null";
					String amountExclusiveGtwSplits = "null";
					String amountParallelGtwSplits = "null";
					String amountTasks = "null";
					String amountElements = "null";
					String sumAmountVotersOfModel ="null";
					String averageAmountVotersOfModel = "null";
					String globalSphereSize = "null";
					String sphereSumOfModel = "null";
					
			
					
					if(api!=null) {
						 allPathsThroughProcess = api.getAllPathsThroughProcess().size()+"";
						amountExclusiveGtwSplits = CommonFunctionality.getAmountExclusiveGtwSplits(api.getModelInstance())+"";
						amountParallelGtwSplits = CommonFunctionality.getAmountParallelGtwSplits(api.getModelInstance())+"";
						amountTasks = CommonFunctionality.getAmountTasks(api.getModelInstance())+"";
						amountElements = CommonFunctionality.getAmountElements(api.getModelInstance())+"";
						sumAmountVotersOfModel =CommonFunctionality.getSumAmountVotersOfModel(api.getModelInstance())+"";
						averageAmountVotersOfModel = CommonFunctionality.getAverageAmountVotersOfModel(api.getModelInstance())+"";
						globalSphereSize =  CommonFunctionality.getGlobalSphere(api.getModelInstance(), api.modelWithLanes())+"";
						sphereSumOfModel =  CommonFunctionality.getSphereSumOfModel(api.getModelInstance())+"";		
						amountSolutions = CommonFunctionality.calculatePossibleCombinationsForProcess(api.getModelInstance(), false)+"";
						
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getReadersForDataObjects(api.getModelInstance()).entrySet()) {
						readersPerDataObject.append(entr.getKey().getName()+": ");
						int amountReadersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountReadersPerDataObject++;
						}
						readersPerDataObject.append(amountReadersPerDataObject+",");
						
					}
					if(readersPerDataObject.length()!=0) {
					readersPerDataObject.deleteCharAt(readersPerDataObject.length()-1);
					}
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getWritersForDataObjects(api.getModelInstance()).entrySet()) {
						writersPerDataObject.append(entr.getKey().getName()+": ");
						int amountWritersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountWritersPerDataObject++;
						}
						writersPerDataObject.append(amountWritersPerDataObject+",");
						
					}
					if(writersPerDataObject.length()!=0) {
					writersPerDataObject.deleteCharAt(writersPerDataObject.length()-1);
					}
					} else {
						readersPerDataObject.append("null");
						writersPerDataObject.append("null");
					}
					
					String exceptionNameLocalMin = "null";
					if(exceptionPerAlgorithm.get("localMin") != null) {
						exceptionNameLocalMin = exceptionPerAlgorithm.get("localMin").getClass().getCanonicalName();
					} 
					String exceptionNameBruteForce = "null";
					if(exceptionPerAlgorithm.get("bruteForce")!=null) {
						exceptionNameBruteForce = exceptionPerAlgorithm.get("bruteForce").getClass().getCanonicalName();
					}
				
					String[] row = new String[] { fileName, pathToFile, exceptionNameLocalMin, exceptionNameBruteForce, amountSolutions, amountSolutionsBruteForce, amountCheapestSolutionsLocalMin, amountCheapestSolutionsBruteForce, costCheapestSolution, averageCostAllSolutions,
							localMinAlgorithmTime, bruteForceAlgorithmTime, timeDifference, "null", allPathsThroughProcess, readersPerDataObject.toString(), writersPerDataObject.toString(), amountExclusiveGtwSplits, amountParallelGtwSplits, amountTasks, amountElements,sumAmountVotersOfModel, averageAmountVotersOfModel, globalSphereSize, sphereSumOfModel};
					
					this.rows.add(row);

					
					
			}	

	
	
	
	
	
	public void writeResultsOfBothAlgorithmsToCSVFile(API bruteForceApi, API localMinApi, HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap, HashMap<String,Exception> exceptionPerAlgorithm, boolean isCheapestSolutionInBruteForce) {
		//call this method after algorithms ran
		// compare the execution of algorithms
		// write the metadata to a csv file
		
		String pathToFile = bruteForceApi.getProcessFile().getAbsolutePath();
		String fileName = bruteForceApi.getProcessFile().getName();
		
		LinkedList<ProcessInstanceWithVoters>pInstancesLocalMin = null;
		LinkedList<ProcessInstanceWithVoters>pInstancesBruteForce = null;
		String localMinAlgorithmTime = "null";
		String bruteForceAlgorithmTime = "null";
		String timeDifference = "null";
		
		if(algorithmMap.get("localMin")!=null&&localMinApi!=null) {
			pInstancesLocalMin = algorithmMap.get("localMin");
			localMinAlgorithmTime = localMinApi.getExecutionTimeLocalMinimumAlgorithm()+"";
		}
				
		if(algorithmMap.get("bruteForce")!=null) {
			pInstancesBruteForce = algorithmMap.get("bruteForce");
			bruteForceAlgorithmTime = bruteForceApi.getExecutionTimeBruteForceAlgorithm()+"";
		}
				
			
			
			if(!(localMinAlgorithmTime.contentEquals("null")&&bruteForceAlgorithmTime.contentEquals("null"))) {
				timeDifference = (Double.parseDouble(localMinAlgorithmTime)-Double.parseDouble(bruteForceAlgorithmTime))+"";
			}

					// map the cheapest process instances to rows in the csv file
					
					String amountCheapestSolutionsLocalMin = "null";
					String costCheapestSolution = "null";
					if(pInstancesLocalMin!=null&&!pInstancesLocalMin.isEmpty()) {
					costCheapestSolution =pInstancesLocalMin.get(0).getCostForModelInstance()+"";					
					amountCheapestSolutionsLocalMin = pInstancesLocalMin.size()+"";
					}
					
					String amountCheapestSolutionsBruteForce = "null";
					String averageCostAllSolutions="null";
					String amountSolutionsBruteForce = "null";
					if(pInstancesBruteForce!=null&&!pInstancesBruteForce.isEmpty()) {
					LinkedList<ProcessInstanceWithVoters>cheapestBruteForceInst = 	CommonFunctionality.getCheapestProcessInstancesWithVoters(pInstancesBruteForce);
					amountCheapestSolutionsBruteForce = cheapestBruteForceInst.size()+"";
					amountSolutionsBruteForce = pInstancesBruteForce.size()+"";
					averageCostAllSolutions = CommonFunctionality.getAverageCostForAllModelInstances(pInstancesBruteForce)+"";
					if(costCheapestSolution.contentEquals("null")) {
						costCheapestSolution =cheapestBruteForceInst.get(0).getCostForModelInstance()+"";					

					}
					
					}
					
					
					StringBuilder readersPerDataObject = new StringBuilder();
					StringBuilder writersPerDataObject = new StringBuilder();
					String allPathsThroughProcess = "null";
					String amountExclusiveGtwSplits = "null";
					String amountParallelGtwSplits = "null";
					String amountTasks = "null";
					String amountElements = "null";
					String sumAmountVotersOfModel ="null";
					String averageAmountVotersOfModel = "null";
					String globalSphereSize = "null";
					String sphereSumOfModel = "null";
					String amountSolutions = "null";
					
			
					
					if(bruteForceApi!=null) {
						 allPathsThroughProcess = bruteForceApi.getAllPathsThroughProcess().size()+"";
						amountExclusiveGtwSplits = CommonFunctionality.getAmountExclusiveGtwSplits(bruteForceApi.getModelInstance())+"";
						amountParallelGtwSplits = CommonFunctionality.getAmountParallelGtwSplits(bruteForceApi.getModelInstance())+"";
						amountTasks = CommonFunctionality.getAmountTasks(bruteForceApi.getModelInstance())+"";
						amountElements = CommonFunctionality.getAmountElements(bruteForceApi.getModelInstance())+"";
						sumAmountVotersOfModel =CommonFunctionality.getSumAmountVotersOfModel(bruteForceApi.getModelInstance())+"";
						averageAmountVotersOfModel = CommonFunctionality.getAverageAmountVotersOfModel(bruteForceApi.getModelInstance())+"";
						globalSphereSize =  CommonFunctionality.getGlobalSphere(bruteForceApi.getModelInstance(), bruteForceApi.modelWithLanes())+"";
						sphereSumOfModel =  CommonFunctionality.getSphereSumOfModel(bruteForceApi.getModelInstance())+"";		
						amountSolutions = CommonFunctionality.calculatePossibleCombinationsForProcess(bruteForceApi.getModelInstance(), false)+"";
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getReadersForDataObjects(bruteForceApi.getModelInstance()).entrySet()) {
						readersPerDataObject.append(entr.getKey().getName()+": ");
						int amountReadersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountReadersPerDataObject++;
						}
						readersPerDataObject.append(amountReadersPerDataObject+",");
						
					}
					if(readersPerDataObject.length()>0) {
					readersPerDataObject.deleteCharAt(readersPerDataObject.length()-1);
					}
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getWritersForDataObjects(bruteForceApi.getModelInstance()).entrySet()) {
						writersPerDataObject.append(entr.getKey().getName()+": ");
						int amountWritersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountWritersPerDataObject++;
						}
						writersPerDataObject.append(amountWritersPerDataObject+",");
						
					}
					if(writersPerDataObject.length()>0) {
					writersPerDataObject.deleteCharAt(writersPerDataObject.length()-1);
					}
					} 
					
					String exceptionNameLocalMin = "null";
					if(exceptionPerAlgorithm.get("localMin") != null) {
						exceptionNameLocalMin = exceptionPerAlgorithm.get("localMin").getClass().getCanonicalName();
					} 
					String exceptionNameBruteForce = "null";
					if(exceptionPerAlgorithm.get("bruteForce")!=null) {
						exceptionNameBruteForce = exceptionPerAlgorithm.get("bruteForce").getClass().getCanonicalName();
					}
				
					String[] row = new String[] { fileName, pathToFile, exceptionNameLocalMin, exceptionNameBruteForce, amountSolutions, amountSolutionsBruteForce, amountCheapestSolutionsLocalMin, amountCheapestSolutionsBruteForce, costCheapestSolution, averageCostAllSolutions,
							localMinAlgorithmTime, bruteForceAlgorithmTime, timeDifference, isCheapestSolutionInBruteForce+"", allPathsThroughProcess, readersPerDataObject.toString(), writersPerDataObject.toString(), amountExclusiveGtwSplits, amountParallelGtwSplits, amountTasks, amountElements,sumAmountVotersOfModel, averageAmountVotersOfModel, globalSphereSize, sphereSumOfModel};
					
					this.rows.add(row);	
					
			}	
		
public void addEmptyRow() {
	String[]emptyRow = new String[this.rows.get(0).length];
	for(int i = 0; i < this.rows.get(0).length; i++) {
		emptyRow[i] = "";
	}
	this.rows.add(emptyRow);
	
}
	
	
	

	
	
public void writeRowsToCSVAndcloseWriter() {
	try {
		this.writer.writeAll(this.rows);
		this.writer.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

}
