#include "Cloth_Common.h"

// Time
uniform float TimeStep;
uniform float OldTimeStep;

// Forces
uniform float GravityStrength = 9.8f;
uniform float3 Wind;

// Structural and shear constraints: Force particles to be at a fixed distance from each other

float2 Responsiveness(Particle particle0, Particle particle1)
{
    if (IsFree(particle0)) {
        if (IsFree(particle1))
            return float2(0.5, 0.5);
        else
            return float2(1, 0);
    }
    else {
        if (IsFree(particle1))
            return float2(0, 1);
        else
            return float2(0, 0);
    }
}

void DistanceConstraint(inout Particle particle0, inout Particle particle1, float targetDistance)
{
    float3 delta = particle1.Position - particle0.Position;
    float distance = max(length(delta), 1e-7);
    float stretching = 1 - targetDistance / distance;
    delta = stretching * delta;
    float2 responsiveness = Responsiveness(particle0, particle1);
    particle0.Position += responsiveness[0] * delta;
    particle1.Position -= responsiveness[1] * delta;
}

void SatisfyStructuralSpringConstraint(inout Particle particle0, int connection0, inout Particle particle1, float targetDistance)
{
    if (IsConnected(particle0, connection0))
        DistanceConstraint(particle0, particle1, targetDistance);
}

void SatisfyShearSpringConstraint(inout Particle particle0, int connection0, inout Particle particle1, float targetDistance)
{
    if (IsConnected(particle0, connection0))
        DistanceConstraint(particle0, particle1, targetDistance);
}

struct Triangle {
    float3 O;
    float3 E[2];
};

bool TriangleIntersectsSegment(Triangle tri, float3 A, float3 B)
{
    float3 dir = B - A;
    float3 dirXedge1 = cross(dir, tri.E[1]);
    float det = dot(tri.E[0], dirXedge1);
    float3 diff = A - tri.O;
    if (det < 0) {
        diff = - diff;
        det = - det;
    }
    if (det > 0.00001) {
        float u = dot(diff, dirXedge1);
        if (0 < u && u < det) {
            float3 diffXedge0 = cross(diff, tri.E[0]);
            float v = dot(dir, diffXedge0);
            if (0 < v && u + v < det) {
                float t = dot(tri.E[1], diffXedge0) / det;
                return 0 < t && t < 1;
            }
            else
                return false;
        }
        else
            return false;
    }
    else
        return false;
}

void TestSpring(inout Particle particle0, int connection0, inout Particle particle1)
{
    Triangle cutter;
    cutter.O = float4(Eye, 1);
    int edgeIndex = FirstCutterTriangle;
    for (int t = 0; t < NumCutterTriangles; ++t) {
        cutter.E[0] = CutterEdge[edgeIndex].xyz;
        ++edgeIndex;
        if (edgeIndex > MAX_CUTTER_TRIANGLES)
            edgeIndex = 0;
        cutter.E[1] = CutterEdge[edgeIndex].xyz;
        if (TriangleIntersectsSegment(cutter, particle0.Position, particle1.Position))
            Disconnect(particle0, connection0);
    }
}

void TestSpringQuad(inout Particle particle[4])
{
    if ((!IsConnected(particle[0], STATE_RIGHT_CONNECTION) && !IsConnected(particle[2], STATE_RIGHT_CONNECTION)) ||
        (!IsConnected(particle[0], STATE_BOTTOM_CONNECTION) && !IsConnected(particle[1], STATE_BOTTOM_CONNECTION)) ||
        !IsConnected(particle[0], STATE_BOTTOMRIGHT_CONNECTION) ||
        !IsConnected(particle[1], STATE_BOTTOMLEFT_CONNECTION)) {
        Disconnect(particle[0], STATE_BOTTOMRIGHT_CONNECTION);
        Disconnect(particle[1], STATE_BOTTOMLEFT_CONNECTION);
    }
}