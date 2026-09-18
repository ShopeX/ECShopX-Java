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

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * 腾讯云 COS 存储驱动：预签名 URL 与上传凭证等。
 */
public class CosStorageDriver implements StorageDriver {

	private final String secretId;
	private final String secretKey;
	private final String region;
	private final String bucket;
	private final String cdn;

	public CosStorageDriver(String appId, String secretId, String secretKey,
			String region, String bucket, String cdn) {
		this.secretId = secretId;
		this.secretKey = secretKey;
		this.region = region;
		this.bucket = bucket;
		this.cdn = cdn;
	}

	@Override
	public void put(String path, byte[] contents) {
		throw new StorageException("腾讯云COS对象写入尚未在本驱动实现");
	}

	@Override
	public byte[] get(String path) {
		throw new StorageException("腾讯云COS驱动尚未实现");
	}

	@Override
	public void delete(String path) {
		throw new StorageException("腾讯云COS驱动尚未实现");
	}

	@Override
	public boolean exists(String path) {
		throw new StorageException("腾讯云COS驱动尚未实现");
	}

	@Override
	public String url(String path) {
		if (cdn != null && !cdn.isEmpty()) {
			String base = cdn.endsWith("/") ? cdn.substring(0, cdn.length() - 1) : cdn;
			return base + "/" + path;
		}
		return String.format("https://%s.cos.%s.myqcloud.com/%s", bucket, region, path);
	}

	@Override
	public String privateDownloadUrl(String path, int expires) {
		throw new StorageException("腾讯云COS驱动尚未实现");
	}

	@Override
	public String driverName() {
		return "cosv5";
	}

	public void fillCosUploadToken(LinkedHashMap<String, Object> token, String key) {
		COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
		ClientConfig clientConfig = new ClientConfig(new Region(region));
		COSClient client = new COSClient(cred, clientConfig);
		try {
			Date expiration = new Date(System.currentTimeMillis() + 3600_000L);
			GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, key, HttpMethodName.PUT);
			req.setExpiration(expiration);
			token.put("url", client.generatePresignedUrl(req).toString());
		} finally {
			client.shutdown();
		}
	}
}
