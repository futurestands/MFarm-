package dev.mfarm.com.mfarm.sync;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class FarmBackupStore {
    public static final String FILE_NAME = "mfarm-shared.mfarm";
    public static final String MIME = "application/octet-stream";

    private FarmBackupStore() {}

    public static byte[] read(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    public static void write(Context context, Uri uri, byte[] bytes) throws Exception {
        OutputStream out = context.getContentResolver().openOutputStream(uri, "w");
        if (out == null) {
            out = context.getContentResolver().openOutputStream(uri);
        }
        if (out == null) {
            throw new Exception("Could not write backup");
        }
        try {
            out.write(bytes);
            out.flush();
        } finally {
            out.close();
        }
    }

    public static DocumentFile backupFile(Context context, Uri treeUri, boolean createIfMissing) {
        DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
        if (dir == null || !dir.canRead()) {
            return null;
        }
        DocumentFile existing = dir.findFile(FILE_NAME);
        if (existing != null) {
            return existing;
        }
        if (createIfMissing && dir.canWrite()) {
            return dir.createFile(MIME, FILE_NAME);
        }
        return null;
    }
}
