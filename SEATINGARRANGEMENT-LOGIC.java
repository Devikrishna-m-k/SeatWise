 import java.util.*;

// 🔹 Custom Exception
class AllocationException extends Exception {
    public AllocationException(String message) {
        super(message);
    }
}

// 🔹 Student Class
class Student {
    String rollNo;
    String name;
    String branch;

    Student(String rollNo, String name, String branch) {
        this.rollNo = rollNo;
        this.name = name;
        this.branch = branch;
    }

    @Override
    public String toString() {
        return rollNo + " - " + name + " (" + branch + ")";
    }
}

// 🔹 Room Class
class Room {
    String roomNo;
    int capacity;
    List<Student> allocated = new ArrayList<>();

    Room(String roomNo, int capacity) {
        this.roomNo = roomNo;
        this.capacity = capacity;
    }

    boolean hasSpace() {
        return allocated.size() < capacity;
    }

    void addStudent(Student s) {
        allocated.add(s);
    }

    @Override
    public String toString() {
        return "Room " + roomNo + " (" + capacity + " seats)";
    }
}

// 🔹 Exam Slot Class
class ExamSlot {
    String slotId;
    String subject;
    String date;

    ExamSlot(String slotId, String subject, String date) {
        this.slotId = slotId;
        this.subject = subject;
        this.date = date;
    }

    @Override
    public String toString() {
        return "Slot " + slotId + " : " + subject + " (" + date + ")";
    }
}

// 🔹 Main Class
public class AllocationExceptionCode {
    public static void main(String[] args) {
        try {
            // Example data (Replace with DB read if needed)
            List<Student> students = Arrays.asList(
                new Student("S01", "Alice", "CSE"),
                new Student("S02", "Bob", "ECE"),
                new Student("S03", "Charlie", "EEE"),
                new Student("S04", "David", "CSE"),
                new Student("S05", "Eva", "ECE"),
                new Student("S06", "Frank", "MECH"),
                new Student("S07", "Grace", "CSE")
            );

            List<Room> normalRooms = Arrays.asList(
                new Room("R1", 3),
                new Room("R2", 3)
            );

            List<Room> backupRooms = Arrays.asList(
                new Room("B1", 2),
                new Room("B2", 2)
            );

            List<ExamSlot> slots = Arrays.asList(
                new ExamSlot("E1", "Maths", "2025-09-23"),
                new ExamSlot("E2", "Physics", "2025-09-24")
            );

            // 🔹 Allocate Students
            allocateStudents(students, normalRooms, backupRooms);

            // 🔹 Print Final Allocation
            System.out.println("✅ Exam Slots:");
            for (ExamSlot s : slots) {
                System.out.println(s);
            }

            System.out.println("\n✅ Seating Arrangement:");
            for (Room r : normalRooms) {
                if (!r.allocated.isEmpty()) {
                    System.out.println(r + " → " + r.allocated);
                }
            }
            for (Room r : backupRooms) {
                if (!r.allocated.isEmpty()) {
                    System.out.println(r + " → " + r.allocated);
                }
            }

        } catch (AllocationException e) {
            System.out.println("❌ Error in allocation: " + e.getMessage());
        }
    }

    // 🔹 Allocation Logic
    public static void allocateStudents(List<Student> students, List<Room> normalRooms, List<Room> backupRooms)
            throws AllocationException {

        Queue<Student> queue = new LinkedList<>(students);

        // Try to fill normal rooms first
        for (Room room : normalRooms) {
            while (room.hasSpace() && !queue.isEmpty()) {
                Student student = queue.poll();

                // Rule: No 2 adjacent students from same branch
                if (!room.allocated.isEmpty()) {
                    Student last = room.allocated.get(room.allocated.size() - 1);
                    if (last.branch.equals(student.branch)) {
                        throw new AllocationException("Branch conflict in " + room.roomNo);
                    }
                }
                room.addStudent(student);
            }
        }

        // If students remain → try backup rooms
        for (Room room : backupRooms) {
            while (room.hasSpace() && !queue.isEmpty()) {
                Student student = queue.poll();

                if (!room.allocated.isEmpty()) {
                    Student last = room.allocated.get(room.allocated.size() - 1);
                    if (last.branch.equals(student.branch)) {
                        throw new AllocationException("Branch conflict in backup " + room.roomNo);
                    }
                }
                room.addStudent(student);
            }
        }

        // If still students remain → error
        if (!queue.isEmpty()) {
            throw new AllocationException("Not enough rooms, backup also full!");
        }
    }
}
