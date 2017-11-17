package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

public final class CPUTLight extends CPUTRenderNode{
    protected final CPUTLightParams mLightParams = new CPUTLightParams();

//    public void             SetLightParameters(CPUTLightParams lightParams);
    public CPUTLightParams GetLightParameters() {return mLightParams; }
    public int       LoadLight(CPUTConfigBlock pBlock/*, int *pParentID*/){
        // set the null/group node name
        mName = pBlock.GetValueByName("name").ValueAsString();

        // get the parent ID
        int parentID = pBlock.GetValueByName("parent").ValueAsInt();

        LoadParentMatrixFromParameterBlock( pBlock );

        String lightType = pBlock.GetValueByName("lighttype").ValueAsString();
        if(lightType.compareTo("spot") == 0)
        {
            mLightParams.nLightType = LightType.CPUT_LIGHT_SPOT;
        }
        else if(lightType.compareTo(_L("directional")) == 0)
        {
            mLightParams.nLightType = LightType.CPUT_LIGHT_DIRECTIONAL;
        }
        else if(lightType.compareTo(_L("point")) == 0)
        {
            mLightParams.nLightType = LightType.CPUT_LIGHT_POINT;
        }
        else
        {
            // ASSERT(0,_L(""));
            // TODO: why doesn't assert work here?
        }

        pBlock.GetValueByName(_L("Color")).ValueAsFloatArray(mLightParams.pColor, 0, 3);
        mLightParams.fIntensity    = pBlock.GetValueByName(_L("Intensity")).ValueAsFloat();
        mLightParams.fHotSpot      = pBlock.GetValueByName(_L("HotSpot")).ValueAsFloat();
        mLightParams.fConeAngle    = pBlock.GetValueByName(_L("ConeAngle")).ValueAsFloat();
        mLightParams.fDecayStart   = pBlock.GetValueByName(_L("DecayStart")).ValueAsFloat();
        mLightParams.bEnableFarAttenuation = pBlock.GetValueByName(_L("EnableNearAttenuation")).ValueAsBool();
        mLightParams.bEnableFarAttenuation = pBlock.GetValueByName(_L("EnableFarAttenuation")).ValueAsBool();
        mLightParams.fNearAttenuationStart = pBlock.GetValueByName(_L("NearAttenuationStart")).ValueAsFloat();
        mLightParams.fNearAttenuationEnd   = pBlock.GetValueByName(_L("NearAttenuationEnd")).ValueAsFloat();
        mLightParams.fFarAttenuationStart  = pBlock.GetValueByName(_L("FarAttenuationStart")).ValueAsFloat();
        mLightParams.fFarAttenuationEnd    = pBlock.GetValueByName(_L("FarAttenuationEnd")).ValueAsFloat();

        return parentID;
    }
}
