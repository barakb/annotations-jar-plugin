package com.github.barakb.ajp;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Created by Barak Bar Orion
 * 5/13/15.
 */
public class JarWriter {
    public static final int BUFFER_SIZE = 10240;
    byte buffer[] = new byte[BUFFER_SIZE];

    private JarOutputStream out;

    public JarWriter(File file) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        out = new JarOutputStream(stream, new Manifest());
    }

    public void write(String name, InputStream is) throws IOException {
        JarEntry jarAdd = new JarEntry(name);
        jarAdd.setTime(System.currentTimeMillis());
        out.putNextEntry(jarAdd);
        while (true) {
            int nRead = is.read(buffer, 0, buffer.length);
            if (nRead <= 0)
                break;
            out.write(buffer, 0, nRead);
        }
        is.close();;
    }


    public void close() throws IOException {
        if(out != null){
            out.flush();
            out.close();
            out = null;
        }
    }

}
