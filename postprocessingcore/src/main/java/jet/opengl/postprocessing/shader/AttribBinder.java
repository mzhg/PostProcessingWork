package jet.opengl.postprocessing.shader;

public class AttribBinder {

	public String attributeName;
	public int index;
	
	public AttribBinder(String attributeName, int index) {
		this.attributeName = attributeName;
		this.index = index;
	}
	
	public AttribBinder() {}

	@Override
	public String toString() {
		return "[attributeName=" + attributeName + ", index=" + index + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeName == null) ? 0 : attributeName.hashCode());
		result = prime * result + index;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttribBinder other = (AttribBinder) obj;
		if (attributeName == null) {
			if (other.attributeName != null)
				return false;
		} else if (!attributeName.equals(other.attributeName))
			return false;
		if (index != other.index)
			return false;
		return true;
	}
	
}
