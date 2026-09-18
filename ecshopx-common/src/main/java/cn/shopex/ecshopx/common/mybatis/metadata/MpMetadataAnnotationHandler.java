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

package cn.shopex.ecshopx.common.mybatis.metadata;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.handlers.AnnotationHandler;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Makes {@link MpTable}, {@link MpField}, and {@link MpId} visible to MyBatis-Plus as native annotations.
 */
public class MpMetadataAnnotationHandler implements AnnotationHandler {

    @Override
    public <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationClass) {
        if (annotationClass == TableName.class) {
            MpTable mpTable = clazz.getAnnotation(MpTable.class);
            if (mpTable != null) {
                return annotationClass.cast(tableNameProxy(mpTable));
            }
        }
        return AnnotationHandler.super.getAnnotation(clazz, annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Field field, Class<T> annotationClass) {
        if (annotationClass == TableField.class) {
            MpField mpField = field.getAnnotation(MpField.class);
            if (mpField != null) {
                return annotationClass.cast(tableFieldProxy(mpField));
            }
        }
        if (annotationClass == TableId.class) {
            MpId mpId = field.getAnnotation(MpId.class);
            if (mpId != null) {
                return annotationClass.cast(tableIdProxy(mpId));
            }
        }
        return AnnotationHandler.super.getAnnotation(field, annotationClass);
    }

    @Override
    public <T extends Annotation> boolean isAnnotationPresent(Class<?> clazz, Class<T> annotationClass) {
        if (annotationClass == TableName.class && clazz.isAnnotationPresent(MpTable.class)) {
            return true;
        }
        return AnnotationHandler.super.isAnnotationPresent(clazz, annotationClass);
    }

    @Override
    public <T extends Annotation> boolean isAnnotationPresent(Field field, Class<T> annotationClass) {
        if (annotationClass == TableField.class && field.isAnnotationPresent(MpField.class)) {
            return true;
        }
        if (annotationClass == TableId.class && field.isAnnotationPresent(MpId.class)) {
            return true;
        }
        return AnnotationHandler.super.isAnnotationPresent(field, annotationClass);
    }

    private static TableName tableNameProxy(MpTable source) {
        Map<String, Object> values = new HashMap<>();
        values.put("value", source.value());
        values.put("schema", source.schema());
        values.put("keepGlobalPrefix", source.keepGlobalPrefix());
        values.put("resultMap", source.resultMap());
        values.put("autoResultMap", source.autoResultMap());
        values.put("properties", source.properties());
        values.put("excludeProperty", source.excludeProperty());
        return proxy(TableName.class, values);
    }

    private static TableField tableFieldProxy(MpField source) {
        Map<String, Object> values = new HashMap<>();
        values.put("value", source.value());
        values.put("exist", source.exist());
        values.put("condition", source.condition());
        values.put("update", source.update());
        values.put("insertStrategy", source.insertStrategy());
        values.put("updateStrategy", source.updateStrategy());
        values.put("whereStrategy", source.whereStrategy());
        values.put("fill", source.fill());
        values.put("select", source.select());
        values.put("keepGlobalFormat", source.keepGlobalFormat());
        values.put("property", source.property());
        values.put("jdbcType", source.jdbcType());
        values.put("typeHandler", source.typeHandler());
        values.put("javaType", source.javaType());
        values.put("numericScale", source.numericScale());
        return proxy(TableField.class, values);
    }

    private static TableId tableIdProxy(MpId source) {
        Map<String, Object> values = new HashMap<>();
        values.put("value", source.value());
        values.put("type", source.type());
        return proxy(TableId.class, values);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T proxy(Class<T> annotationClass, Map<String, Object> values) {
        InvocationHandler handler = new AnnotationProxyHandler(annotationClass, values);
        return (T) Proxy.newProxyInstance(annotationClass.getClassLoader(), new Class<?>[]{annotationClass}, handler);
    }

    private static class AnnotationProxyHandler implements InvocationHandler {
        private final Class<? extends Annotation> annotationClass;
        private final Map<String, Object> values;

        private AnnotationProxyHandler(Class<? extends Annotation> annotationClass, Map<String, Object> values) {
            this.annotationClass = annotationClass;
            this.values = values;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.equals("annotationType") && method.getParameterCount() == 0) {
                return annotationClass;
            }
            if (name.equals("toString") && method.getParameterCount() == 0) {
                return annotationClass.getName() + values;
            }
            if (name.equals("hashCode") && method.getParameterCount() == 0) {
                return values.hashCode();
            }
            if (name.equals("equals") && method.getParameterCount() == 1) {
                return proxy == args[0];
            }
            if (values.containsKey(name)) {
                return values.get(name);
            }
            Object defaultValue = method.getDefaultValue();
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new IllegalStateException("No value for " + annotationClass.getName() + "." + name);
        }
    }
}
