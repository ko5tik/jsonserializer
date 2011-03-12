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


import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * marshall beans to JSON into writer
 */
public class JSONMarshaller {
    private static final String GETTER_PREFIX = "get";
    private static final int BEGIN_INDEX = GETTER_PREFIX.length();
    public static final String IS_PREFIX = "is";
    public static final int IS_LENGTH = 2;

    /**
     * cache methods in classes because on android it seems to be disabled
     */
    static final HashMap<Class, Method[]> methodCache = new HashMap();

    /**
     * marshall supplied object (tree?) to JSON
     *
     * @param object
     * @return
     */
    public static void marshall(JsonWriter writer, Object object) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, IOException {
        marshallRecursive(writer, object);

    }

    /**
     * recursively marshall to JSON writer
     *
     * @param object
     */
    static void marshallRecursive(JsonWriter writer, Object object) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, IOException {
        // nothing to marshall
        if (object == null)
            return;
        // primitive object is a field and does not interest us here
        if (object.getClass().isPrimitive())
            return;
        // object not null,  and is not primitive - iterate through getters
        // begin object writing
        writer.beginObject();

        // cache methods  for classes
        Method[] methods = methodCache.get(object.getClass());
        if (methods == null) {
            methods = object.getClass().getMethods();
            methodCache.put(object.getClass(), methods);
        }

        for (Method method : methods) {

            // System.err.println("method:" + method);
            // our getters are parameterless and start with "get"
            if ((method.getName().startsWith(GETTER_PREFIX) && method.getName().length() > BEGIN_INDEX || method.getName().startsWith(IS_PREFIX) && method.getName().length() > IS_LENGTH) && (method.getModifiers() & Modifier.PUBLIC) != 0 && method.getParameterTypes().length == 0 && method.getReturnType() != void.class && !method.getName().equals("getClass")) {
                //  System.err.println("... eligible");
                // write name:
                writer.name(propertize(method.getName()));
                // retrieve value
                Object value = method.invoke(object);
                marshallValue(writer, value);

            }

        }
        // we are done here
        writer.endObject();
    }

    /**
     * marshall single value
     *
     * @param writer
     * @param value
     * @throws IOException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     */
    private static void marshallValue(JsonWriter writer, Object value) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        // is return value primitive?
        Class<?> type = value.getClass();
        //System.err.println("class:" + type);
        if (String.class.equals(type) || Character.class.equals(type)) {
            //System.err.println("string");
            writer.value(value.toString());
        } else if (Boolean.class.isAssignableFrom(type)) {
            //  System.err.println("boolean");
            writer.value((Boolean) value);
        } else if (Number.class.isAssignableFrom(type)) {
            // System.err.println("number");
            writer.value((Number) value);
            return;
        } else if (type.isArray()) {
            marshallArray(writer, value);
            return;
        } else {
            // does it have default constructor?
            try {
                if (type.getConstructor() != null) {
                    marshall(writer, value);
                    return;
                }
            } catch (NoSuchMethodException ex) {
                // just ignore it here, it means no such constructor was found
                // System.err.println("writing null value, no default constructor");
                writer.nullValue();
            }
        }
    }

    /**
     * recursively marshall [multidimensional? - of course!!! ] array
     *
     * @param array
     * @return
     */
    public static void marshallArray(JsonWriter writer, Object array) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        if (array.getClass().isArray()) {
            writer.beginArray();
            Class componentType = array.getClass().getComponentType();
            //  JSONArray retval = new JSONArray();
            final int arrayLength = Array.getLength(array);
            for (int i = 0; i < arrayLength; i++) {
                marshallValue(writer, Array.get(array, i));
            }
            writer.endArray();
        }
    }

    /**
     * convert method name to property
     *
     * @param name
     */
    public static String propertize(String name) {
        if (name.startsWith(IS_PREFIX)) {
            return name.substring(IS_LENGTH);
        }
        return name.substring(BEGIN_INDEX);
    }
}
