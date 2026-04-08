package com.kowshik.generics;

import java.util.ArrayList;
import java.util.List;

class Animal {

}

class Dog extends Animal {

}

class Labrador extends Dog {

}

class Rottweiler extends Dog {

}

class Cat extends Animal {

}

class PersianCat extends Cat {

}

class MexicanCat extends Cat {

}

public class PECSDemo {
    public static void main(String[] args) {
        // Allow all classes of type Dog and subtypes of Dog.
        List<? super Dog> listOfDogs = new ArrayList<Dog>();
        listOfDogs.add(new Labrador());
        listOfDogs.add(new Rottweiler());
        listOfDogs.add(new Dog());

        // You can't do this, it's read only.
        // List<? extends Dog> listDogsExtending = new ArrayList<Dog>();
        // listDogsExtending.add(new Labrador());
        // listDogsExtending.add(new Rottweiler());
        // listDogsExtending.add(new Dog());
        List<Dog> dogsList = new ArrayList<>() {
            {
                add(new Labrador());
                add(new Rottweiler());
            }
        };

        printDogs(dogsList);
    }

    // This is the way to use ? extends Dog.
    private static void printDogs(List<? extends Dog> dogsList) {
        for (Dog d : dogsList) {
            System.out.println(d.toString());
        }
    }
}
