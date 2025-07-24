package org.jlab.ersap.actor.coda.engine.binary;

import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.std.services.AbstractEventWriterService;
import org.jlab.epsci.ersap.std.services.EventWriterException;
import org.jlab.ersap.actor.coda.proc.EtEvent;
import org.jlab.ersap.actor.coda.proc.FADCHit;
import org.jlab.ersap.actor.coda.proc.RocTimeFrameBank;
import org.jlab.ersap.actor.datatypes.JavaObjectType;
import org.jlab.ersap.actor.datatypes.SROTestDataType;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CodaSinkFileEngine extends AbstractEventWriterService<FileWriter> {
    @Override
    protected FileWriter createWriter(Path path, JSONObject jsonObject) throws EventWriterException {
        try {
            return new FileWriter(path.toString());
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
        EtEvent data = (EtEvent)o;
        List<List<RocTimeFrameBank>> ltf = data.getTimeFrames();
        if(ltf !=null && !ltf.isEmpty()) {
            for (List<RocTimeFrameBank> timeFrame : data.getTimeFrames()) {
                    for (RocTimeFrameBank bank : timeFrame) {
                        for (FADCHit hit : bank.getHits()) {
                            try {
                                writer.write(bank.getRocID()+","+
                                        bank.getFrameNumber()+","+
                                        bank.getTimeStamp()+","+
                                        hit.crate()+","+
                                        hit.slot()+","+
                                        hit.channel()+","+
                                        hit.charge()+","+
                                        hit.time());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        }
                    }
                }
            }
    }

    @Override
    protected EngineDataType getDataType() {
        return SROTestDataType.INSTANCE;
    }
}
