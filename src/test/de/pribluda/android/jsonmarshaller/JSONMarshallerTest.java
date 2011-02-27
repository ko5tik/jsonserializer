package de.pribluda.android.jsonmarshaller;


import com.google.gson.stream.JsonWriter;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * test proper functionality of json marshalling
 */
public class JSONMarshallerTest {
    @Mocked
    JsonWriter writer;

    /**
     * test that getter name is properly converted to property name
     */
    @Test
    public void testGetterPropertisation() {
        assertEquals("Foo", JSONMarshaller.propertize("getFoo"));
        assertEquals("fooBar", JSONMarshaller.propertize("getfooBar"));
        assertEquals("F", JSONMarshaller.propertize("getF"));
        assertEquals("Foo", JSONMarshaller.propertize("isFoo"));
    }

    /**
     * bad getters shall be ignored altogether, we consider getters not following
     * jaba beans patter as "bad"
     */
    @Test
    public void testBadGettersAreNotCalled() throws Exception {
        new JSONMarshaller().marshall(writer, new BadGeters());
    }

    /**
     * inner class hosting getters which shall not acceptable
     */
    public static class BadGeters {
        public String get() {
            fail("called get()");
            return "foo";
        }


        public String getBlam(String glum) {
            fail("called parametriyed method");
            return "qq";
        }

        public void getVrum() {
            fail("called void method");
        }

        private String getGrumps() {
            fail("called non public getter");
            return "";
        }


    }

    /**
     * ugly bean without default constructor
     */
    public static class NotABean {
        private NotABean() {

        }
    }

    @Test
    public void testThatNotABeanComesOutAsNull() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        new Expectations() {
            {
                writer.beginObject();
                writer.name("NotABean");
                writer.nullValue();
                writer.endObject();
            }
        };
        new JSONMarshaller().marshall(writer, new WithNotABean());
    }

    public class WithNotABean {
        public NotABean getNotABean() {
            return new NotABean();
        }
    }

    @Test
    public void testThatNullComesOutAsNull() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        new Expectations() {
            {
                writer.beginObject();
                writer.name("NullBean");
                writer.nullValue();
                writer.endObject();
            }
        };
        new JSONMarshaller().marshall(writer, new WithNullBean());
    }

    public class WithNullBean {
        public GoodPrimitiveGetter getNullBean() {
            return null;
        }
    }

    /**
     * class containing good kosher getters
     */
    public static class GoodPrimitiveGetter {
        public String getFoo() {
            return "foo";
        }
    }


    @Test
    public void testGoodPrimitiveGetterIsRetrieved() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {

        new Expectations() {
            {
                writer.beginObject();
                writer.name("Foo");
                writer.value("foo");
                writer.endObject();
            }
        };
        new JSONMarshaller().marshall(writer, new GoodPrimitiveGetter());


    }


    /**
     * bean shall be inserted as nested JSON object.
     * nested JSON object shall be populated from bean
     */
    @Test
    public void testBeanIsFollowed() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {
                writer.beginObject();
                writer.name("Primitives");

                writer.beginObject();
                writer.name("Foo");
                writer.value("foo");
                writer.endObject();

                writer.endObject();
            }
        };

        (new JSONMarshaller()).marshall(writer, new WithBean());


    }

    public static class WithBean {
        public GoodPrimitiveGetter getPrimitives() {
            return new GoodPrimitiveGetter();
        }
    }


    /**
     * string shall be treated as primitive
     */
    @Test
    public void testThatStringIsTreatedAsPrimitive() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {

                writer.beginObject();
                writer.name("Foo");
                writer.value("foo");
                writer.endObject();
            }
        };
        (new JSONMarshaller()).marshall(writer, new GoodPrimitiveGetter());

    }


    /**
     * shall marshall primitive array in proper way
     */
    @Test
    public void testThatSingleDimensionalPrimitiveArrayIsMarshalledProperly() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {

                writer.beginArray();

                writer.value((Number)1);
                writer.value((Number)2);
                writer.value((Number)3);

                writer.endArray();
            }
        };
        JSONMarshaller.marshallArray(writer, singleDimension);
    }

    static int[] singleDimension = new int[]{1, 2, 3};

    /**
     * strings are also primitives, also ensure they are marshalled to array properly
     */
    @Test
    public void testThatStringArrayIsMarshalledProperly() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {

                writer.beginArray();

                writer.value("foo");
                writer.value("bar");
                writer.value("baz");

                writer.endArray();
            }
        };


        JSONMarshaller.marshallArray(writer, singleDimensionString);
    }

    String[] singleDimensionString = new String[]{"foo", "bar", "baz"};


    /**
     * multidimensional array must be processed recursively
     */
    @Test
    public void testThatMultidimensionalArrayIsProcessedRecursively() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {

                writer.beginArray();
                writer.beginArray();
                writer.value((Number)1);
                writer.value((Number)2);
                writer.value((Number)3);
                writer.endArray();

                writer.beginArray();
                writer.value((Number)4);
                writer.value((Number)5);
                writer.value((Number)6);
                writer.endArray();

                writer.endArray();
            }
        };


        JSONMarshaller.marshallArray(writer, multiDimension);
    }


    int[][] multiDimension = new int[][]{{1, 2, 3}, {4, 5, 6}};


    /**
     * array of beans shall be masrhalled
     */
    @Test
    public void testBeanArrayIsMarshalled() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {

                writer.beginArray();
                writer.beginObject();
                writer.name("Foo");
                writer.value("foo");
                writer.endObject();
                writer.endArray();


            }
        };

        JSONMarshaller.marshallArray(writer, goodBeans);
    }

    GoodPrimitiveGetter[] goodBeans = new GoodPrimitiveGetter[]{new GoodPrimitiveGetter()};


    /**
     * bad types come out as nulls, also in array
     */
    @Test
    public void testThatArrayOfBadTypeContainesNulls() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {
                writer.beginArray();
                writer.nullValue();
                writer.endArray();

            }};
        JSONMarshaller.marshallArray(writer, badBeans);
    }

    NotABean[] badBeans = new NotABean[]{new NotABean()};


    /**
     * array propeprty shall be followed and serialized
     */
    @Test
    public void testArrayPropertyIsSerialized() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {
                writer.beginObject();
                writer.name("IntArray");
                writer.beginArray();
                writer.value((Number)1);
                writer.value((Number)2);
                writer.value((Number)3);
                writer.endArray();
                writer.endObject();

            }
        };
        JSONMarshaller.marshall(writer, new WithArray());
    }

    public static class WithArray {
        public int[] getIntArray() {
            return singleDimension;
        }
    }

    /**
     * primitive boolean with is-getter shall be marshalled
     */
    @Test
    public void testThatPrimitiveBooleanWithIsIsMarshalled() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {
                writer.beginObject();
                writer.name("Bool");
                writer.value(true);
                writer.endObject();

            }};
        (new JSONMarshaller()).marshall(writer, new WithPrimitiveBoolean());

    }


    public static class WithPrimitiveBoolean {

        public boolean isBool() {
            return true;
        }
    }


    /**
     * primitive boolean with is-getter shall be marshalled
     */
    @Test
    public void testThatPrimitiveBooleanWithGetIsMarshalled() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {
                writer.beginObject();
                writer.name("Bool");
                writer.value(true);
                writer.endObject();

            }};
        (new JSONMarshaller()).marshall(writer, new WithPrimitiveGetBoolean());

    }

    public static class WithPrimitiveGetBoolean {

        public boolean getBool() {
            return true;
        }
    }

    /**
     * inherited entities shall be taken into account
     */
    @Test
    public void testThatInheritedPropertiesAreUsed() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        new Expectations() {
            {
                writer.beginObject();
                writer.name("Bool");
                writer.value(true);
                writer.endObject();

            }};
        (new JSONMarshaller()).marshall(writer, new Derived());

    }

    public static class Derived extends WithPrimitiveGetBoolean {

    }
}
