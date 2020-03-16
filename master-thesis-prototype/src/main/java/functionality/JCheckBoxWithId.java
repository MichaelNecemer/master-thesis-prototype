package functionality;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JCheckBox;

import Mapping.BPMNDataObject;
import Mapping.BPMNTask;

public class JCheckBoxWithId extends JCheckBox {
	
	public BPMNTask lastWriter;
	public HashMap<BPMNDataObject, ArrayList<BPMNTask>> map;
	public BPMNTask oneTaskOutOfMap;
	
	public JCheckBoxWithId(String text, BPMNTask lastWriter, HashMap<BPMNDataObject, ArrayList<BPMNTask>> map, BPMNTask oneTaskOutOfMap  ) {
		super(text);
		this.lastWriter=lastWriter;
		this.map=map;
		this.oneTaskOutOfMap=oneTaskOutOfMap;
	}
	

}
