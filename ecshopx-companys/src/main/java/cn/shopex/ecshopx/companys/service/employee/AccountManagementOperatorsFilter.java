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

package cn.shopex.ecshopx.companys.service.employee;

import java.util.List;
import lombok.Data;

@Data
public class AccountManagementOperatorsFilter {

	private Long companyId;
	private Long merchantId;
	private Integer isDistributorMain;
	private List<Long> operatorIds;
	private String mobile;
	private String usernameContains;
	private String isDisable;
	private String loginName;
	private String roleId;
	private String operatorType;
	private List<Long> distributorIds;

	public static final class OrderBy {

		private final String field;
		private final Direction direction;

		public OrderBy(String field, Direction direction) {
			this.field = field;
			this.direction = direction;
		}

		public String getField() {
			return field;
		}

		public Direction getDirection() {
			return direction;
		}

		public enum Direction {
			ASC,
			DESC
		}
	}
}
