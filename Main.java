import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;  // Needed for ArrayList

// Base class Bank
class Bank {
        private static final String NAME = "AITU Bank";

        public String getName() {
            return NAME;
        }
    }


// Child class Branch
class Branch extends Bank {
    private String branchName;
    private Deposit[] deposits;

    public Branch(String branchName, Deposit[] deposits) {
        this.branchName = branchName;
        this.deposits = deposits;
    }

    public String getBranchName() {
        return branchName;
    }

    public Deposit[] getDeposits() {
        return deposits;
    }

    public double getTotalAmount() {
        double total = 0;
        for (Deposit deposit : deposits) {
            total += deposit.getAmount();
        }
        return total;
    }

    public boolean depositMoney(String depositName, double amount) {
        for (Deposit deposit : deposits) {
            if (deposit.getName().equalsIgnoreCase(depositName)) {
                deposit.addAmount(amount);
                Database.updateDepositAmount(depositName, amount);
                return true;
            }
        }
        return false;
    }
}

// Deposit class for individual deposits
class Deposit {
    private String name;
    private double amount;

    public Deposit(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public void addAmount(double additionalAmount) {
        this.amount += additionalAmount;
    }
}

// Database connection
class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/mydatabase?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "kapitanyeye";

    public static Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }

    public static Deposit[] getDepositsByBranch(String branchName) {
        String query = "SELECT deposit_name, amount FROM deposits WHERE branch_name = ?";
        ArrayList<Deposit> depositList = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, branchName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                depositList.add(new Deposit(rs.getString("deposit_name"), rs.getDouble("amount")));
            }
            return depositList.toArray(new Deposit[0]);
        } catch (SQLException e) {
            e.printStackTrace();
            return new Deposit[0];
        }
    }

    public static boolean updateDepositAmount(String depositName, double amount) {
        String query = "UPDATE deposits SET amount = amount + ? WHERE deposit_name = ?";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, depositName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean createDeposit(String branchName, String depositName, double amount) {
        String query = "INSERT INTO deposits (branch_name, deposit_name, amount) VALUES (?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, branchName);
            stmt.setString(2, depositName);
            stmt.setDouble(3, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteDeposit(String depositName) {
        String query = "DELETE FROM deposits WHERE deposit_name = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, depositName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

// UI
public class Main {
    private JFrame frame;
    private JPanel mainPanel, branchPanel, depositPanel;
    private JButton branch1Button, branch2Button, depositMoneyButton, backButton, createDepositButton, deleteDepositButton;
    private JTextArea outputArea;
    private Branch[] branches;

    private void reloadBranches() {
        branches = new Branch[]{
                new Branch("Esil", Database.getDepositsByBranch("Esil")),
                new Branch("Nura", Database.getDepositsByBranch("Nura"))
        };
    }

    public Main() {
        frame = new JFrame("AITU Bank");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new CardLayout());


        // Main Panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JLabel("Welcome to AITU Bank", SwingConstants.CENTER), BorderLayout.CENTER);
        JButton startButton = new JButton("Proceed");
        mainPanel.add(startButton, BorderLayout.SOUTH);

        // Branch Panel
        branchPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        branch1Button = new JButton("Esil");
        branch2Button = new JButton("Nura");
        depositMoneyButton = new JButton("Deposit Money");
        createDepositButton = new JButton("Create Deposit");
        deleteDepositButton = new JButton("Delete Deposit");
        branchPanel.add(branch1Button);
        branchPanel.add(branch2Button);
        branchPanel.add(depositMoneyButton);
        branchPanel.add(createDepositButton);
        branchPanel.add(deleteDepositButton);

        reloadBranches(); // Initialize branches dynamically

        // Deposit Panel
        depositPanel = new JPanel(new BorderLayout());
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        depositPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        backButton = new JButton("Back");
        depositPanel.add(backButton, BorderLayout.SOUTH);

        // Add panels to frame with CardLayout
        frame.add(mainPanel, "Main");
        frame.add(branchPanel, "Branches");
        frame.add(depositPanel, "Deposits");

        // Action listeners
        startButton.addActionListener(e -> switchPanel("Branches"));
        branch1Button.addActionListener(e -> loadDeposits(0));
        branch2Button.addActionListener(e -> loadDeposits(1));
        depositMoneyButton.addActionListener(e -> depositMoney());
        backButton.addActionListener(e -> switchPanel("Branches"));
        createDepositButton.addActionListener(e -> createDeposit());
        deleteDepositButton.addActionListener(e -> deleteDeposit());

        frame.setVisible(true);
    }

    private void switchPanel(String panelName) {
        CardLayout cl = (CardLayout) frame.getContentPane().getLayout();
        cl.show(frame.getContentPane(), panelName);
    }

    private void loadDeposits(int index) {
        Branch selectedBranch = branches[index];
        outputArea.setText("Branch: " + selectedBranch.getBranchName() + "\nDeposits:\n");
        for (Deposit deposit : selectedBranch.getDeposits()) {
            outputArea.append(deposit.getName() + ": $" + deposit.getAmount() + "\n");
        }
        outputArea.append("Total Amount: $" + selectedBranch.getTotalAmount());
        switchPanel("Deposits");
    }

    // depositing money
    private void depositMoney() {
        // choosing branch
        String[] branchNames = new String[branches.length];
        for (int i = 0; i < branches.length; i++) {
            branchNames[i] = branches[i].getBranchName();
        }
        String selectedBranchName = (String) JOptionPane.showInputDialog(
                frame,
                "Select branch:",
                "Deposit Money",
                JOptionPane.PLAIN_MESSAGE,
                null,
                branchNames,
                branchNames[0]
        );
        if (selectedBranchName == null) {
            return; // User canceled
        }

        Branch selectedBranch = null;
        for (Branch branch : branches) {
            if (branch.getBranchName().equalsIgnoreCase(selectedBranchName)) {
                selectedBranch = branch;
                break;
            }
        }
        if (selectedBranch == null) {
            JOptionPane.showMessageDialog(frame, "Branch not found!");
            return;
        }

        // Ask for deposit name
        String depositName = JOptionPane.showInputDialog(frame, "Enter deposit name:");
        if (depositName == null || depositName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Deposit name cannot be empty.");
            return;
        }

        // Ask for deposit amount
        String amountStr = JOptionPane.showInputDialog(frame, "Enter deposit amount:");
        if (amountStr == null || amountStr.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Deposit amount cannot be empty.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid amount entered.");
            return;
        }

        // Perform deposit operation
        boolean success = selectedBranch.depositMoney(depositName, amount);
        if (success) {
            JOptionPane.showMessageDialog(frame, "Deposit successful!");
        } else {
            JOptionPane.showMessageDialog(frame,
                    "Deposit not found in branch " + selectedBranch.getBranchName());
        }
    }
    private void createDeposit() {
        String branchName = JOptionPane.showInputDialog(frame, "Enter branch name:");
        String depositName = JOptionPane.showInputDialog(frame, "Enter deposit name:");
        String amountStr = JOptionPane.showInputDialog(frame, "Enter deposit amount:");
        try {
            double amount = Double.parseDouble(amountStr);
            if (Database.createDeposit(branchName, depositName, amount)) {
                JOptionPane.showMessageDialog(frame, "Deposit created successfully!");
                reloadBranches();
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to create deposit.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid amount entered.");
        }
    }

    private void deleteDeposit() {
        String depositName = JOptionPane.showInputDialog(frame, "Enter deposit name to delete:");
        if (depositName == null || depositName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Deposit name cannot be empty.");
            return;
        }
        if (Database.deleteDeposit(depositName)) {
            JOptionPane.showMessageDialog(frame, "Deposit deleted successfully!");
            reloadBranches();
        } else {
            JOptionPane.showMessageDialog(frame, "Failed to delete deposit.");
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
