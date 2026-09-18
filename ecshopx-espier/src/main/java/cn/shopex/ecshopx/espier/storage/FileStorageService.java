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
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储门面服务。
 * 统一封装上传 token 获取、文件直传、URL 查询等核心文件存储操作。
 */
@Service
public class FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
	private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("file", "image", "videos");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	/** Wall-clock zone for the upload-key digest fragment (see {@link #UPLOAD_YMDHIS_FORMATTER}). */
	private static final ZoneId ASIA_SHANGHAI = ZoneId.of("Asia/Shanghai");
	/** Formats {@link ZonedDateTime} as {@code yyyyMMddHHmmss} for the string whose UTF-8 bytes are MD5-hashed in the key. */
	private static final DateTimeFormatter UPLOAD_YMDHIS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private final StorageFactory storageFactory;

	public FileStorageService(StorageFactory storageFactory) {
		this.storageFactory = storageFactory;
	}

	/**
	 * COS 驱动下：当用户传入非空白文件名时，要求包含有效扩展名（与直传凭证生成一致）。
	 */
	public void assertCosNonBlankFilenameHasExtensionIfNeeded(String filename) {
		if ("cosv5".equals(storageFactory.getDriverName())
				&& filename != null
				&& !filename.isBlank()) {
			int d = filename.lastIndexOf('.');
			if (d <= 0 || d == filename.length() - 1) {
				throw new BadRequestException("文件名必须包含有效的扩展名");
			}
			if (filename.substring(d + 1).trim().isEmpty()) {
				throw new BadRequestException("文件名必须包含有效的扩展名");
			}
		}
	}

	/**
	 * 获取图片上传凭证（固定 image 磁盘），含 COS 驱动下对用户文件名的扩展名校验。
	 */
	public Map<String, Object> getPicUploadToken(long companyId, String filename) {
		assertCosNonBlankFilenameHasExtensionIfNeeded(filename);
		return getUploadToken("image", companyId, "", filename);
	}

	/**
	 * 获取上传 token 信息。
	 * 对于 local 驱动无实际 token，返回上传路径和域名信息供前端拼接。
	 * 对于 oss 驱动返回签名配置信息。
	 */
	public Map<String, Object> getUploadToken(String fileType, long companyId, String group, String filename) {
		fileType = requireSupportedFileType(fileType);
		StorageDriver driver = storageFactory.disk(fileType);
		String key = buildUploadKey(fileType, companyId, group, filename);
		String driverName = driver.driverName();

		LinkedHashMap<String, Object> token = new LinkedHashMap<>();
		switch (driverName) {
			case "local" -> {
				token.put("domain", driver.url(""));
				token.put("key", key);
			}
			case "oss" -> {
				OssStorageDriver ossDriver = (OssStorageDriver) driver;
				ossDriver.fillOssWebPostToken(token, key);
			}
			case "aws" -> {
				AwsStorageDriver awsDriver = (AwsStorageDriver) driver;
				token.put("domain", driver.url(""));
				token.put("key", key);
				token.put("url", awsDriver.presignPutObjectUrl(key, 3600));
			}
			case "qiniu" -> {
				QiniuStorageDriver qiniuDriver = (QiniuStorageDriver) driver;
				token.put("domain", driver.url(""));
				token.put("key", key);
				qiniuDriver.fillQiniuUploadTokenFields(token, key, fileType);
			}
			case "cosv5" -> {
				CosStorageDriver cosDriver = (CosStorageDriver) driver;
				token.put("domain", driver.url(""));
				token.put("key", key);
				cosDriver.fillCosUploadToken(token, key);
			}
			default -> throw new BadRequestException("请选择正确的存储系统！");
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("driver", driverName);
		result.put("token", token);
		return result;
	}

	private LinkedHashMap<String, Object> buildPostUploadToken(StorageDriver driver, String key, String fileType) {
		LinkedHashMap<String, Object> token = new LinkedHashMap<>();
		String driverName = driver.driverName();
		switch (driverName) {
			case "local" -> {
				token.put("domain", driver.url(""));
				token.put("key", key);
			}
			case "oss" -> {
				OssStorageDriver oss = (OssStorageDriver) driver;
				oss.fillOssWebPostToken(token, key);
			}
			case "aws" -> {
				AwsStorageDriver aws = (AwsStorageDriver) driver;
				token.put("domain", driver.url(""));
				token.put("key", key);
				token.put("url", aws.presignPutObjectUrl(key, 3600));
			}
			case "qiniu" -> {
				QiniuStorageDriver q = (QiniuStorageDriver) driver;
				token.put("domain", driver.url(""));
				token.put("key", key);
				q.fillQiniuUploadTokenFields(token, key, fileType);
			}
			case "cosv5" -> {
				CosStorageDriver cos = (CosStorageDriver) driver;
				token.put("domain", driver.url(""));
				token.put("key", key);
				cos.fillCosUploadToken(token, key);
			}
			default -> throw new BadRequestException("请选择正确的存储系统！");
		}
		return token;
	}

	/**
	 * 管理端图片或视频的 multipart 直传：使用 Espier 专用的 multipart 校验与错误语义。
	 * 对象键基名由原始文件名与请求 {@code Content-Type} 子类型推导；不在基底后再次追加解析出的扩展名段。
	 *
	 * @return 包含 driver 与 token（含 domain、key 及驱动特定字段）的 Map
	 */
	public Map<String, Object> uploadeImage(String fileType, long companyId, String group, MultipartFile file) {
		fileType = requireSupportedFileType(fileType);
		assertEspierUploadeImageMultipart(fileType, file, EspierUploadeImageViolationKind.THROW_RESOURCE);
		StorageDriver driver = storageFactory.disk(fileType);
		String basenameForKey = espierUploadeBasenameForStorageKey(file);
		String baseKey = buildUploadKey(fileType, companyId, group, basenameForKey);
		String key = baseKey;
		try {
			driver.put(key, file.getBytes());
		} catch (StorageException e) {
			log.warn("上传失败", e);
			throw new ResourceException("上传失败");
		} catch (java.io.IOException e) {
			log.warn("上传失败", e);
			throw new ResourceException("上传失败");
		}
		LinkedHashMap<String, Object> token = buildPostUploadToken(driver, key, fileType);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("driver", driver.driverName());
		result.put("token", token);
		return result;
	}

	/**
	 * 本地上传：写入 import 对应磁盘并返回相对对象键（与 {@link #uploadeImage} 键生成与 put 语义一致，无 token 包装）。
	 */
	public String uploadImportLocalRelativePath(String fileType, long companyId, String group, MultipartFile file) {
		String t = (fileType == null) ? null : fileType.trim();
		if (t == null || t.isEmpty()) {
			t = "image";
		}
		if (!Set.of("file", "image", "videos").contains(t)) {
			throw new BadRequestException("不支持的文件存储类型" + t);
		}
		if (!"local".equals(storageFactory.getDriverName())) {
			throw new BadRequestException("本地上传仅支持本地存储配置");
		}
		assertEspierUploadeImageMultipart(t, file, EspierUploadeImageViolationKind.THROW_BAD_REQUEST);
		StorageDriver driver = storageFactory.disk(t);
		String basenameForKey = espierUploadeBasenameForStorageKey(file);
		String key = buildUploadKey(t, companyId, group, basenameForKey);
		try {
			driver.put(key, file.getBytes());
		} catch (StorageException e) {
			log.warn("上传失败", e);
			throw new ResourceException("上传失败");
		} catch (java.io.IOException e) {
			log.warn("上传失败", e);
			throw new ResourceException("上传失败");
		}
		return key;
	}

	/**
	 * multipart 直传写入存储；校验规则与错误类型与 {@link #uploadeImage} 不同。
	 * 对象键：非 {@code local} 驱动时在 {@link #buildUploadKey(String, long, String, String)} 基底后追加
	 * {@link #extFromUploadedFile(MultipartFile)}；{@code local} 驱动始终在基底后追加字面量 {@code .png}
	 * （与实际上传字节 MIME 及 query 文件名扩展名无关）。
	 *
	 * @return 包含 driver 与 token（含 domain、key 及驱动特定字段）的 Map
	 */
	public Map<String, Object> upload(String fileType, long companyId, String group,
			String filename, MultipartFile file) {
		fileType = requireSupportedFileType(fileType);
		if (file == null) {
			throw new BadRequestException("上传文件内容必填");
		}
		if (file.getSize() == 0 || file.isEmpty()) {
			throw new BadRequestException("上传文件内容为空", 400);
		}
		assertUploadMultipart(fileType, file);

		StorageDriver driver = storageFactory.disk(fileType);
		String baseKey = buildUploadKey(fileType, companyId, group, filename);
		final String key;
		if ("local".equals(driver.driverName())) {
			key = baseKey + ".png";
		} else {
			String ext = extFromUploadedFile(file);
			key = baseKey + (StringUtils.hasText(ext) ? "." + ext : "");
		}
		try {
			driver.put(key, file.getBytes());
		} catch (StorageException e) {
			log.warn("上传失败", e);
			throw new ResourceException("上传失败");
		} catch (java.io.IOException e) {
			log.warn("上传失败", e);
			throw new ResourceException("上传失败");
		}

		LinkedHashMap<String, Object> token = buildPostUploadToken(driver, key, fileType);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("driver", driver.driverName());
		result.put("token", token);
		return result;
	}

	/**
	 * 上传原始字节到存储。
	 */
	public Map<String, Object> upload(String fileType, long companyId, String group,
			String filename, byte[] contents) {
		fileType = requireSupportedFileType(fileType);
		StorageDriver driver = storageFactory.disk(fileType);
		String key = buildUploadKey(fileType, companyId, group, filename);
		driver.put(key, contents);

		Map<String, Object> token = new LinkedHashMap<>();
		token.put("domain", driver.url(""));
		token.put("key", key);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("driver", driver.driverName());
		result.put("token", token);
		return result;
	}

	/**
	 * 获取文件公开 URL。
	 */
	public String url(String fileType, String path) {
		return storageFactory.disk(fileType).url(path);
	}

	/**
	 * 获取文件私有下载 URL。
	 */
	public String privateDownloadUrl(String fileType, String path, int expires) {
		return storageFactory.disk(fileType).privateDownloadUrl(path, expires);
	}

	/**
	 * 写入文件到存储。
	 */
	public void put(String fileType, String path, byte[] contents) {
		storageFactory.disk(fileType).put(path, contents);
	}

	/**
	 * 读取文件。
	 */
	public byte[] get(String fileType, String path) {
		return storageFactory.disk(fileType).get(path);
	}

	/**
	 * 删除文件。
	 */
	public void delete(String fileType, String path) {
		storageFactory.disk(fileType).delete(path);
	}

	/**
	 * 构建上传文件在存储中的对象键（四参基底，不含实际上传文件的扩展名后缀）。
	 * 路径形如 {@code {projectName}/{fileType}/{companyId}/{yyyy/MM/dd}[/{group}]/{digestHex}{filename}}，
	 * 其中 {@code digestHex} 为当前 wall-clock 在 {@link #ASIA_SHANGHAI} 下按 {@link #UPLOAD_YMDHIS_FORMATTER}
	 * 格式化成 {@code yyyyMMddHHmmss} 的字符串经 UTF-8 编码后做 MD5 的 32 位小写十六进制，与 {@code filename} 直接拼接；
	 * {@code projectName} 为空时省略该段及前导斜杠；{@code filename} 为空时用 32 位小写十六进制 UUID（无连字符）。
	 * {@link #upload(String, long, String, String, MultipartFile)}：{@code local} 下在基底后固定追加 {@code .png}，
	 * 其它驱动在基底后追加 {@code .}{@link #extFromUploadedFile(MultipartFile)}（可能为空）。
	 * {@link #uploadeImage} 将 {@link #espierUploadeBasenameForStorageKey(MultipartFile)} 的结果作为本方法的 {@code filename}，
	 * 不在此再次追加扩展名。
	 */
	String buildUploadKey(String fileType, long companyId, String group, String filename) {
		if (filename == null || filename.isEmpty()) {
			filename = UUID.randomUUID().toString().replace("-", "");
		}

		String ymdHis = ZonedDateTime.now(ASIA_SHANGHAI).format(UPLOAD_YMDHIS_FORMATTER);
		StringBuilder sb = new StringBuilder();
		String projectName = storageFactory.getProjectName();
		if (StringUtils.hasText(projectName)) {
			sb.append(projectName).append("/");
		}
		sb.append(fileType).append("/")
				.append(companyId).append("/")
				.append(LocalDate.now().format(DATE_FMT));
		if (StringUtils.hasText(group)) {
			sb.append("/").append(group);
		}
		sb.append("/").append(md5Hex(ymdHis)).append(filename);
		return sb.toString();
	}

	/**
	 * 返回 {@code Content-Type} 头中的子类型段（去除参数后按 {@code /} 分割的后段，已 trim，不转小写），
	 * 例如 {@code image/png} 对应 {@code png}。头缺失或格式不当时返回空字符串。
	 */
	private static String mimeSubtypeFromContentType(MultipartFile file) {
		if (file == null) {
			return "";
		}
		String ct = file.getContentType();
		if (ct == null || ct.isBlank()) {
			return "";
		}
		String mainPart = ct.split(";", 2)[0].trim();
		int slash = mainPart.indexOf('/');
		if (slash <= 0 || slash == mainPart.length() - 1) {
			return "";
		}
		return mainPart.substring(slash + 1).trim();
	}

	/**
	 * 为 {@link #uploadeImage} 生成传入 {@link #buildUploadKey} 的文件名段：以客户端原始文件名为基础，
	 * 若存在 {@code Content-Type} 子类型且文件名未以 {@code .子类型} 结尾，则在末尾追加该后缀一次。
	 */
	private static String espierUploadeBasenameForStorageKey(MultipartFile file) {
		String clientName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "";
		String ext = mimeSubtypeFromContentType(file);
		if (!StringUtils.hasText(ext)) {
			return StringUtils.hasText(clientName) ? clientName : null;
		}
		String suffix = "." + ext;
		if (!StringUtils.hasText(clientName)) {
			return null;
		}
		if (clientName.length() < suffix.length()
				|| !clientName.regionMatches(clientName.length() - suffix.length(), suffix, 0, suffix.length())) {
			return clientName + suffix;
		}
		return clientName;
	}

	/**
	 * 解析 multipart 的扩展名段（不含点，小写）：优先 {@code originalFilename} 最后一个 {@code .} 之后；
	 * 无显式扩展名时回退为 {@code Content-Type} 子类型。
	 */
	private static String extFromUploadedFile(MultipartFile file) {
		if (file == null) {
			return "";
		}
		String name = file.getOriginalFilename();
		if (StringUtils.hasText(name)) {
			int d = name.lastIndexOf('.');
			if (d >= 0 && d < name.length() - 1) {
				String ext = name.substring(d + 1).trim();
				return StringUtils.hasText(ext) ? ext.toLowerCase() : "";
			}
			return "";
		}
		String ct = file.getContentType();
		if (ct == null || ct.isBlank()) {
			return "";
		}
		String mainPart = ct.split(";", 2)[0].trim();
		int slash = mainPart.indexOf('/');
		if (slash <= 0 || slash == mainPart.length() - 1) {
			return "";
		}
		return mainPart.substring(slash + 1).trim().toLowerCase();
	}

	private static String normalizeFileType(String fileType) {
		if (fileType == null) {
			return null;
		}
		String t = fileType.trim();
		return t.isEmpty() ? null : t;
	}

	private String requireSupportedFileType(String fileType) {
		String t = normalizeFileType(fileType);
		if (t == null) {
			throw new ResourceException("不支持的文件存储类型");
		}
		if (!SUPPORTED_FILE_TYPES.contains(t)) {
			throw new ResourceException("不支持的文件存储类型" + t);
		}
		return t;
	}

	private enum EspierUploadeImageViolationKind {
		THROW_RESOURCE,
		THROW_BAD_REQUEST
	}

	private void throwEspierUploadeImageMultipartViolation(EspierUploadeImageViolationKind kind, String message) {
		switch (kind) {
			case THROW_RESOURCE -> throw new ResourceException(message);
			case THROW_BAD_REQUEST -> throw new BadRequestException(message);
		}
	}

	private void assertEspierUploadeImageMultipart(String fileType, MultipartFile file, EspierUploadeImageViolationKind kind) {
		if ("file".equals(fileType)) {
			throwEspierUploadeImageMultipartViolation(kind, "不支持的文件存储类型file");
		}
		String ct = file.getContentType();
		if (ct == null || ct.isBlank()) {
			throwEspierUploadeImageMultipartViolation(kind, "不支持的图片存储类型");
		}
		String mainPart = ct.split(";", 2)[0].trim();
		int slash = mainPart.indexOf('/');
		if (slash <= 0 || slash == mainPart.length() - 1) {
			throwEspierUploadeImageMultipartViolation(kind, "不支持的图片存储类型");
		}
		String primary = mainPart.substring(0, slash).trim().toLowerCase();
		String sub = mainPart.substring(slash + 1).trim().toLowerCase();
		if (primary.isEmpty() || sub.isEmpty()) {
			throwEspierUploadeImageMultipartViolation(kind, "不支持的图片存储类型");
		}
		if ("image".equals(fileType)) {
			if (!"image".equals(primary)) {
				throwEspierUploadeImageMultipartViolation(kind, "不支持的图片存储类型" + mainPart);
			}
			if (!Set.of("jpeg", "jpg", "png", "gif").contains(sub)) {
				throwEspierUploadeImageMultipartViolation(kind, "不支持的图片存储类型" + mainPart);
			}
			if (file.getSize() > 2L * 1024 * 1024) {
				throwEspierUploadeImageMultipartViolation(kind, "图片大小超过" + (2L * 1024 * 1024));
			}
		} else if ("videos".equals(fileType)) {
			if (!"video".equals(primary)) {
				throwEspierUploadeImageMultipartViolation(kind, "不支持的图片存储类型" + mainPart);
			}
			if (!"mp4".equals(sub)) {
				throwEspierUploadeImageMultipartViolation(kind, "不支持的图片存储类型" + mainPart);
			}
			if (file.getSize() > 50L * 1024 * 1024) {
				throwEspierUploadeImageMultipartViolation(kind, "图片大小超过" + (50L * 1024 * 1024));
			}
		}
	}

	/**
	 * 直传 multipart 的 MIME 与大小校验；{@code file} 盘恒拒绝。
	 */
	private void assertUploadMultipart(String fileType, MultipartFile file) {
		if ("file".equals(fileType)) {
			throw new ResourceException("请上传正确格式或标准大小文件");
		}
		if (file.getSize() == 0 || file.isEmpty()) {
			throw new BadRequestException("上传文件内容为空", 400);
		}
		String ct = file.getContentType();
		if (ct == null || ct.isBlank()) {
			throw new BadRequestException("mineType错误", 400);
		}
		String mainPart = ct.split(";", 2)[0].trim();
		int slash = mainPart.indexOf('/');
		if (slash <= 0 || slash == mainPart.length() - 1) {
			throw new BadRequestException("mineType错误", 400);
		}
		String primary = mainPart.substring(0, slash).trim().toLowerCase();
		if (primary.isEmpty()) {
			throw new BadRequestException("mineType错误", 400);
		}
		if ("image".equals(fileType)) {
			if (!"image".equals(primary)) {
				throw new BadRequestException(String.format("上传文件只支持%s格式", "image"), 400);
			}
			if (file.getSize() > 2L * 1024 * 1024) {
				throw new BadRequestException("上传文件大小超出限制", 400);
			}
		} else if ("videos".equals(fileType)) {
			if (!"video".equals(primary)) {
				throw new BadRequestException(String.format("上传文件只支持%s格式", "video"), 400);
			}
			if (file.getSize() > 50L * 1024 * 1024) {
				throw new BadRequestException("上传文件大小超出限制", 400);
			}
		}
	}

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new StorageException("MD5算法不可用", e);
		}
	}
}
