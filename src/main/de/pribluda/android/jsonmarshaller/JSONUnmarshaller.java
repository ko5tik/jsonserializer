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
    static final HashMap<Class, Method[]> methodCache = new HashMap();
    static final HashMap<Class, Constructor> constructorCache = new HashMap();

    static {
        primitves.put(Integer.TYPE, Integer.class);
        primitves.put(Long.TYPE, Long.class);
        primitves.put(Double.TYPE, Double.class);
        primitves.put(Boolean.TYPE, Boolean.class);
        primitves.put(Character.TYPE, Character.class);
        primitves.put(Short.TYPE, Short.class);
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
            StringBuilder sb = new StringBuilder();
            sb.append(SETTER_PREFIX).append(Character.toUpperCase(key.charAt(0))).append(key.substring(1));
            String methodName = sb.toString();


            Method method = getCandidateMethod(beanToBeCreatedClass, methodName);

            // must be kind of setter method
            if (method != null) {
                Class clazz = method.getParameterTypes()[0];
                // as we have setter, we can process value
                Object v = unmarshalValue(reader, clazz);

                // can we use setter method directly?
                if (clazz.isAssignableFrom(v.getClass())) {
                    method.invoke(value, v);
                    continue;
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

    /**
     * convert unmarshalled value to object. here we thread only primitive values because
     * objects were already processed.  Only 2 types of values can occur and be treated  here
     * - booleans
     * - strings
     *
     * @param clazz
     * @param v
     * @param <T>
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static <T> Object convertToObject(Class clazz, Object v) throws InstantiationException, IllegalAccessException, InvocationTargetException {

        // deprimitivize
        if (clazz.isPrimitive() && primitves.get(clazz) != null) {
            clazz = primitves.get(clazz);
        }

        // if class matches or is assignable - do  nothing ( this is certainly boolean )
        if (clazz.isAssignableFrom(v.getClass())) {
            //System.err.println("is assignable, return object");
            return v;
        }

        // do we have character? it needs special treatment
        if (clazz.equals(Character.class)) {

            final String stingValue = v.toString();
            if (stingValue.length() > 0) {
                return new Character(stingValue.charAt(0));
            } else {
                return null;
            }

        }

        Object obj = null;
        //System.err.println(" **************** retrieve constructor to convert to object " + clazz + " value:" + v.getClass());
        // if we are here, we can process only string.
        if (String.class.equals(v.getClass())) {
            // as reflection is expensive on android,  we go for some direct access
            if (Byte.class.equals(clazz)) {
                return Byte.parseByte((String) v);
            } else if (Double.class.equals(clazz)) {
                return Double.parseDouble((String) v);
            } else if (Float.class.equals(clazz)) {
                return Float.parseFloat((String) v);
            } else if (Integer.class.equals(clazz)) {
                return Integer.parseInt((String) v);
            } else if (Long.class.equals(clazz)) {
                return Long.parseLong((String) v);
            } else if (Short.class.equals(clazz)) {
                return Short.parseShort((String) v);
            }
            // ok, here we go, try to obtain constructor
            Constructor constructor = constructorCache.get(clazz);

            try {
                if (constructor == null) {
                    constructor = clazz.getConstructor(v.getClass());
                    constructorCache.put(clazz, constructor);
                }
            } catch (NoSuchMethodException nsme) {
                // we are failed here,  but so what? be lenient  and ignore this
                return null;
            }

            try {
                obj = constructor.newInstance(v);
            } catch (Exception e) {
                // we can not instantiate - so what...
            }
        }
        return obj;
    }


    /**
     * unmarshal current value, possibly walking down the three
     *
     * @param reader json reader to pull value from
     * @param clazz  expected class
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
                value = reader.nextString();
                break;
            case BOOLEAN:
                value = reader.nextBoolean();
                break;
            case BEGIN_ARRAY:
                //  we are interested in arrays
                if (clazz.isArray()) {
                    // populate field value from JSON Array
                    value = populateRecusrsive(clazz, reader);
                } else {
                    reader.skipValue();
                }
                break;
            case BEGIN_OBJECT:
                // so, we are unmarshalling nested object - recurse
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
                    if (obj != null) {
                        value.add(obj);
                    }
                }
            }
            // copy everything to array,
            retval = Array.newInstance(componentType, value.size());
            for (int i = 0; i < value.size(); i++) {
                Array.set(retval, i, value.get(i));
            }
        } else {
            return null;
        }
        reader.endArray();

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
        Method[] candidates = methodCache.get(clazz);
        if (candidates == null) {
            candidates = clazz.getMethods();
            methodCache.put(clazz, candidates);
        }
        for (Method method : candidates) {
            if (method.getParameterTypes().length == 1 && name.equals(method.getName()))
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
