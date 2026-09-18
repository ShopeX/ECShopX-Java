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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.promotions.service.sms.SmsOemShuyunFlags;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class CompanySmsChannelRouter {

	private static final Logger log = LoggerFactory.getLogger(CompanySmsChannelRouter.class);

	private final AliyunsmsSettingStatusService aliyunsmsSettingStatusService;
	private final Environment environment;
	private final AliyunCompanySmsChannel aliyunCompanySmsChannel;
	private final ShuyunCompanySmsChannel shuyunCompanySmsChannel;
	private final ShopexCompanySmsChannel shopexCompanySmsChannel;

	public CompanySmsChannelRouter(
			AliyunsmsSettingStatusService aliyunsmsSettingStatusService,
			Environment environment,
			AliyunCompanySmsChannel aliyunCompanySmsChannel,
			ShuyunCompanySmsChannel shuyunCompanySmsChannel,
			ShopexCompanySmsChannel shopexCompanySmsChannel) {
		this.aliyunsmsSettingStatusService = aliyunsmsSettingStatusService;
		this.environment = environment;
		this.aliyunCompanySmsChannel = aliyunCompanySmsChannel;
		this.shuyunCompanySmsChannel = shuyunCompanySmsChannel;
		this.shopexCompanySmsChannel = shopexCompanySmsChannel;
	}

	public boolean send(long companyId, String mobile, String sceneTitle, Map<String, String> data) {
		return send(companyId, mobile, sceneTitle, data, null);
	}

	public boolean send(
			long companyId, String mobile, String sceneTitle, Map<String, String> data, String forgetSmsExactBody) {
		if (aliyunsmsSettingStatusService.getStatus(companyId)) {
			log.debug("Company SMS channel selected: aliyun companyId={} sceneTitle={}", companyId, sceneTitle);
			return aliyunCompanySmsChannel.send(companyId, mobile, sceneTitle, data, forgetSmsExactBody);
		}
		if (SmsOemShuyunFlags.isOemShuyun(environment)) {
			return shuyunCompanySmsChannel.send(companyId, mobile, sceneTitle, data, forgetSmsExactBody);
		}
		return shopexCompanySmsChannel.send(companyId, mobile, sceneTitle, data, forgetSmsExactBody);
	}
}
