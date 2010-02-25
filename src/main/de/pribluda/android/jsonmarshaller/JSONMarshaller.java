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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;

/**
 * marshall beans to JSON into writer
 */
public class JSONMarshaller {
    private static final String GETTER_PREFIX = "get";
    private static final int BEGIN_INDEX = GETTER_PREFIX.length();

    /**
     * marshall supplied object (tree?) to JSON
     *
     * @param object
     * @return
     */
    public static JSONObject marshall(Object object) throws InvocationTargetException, JSONException, IllegalAccessException, NoSuchMethodException {
        JSONObject retval = new JSONObject();
        marshallRecursive(retval, object);
        return retval;
    }

    /**
     * recursively marshall to JSON
     *
     * @param sink
     * @param object
     */
    static void marshallRecursive(JSONObject sink, Object object) throws JSONException, InvocationTargetException, IllegalAccessException {
        //System.err.println("marshalling..." + object);
        // nothing to marshall
        if (object == null)
            return;
        // primitive object is a field and does not interes us here
        if (object.getClass().isPrimitive())
            return;
        // object not null,  and is not primitive - iterate through getters
        for (Method method : object.getClass().getDeclaredMethods()) {
            // our getters are parameterless and start with "get"
            // System.err.println("method:" + method);
            //System.err.println("method return type:" + method.getReturnType());
            //System.err.println("method name:" + method.getName());
            if (method.getName().startsWith(GETTER_PREFIX) && method.getName().length() > BEGIN_INDEX && (method.getModifiers() & Modifier.PUBLIC) != 0 && method.getParameterTypes().length == 0 && method.getReturnType() != void.class) {
                //System.err.println("method name passsed");
                // is return value primitive?
                Class<?> type = method.getReturnType();
                if (type.isPrimitive() || String.class.equals(type)) {
                    // it is, marshall it
                    //   System.err.println("marshall primitive " + method.getName() + "/" + method.invoke(object));
                    sink.put(propertize(method.getName()), method.invoke(object));
                    continue;
                }
                // does it have default constructor?
                try {
                    //System.err.println("determining default constructor");
                    if (method.getReturnType().getConstructor() != null) {
                        JSONObject descendant = new JSONObject();
                        //System.err.println("nested created");
                        marshallRecursive(descendant, method.invoke(object));
                        //System.err.println("descendant marshalled");
                        sink.put(propertize(method.getName()), descendant);
                        continue;
                    }
                } catch (NoSuchMethodException ex) {
                    //System.err.println("failed with exception:" + ex);
                    // just ignore it here, it means no such constructor was found
                }
            }
        }
    }

    /**
     * recursively marshall [multidimensional? - of course!!! ] array
     *
     * @param array
     * @return
     */
    static JSONArray marshallArray(Object array) {
        if (array.getClass().isArray()) {
            Class componentType = array.getClass().getComponentType();
            System.err.println("componentType:" + componentType);
            JSONArray retval = new JSONArray();
            final int arrayLength = Array.getLength(array);
            // stirngs and primitives must be marshalled directly
            if (componentType.isPrimitive() || String.class.equals(componentType)) {

                for (int i = 0; i < arrayLength; i++) {
                    retval.put(Array.get(array, i));
                }
            } else if(componentType.isArray()) {
                // that's cool, nested array recurse
                for(int i = 0; i < arrayLength; i++) {
                    retval.put(marshallArray(Array.get(array,i)));
                }
            }

            return retval;
        }

        return null;
    }

    /**
     * convert method name to property
     *
     * @param name
     */
    public static String propertize(String name) {
        return name.substring(BEGIN_INDEX);
    }
}
