package de.pribluda.android.jsonmarshaller;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.assertEquals;
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
}
