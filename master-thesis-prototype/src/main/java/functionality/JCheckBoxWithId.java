package functionality;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JCheckBox;

import Mapping.BPMNDataObject;
import Mapping.BPMNTask;

public class JCheckBoxWithId extends JCheckBox {
	
	public BPMNTask t;
	public HashMap<BPMNDataObject, ArrayList<BPMNTask>> map;
	public BPMNTask t2;
	
	public JCheckBoxWithId(String text, BPMNTask t, HashMap<BPMNDataObject, ArrayList<BPMNTask>> map, BPMNTask t2  ) {
		super(text);
		this.t=t;
		this.map=map;
		this.t2=t2;
	}
	

}
