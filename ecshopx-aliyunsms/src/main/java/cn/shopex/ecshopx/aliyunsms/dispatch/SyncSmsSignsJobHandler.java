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

package cn.shopex.ecshopx.aliyunsms.dispatch;

import cn.shopex.ecshopx.aliyunsms.service.AliyunsmsSignSyncService;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SyncSmsSignsJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncSmsSignsJobHandler.class);

	private final AliyunsmsSignSyncService aliyunsmsSignSyncService;

	public SyncSmsSignsJobHandler(AliyunsmsSignSyncService aliyunsmsSignSyncService) {
		this.aliyunsmsSignSyncService = aliyunsmsSignSyncService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Object raw = payload.get("company_id");
		long companyId = raw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(raw).trim());
		log.info("SyncSmsSigns job consumed companyId={} payloadKeys={}", companyId, payload.keySet());
		aliyunsmsSignSyncService.sync(companyId);
	}
}
