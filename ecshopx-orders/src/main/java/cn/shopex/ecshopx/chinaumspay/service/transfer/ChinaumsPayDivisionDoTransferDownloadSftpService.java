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

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionErrorLog;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadDetail;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionErrorLogMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalRetFileAccessPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteDownloadPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRetSignVerifyPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper;
import cn.shopex.ecshopx.orders.service.division.OrderDivisionRelStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChinaumsPayDivisionDoTransferDownloadSftpService {

	public static final String BACK_STATUS_FAIL = "4";
	public static final String BACK_STATUS_ONGOING = "1";
	private static final String UPLOAD_LOG_BACK_DONE = "1";

	private final ChinaumspayDivisionUploadLogMapper chinaumspayDivisionUploadLogMapper;
	private final ChinaumspayDivisionUploadDetailMapper chinaumspayDivisionUploadDetailMapper;
	private final ChinaumspayDivisionErrorLogMapper chinaumspayDivisionErrorLogMapper;
	private final ChinaumspayDivisionDetailMapper chinaumspayDivisionDetailMapper;
	private final OrdersRelChinaumspayDivisionMapper ordersRelChinaumspayDivisionMapper;
	private final ChinaumsDivisionRemoteDownloadPort chinaumsDivisionRemoteDownloadPort;
	private final ChinaumsDivisionRetSignVerifyPort chinaumsDivisionRetSignVerifyPort;
	private final ChinaumsDivisionLocalRetFileAccessPort chinaumsDivisionLocalRetFileAccessPort;

	@Transactional(rollbackFor = Exception.class)
	public boolean doTransferDownloadSftp(ChinaumspayDivisionUploadLog uploadLog) {
		long companyId = uploadLog.getCompanyId() == null ? 0L : uploadLog.getCompanyId();
		String localPath = uploadLog.getLocalFilePath() == null ? "" : uploadLog.getLocalFilePath();
		String remotePath = uploadLog.getRemoteFilePath() == null ? "" : uploadLog.getRemoteFilePath();
		String fileName = uploadLog.getFileName() == null ? "" : uploadLog.getFileName();
		try {
			chinaumsDivisionLocalRetFileAccessPort.ensureStorageDirectoryForRelativePath(localPath);
			chinaumsDivisionRemoteDownloadPort.downloadFinalRetToStorage(companyId, localPath, remotePath, fileName);
			String relRet = joinRelative(localPath, "final_" + fileName + ".ret");
			chinaumsDivisionRetSignVerifyPort.verifyDataFileSign(companyId, relRet, remotePath, fileName);
			return doFinalTransfer(uploadLog);
		} catch (Exception e) {
			String msg = "file:" + e.getClass().getName() + ",line:—,msg:" + e.getMessage();
			log.info("划付回盘失败 ===>{}", msg);
			String em = e.getMessage() == null ? "划付回盘失败" : e.getMessage();
			log.error("划付回盘失败", e);
			throw new ResourceException(em);
		}
	}

	private boolean doFinalTransfer(ChinaumspayDivisionUploadLog uploadLog) throws IOException {
		String localPath = uploadLog.getLocalFilePath() == null ? "" : uploadLog.getLocalFilePath();
		String fileName = uploadLog.getFileName() == null ? "" : uploadLog.getFileName();
		String relRet = joinRelative(localPath, "final_" + fileName + ".ret");
		log.info("日终回盘文件处理{}", relRet);
		if (!chinaumsDivisionLocalRetFileAccessPort.exists(relRet)) {
			log.info("未查询到日终回盘文件");
			return true;
		}
		String content = chinaumsDivisionLocalRetFileAccessPort.readStringUtf8(relRet);
		ChinaumsPayDivisionRetResponseCodec.FormattedRet parsed =
				ChinaumsPayDivisionRetResponseCodec.parseFileContent(content);
		if (ChinaumsPayDivisionRetResponseCodec.STATUS_FILE_ERROR.equals(parsed.status)) {
			doFileError(uploadLog, parsed.fileErrorMessage == null ? "" : parsed.fileErrorMessage);
			log.info("日终回盘文件失败，原因：{}", parsed.fileErrorMessage);
			return true;
		}
		if (CollectionUtils.isEmpty(parsed.dataRows)) {
			log.info("没有需要处理的数据 content:{}", content);
			return true;
		}
		int now = (int) Instant.now().getEpochSecond();
		for (Map<String, String> value : parsed.dataRows) {
			if (value == null) {
				continue;
			}
			String compositeId = value.get("id");
			long[] triple = ChinaumsPayDivisionRetResponseCodec.parseRetIdTriple(compositeId);
			long uploadDetailId = triple[1];
			if (uploadDetailId <= 0) {
				continue;
			}
			String back = ChinaumsPayDivisionRetResponseCodec.mapUmsStatusCharToLocalBackStatus(value.get("status"));
			ChinaumspayDivisionUploadDetail upd = new ChinaumspayDivisionUploadDetail();
			upd.setId(uploadDetailId);
			upd.setBacksuccFee(parseFee(value.get("final_fee")));
			upd.setRateFee(parseFee(value.get("rate_fee")));
			upd.setBackStatus(back);
			upd.setBackStatusMsg(value.get("status_msg"));
			upd.setChinaumspayId(value.get("chinaumspay_id"));
			upd.setUpdateTime(now);
			chinaumspayDivisionUploadDetailMapper.updateById(upd);
			ChinaumspayDivisionUploadDetail fresh =
					chinaumspayDivisionUploadDetailMapper.selectById(uploadDetailId);
			if (fresh == null) {
				continue;
			}
			if (Objects.equals(fresh.getBackStatus(), BACK_STATUS_FAIL)
					|| Objects.equals(fresh.getBackStatus(), BACK_STATUS_ONGOING)) {
				insertErrorLog(fresh, now);
			}
		}
		ChinaumspayDivisionUploadLog u = new ChinaumspayDivisionUploadLog();
		u.setId(uploadLog.getId());
		u.setBackStatus(UPLOAD_LOG_BACK_DONE);
		u.setUpdateTime(now);
		chinaumspayDivisionUploadLogMapper.updateById(u);
		return true;
	}

	private void insertErrorLog(ChinaumspayDivisionUploadDetail d, int now) {
		ChinaumspayDivisionErrorLog el = new ChinaumspayDivisionErrorLog();
		el.setCompanyId(d.getCompanyId());
		el.setDivisionId(d.getDivisionId());
		el.setUploadDetailId(d.getId());
		el.setType(d.getFileType());
		el.setDistributorId(d.getDistributorId());
		el.setStatus(d.getBackStatus());
		el.setErrorDesc(d.getBackStatusMsg());
		el.setIsResubmit(0);
		el.setCreateTime(now);
		el.setUpdateTime(now);
		chinaumspayDivisionErrorLogMapper.insert(el);
	}

	/**
	 * 整文件失败：按行解析 {@code file_content}、批量失败 {@code upload_detail}、关联单回退为待划付。
	 */
	private boolean doFileError(ChinaumspayDivisionUploadLog uploadLog, String errorMsg) {
		String fc = uploadLog.getFileContent();
		if (!StringUtils.hasText(fc)) {
			return false;
		}
		String[] lines = fc.split("\n", -1);
		Set<Long> divisionIds = new LinkedHashSet<>();
		List<Long> uploadDetailIds = new ArrayList<>();
		for (String row : lines) {
			if (!StringUtils.hasText(row)) {
				continue;
			}
			String retId = row.split("\\|", 2)[0];
			long[] triple = ChinaumsPayDivisionRetResponseCodec.parseRetIdTriple(retId);
			if (triple[0] > 0) {
				divisionIds.add(triple[0]);
			}
			if (triple[1] > 0) {
				uploadDetailIds.add(triple[1]);
			}
		}
		if (divisionIds.isEmpty() || uploadDetailIds.isEmpty()) {
			return false;
		}
		int now = (int) Instant.now().getEpochSecond();
		var uw = new UpdateWrapper<ChinaumspayDivisionUploadDetail>();
		uw.in("id", uploadDetailIds);
		uw.set("back_status", BACK_STATUS_FAIL);
		uw.set("back_status_msg", errorMsg);
		uw.set("update_time", now);
		chinaumspayDivisionUploadDetailMapper.update(null, uw);
		var dq = new LambdaQueryWrapper<ChinaumspayDivisionDetail>();
		dq.in(ChinaumspayDivisionDetail::getDivisionId, divisionIds);
		List<ChinaumspayDivisionDetail> divRows = chinaumspayDivisionDetailMapper.selectList(dq);
		if (divRows.isEmpty()) {
			return true;
		}
		List<Long> orderIds = divRows.stream()
				.map(ChinaumspayDivisionDetail::getOrderId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		if (orderIds.isEmpty()) {
			return true;
		}
		var rw = new LambdaUpdateWrapper<OrdersRelChinaumspayDivision>();
		rw.in(OrdersRelChinaumspayDivision::getOrderId, orderIds);
		rw.set(OrdersRelChinaumspayDivision::getStatus, OrderDivisionRelStatus.READY);
		rw.set(OrdersRelChinaumspayDivision::getUpdateTime, now);
		ordersRelChinaumspayDivisionMapper.update(null, rw);
		return true;
	}

	private static int parseFee(String s) {
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String joinRelative(String dir, String file) {
		String a = dir == null ? "" : dir.replace("\\", "/");
		String f = file == null ? "" : file.replace("\\", "/");
		if (a.isEmpty()) {
			return f.startsWith("/") ? f.substring(1) : f;
		}
		if (a.endsWith("/")) {
			return a + f;
		}
		return a + "/" + f;
	}
}
