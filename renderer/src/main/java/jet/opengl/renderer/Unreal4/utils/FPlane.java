package jet.opengl.renderer.Unreal4.utils;

import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Writable;
import org.lwjgl.util.vector.WritableVector;
import org.lwjgl.util.vector.WritableVector4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Structure for three dimensional planes.<p></p>
 *
 * Stores the coeffecients as Xx+Yy+Zz=W.
 * Note that this is different from many other Plane classes that use Xx+Yy+Zz+W=0.
 */
public class FPlane implements ReadableVector4f, WritableVector4f {
    public float X,Y,Z,W;

    /** Default constructor (no initialization). */
    public FPlane(){}

    /**
     * Copy Constructor.
     *
     * @param P Plane to copy from.
     */
    public FPlane(FPlane P){
        X = P.X;
        Y = P.Y;
        Z = P.Z;
        W = P.W;
    }

    /**
     * Constructor.
     *
     * @param V 4D vector to set up plane.
     */
    public FPlane(ReadableVector4f V){
        X = V.getX();
        Y = V.getY();
        Z = V.getZ();
        W = V.getW();
    }

    /**
     * Constructor.
     *
     * @param InX X-coefficient.
     * @param InY Y-coefficient.
     * @param InZ Z-coefficient.
     * @param InW W-coefficient.
     */
    public FPlane(float InX, float InY, float InZ, float InW){
        X = InX;
        Y = InY;
        Z = InZ;
        W = InW;
    }

    /**
     * Constructor.
     *
     * @param InNormal Plane Normal Vector.
     * @param InW Plane W-coefficient.
     */
    public FPlane(ReadableVector3f InNormal, float InW){
        X = InNormal.getX();
        Y = InNormal.getY();
        Z = InNormal.getZ();
        W = InW;
    }

    /**
     * Constructor.
     *
     * @param InBase Base point in plane.
     * @param InNormal Plane Normal Vector.
     */
    public FPlane(ReadableVector3f InBase, ReadableVector3f InNormal){
        X = InNormal.getX();
        Y = InNormal.getY();
        Z = InNormal.getZ();
        W = Vector3f.dot(InBase, InNormal);
    }

    /**
     * Constructor.
     *
     * @param A First point in the plane.
     * @param B Second point in the plane.
     * @param C Third point in the plane.
     */
    public FPlane(ReadableVector3f A, ReadableVector3f B, ReadableVector3f C){
        Vector3f.computeNormal(A,B,C, this);
        W = Vector3f.dot(A, this);
    }

    /**
     * Constructor
     *
     * @param EForceInit Force Init Enum.

    explicit FORCEINLINE FPlane(EForceInit);*/

    // Functions.

    /**
     * Calculates distance between plane and a point.
     *
     * @param P The other point.
     * @return The distance from the plane to the point. 0: Point is on the plane. >0: Point is in front of the plane. <0: Point is behind the plane.
     */
    public float PlaneDot(ReadableVector3f P) {
        return Vector3f.dot(this, P) - W;
    }

    /**
     * Normalize this plane in-place if it is larger than a given tolerance. Leaves it unchanged if not.
     *
     * @return true if the plane was normalized correctly, false otherwise.
     */
    public final boolean Normalize(){
        return Normalize(1.e-8f);
    }

    /**
     * Normalize this plane in-place if it is larger than a given tolerance. Leaves it unchanged if not.
     *
     * @param Tolerance Minimum squared length of vector for normalization.
     * @return true if the plane was normalized correctly, false otherwise.
     */
    public boolean Normalize(float Tolerance/*=SMALL_NUMBER*/){
        final float SquareSum = X*X + Y*Y + Z*Z;
        if(SquareSum > Tolerance)
        {
            final double Scale = /*Numeric.invSqrt(SquareSum)*/1/ Math.sqrt(SquareSum);
            X *= Scale; Y *= Scale; Z *= Scale; W *= Scale;
            return true;
        }
        return false;
    }

    /**
     * Get a flipped version of the plane.
     *
     * @return A flipped version of the plane.
     */
    public FPlane Flip(){
        return new FPlane(-X, -Y, -Z, -W);
    }

    /**
     * Get the result of transforming the plane by a Matrix.
     *
     * @param M The matrix to transform plane with.
     * @return The result of transform.
     */
    public FPlane TransformBy(Matrix4f M) {
        throw new UnsupportedOperationException();
    }

    /**
     * You can optionally pass in the matrices transpose-adjoint, which save it recalculating it.
     * MSM: If we are going to save the transpose-adjoint we should also save the more expensive
     * determinant.
     *
     * @param M The Matrix to transform plane with.
     * @param DetM Determinant of Matrix.
     * @param TA Transpose-adjoint of Matrix.
     * @return The result of transform.
     */
    public FPlane TransformByUsingAdjointT(Matrix4f M, float DetM, Matrix4f TA) {
        throw new UnsupportedOperationException();
    }

    /**
     * Check if two planes are identical.
     *
     * @param V The other plane.
     * @return true if planes are identical, otherwise false.
     */
    public boolean eqauls(FPlane V) {
        return X == V.X && Y == V.Y &&
                Z == V.Z && W == V.W ;
    }

    /**
     * Checks whether two planes are equal within specified tolerance.
     *
     * @param V The other plane.
     * @param Tolerance Error Tolerance.
     * @return true if the two planes are equal within specified tolerance, otherwise false.
     */
    public boolean Equals(FPlane V, float Tolerance/*=KINDA_SMALL_NUMBER*/){
        return Math.abs(X - V.X) <=Tolerance
                && Math.abs(Y - V.Y) <= Tolerance
                && Math.abs(Y - V.Y) <= Tolerance
                && Math.abs(Y - V.Y) <= Tolerance;
    }

    /**
     * Calculates dot product of two planes.
     *
     * @param V The other plane.
     * @return The dot product.

    FORCEINLINE float operator|(const FPlane& V) const;*/

    /**
     * Gets result of adding a plane to this.
     *
     * @param V The other plane.
     * @return The result of adding a plane to this.

    FPlane operator+(const FPlane& V) const;*/

    /**
     * Gets result of subtracting a plane from this.
     *
     * @param V The other plane.
     * @return The result of subtracting a plane from this.

    FPlane operator-(const FPlane& V) const;*/

    /**
     * Gets result of dividing a plane.
     *
     * @param Scale What to divide by.
     * @return The result of division.

    FPlane operator/(float Scale) const;*/

    /**
     * Gets result of scaling a plane.
     *
     * @param Scale The scaling factor.
     * @return The result of scaling.

    FPlane operator*(float Scale) const;*/

    /**
     * Gets result of multiplying a plane with this.
     *
     * @param V The other plane.
     * @return The result of multiplying a plane with this.

    FPlane operator*(const FPlane& V);*/

    /**
     * Add another plane to this.
     *
     * @param V The other plane.
     * @return Copy of plane after addition.

    FPlane operator+=(const FPlane& V);*/

    /**
     * Subtract another plane from this.
     *
     * @param V The other plane.
     * @return Copy of plane after subtraction.

    FPlane operator-=(const FPlane& V);*/

    /**
     * Scale this plane.
     *
     * @param Scale The scaling factor.
     * @return Copy of plane after scaling.

    FPlane operator*=(float Scale);*/

    /**
     * Multiply another plane with this.
     *
     * @param V The other plane.
     * @return Copy of plane after multiplication.

    FPlane operator*=(const FPlane& V);*/

    /**
     * Divide this plane.
     *
     * @param V What to divide by.
     * @return Copy of plane after division.

    FPlane operator/=(float V);*/

    /**
     * Serializer.
     *
     * @param Ar Serialization Archive.
     * @param P Plane to serialize.
     * @return Reference to Archive after serialization.

    friend FArchive& operator<<(FArchive& Ar, FPlane &P)
    {
        return Ar << (FVector&)P << P.W;
    }

    bool Serialize(FArchive& Ar)
    {
        if (Ar.UE4Ver() >= VER_UE4_ADDED_NATIVE_SERIALIZATION_FOR_IMMUTABLE_STRUCTURES)
        {
            Ar << *this;
            return true;
        }
        return false;
    }*/

    /**
     * Serializes the vector compressed for e.g. network transmission.
     * @param Ar Archive to serialize to/ from.
     * @return false to allow the ordinary struct code to run (this never happens).

    bool NetSerialize(FArchive& Ar, class UPackageMap*, bool& bOutSuccess)
    {
        if(Ar.IsLoading())
        {
            int16 iX, iY, iZ, iW;
            Ar << iX << iY << iZ << iW;
			*this = FPlane(iX,iY,iZ,iW);
        }
        else
        {
            int16 iX(FMath::RoundToInt(X));
            int16 iY(FMath::RoundToInt(Y));
            int16 iZ(FMath::RoundToInt(Z));
            int16 iW(FMath::RoundToInt(W));
            Ar << iX << iY << iZ << iW;
        }
        bOutSuccess = true;
        return true;
    }*/

    @Override
    public float getW() {
        return W;
    }

    @Override
    public float getZ() {
        return Z;
    }

    @Override
    public float getX() {
        return X;
    }

    @Override
    public float getY() {
        return Y;
    }

    @Override
    public float length() {
        return Vector3f.length(X,Y,Z);
    }

    @Override
    public float lengthSquared() {
        return Vector3f.lengthSquare(X,Y,Z);
    }

    @Override
    public FloatBuffer store(FloatBuffer buf) {
        buf.put(X);
        buf.put(Y);
        buf.put(Z);
        buf.put(W);
        return buf;
    }

    @Override
    public float[] store(float[] arr, int offset) {
        arr[offset++] = X;
        arr[offset++] = Y;
        arr[offset++] = Z;
        arr[offset++] = W;
        return arr;
    }

    @Override
    public float get(int index) throws IndexOutOfBoundsException {
        switch (index) {
            case 0: return X;
            case 1: return Y;
            case 2: return Z;
            case 3: return W;
            default:
                throw new IndexOutOfBoundsException("index = " + index);
        }
    }

    @Override
    public void setValue(int index, float v) throws IndexOutOfBoundsException {
        switch (index) {
            case 0: X = v; break;
            case 1: Y = v; break;
            case 2: Z = v; break;
            case 3: W = v; break;
            default:
                throw new IndexOutOfBoundsException("index = " + index);
        }
    }

    @Override
    public FPlane load(FloatBuffer buf) {
        X = buf.get();
        Y = buf.get();
        Z = buf.get();
        W = buf.get();
        return this;
    }

    @Override
    public FPlane load(float[] arr, int offset) {
        X = arr[offset++];
        Y = arr[offset++];
        Z = arr[offset++];
        W = arr[offset++];
        return this;
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public boolean isZero() {
        return X == 0 && Y== 0&& Z ==0 && W == 0;
    }

    @Override
    public boolean isOne() {
        return X == 1 && Y== 1&& Z ==1 && W == 1;
    }

    @Override
    public boolean isNaN() {
        return Float.isNaN(X) || Float.isNaN(Y) || Float.isNaN(Z) || Float.isNaN(W);
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putFloat(X);
        buf.putFloat(Y);
        buf.putFloat(Z);
        buf.putFloat(W);
        return buf;
    }

    @Override
    public void setW(float w) {
        W = w;
    }

    @Override
    public void set(float x, float y, float z, float w) {
        X = x;
        Y = y;
        Z = z;
        W = w;
    }

    @Override
    public void setZ(float z) {
        Z = z;
    }

    @Override
    public void set(float x, float y, float z) {
        X = x;
        Y = y;
        Z = z;
    }

    @Override
    public void setX(float x) {
        X = x;
    }

    @Override
    public void setY(float y) {
        Y = y;
    }

    @Override
    public void set(float x, float y) {
        X = x;
        Y = y;
    }

    @Override
    public FPlane load(ByteBuffer buf) {
        X = buf.getFloat();
        Y = buf.getFloat();
        Z = buf.getFloat();
        W = buf.getFloat();
        return this;
    }
}
