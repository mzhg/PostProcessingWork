package assimp.common;

public class ObjectHolder<T> {

	T value;
	
	public ObjectHolder() {
	}

	public ObjectHolder(T value) {
		this.value = value;
	}
	
	public void reset(){
		value = null;
	}
	
	public void reset(T newValue){
		value = newValue;
	}
	
	public void set(T value){
		this.value = value;
	}
	
	public T get() { return value;}
	
	public boolean notNull() { return value != null;}
}
