package assimp.importer.ase;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssimpConfig;
import assimp.common.DefaultLogger;
import assimp.common.IntFloatPair;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.ParsingUtil;
import assimp.common.QuatKey;
import assimp.common.VectorKey;
import assimp.importer.d3ds.D3DSHelper;
import assimp.importer.d3ds.D3DSTexture;

// 1817
/** Class to parse ASE files */
final class ASEParser {

	static final int AI_ASE_NEW_FILE_FORMAT = 200;
	static final int AI_ASE_OLD_FILE_FORMAT = 110;
	
	//! Pointer to current data
	ParsingUtil filePtr;

	//! background color to be passed to the viewer
	//! QNAN if none was found
	final Vector3f m_clrBackground = new Vector3f();

	//! Base ambient color to be passed to all materials
	//! QNAN if none was found
	final Vector3f m_clrAmbient = new Vector3f();

	//! List of all materials found in the file
	final List<ASEMaterial> m_vMaterials = new ArrayList<ASEMaterial>();

	//! List of all meshes found in the file
	final List<ASEMesh> m_vMeshes = new ArrayList<ASEMesh>();

	//! List of all dummies found in the file
	final List<Dummy> m_vDummies = new ArrayList<Dummy>();

	//! List of all lights found in the file
	final List<ASELight> m_vLights = new ArrayList<ASELight>();

	//! List of all cameras found in the file
	final List<ASECamera> m_vCameras = new ArrayList<ASECamera>();

	//! Current line in the file
	int iLineNumber;

	//! First frame
	int iFirstFrame;

	//! Last frame
	int iLastFrame;

	//! Frame speed - frames per second
	int iFrameSpeed;

	//! Ticks per frame
	int iTicksPerFrame;

	//! true if the last character read was an end-line character
	boolean bLastWasEndLine;

	//! File format version
	int iFileFormat;
	
	String parsedString;
	
	final int[] parsedLongTrip = new int[3];
	final float[] parsedFloatTrip = new float[3];
	
	// -------------------------------------------------------------------
	//! Construct a parser from a given input file which is
	//! guaranted to be terminated with zero.
	//! @param szFile Input file
	//! @param fileFormatDefault Assumed file format version. If the
	//!   file format is specified in the file the new value replaces
	//!   the default value.
	ASEParser (ByteBuffer szFile, int fileFormatDefault){
		filePtr = new ParsingUtil(szFile);
		iFileFormat = fileFormatDefault;

		// make sure that the color values are invalid
		m_clrBackground.x = Float.NaN;
		m_clrAmbient.y    = Float.NaN;

		// setup some default values
		iLineNumber = 0;
		iFirstFrame = 0;
		iLastFrame = 0;
		iFrameSpeed = 30;        // use 30 as default value for this property
		iTicksPerFrame = 1;      // use 1 as default value for this property
		bLastWasEndLine = false; // need to handler\n seqs due to binary file mapping
	}

	// -------------------------------------------------------------------
	//! Parses the file into the parsers internal representation
	void parse(){
		int iDepth = 0;
		int c;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Version should be 200. Validate this ...
				if (filePtr.tokenMatch("3DSMAX_ASCIIEXPORT"))//(TokenMatch(filePtr,"3DSMAX_ASCIIEXPORT",18))
				{
					int fmt = parseLV4MeshLong();

					if (fmt > 200)
					{
						logWarning("Unknown file format version: *3DSMAX_ASCIIEXPORT should be <= 200");
					}
					// *************************************************************
					// - fmt will be 0 if we're unable to read the version number
					// there are some faulty files without a version number ...
					// in this case we'll guess the exact file format by looking
					// at the file extension (ASE, ASK, ASC)
					// *************************************************************

					if (fmt != 0)iFileFormat = fmt;
					continue;
				}
				// main scene information
				if (filePtr.tokenMatch("SCENE"))//(TokenMatch(filePtr,"SCENE",5))
				{
					parseLV1SceneBlock();
					continue;
				}
				// "group" - no implementation yet, in facte
				// we're just ignoring them for the moment
				if (filePtr.tokenMatch("GROUP"))//(TokenMatch(filePtr,"GROUP",5)) 
				{
					parse();
					continue;
				}
				// material list
				if (filePtr.tokenMatch("MATERIAL_LIST"))//(TokenMatch(filePtr,"MATERIAL_LIST",13)) 
				{
					parseLV1MaterialListBlock();
					continue;
				}
				// geometric object (mesh)
				if (filePtr.tokenMatch("GEOMOBJECT"))//(TokenMatch(filePtr,"GEOMOBJECT",10)) 
					
				{
					ASEMesh mesh = new ASEMesh();
					m_vMeshes.add(mesh);
					parseLV1ObjectBlock(mesh);
					continue;
				}
				// helper object = dummy in the hierarchy
				if (filePtr.tokenMatch("HELPEROBJECT"))//(TokenMatch(filePtr,"HELPEROBJECT",12)) 
				{
					Dummy dummy = new Dummy();
					m_vDummies.add(dummy);
					parseLV1ObjectBlock(dummy);
					continue;
				}
				// light object
				if (filePtr.tokenMatch("LIGHTOBJECT"))//(TokenMatch(filePtr,"LIGHTOBJECT",11)) 
					
				{
					ASELight light;
					m_vLights.add(light = new ASELight());
					parseLV1ObjectBlock(light);
					continue;
				}
				// camera object
				if (filePtr.tokenMatch("CAMERAOBJECT"))//(TokenMatch(filePtr,"CAMERAOBJECT",12)) 
				{
					ASECamera camera;
					m_vCameras.add(camera = new ASECamera());
					parseLV1ObjectBlock(camera);
					continue;
				}
				// comment - print it on the console
				if (filePtr.tokenMatch("COMMENT"))//(TokenMatch(filePtr,"COMMENT",7)) 
				{
					String out = "<unknown>";
					parseString("*COMMENT");
					out = parsedString;
					logInfo(("Comment: " + out));
					continue;
				}
				// ASC bone weights
				if ((iFileFormat < 200) && filePtr.tokenMatch("MESH_SOFTSKINVERTS") /*TokenMatch(filePtr,"MESH_SOFTSKINVERTS",18)*/) 
				{
					parseLV1SoftSkinBlock();
				}
			}
			else if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				return;
			}
			
			c = filePtr.get();
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
			{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}
	
	// -------------------------------------------------------------------
	//! Parse the *SCENE block in a file
	void parseLV1SceneBlock(){
		int iDepth = 0;
		int c;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				if (filePtr.tokenMatch("SCENE_BACKGROUND_STATIC"))//(TokenMatch(filePtr,"SCENE_BACKGROUND_STATIC",23)) 
					
				{
					// parse a color triple and assume it is really the bg color
					parseLV4MeshFloatTriple(m_clrBackground );
					continue;
				}
				if (filePtr.tokenMatch("SCENE_AMBIENT_STATIC"))//(TokenMatch(filePtr,"SCENE_AMBIENT_STATIC",20)) 
					
				{
					// parse a color triple and assume it is really the bg color
					parseLV4MeshFloatTriple(m_clrAmbient );
					continue;
				}
				if (filePtr.tokenMatch("SCENE_FIRSTFRAME"))//(TokenMatch(filePtr,"SCENE_FIRSTFRAME",16)) 
				{
					iFirstFrame = parseLV4MeshLong();
					continue;
				}
				if (filePtr.tokenMatch("SCENE_LASTFRAME"))//(TokenMatch(filePtr,"SCENE_LASTFRAME",15))
				{
					iLastFrame = parseLV4MeshLong();
					continue;
				}
				if (filePtr.tokenMatch("SCENE_FRAMESPEED"))//(TokenMatch(filePtr,"SCENE_FRAMESPEED",16)) 
				{
					iFrameSpeed = parseLV4MeshLong();
					continue;
				}
				if (filePtr.tokenMatch("SCENE_TICKSPERFRAME"))//(TokenMatch(filePtr,"SCENE_TICKSPERFRAME",19))
				{
					iTicksPerFrame = parseLV4MeshLong();
					continue;
				}
			}
			else if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth) 
				{ 
//					++filePtr; 
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				return;
			}
			
			c = filePtr.get();
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
			{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse the *MESH_SOFTSKINVERTS block in a file
	void parseLV1SoftSkinBlock(){
		// TODO: fix line counting here

		// **************************************************************
		// The soft skin block is formatted differently. There are no
		// nested sections supported and the single elements aren't
		// marked by keywords starting with an asterisk.

		/** 
		FORMAT BEGIN

		*MESH_SOFTSKINVERTS {
		<nodename>
		<number of vertices>

		[for <number of vertices> times:]
			<number of weights>	[for <number of weights> times:] <bone name> <weight>
		}

		FORMAT END 
		*/
		// **************************************************************
		int c ;
		while (true)
		{
			c = filePtr.get();
			if      (c == '}' )	{/*++filePtr*/ filePtr.inCre();return;}
			else if (c == '\0')	return;
			else if (c == '{' )	{/*++filePtr*/ filePtr.inCre();}

			else // if (!IsSpace(*filePtr) && !IsLineEnd(*filePtr))
			{
				ASEMesh curMesh		= null;
				int numVerts	    = 0;

//				const char* sz = filePtr;
				final int sz = filePtr.getCurrent();
				int cursor = sz;
//				while (!isSpaceOrNewLine(*filePtr))++filePtr;
				while (!ParsingUtil.isSpaceOrNewLine((byte)filePtr.get(cursor))) cursor++;

				final int diff = (cursor-sz);
				if (diff > 0)
				{
//					std::string name = std::string(sz,diff);
					String name = filePtr.getString(sz, cursor);
//					for (std::vector<ASE::Mesh>::iterator it = m_vMeshes.begin();
//						it != m_vMeshes.end(); ++it)
					for(ASEMesh it : m_vMeshes)
					{
						if (it.mName.equals(name))
						{
							curMesh = it;
							break;
						}
					}
					if (curMesh == null)
					{
						logWarning("Encountered unknown mesh in *MESH_SOFTSKINVERTS section");

						// Skip the mesh data - until we find a new mesh
						// or the end of the *MESH_SOFTSKINVERTS section
						while (true)
						{
//							SkipSpacesAndLineEnd(&filePtr);
							filePtr.skipSpacesAndLineEnd();
							c = filePtr.get();
							if (c == '}')
								{/*++filePtr*/ filePtr.inCre();return;}
							else if (!filePtr.isNumeric())
								break;

//							SkipLine(&filePtr);
							filePtr.skipLine();
						}
					}
					else
					{
						filePtr.skipSpacesAndLineEnd();
						numVerts = parseLV4MeshLong();

						// Reserve enough storage
//						curMesh->mBoneVertices.reserve(numVerts);

						for (int i = 0; i < numVerts;++i)
						{
							filePtr.skipSpacesAndLineEnd();
							int numWeights = parseLV4MeshLong();

							BoneVertex vert = new BoneVertex();
							curMesh.mBoneVertices.add(vert);
//							ASE::BoneVertex& vert = curMesh->mBoneVertices.back();

							// Reserve enough storage
//							vert.mBoneWeights.reserve(numWeights);

							for (int w = 0; w < numWeights;++w)
							{
								parseString("*MESH_SOFTSKINVERTS.Bone");
								String bone = parsedString;

								// Find the bone in the mesh's list
//								std::pair<int,float> me;
								IntFloatPair me = new IntFloatPair();
								me.first = -1;
								
								for (int n = 0; n < curMesh.mBones.size();++n)
								{
									if (curMesh.mBones.get(n).mName.equals(bone))
									{
										me.first = n;
										break;
									}
								}
								if (-1 == me.first)
								{
									// We don't have this bone yet, so add it to the list
									me.first = curMesh.mBones.size();
									curMesh.mBones.add(new ASEBone(bone));
								}
								 me.second = parseLV4MeshFloat( );

								// Add the new bone weight to list
								vert.mBoneWeights.add(me);
							}
						}
					}
				}
			}
//			++filePtr;
			filePtr.inCre();
			filePtr.skipSpacesAndLineEnd();
		}
	}

	// -------------------------------------------------------------------
	//! Parse the *MATERIAL_LIST block in a file
	void parseLV1MaterialListBlock(){
		int iDepth = 0;
		
		int iMaterialCount = 0;
		int iOldMaterialCount = m_vMaterials.size();
		int c;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				if (filePtr.tokenMatch("MATERIAL_COUNT"))//(TokenMatch(filePtr,"MATERIAL_COUNT",14))
				{
					iMaterialCount = parseLV4MeshLong();

					// now allocate enough storage to hold all materials
//					m_vMaterials.resize(iOldMaterialCount+iMaterialCount);
					int count = iOldMaterialCount+iMaterialCount - m_vMaterials.size();
					for(int i = 0; i < count; i++)
						m_vMaterials.add(new ASEMaterial());
					continue;
				}
				if (filePtr.tokenMatch("MATERIAL"))//(TokenMatch(filePtr,"MATERIAL",8))
				{
					int iIndex = 0;
					iIndex = parseLV4MeshLong();

					if (iIndex >= iMaterialCount)
					{
						logWarning("Out of range: material index is too large");
						iIndex = iMaterialCount-1;
					}

					// get a reference to the material
					ASEMaterial sMat = m_vMaterials.get(iIndex+iOldMaterialCount);
					// parse the material block
					parseLV2MaterialBlock(sMat);
					continue;
				}
			}
//			AI_ASE_HANDLE_TOP_LEVEL_SECTION();
			else if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth) 
				{ 
//					++filePtr; 
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				return;
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
			{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *<xxx>OBJECT block in a file
	//!param mesh Node to be filled
	void parseLV1ObjectBlock(BaseNode node){
		int iDepth = 0;
		int c;
		
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// first process common tokens such as node name and transform
				// name of the mesh/node
				if (filePtr.tokenMatch("NODE_NAME"))//(TokenMatch(filePtr,"NODE_NAME" ,9))
				{
					if(!parseString("*NODE_NAME"))
						skipToNextToken();
					else
						node.mName = parsedString;
					continue;
				}
				// name of the parent of the node
				if (filePtr.tokenMatch("NODE_PARENT"))//(TokenMatch(filePtr,"NODE_PARENT" ,11) )
				{
					if(!parseString("*NODE_PARENT"))
						skipToNextToken();
					else
						node.mParent = parsedString;
					continue;
				}
				// transformation matrix of the node
				if (filePtr.tokenMatch("NODE_TM"))//(TokenMatch(filePtr,"NODE_TM" ,7))
				{
					parseLV2NodeTransformBlock(node);
					continue;
				}
				// animation data of the node
				if (filePtr.tokenMatch("TM_ANIMATION"))//(TokenMatch(filePtr,"TM_ANIMATION" ,12))
				{
					parseLV2AnimationBlock(node);
					continue;
				}

				if (node.mType == BaseNode.LIGHT)
				{
					// light settings
					if (filePtr.tokenMatch("LIGHT_SETTINGS"))//(TokenMatch(filePtr,"LIGHT_SETTINGS" ,14))
					{
						parseLV2LightSettingsBlock((ASELight)node);
						continue;
					}
					// type of the light source
					if (filePtr.tokenMatch("LIGHT_TYPE"))//(TokenMatch(filePtr,"LIGHT_TYPE" ,10))
					{
//						if (!ASSIMP_strincmp("omni",filePtr,4))
						if(!filePtr.strncmp("omni"))
						{
							((ASELight)node).mLightType = ASELight.OMNI;
						}
						else if (!filePtr.strncmp("target")) //(!ASSIMP_strincmp("target",filePtr,6))
						{
							((ASELight)node).mLightType = ASELight.TARGET;
						}
						else if (!filePtr.strncmp("free")) //(!ASSIMP_strincmp("free",filePtr,4))
						{
							((ASELight)node).mLightType = ASELight.FREE;
						}
						else if (!filePtr.strncmp("directional")) //(!ASSIMP_strincmp("directional",filePtr,11))
						{
							((ASELight)node).mLightType = ASELight.DIRECTIONAL;
						}
						else
						{
							if(DefaultLogger.LOG_OUT)
								logWarning("Unknown kind of light source");
						}
						continue;
					}
				}
				else if (node.mType == BaseNode.CAMERA)
				{
					// Camera settings
					if (filePtr.tokenMatch("CAMERA_SETTINGS"))//(TokenMatch(filePtr,"CAMERA_SETTINGS" ,15))
					{
						parseLV2CameraSettingsBlock((ASECamera)node);
						continue;
					}
					else if (filePtr.tokenMatch("CAMERA_TYPE"))//(TokenMatch(filePtr,"CAMERA_TYPE" ,11))
					{
						if (!filePtr.strncmp("target")) //(!ASSIMP_strincmp("target",filePtr,6))
						{
							((ASECamera)node).mCameraType = ASECamera.TARGET;
						}
						else if (!filePtr.strncmp("free")) //(!ASSIMP_strincmp("free",filePtr,4))
						{
							((ASECamera)node).mCameraType = ASECamera.FREE;
						}
						else
						{
							if(DefaultLogger.LOG_OUT)
								logWarning("Unknown kind of camera");
						}
						continue;
					}
				}
				else if (node.mType == BaseNode.MESH)
				{
					// mesh data
					// FIX: Older files use MESH_SOFTSKIN
					if (filePtr.tokenMatch("MESH")/*(TokenMatch(filePtr,"MESH" ,4)*/ || 
						filePtr.tokenMatch("MESH_SOFTSKIN")/*TokenMatch(filePtr,"MESH_SOFTSKIN",13)*/)
					{
						parseLV2MeshBlock((ASEMesh)node);
						continue;
					}
					// mesh material index
					if (filePtr.tokenMatch("MATERIAL_REF"))//(TokenMatch(filePtr,"MATERIAL_REF" ,12))
					{
						((ASEMesh)node).iMaterialIndex = parseLV4MeshLong();
						continue;
					}
				}
			}
//			AI_ASE_HANDLE_TOP_LEVEL_SECTION();
			else if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth) 
				{ 
//					++filePtr; 
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				return;
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
			{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MATERIAL blocks in a material list
	//!param mat Material structure to be filled
	void parseLV2MaterialBlock(ASEMaterial mat){
		int iDepth = 0;
		
		int iNumSubMaterials = 0;
		int c;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				if (filePtr.tokenMatch("MATERIAL_NAME"))//(TokenMatch(filePtr,"MATERIAL_NAME",13))
				{
					if (!parseString("*MATERIAL_NAME"))
						skipToNextToken();
					else
						mat.mName = parsedString;
					continue;
				}
				// ambient material color
				if (filePtr.tokenMatch("MATERIAL_AMBIENT"))//(TokenMatch(filePtr,"MATERIAL_AMBIENT",16))
				{
					parseLV4MeshFloatTriple(mat.mAmbient);
					continue;
				}
				// diffuse material color
				if (filePtr.tokenMatch("MATERIAL_DIFFUSE"))//(TokenMatch(filePtr,"MATERIAL_DIFFUSE",16) )
				{
					parseLV4MeshFloatTriple(mat.mDiffuse);
					continue;
				}
				// specular material color
				if (filePtr.tokenMatch("MATERIAL_SPECULAR"))//(TokenMatch(filePtr,"MATERIAL_SPECULAR",17))
				{
					parseLV4MeshFloatTriple(mat.mSpecular);
					continue;
				}
				// material shading type
				if (filePtr.tokenMatch("MATERIAL_SHADING"))//(TokenMatch(filePtr,"MATERIAL_SHADING",16))
				{
					if (filePtr.tokenMatch("Blinn"))//(TokenMatch(filePtr,"Blinn",5))
					{
						mat.mShading = D3DSHelper.Blinn;
					}
					else if (filePtr.tokenMatch("Phong"))//(TokenMatch(filePtr,"Phong",5))
					{
						mat.mShading = D3DSHelper.Phong;
					}
					else if (filePtr.tokenMatch("Flat"))//(TokenMatch(filePtr,"Flat",4))
					{
						mat.mShading = D3DSHelper.Flat;
					}
					else if (filePtr.tokenMatch("Wire"))//(TokenMatch(filePtr,"Wire",4))
					{
						mat.mShading = D3DSHelper.Wire;
					}
					else
					{
						// assume gouraud shading
						mat.mShading = D3DSHelper.Gouraud;
						skipToNextToken();
					}
					continue;
				}
				// material transparency
				if (filePtr.tokenMatch("MATERIAL_TRANSPARENCY"))//(TokenMatch(filePtr,"MATERIAL_TRANSPARENCY",21))
				{
					mat.mTransparency = parseLV4MeshFloat();
					mat.mTransparency = 1.0f - mat.mTransparency;continue;
				}
				// material self illumination
				if (filePtr.tokenMatch("MATERIAL_SELFILLUM"))//(TokenMatch(filePtr,"MATERIAL_SELFILLUM",18))
				{
					float f = parseLV4MeshFloat();

					mat.mEmissive.x = f;
					mat.mEmissive.y = f;
					mat.mEmissive.z = f;
					continue;
				}
				// material shininess
				if (filePtr.tokenMatch("MATERIAL_SHINE"))//(TokenMatch(filePtr,"MATERIAL_SHINE",14) )
				{
					mat.mSpecularExponent = parseLV4MeshFloat();
					mat.mSpecularExponent *= 15;
					continue;
				}
				// two-sided material
				if (filePtr.tokenMatch("MATERIAL_TWOSIDED"))//(TokenMatch(filePtr,"MATERIAL_TWOSIDED",17) )
				{
					mat.mTwoSided = true;
					continue;
				}
				// material shininess strength
				if (filePtr.tokenMatch("MATERIAL_SHINESTRENGTH"))//(TokenMatch(filePtr,"MATERIAL_SHINESTRENGTH",22))
				{
					mat.mShininessStrength = parseLV4MeshFloat();
					continue;
				}
				// diffuse color map
				if (filePtr.tokenMatch("MAP_DIFFUSE"))//(TokenMatch(filePtr,"MAP_DIFFUSE",11))
				{
					// parse the texture block
					parseLV3MapBlock(mat.sTexDiffuse);
					continue;
				}
				// ambient color map
				if (filePtr.tokenMatch("MAP_AMBIENT"))//(TokenMatch(filePtr,"MAP_AMBIENT",11))
				{
					// parse the texture block
					parseLV3MapBlock(mat.sTexAmbient);
					continue;
				}
				// specular color map
				if (filePtr.tokenMatch("MAP_SPECULAR"))//(TokenMatch(filePtr,"MAP_SPECULAR",12))
				{
					// parse the texture block
					parseLV3MapBlock(mat.sTexSpecular);
					continue;
				}
				// opacity map
				if (filePtr.tokenMatch("MAP_OPACITY"))//(TokenMatch(filePtr,"MAP_OPACITY",11))
				{
					// parse the texture block
					parseLV3MapBlock(mat.sTexOpacity);
					continue;
				}
				// emissive map
				if (filePtr.tokenMatch("MAP_SELFILLUM"))//(TokenMatch(filePtr,"MAP_SELFILLUM",13))
				{
					// parse the texture block
					parseLV3MapBlock(mat.sTexEmissive);
					continue;
				}
				// bump map
				if (filePtr.tokenMatch("MAP_BUMP"))//(TokenMatch(filePtr,"MAP_BUMP",8))
				{
					// parse the texture block
					parseLV3MapBlock(mat.sTexBump);
				}
				// specular/shininess map
				if (filePtr.tokenMatch("MAP_SHINESTRENGTH"))//(TokenMatch(filePtr,"MAP_SHINESTRENGTH",17))
				{
					// parse the texture block
					parseLV3MapBlock(mat.sTexShininess);
					continue;
				}
				// number of submaterials
				if (filePtr.tokenMatch("NUMSUBMTLS"))//(TokenMatch(filePtr,"NUMSUBMTLS",10))
				{
					iNumSubMaterials = parseLV4MeshLong();

					// allocate enough storage
//					mat.avSubMaterials.resize(iNumSubMaterials);
					int count = iNumSubMaterials - mat.avSubMaterials.size();
					for(int i = 0; i < count; i++)
						mat.avSubMaterials.add(new ASEMaterial());
				}
				// submaterial chunks
				if (filePtr.tokenMatch("SUBMATERIAL"))//(TokenMatch(filePtr,"SUBMATERIAL",11))
				{
				
					int iIndex = parseLV4MeshLong();

					if (iIndex >= iNumSubMaterials)
					{
						logWarning("Out of range: submaterial index is too large");
						iIndex = iNumSubMaterials-1;
					}

					// get a reference to the material
					ASEMaterial sMat = mat.avSubMaterials.get(iIndex);

					// parse the material block
					parseLV2MaterialBlock(sMat);
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("2","*MATERIAL");
			
			String level = "2";
			String msg = "*MATERIAL";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
			{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *NODE_TM block in a file
	//!param mesh Node (!) object to be filled
	void parseLV2NodeTransformBlock(BaseNode mesh){
		int iDepth = 0;
		int c;
		
		int mode   = 0; 
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				// name of the node
				if (filePtr.tokenMatch("NODE_NAME"))// (TokenMatch(filePtr,"NODE_NAME" ,9))
				{
					String temp = "";
					if(!parseString("*NODE_NAME"))
						skipToNextToken();
					else
						temp = parsedString;

					int s;
					if (temp.equals(mesh.mName))
					{
						mode = 1;
					}
					else if (-1 != (s = temp.indexOf(".Target")) && mesh.mName.equals(temp.substring(0,s)))
					{
						// This should be either a target light or a target camera
						if ( (mesh.mType == BaseNode.LIGHT &&  ((ASELight)mesh) .mLightType  == ASELight.TARGET) ||
							 (mesh.mType == BaseNode.CAMERA && ((ASECamera)mesh).mCameraType == ASECamera.TARGET))
						{
							mode = 2;
						}
						else DefaultLogger.error("ASE: Ignoring target transform, this is no spot light or target camera");
					}
					else
					{
						DefaultLogger.error("ASE: Unknown node transformation: " + temp);
						// mode = 0
					}
					continue;
				}
				if (mode != 0)
				{
					// fourth row of the transformation matrix - and also the 
					// only information here that is interesting for targets
					if (filePtr.tokenMatch("TM_ROW3"))//(TokenMatch(filePtr,"TM_ROW3" ,7))
					{
//						parseLV4MeshFloatTriple((mode == 1 ? mesh.mTransform[3] : &mesh.mTargetPosition.x));
						parseLV4MeshFloatTriple();
						if(mode == 1){
							// TODO
							mesh.mTransform.m30 = parsedFloatTrip[0];
							mesh.mTransform.m31 = parsedFloatTrip[1];
							mesh.mTransform.m32 = parsedFloatTrip[2];
						}else{
							mesh.mTargetPosition.load(parsedFloatTrip, 0);
						}
						continue;
					}
					if (mode == 1)
					{
						// first row of the transformation matrix
						if (filePtr.tokenMatch("TM_ROW0"))//(TokenMatch(filePtr,"TM_ROW0" ,7))
						{
							parseLV4MeshFloatTriple(/*mesh.mTransform[0]*/);
							// TODO
							mesh.mTransform.m00 = parsedFloatTrip[0];
							mesh.mTransform.m01 = parsedFloatTrip[1];
							mesh.mTransform.m02 = parsedFloatTrip[2];
							continue;
						}
						// second row of the transformation matrix
						if (filePtr.tokenMatch("TM_ROW1"))//(TokenMatch(filePtr,"TM_ROW1" ,7))
						{
//							parseLV4MeshFloatTriple(mesh.mTransform[1]);
							// TODO
							mesh.mTransform.m10 = parsedFloatTrip[0];
							mesh.mTransform.m11 = parsedFloatTrip[1];
							mesh.mTransform.m12 = parsedFloatTrip[2];
							continue;
						}
						// third row of the transformation matrix
						if (filePtr.tokenMatch("TM_ROW2"))//(TokenMatch(filePtr,"TM_ROW2" ,7))
						{
//							parseLV4MeshFloatTriple(mesh.mTransform[2]);
							// TODO
							mesh.mTransform.m20 = parsedFloatTrip[0];
							mesh.mTransform.m21 = parsedFloatTrip[1];
							mesh.mTransform.m22 = parsedFloatTrip[2];
							continue;
						}
						// inherited position axes
						if (filePtr.tokenMatch("INHERIT_POS"))//(TokenMatch(filePtr,"INHERIT_POS" ,11))
						{
//							int[] aiVal = new int[3];
							parseLV4MeshLongTriple(/*aiVal*/);
							
							for (int i = 0; i < 3;++i)
								mesh.inherit.abInheritPosition[i] = (parsedLongTrip[i] != 0);
							continue;
						}
						// inherited rotation axes
						if (filePtr.tokenMatch("INHERIT_ROT"))//(TokenMatch(filePtr,"INHERIT_ROT" ,11))
						{
//							int[] aiVal = new int[3];
							parseLV4MeshLongTriple(/*aiVal*/);

							for (int i = 0; i < 3;++i)
								mesh.inherit.abInheritRotation[i] = parsedLongTrip[i] != 0;
							continue;
						}
						// inherited scaling axes
						if (filePtr.tokenMatch("INHERIT_SCL"))//(TokenMatch(filePtr,"INHERIT_SCL" ,11))
						{
//							int[] aiVal = new int[3];
							parseLV4MeshLongTriple(/*aiVal*/);

							for (int i = 0; i < 3;++i)
								mesh.inherit.abInheritScaling[i] = parsedLongTrip[i] != 0;
							continue;
						}
					}
				}
			}
//			AI_ASE_HANDLE_SECTION("2","*NODE_TM");
			String level = "2";
			String msg = "*NODE_TM";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *TM_ANIMATION block in a file
	//!param mesh Mesh object to be filled
	void parseLV2AnimationBlock(BaseNode mesh){
		int iDepth = 0;
		int c;
		
		ASEAnimation anim = mesh.mAnim;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				if (filePtr.tokenMatch("NODE_NAME"))//(TokenMatch(filePtr,"NODE_NAME" ,9))
				{
					String temp = "";
					if(!parseString("*NODE_NAME"))
						skipToNextToken();
					else
						temp = parsedString;

					// If the name of the node contains .target it 
					// represents an animated camera or spot light
					// target.
					if (-1 != temp.indexOf(".Target"))
					{
						if  ((mesh.mType != BaseNode.CAMERA || ((ASECamera)mesh).mCameraType != ASECamera.TARGET)  &&
							( mesh.mType != BaseNode.LIGHT  || ((ASELight)mesh).mLightType   != ASELight.TARGET))
						{   

							DefaultLogger.error("ASE: Found target animation channel but the node is neither a camera nor a spot light");
							anim = null;
						}
						else anim = mesh.mTargetAnim;
					}
					continue;
				}

				// position keyframes
				if (filePtr.tokenMatch("CONTROL_POS_TRACK") /*TokenMatch(filePtr,"CONTROL_POS_TRACK"  ,17)*/  ||
					filePtr.tokenMatch("CONTROL_POS_BEZIER") /*TokenMatch(filePtr,"CONTROL_POS_BEZIER" ,18)*/  ||
					filePtr.tokenMatch("CONTROL_POS_TCB") /*TokenMatch(filePtr,"CONTROL_POS_TCB"    ,15)*/)
				{
					if (anim == null)skipSection();
					else parseLV3PosAnimationBlock(anim);
					continue;
				}
				// scaling keyframes
				if (filePtr.tokenMatch("CONTROL_SCALE_TRACK") /*TokenMatch(filePtr,"CONTROL_SCALE_TRACK"  ,19)*/ ||
					filePtr.tokenMatch("CONTROL_SCALE_BEZIER") /*TokenMatch(filePtr,"CONTROL_SCALE_BEZIER" ,20)*/ ||
					filePtr.tokenMatch("CONTROL_SCALE_TCB") /*TokenMatch(filePtr,"CONTROL_SCALE_TCB"    ,17)*/)
				{
					if (anim == null || anim == mesh.mTargetAnim)
					{
						// Target animation channels may have no rotation channels
						DefaultLogger.error("ASE: Ignoring scaling channel in target animation");
						skipSection();
					}
					else parseLV3ScaleAnimationBlock(anim);
					continue;
				}
				// rotation keyframes
				if (filePtr.tokenMatch("CONTROL_ROT_TRACK") /*TokenMatch(filePtr,"CONTROL_ROT_TRACK"  ,17)*/ ||
					filePtr.tokenMatch("CONTROL_ROT_BEZIER") /*TokenMatch(filePtr,"CONTROL_ROT_BEZIER" ,18)*/ ||
					filePtr.tokenMatch("CONTROL_ROT_TCB") /*TokenMatch(filePtr,"CONTROL_ROT_TCB"    ,15)*/)
				{
					if (anim == null|| anim == mesh.mTargetAnim)
					{
						// Target animation channels may have no rotation channels
						DefaultLogger.error("ASE: Ignoring rotation channel in target animation");
						skipSection();
					}
					else parseLV3RotAnimationBlock(anim);
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("2","TM_ANIMATION");
			String level = "2";
			String msg = "TM_ANIMATION";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}
	void parseLV3PosAnimationBlock(ASEAnimation anim){
		int iDepth = 0;
		int c;
		int iIndex;
		
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				
				boolean b = false;

				// For the moment we're just reading the three floats -
				// we ignore the ádditional information for bezier's and TCBs

				// simple scaling keyframe
				if (filePtr.tokenMatch("CONTROL_POS_SAMPLE")) //(TokenMatch(filePtr,"CONTROL_POS_SAMPLE" ,18))
				{
					b = true;
					anim.mPositionType = ASEAnimation.TRACK;
				}

				// Bezier scaling keyframe
				if (filePtr.tokenMatch("CONTROL_BEZIER_POS_KEY")) //(TokenMatch(filePtr,"CONTROL_BEZIER_POS_KEY" ,22))
				{
					b = true;
					anim.mPositionType = ASEAnimation.BEZIER;
				}
				// TCB scaling keyframe
				if (filePtr.tokenMatch("CONTROL_TCB_POS_KEY")) //(TokenMatch(filePtr,"CONTROL_TCB_POS_KEY" ,19))
				{
					b = true;
					anim.mPositionType = ASEAnimation.TCB;
				}
				if (b)
				{
					VectorKey key;
					anim.akeyPositions.add(key = new VectorKey());
//					aiVectorKey& key = anim.akeyPositions.back();
					iIndex = parseLV4MeshFloatTriple(key.mValue, true);
					key.mTime = iIndex;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*CONTROL_POS_TRACK");
			String level = "3";
			String msg = "*CONTROL_POS_TRACK";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}
	
	void parseLV3ScaleAnimationBlock(ASEAnimation anim){
		int iDepth = 0;
		int c;;
		int iIndex;
		
		while (true)
		{
			c  = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				boolean b = false;

				// For the moment we're just reading the three floats -
				// we ignore the ádditional information for bezier's and TCBs

				// simple scaling keyframe
				if (filePtr.tokenMatch("CONTROL_SCALE_SAMPLE") /*TokenMatch(filePtr,"CONTROL_SCALE_SAMPLE" ,20)*/)
				{
					b = true;
					anim.mScalingType = ASEAnimation.TRACK;
				}

				// Bezier scaling keyframe
				if (filePtr.tokenMatch("CONTROL_BEZIER_SCALE_KEY") /*TokenMatch(filePtr,"CONTROL_BEZIER_SCALE_KEY" ,24)*/)
				{
					b = true;
					anim.mScalingType = ASEAnimation.BEZIER;
				}
				// TCB scaling keyframe
				if (filePtr.tokenMatch("CONTROL_TCB_SCALE_KEY") /*TokenMatch(filePtr,"CONTROL_TCB_SCALE_KEY" ,21)*/)
				{
					b = true;
					anim.mScalingType = ASEAnimation.TCB;
				}
				if (b)
				{
					VectorKey key;
					anim.akeyScaling.add(key = new VectorKey());
//					aiVectorKey& key = anim.akeyScaling.back();
					iIndex = parseLV4MeshFloatTriple(key.mValue,true);
					key.mTime = iIndex;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*CONTROL_POS_TRACK");
			String level = "3";
			String msg = "*CONTROL_POS_TRACK";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}
	void parseLV3RotAnimationBlock(ASEAnimation anim){
		int iDepth = 0;
		int c;;
		int iIndex;
		Vector3f v = new Vector3f();
		float f;
		
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				boolean b = false;

				// For the moment we're just reading the  floats -
				// we ignore the ádditional information for bezier's and TCBs

				// simple scaling keyframe
				if (filePtr.tokenMatch("CONTROL_ROT_SAMPLE")) //(TokenMatch(filePtr,"CONTROL_ROT_SAMPLE" ,18))
				{
					b = true;
					anim.mRotationType = ASEAnimation.TRACK;
				}

				// Bezier scaling keyframe
				if (filePtr.tokenMatch("CONTROL_BEZIER_ROT_KEY")) //(TokenMatch(filePtr,"CONTROL_BEZIER_ROT_KEY" ,22))
				{
					b = true;
					anim.mRotationType = ASEAnimation.BEZIER;
				}
				// TCB scaling keyframe
				if (filePtr.tokenMatch("CONTROL_TCB_ROT_KEY")) //(TokenMatch(filePtr,"CONTROL_TCB_ROT_KEY" ,19))
				{
					b = true;
					anim.mRotationType = ASEAnimation.TCB;
				}
				if (b)
				{
					QuatKey key;
					anim.akeyRotations.add(key = new QuatKey());
//					aiQuatKey& key = anim.akeyRotations.back();
					iIndex = parseLV4MeshFloatTriple(v,true);
					f = parseLV4MeshFloat();
					key.mTime = iIndex;
					key.mValue.setFromAxisAngle(v, f);
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*CONTROL_ROT_TRACK");
			String level = "3";
			String msg = "CONTROL_ROT_TRACK";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH block in a file
	//!param mesh Mesh object to be filled
	void parseLV2MeshBlock(ASEMesh mesh){
		int iDepth = 0;
		int c;
		
		int iNumVertices = 0;
		int iNumFaces = 0;
		int iNumTVertices = 0;
		int iNumTFaces = 0;
		int iNumCVertices = 0;
		int iNumCFaces = 0;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				
				// Number of vertices in the mesh
				if (filePtr.tokenMatch("MESH_NUMVERTEX"))//(TokenMatch(filePtr,"MESH_NUMVERTEX" ,14))
				{
					iNumVertices = parseLV4MeshLong();
					continue;
				}
				// Number of texture coordinates in the mesh
				if (filePtr.tokenMatch("MESH_NUMTVERTEX"))//(TokenMatch(filePtr,"MESH_NUMTVERTEX" ,15))
				{
					iNumTVertices = parseLV4MeshLong();
					continue;
				}
				// Number of vertex colors in the mesh
				if (filePtr.tokenMatch("MESH_NUMCVERTEX"))//(TokenMatch(filePtr,"MESH_NUMCVERTEX" ,15))
				{
					iNumCVertices = parseLV4MeshLong();
					continue;
				}
				// Number of regular faces in the mesh
				if (filePtr.tokenMatch("MESH_NUMFACES"))//(TokenMatch(filePtr,"MESH_NUMFACES" ,13))
				{
					iNumFaces = parseLV4MeshLong();
					continue;
				}
				// Number of UVWed faces in the mesh
				if (filePtr.tokenMatch("MESH_NUMTVFACES"))//(TokenMatch(filePtr,"MESH_NUMTVFACES" ,15))
				{
					iNumTFaces = parseLV4MeshLong();
					continue;
				}
				// Number of colored faces in the mesh
				if (filePtr.tokenMatch("MESH_NUMCVFACES"))//(TokenMatch(filePtr,"MESH_NUMCVFACES" ,15))
				{
					iNumCFaces = parseLV4MeshLong();
					continue;
				}
				// mesh vertex list block
				if (filePtr.tokenMatch("MESH_VERTEX_LIST"))//(TokenMatch(filePtr,"MESH_VERTEX_LIST" ,16))
				{
					parseLV3MeshVertexListBlock(iNumVertices,mesh);
					continue;
				}
				// mesh face list block
				if (filePtr.tokenMatch("MESH_FACE_LIST"))//(TokenMatch(filePtr,"MESH_FACE_LIST" ,14))
				{
					parseLV3MeshFaceListBlock(iNumFaces,mesh);
					continue;
				}
				// mesh texture vertex list block
				if (filePtr.tokenMatch("MESH_TVERTLIST"))//(TokenMatch(filePtr,"MESH_TVERTLIST" ,14))
				{
					parseLV3MeshTListBlock(iNumTVertices,mesh,0);
					continue;
				}
				// mesh texture face block
				if (filePtr.tokenMatch("MESH_TFACELIST"))//(TokenMatch(filePtr,"MESH_TFACELIST" ,14))
				{
					parseLV3MeshTFaceListBlock(iNumTFaces,mesh, 0);
					continue;
				}
				// mesh color vertex list block
				if (filePtr.tokenMatch("MESH_CVERTLIST"))//(TokenMatch(filePtr,"MESH_CVERTLIST" ,14))
				{
					parseLV3MeshCListBlock(iNumCVertices,mesh);
					continue;
				}
				// mesh color face block
				if (filePtr.tokenMatch("MESH_CFACELIST"))//(TokenMatch(filePtr,"MESH_CFACELIST" ,14))
				{
					parseLV3MeshCFaceListBlock(iNumCFaces,mesh);
					continue;
				}
				// mesh normals
				if (filePtr.tokenMatch("MESH_NORMALS"))//(TokenMatch(filePtr,"MESH_NORMALS" ,12))
				{
					parseLV3MeshNormalListBlock(mesh);
					continue;
				}
				// another mesh UV channel ...
				if (filePtr.tokenMatch("MESH_MAPPINGCHANNEL"))//(TokenMatch(filePtr,"MESH_MAPPINGCHANNEL" ,19))
				{

					int iIndex = 0;
					iIndex = parseLV4MeshLong();

					if (iIndex < 2)
					{
						if(DefaultLogger.LOG_OUT)
							logWarning("Mapping channel has an invalid index. Skipping UV channel");
						// skip it ...
						skipSection();
					}
					if (iIndex > Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS)
					{
						if(DefaultLogger.LOG_OUT)
							logWarning("Too many UV channels specified. Skipping channel ..");
						// skip it ...
						skipSection();
					}
					else
					{
						// parse the mapping channel
						parseLV3MappingChannel(iIndex-1,mesh);
					}
					continue;
				}
				// mesh animation keyframe. Not supported
				if (filePtr.tokenMatch("MESH_ANIMATION"))//(TokenMatch(filePtr,"MESH_ANIMATION" ,14))
				{
					
					if(DefaultLogger.LOG_OUT)
						logWarning("Found *MESH_ANIMATION element in ASE/ASK file. " +
						"Keyframe animation is not supported by Assimp, this element "+
						"will be ignored");
					//SkipSection();
					continue;
				}
				if (filePtr.tokenMatch("MESH_WEIGHTS"))//(TokenMatch(filePtr,"MESH_WEIGHTS" ,12))
				{
					parseLV3MeshWeightsBlock(mesh);continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("2","*MESH");
			String level = "2";
			String msg = "*MESH";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *LIGHT_SETTINGS block in a file
	//!param light Light object to be filled
	void parseLV2LightSettingsBlock(ASELight light){
		int iDepth = 0;
		int c;
		
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				c =filePtr.get();
				if (filePtr.tokenMatch("LIGHT_COLOR"))//(TokenMatch(filePtr,"LIGHT_COLOR" ,11))
				{
					parseLV4MeshFloatTriple(light.mColor);
					continue;
				}
				if (filePtr.tokenMatch("LIGHT_INTENS"))//(TokenMatch(filePtr,"LIGHT_INTENS" ,12))
				{
					light.mIntensity = parseLV4MeshFloat();
					continue;
				}
				if (filePtr.tokenMatch("LIGHT_HOTSPOT"))//(TokenMatch(filePtr,"LIGHT_HOTSPOT" ,13))
				{
					light.mAngle = parseLV4MeshFloat();
					continue;
				}
				if (filePtr.tokenMatch("LIGHT_FALLOFF"))//(TokenMatch(filePtr,"LIGHT_FALLOFF" ,13))
				{
					light.mFalloff = parseLV4MeshFloat();
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("2","LIGHT_SETTINGS");
			String level = "2";
			String msg = "LIGHT_SETTINGS";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *CAMERA_SETTINGS block in a file
	//!param cam Camera object to be filled
	void parseLV2CameraSettingsBlock(ASECamera camera){
		int iDepth = 0;
		int c;
		
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				c = filePtr.get();
				if (filePtr.tokenMatch("CAMERA_NEAR"))//(TokenMatch(filePtr,"CAMERA_NEAR" ,11))
				{
					camera.mNear = parseLV4MeshFloat();
					continue;
				}
				if (filePtr.tokenMatch("CAMERA_FAR"))//(TokenMatch(filePtr,"CAMERA_FAR" ,10))
				{
					camera.mFar = parseLV4MeshFloat();
					continue;
				}
				if (filePtr.tokenMatch("CAMERA_FOV"))//(TokenMatch(filePtr,"CAMERA_FOV" ,10))
				{
					camera.mFOV = parseLV4MeshFloat();
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("2","CAMERA_SETTINGS");
			String level = "2";
			String msg = "CAMERA_SETTINGS";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse the *MAP_XXXXXX blocks in a material
	//!param map Texture structure to be filled
	void parseLV3MapBlock(D3DSTexture map){
		int iDepth = 0;
		
		int c;
		// ***********************************************************
		// *BITMAP should not be there if *MAP_CLASS is not BITMAP,
		// but we need to expect that case ... if the path is
		// empty the texture won't be used later.
		// ***********************************************************
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();
				c = filePtr.get();
				// type of map
				if (filePtr.tokenMatch("MAP_CLASS")) //(TokenMatch(filePtr,"MAP_CLASS" ,9))
				{
					String temp = "";
					if(!parseString("*MAP_CLASS"))
						skipToNextToken();
					temp = parsedString;
//					if (temp != "Bitmap" && temp != "Normal Bump")
					if(!temp.equals("Bitmap") && !temp.equals("Normal Bump"))
					{
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.warn("ASE: Skipping unknown map type: " + temp);
//						parsePath = false; 
					}
					continue;
				}
				// path to the texture
				if (filePtr.tokenMatch("BITMAP")) //(parsePath && TokenMatch(filePtr,"BITMAP" ,6))
				{
					if(!parseString("*BITMAP"))
						skipToNextToken();

					map.mMapName = parsedString;
					if (map.mMapName.equals("None"))
					{
						// Files with 'None' as map name are produced by
						// an Maja to ASE exporter which name I forgot ..
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.warn("ASE: Skipping invalid map entry");
						map.mMapName = "";
					}

					continue;
				}
				// offset on the u axis
				if (filePtr.tokenMatch("UVW_U_OFFSET")) //(TokenMatch(filePtr,"UVW_U_OFFSET" ,12))
				{
					map.mOffsetU = parseLV4MeshFloat();
					continue;
				}
				// offset on the v axis
				if (filePtr.tokenMatch("UVW_V_OFFSET")) //(TokenMatch(filePtr,"UVW_V_OFFSET" ,12))
				{
					map.mOffsetV = parseLV4MeshFloat();
					continue;
				}
				// tiling on the u axis
				if (filePtr.tokenMatch("UVW_U_TILING")) //(TokenMatch(filePtr,"UVW_U_TILING" ,12))
				{
					map.mScaleU = parseLV4MeshFloat();
					continue;
				}
				// tiling on the v axis
				if (filePtr.tokenMatch("UVW_V_TILING")) //(TokenMatch(filePtr,"UVW_V_TILING" ,12))
				{
					map.mScaleV = parseLV4MeshFloat();
					continue;
				}
				// rotation around the z-axis
				if (filePtr.tokenMatch("UVW_ANGLE")) //(TokenMatch(filePtr,"UVW_ANGLE" ,9))
				{
					map.mRotation = parseLV4MeshFloat();
					continue;
				}
				// map blending factor
				if (filePtr.tokenMatch("MAP_AMOUNT")) //(TokenMatch(filePtr,"MAP_AMOUNT" ,10))
				{
					map.mTextureBlend= parseLV4MeshFloat();
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MAP_XXXXXX");
			String level = "3";
			String msg = "*MAP_XXXXXX";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_VERTEX_LIST block in a file
	//!param iNumVertices Value of *MESH_NUMVERTEX, if present.
	//! Otherwise zero. This is used to check the consistency of the file.
	//! A warning is sent to the logger if the validations fails.
	//!param mesh Mesh object to be filled
	void parseLV3MeshVertexListBlock(int iNumVertices,ASEMesh mesh){
		int iDepth = 0;
		int c;
		Vector3f vTemp = new Vector3f();
		// allocate enough storage in the array
//		mesh.mPositions.resize(iNumVertices);
		mesh.mPositions = MemoryUtil.createFloatBuffer(iNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Vertex entry
				if (filePtr.tokenMatch("MESH_VERTEX"))//(TokenMatch(filePtr,"MESH_VERTEX" ,11))
				{
					
					int iIndex = parseLV4MeshFloatTriple(vTemp, true);

					if (iIndex >= iNumVertices)
					{
						if(DefaultLogger.LOG_OUT)
							logWarning("Invalid vertex index. It will be ignored");
					}
					else {
//						mesh.mPositions[iIndex] = vTemp;
						mesh.mPositions.put(3 * iIndex, vTemp.x);
						mesh.mPositions.put(3 * iIndex + 1, vTemp.y);
						mesh.mPositions.put(3 * iIndex + 2, vTemp.z);
					}
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_VERTEX_LIST");
			String level = "3";
			String msg = "*MESH_VERTEX_LIST";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_FACE_LIST block in a file
	//!param iNumFaces Value of *MESH_NUMFACES, if present.
	//! Otherwise zero. This is used to check the consistency of the file.
	//! A warning is sent to the logger if the validations fails.
	//!param mesh Mesh object to be filled
	void parseLV3MeshFaceListBlock( int iNumFaces,ASEMesh mesh){
		int iDepth = 0;
		int c;
		
		// allocate enough storage in the face array
//		mesh.mFaces.resize(iNumFaces);
		int count = iNumFaces - mesh.mFaces.size();
		while(count-- > 0)
			mesh.mFaces.add(null);
		
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Face entry
				if (filePtr.tokenMatch("MESH_FACE"))//(TokenMatch(filePtr,"MESH_FACE" ,9))
				{

					ASEFace mFace = new ASEFace();
					parseLV4MeshFace(mFace);

					if (mFace.iFace >= iNumFaces)
					{
						if(DefaultLogger.LOG_OUT)
							logWarning("Face has an invalid index. It will be ignored");
					}
					else mesh.mFaces.set(mFace.iFace, mFace);
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_FACE_LIST");
			String level = "3";
			String msg = "*MESH_FACE_LIST";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_TVERT_LIST block in a file
	//!param iNumVertices Value of *MESH_NUMTVERTEX, if present.
	//! Otherwise zero. This is used to check the consistency of the file.
	//! A warning is sent to the logger if the validations fails.
	//!param mesh Mesh object to be filled
	//!param iChannel Output UVW channel
	void parseLV3MeshTListBlock(int iNumVertices,ASEMesh mesh, int iChannel/* = 0*/){
		int iDepth = 0;
		int c;
		Vector3f vTemp = new Vector3f();
		// allocate enough storage in the array
//		mesh.amTexCoords[iChannel].resize(iNumVertices);
		mesh.amTexCoords[iChannel] = MemoryUtil.createFloatBuffer(iNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Vertex entry
				if (filePtr.tokenMatch("MESH_TVERT"))//(TokenMatch(filePtr,"MESH_TVERT" ,10))
				{
//					aiVector3D vTemp;
					int iIndex = parseLV4MeshFloatTriple(vTemp, true);

					if (iIndex >= iNumVertices)
					{
						logWarning("Tvertex has an invalid index. It will be ignored");
					}
					else {
//						mesh.amTexCoords[iChannel][iIndex] = vTemp;
						FloatBuffer buf = mesh.amTexCoords[iChannel];
						buf.put(3 * iChannel, vTemp.x);
						buf.put(3 * iChannel + 1, vTemp.y);
						buf.put(3 * iChannel + 2, vTemp.z);
					}

					if (0.0f != vTemp.z)
					{
						// we need 3 coordinate channels
						mesh.mNumUVComponents[iChannel] = 3;
					}
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_TVERT_LIST");
			
			String level = "3";
			String msg = "*MESH_TVERT_LIST";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}
	// -------------------------------------------------------------------
	//! Parse a *MESH_TFACELIST block in a file
	//!param iNumFaces Value of *MESH_NUMTVFACES, if present.
	//! Otherwise zero. This is used to check the consistency of the file.
	//! A warning is sent to the logger if the validations fails.
	//!param mesh Mesh object to be filled
	//!param iChannel Output UVW channel
	void parseLV3MeshTFaceListBlock(int iNumFaces,ASEMesh mesh, int iChannel ){
		int iDepth = 0;
		int c;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Face entry
				if (filePtr.tokenMatch("MESH_TFACE")) // TokenMatch(filePtr,"MESH_TFACE" ,10))
				{
					int iIndex = parseLV4MeshLongTriple(true);
					if (iIndex >= iNumFaces || iIndex >= mesh.mFaces.size())
					{
						if(DefaultLogger.LOG_OUT)
							logWarning("UV-Face has an invalid index. It will be ignored");
					}
					else
					{
						// copy UV indices
//						mesh.mFaces[iIndex].amUVIndices[iChannel][0] = aiValues[0];
//						mesh.mFaces[iIndex].amUVIndices[iChannel][1] = aiValues[1];
//						mesh.mFaces[iIndex].amUVIndices[iChannel][2] = aiValues[2];
						int[] dst = mesh.mFaces.get(iIndex).amUVIndices[iChannel];
						dst[0] = parsedLongTrip[0];
						dst[1] = parsedLongTrip[1];
						dst[2] = parsedLongTrip[2];
					}
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_TFACE_LIST");
			String level = "3";
			String msg = "*MESH_TFACE_LIST";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse an additional mapping channel 
	//! (specified via *MESH_MAPPINGCHANNEL)
	//!param iChannel Channel index to be filled
	//!param mesh Mesh object to be filled
	void parseLV3MappingChannel( int iChannel, ASEMesh mesh){
		int iDepth = 0;
		int c;
		
		int iNumTVertices = 0;
		int iNumTFaces = 0;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Number of texture coordinates in the mesh
				if (filePtr.tokenMatch("MESH_NUMTVERTEX"))//(TokenMatch(filePtr,"MESH_NUMTVERTEX" ,15))
				{
					iNumTVertices = parseLV4MeshLong();
					continue;
				}
				// Number of UVWed faces in the mesh
				if (filePtr.tokenMatch("MESH_NUMTVFACES"))//(TokenMatch(filePtr,"MESH_NUMTVFACES" ,15))
				{
					iNumTFaces = parseLV4MeshLong();
					continue;
				}
				// mesh texture vertex list block
				if (filePtr.tokenMatch("MESH_TVERTLIST"))//(TokenMatch(filePtr,"MESH_TVERTLIST" ,14))
				{
					parseLV3MeshTListBlock(iNumTVertices,mesh,iChannel);
					continue;
				}
				// mesh texture face block
				if (filePtr.tokenMatch("MESH_TFACELIST"))//(TokenMatch(filePtr,"MESH_TFACELIST" ,14))
				{
					parseLV3MeshTFaceListBlock(iNumTFaces,mesh, iChannel);
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_MAPPING_CHANNEL");
			String level = "3";
			String msg = "*MESH_MAPPING_CHANNEL";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_CVERTLIST block in a file
	//!param iNumVertices Value of *MESH_NUMCVERTEX, if present.
	//! Otherwise zero. This is used to check the consistency of the file.
	//! A warning is sent to the logger if the validations fails.
	//!param mesh Mesh object to be filled
	void parseLV3MeshCListBlock(int iNumVertices, ASEMesh mesh){
		int iDepth = 0;
		int c;
		Vector3f vTemp = new Vector3f();
		// allocate enough storage in the array
//		mesh.mVertexColors.resize(iNumVertices);
		mesh.mVertexColors = MemoryUtil.createFloatBuffer(iNumVertices * 4, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Vertex entry
				if (filePtr.tokenMatch("MESH_VERTCOL"))  //(TokenMatch(filePtr,"MESH_VERTCOL" ,12))
				{
//					vTemp.w = 1.0f;
					int iIndex=parseLV4MeshFloatTriple(vTemp, true);

					if (iIndex >= iNumVertices)
					{
						if(DefaultLogger.LOG_OUT)
							logWarning("Vertex color has an invalid index. It will be ignored");
					}
					else {
//						mesh.mVertexColors[iIndex] = vTemp;
						FloatBuffer buf = mesh.mVertexColors;
						iIndex *= 4;
						buf.put(iIndex++, vTemp.x);
						buf.put(iIndex++, vTemp.y);
						buf.put(iIndex++, vTemp.z);
						buf.put(iIndex++, 1.0f);
					}
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_CVERTEX_LIST");
			String level = "3";
			String msg = "*MESH_CVERTEX_LIST";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_CFACELIST block in a file
	//!param iNumFaces Value of *MESH_NUMCVFACES, if present.
	//! Otherwise zero. This is used to check the consistency of the file.
	//! A warning is sent to the logger if the validations fails.
	//!param mesh Mesh object to be filled
	void parseLV3MeshCFaceListBlock(int iNumFaces, ASEMesh mesh){
		int iDepth = 0;
		int c;
		
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Face entry
				if (filePtr.tokenMatch("MESH_CFACE"))//(TokenMatch(filePtr,"MESH_CFACE" ,11))
				{
					int iIndex = parseLV4MeshLongTriple(true);
					if (iIndex >= iNumFaces || iIndex >= mesh.mFaces.size())
					{
						if(DefaultLogger.LOG_OUT)
							logWarning("UV-Face has an invalid index. It will be ignored");
					}
					else
					{
						// copy color indices
//						mesh.mFaces[iIndex].mColorIndices[0] = aiValues[0];
//						mesh.mFaces[iIndex].mColorIndices[1] = aiValues[1];
//						mesh.mFaces[iIndex].mColorIndices[2] = aiValues[2];
						int[] dst = mesh.mFaces.get(iIndex).mColorIndices;
						dst[0] = parsedLongTrip[0];
						dst[1] = parsedLongTrip[1];
						dst[2] = parsedLongTrip[2];
					}
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_CFACE_LIST");
			String level = "3";
			String msg = "*MESH_CFACE_LIST";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_NORMALS block in a file
	//!param mesh Mesh object to be filled
	void parseLV3MeshNormalListBlock(ASEMesh sMesh){
		int iDepth = 0;
		int c;
		
		// Allocate enough storage for the normals
//		sMesh.mNormals.resize(sMesh.mFaces.size()*3,aiVector3D( 0.f, 0.f, 0.f ));
		sMesh.mNormals = MemoryUtil.createFloatBuffer(sMesh.mFaces.size() * 3 * 3,AssimpConfig.MESH_USE_NATIVE_MEMORY);
		int index, faceIdx = -1;
		Vector3f vNormal = new Vector3f();
		// FIXME: rewrite this and find out how to interpret the normals
		// correctly. This is crap.

		// Smooth the vertex and face normals together. The result
		// will be edgy then, but otherwise everything would be soft ...
		while (true)	{
			c = filePtr.get();
			if ('*' == c)	{
//				++filePtr;
				filePtr.inCre();
				if (faceIdx != -1 && (filePtr.tokenMatch("MESH_VERTEXNORMAL"))/*TokenMatch(filePtr,"MESH_VERTEXNORMAL",17)*/)	{
//					aiVector3D vNormal;
					index = parseLV4MeshFloatTriple(vNormal, true);
					if (faceIdx >=  sMesh.mFaces.size())
						continue;
						
					// Make sure we assign it to the correct face
					ASEFace face = sMesh.mFaces.get(faceIdx);
					if (index == face.mIndices[0])
						index = 0;
					else if (index == face.mIndices[1])
						index = 1;
					else if (index == face.mIndices[2])
						index = 2;
					else	{
						DefaultLogger.error("ASE: Invalid vertex index in MESH_VERTEXNORMAL section");
						continue;
					}
					// We'll renormalize later
//					sMesh.mNormals[faceIdx*3+index] += vNormal;
					index = (faceIdx*3+index) * 3;
					FloatBuffer buf = sMesh.mNormals;
					buf.put(index, buf.get(index ++) + vNormal.x);
					buf.put(index, buf.get(index ++) + vNormal.y);
					buf.put(index, buf.get(index ++) + vNormal.z);
					continue;
				}
				if (filePtr.tokenMatch("MESH_FACENORMAL")) /*(TokenMatch(filePtr,"MESH_FACENORMAL",15))*/{
					faceIdx = parseLV4MeshFloatTriple(vNormal, true);

					if (faceIdx >= sMesh.mFaces.size())	{
						DefaultLogger.error("ASE: Invalid vertex index in MESH_FACENORMAL section");
						continue;
					}

					// We'll renormalize later
//					sMesh.mNormals[faceIdx*3] += vNormal;
//					sMesh.mNormals[faceIdx*3+1] += vNormal;
//					sMesh.mNormals[faceIdx*3+2] += vNormal;
					FloatBuffer buf = sMesh.mNormals;
					for(int i = 0; i < 3; i++){
						index = (faceIdx*3 + i) * 3;
						buf.put(index, buf.get(index ++) + vNormal.x);
						buf.put(index, buf.get(index ++) + vNormal.y);
						buf.put(index, buf.get(index ++) + vNormal.z);
					}
					continue;
				}
		}
			
//			AI_ASE_HANDLE_SECTION("3","*MESH_NORMALS");
			String level = "3";
			String msg = "*MESH_NORMALS";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
	//				++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
	//		++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_WEIGHTSblock in a file
	//!param mesh Mesh object to be filled
	void parseLV3MeshWeightsBlock(ASEMesh mesh){
		int iDepth = 0;
		int c;
		
		int iNumVertices = 0, iNumBones = 0;
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Number of bone vertices ...
				if (filePtr.tokenMatch("MESH_NUMVERTEX"))//(TokenMatch(filePtr,"MESH_NUMVERTEX" ,14))
				{
					iNumVertices = parseLV4MeshLong();
					continue;
				}
				// Number of bones
				if (filePtr.tokenMatch("MESH_NUMBONE"))//(TokenMatch(filePtr,"MESH_NUMBONE" ,11))
				{
					iNumBones = parseLV4MeshLong();
					continue;
				}
				// parse the list of bones
				if (filePtr.tokenMatch("MESH_BONE_LIST"))//(TokenMatch(filePtr,"MESH_BONE_LIST" ,14))
				{
					parseLV4MeshBones(iNumBones,mesh);
					continue;
				}
				// parse the list of bones vertices
				if (filePtr.tokenMatch("MESH_BONE_VERTEX_LIST"))//(TokenMatch(filePtr,"MESH_BONE_VERTEX_LIST" ,21) )
				{
					parseLV4MeshBonesVertices(iNumVertices,mesh);
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_WEIGHTS");
			String level = "3";
			String msg = "*MESH_WEIGHTS";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse the bone list of a file
	//!param mesh Mesh object to be filled
	//!param iNumBones Number of bones in the mesh
	void parseLV4MeshBones(int iNumBones,ASEMesh mesh){
		int iDepth = 0;
		int c;
		
//		mesh.mBones.resize(iNumBones);
		int count = iNumBones - mesh.mBones.size();
		while(count > 0){
			mesh.mBones.add(new ASEBone());
			count--;
		}
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Mesh bone with name ...
				if (filePtr.tokenMatch("MESH_BONE_NAME"))//(TokenMatch(filePtr,"MESH_BONE_NAME" ,16))
				{
					// parse an index ...
//					if(SkipSpaces(&filePtr))
					if(filePtr.skipSpaces())
					{
//						int iIndex = strtoul10(filePtr,&filePtr);
						int iIndex = filePtr.strtoul10();
						if (iIndex >= iNumBones)
						{
							if(DefaultLogger.LOG_OUT)
								logWarning("Bone index is out of bounds");
							continue;
						}
						if (!parseString("*MESH_BONE_NAME"))						
							skipToNextToken();
						else
							mesh.mBones.get(iIndex).mName = parsedString;
						continue;
					}
				}
			}
//			AI_ASE_HANDLE_SECTION("3","*MESH_BONE_LIST");
			String level = "3";
			String msg = "*MESH_BONE_LIST";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse the bone vertices list of a file
	//!param mesh Mesh object to be filled
	//!param iNumVertices Number of vertices to be parsed
	void parseLV4MeshBonesVertices(int iNumVertices,ASEMesh mesh){
		int iDepth = 0;
		int c;
		
//		mesh.mBoneVertices.resize(iNumVertices);
		int count = iNumVertices - mesh.mBoneVertices.size();
		while(count > 0){
			mesh.mBoneVertices.add(new BoneVertex());
			count--;
		}
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)
			{
//				++filePtr;
				filePtr.inCre();

				// Mesh bone vertex
				if (filePtr.tokenMatch("MESH_BONE_VERTEX")) //(TokenMatch(filePtr,"MESH_BONE_VERTEX" ,16))
				{
					// read the vertex index
//					int iIndex = strtoul10(filePtr,&filePtr);
					int iIndex = filePtr.strtoul10();
					if (iIndex >= mesh.getNumPostions())
					{
						iIndex = mesh.getNumPostions()-1;
						if(DefaultLogger.LOG_OUT)
							logWarning("Bone vertex index is out of bounds. Using the largest valid bone vertex index instead");
					}

					// --- ignored
					parseLV4MeshFloatTriple();

//					std::pair<int,float> pairOut;
					while (true)
					{
						IntFloatPair pairOut = new IntFloatPair();
						// first parse the bone index ...
//						if (!SkipSpaces(&filePtr))break;
						if (!filePtr.skipSpaces())break;
//						pairOut.first = strtoul10(filePtr,&filePtr);
						pairOut.first = filePtr.strtoul10();

						// then parse the vertex weight
//						if (!SkipSpaces(&filePtr))break;
						if (!filePtr.skipSpaces())break;
//						filePtr = fast_atoreal_move<float>(filePtr,pairOut.second);
						pairOut.second = (float)filePtr.fast_atoreal_move(true);

						// -1 marks unused entries
						if (-1 != pairOut.first)
						{
							mesh.mBoneVertices.get(iIndex).mBoneWeights.add(pairOut);
						}
					}
					continue;
				}
			}
//			AI_ASE_HANDLE_SECTION("4","*MESH_BONE_VERTEX");
			String level = "4";
			String msg = "*MESH_BONE_VERTEX";
			c = filePtr.get();
			if ('{' == c)iDepth++;
			else if ('}' == c)
			{
				if (0 == --iDepth)
				{
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return;
				}
			}
			else if ('\0' == c)
			{
				logError("Encountered unexpected EOL while parsing a " +msg+
				" chunk (Level " +level+ ")");
			}
			if(ParsingUtil.isLineEnd((byte)c) && !bLastWasEndLine)
				{
				++iLineNumber;
				bLastWasEndLine = true;
			} else bLastWasEndLine = false;
//			++filePtr; 
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_FACE block in a file
	//!param out receive the face data
	void parseLV4MeshFace(ASEFace out){
		// skip spaces and tabs
		if(!filePtr.skipSpaces())
		{
			if(DefaultLogger.LOG_OUT)
				logWarning("Unable to parse *MESH_FACE Element: Unexpected EOL [#1]");
			skipToNextToken();
			return;
		}

		// parse the face index
		out.iFace = filePtr.strtoul10();

		// next character should be ':'
		if(!filePtr.skipSpaces())
		{
			// FIX: there are some ASE files which haven't got : here ....
			if(DefaultLogger.LOG_OUT)
				logWarning("Unable to parse *MESH_FACE Element: Unexpected EOL. \':\' expected [#2]");
			skipToNextToken();
			return;
		}
		// FIX: There are some ASE files which haven't got ':' here 
		int c = filePtr.get();
		if(':' == c)filePtr.inCre();

		// Parse all mesh indices
		for (int i = 0; i < 3;++i)
		{
			int iIndex = 0;
			if(!filePtr.skipSpaces())
			{
				if(DefaultLogger.LOG_OUT)
					logWarning("Unable to parse *MESH_FACE Element: Unexpected EOL");
				skipToNextToken();
				return;
			}
			c = filePtr.get();
			switch (c)
			{
			case 'A':
			case 'a':
				break;
			case 'B':
			case 'b':
				iIndex = 1;
				break;
			case 'C':
			case 'c':
				iIndex = 2;
				break;
			default: 
				if(DefaultLogger.LOG_OUT)
					logWarning("Unable to parse *MESH_FACE Element: Unexpected EOL. A,B or C expected [#3]");
				skipToNextToken();
				return;
			};
//			++filePtr;
			filePtr.inCre();

			// next character should be ':'
			if(!filePtr.skipSpaces() || ':' != filePtr.get())
			{
				if(DefaultLogger.LOG_OUT)
					logWarning("Unable to parse *MESH_FACE Element: Unexpected EOL. \':\' expected [#2]");
				skipToNextToken();
				return;
			}

//			++filePtr;
			filePtr.inCre();
			if(!filePtr.skipSpaces())
			{
				if(DefaultLogger.LOG_OUT)
					logWarning("Unable to parse *MESH_FACE Element: Unexpected EOL. Vertex index ecpected [#4]");
				skipToNextToken();
				return;
			}
			out.mIndices[iIndex] = filePtr.strtoul10();
		}

		// now we need to skip the AB, BC, CA blocks. 
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)break;
			if (ParsingUtil.isLineEnd((byte)c))
			{
				//iLineNumber++;
				return;
			}
//			filePtr++;
			filePtr.inCre();
		}

		// parse the smoothing group of the face
		if (filePtr.tokenMatch("*MESH_SMOOTHING"))//(TokenMatch(filePtr,"*MESH_SMOOTHING",15))
		{
			if(!filePtr.skipSpaces())
			{
				if(DefaultLogger.LOG_OUT)
					logWarning("Unable to parse *MESH_SMOOTHING Element: Unexpected EOL. Smoothing group(s) expected [#5]");
				skipToNextToken();
				return;
			}
			
			// Parse smoothing groups until we don't anymore see commas
			// FIX: There needn't always be a value, sad but true
			while (true)
			{
				c = filePtr.get();
				if (c < '9' && c >= '0')
				{
					out.iSmoothGroup |= (1 << filePtr.strtoul10());
				}
				filePtr.skipSpaces();
				c = filePtr.get();
				if (',' != c)
				{
					break;
				}
//				++filePtr;
				filePtr.inCre();
				filePtr.skipSpaces();
			}
		}

		// *MESH_MTLID  is optional, too
		while (true)
		{
			c = filePtr.get();
			if ('*' == c)break;
			if (ParsingUtil.isLineEnd((byte)c))
			{
				return;
			}
//			filePtr++;
			filePtr.inCre();
		}

		if (filePtr.tokenMatch("*MESH_MTLID"))//(TokenMatch(filePtr,"*MESH_MTLID",11))
		{
			if(!filePtr.skipSpaces())
			{
				if(DefaultLogger.LOG_OUT)
					logWarning("Unable to parse *MESH_MTLID Element: Unexpected EOL. Material index expected [#6]");
				skipToNextToken();
				return;
			}
			out.iMaterial = filePtr.strtoul10();
		}
		return;
	}

	// -------------------------------------------------------------------
	//! Parse a *MESH_VERT block in a file
	//! (also works for MESH_TVERT, MESH_CFACE, MESH_VERTCOL  ...)
	//!param apOut Output buffer (3 floats)
	//!param rIndexOut Output index
	int parseLV4MeshFloatTriple(boolean needIndex){
		int index = 0;
		if(needIndex)
			// parse the index
			index = parseLV4MeshLong();

		// parse the three others
		for (int i = 0; i < 3;++i)
			parsedFloatTrip[i] = parseLV4MeshFloat();
		return index;
	}
	
	int parseLV4MeshFloatTriple(Vector3f v, boolean needIndex){
		int index = 0;
		if(needIndex)
			// parse the index
			index = parseLV4MeshLong();

		// parse the three others
		for (int i = 0; i < 3;++i)
//			parsedFloatTrip[i] = parseLV4MeshFloat();
			v.setValue(i, parseLV4MeshFloat());
		return index;
	}
	
	void parseLV4MeshFloatTriple(Vector3f v){ parseLV4MeshFloatTriple(v, false);}

	// -------------------------------------------------------------------
	//! Parse a *MESH_VERT block in a file
	//! (also works for MESH_TVERT, MESH_CFACE, MESH_VERTCOL  ...)
	//!param apOut Output buffer (3 floats)
	void parseLV4MeshFloatTriple() {parseLV4MeshFloatTriple(false);}

	// -------------------------------------------------------------------
	//! Parse a *MESH_TFACE block in a file
	//! (also works for MESH_CFACE)
	//!param apOut Output buffer (3 ints)
	//!param rIndexOut Output index
	int parseLV4MeshLongTriple(boolean needIndex){
		int index = 0;
		if(needIndex)
			// parse the index
			index = parseLV4MeshLong();

		// parse the three others
		for (int i = 0; i < 3;++i)
			parsedLongTrip[i] = parseLV4MeshLong();
		return index;
	}

	void parseLV4MeshLongTriple(){ parseLV4MeshLongTriple(false);}

	// -------------------------------------------------------------------
	//! Parse a single float element 
	//!param fOut Output float
	float parseLV4MeshFloat(){
		// skip spaces and tabs
		if(!filePtr.skipSpaces())
		{
			// LOG 
			if(DefaultLogger.LOG_OUT)
				logWarning("Unable to parse float: unexpected EOL [#1]");
			++iLineNumber;
			return 0;
		}
		// parse the first float
//		filePtr = fast_atoreal_move<float>(filePtr,fOut);
		return (float)filePtr.fast_atoreal_move(true);
	}

	// -------------------------------------------------------------------
	//! Parse a single int element 
	//!param iOut Output integer
	int parseLV4MeshLong(){
		// Skip spaces and tabs
		if(!filePtr.skipSpaces())
		{
			// LOG 
			if(DefaultLogger.LOG_OUT)
				logWarning("Unable to parse long: unexpected EOL [#1]");
			++iLineNumber;
			return 0;
		}
		// parse the value
		return filePtr.strtoul10();
	}

	// -------------------------------------------------------------------
	//! Skip everything to the next: '*' or '\0'
	boolean skipToNextToken(){
		while (true)
		{
			char me = (char) filePtr.get();

			// increase the line number counter if necessary
			if (ParsingUtil.isLineEnd((byte)me) && !bLastWasEndLine)
			{
				++iLineNumber;
				bLastWasEndLine = true;
			}
			else bLastWasEndLine = false;
			if ('*' == me || '}' == me || '{' == me)return true;
			if ('\0' == me)return false;

//			++filePtr;
			filePtr.inCre();
		}
	}

	// -------------------------------------------------------------------
	//! Skip the current section until the token after the closing }.
	//! This function handles embedded subsections correctly
	boolean skipSection(){
		// must handle subsections ...
		int iCnt = 0;
		while (true)
		{
			int c = filePtr.get();
			if ('}' == c)
			{
				--iCnt;
				if (0 == iCnt)
				{
					// go to the next valid token ...
//					++filePtr;
					filePtr.inCre();
					skipToNextToken();
					return true;
				}
			}
			else if ('{' == c)
			{
				++iCnt;
			}
			else if ('\0' == c)
			{
				logWarning("Unable to parse block: Unexpected EOF, closing bracket'}\' was expected [#1]");	
				return false;
			}
			else if(ParsingUtil.isLineEnd((byte)c))++iLineNumber;
//			++filePtr;
			filePtr.inCre();
			c = filePtr.get();
		}
	}

	// -------------------------------------------------------------------
	//! Output a warning to the logger
	//!param szWarn Warn message
	void logWarning(String szWarn){
		String temp = String.format("Line %i: %s",iLineNumber,szWarn);
		DefaultLogger.warn(temp);
	}

	// -------------------------------------------------------------------
	//! Output a message to the logger
	//!param szWarn Message
	void logInfo(String szWarn){
		String temp = String.format("Line %i: %s",iLineNumber,szWarn);
		DefaultLogger.info(temp);
	}

	// -------------------------------------------------------------------
	//! Output an error to the logger
	//!param szWarn Error message
	void logError(String szWarn){
		String temp = String.format("Line %i: %s",iLineNumber,szWarn);
		DefaultLogger.error(temp);
	}

	// -------------------------------------------------------------------
	//! Parse a string, enclosed in double quotation marks
	//!param out Output string
	//!param szName Name of the enclosing element -> used in error
	//! messages.
	//!return false if an error occured
	boolean parseString(String szName){
		if (!filePtr.skipSpaces())
		{

			if(DefaultLogger.LOG_OUT){
				logWarning(String.format("Unable to parse %s block: Unexpected EOL",szName));
			}
			return false;
		}
		
		int c = filePtr.get();
		// there must be '"'
		if ('\"' != c)
		{

			if(DefaultLogger.LOG_OUT){
				logWarning(String.format("Unable to parse %s block: Strings are expected to be enclosed in double quotation marks",szName));
			}
			return false;
		}
//		++filePtr;
		filePtr.inCre(); c = filePtr.get();
//		const char* sz = filePtr;
		int sz = filePtr.getCurrent();
		while (true)
		{
			if ('\"' == c)break;
			else if ('\0' == c)
			{		
				if(DefaultLogger.LOG_OUT){
//				sprintf(szBuffer,"Unable to parse %s block: Strings are expected to "
//					"be enclosed in double quotation marks but EOF was reached before "
//					"a closing quotation mark was encountered",szName);
					logWarning(String.format("Unable to parse %s block: Strings are expected to be enclosed in double quotation marks but EOF was reached before a closing quotation mark was encountered", szName));
				}
				return false;
			}
			sz++;
			c = filePtr.get(sz);
		}
//		out = std::string(filePtr,(uintptr_t)sz-(uintptr_t)filePtr);
		parsedString = filePtr.getString(filePtr.get(), sz);
//		filePtr = sz+1;
		filePtr.setCurrent(sz + 1);
		return true;
	}
}
