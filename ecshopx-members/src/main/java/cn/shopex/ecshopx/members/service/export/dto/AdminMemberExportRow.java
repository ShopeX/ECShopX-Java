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

package cn.shopex.ecshopx.members.service.export.dto;

import lombok.Data;

@Data
public class AdminMemberExportRow {

	private Long userId;
	private Long companyId;
	private String userCardCode;
	private String mobile;
	private String name;
	private Integer sex;
	private String username;
	private Integer createdYear;
	private Integer createdMonth;
	private Integer createdDay;
	private String shopName;
	private String storeName;
	private Long gradeId;
	private Long inviterId;
	private String birthday;
	private String address;
	private String email;
	private String industry;
	private String income;
	private String eduBackground;
	private String habbit;
	private String unionId;
	private String openId;
}
