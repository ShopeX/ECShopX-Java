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

package cn.shopex.ecshopx.goods.service.ome;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OmeItemsSyncQueueLogWriter {

	private static final String JOB_EXECUTOR_CLASS = "cn.shopex.ecshopx.goods.dispatch.GetItemsFromOmeJobHandler";

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public OmeItemsSyncQueueLogWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public void writeFail(long companyId, String apiMethod, Map<String, Object> goodsSnapshot, long startNano, String resultMsg) {
		double seconds = (System.nanoTime() - startNano) / 1_000_000_000.0;
		String runtime = String.format(java.util.Locale.ROOT, "%.3f", seconds);
		int now = (int) (System.currentTimeMillis() / 1000L);
		Object goodsBn = goodsSnapshot != null ? goodsSnapshot.get("goods_bn") : null;
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("goods_bn", goodsBn);
		params.put("job", JOB_EXECUTOR_CLASS);
		String paramsJson;
		try {
			paramsJson = objectMapper.writeValueAsString(params);
		} catch (Exception e) {
			paramsJson = "{}";
		}
		String msg = resultMsg != null ? resultMsg : "";
		jdbcTemplate.update(
				"INSERT INTO systemlink_oms_queuelog (company_id, api_type, worker, params, result, status, runtime, created, updated) VALUES (?,?,?,?,?,?,?,?,?)",
				companyId,
				"request",
				apiMethod,
				paramsJson,
				msg,
				"fail",
				runtime,
				now,
				now);
	}
}
