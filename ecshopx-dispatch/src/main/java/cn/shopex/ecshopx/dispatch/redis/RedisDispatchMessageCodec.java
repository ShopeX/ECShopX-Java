/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.dispatch.redis;

import cn.shopex.ecshopx.dispatch.DispatchMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RedisDispatchMessageCodec {

    private final ObjectMapper objectMapper;

    public RedisDispatchMessageCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(DispatchMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("encode dispatch message failed", e);
        }
    }

    public DispatchMessage decode(String payload) {
        try {
            return objectMapper.readValue(payload, DispatchMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("decode dispatch message failed", e);
        }
    }
}
