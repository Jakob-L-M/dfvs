package Utilities;

import java.util.ArrayList;
import java.util.List;

public class Model {

    List<List<List<Double>>> weights;
    List<List<Double>> biases;
    double alpha;

    /**
     * Neural Network Model. Weights and Biases must have the same length and matching dimensions
     *
     * @param weights List of weight matrices W_0, ..., W_n
     * @param biases  List of bias vectors B_0, ..., B_n
     * @param alpha   Alpha Value for Leaky Relu. f(x) = max(x, alpha*x)
     */
    public Model(List<List<List<Double>>> weights, List<List<Double>> biases, double alpha) {
        assert weights.size() == biases.size();
        this.weights = weights;
        this.biases = biases;
        this.alpha = alpha;
    }

    /**
     * Essentially does a NN-Calculation
     *
     * @param in List of input values. Has to have a matching size to the network
     * @return prediction. Lies between 0 and 1
     */
    public double predict(List<Double> in) {
        for (int i = 0; i < biases.size(); i++) {
            double[] current = new double[weights.get(i).size()];
            for (int j = 0; j < weights.get(i).size(); j++) {
                for (int k = 0; k < weights.get(i).get(j).size(); k++) {
                    current[j] += (weights.get(i).get(j).get(k) * in.get(k));
                }
            }
            in = new ArrayList<>();
            for (int j = 0; j < biases.get(i).size(); j++) {
                current[j] += biases.get(i).get(j);
                if (i == weights.size() - 1) {
                    in.add(current[j]);
                } else {
                    in.add(Math.max(current[j], alpha * current[j]));
                }
            }
        }
        return utils.round(1 / (1 + Math.exp(-in.get(0))), 5);
    }
}
