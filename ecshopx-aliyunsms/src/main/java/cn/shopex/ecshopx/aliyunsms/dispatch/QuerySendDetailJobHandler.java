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
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySendDetailsClient;
import cn.shopex.ecshopx.common.aliyunsms.QuerySendDetailsResult;
import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QuerySendDetailJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(QuerySendDetailJobHandler.class);

	private static final DateTimeFormatter SEND_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final ZoneId SEND_DATE_ZONE = ZoneId.of("Asia/Shanghai");

	private final RecordMapper recordMapper;
	private final AliyunsmsQuerySendDetailsClient aliyunsmsQuerySendDetailsClient;

	public QuerySendDetailJobHandler(
			RecordMapper recordMapper, AliyunsmsQuerySendDetailsClient aliyunsmsQuerySendDetailsClient) {
		this.recordMapper = recordMapper;
		this.aliyunsmsQuerySendDetailsClient = aliyunsmsQuerySendDetailsClient;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractLong(payload.get("company_id"));
		long id = extractLong(payload.get("id"));
		String mobile = stringify(payload.get("mobile"));
		String bizId = stringify(payload.get("biz_id"));
		int created = extractInt(payload.get("created"));

		if (mobile == null || mobile.isBlank() || bizId == null || bizId.isEmpty()) {
			log.warn("querySendDetail job skipped: invalid mobile or bizId companyId={} id={}", companyId, id);
			return;
		}

		String sendDate =
				Instant.ofEpochSecond(created).atZone(SEND_DATE_ZONE).format(SEND_DATE_FMT);

		QuerySendDetailsResult cloud =
				aliyunsmsQuerySendDetailsClient.querySendDetails(companyId, mobile, bizId, sendDate);
		if (cloud.sendStatus() == null) {
			return;
		}

		Record current = recordMapper.selectOne(new QueryWrapper<Record>().eq("id", id));
		if (current == null) {
			throw new ResourceException("未查询到更新数据");
		}
		int now = (int) Instant.now().getEpochSecond();
		int rows =
				recordMapper.update(
						null,
						new UpdateWrapper<Record>()
								.eq("id", id)
								.set("status", cloud.sendStatus())
								.set("sms_content", cloud.content())
								.set("updated", now));
		if (rows != 1) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private static long extractLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static int extractInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private static String stringify(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}
}
