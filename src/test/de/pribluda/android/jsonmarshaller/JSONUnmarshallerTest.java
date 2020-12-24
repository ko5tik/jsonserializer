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
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * test capabilities of JSON unmarshaler
 */
public class JSONUnmarshallerTest {

    JsonReader source;

    /**
     *
     */
    @Test
    public void testEmptyJsonCreatesNothing() throws InvocationTargetException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            source = new JsonReader(new StringReader(""));
            JSONUnmarshaller.unmarshall(source, BeanToBeCreated.class);
            fail("must bomb with exception");
        } catch (IOException e) {
            // anticipated
        }

    }

    /**
     * object of proper class shall be created via default constructor
     */
    @Test
    public void testObjectCreation() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        source = new JsonReader(new StringReader("{}"));

        assertNotNull(JSONUnmarshaller.unmarshall(source, BeanToBeCreated.class));
    }
    // just a bean with default constructor,  nothing else

    public static class BeanToBeCreated {

    }

    /**
     * no default constructor -no party
     */
    @Test
    public void testThatNotABeanIsBombed() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        try {
            source = new JsonReader(new StringReader("{}"));
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
    public void testThatNotAccessibleDefaultConstructorIsBombed() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        try {
            source = new JsonReader(new StringReader("{}"));
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
    public void testThatStringIsSetToStringField() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{ String:'blam'}"));
        source.setLenient(true);
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
     * shall pass integer to value
     */
    @Test
    public void testThatStringIsUsedAsConstructorParameter() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{ 'integer':555}"));
        source.setLenient(true);

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
     * string shall be also converted to integer
     * TODO: investigate if we really need this feature
     */
    @Test
    public void testThatStringIsUsedAsPrimitive() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{Primitive:555}"));
        source.setLenient(true);

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
    public void testIntegerObjectSetting() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {

        // must be lenient
        source = new JsonReader(new StringReader("{Integer:555}"));
        source.setLenient(true);


        WithInteger withInteger = JSONUnmarshaller.unmarshall(source, WithInteger.class);

        assertEquals(new Integer(555), withInteger.getInteger());
    }


    /**
     * shall set integer as primitive value
     */
    @Test
    public void testIntegerPrimitiveSetting() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{Primitive:555}"));
        source.setLenient(true);

        WithInt withInteger = JSONUnmarshaller.unmarshall(source, WithInt.class);

        assertEquals(555, withInteger.getPrimitive());
    }

    @Test
    public void testThatObjectBooleanIsSet() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {

        // must be lenient
        source = new JsonReader(new StringReader("{bool:true}"));
        source.setLenient(true);

        WithBoolean withInteger = JSONUnmarshaller.unmarshall(source, WithBoolean.class);

        assertTrue(withInteger.getBool());

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
     * primitive array field shall be unmarshalled , also for nested arrays
     * TODO: improve test coverage
     */
    @Test
    public void testPrimitiveArrayUnmarshalling() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{IntegerArray:[[1,2],[3,4]]}"));
        source.setLenient(true);


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
    public void testNestedBeanParsing() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{WithInt:{Primitive:239}}"));
        source.setLenient(true);

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
    public void testThatKeyWithoutSetterIsIgnoredSafely() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{invalidKey:444}"));
        source.setLenient(true);


        WithInt withInteger = JSONUnmarshaller.unmarshall(source, WithInt.class);
        assertNotNull(withInteger);

    }

    /**
     * both capitalisation forms must be allowed
     */
    @Test
    public void testThatLowercasePropertyNamesAreRecognised() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        // must be lenient
        source = new JsonReader(new StringReader("{primitive:555}"));
        source.setLenient(true);


        WithInt withInteger = JSONUnmarshaller.unmarshall(source, WithInt.class);
        assertEquals(555, withInteger.getPrimitive());
    }

    /**
     * shall stop unmarshalling after bean end
     */
    @Test
    public void testUnmarshallerStopsAfterObjectEnd() throws InvocationTargetException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // must be lenient
        source = new JsonReader(new StringReader("{one:239},{two:555}"));
        source.setLenient(true);

        WithTwoProperties bean = JSONUnmarshaller.unmarshall(source, WithTwoProperties.class);
        assertEquals(239, bean.getOne());
        assertEquals(0, bean.getTwo());
    }


    public static class WithTwoProperties {
        int one;
        int two;

        public int getOne() {
            return one;
        }

        public void setOne(int one) {
            this.one = one;
        }

        public int getTwo() {
            return two;
        }

        public void setTwo(int two) {
            this.two = two;
        }
    }


    /**
     * unmarshalling of JSON array shall produce list
     */
    @Test
    public void testUnmarshallingOfJsonArray() throws InvocationTargetException, IOException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        // must be lenient
        source = new JsonReader(new StringReader("[{one:239},{two:555}]"));
        source.setLenient(true);
        final List<WithTwoProperties> list = JSONUnmarshaller.unmarshallArray(source, WithTwoProperties.class);

        assertEquals(2, list.size());
        assertEquals(239, list.get(0).getOne());
        assertEquals(555, list.get(1).getTwo());
    }

    @Test
    public void testUnmarshallingPrimitiveCharacter() throws InvocationTargetException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        source = new JsonReader(new StringReader("{foo:'a'}"));
        source.setLenient(true);

        final WithPrimitiveChar withPrimitiveChar = JSONUnmarshaller.unmarshall(source, WithPrimitiveChar.class);
        System.err.println("set:" +  withPrimitiveChar.getFoo());
        assertEquals((char)'a', (char)withPrimitiveChar.getFoo());
    }


    public static class WithPrimitiveChar {
        char foo = 'x';

        public char getFoo() {
            return foo;
        }

        public void setFoo(char foo) {
            this.foo = foo;
        }
    }

    @Test
    public void testUnmarshallingObjectCharacter() throws InvocationTargetException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        source = new JsonReader(new StringReader("{foo:'a'}"));
        source.setLenient(true);

        final WithObjectChar withObjectChar = JSONUnmarshaller.unmarshall(source, WithObjectChar.class);
        assertEquals(new Character('a'), withObjectChar.getFoo());
    }


    public static class WithObjectChar {
        Character foo;

        public Character getFoo() {
            return foo;
        }

        public void setFoo(Character foo) {
            this.foo = foo;
        }
    }

}
