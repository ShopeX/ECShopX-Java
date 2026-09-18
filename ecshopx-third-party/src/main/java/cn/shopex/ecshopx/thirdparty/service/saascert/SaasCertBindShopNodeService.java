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

package cn.shopex.ecshopx.thirdparty.service.saascert;

import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import cn.shopex.ecshopx.thirdparty.domain.ShopBind;
import cn.shopex.ecshopx.thirdparty.mapper.ShopBindMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 矩阵绑定节点回打（对齐 PHP {@code CertService::bindShopNode} / {@code certiValidate}）。
 */
@Service
public class SaasCertBindShopNodeService {

	private static final Logger log = LoggerFactory.getLogger(SaasCertBindShopNodeService.class);

	private static final int BIND = 1;
	private static final int UNBIND = 0;

	private final ShopBindMapper shopBindMapper;
	private final StringRedisTemplate prismRedisTemplate;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort;
	private final String storeKey;

	public SaasCertBindShopNodeService(
			ShopBindMapper shopBindMapper,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort,
			@Value("${common.store-key:}") String storeKey) {
		this.shopBindMapper = shopBindMapper;
		this.prismRedisTemplate = prismRedisTemplate;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.companyPassportUidByCompanyIdPort = companyPassportUidByCompanyIdPort;
		this.storeKey = storeKey == null ? "" : storeKey;
	}

	public Map<String, String> certiValidate(Map<String, String> postdata) {
		String sign = makeShopexAc(postdata, storeKey);
		String incoming = postdata == null ? "" : trim(postdata.get("certi_ac"));
		if (StringUtils.hasText(incoming) && incoming.equals(sign)) {
			return Map.of("res", "succ", "msg", "", "info", "");
		}
		return Map.of("res", "fail", "msg", "000001", "info", "You have the different ac!");
	}

	/**
	 * @return 回打明文：{@code succ} / {@code sign error} / {@code node_type is exists} 等
	 */
	public String bindShopNode(long companyId, Map<String, String> data) {
		log.info("saascert bindShopNode companyId={} data={}", companyId, data);
		CertSetting certSetting = loadCertSetting(companyId);
		log.info("saascert bindShopNode certSetting={}", certSetting);

		String sign = data == null ? "" : trim(data.get("certi_ac"));
		String mySign = makeShopexAc(data, certSetting.token());
		if (!StringUtils.hasText(sign) || !sign.equals(mySign)) {
			log.debug("saascert bindShopNode sign error");
			return "sign error";
		}

		String nodeType = trim(data.get("node_type"));
		String nodeId = trim(data.get("node_id"));
		String shopName = trim(data.get("shop_name"));
		String status = trim(data.get("status"));

		if (!StringUtils.hasText(nodeType)) {
			log.warn(
					"saascert bindShopNode missing node_type companyId={} nodeId={} status={} raw={}",
					companyId,
					nodeId,
					status,
					data);
		}

		if ("bind".equals(status)) {
			LambdaQueryWrapper<ShopBind> occupiedQ = new LambdaQueryWrapper<>();
			occupiedQ
					.eq(ShopBind::getCompanyId, companyId)
					.eq(ShopBind::getNodeType, nodeType)
					.eq(ShopBind::getStatus, BIND)
					.last("LIMIT 1");
			if (shopBindMapper.selectOne(occupiedQ) != null) {
				log.debug("saascert bindShopNode node_type {} is exists", nodeType);
				return "node_type is exists";
			}
			saveShopNode(companyId, shopName, nodeId, nodeType, BIND);
			companysRedisTemplate.opsForValue().set(SaasCertErpBindReadService.erpBindRedisKey(companyId), nodeId);
			return "succ";
		}
		if ("unbind".equals(status)) {
			deleteByNodeType(companyId, nodeType);
			companysRedisTemplate.delete(SaasCertErpBindReadService.erpBindRedisKey(companyId));
			return "succ";
		}
		return "succ";
	}

	private void saveShopNode(long companyId, String name, String nodeId, String nodeType, int status) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<ShopBind> q = new LambdaQueryWrapper<>();
		q.eq(ShopBind::getCompanyId, companyId)
				.eq(ShopBind::getNodeId, nodeId)
				.eq(ShopBind::getNodeType, nodeType)
				.last("LIMIT 1");
		ShopBind existing = shopBindMapper.selectOne(q);
		if (existing != null) {
			LambdaUpdateWrapper<ShopBind> update = new LambdaUpdateWrapper<>();
			update.eq(ShopBind::getId, existing.getId())
					.set(ShopBind::getName, name)
					.set(ShopBind::getStatus, status)
					.set(ShopBind::getUpdated, now);
			shopBindMapper.update(null, update);
			return;
		}
		ShopBind row = new ShopBind();
		row.setCompanyId(companyId);
		row.setName(name);
		row.setNodeId(nodeId);
		row.setNodeType(nodeType);
		row.setStatus(status);
		row.setCreated(now);
		row.setUpdated(now);
		shopBindMapper.insert(row);
	}

	private void deleteByNodeType(long companyId, String nodeType) {
		LambdaQueryWrapper<ShopBind> q = new LambdaQueryWrapper<>();
		q.eq(ShopBind::getCompanyId, companyId).eq(ShopBind::getNodeType, nodeType);
		shopBindMapper.delete(q);
	}

	private CertSetting loadCertSetting(long companyId) {
		String shopexUid =
				companyPassportUidByCompanyIdPort.findPassportUid(companyId).orElse("").trim();
		String raw = prismRedisTemplate.opsForValue().get(certRedisKey(shopexUid, companyId));
		String certId = "";
		String nodeId = "";
		String token = "";
		if (StringUtils.hasText(raw)) {
			try {
				JsonNode n = objectMapper.readTree(raw);
				if (n != null && n.isObject()) {
					certId = textOrEmpty(n, "cert_id");
					nodeId = textOrEmpty(n, "node_id");
					token = textOrEmpty(n, "token");
				}
			} catch (Exception ignored) {
				// keep empty defaults
			}
		}
		return new CertSetting(certId, nodeId, token);
	}

	private static String makeShopexAc(Map<String, String> tempArr, String token) {
		TreeMap<String, String> sorted = new TreeMap<>();
		if (tempArr != null) {
			tempArr.forEach((k, v) -> sorted.put(k, v == null ? "" : v));
		}
		StringBuilder str = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			if ("certi_ac".equals(e.getKey())) {
				continue;
			}
			str.append(e.getValue());
		}
		return md5Hex(str + (token == null ? "" : token));
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private static String textOrEmpty(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	private static String certRedisKey(String passportUid, long companyId) {
		return "prism:" + sha1Hex(passportUid + "_" + companyId + "_SaasCert");
	}

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private record CertSetting(String certId, String nodeId, String token) {}
}
