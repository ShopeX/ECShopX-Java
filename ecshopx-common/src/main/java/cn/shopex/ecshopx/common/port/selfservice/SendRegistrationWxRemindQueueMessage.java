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

package cn.shopex.ecshopx.common.port.selfservice;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对位 Laravel 侧 {@code SendWxRemindJob} 入参：单活动主键，由慢队列消费端与 PHP worker 协议对齐后解析。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendRegistrationWxRemindQueueMessage implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private long activityId;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SendRegistrationWxRemindQueueMessage that = (SendRegistrationWxRemindQueueMessage) o;
		return activityId == that.activityId;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(activityId);
	}
}
