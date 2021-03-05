package functionality;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.opencsv.CSVWriter;

import Mapping.BPMNParticipant;
import Mapping.ProcessInstanceWithVoters;
import Mapping.VoterForXorArc;

public class ResultsToCSVWriter {

	private CSVWriter writer;
	private List<String[]> rows;

	public ResultsToCSVWriter(File csvFile) {
		try {
			this.writer = new CSVWriter(new FileWriter(csvFile), ';', CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

			rows = new ArrayList<>();
			String[] header = new String[] { "fileName", "pathToFile", "cheapest solution(s)", "cost",
					"executionTime" };
			this.rows.add(header);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
