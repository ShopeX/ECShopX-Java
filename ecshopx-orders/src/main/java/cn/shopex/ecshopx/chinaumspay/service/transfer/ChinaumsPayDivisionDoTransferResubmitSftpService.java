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

package cn.shopex.ecshopx.chinaumspay.service.transfer;

import static cn.shopex.ecshopx.chinaumspay.service.DivisionErrorLogResubmitService.IS_RESUBMIT_SUCC;
import static cn.shopex.ecshopx.chinaumspay.service.DivisionErrorLogResubmitService.IS_RESUBMIT_WAITING;
import static cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferSftpService.FILE_TYPE_DIVISION;
import static cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferSftpService.FILE_TYPE_TRANSFER;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionErrorLog;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionErrorLogMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 划付失败重传 SFTP：从错误日志+历史 {@code upload_detail} 重组文件上传，不访问
 * {@code orders_rel_chinaumspay_division}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChinaumsPayDivisionDoTransferResubmitSftpService {

	private static final String BACK_STATUS_NOT = "0";
	private static final TypeReference<LinkedHashMap<String, Object>> JSON_MAP =
			new TypeReference<LinkedHashMap<String, Object>>() {};

	private final ChinaumspayDivisionErrorLogMapper chinaumspayDivisionErrorLogMapper;
	private final ChinaumspayDivisionUploadDetailMapper chinaumspayDivisionUploadDetailMapper;
	private final ChinaumspayDivisionUploadLogMapper chinaumspayDivisionUploadLogMapper;
	private final ChinaumsDivisionLocalArtifactWriterPort chinaumsDivisionLocalArtifactWriterPort;
	private final ChinaumsDivisionRemoteUploadPort chinaumsDivisionRemoteUploadPort;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.ums.group-no:}")
	private String umsGroupNo;

	public long getErrorlogResubmitCount() {
		return chinaumspayDivisionErrorLogMapper.selectCount(
				new LambdaQueryWrapper<ChinaumspayDivisionErrorLog>()
						.eq(ChinaumspayDivisionErrorLog::getIsResubmit, IS_RESUBMIT_WAITING));
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean doTransferResubmitSftp(long companyId) {
		ReTransferPayload payload = loadReTransferData(companyId);
		if (payload == null) {
			log.info("company_id:{},没有需要重试的划付数据", companyId);
			return false;
		}
		String ymd = DateTimeFormatter.BASIC_ISO_DATE.withZone(ZoneId.systemDefault()).format(Instant.now());
		String localPath = "chinaumsPayment/" + ymd;
		String remotePath = "/upload/" + ymd;
		Map<String, Object> forUpload = new LinkedHashMap<>();
		forUpload.put("transfer", payload.transfer);
		forUpload.put("division", payload.division);
		forUpload.put("division_ids", payload.divisionIds);
		forUpload.put("error_log_ids", payload.errorLogIds);
		forUpload.put("local_file_path", localPath);
		forUpload.put("remote_file_path", remotePath);
		log.info("reDivsionData===> {}", forUpload);
		try {
			ResubmitFileBlob t =
					createResubmitUploadLog(companyId, FILE_TYPE_TRANSFER, forUpload, ymd, localPath, remotePath);
			chinaumsDivisionLocalArtifactWriterPort.put(t.localRelative, t.content);
			log.info("localFile:{}", t.localRelative);
			chinaumsDivisionRemoteUploadPort.uploadSign(companyId, t.dirRel, remotePath, t.fileName);
			chinaumsDivisionRemoteUploadPort.uploadData(companyId, t.localRelative, remotePath, t.fileName);

			ResubmitFileBlob d =
					createResubmitUploadLog(companyId, FILE_TYPE_DIVISION, forUpload, ymd, localPath, remotePath);
			chinaumsDivisionLocalArtifactWriterPort.put(d.localRelative, d.content);
			log.info("localFile:{}", d.localRelative);
			chinaumsDivisionRemoteUploadPort.uploadSign(companyId, d.dirRel, remotePath, d.fileName);
			chinaumsDivisionRemoteUploadPort.uploadData(companyId, d.localRelative, remotePath, d.fileName);

			int now = (int) Instant.now().getEpochSecond();
			ChinaumspayDivisionErrorLog row = new ChinaumspayDivisionErrorLog();
			row.setIsResubmit(IS_RESUBMIT_SUCC);
			row.setUpdateTime(now);
			LambdaUpdateWrapper<ChinaumspayDivisionErrorLog> uw = new LambdaUpdateWrapper<>();
			uw.in(ChinaumspayDivisionErrorLog::getId, payload.errorLogIds);
			uw.eq(ChinaumspayDivisionErrorLog::getCompanyId, companyId);
			chinaumspayDivisionErrorLogMapper.update(row, uw);
			return true;
		} catch (Exception e) {
			String msg = "file:" + e.getClass().getName() + ",line:—,msg:" + e.getMessage();
			log.info("重新提交划付上传失败 ===>{}", msg);
			String em = e.getMessage() == null ? "重新提交划付上传失败" : e.getMessage();
			log.error("company_id:{}, 重新提交划付上传失败", companyId, e);
			throw new ResourceException(em);
		}
	}

	private ReTransferPayload loadReTransferData(long companyId) {
		LambdaQueryWrapper<ChinaumspayDivisionErrorLog> w = new LambdaQueryWrapper<>();
		w.eq(ChinaumspayDivisionErrorLog::getCompanyId, companyId)
				.eq(ChinaumspayDivisionErrorLog::getIsResubmit, IS_RESUBMIT_WAITING);
		List<ChinaumspayDivisionErrorLog> errorLogList = chinaumspayDivisionErrorLogMapper.selectList(w);
		if (CollectionUtils.isEmpty(errorLogList)) {
			return null;
		}
		log.info("getReTransferData errorLogList result===> {}", errorLogList);
		List<Long> uploadDetailIds =
				errorLogList.stream()
						.map(ChinaumspayDivisionErrorLog::getUploadDetailId)
						.filter(id -> id != null && id > 0)
						.distinct()
						.collect(Collectors.toList());
		List<ChinaumspayDivisionUploadDetail> details =
				uploadDetailIds.isEmpty() ? List.of() : chinaumspayDivisionUploadDetailMapper.selectBatchIds(uploadDetailIds);
		List<Map<String, Object>> uploadTransfer = new ArrayList<>();
		List<Map<String, Object>> uploadDivision = new ArrayList<>();
		for (ChinaumspayDivisionUploadDetail value : details) {
			if (value == null || value.getDetail() == null) {
				continue;
			}
			try {
				LinkedHashMap<String, Object> detail = objectMapper.readValue(value.getDetail(), JSON_MAP);
				detail.put("upload_detail_id", value.getId());
				if (FILE_TYPE_DIVISION.equals(value.getFileType())) {
					uploadDivision.add(detail);
				} else if (FILE_TYPE_TRANSFER.equals(value.getFileType())) {
					uploadTransfer.add(detail);
				}
			} catch (Exception e) {
				throw new IllegalStateException("upload_detail json, id=" + value.getId(), e);
			}
		}
		ReTransferPayload result = new ReTransferPayload();
		result.division = uploadDivision;
		result.transfer = uploadTransfer;
		result.divisionIds = errorLogList.stream()
				.map(ChinaumspayDivisionErrorLog::getDivisionId)
				.collect(Collectors.toList());
		result.errorLogIds = errorLogList.stream().map(ChinaumspayDivisionErrorLog::getId).collect(Collectors.toList());
		return result;
	}

	@SuppressWarnings("unchecked")
	private ResubmitFileBlob createResubmitUploadLog(
			long companyId,
			String fileType,
			Map<String, Object> divisionData,
			String ymd,
			String localPath,
			String remotePath) {
		String ts =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(ZonedDateTime.now(ZoneId.systemDefault()));
		String fileName =
				FILE_TYPE_DIVISION.equals(fileType) ? ("04_" + umsGroupNo + "_" + ts + ".txt") : ("02_" + umsGroupNo + "_" + ts + ".txt");
		List<Map<String, Object>> rows = (List<Map<String, Object>>) divisionData.get(fileType);
		if (rows == null) {
			rows = List.of();
		}
		String firstLine = String.join("|", umsGroupNo, String.valueOf(rows.size())) + "\n";
		StringBuilder sb = new StringBuilder();
		sb.append(firstLine);
		int now = (int) Instant.now().getEpochSecond();
		for (Map<String, Object> divRow0 : rows) {
			if (divRow0 == null) {
				continue;
			}
			LinkedHashMap<String, Object> division = new LinkedHashMap<>(divRow0);
			Object udi = division.get("upload_detail_id");
			if (udi != null) {
				long detailId = toLong(udi, "upload_detail_id");
				ChinaumspayDivisionUploadDetail fromDb = chinaumspayDivisionUploadDetailMapper.selectById(detailId);
				if (fromDb == null) {
					throw new IllegalStateException("upload_detail not found: " + detailId);
				}
				int oldTimes = fromDb.getTimes() == null ? 0 : fromDb.getTimes();
				int newTimes = oldTimes + 1;
				fromDb.setTimes(newTimes);
				fromDb.setBackStatus(BACK_STATUS_NOT);
				fromDb.setUpdateTime(now);
				chinaumspayDivisionUploadDetailMapper.updateById(fromDb);
				division.remove("upload_detail_id");
				long rowId = fromDb.getId() == null ? 0L : fromDb.getId();
				writeLineWithDivisionTweak(sb, division, rowId, newTimes, true);
			} else {
				ChinaumspayDivisionUploadDetail up = new ChinaumspayDivisionUploadDetail();
				up.setCompanyId(companyId);
				Object divId0 = division.get("division_id");
				if (divId0 != null) {
					String ds = String.valueOf(divId0);
					if (ds.matches("\\d+")) {
						up.setDivisionId(Long.parseLong(ds));
					}
				}
				Object d2 = division.get("distributor_id");
				if (d2 != null) {
					up.setDistributorId(Long.parseLong(String.valueOf(d2)));
				}
				up.setFileType(fileType);
				try {
					up.setDetail(objectMapper.writeValueAsString(division));
				} catch (Exception e) {
					throw new IllegalStateException("detail json", e);
				}
				up.setTimes(1);
				up.setBackStatus(BACK_STATUS_NOT);
				up.setCreateTime(now);
				up.setUpdateTime(now);
				chinaumspayDivisionUploadDetailMapper.insert(up);
				division.remove("distributor_id");
				writeLineWithDivisionTweak(
						sb, division, up.getId() == null ? 0L : up.getId(), up.getTimes() == null ? 1 : up.getTimes(), false);
			}
		}
		String fileContent = sb.toString();
		ChinaumspayDivisionUploadLog logE = new ChinaumspayDivisionUploadLog();
		logE.setCompanyId(companyId);
		logE.setFileType(fileType);
		logE.setLocalFilePath(localPath);
		logE.setRemoteFilePath(remotePath);
		logE.setFileName(fileName);
		logE.setFileContent(fileContent);
		logE.setBackStatus(BACK_STATUS_NOT);
		logE.setCreateTime(now);
		logE.setUpdateTime(now);
		chinaumspayDivisionUploadLogMapper.insert(logE);
		String rel = localPath + "/" + fileName;
		if (rel.startsWith("/")) {
			rel = rel.substring(1);
		}
		String dir = rel.contains("/") ? rel.substring(0, rel.lastIndexOf('/')) : "";
		return new ResubmitFileBlob(fileName, fileContent, rel, dir, localPath, remotePath);
	}

	/**
	 * @param isUpdatePath false 时与 {@link ChinaumsPayDivisionDoTransferSftpService} 一样移除 distributor_id 后再拼行。
	 */
	private void writeLineWithDivisionTweak(
			StringBuilder sb, Map<String, Object> work, long detailId, int times, boolean isUpdatePath) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>(work);
		if (!isUpdatePath) {
			m.remove("distributor_id");
		}
		Object d0 = m.get("division_id");
		if (d0 == null) {
			return;
		}
		String dStr = String.valueOf(d0);
		String newId = dStr + "0" + detailId + "0" + times;
		m.put("division_id", newId);
		List<Object> line = new ArrayList<>(m.values());
		sb.append(String.join("|", line.stream().map(String::valueOf).toList())).append("\n");
	}

	private static long toLong(Object o, String field) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}

	private static final class ReTransferPayload {
		private List<Map<String, Object>> transfer;
		private List<Map<String, Object>> division;
		private List<Long> divisionIds;
		private List<Long> errorLogIds;
	}

	private static final class ResubmitFileBlob {
		private final String fileName;
		private final String content;
		/** 相对 storage 根，含文件名 */
		private final String localRelative;
		/** 目录段（与 uploadSign 一致） */
		private final String dirRel;

		@SuppressWarnings("unused")
		private final String localPath;

		@SuppressWarnings("unused")
		private final String remotePath;

		private ResubmitFileBlob(
				String fileName, String content, String localRelative, String dirRel, String localPath, String remotePath) {
			this.fileName = fileName;
			this.content = content;
			this.localRelative = localRelative;
			this.dirRel = dirRel;
			this.localPath = localPath;
			this.remotePath = remotePath;
		}
	}
}
