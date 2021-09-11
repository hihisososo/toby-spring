package springbook.learningtest.proxy;

import java.util.Locale;

public class HelloUpperCase implements Hello{
    Hello hello;

    public HelloUpperCase(Hello hello) {
        this.hello = hello;
    }

    @Override
    public String sayHello(String name) {
        return hello.sayHello(name).toUpperCase();
    }

    @Override
    public String sayHi(String name) {
        return hello.sayHi(name).toUpperCase();
    }

    @Override
    public String sayThankYou(String name) {
        return hello.sayHi(name).toUpperCase();
    }
}
