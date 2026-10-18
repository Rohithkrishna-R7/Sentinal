package sentinel;

import java.awt.Color;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import sentinel.core.EventStorage;
import sentinel.core.SentinelService;
import sentinel.ui.Dashboard;

public class Main {
    private static final String CSV_PATH="data/events.csv";
    public static void main(String[] args){
        applyDarkTheme();
        SwingUtilities.invokeLater(()->{
            EventStorage storage=new EventStorage(CSV_PATH,msg->System.err.println("[EventStorage] "+msg));
            SentinelService service=new SentinelService(storage);
            Dashboard dashboard=new Dashboard(service);
            dashboard.setVisible(true);
        });
    }
    private static void applyDarkTheme(){
        Color bgDark=new Color(0x1e,0x1e,0x2e);
        Color bgPanel=new Color(0x24,0x24,0x37);
        Color bgTable=new Color(0x18,0x18,0x25);
        Color fgPrimary=new Color(0xcd,0xd6,0xf4);
        Color fgSecond=new Color(0x88,0x8b,0xa5);
        Color selBg=new Color(0x45,0x47,0x5a);
        Color border=new Color(0x35,0x35,0x50);
        UIManager.put("Panel.background",bgDark);
        UIManager.put("OptionPane.background",bgDark);
        UIManager.put("OptionPane.messageForeground",fgPrimary);
        UIManager.put("Button.background",bgPanel);
        UIManager.put("Button.foreground",fgPrimary);
        UIManager.put("Button.border",BorderFactory_make(border));
        UIManager.put("TextField.background",new Color(0x30,0x30,0x46));
        UIManager.put("TextField.foreground",fgPrimary);
        UIManager.put("TextField.caretForeground",fgPrimary);
        UIManager.put("TextField.selectionBackground",selBg);
        UIManager.put("TextField.selectionForeground",fgPrimary);
        UIManager.put("Table.background",bgTable);
        UIManager.put("Table.foreground",fgPrimary);
        UIManager.put("Table.gridColor",border);
        UIManager.put("Table.selectionBackground",selBg);
        UIManager.put("Table.selectionForeground",fgPrimary);
        UIManager.put("TableHeader.background",bgPanel);
        UIManager.put("TableHeader.foreground",fgSecond);
        UIManager.put("ScrollPane.background",bgTable);
        UIManager.put("Viewport.background",bgTable);
        UIManager.put("Label.foreground",fgPrimary);
        UIManager.put("FileChooser.background",bgDark);
        UIManager.put("FileChooser.foreground",fgPrimary);
        UIManager.put("ToolTip.background",bgPanel);
        UIManager.put("ToolTip.foreground",fgPrimary);
    }
    private static javax.swing.border.Border BorderFactory_make(Color c){
        return javax.swing.BorderFactory.createLineBorder(c);
    }
}
