import javax.swing.*;

public class StudentSeatViewFrame extends JFrame {
    private JLabel seatInfoLabel;

    public StudentSeatViewFrame() {
        setTitle("My Seat Information");
        setSize(400, 200);
        setLayout(null);

        seatInfoLabel = new JLabel("Seat Info: Room 101, Seat 12, Slot: EX01");
        seatInfoLabel.setBounds(50, 80, 300, 30);
        add(seatInfoLabel);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}
