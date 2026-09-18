package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.domain.SceneItem;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneItemMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsTemplateClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySmsTemplateListClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsTemplateResult;
import cn.shopex.ecshopx.common.aliyunsms.QuerySmsTemplateListItem;
import cn.shopex.ecshopx.common.aliyunsms.SyncSmsTemplateResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AliyunsmsTemplateSyncServiceTest {

	@Mock
	private TemplateMapper templateMapper;

	@Mock
	private SceneItemMapper sceneItemMapper;

	@Mock
	private TaskMapper taskMapper;

	@Mock
	private AliyunsmsQuerySmsTemplateListClient querySmsTemplateListClient;

	@Mock
	private AliyunsmsGetSmsTemplateClient getSmsTemplateClient;

	@InjectMocks
	private AliyunsmsTemplateSyncService service;

	@Test
	void creates_when_cloud_template_missing_locally() {
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50))
				.thenReturn(List.of(new QuerySmsTemplateListItem("TCODE_A")));
		when(getSmsTemplateClient.getSmsTemplate(1L, "TCODE_A"))
				.thenReturn(new GetSmsTemplateResult("1", "ok"));
		when(templateMapper.selectList(any())).thenReturn(List.of());

		SyncSmsTemplateResult result = service.sync(1L);

		assertThat(result.created()).isEqualTo(1);
		assertThat(result.updated()).isZero();
		verify(templateMapper, times(1)).insert(any(Template.class));
	}

	@Test
	void updates_when_cloud_template_exists_locally() {
		Template existing = new Template();
		existing.setId(10L);
		existing.setCompanyId(1L);
		existing.setTemplateCode("TCODE_A");
		existing.setStatus("0");
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50))
				.thenReturn(List.of(new QuerySmsTemplateListItem("TCODE_A")));
		when(getSmsTemplateClient.getSmsTemplate(1L, "TCODE_A"))
				.thenReturn(new GetSmsTemplateResult("10", "bad"));
		when(templateMapper.selectList(any())).thenReturn(List.of(existing));

		SyncSmsTemplateResult result = service.sync(1L);

		assertThat(result.updated()).isEqualTo(1);
		verify(templateMapper, times(1)).updateById(any(Template.class));
	}

	@Test
	void deletes_orphan_without_reference() {
		Template orphan = new Template();
		orphan.setId(99L);
		orphan.setCompanyId(1L);
		orphan.setTemplateCode("ORPHAN");
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50)).thenReturn(List.of());
		when(templateMapper.selectList(any())).thenReturn(List.of(orphan));
		when(sceneItemMapper.selectCount(any())).thenReturn(0L);
		when(taskMapper.selectCount(any())).thenReturn(0L);

		SyncSmsTemplateResult result = service.sync(1L);

		assertThat(result.deleted()).isEqualTo(1);
		verify(templateMapper, times(1)).deleteById(99L);
	}

	@Test
	void skips_orphan_with_scene_reference() {
		Template orphan = new Template();
		orphan.setId(88L);
		orphan.setCompanyId(1L);
		orphan.setTemplateCode("SCENE_ORPHAN");
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50)).thenReturn(List.of());
		when(templateMapper.selectList(any())).thenReturn(List.of(orphan));
		when(sceneItemMapper.selectCount(any())).thenReturn(1L);

		SyncSmsTemplateResult result = service.sync(1L);

		assertThat(result.skipped()).isEqualTo(1);
		verify(templateMapper, never()).deleteById(88L);
	}

	@Test
	void creates_scene_id_zero_by_default_for_new_rows() {
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50))
				.thenReturn(List.of(new QuerySmsTemplateListItem("TCODE_Z")));
		when(getSmsTemplateClient.getSmsTemplate(1L, "TCODE_Z"))
				.thenReturn(GetSmsTemplateResult.noTemplateStatus());
		when(templateMapper.selectList(any())).thenReturn(List.of());

		service.sync(1L);

		verify(templateMapper).insert(
				org.mockito.ArgumentMatchers.argThat((Template t) -> t != null && Integer.valueOf(0).equals(t.getSceneId())));
	}

	@Test
	void follows_next_page_when_first_page_is_full() {
		List<QuerySmsTemplateListItem> firstPage =
				java.util.stream.IntStream.range(0, 50)
						.mapToObj(i -> new QuerySmsTemplateListItem("PAGE_" + i))
						.toList();
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50)).thenReturn(firstPage);
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 2, 50))
				.thenReturn(List.of(new QuerySmsTemplateListItem("PAGE_LAST")));
		when(templateMapper.selectList(any())).thenReturn(List.of());
		when(getSmsTemplateClient.getSmsTemplate(any(Long.class), any(String.class)))
				.thenReturn(GetSmsTemplateResult.noTemplateStatus());

		SyncSmsTemplateResult result = service.sync(1L);

		assertThat(result.created()).isEqualTo(51);
		verify(querySmsTemplateListClient).querySmsTemplateList(1L, 2, 50);
		verify(getSmsTemplateClient, times(51)).getSmsTemplate(any(Long.class), any(String.class));
	}

	@Test
	void counts_detail_failure_and_continues_other_templates() {
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50))
				.thenReturn(List.of(new QuerySmsTemplateListItem("BAD"), new QuerySmsTemplateListItem("GOOD")));
		when(templateMapper.selectList(any())).thenReturn(List.of());
		when(getSmsTemplateClient.getSmsTemplate(1L, "BAD"))
				.thenThrow(new RuntimeException("aliyun unavailable"));
		when(getSmsTemplateClient.getSmsTemplate(1L, "GOOD"))
				.thenReturn(GetSmsTemplateResult.noTemplateStatus());

		SyncSmsTemplateResult result = service.sync(1L);

		assertThat(result.failed()).isEqualTo(1);
		assertThat(result.created()).isEqualTo(1);
		assertThat(result.errors()).extracting(SyncSmsTemplateResult.SyncSmsTemplateError::templateCode)
				.containsExactly("BAD");
	}

	@Test
	void skips_orphan_with_active_task_reference() {
		Template orphan = new Template();
		orphan.setId(77L);
		orphan.setCompanyId(1L);
		orphan.setTemplateCode("TASK_ORPHAN");
		when(querySmsTemplateListClient.querySmsTemplateList(1L, 1, 50)).thenReturn(List.of());
		when(templateMapper.selectList(any())).thenReturn(List.of(orphan));
		when(sceneItemMapper.selectCount(any())).thenReturn(0L);
		when(taskMapper.selectCount(any())).thenReturn(1L);

		SyncSmsTemplateResult result = service.sync(1L);

		assertThat(result.skipped()).isEqualTo(1);
		verify(templateMapper, never()).deleteById(77L);
	}
}
