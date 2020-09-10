package functionality;

import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JCheckBox;

import Mapping.BPMNDataObject;
import Mapping.BPMNElement;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNTask;

public class JCheckBoxPossiblePath extends JCheckBox{
	
	public LinkedList<LinkedList<BPMNElement>>pathsBetweenLastWriterAndReader;
	public BPMNElement lastWriter;
	public BPMNElement reader;
	public BPMNExclusiveGateway xorGtw;
	public API api;
	public LinkedList<JCheckBox> pathList;
	public LinkedList<String> pathsAsString;
	public BPMNDataObject dataO; 
	
	public JCheckBoxPossiblePath(BPMNElement lastWriter, BPMNDataObject dataO, BPMNElement reader, BPMNExclusiveGateway xorGtw, API api) {
		this.lastWriter=lastWriter;
		this.reader=reader;
		this.api=api;
		this.xorGtw = xorGtw;
		this.dataO = dataO; 
		this.pathList = new LinkedList<JCheckBox>();
		this.pathsAsString=new LinkedList<String>();
		pathsBetweenLastWriterAndReader = api.allPathsBetweenNodes(lastWriter, reader, new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<BPMNElement>(), new LinkedList<LinkedList<BPMNElement>>());
		this.createPathList();
	}
	
	 
	
	
	public void createPathList(){
		
		for(int i = 0; i < pathsBetweenLastWriterAndReader.size(); i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			JCheckBox box = new JCheckBox();
			
			for(int j = 0; j < pathsBetweenLastWriterAndReader.get(i).size(); j++) {
				if(pathsBetweenLastWriterAndReader.get(i).get(j) instanceof BPMNTask ) {
				sb.append(((BPMNTask)pathsBetweenLastWriterAndReader.get(i).get(j)).getName()+", ");
			}
			}
			sb.delete(sb.length()-2, sb.length());
			sb.append(")");
			box.setText(sb.toString());
			this.pathList.add(box);
			this.pathsAsString.add(sb.toString());
		}
	
	}
	
	
	

}
