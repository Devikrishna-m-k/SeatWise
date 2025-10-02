import javax.swing.*;

public class UserLoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public UserLoginFrame() {
        setTitle("User Login");
        setSize(400, 250);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setBounds(50, 50, 100, 30);
        add(lblUsername);

        usernameField = new JTextField();
        usernameField.setBounds(150, 50, 150, 30);
        add(usernameField);

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setBounds(50, 100, 100, 30);
        add(lblPassword);

        passwordField = new JPasswordField();
        passwordField.setBounds(150, 100, 150, 30);
        add(passwordField);

        loginButton = new JButton("Login");
        loginButton.setBounds(150, 150, 100, 30);
        add(loginButton);

        loginButton.addActionListener(ignored -> {
            String username = usernameField.getText().trim();
            // For demo: treat "admin" as Admin, everything else as Student
            if (username.equalsIgnoreCase("admin")) {
                JOptionPane.showMessageDialog(this, "Logged in as Admin");
                new AdminDashboardFrame();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Logged in as Student: " + username);
                // Lookup student allocation from DB
                SeatAllocator allocator = new SeatAllocator();
                AllocationRecord rec = allocator.generateStudentReport(username);
                if (rec != null) {
                    JOptionPane.showMessageDialog(this,
                            "Your Seat:\nRoom: " + rec.getRoomId() +
                                    "\nSeat No: " + rec.getSeatNo() +
                                    "\nExam Slot: " + rec.getExamSlotId());
                } else {
                    JOptionPane.showMessageDialog(this, "No seat allocated yet.");
                }
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        new UserLoginFrame();
    }
}
