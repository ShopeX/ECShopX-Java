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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员标签关系审计 */
@Data
@MpTable(
		value = "members_rel_tags_audit",
		comment = "会员标签关系审计",
		indexes = {@MpIndex(name = "idx_company_user_tag", columns = {"company_id", "user_id", "tag_id"})})
public class MemberRelTagsAudit {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	@MpField(value = "user_id", columnType = "bigint", comment = "会员id")
	private Long userId;

	@MpField(value = "tag_id", columnType = "bigint", comment = "标签id")
	private Long tagId;

	@MpField(value = "action", columnType = "string", length = 16, comment = "attach|detach")
	private String action;

	@MpField(value = "source", columnType = "string", length = 32, comment = "来源")
	private String source;

	@MpField(value = "rule_version", columnType = "integer", comment = "规则版本", defaultValue = "1")
	private Integer ruleVersion = 1;

	@MpField(value = "reason", columnType = "string", length = 255, nullable = true, comment = "原因")
	private String reason;

	@MpField(value = "event_id", columnType = "string", length = 64, nullable = true, comment = "事件或任务id")
	private String eventId;

	@MpField(value = "created", columnType = "bigint", comment = "时间")
	private Long created;
}
