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

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 与 {@link ChinaumsDivisionRemoteUploadPort} 两类上送对应；日志 alias 均为 {@code
 * chinaums-division-sftp}（含 uploadData / uploadSign）。
 */
@Slf4j
public class NoopChinaumsDivisionRemoteUploadPort implements ChinaumsDivisionRemoteUploadPort {

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public void uploadData(long companyId, String localRelativeToStorageRoot, String remoteDir, String fileName) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][chinaums-division-sftp] called#{}, args={}",
				n,
				Arrays.toString(
						new Object[] {"uploadData", companyId, localRelativeToStorageRoot, remoteDir, fileName}));
	}

	@Override
	public void uploadSign(long companyId, String dataFileRelativeToStorageRoot, String remoteDir, String dataFileName) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][chinaums-division-sftp] called#{}, args={}",
				n,
				Arrays.toString(
						new Object[] {"uploadSign", companyId, dataFileRelativeToStorageRoot, remoteDir, dataFileName}));
	}
}
