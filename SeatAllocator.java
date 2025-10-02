import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class SeatAllocator {

    /**
     * Allocate seats for a given exam slot.
     * Returns a list of admin warnings (if any). If allocation fails completely, throws SQLException or returns false by result.
     */
    public static class AllocationResult {
        public final boolean success;
        public final List<String> warnings;
        public final List<AllocationRecord> allocations;

        public AllocationResult(boolean success, List<String> warnings, List<AllocationRecord> allocations) {
            this.success = success;
            this.warnings = warnings;
            this.allocations = allocations;
        }
    }

    /**
     * Main allocation method.
     * It uses DB data (students, rooms) and saves allocations into Allocation table.
     */
    public AllocationResult allocateSeatsForExam(String examSlotId) {
        List<String> warnings = new ArrayList<>();
        List<AllocationRecord> finalAllocations = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            // 1. fetch students (for this slot) - here we assume all students take examSlot; adapt if student-exam mapping exists
            List<StudentInfo> students = fetchAllStudents(conn); // fetch all students; adapt to slot filter if needed

            // 2. fetch rooms with backup flag
            List<RoomInfo> allRooms = fetchAllRooms(conn);
            List<RoomInfo> normalRooms = allRooms.stream().filter(r -> !r.isBackup).collect(Collectors.toList());
            List<RoomInfo> backupRooms = allRooms.stream().filter(r -> r.isBackup).collect(Collectors.toList());

            int totalCapacity = normalRooms.stream().mapToInt(r -> r.capacity).sum();
            int totalStudents = students.size();

            // If overflow with normal rooms, use backup rooms
            if (totalStudents > totalCapacity) {
                int backupCapacity = backupRooms.stream().mapToInt(r -> r.capacity).sum();
                if (totalStudents > (totalCapacity + backupCapacity)) {
                    // not enough total capacity even with backup -> fail
                    String msg = "Total students (" + totalStudents + ") exceed total capacity (" + (totalCapacity + backupCapacity) + ").";
                    warnings.add(msg);
                    // save admin warning
                    AllocationDAO.saveAdminWarning(conn, msg);
                    conn.rollback();
                    return new AllocationResult(false, warnings, finalAllocations);
                } else {
                    // use backup rooms (move them into rooms list)
                    normalRooms.addAll(backupRooms);
                    warnings.add("Normal rooms insufficient; backup rooms were included for allocation.");
                    AllocationDAO.saveAdminWarning(conn, "Backup rooms used for exam slot " + examSlotId);
                }
            }

            // Recompute capacity
            int capacity = normalRooms.stream().mapToInt(r -> r.capacity).sum();
            if (capacity == 0) {
                String msg = "No rooms available for allocation.";
                warnings.add(msg);
                AllocationDAO.saveAdminWarning(conn, msg);
                conn.rollback();
                return new AllocationResult(false, warnings, finalAllocations);
            }

            // 3. analyze branch distribution
            Map<String, List<StudentInfo>> byBranch = students.stream()
                    .collect(Collectors.groupingBy(s -> s.branch));

            // if one branch hugely dominates, detection threshold:
            int maxBranchCount = byBranch.values().stream().mapToInt(List::size).max().orElse(0);
            if (maxBranchCount > (students.size() / 2 + 1)) {
                // imbalance: cannot fully prevent adjacency. Use relaxed allocation via greedy reordering
                warnings.add("Branch imbalance detected. Relaxed allocation strategy applied to minimize same-branch adjacency.");
                AllocationDAO.saveAdminWarning(conn, "Branch imbalance for exam " + examSlotId);
            }

            // 4. create an ordered list of student IDs using "reorganize" greedy algorithm by branch
            List<StudentInfo> orderedStudents = reorganizeByBranch(byBranch);

            // 5. allocate orderedStudents into rooms sequentially while attempting to avoid adjacency inside rooms
            List<AllocationRecord> allocations = new ArrayList<>();
            int idx = 0;
            for (RoomInfo room : normalRooms) {
                for (int seat = 1; seat <= room.capacity && idx < orderedStudents.size(); seat++) {
                    StudentInfo s = orderedStudents.get(idx++);
                    allocations.add(new AllocationRecord(s.studentId, s.name, s.branch, room.roomId, seat, examSlotId));
                }
                if (idx >= orderedStudents.size()) break;
            }

            // 6. Post-check: compute adjacency violations inside each room and attempt small swaps to reduce (best-effort)
            int violations = reduceAdjacentSameBranch(allocations);
            if (violations > 0) {
                warnings.add("Allocation resulted in " + violations + " same-branch adjacent pairs (could not be avoided).");
                AllocationDAO.saveAdminWarning(conn, "Allocation had " + violations + " adjacency violations for " + examSlotId);
            }

            // 7. Save allocations to DB (clear old for slot then batch insert)
            AllocationDAO.clearAllocationsForExam(conn, examSlotId);
            AllocationDAO.saveAllocationBatch(conn, allocations);

            conn.commit();
            return new AllocationResult(true, warnings, allocations);

        } catch (SQLException ex) {
            ex.printStackTrace();
            warnings.add("Database error during allocation: " + ex.getMessage());
            return new AllocationResult(false, warnings, finalAllocations);
        }
    }

    // ------------------- Helper & inner classes -------------------

    private static class StudentInfo {
        String studentId;
        String name;
        String branch;
        StudentInfo(String id, String name, String branch) { this.studentId = id; this.name = name; this.branch = branch;}
    }

    private static class RoomInfo {
        String roomId;
        int capacity;
        boolean isBackup;
        RoomInfo(String id, int cap, boolean b) { this.roomId = id; this.capacity = cap; this.isBackup = b;}
    }

    private List<StudentInfo> fetchAllStudents(Connection conn) throws SQLException {
        List<StudentInfo> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT student_id, name, branch FROM Student");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new StudentInfo(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        }
        return list;
    }

    private List<RoomInfo> fetchAllRooms(Connection conn) throws SQLException {
        List<RoomInfo> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT room_id, capacity, is_backup FROM Room ORDER BY is_backup, room_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new RoomInfo(rs.getString(1), rs.getInt(2), rs.getInt(3) == 1));
            }
        }
        return list;
    }

    /**
     * Reorganize students by branch to minimize adjacency:
     * Uses greedy approach: maintain a max-heap of (branch,count), pop the top two branches,
     * append one student from first, decrease count, push back if remains.
     * This is similar to reorganize-string algorithm.
     */
    private List<StudentInfo> reorganizeByBranch(Map<String, List<StudentInfo>> byBranch) {
        List<StudentInfo> result = new ArrayList<>();
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
                (a,b) -> b.getValue() - a.getValue()
        );

        // Map from branch to queue of students (so we can pop an actual StudentInfo)
        Map<String, Queue<StudentInfo>> branchQueues = new HashMap<>();
        for (Map.Entry<String, List<StudentInfo>> e : byBranch.entrySet()) {
            pq.add(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().size()));
            branchQueues.put(e.getKey(), new LinkedList<>(e.getValue()));
        }

        while (!pq.isEmpty()) {
            Map.Entry<String,Integer> first = pq.poll();
            String branch1 = first.getKey();
            int count1 = first.getValue();

            Map.Entry<String,Integer> second = null;
            if (!pq.isEmpty()) {
                second = pq.poll();
            }

            // pick from first
            Queue<StudentInfo> q1 = branchQueues.get(branch1);
            if (q1 != null && !q1.isEmpty()) result.add(q1.poll());

            if (count1 - 1 > 0) {
                // will reinsert later with decreased count
                first = new AbstractMap.SimpleEntry<>(branch1, count1 - 1);
            } else first = null;

            if (second != null) {
                String branch2 = second.getKey();
                Queue<StudentInfo> q2 = branchQueues.get(branch2);
                if (q2 != null && !q2.isEmpty()) result.add(q2.poll());
                if (second.getValue() - 1 > 0) {
                    second = new AbstractMap.SimpleEntry<>(branch2, second.getValue() - 1);
                } else second = null;
            }

            if (first != null) pq.add(first);
            if (second != null) pq.add(second);
        }

        // If any student left in any queue (shouldn't happen), append them
        for (Queue<StudentInfo> q : branchQueues.values()) while (!q.isEmpty()) result.add(q.poll());

        return result;
    }

    /**
     * Try to reduce adjacent same-branch pairs by swapping with seats in other rooms (best-effort).
     * Returns the count of remaining adjacency violations.
     */
    private int reduceAdjacentSameBranch(List<AllocationRecord> allocations) {
        // Group allocations by room
        Map<String, List<AllocationRecord>> byRoom = new LinkedHashMap<>();
        for (AllocationRecord a : allocations) {
            byRoom.computeIfAbsent(a.getRoomId(), k -> new ArrayList<>()).add(a);
        }

        int violations = 0;
        // For each room, count adjacency violations and try local swaps
        for (List<AllocationRecord> seats : byRoom.values()) {
            // seats are ordered by seatNo already when created
            for (int i = 1; i < seats.size(); i++) {
                if (seats.get(i).getBranch().equals(seats.get(i-1).getBranch())) {
                    // try to find a seat in a later room which has a student of a different branch to swap
                    boolean fixed = false;
                    for (Map.Entry<String, List<AllocationRecord>> entry : byRoom.entrySet()) {
                        List<AllocationRecord> otherSeats = entry.getValue();
                        if (otherSeats == seats) continue;
                        for (int j = 0; j < otherSeats.size(); j++) {
                            if (!otherSeats.get(j).getBranch().equals(seats.get(i).getBranch())
                                && !otherSeats.get(j).getBranch().equals(seats.get(i-1).getBranch())) {
                                // swap
                                AllocationRecord tmp = otherSeats.get(j);
                                otherSeats.set(j, seats.get(i));
                                seats.set(i, tmp);
                                fixed = true;
                                break;
                            }
                        }
                        if (fixed) break;
                    }
                    if (!fixed) violations++;
                }
            }
        }
        // Flatten back into allocations list in the same room order and seatNo order
        List<AllocationRecord> merged = new ArrayList<>();
        for (Map.Entry<String, List<AllocationRecord>> entry : byRoom.entrySet()) {
            List<AllocationRecord> seatList = entry.getValue();
            // reassign seat numbers sequentially within room
            for (int s=0; s<seatList.size(); s++) {
                AllocationRecord rec = seatList.get(s);
                merged.add(new AllocationRecord(rec.getStudentId(), rec.getStudentName(), rec.getBranch(),
                        rec.getRoomId(), s+1, rec.getExamSlotId()));
            }
        }
        allocations.clear();
        allocations.addAll(merged);
        return violations;
    }

    // ------------------- Report helpers -------------------

    public List<AllocationRecord> generateFullReport() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return AllocationDAO.fetchAllAllocations(conn);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public AllocationRecord generateStudentReport(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return AllocationDAO.fetchAllocationForStudent(conn, studentId);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

