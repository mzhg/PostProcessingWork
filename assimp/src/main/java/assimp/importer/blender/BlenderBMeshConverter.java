/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2013, assimp team
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
package assimp.importer.blender;

import java.util.List;

import assimp.common.DeadlyExportError;

/** Conversion of Blender's new BMesh stuff */
final class BlenderBMeshConverter {
	BLEMesh bmesh;
	BLEMesh triMesh;
	
	BlenderBMeshConverter(BLEMesh mesh ){
		bmesh = mesh;
		triMesh = null;
		
		assertValidMesh();
	}

	boolean containsBMesh( ){
		// TODO - Should probably do some additional verification here
		return bmesh.totpoly != 0 && bmesh.totloop!= 0 && bmesh.totvert != 0;
	}

	BLEMesh triangulateBMesh( ){
		assertValidMesh( );
		assertValidSizes( );
		prepareTriMesh( );

		for ( int i = 0; i < bmesh.totpoly; ++i )
		{
			MPoly poly = bmesh.mpoly.get(i);
			convertPolyToFaces( poly );
		}

		return triMesh;
	}
	
	void assertValidMesh( ){
		if ( !containsBMesh( ) )
		{
			throw new DeadlyExportError( "BlenderBMeshConverter requires a BMesh with \"polygons\" - please call BlenderBMeshConverter::ContainsBMesh to check this first" );
		}
	}
	void assertValidSizes( ){
		if ( bmesh.totpoly != /*static_cast<int>*/( bmesh.mpoly.size( ) ) )
		{
			throw new DeadlyExportError( "BMesh poly array has incorrect size" );
		}
		if ( bmesh.totloop != /*static_cast<int>*/( bmesh.mloop.size( ) ) )
		{
			throw new DeadlyExportError( "BMesh loop array has incorrect size" );
		}
	}
	void prepareTriMesh( ){
		if ( triMesh != null)
		{
			triMesh = null;
		}

		triMesh = new BLEMesh( bmesh );
		triMesh.totface = 0;
		triMesh.mface.clear( );
	}
	
	void convertPolyToFaces(MPoly poly ){
//		MLoop polyLoop = bmesh.mloop.get(poly.loopstart );
		if ( poly.totloop == 3 || poly.totloop == 4 )
		{
			List<MLoop> loops = bmesh.mloop;
			addFace( loops.get(poly.loopstart).v, loops.get(poly.loopstart + 1).v, loops.get(poly.loopstart + 2).v, poly.totloop == 4 ? loops.get(poly.loopstart + 3).v : 0 );
		}
		else if ( poly.totloop > 4 )
		{
//		    if(AssimpConfig.ASSIMP_BLEND_WITH_GLU_TESSELLATE){
//				BlenderTessellatorGL tessGL(this );
//				tessGL.Tessellate( polyLoop, poly.totloop, triMesh->mvert );
//		    }
//		    else if(AssimpConfig.ASSIMP_BLEND_WITH_POLY_2_TRI){
//		    }
			
			MLoop[] polyLoop = new MLoop[poly.totloop];
			for(int i = 0; i < polyLoop.length; i++){
				polyLoop[i] = bmesh.mloop.get(i + poly.loopstart);
			}
			BlenderTessellatorP2T tessP2T = new BlenderTessellatorP2T(this );
			tessP2T.tessellate( polyLoop, poly.totloop, triMesh.mvert );
		}
	}
	void addFace( int v1, int v2, int v3, int v4 /*= 0*/ ){
		MFace face = new MFace();
		face.v1 = v1;
		face.v2 = v2;
		face.v3 = v3;
		face.v4 = v4;
		// TODO - Work out how materials work
		face.mat_nr = 0;
		triMesh.mface.add( face );
		triMesh.totface = triMesh.mface.size( );
	}
}
