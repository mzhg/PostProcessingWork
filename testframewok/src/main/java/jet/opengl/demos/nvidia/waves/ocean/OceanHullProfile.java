package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Vector2f;

import jet.opengl.postprocessing.texture.TextureGL;

final class OceanHullProfile {
    // The SRV for doing the lookup
    private TextureGL m_pSRV;

    OceanHullProfile() {}

    OceanHullProfile(TextureGL pSRV)
    {
        m_pSRV = pSRV;
    }

    OceanHullProfile(OceanHullProfile rhs)
    {
		set(rhs);
    }

    OceanHullProfile set(OceanHullProfile rhs)
    {
        if(this != rhs) {
//            SAFE_RELEASE(m_pSRV);
            m_pSRV = rhs.m_pSRV;
            m_WorldToProfileCoordsOffset.set(rhs.m_WorldToProfileCoordsOffset);
            m_WorldToProfileCoordsScale.set(rhs.m_WorldToProfileCoordsScale);
            m_ProfileToWorldHeightOffset = rhs.m_ProfileToWorldHeightOffset;
            m_ProfileToWorldHeightScale = rhs.m_ProfileToWorldHeightScale;
            m_TexelSizeInWorldSpace = rhs.m_TexelSizeInWorldSpace;

        }

        return this;
    }

	/*~OceanHullProfile()
    {
        SAFE_RELEASE(m_pSRV);
    }*/

    TextureGL GetSRV() { return m_pSRV; }

    // For generating lookup coords from world coords: uv = offset + scale * world_pos
    final Vector2f m_WorldToProfileCoordsOffset = new Vector2f();
    final Vector2f m_WorldToProfileCoordsScale = new Vector2f();

    // For generating a world height from lookup: world_height = offset + scale * lookup
    float m_ProfileToWorldHeightOffset;
    float m_ProfileToWorldHeightScale;

    // For choosing the right mip
    float m_TexelSizeInWorldSpace;
}
