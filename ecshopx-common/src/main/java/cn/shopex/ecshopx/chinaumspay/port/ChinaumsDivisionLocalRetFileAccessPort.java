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
 * 在本地 storage 根下访问日终回盘 {@code .ret} 与目录（exists / 读入 UTF-8）。与 {@code
 * ChinaumsDivisionLocalStorageWriterService} 使用同一可配置根路径。生产为文件系统；test-cron 为 Noop。
 */
public interface ChinaumsDivisionLocalRetFileAccessPort {

	/** 确保相对路径的父目录存在（可递归创建），路径约定与上送落盘相同。 */
	void ensureStorageDirectoryForRelativePath(String fileOrDirRelative) throws IOException;

	/** 相对 storage 根的文件路径是否存在。 */
	boolean exists(String relativeFilePath) throws IOException;

	/** 按 UTF-8 读取相对路径文件全文。 */
	String readStringUtf8(String relativeFilePath) throws IOException;
}
