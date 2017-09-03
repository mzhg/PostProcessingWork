package assimp.common;

import java.util.ArrayList;
import java.util.List;

public class BoneWithHash {
	int first;
	String second;
	List<BoneSrcIndex> pSrcBones = new ArrayList<>();
	int mNumWeights;

	static class BoneSrcIndex{
		Bone first;
		int second;
		
		public BoneSrcIndex(Bone first, int second) {
			this.first = first;
			this.second = second;
		}
		
		
	}
}
