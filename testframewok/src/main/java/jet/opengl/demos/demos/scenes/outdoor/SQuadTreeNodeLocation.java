// Copyright 2013 Intel Corporation
// All Rights Reserved
//
// Permission is granted to use, copy, distribute and prepare derivative works of this
// software for any purpose and without fee, provided, that the above copyright notice
// and this statement appear in all copies.  Intel makes no representations about the
// suitability of this software for any purpose.  THIS SOFTWARE IS PROVIDED "AS IS."
// INTEL SPECIFICALLY DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, AND ALL LIABILITY,
// INCLUDING CONSEQUENTIAL AND OTHER INDIRECT DAMAGES, FOR THE USE OF THIS SOFTWARE,
// INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PROPRIETARY RIGHTS, AND INCLUDING THE
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  Intel does not
// assume any responsibility for any errors which may appear in this software nor any
// responsibility to update it.
package jet.opengl.demos.demos.scenes.outdoor;

/** // Structure describing quad tree node location */
final class SQuadTreeNodeLocation {

	// Position in a tree
	int horzOrder;
	int vertOrder;
	int level;
	
	SQuadTreeNodeLocation(int h, int v, int l){
		horzOrder = h;
		vertOrder = v;
		level     = l;
	}
	
	void set(int h, int v, int l){
		horzOrder = h;
		vertOrder = v;
		level     = l;
	}
	
	SQuadTreeNodeLocation() {}
	
	// Gets location of a child
	static SQuadTreeNodeLocation getChildLocation(SQuadTreeNodeLocation parent,int siblingOrder, SQuadTreeNodeLocation out)
	{
		out.set(parent.horzOrder * 2 + (siblingOrder&1),
			parent.vertOrder * 2 + (siblingOrder>>1),
			parent.level + 1);
		
		return out;
	}

    // Gets location of a parent
	static SQuadTreeNodeLocation getParentLocation(SQuadTreeNodeLocation node, SQuadTreeNodeLocation out)
	{
		assert(node.level > 0);
		out.set(node.horzOrder / 2, node.vertOrder / 2, node.level - 1);
		
		return out;
	}
}
