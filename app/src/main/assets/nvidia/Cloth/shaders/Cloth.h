//----------------------------------------------------------------------------------
// File:   Cloth.h
// Author: Cyril Zeller
// Email:  sdkfeedback@nvidia.com
// 
// Copyright (c) 2007 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, IMPLIED WARRANTIES OF MERCHANTABILITY
// AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA OR ITS SUPPLIERS
// BE  LIABLE  FOR  ANY  SPECIAL,  INCIDENTAL,  INDIRECT,  OR  CONSEQUENTIAL DAMAGES
// WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS OF BUSINESS PROFITS,
// BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY OTHER PECUNIARY LOSS)
// ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE, EVEN IF NVIDIA HAS
// BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
//
//----------------------------------------------------------------------------------

#ifndef CLOTH_H
#define CLOTH_H

#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#ifdef __cplusplus

typedef int int4[4];
typedef D3DXVECTOR2 float2;
typedef D3DXVECTOR3 float3;
typedef D3DXVECTOR4 float4;

struct Particle {
    unsigned int State;
    float3 Position;
};

struct AnchorPoint {
    float Coordinate;
};

struct NormalTangent {
    float3 Normal;
    float3 TangentX;
};

struct MeshVertex {
    float3 Position;
    float3 Normal;
    float3 TangentX;
    float2 TexCoord;
};

#define InOutParticle Particle&

#else

struct Particle {
    uint State ;
    float3 Position;
};

struct OldParticle {
    uint State /*: OldState*/;
    float3 Position /*: OldPosition*/;
};

struct AnchorPoint {
    float Coordinate /*: Coordinate*/;
};

struct NormalTangent {
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
};

struct MeshVertex {
    float3 Position /*: Position*/;
    float3 Normal /*: Normal*/;
    float3 TangentX /*: TangentX*/;
    float2 TexCoord /*: TexCoord*/;
};

#define InOutParticle inout Particle

#endif

// Bitfields
#define SET_BITS(variable, bits)          ((variable) |= (bits))
#define UNSET_BITS(variable, bits)        ((variable) &= ~(bits))
#define BITS_ARE_SET(variable, bits)      (((variable) & (bits)) != 0)
#define SET_BYTE(variable, offset, value) (UNSET_BITS(variable, (0xff << (offset))), SET_BITS(variable, ((value) << (offset))))
#define GET_BYTE(variable, offset)        (((variable) >> (offset)) & 0xff)

// State
#define STATE_CONNECTION_CONFIG_BYTE0 0
#define STATE_CONNECTION_CONFIG_BYTE1 1
#define STATE_CONNECTION_CONFIG_0     0
#define STATE_CONNECTION_CONFIG_1     1
#define STATE_CONNECTION_CONFIG_2     2
#define STATE_CONNECTION_CONFIG_3     3
#define STATE_CONNECTION_CONFIG_4     4
#define STATE_CONNECTION_CONFIG_5     5

#define STATE_FREE                   (1 << 24)
#define STATE_RIGHT_CONNECTION       (1 << 25)
#define STATE_BOTTOM_CONNECTION      (1 << 26)
#define STATE_BOTTOMLEFT_CONNECTION  (1 << 27)
#define STATE_BOTTOMRIGHT_CONNECTION (1 << 28)
#define STATE_STRUCTURAL_CONNECTION  (STATE_RIGHT_CONNECTION | STATE_BOTTOM_CONNECTION)
#define STATE_SHEAR_CONNECTION       (STATE_BOTTOMLEFT_CONNECTION | STATE_BOTTOMRIGHT_CONNECTION)
#define STATE_ALL_CONNECTION         (STATE_STRUCTURAL_CONNECTION | STATE_SHEAR_CONNECTION)

void SetConnectionConfig(InOutParticle particle, int index, int connectionConfig)
{
    SET_BYTE(particle.State, 8 * index, connectionConfig);
}

int ConnectionConfig(Particle particle, int configOffset)
{
    return GET_BYTE(particle.State, configOffset);
}

void SetFree(InOutParticle particle)
{
    SET_BITS(particle.State, STATE_FREE);
}

void UnsetFree(InOutParticle particle)
{
    UNSET_BITS(particle.State, STATE_FREE);
}

bool IsFree(Particle particle)
{
    return BITS_ARE_SET(particle.State, STATE_FREE);
}

void ResetConnectivity(InOutParticle particle)
{
    SET_BITS(particle.State, STATE_ALL_CONNECTION);
}

bool IsConnected(Particle particle, int connection)
{
    return BITS_ARE_SET(particle.State, connection);
}

void Disconnect(InOutParticle particle, int connection)
{
    UNSET_BITS(particle.State, connection);
}

// Collision
#define MAX_PLANES 8
struct Plane {
    float3 Normal;
    float Distance;
};
#define MAX_SPHERES 8
struct Sphere {
    float3 Center;
    float Radius;
};
#define MAX_CAPSULES 8
struct Capsule {
    float3 Origin;
    float Length;
    float4 Axis;
    float2 Radius;
    float2 Padding;
};
#define MAX_ELLIPSOIDS 8
struct Ellipsoid {
    float4 Transform[3];
};

// Cut
#define MAX_CUTTER_TRIANGLES 3

// Rendering
typedef float2 TexCoord;

#endif
