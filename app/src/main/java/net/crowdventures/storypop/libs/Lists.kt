package net.crowdventures.storypop.libs

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class Lists {
        @OptIn(ExperimentalContracts::class)
        internal inline fun <T> fastForEach(list:List<T> ,action: (T) -> Unit) {
            contract { callsInPlace(action) }
            for (index in list.indices) {
                val item = list[index]
                action(item)
            }
        }

        @OptIn(ExperimentalContracts::class)
        internal inline fun <T> fastForEachIndexed(list: List<T>,action: (index: Int, T) -> Unit) {
            contract { callsInPlace(action) }
            for (index in list.indices) {
                val item = list[index]
                action(index, item)
            }
        }



        @OptIn(ExperimentalContracts::class)
        internal inline fun <R> fastMapRange(
            start: Int,
            end: Int,
            transform: (Int) -> R
        ): List<R> {
            contract { callsInPlace(transform) }
            val destination = ArrayList<R>(/* initialCapacity = */ end - start + 1)
            for (i in start..end) {
                destination.add(transform(i))
            }
            return destination
        }
}