package com.kowshik.generics;

public class GenericExperimentDemo {
    public static void main(String[] args) {
        GenericExperiment<String> stringExperiment = new GenericExperiment<>("Hello", 1);
        GenericExperiment<Integer> integerExperiment = new GenericExperiment<>(1, 2);

        System.out.println(stringExperiment.getContent());
        System.out.println(integerExperiment.getContent());
    }
}
