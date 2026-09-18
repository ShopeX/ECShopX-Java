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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.salesperson.domain.SalemanCustomerComplaint;
import java.util.LinkedHashMap;
import java.util.Map;

/** Maps complaint entity rows to admin API list/reply response field names. */
public final class SalemanCustomerComplaintSwgRowConverter {

	private SalemanCustomerComplaintSwgRowConverter() {
	}

	public static Map<String, Object> toRow(SalemanCustomerComplaint e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("user_id", e.getUserId());
		m.put("company_id", e.getCompanyId());
		m.put("user_name", e.getUserName());
		m.put("user_mobile", e.getUserMobile());
		m.put("saleman_id", e.getSalemanId());
		m.put("saleman_name", e.getSalemanName());
		m.put("saleman_avatar", e.getSalemanAvatar());
		m.put("saleman_mobile", e.getSalemanMobile());
		m.put("distributor_id", e.getDistributorId());
		m.put("saleman_distribution_name", e.getSalemanDistributionName());
		m.put("complaints_content", e.getComplaintsContent());
		m.put("complaints_images", e.getComplaintsImages());
		m.put("reply_status", Boolean.TRUE.equals(e.getReplyStatus()) ? 1 : 0);
		m.put("reply_content", e.getReplyContent());
		m.put("reply_time", e.getReplyTime());
		m.put("reply_operator_id", e.getReplyOperatorId());
		m.put("reply_operator_name", e.getReplyOperatorName());
		m.put("reply_operator_mobile", e.getReplyOperatorMobile());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
