package org.mobicents.slee.resource.sip11.test;

import javax.slee.facilities.Tracer;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.ConfigProperties.Property;
import javax.slee.resource.ResourceAdaptorContext;

import org.mobicents.slee.resource.sip11.SipResourceAdaptor;
import org.mobicents.slee.resource.sip11.SleeSipProviderImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.BeforeClass;

import org.powermock.reflect.Whitebox;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import org.mockito.stubbing.*;
import org.mockito.invocation.*;
import java.util.Arrays;

public class SipResourceAdaptorTest {

	// common mocked tracer
	static Tracer tracer1 = mock(Tracer.class);

	// resource adaptor context
	static ResourceAdaptorContext raContext = mock(ResourceAdaptorContext.class);

	// slee sip provider mock
	static SleeSipProviderImpl sleeSipProvider = mock(SleeSipProviderImpl.class);

	// minimal mock just to satisfy SipResourceAdaptor's usage of the tracer
	private static void mockTracer() {
		// mock info() call for tracker
		Answer<Object> answer = new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				System.out.println(" (TRACER-MOCK) :: info() called :: " + Arrays.toString(invocation.getArguments()));
				return null;
			}
		};
		doAnswer(answer).when(tracer1).info(anyString());

	}

	// minimal mock just to satisfy SipResourceAdaptor's usage of the tracer
	private static void mockRaContext() {
		when(raContext.getEntityName()).thenReturn("raContextMock");
	}

	@BeforeClass
	public static void prepareEnvSharedByAllTests() {
		mockTracer();
		mockRaContext();
	}

	@Test
	/*
	 * Purpose of this test it to verify whether two RA can be activated on the
	 * same network interface within same class loader
	 */
	public void activateTwoRa() {
		// create two RAs
		SipResourceAdaptor ra1 = new SipResourceAdaptor();
		SipResourceAdaptor ra2 = new SipResourceAdaptor();

		// initialize internal dependencies of RA#1
		Whitebox.setInternalState(ra1, "tracer", tracer1);
		Whitebox.setInternalState(ra1, "raContext", raContext);
		Whitebox.setInternalState(ra1, "providerWrapper", sleeSipProvider);

		// initialize internal dependencies of RA#2
		Whitebox.setInternalState(ra2, "tracer", tracer1);
		Whitebox.setInternalState(ra2, "raContext", raContext);
		Whitebox.setInternalState(ra2, "providerWrapper", sleeSipProvider);

		// prepare configuration for RA#1, RA#2
		ConfigProperties configRa1 = new ConfigProperties();
		ConfigProperties configRa2;

		configRa1.addProperty(new Property("javax.sip.TRANSPORT", "java.lang.String", "TCP"));
		configRa1.addProperty(new Property("javax.sip.IP_ADDRESS", "java.lang.String", "127.0.0.1"));
		configRa1.addProperty(new Property("org.mobicents.ha.javax.sip.BALANCERS", "java.lang.String", ""));
		configRa1.addProperty(new Property("org.mobicents.ha.javax.sip.LoadBalancerHeartBeatingServiceClassName",
				"java.lang.String", "org.mobicents.ha.javax.sip.LoadBalancerHeartBeatingServiceImpl"));
		configRa1.addProperty(new Property("org.mobicents.ha.javax.sip.LoadBalancerElector", "java.lang.String",
				"org.mobicents.ha.javax.sip.RoundRobinLoadBalancerElector"));
		configRa1.addProperty(new Property("org.mobicents.ha.javax.sip.CACHE_CLASS_NAME", "java.lang.String",
				"org.mobicents.ha.javax.sip.cache.NoCache"));

		// clone common properties
		configRa2 = (ConfigProperties) configRa1.clone();

		// RA#1 custom configuration
		configRa1.addProperty(new Property("javax.sip.PORT", "java.lang.Integer", 5060));
		configRa1.addProperty(new Property("javax.sip.STACK_NAME", "java.lang.String", "stack#1"));

		// RA#2 custom configuration
		configRa2.addProperty(new Property("javax.sip.PORT", "java.lang.Integer", 5059));
		configRa2.addProperty(new Property("javax.sip.STACK_NAME", "java.lang.String", "stack#2"));

		// configure adaptors
		ra1.raConfigure(configRa1);
		Assert.assertEquals(5060, Whitebox.getInternalState(ra1, "port"));
		Assert.assertEquals("127.0.0.1", Whitebox.getInternalState(ra1, "stackAddress"));

		ra2.raConfigure(configRa2);
		Assert.assertEquals(5059, Whitebox.getInternalState(ra2, "port"));
		Assert.assertEquals("127.0.0.1", Whitebox.getInternalState(ra2, "stackAddress"));

		ra1.raActive();
		ra2.raActive();
	}
}
