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
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * test capabilities of JSON unmarshaler
 */
public class JSONUnmarshallerTest {
    @Mocked
    JsonReader source;


    /**
     * object of proper class shall be created via default constructor
     */
    @Test
    public void testObjectCreation(@Mocked final BeanToBeCreated btc) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        new Expectations() {
            {

                new BeanToBeCreated();

                result = Collections.EMPTY_LIST.iterator();
            }
        };

        JSONUnmarshaller.unmarshall(source, BeanToBeCreated.class);
    }
    // just a bean with default constructor,  nothing else

    public static class BeanToBeCreated {

    }

    /**
     * no default constructor -no party
     */
    @Test
    public void testThatNotABeanIsBombed() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            JSONUnmarshaller.unmarshall(source, BeanWithoutDefaultConstructor.class);
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
    public void testThatNotAccessibleDefaultConstructorIsBombed() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            JSONUnmarshaller.unmarshall(source, BeanWithNotAccesibleDefaultConstructor.class);
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
    public void testThatStringIsSetToStringField() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"String"}).iterator();
        new Expectations() {
            {
               // jsonObject.keys();
                result = keys;
              //  jsonObject.get("String");
                result = "blam";

            }
        };

        WithStringField withString = JSONUnmarshaller.unmarshall(source, WithStringField.class);

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
    public void testThatStringIsUsedAsConstructorParameter() throws  InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList("Integer").iterator();
        new Expectations() {
            {
              //  jsonObject.keys();
                result = keys;
              //  jsonObject.get("Integer");
                result = "555";

            }
        };

        WithInteger withInteger = JSONUnmarshaller.unmarshall(source, WithInteger.class);

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
    @Test
    public void testThatStringIsUsedAsPrimitive() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"Primitive"}).iterator();
        new Expectations() {
            {
            //    jsonObject.keys();
                result = keys;
             //   jsonObject.get("Primitive");
                result = "555";

            }
        };

        WithInt withInt = JSONUnmarshaller.unmarshall(source, WithInt.class);

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
    public void testIntegerObjectSetting() throws  InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList(new String[]{"Integer"}).iterator();
        new Expectations() {
            {
            //    jsonObject.keys();
                result = keys;
             //   jsonObject.get("Integer");
                result = 555;

            }
        };

        WithInteger withInteger = JSONUnmarshaller.unmarshall(source, WithInteger.class);

        assertEquals(new Integer(555), withInteger.getInteger());
    }


    /**
     * shall set integer as primitive value
     */
    @Test
    public void testIntegerPrimitiveSetting() throws  InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList("Primitive").iterator();
        new Expectations() {
            {
          //      jsonObject.keys();
                result = keys;
            //    jsonObject.get("Primitive");
                result = 555;

            }
        };

        WithInt withInteger = JSONUnmarshaller.unmarshall(source, WithInt.class);

        assertEquals(555, withInteger.getPrimitive());
    }

    @Test
    public void testThatObjectBooleanIsSet() throws  InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList("bool").iterator();
        new Expectations() {
            {
             //   jsonObject.keys();
                result = keys;
            //    jsonObject.get("bool");
                result = Boolean.TRUE;

            }
        };

        WithBoolean withInteger = JSONUnmarshaller.unmarshall(source,WithBoolean.class);

        assertTrue( withInteger.getBool());

    }

    public static class WithBoolean {
        Boolean bool;

        public Boolean getBool() {
            return bool;
        }

        public void setBool(Boolean bool) {
            this.bool = bool;
        }
    }

    /**
     * primitive array field shall be unmarshalled
     * TODO: improve test coverage
     */
    @Test
    public void testPrimitiveArrayUnmarshalling() throws  InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList("IntegerArray").iterator();
        new Expectations() {
            {
                /*
                jsonObject.keys();
                result = keys;
                jsonObject.get("IntegerArray");
                result = jsonArray;
                jsonArray.length();
                result = 2;
                //recursive parsing first nested array
                jsonArray.get(0);
                result = jsonArray;

                jsonArray.length();
                result = 2;
                jsonArray.get(0);
                result = 1;
                jsonArray.get(1);
                result = 2;
                // recursive parsing second nested array
                jsonArray.get(1);
                result = jsonArray;

                jsonArray.length();
                result = 2;
                jsonArray.get(0);
                result = 3;
                jsonArray.get(1);
                result = 4;
                */
            }
        };

        WithIntegerArray wia = JSONUnmarshaller.unmarshall(source, WithIntegerArray.class);

        assertNotNull(wia.getIntegerArray());
        assertEquals(2, wia.getIntegerArray().length);
        assertEquals(2, wia.getIntegerArray()[0].length);
        assertEquals(2, wia.getIntegerArray()[1].length);

        assertEquals(1, wia.getIntegerArray()[0][0]);
        assertEquals(2, wia.getIntegerArray()[0][1]);
        assertEquals(3, wia.getIntegerArray()[1][0]);
        assertEquals(4, wia.getIntegerArray()[1][1]);
    }

    public static class WithIntegerArray {
        int[][] array;

        public void setIntegerArray(int[][] array) {
            this.array = array;
        }

        public int[][] getIntegerArray() {
            return array;
        }
    }


    /**
     * nested bean shall be parsed
     */
    @Test
    public void testNestedBeanParsing() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList("WithInt").iterator();
        final Iterator nestedKeys = Arrays.asList("Primitive").iterator();
        new Expectations() {
            {
              //  jsonObject.keys();
                result = keys;
              //  jsonObject.get("WithInt");
               // result = jsonObject;
               // jsonObject.keys();
              //  result = nestedKeys;
              //  jsonObject.get("Primitive");
              //  result = 239;
            }
        };


        WithNestedBean wnb = JSONUnmarshaller.unmarshall(source, WithNestedBean.class);
        assertNotNull(wnb);
        assertNotNull(wnb.getWithInt());
        assertEquals(239, wnb.getWithInt().getPrimitive());
    }


    public static class WithNestedBean {
        WithInt withInt;

        public WithInt getWithInt() {
            return withInt;
        }

        public void setWithInt(WithInt withInt) {
            this.withInt = withInt;
        }
    }

    /**
     * if there is a field without corresponding setter, it has to be ignored.
     */
    @Test
    public void testThatKeyWithoutSetterIsIgnoredSafely() throws InvocationTargetException, NoSuchMethodException,  InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList("invalidKey").iterator();
        new Expectations() {
            {
               //jsonObject.keys();
                result = keys;

             //   jsonObject.get("invalidKey");
                result = 444;
            }
        };

        WithInt withInteger = JSONUnmarshaller.unmarshall(source, WithInt.class);
        assertNotNull(withInteger);

    }

    /**
     * both capitalisation forms must be allowed
     */
    @Test
    public void testThatLowercasePropertyNamesAreRecognised() throws  InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final Iterator keys = Arrays.asList("primitive").iterator();
        new Expectations() {
            {
             //   jsonObject.keys();
                result = keys;
            //    jsonObject.get("primitive");
                result = 555;
            }
        };

        WithInt withInteger = JSONUnmarshaller.unmarshall(source, WithInt.class);

        assertEquals(555, withInteger.getPrimitive());
    }
}
