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

/** 数云线下权益发送明细 */
@Data
@MpTable(
		value = "shuyun_offline_benefit_send_item",
		comment = "数云线下权益发送明细",
		indexes = {@MpIndex(name = "IDX_41573DEDF39EBE7A", columns = {"batch_id"})},
		uniqueIndexes = {
			@MpIndex(name = "uk_shuyun_offline_benefit_item_batch_customer", columns = {"batch_id", "customer_id"})
		})
public class ShuyunOfflineBenefitSendItem {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "batch_id", columnType = "bigint")
	private Long batchId;

	@MpField(value = "customer_id", columnType = "string", length = 128)
	private String customerId;

	@MpField(value = "member_user_id", columnType = "bigint", nullable = true)
	private Long memberUserId;

	@MpField(value = "benefit_code", columnType = "string", length = 256, nullable = true)
	private String benefitCode;

	@MpField(value = "fail_reason", columnType = "string", nullable = true)
	private String failReason;

	@MpField(value = "status", columnType = "string", length = 32)
	private String status;

	@MpField(value = "send_time", columnType = "integer", nullable = true, comment = "实际发送时间(秒)")
	private Integer sendTime;

	@MpField(value = "send_reason", columnType = "string", length = 512, nullable = true)
	private String sendReason;

	@MpField(value = "detail_pushed_at", columnType = "integer", nullable = true)
	private Integer detailPushedAt;

	@MpField(value = "last_consume_status", columnType = "string", length = 32, nullable = true, comment = "USED|NOT_USED 等")
	private String lastConsumeStatus;

	@MpField(value = "last_consume_push_at", columnType = "integer", nullable = true)
	private Integer lastConsumePushAt;

	@MpField(value = "local_order_id", columnType = "bigint", nullable = true)
	private Long localOrderId;

	@MpField(value = "created", columnType = "integer", comment = "添加时间")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
	private Integer updated;
}
