import javax.swing.*;

public class BulkUploadStudentFrame extends JFrame {
    private JButton uploadButton;

    public BulkUploadStudentFrame() {
        setTitle("Upload Student Data");
        setSize(350, 200);
        setLayout(null);

        uploadButton = new JButton("Select CSV & Upload");
        uploadButton.setBounds(80, 60, 180, 30);
        add(uploadButton);

        uploadButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Students CSV uploaded"));

        setLocationRelativeTo(null);
        setVisible(true);
    }
}
