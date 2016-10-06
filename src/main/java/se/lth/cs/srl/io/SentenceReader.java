package se.lth.cs.srl.io;

import se.lth.cs.srl.corpus.Sentence;

import java.util.List;

public interface SentenceReader extends Iterable<Sentence> {

    List<Sentence> readAll();

    public void close();

}
