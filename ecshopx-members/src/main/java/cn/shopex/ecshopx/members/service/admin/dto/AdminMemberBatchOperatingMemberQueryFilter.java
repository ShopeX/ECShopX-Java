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

package cn.shopex.ecshopx.members.service.admin.dto;

import java.util.List;
import lombok.Data;

/** WHERE payload shared by batch operating COUNT and user-id page queries. */
@Data
public class AdminMemberBatchOperatingMemberQueryFilter {

	private long companyId;

	/** When set, restricts {@code m.grade_id} (numeric {@code grade_id} request field). */
	private Long membersGradeId;

	private List<Long> userIdsIn;
	private List<Long> userIdsNotIn;

	private String mobileEqEncrypted;
	private String remarksLike;
	private Long inviterId;
	private String userCardCode;
	private String usernameEqEncrypted;
	private String nameEq;

	private Long createdGte;
	private Long createdLte;
	private String birthdayGte;
	private String birthdayLte;

	private Boolean haveConsume;

	private List<Long> shopIds;
	private List<Long> distributorIds;

	private List<Long> tagIds;

	private Long pointGte;
	private Long pointLte;
	private Long pointEq;
}
