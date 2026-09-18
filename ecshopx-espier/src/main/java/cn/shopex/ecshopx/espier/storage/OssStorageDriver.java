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
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PolicyConditions;
import com.aliyun.oss.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云 OSS 存储驱动：对象读写、访问 URL 与直传表单签名等。
 */
public class OssStorageDriver implements StorageDriver {

	private final String accessKey;
	private final String secretKey;
	private final String endpoint;
	private final String bucket;
	private final boolean cname;
	private final String domain;

	public OssStorageDriver(String accessKey, String secretKey, String endpoint,
			String bucket, boolean cname, String domain) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.endpoint = endpoint;
		this.bucket = bucket;
		this.cname = cname;
		this.domain = domain;
	}

	private OSS createClient() {
		String ep = cname && domain != null && !domain.isEmpty() ? domain : endpoint;
		return new OSSClientBuilder().build(ep, accessKey, secretKey);
	}

	@Override
	public void put(String path, byte[] contents) {
		OSS client = createClient();
		try {
			client.putObject(new PutObjectRequest(bucket, path, new ByteArrayInputStream(contents)));
		} catch (Exception e) {
			throw new StorageException("OSS文件写入失败: " + path, e);
		} finally {
			client.shutdown();
		}
	}

	@Override
	public byte[] get(String path) {
		OSS client = createClient();
		try {
			OSSObject obj = client.getObject(bucket, path);
			return obj.getObjectContent().readAllBytes();
		} catch (IOException e) {
			throw new StorageException("OSS文件读取失败: " + path, e);
		} finally {
			client.shutdown();
		}
	}

	@Override
	public void delete(String path) {
		OSS client = createClient();
		try {
			client.deleteObject(bucket, path);
		} catch (Exception e) {
			throw new StorageException("OSS文件删除失败: " + path, e);
		} finally {
			client.shutdown();
		}
	}

	@Override
	public boolean exists(String path) {
		OSS client = createClient();
		try {
			return client.doesObjectExist(bucket, path);
		} finally {
			client.shutdown();
		}
	}

	@Override
	public String url(String path) {
		if (cname && domain != null && !domain.isEmpty()) {
			String base = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
			return base + "/" + path;
		}
		String bucketDomain = bucket + "." + endpoint.replaceFirst("^https?://", "");
		String scheme = endpoint.startsWith("https") ? "https" : "http";
		return scheme + "://" + bucketDomain + "/" + path;
	}

	@Override
	public String privateDownloadUrl(String path, int expires) {
		OSS client = createClient();
		try {
			Date expiration = new Date(System.currentTimeMillis() + (long) expires * 1000);
			URL signedUrl = client.generatePresignedUrl(bucket, path, expiration);
			return signedUrl.toString();
		} finally {
			client.shutdown();
		}
	}

	@Override
	public String driverName() {
		return "oss";
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getBucket() {
		return bucket;
	}

	public void fillOssWebPostToken(LinkedHashMap<String, Object> token, String key) {
		String keyPrefix = key.lastIndexOf('/') >= 0 ? key.substring(0, key.lastIndexOf('/') + 1) : "";
		OSS client = createClient();
		try {
			Date expiration = new Date(System.currentTimeMillis() + 3600_000L);
			PolicyConditions conds = new PolicyConditions();
			conds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1_048_576_000L);
			conds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, keyPrefix);
			String postPolicy = client.generatePostPolicy(expiration, conds);
			String encodedPolicy = BinaryUtil.toBase64String(
					postPolicy.getBytes(StandardCharsets.UTF_8));
			String signature = client.calculatePostSignature(postPolicy);
			long expire = expiration.getTime() / 1000L;
			token.clear();
			token.put("accessid", accessKey);
			token.put("host", postUploadHost());
			token.put("policy", encodedPolicy);
			token.put("signature", signature);
			token.put("expire", expire);
			token.put("callback-var", List.of());
			token.put("dir", key);
		} finally {
			client.shutdown();
		}
	}

	public String postUploadHost() {
		String ep = endpoint != null ? endpoint.trim() : "";
		if (ep.isEmpty()) {
			return "https://" + bucket + "/";
		}
		int sep = ep.indexOf("://");
		if (sep < 0) {
			return "https://" + bucket + "." + ep.replaceAll("/+$", "") + "/";
		}
		String scheme = ep.substring(0, sep + 3);
		String hostPart = ep.substring(sep + 3).replaceAll("/+$", "");
		if (bucket != null && hostPart.startsWith(bucket + ".")) {
			return scheme + hostPart + "/";
		}
		return scheme + bucket + "." + hostPart + "/";
	}
}
