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

import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsTemplateClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsTemplateResult;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsQuerySmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

	@Mock
	private TemplateMapper templateMapper;

	@Mock
	private AliyunsmsGetSmsTemplateClient aliyunsmsGetSmsTemplateClient;

	@Mock
	private AliyunsmsQuerySmsTemplateJobDispatchPublisher querySmsTemplateJobDispatchPublisher;

	@InjectMocks
	private TemplateService templateService;

	@Test
	@DisplayName("schedule: 无审核中，早退出")
	void schedule_empty_pending_returns_zero() {
		when(templateMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
		assertThat(templateService.scheduleQueryTemplateAuditStatus()).isZero();
		verify(querySmsTemplateJobDispatchPublisher, never()).publish(anyLong(), anyString());
		verify(aliyunsmsGetSmsTemplateClient, never()).getSmsTemplate(anyLong(), anyString());
		verify(templateMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("schedule: template_code 为空则跳过且不发布")
	void schedule_skips_blank_template_code() {
		Template row = new Template();
		row.setCompanyId(10L);
		row.setTemplateCode("");
		when(templateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(row));
		assertThat(templateService.scheduleQueryTemplateAuditStatus()).isZero();
		verify(querySmsTemplateJobDispatchPublisher, never()).publish(anyLong(), anyString());
	}

	@Test
	@DisplayName("schedule: 单条待审入队一次")
	void schedule_one_row_dispatches_once() {
		Template row = new Template();
		row.setCompanyId(10L);
		row.setTemplateCode("T1");
		when(templateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(row));
		assertThat(templateService.scheduleQueryTemplateAuditStatus()).isEqualTo(1);
		verify(querySmsTemplateJobDispatchPublisher, times(1)).publish(10L, "T1");
		verify(aliyunsmsGetSmsTemplateClient, never()).getSmsTemplate(anyLong(), anyString());
		verify(templateMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("apply: 云无 TemplateStatus，不写库")
	void apply_no_cloud_status_returns_without_update() {
		when(aliyunsmsGetSmsTemplateClient.getSmsTemplate(10L, "TC1")).thenReturn(GetSmsTemplateResult.noTemplateStatus());
		templateService.applyPendingTemplateAuditFromCloud(10L, "TC1");
		verify(aliyunsmsGetSmsTemplateClient, times(1)).getSmsTemplate(10L, "TC1");
		verify(templateMapper, never()).selectOne(any(QueryWrapper.class));
		verify(templateMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("apply: 单条写库成功")
	void apply_one_row_success() {
		when(aliyunsmsGetSmsTemplateClient.getSmsTemplate(1L, "T"))
				.thenReturn(new GetSmsTemplateResult("1", "reject-hint"));
		Template current = new Template();
		current.setId(5L);
		current.setCompanyId(1L);
		current.setTemplateCode("T");
		current.setStatus("0");
		when(templateMapper.selectOne(any(QueryWrapper.class))).thenReturn(current);
		when(templateMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		templateService.applyPendingTemplateAuditFromCloud(1L, "T");
		verify(templateMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("apply: selectOne 无行抛 ResourceException")
	void apply_select_one_missing_throws() {
		when(aliyunsmsGetSmsTemplateClient.getSmsTemplate(2L, "TX")).thenReturn(new GetSmsTemplateResult("1", ""));
		when(templateMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
		assertThatThrownBy(() -> templateService.applyPendingTemplateAuditFromCloud(2L, "TX"))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("未查询到更新数据");
		verify(templateMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("schedule: 列表查询条件含 status 与 template_code 非空")
	void schedule_filter_includes_status_and_template_code_ne() {
		ArgumentCaptor<QueryWrapper<Template>> listCap = ArgumentCaptor.forClass(QueryWrapper.class);
		when(templateMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
		templateService.scheduleQueryTemplateAuditStatus();
		verify(templateMapper).selectList(listCap.capture());
		QueryWrapper<Template> captured = listCap.getValue();
		String sqlSegment = captured.getCustomSqlSegment();
		assertThat(sqlSegment).contains("status").contains("template_code");
		assertThat(captured.getParamNameValuePairs().values()).contains("0", "");
	}

	@Nested
	@DisplayName("多行路径")
	class MultiRowBranch {

		@Test
		@DisplayName("schedule: 两条不同公司与模板各发布一次")
		void schedule_two_rows_two_publish() {
			Template a = new Template();
			a.setCompanyId(11L);
			a.setTemplateCode("A1");
			Template b = new Template();
			b.setCompanyId(22L);
			b.setTemplateCode("B1");
			when(templateMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(a, b));
			assertThat(templateService.scheduleQueryTemplateAuditStatus()).isEqualTo(2);
			verify(querySmsTemplateJobDispatchPublisher, times(1)).publish(11L, "A1");
			verify(querySmsTemplateJobDispatchPublisher, times(1)).publish(22L, "B1");
		}

		@Test
		@DisplayName("apply: 两条云状态与写库")
		void apply_two_companies() {
			when(aliyunsmsGetSmsTemplateClient.getSmsTemplate(eq(11L), eq("A1")))
					.thenReturn(new GetSmsTemplateResult("1", "r1"));
			when(aliyunsmsGetSmsTemplateClient.getSmsTemplate(eq(22L), eq("B1")))
					.thenReturn(new GetSmsTemplateResult("1", "r2"));
			Template rowA = new Template();
			rowA.setId(1L);
			rowA.setCompanyId(11L);
			rowA.setTemplateCode("A1");
			rowA.setStatus("0");
			Template rowB = new Template();
			rowB.setId(2L);
			rowB.setCompanyId(22L);
			rowB.setTemplateCode("B1");
			rowB.setStatus("0");
			when(templateMapper.selectOne(any(QueryWrapper.class)))
					.thenReturn(rowA, rowB);
			when(templateMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
			templateService.applyPendingTemplateAuditFromCloud(11L, "A1");
			templateService.applyPendingTemplateAuditFromCloud(22L, "B1");
			verify(aliyunsmsGetSmsTemplateClient, times(1)).getSmsTemplate(11L, "A1");
			verify(aliyunsmsGetSmsTemplateClient, times(1)).getSmsTemplate(22L, "B1");
			verify(templateMapper, times(2)).update(isNull(), any(UpdateWrapper.class));
		}
	}
}
