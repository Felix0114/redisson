/**
 * Copyright 2014 Nikita Koksharov, Nickolay Borbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.command.CommandExecutor;
import org.redisson.core.RSemaphore;
import org.redisson.pubsub.LockPubSub;

import io.netty.util.concurrent.Future;

/**
 * Distributed implementation of {@link java.util.concurrent.locks.Lock}
 * Implements reentrant lock.<br>
 * Lock will be removed automatically if client disconnects.
 *
 * @author Nikita Koksharov
 *
 */
public class RedissonSemaphore extends RedissonExpirable implements RSemaphore {

    final UUID id;

    public static final Long unlockMessage = 0L;

    private static final LockPubSub PUBSUB = new LockPubSub();

    final CommandExecutor commandExecutor;

    protected RedissonSemaphore(CommandExecutor commandExecutor, String name, UUID id) {
        super(commandExecutor, name);
        this.commandExecutor = commandExecutor;
        this.id = id;
    }

    private String getEntryName() {
        return id + ":" + getName();
    }

    String getChannelName() {
        return "redisson_semaphore__channel__{" + getName() + "}";
    }

    @Override
    public void acquire() throws InterruptedException {
        acquire(1);
    }

    @Override
    public void acquire(int permits) throws InterruptedException {
        if (tryAcquire(permits)) {
            return;
        }

        Future<RedissonLockEntry> future = subscribe().sync();
        try {
            while (true) {
                if (tryAcquire(permits)) {
                    return;
                }

                getEntry().getLatch().acquire();
            }
        } finally {
            unsubscribe(future);
        }
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(int permits) {
        return commandExecutor.evalWrite(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                  "local value = redis.call('get', KEYS[1]); " +
                  "if (value ~= false and value >= ARGV[1]) then " +
                      "redis.call('decrby', KEYS[1], ARGV[1]); " +
                      "return 1; " +
                  "end; " +
                  "return 0;",
                  Collections.<Object>singletonList(getName()), permits);
    }

    @Override
    public boolean tryAcquire(int permits, long waitTime, TimeUnit unit) throws InterruptedException {
        if (tryAcquire(permits)) {
            return true;
        }

        long time = unit.toMillis(waitTime);
        Future<RedissonLockEntry> future = subscribe();
        if (!future.await(time, TimeUnit.MILLISECONDS)) {
            return false;
        }

        try {
            while (true) {
                if (tryAcquire(permits)) {
                    return true;
                }

                if (time <= 0) {
                    return false;
                }

                // waiting for message
                long current = System.currentTimeMillis();

                getEntry().getLatch().tryAcquire(time, TimeUnit.MILLISECONDS);

                long elapsed = System.currentTimeMillis() - current;
                time -= elapsed;
            }
        } finally {
            unsubscribe(future);
        }
    }

    private RedissonLockEntry getEntry() {
        return PUBSUB.getEntry(getEntryName());
    }

    private Future<RedissonLockEntry> subscribe() {
        return PUBSUB.subscribe(getEntryName(), getChannelName(), commandExecutor.getConnectionManager());
    }

    private void unsubscribe(Future<RedissonLockEntry> future) {
        PUBSUB.unsubscribe(future.getNow(), getEntryName(), getChannelName(), commandExecutor.getConnectionManager());
    }

    @Override
    public boolean tryAcquire(long time, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, time, unit);
    }

    @Override
    public void release() {
        release(1);
    }

    @Override
    public void release(int permits) {
        commandExecutor.evalWrite(getName(), StringCodec.INSTANCE, RedisCommands.EVAL_OBJECT,
            "redis.call('incrby', KEYS[1], ARGV[1]); " +
            "redis.call('publish', KEYS[2], ARGV[2]); ",
            Arrays.<Object>asList(getName(), getChannelName()), permits, unlockMessage);
    }

    @Override
    public int availablePermits() {
        Long res = commandExecutor.read(getName(), LongCodec.INSTANCE, RedisCommands.GET, getName());
        if (res == null) {
            return 0;
        }
        return res.intValue();
    }

}
