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

package cn.shopex.ecshopx.shuyun.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 数云线下权益发送批次 */
@Data
@MpTable(
		value = "shuyun_offline_benefit_send_batch",
		comment = "数云线下权益发送批次",
		uniqueIndexes = {
			@MpIndex(name = "uk_shuyun_offline_benefit_batch_company_request", columns = {"company_id", "request_id"})
		})
public class ShuyunOfflineBenefitSendBatch {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "request_id", columnType = "string", length = 128)
	private String requestId;

	@MpField(value = "benefit_id", columnType = "string", length = 128)
	private String benefitId;

	@MpField(value = "send_kind", columnType = "string", length = 16, comment = "single|batch")
	private String sendKind;

	@MpField(value = "send_time", columnType = "integer", nullable = true)
	private Integer sendTime;

	@MpField(value = "expire_time", columnType = "integer", nullable = true)
	private Integer expireTime;

	@MpField(value = "send_remark", columnType = "string", length = 512, nullable = true)
	private String sendRemark;

	@MpField(value = "status", columnType = "string", length = 32)
	private String status;

	@MpField(value = "total_count", columnType = "integer", nullable = true)
	private Integer totalCount;

	@MpField(value = "success_count", columnType = "integer", nullable = true)
	private Integer successCount;

	@MpField(value = "failure_count", columnType = "integer", nullable = true)
	private Integer failureCount;

	@MpField(value = "report_pushed_at", columnType = "integer", nullable = true)
	private Integer reportPushedAt;

	@MpField(value = "report_last_error", columnType = "string", nullable = true)
	private String reportLastError;

	@MpField(value = "report_retry_count", columnType = "integer", defaultValue = "0")
	private Integer reportRetryCount = 0;

	@MpField(value = "created", columnType = "integer", comment = "添加时间")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
	private Integer updated;
}
