package nv.visualFX.cloth.libs;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackLong;

/**
 * Created by mazhen'gui on 2017/9/16.
 */

final class PsSort {
    interface Predicate{
        boolean compare(int i0, int i1);
    }

    static void smallSort(int[] elements, int first, int last, Predicate compare)
    {
        // selection sort - could reduce to fsel on 360 with floats.
        for(int i = first; i < last; i++)
        {
            int m = i;
            for(int j = i + 1; j <= last; j++)
                if(compare.compare(elements[j], elements[m]))
                    m = j;

            if(m != i) {
//                swap(elements[m], elements[i]);
                Numeric.swap(elements, m,i);
            }
        }
    }

    static void median3(int[] elements, int first, int last, Predicate compare)
    {
        /*
        This creates sentinels because we know there is an element at the start minimum(or equal)
        than the pivot and an element at the end greater(or equal) than the pivot. Plus the
        median of 3 reduces the chance of degenerate behavour.
        */
        int mid = (first + last) / 2;

        if(compare.compare(elements[mid], elements[first])) {
//            swap(elements[first], elements[mid]);
            Numeric.swap(elements, first, mid);
        }

        if(compare.compare(elements[last], elements[first])) {
//            swap(elements[first], elements[last]);
            Numeric.swap(elements, first, last);
        }

        if(compare.compare(elements[last], elements[mid])) {
//            swap(elements[mid], elements[last]);
            Numeric.swap(elements, mid, last);
        }

        // keep the pivot at last-1
//        swap(elements[mid], elements[last - 1]);
        Numeric.swap(elements, mid, last - 1);
    }

    static int partition(int[]elements, int first, int last, Predicate compare)
    {
        median3(elements, first, last, compare);

	/*
	WARNING: using the line:

	T partValue = elements[last-1];

	and changing the scan loops to:

	while(comparator.greater(partValue, elements[++i]));
	while(comparator.greater(elements[--j], partValue);

	triggers a compiler optimizer bug on xenon where it stores a double to the stack for partValue
	then loads it as a single...:-(
	*/

        int i = first;    // we know first is less than pivot(but i gets pre incremented)
        int j = last - 1; // pivot is in last-1 (but j gets pre decremented)

        for(;;)
        {
            while(compare.compare(elements[++i], elements[last - 1]))
                ;
            while(compare.compare(elements[last - 1], elements[--j]))
                ;

            if(i >= j)
                break;

            assert (i <= last && j >= first);
//            swap(elements[i], elements[j]);
            Numeric.swap(elements, i, j);
        }
        // put the pivot in place

        assert(i <= last && first <= (last - 1));
//        swap(elements[i], elements[last - 1]);
        Numeric.swap(elements, i, last - 1);

        return i;
    }

    static void sort(int[] elements, Predicate compare, int initialStackSize /*= 32*/)
    {
        final int SMALL_SORT_CUTOFF = 5; // must be >= 3 since we need 3 for median

//        PX_ALLOCA(stackMem, int32_t, initialStackSize);
//        internal::Stack<Allocator> stack(stackMem, initialStackSize, inAllocator);
        StackLong stack = new StackLong();
        final int count = elements.length;
        int first = 0, last = count - 1;
        if(last > first)
        {
            for(;;)
            {
                while(last > first)
                {
                    assert (first >= 0 && last < count);
                    if(last - first < SMALL_SORT_CUTOFF)
                    {
                        smallSort(elements, first, last, compare);
                        break;
                    }
                    else
                    {
					    final int partIndex = partition(elements, first, last, compare);

                        // push smaller sublist to minimize stack usage
                        if((partIndex - first) < (last - partIndex))
                        {
                            stack.push(Numeric.encode(first, partIndex - 1));
                            first = partIndex + 1;
                        }
                        else
                        {
                            stack.push(Numeric.encode(partIndex + 1, last));
                            last = partIndex - 1;
                        }
                    }
                }

                if(stack.isEmpty())
                    break;

//                stack.pop(first, last);
                long value = stack.pop();
                first = Numeric.decodeFirst(value);
                last = Numeric.decodeSecond(value);
            }
        }
//#if PX_SORT_PARANOIA
        for(int i = 1; i < count; i++)
            assert (!compare.compare(elements[i], elements[i - 1]));
//#endif
    }
}
