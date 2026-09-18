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

package cn.shopex.ecshopx.ali.service.h5;

import cn.shopex.ecshopx.ali.service.minisetting.AliMiniAppSettingInfoService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.alipay.easysdk.base.qrcode.models.AlipayOpenAppQrcodeCreateResponse;
import com.alipay.easysdk.kernel.Config;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class H5AlipayMiniQrcodeFacade {

	private final WeappShareIdRedisService weappShareIdRedisService;
	private final AliMiniAppSettingInfoService aliMiniAppSettingInfoService;
	private final AlipayMiniEasySdkFactory alipayMiniEasySdkFactory;
	private final AlipayMiniQrcodeCreateService alipayMiniQrcodeCreateService;

	public H5AlipayMiniQrcodeFacade(
			WeappShareIdRedisService weappShareIdRedisService,
			AliMiniAppSettingInfoService aliMiniAppSettingInfoService,
			AlipayMiniEasySdkFactory alipayMiniEasySdkFactory,
			AlipayMiniQrcodeCreateService alipayMiniQrcodeCreateService) {
		this.weappShareIdRedisService = weappShareIdRedisService;
		this.aliMiniAppSettingInfoService = aliMiniAppSettingInfoService;
		this.alipayMiniEasySdkFactory = alipayMiniEasySdkFactory;
		this.alipayMiniQrcodeCreateService = alipayMiniQrcodeCreateService;
	}

	public Map<String, String> createQrcodeUrl(
			String companyIdParam,
			String page,
			String cxdid,
			String dtid,
			String smid,
			String uid,
			String distributorId) {
		Map<String, String> promo = new LinkedHashMap<>();
		putIfPresent(promo, "cxdid", cxdid);
		putIfPresent(promo, "dtid", dtid);
		putIfPresent(promo, "smid", smid);
		putIfPresent(promo, "uid", uid);
		putIfPresent(promo, "distributor_id", distributorId);

		String shareId = weappShareIdRedisService.getShareId(companyIdParam, promo);
		String scene =
				URLEncoder.encode("share_id", StandardCharsets.UTF_8)
						+ "="
						+ URLEncoder.encode(shareId, StandardCharsets.UTF_8);

		long parsedCompanyId = parsePositiveCompanyIdOrZero(companyIdParam);
		Map<String, Object> setting = aliMiniAppSettingInfoService.getInfoByCompanyId(parsedCompanyId);
		Config config = alipayMiniEasySdkFactory.buildConfig(setting);

		String pagePath = StringUtils.hasText(page) ? page : "pages/index";
		AlipayOpenAppQrcodeCreateResponse resp =
				alipayMiniQrcodeCreateService.create(pagePath, scene, config);
		if (resp == null) {
			throw new ResourceException("支付宝返回为空");
		}
		String code = resp.getCode();
		if (!"10000".equals(code)) {
			String msg = resp.getMsg();
			throw new ResourceException(msg != null && !msg.isEmpty() ? msg : "支付宝小程序码创建失败");
		}
		String url = resp.getQrCodeUrl();
		if (!StringUtils.hasText(url)) {
			throw new ResourceException("支付宝未返回小程序码地址");
		}
		return Map.of("qr_code_url", url);
	}

	private static void putIfPresent(Map<String, String> m, String key, String value) {
		if (value != null) {
			m.put(key, value);
		}
	}

	private static long parsePositiveCompanyIdOrZero(String companyIdParam) {
		if (companyIdParam == null) {
			return 0L;
		}
		String t = companyIdParam.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0 ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
