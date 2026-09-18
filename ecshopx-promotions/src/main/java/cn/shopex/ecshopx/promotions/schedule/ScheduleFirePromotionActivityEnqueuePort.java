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

package cn.shopex.ecshopx.promotions.schedule;

/** 与 PHP {@code dispatch(ScheduleFirePromotionsActivity)} 对齐的投递口；单条计 1 次。 */
public interface ScheduleFirePromotionActivityEnqueuePort {

	/** @return 本次投递计数值（通常为 1） */
	long enqueue(ScheduleFirePromotionActivityMessage message);
}
