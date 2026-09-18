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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 存储驱动工厂。
 * 根据 StorageProperties 中的 driver 类型和 fileType (file/image/videos)
 * 创建并缓存对应的 StorageDriver 实例。
 */
@Component
public class StorageFactory {

	private static final Logger log = LoggerFactory.getLogger(StorageFactory.class);

	private final StorageProperties props;
	private final Map<String, StorageDriver> cache = new ConcurrentHashMap<>();

	public StorageFactory(StorageProperties props) {
		this.props = props;
	}

	/**
	 * 获取指定 fileType 的存储驱动。
	 *
	 * @param fileType file / image / videos（分别对应通用文件、图片、视频磁盘配置）
	 */
	public StorageDriver disk(String fileType) {
		String driver = props.getDriver();
		String cacheKey = driver + ":" + fileType;
		return cache.computeIfAbsent(cacheKey, k -> createDriver(driver, fileType));
	}

	private StorageDriver createDriver(String driver, String fileType) {
		log.info("创建存储驱动: driver={}, fileType={}", driver, fileType);
		return switch (driver) {
			case "local" -> createLocalDriver();
			case "oss" -> createOssDriver(fileType);
			case "aws" -> createAwsDriver();
			case "qiniu" -> createQiniuDriver(fileType);
			case "cosv5" -> createCosDriver(fileType);
			default -> throw new BadRequestException("请选择正确的存储系统！");
		};
	}

	private StorageDriver createLocalDriver() {
		StorageProperties.Local cfg = props.getLocal();
		return new LocalStorageDriver(cfg.getRoot(), cfg.getUrl());
	}

	private StorageDriver createOssDriver(String fileType) {
		StorageProperties.Oss cfg = props.getOss();
		StorageProperties.Oss.DiskEndpoint ep = switch (fileType) {
			case "file" -> cfg.getFile();
			case "image" -> cfg.getImage();
			case "videos" -> cfg.getVideo();
			default -> throw new StorageException("不支持的文件类型: " + fileType);
		};
		return new OssStorageDriver(cfg.getAccessKey(), cfg.getSecretKey(),
				ep.getEndpoint(), ep.getBucket(), ep.isCname(), ep.getDomain());
	}

	private StorageDriver createAwsDriver() {
		StorageProperties.Aws cfg = props.getAws();
		return new AwsStorageDriver(cfg.getAccessKey(), cfg.getSecretKey(),
				cfg.getRegion(), cfg.getBucket(), cfg.getEndpoint());
	}

	private StorageDriver createQiniuDriver(String fileType) {
		StorageProperties.Qiniu cfg = props.getQiniu();
		StorageProperties.Qiniu.QiniuDisk disk = switch (fileType) {
			case "file" -> cfg.getFile();
			case "image" -> cfg.getImage();
			case "videos" -> cfg.getVideo();
			default -> throw new StorageException("不支持的文件类型: " + fileType);
		};
		return new QiniuStorageDriver(cfg.getAccessKey(), cfg.getSecretKey(),
				disk.getBucket(), disk.getDomain(), disk.getRegion());
	}

	private StorageDriver createCosDriver(String fileType) {
		StorageProperties.Cos cfg = props.getCos();
		StorageProperties.Cos.CosDisk disk = switch (fileType) {
			case "file" -> cfg.getFile();
			case "image" -> cfg.getImage();
			case "videos" -> cfg.getVideo();
			default -> throw new StorageException("不支持的文件类型: " + fileType);
		};
		return new CosStorageDriver(cfg.getAppId(), cfg.getSecretId(), cfg.getSecretKey(),
				disk.getRegion(), disk.getBucket(), disk.getCdn());
	}

	public String getDriverName() {
		return props.getDriver();
	}

	public String getProjectName() {
		return props.getProjectName();
	}
}
