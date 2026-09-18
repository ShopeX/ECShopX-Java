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

/** 会员邮箱激活链接令牌 */
@Data
@MpTable(
		value = "member_email_activation_tokens",
		comment = "会员邮箱激活链接令牌",
		indexes = {
				@MpIndex(name = "idx_company_user", columns = {"company_id", "user_id"}),
				@MpIndex(name = "idx_token_hash", columns = {"token_hash"})
		})
public class MemberEmailActivationToken {

	/** 主键 */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "主键")
	private Long id;

	/** 公司 ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司 ID")
	private Long companyId;

	/** 会员 user_id */
	@MpField(value = "user_id", columnType = "bigint", comment = "会员 user_id")
	private Long userId;

	/** SHA-256 哈希 */
	@MpField(value = "token_hash", columnType = "string", length = 64, comment = "SHA-256 哈希")
	private String tokenHash;

	/** 过期时间戳（Unix 秒） */
	@MpField(value = "expires_at", columnType = "integer", comment = "过期时间戳")
	private Long expiresAt;

	/** 使用时间戳（Unix 秒）；NULL=未使用 */
	@MpField(value = "used_at", columnType = "integer", nullable = true, comment = "使用时间戳")
	private Long usedAt;

	/** 创建时间戳（Unix 秒） */
	@MpField(value = "created_at", columnType = "integer", comment = "创建时间戳")
	private Long createdAt;
}
