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
package org.redisson.client.protocol.decoder;

import java.math.BigDecimal;

import org.redisson.client.handler.State;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class ScoredSortedSetScanDecoder<T> extends ObjectListReplayDecoder<T> {

    @Override
    public Object decode(ByteBuf buf, State state) {
        return new BigDecimal(buf.toString(CharsetUtil.UTF_8));
    }

    @Override
    public boolean isApplicable(int paramNum, State state) {
        return paramNum % 2 != 0;
    }

}
