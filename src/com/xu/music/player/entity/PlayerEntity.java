package com.xu.music.player.entity;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;

public class PlayerEntity {

    private static Table table;
    private static org.eclipse.swt.widgets.Label text;
    private static ProgressBar bar;
    private static String song;
    private static Composite spectrum;


    public static Composite getSpectrum() {
        return spectrum;
    }

    public static void setSpectrum(Composite spectrum) {
        PlayerEntity.spectrum = spectrum;
    }

    public static Table getTable() {
        return table;
    }

    public static void setTable(Table table) {
        PlayerEntity.table = table;
    }

    public static org.eclipse.swt.widgets.Label getText() {
        return text;
    }

    public static void setText(org.eclipse.swt.widgets.Label text) {
        PlayerEntity.text = text;
    }

    public static ProgressBar getBar() {
        return bar;
    }

    public static void setBar(ProgressBar bar) {
        PlayerEntity.bar = bar;
    }

    public static String getSong() {
        return song;
    }

    public static void setSong(String song) {
        PlayerEntity.song = song;
    }

}
