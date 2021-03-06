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

import Mapping.BPMNBusinessRuleTask;
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
			String[] header = new String[] { "fileName", "pathToFile", "exceptionLocalMin", "exceptionBruteForce", "exceptionLocalMinWithLimit", "totalAmountSolutionsWithoutConstraints", "solution(s) bruteForce", "solution(s) localMinimumAlgorithm", "solution(s) localMinimumAlgorithmWithLimit", "amount cheapest solution(s) bruteForce", "costCheapestSolutionBruteForce", "costCheapestSolutionLocalMin", "costCheapestSolutionLocalMinWithLimit", "averageCostAllBruteForceSolutions",
					"executionTimeLocalMinimumAlogrithm in sec", "executionTimeLocalMinWithLimit in sec","executionTimeBruteForce in sec",  "deltaExecutionTime in sec (localMin - bruteForce)", "deltaExecutionTime in sec (localMinWithLimit - bruteForce)", "isCheapestSolutionOfLocalMinInBruteForce", "isCheapestSolutionOfLocalMinWithLimitInBruteForce", "amountPaths", "amountReadersPerDataObject", "amountWritersPerDataObject", "amountExclusiveGateways", "amountParallelGateways", "amountTasks", "amountElements", "amountSumVoters", "averageAmountVoters", "globalSphereSize", "averageSphereSum", "dependentBrts"};
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

	
	
	
	
	
	public void writeResultsOfAlgorithmsToCSVFile(API bruteForceApi, API localMinApi, API localMinWithLimitApi, HashMap<String, LinkedList<ProcessInstanceWithVoters>> algorithmMap, HashMap<String,Exception> exceptionPerAlgorithm, String isCheapestSolutionInBruteForce, String isCheapestSolutionWithLimitInBruteForce) {
		//call this method after algorithms ran
		// compare the execution of algorithms
		// write the metadata to a csv file
		
		String pathToFile = bruteForceApi.getProcessFile().getAbsolutePath();
		String fileName = bruteForceApi.getProcessFile().getName();
		
		LinkedList<ProcessInstanceWithVoters>pInstancesLocalMin = null;
		LinkedList<ProcessInstanceWithVoters>pInstancesBruteForce = null;
		LinkedList<ProcessInstanceWithVoters>pInstancesLocalMinWithLimit = null;
		String localMinAlgorithmTime = "null";
		String bruteForceAlgorithmTime = "null";
		String localMinWithLimitAlgorithmTime = "null";
		String timeDifferenceLocalMinBruteForce = "null";
		String timeDifferenceLocalMinWithLimitBruteForce = "null";
		
		if(algorithmMap.get(localMinApi.getAlgorithmToPerform())!=null) {
			pInstancesLocalMin = algorithmMap.get(localMinApi.getAlgorithmToPerform());
			localMinAlgorithmTime = localMinApi.getExecutionTimeLocalMinimumAlgorithm()+"";
		}
				
		if(algorithmMap.get(bruteForceApi.getAlgorithmToPerform())!=null) {
			pInstancesBruteForce = algorithmMap.get(bruteForceApi.getAlgorithmToPerform());
			bruteForceAlgorithmTime = bruteForceApi.getExecutionTimeBruteForceAlgorithm()+"";
		}
		
		if(algorithmMap.get(localMinWithLimitApi.getAlgorithmToPerform())!=null) {
			pInstancesLocalMinWithLimit = algorithmMap.get(localMinWithLimitApi.getAlgorithmToPerform());
			localMinWithLimitAlgorithmTime = localMinWithLimitApi.getExecutionTimeLocalMinimumAlgorithmWithLimit()+"";
		}
		
		
			
			if((!localMinAlgorithmTime.contentEquals("null"))&&(!bruteForceAlgorithmTime.contentEquals("null"))) {
				timeDifferenceLocalMinBruteForce = (Double.parseDouble(localMinAlgorithmTime)-Double.parseDouble(bruteForceAlgorithmTime))+"";
			}
			
			if((!localMinWithLimitAlgorithmTime.contentEquals("null"))&&(!bruteForceAlgorithmTime.contentEquals("null"))) {
				timeDifferenceLocalMinWithLimitBruteForce = (Double.parseDouble(localMinWithLimitAlgorithmTime)-Double.parseDouble(bruteForceAlgorithmTime))+"";
			}


					// map the cheapest process instances to rows in the csv file
					
					String amountCheapestSolutionsLocalMin = "null";
					String costCheapestSolutionLocalMin = "null";
					if(pInstancesLocalMin!=null&&!pInstancesLocalMin.isEmpty()) {
					costCheapestSolutionLocalMin =pInstancesLocalMin.get(0).getCostForModelInstance()+"";					
					amountCheapestSolutionsLocalMin = pInstancesLocalMin.size()+"";
					}
					
					String amountCheapestSolutionsBruteForce = "null";
					String averageCostAllSolutions="null";
					String amountSolutionsBruteForce = "null";
					String costCheapestSolutionBruteForce = "null";
					if(pInstancesBruteForce!=null&&!pInstancesBruteForce.isEmpty()) {
					LinkedList<ProcessInstanceWithVoters>cheapestBruteForceInst = 	CommonFunctionality.getCheapestProcessInstancesWithVoters(pInstancesBruteForce);
					amountCheapestSolutionsBruteForce = cheapestBruteForceInst.size()+"";
					amountSolutionsBruteForce = pInstancesBruteForce.size()+"";
					averageCostAllSolutions = CommonFunctionality.getAverageCostForAllModelInstances(pInstancesBruteForce)+"";
					
						costCheapestSolutionBruteForce =cheapestBruteForceInst.get(0).getCostForModelInstance()+"";					

					
					
					}
					
					String amountCheapestSolutionsLocalMinWithLimit = "null";
					String costCheapestSolutionLocalMinWithLimit = "null";
					if(pInstancesLocalMinWithLimit!=null&&!pInstancesLocalMinWithLimit.isEmpty()) {
						costCheapestSolutionLocalMinWithLimit =pInstancesLocalMinWithLimit.get(0).getCostForModelInstance()+"";					
						amountCheapestSolutionsLocalMinWithLimit = pInstancesLocalMinWithLimit.size()+"";
						
					}
					
					
					
					
					
					StringBuilder readersPerDataObject = new StringBuilder();
					StringBuilder writersPerDataObject = new StringBuilder();
					StringBuilder dependentBrts = new StringBuilder();
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
					
					
					boolean dependentBrtsInProcess = false;
					for(BPMNBusinessRuleTask brt: bruteForceApi.getBusinessRuleTasks()) {
						if(!brt.getPotentiallyDependentBrts().isEmpty()) {
							dependentBrtsInProcess=true;
							break;
						}
					}
					
					dependentBrts.append(dependentBrtsInProcess+"");
					
					
					} 
					
					String exceptionNameLocalMin = "null";
					if(exceptionPerAlgorithm.get("localMin") != null) {
						exceptionNameLocalMin = exceptionPerAlgorithm.get("localMin").getClass().getCanonicalName();
					} 
					String exceptionNameBruteForce = "null";
					if(exceptionPerAlgorithm.get("bruteForce")!=null) {
						exceptionNameBruteForce = exceptionPerAlgorithm.get("bruteForce").getClass().getCanonicalName();
					}
					
					String exceptionNameLocalMinWithLimit = "null";
					if(exceptionPerAlgorithm.get(localMinWithLimitApi.getAlgorithmToPerform()) != null) {
						exceptionNameLocalMinWithLimit = exceptionPerAlgorithm.get(localMinWithLimitApi.getAlgorithmToPerform()).getClass().getCanonicalName();
					} 
					
					
					String[] row = new String[] { fileName, pathToFile, exceptionNameLocalMin, exceptionNameBruteForce, exceptionNameLocalMinWithLimit, amountSolutions, amountSolutionsBruteForce,  amountCheapestSolutionsLocalMin, amountCheapestSolutionsLocalMinWithLimit, amountCheapestSolutionsBruteForce, costCheapestSolutionBruteForce, costCheapestSolutionLocalMin, costCheapestSolutionLocalMinWithLimit, averageCostAllSolutions,
							localMinAlgorithmTime, localMinWithLimitAlgorithmTime, bruteForceAlgorithmTime, timeDifferenceLocalMinBruteForce, timeDifferenceLocalMinWithLimitBruteForce,isCheapestSolutionInBruteForce,isCheapestSolutionWithLimitInBruteForce, allPathsThroughProcess, readersPerDataObject.toString(), writersPerDataObject.toString(), amountExclusiveGtwSplits, amountParallelGtwSplits, amountTasks, amountElements,sumAmountVotersOfModel, averageAmountVotersOfModel, globalSphereSize, sphereSumOfModel, dependentBrts.toString()};
					
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
