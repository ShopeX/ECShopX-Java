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

@Data
public class AdminMemberListQueryFilter {

	private long companyId;
	private Long userIdEq;
	private List<Long> userIdIn;
	private List<Long> userIdNotIn;
	private Long inviterId;
	private Long gradeIdEq;
	private List<Long> vipPaidUserIds;
	private Long tagIdEq;
	private List<Long> tagIdIn;
	private List<Long> shopIds;
	private String mobileEncEq;
	private String remarksLike;
	private String usernameLike;
	private String nameLike;
	private String sourceEq;
	private String wechatNicknameLike;
	private String haveConsume;
	private String birthdayStart;
	private String birthdayEnd;
	private Long timeStartBegin;
	private Long timeStartEnd;
	private String storeBnEq;
	private Long distributorIdForChiefBranch;
	private Long opDistributorEq;
}
