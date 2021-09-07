package com.xu.music.player.player;

/**
 * @author hyacinth
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.out.println("Thread\tgetId\t" + t.getId());
        System.out.println("Thread\tgetName\t" + t.getName());
        System.out.println("Thread\tgetState\t" + t.getState());
        System.out.println("Thread\tisAlive\t" + t.isAlive());
        System.out.println("Thread\tisInterrupted\t" + t.isInterrupted());
        System.out.println("Thread\texception\t" + e.getMessage());
    }

}

