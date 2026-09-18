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

import cn.shopex.ecshopx.aliyunsms.domain.Record;
import cn.shopex.ecshopx.aliyunsms.mapper.RecordMapper;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySendDetailJobDispatchPublisher;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

	private final RecordMapper recordMapper;
	private final AliyunsmsQuerySendDetailJobDispatchPublisher querySendDetailJobDispatchPublisher;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public SmsService(
			RecordMapper recordMapper,
			AliyunsmsQuerySendDetailJobDispatchPublisher querySendDetailJobDispatchPublisher,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.recordMapper = recordMapper;
		this.querySendDetailJobDispatchPublisher = querySendDetailJobDispatchPublisher;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	/**
	 * Loads up to 100 pending {@code aliyunsms_record} rows ({@code status = "1"}), decrypts mobile numbers,
	 * and dispatches one async job per valid row. Returns the number of jobs successfully published (skipped
	 * rows are not counted).
	 */
	public int scheduleQuerySendDetail() {
		List<Record> pending =
				recordMapper.selectList(
						new QueryWrapper<Record>()
								.eq("status", "1")
								.last("LIMIT 100"));
		if (pending == null || pending.isEmpty()) {
			return 0;
		}
		int published = 0;
		for (Record item : pending) {
			Long id = item.getId();
			Integer created = item.getCreated();
			String bizId = item.getBizId();
			String mobileCipher = item.getMobile();
			if (id == null
					|| created == null
					|| bizId == null
					|| bizId.isEmpty()
					|| mobileCipher == null
					|| mobileCipher.isEmpty()) {
				continue;
			}
			String mobile = sensitiveFieldEncryptor.decrypt(mobileCipher);
			if (mobile == null || mobile.isEmpty()) {
				continue;
			}
			long companyId = item.getCompanyId() != null ? item.getCompanyId() : 0L;
			querySendDetailJobDispatchPublisher.publish(companyId, id, mobile, bizId, created);
			published++;
		}
		return published;
	}
}
