layout(early_fragment_tests) in;

layout(binding = 1) buffer OccluderVisible
{
    uint VisibleBuffer[];
};

in flat int occludeeID;
void main()
{
    VisibleBuffer[occludeeID] = 1;
}