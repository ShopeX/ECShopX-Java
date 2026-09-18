package cn.shopex.ecshopx.aliyunsms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneItemMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsSignClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySmsSignListClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsSignResult;
import cn.shopex.ecshopx.common.aliyunsms.QuerySmsSignListItem;
import cn.shopex.ecshopx.common.aliyunsms.SyncSmsSignResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AliyunsmsSignSyncServiceTest {

	@Mock
	private SignMapper signMapper;

	@Mock
	private SceneItemMapper sceneItemMapper;

	@Mock
	private TaskMapper taskMapper;

	@Mock
	private AliyunsmsQuerySmsSignListClient querySmsSignListClient;

	@Mock
	private AliyunsmsGetSmsSignClient getSmsSignClient;

	@InjectMocks
	private AliyunsmsSignSyncService service;

	@Test
	void creates_when_cloud_sign_missing_locally() {
		when(querySmsSignListClient.querySmsSignList(1L, 1, 50)).thenReturn(List.of(new QuerySmsSignListItem("A")));
		when(getSmsSignClient.getSmsSign(1L, "A")).thenReturn(new GetSmsSignResult("1", "ok"));
		when(signMapper.selectList(any())).thenReturn(List.of());

		SyncSmsSignResult result = service.sync(1L);

		assertThat(result.created()).isEqualTo(1);
		assertThat(result.updated()).isZero();
		verify(signMapper, times(1)).insert(any(Sign.class));
	}

	@Test
	void updates_when_cloud_sign_exists_locally() {
		Sign existing = new Sign();
		existing.setId(10L);
		existing.setCompanyId(1L);
		existing.setSignName("A");
		existing.setStatus("0");
		when(querySmsSignListClient.querySmsSignList(1L, 1, 50)).thenReturn(List.of(new QuerySmsSignListItem("A")));
		when(getSmsSignClient.getSmsSign(1L, "A")).thenReturn(new GetSmsSignResult("10", "bad"));
		when(signMapper.selectList(any())).thenReturn(List.of(existing));

		SyncSmsSignResult result = service.sync(1L);

		assertThat(result.updated()).isEqualTo(1);
		verify(signMapper, times(1)).updateById(any(Sign.class));
	}

	@Test
	void deletes_orphan_without_cloud_delete() {
		Sign orphan = new Sign();
		orphan.setId(99L);
		orphan.setCompanyId(1L);
		orphan.setSignName("Orphan");
		when(querySmsSignListClient.querySmsSignList(1L, 1, 50)).thenReturn(List.of());
		when(signMapper.selectList(any())).thenReturn(List.of(orphan));
		when(sceneItemMapper.selectCount(any())).thenReturn(0L);
		when(taskMapper.selectCount(any())).thenReturn(0L);

		SyncSmsSignResult result = service.sync(1L);

		assertThat(result.deleted()).isEqualTo(1);
		verify(signMapper, times(1)).deleteById(99L);
	}

	@Test
	void skips_orphan_with_scene_reference() {
		Sign orphan = new Sign();
		orphan.setId(88L);
		orphan.setCompanyId(1L);
		orphan.setSignName("SceneOrphan");
		when(querySmsSignListClient.querySmsSignList(1L, 1, 50)).thenReturn(List.of());
		when(signMapper.selectList(any())).thenReturn(List.of(orphan));
		when(sceneItemMapper.selectCount(any())).thenReturn(1L);

		SyncSmsSignResult result = service.sync(1L);

		assertThat(result.skipped()).isEqualTo(1);
		verify(signMapper, never()).deleteById(eq(88L));
	}
}
