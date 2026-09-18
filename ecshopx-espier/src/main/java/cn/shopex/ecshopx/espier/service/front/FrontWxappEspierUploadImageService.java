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

package cn.shopex.ecshopx.espier.service.front;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.espier.storage.StorageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * H5 小程序直传图片：日计数、大小与扩展名校验、写入 image 盘并返回公开 URL。
 * 与 getPicUploadToken 共用 Redis 键 {@value #REDIS_KEY_PREFIX}{@code companyId:userId} 及上限配置 {@code USER_UPLOAD_IMAGE_NUM}。
 */
@Service
public class FrontWxappEspierUploadImageService {

	private static final Logger log = LoggerFactory.getLogger(FrontWxappEspierUploadImageService.class);

	private static final String REDIS_KEY_PREFIX = "uploadImageNum:";
	private static final String STORAGE_FILE_TYPE = "image";
	private static final long MAX_BYTES = 2097152L;
	private static final ZoneId ASIA_SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ASIA_SHANGHAI);
	private static final DateTimeFormatter YMDHIS_COMPACT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ASIA_SHANGHAI);
	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

	private final StringRedisTemplate membersRedis;
	private final FileStorageService fileStorageService;
	private final int userUploadImageNum;

	public FrontWxappEspierUploadImageService(
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis,
			FileStorageService fileStorageService,
			@Value("${USER_UPLOAD_IMAGE_NUM:10}") int userUploadImageNum) {
		this.membersRedis = membersRedis;
		this.fileStorageService = fileStorageService;
		this.userUploadImageNum = userUploadImageNum;
	}

	public Map<String, Object> uploadImage(long companyId, long userId, Map<String, Object> authClaims, MultipartFile file) {
		assertUnderDailyUploadImageLimitAndIncrement(companyId, userId);

		if (file == null) {
			throw new ResourceException("Upload File Error");
		}

		long size = file.getSize();
		byte[] fileBytes = null;
		if (size > MAX_BYTES) {
			throw new ResourceException("图片上传最大为2M");
		}
		if (size < 0) {
			try {
				fileBytes = file.getBytes();
			} catch (IOException e) {
				log.warn("上传失败", e);
				throw new ResourceException("上传失败");
			}
			if (fileBytes.length > MAX_BYTES) {
				throw new ResourceException("图片上传最大为2M");
			}
		}

		String extForStorage = extensionFromOriginalFilename(file.getOriginalFilename());
		String extLower = extForStorage.toLowerCase(Locale.ROOT);
		if (!ALLOWED_EXTENSIONS.contains(extLower)) {
			throw new ResourceException("仅支持jpg，jpeg，png图片的上传");
		}

		String ymdSlash = ZonedDateTime.now(ASIA_SHANGHAI).format(YMD_SLASH);
		String timePart = ZonedDateTime.now(ASIA_SHANGHAI).format(YMDHIS_COMPACT);
		String md5Time = md5Utf8Hex(timePart);

		if (fileBytes == null) {
			try {
				fileBytes = file.getBytes();
			} catch (IOException e) {
				log.warn("上传失败", e);
				throw new ResourceException("上传失败");
			}
		}
		if (fileBytes.length > MAX_BYTES) {
			throw new ResourceException("图片上传最大为2M");
		}
		String md5Content = md5BytesHex(fileBytes);

		String objectKey;
		if (companyIdTruthyForPath(authClaims)) {
			objectKey = Long.toString(companyId) + "/" + ymdSlash + "/" + md5Time + md5Content + "." + extForStorage;
		} else {
			objectKey = ymdSlash + "/" + md5Time + md5Content + "." + extForStorage;
		}

		try {
			fileStorageService.put(STORAGE_FILE_TYPE, objectKey, fileBytes);
		} catch (StorageException e) {
			log.warn("上传失败", e);
			throw new ResourceException("上传失败");
		}

		String publicUrl = fileStorageService.url(STORAGE_FILE_TYPE, objectKey);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("url", publicUrl);
		return result;
	}

	/**
	 * H5 直传凭证：日配额校验并递增 → 校验 filetype → COS 文件名扩展名（按需）→ 生成 token。
	 */
	public Map<String, Object> getPicUploadToken(long companyId, long userId, String filename, String group, String filetype) {
		assertUnderDailyUploadImageLimitAndIncrement(companyId, userId);

		String t = (filetype == null) ? null : filetype.trim();
		boolean supported = t != null && !t.isEmpty() && Set.of("file", "image", "videos").contains(t);
		if (!supported) {
			String suffix = filetype == null ? "" : filetype;
			throw new ResourceException("不支持的文件存储类型" + suffix);
		}
		String normalizedFiletype = t;

		fileStorageService.assertCosNonBlankFilenameHasExtensionIfNeeded(filename);
		return fileStorageService.getUploadToken(normalizedFiletype, companyId, group, filename);
	}

	public Map<String, Object> uploadeLocalImage(long companyId, String fileType, String group, MultipartFile images) {
		String path = fileStorageService.uploadImportLocalRelativePath(fileType, companyId, group, images);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("image_url", path);
		return out;
	}

	private void assertUnderDailyUploadImageLimitAndIncrement(long companyId, long userId) {
		String key = REDIS_KEY_PREFIX + companyId + ":" + userId;
		String raw = membersRedis.opsForValue().get(key);
		long count = 0L;
		if (raw != null && !raw.trim().isEmpty()) {
			try {
				count = Long.parseLong(raw.trim());
			} catch (NumberFormatException e) {
				count = 0L;
			}
		}
		int num = userUploadImageNum;
		if (count >= num) {
			throw new ResourceException("今日图片上传超过限制");
		}
		membersRedis.opsForValue().increment(key);
		membersRedis.expire(key, Duration.ofSeconds(86400));
	}

	private static boolean companyIdTruthyForPath(Map<String, Object> auth) {
		Object v = auth == null ? null : auth.get("company_id");
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() > 0;
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s) || "0".equals(s.trim())) {
				return false;
			}
			try {
				return Long.parseLong(s.trim()) > 0;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		try {
			return Long.parseLong(String.valueOf(v).trim()) > 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static String md5Utf8Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return toLowerHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new StorageException("MD5算法不可用", e);
		}
	}

	private static String md5BytesHex(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(data);
			return toLowerHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new StorageException("MD5算法不可用", e);
		}
	}

	private static String toLowerHex(byte[] digest) {
		StringBuilder hex = new StringBuilder();
		for (byte b : digest) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

	private static String extensionFromOriginalFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isEmpty()) {
			return "";
		}
		int d = originalFilename.lastIndexOf('.');
		if (d < 0 || d >= originalFilename.length() - 1) {
			return "";
		}
		return originalFilename.substring(d + 1);
	}
}
