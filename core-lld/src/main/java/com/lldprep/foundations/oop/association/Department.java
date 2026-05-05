// ASSOCIATION: Department USES a Teacher via constructor injection.
// Department does NOT own Teacher — Teacher is created outside and passed in.
// Teacher survives even after Department is destroyed (no lifecycle dependency).
// Relationship: Department ----> Teacher  (uses-a, bidirectional possible)
package com.lldprep.foundations.oop.association;

public class Department {

    private final String name;
    private final Teacher teacher;   // association — injected, not created here

    public Department(String name, Teacher teacher) {
        this.name = name;
        this.teacher = teacher;
    }

    public void conductClass() {
        System.out.println("Department [" + name + "] conducting class:");
        teacher.teach();
    }
}
