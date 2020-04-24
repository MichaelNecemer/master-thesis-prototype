package functionality;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JCheckBox;

import Mapping.BPMNBusinessRuleTask;
import Mapping.BPMNDataObject;
import Mapping.BPMNExclusiveGateway;
import Mapping.BPMNTask;

public class JCheckBoxWithId extends JCheckBox {
	
	public BPMNTask lastWriter;
	public HashMap<BPMNBusinessRuleTask,HashMap<BPMNDataObject, ArrayList<BPMNTask>>> map;
	public BPMNTask oneTaskOutOfMap;
	public BPMNExclusiveGateway bpmnEx;
	public BPMNDataObject dataObject;
	public String sphere;
	
	
	public JCheckBoxWithId(String text, BPMNTask lastWriter, HashMap<BPMNBusinessRuleTask, HashMap<BPMNDataObject, ArrayList<BPMNTask>>> map, BPMNTask oneTaskOutOfMap, BPMNExclusiveGateway bpmnEx, BPMNDataObject dataObject, String sphere ) {
		super(text);
		this.lastWriter=lastWriter;
		this.map=map;
		this.oneTaskOutOfMap=oneTaskOutOfMap;
		this.bpmnEx=bpmnEx;
		this.dataObject=dataObject;
		this.sphere=sphere;
	}
	

}
