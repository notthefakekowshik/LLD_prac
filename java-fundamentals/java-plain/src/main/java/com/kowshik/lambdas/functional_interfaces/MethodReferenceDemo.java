package com.kowshik.lambdas.functional_interfaces;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Method References (:: operator)
 * A shorthand for lambdas that simply call an existing method.
 * There are 4 kinds:
 *
 * 1. Static Method Reference       -> ClassName::staticMethod
 * 2. Instance Method (bound)       -> instance::method          (specific object)
 * 3. Instance Method (unbound)     -> ClassName::instanceMethod (called on the lambda parameter)
 * 4. Constructor Reference         -> ClassName::new
 */
public class MethodReferenceDemo {

    static class User {
        private final String name;

        User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        // Used for static method reference
        public static String toUpperCase(String s) {
            return s.toUpperCase();
        }

        @Override
        public String toString() {
            return "User{name='" + name + "'}";
        }
    }

    public static void main(String[] args) {

        List<String> names = List.of("alice", "bob", "charlie");

        // 1. Static Method Reference: ClassName::staticMethod
        // Equivalent lambda: s -> User.toUpperCase(s)
        Function<String, String> toUpper = User::toUpperCase;
        List<String> upperNames = names.stream()
                .map(toUpper)
                .collect(Collectors.toList());
        System.out.println("1. Static ref -> " + upperNames);

        // 2. Bound Instance Method Reference: instance::method
        // The specific object is "captured" — the lambda always calls on THIS instance.
        // Equivalent lambda: s -> prefix.concat(s)
        String prefix = "Hello, ";
        Function<String, String> greeter = prefix::concat;
        List<String> greetings = names.stream()
                .map(greeter)
                .collect(Collectors.toList());
        System.out.println("2. Bound instance ref -> " + greetings);

        // 3. Unbound Instance Method Reference: ClassName::instanceMethod
        // The lambda parameter becomes the receiver of the method call.
        // Equivalent lambda: s -> s.toUpperCase()
        Function<String, String> upperCaseFn = String::toUpperCase;
        System.out.println("3. Unbound instance ref -> " + upperCaseFn.apply("kowshik"));

        // Unbound with a method that takes an argument -> needs BiFunction
        // Equivalent lambda: (s, n) -> s.substring(n)
        BiFunction<String, Integer, String> substringFn = String::substring;
        System.out.println("3. Unbound with arg -> " + substringFn.apply("kowshik", 3));

        // 4. Constructor Reference: ClassName::new
        // Equivalent lambda: name -> new User(name)
        Function<String, User> userFactory = User::new;
        List<User> users = names.stream()
                .map(userFactory)
                .collect(Collectors.toList());
        System.out.println("4. Constructor ref -> " + users);

        // Supplier as constructor ref (no-arg constructor isn't defined here,
        // but showing the concept with StringBuilder)
        Supplier<StringBuilder> sbFactory = StringBuilder::new;
        StringBuilder sb = sbFactory.get();
        sb.append("built via constructor ref");
        System.out.println("4. No-arg constructor ref -> " + sb);
    }
}
