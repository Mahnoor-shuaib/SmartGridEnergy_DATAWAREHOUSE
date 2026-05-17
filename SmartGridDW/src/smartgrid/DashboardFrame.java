package smartgrid;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class DashboardFrame extends JFrame {

    private static final Color C_BG          = new Color(245, 243, 238);
    private static final Color C_SURFACE     = new Color(255, 255, 255);
    private static final Color C_SURFACE2    = new Color(238, 236, 230);
    private static final Color C_BORDER      = new Color(210, 207, 198);
    private static final Color C_PRIMARY     = new Color(22,  101,  52);
    private static final Color C_PRIMARY_LT  = new Color(34,  139,  75);
    private static final Color C_PRIMARY_PAL = new Color(220, 240, 228);
    private static final Color C_PRIMARY_TIN = new Color(240, 249, 244);
    private static final Color C_AMBER       = new Color(180, 110,  10);
    private static final Color C_AMBER_PAL   = new Color(254, 243, 199);
    private static final Color C_RED         = new Color(185,  28,  28);
    private static final Color C_RED_PAL     = new Color(254, 226, 226);
    private static final Color C_BLUE        = new Color(29,   78, 216);
    private static final Color C_BLUE_PAL    = new Color(219, 234, 254);
    private static final Color C_TEXT1       = new Color(28,   30,  33);
    private static final Color C_TEXT2       = new Color(71,   85, 105);
    private static final Color C_TEXT3       = new Color(148, 163, 184);
    private static final Color C_NAV         = new Color(22,  101,  52);
    private static final Color C_NAV2        = new Color(15,   80,  38);
    private static final Color TBL_HDR       = new Color(22,  101,  52);
    private static final Color TBL_ROW       = new Color(255, 255, 255);
    private static final Color TBL_ALT       = new Color(242, 249, 245);
    private static final Color TBL_SEL       = new Color(187, 229, 207);
    private static final Color TBL_SEL_FG    = new Color(15,   70,  35);

    // Fonts
    private static final Font F_H1    = new Font("Georgia",  Font.BOLD,  22);
    private static final Font F_H2    = new Font("Georgia",  Font.BOLD,  16);
    private static final Font F_STAT  = new Font("Georgia",  Font.BOLD,  26);
    private static final Font F_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font F_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_TINY  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_MONO  = new Font("Consolas", Font.PLAIN, 13);
    private static final Font F_NAV   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_NAVB  = new Font("Segoe UI", Font.BOLD,  13);

    // Oracle Compatible Queries
    private static final String[] Q_NAMES = {
        "Year-over-Year Growth", "Top 10 High Consumers",
        "Peak vs Off-Peak Analysis", "Revenue by Tariff & City",
        "Grid Station Load Analysis"
    };
    
    private static final String[] QUERIES = {
        // Query 0: YoY Growth (Oracle compatible - no LIMIT)
        "SELECT" +
               "    l.city, " +
               "    t.year, " +
               "    ROUND(SUM(f.kwh_consumed), 2) AS total_kwh, " +
               "    LAG(ROUND(SUM(f.kwh_consumed), 2)) OVER (PARTITION BY l.city ORDER BY t.year) AS prev_year_kwh, " +
               "    ROUND(" +
               "        (SUM(f.kwh_consumed) - LAG(SUM(f.kwh_consumed)) OVER (PARTITION BY l.city ORDER BY t.year)) " +
               "        / NULLIF(LAG(SUM(f.kwh_consumed)) OVER (PARTITION BY l.city ORDER BY t.year), 0) * 100, " +
               "        2 " +
               "    ) AS yoy_growth_pct " +
               "FROM FACT_ENERGY_CONSUMPTION f " +
               "JOIN DIM_TIME t ON f.time_id = t.time_id "  +
               "JOIN DIM_LOCATION l ON f.location_id = l.location_id " +
               "GROUP BY l.city, t.year " +
               "ORDER BY l.city, t.year ",
        
        // Query 1: Top 10 Consumers (Oracle FETCH FIRST)
        "SELECT m.meter_code, m.consumer_type, l.city, " +
        "ROUND(SUM(f.kwh_consumed),2) AS total_kwh, ROUND(SUM(f.cost_pkr),2) AS total_cost_pkr " +
        "FROM FACT_ENERGY_CONSUMPTION f " +
        "JOIN DIM_METER m ON f.meter_id = m.meter_id " +
        "JOIN DIM_LOCATION l ON f.location_id = l.location_id " +
        "GROUP BY m.meter_code, m.consumer_type, l.city ORDER BY total_kwh DESC " +
        "FETCH FIRST 10 ROWS ONLY",
        
        // Query 2: Peak vs Off-Peak
        "SELECT t.season, t.day_type, " +
        "CASE WHEN t.hour BETWEEN 18 AND 22 THEN 'Peak Hour' " +
        "WHEN t.hour BETWEEN 6 AND 9 THEN 'Morning Rush' ELSE 'Off-Peak' END AS time_slot, " +
        "ROUND(SUM(f.kwh_consumed),2) AS total_kwh, ROUND(AVG(f.peak_demand_kw),2) AS avg_peak_kw " +
        "FROM FACT_ENERGY_CONSUMPTION f JOIN DIM_TIME t ON f.time_id = t.time_id " +
        "GROUP BY t.season, t.day_type, " +
        "CASE WHEN t.hour BETWEEN 18 AND 22 THEN 'Peak Hour' " +
        "WHEN t.hour BETWEEN 6 AND 9 THEN 'Morning Rush' ELSE 'Off-Peak' END ORDER BY total_kwh DESC",
        
        // Query 3: Revenue by Tariff & City
        "SELECT l.city, tr.consumer_category, tr.tariff_name, " +
        "ROUND(SUM(f.kwh_consumed),2) AS total_kwh, ROUND(SUM(f.cost_pkr),2) AS revenue_pkr " +
        "FROM FACT_ENERGY_CONSUMPTION f " +
        "JOIN DIM_LOCATION l ON f.location_id = l.location_id " +
        "JOIN DIM_TARIFF tr ON f.tariff_id = tr.tariff_id " +
        "GROUP BY l.city, tr.consumer_category, tr.tariff_name ORDER BY revenue_pkr DESC",
        
        // Query 4: Grid Station Load Analysis
        "SELECT gs.station_name, gs.capacity_mw, gs.city, " +
        "ROUND(SUM(f.kwh_consumed),2) AS delivered_kwh, " +
        "SUM(f.outage_minutes) AS outage_min, ROUND(AVG(f.power_factor),3) AS power_factor " +
        "FROM FACT_ENERGY_CONSUMPTION f " +
        "JOIN DIM_GRID_STATION gs ON f.station_id = gs.station_id " +
        "GROUP BY gs.station_name, gs.capacity_mw, gs.city ORDER BY delivered_kwh DESC"
    };

    // State
    private JPanel  contentArea;
    private JLabel  statusLbl;
    private JButton activeTab = null;

    public DashboardFrame() {
        setTitle("Smart Grid Energy Consumption — Data Warehouse");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 820);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout(0, 0));
        add(buildTopNav(),    BorderLayout.NORTH);
        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(C_BG);
        add(contentArea,      BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        showHome();
    }

    // TOP NAV
    private JPanel buildTopNav() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(C_NAV);
        nav.setPreferredSize(new Dimension(0, 58));
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, C_NAV2));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setBackground(C_NAV);
        left.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        JPanel logoBx = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255,255,255,28));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Georgia",Font.BOLD,16));
                FontMetrics fm = g2.getFontMetrics();
                String s = "\u26A1";
                g2.drawString(s,(getWidth()-fm.stringWidth(s))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2-1);
            }
        };
        logoBx.setOpaque(false); logoBx.setPreferredSize(new Dimension(38,38));

        JPanel tc = new JPanel(); tc.setLayout(new BoxLayout(tc,BoxLayout.Y_AXIS));
        tc.setBackground(C_NAV); tc.setBorder(BorderFactory.createEmptyBorder(0,10,0,30));
        JLabel t1 = new JLabel("Smart Grid Energy DW");
        t1.setFont(new Font("Georgia",Font.BOLD,14)); t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("Data Warehouse Analytics");
        t2.setFont(F_TINY); t2.setForeground(new Color(167,218,185));
        tc.add(t1); tc.add(t2);
        left.add(logoBx); left.add(tc);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        tabs.setBackground(C_NAV);

        JButton btnHome  = navTab("Home");
        JButton btnMeter = navTab("Meters");
        JButton btnLoc   = navTab("Locations");
        JButton btnTime  = navTab("Time");
        JButton btnSt    = navTab("Grid Stations");
        JButton btnTar   = navTab("Tariffs");
        JButton btnFact  = navTab("Fact Table");
        JButton btnView  = navTab("Energy View");
        JButton btnQuery = navTab("Query Analyzer");

        btnHome .addActionListener(e->{ setTab(btnHome);  showHome(); });
        btnMeter.addActionListener(e->{ setTab(btnMeter); showTablePage("SELECT * FROM DIM_METER WHERE ROWNUM <= 200","Meters"); });
        btnLoc  .addActionListener(e->{ setTab(btnLoc);   showTablePage("SELECT * FROM DIM_LOCATION WHERE ROWNUM <= 200","Locations"); });
        btnTime .addActionListener(e->{ setTab(btnTime);  showTablePage("SELECT * FROM DIM_TIME WHERE ROWNUM <= 200","Time Dimension"); });
        btnSt   .addActionListener(e->{ setTab(btnSt);    showTablePage("SELECT * FROM DIM_GRID_STATION","Grid Stations"); });
        btnTar  .addActionListener(e->{ setTab(btnTar);   showTablePage("SELECT * FROM DIM_TARIFF","Tariffs"); });
        btnFact .addActionListener(e->{ setTab(btnFact);  showTablePage("SELECT * FROM FACT_ENERGY_CONSUMPTION WHERE ROWNUM <= 200","Fact Table"); });
        btnView .addActionListener(e->{ setTab(btnView);  showTablePage("SELECT * FROM VW_ENERGY_FULL WHERE ROWNUM <= 200","Full Energy View"); });
        btnQuery.addActionListener(e->{ setTab(btnQuery); showQueryPage(0); });

        for (JButton b : new JButton[]{btnHome,btnMeter,btnLoc,btnTime,btnSt,btnTar,btnFact,btnView,btnQuery})
            tabs.add(b);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setBackground(C_NAV);
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT,6,3));
        badge.setBackground(new Color(15,75,38));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60,160,100),1),
            BorderFactory.createEmptyBorder(2,10,2,10)));
        JLabel dot = new JLabel("●"); dot.setFont(F_TINY); dot.setForeground(new Color(74,222,128));
        JLabel dbl = new JLabel("SmartGridDW"); dbl.setFont(F_TINY); dbl.setForeground(new Color(187,247,208));
        badge.add(dot); badge.add(dbl);
        right.add(badge);

        setTab(btnHome);
        nav.add(left,  BorderLayout.WEST);
        nav.add(tabs,  BorderLayout.CENTER);
        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    private JButton navTab(String text) {
        JButton b = new JButton(text) {
            boolean h=false;
            { addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){h=true;repaint();}
                public void mouseExited(MouseEvent e){h=false;repaint();}
            }); }
            protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sel=(this==activeTab);
                if (sel) {
                    g2.setColor(new Color(255,255,255,20));
                    g2.fillRoundRect(2,5,getWidth()-4,getHeight()-9,6,6);
                    g2.setColor(new Color(134,239,172));
                    g2.fillRect(4,getHeight()-3,getWidth()-8,3);
                } else if (h) {
                    g2.setColor(new Color(255,255,255,12));
                    g2.fillRoundRect(2,5,getWidth()-4,getHeight()-9,6,6);
                }
                g2.setFont(sel?F_NAVB:F_NAV);
                g2.setColor(sel?Color.WHITE:new Color(187,230,205));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2-1);
            }
        };
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        FontMetrics fm = b.getFontMetrics(F_NAV);
        b.setPreferredSize(new Dimension(fm.stringWidth(text)+30, 58));
        return b;
    }

    private void setTab(JButton b){ activeTab=b; repaint(); }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        bar.setBackground(C_SURFACE2); bar.setPreferredSize(new Dimension(0,26));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,C_BORDER));
        JLabel dot = new JLabel("●"); dot.setFont(F_TINY); dot.setForeground(C_PRIMARY_LT);
        statusLbl = new JLabel("Ready — Connected to SmartGridDW");
        statusLbl.setFont(F_TINY); statusLbl.setForeground(C_TEXT2);
        bar.add(dot); bar.add(statusLbl);
        return bar;
    }

    private void setStatus(String msg, Color col) { statusLbl.setText(msg); statusLbl.setForeground(col); }

    // HOME PAGE
    private void showHome() {
        contentArea.removeAll();
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(C_BG);
        root.setBorder(BorderFactory.createEmptyBorder(28,32,28,32));

        JPanel hero = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_PRIMARY); g2.fillRoundRect(0,0,getWidth(),getHeight(),14,14);
                g2.setColor(new Color(255,255,255,7));
                for (int i=-getHeight();i<getWidth()+getHeight();i+=18)
                    g2.drawLine(i,0,i+getHeight(),getHeight());
            }
        };
        hero.setOpaque(false); hero.setAlignmentX(LEFT_ALIGNMENT);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,108));
        hero.setBorder(BorderFactory.createEmptyBorder(22,28,22,28));

        JPanel hL = new JPanel(); hL.setLayout(new BoxLayout(hL,BoxLayout.Y_AXIS)); hL.setOpaque(false);
        JLabel hl1 = new JLabel("Smart Grid Energy Consumption");
        hl1.setFont(F_H1); hl1.setForeground(Color.WHITE);
        JLabel hl2 = new JLabel("Data Warehouse  \u2022  Star Schema  \u2022  Oracle Analytics");
        hl2.setFont(F_BODY); hl2.setForeground(new Color(167,218,185));
        hL.add(hl1); hL.add(Box.createVerticalStrut(5)); hL.add(hl2);

        JPanel hR = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); hR.setOpaque(false);
        hR.add(pill("Star Schema",new Color(187,247,208),new Color(20,90,45)));
        hR.add(pill("5 Dimensions",new Color(187,247,208),new Color(20,90,45)));
        hR.add(pill("OLAP Ready",new Color(187,247,208),new Color(20,90,45)));
        hero.add(hL,BorderLayout.WEST); hero.add(hR,BorderLayout.EAST);

        root.add(hero); root.add(Box.createVerticalStrut(24));

        // KPI cards
        JPanel kpiRow = new JPanel(new GridLayout(1,4,14,0));
        kpiRow.setBackground(C_BG); kpiRow.setAlignmentX(LEFT_ALIGNMENT);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,108));
        
        // Oracle compatible scalar queries
        kpiRow.add(kpi("Total Consumption",
            fetchScalar("SELECT ROUND(SUM(kwh_consumed)/1000,2) || ' MWh' FROM FACT_ENERGY_CONSUMPTION"),
            "kWh delivered",  C_PRIMARY, C_PRIMARY_PAL, C_PRIMARY_TIN));
        kpiRow.add(kpi("Billed Revenue",
            fetchScalar("SELECT 'Rs ' || TO_CHAR(ROUND(SUM(cost_pkr),0)) FROM FACT_ENERGY_CONSUMPTION"),
            "Total billing",  C_AMBER,   C_AMBER_PAL,   new Color(254,252,232)));
        kpiRow.add(kpi("Active Meters",
            fetchScalar("SELECT COUNT(*) FROM DIM_METER WHERE is_active = 1"),
            "Smart meters",   C_BLUE,    C_BLUE_PAL,    new Color(239,246,255)));
        kpiRow.add(kpi("Avg Power Factor",
            fetchScalar("SELECT ROUND(AVG(power_factor),3) FROM FACT_ENERGY_CONSUMPTION"),
            "Grid efficiency",C_RED,     C_RED_PAL,     new Color(255,241,241)));
        root.add(kpiRow); root.add(Box.createVerticalStrut(26));

        // Two-column section
        JPanel twoCol = new JPanel(new GridLayout(1,2,18,0));
        twoCol.setBackground(C_BG); twoCol.setAlignmentX(LEFT_ALIGNMENT);
        twoCol.setMaximumSize(new Dimension(Integer.MAX_VALUE,310));

        JPanel qCard = whiteCard("Quick Analysis Queries");
        JPanel qList = new JPanel(); qList.setLayout(new BoxLayout(qList,BoxLayout.Y_AXIS));
        qList.setBackground(C_SURFACE);
        for (int i=0;i<Q_NAMES.length;i++) {
            final int idx=i;
            JPanel qr = queryRow(i+1,Q_NAMES[i]);
            qr.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ showQueryPage(idx); }});
            qList.add(qr);
            if (i<Q_NAMES.length-1) qList.add(divider());
        }
        qCard.add(qList,BorderLayout.CENTER);

        JPanel sCard = whiteCard("Schema Overview");
        JPanel sList = new JPanel(); sList.setLayout(new BoxLayout(sList,BoxLayout.Y_AXIS));
        sList.setBackground(C_SURFACE);
        String[][] tbls = {
            {"FACT","FACT_ENERGY_CONSUMPTION","16 cols"},
            {"DIM","DIM_TIME","11 cols"},{"DIM","DIM_METER","7 cols"},
            {"DIM","DIM_LOCATION","8 cols"},{"DIM","DIM_GRID_STATION","8 cols"},
            {"DIM","DIM_TARIFF","8 cols"}
        };
        for (String[] t:tbls){ sList.add(schemaRow(t[0],t[1],t[2])); sList.add(divider()); }
        sCard.add(sList,BorderLayout.CENTER);

        twoCol.add(qCard); twoCol.add(sCard);
        root.add(twoCol); root.add(Box.createVerticalStrut(26));

        JLabel rLbl = new JLabel("Recent Consumption Records");
        rLbl.setFont(F_H2); rLbl.setForeground(C_TEXT1); rLbl.setAlignmentX(LEFT_ALIGNMENT);
        root.add(rLbl); root.add(Box.createVerticalStrut(10));

        JPanel tblCard = new JPanel(new BorderLayout());
        tblCard.setBackground(C_SURFACE);
        tblCard.setBorder(BorderFactory.createLineBorder(C_BORDER));
        tblCard.setAlignmentX(LEFT_ALIGNMENT);
        tblCard.setMaximumSize(new Dimension(Integer.MAX_VALUE,340));

        JLabel tHdr = new JLabel("  Recent Fact Records");
        tHdr.setFont(F_NAVB); tHdr.setForeground(Color.WHITE); tHdr.setBackground(TBL_HDR); tHdr.setOpaque(true);
        tHdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,2,0,C_NAV2),
            BorderFactory.createEmptyBorder(9,14,9,14)));

        tblCard.add(tHdr, BorderLayout.NORTH);
        tblCard.add(buildTable(
            "SELECT f.consumption_id AS ID, l.city AS City, m.meter_code AS Meter, " +
            "m.consumer_type AS Type, t.season AS Season, " +
            "ROUND(f.kwh_consumed,3) AS kWh, ROUND(f.cost_pkr,2) AS \"Cost PKR\", " +
            "f.outage_minutes AS \"Outage Min\" " +
            "FROM FACT_ENERGY_CONSUMPTION f " +
            "JOIN DIM_LOCATION l ON f.location_id = l.location_id " +
            "JOIN DIM_METER m ON f.meter_id = m.meter_id " +
            "JOIN DIM_TIME t ON f.time_id = t.time_id WHERE ROWNUM <= 12"),
            BorderLayout.CENTER);
        root.add(tblCard);

        JScrollPane sp = new JScrollPane(root);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(C_BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        contentArea.add(sp,BorderLayout.CENTER);
        contentArea.revalidate(); contentArea.repaint();
    }

    // QUERY PAGE
    private void showQueryPage(int pre) {
        contentArea.removeAll();
        JPanel root = new JPanel(new BorderLayout(0,0));
        root.setBackground(C_BG);
        root.setBorder(BorderFactory.createEmptyBorder(28,32,28,32));

        JPanel hdrPnl = new JPanel(); hdrPnl.setLayout(new BoxLayout(hdrPnl,BoxLayout.Y_AXIS));
        hdrPnl.setBackground(C_BG);
        JLabel pg  = new JLabel("Query Analyzer"); pg.setFont(F_H1); pg.setForeground(C_TEXT1);
        JLabel sub = new JLabel("Execute built-in OLAP queries or write custom SQL against SmartGridDW");
        sub.setFont(F_BODY); sub.setForeground(C_TEXT2);
        hdrPnl.add(pg); hdrPnl.add(Box.createVerticalStrut(4)); hdrPnl.add(sub);
        hdrPnl.add(Box.createVerticalStrut(18));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT,10,8));
        ctrl.setBackground(C_SURFACE);
        ctrl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_BORDER),
            BorderFactory.createEmptyBorder(4,8,4,8)));

        JLabel ql = new JLabel("Select Query:"); ql.setFont(F_BOLD); ql.setForeground(C_TEXT2);
        JComboBox<String> combo = styledCombo(Q_NAMES);
        combo.setSelectedIndex(pre); combo.setPreferredSize(new Dimension(260,34));
        JButton btnLoad = styledBtn("Load",   C_PRIMARY,    Color.WHITE, C_NAV2);
        JButton btnRun  = styledBtn("Run SQL",C_PRIMARY_LT, Color.WHITE, new Color(20,110,55));
        ctrl.add(ql); ctrl.add(combo); ctrl.add(Box.createHorizontalStrut(4));
        ctrl.add(btnLoad); ctrl.add(btnRun);

        JTextArea editor = new JTextArea(QUERIES[pre]);
        editor.setFont(F_MONO);
        editor.setBackground(new Color(248,248,244));
        editor.setForeground(new Color(22,101,52));
        editor.setCaretColor(C_PRIMARY);
        editor.setSelectionColor(C_PRIMARY_PAL);
        editor.setLineWrap(true); editor.setWrapStyleWord(true);
        editor.setBorder(BorderFactory.createEmptyBorder(12,14,12,14));

        JScrollPane edSp = new JScrollPane(editor);
        edSp.setBorder(BorderFactory.createEmptyBorder());
        edSp.setPreferredSize(new Dimension(0,100));

        JPanel edCard = new JPanel(new BorderLayout());
        edCard.setBackground(C_SURFACE);
        edCard.setBorder(BorderFactory.createLineBorder(C_BORDER));
        JLabel edHdr = new JLabel("  SQL Statement");
        edHdr.setFont(new Font("Segoe UI",Font.BOLD,11)); edHdr.setForeground(C_TEXT3);
        edHdr.setBackground(C_SURFACE2); edHdr.setOpaque(true);
        edHdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,C_BORDER),
            BorderFactory.createEmptyBorder(6,12,6,12)));
        edCard.add(edHdr,BorderLayout.NORTH); edCard.add(edSp,BorderLayout.CENTER);

        JTable[] tRef   = { new JTable() };
        JLabel[] rHdr   = { new JLabel("  Results") };
        rHdr[0].setFont(new Font("Segoe UI",Font.BOLD,11)); rHdr[0].setForeground(Color.WHITE);
        rHdr[0].setBackground(TBL_HDR); rHdr[0].setOpaque(true);
        rHdr[0].setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,2,0,C_NAV2),
            BorderFactory.createEmptyBorder(8,14,8,14)));

        JScrollPane[] rSp = { styledTableScroll(tRef[0]) };
        JPanel resCard = new JPanel(new BorderLayout());
        resCard.setBackground(C_SURFACE); resCard.setBorder(BorderFactory.createLineBorder(C_BORDER));
        resCard.add(rHdr[0],BorderLayout.NORTH); resCard.add(rSp[0],BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, edCard, resCard);
        split.setDividerLocation(130); split.setDividerSize(5);
        split.setBackground(C_BG); split.setBorder(BorderFactory.createEmptyBorder());

        Runnable runQ = () -> {
            String sql = editor.getText().trim(); if (sql.isEmpty()) return;
            setStatus("Executing query...", C_AMBER);
            try (Connection c = DBConnection.getConnection();
                 Statement s = c.createStatement(); 
                 ResultSet rs = s.executeQuery(sql)) {
                DefaultTableModel m = buildModel(rs);
                tRef[0].setModel(m); 
                applyTableStyle(tRef[0]);
                rHdr[0].setText("  Results  \u2014  "+m.getRowCount()+" rows");
                setStatus("Done  \u2014  "+m.getRowCount()+" rows returned",C_PRIMARY_LT);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this,"<html><b>SQL Error</b><br>"+ex.getMessage()+"</html>",
                    "Error",JOptionPane.ERROR_MESSAGE);
                setStatus("Error: "+ex.getMessage(),C_RED);
            }
        };

        combo.addActionListener(e->editor.setText(QUERIES[combo.getSelectedIndex()]));
        btnLoad.addActionListener(e->editor.setText(QUERIES[combo.getSelectedIndex()]));
        btnRun.addActionListener(e->runQ.run());

        JPanel top = new JPanel(new BorderLayout(0,10)); top.setBackground(C_BG);
        top.add(hdrPnl,BorderLayout.NORTH); top.add(ctrl,BorderLayout.CENTER);
        root.add(top,BorderLayout.NORTH); root.add(split,BorderLayout.CENTER);
        contentArea.add(root,BorderLayout.CENTER);
        contentArea.revalidate(); contentArea.repaint();
        SwingUtilities.invokeLater(runQ::run);
    }

    // TABLE PAGE
  private void showTablePage(String sql, String title) {
    contentArea.removeAll();
    JPanel root = new JPanel(new BorderLayout(0,0)); root.setBackground(C_BG);
    root.setBorder(BorderFactory.createEmptyBorder(28,32,28,32));

    JPanel hdr = new JPanel(); hdr.setLayout(new BoxLayout(hdr,BoxLayout.Y_AXIS));
    hdr.setBackground(C_BG);
    JLabel pg  = new JLabel(title); pg.setFont(F_H1); pg.setForeground(C_TEXT1);
    JLabel sub = new JLabel("Dimension table data from SmartGridDW");
    sub.setFont(F_BODY); sub.setForeground(C_TEXT2);
    hdr.add(pg); hdr.add(Box.createVerticalStrut(4)); hdr.add(sub);
    hdr.add(Box.createVerticalStrut(18));

    JPanel card = new JPanel(new BorderLayout()); card.setBackground(C_SURFACE);
    card.setBorder(BorderFactory.createLineBorder(C_BORDER));
    JLabel ch = new JLabel("  "+title); ch.setFont(F_NAVB); ch.setForeground(Color.WHITE);
    ch.setBackground(TBL_HDR); ch.setOpaque(true);
    ch.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0,0,2,0,C_NAV2),
        BorderFactory.createEmptyBorder(10,14,10,14)));
        
    JScrollPane tableScrollPane = buildTable(sql);
    JTable table = (JTable) tableScrollPane.getViewport().getView();
    if (table.getTableHeader() != null) {
        table.getTableHeader().setBackground(TBL_HDR);
        table.getTableHeader().setForeground(Color.BLACK);  // ✅ WHITE ki jagah BLACK
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, C_NAV2));
        table.getTableHeader().setVisible(true);
    }
        
    card.add(ch, BorderLayout.NORTH); 
    card.add(tableScrollPane, BorderLayout.CENTER);
    root.add(hdr, BorderLayout.NORTH); 
    root.add(card, BorderLayout.CENTER);
    contentArea.add(root, BorderLayout.CENTER);
    contentArea.revalidate(); 
    contentArea.repaint();
    setStatus("Loaded: "+title, C_PRIMARY_LT);
}

    // COMPONENT HELPERS
    private JPanel whiteCard(String title) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(C_SURFACE);
        p.setBorder(BorderFactory.createLineBorder(C_BORDER));
        JLabel h = new JLabel("  "+title); h.setFont(F_BOLD); h.setForeground(C_TEXT1);
        h.setBackground(C_SURFACE2); h.setOpaque(true);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,C_BORDER),
            BorderFactory.createEmptyBorder(9,14,9,14)));
        p.add(h,BorderLayout.NORTH); return p;
    }

    private JPanel kpi(String lbl, String val, String sub,
                       Color accent, Color paleBg, Color cardBg) {
        JPanel card = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(cardBg); g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(C_BORDER); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                g2.setColor(accent); g2.fillRoundRect(0,0,getWidth(),4,4,4);
            }
        };
        card.setOpaque(false); card.setBorder(BorderFactory.createEmptyBorder(18,18,16,18));
        JPanel inn = new JPanel(); inn.setLayout(new BoxLayout(inn,BoxLayout.Y_AXIS)); inn.setOpaque(false);
        JLabel lb = new JLabel(lbl); lb.setFont(F_SMALL); lb.setForeground(C_TEXT2);
        JLabel vl = new JLabel(val); vl.setFont(F_STAT); vl.setForeground(accent);
        JLabel sl = new JLabel("  "+sub+"  "); sl.setFont(new Font("Segoe UI",Font.PLAIN,10));
        sl.setBackground(paleBg); sl.setForeground(accent); sl.setOpaque(true);
        inn.add(lb); inn.add(Box.createVerticalStrut(5)); inn.add(vl); inn.add(Box.createVerticalStrut(6)); inn.add(sl);
        card.add(inn,BorderLayout.CENTER); return card;
    }

    private JLabel pill(String text, Color bg, Color fg) {
        JLabel l = new JLabel("  "+text+"  "); l.setFont(new Font("Segoe UI",Font.BOLD,11));
        l.setBackground(bg); l.setForeground(fg); l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(fg.getRed(),fg.getGreen(),fg.getBlue(),60)),
            BorderFactory.createEmptyBorder(4,4,4,4)));
        return l;
    }

    private JPanel queryRow(int num, String label) {
        JPanel row = new JPanel(new BorderLayout()) {
            boolean h=false;
            { addMouseListener(new MouseAdapter(){
                public void mouseEntered(MouseEvent e){h=true;repaint();}
                public void mouseExited(MouseEvent e){h=false;repaint();}
            }); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            protected void paintComponent(Graphics g){
                g.setColor(h?C_PRIMARY_TIN:C_SURFACE); g.fillRect(0,0,getWidth(),getHeight());
            }
        };
        row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(9,14,9,14));
        JPanel L=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); L.setOpaque(false);
        JLabel n=new JLabel(String.format("%02d",num));
        n.setFont(new Font("Consolas",Font.BOLD,12)); n.setForeground(C_PRIMARY_LT);
        n.setBackground(C_PRIMARY_PAL); n.setOpaque(true); n.setBorder(BorderFactory.createEmptyBorder(2,7,2,7));
        JLabel t=new JLabel(label); t.setFont(F_BODY); t.setForeground(C_TEXT1);
        L.add(n); L.add(t);
        JLabel a=new JLabel("\u2192"); a.setFont(F_BOLD); a.setForeground(C_TEXT3);
        a.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        row.add(L,BorderLayout.WEST); row.add(a,BorderLayout.EAST);
        return row;
    }

    private JPanel schemaRow(String type, String name, String detail) {
        JPanel row=new JPanel(new BorderLayout()); row.setBackground(C_SURFACE);
        row.setBorder(BorderFactory.createEmptyBorder(8,14,8,14));
        JPanel L=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); L.setBackground(C_SURFACE);
        JLabel badge=new JLabel(type); badge.setFont(new Font("Segoe UI",Font.BOLD,10));
        Color bg=type.equals("FACT")?C_AMBER_PAL:C_PRIMARY_PAL;
        Color fg=type.equals("FACT")?C_AMBER:C_PRIMARY;
        badge.setBackground(bg); badge.setForeground(fg); badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(2,6,2,6));
        JLabel nl=new JLabel(name); nl.setFont(F_BOLD); nl.setForeground(C_TEXT1);
        L.add(badge); L.add(nl);
        JLabel dl=new JLabel(detail); dl.setFont(F_SMALL); dl.setForeground(C_TEXT3);
        row.add(L,BorderLayout.WEST); row.add(dl,BorderLayout.EAST);
        return row;
    }

    private JSeparator divider() {
        JSeparator s=new JSeparator(); s.setForeground(C_BORDER);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE,1)); return s;
    }

    private JScrollPane buildTable(String sql) {
        JTable t=new JTable();
        try (Connection c=DBConnection.getConnection(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery(sql)){
            t.setModel(buildModel(rs)); applyTableStyle(t);
             JTableHeader header = t.getTableHeader();
        if (header != null) {
            header.setBackground(TBL_HDR);
            header.setForeground(Color.BLACK);
            header.setFont(new Font("Segoe UI", Font.BOLD, 12));
            header.setPreferredSize(new Dimension(0, 36));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, C_NAV2));
            header.setVisible(true);
        }
        } catch (SQLException ex) { setStatus("Error: "+ex.getMessage(),C_RED); }
        return styledTableScroll(t);
    }

    private JScrollPane styledTableScroll(JTable t) {
        applyTableStyle(t);
        JTableHeader header = t.getTableHeader();
    if (header != null) {
        header.setBackground(TBL_HDR);
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, C_NAV2));
        header.setVisible(true);
    }
        JScrollPane sp = new JScrollPane(t);
        sp.getViewport().setBackground(TBL_ROW);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    private void applyTableStyle(JTable t) {
        t.setBackground(TBL_ROW); t.setForeground(C_TEXT1); t.setFont(F_SMALL); t.setRowHeight(30);
        t.setGridColor(new Color(220,235,222)); t.setShowVerticalLines(false); t.setShowHorizontalLines(true);
        t.setSelectionBackground(TBL_SEL); t.setSelectionForeground(TBL_SEL_FG);
        t.setIntercellSpacing(new Dimension(0,0)); t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTableHeader th = t.getTableHeader();
        th.setBackground(TBL_HDR);
        th.setForeground(Color.black);
        th.setFont(new Font("Segoe UI",Font.BOLD,11));
        th.setPreferredSize(new Dimension(0,36));
        th.setBorder(BorderFactory.createMatteBorder(0,0,2,0,C_NAV2));
        th.setReorderingAllowed(false);
         th.setVisible(true);  
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable tb,Object val,boolean sel,boolean foc,int row,int col){
                super.getTableCellRendererComponent(tb,val,sel,foc,row,col);
                setFont(F_SMALL); setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
                if(sel){setBackground(TBL_SEL);setForeground(TBL_SEL_FG);}
                else{setBackground(row%2==0?TBL_ROW:TBL_ALT);setForeground(C_TEXT1);}
                return this;
            }
        });
    }

    private DefaultTableModel buildModel(ResultSet rs) throws SQLException {
        ResultSetMetaData m=rs.getMetaData(); int n=m.getColumnCount();
        String[] cols=new String[n];
        for(int i=1;i<=n;i++) cols[i-1]=m.getColumnLabel(i);
        DefaultTableModel model=new DefaultTableModel(cols,0){ public boolean isCellEditable(int r,int c){return false;} };
        while(rs.next()){ Object[] row=new Object[n]; for(int i=1;i<=n;i++) row[i-1]=rs.getObject(i); model.addRow(row); }
        return model;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb=new JComboBox<>(items); cb.setFont(F_BODY); cb.setBackground(C_SURFACE); cb.setForeground(C_TEXT1);
        cb.setRenderer(new DefaultListCellRenderer(){
            public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean sel,boolean foc){
                super.getListCellRendererComponent(l,v,i,sel,foc);
                setBackground(sel?C_PRIMARY_PAL:C_SURFACE); setForeground(sel?C_PRIMARY:C_TEXT1);
                setFont(F_BODY); setBorder(BorderFactory.createEmptyBorder(5,10,5,10)); return this;
            }
        }); return cb;
    }

    private JButton styledBtn(String text, Color bg, Color fg, Color hov) {
        JButton b=new JButton(text){ boolean h=false;
            { addMouseListener(new MouseAdapter(){ public void mouseEntered(MouseEvent e){h=true;repaint();} public void mouseExited(MouseEvent e){h=false;repaint();} }); }
            protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h?hov:bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                g2.setColor(fg); g2.setFont(F_BOLD);
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        b.setOpaque(false); b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.setPreferredSize(new Dimension(110,34));
        return b;
    }

    private String fetchScalar(String sql) {
        try(Connection c=DBConnection.getConnection(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery(sql)){
            if(rs.next()){Object v=rs.getObject(1);return v!=null?v.toString():"—";}
        } catch(SQLException e){return "—";} return "—";
    }

    // MAIN
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new DashboardFrame().setVisible(true));
    }
}