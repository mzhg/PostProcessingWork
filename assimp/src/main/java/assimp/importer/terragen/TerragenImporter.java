package assimp.importer.terragen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.Importer;
import assimp.common.ImporterDesc;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.StreamReader;

/** Importer class to load Terragen (0.9) terrain files.<p>
*
*  The loader is basing on the information found here:
*  <a href = "http://www.planetside.co.uk/terragen/dev/tgterrain.html#chunks">http://www.planetside.co.uk/terragen/dev/tgterrain.html#chunks</a>
*/
public class TerragenImporter extends BaseImporter{

	// Magic strings
	static final String AI_TERR_BASE_STRING    = "TERRAGEN";
	static final String AI_TERR_TERRAIN_STRING = "TERRAIN ";
	static final String AI_TERR_EOF_STRING     = "EOF ";

	// Chunka
	static final String AI_TERR_CHUNK_XPTS     = "XPTS";
	static final String AI_TERR_CHUNK_YPTS     = "YPTS";
	static final String AI_TERR_CHUNK_SIZE     = "SIZE";
	static final String AI_TERR_CHUNK_SCAL     = "SCAL";
	static final String AI_TERR_CHUNK_CRAD     = "CRAD";
	static final String AI_TERR_CHUNK_CRVM     = "CRVM";
	static final String AI_TERR_CHUNK_ALTW     = "ALTW";
	
	static final ImporterDesc desc = new ImporterDesc(
		"Terragen Heightmap Importer",
		"",
		"",
		"http://www.planetside.co.uk/",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"ter" 
	);
	
	boolean configComputeUVs;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		// check file extension 
		String extension = getExtension(pFile);
		
		if( extension.equals("ter"))
			return true;

		if(  extension.length() == 0 || checkSig)	{
			/*  If CanRead() is called in order to check whether we
			 *  support a specific file extension in general pIOHandler
			 *  might be NULL and it's our duty to return true here.
			 */
			if (pIOHandler == null)return true;
			String tokens[] = {"terragen"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() {return desc;}
	
	@Override
	public void setupProperties(Importer pImp) {
		// AI_CONFIG_IMPORT_TER_MAKE_UVS
		configComputeUVs = ( 0 != pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_TER_MAKE_UVS,0) );
	}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		ByteBuffer buf = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		
		@SuppressWarnings("resource")
		StreamReader reader = new StreamReader(buf, true);
		if(reader.getRemainingSize() < 16)
		throw new DeadlyImportError( "TER: file is too small" );

		// Check for the existence of the two magic strings 'TERRAGEN' and 'TERRAIN '
//		if (::strncmp((const char*)reader.GetPtr(),AI_TERR_BASE_STRING,8))
		if(!AssUtil.equals(reader.getPtr(), AI_TERR_BASE_STRING, 0, 8))
			throw new DeadlyImportError( "TER: Magic string \'TERRAGEN\' not found" );
		
		reader.incPtr(8);
//		if (::strncmp((const char*)reader.GetPtr()+8,AI_TERR_TERRAIN_STRING,8))
		if(!AssUtil.equals(reader.getPtr(), AI_TERR_TERRAIN_STRING, 0, 8))
			throw new DeadlyImportError( "TER: Magic string \'TERRAIN\' not found" );
		
		int x = 0,y = 0,mode = 0;
		@SuppressWarnings("unused")
		float rad  = 6370.f;

		Node root = pScene.mRootNode = new Node();
		root.mName = ("<TERRAGEN.TERRAIN>");

		// Default scaling is 30
//		root->mTransformation.a1 = root->mTransformation.b2 = root->mTransformation.c3 = 30.f;
		root.mTransformation.m00 = root.mTransformation.m11 = root.mTransformation.m22 = 30.f;

		// Now read all chunks until we're finished or an EOF marker is encountered
		reader.incPtr(8);
		while (reader.getRemainingSize() >= 4)	
		{
//			const char* head = (const char*)reader.GetPtr();
			ByteBuffer head = reader.getPtr();
			reader.incPtr(4);

			// EOF, break in every case
			if (!strncmp(head,AI_TERR_EOF_STRING,4))
				break;

			// Number of x-data points
			if (!strncmp(head,AI_TERR_CHUNK_XPTS,4))
			{
				x = reader.getI2() & 0xFFFF;
			}
			// Number of y-data points
			else if (!strncmp(head,AI_TERR_CHUNK_YPTS,4))
			{
				y = reader.getI2() & 0xFFFF;
			}
			// Squared terrains width-1. 
			else if (!strncmp(head,AI_TERR_CHUNK_SIZE,4))
			{
				x = y = reader.getI2() & 0xFFFF+1;
			}
			// terrain scaling
			else if (!strncmp(head,AI_TERR_CHUNK_SCAL,4))
			{
				root.mTransformation.m00 = reader.getF4();
				root.mTransformation.m11 = reader.getF4();
				root.mTransformation.m22 = reader.getF4();
			}
			// mapping == 1: earth radius
			else if (!strncmp(head,AI_TERR_CHUNK_CRAD,4))
			{
				rad = reader.getF4();
			}
			// mapping mode
			else if (!strncmp(head,AI_TERR_CHUNK_CRVM,4))
			{
				mode = reader.getI1();
				if (0 != mode)
					DefaultLogger.error("TER: Unsupported mapping mode, a flat terrain is returned");
			}
			// actual terrain data
			else if (!strncmp(head,AI_TERR_CHUNK_ALTW,4))
			{
				float hscale  = (float)reader.getI2()  / 65536;  // TODO
				float bheight = (float)reader.getI2();

				if (hscale == 0)hscale = 1;

				// Ensure we have enough data
				if (reader.getRemainingSize() < x*y*2)
					throw new DeadlyImportError("TER: ALTW chunk is too small");

				if (x <= 1 || y <= 1)
					throw new DeadlyImportError("TER: Invalid terrain size");

				// Allocate the output mesh
				pScene.mMeshes = new Mesh[/*pScene->mNumMeshes =*/ 1];
				Mesh m = pScene.mMeshes[0] = new Mesh();

				// We return quads
//				aiFace* f = m->mFaces = new aiFace[m->mNumFaces = (x-1)*(y-1)];
				m.mFaces = new Face[(x-1)*(y-1)];
				int f = 0;
//				aiVector3D* pv = m->mVertices = new aiVector3D[m->mNumVertices = m->mNumFaces*4];
				m.mNumVertices = m.mFaces.length * 4;
				FloatBuffer pv = m.mVertices = MemoryUtil.createFloatBuffer(m.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				
				FloatBuffer uv = ( null );
				float step_y = ( 0.0f ), step_x = ( 0.0f );
				if (configComputeUVs) {
					uv = m.mTextureCoords[0] = MemoryUtil.createFloatBuffer(m.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
					step_y = 1.f/y;
					step_x = 1.f/x;
				}
//				const int16_t* data = (const int16_t*)reader.GetPtr();
				ShortBuffer data = reader.getPtr().asShortBuffer();

				for (int yy = 0, t = 0; yy < y-1;++yy)	{
					for (int xx = 0; xx < x-1;++xx,++f)	{

						// make verts
						final float fy = (float)yy, fx = (float)xx;
						int tmp,tmp2;
//						*pv++ = aiVector3D(fx,fy,    (float)data[(tmp2=x*yy)    + xx] * hscale + bheight);
						pv.put(fx).put(fy).put((float)data.get((tmp2=x*yy)    + xx) * hscale + bheight);
//						*pv++ = aiVector3D(fx,fy+1,  (float)data[(tmp=x*(yy+1)) + xx] * hscale + bheight);
						pv.put(fx).put(fy + 1).put((float)data.get((tmp=x*(yy+1)) + xx) * hscale + bheight);
//						*pv++ = aiVector3D(fx+1,fy+1,(float)data[tmp  + xx+1]         * hscale + bheight);
						pv.put(fx+1).put(fy+1).put((float)data.get(tmp  + xx+1)         * hscale + bheight);
//						*pv++ = aiVector3D(fx+1,fy,  (float)data[tmp2 + xx+1]         * hscale + bheight);
						pv.put(fx+1).put(fy).put((float)data.get(tmp2 + xx+1)         * hscale + bheight);

						// also make texture coordinates, if necessary
						if (configComputeUVs) {
//							*uv++ = aiVector3D( step_x*xx,     step_y*yy,     0.f );
//							*uv++ = aiVector3D( step_x*xx,     step_y*(yy+1), 0.f );
//							*uv++ = aiVector3D( step_x*(xx+1), step_y*(yy+1), 0.f );
//							*uv++ = aiVector3D( step_x*(xx+1), step_y*yy,     0.f );
							uv.put(step_x*xx)		.put(step_y*yy)		 .put(0.f);
							uv.put(step_x*xx)		.put(step_y*(yy + 1)).put(0.f);
							uv.put(step_x*(xx + 1)) .put(step_y*(yy + 1)).put(0.f);
							uv.put(step_x*(xx + 1)) .put(step_y*yy)		 .put(0.f);
						}

						// make indices
//						f->mIndices = new unsigned int[f->mNumIndices = 4];
//						for (unsigned int i = 0; i < 4;++i)
//							f->mIndices[i] = t++;
						Face face = m.mFaces[f] = Face.createInstance(4);
						for(int i = 0; i < 4; i++)
							face.set(i, t++);
					}
				}
				
				pv.flip();
				uv.flip();

				// Add the mesh to the root node
//				root->mMeshes = new unsigned int[root->mNumMeshes = 1];
//				root->mMeshes[0] = 0;
				root.mMeshes = new int[]{0};
			}

			// Get to the next chunk (4 byte aligned)
			int dtt;
			if ((dtt = reader.getCurrentPos() & 0x3) !=0)
				reader.incPtr(4-dtt);
		}

		// Check whether we have a mesh now
		if (pScene.getNumMeshes() != 1)
			throw new DeadlyImportError("TER: Unable to load terrain");

		// Set the AI_SCENE_FLAGS_TERRAIN bit
		pScene.mFlags |= Scene.AI_SCENE_FLAGS_TERRAIN;
	}
	
	private boolean strncmp(ByteBuffer buf, String str, int len){
		return !AssUtil.equals(buf, str, 0, len);
	}

}
