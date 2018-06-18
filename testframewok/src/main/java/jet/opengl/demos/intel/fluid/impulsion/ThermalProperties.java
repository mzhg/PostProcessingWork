package jet.opengl.demos.intel.fluid.impulsion;

/**
 * Created by Administrator on 2018/4/7 0007.
 */

public class ThermalProperties {
    private float   mTemperature            ;   ///< Temperature of body -- heat per mass.
    private float   mThermalConductivity    ;   ///< Ability to transfer heat by contact.
    private float   mOneOverHeatCapacity    ;   ///< Reciprocal of heat capacity, where heat capacity is specific heat times mass.

    public ThermalProperties(){}

    public ThermalProperties(float temperature, float thermalConductivity, float oneOverHeatCapacity) {
        this.mTemperature = temperature;
        this.mThermalConductivity = thermalConductivity;
        this.mOneOverHeatCapacity = oneOverHeatCapacity;
    }

    public void set(ThermalProperties ohs){
        mTemperature = ohs.mTemperature;
        mThermalConductivity = ohs.mThermalConductivity;
        mOneOverHeatCapacity = ohs.mOneOverHeatCapacity;
    }

    public void setTemperature( float temperature )
    {
        assert ( temperature > 0.0f ) ;
        mTemperature = temperature ;
    }

    public float getTemperature() { return mTemperature ; }

    public void setThermalConductivity( float thermalConductivity ) {
        assert ( thermalConductivity >= 0.0f ) ;
        mThermalConductivity = thermalConductivity ;
    }

    public float getThermalConductivity() { return mThermalConductivity ; }

    public void setOneOverHeatCapacity( float oneOverHeatCapacity )
    {
        assert ( oneOverHeatCapacity > 0.0f ) ;
        mOneOverHeatCapacity = oneOverHeatCapacity ;
    }

    public float getOneOverHeatCapacity() { return mOneOverHeatCapacity ; }
}
