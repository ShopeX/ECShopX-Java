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
 * 与 PHP {@code DivisionSignService::verifySignFile} 同语义：对本地 {@code .ret} 的 MD5 与远端同名
 * {@code .chk} 做验签。test-cron 为 Noop。
 */
public interface ChinaumsDivisionRetSignVerifyPort {

	/**
	 * 将远端 {@code {remoteDir}/final_{dataFileNameBase}.chk} 拉至本地
	 * <code>final_{name}.ret</code> 同目录的 {@code .chk}，用企业公钥校验。
	 *
	 * @param dataFileNameBase 如 {@code 02_xxx.txt}，不含 final_ 与 .ret
	 * @param localRetFileRelative 相对 storage 的 {@code .ret} 路径
	 * @throws IOException 网络/文件错误
	 */
	void verifyDataFileSign(
			long companyId, String localRetFileRelative, String remoteDir, String dataFileNameBase) throws IOException;
}
