/*******************************************************************************
 * Copyright (c) 2017 Opt4J
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/

package org.opt4j.optimizer.ea.espea;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opt4j.core.Individual;

/**
 * Maps <code>Individuals</code> to indices in the range 0 to
 * <code>capacity</code> and vice versa. Allows efficient iteration over the
 * individuals with the incices in a specific range. Stores a maximum of
 * <code>capacity</code> individuals.
 *
 * @author luisgerhorst
 */
public class IndexMap implements Iterable<IndexMap.Entry> {

    private final Map<Individual, Integer> indices;
    private final Individual[] objects;
    private final List<Integer> freeIndices;

    public IndexMap(int capacity) {
        capacity++;
        indices = new HashMap<Individual, Integer>();
        objects = new Individual[capacity];
        freeIndices = new LinkedList<Integer>();
        for (int index = 0; index < capacity; index++) {
            freeIndices.add(index);
        }
    }

    public int size() {
        return indices.size();
    }

    public int get(final Individual object) {
        assert object != null : "object is null";
        return indices.get(object);
    }

    public Individual get(final int index) {
        assert objects[index] != null : "Requested slot is not used";
        return objects[index];
    }

    /**
     * Assigns an index to an individual.
     *
     * @param object The object to be enumerated
     * @return the index assigned to the given object
     */
    public int put(final Individual object) {
        assert freeIndices.size() + indices.size() == objects.length: "indices/freeIndices out of sync";
        final int index = freeIndices.remove(0);
        indices.put(object, index);
        objects[index] = object;
        return index;
    }

    /**
     * Removes the given individual from the map.
     *
     * @param object The individual to be removed from the map
     * @return the index that was mapped to the object
     */
    public int remove(final Individual object) {
        assert freeIndices.size() + indices.size() == objects.length: "indices/freeIndices out of sync";
        final int index = indices.remove(object);
        objects[index] = null;
        freeIndices.add(0, index);
        return index;
    }

    /**
     * Equals <code>iterator(0, capacity)</code>.
     */
    public Iterator<Entry> iterator() {
        return iterator(0, objects.length);
    }

    /**
     * Equals <code>iterator(0, end)</code>.
     */
    public Iterator<Entry> iteratorTo(int end) {
        return iterator(0, end);
    }

    /**
     * Equals <code>iterator(start, capacity)</code>.
     */
    public Iterator<Entry> iteratorFrom(int start) {
        return iterator(start, objects.length);
    }

    /**
     * @param start The first index that may be considered for iteration
     * @param end Exclusive, all iterated indices shall be smaller
     * @return an iterator for all mapped individuals (together with their
     * corresponding index) in order (smaller indices before greater ones). Note
     * that not all indexes in the given range may be encountered since some of
     * them may not be assigned to an individual.
     */
    public Iterator<Entry> iterator(int start, int end) {
        return new IndexMap.IndexMapIterator(objects, start, end);
    }

    private static class IndexMapIterator implements Iterator<IndexMap.Entry> {

        private final Individual[] objects;
        private final int end; // exclusive
        private int index;

        public IndexMapIterator(final Individual[] objects,
                                final int start,
                                final int end)
        {
            assert 0 <= start && end <= objects.length : "Start / end not within array boundaries";
            this.objects = objects;
            this.end = end;
            this.index = start;
            skipUnusedSlots();
        }

        public boolean hasNext() {
            return index < end;
        }

        public Entry next() {
            assert index < end : "index >= end";
            assert objects[index] != null : "objects[index] == null";
            final Entry entry =
                new Entry(index, objects[index]);
            index++;
            skipUnusedSlots();
            return entry;
        }

        private void skipUnusedSlots() {
            while (index < end && objects[index] == null) {
                index++;
            }
        }
    }

    /**
     * An individual - index mapping.
     */
    public static class Entry {
        public final int index;
        public final Individual individual;
        public Entry(int index, Individual individual) {
            this.index = index;
            this.individual = individual;
        }
    }

}
