package jet.opengl.renderer.Unreal4.utils;

import java.util.Arrays;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;
import jet.opengl.renderer.Unreal4.UE4Engine;

/**
 * A dynamically sized bit array.
 * An array of Booleans.  They stored in one bit/Boolean.  There are iterators that efficiently iterate over only set bits.
 */
public class TBitArray implements Comparable<TBitArray>{

    // We want these to be correctly typed as int32, but we don't want them to have linkage, so we make them macros
    private static final int NumBitsPerDWORD  = 32;
    private static final int NumBitsPerDWORDLogTwo  = 5;

    private int         NumBits;
    private int         MaxBits;
    private final StackInt AllocatorInstance = new StackInt();

    /**
     * Minimal initialization constructor.
     */
    public TBitArray() {
        this(false, 0);
    }

    /**
     * Minimal initialization constructor.
     * @param Value - The value to initial the bits to.
     */
    public TBitArray(boolean Value /*= false*/)
    {
        this(Value, 0);
    }

    /**
     * Minimal initialization constructor.
     * @param Value - The value to initial the bits to.
     * @param InNumBits - The initial number of bits in the array.
     */
    public TBitArray(boolean Value /*= false*/, int InNumBits /*= 0*/ )
//	:	NumBits(0)
//	,	MaxBits(0)
    {
        Init(Value,InNumBits);
    }

    public void MoveOrCopy(TBitArray Dest){
        Dest.MaxBits = MaxBits;
        Dest.NumBits = NumBits;
        AllocatorInstance.moveTo(Dest.AllocatorInstance);

        MaxBits = 0;
        NumBits = 0;
    }

    /**
     * Assignment operator.
     */
    public void Set(TBitArray Copy)
    {
        // check for self assignment since we don't use swap() mechanic
        if( this == Copy )
        {
            return;
        }

        Empty(Copy.Num());
        NumBits = Copy.NumBits;
        if(NumBits > 0)
        {
			final int NumDWORDs = Numeric.divideAndRoundUp(MaxBits, NumBitsPerDWORD);
//            FMemory::Memcpy(GetData(),Copy.GetData(),NumDWORDs * sizeof(uint32));
            System.arraycopy(Copy.GetData(), 0, GetData(), 0, NumDWORDs);
        }
//        return this;
    }

    /**
     * Serializer

    friend FArchive& operator<<(FArchive& Ar, TBitArray& BitArray)
    {
        // serialize number of bits
        Ar << BitArray.NumBits;

        if (Ar.IsLoading())
        {
            // no need for slop when reading
            BitArray.MaxBits = BitArray.NumBits;

            // allocate room for new bits
            BitArray.Realloc(0);
        }

        // calc the number of dwords for all the bits
		const int32 NumDWORDs = FMath::DivideAndRoundUp(BitArray.NumBits, NumBitsPerDWORD);

        // serialize the data as one big chunk
        Ar.Serialize(BitArray.GetData(), NumDWORDs * sizeof(uint32));

        return Ar;
    }*/

    /**
     * Adds a bit to the array with the given value.
     * @return The index of the added bit.
     */
    public int Add(boolean Value)
    {
		final int Index = NumBits;

        Reserve(Index + 1);
        ++NumBits;
//        (*this)[Index] = Value;
        Set(Index, Value);

        return Index;
    }

    /**
     * Adds multiple bits to the array with the given value.
     * @return The index of the first added bit.
     */
    public int Add(boolean Value, int NumToAdd)
    {
		final int Index = NumBits;

        if (NumToAdd > 0)
        {
            Reserve(Index + NumToAdd);
            NumBits += NumToAdd;
            for (int It = Index, End = It + NumToAdd; It != End; ++It)
            {
//                (*this)[It] = Value;
                Set(It, Value);
            }
        }

        return Index;
    }

    /**
     * Removes all bits from the array, potentially leaving space allocated for an expected number of bits about to be added.
     * @param ExpectedNumBits - The expected number of bits about to be added.
     */
    void Empty(int ExpectedNumBits /*= 0*/)
    {
        NumBits = 0;

        ExpectedNumBits = Numeric.divideAndRoundUp(ExpectedNumBits, NumBitsPerDWORD) * NumBitsPerDWORD;
        // If the expected number of bits doesn't match the allocated number of bits, reallocate.
        if(MaxBits != ExpectedNumBits)
        {
            MaxBits = ExpectedNumBits;
            Realloc(0);
        }
    }

    /**
     * Reserves memory such that the array can contain at least Number bits.
     *
     * @Number  The number of bits to reserve space for.
     */
    public void Reserve(int Number)
    {
        if (Number > MaxBits)
        {
			final int MaxDWORDs =Numeric.divideAndRoundUp(Number,  NumBitsPerDWORD);  /*AllocatorInstance.CalculateSlackGrow(
                FMath::DivideAndRoundUp(Number,  NumBitsPerDWORD),
            FMath::DivideAndRoundUp(MaxBits, NumBitsPerDWORD),
            sizeof(uint32)
				);*/
            MaxBits = MaxDWORDs * NumBitsPerDWORD;
            Realloc(NumBits);
        }
    }

    /**
     * Removes all bits from the array retaining any space already allocated.
     */
    public void Reset()
    {
        // We need this because iterators often use whole DWORDs when masking, which includes off-the-end elements
//        FMemory::Memset(GetData(), 0, FMath::DivideAndRoundUp(NumBits, NumBitsPerDWORD) * sizeof(uint32));

        Arrays.fill(GetData(), 0);
        NumBits = 0;
    }

    /**
     * Resets the array's contents.
     * @param Value - The value to initial the bits to.
     * @param InNumBits - The number of bits in the array.
     */
    public void Init(boolean Value,int InNumBits)
    {
        Empty(InNumBits);
        if(InNumBits > 0)
        {
            NumBits = InNumBits;
//            FMemory::Memset(GetData(),Value ? 0xff : 0, FMath::DivideAndRoundUp(NumBits, NumBitsPerDWORD) * sizeof(uint32));
            Arrays.fill(GetData(), Value ? 0xff : 0);
        }
    }

    /**
     * Sets or unsets a range of bits within the array.
     * @param  Index  The index of the first bit to set.
     * @param  Num    The number of bits to set.
     * @param  Value  The value to set the bits to.
     */
    public void SetRange(int Index, int Num, boolean Value)
    {
        UE4Engine.check(Index >= 0 && Num >= 0 && Index + Num <= NumBits);

        if (Num == 0)
        {
            return;
        }

        // Work out which uint32 index to set from, and how many
        int StartIndex = Index / NumBitsPerDWORD;
        int Count      = (Index + Num + (NumBitsPerDWORD - 1)) / NumBitsPerDWORD - StartIndex;

        // Work out masks for the start/end of the sequence
        int StartMask  = 0xFFFFFFFF << (Index % NumBitsPerDWORD);
        int EndMask    = 0xFFFFFFFF >> (NumBitsPerDWORD - (Index + Num) % NumBitsPerDWORD) % NumBitsPerDWORD;

//        uint32* Data = GetData() + StartIndex;
        int[] Data = GetData();
        int DataIndex = StartIndex;
        if (Value)
        {
            if (Count == 1)
            {
                Data[DataIndex] |= StartMask & EndMask;
            }
            else
            {
//				*Data++ |= StartMask;
                Data[DataIndex++] |= StartMask;
                Count -= 2;
                while (Count != 0)
                {
//					*Data++ = ~0;
                    Data[DataIndex++] = ~0;
                    --Count;
                }
                Data[DataIndex] |= EndMask;
            }
        }
        else
        {
            if (Count == 1)
            {
                Data[DataIndex] &= ~(StartMask & EndMask);
            }
            else
            {
//				*Data++ &= ~StartMask;
                Data[DataIndex++] &= ~StartMask;
                Count -= 2;
                while (Count != 0)
                {
//					*Data++ = 0;
                    Data[DataIndex++] = 0;
                    --Count;
                }
                Data[DataIndex] &= ~EndMask;
            }
        }
    }

    /**
     * Removes bits from the array.
     * @param BaseIndex - The index of the first bit to remove.
     * @param NumBitsToRemove - The number of consecutive bits to remove.
     */
    public void RemoveAt(int BaseIndex,int NumBitsToRemove /*= 1*/)
    {
        UE4Engine.check(BaseIndex >= 0 && NumBitsToRemove >= 0 && BaseIndex + NumBitsToRemove <= NumBits);

        if (BaseIndex + NumBitsToRemove != NumBits)
        {
            // Until otherwise necessary, this is an obviously correct implementation rather than an efficient implementation.
            /*FIterator WriteIt(*this);
            for(FConstIterator ReadIt(*this);ReadIt;++ReadIt)
            {
                // If this bit isn't being removed, write it back to the array at its potentially new index.
                if(ReadIt.GetIndex() < BaseIndex || ReadIt.GetIndex() >= BaseIndex + NumBitsToRemove)
                {
                    if(WriteIt.GetIndex() != ReadIt.GetIndex())
                    {
                        WriteIt.GetValue() = (bool)ReadIt.GetValue();
                    }
                    ++WriteIt;
                }
            }*/

            throw new UnsupportedOperationException();
        }
        NumBits -= NumBitsToRemove;
    }

    /* Removes bits from the array by swapping them with bits at the end of the array.
     * This is mainly implemented so that other code using TArray::RemoveSwap will have
     * matching indices.
     * @param BaseIndex - The index of the first bit to remove.
     */
    public final void RemoveAtSwap( int BaseIndex){
        RemoveAtSwap(BaseIndex, 1);

    }

    /* Removes bits from the array by swapping them with bits at the end of the array.
     * This is mainly implemented so that other code using TArray::RemoveSwap will have
     * matching indices.
     * @param BaseIndex - The index of the first bit to remove.
     * @param NumBitsToRemove - The number of consecutive bits to remove.
     */
    public void RemoveAtSwap( int BaseIndex, int NumBitsToRemove/*=1*/ )
    {
        UE4Engine.check(BaseIndex >= 0 && NumBitsToRemove >= 0 && BaseIndex + NumBitsToRemove <= NumBits);
        if( BaseIndex < NumBits - NumBitsToRemove )
        {
            // Copy bits from the end to the region we are removing
            for( int Index=0;Index<NumBitsToRemove;Index++ )
            {
//#if PLATFORM_MAC || PLATFORM_LINUX
                // Clang compiler doesn't understand the short syntax, so let's be explicit
//                int FromIndex = NumBits - NumBitsToRemove + Index;
//                FConstBitReference From(GetData()[FromIndex / NumBitsPerDWORD],1 << (FromIndex & (NumBitsPerDWORD - 1)));
//
//                int32 ToIndex = BaseIndex + Index;
//                FBitReference To(GetData()[ToIndex / NumBitsPerDWORD],1 << (ToIndex & (NumBitsPerDWORD - 1)));
//
//                To = (bool)From;
//#else
//                (*this)[BaseIndex + Index] = (bool)(*this)[NumBits - NumBitsToRemove + Index];
                Set(BaseIndex + Index, Get(NumBits - NumBitsToRemove + Index));
//#endif
            }
        }

        // Remove the bits from the end of the array.
        NumBits -= NumBitsToRemove;
    }

    /**
     * Helper function to return the amount of memory allocated by this container
     * @return number of bytes allocated by this container
     */
    public int GetAllocatedSize( )
    {
        return Numeric.divideAndRoundUp(MaxBits, NumBitsPerDWORD) * /*sizeof(uint32)*/ 4;
    }

    /* Tracks the container's memory use through an archive. */
    /*void CountBytes(FArchive& Ar) const
    {
        Ar.CountBytes(
                FMath::DivideAndRoundUp(NumBits, NumBitsPerDWORD) * sizeof(uint32),
            FMath::DivideAndRoundUp(MaxBits, NumBitsPerDWORD) * sizeof(uint32)
		);
    }*/

    /**
     * Finds the first true/false bit in the array, and returns the bit index.
     * If there is none, INDEX_NONE is returned.
     */
    public int Find(boolean bValue)
    {
        // Iterate over the array until we see a word with a matching bit
		final int Test = bValue ? 0 : /*(uint32)*/-1;

		final int[] DwordArray = GetData();
        final int LocalNumBits = NumBits;
        final int DwordCount = Numeric.divideAndRoundUp(LocalNumBits, NumBitsPerDWORD);
        int DwordIndex = 0;
        while (DwordIndex < DwordCount && DwordArray[DwordIndex] == Test)
        {
            ++DwordIndex;
        }

        if (DwordIndex < DwordCount)
        {
            // If we're looking for a false, then we flip the bits - then we only need to find the first one bit
            final int Bits = bValue ? (DwordArray[DwordIndex]) : ~(DwordArray[DwordIndex]);
//            ASSUME(Bits != 0);
			final int LowestBitIndex = Numeric.countTrailingZeros(Bits) + (DwordIndex << NumBitsPerDWORDLogTwo);
            if (LowestBitIndex < LocalNumBits)
            {
                return LowestBitIndex;
            }
        }

        return UE4Engine.INDEX_NONE;
    }

    /**
     * Finds the last true/false bit in the array, and returns the bit index.
     * If there is none, INDEX_NONE is returned.
     */
    public int FindLast(boolean bValue)
    {
		final int LocalNumBits = NumBits;

        // Get the correct mask for the last word
        int SlackIndex = ((LocalNumBits - 1) % NumBitsPerDWORD) + 1;
        int Mask = ~0 >> (NumBitsPerDWORD - SlackIndex);

        // Iterate over the array until we see a word with a zero bit.
        int DwordIndex = Numeric.divideAndRoundUp(LocalNumBits, NumBitsPerDWORD);
		final int[] DwordArray = GetData();
		final int Test = bValue ? 0 : ~0;
        for (;;)
        {
            if (DwordIndex == 0)
            {
                return UE4Engine.INDEX_NONE;
            }
            --DwordIndex;
            if ((DwordArray[DwordIndex] & Mask) != (Test & Mask))
            {
                break;
            }
            Mask = ~0;
        }

        // Flip the bits, then we only need to find the first one bit -- easy.
		final int Bits = (bValue ? DwordArray[DwordIndex] : ~DwordArray[DwordIndex]) & Mask;
//        ASSUME(Bits != 0);

        int BitIndex = (NumBitsPerDWORD - 1) - Numeric.countLeadingZeros(Bits);

        int Result = BitIndex + (DwordIndex << NumBitsPerDWORDLogTwo);
        return Result;
    }

    public boolean Contains(boolean bValue)
    {
        return Find(bValue) != UE4Engine.INDEX_NONE;
    }

    /**
     * Finds the first zero bit in the array, sets it to true, and returns the bit index.
     * If there is none, INDEX_NONE is returned.
     */
    public int FindAndSetFirstZeroBit(int ConservativeStartIndex /*= 0*/)
    {
        // Iterate over the array until we see a word with a zero bit.
        int[] DwordArray = GetData();
		final int LocalNumBits = NumBits;
        final int DwordCount = Numeric.divideAndRoundUp(LocalNumBits, NumBitsPerDWORD);
        int DwordIndex = Numeric.divideAndRoundUp(ConservativeStartIndex, NumBitsPerDWORD);
        while (DwordIndex < DwordCount && DwordArray[DwordIndex] == /*(uint32)*/-1)
        {
            ++DwordIndex;
        }

        if (DwordIndex < DwordCount)
        {
            // Flip the bits, then we only need to find the first one bit -- easy.
			final int Bits = ~(DwordArray[DwordIndex]);
//            ASSUME(Bits != 0);
            final int LowestBit = (Bits) & (-Bits);
            final int LowestBitIndex = Numeric.countTrailingZeros(Bits) + (DwordIndex << NumBitsPerDWORDLogTwo);
            if (LowestBitIndex < LocalNumBits)
            {
                DwordArray[DwordIndex] |= LowestBit;
                return LowestBitIndex;
            }
        }

        return UE4Engine.INDEX_NONE;
    }

    /**
     * Finds the last zero bit in the array, sets it to true, and returns the bit index.
     * If there is none, INDEX_NONE is returned.
     */
    public int FindAndSetLastZeroBit()
    {
		final int LocalNumBits = NumBits;

        // Get the correct mask for the last word
        int SlackIndex = ((LocalNumBits - 1) % NumBitsPerDWORD) + 1;
        int Mask = ~0 >> (NumBitsPerDWORD - SlackIndex);

        // Iterate over the array until we see a word with a zero bit.
        int DwordIndex = Numeric.divideAndRoundUp(LocalNumBits, NumBitsPerDWORD);
        int[] DwordArray = GetData();
        for (;;)
        {
            if (DwordIndex == 0)
            {
                return UE4Engine.INDEX_NONE;
            }
            --DwordIndex;
            if ((DwordArray[DwordIndex] & Mask) != Mask)
            {
                break;
            }
            Mask = ~0;
        }

        // Flip the bits, then we only need to find the first one bit -- easy.
		final int Bits = ~DwordArray[DwordIndex] & Mask;
//        ASSUME(Bits != 0);

        int BitIndex = (NumBitsPerDWORD - 1) - Numeric.countLeadingZeros(Bits);
        DwordArray[DwordIndex] |= 1 << BitIndex;

        int Result = BitIndex + (DwordIndex << NumBitsPerDWORDLogTwo);
        return Result;
    }

    public void Set(int Index, boolean NewValue){
        int WriteIndex = Index / NumBitsPerDWORD;
        int Mask = 1 << (Index & (NumBitsPerDWORD - 1));

        int[] DataArray = GetData();
        int Data = DataArray[WriteIndex];
        if(NewValue)
        {
            Data |= Mask;
        }
        else
        {
            Data &= ~Mask;
        }

        DataArray[WriteIndex] = Data;
    }

    public boolean Get(int Index){
        int WriteIndex = Index / NumBitsPerDWORD;
        int Mask = 1 << (Index & (NumBitsPerDWORD - 1));

        int[] DataArray = GetData();
        int Data = DataArray[WriteIndex];

        return (Data & Mask) != 0;
    }

    // Accessors.
    public boolean IsValidIndex(int InIndex)
    {
        return InIndex >= 0 && InIndex < NumBits;
    }

    public int Num() { return NumBits; }

    public int[] GetData()
    {
        return AllocatorInstance.getData();
    }

    private void Realloc(int PreviousNumBits)
    {
		final int PreviousNumDWORDs = Numeric.divideAndRoundUp(PreviousNumBits, NumBitsPerDWORD);
        final int MaxDWORDs = Numeric.divideAndRoundUp(MaxBits, NumBitsPerDWORD);

//        AllocatorInstance.ResizeAllocation(PreviousNumDWORDs,MaxDWORDs,sizeof(uint32));
        AllocatorInstance.resize(MaxDWORDs);

        if(MaxDWORDs > 0)
        {
            // Reset the newly allocated slack DWORDs.
//            FMemory::Memzero((uint32*)AllocatorInstance.GetAllocation() + PreviousNumDWORDs,(MaxDWORDs - PreviousNumDWORDs) * sizeof(uint32));
            Arrays.fill(AllocatorInstance.getData(), PreviousNumDWORDs, MaxDWORDs - PreviousNumBits, 0);
        }
    }

    @Override
    public int compareTo(TBitArray other) {
        int result = Integer.compare(Num(), other.Num());
        if(result != 0)
            return result;

        int NumWords = Numeric.divideAndRoundUp(Num(), NumBitsPerDWORD);
		int[] Data0 = GetData();
		int[] Data1 = other.GetData();

        //lexicographically compare
        for (int i = 0; i < NumWords; i++)
        {
            result = Integer.compare(Data0[i], Data1[i]);
            if (result != 0)
            {
                return result;
            }
        }

        return 0;
    }
}
