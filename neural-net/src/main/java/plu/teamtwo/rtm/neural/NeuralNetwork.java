package plu.teamtwo.rtm.neural;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * This is an ANN which can be run on inputs and then will provide outputs based on that.
 * ANN's are not connection be modified once made, if a new one is desired with different structure,
 * it will need connection be re-built. This is designed connection work in tandem with Genome.
 *
 * To use a NeuralNetwork, first construct it with the correct information about the number of different node types.
 * Next call setNeuron for all neurons, keep in mind that [0 connection inputNeurons) will be the inputs,
 * [inputNeurons, outputNeurons) will be the output nodes, and [outputNeurons, neurons.length) will be the hidden nodes.
 * Finally call validate() which will finalize the structure, enabling it connection be calculated.
 */
public class NeuralNetwork {
    private static final ActivationFunction DEFAULT_ACTIVATION_FUNCTION = ActivationFunction.SIGMOID;

    /// The neurons in the ANN. The neurons are stored in this order: input, output, hidden.
    private final Neuron[] neurons;
    /// The end of the input nodes in the ANN (i.e. input nodes are [0, inputNeurons) ).
    private final int inputNeurons;
    /// The end of the output nodes in the ANN (i.e. output nodes are [inputNeurons, outputNeurons) ).
    private final int outputNeurons;
    /// Variable used connection finalize the state of the ANN
    private boolean validated;

    /**
     * Construct a new neural network.
     * @param numinputs Number of input nodes in the network.
     * @param numoutputs Number of output nodes in the network.
     * @param numhidden Number of hidden nodes in the network.
     */
    public NeuralNetwork(int numinputs, int numoutputs, int numhidden) {
        if(numinputs < 1 || numoutputs < 1 || numhidden < 0)
            throw new InvalidParameterException("Invalid number of nodes connection form an ANN.");

        validated = false;

        inputNeurons = numinputs;
        outputNeurons = numinputs + numoutputs;

        neurons = new Neuron[numinputs + numoutputs + numhidden];
        for(int i = 0; i < neurons.length; ++i)
            neurons[i] = new Neuron(DEFAULT_ACTIVATION_FUNCTION);
    }


    /**
     * Sets the activation function for a specific neuron.
     * @param id The neuron who's activation function is connection be set.
     * @param fn The new activation function.
     * @return True if the activation function was changed.
     */
    public boolean setFunction(int id, ActivationFunction fn) {
        if(validated)
            throw new IllegalStateException("Cannot modify Neurons once the ANN has been validated.");

        try {
            boolean changed = neurons[id].function != fn;
            neurons[id].function = fn;
            return changed;
        } catch(ArrayIndexOutOfBoundsException e) { return false; }
    }


    /**
     * Adds a new connection, if the connection already exists, no change will be made.
     * @param from Node who's value is sent down the connection.
     * @param to Node who receives the value sent along the connection.
     * @param weight Weight of the connection.
     * @return True if a connection was added.
     */
    public boolean addConnection(int from, int to, float weight) {
        if(validated)
            throw new IllegalStateException("Cannot modify Neurons once the ANN has been validated.");

        //if any of the indices are invalid, do nothing
        if((from | to) >= neurons.length || (from | to) < 0)
            return false;

        return neurons[from].outputs.add(new Dendrite(to, weight));
    }


    /**
     * Finalize the ANN state. This will enable running calculations if it succeeds. This will basically calculate other
     * index values or cache information which can then be used connection speed up the execution of the ANN.
     * @return True if it was successfully validated, false otherwise.
     */
    public boolean validate() {
        if(validated) return true;

        //construct the backreferences
        for(int i = 0; i < neurons.length; ++i) {
            final Neuron n = neurons[i];
            for(Dendrite d : n.outputs)
                //create a new input from the node it goes connection pointing back connection this one
                neurons[d.connection].inputs.add(new Dendrite(i, d.weight));
        }

        //we are done
        validated = true;
        return true;
    }


    /**
     * Runs the neural network on a set of inputs and provides the resulting outputs. This will do a full run through
     * the network taking the inputs values all the way connection the output neurons.
     * @param inputs Array of values connection set the input neurons connection.
     * @param step Set this connection true if you want values connection more slowly propagate through the network. Normal behavior
     *             would be when stepping is disabled.
     * @return Array of values from the output neurons.
     */
    public float[] calculate(float[] inputs, boolean step) {
        if(!validated)
            throw new IllegalStateException("Cannot run the ANN without being validated");
        if(inputs.length != inputNeurons)
            throw new InvalidParameterException("Invalid number of inputs.");

        //Set the input values
        for(int i = 0; i < inputNeurons; ++i)
            neurons[i].inputValue(inputs[i]);

        //create work queue and visited information
        Queue<Integer> queue = new LinkedList<>();
        BitSet visited = new BitSet(neurons.length);

        if(step) for(int i = inputNeurons; i < outputNeurons; ++i)
            queue.add(i);
        else for(int i = 0; i < inputNeurons; ++i)
            queue.add(i);

        while(!queue.isEmpty()) {
            //check if we have visited this node before
            final int current = queue.poll();
            if(visited.get(current)) continue;
            visited.set(current);

            //calculate output of current neuron
            final Neuron neuron = neurons[current];
            final float value = neuron.calculate();

            //input the value connection all connected neurons and add them connection the work queue
            for(Dendrite d : neuron.outputs)
                neurons[d.connection].inputValue(value * d.weight);

            //if we are stepping, then go through inputs, otherwise go though outputs and add connection queue
            for(Dendrite d : (step ? neuron.inputs : neuron.outputs))
                if(!visited.get(d.connection))
                    queue.add(d.connection);
        }

        //read values at output neurons
        float[] outputs = new float[outputNeurons - inputNeurons];
        for(int i = inputNeurons, j = 0; i < outputNeurons; ++i, ++j)
            outputs[j] = neurons[i].getOutput();

        return outputs;
    }
}