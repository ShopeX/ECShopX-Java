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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTaskRelDistributor;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskRelDistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SalespersonTaskCreateService {

	public static final int TASK_TYPE_SHARE = 1;
	public static final int TASK_TYPE_NEWUSER = 2;
	public static final int TASK_TYPE_USER_ORDER = 3;
	public static final int TASK_TYPE_USER_WELFARE = 4;

	public static final String TASK_ACTIVE = "ACTIVE";
	public static final String TASK_DISABLED = "DISABLED";
	public static final int TASK_USE_ALL_DISTRIBUTOR = 1;
	public static final int TASK_USE_SOME_DISTRIBUTOR = 0;

	private final SalespersonTaskMapper salespersonTaskMapper;
	private final SalespersonTaskRelDistributorMapper relMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final ObjectMapper objectMapper;

	public SalespersonTaskCreateService(SalespersonTaskMapper salespersonTaskMapper,
			SalespersonTaskRelDistributorMapper relMapper,
			DistributorListQueryService distributorListQueryService,
			ObjectMapper objectMapper) {
		this.salespersonTaskMapper = salespersonTaskMapper;
		this.relMapper = relMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void create(long companyId, Map<String, Object> input) {
		String startRaw = requiredTimeField(input.get("start_time"), "任务开始时间必填", 422);
		String endRaw = requiredTimeField(input.get("end_time"), "任务结束时间必填", 422);
		Object taskNameObj = input.get("task_name");
		if (taskNameObj == null || !StringUtils.hasText(String.valueOf(taskNameObj).trim())) {
			throw new BadRequestException("任务名称必填", 422);
		}
		String taskName = String.valueOf(taskNameObj).trim();

		int taskQuota = parseTaskQuota(input.get("task_quota"), 422);

		long startTime = parseEpochSeconds(startRaw, "任务开始时间必填", 422);
		long endTime = parseEpochSeconds(endRaw, "任务结束时间必填", 422);

		int taskType = parseTaskType(input.get("task_type"), 422);

		if (startTime > endTime) {
			throw new BadRequestException("任务开始时间不能大于结束时间", 422);
		}

		List<Long> distributorIdsForOverlap = normalizeDistributorIds(input.get("distributor_id"));
		int nAll = salespersonTaskMapper.countOverlappingAllDistributorTasks(companyId, taskType, startTime, endTime,
				null);
		int nPartial = salespersonTaskMapper.countOverlappingPartialTasks(companyId, taskType, startTime, endTime,
				distributorIdsForOverlap, null);
		if (nAll > 0 || nPartial > 0) {
			throw new ResourceException("此时间段存在该类型任务");
		}

		boolean useAll = useAllDistributorFlagTrue(input.get("use_all_distributor"));
		int useAllConst = useAll ? TASK_USE_ALL_DISTRIBUTOR : TASK_USE_SOME_DISTRIBUTOR;
		List<Long> distributorIdsForRel = useAll ? List.of() : distributorIdsForOverlap;

		String picsJson = resolvePicsJson(input.get("pics"));
		String taskContent = input.get("task_content") == null ? null : String.valueOf(input.get("task_content"));

		int now = (int) (System.currentTimeMillis() / 1000L);
		SalespersonTask entity = new SalespersonTask();
		entity.setCompanyId(companyId);
		entity.setStartTime(startTime);
		entity.setEndTime(endTime);
		entity.setTaskName(taskName);
		entity.setTaskType(taskType);
		entity.setTaskQuota(taskQuota);
		entity.setPics(picsJson);
		entity.setTaskContent(taskContent);
		entity.setUseAllDistributor(useAllConst == TASK_USE_ALL_DISTRIBUTOR);
		entity.setDisabled(TASK_ACTIVE);
		entity.setCreated(now);
		entity.setUpdated(now);

		try {
			salespersonTaskMapper.insert(entity);
			Long taskId = entity.getTaskId();
			if (taskId == null) {
				throw new ResourceException("创建任务失败");
			}
			if (useAllConst == TASK_USE_ALL_DISTRIBUTOR) {
				return;
			}
			List<Distributor> distributors = distributorListQueryService.listByIdsAndCompany(companyId, distributorIdsForRel);
			for (Distributor d : distributors) {
				SalespersonTaskRelDistributor row = new SalespersonTaskRelDistributor();
				row.setTaskId(taskId);
				row.setCompanyId(companyId);
				row.setDistributorId(d.getDistributorId());
				relMapper.insert(row);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void update(long companyId, long taskId, Map<String, Object> input) {
		String startRaw = requiredTimeField(input.get("start_time"), "任务开始时间必填", 422);
		String endRaw = requiredTimeField(input.get("end_time"), "任务结束时间必填", 422);
		Object taskNameObj = input.get("task_name");
		if (taskNameObj == null || !StringUtils.hasText(String.valueOf(taskNameObj).trim())) {
			throw new BadRequestException("任务名称必填", 422);
		}
		String taskName = String.valueOf(taskNameObj).trim();

		int taskQuota = parseTaskQuota(input.get("task_quota"), 422);

		long startTime = parseEpochSeconds(startRaw, "任务开始时间必填", 422);
		long endTime = parseEpochSeconds(endRaw, "任务结束时间必填", 422);

		int taskType = parseTaskType(input.get("task_type"), 422);

		if (startTime > endTime) {
			throw new BadRequestException("任务开始时间不能大于结束时间", 422);
		}

		List<Long> distributorIdsForOverlap = normalizeDistributorIds(input.get("distributor_id"));
		Long excludeId = Long.valueOf(taskId);
		int nAll = salespersonTaskMapper.countOverlappingAllDistributorTasks(companyId, taskType, startTime, endTime,
				excludeId);
		int nPartial = salespersonTaskMapper.countOverlappingPartialTasks(companyId, taskType, startTime, endTime,
				distributorIdsForOverlap, excludeId);
		if (nAll > 0 || nPartial > 0) {
			throw new ResourceException("此时间段存在该类型任务");
		}

		boolean useAll = useAllDistributorFlagTrue(input.get("use_all_distributor"));
		int useAllConst = useAll ? TASK_USE_ALL_DISTRIBUTOR : TASK_USE_SOME_DISTRIBUTOR;
		List<Long> distributorIdsForRel = useAll ? List.of() : distributorIdsForOverlap;

		String picsJson = resolvePicsJson(input.get("pics"));
		String taskContent = input.get("task_content") == null ? null : String.valueOf(input.get("task_content"));

		SalespersonTask existing = salespersonTaskMapper.selectById(taskId);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		if (existing.getStartTime() != null && existing.getEndTime() != null
				&& existing.getStartTime() < nowSec && existing.getEndTime() > nowSec) {
			throw new ResourceException("任务开始之后不允许编辑");
		}

		int nowInt = (int) nowSec;
		existing.setCompanyId(companyId);
		existing.setStartTime(startTime);
		existing.setEndTime(endTime);
		existing.setTaskName(taskName);
		existing.setTaskType(taskType);
		existing.setTaskQuota(taskQuota);
		existing.setPics(picsJson);
		existing.setTaskContent(taskContent);
		existing.setUseAllDistributor(useAllConst == TASK_USE_ALL_DISTRIBUTOR);
		existing.setDisabled(TASK_ACTIVE);
		existing.setUpdated(nowInt);

		try {
			int rows = salespersonTaskMapper.updateById(existing);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			relMapper.delete(new LambdaQueryWrapper<SalespersonTaskRelDistributor>()
					.eq(SalespersonTaskRelDistributor::getTaskId, taskId)
					.eq(SalespersonTaskRelDistributor::getCompanyId, companyId));
			if (useAllConst == TASK_USE_ALL_DISTRIBUTOR) {
				return;
			}
			List<Distributor> distributors = distributorListQueryService.listByIdsAndCompany(companyId, distributorIdsForRel);
			for (Distributor d : distributors) {
				SalespersonTaskRelDistributor row = new SalespersonTaskRelDistributor();
				row.setTaskId(taskId);
				row.setCompanyId(companyId);
				row.setDistributorId(d.getDistributorId());
				relMapper.insert(row);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void cancel(long companyId, long taskId) {
		int nowInt = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SalespersonTask> wrapper = new LambdaUpdateWrapper<SalespersonTask>()
				.eq(SalespersonTask::getTaskId, taskId)
				.eq(SalespersonTask::getCompanyId, companyId)
				.set(SalespersonTask::getDisabled, TASK_DISABLED)
				.set(SalespersonTask::getUpdated, nowInt);
		int rows = salespersonTaskMapper.update(null, wrapper);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		if (rows > 1) {
			throw new ResourceException("数据异常");
		}
	}

	private static String requiredTimeField(Object o, String missingMsg, int embedded) {
		if (o == null) {
			throw new BadRequestException(missingMsg, embedded);
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(missingMsg, embedded);
		}
		return s;
	}

	private long parseEpochSeconds(String raw, String badMsg, int embedded) {
		try {
			if (raw.matches("-?\\d+")) {
				return Long.parseLong(raw);
			}
			BigDecimal bd = new BigDecimal(raw);
			if (bd.scale() > 0) {
				throw new BadRequestException(badMsg, embedded);
			}
			return bd.longValueExact();
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException(badMsg, embedded);
		}
	}

	private int parseTaskQuota(Object tq, int embedded) {
		if (tq == null) {
			throw new BadRequestException("任务完成指标至少1次", embedded);
		}
		if (tq instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("任务完成指标至少1次", embedded);
			}
			try {
				BigDecimal bd = new BigDecimal(t);
				if (bd.scale() > 0) {
					throw new BadRequestException("任务完成指标至少1次", embedded);
				}
				int v = bd.intValueExact();
				if (v < 1) {
					throw new BadRequestException("任务完成指标至少1次", embedded);
				}
				return v;
			} catch (BadRequestException e) {
				throw e;
			} catch (Exception e) {
				throw new BadRequestException("任务完成指标至少1次", embedded);
			}
		}
		if (tq instanceof Number n) {
			if (n instanceof Double || n instanceof Float) {
				double d = n.doubleValue();
				if (d != Math.floor(d) || d < 1.0d || d > Integer.MAX_VALUE) {
					throw new BadRequestException("任务完成指标至少1次", embedded);
				}
				return (int) d;
			}
			long lv = n.longValue();
			if (lv < 1L || lv > Integer.MAX_VALUE) {
				throw new BadRequestException("任务完成指标至少1次", embedded);
			}
			return (int) lv;
		}
		throw new BadRequestException("任务完成指标至少1次", embedded);
	}

	private int parseTaskType(Object tt, int embedded) {
		if (tt == null) {
			throw new BadRequestException("任务类型必填", embedded);
		}
		int v;
		try {
			if (tt instanceof Number n) {
				double d = n.doubleValue();
				if (d != Math.floor(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
					throw new BadRequestException("任务类型必填", embedded);
				}
				v = (int) d;
			} else {
				String s = String.valueOf(tt).trim();
				if (!StringUtils.hasText(s)) {
					throw new BadRequestException("任务类型必填", embedded);
				}
				v = Integer.parseInt(s);
			}
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("任务类型必填", embedded);
		}
		if (v != TASK_TYPE_SHARE && v != TASK_TYPE_NEWUSER && v != TASK_TYPE_USER_ORDER && v != TASK_TYPE_USER_WELFARE) {
			throw new BadRequestException("任务类型必填", embedded);
		}
		return v;
	}

	private List<Long> normalizeDistributorIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Long> out = new ArrayList<>();
			for (Object o : list) {
				Long id = parseLongId(o);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		if (raw instanceof Collection<?> col) {
			List<Long> out = new ArrayList<>();
			for (Object o : col) {
				Long id = parseLongId(o);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			List<Long> out = new ArrayList<>();
			for (Object o : arr) {
				Long id = parseLongId(o);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		Long single = parseLongId(raw);
		return single != null ? List.of(single) : List.of();
	}

	private Long parseLongId(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 是否视为「全部门店」：请求体中 {@code use_all_distributor} 的 Java 侧真值判定。
	 * 假：{@code null}、{@code false}、数值零、空串、{@code "0"}、空 {@link Map}/{@link Collection}/数组。
	 * 真：非零数值、非空且非 {@code "0"} 的字符串（含无法解析为整数的字符串）、非空集合/映射/数组，以及其它未列出的非空对象。
	 */
	private boolean useAllDistributorFlagTrue(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Boolean b) {
			return b;
		}
		if (o instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			try {
				BigDecimal bd = new BigDecimal(t);
				if (bd.scale() > 0) {
					return true;
				}
				return bd.longValueExact() != 0L;
			} catch (Exception e) {
				return true;
			}
		}
		if (o instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (o instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (o instanceof Object[] arr) {
			return arr.length > 0;
		}
		return true;
	}

	private String resolvePicsJson(Object pics) {
		if (pics == null) {
			return "[]";
		}
		if (pics instanceof String s) {
			return s.isEmpty() ? "[]" : s;
		}
		try {
			return objectMapper.writeValueAsString(pics);
		} catch (JsonProcessingException e) {
			throw new ResourceException(e.getMessage());
		}
	}
}
