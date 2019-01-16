package jet.opengl.demos.nvidia.waves.packets;

import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.ImageData;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.min;

final class Packets implements GlobalDefs{
    // scene
    int			m_groundSizeX, m_groundSizeY;	// pixel size of the ground texture
    float[]		m_ground;						// texture containing the water depth and land (0.95)
    float[]		m_distMap;						// distance map of the boundary map
    Vector2f[]	m_gndDeriv;
    Vector2f[]	m_bndDeriv;

    // packet managing
    WAVE_PACKET[]	m_packet;						// wave packet data
    GHOST_PACKET[] m_ghostPacket;					// ghost packet data
    int			m_packetBudget;					// this can be changed any time (soft budget)
    int			m_packetNum;					// current size of the buffer used for packets / ghosts
    float		m_softDampFactor;
    int[]	    m_usedPacket;
    int			m_usedPackets;
    int[]	    m_freePacket;
    int			m_freePackets;
    int[]		m_usedGhost;
    int			m_usedGhosts;
    int[]		m_freeGhost;
    int			m_freeGhosts;

    // simulation
    float		m_time;
    float		m_oldTime;
    float		m_elapsedTime;

    Packets(int packetBudget){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        ImageData bmpData = null;
        try {
            bmpData = gl.getNativeAPI().load("nvidia/Packets/textures/" + WATER_TERRAIN_FILE, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bmpData.internalFormat != GLenum.GL_RGB8){
            throw new IllegalArgumentException();
        }

        m_groundSizeX = bmpData.width;
        m_groundSizeY = bmpData.height;

        m_ground = new float[m_groundSizeX*m_groundSizeY];
        float[] bound = new float[m_groundSizeX*m_groundSizeY];

        int bufpos = 0;
        for (int y=0; y<m_groundSizeY; y++)
            for (int x=0; x<m_groundSizeX; x++)
            {
//                int bufpos = (m_groundSizeY-y-1)*psw + x*3;
                m_ground[y*m_groundSizeX + x] = (float)(/*Buffer[bufpos + 0]*/Numeric.unsignedByte(bmpData.pixels.get(bufpos+0))) / 255.0f;  // read the blue channel (which is the smoothed depth for directional Tessendorf shader)
                float v = (float)(/*Buffer[bufpos + 1]*/ Numeric.unsignedByte(bmpData.pixels.get(bufpos+1)))/255.0f;						 // read the green channel (contains terrain heightfield)
                if (v > 11.1f/255.0f)
                    bound[y*m_groundSizeX+x] = 1.0f;
                else
                    bound[y*m_groundSizeX+x] = 0.0f;

                bufpos += 3;
            }

        // boundary texture distance transform
        // init helper distance map (pMap)
        LogUtil.i(LogUtil.LogType.DEFAULT, "Computing boundary distance transform..");
        int[] pMap = new int[m_groundSizeX*m_groundSizeY];
//	#pragma omp parallel for
        for (int y = 0; y < m_groundSizeY; y++)
            for (int x = 0; x < m_groundSizeX; x++)
            {
                // if we are at the boundary, intialize the distance function with 0, otherwise with maximum value
                if ((bound[y*m_groundSizeX + x] > 0.5f) &&
                        ((bound[Math.max(0, Math.min(m_groundSizeY - 1, y + 1))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x + 0))] <= 0.5f)
                                || (bound[Math.max(0, Math.min(m_groundSizeY - 1, y + 0))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x + 1))] <= 0.5f)
                                || (bound[Math.max(0, Math.min(m_groundSizeY - 1, y - 1))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x + 0))] <= 0.5f)
                                || (bound[Math.max(0, Math.min(m_groundSizeY - 1, y + 0))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x - 1))] <= 0.5f)))
                    pMap[y*m_groundSizeX + x] = 0;  // initialize with maximum x distance
                else if ((bound[y*m_groundSizeX + x] <= 0.5f) &&
                        ((bound[Math.max(0, Math.min(m_groundSizeY - 1, y + 1))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x + 0))] > 0.5f)
                                || (bound[Math.max(0, Math.min(m_groundSizeY - 1, y + 0))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x + 1))] > 0.5f)
                                || (bound[Math.max(0, Math.min(m_groundSizeY - 1, y - 1))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x + 0))] > 0.5f)
                                || (bound[Math.max(0, Math.min(m_groundSizeY - 1, y + 0))*m_groundSizeX + Math.max(0, Math.min(m_groundSizeX - 1, x - 1))] > 0.5f)))
                    pMap[y*m_groundSizeX + x] = 0;  // initialize with maximum x distance
                else
                    pMap[y*m_groundSizeX + x] = m_groundSizeX*m_groundSizeX;  // initialize with maximum x distance
            }
        m_distMap = new float[m_groundSizeX*m_groundSizeY];
//	#pragma omp parallel for
        for (int y=0; y<m_groundSizeY; y++)
            for (int x=0; x<m_groundSizeX; x++)
                m_distMap[y*m_groundSizeX+x] = (float)(m_groundSizeX*m_groundSizeX + m_groundSizeY*m_groundSizeY);
//	#pragma omp parallel for
        for (int y=0; y<m_groundSizeY; y++)   // horizontal scan forward
        {
            int lastBoundX = -m_groundSizeX;
            for (int x=0; x<m_groundSizeX; x++)
            {
                if (pMap[y*m_groundSizeX+x] == 0)
                    lastBoundX = x;
                pMap[y*m_groundSizeX+x] = Math.min(pMap[y*m_groundSizeX+x], (x-lastBoundX)*(x-lastBoundX));
            }
        }
//	#pragma omp parallel for
        for (int y=0; y<m_groundSizeY; y++)  // horizontal scan backward
        {
            int lastBoundX = 2*m_groundSizeX;
            for (int x=m_groundSizeX-1; x>=0; x--)
            {
                if (pMap[y*m_groundSizeX+x] == 0)
                    lastBoundX = x;
                pMap[y*m_groundSizeX+x] = Math.min(pMap[y*m_groundSizeX+x], (lastBoundX-x)*(lastBoundX-x));
            }
        }
//	#pragma omp parallel for
        for (int x=0; x<m_groundSizeX; x++)  // vertical scan forward and backward
            for (int y=0; y<m_groundSizeY; y++)
            {
                int minDist = pMap[y*m_groundSizeX+x];
                for (int yd=1; yd+y<=m_groundSizeY-1; yd++)
                {
                    minDist = Math.min(minDist, yd*yd+pMap[(y+yd)*m_groundSizeX+x]);
                    if (minDist < yd*yd)
                        break;
                }
                for (int yd=-1; yd+y>=0; yd--)
                {
                    minDist = Math.min(minDist, yd*yd+pMap[(y+yd)*m_groundSizeX+x]);
                    if (minDist < yd*yd)
                        break;
                }
                m_distMap[y*m_groundSizeX+x] = (float)(minDist);
            }
//        delete[](pMap);
        // m_distMap now contains the _squared_ euklidean distance to closest label boundary, so take the sqroot. And sign the distance
//	#pragma omp parallel for
        for (int y=0; y<m_groundSizeY; y++)
            for (int x=0; x<m_groundSizeX; x++)
            {
                m_distMap[y*m_groundSizeX+x] = (float)Math.sqrt(m_distMap[y*m_groundSizeX+x]);
                if (bound[y*m_groundSizeX+x] > 0.5f)
                    m_distMap[y*m_groundSizeX+x] = -m_distMap[y*m_groundSizeX+x];		// negative distance INSIDE a boundary regions
                m_distMap[y*m_groundSizeX+x] = m_distMap[y*m_groundSizeX+x]*SCENE_EXTENT / m_groundSizeX;
            }
        /*StringCchPrintf(wcInfo, 512, L"done!\n");
        OutputDebugString(wcInfo);*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "done!");

        // derivative (2D normal) of the boundary texture
        LogUtil.i(LogUtil.LogType.DEFAULT, "Computing boundary derivatives..");
        m_bndDeriv = new Vector2f[m_groundSizeX*m_groundSizeY];
//	#pragma omp parallel for
        for (int y=0; y<m_groundSizeY; y++)
            for (int x=0; x<m_groundSizeX; x++)
            {
                float dx = m_distMap[y*m_groundSizeX + Math.max(0,Math.min(m_groundSizeX-1,x+1))] - m_distMap[y*m_groundSizeX + x];
                float dy = m_distMap[Math.max(0,Math.min(m_groundSizeY-1,y+1))*m_groundSizeX + x] - m_distMap[y*m_groundSizeX + x];
                Vector2f dV = new Vector2f(dx,dy);
                dx = m_distMap[y*m_groundSizeX + x] - m_distMap[y*m_groundSizeX + Math.max(0,Math.min(m_groundSizeX-1,x-1))];
                dy = m_distMap[y*m_groundSizeX + x] - m_distMap[Math.max(0,Math.min(m_groundSizeY-1,y-1))*m_groundSizeX + x];
//                dV += Vector2f(dx,dy);
                dV.x += dx;
                dV.y += dy;

//                m_bndDeriv[y*m_groundSizeX+x] = Vector2f(0,0);
                if ((dV.x != 0) || (dV.y != 0)) {
                    dV.normalise();
                    m_bndDeriv[y * m_groundSizeX + x] = dV;
                }else{
                    m_bndDeriv[y * m_groundSizeX + x] = dV;  // zero
                }
            }
        LogUtil.i(LogUtil.LogType.DEFAULT, "done!");

        //smooth the derivative to avoid staricase artifacts of the texture
        /*StringCchPrintf( wcInfo, 512, L"Smoothing boundary derivatives..");
        OutputDebugString( wcInfo );*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "Smoothing boundary derivatives..");
        Vector2f[] m_bndDerivH = new Vector2f[m_groundSizeX*m_groundSizeY];
        for (int i=0; i<2; i++)  //15
        {
//		#pragma omp parallel for
            for (int y=0; y<m_groundSizeY; y++)
                for (int x=0; x<m_groundSizeX; x++)
                {
                    Vector2f dV = new Vector2f(0.0f,0.0f);
                    for (int dy=-1; dy<=1; dy++)
                        for (int dx=-1; dx<=1; dx++)
                        {
                            float w = 1.0f/16.0f;
                            if ((Math.abs(dy) == 0) && (Math.abs(dx) == 0))
                                w = 4.0f/16.0f;
                            else if ((Math.abs(dy) == 0) || (Math.abs(dx) == 0))
                                w = 2.0f/16.0f;
//                            dV += w*m_bndDeriv[Math.max(0,Math.min(m_groundSizeY-1,y+dy))*m_groundSizeX + Math.max(0,Math.min(m_groundSizeX-1,x+dx))];
                            Vector2f.linear(dV, m_bndDeriv[Math.max(0,Math.min(m_groundSizeY-1,y+dy))*m_groundSizeX + Math.max(0,Math.min(m_groundSizeX-1,x+dx))], w, dV);
                        }
                    if ((dV.x != 0) || (dV.y != 0))
//                        m_bndDerivH[y*m_groundSizeX+x] = dV.normalized();
                        dV.normalise();

                    m_bndDerivH[y*m_groundSizeX+x] = dV;
                }
            // copy the result back to derivative map
//            memcpy(m_bndDeriv, m_bndDerivH, sizeof(Vector2f)*m_groundSizeX*m_groundSizeY);
            System.arraycopy(m_bndDerivH, 0, m_bndDeriv, 0, m_bndDeriv.length);
        }
        /*delete[](m_bndDerivH);
        StringCchPrintf( wcInfo, 512, L"done!\n");
        OutputDebugString( wcInfo );*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "done!");

        // derivative (2D normal) of the ground texture
        /*StringCchPrintf( wcInfo, 512, L"Computing ground derivatives..");
        OutputDebugString( wcInfo );*/
        LogUtil.i(LogUtil.LogType.DEFAULT, "Computing ground derivatives..");
        m_gndDeriv = new Vector2f[m_groundSizeX*m_groundSizeY];
//	#pragma omp parallel for
        for (int y=0; y<m_groundSizeY; y++)
            for (int x=0; x<m_groundSizeX; x++)
            {
                float dx = m_ground[y*m_groundSizeX + Math.max(0,Math.min(m_groundSizeX-1,x+1))] - m_ground[y*m_groundSizeX + x];
                float dy = m_ground[Math.max(0,Math.min(m_groundSizeY-1,y+1))*m_groundSizeX + x] - m_ground[y*m_groundSizeX + x];
                Vector2f dV = new Vector2f(dx,dy);
                dx = m_ground[y*m_groundSizeX + x] - m_ground[y*m_groundSizeX + Math.max(0,Math.min(m_groundSizeX-1,x-1))];
                dy = m_ground[y*m_groundSizeX + x] - m_ground[Math.max(0,Math.min(m_groundSizeY-1,y-1))*m_groundSizeX + x];
//                dV += Vector2f(dx,dy);
                dV.x += dx;
                dV.y += dy;
//                m_gndDeriv[y*m_groundSizeX+x] = Vector2f(0,0);
                if ((dV.x != 0) || (dV.y != 0))
                    dV.normalise();
                m_gndDeriv[y*m_groundSizeX+x] = dV;
            }
        /*StringCchPrintf( wcInfo, 512, L"done!\n");
        OutputDebugString( wcInfo );*/

        LogUtil.i(LogUtil.LogType.DEFAULT, "done!");
        // init variables
        m_packetBudget = packetBudget;
        m_usedPackets = 0;
        m_freePackets = 0;
        m_usedGhosts = 0;
        m_freeGhosts = 0;
        m_packetNum = 0;
        m_packet = null;
        ExpandWavePacketMemory(PACKET_BUFFER_DELTA);
        Reset();
        UpdateTime(0.0f);
    }

    void Reset(){
        for (int i = 0; i<m_packetNum; i++)
        {
            m_freePacket[i] = i;
            m_freeGhost[i] = i;
        }
        m_usedPackets = 0;			// points BEHIND the last used packet (= first free slot)
        m_freePackets = m_packetNum;// points BEHIND the last free packet
        m_usedGhosts = 0;			// points BEHIND the last used ghost
        m_freeGhosts = m_packetNum;	// points BEHIND the last free ghost packet
        m_time = 0.0f;
        m_oldTime = -1.0f;
    }

    float GetBoundaryDist(Vector2f p){
        Vector2f pTex = new Vector2f(p.x/SCENE_EXTENT+0.5f,p.y/SCENE_EXTENT+0.5f);		// convert from world space to texture space
        float val1 = m_distMap[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        float val2 = m_distMap[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        float val3 = m_distMap[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        float val4 = m_distMap[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        float xOffs = (pTex.x*m_groundSizeX) - (int)(pTex.x*m_groundSizeX);
        float yOffs = (pTex.y*m_groundSizeY) - (int)(pTex.y*m_groundSizeY);
        float valH1 = (1.0f-xOffs)*val1 + xOffs*val2;
        float valH2 = (1.0f-xOffs)*val3 + xOffs*val4;
        return( (1.0f-yOffs)*valH1 + yOffs*valH2 );
    }

    Vector2f GetBoundaryNormal(Vector2f p){
        Vector2f pTex = new Vector2f(p.x/SCENE_EXTENT+0.5f,p.y/SCENE_EXTENT+0.5f);		// convert from world space to texture space
        Vector2f val1 = m_bndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        Vector2f val2 = m_bndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        Vector2f val3 = m_bndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        Vector2f val4 = m_bndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        float xOffs = (pTex.x*m_groundSizeX) - (int)(pTex.x*m_groundSizeX);
        float yOffs = (pTex.y*m_groundSizeY) - (int)(pTex.y*m_groundSizeY);
        /*Vector2f valH1 = (1.0f-xOffs)*val1 + xOffs*val2;
        Vector2f valH2 = (1.0f-xOffs)*val3 + xOffs*val4;
        Vector2f res = (1.0f-yOffs)*valH1 + yOffs*valH2;*/
        Vector2f valH1 = Vector2f.linear(val1, (1.0f-xOffs), val2, xOffs, null);
        Vector2f valH2 = Vector2f.linear(val3, (1.0f-xOffs), val4, xOffs, null);
        Vector2f res = Vector2f.linear(valH1,(1.0f-yOffs), valH2, yOffs, val1);
        valH2.set(0, 1);
        Vector2f resN = valH2;
        if (Math.abs(res.x) + Math.abs(res.y) > 0.0)
//            resN = res.normalized();
            res.normalise(resN);
        return( resN );
    }

    float GetGroundVal(Vector2f p){
        Vector2f pTex = new Vector2f(p.x/SCENE_EXTENT+0.5f,p.y/SCENE_EXTENT+0.5f);		// convert from world space to texture space
        float val1 = m_ground[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        float val2 = m_ground[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        float val3 = m_ground[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        float val4 = m_ground[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        float xOffs = (pTex.x*m_groundSizeX) - (int)(pTex.x*m_groundSizeX);
        float yOffs = (pTex.y*m_groundSizeY) - (int)(pTex.y*m_groundSizeY);
        float valH1 = (1.0f-xOffs)*val1 + xOffs*val2;
        float valH2 = (1.0f-xOffs)*val3 + xOffs*val4;
        return( (1.0f-yOffs)*valH1 + yOffs*valH2 );
    }

    Vector2f GetGroundNormal(Vector2f p){
        Vector2f pTex = new Vector2f(p.x/SCENE_EXTENT+0.5f,p.y/SCENE_EXTENT+0.5f);		// convert from world space to texture space
        Vector2f val1 = m_gndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        Vector2f val2 = m_gndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        Vector2f val3 = m_gndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,pTex.x*m_groundSizeX)))];
        Vector2f val4 = m_gndDeriv[(int)(Math.max(0,Math.min(m_groundSizeY-1,1+pTex.y*m_groundSizeY)))*m_groundSizeX + (int)(Math.max(0,Math.min(m_groundSizeX-1,1+pTex.x*m_groundSizeX)))];
        float xOffs = (pTex.x*m_groundSizeX) - (int)(pTex.x*m_groundSizeX);
        float yOffs = (pTex.y*m_groundSizeY) - (int)(pTex.y*m_groundSizeY);
        /*Vector2f valH1 = (1.0f-xOffs)*val1 + xOffs*val2;
        Vector2f valH2 = (1.0f-xOffs)*val3 + xOffs*val4;
        Vector2f res = (1.0f-yOffs)*valH1 + yOffs*valH2;*/
        Vector2f valH1 = Vector2f.linear(val1, (1.0f-xOffs), val2, xOffs, null);
        Vector2f valH2 = Vector2f.linear(val3, (1.0f-xOffs), val4, xOffs, null);
        Vector2f res = Vector2f.linear(valH1,(1.0f-yOffs), valH2, yOffs, val1);
        valH2.set(0, 1);
        Vector2f resN = valH2;
        if (Math.abs(res.x) + Math.abs(res.y) > 0.0)
//            resN = res.normalized();
            res.normalise(resN);
        return( resN );
    }

    /** returns water depth at position p */
    float GetWaterDepth(Vector2f p){
        float v = 1.0f-GetGroundVal(p);
        return(MIN_WATER_DEPTH + (MAX_WATER_DEPTH-MIN_WATER_DEPTH)*v*v*v*v);
    }

    /** update the simulation time */
    void UpdateTime(float dTime){
        if (m_oldTime < 0)							// if we are entering the first time, set the current time as time
            m_oldTime = 0.0f;
        else
            m_oldTime = m_time;
        m_time = m_oldTime + dTime;					// time stepping
        m_elapsedTime = Math.abs(m_time - m_oldTime);
    }

    void ExpandWavePacketMemory(int targetNum){
        if (targetNum < m_packetNum)	// this should never happen
            return;
        /*WCHAR wcFileInfo[512];
        StringCchPrintf(wcFileInfo, 512, L"(INFO): Expanding packet memory from %i to %i packets (%i MB).\n", m_packetNum, targetNum, (targetNum)*(sizeof(WAVE_PACKET) + sizeof(GHOST_PACKET) + 4 * sizeof(int)) / (1024 * 1024));
        OutputDebugString(wcFileInfo);*/

        String info = String.format("(INFO): Expanding packet memory from %i to %i packets (%i MB).\n", m_packetNum, targetNum, (targetNum)*(WAVE_PACKET.SIZE + GHOST_PACKET.SIZE + 4 * 4) / (1024 * 1024));
        LogUtil.i(LogUtil.LogType.DEFAULT, info);

        WAVE_PACKET[] p = new WAVE_PACKET[targetNum];
        GHOST_PACKET[] pG = new GHOST_PACKET[targetNum];
        int[] uP = new int[targetNum];
        int[] fP = new int[targetNum];
        int[] uG = new int[targetNum];
        int[] fG = new int[targetNum];
        for (int i=0; i<m_packetNum; i++)
        {
            p[i] = m_packet[i];
            pG[i] = m_ghostPacket[i];
            uP[i] = m_usedPacket[i];
            fP[i] = m_freePacket[i];
            uG[i] = m_usedGhost[i];
            fG[i] = m_freeGhost[i];
        }
        for (int i = 0; i < targetNum - m_packetNum; i++)
            fP[m_freePackets+i] = m_packetNum + i;
        for (int i = 0; i<targetNum - m_packetNum; i++)
            fG[m_freeGhosts+i] = m_packetNum + i;
        /*if (m_packet != null)
        {
            delete[](m_packet);
            delete[](m_ghostPacket);
            delete[](m_usedPacket);
            delete[](m_freePacket);
            delete[](m_usedGhost);
            delete[](m_freeGhost);
        }*/
        m_packet = p;
        m_ghostPacket = pG;
        m_usedPacket = uP;
        m_freePacket = fP;
        m_usedGhost = uG;
        m_freeGhost = fG;
        m_freePackets += (targetNum - m_packetNum);
        m_freeGhosts += (targetNum - m_packetNum);
        m_packetNum = targetNum;
    }

    /** search a new free slot for a wave packet (must be thread safe in case of parallel computing) */
    int GetFreePackedID(){
        int firstfree;
//	#pragma omp critical
        synchronized (this){
            m_freePackets--;
            firstfree = m_freePacket[m_freePackets];	// pop last free packet
            m_usedPacket[m_usedPackets] = firstfree;	// push to used packets
            m_usedPackets++;
        }
        return firstfree;
    }
    void DeletePacket(int id){
//        #pragma omp critical
        synchronized (this){
            m_freePacket[m_freePackets] = m_usedPacket[id];
            m_freePackets++;
            m_usedPackets--;
            m_usedPacket[id] = m_usedPacket[m_usedPackets];
        }
    }

    /** search a new free slot for a wave packet (must be thread safe in case of parallel computing) */
    int GetFreeGhostID(){
        int firstghost;
//	#pragma omp critical
        synchronized (this)
        {
            m_freeGhosts--;
            firstghost = m_freeGhost[m_freeGhosts];
            m_usedGhost[m_usedGhosts] = firstghost;
            m_usedGhosts++;
        }
        return firstghost;
    }

    void DeleteGhost(int id){
//#pragma omp critical
        synchronized (this){
            m_freeGhost[m_freeGhosts] = m_usedGhost[id];
            m_freeGhosts++;
            m_usedGhosts--;
            m_usedGhost[id] = m_usedGhost[m_usedGhosts];
        }
    }

    /** adds a new packet at given positions, directions and wavenumber interval k_L and k_H */
    void CreatePacket(float pos1x, float pos1y, float pos2x, float pos2y, float dir1x, float dir1y, float dir2x, float dir2y, float k_L, float k_H, float E){
        // make sure we have enough memory
        if ( Math.max(m_usedPackets, m_usedGhosts) + 10 > m_packetNum)
            ExpandWavePacketMemory(Math.max(m_usedPackets,m_usedGhosts) + PACKET_BUFFER_DELTA);
        float speedDummy, kDummy;
        long v;
        int	firstfree = GetFreePackedID();
        m_packet[firstfree].pos1.set(pos1x,pos1y);
        m_packet[firstfree].pOld1.set(m_packet[firstfree].pos1);
        m_packet[firstfree].pos2.set(pos2x,pos2y);
        m_packet[firstfree].pOld2.set(m_packet[firstfree].pos2);
        m_packet[firstfree].dir1.set(dir1x,dir1y);
        m_packet[firstfree].dOld1.set(m_packet[firstfree].dir1);
        m_packet[firstfree].dir2.set(dir2x,dir2y);
        m_packet[firstfree].dOld2.set(m_packet[firstfree].dir2);
        m_packet[firstfree].phase = 0.0f;
        m_packet[firstfree].phOld = 0.0f;
        m_packet[firstfree].E = E;
        m_packet[firstfree].use3rd = false;
        m_packet[firstfree].bounced1 = false;
        m_packet[firstfree].bounced2 = false;
        m_packet[firstfree].bounced3 = false;
        m_packet[firstfree].sliding3 = false;
        // set the wavelength/freq interval
        m_packet[firstfree].k_L = k_L;
        float wd = GetWaterDepth(m_packet[firstfree].pos1);
        m_packet[firstfree].w0_L = (float)Math.sqrt((GRAVITY + k_L*k_L*SIGMA/DENSITY)*k_L*rational_tanh(k_L*wd));	// this take surface tension into account
        m_packet[firstfree].k_H = k_H;
        m_packet[firstfree].w0_H = (float)Math.sqrt((GRAVITY + k_H*k_H*SIGMA/DENSITY)*k_H*rational_tanh(k_H*wd));	// this take surface tension into account
        m_packet[firstfree].d_L = 0.0f;
        m_packet[firstfree].d_H = 0.0f;
        // set the representative wave as average of interval boundaries
        m_packet[firstfree].k = 0.5f*(m_packet[firstfree].k_L+m_packet[firstfree].k_H);
        m_packet[firstfree].w0 = (float)Math.sqrt((GRAVITY + m_packet[firstfree].k*m_packet[firstfree].k*SIGMA/DENSITY)*m_packet[firstfree].k*rational_tanh(m_packet[firstfree].k*wd));	// this takes surface tension into account
        v = GetWaveParameters(GetWaterDepth(m_packet[firstfree].pos1), m_packet[firstfree].w0, m_packet[firstfree].k/*, kDummy, m_packet[firstfree].speed1*/);
        m_packet[firstfree].speed1 = Float.intBitsToFloat(Numeric.decodeSecond(v));
        m_packet[firstfree].sOld1 = m_packet[firstfree].speed1;
        v = GetWaveParameters(GetWaterDepth(m_packet[firstfree].pos2), m_packet[firstfree].w0, m_packet[firstfree].k/*, kDummy, m_packet[firstfree].speed2*/);
        m_packet[firstfree].speed2 = Float.intBitsToFloat(Numeric.decodeSecond(v));
        m_packet[firstfree].sOld2 = m_packet[firstfree].speed2;
        m_packet[firstfree].envelope = Math.min(PACKET_ENVELOPE_MAXSIZE, Math.max(PACKET_ENVELOPE_MINSIZE, PACKET_ENVELOPE_SIZE_FACTOR*2.0f*Numeric.PI/m_packet[firstfree].k)); // adjust envelope size to represented wavelength
        m_packet[firstfree].ampOld = 0.0f;
        float a1 = Math.min(MAX_SPEEDNESS*2.0f*Numeric.PI / m_packet[firstfree].k, GetWaveAmplitude(m_packet[firstfree].envelope*Vector2f.distance(m_packet[firstfree].pos1, m_packet[firstfree].pos2), m_packet[firstfree].E, m_packet[firstfree].k));
        m_packet[firstfree].dAmp = 0.5f*(m_packet[firstfree].speed1+m_packet[firstfree].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[firstfree].envelope)*a1;

        // Test for wave number splitting -> if the packet interval crosses the slowest waves, divide so that each part has a monotonic speed function (assumed for travel spread/error calculation)
        int i1=firstfree;
        if ((m_packet[i1].w0_L>PACKET_SLOWAVE_W0) && (m_packet[i1].w0_H<PACKET_SLOWAVE_W0))
        {
            firstfree = GetFreePackedID();
            m_packet[firstfree] = m_packet[i1];  // copy the entiry wave packet information
            // set new interval boundaries to the slowest wave and update all influenced parameters
            m_packet[firstfree].k_L = PACKET_SLOWAVE_K;
            m_packet[firstfree].w0_L = PACKET_SLOWAVE_W0;
            v = GetWaveParameters(wd, m_packet[firstfree].w0_L, m_packet[firstfree].k_L/*, m_packet[firstfree].k_L, speedDummy*/);
            m_packet[firstfree].k_L = Float.intBitsToFloat(Numeric.decodeFirst(v));
            m_packet[firstfree].w0 = 0.5f*(m_packet[firstfree].w0_L+m_packet[firstfree].w0_H);
            m_packet[firstfree].k = 0.5f*(m_packet[firstfree].k_L+m_packet[firstfree].k_H);
            v = GetWaveParameters(wd, m_packet[firstfree].w0, m_packet[firstfree].k/*, m_packet[firstfree].k, m_packet[firstfree].speed1*/);
            m_packet[firstfree].k = Float.intBitsToFloat(Numeric.decodeFirst(v));
            m_packet[firstfree].speed1 = Float.intBitsToFloat(Numeric.decodeSecond(v));

            m_packet[firstfree].speed2 = m_packet[firstfree].speed1;
            m_packet[firstfree].envelope = Math.min(PACKET_ENVELOPE_MAXSIZE, Math.max(PACKET_ENVELOPE_MINSIZE, PACKET_ENVELOPE_SIZE_FACTOR*2.0f*Numeric.PI/m_packet[firstfree].k)); // adjust envelope size to represented wavelength
            m_packet[firstfree].ampOld = 0.0f;
            a1 = Math.min(MAX_SPEEDNESS*2.0f*Numeric.PI / m_packet[firstfree].k, GetWaveAmplitude(m_packet[firstfree].envelope*Vector2f.distance(m_packet[firstfree].pos1, m_packet[firstfree].pos2), m_packet[firstfree].E, m_packet[firstfree].k));
            m_packet[firstfree].dAmp = 0.5f*(m_packet[firstfree].speed1 + m_packet[firstfree].speed2)*m_elapsedTime / (PACKET_BLEND_TRAVEL_FACTOR*m_packet[firstfree].envelope)*a1;
            // also adjust freq. interval and envelope size of existing wave
            m_packet[i1].k_H = PACKET_SLOWAVE_K;
            m_packet[i1].w0_H = PACKET_SLOWAVE_W0;
            v = GetWaveParameters(wd, m_packet[i1].w0_H, m_packet[i1].k_H/*, m_packet[i1].k_H, speedDummy*/);
            m_packet[i1].k_H = Float.intBitsToFloat(Numeric.decodeFirst(v));

            m_packet[i1].w0 = 0.5f*(m_packet[i1].w0_L+m_packet[i1].w0_H);
            m_packet[i1].k = 0.5f*(m_packet[i1].k_L+m_packet[i1].k_H);
            v = GetWaveParameters(wd, m_packet[i1].w0, m_packet[i1].k/*, m_packet[i1].k, m_packet[i1].speed1*/);
            m_packet[i1].k = Float.intBitsToFloat(Numeric.decodeFirst(v));
            m_packet[i1].speed1 = Float.intBitsToFloat(Numeric.decodeSecond(v));

            m_packet[i1].speed2 = m_packet[i1].speed1;
            m_packet[i1].envelope = Math.min(PACKET_ENVELOPE_MAXSIZE, Math.max(PACKET_ENVELOPE_MINSIZE, PACKET_ENVELOPE_SIZE_FACTOR*2.0f*Numeric.PI/m_packet[i1].k)); // adjust envelope size to represented wavelength
            m_packet[i1].ampOld = 0.0f;
            a1 = Math.min(MAX_SPEEDNESS*2.0f*Numeric.PI / m_packet[i1].k, GetWaveAmplitude(m_packet[i1].envelope*Vector2f.distance(m_packet[i1].pos1, m_packet[i1].pos2), m_packet[i1].E, m_packet[i1].k));
            m_packet[i1].dAmp = 0.5f*(m_packet[i1].speed1 + m_packet[i1].speed2)*m_elapsedTime / (PACKET_BLEND_TRAVEL_FACTOR*m_packet[i1].envelope)*a1;
        }
    }

    /** adds a new linear wave at normalized position x,y with wavelength boundaries lambda_L and lambda_H (in meters) */
    void CreateLinearWavefront(float xPos, float yPos, float dirx, float diry, float crestlength, float lambda_L, float lambda_H, float E){
        Vector2f dir = new Vector2f(dirx,diry);
        dir.normalise();
//        Vector2f wfAlign = 0.5f*crestlength*Vector2f(dir.y,-dir.x);
        float wfAlignX = 0.5f*crestlength*dir.y;
        float wfAlignY = 0.5f*crestlength*-dir.x;
        CreatePacket( xPos-wfAlignX, yPos-wfAlignY, xPos+wfAlignX, yPos+wfAlignY, dir.x, dir.y, dir.x, dir.y, 2.0f*Numeric.PI/lambda_L, 2.0f*Numeric.PI/lambda_H, E);
    }

    /** adds a new linear wave at position (xPos,yPos) with wavelength boundaries lambda_L and lambda_H (in meters), spreadFactor = 1 -> 45 degrees */
    void CreateSpreadingPacket(float xPos, float yPos, float dirx, float diry, float spreadFactor, float crestlength, float lambda_L, float lambda_H, float E){
        Vector2f dir = new Vector2f(dirx,diry);
        dir.normalise();
//        Vector2f wfAlign = 0.5f*crestlength*Vector2f(dir.y(),-dir.x());
        float wfAlignX = 0.5f*crestlength*dir.y;
        float wfAlignY = 0.5f*crestlength*-dir.x;
//        Vector2f dirSpread1 = dir - spreadFactor*Vector2f(dir.y(),-dir.x());
        float dirSpread1X = dir.x - spreadFactor * dir.y;
        float dirSpread1Y = dir.y - spreadFactor * -dir.x;
//        Vector2f dirSpread2 = dir + spreadFactor*Vector2f(dir.y(),-dir.x());
        float dirSpread2X = dir.x + spreadFactor * dir.y;
        float dirSpread2Y = dir.y + spreadFactor * -dir.x;
        CreatePacket( xPos-wfAlignX, yPos-wfAlignY, xPos+wfAlignX, yPos+wfAlignY, dirSpread1X, dirSpread1Y,
                dirSpread2X, dirSpread2Y, 2.0f*Numeric.PI/lambda_L, 2.0f*Numeric.PI/lambda_H, E);
    }

    /** adds a new circular wave at normalized position x,y with wavelength boundaries lambda_L and lambda_H (in meters) */
    void CreateCircularWavefront(float xPos, float yPos, float radius, float lambda_L, float lambda_H, float E){
        final float M_PI = Numeric.PI;
        // adapt initial packet crestlength to impact radius and wavelength
        float dAng = Math.min(24.0f, 360.0f * ((0.5f*lambda_L + 0.5f*lambda_H)*3.0f) / (2.0f*M_PI*radius));
        for (float i = 0; i < 360.0f; i += dAng)
            CreatePacket(
                    xPos + radius*(float)Math.sin(i*M_PI / 180.0f), yPos + radius*(float)Math.cos(i*M_PI / 180.0f),
                    xPos + radius*(float)Math.sin((i + dAng)*M_PI / 180.0f), yPos + radius*(float)Math.cos((i + dAng)*M_PI / 180.0f),
                    (float)Math.sin(i*M_PI / 180.0f), (float)Math.cos(i*M_PI / 180.0f),
                    (float)Math.sin((i + dAng)*M_PI / 180.0), (float)Math.cos((i + dAng)*M_PI / 180.0f),
                    2.0f*M_PI / lambda_L, 2.0f*M_PI / lambda_H, E);
    }

    long GetWaveParameters(float waterDepth, float w_0, float kIn/*, float &k_out, float &speed_out*/){
        float k_out, speed_out;

        float k = kIn;
        float dk = 1.0f;
        float kOld;
        int it = 0;
        while ((dk > 1.0e-04) && (it<6))
        {
            kOld = k;
            k = (float) (w_0/Math.sqrt((GRAVITY/k+k*SIGMA/DENSITY)*rational_tanh(k*waterDepth)));		// this includes surface tension / capillary waves
            dk = Math.abs(k-kOld);
            it++;
        }
        k_out = k;
        float t = rational_tanh(k*waterDepth);
	    final float c = SIGMA/DENSITY;
        speed_out = (float) (((c*k*k + GRAVITY)*(t + waterDepth*k*(1.0-t*t)) + 2.0*c*k*k*t) / (2.0*Math.sqrt(k*(c*k*k + GRAVITY)*t)));   // this is group speed as dw/dk
        return Numeric.encode(Float.floatToIntBits(k_out), Float.floatToIntBits(speed_out));
    }

    float GetPhaseSpeed(float w_0, float kIn) { return( w_0/kIn );}
    // area = surface area of the wave packet, E = Energy, k = wavenumber
    // computing the amplitude from energy flux for a wave packet
    float GetWaveAmplitude(float area, float E, float k){
        return (float) Math.sqrt(Math.abs(E)/(Math.abs(area)*0.5f*(DENSITY*GRAVITY+SIGMA*k*k)));
    }

    float GetIntersectionDistance(Vector2f pos1, Vector2f dir1, Vector2f pos2, Vector2f dir2){
        /*ParametrizedLine<float, 2> line1(pos1, dir1);
        Hyperplane<float, 2> line2 = Hyperplane<float, 2>::Through(pos2, pos2+dir2);
        float intPointDist = line1.intersectionParameter(line2);
        if (abs(intPointDist) > 10000.0f)
            intPointDist = 10000.0f;
        return intPointDist;*/

        throw new UnsupportedOperationException();
    }

    static class ReturnedValue{
        float kIn, speedI;
        float speedOut;
        boolean bounced;
    }

    /** advects a single packet vertex with groupspeed, returns 1 if boundary reflection occured, first value is the bound, second is speedOut */
    long AdvectPacketVertex(float elapsedTime, Vector2f posIn, Vector2f dirIn, float w0, float kIn, float speedIn,
                                     final Vector2f posOut, final Vector2f dirOut/*, float &speedOut*/){
        boolean bounced = false;
        // intialize the output with the input
        posOut.set(posIn);
        dirOut.set(dirIn);
        float speedOut = speedIn;

        // compute new direction and speed based on snells law (depending on water depth)
        float speed1, k;
        long v = GetWaveParameters(GetWaterDepth( posIn ), w0, kIn/*, k, speed1*/);
//        k = Float.intBitsToFloat(Numeric.decodeFirst(v));
        speed1 = Float.intBitsToFloat(Numeric.decodeSecond(v));

        speedOut = speed1;  // the new speed is defined by the speed of this wave at this water depth, this is does not necessarily respect snells law!
        Vector2f nDir = GetGroundNormal( posIn );
        if (Math.abs(nDir.x)+Math.abs(nDir.y) > 0.1f)	// if there is a change in water depth here, indicated by a non-zero ground normal
        {
//            Vector2f pNext = posIn + elapsedTime*speed1*dirIn;
            Vector2f pNext = Vector2f.linear(posIn, dirIn, elapsedTime*speed1, null);
            float speed2;
            v= GetWaveParameters(GetWaterDepth(pNext), w0, kIn/*, k, speed2*/);
//            k = Float.intBitsToFloat(Numeric.decodeFirst(v));
            speed2 = Float.intBitsToFloat(Numeric.decodeSecond(v));

            float cos1 = -Vector2f.dot(nDir, dirIn);
            float cos2 = (float) Math.sqrt( Math.max(0.0f, 1.0f - (speed2*speed2)/(speed1*speed1)*(1.0f - cos1*cos1) ));
            Vector2f nRefrac;
            if (cos1 <= 0.0f)
//                nRefrac = speed2/speed1*dirIn + (speed2/speed1*cos1 + cos2)*nDir;
                nRefrac = Vector2f.linear(dirIn, speed2/speed1, nDir, speed2/speed1*cos1 + cos2, null);
            else
//                nRefrac = speed2/speed1*dirIn + (speed2/speed1*cos1 - cos2)*nDir;
                nRefrac = Vector2f.linear(dirIn, speed2/speed1, nDir, speed2/speed1*cos1 - cos2, null);
            if (nRefrac.length() > 0.000001f)
                 nRefrac.normalise(dirOut);
        }
//        posOut = posIn + elapsedTime*speed1*dirOut;  // advect wave vertex position
        Vector2f.linear(posIn, dirOut, elapsedTime*speed1, posOut);

        // if we ran into a boundary -> step back and bounce off
        if (GetBoundaryDist(posOut)<0.0f)
        {
            Vector2f nor = GetBoundaryNormal(posOut);
            float a = Vector2f.dot(nor, dirOut);
            if (a <= -0.08f)  // a wave reflects if it travels with >4.5 degrees towards a surface. Otherwise, it gets diffracted
            {
                bounced = true;
                // step back until we are outside the boundary
                Vector2f pD = posIn;
//                Vector2f vD = elapsedTime*speedIn*dirOut;
                Vector2f vD = Vector2f.scale(dirOut, elapsedTime*speedIn, null);
                for (int j = 0; j < 16; j++)
                {
//                    Vector2f pDD = pD + vD;
                    Vector2f pDD = Vector2f.add(pD, vD, null);
                    if (GetBoundaryDist(pDD) > 0.0f)
                        pD = pDD;
//                    vD = 0.5f*vD;
                    vD.scale(0.5f);
                }
               /* Vector2f wayVec = pD - posIn;
                float lGone = wayVec.norm();*/
               float lGone = Vector2f.distance(pD, posIn);
                posOut.set(pD);
                // compute the traveling direction after the bounce
//                dirOut = -dirOut;
                dirOut.scale(-1);
                Vector2f nor2 = GetBoundaryNormal(posOut);
                float a2 = Vector2f.dot(nor2, dirOut);
                /*Vector2f bFrac = a2*nor2 - dirOut;
                Vector2f d0 = dirOut + 2.0f*bFrac;
                dirOut = d0.normalized();*/

                dirOut.x = dirOut.x + 2 * (a2*nor2.x - dirOut.x);
                dirOut.y = dirOut.y + 2 * (a2*nor2.y - dirOut.y);
                dirOut.normalise();

//                posOut += (elapsedTime*speedOut - lGone)*dirOut;
                Vector2f.linear(posOut, dirOut, elapsedTime*speedOut - lGone, posOut);
            }
        }

        // if we got trapped in a boundary (again), just project onto the nearest surface (this approximates multiple bounces)
        if (GetBoundaryDist(posOut) < 0.0)
            for (int i2=0; i2<16; i2++)
//                posOut += -0.5f*GetBoundaryDist(posOut)*GetBoundaryNormal(posOut);
                Vector2f.linear(posOut, GetBoundaryNormal(posOut), -0.5f*GetBoundaryDist(posOut), posOut);

        return Numeric.encode(bounced ?1:0, Float.floatToIntBits(speedOut));
    }

    /** updates the wavefield using the movin wavefronts and generated an output image from the wavefield */
    void AdvectWavePackets(float dTime){
        UpdateTime(dTime);
        if (m_elapsedTime <= 0.0)  // if there is no time advancement, do not update anything..
            return;

        long v;
        // compute the new packet vertex positions, directions and speeds based on snells law
//	#pragma omp parallel for
        for (int uP=0; uP<m_usedPackets; uP++)
        {
            int i1 = m_usedPacket[uP];
            m_packet[i1].pOld1.set(m_packet[i1].pos1);
            m_packet[i1].dOld1.set(m_packet[i1].dir1);
            m_packet[i1].sOld1 = m_packet[i1].speed1;
            v = AdvectPacketVertex(m_elapsedTime, m_packet[i1].pOld1, m_packet[i1].dOld1, m_packet[i1].w0, m_packet[i1].k, m_packet[i1].sOld1, m_packet[i1].pos1, m_packet[i1].dir1);
            m_packet[i1].bounced1 = (Numeric.decodeFirst(v) != 0);
            m_packet[i1].speed1 = Float.intBitsToFloat(Numeric.decodeSecond(v));

            m_packet[i1].pOld2.set(m_packet[i1].pos2);
            m_packet[i1].dOld2.set(m_packet[i1].dir2);
            m_packet[i1].sOld2 = m_packet[i1].speed2;
            v = AdvectPacketVertex(m_elapsedTime, m_packet[i1].pOld2, m_packet[i1].dOld2, m_packet[i1].w0, m_packet[i1].k, m_packet[i1].sOld2, m_packet[i1].pos2, m_packet[i1].dir2);
            m_packet[i1].bounced2 =(Numeric.decodeFirst(v) != 0);
            m_packet[i1].speed2 = Float.intBitsToFloat(Numeric.decodeSecond(v));
            // measure new wave k at phase speed at wave packet center, advect all representative waves inside
            m_packet[i1].phOld = m_packet[i1].phase;
            float packetSpeed = 0.5f*(m_packet[i1].speed1 + m_packet[i1].speed2);
            m_packet[i1].phase += m_elapsedTime*(GetPhaseSpeed(m_packet[i1].w0, m_packet[i1].k) - packetSpeed)*m_packet[i1].k;	// advect phase with difference between group speed and wave speed
        }

        // compute the new position, direction and speed of 3rd vertex (if present)
//	#pragma omp parallel for
        for (int uP = 0; uP<m_usedPackets; uP++)
            if (m_packet[m_usedPacket[uP]].use3rd)
            {
                int i1 = m_usedPacket[uP];
                m_packet[i1].pOld3.set(m_packet[i1].pos3);
                m_packet[i1].dOld3.set(m_packet[i1].dir3);
                m_packet[i1].sOld3 = m_packet[i1].speed3;
                v = AdvectPacketVertex(m_elapsedTime, m_packet[i1].pOld3,  m_packet[i1].dOld3,  m_packet[i1].w0, m_packet[i1].k, m_packet[i1].sOld3,  m_packet[i1].pos3,  m_packet[i1].dir3);
                m_packet[i1].bounced3 = Numeric.decodeFirst(v) != 0;
                m_packet[i1].speed3 = Float.intBitsToFloat(Numeric.decodeSecond(v));
                if (!m_packet[i1].bounced3)	// advect 3rd as sliding vertex (from now on it is)
                {
                    Vector2f nDir = GetBoundaryNormal( m_packet[i1].pos3 ); // get sliding direction
                    m_packet[i1].dir3.set(-nDir.y, nDir.x);
                    if (Vector2f.dot(m_packet[i1].dir3, m_packet[i1].dOld3) < 0)
//                        m_packet[i1].dir3 = -m_packet[i1].dir3;
                        m_packet[i1].dir3.scale(-1);
                    Vector2f pD = m_packet[i1].pos3;						// project advected point onto closest boundary
                    for (int i3=0; i3<16; i3++)
//                        pD += -0.5f*GetBoundaryDist(pD)*GetBoundaryNormal(pD);
                        Vector2f.linear(pD, GetBoundaryNormal(pD), -0.5f*GetBoundaryDist(pD), pD);
//                    m_packet[i1].pos3 = pD;
                }
                else if ((!m_packet[i1].sliding3) && (!m_packet[i1].bounced1) && (!m_packet[i1].bounced2))  // "has to bounce"-3rd vertex -> find new "has to bounce" point if no other vertex bounced
                {
                    float s = 0.0f;
                    float sD = 0.5f;
                    Vector2f posOld = m_packet[i1].pOld1;
                    Vector2f dirOld = m_packet[i1].dOld1;
                    float speedOld = m_packet[i1].sOld1;
                    Vector2f pos = m_packet[i1].pos1;
                    Vector2f dir = m_packet[i1].dir1;
                    float speed = m_packet[i1].speed1;
                    float wN = m_packet[i1].k;
                    float w0 = m_packet[i1].w0;
                    for (int j=0; j<16; j++)
                    {
//                        Vector2f p = (1.0f-(s+sD))*m_packet[i1].pOld1 + (s+sD)*m_packet[i1].pOld3;
                        Vector2f p = Vector2f.linear(m_packet[i1].pOld1, 1.0f-(s+sD), m_packet[i1].pOld3, s+sD, null);
//                        Vector2f d = (1.0f-(s+sD))*m_packet[i1].dOld1 + (s+sD)*m_packet[i1].dOld3;
                        Vector2f d = Vector2f.linear(m_packet[i1].dOld1, 1.0f-(s+sD), m_packet[i1].dOld3, s+sD, null);
                        float sp = (1.0f-(s+sD))*m_packet[i1].sOld1 + (s+sD)*m_packet[i1].sOld3;
                        Vector2f posD = new Vector2f(), dirD = new Vector2f();
                        float speedD;
                        v = AdvectPacketVertex(m_elapsedTime, p, d, w0, wN, sp, posD, dirD/*, speedD*/);
                        if (Numeric.decodeFirst(v) == 0)
                        {
                            s += sD;
                            posOld = p;
                            dirOld = d;
                            speedOld = sp;
                            pos = posD;
                            dir = dirD;
                            speed = Float.intBitsToFloat(Numeric.decodeSecond(v));
                        }
                        sD = 0.5f*sD;
                    }
                    m_packet[i1].pOld3.set(posOld);
                    m_packet[i1].dOld3.set(dirOld);
                    m_packet[i1].dOld3.normalise();

                    m_packet[i1].sOld3 = speedOld;
                    m_packet[i1].pos3.set(pos);
                    m_packet[i1].dir3.set(dir);
                    m_packet[i1].speed3 = speed;
                }
            }


        // first contact to a boundary -> sent a ghost packet, make packet invisible for now, add 3rd vertex
        if (m_usedGhosts + m_usedPackets > m_packetNum)
            ExpandWavePacketMemory(m_usedGhosts + m_usedPackets + PACKET_BUFFER_DELTA);
//	#pragma omp parallel for
        for (int uP = m_usedPackets-1; uP>=0; uP--)
            if ((!m_packet[m_usedPacket[uP]].use3rd) && (m_packet[m_usedPacket[uP]].bounced1 || m_packet[m_usedPacket[uP]].bounced2))
            {
                int i1 = m_usedPacket[uP];
                int firstghost = GetFreeGhostID();
//                m_ghostPacket[firstghost].pos = 0.5f*(m_packet[i1].pOld1+m_packet[i1].pOld2);
                Vector2f.mix(m_packet[i1].pOld1, m_packet[i1].pOld2, 0.5f, m_ghostPacket[firstghost].pos);
//                m_ghostPacket[firstghost].dir = (m_packet[i1].dOld1+m_packet[i1].dOld2).normalized(); // the new position is wrong after the reflection, so use the old direction instead
                Vector2f.add(m_packet[i1].dOld1,m_packet[i1].dOld2,m_ghostPacket[firstghost].dir);
                m_ghostPacket[firstghost].dir.normalise();
                m_ghostPacket[firstghost].speed = 0.5f*(m_packet[i1].sOld1+m_packet[i1].sOld2);
                m_ghostPacket[firstghost].envelope = m_packet[i1].envelope;
                m_ghostPacket[firstghost].ampOld = m_packet[i1].ampOld;
                m_ghostPacket[firstghost].dAmp = m_ghostPacket[firstghost].ampOld* m_ghostPacket[firstghost].speed*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_ghostPacket[firstghost].envelope);
                m_ghostPacket[firstghost].k = m_packet[i1].k;
                m_ghostPacket[firstghost].phase = m_packet[i1].phOld;
                m_ghostPacket[firstghost].dPhase = m_packet[i1].phase-m_packet[i1].phOld;
                m_ghostPacket[firstghost].bending = GetIntersectionDistance(m_ghostPacket[firstghost].pos, m_ghostPacket[firstghost].dir, m_packet[i1].pOld1, m_packet[i1].dOld1);
                // hide this packet from display
                m_packet[i1].ampOld = 0.0f;
                m_packet[i1].dAmp = 0.0f;
                // emit all (higher-)frequency waves after a bounce
                if ((PACKET_BOUNCE_FREQSPLIT) && (m_packet[i1].k_L < PACKET_BOUNCE_FREQSPLIT_K))  // split the frequency range if smallest wave is > 20cm
                {
                    m_packet[i1].k_L = PACKET_BOUNCE_FREQSPLIT_K;
                    m_packet[i1].w0_L = (float)Math.sqrt(GRAVITY/m_packet[i1].k_L)*m_packet[i1].k_L;  // initial guess for angular frequency
                    m_packet[i1].w0 = 0.5f*(m_packet[i1].w0_L+m_packet[i1].w0_H);
                    // distribute the error according to current speed difference
                    float dummySpeed;
                    Vector2f pos = Vector2f.mix(m_packet[i1].pos1, m_packet[i1].pos2, 0.5f, null);

                    float wd = GetWaterDepth(pos);
                    v = GetWaveParameters(wd, m_packet[i1].w0_L, m_packet[i1].k_L/*, m_packet[i1].k_L, dummySpeed*/);
                    m_packet[i1].k_L = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    v = GetWaveParameters(wd, m_packet[i1].w0_H, m_packet[i1].k_H/*, m_packet[i1].k_H, dummySpeed*/);
                    m_packet[i1].k_H = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    v = GetWaveParameters(wd, m_packet[i1].w0, 0.5f*(m_packet[i1].k_L+m_packet[i1].k_H)/*, m_packet[i1].k, dummySpeed*/);
                    m_packet[i1].k = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    m_packet[i1].d_L = 0.0f; m_packet[i1].d_H = 0.0f; // reset the internally tracked error
                    m_packet[i1].envelope = Math.min(PACKET_ENVELOPE_MAXSIZE, Math.max(PACKET_ENVELOPE_MINSIZE, PACKET_ENVELOPE_SIZE_FACTOR*2.0f*Numeric.PI/m_packet[i1].k));
                }
                //if both vertices bounced, the reflected wave needs to smoothly reappear
                if (m_packet[i1].bounced1==m_packet[i1].bounced2)
                {
                    m_packet[i1].ampOld = 0.0f;
                    m_packet[i1].dAmp = 0.5f*(m_packet[i1].speed1+m_packet[i1].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[i1].envelope)*GetWaveAmplitude( m_packet[i1].envelope* Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos2), m_packet[i1].E, m_packet[i1].k);
                }
                if (m_packet[i1].bounced1 != m_packet[i1].bounced2)	  // only one vertex bounced -> insert 3rd "wait for bounce" vertex and reorder such that 1st is waiting for bounce
                {
                    if (m_packet[i1].bounced1)  // if the first bounced an the second did not -> exchange the two points, as we assume that the second bounced already and the 3rd will be "ahead" of the first..
                    {
                        WAVE_PACKET seg = m_packet[i1];  // use the 3rd vertex as copy element
                        seg.pos3.set(seg.pos2); seg.pos2.set(seg.pos1); seg.pos1.set(seg.pos3);
                        seg.pOld3.set(seg.pOld2); seg.pOld2.set(seg.pOld1); seg.pOld1.set(seg.pOld3);
                        seg.dir3.set(seg.dir2); seg.dir2.set(seg.dir1); seg.dir1.set(seg.dir3);
                        seg.dOld3.set(seg.dOld2); seg.dOld2.set(seg.dOld1); seg.dOld1.set(seg.dOld3);
                        seg.speed3 = seg.speed2; seg.speed2 = seg.speed1; seg.speed1 = seg.speed3;
                        seg.sOld3 = seg.sOld2; seg.sOld2 = seg.sOld1; seg.sOld1 = seg.sOld3;
                        seg.bounced3 = seg.bounced2; seg.bounced2 = seg.bounced1; seg.bounced1 = seg.bounced3;
                    }
                    float s = 0.0f;
                    float sD = 0.5f;
                    Vector2f posOld = m_packet[i1].pOld1;
                    Vector2f dirOld = m_packet[i1].dOld1;
                    float speedOld = m_packet[i1].sOld1;
                    Vector2f pos = m_packet[i1].pos1;
                    Vector2f dir = m_packet[i1].dir1;
                    float speed = m_packet[i1].speed1;
                    float wN = m_packet[i1].k;
                    float w0 = m_packet[i1].w0;
                    for (int j=0; j<16; j++)				// find the last point before the boundary that does not bounce in this timestep, it becomes the 3rd point
                    {
//                        Vector2f p = (1.0f-(s+sD))*m_packet[i1].pOld1 + (s+sD)*m_packet[i1].pOld2;
                        Vector2f p = Vector2f.linear(m_packet[i1].pOld1, 1.0f-(s+sD), m_packet[i1].pOld2, s+sD, null);
//                        Vector2f d = (1.0f-(s+sD))*m_packet[i1].dOld1 + (s+sD)*m_packet[i1].dOld2;
                        Vector2f d = Vector2f.linear(m_packet[i1].dOld1, 1.0f-(s+sD), m_packet[i1].dOld2, s+sD, null);
                        float sp = (1.0f-(s+sD))*m_packet[i1].sOld1 + (s+sD)*m_packet[i1].sOld2;
                        Vector2f posD = new Vector2f(), dirD = new Vector2f();
                        v = AdvectPacketVertex(m_elapsedTime, p, d, w0, wN, sp, posD, dirD/*, speedD*/);
                        if (Numeric.decodeFirst(v) == 0)
                        {
                            s += sD;
                            posOld = p;
                            dirOld = d;
                            speedOld = sp;
                            pos = posD;
                            dir = dirD;
                            speed = Float.intBitsToFloat(Numeric.decodeSecond(v));
                        }
                        sD = 0.5f*sD;
                    }
                    // the new 3rd vertex has "has to bounce" state (not sliding yet)
                    m_packet[i1].pOld3.set(posOld);
                    m_packet[i1].dOld3.set(dirOld);
                    m_packet[i1].dOld3.normalise();
                    m_packet[i1].sOld3 = speedOld;
                    m_packet[i1].pos3.set(pos);
                    m_packet[i1].dir3.set(dir);
                    m_packet[i1].speed3 = speed;
                }
            }


        // define new state based on current state and bouncings
//	#pragma omp parallel for
        for (int uP = 0; uP<m_usedPackets; uP++)
        {
            int i1 = m_usedPacket[uP];
            if (!m_packet[i1].use3rd)  // no 3rd vertex present
            {
                if (m_packet[i1].bounced1!=m_packet[i1].bounced2)  // on point bounced -> case "intiate new 3rd vertex"
                {
                    m_packet[i1].use3rd = true;
                    m_packet[i1].bounced3 = false;
                    m_packet[i1].sliding3 = false;
                }
            }
            else // 3rd vertex already present
            {
                if (!m_packet[i1].sliding3)  // 3rd point was in "waiting for bounce" state
                {
                    if (!m_packet[i1].bounced3)								// case: 3rd "has to bounce" vertex did not bounce -> make it sliding in any case
                        m_packet[i1].sliding3 = true;
                    else if ((m_packet[i1].bounced1) || (m_packet[i1].bounced2))	// case: 3rd "has to bounce" and one other point bounced as well -> release 3rd vertex
                        m_packet[i1].use3rd = false;
                }
                else // 3rd point was already in "sliding" state
                {
                    if (m_packet[i1].bounced3)				// if sliding 3rd point bounced, release it
                        m_packet[i1].use3rd = false;
                }
                // if we released this wave from a boundary (3rd vertex released) -> blend it smoothly in again
                if (!m_packet[i1].use3rd)
                {
                    m_packet[i1].ampOld = 0.0f;
                    m_packet[i1].dAmp = 0.5f*(m_packet[i1].speed1+m_packet[i1].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[i1].envelope)*GetWaveAmplitude( m_packet[i1].envelope*Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos2), m_packet[i1].E, m_packet[i1].k);
                }
            }
        }


        // wavenumber interval subdivision if travel distance between fastest and slowest wave packets differ more than PACKET_SPLIT_DISPERSION x envelope size
        if ( Math.max(m_usedGhosts + m_usedPackets, 2*m_usedPackets) > m_packetNum)
            ExpandWavePacketMemory(Math.max(m_usedGhosts + m_usedPackets, 2 * m_usedPackets) + PACKET_BUFFER_DELTA);
//	#pragma omp parallel for
        for (int uP = m_usedPackets-1; uP>=0; uP--)
            if (!m_packet[m_usedPacket[uP]].use3rd)
            {
                int i1 = m_usedPacket[uP];
                float speedDummy,kDummy;
//                Vector2f pos = 0.5f*(m_packet[i1].pos1+m_packet[i1].pos2);
                Vector2f pos = Vector2f.mix(m_packet[i1].pos1,m_packet[i1].pos2, 0.5f, null);
                float wd = GetWaterDepth(pos);
                v = GetWaveParameters(wd, m_packet[i1].w0, m_packet[i1].k/*, kDummy, speedDummy*/);
                speedDummy = Float.intBitsToFloat(Numeric.decodeSecond(v));
                float dist_Ref = m_elapsedTime*speedDummy;
                v = GetWaveParameters(wd, m_packet[i1].w0_L, m_packet[i1].k_L/*, kDummy, speedDummy*/);
                kDummy = Float.intBitsToFloat(Numeric.decodeFirst(v));
                m_packet[i1].k_L = kDummy;
                m_packet[i1].d_L += Math.abs(m_elapsedTime*speedDummy-dist_Ref);  // taking the abs augments any errors caused by waterdepth independent slowest wave assumption..
                v = GetWaveParameters(wd, m_packet[i1].w0_H, m_packet[i1].k_H/*, kDummy, speedDummy*/);
                kDummy = Float.intBitsToFloat(Numeric.decodeFirst(v));
                m_packet[i1].k_H = kDummy;
                m_packet[i1].d_H += Math.abs(m_elapsedTime*speedDummy-dist_Ref);
                if (m_packet[i1].d_L+m_packet[i1].d_H > PACKET_SPLIT_DISPERSION*m_packet[i1].envelope)  // if fastest/slowest waves in this packet diverged too much -> subdivide
                {
                    // first create a ghost for the old packet
                    int firstghost = GetFreeGhostID();
//                    m_ghostPacket[firstghost].pos = 0.5f*(m_packet[i1].pOld1+m_packet[i1].pOld2);
                    Vector2f.mix(m_packet[i1].pOld1,m_packet[i1].pOld2, 0.5f, m_ghostPacket[firstghost].pos);
//                    m_ghostPacket[firstghost].dir = (0.5f*(m_packet[i1].pos1+m_packet[i1].pos2)-0.5f*(m_packet[i1].pOld1+m_packet[i1].pOld2)).normalized();
                    m_ghostPacket[firstghost].dir.x = (0.5f*(m_packet[i1].pos1.x+m_packet[i1].pos2.x)-0.5f*(m_packet[i1].pOld1.x+m_packet[i1].pOld2.x));
                    m_ghostPacket[firstghost].dir.y = (0.5f*(m_packet[i1].pos1.y+m_packet[i1].pos2.y)-0.5f*(m_packet[i1].pOld1.y+m_packet[i1].pOld2.y));
                    m_ghostPacket[firstghost].dir.normalise();

                    m_ghostPacket[firstghost].speed = 0.5f*(m_packet[i1].sOld1+m_packet[i1].sOld2);
                    m_ghostPacket[firstghost].envelope = m_packet[i1].envelope;
                    m_ghostPacket[firstghost].ampOld = m_packet[i1].ampOld;
                    m_ghostPacket[firstghost].dAmp = m_ghostPacket[firstghost].ampOld* m_ghostPacket[firstghost].speed*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_ghostPacket[firstghost].envelope);
                    m_ghostPacket[firstghost].k = m_packet[i1].k;
                    m_ghostPacket[firstghost].phase = m_packet[i1].phOld;
                    m_ghostPacket[firstghost].dPhase = m_packet[i1].phase-m_packet[i1].phOld;
                    m_ghostPacket[firstghost].bending = GetIntersectionDistance(m_ghostPacket[firstghost].pos, m_ghostPacket[firstghost].dir, m_packet[i1].pOld1, m_packet[i1].dOld1);
                    // create new packet and copy ALL parameters
                    int firstfree = GetFreePackedID();
                    m_packet[firstfree] = m_packet[i1];
                    // split the frequency range
                    m_packet[firstfree].k_H = m_packet[i1].k;
                    m_packet[firstfree].w0_H = m_packet[i1].w0;
                    m_packet[firstfree].w0 = 0.5f*(m_packet[firstfree].w0_L+m_packet[firstfree].w0_H);
                    // distribute the error according to current speed difference
                    float speed_L,speed_M,speed_H;
                    v= GetWaveParameters( wd, m_packet[firstfree].w0_L, m_packet[firstfree].k_L/*, m_packet[firstfree].k_L, speed_L*/);
                    m_packet[firstfree].k_L = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    speed_L = Float.intBitsToFloat(Numeric.decodeSecond(v));
                    v = GetWaveParameters( wd, m_packet[firstfree].w0_H, m_packet[firstfree].k_H/*, m_packet[firstfree].k_H, speed_H*/);
                    m_packet[firstfree].k_H = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    speed_H = Float.intBitsToFloat(Numeric.decodeSecond(v));
                    v = GetWaveParameters( wd, m_packet[firstfree].w0, 0.5f*(m_packet[firstfree].k_L+m_packet[firstfree].k_H)/*, m_packet[firstfree].k, speed_M*/);
                    m_packet[firstfree].k = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    speed_M = Float.intBitsToFloat(Numeric.decodeSecond(v));

                    float dSL = Math.abs(Math.abs(speed_L)-Math.abs(speed_M));
                    float dSH = Math.abs(Math.abs(speed_H)-Math.abs(speed_M));
                    float d_All = m_packet[i1].d_L;
                    m_packet[firstfree].d_L = dSL*d_All / (dSH + dSL);
                    m_packet[firstfree].d_H = d_All - m_packet[firstfree].d_L;
                    m_packet[firstfree].envelope = Math.min(PACKET_ENVELOPE_MAXSIZE, Math.max(PACKET_ENVELOPE_MINSIZE, PACKET_ENVELOPE_SIZE_FACTOR*2.0f*Numeric.PI/m_packet[firstfree].k));
                    // set the new upper freq. boundary and representative freq.
                    m_packet[i1].k_L = m_packet[i1].k;
                    m_packet[i1].w0_L = m_packet[i1].w0;
                    m_packet[i1].w0 = 0.5f*(m_packet[i1].w0_L+m_packet[i1].w0_H);
                    // distribute the error according to current speed difference
                    v = GetWaveParameters( wd, m_packet[i1].w0_L, m_packet[i1].k_L/*, m_packet[i1].k_L, speed_L*/);
                    m_packet[i1].k_L = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    speed_L = Float.intBitsToFloat(Numeric.decodeSecond(v));
                    v = GetWaveParameters( wd, m_packet[i1].w0_H, m_packet[i1].k_H/*, m_packet[i1].k_H, speed_H*/);
                    m_packet[i1].k_H = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    speed_H = Float.intBitsToFloat(Numeric.decodeSecond(v));
                    v = GetWaveParameters( wd, m_packet[i1].w0, 0.5f*(m_packet[i1].k_L+m_packet[i1].k_H)/*, m_packet[i1].k, speed_M*/);
                    m_packet[i1].k = Float.intBitsToFloat(Numeric.decodeFirst(v));
                    speed_M = Float.intBitsToFloat(Numeric.decodeSecond(v));

                    dSL = Math.abs(Math.abs(speed_L)-Math.abs(speed_M));
                    dSH = Math.abs(Math.abs(speed_H)-Math.abs(speed_M));
                    d_All = m_packet[i1].d_H;
                    m_packet[i1].d_L = dSL*d_All / (dSH + dSL);
                    m_packet[i1].d_H = d_All - m_packet[i1].d_L;
                    m_packet[i1].envelope = Math.min(PACKET_ENVELOPE_MAXSIZE, Math.max(PACKET_ENVELOPE_MINSIZE, PACKET_ENVELOPE_SIZE_FACTOR*2.0f*Numeric.PI/m_packet[i1].k));
                    // distribute the energy such that both max. wave gradients are equal -> both get the same wave shape
                    m_packet[firstfree].E = Math.abs(m_packet[i1].E)/(1.0f + (m_packet[i1].envelope*m_packet[firstfree].k*m_packet[firstfree].k*(DENSITY*GRAVITY+SIGMA*m_packet[i1].k*m_packet[i1].k))/(m_packet[firstfree].envelope*m_packet[i1].k*m_packet[i1].k*(DENSITY*GRAVITY+SIGMA*m_packet[firstfree].k*m_packet[firstfree].k)));
                    m_packet[i1].E = Math.abs(m_packet[i1].E)-m_packet[firstfree].E;
                    // smoothly ramp the new waves
                    m_packet[i1].phase=0;
                    m_packet[i1].ampOld = 0.0f;
                    m_packet[i1].dAmp = 0.5f*(m_packet[i1].speed1+m_packet[i1].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[i1].envelope)*GetWaveAmplitude( m_packet[i1].envelope* Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos2), m_packet[i1].E, m_packet[i1].k);
                    m_packet[firstfree].phase = 0.0f;
                    m_packet[firstfree].ampOld = 0.0f;
                    m_packet[firstfree].dAmp = 0.5f*(m_packet[firstfree].speed1+m_packet[firstfree].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[firstfree].envelope)*GetWaveAmplitude( m_packet[firstfree].envelope*Vector2f.distance(m_packet[firstfree].pos1,m_packet[firstfree].pos2), m_packet[firstfree].E, m_packet[firstfree].k);
                }
            }


        // crest-refinement of packets of regular packet (not at any boundary, i.e. having no 3rd vertex)
        if (Math.max(m_usedGhosts + m_usedPackets, 2 * m_usedPackets) > m_packetNum)
            ExpandWavePacketMemory(Math.max(m_usedGhosts + m_usedPackets, 2 * m_usedPackets) + PACKET_BUFFER_DELTA);
//	#pragma omp parallel for
        for (int uP = m_usedPackets-1; uP>=0; uP--)
            if (!m_packet[m_usedPacket[uP]].use3rd)
            {
                int i1 = m_usedPacket[uP];
                float sDiff = Vector2f.distance(m_packet[i1].pos2,m_packet[i1].pos1);
                float aDiff = Vector2f.dot(m_packet[i1].dir1, m_packet[i1].dir2);
                if ((sDiff > m_packet[i1].envelope) || (aDiff <= PACKET_SPLIT_ANGLE))  // if the two vertices move towards each other, do not subdivide
                {
                    int firstghost = GetFreeGhostID();
//                    m_ghostPacket[firstghost].pos = 0.5f*(m_packet[i1].pOld1+m_packet[i1].pOld2);
                    Vector2f.mix(m_packet[i1].pOld1,m_packet[i1].pOld2, 0.5f, m_ghostPacket[firstghost].pos);
//                    m_ghostPacket[firstghost].dir = (m_packet[i1].dOld1+m_packet[i1].dOld2).normalized();
                    Vector2f.add(m_packet[i1].dOld1,m_packet[i1].dOld2, m_ghostPacket[firstghost].dir);
                    m_ghostPacket[firstghost].dir.normalise();

                    m_ghostPacket[firstghost].speed = 0.5f*(m_packet[i1].sOld1+m_packet[i1].sOld2);
                    m_ghostPacket[firstghost].envelope = m_packet[i1].envelope;
                    m_ghostPacket[firstghost].ampOld = m_packet[i1].ampOld;
                    m_ghostPacket[firstghost].dAmp = m_ghostPacket[firstghost].ampOld* m_ghostPacket[firstghost].speed*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_ghostPacket[firstghost].envelope);
                    m_ghostPacket[firstghost].k = m_packet[i1].k;
                    m_ghostPacket[firstghost].phase = m_packet[i1].phOld;
                    m_ghostPacket[firstghost].dPhase = m_packet[i1].phase-m_packet[i1].phOld;
                    m_ghostPacket[firstghost].bending = GetIntersectionDistance(m_ghostPacket[firstghost].pos, m_ghostPacket[firstghost].dir, m_packet[i1].pOld1, m_packet[i1].dOld1);
                    // create new vertex between existing packet vertices
                    int firstfree = GetFreePackedID();   //
                    m_packet[firstfree] = m_packet[i1];	// first copy all data  TODO reference copy
//                    m_packet[firstfree].pOld1 = 0.5f*(m_packet[i1].pOld1 + m_packet[i1].pOld2);
                    Vector2f.mix(m_packet[i1].pOld1 ,m_packet[i1].pOld2, 0.5f, m_packet[firstfree].pOld1);
//                    m_packet[firstfree].dOld1 = (m_packet[i1].dOld1 + m_packet[i1].dOld2).normalized();
                    Vector2f.add(m_packet[i1].dOld1, m_packet[i1].dOld2, m_packet[firstfree].dOld1);
                    m_packet[firstfree].dOld1.normalise();

                    m_packet[firstfree].sOld1 = 0.5f*(m_packet[i1].sOld1 + m_packet[i1].sOld2);
//                    m_packet[firstfree].pos1 = 0.5f*(m_packet[i1].pos1 + m_packet[i1].pos2);
                    Vector2f.mix(m_packet[i1].pos1, m_packet[i1].pos2, 0.5f, m_packet[firstfree].pos1);
//                    m_packet[firstfree].dir1 = (m_packet[i1].dir1  + m_packet[i1].dir2).normalized();
                    Vector2f.add(m_packet[i1].dir1, m_packet[i1].dir2, m_packet[firstfree].dir1);
                    m_packet[firstfree].dir1.normalise();

                    m_packet[firstfree].speed1 = 0.5f*(m_packet[i1].speed1 + m_packet[i1].speed2);
                    m_packet[firstfree].E = 0.5f*m_packet[i1].E;
                    m_packet[firstfree].ampOld = 0.0f;
                    m_packet[firstfree].dAmp = 0.5f*(m_packet[firstfree].speed1+m_packet[firstfree].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[firstfree].envelope)*GetWaveAmplitude( m_packet[firstfree].envelope*Vector2f.distance(m_packet[firstfree].pos1,m_packet[firstfree].pos2), m_packet[firstfree].E, m_packet[firstfree].k);
                    // use the same new middle vertex here
                    m_packet[i1].pOld2.set(m_packet[firstfree].pOld1);
                    m_packet[i1].dOld2.set(m_packet[firstfree].dOld1);
                    m_packet[i1].sOld2 = m_packet[firstfree].sOld1;
                    m_packet[i1].pos2.set(m_packet[firstfree].pos1);
                    m_packet[i1].dir2.set(m_packet[firstfree].dir1);
                    m_packet[i1].speed2 = m_packet[firstfree].speed1;
                    m_packet[i1].E *= 0.5f;
                    m_packet[i1].ampOld = 0.0f;
                    m_packet[i1].dAmp = 0.5f*(m_packet[i1].speed1+m_packet[i1].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[i1].envelope)*GetWaveAmplitude( m_packet[i1].envelope*Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos2), m_packet[i1].E, m_packet[i1].k);
                }
            }



        // crest-refinement of packets with a sliding 3rd vertex
        if ( 3 * m_usedPackets > m_packetNum)
            ExpandWavePacketMemory(3 * m_usedPackets + PACKET_BUFFER_DELTA);
//	#pragma omp parallel for
        for (int uP = m_usedPackets-1; uP>=0; uP--)
            if ((m_packet[m_usedPacket[uP]].use3rd) && (m_packet[m_usedPacket[uP]].sliding3))
            {
                int i1 = m_usedPacket[uP];
                float sDiff1 = Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos3);
                float aDiff1 = Vector2f.dot(m_packet[i1].dir1, m_packet[i1].dir3);
                if ((sDiff1 >= m_packet[i1].envelope))// || (aDiff1 <= PACKET_SPLIT_ANGLE))  // angle criterion is removed here because this would prevent diffraction
                {
                    int firstfree = GetFreePackedID();
                    // first vertex becomes first in new wave packet, third one becomes second
                    m_packet[firstfree] = m_packet[i1];	// first copy all data
//                    m_packet[firstfree].pOld2 = 0.5f*(m_packet[i1].pOld1 + m_packet[i1].pOld3);
                    Vector2f.mix(m_packet[i1].pOld1, m_packet[i1].pOld3, 0.5f, m_packet[firstfree].pOld2);
//                    m_packet[firstfree].dOld2 = (m_packet[i1].dOld1 + m_packet[i1].dOld3).normalized();
                    Vector2f.add(m_packet[i1].dOld1, m_packet[i1].dOld3, m_packet[firstfree].dOld2);
                    m_packet[firstfree].dOld2.normalise();

                    m_packet[firstfree].sOld2 = 0.5f*(m_packet[i1].sOld1 + m_packet[i1].sOld3);
//                    m_packet[firstfree].pos2 = 0.5f*(m_packet[i1].pos1 + m_packet[i1].pos3);
                    Vector2f.mix(m_packet[i1].pos1, m_packet[i1].pos3,0.5f, m_packet[firstfree].pos2);
//                    m_packet[firstfree].dir2 = (m_packet[i1].dir1  + m_packet[i1].dir3).normalized();
                    Vector2f.add(m_packet[i1].dir1, m_packet[i1].dir3, m_packet[firstfree].dir2);
                    m_packet[firstfree].dir2.normalise();
                    m_packet[firstfree].speed2 = 0.5f*(m_packet[i1].speed1 + m_packet[i1].speed3);
                    m_packet[firstfree].ampOld = 0.0f;
                    m_packet[firstfree].dAmp = 0.5f*(m_packet[i1].speed1+m_packet[i1].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[i1].envelope)*GetWaveAmplitude( m_packet[i1].envelope*Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos2), m_packet[i1].E, m_packet[i1].k);
                    m_packet[firstfree].bounced1 = false;
                    m_packet[firstfree].bounced2 = false;
                    m_packet[firstfree].bounced3 = false;
                    m_packet[firstfree].use3rd = false;
                    m_packet[firstfree].sliding3 = false;
                    // use the same new middle vertex here
                    m_packet[i1].pOld1.set(m_packet[firstfree].pOld2);
                    m_packet[i1].dOld1.set(m_packet[firstfree].dOld2);
                    m_packet[i1].sOld1 = m_packet[firstfree].sOld2;
                    m_packet[i1].pos1.set(m_packet[firstfree].pos2);
                    m_packet[i1].dir1.set(m_packet[firstfree].dir2);
                    m_packet[i1].speed1 = m_packet[firstfree].speed2;
                    // distribute the energy according to length of the two new packets
                    float s = Vector2f.distance(m_packet[firstfree].pos1,m_packet[firstfree].pos2)/(Vector2f.distance(m_packet[firstfree].pos1,m_packet[firstfree].pos2) + Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos3) + Vector2f.distance(m_packet[i1].pos2,m_packet[i1].pos3));
                    m_packet[firstfree].E = s*m_packet[i1].E;
                    m_packet[i1].E *= (1.0f-s);
                }
                // same procedure for the other end of sliding vertex..
                sDiff1 = Vector2f.distance(m_packet[i1].pos2,m_packet[i1].pos3);
                aDiff1 = Vector2f.dot(m_packet[i1].dir2, m_packet[i1].dir3);
                if ((sDiff1 >= m_packet[i1].envelope)/* || (aDiff1 <= PACKET_SPLIT_ANGLE)*/)  // angle criterion is removed here because this would prevent diffraction
                {
                    int firstfree = GetFreePackedID();
                    // first vertex becomes first in new packet, third one becomes second
                    m_packet[firstfree] = m_packet[i1];	// first copy all data  todo
//                    m_packet[firstfree].pOld1 = 0.5f*(m_packet[i1].pOld2 + m_packet[i1].pOld3);
                    Vector2f.mix(m_packet[i1].pOld2, m_packet[i1].pOld3,0.5f, m_packet[firstfree].pOld1);
//                    m_packet[firstfree].dOld1 = (m_packet[i1].dOld2 + m_packet[i1].dOld3).normalized();
                    Vector2f.add(m_packet[i1].dOld2, m_packet[i1].dOld3, m_packet[firstfree].dOld1);
                    m_packet[firstfree].dOld1.normalise();
                    m_packet[firstfree].sOld1 = 0.5f*(m_packet[i1].sOld2 + m_packet[i1].sOld3);
//                    m_packet[firstfree].pos1 = 0.5f*(m_packet[i1].pos2 + m_packet[i1].pos3);
                    Vector2f.mix(m_packet[i1].pos2, m_packet[i1].pos3, 0.5f, m_packet[firstfree].pos1);
//                    m_packet[firstfree].dir1 = (m_packet[i1].dir2  + m_packet[i1].dir3).normalized();
                    Vector2f.add(m_packet[i1].dir2, m_packet[i1].dir3, m_packet[firstfree].dir1);
                    m_packet[firstfree].dir1.normalise();
                    m_packet[firstfree].speed1 = 0.5f*(m_packet[i1].speed2 + m_packet[i1].speed3);
                    m_packet[firstfree].ampOld = 0.0f;
                    m_packet[firstfree].dAmp = 0.5f*(m_packet[firstfree].speed1+m_packet[firstfree].speed2)*m_elapsedTime/(PACKET_BLEND_TRAVEL_FACTOR*m_packet[firstfree].envelope)*GetWaveAmplitude( m_packet[firstfree].envelope*Vector2f.distance(m_packet[firstfree].pos1,m_packet[firstfree].pos2), m_packet[firstfree].E, m_packet[firstfree].k);
                    m_packet[firstfree].bounced1 = false;
                    m_packet[firstfree].bounced2 = false;
                    m_packet[firstfree].bounced3 = false;
                    m_packet[firstfree].use3rd = false;
                    m_packet[firstfree].sliding3 = false;
                    // use the same new middle vertex
                    m_packet[i1].pOld2.set(m_packet[firstfree].pOld1);
                    m_packet[i1].dOld2.set(m_packet[firstfree].dOld1);
                    m_packet[i1].sOld2 = m_packet[firstfree].sOld1;
                    m_packet[i1].pos2.set(m_packet[firstfree].pos1);
                    m_packet[i1].dir2.set(m_packet[firstfree].dir1);
                    m_packet[i1].speed2 = m_packet[firstfree].speed1;
                    // distribute the energy according to length of the two new packets
                    float s = Vector2f.distance(m_packet[firstfree].pos1,m_packet[firstfree].pos2)/(Vector2f.distance(m_packet[firstfree].pos1,m_packet[firstfree].pos2) + Vector2f.distance(m_packet[i1].pos1,m_packet[i1].pos3) + Vector2f.distance(m_packet[i1].pos2,m_packet[i1].pos3));
                    m_packet[firstfree].E = s*m_packet[i1].E;
                    m_packet[i1].E *= (1.0f-s);
                }
            }


        // delete packets traveling outside the scene
//	#pragma omp parallel for
        for (int uP = 0; uP < m_usedPackets; uP++)
        {
            int i1 = m_usedPacket[uP];
            m_packet[i1].toDelete = false;
            if (!m_packet[i1].use3rd)
            {
//                Vector2f dir = m_packet[i1].pos1 - m_packet[i1].pOld1;
//                Vector2f dir2 = m_packet[i1].pos2 - m_packet[i1].pOld2;
                Vector2f dir = Vector2f.sub(m_packet[i1].pos1, m_packet[i1].pOld1, null);
                Vector2f dir2 = Vector2f.sub(m_packet[i1].pos2, m_packet[i1].pOld2, null);
                if ((((m_packet[i1].pos1.x < -0.5f*SCENE_EXTENT) && (dir.x < 0.0))
                        || ((m_packet[i1].pos1.x > +0.5f*SCENE_EXTENT) && (dir.x > 0.0))
                        || ((m_packet[i1].pos1.y < -0.5f*SCENE_EXTENT) && (dir.y < 0.0))
                        || ((m_packet[i1].pos1.y > +0.5f*SCENE_EXTENT) && (dir.y > 0.0)))
                        &&
                        (((m_packet[i1].pos2.x < -0.5f*SCENE_EXTENT) && (dir2.x < 0.0))
                                || ((m_packet[i1].pos2.x > +0.5f*SCENE_EXTENT) && (dir2.x > 0.0))
                                || ((m_packet[i1].pos2.y < -0.5f*SCENE_EXTENT) && (dir2.y < 0.0))
                                || ((m_packet[i1].pos2.y > +0.5f*SCENE_EXTENT) && (dir2.y > 0.0))))
                    m_packet[i1].toDelete = true;
            }
        }

        // damping, insignificant packet removal (if too low amplitude), reduce energy of steep waves with too high gradient
        m_softDampFactor = (float) (1.0f + 100.0f*Math.pow(Math.max(0.0f, (float)(m_usedPackets)/(float)(m_packetBudget) - 1.0f), 2.0f));
//	#pragma omp parallel for
        for (int uP = 0; uP < m_usedPackets; uP++)
            if ((!m_packet[m_usedPacket[uP]].use3rd) && (!m_packet[m_usedPacket[uP]].toDelete))
            {
                int i1 = m_usedPacket[uP];
                float dampViscosity = -2.0f*m_packet[i1].k*m_packet[i1].k*KINEMATIC_VISCOSITY;
                float dampJunkfilm = (float) (-0.5f*m_packet[i1].k*Math.sqrt(0.5f*KINEMATIC_VISCOSITY*m_packet[i1].w0));
                m_packet[i1].E *= (float)Math.exp((dampViscosity + dampJunkfilm)*m_elapsedTime*m_softDampFactor);   // wavelength-dependent damping
                // amplitude computation: lower if too steep, delete if too low
                float area = m_packet[i1].envelope*Vector2f.distance(m_packet[i1].pos2, m_packet[i1].pos1);
                float a1 = GetWaveAmplitude( area, m_packet[i1].E, m_packet[i1].k);
                if (a1*m_packet[i1].k < PACKET_KILL_AMPLITUDE_DERIV)
                    m_packet[i1].toDelete = true;
                else
                {
                    // get the biggest wave as reference for energy reduction (conservative but important to not remove too much energy in case of large k intervals)
                    float a_L = GetWaveAmplitude(area, m_packet[i1].E, m_packet[i1].k_L);
                    float a_H = GetWaveAmplitude(area, m_packet[i1].E, m_packet[i1].k_H);
                    float k;
                    if (a_L*m_packet[i1].k_L < a_H*m_packet[i1].k_H)   // take the smallest wave steepness (=amplitude derivative)
                    {
                        a1 = a_L;
                        k = m_packet[i1].k_L;
                    }
                    else
                    {
                        a1 = a_H;
                        k = m_packet[i1].k_H;
                    }
                    if (a1 > MAX_SPEEDNESS*2.0f*Numeric.PI / k)
                    {
                        a1 = MAX_SPEEDNESS*2.0f*Numeric.PI / k;
                        m_packet[i1].E = a1*a1*(area*0.5f*(DENSITY*GRAVITY + SIGMA*k*k));
                    }
                }
                m_packet[i1].ampOld = Math.min(a1, m_packet[i1].ampOld + m_packet[i1].dAmp); // smoothly increase amplitude from last timestep
                // update variables needed for packet display
//                Vector2f posMidNew = 0.5f*(m_packet[i1].pos1 + m_packet[i1].pos2);
//                Vector2f posMidOld = 0.5f*(m_packet[i1].pOld1 + m_packet[i1].pOld2);
                Vector2f posMidNew = Vector2f.mix(m_packet[i1].pos1, m_packet[i1].pos2, 0.5f, null);
                Vector2f posMidOld = Vector2f.mix(m_packet[i1].pOld1, m_packet[i1].pOld2, 0.5f, null);
//                        Vector2f dirN = (posMidNew - posMidOld).normalized();				// vector in traveling direction
                m_packet[i1].midPos.set(posMidNew);
//                m_packet[i1].travelDir = dirN;
                Vector2f.sub(posMidNew, posMidOld, m_packet[i1].travelDir);
                m_packet[i1].travelDir.normalise();
                m_packet[i1].bending = GetIntersectionDistance(posMidNew, m_packet[i1].travelDir, m_packet[i1].pos1, m_packet[i1].dir1);
            }
        // delete all packets identified to be deleted (important: NO parallelization here!)
        for (int uP = 0; uP < m_usedPackets; uP++)
            if (m_packet[m_usedPacket[uP]].toDelete)
                DeletePacket(uP);

        // advect ghost packets
//	#pragma omp parallel for
        for (int uG = 0; uG < m_usedGhosts; uG++)
        {
            int i1 = m_usedGhost[uG];
//            m_ghostPacket[i1].pos += m_elapsedTime*m_ghostPacket[i1].speed*m_ghostPacket[i1].dir;
            Vector2f.linear(m_ghostPacket[i1].pos, m_ghostPacket[i1].dir, m_elapsedTime*m_ghostPacket[i1].speed, m_ghostPacket[i1].pos);
            m_ghostPacket[i1].phase += m_ghostPacket[i1].dPhase;
            m_ghostPacket[i1].ampOld = Math.max(0.0f, m_ghostPacket[i1].ampOld - m_softDampFactor*m_ghostPacket[i1].dAmp);  // decrease amplitude to let this wave disappear (take budget-based softdamping into account)
        }
        // delete all ghost packets if they traveled long enough (important: NO parallelization here!)
        for (int uG = 0; uG < m_usedGhosts; uG++)
            if (m_ghostPacket[m_usedGhost[uG]].ampOld <= 0.0)
                DeleteGhost(uG);
    }

    private static float rational_tanh(float x)
    {
        if (x < -3.0f)
            return -1.0f;
        else if (x > 3.0f)
            return 1.0f;
        else
            return x*(27.0f + x*x) / (27.0f+9.0f*x*x);
    }
}
