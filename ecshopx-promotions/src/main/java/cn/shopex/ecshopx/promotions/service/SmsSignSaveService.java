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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.promotions.domain.SmsIdiograph;
import cn.shopex.ecshopx.promotions.mapper.SmsIdiographMapper;
import cn.shopex.ecshopx.promotions.service.sms.SmsOemShuyunFlags;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismCoreHttpClient;
import cn.shopex.ecshopx.thirdparty.service.prism.ShopexPrismSmsSignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class SmsSignSaveService {

	private static final String[] DISALLOWED_WORDS = {
		"天猫", "tmall", "淘宝", "taobao", "1号店", "易迅", "京东", "亚马逊", "test", "测试"
	};

	private final CompanysMapper companysMapper;
	private final SmsIdiographMapper smsIdiographMapper;
	private final PrismCoreHttpClient prismCoreHttpClient;
	private final ObjectMapper objectMapper;
	private final Environment environment;
	private final StringRedisTemplate prismRedisTemplate;

	public SmsSignSaveService(
			CompanysMapper companysMapper,
			SmsIdiographMapper smsIdiographMapper,
			PrismCoreHttpClient prismCoreHttpClient,
			ObjectMapper objectMapper,
			Environment environment,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate) {
		this.companysMapper = companysMapper;
		this.smsIdiographMapper = smsIdiographMapper;
		this.prismCoreHttpClient = prismCoreHttpClient;
		this.objectMapper = objectMapper;
		this.environment = environment;
		this.prismRedisTemplate = prismRedisTemplate;
	}

	public void saveSmsSign(long companyId, String newContent) {
		checkSignAndThrowIfInvalid(newContent);
		boolean oemShuyun = SmsOemShuyunFlags.isOemShuyun(environment);
		String passportUid;
		if (oemShuyun) {
			passportUid = null;
		} else {
			Companys c = companysMapper.selectById(companyId);
			if (c == null || c.getPassportUid() == null) {
				passportUid = null;
			} else {
				String t = c.getPassportUid().trim();
				passportUid = t.isEmpty() ? null : t;
			}
		}
		String content = "【" + newContent + "】";
		Long shopexUidCol;
		if (passportUid == null) {
			shopexUidCol = null;
		} else {
			try {
				shopexUidCol = Long.parseLong(passportUid);
			} catch (NumberFormatException e) {
				shopexUidCol = null; // 非数字时不写入 shopex_uid 列
			}
		}
		LambdaQueryWrapper<SmsIdiograph> q =
				new LambdaQueryWrapper<SmsIdiograph>().eq(SmsIdiograph::getCompanyId, companyId);
		if (shopexUidCol == null) {
			q.isNull(SmsIdiograph::getShopexUid);
		} else {
			q.eq(SmsIdiograph::getShopexUid, shopexUidCol);
		}
		SmsIdiograph row = smsIdiographMapper.selectOne(q.last("LIMIT 1"));
		ShopexPrismSmsSignClient prismClient =
				oemShuyun
						? null
						: new ShopexPrismSmsSignClient(
								prismCoreHttpClient, prismRedisTemplate, objectMapper, companyId, passportUid);
		int now = (int) Instant.now().getEpochSecond();
		if (row != null) {
			String oldContent = "【" + (row.getIdiograph() == null ? "" : row.getIdiograph()) + "】";
			prismClient.updateSmsSign(content, oldContent);
			LambdaUpdateWrapper<SmsIdiograph> uw = new LambdaUpdateWrapper<>();
			uw.eq(SmsIdiograph::getCompanyId, companyId);
			if (shopexUidCol == null) {
				uw.isNull(SmsIdiograph::getShopexUid);
			} else {
				uw.eq(SmsIdiograph::getShopexUid, shopexUidCol);
			}
			uw.set(SmsIdiograph::getIdiograph, newContent);
			uw.set(SmsIdiograph::getUpdated, now);
			int n = smsIdiographMapper.update(null, uw);
			if (n == 0) {
				throw new ResourceException("记录不存在");
			}
		} else {
			prismClient.addSmsSign(content);
			SmsIdiograph ins = new SmsIdiograph();
			ins.setCompanyId(companyId);
			ins.setShopexUid(shopexUidCol);
			ins.setIdiograph(newContent);
			ins.setCreated(now);
			ins.setUpdated(now);
			smsIdiographMapper.insert(ins);
		}
	}

	private void checkSignAndThrowIfInvalid(@Nullable String sign) {
		String trimmedForLength = sign == null ? "" : sign.trim();
		String decoded;
		try {
			decoded = URLDecoder.decode(trimmedForLength, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			decoded = trimmedForLength;
		}
		int n = decoded.codePointCount(0, decoded.length());
		if (n < 3 || n > 20) {
			throw new BadRequestException("签名长度为3到20字");
		}
		String lower = (sign == null ? "" : sign).toLowerCase(Locale.ROOT);
		for (String word : DISALLOWED_WORDS) {
			if (lower.contains(word.toLowerCase(Locale.ROOT))) {
				throw new BadRequestException("非法签名");
			}
		}
		if (sign != null && sign.contains("【") && sign.contains("】")) {
			throw new BadRequestException("签名中含有非法字符");
		}
	}
}
