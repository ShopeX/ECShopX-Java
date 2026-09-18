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

package cn.shopex.ecshopx.promotions.schedule.port;

import cn.shopex.ecshopx.common.cron.PromotionScheduleFireSceneSmsOutPort;
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySceneSmsSendPort;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 将计划活动场景短信统一下沉到 {@link CompanySceneSmsSendPort}，运维侧在 companys/阿里云模板配置具体 scene。
 */
@Service
public class PromotionScheduleFireSceneSmsOutAdapter implements PromotionScheduleFireSceneSmsOutPort {

	private final CompanySceneSmsSendPort companySceneSmsSendPort;

	public PromotionScheduleFireSceneSmsOutAdapter(CompanySceneSmsSendPort companySceneSmsSendPort) {
		this.companySceneSmsSendPort = companySceneSmsSendPort;
	}

	@Override
	public void sendTemplatedSceneSms(
			long companyId, String mobilePlain, String sceneTitle, Map<String, String> variables) {
		companySceneSmsSendPort.sendSceneTemplatedSms(companyId, mobilePlain, sceneTitle, variables);
	}
}
