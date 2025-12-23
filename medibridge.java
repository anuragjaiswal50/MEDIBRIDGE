
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

// ============================================
// PART 0: THEME 
// ============================================

class Theme {
    public static final Color BG_MAIN = new Color(245, 247, 250);        
    public static final Color CARD_BG = Color.WHITE;
    public static final Color PRIMARY = new Color(0, 150, 136);          
    public static final Color PRIMARY_HOVER = new Color(0, 121, 107);
    public static final Color TEXT_DARK = new Color(51, 51, 51);        
    public static final Color TEXT_MED = new Color(100, 100, 100);      
    public static final Color DANGER = new Color(239, 83, 80);
    public static final Color ACCENT_BLUE = new Color(66, 165, 245);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_NAV = new Font("Segoe UI", Font.BOLD, 16); 
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    
    public static final String[] SPECIALIZATIONS = {
        "General Physician", "Cardiologist", "Dermatologist", 
        "Neurologist", "Pediatrician", "Orthopedist", 
        "Psychiatrist", "Dentist", "ENT Specialist"
    };

    public static void styleTable(JTable table) {
        table.setRowHeight(45);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 240, 240));
        table.setFont(FONT_REGULAR);
        table.setForeground(TEXT_DARK);
        table.setSelectionBackground(new Color(224, 242, 241));
        table.setSelectionForeground(TEXT_DARK);
        
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setBackground(PRIMARY);
                l.setForeground(Color.WHITE);
                l.setFont(FONT_HEADER);
                l.setBorder(new EmptyBorder(10, 10, 10, 10));
                l.setOpaque(true);
                return l;
            }
        });
        header.setPreferredSize(new Dimension(0, 40));
    }

    public static void styleTextField(JTextField txt) {
        txt.setFont(FONT_REGULAR);
        txt.setForeground(TEXT_DARK);
        txt.setBackground(Color.WHITE);
        txt.setCaretColor(PRIMARY);
        txt.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1, true),
            new EmptyBorder(5, 10, 5, 10)
        ));
    }
}

// UI Components
class RoundedPanel extends JPanel {
    private int radius = 20;
    private Color bgColor;
    public RoundedPanel(Color bgColor) { this.bgColor = bgColor; setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setColor(new Color(230, 230, 230)); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
        g2.dispose(); super.paintComponent(g);
    }
}

class RoundedButton extends JButton {
    private Color color, hoverColor;
    public RoundedButton(String text, Color color, Color hoverColor) {
        super(text); this.color = color; this.hoverColor = hoverColor;
        setFont(Theme.FONT_HEADER); setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR)); setForeground(Color.WHITE);
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { setModel(new DefaultButtonModel() { boolean isRollover = true; }); repaint(); }
            public void mouseExited(MouseEvent e) { setModel(new DefaultButtonModel() { boolean isRollover = false; }); repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getModel().isRollover() ? hoverColor : color);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent()) / 2 - 3);
        g2.dispose();
    }
}

// ============================================
// PART 1: DATABASE & DATA
// ============================================

class DB {
    static Connection connect() {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); 
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/medibridge", "root", "Mobie@123"); } 
        catch (Exception e) { e.printStackTrace(); return null; }
    }
}

class User {
    int id; String email, name, phone, type;
    User(int id, String email, String name, String phone, String type) { this.id = id; this.email = email; this.name = name; this.phone = phone; this.type = type; }
}
class Patient extends User {
    int patientId; String gender, address;
    Patient(int id, String email, String name, String phone) { super(id, email, name, phone, "PATIENT"); }
}
class Doctor extends User {
    int doctorId; String specialization; int experience; double fee; boolean verified;
    Doctor(int id, String email, String name, String phone, String spec, int exp) {
        super(id, email, name, phone, "DOCTOR"); this.specialization = spec; this.experience = exp;
    }
}
class Appointment { int id; int patientId; int doctorId; String patientName, doctorName, date, time, status, symptoms; }

// ============================================
// PART 2: DATABASE OPERATIONS
// ============================================

class Database {
    
    static void logAction(String action) {
        try {
            Connection con = DB.connect();
            PreparedStatement ps = con.prepareStatement("INSERT INTO system_logs (action_details, timestamp) VALUES (?, NOW())");
            ps.setString(1, action);
            ps.executeUpdate();
            con.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    static ArrayList<String[]> getSystemLogs() {
        ArrayList<String[]> list = new ArrayList<>();
        try {
            Connection con = DB.connect();
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM system_logs ORDER BY timestamp DESC");
            while (rs.next()) {
                list.add(new String[]{ rs.getString("timestamp"), rs.getString("action_details") });
            }
            con.close();
        } catch (Exception e) {}
        return list;
    }

    static String getMedicalHistory(int pid) {
        StringBuilder history = new StringBuilder();
        try {
            Connection con = DB.connect();
            
            history.append("--- PAST APPOINTMENTS ---\n");
            PreparedStatement ps1 = con.prepareStatement(
                "SELECT a.appointment_date, d.specialization, a.symptoms, a.status " +
                "FROM appointments a JOIN doctors d ON a.doctor_id=d.doctor_id " +
                "WHERE a.patient_id=? ORDER BY a.appointment_date DESC");
            ps1.setInt(1, pid);
            ResultSet rs1 = ps1.executeQuery();
            while(rs1.next()) {
                history.append("• ").append(rs1.getString(1)).append(" (").append(rs1.getString(2)).append(")\n")
                       .append("   Symp: ").append(rs1.getString(3)).append(" | Status: ").append(rs1.getString(4)).append("\n\n");
            }

            history.append("--- MEDICATION HISTORY ---\n");
            PreparedStatement ps2 = con.prepareStatement("SELECT medicine_name, dosage, duration FROM prescriptions WHERE patient_id=?");
            ps2.setInt(1, pid);
            ResultSet rs2 = ps2.executeQuery();
            while(rs2.next()) {
                history.append("• Rx: ").append(rs2.getString(1)).append(" - ").append(rs2.getString(2))
                       .append(" for ").append(rs2.getString(3)).append("\n");
            }
            con.close();
        } catch (Exception e) { return "Error fetching history."; }
        
        if(history.length() < 50) return "No medical history found for this patient.";
        return history.toString();
    }

    static User login(String email, String password, String type) {
        if (type.equals("ADMIN") && email.equals("admin@medibridge.com") && password.equals("admin123")) {
            logAction("Admin Logged In");
            return new User(0, email, "Administrator", "", "ADMIN");
        }
        try {
            Connection con = DB.connect();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE email=? AND password=? AND user_type=?");
            ps.setString(1, email); ps.setString(2, password); ps.setString(3, type);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int uid = rs.getInt("user_id"); String name = rs.getString("full_name"); String ph = rs.getString("phone");
                
                logAction("User Login: " + email + " (" + type + ")");

                if (type.equals("PATIENT")) {
                    Patient p = new Patient(uid, email, name, ph);
                    PreparedStatement ps2 = con.prepareStatement("SELECT * FROM patients WHERE user_id=?");
                    ps2.setInt(1, uid); ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) { p.patientId = rs2.getInt("patient_id"); p.gender = rs2.getString("gender"); p.address = rs2.getString("address"); }
                    con.close(); return p;
                } else if (type.equals("DOCTOR")) {
                    Doctor d = new Doctor(uid, email, name, ph, "", 0);
                    PreparedStatement ps2 = con.prepareStatement("SELECT * FROM doctors WHERE user_id=?");
                    ps2.setInt(1, uid); ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        d.doctorId = rs2.getInt("doctor_id"); d.specialization = rs2.getString("specialization");
                        d.experience = rs2.getInt("experience_years"); d.fee = rs2.getDouble("consultation_fee");
                        d.verified = rs2.getBoolean("is_verified");
                    }
                    con.close(); return d; 
                }
            }
            con.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    static ArrayList<Doctor> getVerifiedDoctors() {
        ArrayList<Doctor> list = new ArrayList<>();
        try {
            Connection con = DB.connect();
            ResultSet rs = con.createStatement().executeQuery("SELECT u.full_name, u.email, u.phone, d.* FROM users u JOIN doctors d ON u.user_id=d.user_id WHERE d.is_verified=1");
            while (rs.next()) {
                Doctor d = new Doctor(rs.getInt("user_id"), rs.getString("email"), rs.getString("full_name"), rs.getString("phone"), rs.getString("specialization"), rs.getInt("experience_years"));
                d.doctorId = rs.getInt("doctor_id"); d.fee = rs.getDouble("consultation_fee"); d.verified = true;
                list.add(d);
            }
            con.close();
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    static ArrayList<Patient> getAllPatients() {
        ArrayList<Patient> list = new ArrayList<>();
        try {
            Connection con = DB.connect();
            ResultSet rs = con.createStatement().executeQuery("SELECT u.full_name, u.email, u.phone, p.* FROM users u JOIN patients p ON u.user_id=p.user_id");
            while (rs.next()) {
                Patient p = new Patient(rs.getInt("user_id"), rs.getString("email"), rs.getString("full_name"), rs.getString("phone"));
                p.patientId = rs.getInt("patient_id"); p.gender = rs.getString("gender"); p.address = rs.getString("address");
                list.add(p);
            }
            con.close();
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    static String registerPatient(String email, String pass, String name, String phone, String gender, String address) {
        try {
            Connection con = DB.connect();
            PreparedStatement ps1 = con.prepareStatement("INSERT INTO users (email, password, full_name, phone, user_type) VALUES (?,?,?,?,'PATIENT')", Statement.RETURN_GENERATED_KEYS);
            ps1.setString(1, email); ps1.setString(2, pass); ps1.setString(3, name); ps1.setString(4, phone); ps1.executeUpdate();
            ResultSet rs = ps1.getGeneratedKeys(); int uid = 0; if (rs.next()) uid = rs.getInt(1);
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO patients (user_id, gender, address) VALUES (?,?,?)");
            ps2.setInt(1, uid); ps2.setString(2, gender); ps2.setString(3, address); ps2.executeUpdate();
            
            logAction("Registered new Patient: " + name);
            con.close(); return "SUCCESS";
        } catch (Exception e) { return e.getMessage(); }
    }

    static String registerDoctor(String email, String pass, String name, String phone, String spec, int exp, String lic, double fee) {
        try {
            Connection con = DB.connect();
            PreparedStatement ps1 = con.prepareStatement("INSERT INTO users (email, password, full_name, phone, user_type) VALUES (?,?,?,?,'DOCTOR')", Statement.RETURN_GENERATED_KEYS);
            ps1.setString(1, email); ps1.setString(2, pass); ps1.setString(3, name); ps1.setString(4, phone); ps1.executeUpdate();
            ResultSet rs = ps1.getGeneratedKeys(); int uid = 0; if (rs.next()) uid = rs.getInt(1);
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO doctors (user_id, specialization, experience_years, qualification, license_number, consultation_fee, is_verified) VALUES (?,?,?,?,?,?,?)");
            ps2.setInt(1, uid); ps2.setString(2, spec); ps2.setInt(3, exp); ps2.setString(4, "MBBS"); ps2.setString(5, lic); ps2.setDouble(6, fee); ps2.setBoolean(7, false);
            ps2.executeUpdate(); 
            
            logAction("Registered new Doctor: " + name);
            con.close(); return "SUCCESS";
        } catch (Exception e) { return e.getMessage(); }
    }

    static boolean bookAppointment(int pid, int did, String date, String time, String symp) {
        try {
            Connection con = DB.connect();
            PreparedStatement ps = con.prepareStatement("INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, symptoms, status) VALUES (?,?,?,?,?, 'PENDING')");
            ps.setInt(1, pid); ps.setInt(2, did); ps.setString(3, date); ps.setString(4, time); ps.setString(5, symp);
            ps.executeUpdate(); 
            
            logAction("Appointment Booked. Patient ID: " + pid);
            con.close(); return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    static ArrayList<Appointment> getPatientAppointments(int pid) {
        ArrayList<Appointment> list = new ArrayList<>();
        try {
            Connection c = DB.connect();
            PreparedStatement ps = c.prepareStatement("SELECT a.*, u.full_name as doctor_name FROM appointments a JOIN doctors d ON a.doctor_id=d.doctor_id JOIN users u ON d.user_id=u.user_id WHERE a.patient_id=? ORDER BY a.appointment_date DESC");
            ps.setInt(1, pid); ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Appointment a = new Appointment(); a.id=rs.getInt("appointment_id"); a.doctorName=rs.getString("doctor_name"); a.date=rs.getString("appointment_date"); a.time=rs.getString("appointment_time"); a.status=rs.getString("status");
                list.add(a);
            }
            c.close();
        } catch(Exception e){} return list;
    }

    static ArrayList<Appointment> getDoctorAppointments(int did) {
        ArrayList<Appointment> list = new ArrayList<>();
        try {
            Connection c = DB.connect();
            PreparedStatement ps = c.prepareStatement("SELECT a.*, u.full_name as patient_name FROM appointments a JOIN patients p ON a.patient_id=p.patient_id JOIN users u ON p.user_id=u.user_id WHERE a.doctor_id=? ORDER BY a.appointment_date DESC");
            ps.setInt(1, did); ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Appointment a = new Appointment(); a.id=rs.getInt("appointment_id"); a.patientId=rs.getInt("patient_id"); a.patientName=rs.getString("patient_name"); a.date=rs.getString("appointment_date"); a.time=rs.getString("appointment_time"); a.status=rs.getString("status"); a.symptoms=rs.getString("symptoms");
                list.add(a);
            }
            c.close();
        } catch(Exception e){} return list;
    }

    static void updateApptStatus(int id, String status) {
        try { 
            Connection c = DB.connect(); 
            c.createStatement().executeUpdate("UPDATE appointments SET status='"+status+"' WHERE appointment_id="+id); 
            logAction("Appointment ID " + id + " updated to " + status);
            c.close(); 
        } catch(Exception e){}
    }

    static boolean addPrescription(int aptId, int pid, int did, String med, String dose, String freq, String dur) {
        try {
            Connection con = DB.connect();
            PreparedStatement ps = con.prepareStatement("INSERT INTO prescriptions (appointment_id, patient_id, doctor_id, medicine_name, dosage, frequency, duration) VALUES (?,?,?,?,?,?,?)");
            ps.setInt(1, aptId); ps.setInt(2, pid); ps.setInt(3, did); ps.setString(4, med); ps.setString(5, dose); ps.setString(6, freq); ps.setString(7, dur);
            ps.executeUpdate(); 
            
            logAction("Prescription Issued by Doc ID: " + did + " for Patient ID: " + pid);
            con.close(); return true;
        } catch(Exception e) { e.printStackTrace(); return false; }
    }

    static ArrayList<String[]> getPatientPrescriptions(int pid) {
        ArrayList<String[]> list = new ArrayList<>();
        try {
            Connection c = DB.connect();
            PreparedStatement ps = c.prepareStatement("SELECT p.*, u.full_name as dr_name FROM prescriptions p JOIN doctors d ON p.doctor_id=d.doctor_id JOIN users u ON d.user_id=u.user_id WHERE p.patient_id=? ORDER BY p.prescribed_date DESC");
            ps.setInt(1, pid); ResultSet rs = ps.executeQuery();
            while(rs.next()) list.add(new String[]{ rs.getString("medicine_name"), rs.getString("dosage") + " (" + rs.getString("frequency") + ")", rs.getString("duration"), "Dr. " + rs.getString("dr_name")});
            c.close();
        } catch(Exception e){} return list;
    }
}

// ============================================
// PART 3: UI SCREENS
// ============================================

public class medibridge extends JFrame {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        new LoginScreen();
    }
}

class LoginScreen extends JFrame {
    JTextField emailBox; JPasswordField passBox; JRadioButton patientBtn, doctorBtn;
    LoginScreen() {
        setTitle("MediBridge Login"); setSize(450, 600); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null); getContentPane().setBackground(Theme.BG_MAIN); setLayout(null);
        RoundedPanel card = new RoundedPanel(Theme.CARD_BG); card.setBounds(50, 50, 340, 460); card.setLayout(null); add(card);
        
        JLabel t = new JLabel("MediBridge"); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.PRIMARY); t.setBounds(30, 20, 200, 35); card.add(t);
        JLabel st = new JLabel("Healing starts here"); st.setFont(Theme.FONT_REGULAR); st.setForeground(Theme.TEXT_MED); st.setBounds(30, 55, 200, 20); card.add(st);
        
        int y = 90;
        card.add(lbl("Email", 30, y)); emailBox = new JTextField(); Theme.styleTextField(emailBox); emailBox.setBounds(30, y+25, 280, 35); card.add(emailBox); y += 70;
        card.add(lbl("Password", 30, y)); passBox = new JPasswordField(); Theme.styleTextField(passBox); passBox.setEchoChar('•'); passBox.setBounds(30, y+25, 280, 35); card.add(passBox); y += 50;
        patientBtn = new JRadioButton("Patient", true); styleRadio(patientBtn, 30, y+20); doctorBtn = new JRadioButton("Doctor"); styleRadio(doctorBtn, 150, y+20);
        ButtonGroup grp = new ButtonGroup(); grp.add(patientBtn); grp.add(doctorBtn); card.add(patientBtn); card.add(doctorBtn); y += 60;
        RoundedButton login = new RoundedButton("Login", Theme.PRIMARY, Theme.PRIMARY_HOVER); login.setBounds(30, y, 280, 40);
        login.addActionListener(e -> doLogin()); card.add(login); y += 50;
        JButton reg = new JButton("Create Account"); reg.setFont(Theme.FONT_SMALL); reg.setForeground(Theme.ACCENT_BLUE); reg.setContentAreaFilled(false); reg.setBorderPainted(false); reg.setBounds(30, y, 280, 30);
        reg.addActionListener(e -> { new RegisterScreen(); dispose(); }); card.add(reg);
        JButton admin = new JButton("Admin Portal"); admin.setFont(Theme.FONT_SMALL); admin.setForeground(Theme.TEXT_MED); admin.setContentAreaFilled(false); admin.setBorderPainted(false); admin.setBounds(120, 530, 200, 30);
        admin.addActionListener(e -> new AdminLogin()); add(admin); setVisible(true);
    }
    JLabel lbl(String s, int x, int y) { JLabel l = new JLabel(s); l.setFont(Theme.FONT_HEADER); l.setForeground(Theme.TEXT_MED); l.setBounds(x, y, 200, 25); return l; }
    void styleRadio(JRadioButton r, int x, int y) { r.setOpaque(false); r.setFont(Theme.FONT_REGULAR); r.setForeground(Theme.TEXT_DARK); r.setBounds(x, y, 100, 30); r.setFocusPainted(false); }
    void doLogin() {
        String type = patientBtn.isSelected() ? "PATIENT" : "DOCTOR";
        User u = Database.login(emailBox.getText(), new String(passBox.getPassword()), type);
        if (u != null) { dispose(); if (u instanceof Patient) new PatientDashboard((Patient)u); else if (u instanceof Doctor) new DoctorDashboard((Doctor)u); } 
        else JOptionPane.showMessageDialog(this, "Login Failed. Ensure account is Verified.");
    }
}

class RegisterScreen extends JFrame {
    JTextField nameF, emailF, phoneF, addrF, feeF, licF, expF; JPasswordField passF; JComboBox<String> genderBox, specBox; JRadioButton pBtn, dBtn; JPanel docPanel;
    RegisterScreen() {
        setTitle("Join MediBridge"); setSize(500, 750); setLocationRelativeTo(null); getContentPane().setBackground(Theme.BG_MAIN); setLayout(null);
        RoundedPanel card = new RoundedPanel(Color.WHITE); card.setBounds(30, 20, 425, 670); card.setLayout(null); add(card);
        JLabel t = new JLabel("Create Account"); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.PRIMARY); t.setBounds(30, 15, 300, 35); card.add(t);
        int y = 60; pBtn = new JRadioButton("Patient", true); styleRadio(pBtn, 30, y); dBtn = new JRadioButton("Doctor"); styleRadio(dBtn, 140, y);
        ButtonGroup grp = new ButtonGroup(); grp.add(pBtn); grp.add(dBtn); card.add(pBtn); card.add(dBtn); y += 40;
        nameF = addF(card, "Full Name", y); y+=50; emailF = addF(card, "Email", y); y+=50; phoneF = addF(card, "Phone", y); y+=50;
        JLabel pl = new JLabel("Password"); pl.setFont(Theme.FONT_HEADER); pl.setForeground(Theme.TEXT_MED); pl.setBounds(30, y, 100, 20); card.add(pl);
        passF = new JPasswordField(); Theme.styleTextField(passF); passF.setBounds(140, y, 250, 30); card.add(passF); y+=50;
        JLabel gl = new JLabel("Gender"); gl.setFont(Theme.FONT_HEADER); gl.setForeground(Theme.TEXT_MED); gl.setBounds(30, y, 100, 20); card.add(gl);
        genderBox = new JComboBox<>(new String[]{"Male", "Female", "Other"}); genderBox.setBounds(140, y, 250, 30); card.add(genderBox); y+=50;
        addrF = addF(card, "Address", y); y+=50;
        docPanel = new JPanel(); docPanel.setLayout(null); docPanel.setBackground(new Color(248, 250, 252));
        docPanel.setBounds(20, y, 380, 160); docPanel.setBorder(new LineBorder(Theme.PRIMARY, 1, true)); docPanel.setVisible(false);
        int dy = 10; JLabel sl = new JLabel("Specialty:"); sl.setBounds(10, dy, 80, 25); docPanel.add(sl);
        specBox = new JComboBox<>(Theme.SPECIALIZATIONS); specBox.setBounds(90, dy, 270, 25); docPanel.add(specBox); dy+=35;
        expF = addInner(docPanel, "Exp (Yrs):", dy); dy+=35; licF = addInner(docPanel, "License #:", dy); dy+=35; feeF = addInner(docPanel, "Fee (₹):", dy);
        card.add(docPanel);
        pBtn.addActionListener(e -> docPanel.setVisible(false)); dBtn.addActionListener(e -> docPanel.setVisible(true));
        RoundedButton sub = new RoundedButton("Register", Theme.PRIMARY, Theme.PRIMARY_HOVER); sub.setBounds(30, 600, 180, 40);
        sub.addActionListener(e -> register()); card.add(sub);
        JButton back = new JButton("Cancel"); back.setBounds(230, 600, 100, 40); back.addActionListener(e -> { new LoginScreen(); dispose(); }); card.add(back); setVisible(true);
    }
    JTextField addF(JPanel p, String l, int y) { JLabel lbl = new JLabel(l); lbl.setFont(Theme.FONT_HEADER); lbl.setForeground(Theme.TEXT_MED); lbl.setBounds(30, y, 100, 20); p.add(lbl); JTextField t = new JTextField(); Theme.styleTextField(t); t.setBounds(140, y, 250, 30); p.add(t); return t; }
    JTextField addInner(JPanel p, String l, int y) { JLabel lbl = new JLabel(l); lbl.setBounds(10, y, 80, 25); p.add(lbl); JTextField t = new JTextField(); Theme.styleTextField(t); t.setBounds(90, y, 270, 25); p.add(t); return t; }
    void styleRadio(JRadioButton r, int x, int y) { r.setOpaque(false); r.setBounds(x, y, 100, 30); }
    void register() {
        String res;
        if (pBtn.isSelected()) res = Database.registerPatient(emailF.getText(), new String(passF.getPassword()), nameF.getText(), phoneF.getText(), (String)genderBox.getSelectedItem(), addrF.getText());
        else res = Database.registerDoctor(emailF.getText(), new String(passF.getPassword()), nameF.getText(), phoneF.getText(), (String)specBox.getSelectedItem(), Integer.parseInt(expF.getText()), licF.getText(), Double.parseDouble(feeF.getText()));
        if (res.equals("SUCCESS")) { JOptionPane.showMessageDialog(this, "Success! Please Login."); new LoginScreen(); dispose(); } else JOptionPane.showMessageDialog(this, "Error: " + res);
    }
}

// ============================================
// PART 4: DASHBOARDS
// ============================================

class PatientDashboard extends JFrame {
    Patient patient; JPanel content;
    PatientDashboard(Patient p) {
        this.patient = p; setTitle("MediBridge Patient"); setSize(1100, 700); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null); setLayout(new BorderLayout());
        JPanel side = new JPanel(); side.setPreferredSize(new Dimension(240, 700)); side.setBackground(Color.WHITE); side.setBorder(BorderFactory.createMatteBorder(0,0,0,1,new Color(230,230,230)));
        JLabel n = new JLabel("Hi, " + p.name.split(" ")[0]); n.setFont(Theme.FONT_TITLE); n.setForeground(Theme.PRIMARY); n.setBorder(new EmptyBorder(30, 20, 20, 0)); side.add(n);
        
        side.add(btn("Find Doctor", e->findDocs())); 
        side.add(btn("Appointments", e->showAppts()));
        side.add(btn("My Prescriptions", e->showRx())); 
        
        JButton out = btn("Logout", e->{ new LoginScreen(); dispose(); }); out.setForeground(Theme.DANGER); side.add(out);
        add(side, BorderLayout.WEST);
        content = new JPanel(new BorderLayout()); content.setBackground(Theme.BG_MAIN); content.setBorder(new EmptyBorder(20,20,20,20)); add(content, BorderLayout.CENTER);
        findDocs(); setVisible(true);
    }
    JButton btn(String t, ActionListener a) { 
        JButton b = new JButton(t); b.setPreferredSize(new Dimension(220, 55)); 
        b.setFont(Theme.FONT_NAV); 
        b.setBackground(Color.WHITE); b.setBorderPainted(false); b.setFocusPainted(false); b.addActionListener(a); return b; 
    }
    void update(String t, JComponent c) { content.removeAll(); JLabel l = new JLabel(t); l.setFont(Theme.FONT_TITLE); l.setForeground(Theme.TEXT_DARK); content.add(l, BorderLayout.NORTH); content.add(c, BorderLayout.CENTER); content.revalidate(); content.repaint(); }
    
    void findDocs() {
        JPanel p = new JPanel(new BorderLayout(10, 10)); p.setOpaque(false);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.setOpaque(false);
        JComboBox<String> specFilter = new JComboBox<>(Theme.SPECIALIZATIONS); specFilter.insertItemAt("All Specializations", 0); specFilter.setSelectedIndex(0); specFilter.setPreferredSize(new Dimension(250, 35));
        top.add(new JLabel("Filter by: ")); top.add(specFilter); p.add(top, BorderLayout.NORTH);
        String[] col = {"ID", "Name", "Specialty", "Experience", "Fee", "Action"};
        DefaultTableModel model = new DefaultTableModel(col, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        
        ArrayList<Doctor> docs = Database.getVerifiedDoctors();
        for(Doctor d : docs) model.addRow(new Object[]{d.doctorId, "Dr. "+d.name, d.specialization, d.experience+" yrs", "₹"+d.fee, "Book"});
        
        JTable table = new JTable(model); Theme.styleTable(table);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model); table.setRowSorter(sorter);
        specFilter.addActionListener(e -> { String txt = (String)specFilter.getSelectedItem(); if(txt.equals("All Specializations")) sorter.setRowFilter(null); else sorter.setRowFilter(RowFilter.regexFilter(txt)); });
        table.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if(table.getSelectedColumn() == 5) { int row = table.convertRowIndexToModel(table.getSelectedRow()); book((int)model.getValueAt(row, 0), (String)model.getValueAt(row, 1)); } } });
        p.add(new JScrollPane(table)); update("Find Specialists", p);
    }
    
    void book(int did, String dname) {
        JDialog d = new JDialog(this, "Book Appointment", true); d.setSize(400, 450); d.setLocationRelativeTo(this); d.setLayout(null); d.getContentPane().setBackground(Color.WHITE);
        JLabel t = new JLabel("Book: " + dname); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.PRIMARY); t.setBounds(20, 10, 300, 30); d.add(t);
        d.add(new JLabel("Date:") {{ setBounds(20, 60, 100, 20); }});
        JSpinner dateSpin = new JSpinner(new SpinnerDateModel()); dateSpin.setEditor(new JSpinner.DateEditor(dateSpin, "yyyy-MM-dd")); dateSpin.setBounds(20, 80, 150, 30); d.add(dateSpin);
        d.add(new JLabel("Time:") {{ setBounds(200, 60, 100, 20); }});
        JSpinner timeSpin = new JSpinner(new SpinnerDateModel()); timeSpin.setEditor(new JSpinner.DateEditor(timeSpin, "HH:mm")); timeSpin.setBounds(200, 80, 150, 30); d.add(timeSpin);
        d.add(new JLabel("Symptoms:") {{ setBounds(20, 130, 100, 20); }});
        JTextArea symp = new JTextArea(); symp.setBorder(new LineBorder(Color.LIGHT_GRAY)); symp.setBounds(20, 150, 340, 100); d.add(symp);
        RoundedButton b = new RoundedButton("Confirm", Theme.PRIMARY, Theme.PRIMARY_HOVER); b.setBounds(100, 300, 200, 40);
        b.addActionListener(e -> {
            // ISSUE 1 FIX: MANDATORY SYMPTOMS CHECK
            if (symp.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(d, "Please tell your symptoms before booking.");
                return;
            }
            Date dt = (Date)dateSpin.getValue(); Date tm = (Date)timeSpin.getValue();
            if(Database.bookAppointment(patient.patientId, did, new SimpleDateFormat("yyyy-MM-dd").format(dt), new SimpleDateFormat("HH:mm:00").format(tm), symp.getText())) { JOptionPane.showMessageDialog(d, "Booked! Waiting for Doctor Approval."); d.dispose(); showAppts(); } 
            else JOptionPane.showMessageDialog(d, "Failed.");
        });
        d.add(b); d.setVisible(true);
    }
    void showAppts() {
        String[] c = {"ID", "Doctor", "Date", "Time", "Status"}; DefaultTableModel m = new DefaultTableModel(c, 0);
        for(Appointment a : Database.getPatientAppointments(patient.patientId)) m.addRow(new Object[]{a.id, a.doctorName, a.date, a.time, a.status});
        JTable t = new JTable(m); Theme.styleTable(t); update("My Appointments", new JScrollPane(t));
    }
    void showRx() {
        String[] c = {"Medicine", "Dosage", "Duration", "Prescribed By"}; DefaultTableModel m = new DefaultTableModel(c, 0);
        for(String[] s : Database.getPatientPrescriptions(patient.patientId)) m.addRow(s);
        JTable t = new JTable(m); Theme.styleTable(t); update("My Prescriptions", new JScrollPane(t));
    }
}

class DoctorDashboard extends JFrame {
    Doctor doc; JPanel content;
    DoctorDashboard(Doctor d) {
        this.doc = d; setTitle("Dr. "+d.name); setSize(1000,700); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null); setLayout(new BorderLayout());
        JPanel side = new JPanel(); side.setPreferredSize(new Dimension(240,700)); side.setBackground(Color.WHITE);
        JLabel l = new JLabel("Dr. "+d.name); l.setFont(Theme.FONT_TITLE); l.setForeground(Theme.PRIMARY); l.setBorder(new EmptyBorder(30,10,20,0)); side.add(l);
        
        JButton b1 = new JButton("Appointments"); b1.setFont(Theme.FONT_NAV); b1.setBackground(Color.WHITE); b1.setBorderPainted(false); b1.addActionListener(e->showAppts()); side.add(b1);
        JButton b2 = new JButton("Logout"); b2.setFont(Theme.FONT_NAV); b2.setBackground(Color.WHITE); b2.setForeground(Theme.DANGER); b2.setBorderPainted(false); b2.addActionListener(e->{new LoginScreen(); dispose();}); side.add(b2);
        
        add(side, BorderLayout.WEST); content = new JPanel(new BorderLayout()); content.setBackground(Theme.BG_MAIN); add(content);
        showAppts(); setVisible(true);
    }

    void showAppts() {
        content.removeAll();
        String[] c = {"ID", "Patient", "Date", "Time", "Status", "Action", "Rx", "History"}; DefaultTableModel m = new DefaultTableModel(c,0);
        
        ArrayList<Appointment> appts = Database.getDoctorAppointments(doc.doctorId);
        for(Appointment a : appts) m.addRow(new Object[]{a.id, a.patientName, a.date, a.time, a.status, "Accept/Decline", "Prescribe", "View History"});
        
        JTable t = new JTable(m); Theme.styleTable(t);
        t.addMouseListener(new MouseAdapter(){ 
            public void mouseClicked(MouseEvent e){ 
                int r = t.getSelectedRow(); 
                if(r!=-1) { 
                    int id = (int)m.getValueAt(r,0);
                    int patId = appts.get(r).patientId; 

                    // ISSUE 1 FIX: DOCTOR SEES SYMPTOMS BEFORE ACTION
                    if(t.getSelectedColumn()==5) {
                        Appointment apt = appts.get(r);
                        String msg = "<html><b>Patient:</b> " + apt.patientName + 
                                     "<br><b>Symptoms:</b> " + (apt.symptoms == null || apt.symptoms.isEmpty() ? "None provided" : apt.symptoms) + 
                                     "<br><br>Do you want to accept this appointment?</html>";
                        String[] op = {"Accept", "Decline", "Cancel"};
                        int ch = JOptionPane.showOptionDialog(null, msg, "Review Request", 0, JOptionPane.QUESTION_MESSAGE, null, op, op[0]); 
                        
                        if(ch==0) Database.updateApptStatus(id, "ACCEPTED"); 
                        else if(ch==1) Database.updateApptStatus(id, "DECLINED"); 
                        showAppts(); 
                    }
                    
                    if(t.getSelectedColumn()==6) {
                        if(m.getValueAt(r, 4).toString().equals("ACCEPTED")) {
                            writeRx(id, patId);
                        } else JOptionPane.showMessageDialog(null, "Accept appointment first!");
                    }
                    
                    if(t.getSelectedColumn()==7) {
                        String hist = Database.getMedicalHistory(patId);
                        JTextArea ta = new JTextArea(hist); ta.setFont(Theme.FONT_REGULAR); ta.setEditable(false);
                        JScrollPane sp = new JScrollPane(ta); sp.setPreferredSize(new Dimension(400, 300));
                        JOptionPane.showMessageDialog(null, sp, "Patient Medical History", JOptionPane.INFORMATION_MESSAGE);
                        Database.logAction("Dr. "+doc.name+" viewed history of Patient ID "+patId);
                    }
                } 
            } 
        });
        content.add(new JLabel(" Appointments"){{setFont(Theme.FONT_TITLE);}}, BorderLayout.NORTH); content.add(new JScrollPane(t), BorderLayout.CENTER); content.revalidate(); content.repaint();
    }
    
    void writeRx(int aptId, int patId) {
        JDialog d = new JDialog(this, "Write Prescription", true); d.setSize(400, 400); d.setLocationRelativeTo(this); d.setLayout(null); d.getContentPane().setBackground(Color.WHITE);
        JLabel t = new JLabel("New Prescription"); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.PRIMARY); t.setBounds(20, 10, 300, 30); d.add(t);
        int y=60;
        JTextField mF = f(d, "Medicine:", y); y+=50;
        JTextField dF = f(d, "Dosage:", y); y+=50;
        JTextField fF = f(d, "Frequency:", y); y+=50;
        JTextField duF = f(d, "Duration:", y); y+=60;
        RoundedButton b = new RoundedButton("Save", Theme.PRIMARY, Theme.PRIMARY_HOVER); b.setBounds(100, y, 200, 40);
        b.addActionListener(e -> {
            if(Database.addPrescription(aptId, patId, doc.doctorId, mF.getText(), dF.getText(), fF.getText(), duF.getText())) {
                JOptionPane.showMessageDialog(d, "Prescription Sent!"); d.dispose();
            } else JOptionPane.showMessageDialog(d, "Error sending.");
        });
        d.add(b); d.setVisible(true);
    }
    JTextField f(JDialog d, String l, int y) { JLabel lb=new JLabel(l); lb.setBounds(20, y, 100, 25); d.add(lb); JTextField t=new JTextField(); Theme.styleTextField(t); t.setBounds(120, y, 240, 30); d.add(t); return t; }
}

class AdminLogin extends JFrame {
    AdminLogin() {
        setTitle("Admin Portal"); setSize(400, 300); setLocationRelativeTo(null); setLayout(null); getContentPane().setBackground(Theme.BG_MAIN);
        JLabel l = new JLabel("Admin Login"); l.setFont(Theme.FONT_TITLE); l.setBounds(110, 30, 200, 30); add(l);
        JTextField u = new JTextField("admin@medibridge.com"); Theme.styleTextField(u); u.setBounds(50, 80, 280, 35); add(u);
        JPasswordField p = new JPasswordField("admin123"); Theme.styleTextField(p); p.setBounds(50, 130, 280, 35); add(p);
        RoundedButton b = new RoundedButton("Login", Theme.PRIMARY, Theme.PRIMARY_HOVER); b.setBounds(100, 190, 180, 40);
        b.addActionListener(e -> { if(Database.login(u.getText(), new String(p.getPassword()), "ADMIN") != null) { new AdminDashboard(); dispose(); } else JOptionPane.showMessageDialog(this, "Invalid"); });
        add(b); setVisible(true);
    }
}

class AdminDashboard extends JFrame {
    JPanel contentPanel;
    AdminDashboard() {
        setTitle("Admin Dashboard"); setSize(1000, 650); setDefaultCloseOperation(EXIT_ON_CLOSE); setLocationRelativeTo(null); setLayout(new BorderLayout());
        JPanel side = new JPanel(); side.setPreferredSize(new Dimension(220, 650)); side.setBackground(Color.WHITE); side.setBorder(BorderFactory.createMatteBorder(0,0,0,1,Color.LIGHT_GRAY));
        JLabel t = new JLabel("Admin Panel"); t.setFont(Theme.FONT_TITLE); t.setForeground(Theme.PRIMARY); t.setBorder(new EmptyBorder(30, 10, 30, 10)); side.add(t);
        
        JButton btnDocs = new JButton("Manage Doctors"); btnDocs.setPreferredSize(new Dimension(200, 40)); btnDocs.addActionListener(e -> loadDocs()); side.add(btnDocs);
        JButton btnPats = new JButton("View Patients"); btnPats.setPreferredSize(new Dimension(200, 40)); btnPats.addActionListener(e -> loadPats()); side.add(btnPats);
            
        JButton btnLogs = new JButton("System Logs"); btnLogs.setPreferredSize(new Dimension(200, 40)); btnLogs.addActionListener(e -> loadLogs()); side.add(btnLogs);

        JButton btnOut = new JButton("Logout"); btnOut.setPreferredSize(new Dimension(200, 40)); btnOut.setForeground(Theme.DANGER); btnOut.addActionListener(e -> { new LoginScreen(); dispose(); }); side.add(btnOut);
        
        add(side, BorderLayout.WEST); contentPanel = new JPanel(new BorderLayout()); contentPanel.setBackground(Theme.BG_MAIN); contentPanel.setBorder(new EmptyBorder(20,20,20,20)); add(contentPanel, BorderLayout.CENTER);
        loadDocs(); setVisible(true);
    }
    void loadDocs() {
        contentPanel.removeAll(); contentPanel.add(new JLabel("Doctor Verification") {{ setFont(Theme.FONT_TITLE); }}, BorderLayout.NORTH);
        String[] cols = {"Doc ID", "Name", "Specialization", "Verified", "Action"}; DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model); Theme.styleTable(table);
        try { Connection c = DB.connect(); ResultSet rs = c.createStatement().executeQuery("SELECT d.doctor_id, u.full_name, d.specialization, d.is_verified FROM doctors d JOIN users u ON d.user_id=u.user_id");
        while(rs.next()) model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), "Toggle Verify"}); c.close(); } catch(Exception e){}
        table.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                if(table.getSelectedColumn() == 4) {
                    int r = table.getSelectedRow(); int id = (int)model.getValueAt(r, 0); boolean status = (boolean)model.getValueAt(r, 3);
                    try { 
                        Connection c = DB.connect(); 
                        c.createStatement().executeUpdate("UPDATE doctors SET is_verified=" + (!status) + " WHERE doctor_id="+id); 
                        Database.logAction("Admin changed verification for Doc ID " + id);
                        c.close(); loadDocs(); 
                    } catch(Exception ex){}
                }
            }
        });
        contentPanel.add(new JScrollPane(table), BorderLayout.CENTER); contentPanel.revalidate(); contentPanel.repaint();
    }
    void loadPats() {
        contentPanel.removeAll(); contentPanel.add(new JLabel("Registered Patients") {{ setFont(Theme.FONT_TITLE); }}, BorderLayout.NORTH);
        String[] cols = {"ID", "Name", "Email", "Phone", "Gender"}; DefaultTableModel model = new DefaultTableModel(cols, 0);
        ArrayList<Patient> pats = Database.getAllPatients(); for(Patient p : pats) model.addRow(new Object[]{p.patientId, p.name, p.email, p.phone, p.gender});
        JTable table = new JTable(model); Theme.styleTable(table); contentPanel.add(new JScrollPane(table), BorderLayout.CENTER); contentPanel.revalidate(); contentPanel.repaint();
    }
    
    void loadLogs() {
        contentPanel.removeAll(); 
        contentPanel.add(new JLabel("System Activity Logs") {{ setFont(Theme.FONT_TITLE); }}, BorderLayout.NORTH);
        
        String[] cols = {"Time", "Action Details"}; 
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        
        ArrayList<String[]> logs = Database.getSystemLogs();
        for(String[] s : logs) model.addRow(s);
        
        JTable table = new JTable(model); 
        Theme.styleTable(table); 
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(0).setMaxWidth(200);

        contentPanel.add(new JScrollPane(table), BorderLayout.CENTER); 
        contentPanel.revalidate(); contentPanel.repaint();
    }
}