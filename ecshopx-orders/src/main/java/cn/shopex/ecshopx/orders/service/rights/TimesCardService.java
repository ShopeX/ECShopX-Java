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

package cn.shopex.ecshopx.orders.service.rights;

import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 与 PHP {@code OrdersBundle\Services\Rights\TimesCardService} 对齐的权益批处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimesCardService {

	/**
	 * PHP {@code RightsRepository::updateStatusBy} 将非 boolean true 的字段值折叠为 0 再写入；
	 * 注释中的 expire/invalid 在现网 SQL 中实际为数值/字符串 {@code 0}。
	 */
	private static final String STATUS_AS_ZERO = "0";

	private static final String ERR_LOG_PREFIX = "定时修改权益的状态出错:";

	private final RightsMapper rightsMapper;

	/**
	 * 定时批量更新权益状态（两次独立 UPDATE，与 PHP 无显式事务一致）。
	 * 与 PHP 一致：底层异常在方法内吞掉并打 debug，不向调用方抛出。
	 *
	 * @return 成功时为两次影响行数之和；任一步异常时为 {@code 0}
	 */
	public int scheduleUpdateRightStatus() {
		try {
			int now = (int) Instant.now().getEpochSecond();

			LambdaUpdateWrapper<Rights> expired = new LambdaUpdateWrapper<Rights>()
					.le(Rights::getEndTime, now)
					.set(Rights::getStatus, STATUS_AS_ZERO);
			int n1 = rightsMapper.update(null, expired);

			LambdaUpdateWrapper<Rights> consumed = new LambdaUpdateWrapper<Rights>()
					.eq(Rights::getIsNotLimitNum, 2)
					.apply("total_num <= total_consum_num")
					.set(Rights::getStatus, STATUS_AS_ZERO);
			int n2 = rightsMapper.update(null, consumed);

			return n1 + n2;
		} catch (Exception e) {
			log.debug("{}{}", ERR_LOG_PREFIX, e.getMessage());
			return 0;
		}
	}
}
