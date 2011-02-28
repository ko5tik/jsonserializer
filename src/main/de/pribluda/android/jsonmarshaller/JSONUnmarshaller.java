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


import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
     * unmarshall single JSON object
     *
     * @param beanToBeCreatedClass
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static <T> T unmarshall(JsonReader reader, java.lang.Class<T> beanToBeCreatedClass) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        // nothing there - bail out
        reader.beginObject();

        if (reader.peek() == null) {

            return null;
        }
        T value = beanToBeCreatedClass.getConstructor().newInstance();
        while (reader.hasNext()) {
            String key = reader.nextName();


            //  capitalise to standard setter pattern
            String methodName = SETTER_PREFIX + key.substring(0, 1).toUpperCase() + key.substring(1);


            Method method = getCandidateMethod(beanToBeCreatedClass, methodName);

            // must be kind of setter method
            if (method != null && method.getParameterTypes().length == 1) {
                Class clazz = method.getParameterTypes()[0];
                // as we have setter, we can process value
                Object v = unmarshalValue(reader, clazz);
                System.err.println("class to set:" + clazz);
                System.err.println("value:" + v);
                // can we use setter method directly?
                if (clazz.isAssignableFrom(v.getClass())) {
                    method.invoke(value, v);
                    continue;
                } else {
                    System.err.println("... not assignable");
                }
                Object obj = convertToObject(clazz, v);
                if (obj != null)
                    method.invoke(value, obj);
            } else {
                // no suitable method was found - skip this value altogether
                reader.skipValue();
            }
        }


        reader.endObject();

        return value;

    }

    /**
     * read array into list
     *
     * @param reader
     * @param beanToBeCreatedClass
     * @return
     */
    public static <T> List<T> unmarshallArray(JsonReader reader, java.lang.Class<T> beanToBeCreatedClass) throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ArrayList<T> retval = new ArrayList();
        reader.beginArray();
        // read objects after each other
        while (reader.peek() == JsonToken.BEGIN_OBJECT) {
            retval.add(unmarshall(reader, beanToBeCreatedClass));
        }
        reader.endArray();

        return retval;
    }


    private static <T> Object convertToObject(Class clazz, Object v) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        // or maybe there is method with suitable parameter?
        if (clazz.isPrimitive() && primitves.get(clazz) != null) {
            //System.err.println("... primitive found");
            clazz = primitves.get(clazz);
        }
        Constructor constructor = null;
        try {
            constructor = clazz.getConstructor(v.getClass());
        } catch (NoSuchMethodException nsme) {
            // we are failed here,  but so what? be lenient  and ignore this
            return null;
        }
        Object obj = null;
        try {
            obj = constructor.newInstance(v);
        } catch (Exception e) {
            // we can not instantiate - so what...
        }
        return obj;
    }

    /**
     * unmarshal current value, possibly walking down the three
     *
     * @param reader
     * @return
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     */
    private static Object unmarshalValue(JsonReader reader, Class clazz) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {

        Object value = null;

        switch (reader.peek()) {
            case STRING:
            case NUMBER:
                // process string
                System.err.println("processing string");
                value = reader.nextString();
                break;
            case BOOLEAN:
                value = reader.nextBoolean();
                break;
            case BEGIN_ARRAY:
                //  we are interested in arrays
                if (clazz.isArray()) {
                    System.err.println("... is array");

                    // populate field value from JSON Array
                    value = populateRecusrsive(clazz, reader);
                } else {
                    reader.skipValue();
                }
                break;
            case BEGIN_OBJECT:
                // so, we are unmarshalling nested object - recyrse
                value = unmarshall(reader, clazz);
                break;
            default:
                // do not know what to do with it,  skip
                reader.skipValue();
        }

        return value;
    }

    /**
     * recursively populate array out of hierarchy of JSON Objects
     *
     * @param arrayClass original array class
     * @param reader     reader to be processed
     * @return
     */
    private static Object populateRecusrsive(Class arrayClass, JsonReader reader) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        System.err.println("recursive populating " + arrayClass);
        ArrayList value = new ArrayList();
        Object retval = null;
        reader.beginArray();
        if (arrayClass.isArray()) {
            // create list, as we do not know size yet


            final Class componentType = arrayClass.getComponentType();
            // iterate over reader
            while (reader.hasNext()) {
                Object component;
                if (componentType.isArray()) {
                    // component is array - dive down
                    component = populateRecusrsive(componentType, reader);
                    if (component != null) {
                        value.add(component);
                    }
                } else {
                    // component is leaf,
                    Object leaf = unmarshalValue(reader, componentType);
                    Object obj = convertToObject(componentType, leaf);
                    System.err.println("converted to class:" + obj);
                    if (obj != null) {
                        System.err.println("... add to list");
                        value.add(obj);
                    }
                }
            }
            // copy everything to array,
            System.err.println("creating array of size:" + value.size());
            retval = Array.newInstance(componentType, value.size());
            for (int i = 0; i < value.size(); i++) {
                Array.set(retval, i, value.get(i));
            }
        } else {
            return null;
        }
        reader.endArray();


        System.err.println("array processing ends" + retval);
        return retval;
    }

    /**
     * retrieve candidate setter method
     *
     * @param clazz
     * @param name
     * @return
     */
    private static Method getCandidateMethod(Class clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (name.equals(method.getName()) && method.getParameterTypes().length == 1)
                return method;
        }
        return null;
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
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static <T> T unmarshall(String json, java.lang.Class<T> beanToBeCreatedClass) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        return unmarshall(new JsonReader(new StringReader(json)), beanToBeCreatedClass);
    }
}
