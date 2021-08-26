package springbook.learningtest.junit.template;

public interface LineCallback<T> {
    T doSomethisWithLine(String line, T value);
}
