package jet.opengl.demos.nvidia.waves.ocean;

import com.nvidia.developer.opengl.models.sdkmesh.D3D9Enums;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshMesh;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshSubset;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshVertexBufferHeader;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;
import com.nvidia.developer.opengl.models.sdkmesh.VertexElement9;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

final class OceanHullSensors implements D3D9Enums, OceanConst{
    static final int MaxNumSensors = 20000,
            MaxNumRimSensors = 2000;

    private int				m_NumSensors;
    private final Vector3f[] m_SensorPositions = CommonUtil.initArray(new Vector3f[MaxNumSensors]);
    private final Vector3f[] m_SensorNormals = CommonUtil.initArray(new Vector3f[MaxNumSensors]);
    private float			m_MeanSensorRadius;

    private int				m_NumRimSensors;
    private final Vector3f[]		m_RimSensorPositions = CommonUtil.initArray(new Vector3f[MaxNumRimSensors]);
    private final Vector3f[]		m_RimSensorNormals = CommonUtil.initArray(new Vector3f[MaxNumRimSensors]);
    private float			m_MeanRimSensorSeparation;

    private final Vector3f		m_sensorBoundsMinCorner = new Vector3f();
    private final Vector3f		m_sensorBoundsMaxCorner = new Vector3f();

    // Sensor data
    private final Vector2f[]	m_ReadbackCoords = CommonUtil.initArray(new Vector2f[MaxNumSensors]);
    private final Vector4f[] 	m_Displacements = CommonUtil.initArray(new Vector4f[MaxNumSensors]);
    private final Vector3f[]		m_WorldSensorPositions = CommonUtil.initArray(new Vector3f[MaxNumSensors]);
    private final Vector3f[]		m_WorldSensorNormals = CommonUtil.initArray(new Vector3f[MaxNumSensors]);

    private final Vector2f[]	m_RimReadbackCoords = CommonUtil.initArray(new Vector2f[MaxNumRimSensors]);
    private final Vector4f[] 	m_RimDisplacements = CommonUtil.initArray(new Vector4f[MaxNumRimSensors]);
    private final Vector3f[]		m_WorldRimSensorPositions = CommonUtil.initArray(new Vector3f[MaxNumRimSensors]);
    private final Vector3f[]		m_WorldRimSensorNormals = CommonUtil.initArray(new Vector3f[MaxNumRimSensors]);

    // Diagnostic visualisations
//    ID3D11Device* m_pd3dDevice;
    private BufferGL m_pVisualisationMeshIB;
    private int			  m_VisualisationMeshIndexCount;
    private int	  m_VisualisationMeshIndexFormat;

    private interface FilterCallback{
        boolean invoke(Vector3f v0,Vector3f v1,Vector3f v2,Vector3f n0,Vector3f n1,Vector3f n2);
    }

    private interface Functor{
        void invoke(int i0, int i1, int i2, Vector3f v0,Vector3f v1,Vector3f v2, Vector3f n0, Vector3f n1,Vector3f n2);
    }

    private static void for_each_triangle(Functor functor, FilterCallback filter, BoatMesh pMesh, Matrix4f matMeshToLocal)
    {
		final int num_meshes = pMesh.getNumMeshes();

        for(int mesh_ix = 0; mesh_ix != num_meshes; ++mesh_ix)
        {
            SDKMeshMesh pSubMesh = pMesh.getMesh(mesh_ix);

            // Find the positions stream
            int pVertexPositionBytes = -1;
            int pVertexNormalBytes = -1;
            int positions_stride = -1;
            int normals_stride = -1;
            for(int vbix = 0; vbix != pSubMesh.numVertexBuffers && (-1 == pVertexPositionBytes || -1 == pVertexNormalBytes); ++vbix) {
				SDKMeshVertexBufferHeader pVBHeader = pMesh.getVBHeader(pSubMesh.vertexBuffers[vbix]);
                for(int declix = 0; declix < pVBHeader.decl.length && (-1 == pVertexPositionBytes || -1 == pVertexNormalBytes); ++declix) {
                    VertexElement9 decl_elem = pVBHeader.decl[declix];
                    if(decl_elem.stream >= 0 && decl_elem.stream < SDKmesh.MAX_D3D10_VERTEX_STREAMS) {

                        if (D3DDECLUSAGE_POSITION == decl_elem.usage && 0 == decl_elem.usageIndex) {
                            if (D3DDECLTYPE_FLOAT3 == decl_elem.type || D3DDECLTYPE_FLOAT4 == decl_elem.type) {
                                int pVertexBytes = pMesh.getRawVerticesAt(pSubMesh.vertexBuffers[vbix]);
                                pVertexPositionBytes = pVertexBytes + decl_elem.offset;
                                positions_stride = (int) pVBHeader.strideBytes;
                            }
                        } else if (D3DDECLUSAGE_NORMAL == decl_elem.usage && 0 == decl_elem.usageIndex) {
                            if (D3DDECLTYPE_FLOAT3 == decl_elem.type || D3DDECLTYPE_FLOAT4 == decl_elem.type) {
                                int pVertexBytes = pMesh.getRawVerticesAt(pSubMesh .vertexBuffers[vbix]);
                                pVertexNormalBytes = pVertexBytes + decl_elem.offset;
                                normals_stride = (int) pVBHeader.strideBytes;
                            }
                        }
                    }
                }
            }

            if(pVertexPositionBytes != -1 && pVertexNormalBytes!=-1) {
                Vector3f v0 = CacheBuffer.getCachedVec3();
                Vector3f v1 = CacheBuffer.getCachedVec3();
                Vector3f v2 = CacheBuffer.getCachedVec3();

                Vector3f n0 = CacheBuffer.getCachedVec3();
                Vector3f n1 = CacheBuffer.getCachedVec3();
                Vector3f n2 = CacheBuffer.getCachedVec3();

                final byte[] meshData = pMesh.getRawData();
				final int index_size_in_bytes = (SDKmesh.IT_16BIT == pMesh.getIndexType(pSubMesh.indexBuffer)) ? 2 : 4;
                int pIndexBytes = pMesh.getRawIndicesAt(pSubMesh.indexBuffer);
                for (int subset = 0; subset < pMesh.getNumSubsets(mesh_ix); ++subset)
                {
                    SDKMeshSubset pSubset = pMesh.getSubset(mesh_ix, subset);
                    int pCurrIndexByte = pIndexBytes + index_size_in_bytes * (int)pSubset.indexStart;
                    int pEndIndexByte = pCurrIndexByte + index_size_in_bytes * (int)pSubset.indexCount;
                    switch(pSubset.primitiveType) {
                        case SDKmesh.PT_TRIANGLE_LIST:

                            while((pCurrIndexByte+2*index_size_in_bytes) < pEndIndexByte) {
                                int i0, i1, i2;
                                if(index_size_in_bytes == 2) {
                                    /*WORD* pCurrIndex = (WORD*)pCurrIndexByte;
                                    i0 = (DWORD)pSubset->VertexStart + pCurrIndex[0];
                                    i1 = (DWORD)pSubset->VertexStart + pCurrIndex[1];
                                    i2 = (DWORD)pSubset->VertexStart + pCurrIndex[2];*/

                                    i0 = Numeric.unsignedShort(Numeric.getShort(meshData, pCurrIndexByte));
                                    i1 = Numeric.unsignedShort(Numeric.getShort(meshData, pCurrIndexByte+2));
                                    i2 = Numeric.unsignedShort(Numeric.getShort(meshData, pCurrIndexByte+4));
                                } else {
                                    /*DWORD* pCurrIndex = (DWORD*)pCurrIndexByte;
                                    i0 = (DWORD)pSubset->VertexStart + pCurrIndex[0];
                                    i1 = (DWORD)pSubset->VertexStart + pCurrIndex[1];
                                    i2 = (DWORD)pSubset->VertexStart + pCurrIndex[2];*/

                                    i0 = Numeric.getInt(meshData, pCurrIndexByte);
                                    i1 = Numeric.getInt(meshData, pCurrIndexByte+4);
                                    i2 = Numeric.getInt(meshData, pCurrIndexByte+8);
                                }

                                assert (i0>=0&&i1>=0&&i2>=0);

                                /*D3DXVec3TransformCoord(&v0, (D3DXVECTOR3*)(pVertexPositionBytes + positions_stride * i0), &matMeshToLocal);
                                D3DXVec3TransformCoord(&v1, (D3DXVECTOR3*)(pVertexPositionBytes + positions_stride * i1), &matMeshToLocal);
                                D3DXVec3TransformCoord(&v2, (D3DXVECTOR3*)(pVertexPositionBytes + positions_stride * i2), &matMeshToLocal);*/

                                Numeric.getVector3f(meshData, pVertexPositionBytes + positions_stride * i0, v0);
                                Numeric.getVector3f(meshData, pVertexPositionBytes + positions_stride * i1, v1);
                                Numeric.getVector3f(meshData, pVertexPositionBytes + positions_stride * i2, v2);

                                Matrix4f.transformCoord(matMeshToLocal, v0, v0);
                                Matrix4f.transformCoord(matMeshToLocal, v1, v1);
                                Matrix4f.transformCoord(matMeshToLocal, v2, v2);


                                /*D3DXVECTOR3 n0,n1,n2;
                                D3DXVec3TransformNormal(&n0, (D3DXVECTOR3*)(pVertexNormalBytes + normals_stride * i0), &matMeshToLocal);
                                D3DXVec3TransformNormal(&n1, (D3DXVECTOR3*)(pVertexNormalBytes + normals_stride * i1), &matMeshToLocal);
                                D3DXVec3TransformNormal(&n2, (D3DXVECTOR3*)(pVertexNormalBytes + normals_stride * i2), &matMeshToLocal);*/

                                Numeric.getVector3f(meshData, pVertexNormalBytes + normals_stride * i0, n0);
                                Numeric.getVector3f(meshData, pVertexNormalBytes + normals_stride * i1, n1);
                                Numeric.getVector3f(meshData, pVertexNormalBytes + normals_stride * i2, n2);

                                Matrix4f.transformNormal(matMeshToLocal, n0, n0);
                                Matrix4f.transformNormal(matMeshToLocal, n1, n1);
                                Matrix4f.transformNormal(matMeshToLocal, n2, n2);

                                if(filter.invoke(v0,v1,v2,n0,n1,n2)) {
                                    functor.invoke(i0,i1,i2,v0,v1,v2,n0,n1,n2);
                                }

                                pCurrIndexByte += 3 * index_size_in_bytes;
                            }
                            break;
                        case SDKmesh.PT_TRIANGLE_STRIP:
                            assert(false); // tristrips TBD
                            break;
                    }
                }

                CacheBuffer.free(v0);
                CacheBuffer.free(v1);
                CacheBuffer.free(v2);

                CacheBuffer.free(n0);
                CacheBuffer.free(n1);
                CacheBuffer.free(n2);
            }
        }
    }

    private static int PoissonOutcomeKnuth(float lambda) {

        // Adapted to log space to avoid exp() overflow for large lambda
        int k = 0;
        float p = 0.f;
        do {
            k = k+1;
            p = (float) (p - Math.log(Numeric.random()));
        }
        while (p < lambda);

        return k-1;
    }

    private static int PoissonOutcome(float lambda) {
        return PoissonOutcomeKnuth(lambda);
    }

    private static final float kRimVerticalOffset = 0.6f;

    private static float adhoc_deck_profile(float z)
    {
        if(z < 0.f)  {
            // Straight-line for rear half
            return 1.5f;
        } else {
            // Elliptical upward sweep for front half
			final float major_r = 40.f;
            final float minor_r = 10.f;
            return (float) (2.5f + minor_r - Math.sqrt(major_r*major_r-z*z) * minor_r/major_r);
        }
    }

    private static float adhoc_deck_profile_for_rim(float z)
    {
        if(z < 0.f)  {
            // Straight-line for rear half
            return 1.5f;
        } else {
            // Elliptical upward sweep for front half
            final float major_r = 40.f;
            final float minor_r = 10.f;
            return (float) (1.5f + minor_r - Math.sqrt(major_r*major_r-z*z) * minor_r/major_r);
        }
    }

    private static void adhoc_get_deck_intersection(ReadableVector3f v0,ReadableVector3f v1, Vector3f out)
    {
        float h0 = v0.getY() - adhoc_deck_profile_for_rim(v0.getZ());
        float h1 = v1.getY() - adhoc_deck_profile_for_rim(v1.getZ());

        float lambda = (0.f-h0)/(h1-h0);

//        return lambda * v1 + (1.f - lambda) * v0;
        Vector3f.mix(v0, v1, lambda, out);
    }

    private static void adhoc_get_deck_intersection(Vector3f v0,Vector3f v1, Vector3f n0,Vector3f n1, Vector3f o, Vector3f on)
    {
        float h0 = v0.y - adhoc_deck_profile_for_rim(v0.z);
        float h1 = v1.y - adhoc_deck_profile_for_rim(v1.z);

        float lambda = (0.f-h0)/(h1-h0);

//        o = lambda * v1 + (1.f - lambda) * v0;
//        on = lambda * n1 + (1.f - lambda) * n0;
        Vector3f.mix(v0, v1, lambda, o);
        Vector3f.mix(n0, n1, lambda, on);
    }

    private static boolean adhoc_get_deck_line(Vector3f v0,Vector3f v1,Vector3f v2,Vector3f o0, Vector3f o1)
    {
        int above_flags = 0;
        if(v0.y > adhoc_deck_profile_for_rim(v0.z))
            above_flags += 1;
        if(v1.y > adhoc_deck_profile_for_rim(v1.z))
            above_flags += 2;
        if(v2.y > adhoc_deck_profile_for_rim(v2.z))
            above_flags += 4;

        if(above_flags == 0 || above_flags ==7)
            return false;

        switch(above_flags) {
            case 1:
            case 6:
                 adhoc_get_deck_intersection(v0,v1,o0);
                 adhoc_get_deck_intersection(v0,v2, o1);
                break;
            case 2:
            case 5:
                 adhoc_get_deck_intersection(v1,v2, o0);
                 adhoc_get_deck_intersection(v1,v0,o1);
                break;
            case 3:
            case 4:
                 adhoc_get_deck_intersection(v2,v0,o0);
                 adhoc_get_deck_intersection(v2,v1,o1);
                break;
        }

        return true;
    }

    private static boolean adhoc_get_deck_line(Vector3f v0,Vector3f v1,Vector3f v2,Vector3f n0,Vector3f n1,Vector3f n2,
                                               Vector3f o0, Vector3f o1,Vector3f on0, Vector3f on1)
    {
        int above_flags = 0;
        if(v0.y > adhoc_deck_profile_for_rim(v0.z))
            above_flags += 1;
        if(v1.y > adhoc_deck_profile_for_rim(v1.z))
            above_flags += 2;
        if(v2.y > adhoc_deck_profile_for_rim(v2.z))
            above_flags += 4;

        if(above_flags == 0 || above_flags ==7)
            return false;

        switch(above_flags) {
            case 1:
            case 6:
                adhoc_get_deck_intersection(v0,v1,n0,n1,o0,on0);
                adhoc_get_deck_intersection(v0,v2,n0,n2,o1,on1);
                break;
            case 2:
            case 5:
                adhoc_get_deck_intersection(v1,v2,n1,n2,o0,on0);
                adhoc_get_deck_intersection(v1,v0,n1,n0,o1,on1);
                break;
            case 3:
            case 4:
                adhoc_get_deck_intersection(v2,v0,n2,n0,o0,on0);
                adhoc_get_deck_intersection(v2,v1,n2,n1,o1,on1);
                break;
        }

        return true;
    }

    private static final float calc_area(Vector3f v0,Vector3f v1,Vector3f v2) {
//                D3DXVECTOR3 edge01 = v1-v0;
//                D3DXVECTOR3 edge02 = v2-v0;
//                D3DXVECTOR3 cp;
//                D3DXVec3Cross(&cp,&edge01,&edge02);

        Vector3f cp = CacheBuffer.getCachedVec3();
        try{
            Vector3f.computeNormal(v0, v1,v2, cp);
            return 0.5f * /*D3DXVec3Length(&cp)*/Vector3f.length(cp);
        }finally {
            CacheBuffer.free(cp);
        }
    }

    private static final boolean test_vertex_pos(Vector3f v) {

        // Reject verts that are part of the above-deck
        if(v.y > adhoc_deck_profile(v.z))
            return false;

        // Strip our sundry internal fittings
        if(Math.abs(v.x) < 3.f && v.y > 0.f && v.z < 25.f && v.z > -30.f)
            return false;
        if(Math.abs(v.x) < 4.f && v.y > 0.f && v.z < 15.f && v.z > -25.f)
            return false;

        return true;
    }

    void init(BoatMesh pMesh, Matrix4f matMeshToLocal){
        // We use an ad-hoc filter to try and pick outer-hull-skin only. This is a stop-gap until we have
        // a dedicated hull mesh for this
        class ad_hoc_filter implements FilterCallback {
            public boolean invoke(Vector3f v0,Vector3f v1,Vector3f v2,Vector3f n0,Vector3f n1,Vector3f n2) {

//                D3DXVECTOR3 n0n,n1n,n2n;
//                D3DXVec3Normalize(&n0n,&n0);
//                D3DXVec3Normalize(&n1n,&n1);
//                D3DXVec3Normalize(&n2n,&n2);

                Vector3f n0n = CacheBuffer.getCachedVec3();
                Vector3f n1n = CacheBuffer.getCachedVec3();
                Vector3f n2n = CacheBuffer.getCachedVec3();

                try{
                    n0.normalise(n0n);
                    n1.normalise(n1n);
                    n2.normalise(n2n);

                    // Reject if all of the verts are out-range
                    if(!test_vertex(v0,n0n) && !test_vertex(v1,n1n) && !test_vertex(v2,n2n))
                        return false;

                    // Reject thin and outward-facing features
                    float min_z = Math.min(Math.min(v0.z,v1.z),v2.z);
                    float max_z = Math.max(Math.max(v0.z,v1.z),v2.z);
                    float mean_n = (n0n.x+n1n.x+n2n.x)/3.f;
                    if(Math.abs(mean_n) > 0.9f && (max_z-min_z) < 0.235f)
                        return false;

                    return true;
                }finally {
                    CacheBuffer.free(n0n);
                    CacheBuffer.free(n1n);
                    CacheBuffer.free(n2n);
                }
            }

            boolean test_vertex(Vector3f v, Vector3f n) {

                // Reject triangles that do not point down
                if(n.y > -0.1f)
                    return false;

                Vector3f v_centreline = CacheBuffer.getCachedVec3();

                // Anything that does not face outwards we also kill
                /*if(v.y > 0.f)*/ {
                    // Project vertex onto centreline
//                    D3DXVECTOR3 v_centreline = v;
                }

                try{
                    v_centreline.set(v);
                    v_centreline.x = 0;
                    if(v_centreline.y < 0.f)
                        v_centreline.y = 0;

                    // Clamp ends
                    if(v_centreline.z > 30.f)
                        v_centreline.z = 30.f;
                    else if(v_centreline.z < -30.f)
                        v_centreline.z = -30.f;

//                    D3DXVECTOR3 vn = v - v_centreline;
//                    D3DXVec3Normalize(&vn,&vn);
                    Vector3f.sub(v,v_centreline, v_centreline);
                    v_centreline.normalise();
                    if(Vector3f.dot(v_centreline,n) < 0.5f)
                        return false;
                    return test_vertex_pos(v);
                }finally {
                    CacheBuffer.free(v_centreline);
                }
            }
        }

        class preprocess_functor implements Functor{
            float area;
            float rim_length;
            StackInt spray_mesh_indices = new StackInt();
//            preprocess_functor() : area(0.f), rim_length(0.f) {}

            public void invoke(int i0, int i1, int i2, Vector3f v0,Vector3f v1,Vector3f v2, Vector3f n0, Vector3f n1,Vector3f n2) {
                area += calc_area(v0,v1,v2);
                spray_mesh_indices.push(i0);
                spray_mesh_indices.push(i1);
                spray_mesh_indices.push(i2);

                // Test for a deck intersection
                Vector3f d0 = CacheBuffer.getCachedVec3();
                Vector3f d1 = CacheBuffer.getCachedVec3();
                if(adhoc_get_deck_line(v0,v1,v2,d0,d1)) {
//                    D3DXVECTOR3 e01 = d1 - d0;
                    rim_length += /*D3DXVec3Length(&e01)*/Vector3f.distance(d1, d0);
                }

                CacheBuffer.free(d0);
                CacheBuffer.free(d1);
            }

        } /*preprocess;*/

        preprocess_functor preprocess = new preprocess_functor();
        ad_hoc_filter filter = new ad_hoc_filter();

        for_each_triangle(preprocess, filter, pMesh, matMeshToLocal);

	    final float length_multiplier = 1.02f;	// We use a Poission distribution to choose sensor locations,

        // so we add a little slop in the length calc to allow for
        // the likelihood that the Poisson process will generate more
        // sensors than expected ('expect' used here in the strict
        // probability theory sense)
        final float area = preprocess.area * length_multiplier * length_multiplier;
        final float mean_density = MaxNumSensors/area;
        final float mean_area_per_gen = area/MaxNumSensors;
        m_MeanSensorRadius = (float)Math.sqrt(mean_area_per_gen/ Math.PI);

        final float rim_len = preprocess.rim_length * length_multiplier;
        final float mean_rim_density = MaxNumRimSensors/rim_len;
        m_MeanRimSensorSeparation = rim_len/MaxNumRimSensors;

        // Set up spray mesh
        m_VisualisationMeshIndexCount = preprocess.spray_mesh_indices.size();
        m_VisualisationMeshIndexFormat = DXGI_FORMAT_R32_UINT;

        /*D3D11_BUFFER_DESC ib_buffer_desc;
        ib_buffer_desc.ByteWidth = sizeof(DWORD) * m_VisualisationMeshIndexCount;
        ib_buffer_desc.Usage = D3D11_USAGE_IMMUTABLE;
        ib_buffer_desc.BindFlags = D3D11_BIND_INDEX_BUFFER;
        ib_buffer_desc.CPUAccessFlags = 0;
        ib_buffer_desc.MiscFlags = 0;
        ib_buffer_desc.StructureByteStride = 0;

        D3D11_SUBRESOURCE_DATA ib_srd;
        ib_srd.pSysMem = &preprocess.spray_mesh_indices[0];
        ib_srd.SysMemPitch = ib_buffer_desc.ByteWidth;
        ib_srd.SysMemSlicePitch = ib_buffer_desc.ByteWidth;

        V_RETURN(m_pd3dDevice->CreateBuffer(&ib_buffer_desc, &ib_srd, &m_pVisualisationMeshIB));*/

        m_pVisualisationMeshIB = new BufferGL();
        m_pVisualisationMeshIB.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, 4 * m_VisualisationMeshIndexCount, CacheBuffer.wrap(preprocess.spray_mesh_indices.getData(), 0, m_VisualisationMeshIndexCount), GLenum.GL_STATIC_DRAW);

        class init_sensors_functor implements Functor{
            float mean_density;
            float mean_rim_density;
            Vector3f[] pPositions;
            Vector3f[] pNormals;
            Vector3f[] pRimPositions;
            Vector3f[] pRimNormals;
            Vector3f bounds_min = new Vector3f();
            Vector3f bounds_max = new Vector3f();
            int max_num_sensors;
            int num_sensors;
            int max_num_rim_sensors;
            int num_rim_sensors;
            init_sensors_functor(Vector3f[] pPositionsArg, Vector3f[] pNormalsArg, Vector3f[] pRimPositionsArg, Vector3f[] pRimNormalsArg, float mean_density_arg, float mean_rim_density_arg, int max_num_sensors_arg, int max_num_rim_sensors_arg)
            {
                mean_density = mean_density_arg;
                mean_rim_density=(mean_rim_density_arg);
                pPositions=(pPositionsArg);
                pNormals=(pNormalsArg);
                pRimPositions=(pRimPositionsArg);
                pRimNormals=(pRimNormalsArg);
                num_sensors=(0);
                max_num_sensors=(max_num_sensors_arg);
                num_rim_sensors=(0);
                max_num_rim_sensors=(max_num_rim_sensors_arg);
            }

            public void invoke(int i0, int i1, int i2, Vector3f v0,Vector3f v1,Vector3f v2, Vector3f n0, Vector3f n1,Vector3f n2) {

                // Add area sensors
                {
                    final float area = calc_area(v0,v1,v2);
                    final float mean_num = mean_density * area;
                    int actual_num = PoissonOutcome(mean_num);
                    if(num_sensors + actual_num > max_num_sensors) {
                        actual_num = max_num_sensors - num_sensors;
                    }

                    Vector3f pos = new Vector3f();
                    Vector3f nrm = new Vector3f();
                    for(int i = 0; i != actual_num; ++i) {
                        // Pick random points in triangle - note that for an unbiased pick, we first pick a point in the
                        // entire parallelogram, then transform the outer half back onto the inner half
                        float u = /*(float)rand()/(float)(RAND_MAX)*/Numeric.random();
                        float v = /*(float)rand()/(float)(RAND_MAX)*/Numeric.random();
                        if((u+v)>1.f) {
                            float new_u = 1.f-v;
                            float new_v = 1.f-u;
                            u = new_u;
                            v = new_v;
                        }

                        pos.x = (1.f-u-v)*v0.x + u*v1.x + v*v2.x;
                        pos.y = (1.f-u-v)*v0.y + u*v1.y + v*v2.y;
                        pos.z = (1.f-u-v)*v0.z + u*v1.z + v*v2.z;

                        nrm.x = (1.f-u-v)*n0.x + u*n1.x + v*n2.x;
                        nrm.y = (1.f-u-v)*n0.y + u*n1.y + v*n2.y;
                        nrm.z = (1.f-u-v)*n0.z + u*n1.z + v*n2.z;

                        if(test_vertex_pos(pos)) {
                            pPositions[num_sensors].set(pos);
//                            D3DXVec3Normalize(&pNormals[num_sensors],&nrm);
                            nrm.normalise(pNormals[num_sensors]);

                            if(num_sensors+num_rim_sensors > 0) {
                                bounds_min.x = Math.min(pos.x,bounds_min.x);
                                bounds_min.y = Math.min(pos.y,bounds_min.y);
                                bounds_min.z = Math.min(pos.z,bounds_min.z);
                                bounds_max.x = Math.max(pos.x,bounds_max.x);
                                bounds_max.y = Math.max(pos.y,bounds_max.y);
                                bounds_max.z = Math.max(pos.z,bounds_max.z);
                            } else {
                                bounds_min.set(pos);
                                bounds_max.set(pos);
                            }
                            ++num_sensors;
                        }
                    }
                }

                // Add rim sensors
                Vector3f d0 = new Vector3f(),d1 = new Vector3f(),dn0 = new Vector3f(),dn1 = new Vector3f();
                if(adhoc_get_deck_line(v0,v1,v2,n0,n1,n2,d0,d1,dn0,dn1)) {
//                    D3DXVECTOR3 e01 = d1 - d0;
                    float edge_length = /*D3DXVec3Length(&e01)*/Vector3f.distance(d1, d0);
				    final float mean_num = mean_rim_density * edge_length;
                    int actual_num = PoissonOutcome(mean_num);
                    if(num_rim_sensors + actual_num > max_num_rim_sensors) {
                        actual_num = max_num_rim_sensors - num_rim_sensors;
                    }

                    Vector3f pos = new Vector3f();
                    Vector3f nrm = new Vector3f();
                    for(int i = 0; i != actual_num; ++i) {

                        // Pick random points on line
                        float u = /*(float)rand()/(float)(RAND_MAX)*/Numeric.random();
//                        D3DXVECTOR3 pos = u * d0 + (1.f-u) * d1;
                        Vector3f.mix(d1, d0, u, pos);
                        pos.y += kRimVerticalOffset;
//                        D3DXVECTOR3 nrm = u * dn0 + (1.f-u) * dn1;
                        Vector3f.mix(dn1, dn0, u, nrm);
                        pRimPositions[num_rim_sensors].set(pos);
//                        D3DXVec3Normalize(&pRimNormals[num_rim_sensors],&nrm);
                        nrm.normalise(pRimNormals[num_rim_sensors]);
                        if(num_sensors+num_rim_sensors > 0) {
                            bounds_min.x = Math.min(pos.x,bounds_min.x);
                            bounds_min.y = Math.min(pos.y,bounds_min.y);
                            bounds_min.z = Math.min(pos.z,bounds_min.z);
                            bounds_max.x = Math.max(pos.x,bounds_max.x);
                            bounds_max.y = Math.max(pos.y,bounds_max.y);
                            bounds_max.z = Math.max(pos.z,bounds_max.z);
                        } else {
                            bounds_min = pos;
                            bounds_max = pos;
                        }
                        ++num_rim_sensors;
                    }
                }
            }
        };
        init_sensors_functor init_sensors = new init_sensors_functor(m_SensorPositions, m_SensorNormals, m_RimSensorPositions, m_RimSensorNormals, mean_density, mean_rim_density, MaxNumSensors, MaxNumRimSensors);
        for_each_triangle(init_sensors, filter, pMesh, matMeshToLocal);

        m_NumSensors = init_sensors.num_sensors;
        m_NumRimSensors = init_sensors.num_rim_sensors;
        m_sensorBoundsMinCorner.set(init_sensors.bounds_min);
        m_sensorBoundsMaxCorner.set(init_sensors.bounds_max);
    }

    void update(OceanSurfaceHeights pHeights,  Matrix4f matLocalToWorld){
        Vector3f rv = CacheBuffer.getCachedVec3();

        for(int i = 0; i<m_NumSensors; i++)
        {
//            D3DXVec3TransformCoord(&rv,&m_SensorPositions[i], &matLocalToWorld);
            Matrix4f.transformCoord(matLocalToWorld, m_SensorPositions[i], rv);
            m_WorldSensorPositions[i].x = rv.x;
            m_WorldSensorPositions[i].y = rv.z;
            m_WorldSensorPositions[i].z = rv.y;
            m_ReadbackCoords[i].x = m_WorldSensorPositions[i].x;
            m_ReadbackCoords[i].y = m_WorldSensorPositions[i].y;

//            D3DXVec3TransformNormal(&rv,&m_SensorNormals[i], &matLocalToWorld);
            Matrix4f.transformNormal(matLocalToWorld, m_SensorPositions[i], rv);
            m_WorldSensorNormals[i].x = rv.x;
            m_WorldSensorNormals[i].y = rv.z;
            m_WorldSensorNormals[i].z = rv.y;
        }

        pHeights.getDisplacements(m_ReadbackCoords, m_Displacements, m_NumSensors);

        for(int i = 0; i<m_NumRimSensors; i++)
        {
//            D3DXVec3TransformCoord(&rv,&m_RimSensorPositions[i], &matLocalToWorld);
            Matrix4f.transformCoord(matLocalToWorld, m_RimSensorPositions[i], rv);
            m_WorldRimSensorPositions[i].x = rv.x;
            m_WorldRimSensorPositions[i].y = rv.z;
            m_WorldRimSensorPositions[i].z = rv.y;
            m_RimReadbackCoords[i].x = m_WorldRimSensorPositions[i].x;
            m_RimReadbackCoords[i].y = m_WorldRimSensorPositions[i].y;

//            D3DXVec3TransformNormal(&rv,&m_RimSensorNormals[i], &matLocalToWorld);
            Matrix4f.transformNormal(matLocalToWorld, m_RimSensorPositions[i], rv);
            m_WorldRimSensorNormals[i].x = rv.x;
            m_WorldRimSensorNormals[i].y = rv.z;
            m_WorldRimSensorNormals[i].z = rv.y;
        }

        pHeights.getDisplacements(m_RimReadbackCoords, m_RimDisplacements, m_NumRimSensors);

        CacheBuffer.free(rv);
    }

    int getNumSensors() { return m_NumSensors; }
	Vector3f[] getSensorPositions() { return m_SensorPositions; }
    Vector3f[] getSensorNormals() { return m_SensorNormals; }
    Vector3f[] getWorldSensorPositions()  { return m_WorldSensorPositions; }
    Vector3f[] getWorldSensorNormals()  { return m_WorldSensorNormals; }
	Vector4f[] getDisplacements()  { return m_Displacements; }
	Vector2f[] getReadbackCoords()  { return m_ReadbackCoords; }

    int getNumRimSensors()  { return m_NumRimSensors; }
    Vector3f[] getRimSensorPositions()  { return m_RimSensorPositions; }
    Vector3f[] getRimSensorNormals()  { return m_RimSensorNormals; }
    Vector3f[] getWorldRimSensorPositions()  { return m_WorldRimSensorPositions; }
    Vector3f[] getWorldRimSensorNormals()  { return m_WorldRimSensorNormals; }
    Vector4f[] getRimDisplacements()  { return m_RimDisplacements; }
    Vector2f[] getRimReadbackCoords()  { return m_RimReadbackCoords; }

	Vector3f getBoundsMinCorner()  { return m_sensorBoundsMinCorner; }
	Vector3f getBoundsMaxCorner()  { return m_sensorBoundsMaxCorner; }

    float getMeanSensorRadius()  { return m_MeanSensorRadius; }
    float getMeanRimSensorSeparation()  { return m_MeanRimSensorSeparation; }

    BufferGL GetVizMeshIndexBuffer()  { return m_pVisualisationMeshIB; }
    int GetVizMeshIndexCount()  { return m_VisualisationMeshIndexCount; }
    int GetVizMeshIndexFormat()  { return m_VisualisationMeshIndexFormat; }
}
