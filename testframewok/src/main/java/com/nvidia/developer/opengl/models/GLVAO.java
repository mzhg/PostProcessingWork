package com.nvidia.developer.opengl.models;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/** OpenGL Vertex Array Object. */
public class GLVAO {

//	final Model parent;
	int vaoID;
	GLBuffer[] glbuffers;
	AttribInfo[] attribInfos;
	byte[] validIds;
	int stride;
	int vertexCount;
	
	GLBuffer element;
	int elementCount;
	int elementType;
	int elementID;
	int elementModified;
	
	boolean programed = true;
	boolean prepared;
	
	/*public */GLVAO(Model parent) {
//		this.parent = parent;

		// Generate the GL-Buffers
		List<AttribArray> attribs = parent.attribs;
		List<AttribInfo>  infos   = parent.attribInfos;
		
//		if(parent.isProgramFlag()){
//			connects = new AttribConnect[1];
//			connects[0] = AttribConnect.VERTEX_ATTRIB;
//		}else{
//			connects = new AttribConnect[]
//		}
		
		attribInfos = new AttribInfo[infos.size()];
		for(int i = 0; i < attribInfos.length; i++)
			attribInfos[i] = new AttribInfo();
		
		if(parent.dymatic || parent.sperate_buffer){
			// Generate the separate buffer.
			glbuffers = new GLBuffer[attribs.size()];
			for(int i = 0; i < glbuffers.length; i++){
				AttribArray attrib = attribs.get(i);
				AttribInfo  info   = infos.get(i);
				attribInfos[i].index = info.index;
				attribInfos[i].size = info.size;
				attribInfos[i].type = info.type;
				attribInfos[i].offset = 0;
				attribInfos[i].modified = attrib.modified;  // record the 
				attribInfos[i].divisor = attrib.divisor;
				
				if(attrib != null){
					GLBuffer buffer = glbuffers[i] = new GLBuffer(GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW);
					int size = attrib.getByteSize();
					ByteBuffer buf = CacheBuffer.getCachedByteBuffer(size);
					attrib.store(buf);
					buf.flip();
					buffer.load(buf);
					if(attrib.divisor == 0)
						vertexCount = attrib.getSize();
				}
			}
			
		}else{
			// Generate the combined gl-buffer-object.
			glbuffers = new GLBuffer[1];
			GLBuffer buffer = glbuffers[0] = new GLBuffer(GLenum.GL_ARRAY_BUFFER, GLenum.GL_STATIC_DRAW);
			validIds = new byte[parent.size()];
			
			// measure the attribute informations.
			int bufferSize = 0;
			stride = 0;
			int validIndex = 0;
			for(int i = 0; i < attribs.size(); i++){
				AttribArray attrib = attribs.get(i);
				AttribInfo  info   = infos.get(i);
				if(attrib != null){
					bufferSize += attrib.getByteSize();
					attribInfos[i].index = info.index;
					attribInfos[i].offset = stride;
					attribInfos[i].size = info.size;
					attribInfos[i].type = info.type;
					attribInfos[i].modified = attrib.modified;
					attribInfos[i].divisor = attrib.divisor;
					
					int cmpSize = attrib.getByteSize()/attrib.getSize();
					stride += cmpSize;
					
					if(attrib.divisor == 0){
						vertexCount = attrib.getSize();
					}
					validIds[validIndex ++] = (byte)i;
				}
			}
			
			// fill the data
			ByteBuffer buf = CacheBuffer.getCachedByteBuffer(bufferSize);
			for(int j = 0; j < vertexCount; j++){
				for(int i = 0; i < attribs.size(); i++){
					AttribArray attrib = attribs.get(i);
					if(attrib != null){
						attrib.store(j, buf);
					}
				}
			}
			buf.flip();
			
			// generate the gl buffer
			buffer.bind();
			buffer.load(buf);
			buffer.unbind();
		}

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		
		if(parent.element != null){
			createElementBuffer(parent);
		}
		
		// Generate the VAO if the video card supported.
		if(programed){ // TODO Need precise check
			vaoID = gl.glGenVertexArray();
			gl.glBindVertexArray(vaoID);
			_bind();
			gl.glBindVertexArray(0);
			_unbind();
		}
	}
	
	// Unimplemented
//	void prepareUpdate(){
//		for(int i = 0; i < glbuffers.length; i++){
//			GLBuffer buffer = glbuffers[i];
//			if(buffer != null){
//				
//			}
//		}
//	}
	
	void update(Model parent, boolean isProgram){
		List<AttribArray> attribs = parent.attribs;
		List<AttribInfo>  infos   = parent.attribInfos;
		
		boolean needReBind = false;
		// update the attribute informations
		if(attribInfos.length != infos.size()){
			int old_length = attribInfos.length;
			attribInfos = Arrays.copyOf(attribInfos, infos.size());
			for(int i = old_length; i < attribInfos.length; i++)
				attribInfos[i] = new AttribInfo();
			
			needReBind = true;
		}
		
//		boolean isProgram = parent.flag == Model.FLAG_WITH_PIPELINE;
		if(programed != isProgram){
			programed = isProgram;
			needReBind = true;
		}
		
		// update the buffer content
		if(parent.dymatic){
			// update the separate buffer.
			if(glbuffers.length != attribs.size()){
				glbuffers =  Arrays.copyOf(glbuffers, attribs.size());
			}
			
			int newVertexCount = -1;
			for(int i = 0; i < glbuffers.length; i++){
				AttribArray attrib = attribs.get(i);
				AttribInfo  info   = infos.get(i);
				attribInfos[i].index = info.index;
				attribInfos[i].type = info.type;
				attribInfos[i].offset = 0;
				GLBuffer buffer = glbuffers[i];
				
				if(attrib != null){
					if(buffer == null){ // create a new one.
						buffer = glbuffers[i] = new GLBuffer(GLenum.GL_ARRAY_BUFFER, GLenum.GL_DYNAMIC_DRAW);
						needReBind = true;
						ByteBuffer buf = wrap(attrib);
						buffer.load(buf);
						newVertexCount = attrib.getSize();
					}else{
						// check weather the content of the new attribute has changed.
						if(attribInfos[i].modified != attrib.modified){
							buffer.bind();
							if(vertexCount != attrib.getSize()){
								// The buffer length has changed. reload the buffer data.
								ByteBuffer buf = wrap(attrib);
								buffer.load(buf);
								newVertexCount = attrib.getSize();
							}else{
								ByteBuffer buf = CacheBuffer.getCachedByteBuffer(attrib.getSize());
								attrib.store(buf);
								buf.flip();
								buffer.update(0, attrib.getSize(), buf);
							}
							attribInfos[i].modified = attrib.modified;
						}else{
							// nothing need todo.
						}
					}
				}else{ // attribute is null, maybe removed.
					if(buffer != null){
						// remove the buffer
						buffer.unbind();
						buffer.dispose();
						glbuffers[i] = null;
					}
				}
			}
			
			if(newVertexCount != -1)
				vertexCount = newVertexCount;
		}else{
			// Generate the combined gl-buffer-object.
//			GLBuffer buffer = glbuffers[0] = new GLBuffer(GL15.GL_ARRAY_BUFFER, GL15.GL_STATIC_DRAW);
//			validIds = new byte[parent.size()];
			boolean needUpdate = false;
			GLBuffer buffer = glbuffers[0];
			if(validIds.length != parent.size()){
				validIds = new byte[parent.size()];
				needUpdate = true;
			}
			
			// measure the attribute informations.
			int bufferSize = 0;
			int stride = 0;
			int validIndex = 0;
			int[] tmpOffsets = new int[attribs.size()];
			int[] elemByteSizes = new int[attribs.size()];
			for(int i = 0; i < attribs.size(); i++){
				AttribArray attrib = attribs.get(i);
				AttribInfo  info   = infos.get(i);
				
				if(attrib != null){
					bufferSize += attrib.getByteSize();
					attribInfos[i].index = info.index;
//					attribInfos[i].offset = stride;
					tmpOffsets[i] = stride;
//					attribInfos[i].size = info.size;
//					attribInfos[i].type = info.type;
					int cmpSize = attrib.getByteSize()/attrib.getSize();
					elemByteSizes[i] = cmpSize;
					stride += cmpSize;
					vertexCount = attrib.getSize();
					validIds[validIndex ++] = (byte)i;
				}
			}
			
			ByteBuffer buf;
			if(bufferSize != buffer.getBufferSize()){
				// fill the data
				buf = CacheBuffer.getCachedByteBuffer(bufferSize);
				for(int j = 0; j < vertexCount; j++){
					for(int i = 0; i < attribs.size(); i++){
						AttribArray attrib = attribs.get(i);
						if(attrib != null){
							attrib.store(j, buf);
						}
					}
				}
				
				for(int i = 0; i < tmpOffsets.length; i++){
					AttribArray attrib = attribs.get(i);
					if(attrib != null){
						attribInfos[i].offset = tmpOffsets[i];
						attribInfos[i].modified = attrib.modified;
					}
				}
				
				buf.flip();
				// generate the gl buffer
				buffer.bind();
				buffer.load(buf);
			}else{
				buffer.bind();
//				buffer.prepareUpdate(GLES31.GL_WRITE_ONLY);
//				buf = buffer.getMappingBuffer();
				buf = CacheBuffer.getCachedByteBuffer((int)buffer.getBufferSize());
				for(int j = 0; j < vertexCount; j++){
					for(int i = 0; i < attribs.size(); i++){
						AttribArray attrib = attribs.get(i);
						if(attrib != null){
							if(attrib.modified != attribInfos[i].modified || tmpOffsets[i] != attribInfos[i].offset){
								attrib.store(j, buf);
								attribInfos[i].modified = attrib.modified;
								attribInfos[i].offset = tmpOffsets[i];
							}else{
								buf.position(buf.position() + elemByteSizes[i]);  // unmodifier the data.
							}
						}
					}
				}
				buf.flip();
				buffer.update(0, (int)buffer.getBufferSize(), buf);
			}
		}

		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
		
		//update the element buffer
		if(parent.element != null){
			if(element == null){
				createElementBuffer(parent);
			}else{
				if(elementID != parent.element.getUniqueID() || elementModified != parent.element.modified){
					int bufferSize = parent.element.getByteSize();
					element.bind();
					ByteBuffer buf;
					if(bufferSize != element.getBufferSize()){
						buf = CacheBuffer.getCachedByteBuffer(bufferSize);
						parent.element.store(buf);
						buf.flip();
						element.load(buf);
					}else{
//						element.beginUpdate(GLES31.GL_WRITE_ONLY);
//						buf = element.getMappingBuffer();
//						parent.element.store(buf);
//						element.finishUpdate();

						buf = CacheBuffer.getCachedByteBuffer((int)element.getBufferSize());
						parent.element.store(buf);
						buf.flip();
						element.update(0, buf.remaining(), buf);
					}
					element.unbind();
					
					elementCount = parent.element.getSize();
					elementType = fixElementType(parent.element.getType());
					elementID = parent.element.getUniqueID();
					elementModified = parent.element.modified;
				}
			}
		}else{
			if(element != null){
				element.dispose();
				element = null;
			}
		}
		
		// Generate the VAO if the video card supported.
		if(needReBind &&  programed/*GL.getCapabilities().GL_ARB_vertex_array_object*/){
			gl.glDeleteVertexArray(vaoID);  // necessary do this?
			vaoID = gl.glGenVertexArray();
			gl.glBindVertexArray(vaoID);
			_bind();
			gl.glBindVertexArray(0);
			_unbind();
		}
	}
	
	void createElementBuffer(Model parent){
		element = new GLBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, parent.dymatic ? GLenum.GL_DYNAMIC_DRAW : GLenum.GL_STATIC_DRAW);
		ByteBuffer buf = CacheBuffer.getCachedByteBuffer(parent.element.getByteSize());
		parent.element.store(buf);
		buf.flip();
		element.load(buf);
		element.unbind();
		elementCount = parent.element.getSize();
		elementType = fixElementType(parent.element.getType());
		elementID = parent.element.getUniqueID();
		elementModified = parent.element.modified;
	}
	
	private static ByteBuffer wrap(AttribArray attrib){
		int size = attrib.getByteSize();
		ByteBuffer buf = CacheBuffer.getCachedByteBuffer(size);
		attrib.store(buf);
		buf.flip();
		return buf;
	}
	
	private static int fixElementType(int type){
		switch (type) {
		case GLenum.GL_UNSIGNED_BYTE:
		case GLenum.GL_BYTE:
			return GLenum.GL_UNSIGNED_BYTE;

		case GLenum.GL_UNSIGNED_SHORT:
		case GLenum.GL_SHORT:
			return GLenum.GL_UNSIGNED_SHORT;
		case GLenum.GL_UNSIGNED_INT:
		case GLenum.GL_INT:
			return GLenum.GL_UNSIGNED_INT;
		default:
			throw new IllegalArgumentException("Unsupport Element type: " + type);
		}
	}
	
	public void draw(int mode){
		this.draw(mode, 1);
	}
	
	public void draw(int mode, int instanceCount){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(element != null){
			if(instanceCount > 1){
				gl.glDrawElementsInstanced(mode, elementCount, elementType, 0, instanceCount);
			}else{
				gl.glDrawElements(mode, elementCount, elementType, 0);
			}
		}else{
			if(instanceCount > 1){
				gl.glDrawArraysInstanced(mode, 0, vertexCount, instanceCount);
			}else{
				gl.glDrawArrays(mode, 0, vertexCount);
			}
		}
	}
	
	/** Bind the current buffer. */
	public void bind(){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(vaoID != 0){
			gl.glBindVertexArray(vaoID);
		}else{
			_bind();  // bind the buffer directly.
		}
	}
	
	public void unbind(){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(vaoID != 0){
			gl.glBindVertexArray(0);
		}else{
			_unbind();  // unbind the buffer directly.
		}
	}
	
	// bind the buffer
	private void _bind(){
		if(glbuffers.length == attribInfos.length){ // Separate buffer
			for(int i = 0; i < glbuffers.length; i++){
				GLBuffer buffer = glbuffers[i];
				AttribInfo info = attribInfos[i];
				if(buffer != null){
					buffer.bind();
					AttribConnect connect = getCorrespondConnect(info.index);
					connect.enable(info.index, info.size, info.type, 0, info.offset, info.divisor);
				}
			}
		}else{  // combined buffer
			for(int i = 0; i < glbuffers.length; i++){
				GLBuffer buffer = glbuffers[i];
				buffer.bind();
				for(int j = 0; j < validIds.length; j++){
					AttribInfo info = attribInfos[validIds[j]];
					AttribConnect connect = getCorrespondConnect(info.index);
					connect.enable(info.index, info.size, info.type, stride, info.offset, info.divisor);
//					System.out.println(info);
				}
			}
		}
		
		// bind element buffer if it exits.
		if(element != null){
			element.bind();
		}
	}
	
	// unbind the buffer
	private void _unbind(){
		if(glbuffers.length == attribInfos.length){ // Separate buffer
			for(int i = 0; i < glbuffers.length; i++){
				GLBuffer buffer = glbuffers[i];
				AttribInfo info = attribInfos[i];
				if(buffer != null){
					buffer.unbind();
					AttribConnect connect = getCorrespondConnect(info.index);
					connect.disable(info.index);
				}
			}
		}else{  // combined buffer
			for(int i = 0; i < glbuffers.length; i++){
				GLBuffer buffer = glbuffers[i];
				buffer.unbind();
				for(int j = 0; j < validIds.length; j++){
					AttribInfo info = attribInfos[validIds[j]];
					AttribConnect connect = getCorrespondConnect(info.index);
					connect.disable(info.index);
				}
			}
		}
		
		if(element != null){
			element.unbind();
		}
	}
	
	public void dispose(){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		if(vaoID != 0){
			gl.glDeleteVertexArray(vaoID);
			vaoID = 0;
		}
		
		for(int i = 0; i < glbuffers.length; i++){
			GLBuffer buffer = glbuffers[i];
			if(buffer != null){
				buffer.dispose();
				glbuffers[i] = null;
			}
		}
		
		if(element != null){
			element.dispose();
			element = null;
		}
	}
	
	private AttribConnect getCorrespondConnect(int index){
		return AttribConnect.VERTEX_ATTRIB;
	}
}
