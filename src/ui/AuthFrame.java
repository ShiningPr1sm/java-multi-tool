package ui;

import db.DB;

import javax.swing.*;
import java.awt.*;

public class AuthFrame extends JFrame {
    private static final int FRAME_SIZE_WIDTH = 700;
    private static final int FRAME_SIZE_HEIGHT = 450;
    private boolean isLoginMode = true;

    public AuthFrame() {
        setTitle("MultiTool - Authentication");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(
                (int) ((screenSize.getWidth() - FRAME_SIZE_WIDTH) / 2),
                (int) ((screenSize.getHeight() - FRAME_SIZE_HEIGHT) / 2),
                FRAME_SIZE_WIDTH,
                FRAME_SIZE_HEIGHT
        );

        JPanel outerPanel = new JPanel(new GridBagLayout());
        outerPanel.setBackground(new Color(30, 30, 30));

        JPanel formPanel = new JPanel();
        formPanel.setBackground(new Color(45, 45, 45));
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setPreferredSize(new Dimension(300, 200));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel loginLabel = new JLabel("Login:");
        loginLabel.setForeground(Color.WHITE);
        JTextField loginField = new JTextField(20);
        loginField.setMaximumSize(new Dimension(300, 30));

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setMaximumSize(new Dimension(300, 30));

        JButton authButton = new JButton("Login");
        JButton switchModeButton = new JButton("Switch to Registration");

        for (JComponent comp : new JComponent[]{loginLabel, loginField, passwordLabel, passwordField, authButton, switchModeButton}) {
            comp.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        authButton.setFocusPainted(false);
        authButton.setBackground(new Color(70, 70, 70));
        authButton.setForeground(Color.WHITE);

        switchModeButton.setFocusPainted(false);
        switchModeButton.setBackground(new Color(60, 60, 60));
        switchModeButton.setForeground(Color.LIGHT_GRAY);

        authButton.addActionListener(_ -> {
            String login = loginField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            if (login.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty");
                return;
            }
            boolean success;
            if (isLoginMode) {
                success = DB.checkLogin(login, password);
                if (success) {
                    new MainFrame(login);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid login or password");
                }
            } else {
                success = DB.register(login, password);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Registration successful! Now log in.");
                    isLoginMode = true;
                    authButton.setText("Login");
                    switchModeButton.setText("Switch to Registration");
                } else {
                    JOptionPane.showMessageDialog(this, "Login already taken");
                }
            }
        });

        switchModeButton.addActionListener(_ -> {
            isLoginMode = !isLoginMode;
            authButton.setText(isLoginMode ? "Login" : "Register");
            switchModeButton.setText(isLoginMode ? "Switch to Registration" : "Switch to Login");
        });

        formPanel.add(loginLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(loginField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(passwordLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(passwordField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(authButton);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(switchModeButton);

        outerPanel.add(formPanel);
        setContentPane(outerPanel);
        setVisible(true);
    }
}