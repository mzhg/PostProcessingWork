package assimp.importer.csm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.FileUtils;
import assimp.common.Importer;
import assimp.common.ImporterDesc;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.ParsingUtil;
import assimp.common.Scene;
import assimp.common.SkeletonMeshBuilder;
import assimp.common.VectorKey;

/** Importer class to load MOCAPs in CharacterStudio Motion format.<p>
*
*  A very rudimentary loader for the moment. No support for the hierarchy,
*  every marker is returned as child of root.<p>
*
*  Link to file format specification:
*  <max_8_dvd>\samples\Motion\Docs\CSM.rtf
*/
public class CSMImporter extends BaseImporter{

	private static final ImporterDesc dest = new ImporterDesc(
			"CharacterStudio Motion Importer (MoCap)",
			"",
			"",
			"",
			ImporterDesc.aiImporterFlags_SupportTextFlavour,
			0,
			0,
			0,
			0,
			"csm" );
	
	private boolean noSkeletonMesh;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		// check file extension 
		String extension = getExtension(pFile);
		
		if( extension.equals("csm"))
			return true;

		if ((checkSig || extension.length() == 0) && pIOHandler != null) {
			String tokens[] = {"$Filename"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return dest; }
	
	@Override
	public void setupProperties(Importer pImp) {
		noSkeletonMesh = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_NO_SKELETON_MESHES,0) != 0;
	}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		ByteBuffer bytes = FileUtils.loadText(pFile, false, false);
		ParsingUtil buffer = new ParsingUtil(bytes);
		
		Animation anim = new Animation();
		int first = 0, last = 0x00ffffff;

		// now process the file and look out for '$' sections
		while (true)	{
//			SkipSpaces(&buffer);
			buffer.skipSpaces();
			if ('\0' == buffer.get())
				break;

			if ('$'  == buffer.get())	{
//				++buffer;
				buffer.inCre();
//				if (TokenMatchI(buffer,"firstframe",10))	{
				if (buffer.tokenMatchI("firstframe")) {
//					SkipSpaces(&buffer);
					buffer.skipSpaces();
//					first = strtol10(buffer,&buffer);
					first = buffer.strtoul10();
				}
//				else if (TokenMatchI(buffer,"lastframe",9))		{
				else if( buffer.tokenMatchI("lastframe")) {
//					SkipSpaces(&buffer);
//					last = strtol10(buffer,&buffer);
					buffer.skipSpaces();
					last = buffer.strtoul10();
				}
				else if (buffer.tokenMatchI("rate")/*TokenMatchI(buffer,"rate",4)*/)	{
//					SkipSpaces(&buffer);
					buffer.skipSpaces();
//					float d;
//					buffer = fast_atoreal_move<float>(buffer,d);
//					anim->mTicksPerSecond = d;
					anim.mTicksPerSecond = buffer.fast_atoreal_move(true);
				}
				else if (buffer.tokenMatchI("order")/*TokenMatchI(buffer,"order",5)*/)	{
//					std::vector< aiNodeAnim* > anims_temp;
//					anims_temp.reserve(30);
					ArrayList<NodeAnim> anims_temp = new ArrayList<NodeAnim>(30);
					while (/*1*/ true)	{
//						SkipSpaces(&buffer);
						buffer.skipSpaces();
						if (ParsingUtil.isLineEnd((byte)buffer.get()) && /*SkipSpacesAndLineEnd(&buffer)*/buffer.skipSpacesAndLineEnd() && buffer.get() == '$')
							break; // next section

						// Construct a new node animation channel and setup its name
						NodeAnim nda;
						anims_temp.add(nda = new NodeAnim());
//						aiNodeAnim* nda = anims_temp.back();

//						char* ot = nda->mNodeName.data;
//						while (!IsSpaceOrNewLine(*buffer))
//							*ot++ = *buffer++;
//
//						*ot = '\0';
//						nda->mNodeName.length = (size_t)(ot-nda->mNodeName.data);
						int start = buffer.getCurrent();
						while(ParsingUtil.isSpaceOrNewLine((byte)buffer.get()))
							buffer.inCre();
						nda.mNodeName = buffer.getString(start, buffer.getCurrent());
					}

//					anim->mNumChannels = anims_temp.size();
					if (/*!anim->mNumChannels*/anims_temp.size() == 0)
						throw new DeadlyImportError("CSM: Empty $order section");

					// copy over to the output animation
//					anim->mChannels = new aiNodeAnim*[anim->mNumChannels];
//					::memcpy(anim->mChannels,&anims_temp[0],sizeof(aiNodeAnim*)*anim->mNumChannels);
					anim.mChannels = AssUtil.toArray(anims_temp, NodeAnim.class);
				}
				else if (buffer.tokenMatchI("points")/*TokenMatchI(buffer,"points",6)*/)	{
					if (anim.mChannels == null)
						throw new DeadlyImportError("CSM: \'$order\' section is required to appear prior to \'$points\'");

					// If we know how many frames we'll read, we can preallocate some storage
					int alloc = 100;
					if (last != 0x00ffffff)
					{
						alloc = last-first;
						alloc += alloc>>2; // + 25%
						for (int i = 0; i < anim.mChannels.length;++i)
//							anim->mChannels[i]->mPositionKeys = new aiVectorKey[alloc];
							anim.mChannels[i].mPositionKeys = new VectorKey[alloc];
					}

					int filled = 0;

					// Now read all point data.
					while (true)	{
//						SkipSpaces(&buffer);
						buffer.skipSpaces();
						if (ParsingUtil.isLineEnd((byte)buffer.get()) && (buffer.skipSpacesAndLineEnd(/*&buffer*/) || buffer.get() == '$'))	{
							break; // next section
						}

						// read frame
						int frame = buffer.strtoul10();
						last  = Math.max(frame,last);
						first = Math.min(frame,last);
						for (int i = 0; i < anim.mChannels.length;++i)	{

							NodeAnim s = anim.mChannels[i];
							if (s.getNumPositionKeys() == alloc)	{ /* need to reallocate? */

//								aiVectorKey* old = s->mPositionKeys;
//								s->mPositionKeys = new aiVectorKey[s->mNumPositionKeys = alloc*2];
//								::memcpy(s->mPositionKeys,old,sizeof(aiVectorKey)*alloc);
//								delete[] old;
								s.mPositionKeys = Arrays.copyOf(s.mPositionKeys, s.mPositionKeys.length * 2);
							}

							// read x,y,z
							if(!buffer.skipSpacesAndLineEnd())
								throw new DeadlyImportError("CSM: Unexpected EOF occured reading sample x coord");

							if (buffer.tokenMatchI("DROPOUT")/*TokenMatchI(buffer, "DROPOUT", 7)*/)	{
								// seems this is invalid marker data; at least the doc says it's possible
								DefaultLogger.warn("CSM: Encountered invalid marker data (DROPOUT)");
							}
							else	{
								int numPositionKeys = AssUtil.findfirstNull(s.mPositionKeys);
//								aiVectorKey* sub = s->mPositionKeys + s->mNumPositionKeys;
								VectorKey sub = s.mPositionKeys[numPositionKeys];
								sub.mTime = frame;
//								buffer = fast_atoreal_move<float>(buffer, (float&)sub->mValue.x);
								sub.mValue.x = (float) buffer.fast_atoreal_move(true);

								if(!buffer.skipSpacesAndLineEnd()/*SkipSpacesAndLineEnd(&buffer)*/)
									throw new DeadlyImportError("CSM: Unexpected EOF occured reading sample y coord");
//								buffer = fast_atoreal_move<float>(buffer, (float&)sub->mValue.y);
								sub.mValue.y = (float) buffer.fast_atoreal_move(true);

								if(!buffer.skipSpacesAndLineEnd())
									throw new DeadlyImportError("CSM: Unexpected EOF occured reading sample z coord");
//								buffer = fast_atoreal_move<float>(buffer, (float&)sub->mValue.z);
								sub.mValue.z = (float) buffer.fast_atoreal_move(true);

//								++s->mNumPositionKeys;
							}
						}

						// update allocation granularity
						if (filled == alloc)
							alloc *= 2;

						++filled;
					}
					// all channels must be complete in order to continue safely.
					for (int i = 0; i < anim.getNumChannels();++i)	{

						if (anim.mChannels[i].getNumPositionKeys() == 0)
							throw new DeadlyImportError("CSM: Invalid marker track");
					}
				}
			}
			else	{
				// advance to the next line
				buffer.skipLine();
			}
		}

		// Setup a proper animation duration
		anim.mDuration = last - Math.min( first, 0 );

		// build a dummy root node with the tiny markers as children
		pScene.mRootNode = new Node();
		pScene.mRootNode.mName = ("$CSM_DummyRoot");

//		pScene.mRootNode.mNumChildren = anim->mNumChannels;
		pScene.mRootNode.mChildren = new Node[anim.getNumChannels()];

		for (int i = 0; i < anim.mChannels.length;++i)	{
			NodeAnim na = anim.mChannels[i]; 

			Node nd    = pScene.mRootNode.mChildren[i] = new Node();
			nd.mName   = anim.mChannels[i].mNodeName;
			nd.mParent = pScene.mRootNode;

//			aiMatrix4x4::Translation(na->mPositionKeys[0].mValue, nd->mTransformation);
			nd.mTransformation.translate(na.mPositionKeys[0].mValue);
		}

		// Store the one and only animation in the scene
		pScene.mAnimations    = new Animation[/*pScene.mNumAnimations=*/1];
		pScene.mAnimations[0] = anim;
		anim.mName = ("$CSM_MasterAnim");

		// mark the scene as incomplete and run SkeletonMeshBuilder on it
		pScene.mFlags |= Scene.AI_SCENE_FLAGS_INCOMPLETE;
		
		if (!noSkeletonMesh) {
			new SkeletonMeshBuilder(pScene,pScene.mRootNode,true);
		}
	}

}
