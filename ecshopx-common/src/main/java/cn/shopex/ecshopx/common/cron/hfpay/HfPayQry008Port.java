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

package cn.shopex.ecshopx.common.cron.hfpay;

import java.util.Map;

/**
 * 汇付对账/交易状态查询（qry008）窄口，与 {@code HfPayAcouJsonPostClient#qry008} 语义一致，供需隔离 HTTP 外呼的定时任务等场景注入。
 */
public interface HfPayQry008Port {

	Map<String, Object> qry008(Map<String, Object> setting, Map<String, Object> payload);
}
