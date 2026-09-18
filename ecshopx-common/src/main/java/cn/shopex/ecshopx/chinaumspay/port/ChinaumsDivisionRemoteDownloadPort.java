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

import java.io.IOException;

/**
 * 从银联 UMS SFTP 将 {@code final_{dataFileName}.ret} 拉取到本地可配置根目录下；与 PHP
 * {@code SftpDataService::downftp} 及 {@code downloadFile} 的远程路径同口径。生产为 JSch；test-cron 下为
 * Noop。
 */
public interface ChinaumsDivisionRemoteDownloadPort {

	/**
	 * 确保相对目录存在后，从远端 {@code {remoteDir}/final_{dataFileName}.ret} 下载到
	 * <code>storage</code> 根下 {@code {localDirRelative}/final_{dataFileName}.ret}。
	 */
	void downloadFinalRetToStorage(
			long companyId, String localDirRelative, String remoteDir, String dataFileName) throws IOException;
}
