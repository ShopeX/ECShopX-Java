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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import cn.shopex.ecshopx.promotions.service.TurntableConfigService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LuckyDrawLogExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private final LuckyDrawLogExportFileJobHandler jobHandler;
	private final TurntableConfigService turntableConfigService;

	public LuckyDrawLogExportFileJobTypeHandler(
			LuckyDrawLogExportFileJobHandler jobHandler, TurntableConfigService turntableConfigService) {
		this.jobHandler = jobHandler;
		this.turntableConfigService = turntableConfigService;
	}

	@Override
	public String exportType() {
		return LuckyDrawLogExportFileJobTypes.TYPE_EXPORT_LUCKDRAW_LOG;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		jobHandler.run(LuckyDrawLogExportFileJobPayloadSupport.contextFromPayload(payload, turntableConfigService));
	}
}
