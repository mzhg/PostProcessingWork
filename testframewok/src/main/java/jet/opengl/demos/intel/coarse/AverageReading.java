package jet.opengl.demos.intel.coarse;

final class AverageReading {
    private int Index;
    private int mSize;
    private final float[] Readings = new float[64];

    AverageReading()
    {
        Index = 0;
        mSize = 64;
        for(int i=0;i<mSize;i++)
            Readings[i] = -1.0f;

    }

    AverageReading(int Size)
    {
        Index = 0;
        mSize = Size;
        for(int i=0;i<mSize;i++)
            Readings[i] = -1.0f;

    }
    void Set(float NewReading)
    {
        Readings[Index]=NewReading;
        Index++;
        Index = Index%mSize;
    }

    float Get()
    {
        float Result=0;
        int dataGathered = 0;
        for(int i=0;i<mSize;i++)
        {
            if(Readings[i]!=-1.0f)
            {
                dataGathered++;
                Result+=Readings[i];
            }
        }
        if( dataGathered == 0 )
            Result = 0.0f;
        else
            Result /= (float)dataGathered;

        return Result;
    }
}
