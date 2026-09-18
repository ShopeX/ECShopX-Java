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

package cn.shopex.ecshopx.common.port.orders;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;

/**
 * 社区拼团活动到期后批量取消单：与 {@code community_setting} 无关的独立投递载荷，由慢队列消费侧调用订单取消。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommunityActivityCancelOrderRow implements Serializable {

	@JsonProperty("company_id")
	private long companyId;

	@JsonProperty("order_id")
	private long orderId;

	@JsonProperty("cancel_reason")
	private String cancelReason;

	@JsonProperty("user_id")
	private long userId;

	@JsonProperty("mobile")
	private String mobile;

	@JsonProperty("cancel_from")
	private String cancelFrom = "system";

	@JsonProperty("chief_id")
	private Long chiefId;
}
