package vbe;

import java.io.Serializable;

public abstract class SystemElement implements ISystemElement, Serializable {
	private static final long serialVersionUID = -533883450461248454L;
	protected String name;
	
	public SystemElement(String name) {
		this.name = name;
	}
	
	public String getName() { return name; }
	public void setName(String newName) { name = newName; }

	public int getSize() {
		if (name != null)
			return name.length()*2;
		return 0; // null should be nothing in size
	}
}
