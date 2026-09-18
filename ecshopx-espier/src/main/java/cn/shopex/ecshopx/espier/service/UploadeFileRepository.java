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

package cn.shopex.ecshopx.espier.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.domain.UploadeFile;
import cn.shopex.ecshopx.espier.mapper.UploadeFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadeFileRepository {

	private final UploadeFileMapper uploadeFileMapper;
	private final ObjectMapper objectMapper;
	private final UploadeFileRelationIdSchemaSupport relationIdSchemaSupport;

	public UploadeFileRepository(
			UploadeFileMapper uploadeFileMapper,
			ObjectMapper objectMapper,
			UploadeFileRelationIdSchemaSupport relationIdSchemaSupport) {
		this.uploadeFileMapper = uploadeFileMapper;
		this.objectMapper = objectMapper;
		this.relationIdSchemaSupport = relationIdSchemaSupport;
	}

	public LinkedHashMap<String, Object> create(
			long companyId,
			long operatorId,
			long supplierId,
			long distributorId,
			long merchantId,
			long relationId,
			String requestFileType,
			MultipartFile file,
			String storagePath,
			boolean queued,
			int uploadEpochSeconds) {
		UploadeFile e = new UploadeFile();
		e.setCompanyId(companyId);
		e.setOperatorId(operatorId);
		e.setSupplierId(supplierId);
		e.setDistributorId(distributorId);
		e.setMerchantId(merchantId);
		e.setRelationId(relationId);
		e.setFileType(requestFileType);
		String fn = file.getOriginalFilename();
		e.setFileName(fn == null || fn.isBlank() ? "upload.xlsx" : fn);
		e.setFileSize(String.valueOf(file.getSize()));
		e.setHandleStatus("wait");
		e.setHandleLineNum("0");
		e.setFinishTime(null);
		e.setHandleMessage(null);
		e.setCreated(uploadEpochSeconds);
		e.setUpdated(uploadEpochSeconds);
		e.setLeftJobNum(1);
		uploadeFileMapper.insert(e);
		if (relationId > 0 && relationIdSchemaSupport.isColumnPresent() && e.getId() != null) {
			uploadeFileMapper.updateRelationId(e.getId(), relationId);
		}
		LinkedHashMap<String, Object> row = getColumnNamesData(e);
		row.put("relation_id", relationId);
		return row;
	}

	/**
	 * 按数值主键查询；其它功能使用；上传错误文件导出接口请使用 {@link #getInfoByRouteId(String)}。
	 */
	public LinkedHashMap<String, Object> getInfoById(long id) {
		UploadeFile e = uploadeFileMapper.selectById(id);
		if (e == null) {
			return new LinkedHashMap<>();
		}
		return getColumnNamesData(e);
	}

	/**
	 * 按路径参数 id 的字符串形式查询上传文件表主键列：先 trim，再按主键取至多一行，
	 * 无匹配行则返回空映射，有则返回与 {@link #getColumnNamesData(UploadeFile)} 一致的列映射。
	 */
	public LinkedHashMap<String, Object> getInfoByRouteId(String id) {
		String key = id == null ? "" : id.trim();
		UploadeFile e = uploadeFileMapper.selectOne(
				new LambdaQueryWrapper<UploadeFile>().apply("id = {0}", key).last("LIMIT 1"));
		if (e == null) {
			return new LinkedHashMap<>();
		}
		return getColumnNamesData(e);
	}

	public long countScheduleDeleteErrorFileCandidates(long cutoffEpochSeconds) {
		LambdaQueryWrapper<UploadeFile> w = new LambdaQueryWrapper<UploadeFile>()
				.eq(UploadeFile::getHandleStatus, "finish")
				.le(UploadeFile::getFinishTime, cutoffEpochSeconds);
		return uploadeFileMapper.selectCount(w);
	}

	public LinkedHashMap<String, Object> listScheduleDeleteErrorFileCandidates(
			long cutoffEpochSeconds, int page, int pageSize) {
		LambdaQueryWrapper<UploadeFile> w = new LambdaQueryWrapper<UploadeFile>()
				.eq(UploadeFile::getHandleStatus, "finish")
				.le(UploadeFile::getFinishTime, cutoffEpochSeconds);
		long totalCount = uploadeFileMapper.selectCount(w);
		w.orderByDesc(UploadeFile::getCreated);
		Page<UploadeFile> pg = Page.of(page, pageSize);
		Page<UploadeFile> pageResult = uploadeFileMapper.selectPage(pg, w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (UploadeFile row : pageResult.getRecords()) {
			list.add(getColumnNamesData(row));
		}
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	public LinkedHashMap<String, Object> lists(
			long companyId,
			String fileType,
			long supplierId,
			Long merchantIdOrNull,
			Long distributorIdOrNull,
			Long relationIdOrNull,
			int page,
			int pageSize) {
		LambdaQueryWrapper<UploadeFile> w = new LambdaQueryWrapper<UploadeFile>()
				.eq(UploadeFile::getCompanyId, companyId)
				.eq(UploadeFile::getFileType, fileType)
				.eq(UploadeFile::getSupplierId, supplierId);
		if (merchantIdOrNull != null) {
			w.eq(UploadeFile::getMerchantId, merchantIdOrNull);
		}
		if (distributorIdOrNull != null) {
			w.eq(UploadeFile::getDistributorId, distributorIdOrNull);
		}
		if (relationIdOrNull != null && relationIdSchemaSupport.isColumnPresent()) {
			w.apply("relation_id = {0}", relationIdOrNull);
		}
		long totalCount = uploadeFileMapper.selectCount(w);
		w.orderByDesc(UploadeFile::getCreated);
		Page<UploadeFile> pg = Page.of(page, pageSize);
		Page<UploadeFile> pageResult = uploadeFileMapper.selectPage(pg, w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (UploadeFile row : pageResult.getRecords()) {
			list.add(getColumnNamesData(row));
		}
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", list);
		return data;
	}

	public LinkedHashMap<String, Object> updateOneBy(Map<String, Object> filter, Map<String, Object> data) {
		Object idObj = filter.get("id");
		if (idObj == null) {
			throw new ResourceException("未查询到更新数据");
		}
		long id = toLong(idObj);
		LambdaQueryWrapper<UploadeFile> w = new LambdaQueryWrapper<UploadeFile>().eq(UploadeFile::getId, id);
		Object ljn = filter.get("left_job_num");
		if (ljn != null) {
			w.eq(UploadeFile::getLeftJobNum, toInt(ljn));
		}
		UploadeFile e = uploadeFileMapper.selectOne(w);
		if (e == null) {
			throw new ResourceException("未查询到更新数据");
		}
		applyData(e, data);
		uploadeFileMapper.updateById(e);
		return getColumnNamesData(e);
	}

	private void applyData(UploadeFile e, Map<String, Object> data) {
		if (data.containsKey("handle_status")) {
			e.setHandleStatus(stringOrNull(data.get("handle_status")));
		}
		if (data.containsKey("handle_line_num")) {
			e.setHandleLineNum(stringOrNull(data.get("handle_line_num")));
		}
		if (data.containsKey("finish_time")) {
			e.setFinishTime(toLongOrNull(data.get("finish_time")));
		}
		if (data.containsKey("handle_message")) {
			Object hm = data.get("handle_message");
			if (hm == null) {
				e.setHandleMessage(null);
			} else if (hm instanceof String s) {
				e.setHandleMessage(s);
			} else {
				try {
					e.setHandleMessage(objectMapper.writeValueAsString(hm));
				} catch (Exception ex) {
					throw new BadRequestException("导入结果序列化失败");
				}
			}
		}
		if (data.containsKey("updated")) {
			e.setUpdated(toIntOrNull(data.get("updated")));
		}
		if (data.containsKey("left_job_num")) {
			e.setLeftJobNum(toIntOrNull(data.get("left_job_num")));
		}
	}

	public void updateStatusSimple(long id, String handleStatus, Object handleMessage, int leftJobNum, long finishTime) {
		LambdaUpdateWrapper<UploadeFile> u = new LambdaUpdateWrapper<UploadeFile>()
				.eq(UploadeFile::getId, id)
				.set(UploadeFile::getHandleStatus, handleStatus)
				.set(UploadeFile::getLeftJobNum, leftJobNum)
				.set(UploadeFile::getFinishTime, finishTime)
				.set(UploadeFile::getUpdated, (int) (System.currentTimeMillis() / 1000L));
		if (handleMessage == null) {
			u.set(UploadeFile::getHandleMessage, null);
		} else if (handleMessage instanceof String s) {
			u.set(UploadeFile::getHandleMessage, s);
		} else {
			try {
				u.set(UploadeFile::getHandleMessage, objectMapper.writeValueAsString(handleMessage));
			} catch (Exception ex) {
				throw new BadRequestException("导入结果序列化失败");
			}
		}
		uploadeFileMapper.update(null, u);
	}

	public LinkedHashMap<String, Object> getColumnNamesData(UploadeFile entity) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", entity.getId() == null ? null : String.valueOf(entity.getId()));
		m.put("company_id", entity.getCompanyId() == null ? null : String.valueOf(entity.getCompanyId()));
		m.put("operator_id", entity.getOperatorId() == null ? null : String.valueOf(entity.getOperatorId()));
		m.put("supplier_id", entity.getSupplierId());
		m.put("file_name", entity.getFileName());
		m.put("file_type", entity.getFileType());
		m.put("file_size", parseLongOrString(entity.getFileSize()));
		m.put("file_size_format", formatFilesize(entity.getFileSize()));
		m.put("handle_status", entity.getHandleStatus());
		m.put("handle_line_num", parseLongOrString(entity.getHandleLineNum()));
		m.put("finish_time", entity.getFinishTime());
		Long ft = entity.getFinishTime();
		m.put("finish_date", ft == null || ft == 0 ? null : formatEpochSeconds(ft));
		m.put("handle_message", decodeHandleMessage(entity.getHandleMessage()));
		m.put("created", entity.getCreated());
		Integer cr = entity.getCreated();
		m.put("created_date", cr == null ? null : formatEpochSeconds(cr.longValue()));
		m.put("updated", entity.getUpdated());
		m.put("distributor_id", entity.getDistributorId());
		putRelationIdIfPresent(entity, m);
		m.put("left_job_num", entity.getLeftJobNum());
		m.put("merchant_id", entity.getMerchantId() == null ? null : String.valueOf(entity.getMerchantId()));
		return m;
	}

	private void putRelationIdIfPresent(UploadeFile entity, LinkedHashMap<String, Object> m) {
		if (!relationIdSchemaSupport.isColumnPresent() || entity.getId() == null) {
			return;
		}
		Long relationId = uploadeFileMapper.selectRelationId(entity.getId());
		if (relationId != null && relationId > 0) {
			m.put("relation_id", relationId);
		}
	}

	private static Object parseLongOrString(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return raw;
		}
	}

	private Object decodeHandleMessage(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Object>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private static String formatEpochSeconds(long epoch) {
		return java.time.Instant.ofEpochSecond(epoch)
				.atZone(java.time.ZoneId.of("Asia/Shanghai"))
				.toLocalDateTime()
				.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	private static String formatFilesize(String fileSize) {
		if (fileSize == null || fileSize.isBlank()) {
			return "0B";
		}
		try {
			double bytes = Double.parseDouble(fileSize);
			if (bytes < 1024) {
				return ((long) bytes) + "B";
			}
			if (bytes < Math.pow(1024, 2)) {
				return String.valueOf(Math.round(bytes / 1024 * 100.0) / 100.0) + "KB";
			}
			double mb = bytes / Math.pow(1024, 2);
			return String.valueOf(Math.round(mb * 100.0) / 100.0) + "MB";
		} catch (NumberFormatException e) {
			return "0B";
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}

	private static Long toLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (s.isEmpty()) {
			return null;
		}
		return Long.parseLong(s);
	}

	private static Integer toIntOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(o));
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(o));
	}

	private static String stringOrNull(Object o) {
		return o == null ? null : String.valueOf(o);
	}
}
