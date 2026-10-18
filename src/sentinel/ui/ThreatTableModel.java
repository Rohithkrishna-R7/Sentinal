package sentinel.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import sentinel.model.ThreatAlert;

@SuppressWarnings("serial")
public class ThreatTableModel extends AbstractTableModel {
    private static final long serialVersionUID=1L;
    private static final int MAX_ROWS=200;
    private static final String[] COLUMNS={"Time","Severity","Score","Rule","Explanation"};
    private final List<ThreatAlert> alerts=new ArrayList<>();
    private final SimpleDateFormat timeFormat=new SimpleDateFormat("HH:mm:ss");
    public void addThreat(ThreatAlert alert){
        if(alerts.size()>=MAX_ROWS){
            alerts.remove(0);
            fireTableRowsDeleted(0,0);
        }
        alerts.add(alert);
        int lastRow=alerts.size()-1;
        fireTableRowsInserted(lastRow,lastRow);
    }
    public void clear(){
        int size=alerts.size();
        if(size>0){
            alerts.clear();
            fireTableRowsDeleted(0,size-1);
        }
    }
    public ThreatAlert getAlertAt(int row){
        return alerts.get(row);
    }
    public int getThreatCount(){
        return alerts.size();
    }
    @Override
    public int getRowCount(){
        return alerts.size();
    }
    @Override
    public int getColumnCount(){
        return COLUMNS.length;
    }
    @Override
    public String getColumnName(int column){
        return COLUMNS[column];
    }
    @Override
    public Class<?> getColumnClass(int column){
        return String.class;
    }
    @Override
    public Object getValueAt(int row,int column){
        ThreatAlert alert=alerts.get(row);
        switch(column){
            case 0: return timeFormat.format(Date.from(alert.getTimestamp()));
            case 1: return alert.getSeverity().name();
            case 2: return String.valueOf(alert.getScore());
            case 3: return alert.getRule();
            case 4: return alert.getExplanation();
            default: return "";
        }
    }
}
