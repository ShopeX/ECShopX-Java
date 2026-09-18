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

import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AddSmsBatchRecordJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(AddSmsBatchRecordJobHandler.class);

	private final RecordMapper recordMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AddSmsBatchRecordJobHandler(
			RecordMapper recordMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.recordMapper = recordMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"));
		int taskId = extractInt(payload.get("task_id"));
		int sceneId = extractInt(payload.get("scene_id"));
		String templateCode = stringify(payload.get("template_code"));
		String templateType = stringify(payload.get("template_type"));
		String smsContent = stringify(payload.get("sms_content"));
		int status = extractInt(payload.get("status"));
		String bizId = stringify(payload.get("biz_id"));
		List<String> mobiles = asStringList(payload.get("mobile"));

		if (templateCode == null || templateCode.isBlank()) {
			log.warn("addSmsBatchRecord job skipped: empty templateCode companyId={} taskId={}", companyId, taskId);
			return;
		}

		int nowSec = (int) Instant.now().getEpochSecond();
		String recordStatus = status == 1 ? "1" : String.valueOf(status);

		for (String mobilePlain : mobiles) {
			if (mobilePlain == null || mobilePlain.isBlank()) {
				continue;
			}
			Record rec = new Record();
			rec.setCompanyId(companyId);
			rec.setTaskId(taskId);
			rec.setSceneId(sceneId);
			rec.setTemplateCode(templateCode);
			rec.setTemplateType(templateType);
			rec.setSmsContent(smsContent);
			rec.setStatus(recordStatus);
			rec.setBizId(bizId);
			rec.setMobile(sensitiveFieldEncryptor.encrypt(mobilePlain.trim()));
			rec.setCreated(nowSec);
			rec.setUpdated(nowSec);
			recordMapper.insert(rec);
		}
	}

	private static List<String> asStringList(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String s = String.valueOf(o).trim();
				if (!s.isEmpty()) {
					out.add(s);
				}
			}
			return out;
		}
		if (raw instanceof Collection<?> col) {
			List<String> out = new ArrayList<>();
			for (Object o : col) {
				if (o == null) {
					continue;
				}
				String s = String.valueOf(o).trim();
				if (!s.isEmpty()) {
					out.add(s);
				}
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			List<String> out = new ArrayList<>();
			for (Object o : arr) {
				if (o == null) {
					continue;
				}
				String s = String.valueOf(o).trim();
				if (!s.isEmpty()) {
					out.add(s);
				}
			}
			return out;
		}
		String single = String.valueOf(raw).trim();
		return single.isEmpty() ? List.of() : List.of(single);
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static int extractInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw == null) {
			return 0;
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
