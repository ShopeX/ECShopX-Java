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

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.qiniu.util.Auth;
import java.util.LinkedHashMap;

/**
 * 七牛云对象存储驱动：上传凭证与访问域名等。
 */
public class QiniuStorageDriver implements StorageDriver {

	private final String accessKey;
	private final String secretKey;
	private final String bucket;
	private final String domain;
	private final String region;

	public QiniuStorageDriver(String accessKey, String secretKey, String bucket,
			String domain, String region) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.bucket = bucket;
		this.domain = domain;
		this.region = region;
	}

	@Override
	public void put(String path, byte[] contents) {
		throw new StorageException("七牛云对象写入尚未在本驱动实现");
	}

	@Override
	public byte[] get(String path) {
		throw new StorageException("七牛云驱动尚未实现");
	}

	@Override
	public void delete(String path) {
		throw new StorageException("七牛云驱动尚未实现");
	}

	@Override
	public boolean exists(String path) {
		throw new StorageException("七牛云驱动尚未实现");
	}

	@Override
	public String url(String path) {
		if (domain != null && !domain.isEmpty()) {
			String base = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
			return base + "/" + path;
		}
		return path;
	}

	@Override
	public String privateDownloadUrl(String path, int expires) {
		throw new StorageException("七牛云驱动尚未实现");
	}

	@Override
	public String driverName() {
		return "qiniu";
	}

	public void fillQiniuUploadTokenFields(LinkedHashMap<String, Object> token, String key, String fileType) {
		String r = region == null ? "" : region.trim();
		if (r.isEmpty()) {
			throw new ResourceException(fileType + "配置文件错误");
		}
		String upToken = Auth.create(accessKey, secretKey).uploadToken(bucket, key);
		token.put("host", qiniuUploadHost(r));
		token.put("token", upToken);
		token.put("domain", url(""));
		token.put("region", region);
		token.put("key", key);
	}

	private static String qiniuUploadHost(String regionNorm) {
		String n = regionNorm.toLowerCase();
		return switch (n) {
			case "z0", "cn-east-2" -> "https://upload.qiniup.com";
			case "z1" -> "https://upload-z1.qiniup.com";
			case "z2" -> "https://upload-z2.qiniup.com";
			case "na0" -> "https://upload-na0.qiniup.com";
			case "as0" -> "https://upload-as0.qiniup.com";
			case "cn-north-1" -> "https://upload-cn-north-1.qiniup.com";
			default -> "https://upload-" + regionNorm + ".qiniup.com";
		};
	}
}
