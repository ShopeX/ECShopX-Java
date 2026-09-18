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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.companys.domain.OperatorLogs;
import cn.shopex.ecshopx.companys.mapper.OperatorLogsMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorLogsWriteService {

	private final OperatorLogsMapper operatorLogsMapper;

	public OperatorLogsWriteService(OperatorLogsMapper operatorLogsMapper) {
		this.operatorLogsMapper = operatorLogsMapper;
	}

	public void addLogs(Map<String, Object> context) {
		OperatorLogs log = new OperatorLogs();
		Object companyId = context.get("company_id");
		if (companyId instanceof Number n) {
			log.setCompanyId(n.longValue());
		}
		Object operatorId = context.get("operator_id");
		if (operatorId instanceof Number n) {
			log.setOperatorId(n.intValue());
		}
		Object uri = context.get("request_uri");
		if (uri != null) {
			log.setRequestUri(uri.toString());
		}
		Object ip = context.get("ip");
		if (ip != null) {
			log.setIp(ip.toString());
		}
		Object params = context.get("params");
		if (params != null) {
			log.setParams(params.toString());
		}
		Object name = context.get("operator_name");
		if (name != null) {
			log.setOperatorName(name.toString());
		}
		Object logType = context.get("log_type");
		if (logType != null) {
			log.setLogType(logType.toString());
		}
		Object merchantId = context.get("merchant_id");
		if (merchantId instanceof Number n) {
			log.setMerchantId(n.longValue());
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		log.setCreated(now);
		log.setUpdated(now);
		operatorLogsMapper.insert(log);
	}
}
