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
			String[] header = new String[] { "fileName", "pathToFile", "exceptionLocalMin", "exceptionBruteForce", "totalAmountSolutions", "amount cheapest solution(s) localMinimumAlgorithm", "amount cheapest solution(s) bruteForce", "costCheapestSolution", "averageCostAllSolutions",
					"executionTimeLocalMinimumAlogrithm in sec", "executionTimeBruteForce in sec", "deltaExecutionTime in sec", "isCheapestSolutionInBruteForce", "amountPaths", "amountReadersPerDataObject", "amountWritersPerDataObject", "amountExclusiveGateways", "amountParallelGateways", "amountTasks", "amountElements", "amountSumVoters", "averageAmountVoters", "globalSphereSize", "averageSphereSum"};
			this.rows.add(header);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	
	
	public void writeResultsOfBothAlgorithmsToCSVFile(API api, HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap, HashMap<String,Exception> exceptionPerAlgorithm, boolean isCheapestSolutionInBruteForce) {
		//call this method after both algorithms ran
		// compare the execution of both algorithms
		// write the metadata to a csv file
		
		String pathToFile = "null";
		String fileName = "null";
		int indexBruteForce = -1;
		int indexLocalMin = -1;
		LinkedList<ProcessInstanceWithVoters>pInstancesLocalMin = null;
		LinkedList<ProcessInstanceWithVoters>pInstancesBruteForce = null;
		String localMinAlgorithmTime = "null";
		String bruteForceAlgorithmTime = "null";
		String timeDifference = "null";
		
		
				int index = 0; 
				for(Entry<String, LinkedList<ProcessInstanceWithVoters>>algorithmEntry: algorithmMap.entrySet()) {
					String algorithm = algorithmEntry.getKey();
					if(api!=null&&pathToFile.contentEquals("null")&&fileName.contentEquals("null")) {
						pathToFile = api.getProcessFile().getAbsolutePath();
						fileName = api.getProcessFile().getName();
					}
					if(algorithm.contentEquals("bruteForce")) {
						pInstancesBruteForce = algorithmEntry.getValue();
						indexBruteForce = index;
						bruteForceAlgorithmTime = api.getExecutionTimeBruteForceAlgorithm()+"";
					} else if(algorithm.contentEquals("localMin")) {
						pInstancesLocalMin = algorithmEntry.getValue();
						indexLocalMin = index;
						localMinAlgorithmTime = api.getExecutionTimeLocalMinimumAlgorithm()+"";
					}
					
					index++;
					
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
						
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getReadersForDataObjects(api.getModelInstance()).entrySet()) {
						readersPerDataObject.append(entr.getKey().getName()+": ");
						int amountReadersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountReadersPerDataObject++;
						}
						readersPerDataObject.append(amountReadersPerDataObject+",");
						
					}
					readersPerDataObject.deleteCharAt(readersPerDataObject.length()-1);
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getWritersForDataObjects(api.getModelInstance()).entrySet()) {
						writersPerDataObject.append(entr.getKey().getName()+": ");
						int amountWritersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountWritersPerDataObject++;
						}
						writersPerDataObject.append(amountWritersPerDataObject+",");
						
					}
					writersPerDataObject.deleteCharAt(writersPerDataObject.length()-1);
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
				
					String[] row = new String[] { fileName, pathToFile, exceptionNameLocalMin, exceptionNameBruteForce, amountSolutionsBruteForce, amountCheapestSolutionsLocalMin, amountCheapestSolutionsBruteForce, costCheapestSolution, averageCostAllSolutions,
							localMinAlgorithmTime, bruteForceAlgorithmTime, timeDifference, isCheapestSolutionInBruteForce+"", allPathsThroughProcess, readersPerDataObject.toString(), writersPerDataObject.toString(), amountExclusiveGtwSplits, amountParallelGtwSplits, amountTasks, amountElements,sumAmountVotersOfModel, averageAmountVotersOfModel, globalSphereSize, sphereSumOfModel};
					
					this.rows.add(row);

					
					
			}	
		
		
	
	
	

	public void writeResultsOfAlgorithmToCSVFile(API api, List<ProcessInstanceWithVoters> pInstances) {
		// Results of using either brute force or local minimum algorithm will be
		// written to a csv file
		// store e.g. the execution duration of the algorithm, amount of
		// writers/readers, the maximum depth of the process, amount of xor/parallel
		// splits etc.

			String pathToFile = api.getProcessFile().getAbsolutePath();
			String fileName = api.getProcessFile().getName();

			// map the cheapest process instances to rows in the csv file
			StringBuilder sb = new StringBuilder();
			double cost = 0;
			for(ProcessInstanceWithVoters pInst: pInstances) {
			cost = pInst.getCostForModelInstance();

			for (VoterForXorArc arc : pInst.getListOfArcs()) {
				sb.append(arc.getBrt().getName() + ": ");
				for (BPMNParticipant part : arc.getChosenCombinationOfParticipants()) {
					sb.append(part.getName() + ", ");
				}
			}
			}
			String[] row = new String[] { fileName, pathToFile, sb.toString(), cost + "",
					api.getExecutionTimeLocalMinimumAlgorithm() + "" };
			
			this.rows.add(row);

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
