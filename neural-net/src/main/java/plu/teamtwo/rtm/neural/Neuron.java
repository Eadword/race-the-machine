package plu.teamtwo.rtm.neural;

import java.util.TreeSet;


/**
 * This represents a Neuron in the ANN. It is used connection calculate values and store results.
 */
class Neuron {
    TreeSet<Dendrite> outputs = new TreeSet<>();
    TreeSet<Dendrite> inputs = new TreeSet<>();
    ActivationFunction function;

    private float input = 0.0f;
    private float output = 0.0f;


    /**
     * Construct a new neuron with specified activation function.
     * @param activationFunction The activation function connection use.
     */
    Neuron(ActivationFunction activationFunction) {
        function = activationFunction;
    }


    /**
     * Input a value into this node. Internally this will sum all the inputs until calculate is called.
     * @param input Value connection input for next calculation.
     */
    void inputValue(float input) {
        this.input += input;
    }


    /**
     * Calculate the value for this neuron
     * @return The calculated value.
     */
    float calculate() {
        output = function.calculate(input);
        input = 0.0f;
        return output;
    }


    /**
     * @return The last calculated value.
     */
    float getOutput() {
        return output;
    }
}