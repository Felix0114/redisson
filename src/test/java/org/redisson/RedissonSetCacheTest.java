package org.redisson;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.codec.MsgPackJacksonCodec;
import org.redisson.core.RSetCache;

public class RedissonSetCacheTest extends BaseTest {

    public static class SimpleBean implements Serializable {

        private Long lng;

        public Long getLng() {
            return lng;
        }

        public void setLng(Long lng) {
            this.lng = lng;
        }

    }

    @Test
    public void testAddBean() throws InterruptedException, ExecutionException {
        SimpleBean sb = new SimpleBean();
        sb.setLng(1L);
        RSetCache<SimpleBean> set = redisson.getSetCache("simple");
        set.add(sb);
        Assert.assertEquals(sb.getLng(), set.iterator().next().getLng());
    }

    @Test
    public void testAddExpire() throws InterruptedException, ExecutionException {
        RSetCache<String> set = redisson.getSetCache("simple3");
        set.add("123", 1, TimeUnit.SECONDS);
        Assert.assertThat(set, Matchers.contains("123"));

        Thread.sleep(1000);

        Assert.assertFalse(set.contains("123"));

        Thread.sleep(50);

        Assert.assertEquals(0, set.size());
    }

    @Test
    public void testAddExpireTwise() throws InterruptedException, ExecutionException {
        RSetCache<String> set = redisson.getSetCache("simple31");
        set.add("123", 1, TimeUnit.SECONDS);
        Thread.sleep(1000);

        Assert.assertFalse(set.contains("123"));

        Thread.sleep(50);

        Assert.assertEquals(0, set.size());

        set.add("4341", 1, TimeUnit.SECONDS);
        Thread.sleep(1000);

        Assert.assertFalse(set.contains("4341"));

        // can't be evicted due to 1 sec delay
        Assert.assertEquals(1, set.size());
    }

    @Test
    public void testExpireOverwrite() throws InterruptedException, ExecutionException {
        RSetCache<String> set = redisson.getSetCache("simple");
        set.add("123", 1, TimeUnit.SECONDS);

        Thread.sleep(800);

        set.add("123", 1, TimeUnit.SECONDS);

        Thread.sleep(800);
        Assert.assertTrue(set.contains("123"));

        Thread.sleep(200);

        Assert.assertFalse(set.contains("123"));
    }

    @Test
    public void testRemove() throws InterruptedException, ExecutionException {
        RSetCache<Integer> set = redisson.getSetCache("simple");
        set.add(1, 1, TimeUnit.SECONDS);
        set.add(3, 2, TimeUnit.SECONDS);
        set.add(7, 3, TimeUnit.SECONDS);

        Assert.assertTrue(set.remove(1));
        Assert.assertFalse(set.contains(1));
        Assert.assertThat(set, Matchers.containsInAnyOrder(3, 7));

        Assert.assertFalse(set.remove(1));
        Assert.assertThat(set, Matchers.containsInAnyOrder(3, 7));

        Assert.assertTrue(set.remove(3));
        Assert.assertFalse(set.contains(3));
        Assert.assertThat(set, Matchers.contains(7));
        Assert.assertEquals(1, set.size());
    }

    @Test
    public void testIteratorRemove() throws InterruptedException {
        RSetCache<String> set = redisson.getSetCache("list");
        set.add("1");
        set.add("4", 1, TimeUnit.SECONDS);
        set.add("2");
        set.add("5", 1, TimeUnit.SECONDS);
        set.add("3");

        Thread.sleep(1000);

        for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
            String value = iterator.next();
            if (value.equals("2")) {
                iterator.remove();
            }
        }

        Assert.assertThat(set, Matchers.containsInAnyOrder("1", "3"));

        int iteration = 0;
        for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
            iterator.next();
            iterator.remove();
            iteration++;
        }

        Assert.assertEquals(2, iteration);

        Assert.assertFalse(set.contains("4"));
        Assert.assertFalse(set.contains("5"));

        Thread.sleep(50);

        Assert.assertEquals(0, set.size());
        Assert.assertTrue(set.isEmpty());
    }

    @Test
    public void testIteratorSequence() {
        RSetCache<Long> set = redisson.getSetCache("set");
        for (int i = 0; i < 1000; i++) {
            set.add(Long.valueOf(i));
        }

        Set<Long> setCopy = new HashSet<Long>();
        for (int i = 0; i < 1000; i++) {
            setCopy.add(Long.valueOf(i));
        }

        checkIterator(set, setCopy);
    }

    private void checkIterator(Set<Long> set, Set<Long> setCopy) {
        for (Iterator<Long> iterator = set.iterator(); iterator.hasNext();) {
            Long value = iterator.next();
            if (!setCopy.remove(value)) {
                Assert.fail();
            }
        }

        Assert.assertEquals(0, setCopy.size());
    }

    @Test
    public void testRetainAll() {
        RSetCache<Integer> set = redisson.getSetCache("set");
        for (int i = 0; i < 10000; i++) {
            set.add(i);
            set.add(i*10, 10, TimeUnit.SECONDS);
        }

        Assert.assertTrue(set.retainAll(Arrays.asList(1, 2)));
        Assert.assertThat(set, Matchers.containsInAnyOrder(1, 2));
        Assert.assertEquals(2, set.size());
    }

    @Test
    public void testIteratorRemoveHighVolume() throws InterruptedException {
        RSetCache<Integer> set = redisson.getSetCache("set");
        for (int i = 1; i <= 5000; i++) {
            set.add(i);
            set.add(i*100000, 20, TimeUnit.SECONDS);
        }
        int cnt = 0;

        Iterator<Integer> iterator = set.iterator();
        while (iterator.hasNext()) {
            Integer integer = iterator.next();
            iterator.remove();
            cnt++;
        }
        Assert.assertEquals(10000, cnt);
        Assert.assertEquals(0, set.size());
    }

    @Test
    public void testContainsAll() {
        RSetCache<Integer> set = redisson.getSetCache("set");
        for (int i = 0; i < 200; i++) {
            set.add(i);
        }

        Assert.assertTrue(set.containsAll(Collections.emptyList()));
        Assert.assertTrue(set.containsAll(Arrays.asList(30, 11)));
        Assert.assertFalse(set.containsAll(Arrays.asList(30, 711, 11)));
    }

    @Test
    public void testToArray() throws InterruptedException {
        RSetCache<String> set = redisson.getSetCache("set");
        set.add("1");
        set.add("4");
        set.add("2", 1, TimeUnit.SECONDS);
        set.add("5");
        set.add("3");

        Thread.sleep(1000);

        MatcherAssert.assertThat(Arrays.asList(set.toArray()), Matchers.<Object>containsInAnyOrder("1", "4", "5", "3"));

        String[] strs = set.toArray(new String[0]);
        MatcherAssert.assertThat(Arrays.asList(strs), Matchers.containsInAnyOrder("1", "4", "5", "3"));
    }

    @Test
    public void testContains() throws InterruptedException {
        RSetCache<TestObject> set = redisson.getSetCache("set");

        set.add(new TestObject("1", "2"));
        set.add(new TestObject("1", "2"));
        set.add(new TestObject("2", "3"), 1, TimeUnit.SECONDS);
        set.add(new TestObject("3", "4"));
        set.add(new TestObject("5", "6"));

        Thread.sleep(1000);

        Assert.assertFalse(set.contains(new TestObject("2", "3")));
        Assert.assertTrue(set.contains(new TestObject("1", "2")));
        Assert.assertFalse(set.contains(new TestObject("1", "9")));
    }

    @Test
    public void testDuplicates() {
        RSetCache<TestObject> set = redisson.getSetCache("set");

        set.add(new TestObject("1", "2"));
        set.add(new TestObject("1", "2"));
        set.add(new TestObject("2", "3"));
        set.add(new TestObject("3", "4"));
        set.add(new TestObject("5", "6"));

        Assert.assertEquals(4, set.size());
    }

    @Test
    public void testSize() {
        RSetCache<Integer> set = redisson.getSetCache("set");
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(5);

        Assert.assertEquals(5, set.size());
    }


    @Test
    public void testRetainAllEmpty() {
        RSetCache<Integer> set = redisson.getSetCache("set");
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);

        Assert.assertTrue(set.retainAll(Collections.<Integer>emptyList()));
        Assert.assertEquals(0, set.size());
    }

    @Test
    public void testRetainAllNoModify() {
        RSetCache<Integer> set = redisson.getSetCache("set");
        set.add(1);
        set.add(2);

        Assert.assertFalse(set.retainAll(Arrays.asList(1, 2))); // nothing changed
        Assert.assertThat(set, Matchers.containsInAnyOrder(1, 2));
    }

    @Test
    public void testExpiredIterator() throws InterruptedException {
        RSetCache<String> cache = redisson.getSetCache("simple");
        cache.add("0");
        cache.add("1", 1, TimeUnit.SECONDS);
        cache.add("2", 3, TimeUnit.SECONDS);
        cache.add("3", 4, TimeUnit.SECONDS);
        cache.add("4", 1, TimeUnit.SECONDS);

        Thread.sleep(1000);

        MatcherAssert.assertThat(cache, Matchers.contains("0", "2", "3"));
    }

    @Test
    public void testExpire() throws InterruptedException {
        RSetCache<String> cache = redisson.getSetCache("simple");
        cache.add("8", 1, TimeUnit.SECONDS);

        cache.expire(100, TimeUnit.MILLISECONDS);

        Thread.sleep(500);

        Assert.assertEquals(0, cache.size());
    }

    @Test
    public void testExpireAt() throws InterruptedException {
        RSetCache<String> cache = redisson.getSetCache("simple");
        cache.add("8", 1, TimeUnit.SECONDS);

        cache.expireAt(System.currentTimeMillis() + 100);

        Thread.sleep(500);

        Assert.assertEquals(0, cache.size());
    }

    @Test
    public void testClearExpire() throws InterruptedException {
        RSetCache<String> cache = redisson.getSetCache("simple");
        cache.add("8", 1, TimeUnit.SECONDS);

        cache.expireAt(System.currentTimeMillis() + 100);

        cache.clearExpire();

        Thread.sleep(500);

        Assert.assertEquals(1, cache.size());
    }

    @Test
    public void testScheduler() throws InterruptedException {
        RSetCache<String> cache = redisson.getSetCache("simple33", new MsgPackJacksonCodec());
        Assert.assertFalse(cache.contains("33"));

        Assert.assertTrue(cache.add("33", 5, TimeUnit.SECONDS));

        Thread.sleep(11000);

        Assert.assertEquals(0, cache.size());

    }

}
