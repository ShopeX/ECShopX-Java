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

package cn.shopex.ecshopx.chinaumspay.port;

/**
 * 银联分账/划付：数据文件上送 + 与 PHP {@code DivisionSignService::uploadSignFile} 等价的
 * 签名文件上送。生产实现为 SFTP；test-cron 下为 Noop。
 */
public interface ChinaumsDivisionRemoteUploadPort {

	void uploadData(long companyId, String localRelativeToStorageRoot, String remoteDir, String fileName);

	void uploadSign(long companyId, String dataFileRelativeToStorageRoot, String remoteDir, String dataFileName);
}
