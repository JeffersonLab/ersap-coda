package org.jlab.ersap.actor.coda.engine.binary;

import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.std.services.AbstractEventWriterService;
import org.jlab.epsci.ersap.std.services.EventWriterException;
import org.jlab.ersap.actor.datatypes.SROTestDataType;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class CodaSinkBinaryEngine extends AbstractEventWriterService<FileOutputStream> {
    private static final String OUTPUT_FILE = "output_file";
    private static final String FRAMES_PER_FILE = "frames_per_file";
    private int framesPerFile = 1000;
    private int frameCount = 0;
    private int fileIndex = 1;
    private String baseFilePath = null;

    @Override
    protected FileOutputStream createWriter(Path path, JSONObject opts) throws EventWriterException {
        if (opts != null && opts.has(FRAMES_PER_FILE)) {
            framesPerFile = opts.getInt(FRAMES_PER_FILE);
        }
        baseFilePath = path.toString();
        if (opts != null && opts.has(OUTPUT_FILE)) {
            baseFilePath = opts.getString(OUTPUT_FILE);
        }
        frameCount = 0;
        fileIndex = 1;
        return openNewFile();
    }

    private FileOutputStream openNewFile() throws EventWriterException {
        String filePath = baseFilePath;
        if (fileIndex > 1) {
            int dot = baseFilePath.lastIndexOf('.');
            if (dot > 0) {
                filePath = baseFilePath.substring(0, dot) + "-" + fileIndex + baseFilePath.substring(dot);
            } else {
                filePath = baseFilePath + "-" + fileIndex;
            }
        }
        try {
            return new FileOutputStream(filePath, false); // new file each time
        } catch (IOException e) {
            throw new EventWriterException(e);
        }
    }

    @Override
    protected void closeWriter() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeEvent(Object o) throws EventWriterException {
        try {
            ByteBuffer buffer = SROTestDataType.INSTANCE.serializer().write(o);
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            writer.write(bytes);
            writer.flush();
            frameCount++;
            if (frameCount >= framesPerFile) {
                writer.close();
                fileIndex++;
                frameCount = 0;
                writer = openNewFile();
            }
        } catch (Exception e) {
            throw new EventWriterException(e);
        }
    }

    @Override
    protected EngineDataType getDataType() {
        return SROTestDataType.INSTANCE;
    }
}
