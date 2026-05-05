// ASSOCIATION: Teacher is an independent object.
// It exists on its own — no ownership by any Department.
// Multiple Departments can associate with the same Teacher.
package com.lldprep.foundations.oop.association;

public class Teacher {

    private final String name;
    private final String subject;

    public Teacher(String name, String subject) {
        this.name = name;
        this.subject = subject;
    }

    public String getName() { return name; }
    public String getSubject() { return subject; }

    public void teach() {
        System.out.println(name + " teaches " + subject);
    }
}
