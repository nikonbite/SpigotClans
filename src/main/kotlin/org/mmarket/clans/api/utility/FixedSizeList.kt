package org.mmarket.clans.api.utility

/**
 * Класс FixedSizeList реализует список с фиксированным количеством элементов.
 *
 * При добавлении нового элемента, если список полон, самый старый элемент удаляется,
 * а новый добавляется в начало списка, смещая остальные элементы вправо.
 *
 * @param T тип хранимых элементов
 * @property maxSize максимальное количество элементов в списке
 * @author <a href="https://t.me/nikonbitecode">@nikonbitecode</a>
 */
class FixedSizeList<T>(private val maxSize: Int) {
    // Используем ArrayDeque для эффективного управления элементами
    private val deque = ArrayDeque<T>(maxSize)

    /**
     * Добавляет новый элемент в список.
     * Если список уже полон, удаляется самый старый элемент (последний в очереди).
     *
     * @param item элемент для добавления
     */
    fun add(item: T) {
        if (deque.size == maxSize) {
            deque.removeLast() // Удаляем последний элемент, если достигнут лимит
        }
        deque.addFirst(item) // Добавляем новый элемент в начало
    }

    /**
     * Возвращает текущее содержимое списка в виде неизменяемого списка.
     *
     * @return список элементов в порядке их актуальности (новые в начале)
     */
    fun toList(): List<T> = deque.toList()
}