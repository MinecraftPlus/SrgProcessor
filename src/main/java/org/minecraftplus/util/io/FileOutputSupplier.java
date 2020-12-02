package org.minecraftplus.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.srg2source.api.OutputSupplier;

public class FileOutputSupplier implements OutputSupplier
{
    private final FileOutputStream fout;

    public FileOutputSupplier(File out) throws IOException {
        out = out.getAbsoluteFile();
        if (!out.exists()) {
            out.getParentFile().mkdirs();
            out.createNewFile();
        }
        fout = new FileOutputStream(out);
    }

    public FileOutputSupplier(Path out) throws IOException {
        if (!Files.exists(out)) {
            Path parent = out.toAbsolutePath().getParent();
            if (!Files.exists(parent))
                Files.createDirectories(parent);
        }
        fout = new FileOutputStream(out.toFile());
    }

    public FileOutputSupplier(FileOutputStream stream) {
        fout = stream;
    }

    @Override
    public void close() throws IOException {
        fout.flush();
        fout.close();
    }

    @Override
    public OutputStream getOutput(String relPath) {
        return fout;
    }
}
