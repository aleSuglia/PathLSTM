package uk.ac.ed.inf.srl.ml;

import uk.ac.ed.inf.srl.ml.liblinear.Label;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Model extends Serializable {

    public List<Label> classifyProb(Collection<Integer> indices,
                                    Map<Integer, Double> nonbinFeats);

    public Integer classify(Collection<Integer> indices,
                            Map<Integer, Double> nonbinFeats);
    // public void sparsify();

}
