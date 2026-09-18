package cn.shopex.ecshopx.espier.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * OSS 驱动集成测试 — 需要真实 OSS 配置才能运行。
 * 通过环境变量注入密钥，未配置则跳过（避免真实密钥入库）：
 *   OSS_ACCESS_KEY / OSS_SECRET_KEY / OSS_ENDPOINT / OSS_IMAGE_BUCKET / OSS_FILE_BUCKET
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OssIntegrationTest {

	private FileStorageService service;
	private String uploadedKey;

	@BeforeAll
	void setUp() {
		String accessKey = System.getenv("OSS_ACCESS_KEY");
		String secretKey = System.getenv("OSS_SECRET_KEY");
		Assumptions.assumeTrue(
				accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty(),
				"未设置 OSS_ACCESS_KEY/OSS_SECRET_KEY，跳过 OSS 集成测试");
		String endpoint = env("OSS_ENDPOINT", "https://oss-cn-shanghai.aliyuncs.com");
		String imageBucket = env("OSS_IMAGE_BUCKET", "shopex-onex-yundian-image");
		String fileBucket = env("OSS_FILE_BUCKET", "shopex-onex-yundian-file");

		StorageProperties props = new StorageProperties();
		props.setDriver("oss");
		props.setProjectName("java-test");

		StorageProperties.Oss oss = new StorageProperties.Oss();
		oss.setAccessKey(accessKey);
		oss.setSecretKey(secretKey);

		StorageProperties.Oss.DiskEndpoint fileDisk = new StorageProperties.Oss.DiskEndpoint();
		fileDisk.setEndpoint(endpoint);
		fileDisk.setBucket(fileBucket);
		oss.setFile(fileDisk);

		StorageProperties.Oss.DiskEndpoint imageDisk = new StorageProperties.Oss.DiskEndpoint();
		imageDisk.setEndpoint(endpoint);
		imageDisk.setBucket(imageBucket);
		oss.setImage(imageDisk);

		StorageProperties.Oss.DiskEndpoint videoDisk = new StorageProperties.Oss.DiskEndpoint();
		videoDisk.setEndpoint(endpoint);
		videoDisk.setBucket(imageBucket);
		oss.setVideo(videoDisk);

		props.setOss(oss);

		StorageFactory factory = new StorageFactory(props);
		service = new FileStorageService(factory);
	}

	@Test
	@Order(1)
	@DisplayName("OSS 上传文件")
	void testOssUpload() {
		byte[] content = "hello oss from java test".getBytes(StandardCharsets.UTF_8);
		Map<String, Object> result = service.upload("file", 1L, "test", "oss-test.txt", content);

		assertEquals("oss", result.get("driver"));
		@SuppressWarnings("unchecked")
		Map<String, Object> token = (Map<String, Object>) result.get("token");
		uploadedKey = (String) token.get("key");
		assertNotNull(uploadedKey);
		System.out.println("OSS uploaded key: " + uploadedKey);
		System.out.println("OSS url: " + token.get("domain"));
	}

	@Test
	@Order(2)
	@DisplayName("OSS 读取文件")
	void testOssGet() {
		assertNotNull(uploadedKey, "需要先执行上传测试");
		byte[] data = service.get("file", uploadedKey);
		String content = new String(data, StandardCharsets.UTF_8);
		assertEquals("hello oss from java test", content);
		System.out.println("OSS read content: " + content);
	}

	@Test
	@Order(3)
	@DisplayName("OSS 获取私有下载URL")
	void testOssPrivateUrl() {
		assertNotNull(uploadedKey, "需要先执行上传测试");
		String downloadUrl = service.privateDownloadUrl("file", uploadedKey, 3600);
		assertNotNull(downloadUrl);
		assertTrue(downloadUrl.startsWith("https://") || downloadUrl.startsWith("http://"));
		System.out.println("OSS private download URL: " + downloadUrl);
	}

	@Test
	@Order(4)
	@DisplayName("OSS 获取公开URL")
	void testOssPublicUrl() {
		assertNotNull(uploadedKey, "需要先执行上传测试");
		String url = service.url("file", uploadedKey);
		assertNotNull(url);
		assertTrue(url.contains("shopex-onex-yundian-file"));
		System.out.println("OSS public URL: " + url);
	}

	@Test
	@Order(5)
	@DisplayName("OSS 上传图片到 image bucket")
	void testOssUploadImage() {
		byte[] fakeImage = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
		Map<String, Object> result = service.upload("image", 1L, "item", "test-img.png", fakeImage);
		assertEquals("oss", result.get("driver"));
		@SuppressWarnings("unchecked")
		Map<String, Object> token = (Map<String, Object>) result.get("token");
		String key = (String) token.get("key");
		assertTrue(key.contains("image/"));
		String url = service.url("image", key);
		assertTrue(url.contains("shopex-onex-yundian-image"));
		System.out.println("OSS image uploaded: " + url);
	}

	@Test
	@Order(6)
	@DisplayName("OSS 删除文件")
	void testOssDelete() {
		assertNotNull(uploadedKey, "需要先执行上传测试");
		assertDoesNotThrow(() -> service.delete("file", uploadedKey));
		System.out.println("OSS deleted: " + uploadedKey);
	}

	@Test
	@Order(7)
	@DisplayName("OSS getUploadToken 返回信息")
	void testOssGetUploadToken() {
		Map<String, Object> result = service.getUploadToken("image", 1L, "item", "photo.jpg");
		assertEquals("oss", result.get("driver"));
		@SuppressWarnings("unchecked")
		Map<String, Object> token = (Map<String, Object>) result.get("token");
		assertNotNull(token.get("accessid"));
		assertNotNull(token.get("host"));
		assertTrue(((String) token.get("host")).startsWith("https://"));
		assertTrue(((String) token.get("host")).contains("shopex-onex-yundian-image"));
		assertNotNull(token.get("policy"));
		assertNotNull(token.get("signature"));
		assertNotNull(token.get("expire"));
		Object cbVar = token.get("callback-var");
		assertNotNull(cbVar);
		String dir = (String) token.get("dir");
		assertNotNull(dir);
		assertTrue(dir.contains("java-test/image/1/"));
		assertTrue(dir.contains("/item/"));
		assertTrue(dir.endsWith("photo.jpg"));
		System.out.println("OSS upload token: " + token);
	}

	private static String env(String key, String fallback) {
		String value = System.getenv(key);
		return value != null && !value.isEmpty() ? value : fallback;
	}
}
