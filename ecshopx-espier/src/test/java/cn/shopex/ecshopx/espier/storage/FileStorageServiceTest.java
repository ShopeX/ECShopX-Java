package cn.shopex.ecshopx.espier.storage;

import static org.junit.jupiter.api.Assertions.*;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

	@Nested
	@DisplayName("Local Driver 测试")
	class LocalDriverTest {

		@TempDir
		Path tempDir;

		private FileStorageService service;

		@BeforeEach
		void setUp() {
			StorageProperties props = new StorageProperties();
			props.setDriver("local");
			props.setProjectName("test-project");
			StorageProperties.Local local = new StorageProperties.Local();
			local.setRoot(tempDir.toString());
			local.setUrl("http://localhost:18080/storage");
			props.setLocal(local);

			StorageFactory factory = new StorageFactory(props);
			service = new FileStorageService(factory);
		}

		@Test
		@DisplayName("getUploadToken 返回 local 驱动信息")
		void testGetUploadToken() {
			Map<String, Object> result = service.getUploadToken("image", 1L, "item", "test.png");
			assertEquals("local", result.get("driver"));
			@SuppressWarnings("unchecked")
			Map<String, Object> token = (Map<String, Object>) result.get("token");
			assertNotNull(token.get("domain"));
			assertNotNull(token.get("key"));
			String key = (String) token.get("key");
			assertTrue(key.startsWith("test-project/image/1/"), "key 应以 project/image/companyId/ 开头");
			assertTrue(key.contains("item"), "key 应包含 group");
			assertTrue(key.contains("test.png"), "key 应包含文件名");
		}

		@Test
		@DisplayName("upload 字节数组成功写入本地文件系统")
		void testUploadBytes() {
			byte[] content = "hello storage".getBytes(StandardCharsets.UTF_8);
			Map<String, Object> result = service.upload("file", 1L, null, "readme.txt", content);
			assertEquals("local", result.get("driver"));

			@SuppressWarnings("unchecked")
			Map<String, Object> token = (Map<String, Object>) result.get("token");
			String key = (String) token.get("key");
			assertNotNull(key);

			Path written = tempDir.resolve(key);
			assertTrue(Files.exists(written), "文件应写入到本地磁盘");
			try {
				String actual = Files.readString(written);
				assertEquals("hello storage", actual);
			} catch (IOException e) {
				fail("读取写入文件失败: " + e.getMessage());
			}
		}

		@Test
		@DisplayName("put / get / delete 基本文件操作")
		void testPutGetDelete() {
			byte[] data = "abc123".getBytes(StandardCharsets.UTF_8);
			service.put("file", "test/demo.txt", data);

			Path target = tempDir.resolve("test/demo.txt");
			assertTrue(Files.exists(target));

			byte[] read = service.get("file", "test/demo.txt");
			assertArrayEquals(data, read);

			service.delete("file", "test/demo.txt");
			assertFalse(Files.exists(target));
		}

		@Test
		@DisplayName("url 返回正确的 HTTP 地址")
		void testUrl() {
			String url = service.url("image", "test-project/image/1/2025/01/01/abc.png");
			assertEquals("http://localhost:18080/storage/test-project/image/1/2025/01/01/abc.png", url);
		}

		@Test
		@DisplayName("不支持的文件类型应抛异常")
		void testUnsupportedFileType() {
			ResourceException ex = assertThrows(ResourceException.class, () ->
					service.getUploadToken("unknown", 1L, null, null));
			assertEquals("不支持的文件存储类型unknown", ex.getMessage());
		}

		@Test
		@DisplayName("getUploadToken 缺失 filetype 抛 ResourceException")
		void testGetUploadTokenNullFileType() {
			ResourceException ex = assertThrows(ResourceException.class, () ->
					service.getUploadToken(null, 1L, null, null));
			assertEquals("不支持的文件存储类型", ex.getMessage());
		}

		@Test
		@DisplayName("getUploadToken 仅空白 filetype 抛 ResourceException")
		void testGetUploadTokenWhitespaceOnlyFileType() {
			ResourceException ex = assertThrows(ResourceException.class, () ->
					service.getUploadToken("   ", 1L, null, null));
			assertEquals("不支持的文件存储类型", ex.getMessage());
		}

		@Test
		@DisplayName("getUploadToken filetype 前后空白 trim 后合法")
		void testGetUploadTokenTrimmedFileType() {
			Map<String, Object> result = service.getUploadToken(" image ", 1L, "item", "test.png");
			assertEquals("local", result.get("driver"));
			@SuppressWarnings("unchecked")
			Map<String, Object> token = (Map<String, Object>) result.get("token");
			String key = (String) token.get("key");
			assertTrue(key.startsWith("test-project/image/1/"), "trim 后应按 image 盘解析");
			assertTrue(key.contains("item"));
			assertTrue(key.contains("test.png"));
		}

		@Test
		@DisplayName("buildUploadKey 格式正确")
		void testBuildUploadKey() {
			String key = service.buildUploadKey("image", 1L, "item", "photo.jpg");
			assertTrue(key.startsWith("test-project/image/1/"));
			assertTrue(key.contains("/item/"));
			assertTrue(key.endsWith("photo.jpg"));
		}

		@Test
		@DisplayName("buildUploadKey 无 group 时不包含 group 目录")
		void testBuildUploadKeyNoGroup() {
			String key = service.buildUploadKey("file", 2L, null, "data.xlsx");
			assertTrue(key.startsWith("test-project/file/2/"));
			assertTrue(key.endsWith("data.xlsx"));
			assertFalse(key.contains("//"));
		}

		@Test
		@DisplayName("uploadeImage 允许 0 字节 multipart，仍校验 MIME")
		void testUploadeImageAllowsZeroBytes() throws IOException {
			MockMultipartFile file = new MockMultipartFile(
					"images", "empty.png", "image/png", new byte[0]);
			Map<String, Object> result = service.uploadeImage("image", 1L, "item", file);
			assertEquals("local", result.get("driver"));
			@SuppressWarnings("unchecked")
			Map<String, Object> token = (Map<String, Object>) result.get("token");
			String key = (String) token.get("key");
			assertNotNull(key);
			Path written = tempDir.resolve(key);
			assertTrue(Files.exists(written));
			assertEquals(0, Files.size(written));
		}
	}

	@Nested
	@DisplayName("OSS Driver 基本构造测试")
	class OssDriverBasicTest {

		@Test
		@DisplayName("OssStorageDriver 构造与 driverName")
		void testOssDriverName() {
			OssStorageDriver driver = new OssStorageDriver(
					"testAK", "testSK",
					"https://oss-cn-shanghai.aliyuncs.com",
					"test-bucket", false, "");
			assertEquals("oss", driver.driverName());
		}

		@Test
		@DisplayName("OssStorageDriver url 非 CNAME 模式")
		void testOssUrlNoCname() {
			OssStorageDriver driver = new OssStorageDriver(
					"testAK", "testSK",
					"https://oss-cn-shanghai.aliyuncs.com",
					"my-bucket", false, "");
			String url = driver.url("image/1/2025/01/01/test.png");
			assertEquals("https://my-bucket.oss-cn-shanghai.aliyuncs.com/image/1/2025/01/01/test.png", url);
		}

		@Test
		@DisplayName("OssStorageDriver url CNAME 模式")
		void testOssUrlWithCname() {
			OssStorageDriver driver = new OssStorageDriver(
					"testAK", "testSK",
					"https://oss-cn-shanghai.aliyuncs.com",
					"my-bucket", true, "https://cdn.example.com");
			String url = driver.url("image/1/2025/01/01/test.png");
			assertEquals("https://cdn.example.com/image/1/2025/01/01/test.png", url);
		}

		@Test
		@DisplayName("StorageFactory 创建 OSS driver 分 fileType")
		void testFactoryCreatesOssDriverPerFileType() {
			StorageProperties props = new StorageProperties();
			props.setDriver("oss");
			props.setProjectName("test");
			StorageProperties.Oss oss = new StorageProperties.Oss();
			oss.setAccessKey("ak");
			oss.setSecretKey("sk");

			StorageProperties.Oss.DiskEndpoint fileDisk = new StorageProperties.Oss.DiskEndpoint();
			fileDisk.setEndpoint("https://oss-cn-shanghai.aliyuncs.com");
			fileDisk.setBucket("file-bucket");
			oss.setFile(fileDisk);

			StorageProperties.Oss.DiskEndpoint imageDisk = new StorageProperties.Oss.DiskEndpoint();
			imageDisk.setEndpoint("https://oss-cn-shanghai.aliyuncs.com");
			imageDisk.setBucket("image-bucket");
			oss.setImage(imageDisk);

			StorageProperties.Oss.DiskEndpoint videoDisk = new StorageProperties.Oss.DiskEndpoint();
			videoDisk.setEndpoint("https://oss-cn-shanghai.aliyuncs.com");
			videoDisk.setBucket("video-bucket");
			oss.setVideo(videoDisk);

			props.setOss(oss);

			StorageFactory factory = new StorageFactory(props);

			StorageDriver fileDrv = factory.disk("file");
			StorageDriver imageDrv = factory.disk("image");
			assertNotSame(fileDrv, imageDrv, "不同 fileType 应得到不同 driver 实例");
			assertEquals("oss", fileDrv.driverName());
			assertEquals("oss", imageDrv.driverName());

			assertTrue(fileDrv.url("a.txt").contains("file-bucket"));
			assertTrue(imageDrv.url("b.png").contains("image-bucket"));
		}
	}

	@Nested
	@DisplayName("Qiniu Driver 测试")
	class QiniuDriverTest {

		@Test
		@DisplayName("region 缺失时异常文案含 filetype")
		void testQiniuRegionMissingMessage() {
			QiniuStorageDriver driver = new QiniuStorageDriver("ak", "sk", "bucket", "https://cdn.example.com", null);
			LinkedHashMap<String, Object> token = new LinkedHashMap<>();
			ResourceException ex = assertThrows(ResourceException.class, () ->
					driver.fillQiniuUploadTokenFields(token, "path/key.png", "file"));
			assertEquals("file配置文件错误", ex.getMessage());
		}
	}
}
