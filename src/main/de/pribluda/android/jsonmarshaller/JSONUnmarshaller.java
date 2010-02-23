/*
 * Copyright (c) 2010. Konstantin Pribluda (konstantin.pribluda@gmail.com)
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.pribluda.android.jsonmarshaller;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;

/**
 * performs unmarshalling of JSON data creating objects
 */
public class JSONUnmarshaller {
    private static final String SETTER_PREFIX = "set";


    static final HashMap<Class, Class> primitves = new HashMap<Class, Class>();

    static {
        primitves.put(Integer.TYPE, Integer.class);
        primitves.put(Long.TYPE, Long.class);
        primitves.put(Double.TYPE, Double.class);
        primitves.put(Boolean.TYPE, Boolean.class);
    }

    /**
     * TODO: provide support for nested JSON objects
     * TODO: provide support for embedded JSON Arrays
     *
     * @param jsonObject
     * @param beanToBeCreatedClass
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws JSONException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static <T> T unmarshall(JSONObject jsonObject, java.lang.Class<T> beanToBeCreatedClass) throws IllegalAccessException, InstantiationException, JSONException, NoSuchMethodException, InvocationTargetException {
        T value = beanToBeCreatedClass.getConstructor().newInstance();

        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object field = jsonObject.get(key);
            String methodName = SETTER_PREFIX + key;
            // discriminate based on type
            if (field instanceof String) {
                //System.err.println("contains string" + field);
                // string shall be used directly
                try {

                    beanToBeCreatedClass.getMethod(methodName, String.class).invoke(value, field);
                    continue;
                } catch (NoSuchMethodException e) {
                    // that means there was no such method, proceed
                }
                // or maybe there is method with suitable parameter?
                for (Method method : beanToBeCreatedClass.getMethods()) {
                    //System.err.println("method:" + method);
                    if (methodName.equals(method.getName()) && method.getParameterTypes().length == 1) {
                        //System.err.println("...candidate with 1 param");
                        Class<?> paramClass = method.getParameterTypes()[0];
                        if (paramClass.isPrimitive() && primitves.get(paramClass) != null) {
                            //System.err.println("... primitive found");
                            paramClass = primitves.get(paramClass);
                        }
                        try {
                            method.invoke(value, paramClass.getConstructor(String.class).newInstance(field));
                        } catch (NoSuchMethodException nsme) {
                            // we are failed here,  but so what? be lenient
                        }
                        break;

                    }
                }
                // we are done with string
            } else {
                // catch all - everything except string shall be processed via dedicated setter
                for (Method method : beanToBeCreatedClass.getMethods()) {
                    //System.err.println("method:" + method);
                    //if (method.getParameterTypes().length == 1) {
                    // System.err.println("type:" + method.getParameterTypes()[0]);
                    //}
                    //System.err.println("field:" + field.getClass());
                    if (methodName.equals(method.getName()) && method.getParameterTypes().length == 1 /*&& method.getParameterTypes()[0].isAssignableFrom(field.getClass())*/) {
                        method.invoke(value, field);
                        break;
                    }
                }
            }
        }
        return value;
    }

    /**
     * convenience method parsing JSON on the fly
     *
     * @param json
     * @param beanToBeCreatedClass
     * @param <T>
     * @return
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws JSONException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> T unmarshall(String json, java.lang.Class<T> beanToBeCreatedClass) throws InvocationTargetException, NoSuchMethodException, JSONException, InstantiationException, IllegalAccessException {
        return unmarshall(new JSONObject(json), beanToBeCreatedClass);
    }
}
