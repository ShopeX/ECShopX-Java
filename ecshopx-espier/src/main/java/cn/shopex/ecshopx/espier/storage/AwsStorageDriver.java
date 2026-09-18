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

import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * AWS S3 兼容存储驱动：对象读写与预签名上传 URL 等。
 */
public class AwsStorageDriver implements StorageDriver {

	private final String bucket;
	private final String endpointUrl;
	private final S3Client s3;
	private final S3Presigner presigner;

	public AwsStorageDriver(String accessKey, String secretKey, String region,
			String bucket, String endpoint) {
		this.bucket = bucket;
		this.endpointUrl = endpoint;
		StaticCredentialsProvider creds = StaticCredentialsProvider.create(
				AwsBasicCredentials.create(accessKey, secretKey));
		S3ClientBuilder builder = S3Client.builder()
				.credentialsProvider(creds)
				.region(Region.of(region));
		S3Presigner.Builder presignerBuilder = S3Presigner.builder()
				.credentialsProvider(creds)
				.region(Region.of(region));
		if (endpoint != null && !endpoint.isEmpty()) {
			builder.endpointOverride(URI.create(endpoint));
			presignerBuilder.endpointOverride(URI.create(endpoint));
		}
		this.s3 = builder.build();
		this.presigner = presignerBuilder.build();
	}

	@Override
	public void put(String path, byte[] contents) {
		try {
			s3.putObject(PutObjectRequest.builder().bucket(bucket).key(path).build(),
					RequestBody.fromBytes(contents));
		} catch (Exception e) {
			throw new StorageException("AWS S3文件写入失败: " + path, e);
		}
	}

	@Override
	public byte[] get(String path) {
		try {
			return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(path).build()).asByteArray();
		} catch (Exception e) {
			throw new StorageException("AWS S3文件读取失败: " + path, e);
		}
	}

	@Override
	public void delete(String path) {
		try {
			s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path).build());
		} catch (Exception e) {
			throw new StorageException("AWS S3文件删除失败: " + path, e);
		}
	}

	@Override
	public boolean exists(String path) {
		try {
			s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build());
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		}
	}

	@Override
	public String url(String path) {
		if (endpointUrl != null && !endpointUrl.isEmpty()) {
			String base = endpointUrl.endsWith("/") ? endpointUrl.substring(0, endpointUrl.length() - 1) : endpointUrl;
			return base + "/" + path;
		}
		return path;
	}

	@Override
	public String privateDownloadUrl(String path, int expires) {
		return presigner.presignGetObject(
				GetObjectPresignRequest.builder()
						.signatureDuration(Duration.ofSeconds(expires))
						.getObjectRequest(r -> r.bucket(bucket).key(path))
						.build()
		).url().toString();
	}

	@Override
	public String driverName() {
		return "aws";
	}

	/**
	 * 生成指向指定对象的 PUT 预签名 URL，用于浏览器直传等场景。
	 */
	public String presignPutObjectUrl(String key, int expiresSeconds) {
		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofSeconds(expiresSeconds))
				.putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).build())
				.build();
		return presigner.presignPutObject(presignRequest).url().toString();
	}
}
