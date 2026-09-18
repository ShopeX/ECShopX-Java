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

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper;
import cn.shopex.ecshopx.orders.service.division.DivisionFormatResult;
import cn.shopex.ecshopx.orders.service.division.OrderDivisionRelStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 与 PHP {@code OrderDivisionService::doTransferSftp} 同一事务边界的上传段；合并键路径见
 * plan §2 / analysis §8 第 3 行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChinaumsPayDivisionDoTransferSftpService {

	public static final String FILE_TYPE_DIVISION = "division";
	public static final String FILE_TYPE_TRANSFER = "transfer";
	private static final String BACK_STATUS_NOT = "0";

	private final ChinaumspayDivisionUploadLogMapper chinaumspayDivisionUploadLogMapper;
	private final ChinaumspayDivisionUploadDetailMapper chinaumspayDivisionUploadDetailMapper;
	private final ChinaumsDivisionLocalArtifactWriterPort chinaumsDivisionLocalArtifactWriterPort;
	private final ChinaumsDivisionRemoteUploadPort chinaumsDivisionRemoteUploadPort;
	private final OrdersRelChinaumspayDivisionMapper ordersRelChinaumspayDivisionMapper;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.ums.group-no:}")
	private String umsGroupNo;

	@Transactional(rollbackFor = Exception.class)
	public boolean doTransferSftp(long companyId, List<DivisionFormatResult> divisionListData, String statusToApply) {
		if (CollectionUtils.isEmpty(divisionListData)) {
			return false;
		}
		var merged = new Merged();
		for (DivisionFormatResult data : divisionListData) {
			if (data == null) {
				continue;
			}
			mergePatch(merged, data);
		}
		if (merged.transfer.isEmpty() && merged.division.isEmpty()) {
			return false;
		}
		String ymd = DateTimeFormatter.BASIC_ISO_DATE.withZone(ZoneId.systemDefault()).format(Instant.now());
		String localPath = "chinaumsPayment/" + ymd;
		String remotePath = "/upload/" + ymd;
		merged.mergedPayload.put("local_file_path", localPath);
		merged.mergedPayload.put("remote_file_path", remotePath);
		merged.mergedPayload.put("transfer", new ArrayList<>(merged.transfer));
		merged.mergedPayload.put("division", new ArrayList<>(merged.division));
		merged.mergedPayload.put("division_ids", new ArrayList<>(merged.divisionIds));
		merged.mergedPayload.put("order_ids", new ArrayList<>(merged.orderIds));
		log.info("divisionData===> {}", merged.mergedPayload);
		try {
			UploadFileBlob u1 =
					buildUpload(companyId, FILE_TYPE_TRANSFER, merged.mergedPayload, ymd, localPath, remotePath);
			String rel1 = u1.localRelative;
			String fn1 = u1.fileName;
			chinaumsDivisionLocalArtifactWriterPort.put(rel1, u1.content);
			log.info("transfer localFile:{}", rel1);
			chinaumsDivisionRemoteUploadPort.uploadSign(companyId, u1.dirRel, remotePath, fn1);
			chinaumsDivisionRemoteUploadPort.uploadData(companyId, rel1, remotePath, fn1);
			UploadFileBlob u2 =
					buildUpload(companyId, FILE_TYPE_DIVISION, merged.mergedPayload, ymd, localPath, remotePath);
			String rel2 = u2.localRelative;
			String fn2 = u2.fileName;
			chinaumsDivisionLocalArtifactWriterPort.put(rel2, u2.content);
			log.info("division localFile:{}", rel2);
			chinaumsDivisionRemoteUploadPort.uploadSign(companyId, u2.dirRel, remotePath, fn2);
			chinaumsDivisionRemoteUploadPort.uploadData(companyId, rel2, remotePath, fn2);
			if (!merged.orderIds.isEmpty()) {
				int now = (int) Instant.now().getEpochSecond();
				var uw = new LambdaUpdateWrapper<OrdersRelChinaumspayDivision>();
				uw.in(OrdersRelChinaumspayDivision::getOrderId, merged.orderIds);
				uw.set(OrdersRelChinaumspayDivision::getStatus, statusToApply);
				uw.set(OrdersRelChinaumspayDivision::getUpdateTime, now);
				ordersRelChinaumspayDivisionMapper.update(null, uw);
			}
			return true;
		} catch (Exception e) {
			String msg = "file:" + e.getClass().getName() + ",line:—,msg:" + e.getMessage();
			log.info("company_id:{},划付上传失败 ===>{}", companyId, msg);
			String em = e.getMessage() == null ? "划付上传失败" : e.getMessage();
			log.error("company_id:{}, 划付上传失败", companyId, e);
			throw new ResourceException(em);
		}
	}

	private void mergePatch(Merged m, DivisionFormatResult data) {
		if (data.getTransfer() != null) {
			m.transfer.addAll(data.getTransfer());
		}
		if (data.getDivision() != null) {
			m.division.addAll(data.getDivision());
		}
		if (data.getDivisionId() > 0) {
			m.divisionIds.add(data.getDivisionId());
		}
		if (data.getOrderIds() != null) {
			m.orderIds.addAll(data.getOrderIds());
		}
	}

	private static final class Merged {
		private final List<Map<String, Object>> transfer = new ArrayList<>();
		private final List<Map<String, Object>> division = new ArrayList<>();
		private final List<Long> divisionIds = new ArrayList<>();
		private final List<Long> orderIds = new ArrayList<>();
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		private final Map<String, Object> mergedPayload = new LinkedHashMap<>();
	}

	/** 与 PHP createUploadLog 输出同源 {@code file_content} 并落库（新建明细路径）。 */
	@SuppressWarnings("unchecked")
	private UploadFileBlob buildUpload(
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
		var sb = new StringBuilder();
		sb.append(firstLine);
		int now = (int) Instant.now().getEpochSecond();
		for (Map<String, Object> divRow : rows) {
			if (divRow == null) {
				continue;
			}
			LinkedHashMap<String, Object> work = new LinkedHashMap<>(divRow);
			if (work.containsKey("upload_detail_id")) {
				continue;
			}
			ChinaumspayDivisionUploadDetail up = new ChinaumspayDivisionUploadDetail();
			up.setCompanyId(companyId);
			Object divId0 = work.get("division_id");
			if (divId0 != null) {
				String ds = String.valueOf(divId0);
				if (ds.matches("\\d+")) {
					up.setDivisionId(Long.parseLong(ds));
				}
			}
			Object d2 = work.get("distributor_id");
			if (d2 != null) {
				up.setDistributorId(Long.parseLong(String.valueOf(d2)));
			}
			up.setFileType(fileType);
			try {
				up.setDetail(objectMapper.writeValueAsString(divRow));
			} catch (Exception e) {
				throw new IllegalStateException("detail json", e);
			}
			up.setTimes(1);
			up.setBackStatus(BACK_STATUS_NOT);
			up.setCreateTime(now);
			up.setUpdateTime(now);
			chinaumspayDivisionUploadDetailMapper.insert(up);
			work.remove("distributor_id");
			writeLineWithDivisionTweak(
					sb, work, up.getId() == null ? 0L : up.getId(), up.getTimes() == null ? 1 : up.getTimes());
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
		return new UploadFileBlob(fileName, fileContent, rel, dir, localPath, remotePath);
	}

	/** 与 PHP 中 division_id 注入 upload_detail 主键/次数 拼接指令 ID 的语义一致。 */
	private void writeLineWithDivisionTweak(StringBuilder sb, Map<String, Object> work, long detailId, int times) {
		Object d0 = work.get("division_id");
		if (d0 == null) {
			return;
		}
		String dStr = String.valueOf(d0);
		String newId = dStr + "0" + detailId + "0" + times;
		work.put("division_id", newId);
		List<Object> line = new ArrayList<>(work.values());
		sb.append(String.join("|", line.stream().map(String::valueOf).toList())).append("\n");
	}

	private static final class UploadFileBlob {
		private final String fileName;
		private final String content;
		/** 相对 storage 根的文件全路径，含文件名 */
		private final String localRelative;
		/** 目录段（与文件名同父，用于 uploadSign） */
		private final String dirRel;
		@SuppressWarnings("unused")
		private final String localPath;
		@SuppressWarnings("unused")
		private final String remotePath;

		private UploadFileBlob(
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
