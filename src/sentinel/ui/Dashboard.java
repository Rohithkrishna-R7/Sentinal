package sentinel.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import sentinel.core.EventStorage;
import sentinel.core.SentinelService;
import sentinel.model.AppEvent;
import sentinel.model.Severity;
import sentinel.model.ThreatAlert;

@SuppressWarnings({"serial","this-escape"})
public class Dashboard extends JFrame implements SentinelService.Listener {
    private static final long serialVersionUID=1L;
    private static final Color BG_DARK=new Color(0x1e,0x1e,0x2e);
    private static final Color BG_PANEL=new Color(0x24,0x24,0x37);
    private static final Color BG_TABLE=new Color(0x18,0x18,0x25);
    private static final Color BG_CARD=new Color(0x2d,0x2d,0x43);
    private static final Color FG_PRIMARY=new Color(0xcd,0xd6,0xf4);
    private static final Color FG_SECONDARY=new Color(0x88,0x8b,0xa5);
    private static final Color FG_ACCENT=new Color(0x89,0xdc,0xeb);
    private static final Color STATUS_RUNNING=new Color(0xa6,0xe3,0xa1);
    private static final Color STATUS_STOPPED=new Color(0xf3,0x8b,0xa8);
    private static final Color ROW_LOW=new Color(0x1e,0x2a,0x3a);
    private static final Color ROW_MEDIUM=new Color(0x3a,0x2e,0x1a);
    private static final Color ROW_HIGH=new Color(0x3a,0x1e,0x1e);
    private static final Color ROW_CRITICAL=new Color(0x4a,0x10,0x10);
    private static final Color TABLE_SELECTION_BG=new Color(0x45,0x47,0x5a);
    private static final Color TABLE_GRID=new Color(0x35,0x35,0x50);
    private static final Font FONT_TITLE=new Font("Segoe UI",Font.BOLD,20);
    private static final Font FONT_SUBTITLE=new Font("Segoe UI",Font.PLAIN,12);
    private static final Font FONT_CARD_VAL=new Font("Segoe UI",Font.BOLD,24);
    private static final Font FONT_CARD_LBL=new Font("Segoe UI",Font.PLAIN,11);
    private static final Font FONT_TABLE=new Font("Segoe UI",Font.PLAIN,12);
    private static final Font FONT_STATUS=new Font("Segoe UI",Font.PLAIN,12);

    private final SentinelService service;
    private JLabel statusDot;
    private JLabel statusLabel;
    private JTextField dirField;
    private JButton startButton;
    private JButton stopButton;
    private JLabel totalEventsValue;
    private JLabel activeThreatsValue;
    private JLabel riskScoreValue;
    private EventTableModel eventModel;
    private ThreatTableModel threatModel;
    private JLabel statusBar;

    public Dashboard(SentinelService service){
        super("Sentinel \u2013 Security Monitor");
        this.service=service;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(1100,700));
        setMinimumSize(new Dimension(800,500));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0,0));
        addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
                service.stop();
                dispose();
                System.exit(0);
            }
        });
        add(buildTopBar(),BorderLayout.NORTH);
        add(buildCenterArea(),BorderLayout.CENTER);
        add(buildStatusBar(),BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        updateButtonStates();
        service.setListener(this);
    }
    private JPanel buildTopBar(){
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        JPanel left=new JPanel();
        left.setBackground(BG_PANEL);
        left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        JLabel title=new JLabel("🛡 Sentinel");
        title.setFont(FONT_TITLE);
        title.setForeground(FG_ACCENT);
        left.add(title);
        JLabel subtitle=new JLabel("Real-time security monitoring  |  KTU S3 CSE OOP Project");
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setForeground(FG_SECONDARY);
        left.add(subtitle);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        right.setBackground(BG_PANEL);
        statusDot=new JLabel("●");
        statusDot.setFont(new Font("Segoe UI",Font.PLAIN,18));
        statusDot.setForeground(STATUS_STOPPED);
        statusLabel=new JLabel("Stopped");
        statusLabel.setFont(FONT_STATUS);
        statusLabel.setForeground(FG_PRIMARY);
        right.add(statusDot);
        right.add(statusLabel);
        panel.add(left,BorderLayout.WEST);
        panel.add(right,BorderLayout.EAST);
        return panel;
    }
    private JPanel buildControlPanel(){
        JPanel panel=new JPanel();
        panel.setBackground(BG_PANEL);
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10,16,10,16));
        JPanel dirRow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        dirRow.setBackground(BG_PANEL);
        JLabel dirLabel=new JLabel("Watch directory:");
        dirLabel.setForeground(FG_PRIMARY);
        dirLabel.setFont(FONT_STATUS);
        dirField=new JTextField(40);
        styleTextField(dirField);
        JButton browseButton=new JButton("Browse…");
        styleButton(browseButton,new Color(0x74,0xc7,0xec));
        browseButton.addActionListener(e->browseForDirectory());
        dirRow.add(dirLabel);
        dirRow.add(dirField);
        dirRow.add(browseButton);
        JPanel btnRow=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        btnRow.setBackground(BG_PANEL);
        startButton=new JButton("▶  Start");
        styleButton(startButton,STATUS_RUNNING);
        startButton.addActionListener(e->onStartClicked());
        stopButton=new JButton("■  Stop");
        styleButton(stopButton,STATUS_STOPPED);
        stopButton.addActionListener(e->onStopClicked());
        btnRow.add(startButton);
        btnRow.add(stopButton);
        panel.add(dirRow);
        panel.add(Box.createVerticalStrut(6));
        panel.add(btnRow);
        return panel;
    }
    private JPanel buildStatsPanel(){
        JPanel panel=new JPanel(new GridLayout(1,3,10,0));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(0,16,0,16));
        totalEventsValue=new JLabel("0");
        activeThreatsValue=new JLabel("0");
        riskScoreValue=new JLabel("0");
        panel.add(buildStatCard("Total Events",totalEventsValue,FG_ACCENT));
        panel.add(buildStatCard("Active Threats",activeThreatsValue,STATUS_STOPPED));
        panel.add(buildStatCard("Risk Score",riskScoreValue,new Color(0xfa,0xb3,0x87)));
        return panel;
    }
    private JPanel buildStatCard(String heading,JLabel valueLabel,Color valueColor){
        JPanel card=new JPanel();
        card.setBackground(BG_CARD);
        card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(14,20,14,20));
        JLabel headingLabel=new JLabel(heading);
        headingLabel.setFont(FONT_CARD_LBL);
        headingLabel.setForeground(FG_SECONDARY);
        headingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setFont(FONT_CARD_VAL);
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(headingLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        return card;
    }
    private JPanel buildEventsTable(){
        eventModel=new EventTableModel();
        JTable table=createStyledTable(eventModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        JScrollPane scroll=new JScrollPane(table);
        styleScrollPane(scroll);
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        JLabel heading=sectionHeading("📋  File & Process Events");
        panel.add(heading,BorderLayout.NORTH);
        panel.add(scroll,BorderLayout.CENTER);
        return panel;
    }
    private JPanel buildThreatsTable(){
        threatModel=new ThreatTableModel();
        JTable table=createStyledTable(threatModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(500);
        SeverityCellRenderer renderer=new SeverityCellRenderer(threatModel);
        for(int i=0;i<threatModel.getColumnCount();i++){
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        JScrollPane scroll=new JScrollPane(table);
        styleScrollPane(scroll);
        JPanel panel=new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        JLabel heading=sectionHeading("⚠  Threat Alerts");
        panel.add(heading,BorderLayout.NORTH);
        panel.add(scroll,BorderLayout.CENTER);
        return panel;
    }
    private JPanel buildStatusBar(){
        JPanel panel=new JPanel(new FlowLayout(FlowLayout.LEFT,12,4));
        panel.setBackground(BG_PANEL);
        statusBar=new JLabel("Ready. Select a directory and click Start.");
        statusBar.setFont(FONT_STATUS);
        statusBar.setForeground(FG_SECONDARY);
        panel.add(statusBar);
        return panel;
    }
    private JPanel buildCenterArea(){
        JPanel wrapper=new JPanel(new BorderLayout(0,10));
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        JPanel top=new JPanel(new BorderLayout(0,8));
        top.setBackground(BG_DARK);
        top.add(buildControlPanel(),BorderLayout.NORTH);
        top.add(buildStatsPanel(),BorderLayout.CENTER);
        JPanel tables=new JPanel(new GridLayout(1,2,10,0));
        tables.setBackground(BG_DARK);
        tables.setBorder(BorderFactory.createEmptyBorder(0,16,10,16));
        tables.add(buildEventsTable());
        tables.add(buildThreatsTable());
        wrapper.add(top,BorderLayout.NORTH);
        wrapper.add(tables,BorderLayout.CENTER);
        return wrapper;
    }
    private void browseForDirectory(){
        JFileChooser chooser=new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select directory to monitor");
        String current=dirField.getText().trim();
        if(!current.isEmpty()){
            File f=new File(current);
            if(f.isDirectory()){
                chooser.setCurrentDirectory(f);
            }
        }
        int result=chooser.showOpenDialog(this);
        if(result==JFileChooser.APPROVE_OPTION){
            dirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    private void onStartClicked(){
        String dirText=dirField.getText().trim();
        if(dirText.isEmpty()){
            setStatusBar("Please enter or browse to a directory first.",true);
            return;
        }
        File dir=new File(dirText);
        if(!dir.isDirectory()){
            setStatusBar("'"+dirText+"' is not a valid directory.",true);
            return;
        }
        if(service.isRunning()){
            return;
        }
        eventModel.clear();
        threatModel.clear();
        updateStatCards();
        Path path=Paths.get(dirText);
        try{
            service.start(path);
        }catch(Exception ex){
            setStatusBar("Error starting monitor: "+ex.getMessage(),true);
        }
        updateButtonStates();
    }
    private void onStopClicked(){
        service.stop();
        updateButtonStates();
    }
    @Override
    public void onEvent(AppEvent event){
        SwingUtilities.invokeLater(()->{
            eventModel.addEvent(event);
            updateStatCards();
        });
    }
    @Override
    public void onThreat(ThreatAlert alert,AppEvent sourceEvent){
        SwingUtilities.invokeLater(()->{
            threatModel.addThreat(alert);
            updateStatCards();
            setStatusBar("⚠ Threat detected: "+alert.getExplanation(),true);
        });
    }
    @Override
    public void onError(String message){
        SwingUtilities.invokeLater(()->setStatusBar("Error: "+message,true));
    }
    @Override
    public void onStatusChange(String status){
        SwingUtilities.invokeLater(()->{
            boolean running="Running".equals(status);
            statusLabel.setText(status);
            statusDot.setForeground(running?STATUS_RUNNING:STATUS_STOPPED);
            updateButtonStates();
            if(running){
                setStatusBar("Monitoring started: "+dirField.getText(),false);
            }else{
                setStatusBar("Monitoring stopped.",false);
            }
        });
    }
    private void updateStatCards(){
        totalEventsValue.setText(String.valueOf(eventModel.getEventCount()));
        activeThreatsValue.setText(String.valueOf(threatModel.getThreatCount()));
        riskScoreValue.setText(String.valueOf(service.getSessionRiskScore()));
    }
    private void updateButtonStates(){
        boolean running=service.isRunning();
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
    }
    private void setStatusBar(String message,boolean isError){
        statusBar.setText(message);
        statusBar.setForeground(isError?new Color(0xf3,0x8b,0xa8):FG_SECONDARY);
    }
    private JLabel sectionHeading(String text){
        JLabel label=new JLabel(text);
        label.setFont(new Font("Segoe UI",Font.BOLD,13));
        label.setForeground(FG_PRIMARY);
        label.setBorder(BorderFactory.createEmptyBorder(6,4,4,0));
        return label;
    }
    private JTable createStyledTable(javax.swing.table.TableModel model){
        JTable table=new JTable(model);
        table.setBackground(BG_TABLE);
        table.setForeground(FG_PRIMARY);
        table.setSelectionBackground(TABLE_SELECTION_BG);
        table.setSelectionForeground(FG_PRIMARY);
        table.setGridColor(TABLE_GRID);
        table.setFont(FONT_TABLE);
        table.setRowHeight(22);
        table.setShowGrid(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setBackground(BG_PANEL);
        table.getTableHeader().setForeground(FG_SECONDARY);
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        return table;
    }
    private void styleScrollPane(JScrollPane scroll){
        scroll.setBackground(BG_TABLE);
        scroll.getViewport().setBackground(BG_TABLE);
        scroll.setBorder(BorderFactory.createLineBorder(TABLE_GRID,1));
    }
    private void styleTextField(JTextField field){
        field.setBackground(new Color(0x30,0x30,0x46));
        field.setForeground(FG_PRIMARY);
        field.setCaretColor(FG_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TABLE_GRID),
                BorderFactory.createEmptyBorder(4,6,4,6)));
        field.setFont(FONT_STATUS);
    }
    private void styleButton(JButton button,Color fgColor){
        button.setBackground(BG_CARD);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fgColor.darker(),1),
                BorderFactory.createEmptyBorder(6,16,6,16)));
        button.setFont(new Font("Segoe UI",Font.BOLD,12));
        button.setOpaque(true);
    }
    private static class SeverityCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID=1L;
        private final ThreatTableModel model;
        SeverityCellRenderer(ThreatTableModel model){
            this.model=model;
        }
        @Override
        public Component getTableCellRendererComponent(
                JTable table,Object value,
                boolean isSelected,boolean hasFocus,
                int row,int column){
            super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
            if(isSelected){
                setBackground(TABLE_SELECTION_BG);
                setForeground(FG_PRIMARY);
                return this;
            }
            if(row>=0&&row<model.getRowCount()){
                ThreatAlert alert=model.getAlertAt(row);
                Severity severity=alert.getSeverity();
                Color bg;
                switch(severity){
                    case LOW: bg=ROW_LOW; break;
                    case MEDIUM: bg=ROW_MEDIUM; break;
                    case HIGH: bg=ROW_HIGH; break;
                    case CRITICAL: bg=ROW_CRITICAL; break;
                    default: bg=BG_TABLE; break;
                }
                setBackground(bg);
                setForeground(FG_PRIMARY);
            }
            return this;
        }
    }
}
