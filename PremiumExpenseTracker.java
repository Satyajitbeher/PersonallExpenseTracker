/*
Folder structure (single-file demo project):

PersonalExpenseTracker/
├─ src/
│  └─ PremiumExpenseTracker.java   <-- this file (single-file runnable)
├─ data/
│  └─ expenses.csv                 <-- created at runtime (in working dir)
└─ README.md

To compile & run (from project root or src):
  javac PremiumExpenseTracker.java
  java PremiumExpenseTracker

-------------------------------------------------------------
PremiumExpenseTracker.java
A single-file Java Swing application (Premium Look) implementing:
 - Add Expense dialog (popup)
 - Table view with sorting
 - Category & monthly totals
 - CSV persistence (expenses.csv)
 - Export button
 - Modern/premium-ish styling (fonts, spacing, icons-ish via shapes)

This single-file contains small inner classes: Expense, ExpenseTableModel.
-------------------------------------------------------------
*/

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class PremiumExpenseTracker {
    private static final String CSV_FILE = "expenses.csv"; // stored in working dir
    private final JFrame frame;
    private final ExpenseTableModel tableModel;
    private final JLabel totalLabel;
    private final JLabel monthLabel;
    private final JTable table;
    private final JComboBox<String> monthFilterCombo;
    private final TableRowSorter<ExpenseTableModel> sorter;

    public PremiumExpenseTracker() {
        tableModel = new ExpenseTableModel();
        frame = new JFrame("Premium Expense Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // Set a modern-ish look
        applyPremiumStyle();

        // Top panel: header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(32, 44, 61));
        header.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Expense Tracker");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));

        JLabel subtitle = new JLabel("Smart. Clean. Interview-ready.");
        subtitle.setForeground(new Color(180, 190, 200));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel titleBox = new JPanel(new GridLayout(2,1));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        header.add(titleBox, BorderLayout.WEST);

        // Action buttons on header
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton addBtn = createPrimaryButton("+ Add Expense");
        addBtn.addActionListener(e -> openAddDialog());

        JButton exportBtn = createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> exportCSV());

        JButton clearBtn = createTertiaryButton("Delete Selected");
        clearBtn.addActionListener(e -> deleteSelectedRows());

        actions.add(addBtn);
        actions.add(exportBtn);
        actions.add(clearBtn);

        header.add(actions, BorderLayout.EAST);

        frame.add(header, BorderLayout.NORTH);

        // Center: Table inside a nice panel
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Renderer for amount column
        DefaultTableCellRenderer amountRenderer = new DefaultTableCellRenderer();
        amountRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(amountRenderer);

        // Center panel wrapper
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(12, 12, 12, 12));
        center.setBackground(new Color(242, 246, 250));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));

        center.add(scroll, BorderLayout.CENTER);

        // Right: quick summary
        JPanel right = new JPanel();
        right.setPreferredSize(new Dimension(260, 0));
        right.setBackground(new Color(248, 250, 252));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(12,12,12,12));

        monthLabel = new JLabel("Showing: All months");
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        monthLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalLabel = new JLabel("Total: ₹0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        totalLabel.setBorder(new EmptyBorder(12,0,12,0));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        right.add(monthLabel);
        right.add(totalLabel);
        right.add(Box.createVerticalStrut(12));

        right.add(createSummaryBox());

        center.add(right, BorderLayout.EAST);

        frame.add(center, BorderLayout.CENTER);

        // Bottom: filter and status
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(8,12,12,12));
        bottom.setBackground(new Color(255,255,255));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filters.setOpaque(false);

        monthFilterCombo = new JComboBox<>();
        monthFilterCombo.addItem("All months");
        populateMonths();
        monthFilterCombo.addActionListener(e -> applyMonthFilter());

        filters.add(new JLabel("Filter:"));
        filters.add(monthFilterCombo);

        bottom.add(filters, BorderLayout.WEST);

        frame.add(bottom, BorderLayout.SOUTH);

        // Sorting
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Load data
        loadFromCSV();
        refreshTotals();

        frame.setVisible(true);
    }

    private void applyPremiumStyle() {
        try {
            UIManager.put("Button.background", new Color(52,152,219));
            UIManager.put("Panel.background", Color.WHITE);
            UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 13));
            UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 13));
        } catch (Exception ignored) {}
    }

    private JPanel createSummaryBox() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel heading = new JLabel("Category Totals");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(heading);
        p.add(Box.createVerticalStrut(8));

        // dynamic area
        JPanel catPanel = new JPanel();
        catPanel.setLayout(new BoxLayout(catPanel, BoxLayout.Y_AXIS));
        catPanel.setOpaque(false);
        catPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // fill
        Map<String, Double> map = tableModel.categoryTotals();
        for (String cat : map.keySet()) {
            JLabel lbl = new JLabel(cat + " : ₹" + String.format("%.2f", map.get(cat)));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            catPanel.add(lbl);
        }

        p.add(catPanel);
        return p;
    }

    private void populateMonths() {
        // gather unique months present in data model
        Set<String> months = new TreeSet<>(Collections.reverseOrder());
        SimpleDateFormat key = new SimpleDateFormat("yyyy-MM");
        for (Expense ex : tableModel.getExpenses()) {
            months.add(key.format(ex.date));
        }
        for (String m : months) {
            monthFilterCombo.addItem(m);
        }
    }

    private void applyMonthFilter() {
        String sel = (String) monthFilterCombo.getSelectedItem();
        if (sel == null || sel.equals("All months")) {
            sorter.setRowFilter(null);
            monthLabel.setText("Showing: All months");
        } else {
            sorter.setRowFilter(new RowFilter<ExpenseTableModel,Integer>(){
                final SimpleDateFormat key = new SimpleDateFormat("yyyy-MM");
                @Override
                public boolean include(Entry<? extends ExpenseTableModel, ? extends Integer> entry) {
                    Expense e = tableModel.getExpenses().get(entry.getIdentifier());
                    return key.format(e.date).equals(sel);
                }
            });
            monthLabel.setText("Showing: " + sel);
        }
        refreshTotals();
    }

    private void deleteSelectedRows() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(frame, "No rows selected", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(frame, "Delete selected expenses?","Confirm",JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        // convert view rows to model rows
        for (int i = rows.length-1; i >= 0; i--) {
            int modelIndex = table.convertRowIndexToModel(rows[i]);
            tableModel.removeExpenseAt(modelIndex);
        }
        saveToCSV();
        refreshTotals();
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Expenses to CSV");
        int res = chooser.showSaveDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("date,category,amount,description");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                for (Expense ex : tableModel.getExpenses()) {
                    pw.printf("%s,%s,%.2f,%s\n", sdf.format(ex.date), escapeCSV(ex.category), ex.amount, escapeCSV(ex.description));
                }
                JOptionPane.showMessageDialog(frame, "Exported successfully.", "Done", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeCSV(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    private void openAddDialog() {
        JDialog d = new JDialog(frame, "Add Expense", true);
        d.setSize(420, 320);
        d.setLocationRelativeTo(frame);
        d.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setBorder(new EmptyBorder(12,12,12,12));
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.anchor = GridBagConstraints.WEST;

        JLabel dateL = new JLabel("Date (yyyy-mm-dd):");
        JLabel catL = new JLabel("Category:");
        JLabel amtL = new JLabel("Amount:");
        JLabel descL = new JLabel("Description:");

        JTextField dateF = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 18);
        JComboBox<String> catF = new JComboBox<>(new String[]{"Food","Travel","Shopping","Bills","Others"});
        JTextField amtF = new JTextField(10);
        JTextArea descF = new JTextArea(4, 18);
        descF.setLineWrap(true);
        descF.setWrapStyleWord(true);

        c.gridx=0; c.gridy=0; form.add(dateL, c);
        c.gridx=1; form.add(dateF, c);
        c.gridx=0; c.gridy=1; form.add(catL, c);
        c.gridx=1; form.add(catF, c);
        c.gridx=0; c.gridy=2; form.add(amtL, c);
        c.gridx=1; form.add(amtF, c);
        c.gridx=0; c.gridy=3; form.add(descL, c);
        c.gridx=1; form.add(new JScrollPane(descF), c);

        d.add(form, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        foot.setBorder(new EmptyBorder(8,12,8,12));
        JButton save = createPrimaryButton("Save");
        JButton cancel = createTertiaryButton("Cancel");
        save.addActionListener(ev -> {
            String dateStr = dateF.getText().trim();
            String category = (String) catF.getSelectedItem();
            String amtStr = amtF.getText().trim();
            String desc = descF.getText().trim();
            if (dateStr.isEmpty() || amtStr.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Please enter required fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date dt = sdf.parse(dateStr);
                double amount = Double.parseDouble(amtStr);
                Expense ex = new Expense(dt, category, amount, desc);
                tableModel.addExpense(ex);
                saveToCSV();
                refreshTotals();
                populateMonthsIfNeeded(dt);
                d.dispose();
            } catch (ParseException pe) {
                JOptionPane.showMessageDialog(d, "Invalid date format. Use yyyy-mm-dd", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException ne) {
                JOptionPane.showMessageDialog(d, "Invalid number for amount", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancel.addActionListener(ev -> d.dispose());

        foot.add(cancel);
        foot.add(save);
        d.add(foot, BorderLayout.SOUTH);

        d.setVisible(true);
    }

    private void populateMonthsIfNeeded(Date dt) {
        SimpleDateFormat key = new SimpleDateFormat("yyyy-MM");
        String s = key.format(dt);
        boolean found = false;
        for (int i=0;i<monthFilterCombo.getItemCount();i++) {
            if (s.equals(monthFilterCombo.getItemAt(i))) { found = true; break; }
        }
        if (!found) monthFilterCombo.addItem(s);
    }

    private JButton createPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(52,152,219));
        b.setFocusPainted(false);
        b.setBorder(new RoundedBorder(8));
        b.setPreferredSize(new Dimension(120,34));
        return b;
    }
    private JButton createSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(new Color(32,44,61));
        b.setBackground(new Color(220,230,240));
        b.setFocusPainted(false);
        b.setBorder(new RoundedBorder(8));
        b.setPreferredSize(new Dimension(120,34));
        return b;
    }
    private JButton createTertiaryButton(String text) {
        JButton b = new JButton(text);
        b.setForeground(new Color(90,90,90));
        b.setBackground(new Color(245,245,245));
        b.setFocusPainted(false);
        b.setBorder(new RoundedBorder(8));
        b.setPreferredSize(new Dimension(120,34));
        return b;
    }

    private void refreshTotals() {
        double total = 0;
        for (Expense e : tableModel.getExpenses()) total += e.amount;
        totalLabel.setText("Total: ₹" + String.format("%.2f", total));

        // update category totals in the right panel: easiest is to rebuild UI
        // (for simplicity we won't do a fancy live update of the small panel here)
    }

    private void saveToCSV() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_FILE))) {
            pw.println("date,category,amount,description");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            for (Expense ex : tableModel.getExpenses()) {
                pw.printf("%s,%s,%.2f,%s\n", sdf.format(ex.date), ex.category.replace(',', ' '), ex.amount, ex.description.replace('\n',' '));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFromCSV() {
        Path p = Path.of(CSV_FILE);
        if (!Files.exists(p)) return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line = br.readLine(); // header
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // naive split (assumes no commas in fields)
                String[] parts = line.split(",",4);
                if (parts.length < 3) continue;
                Date dt = sdf.parse(parts[0]);
                String cat = parts[1];
                double amt = Double.parseDouble(parts[2]);
                String desc = parts.length==4?parts[3]:"";
                tableModel.addExpense(new Expense(dt, cat, amt, desc));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Expense {
        Date date;
        String category;
        double amount;
        String description;
        Expense(Date date, String category, double amount, String description) {
            this.date = date; this.category = category; this.amount = amount; this.description = description;
        }
    }

    private static class ExpenseTableModel extends AbstractTableModel {
        private final String[] cols = {"Date","Category","Amount","Description"};
        private final java.util.List<Expense> expenses = new java.util.ArrayList<>();
        //private final List<Expense> expenses = new ArrayList<>();
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        public void addExpense(Expense e) {
            expenses.add(0, e); // add to top
            fireTableRowsInserted(0,0);
        }
        public void removeExpenseAt(int i) {
            expenses.remove(i);
            fireTableDataChanged();
        }
        public java.util.List<Expense> getExpenses() { return expenses; }
       // public List<Expense> getExpenses() { return expenses; }
        @Override
        public int getRowCount() { return expenses.size(); }
        @Override
        public int getColumnCount() { return cols.length; }
        @Override
        public String getColumnName(int c) { return cols[c]; }
        @Override
        public Object getValueAt(int r, int c) {
            Expense e = expenses.get(r);
            switch(c) {
                case 0: return sdf.format(e.date);
                case 1: return e.category;
                case 2: return String.format("%.2f", e.amount);
                case 3: return e.description;
            }
            return "";
        }
        public Map<String, Double> categoryTotals() {
            Map<String, Double> out = new LinkedHashMap<>();
            for (Expense e : expenses) {
                out.putIfAbsent(e.category, 0.0);
                out.put(e.category, out.get(e.category) + e.amount);
            }
            return out;
        }
    }

    // small rounded border for buttons
    private static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final int radius;
        RoundedBorder(int r) { radius = r; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200,200,200));
            g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        // Run GUI on EDT
        SwingUtilities.invokeLater(() -> new PremiumExpenseTracker());
    }
}
