package se.lth.cs.srl.pipeline;

import se.lth.cs.srl.Parse;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Word;
import uk.ac.ed.inf.srl.features.DependencyPathEmbedding;
import uk.ac.ed.inf.srl.features.Feature;
import uk.ac.ed.inf.srl.features.FeatureSet;
import uk.ac.ed.inf.srl.ml.Model;

import java.util.*;
import java.util.Map.Entry;

public abstract class ArgumentStep extends AbstractStep {

    public ArgumentStep(FeatureSet fs) {
        super(fs);
    }

    protected abstract Integer getLabel(Predicate pred, Word arg);

    protected String getPOSPrefix(String pos) {
        for (String prefix : featureSet.POSPrefixes) {
            if (pos.startsWith(prefix))
                return prefix;
        }
        return null;
    }


    protected void collectFeatures(Predicate pred, Word arg, String POSPrefix,
                                   Collection<Integer> indices, Map<Integer, Double> nonbinFeats) {
        if (POSPrefix == null)
            return;

        Integer offset = 0;
        boolean clear = false;
        List<NNThread> nnfeats = new LinkedList<NNThread>();

        for (Feature f : featureSet.get(POSPrefix)) {
            if (f instanceof DependencyPathEmbedding) {
                NNThread t = new NNThread(f, indices, pred, arg, offset);
                nnfeats.add(t);
                t.start();
                clear = true;
            } else
                f.addFeatures(indices, nonbinFeats, pred, arg, offset, false);

            offset += f.size(false);
        }

        if (clear) {
            for (NNThread t : nnfeats) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                for (Entry<Integer, Double> e : t.getFeats().entrySet()) {
                    if (e.getValue() != 0.0) nonbinFeats.put(e.getKey(), e.getValue());
                }
            }
            indices.clear(); // only use NN hidden states instead
        }
        return;
    }

    private class NNThread extends Thread {
        Feature f;
        Collection<Integer> indices;
        Predicate p;
        Word a;
        int offset;
        Map<Integer, Double> feats;

        private NNThread(Feature f, Collection<Integer> indices, Predicate p, Word a, int offset) {
            this.f = f;
            this.indices = indices;
            this.p = p;
            this.a = a;
            this.offset = offset;
            this.feats = new HashMap<Integer, Double>();
        }

        @Override
        public void run() {
            f.addFeatures(indices, feats, p, a, offset, false);
        }

        private Map<Integer, Double> getFeats() {
            return feats;
        }
    }

    // TODO same thing as above.
    public Integer classifyInstance(Predicate pred, Word arg) {
        String POSPrefix = getPOSPrefix(pred.getPOS());
        if (POSPrefix == null) {
            POSPrefix = featureSet.POSPrefixes[0];
            // System.out.println("Unknown POS-tag for predicate '"+pred.getForm()+"', falling back to "+POSPrefix);
        }
        Model m = models.get(POSPrefix);

        Collection<Integer> indices = new TreeSet<Integer>();
        Map<Integer, Double> nonbinFeats = new TreeMap<Integer, Double>();
        collectFeatures(pred, arg, POSPrefix, indices, nonbinFeats);

        if (Parse.parseOptions.externalNNs) {
            int numoutputs = 0;
            float[] outputs = null;
            for (Feature f : featureSet.get(POSPrefix)) {
                if (f instanceof DependencyPathEmbedding) {
                    numoutputs++;
                    if (outputs == null) outputs = ((DependencyPathEmbedding) f).getOutput();
                    else {
                        float[] tmp = ((DependencyPathEmbedding) f).getOutput();
                        for (int j = 0; j < outputs.length; j++)
                            outputs[j] += tmp[j];
                    }
                }
            }
            if (outputs != null) {
                float max = outputs[0];
                int label = 0;
                for (int j = 1; j < outputs.length; ++j) {
                    if (outputs[j] > max) {
                        max = outputs[j];
                        label = j;
                    }
                }
                return label;
            }
        }

        return m.classify(indices, nonbinFeats);
    }
}
