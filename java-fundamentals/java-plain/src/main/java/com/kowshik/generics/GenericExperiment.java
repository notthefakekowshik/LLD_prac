package com.kowshik.generics;

public class GenericExperiment<T> {

    private T content;
    private int number;

    GenericExperiment(T content, int number) {
        this.content = content;
        this.number = number;
    }

    public T getContent() {
        return this.content;
    }

    public int getNumber() {
        return this.number;
    }

}
