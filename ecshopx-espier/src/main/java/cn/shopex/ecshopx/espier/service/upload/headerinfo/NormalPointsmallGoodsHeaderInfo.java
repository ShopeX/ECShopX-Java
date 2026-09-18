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

package cn.shopex.ecshopx.espier.service.upload.headerinfo;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderColumnInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NormalPointsmallGoodsHeaderInfo {
	private NormalPointsmallGoodsHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("管理分类", new UploadHeaderColumnInfo(255, "类目名称，一级类目->二级类目->三级类目", true));
		map.put("商品名称", new UploadHeaderColumnInfo(255, "", true));
		map.put("商品编码", new UploadHeaderColumnInfo(32, "", false));
		map.put("简介", new UploadHeaderColumnInfo(20, "", false));
		map.put("商品价格", new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", true));
		map.put("市场价", new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", false));
		map.put("成本价", new UploadHeaderColumnInfo(255, "单位为(元)，最多两位小数", false));
		map.put("积分价格", new UploadHeaderColumnInfo(255, "", true));
		map.put("图片", new UploadHeaderColumnInfo(255, "多个图片使用英文逗号隔开，最多上传9个", false));
		map.put("视频", new UploadHeaderColumnInfo(255, "在视频素材复制对应的ID", false));
		map.put("库存", new UploadHeaderColumnInfo(255, "库存为0-999999999的整数", true));
		map.put("品牌", new UploadHeaderColumnInfo(255, "已有的品牌名称", false));
		map.put("运费模板", new UploadHeaderColumnInfo(255, "运费模板名称", true));
		map.put("分类", new UploadHeaderColumnInfo(255, "分类名称，一级分类->二级分类|一级分类->二级分类->三级分类 多个二级三级分类使用|隔开", true));
		map.put("重量", new UploadHeaderColumnInfo(255, "商品重量，单位KG", false));
		map.put("条形码", new UploadHeaderColumnInfo(255, "条形码", false));
		map.put("单位", new UploadHeaderColumnInfo(255, "单位", false));
		map.put("规格值", new UploadHeaderColumnInfo(255, "例如：颜色:红色|尺码:20cm", false));
		map.put("参数值", new UploadHeaderColumnInfo(255, "例如：系列:生机展颜|功效:美白提亮", false));
		return Collections.unmodifiableMap(map);
	}
}
