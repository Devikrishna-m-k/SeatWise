import javax.swing.*;

public class AdminDashboardFrame extends JFrame {
    private JButton uploadStudentsButton, uploadRoomsButton, uploadExamSlotsButton, allocateSeatsButton, viewReportButton;

    public AdminDashboardFrame() {
        setTitle("Admin Dashboard");
        setSize(500, 400);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        uploadStudentsButton = new JButton("Upload Students");
        uploadRoomsButton = new JButton("Upload Rooms");
        uploadExamSlotsButton = new JButton("Upload Exam Slots");
        allocateSeatsButton = new JButton("Allocate Seats");
        viewReportButton = new JButton("View Reports");

        uploadStudentsButton.setBounds(150, 50, 200, 30);
        uploadRoomsButton.setBounds(150, 100, 200, 30);
        uploadExamSlotsButton.setBounds(150, 150, 200, 30);
        allocateSeatsButton.setBounds(150, 200, 200, 30);
        viewReportButton.setBounds(150, 250, 200, 30);

        add(uploadStudentsButton);
        add(uploadRoomsButton);
        add(uploadExamSlotsButton);
        add(allocateSeatsButton);
        add(viewReportButton);

        // Dummy uploads for now
        uploadStudentsButton.addActionListener(ignored -> JOptionPane.showMessageDialog(this, "Students CSV upload not yet implemented."));
        uploadRoomsButton.addActionListener(ignored -> JOptionPane.showMessageDialog(this, "Rooms CSV upload not yet implemented."));
        uploadExamSlotsButton.addActionListener(ignored -> JOptionPane.showMessageDialog(this, "Exam slots CSV upload not yet implemented."));

        // Allocate Seats (calls logic)
        allocateSeatsButton.addActionListener(ignored -> {
            new SwingWorker<SeatAllocator.AllocationResult, Void>() {
                protected SeatAllocator.AllocationResult doInBackground() {
                    SeatAllocator allocator = new SeatAllocator();
                    // For demo: hardcode examSlotId = "EX01"
                    return allocator.allocateSeatsForExam("EX01");
                }
                protected void done() {
                    try {
                        SeatAllocator.AllocationResult res = get();
                        if (res.success) {
                            JOptionPane.showMessageDialog(AdminDashboardFrame.this, "Seat allocation completed.");
                        } else {
                            JOptionPane.showMessageDialog(AdminDashboardFrame.this, "Seat allocation failed.");
                        }
                        if (!res.warnings.isEmpty()) {
                            JOptionPane.showMessageDialog(AdminDashboardFrame.this,
                                    "ADMIN WARNINGS:\n" + String.join("\n", res.warnings));
                        }
                        // TODO: Open report frame (reading allocations from DB)
                        // new AllocationReportFrame();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AdminDashboardFrame.this,
                                "Error during allocation: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        // View Reports (fetch from DB, e.g., in a JTable)
        viewReportButton.addActionListener(ignored -> {
            SeatAllocator allocator = new SeatAllocator();
            var records = allocator.generateFullReport();
            if (records.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No allocations found.");
            } else {
                StringBuilder sb = new StringBuilder("Seat Allocations:\n");
                for (var rec : records) {
                    sb.append(rec.toString()).append("\n");
                }
                JTextArea area = new JTextArea(sb.toString(), 20, 50);
                area.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(area), "Report", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }
}
