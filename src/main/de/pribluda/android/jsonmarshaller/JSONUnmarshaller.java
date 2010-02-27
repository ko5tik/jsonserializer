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
import org.json.JSONArray;
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

            Method method = getCandidateMethod(beanToBeCreatedClass, methodName);

            Class clazz = method.getParameterTypes()[0];

            if (method != null) {
                // discriminate based on type
                if (field instanceof String) {

                    // string shall be used directly, either to set or as constructor parameter (if suitable)
                    try {

                        beanToBeCreatedClass.getMethod(methodName, String.class).invoke(value, field);
                        continue;
                    } catch (NoSuchMethodException e) {
                        // that means there was no such method, proceed
                    }
                    // or maybe there is method with suitable parameter?
                    if (clazz.isPrimitive() && primitves.get(clazz) != null) {
                        //System.err.println("... primitive found");
                        clazz = primitves.get(clazz);
                    }
                    try {
                        method.invoke(value, clazz.getConstructor(String.class).newInstance(field));
                    } catch (NoSuchMethodException nsme) {
                        // we are failed here,  but so what? be lenient
                    }

                }
                // we are done with string
                else if (field instanceof JSONArray) {
                    // JSON array corresponds either to array type,  or  to some collection

                    // we are interested in arrays
                    if (clazz.isArray()) {
                        //  is base class suitable?
                        Class baseClass = retrieveArrayBase(clazz);
                    }
                    //  TODO: implement collections (how???)

                } else if (field instanceof JSONObject) {
                    // JSON object means nested bean - process recusively
                    method.invoke(value, unmarshall((JSONObject) field,clazz));

                } else {
                  
                    // fallback here,  types not yet processed will be
                    // set directly ( if possible )
                    // TODO: guard this? for better leniency
                    method.invoke(value, field);
                }

            }
        }
        return value;
    }

    private static Method getCandidateMethod(Class clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (name.equals(method.getName()) && method.getParameterTypes().length == 1)
                return method;
        }
        return null;
    }

    /**
     * recursively retrieve base array class
     *
     * @param clazz
     * @return
     */
    private static Class retrieveArrayBase(Class clazz) {
        if (clazz.isArray())
            return retrieveArrayBase(clazz.getComponentType());
        return clazz;
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
