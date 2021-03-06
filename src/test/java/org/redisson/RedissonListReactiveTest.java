package org.redisson;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.redisson.api.RListReactive;
import org.redisson.client.RedisException;
import org.redisson.core.RMap;

import reactor.rx.Promise;

public class RedissonListReactiveTest extends BaseReactiveTest {

    @Test
    public void testEquals() {
        RListReactive<String> list1 = redisson.getList("list1");
        sync(list1.add("1"));
        sync(list1.add("2"));
        sync(list1.add("3"));

        RListReactive<String> list2 = redisson.getList("list2");
        sync(list2.add("1"));
        sync(list2.add("2"));
        sync(list2.add("3"));

        RListReactive<String> list3 = redisson.getList("list3");
        sync(list3.add("0"));
        sync(list3.add("2"));
        sync(list3.add("3"));

        Assert.assertEquals(list1, list2);
        Assert.assertNotEquals(list1, list3);
    }

    @Test
    public void testHashCode() throws InterruptedException {
        RListReactive<String> list = redisson.getList("list");
        sync(list.add("a"));
        sync(list.add("b"));
        sync(list.add("c"));

        Assert.assertEquals(126145, list.hashCode());
    }

    @Test
    public void testAddByIndex() {
        RListReactive<String> test2 = redisson.getList("test2");
        sync(test2.add("foo"));
        sync(test2.add(0, "bar"));

        MatcherAssert.assertThat(sync(test2), Matchers.contains("bar", "foo"));
    }

    @Test
    public void testAddAllReactive() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        RListReactive<Integer> list2 = redisson.getList("list2");
        Assert.assertEquals(5, sync(list2.addAll(list.iterator())).intValue());
        Assert.assertEquals(5, sync(list2.size()).intValue());
    }

    @Test
    public void testAddAllWithIndex() throws InterruptedException {
        final RListReactive<Long> list = redisson.getList("list");
        final CountDownLatch latch = new CountDownLatch(1);
        list.addAll(Arrays.asList(1L, 2L, 3L)).subscribe(new Promise<Long>() {

            @Override
            public void onNext(Long element) {
                list.addAll(Arrays.asList(1L, 24L, 3L)).subscribe(new Promise<Long>() {
                    @Override
                    public void onNext(Long value) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        Assert.fail(error.getMessage());
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                Assert.fail(error.getMessage());
            }
        });

        latch.await();

        Assert.assertThat(sync(list), Matchers.contains(1L, 2L, 3L, 1L, 24L, 3L));
    }

    @Test
    public void testAdd() throws InterruptedException {
        final RListReactive<Long> list = redisson.getList("list");
        final CountDownLatch latch = new CountDownLatch(1);
        list.add(1L).subscribe(new Promise<Long>() {
            @Override
            public void onNext(Long value) {
                list.add(2L).subscribe(new Promise<Long>() {
                    @Override
                    public void onNext(Long value) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        Assert.fail(error.getMessage());
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                Assert.fail(error.getMessage());
            }
        });

        latch.await();

        Assert.assertThat(sync(list), Matchers.contains(1L, 2L));
    }

    @Test
    public void testLong() {
        RListReactive<Long> list = redisson.getList("list");
        sync(list.add(1L));
        sync(list.add(2L));

        Assert.assertThat(sync(list), Matchers.contains(1L, 2L));
    }

    @Test
    public void testListIteratorIndex() {
        RListReactive<Integer> list = redisson.getList("list2");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));
        sync(list.add(0));
        sync(list.add(7));
        sync(list.add(8));
        sync(list.add(0));
        sync(list.add(10));

        Iterator<Integer> iterator = toIterator(list.iterator());

        Assert.assertTrue(1 == iterator.next());
        Assert.assertTrue(2 == iterator.next());
        Assert.assertTrue(3 == iterator.next());
        Assert.assertTrue(4 == iterator.next());
        Assert.assertTrue(5 == iterator.next());
        Assert.assertTrue(0 == iterator.next());
        Assert.assertTrue(7 == iterator.next());
        Assert.assertTrue(8 == iterator.next());
        Assert.assertTrue(0 == iterator.next());
        Assert.assertTrue(10 == iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testListIteratorPrevious() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));
        sync(list.add(0));
        sync(list.add(7));
        sync(list.add(8));
        sync(list.add(0));
        sync(list.add(10));

        Iterator<Integer> iterator = toIterator(list.descendingIterator());

        Assert.assertTrue(10 == iterator.next());
        Assert.assertTrue(0 == iterator.next());
        Assert.assertTrue(8 == iterator.next());
        Assert.assertTrue(7 == iterator.next());
        Assert.assertTrue(0 == iterator.next());
        Assert.assertTrue(5 == iterator.next());
        Assert.assertTrue(4 == iterator.next());
        Assert.assertTrue(3 == iterator.next());
        Assert.assertTrue(2 == iterator.next());
        Assert.assertTrue(1 == iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testLastIndexOfNone() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Assert.assertEquals(-1, sync(list.lastIndexOf(10)).intValue());
    }

    @Test
    public void testLastIndexOf2() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));
        sync(list.add(0));
        sync(list.add(7));
        sync(list.add(8));
        sync(list.add(0));
        sync(list.add(10));

        int index = sync(list.lastIndexOf(3));
        Assert.assertEquals(2, index);
    }

    @Test
    public void testLastIndexOf1() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));
        sync(list.add(3));
        sync(list.add(7));
        sync(list.add(8));
        sync(list.add(0));
        sync(list.add(10));

        int index = sync(list.lastIndexOf(3));
        Assert.assertEquals(5, index);
    }

    @Test
    public void testLastIndexOf() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));
        sync(list.add(3));
        sync(list.add(7));
        sync(list.add(8));
        sync(list.add(3));
        sync(list.add(10));

        int index = sync(list.lastIndexOf(3));
        Assert.assertEquals(8, index);
    }

    @Test
    public void testIndexOf() {
        RListReactive<Integer> list = redisson.getList("list");
        for (int i = 1; i < 200; i++) {
            sync(list.add(i));
        }

        Assert.assertTrue(55 == sync(list.indexOf(56)));
        Assert.assertTrue(99 == sync(list.indexOf(100)));
        Assert.assertTrue(-1 == sync(list.indexOf(200)));
        Assert.assertTrue(-1 == sync(list.indexOf(0)));
    }

    @Test
    public void testRemove() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Integer val = sync(list.remove(0));
        Assert.assertTrue(1 == val);

        Assert.assertThat(sync(list), Matchers.contains(2, 3, 4, 5));
    }

    @Test
    public void testSet() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        sync(list.set(4, 6));

        Assert.assertThat(sync(list), Matchers.contains(1, 2, 3, 4, 6));
    }

    @Test(expected = RedisException.class)
    public void testSetFail() throws InterruptedException {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        sync(list.set(5, 6));
    }

    @Test
    public void testRemoveAllEmpty() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Assert.assertFalse(sync(list.removeAll(Collections.emptyList())));
        Assert.assertFalse(Arrays.asList(1).removeAll(Collections.emptyList()));
    }

    @Test
    public void testRemoveAll() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Assert.assertFalse(sync(list.removeAll(Collections.emptyList())));
        Assert.assertTrue(sync(list.removeAll(Arrays.asList(3, 2, 10, 6))));

        Assert.assertThat(sync(list), Matchers.contains(1, 4, 5));

        Assert.assertTrue(sync(list.removeAll(Arrays.asList(4))));

        Assert.assertThat(sync(list), Matchers.contains(1, 5));

        Assert.assertTrue(sync(list.removeAll(Arrays.asList(1, 5, 1, 5))));

        Assert.assertEquals(0, sync(list.size()).longValue());
    }

    @Test
    public void testRetainAll() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Assert.assertTrue(sync(list.retainAll(Arrays.asList(3, 2, 10, 6))));

        Assert.assertThat(sync(list), Matchers.contains(2, 3));
        Assert.assertEquals(2, sync(list.size()).longValue());
    }

    @Test
    public void testFastSet() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));

        sync(list.fastSet(0, 3));
        Assert.assertEquals(3, (int)sync(list.get(0)));
    }

    @Test
    public void testRetainAllEmpty() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Assert.assertTrue(sync(list.retainAll(Collections.<Integer>emptyList())));
        Assert.assertEquals(0, sync(list.size()).intValue());
    }

    @Test
    public void testRetainAllNoModify() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));

        Assert.assertFalse(sync(list.retainAll(Arrays.asList(1, 2)))); // nothing changed
        Assert.assertThat(sync(list), Matchers.contains(1, 2));
    }

    @Test(expected = RedisException.class)
    public void testAddAllIndexError() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.addAll(2, Arrays.asList(7, 8, 9)));
    }

    @Test
    public void testAddAllIndex() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Assert.assertEquals(8, sync(list.addAll(2, Arrays.asList(7, 8, 9))).longValue());

        Assert.assertThat(sync(list), Matchers.contains(1, 2, 7, 8, 9, 3, 4, 5));

        sync(list.addAll(sync(list.size())-1, Arrays.asList(9, 1, 9)));

        Assert.assertThat(sync(list), Matchers.contains(1, 2, 7, 8, 9, 3, 4, 9, 1, 9, 5));

        sync(list.addAll(sync(list.size()), Arrays.asList(0, 5)));

        Assert.assertThat(sync(list), Matchers.contains(1, 2, 7, 8, 9, 3, 4, 9, 1, 9, 5, 0, 5));

        Assert.assertEquals(15, sync(list.addAll(0, Arrays.asList(6, 7))).intValue());

        Assert.assertThat(sync(list), Matchers.contains(6,7,1, 2, 7, 8, 9, 3, 4, 9, 1, 9, 5, 0, 5));
    }

    @Test
    public void testAddAll() {
        RListReactive<Integer> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2));
        sync(list.add(3));
        sync(list.add(4));
        sync(list.add(5));

        Assert.assertEquals(8, sync(list.addAll(Arrays.asList(7, 8, 9))).intValue());

        Assert.assertEquals(11, sync(list.addAll(Arrays.asList(9, 1, 9))).intValue());

        Assert.assertThat(sync(list), Matchers.contains(1, 2, 3, 4, 5, 7, 8, 9, 9, 1, 9));
    }

    @Test
    public void testAddAllEmpty() {
        RListReactive<Integer> list = redisson.getList("list");
        Assert.assertEquals(0, sync(list.addAll(Collections.<Integer>emptyList())).intValue());
        Assert.assertEquals(0, sync(list.size()).intValue());
    }

    @Test
    public void testContainsAll() {
        RListReactive<Integer> list = redisson.getList("list");
        for (int i = 0; i < 200; i++) {
            sync(list.add(i));
        }

        Assert.assertTrue(sync(list.containsAll(Arrays.asList(30, 11))));
        Assert.assertFalse(sync(list.containsAll(Arrays.asList(30, 711, 11))));
        Assert.assertTrue(sync(list.containsAll(Arrays.asList(30))));
    }

    @Test
    public void testContainsAllEmpty() {
        RListReactive<Integer> list = redisson.getList("list");
        for (int i = 0; i < 200; i++) {
            sync(list.add(i));
        }

        Assert.assertTrue(sync(list.containsAll(Collections.emptyList())));
        Assert.assertTrue(Arrays.asList(1).containsAll(Collections.emptyList()));
    }

    @Test
    public void testIteratorSequence() {
        RListReactive<String> list = redisson.getList("list2");
        sync(list.add("1"));
        sync(list.add("4"));
        sync(list.add("2"));
        sync(list.add("5"));
        sync(list.add("3"));

        checkIterator(list);
        // to test "memory effect" absence
        checkIterator(list);
    }

    private void checkIterator(RListReactive<String> list) {
        int iteration = 0;
        for (Iterator<String> iterator = toIterator(list.iterator()); iterator.hasNext();) {
            String value = iterator.next();
            String val = sync(list.get(iteration));
            Assert.assertEquals(val, value);
            iteration++;
        }

        Assert.assertEquals(sync(list.size()).intValue(), iteration);
    }


    @Test
    public void testContains() {
        RListReactive<String> list = redisson.getList("list");
        sync(list.add("1"));
        sync(list.add("4"));
        sync(list.add("2"));
        sync(list.add("5"));
        sync(list.add("3"));

        Assert.assertTrue(sync(list.contains("3")));
        Assert.assertFalse(sync(list.contains("31")));
        Assert.assertTrue(sync(list.contains("1")));
    }

//    @Test(expected = RedisException.class)
//    public void testGetFail() {
//        RListReactive<String> list = redisson.getList("list");
//
//        sync(list.get(0));
//    }

    @Test
    public void testAddGet() {
        RListReactive<String> list = redisson.getList("list");
        sync(list.add("1"));
        sync(list.add("4"));
        sync(list.add("2"));
        sync(list.add("5"));
        sync(list.add("3"));

        String val1 = sync(list.get(0));
        Assert.assertEquals("1", val1);

        String val2 = sync(list.get(3));
        Assert.assertEquals("5", val2);
    }

    @Test
    public void testDuplicates() {
        RListReactive<TestObject> list = redisson.getList("list");

        sync(list.add(new TestObject("1", "2")));
        sync(list.add(new TestObject("1", "2")));
        sync(list.add(new TestObject("2", "3")));
        sync(list.add(new TestObject("3", "4")));
        sync(list.add(new TestObject("5", "6")));

        Assert.assertEquals(5, sync(list.size()).intValue());
    }

    @Test
    public void testSize() {
        RListReactive<String> list = redisson.getList("list");

        sync(list.add("1"));
        sync(list.add("2"));
        sync(list.add("3"));
        sync(list.add("4"));
        sync(list.add("5"));
        sync(list.add("6"));
        Assert.assertThat(sync(list), Matchers.contains("1", "2", "3", "4", "5", "6"));

        sync(list.remove("2"));
        Assert.assertThat(sync(list), Matchers.contains("1", "3", "4", "5", "6"));

        sync(list.remove("4"));
        Assert.assertThat(sync(list), Matchers.contains("1", "3", "5", "6"));
    }

    @Test
    public void testCodec() {
        RListReactive<Object> list = redisson.getList("list");
        sync(list.add(1));
        sync(list.add(2L));
        sync(list.add("3"));
        sync(list.add("e"));

        Assert.assertThat(sync(list), Matchers.<Object>contains(1, 2L, "3", "e"));
    }
}
