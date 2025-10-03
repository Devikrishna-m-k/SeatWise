package gui;

import javax.swing.*;

public class BulkUploadExamSlotFrame extends JFrame {
    private JButton uploadButton;

    public BulkUploadExamSlotFrame() {
        setTitle("Upload Exam Slot Data");
        setSize(350, 200);
        setLayout(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        uploadButton = new JButton("Select CSV & Upload");
        uploadButton.setBounds(80, 60, 180, 30);
        add(uploadButton);

        uploadButton.addActionListener(ignored ->
            JOptionPane.showMessageDialog(this, "Exam Slots CSV uploaded (dummy action)")
        );

        setLocationRelativeTo(null);
        setVisible(true);
    }
}

