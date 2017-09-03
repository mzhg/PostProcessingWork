package assimp.importer.blender;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.Mesh;
import assimp.common.Node;

/** Mirror modifier. Status: implemented. */
final class BlenderModifier_Mirror extends BlenderModifier{

	@Override
	boolean isActive(ModifierData md) {
		return md.type == ModifierData.eModifierType_Mirror;
	}
	
	@Override
	void doIt(Node out, ConversionData conv_data, ElemBase orig_modifier, BLEScene in, BLEObject orig_object) {
		// hijacking the ABI, see the big note in BlenderModifierShowcase::ApplyModifiers()
		MirrorModifierData mir = (MirrorModifierData) orig_modifier;
//		ai_assert(mir.modifier.type == ModifierData::eModifierType_Mirror);
		assert mir.modifier.type == ModifierData.eModifierType_Mirror;

//		conv_data.meshes->reserve(conv_data.meshes->size() + out.mNumMeshes);
		conv_data.meshes.ensureCapacity(conv_data.meshes.size() + out.getNumMeshes());

		// XXX not entirely correct, mirroring on two axes results in 4 distinct objects in blender ...

		// take all input meshes and clone them
		for (int i = 0; i < out.getNumMeshes(); ++i) {
			Mesh mesh = conv_data.meshes.get(out.mMeshes[i]).copy();
//			SceneCombiner::Copy(&mesh,conv_data.meshes[out.mMeshes[i]]);

			final float xs = (mir.flag & MirrorModifierData.Flags_AXIS_X) != 0 ? -1.f : 1.f;
			final float ys = (mir.flag & MirrorModifierData.Flags_AXIS_Y) != 0? -1.f : 1.f;
			final float zs = (mir.flag & MirrorModifierData.Flags_AXIS_Z) != 0? -1.f : 1.f;

			if (mir.mirror_ob != null) {
//				const aiVector3D center( mir.mirror_ob->obmat[3][0],mir.mirror_ob->obmat[3][1],mir.mirror_ob->obmat[3][2] );
				Vector3f center = new Vector3f(mir.mirror_ob.obmat[3][0],mir.mirror_ob.obmat[3][1],mir.mirror_ob.obmat[3][2]);
				for (int j = 0; j < mesh.mNumVertices; ++j) {
//					aiVector3D& v = mesh->mVertices[i];
//			
//					v.x = center.x + xs*(center.x - v.x);
//					v.y = center.y + ys*(center.y - v.y);
//					v.z = center.z + zs*(center.z - v.z);
					int index = 3 * j;
					mesh.mVertices.put(index, center.x + xs * (center.x - mesh.mVertices.get(index ++)));
					mesh.mVertices.put(index, center.y + ys * (center.y - mesh.mVertices.get(index ++)));
					mesh.mVertices.put(index, center.z + zs * (center.z - mesh.mVertices.get(index ++)));
				}
			}
			else {
				for (int j = 0; j < mesh.mNumVertices; ++j) {
//					aiVector3D& v = mesh->mVertices[i];
//					v.x *= xs;v.y *= ys;v.z *= zs;
					
					int index = 3 * j;
					mesh.mVertices.put(index, xs * mesh.mVertices.get(index ++));
					mesh.mVertices.put(index, ys * mesh.mVertices.get(index ++));
					mesh.mVertices.put(index, zs * mesh.mVertices.get(index ++));
				}
			}

			if (mesh.mNormals != null) {
				for (int j = 0; j < mesh.mNumVertices; ++j) {
//					aiVector3D& v = mesh->mNormals[i];
//					v.x *= xs;v.y *= ys;v.z *= zs;
					int index = 3 * j;
					mesh.mNormals.put(index, xs * mesh.mNormals.get(index ++));
					mesh.mNormals.put(index, ys * mesh.mNormals.get(index ++));
					mesh.mNormals.put(index, zs * mesh.mNormals.get(index ++));
				}
			}

			if (mesh.mTangents != null) {
				for (int j = 0; j < mesh.mNumVertices; ++j) {
//					aiVector3D& v = mesh->mTangents[i];
//					v.x *= xs;v.y *= ys;v.z *= zs;
					int index = 3 * j;
					mesh.mTangents.put(index, xs * mesh.mTangents.get(index ++));
					mesh.mTangents.put(index, ys * mesh.mTangents.get(index ++));
					mesh.mTangents.put(index, zs * mesh.mTangents.get(index ++));
				}
			}

			if (mesh.mBitangents != null) {
				for (int j = 0; j < mesh.mNumVertices; ++j) {
//					aiVector3D& v = mesh->mBitangents[i];
//					v.x *= xs;v.y *= ys;v.z *= zs;
					int index = 3 * j;
					mesh.mBitangents.put(index, xs * mesh.mBitangents.get(index ++));
					mesh.mBitangents.put(index, ys * mesh.mBitangents.get(index ++));
					mesh.mBitangents.put(index, zs * mesh.mBitangents.get(index ++));
				}
			}

			final float us = (mir.flag & MirrorModifierData.Flags_MIRROR_U) != 0 ? -1.f : 1.f;
			final float vs = (mir.flag & MirrorModifierData.Flags_MIRROR_V) != 0 ? -1.f : 1.f;

			for (int n = 0; mesh.hasTextureCoords(n); ++n) {
				for (int j = 0; j < mesh.mNumVertices; ++j) {
//					aiVector3D& v = mesh->mTextureCoords[n][i];
//					v.x *= us;v.y *= vs;
					int index = 3 * j;
					mesh.mBitangents.put(index, us * mesh.mBitangents.get(index ++));
					mesh.mBitangents.put(index, vs * mesh.mBitangents.get(index ++));
				}
			}

			// Only reverse the winding order if an odd number of axes were mirrored.
			if (xs * ys * zs < 0) {
				for(int j = 0; j < mesh.getNumFaces(); j++) {
					Face face = mesh.mFaces[i];
					for(int fi = 0; fi < face.getNumIndices() / 2; ++fi){
//						std::swap( face.mIndices[fi], face.mIndices[face.mNumIndices - 1 - fi]);
						int temp = face.get(fi);
						int temp2 = face.get(face.getNumIndices() - 1 - fi);
						face.set(fi, temp2);
						face.set(face.getNumIndices() - 1 - fi, temp);
					}
				}
			}

			conv_data.meshes.add(mesh);
		}
		int[] nind = new int[out.getNumMeshes()*2];

//		std::copy(out.mMeshes,out.mMeshes+out.mNumMeshes,nind);
//		std::transform(out.mMeshes,out.mMeshes+out.mNumMeshes,nind+out.mNumMeshes,
//			std::bind1st(std::plus< unsigned int >(),out.mNumMeshes));
		System.arraycopy(out.mMeshes, 0, nind, 0, out.mMeshes.length);
		for(int i = out.mMeshes.length; i < nind.length; i++){
			nind[i] = out.mMeshes[i - out.mMeshes.length] + out.mMeshes.length;
		}

//		delete[] out.mMeshes;
		out.mMeshes = nind;
//		out.mNumMeshes *= 2;
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.info("BlendModifier: Applied the `Mirror` modifier to `" + orig_object.id.name + "`");
	}
}
