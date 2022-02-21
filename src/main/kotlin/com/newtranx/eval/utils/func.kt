package com.newtranx.eval.utils

import kotlin.math.ln
import kotlin.math.min

/**
 * @Author: anson
 * @Date: 2022/1/29 11:41 PM
 */
inline fun <T> zip(vararg lists: List<T>): List<List<T>> {
    return zip(*lists, transform = { it })
}

inline fun <T, V> zip(vararg lists: List<T>, transform: (List<T>) -> V): List<V> {
    val minSize = lists.map(List<T>::size).minOrNull() ?: return emptyList()
    val list = ArrayList<V>(minSize)

    val iterators = lists.map { it.iterator() }
    var i = 0
    while (i < minSize) {
        list.add(transform(iterators.map { it.next() }))
        i++
    }
    return list
}

/**
 * Extracts all ngrams (min_order <= n <= max_order) from a sentence.
 * @param line: A string sentence.
 * @param minOrder: Minimum n-gram order.
 * @param maxOrder: Maximum n-gram order.
 * @return a Counter object with n-grams counts and the sequence length.
 */
fun extractAllWordNgrams(
    line: String,
    minOrder: Int,
    maxOrder: Int
): Pair<Counter<List<String>>, Double> {
    val counter = Counter<List<String>>()
    val tokens = line.split(" ")
    (minOrder until maxOrder + 1).forEach { n ->
        val nGrams = extractNgrams(tokens, n)
        counter.update(nGrams)
    }
    return Pair(counter, tokens.size.toDouble())
}

fun extractNgrams(
    tokens: List<String>,
    n: Int = 1
): List<List<String>> {
    return (0 until (tokens.size - n + 1)).map { i ->
        tokens.subList(i, i + n)
    }
}

class Counter<T>(
    data: List<T>? = null
) {
    private val store = mutableMapOf<T, Int>()
    private val keys = mutableSetOf<T>()

    init {
        if (data != null)
            this.update(data)
    }

    fun update(data: List<T>) {
        data.forEach { key ->
            val count = store[key] ?: 0
            if (count == 0) {
                keys.add(key)
            }
            store[key] = count + 1
        }
    }

    private fun add(key: T, count: Int) {
        store[key] = count
        keys.add(key)
    }

    fun keys(): Set<T> {
        return keys
    }

    fun values(): List<Int> {
        return store.values.toList()
    }

    fun count(key: T): Int {
        return store[key] ?: 0
    }

    fun exist(key: T): Boolean {
        return store.containsKey(key)
    }

    fun intersect(counter: Counter<T>): Counter<T> {
        val iKeys = this.keys intersect counter.keys
        val newCounter = Counter<T>()
        iKeys.forEach {
            val cur = count(it)
            val income = counter.count(it)
            val value = min(cur, income)
            newCounter.add(it, value)
        }
        return newCounter
    }

    operator fun set(key: T, count: Int) {
        if (!store.containsKey(key)) {
            keys.add(key)
        }
        store[key] = 0.takeIf { count < 0 } ?: count
    }

}

inline fun <T, V> Counter<T>.map(transform: (key: T, count: Int) -> V): List<V> {
    val array = mutableListOf<V>()
    this.keys().forEach {
        val item = transform.invoke(it, this.count(it))
        array.add(item)
    }
    return array
}

inline fun <T> Counter<T>.forEach(action: (key: T, count: Int) -> Unit) {
    this.keys().forEach {
        action.invoke(it, this.count(it))
    }
}

fun main() {
    val a = extractAllWordNgrams("how word are you hello word are", 1, 4)
    println(a)
}


/**
 * Floors the log function
 * @param num: the number
 * @return log(num) floored to a very low number
 */
fun myLog(num: Double): Double {
    if (num == 0.0)
        return -9999999999.0
    return ln(num)
}

/**
 * Aggregates list of numeric lists by summing.
 */
fun sumOfLists(lists: List<List<Double>>): List<Double> {
    // Aggregates list of numeric lists by summing.
    if (lists.size == 1) {
        return lists[0]
    }

    // Preserve datatype
    val size = lists[0].size
    val total = DoubleArray(size) { 0.0 }
    lists.forEach { ll ->
        (0 until size).forEach { i ->
            total[i] = total[i].plus(ll[i])
        }
    }
    return total.toList()
}