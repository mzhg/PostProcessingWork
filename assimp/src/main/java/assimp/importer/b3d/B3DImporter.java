package assimp.importer.b3d;

import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.Bone;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.QuatKey;
import assimp.common.Scene;
import assimp.common.TextureType;
import assimp.common.VectorKey;
import assimp.common.VertexWeight;

/** Definition of the .b3d importer class. */
public class B3DImporter extends BaseImporter{

	static final ImporterDesc desc =new ImporterDesc(
		"BlitzBasic 3D Importer",
		"",
		"",
		"http://www.blitzbasic.com/",
		ImporterDesc.aiImporterFlags_SupportBinaryFlavour,
		0,
		0,
		0,
		0,
		"b3d" 
	);
	
	
//	unsigned _size;
//	final List<unsigned char> _buf;
	ByteBuffer _buf;
//	final List<unsigned> _stack;
	final IntArrayList _stack = new IntArrayList();
	
	final List<String> _textures = new ArrayList<>();
	final List<Material> _materials = new ArrayList<>();

	int _vflags,_tcsets,_tcsize;
	final List<B3DVertex> _vertices = new ArrayList<>();

	final List<Node> _nodes = new ArrayList<>();
	final List<Mesh> _meshes = new ArrayList<>();
	final List<NodeAnim> _nodeAnims = new ArrayList<>();
	final List<Animation> _animations = new ArrayList<>();
	
	final Vector3f tmp3 = new Vector3f();
	final Vector2f tmp2 = new Vector2f();
	final Quaternion tmpQuat = new Quaternion();
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) {
		int pos=pFile.lastIndexOf( '.' );
		if( pos== -1 ) return false;

		String ext=pFile.substring(pos+1);
		if( ext.length()!=3 ) return false;

		return (ext.charAt(0)=='b' || ext.charAt(0)=='B') && (ext.charAt(1)=='3') && (ext.charAt(2)=='d' || ext.charAt(2)=='D');
	}

	@Override
	protected ImporterDesc getInfo() {
		return desc;
	}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		// Load the file data into memory.
		ByteBuffer buf = FileUtils.loadText(pFile, false, false);
		// check whether the .b3d file is large enough to contain
		// at least one chunk.
		if(buf.remaining() < 8)
			throw new DeadlyImportError("B3D File is too small.");
				
		_stack.clear();
		readBB3D( pScene );
	}
	
	void oops(){ throw new DeadlyImportError("B3D Importer - INTERNAL ERROR");}
	void fail(String str ){throw new DeadlyImportError( "B3D Importer - error in B3D file data: "+str );}
	
	int readByte(){
		if(_buf.remaining() > 0)
			return _buf.get();
		fail( "EOF" );
		return 0;
	}
	
	// ------------------------------------------------------------------------------------------------
	int readInt(){
		if(_buf.remaining() > 3)
			return _buf.getInt();
		fail( "EOF" );
		return 0;
	}

	// ------------------------------------------------------------------------------------------------
	float readFloat(){
		if(_buf.remaining() > 3)
			return _buf.getFloat();
		fail( "EOF" );
		return 0.0f;
	}

	// ------------------------------------------------------------------------------------------------
	Vector2f readVec2(){
		float x=readFloat();
		float y=readFloat();
		tmp2.set(x, y);
		return tmp2;
	}

	// ------------------------------------------------------------------------------------------------
	Vector3f readVec3(){
		float x=readFloat();
		float y=readFloat();
		float z=readFloat();
		tmp3.set(x, y, z);
		return tmp3;
	}

	// ------------------------------------------------------------------------------------------------
	Quaternion readQuat(){
		// (aramis_acg) Fix to adapt the loader to changed quat orientation
		float w=-readFloat();
		float x=readFloat();
		float y=readFloat();
		float z=readFloat();
		tmpQuat.set(x, y, z, w);
		return tmpQuat;
	}

	// ------------------------------------------------------------------------------------------------
	String readString(){
		StringBuilder str = new StringBuilder();
		while( _buf.remaining() >0 ){
			char c=(char)readByte();
			if(c == 0) return str.toString();
//			str+=c;
			str.append(c);
		}
		fail( "EOF" );
		return "";
	}

	// ------------------------------------------------------------------------------------------------
	String readChunk(){
		StringBuilder tag = new StringBuilder();
		for( int i=0;i<4;++i ){
//			tag+=char( ReadByte() );
			tag.append((char)readByte());
		}
		int sz=readInt();
		_stack.add( _buf.position()+sz );
		return tag.toString();
	}

	// ------------------------------------------------------------------------------------------------
	void exitChunk(){
//		_pos=_stack.back();
		_buf.position(_stack.popInt());
	}

	// ------------------------------------------------------------------------------------------------
	int chunkSize(){
		return _stack.topInt() - _buf.position();
	}
	// ------------------------------------------------------------------------------------------------

//	template<class T>
//	T *B3DImporter::to_array( const vector<T> &v ){
//		if( !v.size() ) return 0;
//		T *p=new T[v.size()];
//		for( size_t i=0;i<v.size();++i ){
//			p[i]=v[i];
//		}
//		return p;
//	}

	// ------------------------------------------------------------------------------------------------
	void readTEXS(){
		while( chunkSize() > 0){
			String name=readString();
			/*int flags=*/readInt();
			/*int blend=*/readInt();
			/*aiVector2D pos=*/readVec2();
			/*aiVector2D scale=*/readVec2();
			/*float rot=*/readFloat();

			_textures.add( name );
		}
	}

	// ------------------------------------------------------------------------------------------------
	void readBRUS(){
		int n_texs=readInt();
		if( n_texs<0 || n_texs>8 ){
			fail( "Bad texture count" );
		}
		while( chunkSize() > 0){
			String name=readString();
			Vector3f color=readVec3();
			float alpha=readFloat();
			float shiny=readFloat();
			/*int blend=**/readInt();
			int fx=readInt();

			Material mat=new Material();
			_materials.add( mat );
			
			// Name
			mat.addProperty(name,Material.AI_MATKEY_NAME,0,0 );
			
			// Diffuse color 
			mat.addProperty(color,Material.AI_MATKEY_COLOR_DIFFUSE,0,0 );

			// Opacity
			mat.addProperty(alpha,Material.AI_MATKEY_OPACITY,0,0 );

			// Specular color
			Vector3f speccolor =tmp3;
			speccolor.set( shiny,shiny,shiny );
			mat.addProperty( speccolor, Material.AI_MATKEY_COLOR_SPECULAR,0,0 );
			
			// Specular power
			float specpow=shiny*128;
			mat.addProperty(specpow, Material.AI_MATKEY_SHININESS,0,0 );
			
			// Double sided
			if( (fx & 0x10) !=0 ){
				mat.addProperty(1,Material.AI_MATKEY_TWOSIDED,0,0 );
			} 		

			//Textures
			for( int i=0;i<n_texs;++i ){
				int texid=readInt();
				if( texid<-1 || (texid>=0 && texid>=_textures.size())){
					fail( "Bad texture id" );
				}
				if( i==0 && texid>=0 ){
//					aiString texname( _textures[texid] );
					mat.addProperty(_textures.get(texid),Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(),0);
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------------
	void readVRTS(){
		_vflags=readInt();
		_tcsets=readInt();
		_tcsize=readInt();
		if( _tcsets<0 || _tcsets>4 || _tcsize<0 || _tcsize>4 ){
			fail( "Bad texcoord data" );
		}

		int sz=12+((_vflags&1) != 0?12:0)+((_vflags&2) != 0?16:0)+(_tcsets*_tcsize*4);
		int n_verts=chunkSize()/sz;

		int v0=_vertices.size();
//		_vertices.resize( v0+n_verts );
		// for better performance we create a new list
		List<B3DVertex> tmpList = new ArrayList<B3DVertex>(n_verts);
		for(int i = 0; i < n_verts; i++){
			tmpList.add(new B3DVertex());
		}
		_vertices.addAll(tmpList);
		tmpList = null;

		float t[]={0,0,0,0};
		for( int l=0;l<n_verts;++l ){
			B3DVertex v=_vertices.get(v0+l);

//			memset( v.bones,0,sizeof(v.bones) );
//			memset( v.weights,0,sizeof(v.weights) );

//			v.vertex=ReadVec3();
			v.setVertex(readVec3());

			if((_vflags & 1) != 0 ) v.setNormal(readVec3());;

			if((_vflags & 2)!=0 ) readQuat();	//skip v 4bytes...

			for( int i=0;i<_tcsets;++i ){
				for( int j=0;j<_tcsize;++j ){
					t[j]=readFloat();
				}
				t[1]=1-t[1];
				if( i == 0) //v.texcoords=aiVector3D( t[0],t[1],t[2] );{
				{
					v.tx = t[0];
					v.ty = t[1];
					v.tz = t[2];
			}
			}
		}
	}

	// ------------------------------------------------------------------------------------------------
	void readTRIS( int v0 ){
		int matid=readInt();
		if( matid==-1 ){
			matid=0;
		}else if( matid<0 || matid>=_materials.size() ){
//	#ifdef DEBUG_B3D
//			cout<<"material id="<<matid<<endl;
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.debug("material id=" + matid);
//	#endif
			fail( "Bad material id" );
		}

		Mesh mesh=new Mesh();
		_meshes.add( mesh );

		mesh.mMaterialIndex=matid;
//		mesh.mNumFaces=0;
		mesh.mPrimitiveTypes=Mesh.aiPrimitiveType_TRIANGLE;

		int n_tris=chunkSize()/12;
//		aiFace *face=mesh->mFaces=new aiFace[n_tris];
		Face[] faces = mesh.mFaces = new Face[n_tris];
		int face_index = 0;

		for( int i=0;i<n_tris;++i ){
			int i0=readInt()+v0;
			int i1=readInt()+v0;
			int i2=readInt()+v0;
			if( i0<0 || i0>=_vertices.size() || i1<0 || i1>=_vertices.size() || i2<0 || i2>=_vertices.size() ){
//	#ifdef DEBUG_B3D
//				cout<<"Bad triangle index: i0="<<i0<<", i1="<<i1<<", i2="<<i2<<endl;
//	#endif
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.debug("Bad triangle index: i0=" + i0 +", i1=" + i1 + ", i2=" + i2);
				fail( "Bad triangle index" );
				continue;
			}
//			face->mNumIndices=3;
//			face->mIndices=new unsigned[3];
			Face face = faces[face_index] = Face.createInstance(3);
//			face->mIndices[0]=i0;
//			face->mIndices[1]=i1;
//			face->mIndices[2]=i2;
			face.set(0, i0);
			face.set(1, i1);
			face.set(2, i2);
//			++mesh.mNumFaces;
			++face_index;
		}
	}

	// ------------------------------------------------------------------------------------------------
	void readMESH(){
		/*int matid=*/readInt();

		int v0=_vertices.size();

		while( chunkSize() > 0){
			String t=readChunk();
			if( t=="VRTS" ){
				readVRTS();
			}else if( t=="TRIS" ){
				readTRIS( v0 );
			}
			exitChunk();
		}
	}

	// ------------------------------------------------------------------------------------------------
	void readBONE( int id ){
		while( chunkSize() > 0){
			int vertex=readInt();
			float weight=readFloat();
			if( vertex<0 || vertex>=(int)_vertices.size() ){
				fail( "Bad vertex index" );
			}

			B3DVertex v=_vertices.get(vertex);
			int i;
			for( i=0;i<4;++i ){
				if( v.weights[i]  == 0){
					v.bones[i]=(byte) id;
					v.weights[i]=weight;
					break;
				}
			}
//	#ifdef DEBUG_B3D
			if(DefaultLogger.LOG_OUT&& i==4 ){
//				cout<<"Too many bone weights"<<endl;
				DefaultLogger.debug("Too many bone weights");
			}
//	#endif
		}
	}

	// ------------------------------------------------------------------------------------------------
	void readKEYS(NodeAnim nodeAnim ){
//		vector<aiVectorKey> trans,scale;
//		vector<aiQuatKey> rot;
		List<VectorKey> trans = new ArrayList<VectorKey>();
		List<VectorKey> scale = new ArrayList<VectorKey>();
		List<QuatKey> rot = new ArrayList<QuatKey>();
		int flags=readInt();
		while( chunkSize() > 0){
			int frame=readInt();
			if((flags & 1) != 0 ){
				trans.add( new VectorKey( frame,readVec3() ) );
			}
			if((flags & 2)!=0 ){
				scale.add( new VectorKey( frame,readVec3() ) );
			}
			if((flags & 4) !=0 ){
				rot.add( new QuatKey( frame,readQuat() ) );
			}
		}

		if((flags & 1) != 0 ){
//			nodeAnim->mNumPositionKeys=trans.size();
//			nodeAnim->mPositionKeys=to_array( trans );
			nodeAnim.mPositionKeys = trans.toArray(new VectorKey[trans.size()]);
		}

		if((flags & 2)!=0 ){
//			nodeAnim->mNumScalingKeys=scale.size();
//			nodeAnim->mScalingKeys=to_array( scale );
			nodeAnim.mScalingKeys = scale.toArray(new VectorKey[scale.size()]);
		}

		if((flags & 4) !=0 ){
//			nodeAnim->mNumRotationKeys=rot.size();
//			nodeAnim->mRotationKeys=to_array( rot );
			nodeAnim.mRotationKeys = rot.toArray(new QuatKey[rot.size()]);
		}
	}

	// ------------------------------------------------------------------------------------------------
	void readANIM(){
		/*int flags=*/readInt();
		int frames=readInt();
		float fps=readFloat();

		Animation anim=new Animation();
		_animations.add( anim );

		anim.mDuration=frames;
		anim.mTicksPerSecond=fps;
	}

	// ------------------------------------------------------------------------------------------------
	Node readNODE( Node parent ){

		String name=readString();
		Vector3f t=new Vector3f(readVec3());
		Vector3f s=new Vector3f(readVec3());
		Quaternion r=readQuat();

//		aiMatrix4x4 trans,scale,rot;
//
//		aiMatrix4x4::Translation( t,trans );
//		aiMatrix4x4::Scaling( s,scale );
//		rot=aiMatrix4x4( r.GetMatrix() );
//
//		aiMatrix4x4 tform=trans * rot * scale;
		Matrix4f scale = new Matrix4f();
		scale.m00 = s.x;
		scale.m11 = s.y;
		scale.m22 = s.z;
		Matrix4f rot = new Matrix4f();
		r.toMatrix(rot);
		Matrix4f.mul(scale, rot, scale);
		scale.translate(t);
		scale.transpose();

		int nodeid=_nodes.size();

		Node node=new Node( name );
		_nodes.add( node );

		node.mParent=parent;
		node.mTransformation.load(scale);

		NodeAnim nodeAnim=null;
//		vector<unsigned> meshes;
//		vector<aiNode*> children;
		IntArrayList meshes = new IntArrayList();
		List<Node> children = new ArrayList<Node>();

		while( chunkSize() > 0){
			String str=readChunk();
			if(str.equals("MESH" )){
				int n=_meshes.size();
				readMESH();
				for( int i=n;i<(int)_meshes.size();++i ){
					meshes.add( i );
				}
			}else if(str.equals("BONE") ){
				readBONE( nodeid );
			}else if(str.equals("ANIM") ){
				readANIM();
			}else if(str.equals("KEYS") ){
				if( nodeAnim == null ){
					nodeAnim=new NodeAnim();
					_nodeAnims.add( nodeAnim );
					nodeAnim.mNodeName=node.mName;
				}
				readKEYS( nodeAnim );
			}else if( str.equals("NODE" )){
				Node child=readNODE( node );
				children.add( child );
			}
			exitChunk();
		}

//		node->mNumMeshes=meshes.size();
//		node->mMeshes=to_array( meshes );
		if(!meshes.isEmpty())
			node.mMeshes = meshes.toIntArray();

//		node->mNumChildren=children.size();
//		node->mChildren=to_array( children );
		if(!children.isEmpty())
			node.mChildren = children.toArray(new Node[children.size()]);

		return node;
	}

	@SuppressWarnings("unchecked")
	// ------------------------------------------------------------------------------------------------
	void readBB3D( Scene scene ){

		Matrix4f tmpMat = new Matrix4f();
		_textures.clear();
		_materials.size();

		_vertices.clear();
		_meshes.clear();

		_nodes.clear();
		_nodeAnims.clear();
		_animations.clear();

		String t=readChunk();
		if( t.equals("BB3D") ){
			int version=readInt();
			
			if (!DefaultLogger.LOG_OUT) {
//				char dmp[128];
//				sprintf(dmp,"B3D file format version: %i",version);
//				DefaultLogger::get()->info(dmp);
				DefaultLogger.info(String.format("B3D file format version: %i",version));
			}

			while( chunkSize() > 0){
				t=readChunk();
				if( t.equals("TEXS") ){
					readTEXS();
				}else if( t.equals("BRUS") ){
					readBRUS();
				}else if( t.equals("NODE") ){
					readNODE( null );
				}
				exitChunk();
			}
		}
		exitChunk();

		if( _nodes.size()  == 0) fail( "No nodes" );

		if( _meshes.size() == 0) fail( "No meshes" );

		//Fix nodes/meshes/bones
		for(int i=0;i<_nodes.size();++i ){
			Node node=_nodes.get(i);

			for( int j=0;j<node.getNumMeshes();++j ){
				Mesh mesh=_meshes.get(node.mMeshes[j]);

				int n_tris=mesh.getNumFaces();
				int n_verts=mesh.mNumVertices=n_tris * 3;

//				aiVector3D *mv=mesh->mVertices=new aiVector3D[ n_verts ],*mn=0,*mc=0;
				mesh.mVertices = MemoryUtil.createFloatBuffer(n_verts * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				int mv = 0, mn = 0, mc = 0;
				if((_vflags & 1) != 0)mesh.mNormals=MemoryUtil.createFloatBuffer(n_verts * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
				if( _tcsets!=0 ) mesh.mTextureCoords[0]=MemoryUtil.createFloatBuffer(n_verts * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);

				Face[] faces=mesh.mFaces;
				int face_index = 0;

//				vector< vector<aiVertexWeight> > vweights( _nodes.size() );
				List<VertexWeight>[] vweights = new List[_nodes.size()];
				for(int k = 0; k < vweights.length; k++)
					vweights[k] = new ArrayList<VertexWeight>();

				for( int k=0;k<n_verts;k+=3 ){
					for( int l=0;l<3;++l ){
						Face face = faces[face_index];
						B3DVertex v=_vertices.get(face.get(l));

//						*mv++=v.vertex;
						int index = 3 * mv++;
						mesh.mVertices.put(index++, v.x);
						mesh.mVertices.put(index++, v.y);
						mesh.mVertices.put(index++, v.z);
						
						if( (_vflags & 1) != 0 ) {
//							*mn++=v.normal;
							index = 3 * mn++;
							mesh.mNormals.put(index++, v.nx);
							mesh.mNormals.put(index++, v.ny);
							mesh.mNormals.put(index++, v.nz);
						}
						if(  _tcsets!=0  ) {
//							*mc++=v.texcoords;
							index = 3 * mc++;
							mesh.mTextureCoords[0].put(index++, v.tx);
							mesh.mTextureCoords[0].put(index++, v.ty);
							mesh.mTextureCoords[0].put(index++, v.tz);
						}

//						face->mIndices[j]=i+j;
						face.set(l, k+l);

						for( int n=0;n<4;++n ){
							if( v.weights[n] == 0) break;

							int bone=v.bones[n] & 0xFF;
							float weight=v.weights[n];

							vweights[bone].add(new VertexWeight(k+l,weight) );
						}
					}
					++face_index;
				}

//				vector<aiBone*> bones;
				List<Bone> bones = new ArrayList<Bone>();
				for(int l=0;l<vweights.length;++l ){
					List<VertexWeight> weights=vweights[l];
					if(weights.size() == 0) continue;

					Bone bone=new Bone();
					bones.add( bone );

					Node bnode=_nodes.get(l);

					bone.mName=bnode.mName;
//					bone.mNumWeights=weights.size();
//					bone.mWeights=to_array( weights );
					if(weights.size() > 0)
						bone.mWeights = weights.toArray(new VertexWeight[weights.size()]);

					Matrix4f mat=tmpMat;
					mat.load(bnode.mTransformation);
					boolean hasMat = false;
					while( bnode.mParent!=null ){
						bnode=bnode.mParent;
//						mat=bnode->mTransformation * mat;
						Matrix4f.mul(mat, bnode.mTransformation, mat);
						hasMat = true;
					}
					if(hasMat)
						mat.transpose();
//					bone->mOffsetMatrix=mat.Inverse();
					Matrix4f.invert(mat, bone.mOffsetMatrix);
				}
//				mesh->mNumBones=bones.size();
//				mesh->mBones=to_array( bones );
				mesh.mBones = bones.toArray(new Bone[bones.size()]);
			}
		}

		//nodes
		scene.mRootNode=_nodes.get(0);

		//material
		if( _materials.size() == 0){
			_materials.add( new Material() );
		}
//		scene->mNumMaterials=_materials.size();
//		scene->mMaterials=to_array( _materials );
		scene.mMaterials = _materials.toArray(new Material[_materials.size()]);
		
		//meshes
//		scene->mNumMeshes=_meshes.size();
//		scene->mMeshes=to_array( _meshes );
		scene.mMeshes = _meshes.toArray(new Mesh[_meshes.size()]);

		//animations
		if( _animations.size()==1 && _nodeAnims.size() > 0){

			Animation anim= AssUtil.back(_animations);
//			anim->mNumChannels=_nodeAnims.size();
//			anim->mChannels=to_array( _nodeAnims );
			anim.mChannels = _nodeAnims.toArray(new NodeAnim[_nodeAnims.size()]);

//			scene->mNumAnimations=_animations.size();
			scene.mAnimations=_animations.toArray(new Animation[_animations.size()]);
		}

		// convert to RH
		MakeLeftHandedProcess makeleft;
		makeleft.Execute( scene );

		FlipWindingOrderProcess flip;
		flip.Execute( scene );
	}

}
