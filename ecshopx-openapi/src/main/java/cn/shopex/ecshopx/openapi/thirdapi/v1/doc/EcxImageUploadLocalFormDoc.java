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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；multipart 表单字段部分（文件本体 images 为独立 multipart part，不在本 JSON schema 中）。
 * 字段来源 {@code ImageController#uploadeImage} / {@code resolveFileTypeForUploade}。
 */
@Schema(
		name = "EcxImageUploadLocalForm",
		description =
				"multipart/form-data 表单字段（与文件 part images 一起提交；同名字段也可经 query 或 JSON body 提供，"
						+ "filetype 走 originalString（body 含键优先），group 走 mergeString）")
public class EcxImageUploadLocalFormDoc {

	@Schema(
			description =
					"文件类型：image 图片（jpeg/jpg/png/gif）；videos 视频（mp4）；空/\"0\"/缺省按 image；"
							+ "file 类型被拒绝（不支持的文件存储类型file）",
			defaultValue = "image",
			allowableValues = {"image", "videos"},
			example = "image")
	public String filetype;

	@Schema(description = "存储分组（对象键中的可选路径段）；可选")
	public String group;
}