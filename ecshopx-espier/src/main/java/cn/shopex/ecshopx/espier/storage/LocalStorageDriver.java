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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地文件存储驱动。
 * 文件写入到 root 目录下，通过配置的 url 前缀生成可访问地址。
 */
public class LocalStorageDriver implements StorageDriver {

	private final Path root;
	private final String urlPrefix;

	public LocalStorageDriver(String root, String urlPrefix) {
		this.root = Path.of(root);
		this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
		try {
			Files.createDirectories(this.root);
		} catch (IOException e) {
			throw new IllegalStateException("无法创建本地存储目录: " + root, e);
		}
	}

	@Override
	public void put(String path, byte[] contents) {
		Path target = root.resolve(path);
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, contents);
		} catch (IOException e) {
			throw new StorageException("本地文件写入失败: " + path, e);
		}
	}

	@Override
	public byte[] get(String path) {
		Path target = root.resolve(path);
		try {
			return Files.readAllBytes(target);
		} catch (IOException e) {
			throw new StorageException("本地文件读取失败: " + path, e);
		}
	}

	@Override
	public void delete(String path) {
		Path target = root.resolve(path);
		try {
			Files.deleteIfExists(target);
		} catch (IOException e) {
			throw new StorageException("本地文件删除失败: " + path, e);
		}
	}

	@Override
	public boolean exists(String path) {
		return Files.exists(root.resolve(path));
	}

	@Override
	public String url(String path) {
		return urlPrefix + "/" + path;
	}

	@Override
	public String privateDownloadUrl(String path, int expires) {
		return url(path);
	}

	@Override
	public String driverName() {
		return "local";
	}
}
