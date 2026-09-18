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

package cn.shopex.ecshopx.hfpay.service.payment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.hfpay.payment.HfPayPaymentSettingLoadPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfPayPaymentSettingService implements HfPayPaymentSettingLoadPort {

	private static final Logger log = LoggerFactory.getLogger(HfPayPaymentSettingService.class);

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final String storageRoot;

	public HfPayPaymentSettingService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${ecshopx.hfpay.storage-root:}") String storageRoot) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.storageRoot = storageRoot;
	}

	@Override
	public Map<String, Object> loadForCompany(long companyId) {
		if (!StringUtils.hasText(storageRoot)) {
			throw new ResourceException("汇付天下参数未配置");
		}
		String key = "hfPaymentSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("汇付天下参数未配置");
		}
		Map<String, Object> data;
		try {
			data = objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (IOException e) {
			throw new ResourceException("汇付天下参数未配置");
		}
		if (data == null || data.isEmpty()) {
			throw new ResourceException("汇付天下参数未配置");
		}
		ensureCertificateFilesSynced(data);

		String merCustId = String.valueOf(data.get("mer_cust_id")).trim();
		Path root = Path.of(storageRoot).toAbsolutePath().normalize();
		String relBase = "chinapnrPayment/" + merCustId + "/";
		Path pfxPath = root.resolve(Path.of(relBase, "cfca.pfx")).normalize();
		Path caPath = root.resolve(Path.of(relBase, "CFCA_ACS_CA.cer")).normalize();
		Path ocaPath = root.resolve(Path.of(relBase, "CFCA_ACS_OCA31.cer")).normalize();
		data.put("pfx_file_name", "cfca.pfx");
		data.put("pfx_file_url", pfxPath.toString());
		data.put("ca_pfx_file_name", "CFCA_ACS_CA.cer");
		data.put("ca_pfx_file_url", caPath.toString());
		data.put("oca31_pfx_file_name", "CFCA_ACS_OCA31.cer");
		data.put("oca31_pfx_file_url", ocaPath.toString());
		return data;
	}

	/**
	 * 运营端支付配置列表：Redis 空或未配置本地存储根时返回空配置（不抛错）；已进入解析路径时与 {@link #loadForCompany} 同源校验。
	 */
	/**
	 * 管理端读取单渠道汇付配置：Redis 解析失败或证书同步失败均不向外抛错，返回当前可展示的字段集合。
	 */
	public Map<String, Object> loadForAdminPaymentSettingGet(long companyId) {
		String key = "hfPaymentSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> data;
		try {
			data = objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (Exception e) {
			log.warn(
					"hf payment setting json parse failed companyId={}: {} {}",
					companyId,
					e.getClass().getSimpleName(),
					e.getMessage() != null ? e.getMessage() : "");
			return new LinkedHashMap<>();
		}
		if (data == null) {
			return new LinkedHashMap<>();
		}
		try {
			if (StringUtils.hasText(storageRoot)) {
				ensureCertificateFilesSynced(data);
				Object mer = data.get("mer_cust_id");
				if (mer != null && StringUtils.hasText(String.valueOf(mer))) {
					String merCustId = String.valueOf(mer).trim();
					Path root = Path.of(storageRoot).toAbsolutePath().normalize();
					String relBase = "chinapnrPayment/" + merCustId + "/";
					Path pfxPath = root.resolve(Path.of(relBase, "cfca.pfx")).normalize();
					Path caPath = root.resolve(Path.of(relBase, "CFCA_ACS_CA.cer")).normalize();
					Path ocaPath = root.resolve(Path.of(relBase, "CFCA_ACS_OCA31.cer")).normalize();
					data.put("pfx_file_name", "cfca.pfx");
					data.put("pfx_file_url", pfxPath.toString());
					data.put("ca_pfx_file_name", "CFCA_ACS_CA.cer");
					data.put("ca_pfx_file_url", caPath.toString());
					data.put("oca31_pfx_file_name", "CFCA_ACS_OCA31.cer");
					data.put("oca31_pfx_file_url", ocaPath.toString());
				}
			}
		} catch (Exception e) {
			log.warn("hf payment setting certificate sync skipped companyId={}", companyId, e);
		}
		return data;
	}

	public Map<String, Object> loadForAdminPaymentList(long companyId) {
		String key = "hfPaymentSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return new LinkedHashMap<>();
		}
		if (!StringUtils.hasText(storageRoot)) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> data;
		try {
			data = objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (IOException e) {
			throw new ResourceException("汇付天下参数未配置");
		}
		if (data == null || data.isEmpty()) {
			throw new ResourceException("汇付天下参数未配置");
		}
		ensureCertificateFilesSynced(data);
		return data;
	}

	public void applySetPaymentSetting(long companyId, Map<String, Object> mergedRedisPayload) {
		if (!StringUtils.hasText(storageRoot)) {
			throw new ResourceException("汇付天下参数未配置");
		}
		Object mer = mergedRedisPayload.get("mer_cust_id");
		if (mer == null || !StringUtils.hasText(String.valueOf(mer))) {
			throw new ResourceException("汇付天下参数未配置");
		}
		String redisKey = "hfPaymentSetting:" + sha1Hex(String.valueOf(companyId));
		try {
			companysRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(mergedRedisPayload));
		} catch (JsonProcessingException e) {
			throw new ResourceException("汇付天下参数未配置");
		}
		String merCustId = String.valueOf(mer).trim();
		companysRedisTemplate.opsForValue().set("hfPayment:companyId:" + merCustId, String.valueOf(companyId));
		ensureCertificateFilesSynced(mergedRedisPayload);
	}

	private void ensureCertificateFilesSynced(Map<String, Object> data) {
		if (!StringUtils.hasText(storageRoot)) {
			throw new ResourceException("汇付天下参数未配置");
		}
		Object mer = data.get("mer_cust_id");
		if (mer == null || !StringUtils.hasText(String.valueOf(mer))) {
			throw new ResourceException("汇付天下参数未配置");
		}
		Object pwd = data.get("pfx_password");
		if (pwd == null || String.valueOf(pwd).isEmpty()) {
			throw new ResourceException("汇付天下参数未配置");
		}
		String merCustId = String.valueOf(mer).trim();
		Path root = Path.of(storageRoot).toAbsolutePath().normalize();
		String relBase = "chinapnrPayment/" + merCustId + "/";
		Path pfxRel = Path.of(relBase, "cfca.pfx");
		Path caRel = Path.of(relBase, "CFCA_ACS_CA.cer");
		Path ocaRel = Path.of(relBase, "CFCA_ACS_OCA31.cer");
		Path pfxPath = root.resolve(pfxRel).normalize();
		Path caPath = root.resolve(caRel).normalize();
		Path ocaPath = root.resolve(ocaRel).normalize();

		Object pfxFileObj = data.get("pfx_file");
		if (pfxFileObj == null || !StringUtils.hasText(String.valueOf(pfxFileObj))) {
			throw new ResourceException("汇付天下参数未配置");
		}
		byte[] pfxDecoded;
		try {
			pfxDecoded = Base64.getDecoder().decode(String.valueOf(pfxFileObj).replaceAll("\\s", ""));
		} catch (IllegalArgumentException e) {
			throw new ResourceException("汇付天下参数未配置");
		}
		syncIfMd5Differs(pfxPath, pfxDecoded);

		byte[] caBytes = toUtf8Bytes(data.get("ca_pfx_file"));
		byte[] ocaBytes = toUtf8Bytes(data.get("oca31_pfx_file"));
		if (caBytes.length == 0 || ocaBytes.length == 0) {
			throw new ResourceException("汇付天下参数未配置");
		}
		syncIfMd5Differs(caPath, caBytes);
		syncIfMd5Differs(ocaPath, ocaBytes);
	}

	private static byte[] toUtf8Bytes(Object v) {
		if (v == null) {
			return new byte[0];
		}
		return String.valueOf(v).getBytes(StandardCharsets.UTF_8);
	}

	private void syncIfMd5Differs(Path absoluteFile, byte[] newContent) {
		try {
			Files.createDirectories(absoluteFile.getParent());
			String newMd5 = md5Hex(newContent);
			if (Files.isRegularFile(absoluteFile)) {
				byte[] existing = Files.readAllBytes(absoluteFile);
				if (newMd5.equals(md5Hex(existing))) {
					return;
				}
			}
			Files.write(absoluteFile, newContent);
		} catch (IOException e) {
			throw new ResourceException("汇付天下参数未配置");
		}
	}

	private static String md5Hex(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(data);
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
