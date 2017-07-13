layout(location = 0) in uint In_uiParticleID;

flat out uint m_uiParticleID;

void main()
{
    m_uiParticleID = In_uiParticleID;
}