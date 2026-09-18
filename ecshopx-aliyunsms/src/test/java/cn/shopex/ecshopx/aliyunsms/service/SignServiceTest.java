package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsSignResult;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySmsSignJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignServiceTest {

	@Mock
	private SignMapper signMapper;

	@Mock
	private AliyunsmsGetSmsSignClient aliyunsmsGetSmsSignClient;

	@Mock
	private AliyunsmsQuerySmsSignJobDispatchPublisher querySmsSignJobDispatchPublisher;

	@InjectMocks
	private SignService signService;

	@Test
	@DisplayName("schedule: 无审核中，早退出")
	void schedule_empty_pending_returns_zero() {
		when(signMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
		assertThat(signService.scheduleQueryAuditStatus()).isZero();
		verify(querySmsSignJobDispatchPublisher, never()).publish(anyLong(), anyString());
		verify(aliyunsmsGetSmsSignClient, never()).getSmsSign(anyLong(), anyString());
		verify(signMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("schedule: sign_name 为空则跳过且不发布")
	void schedule_skips_null_sign_name() {
		Sign row = new Sign();
		row.setCompanyId(10L);
		row.setSignName(null);
		when(signMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(row));
		assertThat(signService.scheduleQueryAuditStatus()).isZero();
		verify(querySmsSignJobDispatchPublisher, never()).publish(anyLong(), anyString());
	}

	@Test
	@DisplayName("schedule: 单条待审入队一次")
	void schedule_one_row_dispatches_once() {
		Sign row = new Sign();
		row.setCompanyId(10L);
		row.setSignName("N1");
		when(signMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(row));
		assertThat(signService.scheduleQueryAuditStatus()).isEqualTo(1);
		verify(querySmsSignJobDispatchPublisher, times(1)).publish(10L, "N1");
		verify(aliyunsmsGetSmsSignClient, never()).getSmsSign(anyLong(), anyString());
		verify(signMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("apply: 云无 SignStatus，不写库")
	void apply_no_cloud_status_returns_without_update() {
		when(aliyunsmsGetSmsSignClient.getSmsSign(10L, "N1")).thenReturn(GetSmsSignResult.noSignStatus());
		signService.applyPendingSignAuditFromCloud(10L, "N1");
		verify(aliyunsmsGetSmsSignClient, times(1)).getSmsSign(10L, "N1");
		verify(signMapper, never()).selectOne(any(QueryWrapper.class));
		verify(signMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("apply: 单条写库成功")
	void apply_one_row_success() {
		when(aliyunsmsGetSmsSignClient.getSmsSign(1L, "S"))
				.thenReturn(new GetSmsSignResult("1", "reject-hint"));
		Sign current = new Sign();
		current.setId(5L);
		current.setCompanyId(1L);
		current.setSignName("S");
		current.setStatus("0");
		when(signMapper.selectOne(any(QueryWrapper.class))).thenReturn(current);
		when(signMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		signService.applyPendingSignAuditFromCloud(1L, "S");
		verify(signMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("apply: selectOne 无行抛 ResourceException")
	void apply_select_one_missing_throws() {
		when(aliyunsmsGetSmsSignClient.getSmsSign(2L, "X")).thenReturn(new GetSmsSignResult("1", ""));
		when(signMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		assertThatThrownBy(() -> signService.applyPendingSignAuditFromCloud(2L, "X"))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("未查询到更新数据");
	}

	@Nested
	@DisplayName("多行路径")
	class MultiRowBranch {

		@Test
		@DisplayName("schedule: 两条不同公司与签名各发布一次")
		void schedule_two_rows_two_publish() {
			Sign a = new Sign();
			a.setCompanyId(11L);
			a.setSignName("A1");
			Sign b = new Sign();
			b.setCompanyId(22L);
			b.setSignName("B1");
			when(signMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(a, b));
			assertThat(signService.scheduleQueryAuditStatus()).isEqualTo(2);
			verify(querySmsSignJobDispatchPublisher, times(1)).publish(11L, "A1");
			verify(querySmsSignJobDispatchPublisher, times(1)).publish(22L, "B1");
		}

		@Test
		@DisplayName("apply: 两条云状态与写库")
		void apply_two_companies() {
			when(aliyunsmsGetSmsSignClient.getSmsSign(eq(11L), eq("A1")))
					.thenReturn(new GetSmsSignResult("1", "r1"));
			when(aliyunsmsGetSmsSignClient.getSmsSign(eq(22L), eq("B1")))
					.thenReturn(new GetSmsSignResult("1", "r2"));
			Sign rowA = new Sign();
			rowA.setId(1L);
			rowA.setCompanyId(11L);
			rowA.setSignName("A1");
			rowA.setStatus("0");
			Sign rowB = new Sign();
			rowB.setId(2L);
			rowB.setCompanyId(22L);
			rowB.setSignName("B1");
			rowB.setStatus("0");
			when(signMapper.selectOne(any(QueryWrapper.class)))
					.thenReturn(rowA, rowB);
			when(signMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
			signService.applyPendingSignAuditFromCloud(11L, "A1");
			signService.applyPendingSignAuditFromCloud(22L, "B1");
			verify(aliyunsmsGetSmsSignClient, times(1)).getSmsSign(11L, "A1");
			verify(aliyunsmsGetSmsSignClient, times(1)).getSmsSign(22L, "B1");
			verify(signMapper, times(2)).update(isNull(), any(UpdateWrapper.class));
		}
	}
}
