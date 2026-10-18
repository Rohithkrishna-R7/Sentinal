package sentinel.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import sentinel.model.AppEvent;

@SuppressWarnings("serial")
public class EventTableModel extends AbstractTableModel {
    private static final long serialVersionUID=1L;
    private static final int MAX_ROWS=200;
    private static final String[] COLUMNS={"Time","Type","Description","Source"};
    private final List<AppEvent> events=new ArrayList<>();
    private final SimpleDateFormat timeFormat=new SimpleDateFormat("HH:mm:ss");
    public void addEvent(AppEvent event){
        if(events.size()>=MAX_ROWS){
            events.remove(0);
            fireTableRowsDeleted(0,0);
        }
        events.add(event);
        int lastRow=events.size()-1;
        fireTableRowsInserted(lastRow,lastRow);
    }
    public void clear(){
        int size=events.size();
        if(size>0){
            events.clear();
            fireTableRowsDeleted(0,size-1);
        }
    }
    public int getEventCount(){
        return events.size();
    }
    @Override
    public int getRowCount(){
        return events.size();
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
        AppEvent event=events.get(row);
        switch(column){
            case 0: return timeFormat.format(Date.from(event.getTimestamp()));
            case 1: return event.getEventType();
            case 2: return event.getDescription();
            case 3: return event.getSource();
            default: return "";
        }
    }
}
