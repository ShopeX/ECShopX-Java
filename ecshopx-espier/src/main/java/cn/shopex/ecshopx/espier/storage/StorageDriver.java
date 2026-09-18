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

package cn.shopex.ecshopx.espier.storage;

/**
 * 文件存储驱动抽象接口（对象存储与本地盘统一入口）。
 * 每种云存储（local / oss / qiniu / aws / cos）实现此接口。
 */
public interface StorageDriver {

	/**
	 * 写入文件。
	 *
	 * @param path     存储路径（不含 bucket / 根目录前缀）
	 * @param contents 文件字节
	 */
	void put(String path, byte[] contents);

	/**
	 * 读取文件内容。
	 */
	byte[] get(String path);

	/**
	 * 删除文件。
	 */
	void delete(String path);

	/**
	 * 判断文件是否存在。
	 */
	boolean exists(String path);

	/**
	 * 获取文件的公开可访问 URL（公共读 bucket 或本地 public 目录）。
	 */
	String url(String path);

	/**
	 * 获取私有文件临时下载 URL。
	 *
	 * @param path    文件路径
	 * @param expires 有效期（秒）
	 */
	String privateDownloadUrl(String path, int expires);

	/**
	 * 返回当前驱动名称标识，如 "local"、"oss"、"qiniu"、"aws"、"cosv5"。
	 */
	String driverName();
}
