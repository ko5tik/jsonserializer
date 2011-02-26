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

/**
 * marshall beans to JSON into writer
 */
public class JSONMarshaller {
    private static final String GETTER_PREFIX = "get";
    private static final int BEGIN_INDEX = GETTER_PREFIX.length();
    public static final String IS_PREFIX = "is";
    public static final int IS_LENGTH = 2;

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
        for (Method method : object.getClass().getMethods()) {

            System.err.println("method:" + method);
            // our getters are parameterless and start with "get"
            if ((method.getName().startsWith(GETTER_PREFIX) && method.getName().length() > BEGIN_INDEX || method.getName().startsWith(IS_PREFIX) && method.getName().length() > IS_LENGTH) && (method.getModifiers() & Modifier.PUBLIC) != 0 && method.getParameterTypes().length == 0 && method.getReturnType() != void.class) {
                System.err.println("... eligible");
                // write name:
                writer.name(propertize(method.getName()));
                // is return value primitive?
                Class<?> type = method.getReturnType();
                if (type.isPrimitive() || String.class.equals(type)) {
                    System.err.println("primitive");
                    // TODO: better discrimination may be necessary
                    writer.value(method.invoke(object).toString());
                    continue;
                } else if (type.isArray()) {
                    marshallArray(writer, method.invoke(object));
                    continue;
                } else {
                    // does it have default constructor?
                    try {
                        if (method.getReturnType().getConstructor() != null) {
                            marshall(writer, method.invoke(object));
                            continue;
                        }
                    } catch (NoSuchMethodException ex) {
                        // just ignore it here, it means no such constructor was found
                        writer.nullValue();
                    }
                }
            }

        }
        // we are done here
        writer.endObject();
    }

    /**
     * recursively marshall [multidimensional? - of course!!! ] array
     *
     * @param array
     * @return
     */
    static void marshallArray(JsonWriter sink, Object array) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (array.getClass().isArray()) {
            Class componentType = array.getClass().getComponentType();
            //  JSONArray retval = new JSONArray();
            final int arrayLength = Array.getLength(array);
            // stirngs and primitives must be marshalled directly
            if (componentType.isPrimitive() || String.class.equals(componentType)) {

                for (int i = 0; i < arrayLength; i++) {
                    //   retval.put(Array.get(array, i));
                }
            } else if (componentType.isArray()) {
                // that's cool, nested array recurse
                for (int i = 0; i < arrayLength; i++) {
                    //   retval.put(marshallArray(Array.get(array, i)));
                }
            } else {
                // treat component as a bean   if it got default constructor
                try {
                    //System.err.println("determining default constructor:" + componentType.getConstructor());
                    if (componentType.getConstructor() != null) {
                        for (int i = 0; i < arrayLength; i++) {
                            //    retval.put(marshall(Array.get(array, i)));
                        }
                    }
                } catch (NoSuchMethodException ex) {
                    // just ignore it here, it means no such constructor was found
                }
            }


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
