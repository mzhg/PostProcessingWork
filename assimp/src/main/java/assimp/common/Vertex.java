/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2012, assimp team
All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the 
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/
/** @file Defines a helper class to represent an interleaved vertex
  along with arithmetic operations to support vertex operations
  such as subdivision, smoothing etc.
  
  While the code is kept as general as possible, arithmetic operations
  that are not currently well-defined (and would cause compile errors
  due to missing operators in the math library), are commented.
  */
package assimp.common;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/** Intermediate description a vertex with all possible components. Defines a full set of 
 *  operators, so you may use such a 'Vertex' in basic arithmetics. All operators are applied
 *  to *all* vertex components equally. This is useful for stuff like interpolation
 *  or subdivision, but won't work if special handling is required for some vertex components. */
public class Vertex {
	
	public final Vector3f position = new Vector3f();
	public final Vector3f normal = new Vector3f();
	public final Vector3f tangent = new Vector3f();
	public final Vector3f bitangent = new Vector3f();
	
	public final Vector3f[] texcoords = new Vector3f[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];
	public final Vector4f[] colors = new Vector4f[Mesh.AI_MAX_NUMBER_OF_COLOR_SETS];
	
	public Vertex() {
		initArray();
	}
	
	// ----------------------------------------------------------------------------
	/** Extract a particular vertex from a mesh and interleave all components */
	public Vertex(Mesh msh, int idx){
		assign(msh, idx);
	}
	
	/** Convert back to non-interleaved storage */
	public void sortBack(Mesh out, int idx){
//		out.mVertices[idx].set(position);
		int i0 = 3 * idx + 0;
		int i1 = 3 * idx + 1;
		int i2 = 3 * idx + 2;
		out.mVertices.put(i0, position.x);
		out.mVertices.put(i1, position.y);
		out.mVertices.put(i2, position.z);

		if (out.hasNormals()) {
//			out.mNormals[idx].set(normal);
			out.mNormals.put(i0, normal.x);
			out.mNormals.put(i1, normal.y);
			out.mNormals.put(i2, normal.z);
		}

		if (out.hasTangentsAndBitangents()) {
//			out.mTangents[idx].set(tangent);
//			out.mBitangents[idx].set(bitangent);
			
			out.mTangents.put(i0, tangent.x);
			out.mTangents.put(i1, tangent.y);
			out.mTangents.put(i2, tangent.z);
			
			out.mBitangents.put(i0, bitangent.x);
			out.mBitangents.put(i1, bitangent.y);
			out.mBitangents.put(i2, bitangent.z);
		}

		for(int i = 0; out.hasTextureCoords(i); ++i) {
//			out.mTextureCoords[i][idx].set(texcoords[i]);
			out.mTextureCoords[i].put(i0, texcoords[i].x);
			out.mTextureCoords[i].put(i1, texcoords[i].y);
			out.mTextureCoords[i].put(i2, texcoords[i].z);
		}

		for(int i = 0; out.hasVertexColors(i); ++i) {
//			out.mColors[i][idx].set(colors[i]);
			int index = 4 * idx;
			out.mColors[i].put(index++, colors[i].x);
			out.mColors[i].put(index++, colors[i].y);
			out.mColors[i].put(index++, colors[i].z);
			out.mColors[i].put(index++, colors[i].w);
			
		}
	}
	
	public void assign(Mesh msh, int idx){
//		position.set(msh.mVertices[idx]);
		int i0 = 3 * idx + 0;
		int i1 = 3 * idx + 1;
		int i2 = 3 * idx + 2;
		position.x = msh.mVertices.get(i0);
		position.y = msh.mVertices.get(i1);
		position.z = msh.mVertices.get(i2);

		if (msh.hasNormals()) {
//			normal.set(msh.mNormals[idx]);
			normal.x = msh.mNormals.get(i0);
			normal.y = msh.mNormals.get(i1);
			normal.z = msh.mNormals.get(i2);
		}

		if (msh.hasTangentsAndBitangents()) {
//			tangent.set(msh.mTangents[idx]);
//			bitangent.set(msh.mBitangents[idx]);
			tangent.x = msh.mTangents.get(i0);
			tangent.y = msh.mTangents.get(i1);
			tangent.z = msh.mTangents.get(i2);
			
			bitangent.x = msh.mBitangents.get(i0);
			bitangent.y = msh.mBitangents.get(i1);
			bitangent.z = msh.mBitangents.get(i2);
		}

		for (int i = 0; msh.hasTextureCoords(i); ++i) {
//			texcoords[i].set(msh.mTextureCoords[i][idx]);
			
			texcoords[i].x = msh.mTextureCoords[i].get(i0);
			texcoords[i].y = msh.mTextureCoords[i].get(i1);
			texcoords[i].z = msh.mTextureCoords[i].get(i2);
		}

		for (int i = 0; msh.hasVertexColors(i); ++i) {
//			colors[i].set(msh.mColors[i][idx]);
			int index = 4 * idx;
			colors[i].x = msh.mColors[i].get(index ++);
			colors[i].y = msh.mColors[i].get(index ++);
			colors[i].z = msh.mColors[i].get(index ++);
			colors[i].w = msh.mColors[i].get(index ++);
		}
	}
	
	final void initArray(){
		for(int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; i++){
			texcoords[i] = new Vector3f();
		}
		
		for(int i = 0; i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; i++){
			colors[i] = new Vector4f();
		}
	}
	
	public static Vertex add(Vertex left, Vertex right, Vertex dest){
		if(dest == null)
			dest = new Vertex();
		
		Vector3f.add(left.position, right.position, dest.position);
		Vector3f.add(left.normal, right.normal, dest.normal);
		Vector3f.add(left.tangent, right.tangent, dest.tangent);
		Vector3f.add(left.bitangent, right.bitangent, dest.bitangent);
		
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
//			res.texcoords[i] = op<aiVector3D>()(v0.texcoords[i],v1.texcoords[i]);
			Vector3f.add(left.texcoords[i], right.texcoords[i], dest.texcoords[i]);
		}
		
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; ++i) {
//			res.colors[i] = op<aiColor4D>()(v0.colors[i],v1.colors[i]);
			Vector4f.add(left.colors[i], right.colors[i], dest.colors[i]);
		}
		
		return dest;
	}
	
	public static Vertex sub(Vertex left, Vertex right, Vertex dest){
		if(dest == null)
			dest = new Vertex();
		
		Vector3f.sub(left.position, right.position, dest.position);
		Vector3f.sub(left.normal, right.normal, dest.normal);
		Vector3f.sub(left.tangent, right.tangent, dest.tangent);
		Vector3f.sub(left.bitangent, right.bitangent, dest.bitangent);
		
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
//			res.texcoords[i] = op<aiVector3D>()(v0.texcoords[i],v1.texcoords[i]);
			Vector3f.sub(left.texcoords[i], right.texcoords[i], dest.texcoords[i]);
		}
		
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; ++i) {
//			res.colors[i] = op<aiColor4D>()(v0.colors[i],v1.colors[i]);
			Vector4f.sub(left.colors[i], right.colors[i], dest.colors[i]);
		}
		
		return dest;
	}
	
	public static Vertex mul(Vertex left, float right, Vertex dest){
		if(dest == null)
			dest = new Vertex();
		
		Vector3f.scale(left.position, right, dest.position);
		Vector3f.scale(left.normal, right, dest.normal);
		Vector3f.scale(left.tangent, right, dest.tangent);
		Vector3f.scale(left.bitangent, right, dest.bitangent);
		
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
//			res.texcoords[i] = op<aiVector3D>()(v0.texcoords[i],v1.texcoords[i]);
			Vector3f.scale(left.texcoords[i], right, dest.texcoords[i]);
		}
		
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; ++i) {
//			res.colors[i] = op<aiColor4D>()(v0.colors[i],v1.colors[i]);
			Vector4f.scale(left.colors[i], right, dest.colors[i]);
		}
		
		return dest;
	}
	
	public static Vertex div(Vertex left, float right, Vertex dest){
		return mul(left, 1.0f/right, dest);
	}
}
