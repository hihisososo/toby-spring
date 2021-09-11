package springbook.learningtest.proxy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProxyTest {
    @Test
    public void simpleProxy() {
        Hello hello = new HelloTarget();
        assertThat(hello.sayHello("Toby"), is("Hello Toby"));
        assertThat(hello.sayHi("Toby"), is("Hi Toby"));
        assertThat(hello.sayThankYou("Toby"), is("Thank You Toby"));
    }

    @Test
    public void test() {
        Hello proxiedHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Hello.class},
                new UpperCaseHandler(new HelloTarget()));
    }

}
