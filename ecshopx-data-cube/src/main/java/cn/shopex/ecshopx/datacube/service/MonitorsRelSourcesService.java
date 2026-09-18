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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MonitorsRelSourcesService {

	private static final int MAX_DRIVER_MESSAGE_LEN = 2000;

	private static final RowMapper<Map<String, Object>> REL_ROW_MAPPER =
			new RowMapper<>() {
				@Override
				public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					row.put("monitor_id", rs.getObject("monitor_id"));
					row.put("source_id", rs.getObject("source_id"));
					row.put("company_id", rs.getObject("company_id"));
					return row;
				}
			};

	private final JdbcTemplate jdbcTemplate;
	private final SourcesMapper sourcesMapper;

	public MonitorsRelSourcesService(JdbcTemplate jdbcTemplate, SourcesMapper sourcesMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.sourcesMapper = sourcesMapper;
	}

	public List<Map<String, Object>> listRelSourcesWithSourceNames(long companyId, Object rawMonitorId) {
		List<Map<String, Object>> rows;
		try {
			rows =
					jdbcTemplate.query(
							"SELECT monitor_id, source_id, company_id FROM datacube_relsources WHERE company_id = ? AND"
									+ " monitor_id = ? ORDER BY source_id ASC",
							REL_ROW_MAPPER,
							companyId,
							rawMonitorId);
		} catch (DataAccessException e) {
			throw new ResourceException(truncateDriverMessage(e));
		}
		if (rows == null || rows.isEmpty()) {
			return new ArrayList<>();
		}
		for (Map<String, Object> row : rows) {
			long sourceId = longFromJdbcObject(row.get("source_id"));
			Sources src = sourcesMapper.selectById(sourceId);
			if (src == null) {
				throw new ResourceException("source_id=" + sourceId + "的来源不存在");
			}
			row.put("source_name", src.getSourceName() != null ? src.getSourceName() : "");
		}
		return rows;
	}

	private static long longFromJdbcObject(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	@Transactional(rollbackFor = Exception.class)
	public List<Map<String, Object>> replaceRelSources(long companyId, Object rawMonitorId, List<?> sourceIds) {
		try {
			jdbcTemplate.update(
					"DELETE FROM datacube_relsources WHERE company_id = ? AND monitor_id = ?",
					companyId,
					rawMonitorId);
			if (sourceIds != null && !sourceIds.isEmpty()) {
				for (Object sourceId : sourceIds) {
					jdbcTemplate.update(
							"INSERT INTO datacube_relsources (company_id, monitor_id, source_id) VALUES (?, ?, ?)",
							companyId,
							rawMonitorId,
							sourceId);
				}
			}
			return jdbcTemplate.query(
					"SELECT monitor_id, source_id, company_id FROM datacube_relsources WHERE company_id = ? AND"
							+ " monitor_id = ? ORDER BY source_id ASC",
					REL_ROW_MAPPER,
					companyId,
					rawMonitorId);
		} catch (DataAccessException e) {
			throw new ResourceException(truncateDriverMessage(e));
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteOneRelSource(long companyId, Object rawMonitorId, Object rawSourceId) {
		if (companyId <= 0L) {
			throw new ResourceException("删除跟踪链接信息有误.");
		}
		if (isFalsyPathRaw(rawMonitorId)) {
			throw new ResourceException("删除来源监控缺少参数.");
		}
		if (isFalsyPathRaw(rawSourceId)) {
			throw new ResourceException("删除来源监控缺少参数.");
		}
		try {
			jdbcTemplate.update(
					"DELETE FROM datacube_relsources WHERE company_id = ? AND monitor_id = ? AND source_id = ?",
					companyId,
					rawMonitorId,
					rawSourceId);
		} catch (DataAccessException e) {
			throw new ResourceException(truncateDriverMessage(e));
		}
	}

	private static boolean isFalsyPathRaw(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof String s) {
			return !StringUtils.hasText(s) || "0".equals(s);
		}
		throw new IllegalStateException("deleteOneRelSource: monitor_id/source_id must be Long or String");
	}

	private static String truncateDriverMessage(DataAccessException e) {
		Throwable root = e.getMostSpecificCause();
		String msg = root != null ? root.getMessage() : null;
		if (msg == null || msg.isBlank()) {
			msg = e.getMessage();
		}
		if (msg == null) {
			return "";
		}
		if (msg.length() <= MAX_DRIVER_MESSAGE_LEN) {
			return msg;
		}
		return msg.substring(0, MAX_DRIVER_MESSAGE_LEN);
	}
}
