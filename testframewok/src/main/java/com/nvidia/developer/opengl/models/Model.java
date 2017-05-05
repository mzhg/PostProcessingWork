package com.nvidia.developer.opengl.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jet.opengl.postprocessing.common.GLenum;

/** A model represent a attributes' assembly. */
public class Model {
	
	public static final int TYPE_VERTEX = 0;
	public static final int TYPE_NORMAL = 1;
	public static final int TYPE_COLOR = 2;
	public static final int TYPE_TEXTURE0 = 3;
	
	final List<AttribArray> attribs = new ArrayList<AttribArray>();
	final List<AttribInfo>  attribInfos = new ArrayList<AttribInfo>();
	final List<GLVAO>  vaos = new ArrayList<GLVAO>();
	AttribArray element;
	final boolean dymatic;  // dymatic data.
	private int size; // the number of the attributes.
	private DrawMode mode;
	boolean sperate_buffer;
	
	public Model() {
		this(false);
	}
	

	Model(boolean dymatic) {
		this.dymatic = dymatic;
	}

	void setDrawMode(DrawMode mode){
		this.mode = mode;
	}
	
	public DrawMode getDrawMode() { return mode;}
	
	public int size() { return size;}
	
	public void enableSperateBuffer(){ sperate_buffer = true;}
	public void disableSperateBuffer(){ sperate_buffer = false;}
	public boolean isSperateBuffer() { return sperate_buffer;}
	
	/**
	 * Add a AttribArray to the model with the given <i>index</i> binding.
	 * @param attrib
	 * @param index
	 * @return
	 */
	public boolean addAttrib(AttribArray attrib, int index){
		if(attrib == null)
			return false;
		
		if(!attribs.contains(attrib)){
//			if(flag == FLAG_WITH_PROGRAM){
//				throw new IllegalStateException("Can't use both program and without_program attribs");
//			}
			
			if(index < 0)
				throw new IllegalArgumentException("The index less than 0. index = " + index);
			
			attribs.add(attrib);
//			System.out.println("index = " + index);
			AttribInfo info = new AttribInfo();
			info.index = index;
			info.size = attrib.cmpSize;
			info.type = attrib.getType();
			attribInfos.add(info);
			size++;
//			flag = FLAG_WITHOUT_PROGRAM;
			return true;
		}
		
		return false;
	}
	
	public int indexOf(AttribArray attrib){
		if(attrib == null)
			return -1;
		
		return attribs.indexOf(attrib);
	}
	
	public void bindAttribIndex(AttribArray attrib, int index){
		if(attrib == null)
			return;
		
		int i = indexOf(attrib);
		attribInfos.get(i).index = index;
	}
	
	public void bindAttribIndex(int attrib_index, int index){
		attribInfos.get(attrib_index).index = index;
	}
	
	public AttribArray getAttribute(int index){ return attribs.get(index);}
	
	/** Test weather the attributes assembly valid. */
	public void valid(){
		// Check empty
		if(size == 0)
			throw new NullPointerException("The model is empty.");
		
		// Check weather the number of attributes element has matched.
		int count = -1;
		int i;
		for(i = 0; i < attribs.size(); i++){
			if(attribs.get(i) != null)
				count = attribs.get(i).getSize();
		}
		
		for(; i < attribs.size(); i++){
			AttribArray attrib = attribs.get(i);
			if(attrib != null && attrib.getSize() != count){
				throw new IllegalStateException("The element count doesn't match!");
			}
		}
		
		// Check weather the binding index has duplicated.
//		boolean[] index = new boolean[attribInfos.size()];
		HashMap<Integer, Boolean> index = new HashMap<>();
		for(i = 0; i < attribInfos.size(); i++){
			AttribArray attrib = attribs.get(i);
			if(attrib != null){
				AttribInfo info = attribInfos.get(i);
				if(_To(index.get(info.index))){
					throw new IllegalStateException("dumplicate the binding index: " + info.index);
				}else{
//					index[info.index] = true;
					index.put(info.index, true);
				}
			}
		}
	}

	private static boolean _To(Boolean b){
		if(b != null){
			return b;
		}else{
			return false;
		}
	}
	
	public GLVAO genVAO(){
		valid();
		
		GLVAO vao = new GLVAO(this);
		vaos.add(vao);
		return vao;
	}
	
	public boolean removeVAO(GLVAO vao){
		int index = vaos.indexOf(vao);
		if(index >=0){
			vaos.set(index, null);
			return true;
		}
		
		return false;
	}
	
	public int indexOf(GLVAO vao){if(vao == null) return -1; return vaos.indexOf(vao);}
	
	public GLVAO getVAO(int index){ return vaos.get(index);}
	
	public boolean update(int vao_index, boolean programed){
		GLVAO vao = vaos.get(vao_index);
		if(vao != null){
			vao.update(this,programed);
			return true;
		}
		
		return false;
	}
	
	public boolean update(GLVAO vao, boolean programed){
		if(vao == null)
			return false;
		
		if(vaos.contains(vao)){
			vao.update(this, programed);
			return true;
		}
		
		return false;
	}
	
	/** Set the elements array. Pass null to remove the elements array. */
	public void setElements(AttribArray element){
		this.element = element;
		if(/*LWJGLUtil.CHECKS &&*/ element != null){
			int type = element.getType();
			if(type != GLenum.GL_BYTE  && type != GLenum.GL_UNSIGNED_BYTE &&
			   type != GLenum.GL_INT   && type != GLenum.GL_UNSIGNED_INT &&
			   type != GLenum.GL_SHORT && type != GLenum.GL_UNSIGNED_SHORT)
				throw new IllegalArgumentException("The attrib is not a element array.");
		}
	}
	
	public AttribArray getElements() { return element;}

	public boolean removeAttrib(AttribArray attrib){
		if(attrib == null)
			return false;
		
		int index = attribs.indexOf(attrib);
		if(index >=0){
			attribs.set(index, null);
			attribInfos.set(index,null);
			size--;
			return true;
		}else
			return false;
		
	}
}
