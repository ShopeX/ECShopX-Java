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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.wxapp.MemberBarcodeGenerateService;
import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RightsFrontWxappGetRightsCodeService {

	private final StringRedisTemplate wechatStringRedisTemplate;

	private final MemberBarcodeGenerateService memberBarcodeGenerateService;

	private final RightsMapper rightsMapper;

	public RightsFrontWxappGetRightsCodeService(
			@Qualifier("wechatStringRedisTemplate") StringRedisTemplate wechatStringRedisTemplate,
			MemberBarcodeGenerateService memberBarcodeGenerateService,
			RightsMapper rightsMapper) {
		this.wechatStringRedisTemplate = wechatStringRedisTemplate;
		this.memberBarcodeGenerateService = memberBarcodeGenerateService;
		this.rightsMapper = rightsMapper;
	}

	public Map<String, Object> getRightsCode(String rightsIdRaw, Map<String, Object> authClaims) {
		// ECX-10924：对齐 PHP GuideOrderTenantScopeGuard::assertRightsBelongsToAuthUser
		if (rightsIdRaw == null || rightsIdRaw.isBlank()) {
			throw new BadRequestException("获取核销码出错", 422);
		}
		long rightsId;
		try {
			rightsId = Long.parseLong(rightsIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("获取核销码出错", 422);
		}
		if (rightsId < 1L) {
			throw new BadRequestException("获取核销码出错", 422);
		}

		Rights r = rightsMapper.selectById(rightsId);
		if (r == null) {
			throw new ResourceException("无权访问该权益");
		}
		long authCompanyId = longVal(authClaims == null ? null : authClaims.get("company_id"));
		long authUserId = longVal(authClaims == null ? null : authClaims.get("user_id"));
		long rowCompanyId = r.getCompanyId() != null ? r.getCompanyId() : 0L;
		long rowUserId = r.getUserId() != null ? r.getUserId() : 0L;
		if (authCompanyId != rowCompanyId || authUserId <= 0L || authUserId != rowUserId) {
			throw new ResourceException("无权访问该权益");
		}

		StringBuilder sb = new StringBuilder(16);
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		for (int i = 0; i < 16; i++) {
			sb.append(rnd.nextInt(10));
		}
		String code = sb.toString();

		wechatStringRedisTemplate.opsForValue().set(
				"timescardcode:" + code,
				String.valueOf(rightsId),
				Duration.ofSeconds(60));

		LinkedHashMap<String, String> urls =
				memberBarcodeGenerateService.generateBarcodeAndQrcodeDataUrlsForPlainContent(code);

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("barcode_url", urls.get("barcode_url"));
		body.put("qrcode_url", urls.get("qrcode_url"));
		body.put("code", code);
		body.put("_ignore_data", Boolean.TRUE);
		return body;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
