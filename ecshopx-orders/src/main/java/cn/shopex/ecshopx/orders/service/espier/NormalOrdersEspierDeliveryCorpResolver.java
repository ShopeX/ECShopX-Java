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

package cn.shopex.ecshopx.orders.service.espier;

import cn.shopex.ecshopx.orders.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NormalOrdersEspierDeliveryCorpResolver {

	private static final String KUAIDI_REDIS_PREFIX = "kuaidiTypeOpenConfig:";

	private final CompanyRelLogisticsMapper companyRelLogisticsMapper;
	private final StringRedisTemplate stringRedisTemplate;

	public NormalOrdersEspierDeliveryCorpResolver(
			CompanyRelLogisticsMapper companyRelLogisticsMapper, StringRedisTemplate stringRedisTemplate) {
		this.companyRelLogisticsMapper = companyRelLogisticsMapper;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/** @return logistics company code (or OTHER) */
	public String resolveDeliveryCorpCode(long companyId, String deliveryCorpName, long supplierId) {
		if (!StringUtils.hasText(deliveryCorpName)) {
			return "OTHER";
		}
		String openType = readKuaidiOpenType(companyId);
		LambdaQueryWrapper<CompanyRelLogistics> w = new LambdaQueryWrapper<>();
		w.eq(CompanyRelLogistics::getCompanyId, (int) companyId)
				.eq(CompanyRelLogistics::getCorpName, deliveryCorpName.trim())
				.eq(CompanyRelLogistics::getSupplierId, supplierId);
		CompanyRelLogistics row = companyRelLogisticsMapper.selectOne(w);
		if (row == null) {
			return "OTHER";
		}
		if ("kuaidi100".equalsIgnoreCase(openType) && StringUtils.hasText(row.getKuaidiCode())) {
			return row.getKuaidiCode().trim();
		}
		if (StringUtils.hasText(row.getCorpCode())) {
			return row.getCorpCode().trim();
		}
		return "OTHER";
	}

	/** @return display name for a logistics code (or 其他) */
	public String resolveDeliveryCorpName(long companyId, String deliveryCorp, long supplierId) {
		if (!StringUtils.hasText(deliveryCorp)) {
			return "其他";
		}
		String code = deliveryCorp.trim();
		String openType = readKuaidiOpenType(companyId);
		LambdaQueryWrapper<CompanyRelLogistics> w = new LambdaQueryWrapper<>();
		w.eq(CompanyRelLogistics::getCompanyId, (int) companyId)
				.eq(CompanyRelLogistics::getSupplierId, supplierId);
		if ("kuaidi100".equals(openType) && code.equals(code.toLowerCase(Locale.ROOT))) {
			w.eq(CompanyRelLogistics::getKuaidiCode, code);
		} else {
			w.eq(CompanyRelLogistics::getCorpCode, code);
		}
		w.last("LIMIT 1");
		CompanyRelLogistics row = companyRelLogisticsMapper.selectOne(w);
		if (row != null && StringUtils.hasText(row.getCorpName())) {
			return row.getCorpName().trim();
		}
		return "其他";
	}

	private String readKuaidiOpenType(long companyId) {
		String key = KUAIDI_REDIS_PREFIX + sha1Hex(String.valueOf(companyId));
		try {
			String v = stringRedisTemplate.opsForValue().get(key);
			return StringUtils.hasText(v) ? v.trim() : "";
		} catch (DataAccessException e) {
			return "";
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
