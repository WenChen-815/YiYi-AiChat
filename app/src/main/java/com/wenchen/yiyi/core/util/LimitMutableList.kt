package com.wenchen.yiyi.core.util

/**
 * 创建一个具有指定限制大小的可变列表
 * 当添加新元素导致超过限制时，会自动移除最旧的元素
 * @param limit 最大存储元素数量
 * @return LimitMutableList 实例
 */
fun <T> limitMutableListOf(limit: Int): LimitMutableList<T> = LimitMutableList(limit)

/**
 * 创建一个具有指定限制大小的可变列表，并用指定元素初始化
 * 当添加新元素导致超过限制时，会自动移除最旧的元素
 * @param limit 最大存储元素数量
 * @param elements 用于初始化列表的元素
 * @return LimitMutableList 实例
 */
fun <T> limitMutableListOf(limit: Int, vararg elements: T): LimitMutableList<T> {
    val list = LimitMutableList<T>(limit)
    elements.forEach { element ->
        list.add(element)
    }
    return list
}

/**
 * 一个有大小限制的可变列表
 * 当添加新元素导致超过限制时，会自动移除最旧的元素
 * @param limit 最大存储元素数量
 */
class LimitMutableList<T>(private var limit: Int) : MutableList<T> {
    private val list = mutableListOf<T>()
    override val size: Int
        get() = list.size

    fun setLimit(limit: Int) {
        this.limit = limit
        // 当限制大小减少时，移除超出限制的元素
        while (list.size > limit) {
            list.removeAt(0)
        }
    }

    fun getLimit(): Int {
        return limit
    }

    override fun contains(element: T): Boolean = list.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = list.containsAll(elements)

    override fun get(index: Int): T = list.get(index)

    override fun indexOf(element: T): Int = list.indexOf(element)

    override fun isEmpty(): Boolean = list.isEmpty()

    override fun iterator(): MutableIterator<T> = list.iterator()

    override fun lastIndexOf(element: T): Int = list.lastIndexOf(element)

    override fun add(element: T): Boolean {
        // 如果列表已达到限制，移除第一个元素
        if (list.size >= limit && limit > 0) {
            list.removeAt(0)
        }
        // 添加新元素
        return list.add(element)
    }

    override fun add(index: Int, element: T) {
        if (index < 0 || index > list.size) {
            throw IndexOutOfBoundsException("Index: $index, Size: ${list.size}")
        }

        // 如果列表已达到限制且是在末尾添加，移除第一个元素
        if (list.size >= limit && limit > 0) {
            list.removeAt(0)
        }

        list.add(index, element)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        if (index < 0 || index > list.size) {
            throw IndexOutOfBoundsException("Index: $index, Size: ${list.size}")
        }
        var currentIndex = index
        var added = false
        elements.forEach { element ->
            add(currentIndex, element)
            added = true
            currentIndex++
        }
        return added
    }

    override fun addAll(elements: Collection<T>): Boolean {
        elements.forEach { element ->
            add(element)
        }
        return elements.isNotEmpty()
    }

    override fun clear() {
        list.clear()
    }

    override fun listIterator(): MutableListIterator<T> = list.listIterator()

    override fun listIterator(index: Int): MutableListIterator<T> = list.listIterator(index)

    override fun remove(element: T): Boolean = list.remove(element)

    override fun removeAll(elements: Collection<T>): Boolean = list.removeAll(elements)

    override fun removeAt(index: Int): T = list.removeAt(index)

    override fun retainAll(elements: Collection<T>): Boolean = list.retainAll(elements)

    override fun set(index: Int, element: T): T {
        return list.set(index, element)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        list.subList(fromIndex, toIndex)

    override fun equals(other: Any?): Boolean = list.equals(other)

    override fun hashCode(): Int = list.hashCode()

    override fun toString(): String = list.toString()
}