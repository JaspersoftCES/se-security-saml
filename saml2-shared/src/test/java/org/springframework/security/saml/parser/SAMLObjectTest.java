/* Copyright 2009 Vladimir Sch�fer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.saml.parser;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.impl.ActionImpl;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.springframework.security.saml.SAMLTestBase;
import org.w3c.dom.Element;

import java.io.*;

/**
 * @author Vladimir Sch�fer
 */
public class SAMLObjectTest extends SAMLTestBase {

    SAMLObject<Assertion> assertionObject;
    Assertion assertion;

    @Before
    public void initializeValues() {
        assertion = ((SAMLObjectBuilder<Assertion>) SAMLTestBase.builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME)).buildObject();
        assertion.setID("testID");

        assertionObject = new SAMLObject<Assertion>(assertion);
    }

    /**
     * Verfies that the inner object is set correctly.
     */
    @Test
    public void testGetInnerObject() {
        assertEquals(assertion, assertionObject.getObject());
    }

    /**
     * Verfies that SAMLObject can't be creaed with null argument.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNoNullArgument() {
        new SAMLObject(null);
    }

    /**
     * Verifies that deserializaion fails is parserPool isn't set.
     *
     * @throws Exception error
     */
    @Test(expected = IOException.class)
    public void testMarshalWihoutPoolSet() throws Exception {
        new ParserPoolHolder(null);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(outStream);
        stream.writeObject(assertionObject);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());
        ObjectInputStream input = new ObjectInputStream(inputStream);
        input.readObject();
    }

    /**
     * Verifies that mashalling of object which doesn't have marhasller reigstered will fail.
     *
     * @throws Exception error
     */
    @Test(expected = MessageEncodingException.class)
    public void testMarshallObjectWithoutMashaller() throws Exception {
        TestObject to = new TestObject("xxx", "", "");
        SAMLObject<TestObject> tso = new SAMLObject<TestObject>(to);

        Configuration.getMarshallerFactory().deregisterMarshaller(to.getElementQName());
        tso.marshallMessage(to);
    }

    /**
     * Verifies that error during marshalling of object will be reported.
     *
     * @throws Exception error
     */
    @Test(expected = IOException.class)
    public void testMarshallingError() throws Exception {
        TestObject to = new TestObject("xxx", "", "");
        SAMLObject<TestObject> tso = new SAMLObject<TestObject>(to);

        Marshaller mock = createMock(Marshaller.class);
        Configuration.getMarshallerFactory().registerMarshaller(to.getElementQName(), mock);

        expect(mock.marshall(to)).andThrow(new MarshallingException("Error"));

        replay(mock);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(outStream);
        stream.writeObject(tso);
        verify(mock);
    }

    /**
     * Verifies that unmarshalling XML for which no unmarshaller is registered will fail with exception.
     *
     * @throws Exception error
     */
    @Test(expected = IOException.class)
    public void testNoUnmarshaller() throws Exception {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(outStream);
        stream.writeObject(assertionObject);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());
        ObjectInputStream input = new ObjectInputStream(inputStream);

        Unmarshaller old = Configuration.getUnmarshallerFactory().getUnmarshaller(assertion.getElementQName());

        try {
            Configuration.getUnmarshallerFactory().deregisterUnmarshaller(assertion.getElementQName());
            input.readObject();
        } finally {
            Configuration.getUnmarshallerFactory().registerUnmarshaller(assertion.getElementQName(), old);
        }
    }

    class TestObject extends ActionImpl {
        TestObject(String namespaceURI, String elementLocalName, String namespacePrefix) {
            super(namespaceURI, elementLocalName, namespacePrefix);
        }
    }

    /**
     * Verifies that errror during unmarshalling will be reported.
     *
     * @throws Exception error
     */
    @Test(expected = IOException.class)
    public void testWrongXMLInStream() throws Exception {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(outStream);
        stream.writeObject(assertionObject);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());
        ObjectInputStream input = new ObjectInputStream(inputStream);

        Unmarshaller mock = createMock(Unmarshaller.class);
        Unmarshaller old = Configuration.getUnmarshallerFactory().getUnmarshaller(assertion.getElementQName());
        Configuration.getUnmarshallerFactory().registerUnmarshaller(assertion.getElementQName(), mock);

        expect(mock.unmarshall((Element) notNull())).andThrow(new UnmarshallingException(""));

        try {
            replay(mock);
            input.readObject();
            verify(mock);
        } finally {
            Configuration.getUnmarshallerFactory().registerUnmarshaller(assertion.getElementQName(), old);
        }
    }

    /**
     * Verifies that the SAMLCredential can be serialized/deserialized correctly.
     *
     * @throws Exception error
     */
    @Test
    public void testSerialization() throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(outStream);
        stream.writeObject(assertionObject);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outStream.toByteArray());
        ObjectInputStream input = new ObjectInputStream(inputStream);
        SAMLObject<Assertion> desAssertion = (SAMLObject<Assertion>) input.readObject();

        assertEquals("testID", desAssertion.getObject().getID());

        // And for the second time, as we cacche some data
        outStream = new ByteArrayOutputStream();
        stream = new ObjectOutputStream(outStream);
        stream.writeObject(assertionObject);

        inputStream = new ByteArrayInputStream(outStream.toByteArray());
        input = new ObjectInputStream(inputStream);
        desAssertion = (SAMLObject<Assertion>) input.readObject();

        assertEquals("testID", desAssertion.getObject().getID());
    }

}