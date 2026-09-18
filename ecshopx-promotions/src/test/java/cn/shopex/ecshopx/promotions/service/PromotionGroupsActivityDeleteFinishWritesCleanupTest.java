package cn.shopex.ecshopx.promotions.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Locale;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

class PromotionGroupsActivityDeleteFinishWritesCleanupTest {

	private static final long COMPANY_ID = 1L;
	private static final long ACT_ID = 42L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PromotionGroupsActivity.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PromotionGroupsTeam.class);
	}

	@Test
	void delete_applyDeleteWrites_invokesRelAndRedisCleanup() {
		MessageSource messageSource = mock(MessageSource.class);
		PromotionGroupsActivityMapper activityMapper = mock(PromotionGroupsActivityMapper.class);
		PromotionsItemsTagMapper tagMapper = mock(PromotionsItemsTagMapper.class);
		PromotionGroupsRelGoodsWriteService relWriteService = mock(PromotionGroupsRelGoodsWriteService.class);

		PromotionGroupsActivity entity = new PromotionGroupsActivity();
		entity.setGroupsActivityId(ACT_ID);
		entity.setCompanyId(COMPANY_ID);
		when(activityMapper.selectOne(any())).thenReturn(entity);
		when(activityMapper.updateById(any(PromotionGroupsActivity.class))).thenReturn(1);

		PromotionGroupsActivityDeleteWritesService service =
				new PromotionGroupsActivityDeleteWritesService(
						messageSource, activityMapper, tagMapper, relWriteService);

		service.applyDeleteWrites(COMPANY_ID, ACT_ID, Locale.CHINA);

		verify(relWriteService).cleanupRelRowsAndRedisKeys(COMPANY_ID, ACT_ID);
	}

	@Test
	void finish_applyFinishWrites_invokesRelAndRedisCleanup() {
		MessageSource messageSource = mock(MessageSource.class);
		PromotionGroupsActivityMapper activityMapper = mock(PromotionGroupsActivityMapper.class);
		PromotionsItemsTagMapper tagMapper = mock(PromotionsItemsTagMapper.class);
		PromotionGroupsTeamMapper teamMapper = mock(PromotionGroupsTeamMapper.class);
		PromotionGroupsActivityAdminRowAssembler assembler = mock(PromotionGroupsActivityAdminRowAssembler.class);
		PromotionGroupsRelGoodsWriteService relWriteService = mock(PromotionGroupsRelGoodsWriteService.class);

		PromotionGroupsActivity entity = new PromotionGroupsActivity();
		entity.setGroupsActivityId(ACT_ID);
		entity.setCompanyId(COMPANY_ID);
		when(activityMapper.selectOne(any())).thenReturn(entity);
		when(activityMapper.updateById(any(PromotionGroupsActivity.class))).thenReturn(1);
		when(assembler.toRow(eq(entity), any(Integer.class))).thenReturn(Map.of("status", "finished"));

		PromotionGroupsActivityFinishWritesService service =
				new PromotionGroupsActivityFinishWritesService(
						messageSource, activityMapper, tagMapper, teamMapper, assembler, relWriteService);

		service.applyFinishWrites(COMPANY_ID, ACT_ID, Locale.CHINA);

		verify(relWriteService).cleanupRelRowsAndRedisKeys(COMPANY_ID, ACT_ID);
	}
}
