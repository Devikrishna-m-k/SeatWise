

public class AllocationRecord {
    private String studentId;
    private String studentName;
    private String branch;
    private String roomId;
    private int seatNo;
    private String examSlotId;

    public AllocationRecord(String studentId, String studentName, String branch,
                            String roomId, int seatNo, String examSlotId) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.branch = branch;
        this.roomId = roomId;
        this.seatNo = seatNo;
        this.examSlotId = examSlotId;
    }

    // getters
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getBranch() { return branch; }
    public String getRoomId() { return roomId; }
    public int getSeatNo() { return seatNo; }
    public String getExamSlotId() { return examSlotId; }

    @Override
    public String toString() {
        return studentId + " | " + studentName + " | " + branch + " | " + roomId + " | " + seatNo + " | " + examSlotId;
    }
}

