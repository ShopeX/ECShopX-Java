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

package cn.shopex.ecshopx.employeepurchase.service.dto;

import java.util.List;
import lombok.Data;

/** SQL 条件载体，与活动商品分页查询 Mapper 绑定。 */
@Data
public class ActivityGoodsListSqlFilter {

	private Long companyId;
	private Long activityId;
	/** null 或空：不限制 item_category；非空：IN 列表（可含 "-1" 表示无匹配） */
	private List<String> mainCatIds;
	/** null：不限制销售分类；非空且非空列表：IN category_id */
	private List<Long> categoryIds;
	private String itemName;
	private String itemBn;
	/** 非空时 i.approve_status IN (...) */
	private List<String> approveStatusIn;
	/** true：JOIN 分销可售表且要求可售 */
	private Boolean requireCanSaleJoin;
	/** 非空时 (item_name LIKE OR item_bn LIKE) */
	private String keywords;
	/** null：不限制；0/1：按活动商品上下架过滤 */
	private Integer shelfStatus;
}
