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

package cn.shopex.ecshopx.datacube.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Bloom filter for UV deduplication; bit layout and Lua script match the existing implementation.
 */
@Service
public class UvBloomFilterService {

	private static final int DEFAULT_EXPECTED_ITEMS = 100_000;
	private static final double DEFAULT_FALSE_POSITIVE_RATE = 0.01;

	private static final String LUA = ""
			+ "local key = KEYS[1]\n"
			+ "local k = tonumber(ARGV[1])\n"
			+ "\n"
			+ "-- 检查所有位是否已设置\n"
			+ "for i = 2, k + 1 do\n"
			+ "    if redis.call('GETBIT', key, ARGV[i]) == 0 then\n"
			+ "        -- 有新位未设置，添加用户\n"
			+ "        for j = 2, k + 1 do\n"
			+ "            redis.call('SETBIT', key, ARGV[j], 1)\n"
			+ "        end\n"
			+ "        return 1\n"
			+ "    end\n"
			+ "end\n"
			+ "\n"
			+ "-- 所有位已设置，用户已存在\n"
			+ "return 0\n";

	private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();
	static {
		SCRIPT.setScriptText(LUA);
		SCRIPT.setResultType(Long.class);
	}

	private final StringRedisTemplate sharedStringRedisTemplate;

	public UvBloomFilterService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	/**
	 * @param openId non-null (caller passes "" when absent)
	 * @return true if this open id was treated as a new visitor (Lua returned 1)
	 */
	public boolean checkAndAdd(String monitorId, String sourceId, String openId) {
		String bitKey = "uv_bloom_filter:" + monitorId + ":" + sourceId;
		int size = calculateSize(DEFAULT_EXPECTED_ITEMS, DEFAULT_FALSE_POSITIVE_RATE);
		int hashCount = calculateHashCount(size, DEFAULT_EXPECTED_ITEMS);
		int[] positions = getHashes(openId, size, hashCount);

		List<String> keys = List.of(bitKey);
		List<String> argv = new ArrayList<>(1 + positions.length);
		argv.add(String.valueOf(hashCount));
		for (int p : positions) {
			argv.add(String.valueOf(p));
		}

		Long raw = sharedStringRedisTemplate.execute(SCRIPT, keys, argv.toArray());
		return raw != null && raw == 1L;
	}

	private static int calculateSize(int n, double p) {
		return (int) Math.ceil(-(n * Math.log(p)) / Math.pow(Math.log(2.0), 2.0));
	}

	private static int calculateHashCount(int m, int n) {
		return (int) Math.ceil((m / (double) n) * Math.log(2.0));
	}

	private static int[] getHashes(String str, int size, int hashCount) {
		long hash1 = crc32(str);
		int hash2Signed = fnv1a32Signed(str);
		int[] hashes = new int[hashCount];
		for (int i = 0; i < hashCount; i++) {
			long sum = hash1 + (long) i * hash2Signed;
			long rem = sum % size;
			if (rem < 0) {
				rem = -rem;
			}
			hashes[i] = (int) rem;
		}
		return hashes;
	}

	private static long crc32(String str) {
		CRC32 c = new CRC32();
		c.update(str.getBytes(StandardCharsets.UTF_8));
		return c.getValue() & 0xFFFFFFFFL;
	}

	/**
	 * FNV-1a over UTF-8 bytes; 32-bit state with {@code & 0xFFFFFFFF} after each multiply (保持与原有实现一致).
	 */
	private static int fnv1a32Signed(String str) {
		long hash = 2166136261L;
		long prime = 16777619L;
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		for (byte b : bytes) {
			hash ^= (b & 0xFF);
			hash = (hash * prime) & 0xFFFFFFFFL;
		}
		return (int) hash;
	}
}
