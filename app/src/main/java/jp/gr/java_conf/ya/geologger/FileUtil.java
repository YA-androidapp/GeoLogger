package jp.gr.java_conf.ya.geologger;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class FileUtil {
    public static boolean isExternalStorageWritable() {
        final String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static String writeFile(String filename, String content) {
        if (isExternalStorageWritable()) {
            final String filePath = Environment.getExternalStorageDirectory() + File.separator + filename;
            final File file = new File(filePath);
            file.getParentFile().mkdir();

            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file, true);
                final OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
                final BufferedWriter bw = new BufferedWriter(osw);
                bw.write(content);
                bw.flush();
                bw.close();
                return "";
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return "false";
    }
}
