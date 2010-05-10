package de.pribluda.android.jsonmarshaller;

import android.R;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * test proper functionality of json marshalling
 */
public class JSONMarshallerTest {
    @Mocked
    JSONObject jsonObject;

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
     * bad getters shall be ignored altogether
     */
    @Test
    public void testBadGettersAreNotCalled() throws Exception {
        new JSONMarshaller().marshall(new BadGeters());
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

        public NotABean getNotABean() {
            fail("called getter of not a bean class (without default constructor)");
            return null;
        }
    }

    /**
     * ugly bean without default constructor
     */
    public static class NotABean {
        private NotABean() {

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
    public void testGoodPrimitiveGetterIsRetrieved(@Mocked final GoodPrimitiveGetter gpg) throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {

        new JSONMarshaller().marshall(new GoodPrimitiveGetter());

        new Verifications() {
            {
                gpg.getFoo();
            }
        };
    }


    /**
     * bean shall be inserted as nested JSON object.
     * nested JSON object shall be populated from bean
     */
    @Test
    public void testBeanIsFollowed() throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {
        new Expectations() {
            {
                // create root json object
                new JSONObject();
                // create descendant object
                new JSONObject();
                // retrieve primitive bean (nothing to mock here)

                // marshall getter of  primitive nested bean
                jsonObject.put("Foo", "foo");
                // put nested object into parent
                jsonObject.put("Primitives", withAny(JSONObject.class));

            }
        };

        (new JSONMarshaller()).marshall(new WithBean());


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
    public void testThatStringIsTreatedAsPrimitive() throws JSONException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        (new JSONMarshaller()).marshall(new GoodPrimitiveGetter());
        new Verifications() {
            {
                jsonObject.put("Foo", "foo");
            }
        };

    }


    /**
     * shall marshall primitive array in proper way
     */
    @Test
    public void testThatSingleDimensionalPrimitiveArrayIsMarshalledProperly(@Mocked final JSONArray array) throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {

        new Expectations() {
            {
                // shall create JSON array
                new JSONArray();
                // and put values there
                array.put((Object) 1);
                array.put((Object) 2);
                array.put((Object) 3);
            }
        };
        assertNotNull(JSONMarshaller.marshallArray(singleDimension));
    }

    static int[] singleDimension = new int[]{1, 2, 3};

    /**
     * strings are also primitives, also ensure they are marshalled to array properly
     *
     * @param array
     */
    @Test
    public void testThatStringArrayIsMarshalledProperly(@Mocked final JSONArray array) throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {

        new Expectations() {
            {
                // shall create JSON array
                new JSONArray();
                // and put values there
                array.put("foo");
                array.put("bar");
                array.put("baz");
            }
        };
        assertNotNull(JSONMarshaller.marshallArray(singleDimensionString));
    }

    String[] singleDimensionString = new String[]{"foo", "bar", "baz"};


    /**
     * multidimensional array must be processed recursively
     */
    @Test
    public void testThatMultidimensionalArrayIsProcessedRecursively(@Mocked final JSONArray array) throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {
        new Expectations() {
            {
                JSONArray root = new JSONArray();
                JSONArray first = new JSONArray();
                first.put((Object) 1);
                first.put((Object) 2);
                first.put((Object) 3);
                root.put(first);
                JSONArray second = new JSONArray();
                second.put((Object) 4);
                second.put((Object) 5);
                second.put((Object) 6);
                root.put(second);
            }
        };

        assertNotNull(JSONMarshaller.marshallArray(multiDimension));
    }


    int[][] multiDimension = new int[][]{{1, 2, 3}, {4, 5, 6}};


    /**
     * array of beans shall be masrhalled
     */
    @Test
    public void testBeanArrayIsMarshalled(@Mocked final JSONArray array) throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {

        new Expectations() {
            {
                // shall create json array
                new JSONArray();
                // create and populate object out of bean
                new JSONObject();
                jsonObject.put("Foo", "foo");
                // and store it in resulting array
                array.put(withAny(JSONObject.class));
            }
        };
        assertNotNull(JSONMarshaller.marshallArray(goodBeans));
    }

    GoodPrimitiveGetter[] goodBeans = new GoodPrimitiveGetter[]{new GoodPrimitiveGetter()};


    /**
     * array of not suitable type shall come out empty
     * (TODO: this behaviour is discutable, whether this shall come out as null )
     */
    @Test
    public void testThatArrayOfBadTypeComesOutEmpty(@Mocked final JSONArray array) throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {
        new Expectations() {
            {
                // shall create json array
                new JSONArray();

            }};
        assertNotNull(JSONMarshaller.marshallArray(badBeans));
    }

    NotABean[] badBeans = new NotABean[]{new NotABean()};


    /**
     * array propeprty shall be followed and serialized
     */
    @Test
    public void testArrayPropertyIsSerialized(@Mocked final JSONArray array) throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {
        new Expectations() {
            {
                //  shall create JSON Object
                new JSONObject();

                // and serialize aeeay
                // shall create JSON array
                JSONArray first = new JSONArray();
                // and put values there
                first.put((Object) 1);
                first.put((Object) 2);
                first.put((Object) 3);

                jsonObject.put("IntArray", first);
            }
        };
        assertNotNull(JSONMarshaller.marshall(new WithArray()));
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
    public void testThatPrimitiveBooleanWithIsIsMarshalled() throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {

        (new JSONMarshaller()).marshall(new WithPrimitiveBoolean());
        new Verifications() {
            {
                jsonObject.put("Bool", (Object) true);
            }
        };
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
    public void testThatPrimitiveBooleanWithGetIsMarshalled() throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {

        (new JSONMarshaller()).marshall(new WithPrimitiveGetBoolean());
        new Verifications() {
            {
                jsonObject.put("Bool", (Object) true);
            }
        };
    }

    public static class WithPrimitiveGetBoolean {

        public boolean getBool() {
            return true;
        }
    }

    /**
     * primitive boolean with is-getter shall be marshalled
     */
    @Test
    public void testThatObjectBooleanWithGetIsMarshalled() throws InvocationTargetException, NoSuchMethodException, JSONException, IllegalAccessException {

        (new JSONMarshaller()).marshall(new WithPrimitiveGetBoolean());
        new Verifications() {
            {
                jsonObject.put("Bool", (Object) true);
            }
        };
    }

    public static class WithObjectGetBoolean {

        public Boolean getBool() {
            return true;
        }
    }
}
