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
 * 将上传文件落盘到本地可配置根目录下（如 {@code storage/app/public} 下的相对路径，与 PHP
 * storage_path 行为一致）。
 */
public interface ChinaumsDivisionLocalArtifactWriterPort {

	/**
	 * @param relativePathFromStorageRoot 使用 {@code /} 分隔，不以 {@code /} 开头
	 * @param fileContent 与 PHP {@code file_content} 同源字符串
	 */
	void put(String relativePathFromStorageRoot, String fileContent);
}
