package functionality;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

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
			String[] header = new String[] { "fileName", "pathToFile", "totalAmountSolutions", "amount cheapest solution(s) localMinimumAlgorithm", "amount cheapest solution(s) bruteForce", "costCheapestSolution", "averageCostAllSolutions",
					"executionTimeLocalMinimumAlogrithm in sec", "executionTimeBruteForce in sec", "deltaExecutionTime in sec", "isCheapestSolutionInBruteForce", "amountPaths", "amountReadersPerDataObject", "amountWritersPerDataObject", "amountExclusiveGateways", "amountParallelGateways", "amountTasks", "amountElements", "amountSumVoters", "averageAmountVoters", "globalSphereSize", "averageSphereSum"};
			this.rows.add(header);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public void writeResultsOfBothAlgorithmsToCSVFile(API api, LinkedList<ProcessInstanceWithVoters> pInstancesLocalMin, LinkedList<ProcessInstanceWithVoters> pInstancesBruteForce, boolean isCheapestSolutionInBruteForce) {
		//call this method after both algorithms ran
		// compare the execution of both algorithms
		// write the metadata to a csv file
				
					String pathToFile = api.getProcessFile().getAbsolutePath();
					String fileName = api.getProcessFile().getName();

					// map the cheapest process instances to rows in the csv file
					double costCheapestSolution =pInstancesLocalMin.get(0).getCostForModelInstance();					
					int amountCheapestSolutionsLocalMin = pInstancesLocalMin.size();
					int amountCheapestSolutionsBruteForce = api.getCheapestProcessInstancesWithVoters(pInstancesBruteForce).size();
					
					double averageCostAllSolutions = CommonFunctionality.getAverageCostForAllModelInstances(pInstancesBruteForce);
					
					
					
					StringBuilder readersPerDataObject = new StringBuilder();
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getReadersForDataObjects(api.getModelInstance()).entrySet()) {
						readersPerDataObject.append(entr.getKey().getName()+": ");
						int amountReadersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountReadersPerDataObject++;
						}
						readersPerDataObject.append(amountReadersPerDataObject+",");
						
					}
					readersPerDataObject.deleteCharAt(readersPerDataObject.length()-1);
					StringBuilder writersPerDataObject = new StringBuilder();
					for(Entry<DataObjectReference, LinkedList<FlowNode>> entr: CommonFunctionality.getWritersForDataObjects(api.getModelInstance()).entrySet()) {
						writersPerDataObject.append(entr.getKey().getName()+": ");
						int amountWritersPerDataObject = 0; 
						for(FlowNode f: entr.getValue()) {
							amountWritersPerDataObject++;
						}
						writersPerDataObject.append(amountWritersPerDataObject+",");
						
					}
					writersPerDataObject.deleteCharAt(writersPerDataObject.length()-1);
					
					
					String[] row = new String[] { fileName, pathToFile, pInstancesBruteForce.size()+"", amountCheapestSolutionsLocalMin+"", amountCheapestSolutionsBruteForce+"", costCheapestSolution + "", averageCostAllSolutions+"",
							api.getExecutionTimeLocalMinimumAlgorithm() + "", api.getExecutionTimeBruteForceAlgorithm()+"", api.getExecutionTimeLocalMinimumAlgorithm()-api.getExecutionTimeBruteForceAlgorithm()+"", isCheapestSolutionInBruteForce+"", api.getAllPathsThroughProcess().size()+"", readersPerDataObject.toString(), writersPerDataObject.toString(), CommonFunctionality.getAmountExclusiveGtwSplits(api.getModelInstance())+"", CommonFunctionality.getAmountParallelGtwSplits(api.getModelInstance())+"", CommonFunctionality.getAmountTasks(api.getModelInstance())+"",CommonFunctionality.getAmountElements(api.getModelInstance())+"", CommonFunctionality.getSumAmountVotersOfModel(api.getModelInstance())+"", CommonFunctionality.getAverageAmountVotersOfModel(api.getModelInstance())+"", CommonFunctionality.getGlobalSphere(api.getModelInstance(), api.modelWithLanes())+"", CommonFunctionality.getSphereSumOfModel(api.getModelInstance())+""};
					
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
