package com.xu.music.player.download;

import com.xu.music.player.system.Constant;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Asynchronous {

    private long length = 0;
    private int index = 1;

    public static void main(String[] args) throws InterruptedException {
        new Asynchronous().download(object -> {
        }, "http://localhost:8080/WEB/a/a.pdf", "kk");
    }

    public void download(DownloadNotify notify, String url) {
        String name = url.substring(url.lastIndexOf("/") + 1);
        HttpURLConnection connection;
        String newname;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            length = connection.getContentLengthLong();

            File file = new File(Constant.MUSIC_PLAYER_DOWNLOAD_PATH + name);
            while (file.exists()) {
                newname = name.substring(0, name.lastIndexOf(".")) + "[" + index + "]" + name.substring(name.lastIndexOf("."));
                file = new File(Constant.MUSIC_PLAYER_DOWNLOAD_PATH + newname);
                index++;
                name = newname;
            }

            RandomAccessFile raf = new RandomAccessFile(Constant.MUSIC_PLAYER_DOWNLOAD_PATH + name, "rw");
            raf.setLength(length);
            raf.close();
            connection.disconnect();

            task(notify, url, Constant.MUSIC_PLAYER_DOWNLOAD_PATH + name, length);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void download(DownloadNotify notify, String url, String name) {
        name += url.substring(url.lastIndexOf("."));
        String newname;
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            length = connection.getContentLengthLong();

            File file = new File(Constant.MUSIC_PLAYER_DOWNLOAD_PATH + name);
            while (file.exists()) {
                newname = name.substring(0, name.lastIndexOf(".")) + "[" + index + "]" + name.substring(name.lastIndexOf("."));
                file = new File(Constant.MUSIC_PLAYER_DOWNLOAD_PATH + newname);
                index++;
                name = newname;
            }

            RandomAccessFile raf = new RandomAccessFile(Constant.MUSIC_PLAYER_DOWNLOAD_PATH + name, "rw");
            raf.setLength(length);
            raf.close();
            connection.disconnect();

            task(notify, url, Constant.MUSIC_PLAYER_DOWNLOAD_PATH + name, length);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void task(DownloadNotify notify, String url, String path, long length) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(Constant.MUSIC_PLAYER_DOWNLOAD_CORE_POOL_SIZE, Constant.MUSIC_PLAYER_DOWNLOAD_MAX_POOL_SIZE, 10, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(5), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        if (length <= 10 * 1024 * 1024) {
            executor.execute(new DownLoadTask(notify, url, path, 0, length));
        } else {
            for (long i = 0, len = length / Constant.MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD; i <= len; i++) {
                if (i == len && i > 0) {
                    System.out.println("A-->" + length + "\t" + i * Constant.MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD + "--" + i + "--" + length);
                    executor.execute(new DownLoadTask(notify, url, path, i * Constant.MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD, length));
                } else {
                    System.out.println("B-->" + length + "\t" + i * Constant.MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD + "--" + i + "--" + ((i + 1) * Constant.MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD - 1));
                    executor.execute(new DownLoadTask(notify, url, path, i * Constant.MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD, (i + 1) * Constant.MUSIC_PLAYER_DOWNLOAD_FILE_SIZE_PER_THREAD - 1));
                }
            }
        }
        executor.shutdown();
    }

}


class DownLoadTask implements Runnable {

    private DownloadNotify notify;
    private String url;
    private String path;
    private long start;
    private long end;

    public DownLoadTask(DownloadNotify notify, String url, String path, long start, long end) {
        this.notify = notify;
        this.url = url;
        this.path = path;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        BufferedInputStream stream = null;
        RandomAccessFile access = null;
        HttpURLConnection connection = null;
        try {
            access = new RandomAccessFile(path, "rw");
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Range", "bytes=" + this.start + "-" + this.end);
            access.seek(this.start);
            stream = new BufferedInputStream(connection.getInputStream());
            byte[] bt = new byte[10 * 1024];
            int length;
            while ((length = stream.read(bt, 0, bt.length)) != -1) {
                access.write(bt, 0, length);
                if (notify != null) {
                    synchronized (this) {
                        this.notify.result(length);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (access != null) {
                try {
                    access.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

    }
}