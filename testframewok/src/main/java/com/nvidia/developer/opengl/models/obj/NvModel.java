package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

//
//nvModel.h - Model support class
//
//The nvModel class implements an interface for a multipurpose model
//object. This class is useful for loading and formatting meshes
//for use by OpenGL. It can compute face normals, tangents, and
//adjacency information. The class supports the obj file format.
//
//Author: Evan Hart
//Email: sdkfeedback@nvidia.com
//
//Copyright (c) NVIDIA Corporation. All rights reserved.
////////////////////////////////////////////////////////////////////////////////
public class NvModel {

	/* Enumeration of primitive types */
	/** Not Set */
	public static final int NONE = 0x0;
	/** single-vertex points */
	public static final int POINTS = 0x1;
	/** two vertex edges */
	public static final int EDGES = 0x2;
	/** three-vertex triangles */
	public static final int TRIANGLES = 0x4;
	/** triangles with adjancency info */
	public static final int TRIANGLES_WITH_ADJACENCY = 0x8;
	/** mask of all values */
	public static final int ALL = 0xf;

	public static final int NumPrimTypes = 4;

	// Would all this be better done as a channel abstraction to handle more
	// arbitrary data?

	// data structures for model data, not optimized for rendering
	protected StackFloat _positions = new StackFloat();
	protected StackFloat _normals = new StackFloat();
	protected StackFloat _texCoords = new StackFloat();
	protected StackFloat _sTangents = new StackFloat();
	protected StackFloat _colors = new StackFloat();

	protected int _posSize;
	protected int _tcSize;
	protected int _cSize;

	protected StackInt _pIndex = new StackInt();
	protected StackInt _nIndex = new StackInt();
	protected StackInt _tIndex = new StackInt();
	protected StackInt _tanIndex = new StackInt();
	protected StackInt _cIndex = new StackInt();

	// data structures optimized for rendering, compiled model
	protected StackInt[] _indices = new StackInt[NumPrimTypes];
	protected StackFloat _vertices = new StackFloat();
	protected int _pOffset = -1;
	protected int _nOffset = -1;
	protected int _tcOffset = -1;
	protected int _sTanOffset = -1;
	protected int _cOffset = -1;
	protected int _vtxSize = -1;

	protected int _openEdges;
	
	public NvModel() {
		for(int i = 0; i < NumPrimTypes; i++)
			_indices[i] = new StackInt();
	}
	
	/*
	 * Create a model from OBJ data
	 * @param data pointer to OBJ file data
	 * @param scale the target radius to which we want the model scaled, or <0 if no scaling should be done
	 * @param computeNormals indicate whether per-vertex normals should be estimated and added
	 * @param computeTangents indicate whether per-vertex tangent vectors should be estimated and added
	 * @return a new model
	 */
	public static NvModel createFromObj(ByteBuffer data, float scale, boolean computeNormals, boolean computeTangents){
		return null;
	}

	/**
	 * This function attempts to determine the type of the filename passed as a
	 * parameter. If it understands that file type, it attempts to parse and
	 * load the file into its raw data structures.
	 */
	public void loadModelFromFile(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			float x, y, z, w;
			int idx[][] = new int[3][3];
			boolean hasTC = false;
			boolean hasNormals = false;

			String line;
			while ((line = reader.readLine()) != null) {
				if(line.length() == 0)  // skip the empty string.
					continue;
				
				switch (line.charAt(0)) {
				case '#':
					// comment line, eat the remainder
					break;
				case 'v':
					StringTokenizer token = new StringTokenizer(line);
					token.nextToken(); // skip the first word.(ie. v, vt,vn)
					switch (line.charAt(1)) {
					case ' ':
						// vertex, 3 or 4 components
						x = Float.parseFloat(token.nextToken());
						y = Float.parseFloat(token.nextToken());
						z = Float.parseFloat(token.nextToken());
						
						_positions.push(x);
						_positions.push(y);
						_positions.push(z);
						if (token.hasMoreElements()) {
							w = Float.parseFloat(token.nextToken());
							_positions.push(w);
							_posSize = 4;
						} else {
							w = 1.0f; // default w coordinate
							_posSize = 3;
						}
						
						break;
					case 'n':
						// normal, 3 components
						x = Float.parseFloat(token.nextToken());
						y = Float.parseFloat(token.nextToken());
						z = Float.parseFloat(token.nextToken());

						_normals.push(x);
						_normals.push(y);
						_normals.push(z);
						break;
					case 't':
						// texcoord, 2 or 3 components

						x = Float.parseFloat(token.nextToken()); // s
						y = Float.parseFloat(token.nextToken()); // t

						_texCoords.push(x);
						_texCoords.push(y);
						if (token.hasMoreElements()) {
							z = Float.parseFloat(token.nextToken()); // r
							_texCoords.push(z);
							_tcSize = 3;
						} else {
							z = 0.0f; // default r coordinate
							_tcSize = 2;
						}
						
						break;
					}
					break;
				case 'f':
					// face
					// determine the type, and read the initial vertex, all entries in a face must have the same format
	                // formats are:
	                // 1  #
	                // 2  #/#
	                // 3  #/#/#
	                // 4  #//#
					token = new StringTokenizer(line);
					token.nextToken(); // skip the first letter 'f'.

					final String buf = token.nextToken();

					// determine the type, and read the initial vertex, all
					// entries in a face must have the same format
					String[] strs = buf.split("//");
					// "%d//%d"
					if (strs.length == 2) {
						// This face has vertex and normal indices
						idx[0][0] = Integer.parseInt(strs[0]);
						idx[0][1] = Integer.parseInt(strs[1]);

						// remap them to the right spot
						idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : (_positions.size() - idx[0][0]);
						idx[0][1] = (idx[0][1] > 0) ? (idx[0][1] - 1) : (_normals.size() - idx[0][1]);

						// grab the second vertex to prim
						String second = token.nextToken();
						String[] _ss = second.split("//");
						idx[1][0] = Integer.parseInt(_ss[0]);
						idx[1][1] = Integer.parseInt(_ss[1]);

						// remap them to the right spot
						idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : (_positions.size() - idx[1][0]);
						idx[1][1] = (idx[1][1] > 0) ? (idx[1][1] - 1) : (_normals.size() - idx[1][1]);

						// create the fan
						while (token.hasMoreTokens()) {
							// remap them to the right spot
							String third = token.nextToken();
							String[] _tt = third.split("//");
							if (_tt.length != 2)
								break;
							idx[2][0] = Integer.parseInt(_tt[0]);
							idx[2][1] = Integer.parseInt(_tt[1]);

							// remap them to the right spot
							idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : (_positions.size() - idx[2][0]);
							idx[2][1] = (idx[2][1] > 0) ? (idx[2][1] - 1) : (_normals.size() - idx[2][1]);

							// add the indices
							for (int ii = 0; ii < 3; ii++) {
								_pIndex.push(idx[ii][0]);
								_nIndex.push(idx[ii][1]);
								_tIndex.push(0); // dummy index, to ensure that
													// the buffers are of
													// identical size
							}

							// prepare for the next iteration
							idx[1][0] = idx[2][0];
							idx[1][1] = idx[2][1];
						}

						hasNormals = true;
						break;
					}

					strs = buf.split("/");
					// "%d/%d/%d"
					if (strs.length == 3) {
						// This face has vertex, texture coordinate, and normal
						// indices
						idx[0][0] = Integer.parseInt(strs[0]);
						idx[0][1] = Integer.parseInt(strs[1]);
						idx[0][2] = Integer.parseInt(strs[2]);

						// remap them to the right spot
						idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : (_positions.size() - idx[0][0]);
						idx[0][1] = (idx[0][1] > 0) ? (idx[0][1] - 1) : (_texCoords.size() - idx[0][1]);
						idx[0][2] = (idx[0][2] > 0) ? (idx[0][2] - 1) : (_normals.size() - idx[0][2]);

						// grab the second vertex to prim
						String second = token.nextToken();
						String[] _ss = second.split("/");
						idx[1][0] = Integer.parseInt(_ss[0]);
						idx[1][1] = Integer.parseInt(_ss[1]);
						idx[1][2] = Integer.parseInt(_ss[2]);

						// remap them to the right spot
						idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : (_positions.size() - idx[1][0]);
						idx[1][1] = (idx[1][1] > 0) ? (idx[1][1] - 1) : (_texCoords.size() - idx[1][1]);
						idx[1][2] = (idx[1][2] > 0) ? (idx[1][2] - 1) : (_normals.size() - idx[1][2]);

						// create the fan
						while (token.hasMoreTokens()) {
							second = token.nextToken();
							_ss = second.split("/");
							idx[2][0] = Integer.parseInt(_ss[0]);
							idx[2][1] = Integer.parseInt(_ss[1]);
							idx[2][2] = Integer.parseInt(_ss[2]);

							// remap them to the right spot
							idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : (_positions.size() - idx[2][0]);
							idx[2][1] = (idx[2][1] > 0) ? (idx[2][1] - 1) : (_texCoords.size() - idx[2][1]);
							idx[2][2] = (idx[2][2] > 0) ? (idx[2][2] - 1) : (_normals.size() - idx[2][2]);

							// add the indices
							for (int ii = 0; ii < 3; ii++) {
								_pIndex.push(idx[ii][0]);
								_tIndex.push(idx[ii][1]);
								_nIndex.push(idx[ii][2]);
							}

							// prepare for the next iteration
							idx[1][0] = idx[2][0];
							idx[1][1] = idx[2][1];
							idx[1][2] = idx[2][2];
						}

						hasTC = true;
						hasNormals = true;
					} else if (strs.length == 2) {
						// This face has vertex and texture coordinate indices
						// "%d/%d"
						idx[0][0] = Integer.parseInt(strs[0]);
						idx[0][1] = Integer.parseInt(strs[1]);

						// remap them to the right spot
						idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : (_positions.size() - idx[0][0]);
						idx[0][1] = (idx[0][1] > 0) ? (idx[0][1] - 1) : (_texCoords.size() - idx[0][1]);

						// grab the second vertex to prim
						String second = token.nextToken();
						String[] _ss = second.split("/");
						idx[1][0] = Integer.parseInt(_ss[0]);
						idx[1][1] = Integer.parseInt(_ss[1]);

						// remap them to the right spot
						idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : (_positions.size() - idx[1][0]);
						idx[1][1] = (idx[1][1] > 0) ? (idx[1][1] - 1) : (_texCoords.size() - idx[1][1]);

						// create the fan
						while (token.hasMoreTokens()) {
							second = token.nextToken();
							_ss = second.split("/");
							idx[2][0] = Integer.parseInt(_ss[0]);
							idx[2][1] = Integer.parseInt(_ss[1]);

							// remap them to the right spot
							idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : (_positions.size() - idx[2][0]);
							idx[2][1] = (idx[2][1] > 0) ? (idx[2][1] - 1) : (_texCoords.size() - idx[2][1]);

							// add the indices
							for (int ii = 0; ii < 3; ii++) {
								_pIndex.push(idx[ii][0]);
								_tIndex.push(idx[ii][1]);
								_nIndex.push(0); // dummy normal index to keep
													// everything in synch
							}

							// prepare for the next iteration
							idx[1][0] = idx[2][0];
							idx[1][1] = idx[2][1];
						}

						hasTC = true;
						// "%d"
					} else {
						// This face has only vertex indices
						idx[0][0] = Integer.parseInt(strs[0]);

						// remap them to the right spot
						idx[0][0] = (idx[0][0] > 0) ? (idx[0][0] - 1) : (_positions.size() - idx[0][0]);

						// grab the second vertex to prime
						idx[1][0] = Integer.parseInt(token.nextToken());

						// remap them to the right spot
						idx[1][0] = (idx[1][0] > 0) ? (idx[1][0] - 1) : (_positions.size() - idx[1][0]);

						// create the fan
						while (token.hasMoreElements()) {
							idx[2][0] = Integer.parseInt(token.nextToken());

							// remap them to the right spot
							idx[2][0] = (idx[2][0] > 0) ? (idx[2][0] - 1) : (_positions.size() - idx[2][0]);
							// add the indices
							for (int ii = 0; ii < 3; ii++) {
								_pIndex.push(idx[ii][0]);
								_tIndex.push(0); // dummy index to keep things
													// in synch
								_nIndex.push(0); // dummy normal index to keep
													// everything in synch
							}

							// prepare for the next iteration
							idx[1][0] = idx[2][0];
						}
					}
					break;
				case 's':
				case 'g':
				case 'u':
					// all presently ignored
				default:
					break;
				}
			}

			reader.close();

			// free anything that ended up being unused
			if (!hasNormals) {
				_normals.clear();
				_nIndex.clear();
			}

			if (!hasTC) {
				_texCoords.clear();
				_tIndex.clear();
			}

			// set the defaults as the worst-case for an obj file

			// compact to 3 component vertices if possible
			// TODO not necessary
//			 if (!vtx4Comp) {
//			     StackFloat positions = new StackFloat(_positions.size()/4 * 3);
//			     int size = _positions.size() / 4;
//			     for(int i = 0; i < size; i++){
//			    	 int index = i * 4;
//			    	 positions.push(_positions.get(index + 0));
//			    	 positions.push(_positions.get(index + 1));
//			    	 positions.push(_positions.get(index + 2));
//			     }
//			
//				 _positions = positions;
//				 _posSize = 3;
//			 }
			//
			// //compact to 2 component tex coords if possible
			// if (!tex3Comp) {
			// vector<float>::iterator src = m._texCoords.begin();
			// vector<float>::iterator dst = m._texCoords.begin();
			//
			// for ( ; src < m._texCoords.end(); ) {
			// *(dst++) = *(src++);
			// *(dst++) = *(src++);
			// src++;
			// }
			//
			// m._texCoords.resize( (m._texCoords.size() / 3) * 2);
			//
//			 _tcSize = 2;
			// }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function takes the raw model data in the internal structures, and
	 * attempts to bring it to a format directly accepted for vertex array style
	 * rendering. This means that a unique compiled vertex will exist for each
	 * unique combination of position, normal, tex coords, etc that are used in
	 * the model. The <i>prim</i> parameter, tells the model what type of index
	 * list to compile. By default it compiles a simple triangle mesh with no
	 * connectivity.
	 */
	public void compileModel() {
		compileModel(TRIANGLES);
	}

	/**
	 * This function takes the raw model data in the internal structures, and
	 * attempts to bring it to a format directly accepted for vertex array style
	 * rendering. This means that a unique compiled vertex will exist for each
	 * unique combination of position, normal, tex coords, etc that are used in
	 * the model. The <i>prim</i> parameter, tells the model what type of index
	 * list to compile.
	 */
	public void compileModel(int prim) {
		boolean needsTriangles = false;
		boolean needsTrianglesWithAdj = false;
		boolean needsEdges = false;
		boolean needsPoints = false;

		if ((prim & POINTS) == POINTS)
			needsPoints = true;

		if ((prim & TRIANGLES) == TRIANGLES)
			needsTriangles = true;

		if ((prim & TRIANGLES_WITH_ADJACENCY) == TRIANGLES_WITH_ADJACENCY) {
			needsTriangles = true;
			needsTrianglesWithAdj = true;
		}

		if ((prim & EDGES) == EDGES) {
			needsTriangles = true;
			needsEdges = true;
		}

		// merge the points
		HashMap<IdxSet, Integer> pts = new HashMap<IdxSet, Integer>();

		// find whether a position is unique
		HashSet<Integer> ptSet = new HashSet<Integer>();

		{
			int pit = 0;
			int nit = 0;
			int tit = 0;
			int tanit = 0;
			int cit = 0;

			while (pit < _pIndex.size()) {
				IdxSet idx = new IdxSet();
				idx.pIndex = _pIndex.get(pit);

				if (_normals.size() > 0)
					idx.nIndex = _nIndex.get(nit);
				else
					idx.nIndex = 0;

				if (_tIndex.size() > 0)
					idx.tIndex = _tIndex.get(tit);
				else
					idx.tIndex = 0;

				if (_tanIndex.size() > 0)
					idx.tanIndex = _tanIndex.get(tanit);
				else
					idx.tanIndex = 0;

				if (_cIndex.size() > 0)
					idx.cIndex = _cIndex.get(cit);
				else
					idx.cIndex = 0;

				if (!pts.containsKey(idx)) {

					if (needsTriangles)
						_indices[2].push(pts.size());

					// since this one is a new vertex, check to see if this
					// position is already referenced
					if (needsPoints
							&& /* ptSet.find(idx.pIndex) != ptSet.end() */ptSet
									.contains(idx.pIndex)) {
						ptSet.add(idx.pIndex);
					}

					// pts.insert( map<IdxSet,GLuint>::value_type(idx,
					// (GLuint)pts.size()));
					pts.put(idx, pts.size());

					// position
					_vertices.push(_positions.get(idx.pIndex * _posSize));
					_vertices.push(_positions.get(idx.pIndex * _posSize + 1));
					_vertices.push(_positions.get(idx.pIndex * _posSize + 2));
					if (_posSize == 4)
						_vertices.push(_positions.get(idx.pIndex * _posSize + 3));

					// normal
					if (_normals.size() > 0) {
						_vertices.push(_normals.get(idx.nIndex * 3));
						_vertices.push(_normals.get(idx.nIndex * 3 + 1));
						_vertices.push(_normals.get(idx.nIndex * 3 + 2));
					}

					// texture coordinate
					if (_texCoords.size() > 0) {
						_vertices.push(_texCoords.get(idx.tIndex * _tcSize));
						_vertices.push(_texCoords.get(idx.tIndex * _tcSize + 1));
						if (_tcSize == 3)
							_vertices.push(_texCoords.get(idx.tIndex * _tcSize + 2));
					}

					// tangents
					if (_sTangents.size() > 0) {
						_vertices.push(_sTangents.get(idx.tanIndex * 3));
						_vertices.push(_sTangents.get(idx.tanIndex * 3 + 1));
						_vertices.push(_sTangents.get(idx.tanIndex * 3 + 2));
					}

					// colors
					if (_colors.size() > 0) {
						_vertices.push(_colors.get(idx.cIndex * _cSize));
						_vertices.push(_colors.get(idx.cIndex * _cSize + 1));
						_vertices.push(_colors.get(idx.cIndex * _cSize + 2));
						if (_cSize == 4)
							_vertices.push(_colors.get(idx.cIndex * _cSize + 3));
					}
				} else {
					if (needsTriangles)
						_indices[2].push(pts.get(idx));
				}

				// advance the iterators if the components are present
				pit++;

				if (hasNormals())
					nit++;

				if (hasTexCoords())
					tit++;

				if (hasTangents())
					tanit++;

				if (hasColors())
					cit++;
			}
		}

		// create an edge list, if necessary
		if (needsEdges || needsTrianglesWithAdj) {
			// std::multimap<Edge, GLuint> edges;
			HashMap<Edge, StackInt> edges = new HashMap<Edge, StackInt>();

			// edges are only based on positions only
			for (int ii = 0; ii < _pIndex.size(); ii += 3) {
				for (int jj = 0; jj < 3; jj++) {
					Edge w = new Edge(_pIndex.get(ii + jj), _pIndex.get(ii + (jj + 1) % 3));
					// std::multimap<Edge, GLuint>::iterator it = edges.find(w);
					StackInt it = edges.get(w);

					// if we are storing edges, make sure we store only one copy
					if (needsEdges && it == null) {
						_indices[1].push(_indices[2].get(ii + jj));
						_indices[1].push(_indices[2].get(ii + (jj + 1) % 3));
						it = new StackInt();
						edges.put(w, it);
					}
					// edges.insert( std::multimap<Edge, GLuint>::value_type( w,
					// ii / 3));
					it.push(ii / 3);
				}
			}

			// now handle triangles with adjacency
			if (needsTrianglesWithAdj) {
				for (int ii = 0; ii < _pIndex.size(); ii += 3) {
					for (int jj = 0; jj < 3; jj++) {
						Edge w = new Edge(_pIndex.get(ii + jj), _pIndex.get(ii + (jj + 1) % 3));
						// std::multimap<Edge, GLuint>::iterator it =
						// edges.lower_bound(w);
						// std::multimap<Edge, GLuint>::iterator limit =
						// edges.upper_bound(w);
						StackInt stack = edges.get(w);
						int adjVertex = 0;

						// while ( it != edges.end() && it->second == ii /3 &&
						// it != limit)
						int it = 0;
						for (; stack != null && it < stack.size() && stack.get(it) == ii / 3;)
							it++;

						if (stack == null || it == stack.size()) {
							// no adjacent triangle found, duplicate the vertex
							adjVertex = _indices[2].get(ii + jj);
							_openEdges++;

						} else {
							//compute the starting index of the triangle
							int triOffset = stack.get(it) * 3; 
							//set the vertex to a default, in case the adjacent triangle it a degenerate
							adjVertex = _indices[2].get(triOffset); 

							// find the unshared vertex
							for (int kk = 0; kk < 3; kk++) {
								if (_pIndex.get(triOffset + kk) != w.pIndex0 && _pIndex.get(triOffset + kk) != w.pIndex1) {
									adjVertex = _indices[2].get(triOffset + kk);
									break;
								}
							}
						}

						// store the vertices for this edge
						_indices[3].push(_indices[2].get(ii + jj));
						_indices[3].push(adjVertex);
					}
				}
			}

		}

		// create selected prim

		// set the offsets and vertex size
		_pOffset = 0; // always first
		_vtxSize = _posSize;
		if (hasNormals()) {
			_nOffset = _vtxSize;
			_vtxSize += 3;
		} else {
			_nOffset = -1;
		}
		if (hasTexCoords()) {
			_tcOffset = _vtxSize;
			_vtxSize += _tcSize;
		} else {
			_tcOffset = -1;
		}
		if (hasTangents()) {
			_sTanOffset = _vtxSize;
			_vtxSize += 3;
		} else {
			_sTanOffset = -1;
		}
		if (hasColors()) {
			_cOffset = _vtxSize;
			_vtxSize += _cSize;
		} else {
			_cOffset = -1;
		}
		
		// clear the heap cache.
		_positions = null;
		_normals   = null;
		_texCoords = null;
		_sTangents = null;
		_colors    = null;
		_pIndex    = null;
		_cIndex    = null;
		_nIndex    = null;
		_tanIndex  = null;
		_tIndex    = null;
	}

	/**
	 * This function returns the points defining the axis- aligned bounding box
	 * containing the model.
	 * 
	 * @param minVal
	 *            returned value to hold the low point of the bounding box.
	 * @param maxVal
	 *            returned value to hold the up point of the bounding box.
	 */
	public void computeBoundingBox(Vector3f minVal, Vector3f maxVal) {
		if ( _positions.isEmpty())
	        return;

	    minVal.set(1e10f, 1e10f, 1e10f);
	    maxVal.set(-1e10f, -1e10f, -1e10f);

	    for ( int pit = _posSize; pit < _positions.size(); pit += _posSize) {
	       min( minVal, _positions, pit);
	       max( maxVal, _positions, pit);
	    }
	}
	
	private static void min(Vector3f src, StackFloat data, int pos){
		float cx = data.get(pos);
		float cy = data.get(pos + 1);
		float cz = data.get(pos + 2);
		
		src.x = Math.min(src.x, cx);
		src.y = Math.min(src.y, cy);
		src.z = Math.min(src.z, cz);
	}
	
	private static void max(Vector3f src, StackFloat data, int pos){
		float cx = data.get(pos);
		float cy = data.get(pos + 1);
		float cz = data.get(pos + 2);
		
		src.x = Math.max(src.x, cx);
		src.y = Math.max(src.y, cy);
		src.z = Math.max(src.z, cz);
	}

	/**
	 * rescales object based on bounding box.
	 * 
	 * @param radius
	 */
	public void rescaleToOrigin(float radius) {
		if ( _positions.isEmpty())
	        return;

	    Vector3f minVal = new Vector3f(), maxVal = new Vector3f();
	    computeBoundingBox(minVal, maxVal);

//	    vec3f r = 0.5f*(maxVal - minVal);
	    Vector3f r = ((Vector3f) Vector3f.sub(maxVal, minVal, null)).scale(0.5f);
//	    vec3f center = minVal + r;
	    Vector3f center = Vector3f.add(minVal, r, null);
//	    float oldRadius = length(r);
	    float oldRadius = Math.max(r.x, Math.max(r.y, r.z));
	    float scale = radius / oldRadius;

	    for (int pit = 0; pit < _positions.size(); pit += _posSize) {
//	        vec3f np = scale*(vec3f(&pit[0]) - center);
	    	float x = scale * (_positions.get(pit) - center.x); 
	    	float y = scale * (_positions.get(pit + 1) - center.y); 
	    	float z = scale * (_positions.get(pit + 2) - center.z); 
//	        pit[0] = np.x;
//	        pit[1] = np.y;
//	        pit[2] = np.z;
	    	
	    	_positions.set(pit, x);
	    	_positions.set(pit + 1, y);
	    	_positions.set(pit + 2, z);
	    }
	}
	
	public void rescale(float radius) {
		if ( _positions.isEmpty())
	        return;

	    Vector3f minVal = new Vector3f(), maxVal = new Vector3f();
	    computeBoundingBox(minVal, maxVal);

//	    vec3f r = 0.5f*(maxVal - minVal);
	    Vector3f r = ((Vector3f) Vector3f.sub(maxVal, minVal, null)).scale(0.5f);
//	    vec3f center = minVal + r;
	    Vector3f center = Vector3f.add(minVal, r, null);
//	    float oldRadius = length(r);
	    float oldRadius = Math.max(r.x, Math.max(r.y, r.z));
	    float scale = radius / oldRadius;

	    for (int pit = 0; pit < _positions.size(); pit += _posSize) {
//	        vec3f np = scale*(vec3f(&pit[0]) - center);
	    	float x = center.x + scale * (_positions.get(pit) - center.x); 
	    	float y = center.y + scale * (_positions.get(pit + 1) - center.y); 
	    	float z = center.z + scale * (_positions.get(pit + 2) - center.z); 
//	        pit[0] = np.x;
//	        pit[1] = np.y;
//	        pit[2] = np.z;
	    	
	    	_positions.set(pit, x);
	    	_positions.set(pit + 1, y);
	    	_positions.set(pit + 2, z);
	    }
	}

	public void rescaleWithCenter(Vector3f center, Vector3f r, float radius){
		if ( _positions.isEmpty())
	        return;
		
		float oldRadius = Math.max(r.x, Math.max(r.y, r.z));
	    float scale = radius / oldRadius;

	    for (int pit = 0; pit < _positions.size(); pit += _posSize) {
//	        vec3f np = scale*(vec3f(&pit[0]) - center);
	    	float x = center.x + scale * (_positions.get(pit) - center.x); 
	    	float y = center.y + scale * (_positions.get(pit + 1) - center.y); 
	    	float z = center.z + scale * (_positions.get(pit + 2) - center.z); 
//	        pit[0] = np.x;
//	        pit[1] = np.y;
//	        pit[2] = np.z;
	    	
	    	_positions.set(pit, x);
	    	_positions.set(pit + 1, y);
	    	_positions.set(pit + 2, z);
	    }
	}
	
	public void addToAllPositions(Vector3f center){
		if ( _positions.isEmpty())
	        return;
		
		for (int pit = 0; pit < _positions.size(); pit += _posSize) {
//	        vec3f np = vec3f(&pit[0]) + center;
	    	float x = _positions.get(pit) + center.x; 
	    	float y = _positions.get(pit + 1) + center.y; 
	    	float z = _positions.get(pit + 2) + center.z; 
	    	
	    	_positions.set(pit, x);
	    	_positions.set(pit + 1, y);
	    	_positions.set(pit + 2, z);
	    }
	}
	
	/**
	 * This function computes tangents in the s direction on the model. It
	 * operates on the raw data, so it should only be used before compiling a
	 * model into a HW friendly form.
	 */
	public void computeTangents() {
		//make sure tangents don't already exist
	    if ( hasTangents()) 
	        return;

	    //make sure that the model has texcoords
	    if ( !hasTexCoords())
	        return;

	    //alloc memory and initialize to 0
//	    _tanIndex.reserve( _pIndex.size());
	    _sTangents.resize( (_texCoords.size() / _tcSize) * 3, 0.0f);

	    // the collision map records any alternate locations for the tangents
//	    std::multimap< GLuint, GLuint> collisionMap;
	    HashMap<Integer, StackInt> collisionMap = new HashMap<Integer, StackInt>();

	    //process each face, compute the tangent and try to add it
	    for (int ii = 0; ii < _pIndex.size(); ii += 3) {
//	        vec3f p0(&_positions[_pIndex[ii]*_posSize]);
	    	Vector3f p0 = vec3(_positions, _pIndex.get(ii) * _posSize);
//	        vec3f p1(&_positions[_pIndex[ii+1]*_posSize]);
	    	Vector3f p1 = vec3(_positions, _pIndex.get(ii + 1) * _posSize);
//	        vec3f p2(&_positions[_pIndex[ii+2]*_posSize]);
	    	Vector3f p2 = vec3(_positions, _pIndex.get(ii + 2) * _posSize);
//	        vec2f st0(&_texCoords[_tIndex[ii]*_tcSize]);
	    	Vector2f st0 = vec2(_texCoords, _tIndex.get(ii) * _tcSize);
//	        vec2f st1(&_texCoords[_tIndex[ii+1]*_tcSize]);
	    	Vector2f st1 = vec2(_texCoords, _tIndex.get(ii + 1) * _tcSize);
//	        vec2f st2(&_texCoords[_tIndex[ii+2]*_tcSize]);
	    	Vector2f st2 = vec2(_texCoords, _tIndex.get(ii + 2) * _tcSize);

	        //compute the edge and tc differentials
//	        vec3f dp0 = p1 - p0;
//	        vec3f dp1 = p2 - p0;
//	        vec2f dst0 = st1 - st0;
//	        vec2f dst1 = st2 - st0;
	    	Vector3f dp0 = Vector3f.sub(p1, p0, null);
	    	Vector3f dp1 = Vector3f.sub(p2, p0, null);
	    	Vector2f dst0 = Vector2f.sub(st1, st0, null);
	    	Vector2f dst1 = Vector2f.sub(st2, st0, null);

	        float factor = 1.0f / (dst0.x * dst1.y - dst1.x * dst0.y);

	        //compute sTangent
	        Vector3f sTan = new Vector3f();
	        sTan.x = dp0.x * dst1.y - dp1.x * dst0.y;
	        sTan.y = dp0.y * dst1.y - dp1.y * dst0.y;
	        sTan.z = dp0.z * dst1.y - dp1.z * dst0.y;
//	        sTan *= factor;
	        sTan.scale(factor);

	        //should this really renormalize?
//	        sTan =normalize( sTan);
	        sTan.normalise();

	        //loop over the vertices, to update the tangents
	        for (int jj = 0; jj < 3; jj++) {
	            //get the present accumulated tangnet
//	            vec3f curTan(&_sTangents[_tIndex[ii + jj]*3]);
	        	final int i = _tIndex.get(ii + jj);
	        	Vector3f curTan = vec3(_sTangents, i * 3);

	            //check to see if it is uninitialized, if so, insert it
	            if (curTan.x == 0.0f && curTan.y == 0.0f && curTan.z == 0.0f) {
//	                _sTangents[_tIndex[ii + jj]*3] = sTan[0];
	            	_sTangents.set(i*3, sTan.x);
//	                _sTangents[_tIndex[ii + jj]*3+1] = sTan[1];
	            	_sTangents.set(i*3 + 1, sTan.y);
//	                _sTangents[_tIndex[ii + jj]*3+2] = sTan[2];
	            	_sTangents.set(i*3 + 2, sTan.z);
//	                _tanIndex.push_back(_tIndex[ii + jj]);
	            	_tanIndex.push(i);
	            }
	            else {
	                //check for agreement
//	                curTan = normalize( curTan);
	            	curTan.normalise();

	                if ( Vector3f.dot( curTan, sTan) >= Math.cos( 3.1415926f * 0.333333f)) {
	                    //tangents are in agreement
//	                    _sTangents[_tIndex[ii + jj]*3] += sTan[0];
//	                    _sTangents[_tIndex[ii + jj]*3+1] += sTan[1];
//	                    _sTangents[_tIndex[ii + jj]*3+2] += sTan[2];
	                	_sTangents.plus(i*3, sTan.x);
	                	_sTangents.plus(i*3 + 1, sTan.y);
	                	_sTangents.plus(i*3 + 2, sTan.z);
//	                    _tanIndex.push_back(_tIndex[ii + jj]);
	                	_tanIndex.push(i);
	                }
	                else {
	                    //tangents disagree, this vertex must be split in tangent space 
//	                    std::multimap< GLuint, GLuint>::iterator it = collisionMap.find( _tIndex[ii + jj]);
	                	StackInt ints = collisionMap.get(i);
	                	int it = 0;

	                    //loop through all hits on this index, until one agrees
//	                    while ( it != collisionMap.end() && it->first == _tIndex[ii + jj]) {
//	                        curTan = vec3f( &_sTangents[it->second*3]);
//
//	                        curTan = normalize(curTan);
//	                        if ( dot( curTan, sTan) >= cosf( 3.1415926f * 0.333333f))
//	                            break;
//
//	                        it++;
//	                    }
	                	//loop through all hits on this index, until one agrees
	                    if(ints != null){
	                    	for(; it < ints.size();it++){
	                    		curTan = vec3(_sTangents, ints.get(it) * 3);
	                    		curTan.normalise();
	                    		if(Vector3f.dot(curTan, sTan) >= Math.cos(3.1415926f * 0.333333f))
	                    			break;
	                    	}
	                    }

	                    //check for agreement with an earlier collision
	                    if (ints!= null && it != ints.size()) {
	                        //found agreement with an earlier collision, use that one
//	                        _sTangents[it->second*3] += sTan[0];
//	                        _sTangents[it->second*3+1] += sTan[1];
//	                        _sTangents[it->second*3+2] += sTan[2];
//	                        _tanIndex.push_back(it->second);
	                        final int second = ints.get(it);      	
	                        _sTangents.plus(second * 3 + 0, sTan.x);
	                        _sTangents.plus(second * 3 + 1, sTan.y);
	                        _sTangents.plus(second * 3 + 2, sTan.z);
	                        _tanIndex.push(second);
	                    }
	                    else {
	                        //we have a new collision, create a new tangent
	                        int target = _sTangents.size() / 3;
	                        _sTangents.push( sTan.x);
	                        _sTangents.push( sTan.y);
	                        _sTangents.push( sTan.z);
	                        _tanIndex.push( target);
//	                        collisionMap.insert( std::multimap< GLuint, GLuint>::value_type( _tIndex[ii + jj], target));
	                        if(ints == null){
	                        	ints = new StackInt();
	                        	collisionMap.put(i, ints);
	                        }
	                        ints.push(target);
	                    }
	                } // else ( if tangent agrees)
	            } // else ( if tangent is uninitialized )
	        } // for jj = 0 to 2 ( iteration of triangle verts)
	    } // for ii = 0 to numFaces *3 ( iterations over triangle faces

	    //normalize all the tangents
	    for (int ii = 0; ii < _sTangents.size(); ii += 3) {
//	        vec3f tan(&_sTangents[ii]);
	    	Vector3f tan = vec3(_sTangents,ii);
	    	tan.normalise();
	    	
//	        _sTangents[ii] = tan[0];
//	        _sTangents[ii+1] = tan[1];
//	        _sTangents[ii+2] = tan[2];
	        
	        _sTangents.set(ii, tan.x);
	        _sTangents.set(ii + 1, tan.y);
	        _sTangents.set(ii + 2, tan.z);
	    }
	}
	
	private final static Vector3f vec3(StackFloat stack, int pos){
		return new Vector3f(stack.get(pos), stack.get(pos + 1), stack.get(pos + 2));
	}
	
	private final static Vector2f vec2(StackFloat stack, int pos){
		return new Vector2f(stack.get(pos), stack.get(pos + 1));
	}

	/**
	 * This function computes vertex normals for a model which did not have
	 * them. It computes them on the raw data, so it should be done before
	 * compiling the model into a HW friendly format.
	 */
	public void computeNormals() {
		// don't recompute normals
	    if (hasNormals())
	        return;

	    //allocate and initialize the normal values
	    _normals.resize( (_positions.size() / _posSize) * 3, 0.0f);
//	    _nIndex.reserve( _pIndex.size());

	    // the collision map records any alternate locations for the normals
	    HashMap<Integer, StackInt> collisionMap = new HashMap<Integer, StackInt>();

	    //iterate over the faces, computing the face normal and summing it them
	    for ( int ii = 0; ii < _pIndex.size(); ii += 3) {
//	        vec3f p0(&_positions[_pIndex[ii]*_posSize]);
//	        vec3f p1(&_positions[_pIndex[ii+1]*_posSize]);
//	        vec3f p2(&_positions[_pIndex[ii+2]*_posSize]);
	    	Vector3f p0 = vec3(_positions, _pIndex.get(ii) * _posSize);
	    	Vector3f p1 = vec3(_positions, _pIndex.get(ii+1) * _posSize);
	    	Vector3f p2 = vec3(_positions, _pIndex.get(ii+2) * _posSize);

	        //compute the edge vectors
//	        vec3f dp0 = p1 - p0;
//	        vec3f dp1 = p2 - p0;
	    	Vector3f dp0 = Vector3f.sub(p1, p0, null);
	    	Vector3f dp1 = Vector3f.sub(p2, p0, null);

//	        vec3f fNormal = cross( dp0, dp1); // compute the face normal
//	        vec3f nNormal = normalize(fNormal);  // compute a normalized normal
	    	Vector3f fNormal = Vector3f.cross(dp0, dp1, null);
	    	Vector3f nNormal = fNormal.normalise(null);

	        //iterate over the vertices, adding the face normal influence to each
	        for ( int jj = 0; jj < 3; jj++) {
	        	final int i = _pIndex.get(ii + jj);
	            // get the current normal from the default location (index shared with position) 
//	            vec3f cNormal( &_normals[_pIndex[ii + jj]*3]);
	        	Vector3f cNormal = vec3(_normals, i * 3);

	            // check to see if this normal has not yet been touched 
	            if ( cNormal.x == 0.0f && cNormal.y == 0.0f && cNormal.z == 0.0f) {
	                // first instance of this index, just store it as is
//	                _normals[_pIndex[ii + jj]*3] = fNormal[0];
//	                _normals[_pIndex[ii + jj]*3 + 1] = fNormal[1];
//	                _normals[_pIndex[ii + jj]*3 + 2] = fNormal[2];
//	                _nIndex.push_back( _pIndex[ii + jj]); 
	            	
	            	_normals.set(i * 3, fNormal.x);
	            	_normals.set(i * 3 + 1, fNormal.y);
	            	_normals.set(i * 3 + 2, fNormal.z);
	            	_nIndex.push(i);
	            }
	            else {
	                // check for agreement
//	                cNormal = normalize( cNormal);
	            	cNormal.normalise();

	                if ( Vector3f.dot( cNormal, nNormal) >= Math.cos( 3.1415926f * 0.333333f)) {
	                    //normal agrees, so add it
//	                    _normals[_pIndex[ii + jj]*3] += fNormal[0];
//	                    _normals[_pIndex[ii + jj]*3 + 1] += fNormal[1];
//	                    _normals[_pIndex[ii + jj]*3 + 2] += fNormal[2];
//	                    _nIndex.push_back( _pIndex[ii + jj]);
	                    
	                    _normals.plus(i * 3, fNormal.x);
		            	_normals.plus(i * 3 + 1, fNormal.y);
		            	_normals.plus(i * 3 + 2, fNormal.z);
		            	_nIndex.push(i);
	                }
	                else {
	                    //normals disagree, this vertex must be along a facet edge 
//	                    std::multimap< GLuint, GLuint>::iterator it = collisionMap.find( _pIndex[ii + jj]);
//
//	                    //loop through all hits on this index, until one agrees
//	                    while ( it != collisionMap.end() && it->first == _pIndex[ii + jj]) {
//	                        cNormal = normalize(vec3f( &_normals[it->second*3]));
//
//	                        if ( dot( cNormal, nNormal) >= cosf( 3.1415926f * 0.333333f))
//	                            break;
//
//	                        it++;
//	                    }
	                    
	                	StackInt ints = collisionMap.get(i);
	                	int it = 0;
	                  //loop through all hits on this index, until one agrees
	                    if(ints != null){
	                    	for(; it < ints.size();it++){
	                    		cNormal = vec3(_normals, ints.get(it) * 3);
	                    		cNormal.normalise();
	                    		
	                    		if(Vector3f.dot(cNormal, nNormal) >= Math.cos(3.1415926f * 0.333333f))
	                    			break;
	                    	}
	                    }


	                    //check for agreement with an earlier collision
	                    if (ints!= null && it != ints.size()) {
	                        //found agreement with an earlier collision, use that one
//	                        _normals[it->second*3] += fNormal[0];
//	                        _normals[it->second*3+1] += fNormal[1];
//	                        _normals[it->second*3+2] += fNormal[2];
//	                        _nIndex.push_back(it->second);
	                        
	                    	final int second = ints.get(it);
	                        _normals.plus(second * 3, fNormal.x);
			            	_normals.plus(second * 3 + 1, fNormal.y);
			            	_normals.plus(second * 3 + 2, fNormal.z);
			            	_nIndex.push(second);
	                    }
	                    else {
	                        //we have a new collision, create a new normal
	                        int target = _normals.size() / 3;
	                        _normals.push( fNormal.x);
	                        _normals.push( fNormal.y);
	                        _normals.push( fNormal.z);
	                        _nIndex.push( target);
//	                        collisionMap.insert( std::multimap< GLuint, GLuint>::value_type( _pIndex[ii + jj], target));
	                        if(ints == null){
	                        	ints = new StackInt();
	                        	collisionMap.put(i, ints);
	                        }
	                        
	                        ints.push(target);
	                    }
	                } // else ( if normal agrees)
	            } // else (if normal is uninitialized)
	        } // for each vertex in triangle
	    } // for each face

	    //now normalize all the normals
	    for ( int ii = 0; ii < _normals.size(); ii += 3) {
//	        vec3f norm(&_normals[ii]);
//	        norm =normalize(norm);
//	        _normals[ii] = norm[0];
//	        _normals[ii+1] = norm[1];
//	        _normals[ii+2] = norm[2];
	    	
	    	Vector3f norm = vec3(_normals, ii);
	    	norm.normalise();
	    	
	    	_normals.set(ii, norm.x);
	    	_normals.set(ii + 1, norm.y);
	    	_normals.set(ii + 2, norm.z);
	    }
	}

	public void removeDegeneratePrims() {
		int pSrc = 0, pDst = 0, tSrc = 0, tDst = 0, nSrc = 0, nDst = 0, cSrc = 0, cDst = 0;
	    int degen = 0;
	    
	    int[] _pIndex = this._pIndex.getData();
	    int[] _tIndex = this._tIndex.getData();
	    int[] _nIndex = this._nIndex.getData();
	    int[] _cIndex = this._cIndex.getData();
	    
	    for (int ii = 0; ii < this._pIndex.size(); ii += 3, pSrc += 3, tSrc += 3, nSrc += 3, cSrc += 3) {
	        if ( _pIndex[pSrc] == _pIndex[pSrc + 1] || _pIndex[pSrc] == _pIndex[pSrc + 2] || _pIndex[pSrc + 1] == _pIndex[pSrc + 2]) {
	            degen++;
	            continue; //skip updating the dest
	        }

	        for (int jj = 0; jj < 3; jj++) {
//	            *pDst++ = pSrc[jj];
	            _pIndex[pDst++] = _pIndex[pSrc + jj];

	            if (hasTexCoords())
//	                *tDst++ = tSrc[jj];
	            	 _tIndex[tDst++] = _tIndex[tSrc + jj];

	            if (hasNormals())
//	                *nDst++ = nSrc[jj];
	            	 _nIndex[nDst++] = _nIndex[nSrc + jj];

	            if (hasColors())
//	                *cDst++ = cSrc[jj];
	            	 _cIndex[cDst++] = _cIndex[cSrc + jj];
	        }
	    }

	    this._pIndex.resize( this._pIndex.size() - degen * 3);

	    if (hasTexCoords())
	    	this._tIndex.resize( this._tIndex.size() - degen * 3);

	    if (hasNormals())
	    	this._nIndex.resize( this._nIndex.size() - degen * 3);

	    if (hasColors())
	    	this._cIndex.resize( this._cIndex.size() - degen * 3);
	}

	public boolean hasNormals() {
		return !_normals.isEmpty();
	}

	public boolean hasTexCoords() {
		return _texCoords.size() > 0;
	}

	public boolean hasTangents() {
		return _sTangents.size() > 0;
	}

	public boolean hasColors() {
		return _colors.size() > 0;
	}

	public int getPositionSize() {
		return _posSize;
	}

	public int getNormalSize() {
		return 3;
	}

	public int getTexCoordSize() {
		return _tcSize;
	}

	public int getTangentSize() {
		return 3;
	}

	public int getColorSize() {
		return _cSize;
	}

	//
	// Functions for the management of raw data
	//
	public void clearNormals() {
		_normals.clear();
		_nIndex.clear();
	}

	public void clearTexCoords() {
		_texCoords.clear();
		_tIndex.clear();
	}

	public void clearTangents() {
		_sTangents.clear();
		 _tanIndex.clear();
	}

	public void clearColors() {
		_colors.clear();
		 _cIndex.clear();
	}

	//
	// raw data access functions
	// These are to be used to get the raw array data from the file, each array
	// has its own index
	//
	public float[] getPositions() {
		return (_positions.size() > 0) ? (_positions.getData()) : null;
	}

	public float[] getNormals() {
		return (_normals.size() > 0) ? (_normals.getData()) : null;
	}

	public float[] getTexCoords() {
		return (_texCoords.size() > 0) ? (_texCoords.getData()) : null;
	}

	public float[] getTangents() {
		return (_sTangents.size() > 0) ? (_sTangents.getData()) : null;
	}

	public float[] getColors() {
		return (_colors.size() > 0) ? (_colors.getData()) : null;
	}

	public int[] getPositionIndices() {
		return (_pIndex.size() > 0) ? (_pIndex.getData()) : null;
	}

	public int[] getNormalIndices() {
		return (_nIndex.size() > 0) ? (_nIndex.getData()) : null;
	}

	public int[] getTexCoordIndices() {
		return (_tIndex.size() > 0) ? (_tIndex.getData()) : null;
	}

	public int[] getTangentIndices() {
		return (_tanIndex.size() > 0) ? (_tanIndex.getData()) : null;
	}

	public int[] getColorIndices() {
		return (_cIndex.size() > 0) ? (_cIndex.getData()) : null;
	}

	public int getPositionCount() {
		return (_posSize > 0) ? _positions.size() / _posSize : 0;
	}

	public int getNormalCount() {
		return _normals.size() / 3;
	}

	public int getTexCoordCount() {
		return (_tcSize > 0) ? _texCoords.size() / _tcSize : 0;
	}

	public int getTangentCount() {
		return _sTangents.size() / 3;
	}

	public int getColorCount() {
		return (_cSize > 0) ? _colors.size() / _cSize : 0;
	}

	public int getIndexCount() {
		return _pIndex.size();
	}

	//
	// compiled data access functions
	//
	public float[] getCompiledVertices() {
		return (_vertices.size() > 0) ? _vertices.getData() : null;
	}

	public int[] getCompiledIndices() {
		return getCompiledIndices(TRIANGLES);
	}

	public int[] getCompiledIndices(int prim) {
		switch (prim) {
		case POINTS:
			return (_indices[0].size() > 0) ? _indices[0].getData() : null;
		case EDGES:
			return (_indices[1].size() > 0) ? _indices[1].getData() : null;
		case TRIANGLES:
			return (_indices[2].size() > 0) ? _indices[2].getData() : null;
		case TRIANGLES_WITH_ADJACENCY:
			return (_indices[3].size() > 0) ? _indices[3].getData() : null;
		}
		return null;
	}

	public int getCompiledPositionOffset() {
		return _pOffset;
	}

	public int getCompiledNormalOffset() {
		return _nOffset;
	}

	public int getCompiledTexCoordOffset() {
		return _tcOffset;
	}

	public int getCompiledTangentOffset() {
		return _sTanOffset;
	}

	public int getCompiledColorOffset() {
		return _cOffset;
	}

	// returns the size of the merged vertex in # of floats
	public int getCompiledVertexSize() {
		return _vtxSize;
	}

	public int getCompiledVertexCount() {
		return (_vtxSize > 0) ? _vertices.size() / _vtxSize : 0;
	}

	public int getCompiledIndexCount() {
		return getCompiledIndexCount(TRIANGLES);
	}

	public int getCompiledIndexCount(int prim) {
		switch (prim) {
		case POINTS:
			return _indices[0].size();
		case EDGES:
			return _indices[1].size();
		case TRIANGLES:
			return _indices[2].size();
		case TRIANGLES_WITH_ADJACENCY:
			return _indices[3].size();
		}

		return 0;
	}
	
	public int getOpenEdgeCount() {
		return _openEdges;
	}

	/** Index gathering and ordering structure */
	private static final class IdxSet implements Comparable<IdxSet> {
		int pIndex;
		int nIndex;
		int tIndex;
		int tanIndex;
		int cIndex;

		@Override
		public int compareTo(IdxSet rhs) {
			if (pIndex < rhs.pIndex)
				return -1;
			else if (pIndex == rhs.pIndex) {
				if (nIndex < rhs.nIndex)
					return -1;
				else if (nIndex == rhs.nIndex) {

					if (tIndex < rhs.tIndex)
						return -1;
					else if (tIndex == rhs.tIndex) {
						if (tanIndex < rhs.tanIndex)
							return -1;
						else if (tanIndex == rhs.tanIndex)
							return (cIndex < rhs.cIndex) ? -1 : 1;
					}
				}
			}

			return 1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + cIndex;
			result = prime * result + nIndex;
			result = prime * result + pIndex;
			result = prime * result + tIndex;
			result = prime * result + tanIndex;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			IdxSet other = (IdxSet) obj;
			if (cIndex != other.cIndex)
				return false;
			if (nIndex != other.nIndex)
				return false;
			if (pIndex != other.pIndex)
				return false;
			if (tIndex != other.tIndex)
				return false;
			if (tanIndex != other.tanIndex)
				return false;
			return true;
		}
	}

	/** Edge connectivity structure */
	private static final class Edge implements Comparable<Edge> {
		int pIndex0;
		int pIndex1;

		public Edge(int v0, int v1) {
			pIndex0 = Math.min(v0, v1);
			pIndex1 = Math.max(v0, v1);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + pIndex0;
			result = prime * result + pIndex1;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			Edge other = (Edge) obj;
			if (pIndex0 != other.pIndex0)
				return false;
			if (pIndex1 != other.pIndex1)
				return false;
			return true;
		}

		@Override
		public int compareTo(Edge rhs) {
			boolean b = (pIndex0 == rhs.pIndex0) ? (pIndex1 < rhs.pIndex1)
					: pIndex0 < rhs.pIndex0;
			return b ? -1 : 1;
		}
	}
}
