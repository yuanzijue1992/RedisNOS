/*
 *    Copyright 2009-2014 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package RedisORM.cache.decorators;

import RedisORM.cache.Cache;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 最近最少使用缓存
 * 基于 LinkedHashMap 重写其removeEldestEntry 方法实现。
 */
public class LruCache implements Cache {

    private final Cache delegate;

    //额外用了一个map做lru，但是委托的Cache里面其实也是一个map，这样等于用2倍的内存实现lru功能
    private Map<Object, Object> keyMap;

    // 最近最久未被使用的缓存的key
    private Object eldestKey;

    public LruCache(Cache delegate, int size) {
        this.delegate = delegate;
        setSize(size);
    }


    @Override
    public int getSize() {
        return delegate.getSize();
    }

    /**
     * @Description: 设置缓存的大小
     * @Date 2018/9/12 13:12
     * @param size  设置的大小阈值
     * @return void
     */
    public void setSize(final int size) {
        keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                boolean over = size()>size;
                if (over) {
                    //获取需要移除的那个键
                    eldestKey = eldest.getKey();
                }
                return over;
            }
        };
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public void putObject(Object key, Object value) {
        delegate.putObject(key, value);
        //增加新纪录后，判断是否要将最老元素移除
        cycleKeyList(key);
    }

    /**
     * @Description:  根据插入的情况来更新缓存（也就是若需要移除则移除数据）
     * @Date 2018/9/12 13:15
     * @param key 插入的键
     * @return void
     */
    private void cycleKeyList(Object key) {
        // 插入键，在linkedhashmap中，我们值关心键，所以插入的值不重要
        keyMap.put(key, key);
        //keyMap是linkedhashmap，最老的记录已经被移除了，然而这里我们还需要移除被实际保存Cache的中对应的Cache
        if (eldestKey != null) {
            delegate.removeObject(eldestKey);
            eldestKey = null;
        }
    }

    @Override
    public Object getObject(Object key) {
        //get的时候调用一下LinkedHashMap.get，让经常访问的值移动到链表末尾
        keyMap.get(key); //touch
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyMap.clear();
    }

}
