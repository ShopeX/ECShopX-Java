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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterFrontQrcodePngService {

	private static final Logger log = LoggerFactory.getLogger(PromoterFrontQrcodePngService.class);

	private static final String PRESCRIPTION_KEY_PREFIX = "dianwu_prescription_order_random:";

	private static final char[] RANDOM_ALPHANUM =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;
	private final StringRedisTemplate stringRedisTemplate;

	public PromoterFrontQrcodePngService(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WechatAuthQueryService wechatAuthQueryService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient,
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public byte[] getPromoterQrcodePng(
			long companyId,
			String appidQuery,
			String uidRaw,
			String userIdRaw,
			String dtidRaw,
			boolean dtidPresent,
			String qrRaw,
			boolean qrPresent,
			String prescriptionOrderId,
			String pathRaw,
			String pageOverrideRaw,
			boolean pageOverridePresent,
			String orderIdRaw,
			boolean orderIdPresent) {

		boolean needsResolveDefaultAppid =
				(appidQuery == null || appidQuery.isEmpty() || "0".equals(appidQuery));
		String wxappAppid;
		if (needsResolveDefaultAppid) {
			Optional<String> resolved = weappAuthorizerAppidRepository.findAuthorizerAppid(companyId, "yykweishop");
			wxappAppid = resolved.orElse(null);
		} else {
			wxappAppid = appidQuery;
		}

		if (companyId > 0L
				&& wxappAppid != null
				&& !wxappAppid.isEmpty()
				&& !"0".equals(wxappAppid)) {
			if (!wechatAuthQueryService.isAuthorizerAppidBoundToCompany(companyId, wxappAppid)) {
				throw new BadRequestException("小程序未绑定，请重新绑定", 400001);
			}
		}

		if (wxappAppid == null || wxappAppid.isEmpty() || "0".equals(wxappAppid)) {
			throw new BadRequestException("缺少小程序 appid");
		}

		String scene = "uid=" + coalesceUidForScene(uidRaw, userIdRaw);
		String page =
				(pathRaw != null && !pathRaw.trim().isEmpty()) ? pathRaw.trim() : "pages/index";

		if (queryStringKeyPresentAndMeaningful(dtidRaw, dtidPresent)) {
			page = "subpages/store/index";
			scene = scene + "&id=" + dtidRaw.trim();
		}

		if (queryStringKeyPresentAndMeaningful(qrRaw, qrPresent)) {
			page = "subpages/store/index";
			scene = scene + "&qr=" + qrRaw.trim();
		}

		if (StringUtils.hasText(prescriptionOrderId)) {
			String random = randomAlphanumeric4();
			String key = PRESCRIPTION_KEY_PREFIX + prescriptionOrderId.trim() + random;
			stringRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(1800));
			scene = "oi=" + prescriptionOrderId.trim() + "&r=" + random + "&t=1";
		}

		if (queryStringKeyPresentAndMeaningful(pageOverrideRaw, pageOverridePresent)
				&& queryStringKeyPresentAndMeaningful(orderIdRaw, orderIdPresent)) {
			page = pageOverrideRaw.trim();
			scene = "oi=" + orderIdRaw.trim();
		}

		if (page.startsWith("/")) {
			page = page.substring(1);
		}

		log.debug("推广码PNG page={} scene={}", page, scene);

		return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxappAppid, scene, page);
	}

	private static boolean queryStringKeyPresentAndMeaningful(String raw, boolean keyPresent) {
		if (!keyPresent) {
			return false;
		}
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equals(t);
	}

	private static String coalesceUidForScene(String uidRaw, String userIdRaw) {
		String u = uidRaw == null ? "" : uidRaw.trim();
		if (u.isEmpty() || "0".equals(u)) {
			u = userIdRaw == null ? "" : String.valueOf(userIdRaw).trim();
		}
		return u;
	}

	private static String randomAlphanumeric4() {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		char[] buf = new char[4];
		for (int i = 0; i < 4; i++) {
			buf[i] = RANDOM_ALPHANUM[r.nextInt(RANDOM_ALPHANUM.length)];
		}
		return new String(buf);
	}
}
