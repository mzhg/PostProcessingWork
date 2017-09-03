package assimp.importer.collada;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.IntPair;
import assimp.common.LightSourceType;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.ParsingUtil;
import assimp.common.TextureOp;

/** Parser helper class for the Collada loader.<p> 
*
*  Does all the XML reading and builds internal data structures from it, 
*  but leaves the resolving of all the references to the loader.
*/
final class ColladaParser implements COLEnum{
	
	static final int UP_X = 0;
	static final int UP_Y = 1;
	static final int UP_Z = 2;

	/** Filename, for a verbose error message */
	String mFileName;

	/** XML reader, member for everyday use */
//	irr::io::IrrXMLReader* mReader;
	XmlPullParser mReader;

	/** All data arrays found in the file by ID. Might be referred to by actually 
	    everyone. Collada, you are a steaming pile of indirection. */
//	typedef std::map<String, Collada::Data> DataLibrary;
//	DataLibrary mDataLibrary
	Map<String, Data> mDataLibrary;

	/** Same for accessors which define how the data in a data array is accessed. */
//	typedef std::map<String, Collada::Accessor> AccessorLibrary;
//	AccessorLibrary mAccessorLibrary;
	Map<String, Accessor> mAccessorLibrary;

	/** Mesh library: mesh by ID */
//	typedef std::map<String, COLMesh> MeshLibrary;
//	MeshLibrary mMeshLibrary;
	Map<String, COLMesh> mMeshLibrary;

	/** node library: root node of the hierarchy part by ID */
//	typedef std::map<String, Collada::Node*> NodeLibrary;
//	NodeLibrary mNodeLibrary;
	Map<String, COLNode> mNodeLibrary;

	/** Image library: stores texture properties by ID */
//	typedef std::map<String, Collada::Image> ImageLibrary;
//	ImageLibrary mImageLibrary;
	Map<String, COLImage> mImageLibrary;

	/** Effect library: surface attributes by ID */
//	typedef std::map<String, Collada::Effect> EffectLibrary;
//	EffectLibrary mEffectLibrary;
	Map<String, Effect> mEffectLibrary;

	/** Material library: surface material by ID */
//	typedef std::map<String, Collada::Material> MaterialLibrary;
//	MaterialLibrary mMaterialLibrary;
	Map<String, COLMaterial> mMaterialLibrary;

	/** Light library: surface light by ID */
//	typedef std::map<String, Collada::Light> LightLibrary;
//	LightLibrary mLightLibrary;
	Map<String, COLLight> mLightLibrary;

	/** Camera library: surface material by ID */
//	typedef std::map<String, Collada::Camera> CameraLibrary;
//	CameraLibrary mCameraLibrary;
	Map<String, COLCamera> mCameraLibrary;

	/** Controller library: joint controllers by ID */
//	typedef std::map<String, Collada::Controller> ControllerLibrary;
//	ControllerLibrary mControllerLibrary;
	Map<String, Controller> mControllerLibrary;

	/** Pointer to the root node. Don't delete, it just points to one of 
	    the nodes in the node library. */
	COLNode mRootNode;

	/** Root animation container */
	COLAnimation mAnims;

	/** Size unit: how large compared to a meter */
	float mUnitSize;

	/** Which is the up vector */
	int /*enum { UP_X, UP_Y, UP_Z }*/ mUpDirection;

	/** Collada file format version */
	int /*Collada::FormatVersion*/ mFormat;
	
	final ArrayList<String> elements = new ArrayList<>();
	
	/** Constructor from XML file */
	ColladaParser(File file) throws IOException{
		mRootNode = null;
		mUnitSize = 1.0f;
		mUpDirection = UP_Z;

		// We assume the newest file format by default
		mFormat = FV_1_5_n;
		
//		try(KXmlParser reader = new KXmlParser()){
//			reader.setInput(new FileInputStream(file), null);
//			mReader = reader;
//			
//			readContents();
//		} catch (XmlPullParserException e) {
//			e.printStackTrace();
//		}
		
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			mReader = factory.newPullParser();
			mReader.setInput(new FileInputStream(file), null);
			
			readContents();
		} catch (XmlPullParserException e) {
			throw new DeadlyImportError(e);
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	/** Read bool from text contents of current element */
	boolean readBoolFromTextContent() throws IOException
	{
		String cur = getTextContent();
//		return (!ASSIMP_strincmp(cur,"true",4) || '0' != *cur);
		return Boolean.parseBoolean(cur);
	}

	// ------------------------------------------------------------------------------------------------
	/** Read float from text contents of current element */
	float readFloatFromTextContent() throws IOException
	{
		String cur = getTextContent();
		return  AssUtil.parseFloat(cur);  //fast_atof(cur);
	}
	
	int getEventType(){
		try {
			return mReader.getEventType();
		} catch (XmlPullParserException e) {
			throw new DeadlyImportError(e);
		}
	}
	
	int next() throws IOException{
		try {
			return mReader.next();
		} catch (XmlPullParserException e) {
			throw new DeadlyImportError(e);
		}
	}
	
	boolean isEmptyElementTag(){
		try {
			return mReader.isEmptyElementTag();
		} catch (XmlPullParserException e) {
			throw new DeadlyImportError(e);
		}
	}

	/** Reads the contents of the file 
	 * @throws XmlPullParserException */
	void readContents() throws IOException{
		int event = getEventType();
		while(event != XmlPullParser.END_DOCUMENT){
			// handle the root element "COLLADA"
			switch (event) {
			case XmlPullParser.START_TAG:
				String tag = mReader.getName();
				if(tag == null)
					continue;
				
				if(tag.equals("COLLADA")){
					String version = mReader.getAttributeValue(null, "version");
					if(version != null){
						if(version.startsWith("1.5")){
							mFormat =  FV_1_5_n;
						}else if(version.startsWith("1.4")){
							mFormat =  FV_1_4_n;
						}else if(version.startsWith("1.3")){
							mFormat = FV_1_3_n;
						}
					}
					
					if (DefaultLogger.LOG_OUT) {
						DefaultLogger.debug("Collada schema version is " + version);
					}
					
					readStructure();
				}else{
					DefaultLogger.debug("Ignoring global element <%s>." + mReader.getName());
					skipElement();
				}
				
				break;

			default:
				break;
			}
		}
	}

	/** Reads the structure of the file 
	 * @throws IOException 
	 * @throws XmlPullParserException */
	void readStructure() throws IOException{
		int event = next();
		
		while(true){
			switch (event) {
			case XmlPullParser.START_TAG:
				String tag = mReader.getName();
				if(tag!= null){
					if(tag.equals("asset"))
						readAssetInfo();
					else if(tag.equals("library_animations"))
						readAnimationLibrary();
					else if(tag.equals("library_controllers"))
						readControllerLibrary();
					else if(tag.equals("library_images"))
						readImageLibrary();
					else if( tag.equals( "library_materials"))
						readMaterialLibrary();
					else if( tag.equals( "library_effects"))
						readEffectLibrary();
					else if( tag.equals( "library_geometries"))
						readGeometryLibrary();
					else if( tag.equals( "library_visual_scenes"))
						readSceneLibrary();
					else if( tag.equals( "library_lights"))
						readLightLibrary();
					else if( tag.equals( "library_cameras"))
						readCameraLibrary();
					else if( tag.equals( "library_nodes"))
						readSceneNode(null); /* some hacking to reuse this piece of code */
					else if( tag.equals( "scene"))
						readScene();
					else
						skipElement();
				}
				break;
			case XmlPullParser.END_TAG:
				return;
			default:
				break;
			}
			
			event = next();
		}
	}

	/** Reads asset informations such as coordinate system informations and legal blah 
	 * @throws XmlPullParserException, IOException */
	void readAssetInfo() throws IOException{
		if(isEmptyElementTag())
			return;
		
		int event = next();
		while(true){
			switch (event) {
			case XmlPullParser.START_TAG:
				String tag = mReader.getName();
				if(tag.equals("unit")){
					String attr = mReader.getAttributeValue(null, "meter");
					if(attr != null){
						mUnitSize = Float.parseFloat(attr);
					}else{
						mUnitSize = 1.f;
					}
					
					if(!isEmptyElementTag())
						skipElement();
				}else if(tag.equals("up_axis")){
					// read content, strip whitespace, compare
					String content = getTextContent();
					if(content.equals("X_UP"))
						mUpDirection = UP_X;
					else if(content.equals("Y_UP"))
						mUpDirection = UP_Y;
					else 
						mUpDirection = UP_Z;
					
					// check element end
					testClosing("up_axis");
				}else{
					skipElement();
				}
				break;
			case XmlPullParser.END_TAG:
				if(!mReader.getName().equals("asset")){
					throw new DeadlyImportError("Expected end of <asset> element.");
				}
				
				return;
			default:
				break;
			}
			
			event = next();
		}
	}

	/** Reads the animation library 
	 * @throws XmlPullParserException, IOException */
	void readAnimationLibrary() throws IOException{
		if(isEmptyElementTag())
			return;
		
		int event;
		while((event = next()) != XmlPullParser.END_TAG){
			switch (event) {
			case XmlPullParser.START_TAG:
				String tag = mReader.getName();
				if(tag.equals("animation")){
					// delegate the reading. Depending on the inner elements it will be a container or a anim channel
					readAnimation(mAnims);
				}else{
					skipElement();
				}
				break;

			default:
				break;
			}
		}
		
		if(mReader.getName().compareTo("library_animations") != 0){
			throw new DeadlyImportError("Expected end of <library_animations> element.");
		}
	}

	/** Reads an animation into the given parent structure 
	 * @throws XmlPullParserException */
	void readAnimation( COLAnimation pParent) throws IOException{
		if(isEmptyElementTag())
			return;
		
		// an <animation> element may be a container for grouping sub-elements or an animation channel
		// this is the channel collection by ID, in case it has channels
//		typedef std::map<std::string, AnimationChannel> ChannelMap;
//		ChannelMap channels;
		Map<String, AnimationChannel> channels = new HashMap<String, AnimationChannel>();
		// this is the anim container in case we're a container
		COLAnimation anim = null;

		// optional name given as an attribute
		String animName;
		int indexName = testAttribute( "name");
		int indexID = testAttribute( "id");
		if( indexName >= 0)
			animName = mReader.getAttributeValue( indexName);
		else if( indexID >= 0)
			animName = mReader.getAttributeValue( indexID);
		else
			animName = "animation";
		
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG){
			if(event == XmlPullParser.START_TAG){
				String tag = mReader.getName();
				// we have subanimations
				if(tag.equals("animation")){
					// create container from our element
					if( anim == null)
					{
						anim = new COLAnimation();
						anim.mName = animName;
						pParent.mSubAnims.add( anim);
					}

					// recurse into the subelement
					readAnimation( anim);
				}else if(tag.equals("source")){
					// possible animation data - we'll never know. Better store it
					readSource();
				}else if(tag.equals("sampler")){
					// read the ID to assign the corresponding collada channel afterwards.
					indexID = getAttribute( "id");
					String id = mReader.getAttributeValue( indexID);
//					ChannelMap::iterator newChannel = channels.insert( std::make_pair( id, AnimationChannel())).first;
					AnimationChannel value = channels.get(id);
					if(value == null){
						value = new AnimationChannel();
						channels.put(id, value);
					}

					// have it read into a channel
					readAnimationSampler( /*newChannel.second*/value);
				}else if(tag.compareTo("channel") == 0){
					// the binding element whose whole purpose is to provide the target to animate
					// Thanks, Collada! A directly posted information would have been too simple, I guess.
					// Better add another indirection to that! Can't have enough of those.
					int indexTarget = getAttribute( "target");
					int indexSource = getAttribute( "source");
					String sourceId = mReader.getAttributeValue( indexSource);
					if( sourceId.charAt(0) == '#')
//						sourceId++;
						sourceId = sourceId.substring(1);
//					ChannelMap::iterator cit = channels.find( sourceId);
//					if( cit != channels.end())
//						cit->second.mTarget = mReader->getAttributeValue( indexTarget);
					
					AnimationChannel cit = channels.get(sourceId);
					if(cit != null){
						cit.mTarget = mReader.getAttributeValue(indexTarget);
					}

					if( !isEmptyElementTag())
						skipElement();
				}else{
					// ignore the rest
					skipElement();
				}
			}
		}
		
		if(mReader.getName().compareTo("animation") != 0){
			throw new DeadlyImportError("Expected end of <animation> element.");
		}
		
		// it turned out to have channels - add them
		if( !channels.isEmpty())
		{
			// special filtering for stupid exporters packing each channel into a separate animation
			if( channels.size() == 1)
			{
				pParent.mChannels.add( /*channels.begin()->second*/ channels.values().iterator().next());
			} else
			{
				// else create the animation, if not done yet, and store the channels
				if( anim == null)
				{
					anim = new COLAnimation();
					anim.mName = animName;
					pParent.mSubAnims.add( anim);
				}
//				for( ChannelMap::const_iterator it = channels.begin(); it != channels.end(); ++it)
//					anim->mChannels.push_back( it->second);
				anim.mChannels.addAll(channels.values());
			}
		}
	}

	/** Reads an animation sampler into the given anim channel */
	void readAnimationSampler(AnimationChannel pChannel) throws IOException{
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "input"))
				{
//					int indexSemantic = getAttribute( "semantic");
//					const char* semantic = mReader->getAttributeValue( indexSemantic);
//					int indexSource = GetAttribute( "source");
//					const char* source = mReader->getAttributeValue( indexSource);
					
					final String semantic = mReader.getAttributeValue(null, "semantic");
					String source = mReader.getAttributeValue(null, "source");
					if( source.charAt(0) != '#')
						throw new DeadlyImportError( "Unsupported URL format");
//					source++;
					source = source.substring(1);
					
					if( strcmp( semantic, "INPUT") == 0)
						pChannel.mSourceTimes = source;
					else if( strcmp( semantic, "OUTPUT") == 0)
						pChannel.mSourceValues = source;

					if( !isEmptyElementTag())
						skipElement();
				} 
				else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) 
//			{
//				if( strcmp( mReader->getNodeName(), "sampler") != 0)
//					ThrowException( "Expected end of <sampler> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("sampler") != 0){
			throw new DeadlyImportError("Expected end of <sampler> element.");
		}
	}

	/** Reads the skeleton controller library */
	void readControllerLibrary() throws IOException{
		if (isEmptyElementTag())
			return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "controller"))
				{
					// read ID. Ask the spec if it's neccessary or optional... you might be surprised.
//					int attrID = GetAttribute( "id");
//					std::string id = mReader->getAttributeValue( attrID);
					String id = mReader.getAttributeValue(null, "id");

					// create an entry and store it in the library under its ID
//					mControllerLibrary[id] = Controller();
					Controller c;
					mControllerLibrary.put(id, c = new Controller());

					// read on from there
					readController( /*mControllerLibrary[id]*/ c);
				} else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) 
//			{
//				if( strcmp( mReader->getNodeName(), "library_controllers") != 0)
//					ThrowException( "Expected end of <library_controllers> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("library_controllers") != 0)
			throw new DeadlyImportError("Expected end of <library_controllers> element.");
	}

	/** Reads a controller into the given mesh structure */
	void readController( Controller pController) throws IOException{
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				// two types of controllers: "skin" and "morph". Only the first one is relevant, we skip the other
				if( tag.equals( "morph"))
				{
					// should skip everything inside, so there's no danger of catching elements inbetween
					skipElement();
				} 
				else if( tag.equals( "skin"))
				{
					// read the mesh it refers to. According to the spec this could also be another
					// controller, but I refuse to implement every single idea they've come up with
					int sourceIndex = getAttribute( "source");
					pController.mMeshId = mReader.getAttributeValue( sourceIndex) + 1;
				} 
				else if( tag.equals( "bind_shape_matrix"))
				{
					// content is 16 floats to define a matrix... it seems to be important for some models
			      String content = getTextContent();
			      StringTokenizer tokens = new StringTokenizer(content);
			      // read the 16 floats
			      for(int a = 0; a < 16; a++)
			      {
				      // read a number
//			    	  content = fast_atoreal_move<float>( content, pController.mBindShapeMatrix[a]);
			    	  pController.mBindShapeMatrix[a] = Float.parseFloat(tokens.nextToken());
//				      // skip whitespace after it
//				      SkipSpacesAndLineEnd( &content);
			      }
	
			      testClosing( "bind_shape_matrix");
				} 
				else if( tag.equals( "source"))
				{
					// data array - we have specialists to handle this
					readSource();
				} 
				else if( tag.equals( "joints"))
				{
					readControllerJoints( pController);
				}
				else if( tag.equals( "vertex_weights"))
				{
					readControllerWeights( pController);
				}
				else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) 
//			{
//				if( strcmp( mReader->getNodeName(), "controller") == 0)
//					break;
//				else if( strcmp( mReader->getNodeName(), "skin") != 0)
//					ThrowException( "Expected end of <controller> element.");
//			}
		}
		
		String name = mReader.getName();
		if(!name.equals("controller") && name.compareTo("skin") != 0)
			throw new DeadlyImportError("Expected end of <controller> element.");
	}
		
	static int strcmp(String l, String r) {
		return l.compareTo(r);
	}

	/** Reads the joint definitions for the given controller */
	void readControllerJoints( Controller pController) throws IOException{
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				// Input channels for joint data. Two possible semantics: "JOINT" and "INV_BIND_MATRIX"
				if( tag.equals( "input"))
				{
//					int indexSemantic = GetAttribute( "semantic");
//					const char* attrSemantic = mReader->getAttributeValue( indexSemantic);
//					int indexSource = GetAttribute( "source");
//					const char* attrSource = mReader->getAttributeValue( indexSource);
					
					String attrSemantic = mReader.getAttributeValue(null, "semantic");
					String attrSource   = mReader.getAttributeValue(null, "source");

					// local URLS always start with a '#'. We don't support global URLs
//					if( attrSource[0] != '#')
//						ThrowException( boost::str( boost::format( "Unsupported URL format in \"%s\" in source attribute of <joints> data <input> element") % attrSource));
//					attrSource++;
					if(attrSource.charAt(0) != '#')
						throw new DeadlyImportError(String.format("Unsupported URL format in \"%s\" in source attribute of <joints> data <input> element", attrSource));
					attrSource = attrSource.substring(1);

					// parse source URL to corresponding source
					if( strcmp( attrSemantic, "JOINT") == 0)
						pController.mJointNameSource = attrSource;
					else if( strcmp( attrSemantic, "INV_BIND_MATRIX") == 0)
						pController.mJointOffsetMatrixSource = attrSource;
					else
//						ThrowException( boost::str( boost::format( "Unknown semantic \"%s\" in <joints> data <input> element") % attrSemantic));
						throw new DeadlyImportError(String.format("Unknown semantic \"%s\" in <joints> data <input> element", attrSemantic));

					// skip inner data, if present
					if( !isEmptyElementTag())
						skipElement();
				}
				else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) 
//			{
//				if( strcmp( mReader->getNodeName(), "joints") != 0)
//					ThrowException( "Expected end of <joints> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("joints") != 0)
			throw new DeadlyImportError("Expected end of <joints> element.");
	}

	/** Reads the joint weights for the given controller */
	void readControllerWeights( Controller pController) throws IOException{
		// read vertex count from attributes and resize the array accordingly
		int indexCount = getAttribute( "count");
		int vertexCount = AssUtil.parseInt(mReader.getAttributeValue( indexCount));
		pController.mWeightCounts.size( vertexCount);

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				// Input channels for weight data. Two possible semantics: "JOINT" and "WEIGHT"
				if( tag.equals( "input") && vertexCount > 0 )
				{
					InputChannel channel = new InputChannel();

//					int indexSemantic = GetAttribute( "semantic");
//					const char* attrSemantic = mReader->getAttributeValue( indexSemantic);
//					int indexSource = GetAttribute( "source");
//					const char* attrSource = mReader->getAttributeValue( indexSource);
					String attrSemantic = mReader.getAttributeValue(null, "semantic");
					String attrSource   = mReader.getAttributeValue(null, "source");
					int indexOffset = testAttribute( "offset");
					if( indexOffset >= 0)
						channel.mOffset = AssUtil.parseInt(mReader.getAttributeValue( indexOffset));

					// local URLS always start with a '#'. We don't support global URLs
//					if( attrSource[0] != '#')
//						ThrowException( boost::str( boost::format( "Unsupported URL format in \"%s\" in source attribute of <vertex_weights> data <input> element") % attrSource));
//					channel.mAccessor = attrSource + 1;
					if(attrSource.charAt(0) != '#')
						throw new DeadlyImportError(String.format("Unsupported URL format in \"%s\" in source attribute of <vertex_weights> data <input> element", attrSource));
					attrSource = attrSource.substring(1);

					// parse source URL to corresponding source
					if( strcmp( attrSemantic, "JOINT") == 0)
						pController.mWeightInputJoints = channel;
					else if( strcmp( attrSemantic, "WEIGHT") == 0)
						pController.mWeightInputWeights = channel;
					else
//						ThrowException( boost::str( boost::format( "Unknown semantic \"%s\" in <vertex_weights> data <input> element") % attrSemantic));
						throw new DeadlyImportError(String.format("Unknown semantic \"%s\" in <vertex_weights> data <input> element", attrSemantic));

					// skip inner data, if present
					if( !isEmptyElementTag())
						skipElement();
				}
				else if( tag.equals( "vcount") && vertexCount > 0 )
				{
					// read weight count per vertex
					String text = getTextContent();
					StringTokenizer tokens = new StringTokenizer(text);
					int numWeights = 0;
//					for( std::vector<size_t>::iterator it = pController.mWeightCounts.begin(); it != pController.mWeightCounts.end(); ++it)
					for (int i = 0; i < pController.mWeightCounts.size(); i++)
					{
//						if( *text == 0)
						if(!tokens.hasMoreTokens())
							throw new DeadlyImportError( "Out of data while reading <vcount>");
						
//						*it = strtoul10( text, &text);
						int it = AssUtil.parseInt(tokens.nextToken());
						numWeights += it;
//						SkipSpacesAndLineEnd( &text);
						pController.mWeightCounts.set(i, it);
					}

					testClosing( "vcount");

					// reserve weight count 
//					pController.mWeights.size( numWeights);
					AssUtil.resize(pController.mWeights, numWeights, IntPair.class);
				}
				else if( tag.equals( "v") && vertexCount > 0 )
				{
					// read JointIndex - WeightIndex pairs
//					const char* text = GetTextContent();
					StringTokenizer tokens = new StringTokenizer(getTextContent());

//					for( std::vector< std::pair<size_t, size_t> >::iterator it = pController.mWeights.begin(); it != pController.mWeights.end(); ++it)
					for( IntPair it : pController.mWeights)
					{
//						if( *text == 0)
//							ThrowException( "Out of data while reading <vertex_weights>");
						if(!tokens.hasMoreTokens())
							throw new DeadlyImportError( "Out of data while reading <vertex_weights>");
						
//						it->first = strtoul10( text, &text);
//						SkipSpacesAndLineEnd( &text);
						it.first = AssUtil.parseInt(tokens.nextToken());
//						if( *text == 0)
//							ThrowException( "Out of data while reading <vertex_weights>");
						if(!tokens.hasMoreTokens())
							throw new DeadlyImportError( "Out of data while reading <vertex_weights>");
//						it->second = strtoul10( text, &text);
//						SkipSpacesAndLineEnd( &text);
						it.first = AssUtil.parseInt(tokens.nextToken());
					}

					testClosing( "v");
				}
				else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) 
//			{
//				if( strcmp( mReader->getNodeName(), "vertex_weights") != 0)
//					ThrowException( "Expected end of <vertex_weights> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("vertex_weights") != 0)
			throw new DeadlyImportError("Expected end of <vertex_weights> element.");
	}

	/** Reads the image library contents */
	void readImageLibrary() throws IOException{
		if( isEmptyElementTag())
			return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "image"))
				{
					// read ID. Another entry which is "optional" by design but obligatory in reality
//					int attrID = getAttribute( "id");
//					std::string id = mReader->getAttributeValue( attrID);
					String id = mReader.getAttributeValue(null, "id");

					// create an entry and store it in the library under its ID
//					mImageLibrary[id] = Image();
					COLImage image = new COLImage();
					mImageLibrary.put(id, image);

					// read on from there
					readImage( /*mImageLibrary[id]*/ image);
				} else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) {
//				if( strcmp( mReader->getNodeName(), "library_images") != 0)
//					ThrowException( "Expected end of <library_images> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("library_images") != 0)
			throw new DeadlyImportError("Expected end of <library_images> element.");
	}

	/** Reads an image entry into the given image */
	void readImage( COLImage pImage) throws IOException{
		int event;
		
		while(/*(event = mReader.next()) != XmlPullParser.END_TAG*/ true)
		{
			event = next();
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				// Need to run different code paths here, depending on the Collada XSD version
				if (tag.equals("image")) {
	                skipElement();
	            }
				else if(  tag.equals( "init_from"))
				{
					if (mFormat == FV_1_4_n) 
					{
						// FIX: C4D exporter writes empty <init_from/> tags
						if (!isEmptyElementTag()) {
							// element content is filename - hopefully
							String sz = testTextContent();
							if (sz != null)pImage.mFileName = sz;
							testClosing( "init_from");
						}
						if (pImage.mFileName.length() == 0) {
							pImage.mFileName = "unknown_texture";
						}
					}
					else if (mFormat == FV_1_5_n) 
					{
						// make sure we skip over mip and array initializations, which
						// we don't support, but which could confuse the loader if 
						// they're not skipped.
						int attrib = testAttribute("array_index");
						if (attrib != -1 && AssUtil.parseInt(mReader.getAttributeValue(attrib)) > 0) {
							if(DefaultLogger.LOG_OUT)
								DefaultLogger.warn("Collada: Ignoring texture array index");
							continue;
						}

						attrib = testAttribute("mip_index");
						if (attrib != -1 && AssUtil.parseInt(mReader.getAttributeValue(attrib)) > 0) {
							if(DefaultLogger.LOG_OUT)
								DefaultLogger.warn("Collada: Ignoring MIP map layer");
							continue;
						}

						// TODO: correctly jump over cube and volume maps?
					}
				}
				else if (mFormat == FV_1_5_n) 
				{
					if( tag.equals( "ref"))
					{
						// element content is filename - hopefully
						String sz = testTextContent();
						if (sz != null)pImage.mFileName = sz;
						testClosing( "ref");
					} 
					else if( tag.equals( "hex") && pImage.mFileName.length() == 0)
					{
						// embedded image. get format
						final int attrib = testAttribute("format");
						if (-1 == attrib && DefaultLogger.LOG_OUT) 
							DefaultLogger.warn("Collada: Unknown image file format");
						else pImage.mEmbeddedFormat = mReader.getAttributeValue(attrib);

//						const char* data = GetTextContent();
//
//						// hexadecimal-encoded binary octets. First of all, find the
//						// required buffer size to reserve enough storage.
//						const char* cur = data;
//						while (!IsSpaceOrNewLine(*cur)) cur++;
//
//						const unsigned int size = (unsigned int)(cur-data) * 2;
//						pImage.mImageData.resize(size);
//						for (unsigned int i = 0; i < size;++i) 
//							pImage.mImageData[i] = HexOctetToDecimal(data+(i<<1));
						
						String data = getTextContent();
						// hexadecimal-encoded binary octets. First of all, find the
						// required buffer size to reserve enough storage.
						int cur = 0;
						while(!ParsingUtil.isSpaceOrNewLine((byte)data.charAt(cur))) cur++;
						
						final int size = cur * 2;
						pImage.mImageData = MemoryUtil.createByteBuffer(size, AssimpConfig.MESH_USE_NATIVE_MEMORY);
						for(int i = 0; i < size; i++)
							pImage.mImageData.put(AssUtil.hexOctetToDecimal(data, i << 1));
						pImage.mImageData.flip();
						testClosing( "hex");
					} 
				}
				else	
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) {
			else if(event == XmlPullParser.END_TAG) {
				if( strcmp( mReader.getName(), "image") == 0)
					break;
			}
		}
	}

	/** Reads the material library */
	void readMaterialLibrary() throws IOException{
		if( isEmptyElementTag())
			return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "material"))
				{
					// read ID. By now you propably know my opinion about this "specification"
//					int attrID = GetAttribute( "id");
//					std::string id = mReader->getAttributeValue( attrID);
					String id = mReader.getAttributeValue(null, "id");

					// create an entry and store it in the library under its ID
//					ReadMaterial(mMaterialLibrary[id] = Material());
					COLMaterial mat = new COLMaterial();
					mMaterialLibrary.put(id, mat);
					readMaterial(mat);
				} else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader.getNodeType() == irr::io::EXN_ELEMENT_END) 
//			{
//				if( strcmp( mReader->getNodeName(), "library_materials") != 0)
//					ThrowException( "Expected end of <library_materials> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("library_materials") != 0)
			throw new DeadlyImportError("Expected end of <library_materials> element.");
	}

	/** Reads a material entry into the given material */
	void readMaterial( COLMaterial pMaterial) throws IOException{
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if (tag.equals("material")) {
	                skipElement();
	            }
				else if( tag.equals( "instance_effect"))
				{
					// referred effect by URL
					int attrUrl = getAttribute( "url");
					String url = mReader.getAttributeValue( attrUrl);
//					if( url[0] != '#')
//						ThrowException( "Unknown reference format");
					if(url.charAt(0) != '#')
						throw new DeadlyImportError("Unknown reference format");

					pMaterial.mEffect = url.substring(1);

					skipElement();
				} else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) {
//				if( strcmp( mReader->getNodeName(), "material") != 0)
//					ThrowException( "Expected end of <material> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("material") != 0)
			throw new DeadlyImportError("Expected end of <material> element.");
	}

	/** Reads the camera library */
	void readCameraLibrary() throws IOException{
		if( isEmptyElementTag())
			return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "camera"))
				{
					// read ID. By now you propably know my opinion about this "specification"
//					int attrID = GetAttribute( "id");
//					std::string id = mReader->getAttributeValue( attrID);
					String id = mReader.getAttributeValue(null, "id");

					// create an entry and store it in the library under its ID
//					Camera& cam = mCameraLibrary[id]; 
					COLCamera cam = new COLCamera();
					mCameraLibrary.put(id, cam);
					int attrID = testAttribute( "name");
					if (attrID != -1) 
						cam.mName = mReader.getAttributeValue( attrID);

					readCamera(cam);

				} else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)	{
//				if( strcmp( mReader->getNodeName(), "library_cameras") != 0)
//					ThrowException( "Expected end of <library_cameras> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("library_cameras") != 0)
			throw new DeadlyImportError("Expected end of <library_cameras> element.");
	}

	/** Reads a camera entry into the given camera */
	void readCamera( COLCamera pCamera)throws IOException{
		int event;
		while( /*mReader->read()*/ true)
		{
			event = next();
//			if( mReader->getNodeType() == irr::io::EXN_ELEMENT) {
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if (tag.equals("camera")) {
	                skipElement();
	            }
				else if (tag.equals("orthographic")) {
					pCamera.mOrtho = true;
				}
				else if (tag.equals("xfov") || tag.equals("xmag")) {
					pCamera.mHorFov = readFloatFromTextContent();
					testClosing((pCamera.mOrtho ? "xmag" : "xfov"));
				}
				else if (tag.equals("yfov") || tag.equals("ymag")) {
					pCamera.mVerFov = readFloatFromTextContent();
					testClosing((pCamera.mOrtho ? "ymag" : "yfov"));
				}
				else if (tag.equals("aspect_ratio")) {
					pCamera.mAspect = readFloatFromTextContent();
					testClosing("aspect_ratio");
				}
				else if (tag.equals("znear")) {
					pCamera.mZNear = readFloatFromTextContent();
					testClosing("znear");
				}
				else if (tag.equals("zfar")) {
					pCamera.mZFar = readFloatFromTextContent();
					testClosing("zfar");
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) {
			else if( event == XmlPullParser.END_TAG) {
				if( strcmp( mReader.getName(), "camera") == 0)
					break;
			}
		}
	}

	/** Reads the light library */
	void readLightLibrary() throws IOException{
		if( isEmptyElementTag())
			return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "light"))
				{
					// read ID. By now you propably know my opinion about this "specification"
//					int attrID = GetAttribute( "id");
//					std::string id = mReader->getAttributeValue( attrID);
					String id = mReader.getAttributeValue(null, "id");

					// create an entry and store it in the library under its ID
//					ReadLight(mLightLibrary[id] = Light());
					COLLight light = new COLLight();
					mLightLibrary.put(id, light);
					readLight(light);

				} else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)	{
//				if( strcmp( mReader->getNodeName(), "library_lights") != 0)
//					ThrowException( "Expected end of <library_lights> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("library_lights") != 0)
			throw new DeadlyImportError("Expected end of <library_lights> element.");
	}

	/** Reads a light entry into the given light */
	void readLight( COLLight pLight)throws IOException{
		int event;
		
		while(/*(event = mReader.next()) != XmlPullParser.END_TAG*/ true)
		{
			event = next();
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
	            if (tag.equals("light")) {
	                skipElement();
	            }
				else if (tag.equals("spot")) {
					pLight.mType = LightSourceType.aiLightSource_SPOT.ordinal();
				}
				else if (tag.equals("ambient")) {
					pLight.mType = aiLightSource_AMBIENT;
				}
				else if (tag.equals("directional")) {
					pLight.mType = LightSourceType.aiLightSource_DIRECTIONAL.ordinal();
				}
				else if (tag.equals("point")) {
					pLight.mType = LightSourceType.aiLightSource_POINT.ordinal();
				}
				else if (tag.equals("color")) {
					// text content contains 3 floats
//					const char* content = GetTextContent();
//					  
//					content = fast_atoreal_move<float>( content, (float&)pLight.mColor.r);
//					SkipSpacesAndLineEnd( &content);
//					
//					content = fast_atoreal_move<float>( content, (float&)pLight.mColor.g);
//					SkipSpacesAndLineEnd( &content);
//
//					content = fast_atoreal_move<float>( content, (float&)pLight.mColor.b);
//					SkipSpacesAndLineEnd( &content);

					StringTokenizer tokens = new StringTokenizer(getTextContent());
					pLight.mColor.x = AssUtil.parseFloat(tokens.nextToken());
					pLight.mColor.y = AssUtil.parseFloat(tokens.nextToken());
					pLight.mColor.z = AssUtil.parseFloat(tokens.nextToken());
					testClosing( "color");
				}
				else if (tag.equals("constant_attenuation")) {
					pLight.mAttConstant = readFloatFromTextContent();
					testClosing("constant_attenuation");
				}
				else if (tag.equals("linear_attenuation")) {
					pLight.mAttLinear = readFloatFromTextContent();
					testClosing("linear_attenuation");
				}
				else if (tag.equals("quadratic_attenuation")) {
					pLight.mAttQuadratic = readFloatFromTextContent();
					testClosing("quadratic_attenuation");
				}
				else if (tag.equals("falloff_angle")) {
					pLight.mFalloffAngle = readFloatFromTextContent();
					testClosing("falloff_angle");
				}
				else if (tag.equals("falloff_exponent")) {
					pLight.mFalloffExponent = readFloatFromTextContent();
					testClosing("falloff_exponent");
				}
				// FCOLLADA extensions 
				// -------------------------------------------------------
				else if (tag.equals("outer_cone")) {
					pLight.mOuterAngle = readFloatFromTextContent();
					testClosing("outer_cone");
				}
				// ... and this one is even deprecated
				else if (tag.equals("penumbra_angle")) {
					pLight.mPenumbraAngle = readFloatFromTextContent();
					testClosing("penumbra_angle");
				}
				else if (tag.equals("intensity")) {
					pLight.mIntensity = readFloatFromTextContent();
					testClosing("intensity");
				}
				else if (tag.equals("falloff")) {
					pLight.mOuterAngle = readFloatFromTextContent();
					testClosing("falloff");
				}
				else if (tag.equals("hotspot_beam")) {
					pLight.mFalloffAngle = readFloatFromTextContent();
					testClosing("hotspot_beam");
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) {
			else if(event == XmlPullParser.END_TAG){
				if( strcmp( mReader.getName(), "light") == 0)
					break;
			}
		}
	}

	/** Reads the effect library */
	void readEffectLibrary()throws IOException{
		if (isEmptyElementTag()) {
			return;
		}

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "effect"))
				{
					// read ID. Do I have to repeat my ranting about "optional" attributes?
//					int attrID = GetAttribute( "id");
//					std::string id = mReader->getAttributeValue( attrID);
					String id = mReader.getAttributeValue(null, "id");
					Effect effect = new Effect();

					// create an entry and store it in the library under its ID
//					mEffectLibrary[id] = Effect();
					mEffectLibrary.put(id, effect);
					// read on from there
					readEffect( /*mEffectLibrary[id]*/ effect);
				} else
				{
					// ignore the rest
					skipElement();
				}
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) {
//				if( strcmp( mReader->getNodeName(), "library_effects") != 0)
//					ThrowException( "Expected end of <library_effects> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("library_effects") != 0)
			throw new DeadlyImportError("Expected end of <library_effects> element.");
	}

	/** Reads an effect entry into the given effect*/
	void readEffect( Effect pEffect)throws IOException{
		// for the moment we don't support any other type of effect.
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
				if( tag.equals( "profile_COMMON"))
					readEffectProfileCommon( pEffect);
				else
					skipElement();
			}
//			else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) 
//			{
//				if( strcmp( mReader->getNodeName(), "effect") != 0)
//					ThrowException( "Expected end of <effect> element.");
//
//				break;
//			}
		}
		
		if(mReader.getName().compareTo("effect") != 0)
			throw new DeadlyImportError("Expected end of <effect> element.");
	}

	/** Reads an COMMON effect profile */
    void readEffectProfileCommon(Effect pEffect)throws IOException{
		int event;
		
		while(/*(event = mReader.next()) != XmlPullParser.END_TAG*/ true)
		{
			event = next();
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "newparam"))	{
    				// save ID
    				int attrSID = getAttribute( "sid");
    				String sid = mReader.getAttributeValue( attrSID);
    				EffectParam param;
//    				pEffect.mParams[sid] = EffectParam();
    				pEffect.mParams.put(sid, param = new EffectParam());
    				readEffectParam( /*pEffect.mParams[sid]*/ param);
    			} 
    			else if( tag.equals( "technique") || tag.equals( "extra"))
    			{
    				// just syntactic sugar
    			}

    			/* Shading modes */
    			else if( tag.equals( "phong"))
    				pEffect.mShadeType = Shade_Phong;
    			else if( tag.equals( "constant"))
    				pEffect.mShadeType = Shade_Constant;
    			else if( tag.equals( "lambert"))
    				pEffect.mShadeType = Shade_Lambert;
    			else if( tag.equals( "blinn"))
    				pEffect.mShadeType = Shade_Blinn;

    			/* Color + texture properties */
    			else if( tag.equals( "emission"))
    				readEffectColor( pEffect.mEmissive, pEffect.mTexEmissive);
    			else if( tag.equals( "ambient"))
    				readEffectColor( pEffect.mAmbient, pEffect.mTexAmbient);
    			else if( tag.equals( "diffuse"))
    				readEffectColor( pEffect.mDiffuse, pEffect.mTexDiffuse);
    			else if( tag.equals( "specular"))
    				readEffectColor( pEffect.mSpecular, pEffect.mTexSpecular);
    			else if( tag.equals( "reflective")) {
    				readEffectColor( pEffect.mReflective, pEffect.mTexReflective);
    			}
    			else if( tag.equals( "transparent")) {
    				readEffectColor( pEffect.mTransparent,pEffect.mTexTransparent);
    			}
    			else if( tag.equals( "shininess"))
    				pEffect.mShininess = readEffectFloat( );
    			else if( tag.equals( "reflectivity"))
    				pEffect.mReflectivity = readEffectFloat();

    			/* Single scalar properties */
    			else if( tag.equals( "transparency"))
    				pEffect.mTransparency = readEffectFloat();
    			else if( tag.equals( "index_of_refraction"))
    				pEffect.mRefractIndex = readEffectFloat();

    			// GOOGLEEARTH/OKINO extensions 
    			// -------------------------------------------------------
    			else if( tag.equals( "double_sided"))
    				pEffect.mDoubleSided = readBoolFromTextContent();

    			// FCOLLADA extensions
    			// -------------------------------------------------------
    			else if( tag.equals( "bump")) {
    				Vector4f dummy = new Vector4f();
    				readEffectColor( dummy,pEffect.mTexBump);
    			}

    			// MAX3D extensions
    			// -------------------------------------------------------
    			else if( tag.equals( "wireframe"))	{
    				pEffect.mWireframe = readBoolFromTextContent();
    				testClosing( "wireframe");
    			}
    			else if( tag.equals( "faceted"))	{
    				pEffect.mFaceted = readBoolFromTextContent();
    				testClosing( "faceted");
    			}
    			else 
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
    		else if( /*mReader->getNodeType() == irr::io::EXN_ELEMENT_END*/ event == XmlPullParser.END_TAG) {
    			if( strcmp( mReader.getName(), "profile_COMMON") == 0)
    			{
    				break;
    			} 
    		}
    	}
    }

	/** Read sampler properties */
    void readSamplerProperties( Sampler out)throws IOException{
    	if (isEmptyElementTag()) {
    		return;
    	}

    	while(/* mReader->read()*/ true)
    	{
    		int event = next();
//    		if( mReader->getNodeType() == irr::io::EXN_ELEMENT) {
    		if( event == XmlPullParser.START_TAG){
    			String tag = mReader.getName();
    			// MAYA extensions
    			// -------------------------------------------------------
    			if( tag.equals( "wrapU"))		{
    				out.mWrapU = readBoolFromTextContent();
    				testClosing( "wrapU");
    			}
    			else if( tag.equals( "wrapV"))	{
    				out.mWrapV = readBoolFromTextContent();
    				testClosing( "wrapV");
    			}
    			else if( tag.equals( "mirrorU"))		{
    				out.mMirrorU = readBoolFromTextContent();
    				testClosing( "mirrorU");
    			}
    			else if( tag.equals( "mirrorV"))	{
    				out.mMirrorV = readBoolFromTextContent();
    				testClosing( "mirrorV");
    			}
    			else if( tag.equals( "repeatU"))	{
    				out.mTransform.mScaling.x = readFloatFromTextContent();
    				testClosing( "repeatU");
    			}
    			else if( tag.equals( "repeatV"))	{
    				out.mTransform.mScaling.y = readFloatFromTextContent();
    				testClosing( "repeatV");
    			}
    			else if( tag.equals( "offsetU"))	{
    				out.mTransform.mTranslation.x = readFloatFromTextContent();
    				testClosing( "offsetU");
    			}
    			else if( tag.equals( "offsetV"))	{
    				out.mTransform.mTranslation.y = readFloatFromTextContent();
    				testClosing( "offsetV");
    			}
    			else if( tag.equals( "rotateUV"))	{
    				out.mTransform.mRotation = readFloatFromTextContent();
    				testClosing( "rotateUV");
    			}
    			else if( tag.equals( "blend_mode"))	{
    				
    				String sz = getTextContent();
    				// http://www.feelingsoftware.com/content/view/55/72/lang,en/
    				// NONE, OVER, IN, OUT, ADD, SUBTRACT, MULTIPLY, DIFFERENCE, LIGHTEN, DARKEN, SATURATE, DESATURATE and ILLUMINATE
    				if (0 == strcmp(sz,"ADD")) 
    					out.mOp = TextureOp.aiTextureOp_Add;
    				else if (0 == strcmp(sz,"SUBTRACT")) 
    					out.mOp = TextureOp.aiTextureOp_Subtract;
    				else if (0 == strcmp(sz,"MULTIPLY")) 
    					out.mOp = TextureOp.aiTextureOp_Multiply;
    				else  {
    					if(DefaultLogger.LOG_OUT)
    						DefaultLogger.warn("Collada: Unsupported MAYA texture blend mode");
    				}
    				testClosing( "blend_mode");
    			}
    			// OKINO extensions
    			// -------------------------------------------------------
    			else if( tag.equals( "weighting"))	{
    				out.mWeighting = readFloatFromTextContent();
    				testClosing( "weighting");
    			}
    			else if( tag.equals( "mix_with_previous_layer"))	{
    				out.mMixWithPrevious = readFloatFromTextContent();
    				testClosing( "mix_with_previous_layer");
    			}
    			// MAX3D extensions
    			// -------------------------------------------------------
    			else if( tag.equals( "amount"))	{
    				out.mWeighting = readFloatFromTextContent();
    				testClosing( "amount");
    			}
    		}
    		else if( event == XmlPullParser.END_TAG /*mReader->getNodeType() == irr::io::EXN_ELEMENT_END*/) {
    			if( strcmp( mReader.getName(), "technique") == 0)
    				break;
    		}
    	}
    }

	/** Reads an effect entry containing a color or a texture defining that color */
    void readEffectColor(Vector4f pColor, Sampler pSampler)throws IOException{
    	if (isEmptyElementTag())
    		return;

    	// Save current element name
    	String curElem = mReader.getName();

    	while( /*mReader->read()*/ true)
    	{
    		int event = next();
//    		if( mReader->getNodeType() == irr::io::EXN_ELEMENT) {
    		if(event == XmlPullParser.START_TAG){
    			String tag = mReader.getName();
    			if( tag.equals( "color"))
    			{
    				// text content contains 4 floats
    				String content = getTextContent();

//    				content = fast_atoreal_move<float>( content, (float&)pColor.r);
//    				SkipSpacesAndLineEnd( &content);
//
//    				content = fast_atoreal_move<float>( content, (float&)pColor.g);
//    				SkipSpacesAndLineEnd( &content);
//
//    				content = fast_atoreal_move<float>( content, (float&)pColor.b);
//    				SkipSpacesAndLineEnd( &content);
//
//    				content = fast_atoreal_move<float>( content, (float&)pColor.a);
//    				SkipSpacesAndLineEnd( &content);
    				StringTokenizer tokens = new StringTokenizer(content);
    				pColor.x = AssUtil.parseFloat(tokens.nextToken());
    				pColor.y = AssUtil.parseFloat(tokens.nextToken());
    				pColor.z = AssUtil.parseFloat(tokens.nextToken());
    				pColor.w = AssUtil.parseFloat(tokens.nextToken());
    				
    				testClosing( "color");
    			} 
    			else if( tag.equals( "texture"))
    			{
    				// get name of source textur/sampler
    				int attrTex = getAttribute( "texture");
    				pSampler.mName = mReader.getAttributeValue( attrTex);

    				// get name of UV source channel. Specification demands it to be there, but some exporters
    				// don't write it. It will be the default UV channel in case it's missing.
    				attrTex = testAttribute( "texcoord");
    				if( attrTex >= 0 )
    	  				pSampler.mUVChannel = mReader.getAttributeValue( attrTex);
    				//SkipElement();
    			}
    			else if( tag.equals( "technique"))
    			{
    				final int _profile = getAttribute( "profile");
    				String profile = mReader.getAttributeValue( _profile );

    				// Some extensions are quite useful ... ReadSamplerProperties processes
    				// several extensions in MAYA, OKINO and MAX3D profiles.
    				if (strcmp(profile,"MAYA") == 0|| strcmp(profile,"MAX3D") == 0|| strcmp(profile,"OKINO") == 0)
    				{
    					// get more information on this sampler
    					readSamplerProperties(pSampler);
    				}
    				else skipElement();
    			}
    			else if( !tag.equals( "extra"))
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END){
//    			if (mReader->getNodeName() == curElem)
    		else if(event == XmlPullParser.END_TAG ){
    			if(mReader.getName().equals(curElem))
    				break;
    		}
    	}
    }

	/** Reads an effect entry containing a float */
    float readEffectFloat()throws IOException{
    	float pFloat = 0;
    	while( true/*mReader->read()*/)
    	{
    		int event = next();
//    		if( mReader->getNodeType() == irr::io::EXN_ELEMENT) {
    		if( event == XmlPullParser.START_TAG){
    			String tag = mReader.getName();
    			if( tag.equals( "float"))
    			{
    				// text content contains a single floats
//    				const char* content = GetTextContent();
//    				content = fast_atoreal_move<float>( content, pFloat);
//    				SkipSpacesAndLineEnd( &content);

    				pFloat = AssUtil.parseFloat(getTextContent());
    				testClosing( "float");
    			} else
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
    		else if( event == XmlPullParser.END_TAG /*mReader->getNodeType() == irr::io::EXN_ELEMENT_END*/){
    			break;
    		}
    	}
    	
    	return pFloat;
    }

	/** Reads an effect parameter specification of any kind */
    void readEffectParam(EffectParam pParam)throws IOException{
    	while( true/*mReader->read()*/)
    	{
    		int event = next();
//    		if( mReader->getNodeType() == irr::io::EXN_ELEMENT) {
    		if( event == XmlPullParser.START_TAG){
    			String tag = mReader.getName();
    			if( tag.equals( "surface"))
    			{
    				// image ID given inside <init_from> tags
    				testOpening( "init_from");
    				String content = getTextContent();
    				pParam.mType = Param_Surface;
    				pParam.mReference = content;
    				testClosing( "init_from");

    				// don't care for remaining stuff
    				skipElement( "surface");
    			} 
    			else if( tag.equals( "sampler2D"))
    			{
    				// surface ID is given inside <source> tags
    				testOpening( "source");
    				String content = getTextContent();
    				pParam.mType = Param_Sampler;
    				pParam.mReference = content;
    				testClosing( "source");

    				// don't care for remaining stuff
    				skipElement( "sampler2D");
    			} else
    			{
    				// ignore unknown element
    				skipElement();
    			}
    		}
    		else if( event == XmlPullParser.END_TAG /*mReader->getNodeType() == irr::io::EXN_ELEMENT_END*/) {
    			break;
    		}
    	}
    }

	/** Reads the geometry library contents */
    void readGeometryLibrary()throws IOException{
    	if(isEmptyElementTag())
    		return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "geometry"))
    			{
    				// read ID. Another entry which is "optional" by design but obligatory in reality
    				int indexID = getAttribute( "id");
    				String id = mReader.getAttributeValue( indexID);

    				// TODO: (thom) support SIDs
    				// ai_assert( TestAttribute( "sid") == -1);

    				// create a mesh and store it in the library under its ID
    				COLMesh mesh = new COLMesh();
//    				mMeshLibrary[id] = mesh;
    				mMeshLibrary.put(id, mesh);
                    
                    // read the mesh name if it exists
                    final int nameIndex = testAttribute("name");
                    if(nameIndex != -1)
                    {
                        mesh.mName = mReader.getAttributeValue(nameIndex);
                    }

    				// read on from there
    				readGeometry( mesh);
    			} else
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
//    		{
//    			if( strcmp( mReader->getNodeName(), "library_geometries") != 0)
//    				ThrowException( "Expected end of <library_geometries> element.");
//
//    			break;
//    		}
    	}
		
		if(mReader.getName().compareTo("library_geometries") != 0)
			throw new DeadlyImportError("Expected end of <library_geometries> element.");
    }

	/** Reads a geometry from the geometry library. */
    void readGeometry( COLMesh pMesh)throws IOException{
    	if( isEmptyElementTag())
    		return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "mesh"))
    			{
    				// read on from there
    				readMesh( pMesh);
    			} else
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
//    		{
//    			if( strcmp( mReader->getNodeName(), "geometry") != 0)
//    				ThrowException( "Expected end of <geometry> element.");
//
//    			break;
//    		}
    	}
		
		if(mReader.getName().compareTo("geometry") != 0)
			throw new DeadlyImportError("Expected end of <geometry> element.");
    }

	/** Reads a mesh from the geometry library */
    void readMesh( COLMesh pMesh)throws IOException{
    	if(isEmptyElementTag())
    		return;

    	while( /*mReader->read()*/ true)
    	{
    		int event = next();
    		if( event == XmlPullParser.START_TAG /*mReader->getNodeType() == irr::io::EXN_ELEMENT*/)
    		{
    			String tag = mReader.getName();
    			if( tag.equals( "source"))
    			{
    				// we have professionals dealing with this
    				readSource();
    			}
    			else if( tag.equals( "vertices"))
    			{
    				// read per-vertex mesh data
    				readVertexData( pMesh);
    			}
    			else if( tag.equals( "triangles") || tag.equals( "lines") || tag.equals( "linestrips")
    				|| tag.equals( "polygons") || tag.equals( "polylist") || tag.equals( "trifans") || tag.equals( "tristrips")) 
    			{
    				// read per-index mesh data and faces setup
    				readIndexData( pMesh);
    			} else
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
    		else if( event == XmlPullParser.END_TAG /*mReader->getNodeType() == irr::io::EXN_ELEMENT_END*/)
    		{
    			String tag = mReader.getName();
    			if( strcmp( tag, "technique_common") == 0)
    			{
    				// end of another meaningless element - read over it
    			} 
    			else if( strcmp(tag, "mesh") == 0)
    			{
    				// end of <mesh> element - we're done here
    				break;
    			} else
    			{
    				// everything else should be punished
    				throw new DeadlyImportError( "Expected end of <mesh> element.");
    			}
    		}
    	}
    }

	/** Reads a source element - a combination of raw data and an accessor defining 
	 * things that should not be redefinable. Yes, that's another rant.
	 */
    void readSource()throws IOException{
    	int indexID = getAttribute( "id");
    	String sourceID = mReader.getAttributeValue( indexID);

    	while( true/*mReader->read()*/)
    	{
    		int event = next();
//    		if( mReader->getNodeType() == irr::io::EXN_ELEMENT)
    		if(event == XmlPullParser.START_TAG) 
    		{
    			String tag = mReader.getName();
    			if( tag.equals( "float_array") || tag.equals( "IDREF_array") || tag.equals( "Name_array"))
    			{
    				readDataArray();
    			}
    			else if( tag.equals( "technique_common"))
    			{
    				// I don't care for your profiles 
    			}
    			else if( tag.equals( "accessor"))
    			{
    				readAccessor( sourceID);
    			} else
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
    		else if(event == XmlPullParser.END_TAG)
    		{
    			String tag = mReader.getName();
    			if( strcmp(tag, "source") == 0)
    			{
    				// end of <source> - we're done
    				break;
    			}
    			else if( strcmp(tag, "technique_common") == 0)
    			{
    				// end of another meaningless element - read over it
    			} else
    			{
    				// everything else should be punished
    				throw new DeadlyImportError( "Expected end of <source> element.");
    			}
    		}
    	}
    }

	/** Reads a data array holding a number of elements, and stores it in the global library.
	 * Currently supported are array of floats and arrays of strings.
	 */
    void readDataArray()throws IOException{
    	String elmName = mReader.getName();
    	boolean isStringArray = (elmName.equals("IDREF_array") || elmName.equals("Name_array"));
        boolean isEmptyElement = isEmptyElementTag();

    	// read attributes
    	int indexID = getAttribute( "id");
    	String id = mReader.getAttributeValue( indexID);
    	int indexCount = getAttribute( "count");
    	int count = AssUtil.parseInt(mReader.getAttributeValue( indexCount));
    	String content = testTextContent();

      // read values and store inside an array in the data library
//      mDataLibrary[id] = Data();
//      Data& data = mDataLibrary[id];
    	Data data = new Data();
    	mDataLibrary.put(id, data);
    	data.mIsStringArray = isStringArray;

    	// some exporters write empty data arrays, but we need to conserve them anyways because others might reference them
    	if (content != null) 
    	{ 
    		StringTokenizer tokens = new StringTokenizer(content);
    		if( isStringArray)
    		{
//    			data.mStrings.reserve( count);
    			data.mStrings = new ArrayList<String>(count);
//    			std::string s;
    			for(int a = 0; a < count; a++)
    			{
//    				if( *content == 0)
//    					ThrowException( "Expected more values while reading IDREF_array contents.");

//    				s.clear();
//    				while( !IsSpaceOrNewLine( *content))
//    					s += *content++;
//    				data.mStrings.push_back( s);
//
//    				SkipSpacesAndLineEnd( &content);
    				
    				if(!tokens.hasMoreTokens())
    					throw new DeadlyImportError("Expected more values while reading IDREF_array contents.");
    				
    				data.mStrings.add(tokens.nextToken());
    			}
    		} else
    		{
//    			data.mValues.reserve( count);
    			data.mValues = new FloatArrayList(count);

    			for(int a = 0; a < count; a++)
    			{
//    				if( *content == 0)
//    					ThrowException( "Expected more values while reading float_array contents.");
//
//    				float value;
//    				// read a number
//    				content = fast_atoreal_move<float>( content, value);
//    				data.mValues.push_back( value);
//    				// skip whitespace after it
//    				SkipSpacesAndLineEnd( &content);
    				
    				if(!tokens.hasMoreTokens())
    					throw new DeadlyImportError("Expected more values while reading float_array contents.");
    				
    				data.mValues.add(AssUtil.parseFloat(tokens.nextToken()));
    			}
    		}
    	}

      // test for closing tag
      if( !isEmptyElement )
        testClosing( elmName);
    }

	/** Reads an accessor and stores it in the global library under the given ID - 
	 * accessors use the ID of the parent <source> element
	 */
    void readAccessor(String pID)throws IOException{
    	// read accessor attributes
    	int attrSource = getAttribute( "source");
    	String source = mReader.getAttributeValue( attrSource);
    	if( source.charAt(0) != '#')
//    		ThrowException( boost::str( boost::format( "Unknown reference format in url \"%s\" in source attribute of <accessor> element.") % source));
    		throw new DeadlyImportError(String.format("Unknown reference format in url \"%s\" in source attribute of <accessor> element.", source));
    	int attrCount = getAttribute( "count");
    	int count = AssUtil.parseInt(mReader.getAttributeValue( attrCount));
    	int attrOffset = testAttribute( "offset");
    	int offset = 0;
    	if( attrOffset > -1)
    		offset = AssUtil.parseInt(mReader.getAttributeValue( attrOffset));
    	int attrStride = testAttribute( "stride");
    	int stride = 1;
    	if( attrStride > -1)
    		stride = AssUtil.parseInt(mReader.getAttributeValue( attrStride));

    	// store in the library under the given ID
//    	mAccessorLibrary[pID] = Accessor();
//    	Accessor& acc = mAccessorLibrary[pID];
    	Accessor acc = new Accessor();
    	mAccessorLibrary.put(pID, acc);
    	acc.mCount = count;
    	acc.mOffset = offset;
    	acc.mStride = stride;
    	acc.mSource = source+1; // ignore the leading '#'
    	acc.mSize = 0; // gets incremented with every param

    	// and read the components
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "param"))
    			{
    				// read data param
    				int attrName = testAttribute( "name");
    				char name = 0;
    				if( attrName > -1)
    				{
    					name = mReader.getAttributeValue( attrName).charAt(0);

    					// analyse for common type components and store it's sub-offset in the corresponding field

    					/* Cartesian coordinates */
    					if( name == 'X') acc.mSubOffset[0] = acc.mParams.size();
    					else if( name == 'Y') acc.mSubOffset[1] = acc.mParams.size();
    					else if( name == 'Z') acc.mSubOffset[2] = acc.mParams.size();

    					/* RGBA colors */
    					else if( name == 'R') acc.mSubOffset[0] = acc.mParams.size();
    					else if( name == 'G') acc.mSubOffset[1] = acc.mParams.size();
    					else if( name == 'B') acc.mSubOffset[2] = acc.mParams.size();
    					else if( name == 'A') acc.mSubOffset[3] = acc.mParams.size();

    					/* UVWQ (STPQ) texture coordinates */
    					else if( name == 'S') acc.mSubOffset[0] = acc.mParams.size();
    					else if( name == 'T') acc.mSubOffset[1] = acc.mParams.size();
    					else if( name == 'P') acc.mSubOffset[2] = acc.mParams.size();
    				//	else if( name == "Q") acc.mSubOffset[3] = acc.mParams.size(); 
    					/* 4D uv coordinates are not supported in Assimp */

    					/* Generic extra data, interpreted as UV data, too*/
    					else if( name == 'U') acc.mSubOffset[0] = acc.mParams.size();
    					else if( name == 'V') acc.mSubOffset[1] = acc.mParams.size();
    					//else
    					//	DefaultLogger::get()->warn( boost::str( boost::format( "Unknown accessor parameter \"%s\". Ignoring data channel.") % name));
    				}

    				// read data type
    				int attrType = testAttribute( "type");
    				if( attrType > -1)
    				{
    					// for the moment we only distinguish between a 4x4 matrix and anything else. 
    					// TODO: (thom) I don't have a spec here at work. Check if there are other multi-value types
    					// which should be tested for here.
    					String type = mReader.getAttributeValue( attrType);
    					if( type.equals("float4x4"))
    						acc.mSize += 16;
    					else 
    						acc.mSize += 1;
    				}

    				if(name != 0)
    					acc.mParams.add( Character.toString(name));

    				// skip remaining stuff of this element, if any
    				skipElement();
    			} else
    			{
//    				ThrowException( boost::str( boost::format( "Unexpected sub element <%s> in tag <accessor>") % mReader->getNodeName()));
    				throw new DeadlyImportError(String.format("Unexpected sub element <%s> in tag <accessor>", mReader.getName()));
    			}
    		} 
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
//    		{
//    			if( strcmp( mReader->getNodeName(), "accessor") != 0)
//    				ThrowException( "Expected end of <accessor> element.");
//    			break;
//    		}
    	}
		
		if(mReader.getName().compareTo("accessor") != 0)
			throw new DeadlyImportError("Expected end of <accessor> element.");
    }

	/** Reads input declarations of per-vertex mesh data into the given mesh */
    void readVertexData( COLMesh pMesh) throws IOException{
    	// extract the ID of the <vertices> element. Not that we care, but to catch strange referencing schemes we should warn about
    	int attrID= getAttribute( "id");
    	pMesh.mVertexID = mReader.getAttributeValue( attrID);

    	// a number of <input> elements
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "input"))
    			{
    				readInputChannel( pMesh.mPerVertexData);
    			} else
    			{
//    				ThrowException( boost::str( boost::format( "Unexpected sub element <%s> in tag <vertices>") % mReader->getNodeName()));
    				throw new DeadlyImportError(String.format("Unexpected sub element <%s> in tag <vertices>", mReader.getName()));
    			}
    		} 
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
//    		{
//    			if( strcmp( mReader->getNodeName(), "vertices") != 0)
//    				ThrowException( "Expected end of <vertices> element.");
//
//    			break;
//    		}
    	}
		
		if(mReader.getName().compareTo("vertices") != 0)
			throw new DeadlyImportError("Expected end of <vertices> element.");
    }

	/** Reads input declarations of per-index mesh data into the given mesh */
    void readIndexData( COLMesh pMesh)throws IOException{
    	IntArrayList vcount = new IntArrayList();
    	ArrayList<InputChannel> perIndexData = new ArrayList<InputChannel>();

    	// read primitive count from the attribute
    	int attrCount = getAttribute( "count");
    	int numPrimitives = AssUtil.parseInt(mReader.getAttributeValue( attrCount));

    	// material subgroup 
    	int attrMaterial = testAttribute( "material");
    	SubMesh subgroup = new SubMesh();
    	if( attrMaterial > -1)
    		subgroup.mMaterial = mReader.getAttributeValue( attrMaterial);
    	subgroup.mNumFaces = numPrimitives;
    	pMesh.mSubMeshes.add( subgroup);

    	// distinguish between polys and triangles
    	String elementName = mReader.getName();
    	int primType = Prim_Invalid;
    	if( elementName.equals( "lines"))
    		primType = Prim_Lines;
    	else if( elementName.equals( "linestrips"))
    		primType = Prim_LineStrip;
    	else if( elementName.equals( "polygons"))
    		primType = Prim_Polygon;
    	else if( elementName.equals( "polylist"))
    		primType = Prim_Polylist;
    	else if( elementName.equals( "triangles"))
    		primType = Prim_Triangles;
    	else if( elementName.equals( "trifans"))
    		primType = Prim_TriFans;
    	else if( elementName.equals( "tristrips"))
    		primType = Prim_TriStrips;

//    	ai_assert( primType != Prim_Invalid);

    	// also a number of <input> elements, but in addition a <p> primitive collection and propably index counts for all primitives
		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "input"))
    			{
    				readInputChannel( perIndexData);
    			} 
    			else if( tag.equals( "vcount"))
    			{
    				if( !isEmptyElementTag())
    				{
    					if (numPrimitives > 0)	// It is possible to define a mesh without any primitives
    					{
    						// case <polylist> - specifies the number of indices for each polygon
    						String content = getTextContent();  // TODO bad performance.
    						StringTokenizer tokens = new StringTokenizer(content);
//    						vcount.reserve( numPrimitives);
    						vcount = new IntArrayList(numPrimitives);
    						for(int a = 0; a < numPrimitives; a++)
    						{
//    							if( *content == 0)
//    								ThrowException( "Expected more values while reading <vcount> contents.");
    							if(!tokens.hasMoreTokens())
    								throw new DeadlyImportError("Expected more values while reading <vcount> contents.");
    							// read a number
    							vcount.add(AssUtil.parseInt(tokens.nextToken()) /*(size_t) strtoul10( content, &content)*/);
    							// skip whitespace after it
//    							SkipSpacesAndLineEnd( &content);
    						}
    					}

    					testClosing( "vcount");
    				}
    			}
    			else if( tag.equals( "p"))
    			{
    				if( !isEmptyElementTag())
    				{
    					// now here the actual fun starts - these are the indices to construct the mesh data from
    					readPrimitives( pMesh, perIndexData, numPrimitives, vcount, primType);
    				}
    			} else
    			{
//    				ThrowException( boost::str( boost::format( "Unexpected sub element <%s> in tag <%s>") % mReader->getNodeName() % elementName));
    				throw new DeadlyImportError(String.format("Unexpected sub element <%s> in tag <%s>" , mReader.getName() , elementName));
    			}
    		} 
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
//    		{
//    			if( mReader->getNodeName() != elementName)
//    				ThrowException( boost::str( boost::format( "Expected end of <%s> element.") % elementName));
//
//    			break;
//    		}
    	}
		
		if(mReader.getName().compareTo(elementName) != 0)
			throw new DeadlyImportError(String.format("Expected end of <%s> element.", elementName ));
    }

	/** Reads a single input channel element and stores it in the given array, if valid */
    void readInputChannel( ArrayList<InputChannel> poChannels)throws IOException{
    	InputChannel channel = new InputChannel();
    	
    	// read semantic
    	int attrSemantic = getAttribute( "semantic");
    	String semantic = mReader.getAttributeValue( attrSemantic);
    	channel.mType = getTypeForSemantic( semantic);

    	// read source
    	int attrSource = getAttribute( "source");
    	String source = mReader.getAttributeValue( attrSource);
    	if( source.charAt(0) != '#')
//    		ThrowException( boost::str( boost::format( "Unknown reference format in url \"%s\" in source attribute of <input> element.") % source));
    		throw new DeadlyImportError(String.format("Unknown reference format in url \"%s\" in source attribute of <input> element.", source));
    	channel.mAccessor = source+1; // skipping the leading #, hopefully the remaining text is the accessor ID only

    	// read index offset, if per-index <input>
    	int attrOffset = testAttribute( "offset");
    	if( attrOffset > -1)
    		channel.mOffset = AssUtil.parseInt(mReader.getAttributeValue( attrOffset));

    	// read set if texture coordinates
    	if(channel.mType == IT_Texcoord || channel.mType == IT_Color){
    		int attrSet = testAttribute("set");
    		if(attrSet > -1){
    			attrSet = AssUtil.parseInt(mReader.getAttributeValue( attrSet));
    			if(attrSet < 0)
//    				ThrowException( boost::str( boost::format( "Invalid index \"%d\" in set attribute of <input> element") % (attrSet)));
    				throw new DeadlyImportError(String.format("Invalid index \"%d\" in set attribute of <input> element",attrSet));
    			
    			channel.mIndex = attrSet;
    		}
    	}

    	// store, if valid type
    	if( channel.mType != IT_Invalid)
    		poChannels.add( channel);

    	// skip remaining stuff of this element, if any
    	skipElement();
    }

	/** Reads a <p> primitive index list and assembles the mesh data into the given mesh */
    void readPrimitives( COLMesh pMesh, ArrayList<InputChannel> pPerIndexChannels, 
		int pNumPrimitives, IntArrayList pVCount, int pPrimType)throws IOException{
    	// determine number of indices coming per vertex 
    	// find the offset index for all per-vertex channels
    	int numOffsets = 1;
    	int perVertexOffset = -1; // invalid value
//    	BOOST_FOREACH( const InputChannel& channel, pPerIndexChannels)
    	for (InputChannel channel : pPerIndexChannels)
    	{
    		numOffsets = Math.max( numOffsets, channel.mOffset+1);
    		if( channel.mType == IT_Vertex)
    			perVertexOffset = channel.mOffset;
    	}

    	// determine the expected number of indices 
    	int expectedPointCount = 0;
    	switch( pPrimType)
    	{
    		case Prim_Polylist:
    		{
//    			BOOST_FOREACH( size_t i, pVCount)
    			for(int k = 0; k < pVCount.size(); k++)
    				expectedPointCount += pVCount.getInt(k);
    			
    			break;
    		}
    		case Prim_Lines:
    			expectedPointCount = 2 * pNumPrimitives;
    			break;
    		case Prim_Triangles:
    			expectedPointCount = 3 * pNumPrimitives;
    			break;
    		default:
    			// other primitive types don't state the index count upfront... we need to guess
    			break;
    	}

    	// and read all indices into a temporary array
//    	std::vector<size_t> indices;
    	IntArrayList indices = new IntArrayList();
    	if( expectedPointCount > 0)
//    		indices.reserve( expectedPointCount * numOffsets);
    		indices.ensureCapacity(expectedPointCount * numOffsets);

    	if (pNumPrimitives > 0)	// It is possible to not contain any indicies
    	{
    		String content = getTextContent();
    		StringTokenizer tokens = new StringTokenizer(content);
    		while( /**content != 0*/ tokens.hasMoreTokens())
    		{
    			// read a value. 
    			// Hack: (thom) Some exporters put negative indices sometimes. We just try to carry on anyways.
//    			int value = std::max( 0, strtol10( content, &content));
//    			indices.push_back( size_t( value));
//    			// skip whitespace after it
//    			SkipSpacesAndLineEnd( &content);
    			indices.add(Math.max(0, AssUtil.parseInt(tokens.nextToken())));
    		}
    	}

    	// complain if the index count doesn't fit
    	if( expectedPointCount > 0 && indices.size() != expectedPointCount * numOffsets)
    		throw new DeadlyImportError( "Expected different index count in <p> element.");
    	else if( expectedPointCount == 0 && (indices.size() % numOffsets) != 0)
    		throw new DeadlyImportError( "Expected different index count in <p> element.");

    	// find the data for all sources
//      for( std::vector<InputChannel>::iterator it = pMesh->mPerVertexData.begin(); it != pMesh->mPerVertexData.end(); ++it)
    	for( InputChannel input : pMesh.mPerVertexData)
    	{
//        InputChannel& input = *it;
    		if( input.mResolved != null)
    			continue;

    		// find accessor
    		input.mResolved = resolveLibraryReference( mAccessorLibrary, input.mAccessor);
    		// resolve accessor's data pointer as well, if neccessary
    		Accessor acc = input.mResolved;
    		if( acc.mData == null)
    			acc.mData = resolveLibraryReference( mDataLibrary, acc.mSource);
    	}
    	// and the same for the per-index channels
//      for( std::vector<InputChannel>::iterator it = pPerIndexChannels.begin(); it != pPerIndexChannels.end(); ++it)
      for (InputChannel input : pPerIndexChannels)
      {
//        InputChannel& input = *it;
    		if( input.mResolved != null)
    			continue;

    		// ignore vertex pointer, it doesn't refer to an accessor
    		if( input.mType == IT_Vertex)
    		{
    			// warn if the vertex channel does not refer to the <vertices> element in the same mesh
    			if( input.mAccessor != pMesh.mVertexID)
    				throw new DeadlyImportError( "Unsupported vertex referencing scheme.");
    			continue;
    		}

    		// find accessor
    		input.mResolved = resolveLibraryReference( mAccessorLibrary, input.mAccessor);
    		// resolve accessor's data pointer as well, if neccessary
    		Accessor acc = input.mResolved;
    		if( acc.mData == null)
    			acc.mData = resolveLibraryReference( mDataLibrary, acc.mSource);
    	}


    	// now assemble vertex data according to those indices
//    	std::vector<size_t>::const_iterator idx = indices.begin();
        int idx = 0;

    	// For continued primitives, the given count does not come all in one <p>, but only one primitive per <p>
    	int numPrimitives = pNumPrimitives;
    	if( pPrimType == Prim_TriFans || pPrimType == Prim_Polygon)
    		numPrimitives = 1;

    	if(pMesh.mFacePosIndices == null)
    		pMesh.mFacePosIndices = new IntArrayList(numPrimitives);
    	else
    		pMesh.mFaceSize.ensureCapacity(numPrimitives);
    	
    	if(pMesh.mFacePosIndices == null)
    		pMesh.mFacePosIndices = new IntArrayList(indices.size()/numOffsets);
    	else
    		pMesh.mFacePosIndices.ensureCapacity(indices.size()/numOffsets);
    	
//    	pMesh.mFacePosIndices.reserve( indices.size() / numOffsets);

    	for( int a = 0; a < numPrimitives; a++)
    	{
    		// determine number of points for this primitive
    		int numPoints = 0;
    		switch( pPrimType)
    		{
    			case Prim_Lines:
    				numPoints = 2; 
    				break;
    			case Prim_Triangles: 
    				numPoints = 3; 
    				break;
    			case Prim_Polylist: 
    				numPoints = pVCount.getInt(a);
    				break;
    			case Prim_TriFans: 
    			case Prim_Polygon:
    				numPoints = indices.size() / numOffsets; 
    				break;
    			default:
    				// LineStrip and TriStrip not supported due to expected index unmangling
    				throw new DeadlyImportError( "Unsupported primitive type.");
    		}

    		// store the face size to later reconstruct the face from
    		pMesh.mFaceSize.add( numPoints);

    		// gather that number of vertices
    		for( int b = 0; b < numPoints; b++)
    		{
    			// read all indices for this vertex. Yes, in a hacky local array
//    			ai_assert( numOffsets < 20 && perVertexOffset < 20);
    			int[] vindex = new int[20];
    			for(int offsets = 0; offsets < numOffsets; ++offsets)
    				vindex[offsets] = indices.getInt(idx++);

    			// extract per-vertex channels using the global per-vertex offset
//          	for( std::vector<InputChannel>::iterator it = pMesh->mPerVertexData.begin(); it != pMesh->mPerVertexData.end(); ++it)
    			for(InputChannel it : pMesh.mPerVertexData)
    				extractDataObjectFromChannel( it, vindex[perVertexOffset], pMesh);
    			// and extract per-index channels using there specified offset
//    			for( std::vector<InputChannel>::iterator it = pPerIndexChannels.begin(); it != pPerIndexChannels.end(); ++it)
    			for(InputChannel it : pPerIndexChannels)
    				extractDataObjectFromChannel( it, vindex[it.mOffset], pMesh);

    			// store the vertex-data index for later assignment of bone vertex weights
    			pMesh.mFacePosIndices.add( vindex[perVertexOffset]);
    		}
    	}


    	// if I ever get my hands on that guy who invented this steaming pile of indirection...
    	testClosing( "p");
    }
    
    /** Finds the item in the given library by its reference, throws if not found */
    static<T> T resolveLibraryReference(Map<String, T> pLibrary, String pURL){
    	T value = pLibrary.get(pURL);
    	if(value == null)
    		throw new DeadlyImportError(String.format("Unable to resolve library reference \"%s\".", pURL));
    	
    	return value;
    }

	/** Extracts a single object from an input channel and stores it in the appropriate mesh data array */
	void extractDataObjectFromChannel(InputChannel pInput, int pLocalIndex, COLMesh pMesh){
		// ignore vertex referrer - we handle them that separate
		if( pInput.mType == IT_Vertex)
			return;

		Accessor acc = pInput.mResolved;
		if( pLocalIndex >= acc.mCount)
//			ThrowException( boost::str( boost::format( "Invalid data index (%d/%d) in primitive specification") % pLocalIndex % acc.mCount));
			throw new DeadlyImportError(String.format("Invalid data index (%d/%d) in primitive specification", pLocalIndex , acc.mCount));

		// get a pointer to the start of the data object referred to by the accessor and the local index
//		const float* dataObject = &(acc.mData.mValues[0]) + acc.mOffset + pLocalIndex* acc.mStride;
		int dataObject = acc.mOffset + pLocalIndex* acc.mStride;

		// assemble according to the accessors component sub-offset list. We don't care, yet,
		// what kind of object exactly we're extracting here
		float[] obj = new float[4];
		for( int c = 0; c < 4; ++c)
			obj[c] = acc.mData.mValues.getFloat(dataObject + acc.mSubOffset[c])/*dataObject*/;

		// now we reinterpret it according to the type we're reading here
		switch( pInput.mType)
		{
			case IT_Position: // ignore all position streams except 0 - there can be only one position
				if( pInput.mIndex == 0)
//					pMesh.mPositions.push_back( aiVector3D( obj[0], obj[1], obj[2])); 
					pMesh.mPositions.put(obj, 0, 3);
				else 
					DefaultLogger.error("Collada: just one vertex position stream supported");
				break;
			case IT_Normal: 
				// pad to current vertex count if necessary
//				if( pMesh.mNormals.size() < pMesh.mPositions.size()-1)
//					pMesh.mNormals.insert( pMesh->mNormals.end(), pMesh->mPositions.size() - pMesh->mNormals.size() - 1, aiVector3D( 0, 1, 0));
				if( pMesh.mNormals.position() < pMesh.mPositions.position() - 3){
					int count = (pMesh.mPositions.position() - 3 - pMesh.mNormals.position())/3;
					while(count -- > 0)
						pMesh.mNormals.put(0).put(1).put(0);
				}
				// ignore all normal streams except 0 - there can be only one normal
				if( pInput.mIndex == 0)
//					pMesh->mNormals.push_back( aiVector3D( obj[0], obj[1], obj[2])); 
					pMesh.mNormals.put(obj, 0, 3);
				else 
					DefaultLogger.error("Collada: just one vertex normal stream supported");
				break;
			case IT_Tangent: 
				// pad to current vertex count if necessary
//				if( pMesh->mTangents.size() < pMesh->mPositions.size()-1)
//					pMesh->mTangents.insert( pMesh->mTangents.end(), pMesh->mPositions.size() - pMesh->mTangents.size() - 1, aiVector3D( 1, 0, 0));
				if( pMesh.mTangents.position() < pMesh.mPositions.position() - 3){
					int count = (pMesh.mPositions.position() - 3 - pMesh.mTangents.position())/3;
					while(count -- > 0)
						pMesh.mTangents.put(1).put(0).put(0);
				}
				// ignore all tangent streams except 0 - there can be only one tangent
				if( pInput.mIndex == 0)
//					pMesh->mTangents.push_back( aiVector3D( obj[0], obj[1], obj[2])); 
					pMesh.mTangents.put(obj, 0, 3);
				else 
					DefaultLogger.error("Collada: just one vertex tangent stream supported");
				break;
			case IT_Bitangent: 
				// pad to current vertex count if necessary
//				if( pMesh->mBitangents.size() < pMesh->mPositions.size()-1)
//					pMesh->mBitangents.insert( pMesh->mBitangents.end(), pMesh->mPositions.size() - pMesh->mBitangents.size() - 1, aiVector3D( 0, 0, 1));
				if( pMesh.mBitangents.position() < pMesh.mPositions.position() - 3){
					int count = (pMesh.mPositions.position() - 3 - pMesh.mBitangents.position())/3;
					while(count -- > 0)
						pMesh.mBitangents.put(0).put(0).put(1);
				}
				
				// ignore all bitangent streams except 0 - there can be only one bitangent
				if( pInput.mIndex == 0)
//					pMesh->mBitangents.push_back( aiVector3D( obj[0], obj[1], obj[2])); 
					pMesh.mBitangents.put(obj, 0, 3);
				else 
					DefaultLogger.error("Collada: just one vertex bitangent stream supported");
				break;
			case IT_Texcoord: 
				// up to 4 texture coord sets are fine, ignore the others
				if( pInput.mIndex < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS) 
				{
					// pad to current vertex count if necessary
//					if( pMesh->mTexCoords[pInput.mIndex].size() < pMesh->mPositions.size()-1)
//						pMesh->mTexCoords[pInput.mIndex].insert( pMesh->mTexCoords[pInput.mIndex].end(), 
//							pMesh->mPositions.size() - pMesh->mTexCoords[pInput.mIndex].size() - 1, aiVector3D( 0, 0, 0));
					if( pMesh.mTexCoords[pInput.mIndex].position() < pMesh.mPositions.position() - 3){
						int count = (pMesh.mPositions.position() - 3 - pMesh.mTexCoords[pInput.mIndex].position())/3;
						while(count -- > 0)
							pMesh.mTexCoords[pInput.mIndex].put(0).put(0).put(0);
					}
//					pMesh->mTexCoords[pInput.mIndex].push_back( aiVector3D( obj[0], obj[1], obj[2]));
					pMesh.mTexCoords[pInput.mIndex].put(obj, 0, 3);
					if (0 != acc.mSubOffset[2] || 0 != acc.mSubOffset[3]) /* hack ... consider cleaner solution */
						pMesh.mNumUVComponents[pInput.mIndex]=3;
				}	else 
				{
					DefaultLogger.error("Collada: too many texture coordinate sets. Skipping.");
				}
				break;
			case IT_Color: 
				// up to 4 color sets are fine, ignore the others
				if( pInput.mIndex < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS)
				{
					// pad to current vertex count if necessary
//					if( pMesh->mColors[pInput.mIndex].size() < pMesh->mPositions.size()-1)
//						pMesh->mColors[pInput.mIndex].insert( pMesh->mColors[pInput.mIndex].end(), 
//							pMesh->mPositions.size() - pMesh->mColors[pInput.mIndex].size() - 1, aiColor4D( 0, 0, 0, 1));
					if( pMesh.mColors[pInput.mIndex].position()/4 < pMesh.mPositions.position()/3 - 1){
						int count = pMesh.mPositions.position()/3 - 1 - pMesh.mColors[pInput.mIndex].position()/4;
						while(count -- > 0)
							pMesh.mColors[pInput.mIndex].put(0).put(0).put(0).put(0);
					}
//					aiColor4D result(0, 0, 0, 1);
					Vector4f result = new Vector4f(0, 0, 0, 1);
					for (int i = 0; i < pInput.mResolved.mSize; ++i)
					{
						result.set(i, obj[pInput.mResolved.mSubOffset[i]]);
					}
//					pMesh.mColors[pInput.mIndex].push_back(result);
					result.store(pMesh.mColors[pInput.mIndex]);
				} else 
				{
					DefaultLogger.error("Collada: too many vertex color sets. Skipping.");
				}

				break;
			default:
				// IT_Invalid and IT_Vertex 
//				ai_assert(false && "shouldn't ever get here");
				break;
		}
	}

	/** Reads the library of node hierarchies and scene parts */
    void readSceneLibrary()throws IOException{
    	if( isEmptyElementTag())
    		return;

    	while(/* mReader->read()*/ true)
    	{
    		int event = next();
//    		if( mReader->getNodeType() == irr::io::EXN_ELEMENT)
    		if(event == XmlPullParser.START_TAG)
    		{
    			String tag = mReader.getName();
    			// a visual scene - generate root node under its ID and let ReadNode() do the recursive work
    			if( tag.equals( "visual_scene"))
    			{
    				// read ID. Is optional according to the spec, but how on earth should a scene_instance refer to it then?
    				int indexID = getAttribute( "id");
    				String attrID = mReader.getAttributeValue( indexID);

    				// read name if given. 
    				int indexName = testAttribute( "name");
    				String attrName = "unnamed";
    				if( indexName > -1)
    					attrName = mReader.getAttributeValue( indexName);

    				// create a node and store it in the library under its ID
    				COLNode node = new COLNode();
    				node.mID = attrID;
    				node.mName = attrName;
//    				mNodeLibrary[node->mID] = node;
    				mNodeLibrary.put(node.mID, node);

    				readSceneNode( node);
    			} else
    			{
    				// ignore the rest
    				skipElement();
    			}
    		}
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
    		else if(event == XmlPullParser.END_TAG)
    		{
    			if( strcmp( mReader.getName(), "library_visual_scenes") == 0)
    				//ThrowException( "Expected end of \"library_visual_scenes\" element.");

    			break;
    		}
    	}
    }

	/** Reads a scene node's contents including children and stores it in the given node */
    void readSceneNode( COLNode pNode) throws IOException{
    	// quit immediately on <bla/> elements
    	if( isEmptyElementTag())
    		return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "node"))
    			{
    				COLNode child = new COLNode();
    				int attrID = testAttribute( "id");
    				if( attrID > -1)
    					child.mID = mReader.getAttributeValue( attrID);
    				int attrSID = testAttribute( "sid");
    				if( attrSID > -1)
    					child.mSID = mReader.getAttributeValue( attrSID);

    				int attrName = testAttribute( "name");
    				if( attrName > -1)
    					child.mName = mReader.getAttributeValue( attrName);

    				// TODO: (thom) support SIDs
    				// ai_assert( TestAttribute( "sid") == -1);

    				if (pNode != null) 
    				{
    					pNode.mChildren.add( child);
    					child.mParent = pNode;
    				}
    				else 
    				{
    					// no parent node given, probably called from <library_nodes> element.
    					// create new node in node library
//    					mNodeLibrary[child->mID] = child;
    					mNodeLibrary.put(child.mID, child);
    				}

    				// read on recursively from there
    				readSceneNode( child);
    				continue;
    			}
    			// For any further stuff we need a valid node to work on
    			else if (pNode == null)
    				continue;

    			if( tag.equals( "lookat"))
    				readNodeTransformation( pNode, TF_LOOKAT);
    			else if( tag.equals( "matrix"))
    				readNodeTransformation( pNode, TF_MATRIX);
    			else if( tag.equals( "rotate"))
    				readNodeTransformation( pNode, TF_ROTATE);
    			else if( tag.equals( "scale"))
    				readNodeTransformation( pNode, TF_SCALE);
    			else if( tag.equals( "skew"))
    				readNodeTransformation( pNode, TF_SKEW);
    			else if( tag.equals( "translate"))
    				readNodeTransformation( pNode, TF_TRANSLATE);
    			else if( tag.equals( "render") && pNode.mParent == null && 0 == pNode.mPrimaryCamera.length())
    			{
    				// ... scene evaluation or, in other words, postprocessing pipeline,
    				// or, again in other words, a turing-complete description how to
    				// render a Collada scene. The only thing that is interesting for
    				// us is the primary camera.
    				int attrId = testAttribute("camera_node");
    				if (-1 != attrId) 
    				{
    					String s = mReader.getAttributeValue(attrId);
    					if (s.charAt(0) != '#')
    						DefaultLogger.error("Collada: Unresolved reference format of camera");
    					else 
    						pNode.mPrimaryCamera = s.substring(1);
    				}
    			}
    			else if( tag.equals( "instance_node")) 
    			{
    				// find the node in the library
    				int attrID = testAttribute( "url");
    				if( attrID != -1) 
    				{
    					String s = mReader.getAttributeValue(attrID);
    					if (s.charAt(0) != '#')
    						DefaultLogger.error("Collada: Unresolved reference format of node");
    					else 
    					{
    						NodeInstance back;
    						pNode.mNodeInstances.add(back = new NodeInstance());
    						back.mNode = s.substring(1);
    					}
    				}
    			} 
    			else if( tag.equals( "instance_geometry") || tag.equals( "instance_controller"))
    			{
    				// Reference to a mesh or controller, with possible material associations
    				readNodeGeometry( pNode);
    			}
    			else if( tag.equals( "instance_light")) 
    			{
    				// Reference to a light, name given in 'url' attribute
    				int attrID = testAttribute("url");
    				if (-1 == attrID)
    					DefaultLogger.warn("Collada: Expected url attribute in <instance_light> element");
    				else 
    				{
    					String url = mReader.getAttributeValue( attrID);
    					if( url.charAt(0) != '#')
    						throw new DeadlyImportError( "Unknown reference format in <instance_light> element");

    					LightInstance back;
    					pNode.mLights.add(back = new LightInstance());
    					back.mLight = url.substring(1);
    				}
    			}
    			else if( tag.equals( "instance_camera")) 
    			{
    				// Reference to a camera, name given in 'url' attribute
    				int attrID = testAttribute("url");
    				if (-1 == attrID && DefaultLogger.LOG_OUT)
    					DefaultLogger.warn("Collada: Expected url attribute in <instance_camera> element");
    				else 
    				{
    					String url = mReader.getAttributeValue( attrID);
    					if( url.charAt(0) != '#')
    						throw new DeadlyImportError( "Unknown reference format in <instance_camera> element");

    					CameraInstance back;
    					pNode.mCameras.add(back = new CameraInstance());
    					back.mCamera = url.substring(1);
    				}
    			}
    			else
    			{
    				// skip everything else for the moment
    				skipElement();
    			}
    		} 
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END) {
//    			break;
//    		}
    	}
    }

 // how many parameters to read per transformation type
	static final int sNumParameters[] = { 9, 4, 3, 3, 7, 16 };
	/** Reads a node transformation entry of the given type and adds it to the given node's transformation list. */
    void readNodeTransformation( COLNode pNode, int pType)throws IOException{
    	if( isEmptyElementTag())
    		return;

    	String tagName = mReader.getName();

    	Transform tf = new Transform();
    	tf.mType = pType;
    	
    	// read SID
    	int indexSID = testAttribute( "sid");
    	if( indexSID >= 0)
    		tf.mID = mReader.getAttributeValue( indexSID);

//    	const char* content = GetTextContent();
    	StringTokenizer tokens = new StringTokenizer(getTextContent());

    	// read as many parameters and store in the transformation
    	for(int a = 0; a < sNumParameters[pType]; a++)
    	{
//    		// read a number
//    		content = fast_atoreal_move<float>( content, tf.f[a]);
//    		// skip whitespace after it
//    		SkipSpacesAndLineEnd( &content);
    		tf.f[a] = AssUtil.parseFloat(tokens.nextToken());
    	}

    	// place the transformation at the queue of the node
    	pNode.mTransforms.add( tf);

    	// and consume the closing tag
    	testClosing( tagName);
    }

	/** Reads a mesh reference in a node and adds it to the node's mesh list */
    void readNodeGeometry( COLNode pNode)throws IOException{
    	// referred mesh is given as an attribute of the <instance_geometry> element
    	int attrUrl = getAttribute( "url");
    	String url = mReader.getAttributeValue( attrUrl);
    	if( url.charAt(0) != '#')
    		throw new DeadlyImportError( "Unknown reference format");
    	
    	MeshInstance instance = new MeshInstance();
    	instance.mMeshOrController = url.substring(1); // skipping the leading #

    	if( !isEmptyElementTag())
    	{
    		// read material associations. Ignore additional elements inbetween
    		while( /*mReader->read()*/ true)
    		{
    			int event = next();
    			if( event == XmlPullParser.START_TAG/*mReader->getNodeType() == irr::io::EXN_ELEMENT*/)	
    			{
    				if( mReader.getName().equals( "instance_material"))
    				{
    					// read ID of the geometry subgroup and the target material
    					int attrGroup = getAttribute( "symbol");
    					String group = mReader.getAttributeValue( attrGroup);
    					int attrMaterial = getAttribute( "target");
    					String urlMat = mReader.getAttributeValue( attrMaterial);
    					SemanticMappingTable s = new SemanticMappingTable();
    					if( urlMat.charAt(0) == '#')
    						urlMat = urlMat.substring(1);

    					s.mMatName = urlMat;

    					// resolve further material details + THIS UGLY AND NASTY semantic mapping stuff
    					if( !isEmptyElementTag())
    						readMaterialVertexInputBinding(s);

    					// store the association
//    					instance.mMaterials[group] = s;
    					instance.mMaterials.put(group, s);
    				} 
    			} 
    			else if( event == XmlPullParser.END_TAG/*mReader->getNodeType() == irr::io::EXN_ELEMENT_END*/)	
    			{
    				if( strcmp( mReader.getName(), "instance_geometry") == 0 
    					|| strcmp( mReader.getName(), "instance_controller") == 0)
    					break;
    			} 
    		}
    	}

    	// store it
    	pNode.mMeshes.add( instance);
    }

	/** Reads the collada scene */
    void readScene()throws IOException{
    	if(isEmptyElementTag())
    		return;

		int event;
		
		while((event = next()) != XmlPullParser.END_TAG)
		{
			if(event == XmlPullParser.START_TAG)
			{
				String tag = mReader.getName();
    			if( tag.equals( "instance_visual_scene"))
    			{
    				// should be the first and only occurence
    				if( mRootNode != null)
    					throw new DeadlyImportError( "Invalid scene containing multiple root nodes in <instance_visual_scene> element");

    				// read the url of the scene to instance. Should be of format "#some_name"
    				int urlIndex = getAttribute( "url");
    				String url = mReader.getAttributeValue( urlIndex);
    				if( url.charAt(0) != '#')
    					throw new DeadlyImportError( "Unknown reference format in <instance_visual_scene> element");

    				// find the referred scene, skip the leading # 
    				COLNode sit = mNodeLibrary.get( url+1);
    				if( sit == null/*mNodeLibrary.end()*/)
    					throw new DeadlyImportError( "Unable to resolve visual_scene reference \"" + (url) + "\" in <instance_visual_scene> element.");
    				mRootNode = sit;
    			} else	{
    				skipElement();
    			}
    		} 
//    		else if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END){
//    			break;
//    		} 
    	}
    }

	// Processes bind_vertex_input and bind elements
    void readMaterialVertexInputBinding( SemanticMappingTable tbl)throws IOException{
    	while(/* mReader->read()*/ true)
    	{
    		int event = next();
    		if( event == XmlPullParser.START_TAG/*mReader->getNodeType() == irr::io::EXN_ELEMENT*/)	{
    			String tag = mReader.getName();
    			if( tag.equals( "bind_vertex_input"))
    			{
    				InputSemanticMapEntry vn = new InputSemanticMapEntry();

    				// effect semantic
    				int n = getAttribute("semantic");
    				String s = mReader.getAttributeValue(n);

    				// input semantic
    				n = getAttribute("input_semantic");
    				vn.mType = getTypeForSemantic( mReader.getAttributeValue(n) );
    				
    				// index of input set
    				n = testAttribute("input_set");
    				if (-1 != n)
    					vn.mSet = AssUtil.parseInt(mReader.getAttributeValue(n));

//    				tbl.mMap[s] = vn;
    				tbl.mMap.put(s, vn);
    			} 
    			else if( tag.equals( "bind")) {
    				DefaultLogger.warn("Collada: Found unsupported <bind> element");
    			}
    		} 
    		else if( event == XmlPullParser.END_TAG/*mReader->getNodeType() == irr::io::EXN_ELEMENT_END*/)	{
    			if( strcmp( mReader.getName(), "instance_material") == 0)
    				break;
    		} 
    	}
    }

	/** Skips all data until the end node of the current element */
	void skipElement()throws IOException{
		// nothing to skip if it's an <element />
		if(isEmptyElementTag())
			return;

		// reroute
		skipElement( mReader.getName());
	}

	/** Skips all data until the end node of the given element */
	void skipElement( String element)throws IOException{
		// copy the current node's name because it'a pointer to the reader's internal buffer, 
		// which is going to change with the upcoming parsing 
		while( /*mReader->read()*/ true)
		{
//			if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END)
//				if( mReader->getNodeName() == element)
//					break;
			if(next() == XmlPullParser.END_TAG)
				if(mReader.getName().equals(element))
					break;
		}
	}

	/** Tests for the opening tag of the given element, throws an exception if not found */
	void testOpening( String pName) throws IOException{
		// read element start
		int event = next();
		if( /*!mReader->read()*/ event == XmlPullParser.END_DOCUMENT)
//			ThrowException( boost::str( boost::format( "Unexpected end of file while beginning of <%s> element.") % pName));
			throw new DeadlyImportError(String.format("Unexpected end of file while beginning of <%s> element.", pName));
		// whitespace in front is ok, just read again if found
		if(event == XmlPullParser.TEXT)
//			if( !mReader->read())
			if((event = next()) == XmlPullParser.END_DOCUMENT)
//				ThrowException( boost::str( boost::format( "Unexpected end of file while reading beginning of <%s> element.") % pName));
				throw new DeadlyImportError(String.format("Unexpected end of file while beginning of <%s> element.", pName));

//		if( mReader->getNodeType() != irr::io::EXN_ELEMENT || strcmp( mReader->getNodeName(), pName) != 0)
//			ThrowException( boost::str( boost::format( "Expected start of <%s> element.") % pName));
		if(event != XmlPullParser.START_TAG || strcmp(mReader.getName(), pName) != 0)
			throw new DeadlyImportError(String.format("Expected start of <%s> element.", pName));
	}

	/** Tests for the closing tag of the given element, throws an exception if not found 
	 * @throws  
	 * @throws IOException */
	void testClosing(String pName) throws IOException{
		// check if we're already on the closing tag and return right away
//		if( mReader->getNodeType() == irr::io::EXN_ELEMENT_END && strcmp( mReader->getNodeName(), pName) == 0)
//			return;
		try {
			if(mReader.getEventType() == XmlPullParser.END_TAG && strcmp(mReader.getName(), pName) == 0)
				return;

			int event;
			if((event = mReader.next()) == XmlPullParser.END_DOCUMENT)
				throw new DeadlyImportError(String.format("Unexpected end of file while beginning of <%s> element.", pName));
			if(event == XmlPullParser.TEXT)
				if((event = mReader.next()) == XmlPullParser.END_DOCUMENT)
					throw new DeadlyImportError(String.format("Unexpected end of file while beginning of <%s> element.", pName));
			if(event != XmlPullParser.END_TAG || strcmp(mReader.getName(), pName) != 0)
				throw new DeadlyImportError(String.format("Expected start of <%s> element.", pName));
		} catch (XmlPullParserException e) {
			throw new DeadlyImportError(String.format("Unexpected end of file while beginning of <%s> element.", pName));
		}
	}

	/** Checks the present element for the presence of the attribute, returns its index 
	    or throws an exception if not found */
	int getAttribute(String pAttr){
		int index = testAttribute( pAttr);
		if( index != -1)
			return index;

		// attribute not found -> throw an exception
//		ThrowException( boost::str( boost::format( "Expected attribute \"%s\" for element <%s>.") % pAttr % mReader->getNodeName()));
		throw new DeadlyImportError(String.format("Expected attribute \"%s\" for element <%s>.", pAttr ,mReader.getName()));
	}

	/** Returns the index of the named attribute or -1 if not found. Does not throw,
	    therefore useful for optional attributes */
	int testAttribute(String pAttr){
		for(int i = 0; i < mReader.getAttributeCount(); i++){
			if(mReader.getAttributeName(i).equals(pAttr))
				return i;
		}
		
		return -1;
	}

	/** Reads the text contents of an element, throws an exception if not given. 
	    Skips leading whitespace. */
	String getTextContent() throws IOException{
		String sz = testTextContent();
		if(sz == null)
			throw new DeadlyImportError("Invalid contents in element \"n\".");
		
		return sz;
	}

	/** Reads the text contents of an element, returns NULL if not given.
	    Skips leading whitespace. 
	 * @throws  */
	String testTextContent() throws IOException{
//		if(mReader.getEventType() != XmlPullParser.START_TAG || mReader.isEmptyElementTag())
//			return null;
//		
//		// read contents of the element
//		int event = mReader.next();
//		if(event != XmlPullParser.TEXT)
//			return null;
//		
//		// skip leading whitespace
//		return mReader.getText().trim();
		
		try {
			return mReader.nextText();
		} catch (XmlPullParserException e) {
			throw new DeadlyImportError(e);
		}
	}

	/** Reads a single bool from current text content */
//	bool ReadBoolFromTextContent();

	/** Reads a single float from current text content */
//	float ReadFloatFromTextContent();

	/** Calculates the resulting transformation from all the given transform steps */
	void calculateResultTransform( ArrayList<Transform> pTransforms, Matrix4f res){

//		for( std::vector<Transform>::const_iterator it = pTransforms.begin(); it != pTransforms.end(); ++it)
		Matrix4f tmp = new Matrix4f();
		for(Transform tf : pTransforms)
		{
//			const Transform& tf = *it;
			switch( tf.mType)
			{
				case TF_LOOKAT:
	      {
//	        aiVector3D pos( tf.f[0], tf.f[1], tf.f[2]);
//	        aiVector3D dstPos( tf.f[3], tf.f[4], tf.f[5]);
//	        aiVector3D up = aiVector3D( tf.f[6], tf.f[7], tf.f[8]).Normalize();
//	        aiVector3D dir = aiVector3D( dstPos - pos).Normalize();
//	        aiVector3D right = (dir ^ up).Normalize();
//
//	        res *= aiMatrix4x4( 
//	          right.x, up.x, -dir.x, pos.x, 
//	          right.y, up.y, -dir.y, pos.y,
//	          right.z, up.z, -dir.z, pos.z,
//	          0, 0, 0, 1);
	    	  
	    	  Vector3f pos = new Vector3f(tf.f[0], tf.f[1], tf.f[2]);
	    	  Vector3f dstPos = new Vector3f(tf.f[3], tf.f[4], tf.f[5]);
	    	  Vector3f up = new Vector3f( tf.f[6], tf.f[7], tf.f[8]);
	    	  up.normalise();
	    	  Vector3f dir = Vector3f.sub(dstPos, pos, null);
	    	  dir.normalise();
	    	  Vector3f right = Vector3f.cross(dir, up, null);
	    	  right.normalise();
	    	  
	    	  tmp.m00 = right.x;
	    	  tmp.m01 = right.y;
	    	  tmp.m02 = right.z;
	    	  tmp.m03 = 0;
	    	  tmp.m10 = up.x;
	    	  tmp.m11 = up.y;
	    	  tmp.m12 = up.z;
	    	  tmp.m13 = 0;
	    	  tmp.m20 = -dir.x;
	    	  tmp.m21 = -dir.y;
	    	  tmp.m22 = -dir.z;
	    	  tmp.m23 = 0;
	    	  tmp.m30 = pos.x;
	    	  tmp.m31 = pos.y;
	    	  tmp.m32 = pos.z;
	    	  tmp.m33 = 1;
	    	  
	    	  Matrix4f.mul(res, tmp, res);
					break;
	      }
				case TF_ROTATE:
				{
//					aiMatrix4x4 rot;
//					float angle = tf.f[3] * float( AI_MATH_PI) / 180.0f;
//					aiVector3D axis( tf.f[0], tf.f[1], tf.f[2]);
//					aiMatrix4x4::Rotation( angle, axis, rot);
//					res *= rot;
					
					float angle = tf.f[3] * (float)Math.PI /180.0f;
					Quaternion quat = new Quaternion();
					quat.setFromAxisAngle(tf.f[0], tf.f[1], tf.f[2], angle);
					quat.toMatrix(tmp);
					tmp.m13 = tmp.m23 = tmp.m03 = 0;
					tmp.m33 = 1;
					Matrix4f.mul(res, tmp, res);
					break;
				}
				case TF_TRANSLATE:
				{
//					aiMatrix4x4 trans;
//					aiMatrix4x4::Translation( aiVector3D( tf.f[0], tf.f[1], tf.f[2]), trans);
//					res *= trans;
					res.translate(tf.f[0], tf.f[1], tf.f[2]);
					break;
				}
				case TF_SCALE:
				{
//					aiMatrix4x4 scale( tf.f[0], 0.0f, 0.0f, 0.0f, 0.0f, tf.f[1], 0.0f, 0.0f, 0.0f, 0.0f, tf.f[2], 0.0f, 
//						0.0f, 0.0f, 0.0f, 1.0f);
//					res *= scale;
					res.scale(tf.f[0], tf.f[1], tf.f[2]);
					break;
				}
				case TF_SKEW:
					// TODO: (thom)
//					ai_assert( false);
					break;
				case TF_MATRIX:
				{
//					aiMatrix4x4 mat( tf.f[0], tf.f[1], tf.f[2], tf.f[3], tf.f[4], tf.f[5], tf.f[6], tf.f[7],
//						tf.f[8], tf.f[9], tf.f[10], tf.f[11], tf.f[12], tf.f[13], tf.f[14], tf.f[15]);
//					res *= mat;
					tmp.loadTranspose(tf.f, 0);
					Matrix4f.mul(res, tmp, res);
					break;
				}
				default: 
//					ai_assert( false);
					break;
			}
		}

	}

	/** Determines the input data type for the given semantic string */
	int getTypeForSemantic( String pSemantic){
		if( pSemantic.equals( "POSITION"))
			return IT_Position;
		else if( pSemantic.equals( "TEXCOORD"))
			return IT_Texcoord;
		else if( pSemantic.equals( "NORMAL"))
			return IT_Normal;
		else if( pSemantic.equals( "COLOR"))
			return IT_Color;
		else if( pSemantic.equals( "VERTEX"))
			return IT_Vertex;
		else if( pSemantic.equals( "BINORMAL") || pSemantic.equals(  "TEXBINORMAL"))
			return IT_Bitangent;
		else if( pSemantic.equals( "TANGENT") || pSemantic.equals( "TEXTANGENT"))
			return IT_Tangent;

		if(DefaultLogger.LOG_OUT)
			DefaultLogger.warn(String.format("Unknown vertex input type \"%s\". Ignoring.", pSemantic));
		return IT_Invalid;
	}

	/** Finds the item in the given library by its reference, throws if not found */
//	template <typename Type> const Type& ResolveLibraryReference(
//		const std::map<std::string, Type>& pLibrary, const std::string& pURL) const;
}
