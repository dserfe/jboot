/**
 * Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jboot.web.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.ActionException;
import com.jfinal.core.Controller;
import com.jfinal.kit.LogKit;
import com.jfinal.render.RenderManager;
import io.jboot.aop.InterceptorBuilder;
import io.jboot.aop.Interceptors;
import io.jboot.aop.annotation.AutoLoad;
import io.jboot.utils.ClassUtil;
import io.jboot.utils.DateUtil;
import io.jboot.utils.StrUtil;
import io.jboot.web.controller.JbootController;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

@AutoLoad
public class JsonBodyParseInterceptor implements Interceptor, InterceptorBuilder {

    @Override
    public void intercept(Invocation inv) {
        String rawData = inv.getController().getRawData();
        if (StrUtil.isBlank(rawData)) {
            inv.invoke();
            return;
        }

        Parameter[] parameters = inv.getMethod().getParameters();
        Type[] paraTypes = inv.getMethod().getGenericParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            JsonBody jsonBody = parameters[index].getAnnotation(JsonBody.class);
            if (jsonBody != null) {
                Class typeClass = parameters[index].getType();
                Object result = null;
                try {
                    if (Collection.class.isAssignableFrom(typeClass) || typeClass.isArray()) {
                        result = parseArray(rawData, typeClass, paraTypes[index], jsonBody);
                    } else {
                        result = parseObject(rawData, typeClass, paraTypes[index], jsonBody);
                    }
                } catch (Exception e) {
                    String message = "Can not parse json to type: " + parameters[index].getType()
                            + " in method " + ClassUtil.buildMethodString(inv.getMethod()) + ", cause: " + e.getMessage();
                    if (jsonBody.skipConvertError()) {
                        LogKit.error(message);
                    } else {
                        throw new ActionException(400, RenderManager.me().getRenderFactory().getErrorRender(400), message);
                    }
                }
                inv.setArg(index, result);
            }
        }

        inv.invoke();
    }


    private Object parseObject(String rawData, Class typeClass, Type type, JsonBody jsonBody) throws IllegalAccessException, InstantiationException {
        JSONObject rawObject = JSON.parseObject(rawData);
        Object parseResult = null;
        if (StrUtil.isNotBlank(jsonBody.value())) {
            String[] values = jsonBody.value().split("\\.");
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                if (StrUtil.isNotBlank(value)) {
                    if (i == values.length - 1) {
                        parseResult = rawObject.get(value);
                    } else {
                        rawObject = rawObject.getJSONObject(value);
                        if (rawObject == null || rawObject.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        if (parseResult == null) {
            return null;
        }

        if (parseResult instanceof JSONObject) {
            rawObject = (JSONObject) parseResult;
        } else {
            return convert(parseResult, typeClass);
        }

        if (rawObject.isEmpty()) {
            return null;
        }

        //非泛型 的 map
        if ((typeClass == Map.class || typeClass == JSONObject.class) && typeClass == type) {
            return rawObject;
        }

        //非泛型 的 map
        if (Map.class.isAssignableFrom(typeClass) && typeClass == type && canNewInstance(typeClass)) {
            Map map = (Map) typeClass.newInstance();
            for (String key : rawObject.keySet()) {
                map.put(key, rawObject.get(key));
            }
            return map;
        }

        return rawObject.toJavaObject(type);
    }


    private static Object convert(Object value, Class targetClass) {

        if (value.getClass().isAssignableFrom(targetClass)) {
            return value;
        }

        if (targetClass == Integer.class || targetClass == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } else if (targetClass == Long.class || targetClass == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } else if (targetClass == Double.class || targetClass == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } else if (targetClass == Float.class || targetClass == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        } else if (targetClass == Boolean.class || targetClass == boolean.class) {
            String v = value.toString().toLowerCase();
            if ("1".equals(v) || "true".equals(v)) {
                return Boolean.TRUE;
            } else if ("0".equals(v) || "false".equals(v)) {
                return Boolean.FALSE;
            } else {
                throw new RuntimeException("Can not parse to boolean type of value: " + value);
            }
        } else if (targetClass == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(value.toString());
        } else if (targetClass == java.math.BigInteger.class) {
            return new java.math.BigInteger(value.toString());
        } else if (targetClass == byte[].class) {
            return value.toString().getBytes();
        } else if (targetClass == Date.class) {
            return DateUtil.parseDate(value.toString());
        }

        throw new RuntimeException(targetClass.getName() + " can not be parsed in json!");
    }


    private Object parseArray(String rawData, Class typeClass, Type type, JsonBody jsonBody) {
        JSONArray jsonArray = null;
        if (StrUtil.isBlank(jsonBody.value())) {
            jsonArray = JSON.parseArray(rawData);
        } else {
            JSONObject jsonObject = JSON.parseObject(rawData);
            String[] values = jsonBody.value().split("\\.");
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                if (StrUtil.isNotBlank(value)) {
                    if (i == values.length - 1) {
                        jsonArray = jsonObject.getJSONArray(value);
                    } else {
                        jsonObject = jsonObject.getJSONObject(value);
                        if (jsonObject == null || jsonObject.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        if (jsonArray == null || jsonArray.isEmpty()) {
            return null;
        }

        //非泛型 set
        if ((typeClass == Set.class || typeClass == HashSet.class) && typeClass == type) {
            return new HashSet<>(jsonArray);
        }

        return jsonArray.toJavaObject(type);
    }


    private boolean canNewInstance(Class clazz) {
        int modifiers = clazz.getModifiers();
        return !Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers);
    }


    @Override
    public void build(Class<?> serviceClass, Method method, Interceptors interceptors) {
        if (Controller.class.isAssignableFrom(serviceClass)) {
            Parameter[] parameters = method.getParameters();
            if (parameters != null && parameters.length > 0) {
                for (Parameter p : parameters) {
                    if (p.getAnnotation(JsonBody.class) != null) {
                        Class typeClass = p.getType();
                        if ((Map.class.isAssignableFrom(typeClass) || Collection.class.isAssignableFrom(typeClass) || typeClass.isArray())
                                && !JbootController.class.isAssignableFrom(serviceClass)) {
                            throw new IllegalArgumentException("Can not use @JsonBody for Map/List(Collection)/Array type if your controller not extends JbootController, method: " + ClassUtil.buildMethodString(method));
                        }

                        interceptors.add(this);
                        return;
                    }
                }
            }
        }
    }
}