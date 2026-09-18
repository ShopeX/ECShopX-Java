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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.companys.domain.OperatorLogs;
import cn.shopex.ecshopx.companys.mapper.OperatorLogsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 操作日志定时任务编排（如按策略清理历史操作日志）。查询/单条写入仍由
 * {@link OperatorLogsQueryService}、{@link OperatorLogsWriteService} 承担。
 */
@Slf4j
@Service
public class OperatorLogsService {

	private static final String DEL_OPERATOR_LOGS_DATE_PROP = "common.del-operator-logs-date";
	private static final DateTimeFormatter LOG_DAY = DateTimeFormatter.ISO_LOCAL_DATE;

	private final OperatorLogsMapper operatorLogsMapper;
	private final Environment environment;
	private final Clock clock;
	private final ZoneId zone;

	@Autowired
	public OperatorLogsService(OperatorLogsMapper operatorLogsMapper, Environment environment) {
		this(
				operatorLogsMapper,
				environment,
				Clock.system(ZoneId.systemDefault()),
				ZoneId.systemDefault());
	}

	/**
	 * 可注入固定时钟与时区，供单测对账；生产请使用两参构造器所委托的默认实现。
	 */
	OperatorLogsService(
			OperatorLogsMapper operatorLogsMapper,
			Environment environment,
			Clock clock,
			ZoneId zone) {
		this.operatorLogsMapper = operatorLogsMapper;
		this.environment = environment;
		this.clock = clock;
		this.zone = zone;
	}

	/**
	 * 按配置清理早于阈值（Unix 秒）的操作日志，返回物理删除行数。
	 */
	public int scheduleDeleteOperatorLogs() {
		int thresholdSec = resolveThresholdEpochSeconds();
		String logDay =
				ZonedDateTime.ofInstant(Instant.ofEpochSecond((long) thresholdSec), zone)
						.toLocalDate()
						.format(LOG_DAY);
		log.info("开始执行删除操作日志脚本，清理{}之前的记录", logDay);
		LambdaQueryWrapper<OperatorLogs> w = new LambdaQueryWrapper<>();
		w.le(OperatorLogs::getCreated, thresholdSec);
		int removed = operatorLogsMapper.delete(w);
		log.info("本次执行共删除{}条记录", removed);
		return removed;
	}

	private int resolveThresholdEpochSeconds() {
		String raw = environment.getProperty(DEL_OPERATOR_LOGS_DATE_PROP, "");
		ZonedDateTime now = ZonedDateTime.ofInstant(clock.instant(), zone);
		if (isFalsyConfig(raw)) {
			return toIntSeconds(now.minusMonths(3).toEpochSecond());
		}
		String t = raw.trim();
		int days = parsePositiveDays(t);
		if (days <= 0) {
			return toIntSeconds(now.minusMonths(3).toEpochSecond());
		}
		return toIntSeconds(now.minusDays((long) days).toEpochSecond());
	}

	private static boolean isFalsyConfig(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return true;
		}
		if ("0".equals(t)) {
			return true;
		}
		try {
			return Integer.parseInt(t) == 0;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static int parsePositiveDays(String trimmed) {
		try {
			return Integer.parseInt(trimmed);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int toIntSeconds(long epochSecond) {
		if (epochSecond > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (epochSecond < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) epochSecond;
	}
}
