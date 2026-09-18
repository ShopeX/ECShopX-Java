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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储总配置，与常见环境变量 DISK_DRIVER / OSS_PROJECT_NAME 及
 * 文件存储各 driver 分组配置（与历史部署的环境变量命名一致）。
 */
@ConfigurationProperties(prefix = "ecshopx.storage")
public class StorageProperties {

	/**
	 * 存储驱动：local / oss / qiniu / aws / cosv5，对应环境变量 DISK_DRIVER。
	 */
	private String driver = "local";

	/**
	 * 项目名称前缀（所有客户共用同一 bucket 时用来隔离），对应 OSS_PROJECT_NAME。
	 */
	private String projectName = "";

	private Local local = new Local();
	private Oss oss = new Oss();
	private Qiniu qiniu = new Qiniu();
	private Aws aws = new Aws();
	private Cos cos = new Cos();

	// ─── getters / setters ───

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public Local getLocal() {
		return local;
	}

	public void setLocal(Local local) {
		this.local = local;
	}

	public Oss getOss() {
		return oss;
	}

	public void setOss(Oss oss) {
		this.oss = oss;
	}

	public Qiniu getQiniu() {
		return qiniu;
	}

	public void setQiniu(Qiniu qiniu) {
		this.qiniu = qiniu;
	}

	public Aws getAws() {
		return aws;
	}

	public void setAws(Aws aws) {
		this.aws = aws;
	}

	public Cos getCos() {
		return cos;
	}

	public void setCos(Cos cos) {
		this.cos = cos;
	}

	// ─── nested config classes ───

	/**
	 * 本地磁盘存储配置。
	 */
	public static class Local {

		private String root = "";

		private String url = "http://localhost:18080/storage";

		public String getRoot() {
			return root;
		}

		public void setRoot(String root) {
			this.root = root;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}

	/**
	 * 阿里云 OSS 配置（OSS_* 系列环境变量）。
	 */
	public static class Oss {

		private String accessKey = "";
		private String secretKey = "";

		private DiskEndpoint file = new DiskEndpoint();
		private DiskEndpoint image = new DiskEndpoint();
		private DiskEndpoint video = new DiskEndpoint();

		public String getAccessKey() {
			return accessKey;
		}

		public void setAccessKey(String accessKey) {
			this.accessKey = accessKey;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}

		public DiskEndpoint getFile() {
			return file;
		}

		public void setFile(DiskEndpoint file) {
			this.file = file;
		}

		public DiskEndpoint getImage() {
			return image;
		}

		public void setImage(DiskEndpoint image) {
			this.image = image;
		}

		public DiskEndpoint getVideo() {
			return video;
		}

		public void setVideo(DiskEndpoint video) {
			this.video = video;
		}

		public static class DiskEndpoint {
			private String endpoint = "";
			private String bucket = "";
			private boolean cname = false;
			private String domain = "";

			public String getEndpoint() {
				return endpoint;
			}

			public void setEndpoint(String endpoint) {
				this.endpoint = endpoint;
			}

			public String getBucket() {
				return bucket;
			}

			public void setBucket(String bucket) {
				this.bucket = bucket;
			}

			public boolean isCname() {
				return cname;
			}

			public void setCname(boolean cname) {
				this.cname = cname;
			}

			public String getDomain() {
				return domain;
			}

			public void setDomain(String domain) {
				this.domain = domain;
			}
		}
	}

	/**
	 * 七牛云配置（QINIU_* 系列环境变量）。
	 */
	public static class Qiniu {

		private String accessKey = "";
		private String secretKey = "";

		private QiniuDisk file = new QiniuDisk();
		private QiniuDisk image = new QiniuDisk();
		private QiniuDisk video = new QiniuDisk();

		public String getAccessKey() {
			return accessKey;
		}

		public void setAccessKey(String accessKey) {
			this.accessKey = accessKey;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}

		public QiniuDisk getFile() {
			return file;
		}

		public void setFile(QiniuDisk file) {
			this.file = file;
		}

		public QiniuDisk getImage() {
			return image;
		}

		public void setImage(QiniuDisk image) {
			this.image = image;
		}

		public QiniuDisk getVideo() {
			return video;
		}

		public void setVideo(QiniuDisk video) {
			this.video = video;
		}

		public static class QiniuDisk {
			private String bucket = "";
			private String domain = "";
			private String region = "z2";

			public String getBucket() {
				return bucket;
			}

			public void setBucket(String bucket) {
				this.bucket = bucket;
			}

			public String getDomain() {
				return domain;
			}

			public void setDomain(String domain) {
				this.domain = domain;
			}

			public String getRegion() {
				return region;
			}

			public void setRegion(String region) {
				this.region = region;
			}
		}
	}

	/**
	 * 亚马逊 S3 配置（AWS_* 系列环境变量）。
	 */
	public static class Aws {

		private String accessKey = "";
		private String secretKey = "";
		private String region = "";
		private String bucket = "";
		private String endpoint = "";
		private String arn = "";

		public String getAccessKey() {
			return accessKey;
		}

		public void setAccessKey(String accessKey) {
			this.accessKey = accessKey;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

		public String getBucket() {
			return bucket;
		}

		public void setBucket(String bucket) {
			this.bucket = bucket;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public void setEndpoint(String endpoint) {
			this.endpoint = endpoint;
		}

		public String getArn() {
			return arn;
		}

		public void setArn(String arn) {
			this.arn = arn;
		}
	}

	/**
	 * 腾讯云 COS 配置（COS_* 系列环境变量）。
	 */
	public static class Cos {

		private String appId = "";
		private String secretId = "";
		private String secretKey = "";

		private CosDisk file = new CosDisk();
		private CosDisk image = new CosDisk();
		private CosDisk video = new CosDisk();

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public String getSecretId() {
			return secretId;
		}

		public void setSecretId(String secretId) {
			this.secretId = secretId;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}

		public CosDisk getFile() {
			return file;
		}

		public void setFile(CosDisk file) {
			this.file = file;
		}

		public CosDisk getImage() {
			return image;
		}

		public void setImage(CosDisk image) {
			this.image = image;
		}

		public CosDisk getVideo() {
			return video;
		}

		public void setVideo(CosDisk video) {
			this.video = video;
		}

		public static class CosDisk {
			private String region = "";
			private String bucket = "";
			private String cdn = "";

			public String getRegion() {
				return region;
			}

			public void setRegion(String region) {
				this.region = region;
			}

			public String getBucket() {
				return bucket;
			}

			public void setBucket(String bucket) {
				this.bucket = bucket;
			}

			public String getCdn() {
				return cdn;
			}

			public void setCdn(String cdn) {
				this.cdn = cdn;
			}
		}
	}
}
