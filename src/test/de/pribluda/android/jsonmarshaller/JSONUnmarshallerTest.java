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

import mockit.Expectations;
import mockit.Mocked;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * test capabilities of JSON unmarshaler
 */
public class JSONUnmarshallerTest {
    @Mocked
    JSONObject jsonObject;


    /**
     * object of proper class shall be created via default constructor
     */
    @Test
    public void testObjectCreation(@Mocked final BeanToBeCreated btc) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, JSONException {

        new Expectations() {
            {

                new BeanToBeCreated();
                jsonObject.keys();
                result = Collections.EMPTY_LIST.iterator();
            }
        };

        JSONUnmarshaller.unmarshall(jsonObject, BeanToBeCreated.class);
    }
    // just a bean with default constructor,  nothing else

    public static class BeanToBeCreated {

    }

    /**
     * no default constructor -no party
     */
    @Test
    public void testThatNotABeanIsBombed() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, JSONException {
        try {
            JSONUnmarshaller.unmarshall(jsonObject, BeanWithoutDefaultConstructor.class);
            fail("has to bomb on absent defaut constructor");
        } catch (NoSuchMethodException ex) {
            // that's OK
        }
    }


    public static class BeanWithoutDefaultConstructor {
        public BeanWithoutDefaultConstructor(Integer foo) {

        }
    }


    /**
     * shall bomb on not accesible default constructor
     */
    @Test
    public void testThatNotAccessibleDefaultConstructorIsBombed() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, JSONException {
        try {
            JSONUnmarshaller.unmarshall(jsonObject, BeanWithNotAccesibleDefaultConstructor.class);
            fail("I have expected exceptio here because  default consstructor was quite provate");
        } catch (NoSuchMethodException ex) {
            // that's OK...
        }
    }

    public static class BeanWithNotAccesibleDefaultConstructor {
        protected BeanWithNotAccesibleDefaultConstructor() {
        }
    }


    /**
     * string field shall be set to string field
     */
    @Test
    public void testThatStringIsSetToStringField() throws InvocationTargetException, NoSuchMethodException, JSONException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"String"}).iterator();
        new Expectations() {
            {
                jsonObject.keys();
                result = keys;
                jsonObject.get("String");
                result = "blam";

            }
        };

        WithStringField withString = JSONUnmarshaller.unmarshall(jsonObject, WithStringField.class);

        assertEquals("blam", withString.getString());
    }

    public static class WithStringField {
        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        String string;

    }

    /**
     * string shall be used to construct (i.e integer) object as only constructor paramrter
     */
    @Test
    public void testThatStringIsUsedAsConstructorParameter() throws JSONException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"Integer"}).iterator();
        new Expectations() {
            {
                jsonObject.keys();
                result = keys;
                jsonObject.get("Integer");
                result = "555";

            }
        };

        WithInteger withInteger = JSONUnmarshaller.unmarshall(jsonObject, WithInteger.class);

        assertEquals(new Integer(555), withInteger.getInteger());
    }

    public static class WithInteger {
        Integer integer;

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }
    }


    /**
     * not sure this is really necessary, but for sake of completeness
     * string shjall be also converted to integer
     * TODO: investigate if we really need this feature
     */

    public void testThatStringIsUsedAsPrimitive() throws JSONException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"Primitive"}).iterator();
        new Expectations() {
            {
                jsonObject.keys();
                result = keys;
                jsonObject.get("Primitive");
                result = "555";

            }
        };

        WithInt withInt = JSONUnmarshaller.unmarshall(jsonObject, WithInt.class);

        assertEquals(555, withInt.getPrimitive());
    }

    public static class WithInt {
        int primitive;

        public int getPrimitive() {
            return primitive;
        }

        public void setPrimitive(int primitive) {
            this.primitive = primitive;
        }
    }


    /**
     * shall set integer as object
     */
    @Test
    public void testIntegerObjectSetting() throws JSONException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"Integer"}).iterator();
        new Expectations() {
            {
                jsonObject.keys();
                result = keys;
                jsonObject.get("Integer");
                result = 555;

            }
        };

        WithInteger withInteger = JSONUnmarshaller.unmarshall(jsonObject, WithInteger.class);

        assertEquals(new Integer(555), withInteger.getInteger());
    }


    /**
     * shall set integer as primitive value
     */
    @Test
    public void testIntegerPrimitiveSetting() throws JSONException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"Primitive"}).iterator();
        new Expectations() {
            {
                jsonObject.keys();
                result = keys;
                jsonObject.get("Primitive");
                result = 555;

            }
        };

        WithInt withInteger = JSONUnmarshaller.unmarshall(jsonObject, WithInt.class);

        assertEquals(555, withInteger.getPrimitive());
    }
}