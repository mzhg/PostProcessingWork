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
package com.nvidia.developer.opengl.demos.scenes.outdoor;

import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;

class CElevationDataSource {
	
	private final SQuadTreeNodeLocation QuadTreeNodeLocation = new SQuadTreeNodeLocation();

	// Hierarchy array storing minimal and maximal heights for quad tree nodes
//    HierarchyArray< std::pair<UINT16, UINT16> > m_MinMaxElevation;
	final HierarchyArrayInt m_MinMaxElevation = new HierarchyArrayInt();
    
    int m_iNumLevels;
    int m_iPatchSize = 128;
    int m_iColOffset, m_iRowOffset;
    
    // The whole terrain height map
//    final StackShort m_TheHeightMap = new StackShort();
    short[] m_TheHeightMap;
    int m_iNumCols, m_iNumRows;
    
    public void print(){
    	System.out.println("m_iNumLevels = " + m_iNumLevels);
    	System.out.println("m_iPatchSize = " + m_iPatchSize);
    	System.out.println("m_iNumCols = " + m_iNumCols);
    	System.out.println("m_iNumRows = " + m_iNumRows);
    	System.out.println("GlobalMinElevation = " + getGlobalMinElevation());
    	System.out.println("GlobalMaxElevation = " + getGlobalMaxElevation());
    }
    
    public static void main(String[] args) {
    	int width = 2048;
    	int height = 2048;
    	
    	// Calculate minimal number of columns and rows
        // in the form 2^n+1 that encompass the data
        int m_iNumCols = 1;
        int m_iNumRows = 1;
        while( m_iNumCols+1 < width || m_iNumRows+1 < height)
        {
            m_iNumCols *= 2;
            m_iNumRows *= 2;
        }

        int m_iNumLevels = 1;
        int m_iPatchSize = 128;
        while( (m_iPatchSize << (m_iNumLevels-1)) < (int)m_iNumCols ||
               (m_iPatchSize << (m_iNumLevels-1)) < (int)m_iNumRows )
            m_iNumLevels++;

        m_iNumCols++;
        m_iNumRows++;
        
        System.out.println("m_iNumCols = " + m_iNumCols);
        System.out.println("m_iNumRows = " + m_iNumRows);
        System.out.println("m_iNumLevels = " + m_iNumLevels);
	}
    
 // Creates data source from the specified raw data file
    CElevationDataSource(String strSrcDemFile){
//    	int width = 2048;
//    	int height = 2048;
    	
    	m_iNumCols = 2049;
    	m_iNumRows = 2049;
    	m_iNumLevels = 5;


    	m_TheHeightMap = new short[m_iNumCols * m_iNumRows];
    	try(InputStream fin = FileUtils.open(strSrcDemFile);
            BufferedInputStream in = new BufferedInputStream(fin)
    			){
    		int index = 0;
    		while(index < m_TheHeightMap.length){
    			int low = in.read();
    			int high = in.read();
    			
    			m_TheHeightMap[index++] = (short) Numeric.makeRGBA(low, high, 0, 0);
    		}
    		
    		if(in.available() > 0 || index != m_TheHeightMap.length)
    			throw new IllegalArgumentException();
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
//    	short[] newHeightMap = new short[2048 * 2048];
//    	for(int i = 0; i < 2048; i++){
//    		System.arraycopy(m_TheHeightMap, i * m_iNumCols, newHeightMap, i * 2048, 2048);
//    	}
//    	
//    	m_TheHeightMap = newHeightMap;
//    	m_iNumCols = 2048;
//    	m_iNumRows = 2048;
    	m_MinMaxElevation.resize(m_iNumLevels);
    	// Calcualte min/max elevations
        calculateMinMaxElevations();
        
        System.gc();
    }

	short[] getDataPtr(){ return m_TheHeightMap;}
	int getPitch() { return m_iNumCols;}
    
    // Returns minimal height of the whole terrain
    int getGlobalMinElevation(){
    	return Numeric.decodeFirst(m_MinMaxElevation.get(QuadTreeNodeLocation));
    }

    // Returns maximal height of the whole terrain
    int getGlobalMaxElevation(){
    	return Numeric.decodeSecond(m_MinMaxElevation.get(QuadTreeNodeLocation));
    }
    
    private static int mirrorCoord(int iCoord, int iDim)
    {
        iCoord = Math.abs(iCoord);
        int iPeriod = iCoord / iDim;
        iCoord = iCoord % iDim;
        if( (iPeriod & 0x01) !=0 )
        {
            iCoord = (iDim-1) - iCoord;
        }
        return iCoord;
    }


    void recomputePatchMinMaxElevations(SQuadTreeNodeLocation pos){
    	if( pos.level == m_iNumLevels-1 )
    	{
//    		std::pair<UINT16, UINT16> &CurrPatchMinMaxElev = m_MinMaxElevation[SQuadTreeNodeLocation(pos.horzOrder, pos.vertOrder, pos.level)];
    		int CurrPatchMinMaxElev =m_MinMaxElevation.get(pos);
    		int CurrPatchMinMaxElevFirst = Numeric.decodeFirst(CurrPatchMinMaxElev);
    		int CurrPatchMinMaxElevSecond = Numeric.decodeSecond(CurrPatchMinMaxElev);
            int iStartCol = pos.horzOrder*m_iPatchSize;
            int iStartRow = pos.vertOrder*m_iPatchSize;
            CurrPatchMinMaxElevFirst = CurrPatchMinMaxElevSecond = m_TheHeightMap[iStartCol + iStartRow*m_iNumCols] & 0XFFFF;
            for(int iRow = iStartRow; iRow <= iStartRow + m_iPatchSize; iRow++)
                for(int iCol = iStartCol; iCol <= iStartCol + m_iPatchSize; iCol++)
                {
                    int CurrElev = m_TheHeightMap[iCol + iRow*m_iNumCols] & 0XFFFF;
                    CurrPatchMinMaxElevFirst = Math.min(CurrPatchMinMaxElevFirst, CurrElev);
                    CurrPatchMinMaxElevSecond = Math.max(CurrPatchMinMaxElevSecond, CurrElev);
                }
            
            m_MinMaxElevation.set(pos, Numeric.encode((short)CurrPatchMinMaxElevFirst, (short)CurrPatchMinMaxElevSecond));
    	}
    	else
    	{
//            std::pair<UINT16, UINT16> &CurrPatchMinMaxElev = m_MinMaxElevation[pos];
//            std::pair<UINT16, UINT16> &LBChildMinMaxElev = m_MinMaxElevation[GetChildLocation(pos, 0)];
//            std::pair<UINT16, UINT16> &RBChildMinMaxElev = m_MinMaxElevation[GetChildLocation(pos, 1)];
//            std::pair<UINT16, UINT16> &LTChildMinMaxElev = m_MinMaxElevation[GetChildLocation(pos, 2)];
//            std::pair<UINT16, UINT16> &RTChildMinMaxElev = m_MinMaxElevation[GetChildLocation(pos, 3)];
//
    		SQuadTreeNodeLocation tmpVar = new SQuadTreeNodeLocation();
    		int CurrPatchMinMaxElev = m_MinMaxElevation.get(pos);
    		int LBChildMinMaxElev =   m_MinMaxElevation.get(SQuadTreeNodeLocation.getChildLocation(pos, 0, tmpVar));
    		int RBChildMinMaxElev =   m_MinMaxElevation.get(SQuadTreeNodeLocation.getChildLocation(pos, 1, tmpVar));
    		int LTChildMinMaxElev =   m_MinMaxElevation.get(SQuadTreeNodeLocation.getChildLocation(pos, 2, tmpVar));
    		int RTChildMinMaxElev =   m_MinMaxElevation.get(SQuadTreeNodeLocation.getChildLocation(pos, 3, tmpVar));
    		
    		int CurrPatchMinMaxElevFirst = Numeric.decodeFirst(CurrPatchMinMaxElev);
    		int CurrPatchMinMaxElevSecond = Numeric.decodeSecond(CurrPatchMinMaxElev);
    		int LBChildMinMaxElevFirst   = Numeric.decodeFirst(LBChildMinMaxElev);
    		int LBChildMinMaxElevSecond  = Numeric.decodeSecond(LBChildMinMaxElev);
    		int RBChildMinMaxElevFirst   = Numeric.decodeFirst(RBChildMinMaxElev);
    		int RBChildMinMaxElevSecond  = Numeric.decodeSecond(RBChildMinMaxElev);
    		int LTChildMinMaxElevFirst   = Numeric.decodeFirst(LTChildMinMaxElev);
    		int LTChildMinMaxElevSecond  = Numeric.decodeSecond(LTChildMinMaxElev);
    		int RTChildMinMaxElevFirst   = Numeric.decodeFirst(RTChildMinMaxElev);
    		int RTChildMinMaxElevSecond  = Numeric.decodeSecond(RTChildMinMaxElev);
    		
          CurrPatchMinMaxElevFirst = Math.min( LBChildMinMaxElevFirst, RBChildMinMaxElevFirst );
          CurrPatchMinMaxElevFirst = Math.min( CurrPatchMinMaxElevFirst, LTChildMinMaxElevFirst );
          CurrPatchMinMaxElevFirst = Math.min( CurrPatchMinMaxElevFirst, RTChildMinMaxElevFirst );

          CurrPatchMinMaxElevSecond = Math.max( LBChildMinMaxElevSecond, RBChildMinMaxElevSecond);
          CurrPatchMinMaxElevSecond = Math.max( CurrPatchMinMaxElevSecond, LTChildMinMaxElevSecond );
          CurrPatchMinMaxElevSecond = Math.max( CurrPatchMinMaxElevSecond, RTChildMinMaxElevSecond );
    		
          m_MinMaxElevation.set(pos, Numeric.encode((short)CurrPatchMinMaxElevFirst, (short)CurrPatchMinMaxElevSecond));
          
//          m_MinMaxElevation.set(SQuadTreeNodeLocation.getChildLocation(pos, 0, tmpVar), Numeric.encode((short)LBChildMinMaxElevFirst, (short)LBChildMinMaxElevSecond));
//          m_MinMaxElevation.set(SQuadTreeNodeLocation.getChildLocation(pos, 1, tmpVar), Numeric.encode((short)RBChildMinMaxElevFirst, (short)RBChildMinMaxElevSecond));
//          m_MinMaxElevation.set(SQuadTreeNodeLocation.getChildLocation(pos, 2, tmpVar), Numeric.encode((short)LTChildMinMaxElevFirst, (short)LTChildMinMaxElevSecond));
//          m_MinMaxElevation.set(SQuadTreeNodeLocation.getChildLocation(pos, 3, tmpVar), Numeric.encode((short)RTChildMinMaxElevFirst, (short)RTChildMinMaxElevSecond));
    	}
    }
    
    void setOffsets(int iColOffset, int iRowOffset){m_iColOffset = iColOffset; m_iRowOffset = iRowOffset;}
//    void getOffsets(int &iColOffset, int &iRowOffset)const{iColOffset = m_iColOffset; iRowOffset = m_iRowOffset;}
    int getColOffset() { return m_iColOffset;}
    int getRowOffset() { return m_iRowOffset;}

    float getInterpolatedHeight(float fCol, float fRow){
    	return getInterpolatedHeight(fCol, fRow, 1);
    }
    float getInterpolatedHeight(float fCol, float fRow, int iStep /*= 1*/){
    	float fCol0 = (float) Math.floor(fCol);
        float fRow0 = (float) Math.floor(fRow);
        int iCol0 = (int)(fCol0);
        int iRow0 = (int)(fRow0);
        iCol0 = (iCol0/iStep)*iStep;
        iRow0 = (iRow0/iStep)*iStep;
        float fHWeight = (fCol - (float)iCol0) / (float)iStep;
        float fVWeight = (fRow - (float)iRow0) / (float)iStep;
        iCol0 += m_iColOffset;
        iRow0 += m_iRowOffset;
        //if( iCol0 < 0 || iCol0 >= (int)m_iNumCols || iRow0 < 0 || iRow0 >= (int)m_iNumRows )
        //    return -FLT_MAX;

        int iCol1 = iCol0+iStep;//min(iCol0+iStep, (int)m_iNumCols-1);
        int iRow1 = iRow0+iStep;//min(iRow0+iStep, (int)m_iNumRows-1);

        iCol0 = mirrorCoord(iCol0, m_iNumCols);
        iCol1 = mirrorCoord(iCol1, m_iNumCols);
        iRow0 = mirrorCoord(iRow0, m_iNumRows);
        iRow1 = mirrorCoord(iRow1, m_iNumRows);

        int H00 = m_TheHeightMap[iCol0 + iRow0 * m_iNumCols] & 0XFFFF;
        int H10 = m_TheHeightMap[iCol1 + iRow0 * m_iNumCols] & 0XFFFF;
        int H01 = m_TheHeightMap[iCol0 + iRow1 * m_iNumCols] & 0XFFFF;
        int H11 = m_TheHeightMap[iCol1 + iRow1 * m_iNumCols] & 0XFFFF;
        float fInterpolatedHeight = (H00 * (1 - fHWeight) + H10 * fHWeight) * (1-fVWeight) + 
                                    (H01 * (1 - fHWeight) + H11 * fHWeight) * fVWeight;
        return fInterpolatedHeight;
    }
    
    void computeSurfaceNormal(float fCol, float fRow,
                                     float fSampleSpacing,
                                     float fHeightScale, 
                                     int iStep /*= 1*/, Vector3f out){
    	float Height1 = getInterpolatedHeight(fCol + (float)iStep, fRow, iStep);
        float Height2 = getInterpolatedHeight(fCol - (float)iStep, fRow, iStep);
        float Height3 = getInterpolatedHeight(fCol, fRow + (float)iStep, iStep);
        float Height4 = getInterpolatedHeight(fCol, fRow - (float)iStep, iStep);
           
        float GradX = Height2 - Height1;
        float GradY = Height4 - Height3;
        float GradZ = (float)iStep * fSampleSpacing * 2.f;

        GradX *= fHeightScale;
        GradY *= fHeightScale;
//        D3DXVECTOR3 Normal;
//        D3DXVec3Normalize(&Normal, &Grad);
        out.set(GradX, GradY, GradZ);
        out.normalise();
        
//        return Normal;
    }

    int getNumCols(){return m_iNumCols;}
    int getNumRows(){return m_iNumRows;}
    
 // Calculates min/max elevations for all patches in the tree
    private void calculateMinMaxElevations(){
    	// Calculate min/max elevations starting from the finest level
        for( HierarchyReverseIterator it = new HierarchyReverseIterator(m_iNumLevels); it.isValid(); it.next() )
        {
    		recomputePatchMinMaxElevations(it.get());
        }
    }
}
