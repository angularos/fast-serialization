/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nustaq.serialization.util;
import java.util.Map;

public class FSTInt2ObjectMap<V> {

    public int mKeys[];
    public Object mValues[];
    public int mNumberOfElements;
    FSTInt2ObjectMap<V> next;
    private static final int GROWFAC = 2;

    public FSTInt2ObjectMap(int initialSize) {
        if (initialSize < 2) {
            initialSize = 2;
        }

        initialSize = FSTObject2IntMap.adjustSize(initialSize * 2);

        mKeys = new int[initialSize];
        mValues = new Object[initialSize];
        mNumberOfElements = 0;
    }

    public int size() {
        return mNumberOfElements + (next != null ? next.size() : 0);
    }

    final public void put(int key, V value) {
        int hash = key & 0x7FFFFFFF;
        if (key == 0 && value == null) {
            throw new RuntimeException("key value pair not supported " + key + " " + value);
        }
        putHash(key, value, hash, this);
    }

    final private static <V> void putHash(int key, V value, int hash, FSTInt2ObjectMap<V> current, FSTInt2ObjectMap<V> parent) {
        while(true){
            if (current.mNumberOfElements * GROWFAC > current.mKeys.length) {
                if (parent != null) {
                    if ((parent.mNumberOfElements + current.mNumberOfElements) * GROWFAC > parent.mKeys.length) {
                        parent.resize(parent.mKeys.length * GROWFAC);
                        current = parent;
                        continue;
                    } else {
                        current.resize(current.mKeys.length * GROWFAC);
                    }
                } else {
                    current.resize(current.mKeys.length * GROWFAC);
                }
            }

            int idx = hash % current.mKeys.length;

            if (current.mKeys[idx] == 0 && current.mValues[idx] == null) // new
            {
                current.mNumberOfElements++;
                current.mValues[idx] = value;
                current.mKeys[idx] = key;
                return;
            } else if (current.mKeys[idx] == key)  // overwrite
            {
                current.mValues[idx] = value;
                return;
            } else {
                if (current.next == null) {
                    int newSiz = current.mNumberOfElements / 3;
                    current.next = new FSTInt2ObjectMap<V>(newSiz);
                }
                parent = current;
                current = current.next;
            }
        }
    }

    final void putHash(int key, V value, int hash, FSTInt2ObjectMap<V> parent) {
        putHash(key, value, hash,this, parent);
    }

    final public V get(int key) {
        int hash = key & 0x7FFFFFFF;
        return getHash(key, hash);
    }

    final V getHash(int key, int hash) {
        final int idx = hash % mKeys.length;

        final int mKey = mKeys[idx];
        final Object mValue = mValues[idx];
        if (mKey == 0 && mValue == null) // not found
        {
//            hit++;
            return null;
        } else if (mKey == key)  // found
        {
//            hit++;
            return (V) mValue;
        } else {
            if (next == null) {
                return null;
            }
//            miss++;
            return next.getHash(key, hash);
        }
    }

    final void resize(int newSize) {
        newSize = FSTObject2IntMap.adjustSize(newSize);
        int[] oldTabKey = mKeys;
        Object[] oldTabVal = mValues;

        mKeys = new int[newSize];
        mValues = new Object[newSize];
        mNumberOfElements = 0;

        for (int n = 0; n < oldTabKey.length; n++) {
            if (oldTabKey[n] != 0 || oldTabVal[n] != null) {
                put(oldTabKey[n], (V) oldTabVal[n]);
            }
        }
        if (next != null) {
            FSTInt2ObjectMap oldNext = next;
            next = null;
            oldNext.rePut(this);
        }
    }

    private void rePut(FSTInt2ObjectMap<V> kfstObject2IntMap) {
        for (int i = 0; i < mKeys.length; i++) {
            int mKey = mKeys[i];
            if (mKey != 0 || mValues[i] != null) {
                kfstObject2IntMap.put(mKey, (V) mValues[i]);
            }
        }
        if (next != null) {
            next.rePut(kfstObject2IntMap);
        }
    }

    public void clear() {
        if (size() == 0)
            return;
        FSTUtil.clear(mKeys);
        FSTUtil.clear(mValues);
        mNumberOfElements = 0;
        if (next != null) {
            next.clear();
        }
    }

}