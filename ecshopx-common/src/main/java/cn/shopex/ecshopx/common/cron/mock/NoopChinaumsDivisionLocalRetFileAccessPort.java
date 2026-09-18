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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalRetFileAccessPort;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ChinaumsDivisionLocalRetFileAccessPort} 的 Noop 实现；阶段 4 通过
 * <code>[cron-mock][chinaums-division-local-ret-file]</code> 做断言。默认 exists=false、read 空串。
 */
@Slf4j
public class NoopChinaumsDivisionLocalRetFileAccessPort implements ChinaumsDivisionLocalRetFileAccessPort {

	private final AtomicInteger callCount = new AtomicInteger();

	/** 可覆盖的固定 exists；null 表示始终 false。 */
	public volatile Boolean fixedExists = null;

	/** 可覆盖的 read 内容。 */
	public volatile String readFixture = "";

	@Override
	public void ensureStorageDirectoryForRelativePath(String fileOrDirRelative) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][chinaums-division-local-ret-file] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {"ensure", fileOrDirRelative}));
	}

	@Override
	public boolean exists(String relativeFilePath) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][chinaums-division-local-ret-file] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {"exists", relativeFilePath}));
		if (fixedExists != null) {
			return fixedExists;
		}
		return false;
	}

	@Override
	public String readStringUtf8(String relativeFilePath) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][chinaums-division-local-ret-file] called#{}, args={}",
				n,
				Arrays.toString(new Object[] {"read", relativeFilePath}));
		return readFixture == null ? "" : readFixture;
	}
}
