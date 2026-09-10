package com.xu.music.player.lyric;

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/** 保留原生歌词表格，通过上下空行让首尾歌词也能居中。 */
public final class CenteredLyrics {
    private final Table table;
    private final TableColumn textColumn;
    private List<LrcLine> lines = List.of();
    private int active = -1;
    private int padding;

    public CenteredLyrics(Composite parent) {
        table = new Table(parent, SWT.NONE);
        new TableColumn(table, SWT.CENTER);
        textColumn = new TableColumn(table, SWT.CENTER);
        textColumn.setText("歌词");
        table.addListener(SWT.Resize, event -> {
            textColumn.setWidth(Math.max(0, table.getClientArea().width));
            int next = padding(table.getClientArea().height, table.getItemHeight());
            if (next != padding) { padding = next; rebuild(); }
            else center();
        });
        // 用户选中行时不覆盖播放行的蓝色，也不引入系统选择背景。
        table.addListener(SWT.EraseItem, event -> event.detail &= ~SWT.SELECTED);
    }

    public void setLines(List<LrcLine> value) {
        lines = List.copyOf(value);
        active = -1;
        rebuild();
    }

    public String update(double seconds) {
        int next = indexAt(lines, seconds);
        if (next != active) {
            style(active, false);
            active = next;
            style(active, true);
            center();
        }
        return active < 0 ? null : lines.get(active).text();
    }

    private void rebuild() {
        table.setRedraw(false);
        try {
            table.removeAll();
            if (lines.isEmpty()) return;
            for (int i = 0; i < padding; i++) new TableItem(table, SWT.NONE);
            for (LrcLine line : lines) {
                var item = new TableItem(table, SWT.NONE);
                item.setText(new String[]{line.tag(), line.text()});
            }
            for (int i = 0; i < padding + 1; i++) new TableItem(table, SWT.NONE);
            style(active, true);
            center();
        } finally { table.setRedraw(true); }
    }

    private void style(int index, boolean playing) {
        if (index < 0 || index >= lines.size()) return;
        var item = table.getItem(padding + index);
        item.setForeground(playing ? table.getDisplay().getSystemColor(SWT.COLOR_BLUE) : null);
        item.setBackground(playing ? table.getDisplay().getSystemColor(SWT.COLOR_GRAY) : null);
    }

    private void center() {
        if (!lines.isEmpty()) table.setTopIndex(Math.max(0, active));
    }

    static int padding(int height, int rowHeight) {
        return Math.max(0, (int) Math.round((height - rowHeight) / (2.0 * Math.max(1, rowHeight))));
    }

    static int indexAt(List<LrcLine> lines, double seconds) {
        int low = 0, high = lines.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (lines.get(middle).seconds() <= seconds) low = middle + 1;
            else high = middle;
        }
        return low - 1;
    }
}
