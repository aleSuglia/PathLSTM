package dmonner.xlbp.stat;

import dmonner.xlbp.util.CSVWriter;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractStat implements Stat {
    @Override
    public void addTo(final Map<String, Object> map) {
        addTo("", map);
    }

    @Override
    public void saveHeader(final CSVWriter out) throws IOException {
        saveHeader("", out);
    }

    @Override
    public String toString() {
        return toString("");
    }
}
