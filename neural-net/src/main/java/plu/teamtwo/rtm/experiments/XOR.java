package plu.teamtwo.rtm.experiments;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import plu.teamtwo.rtm.neat.Encoding;
import plu.teamtwo.rtm.neat.Genome;
import plu.teamtwo.rtm.neat.NEATController;
import plu.teamtwo.rtm.neat.ScoringFunction;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XOR implements Runnable {
    //private static final int TOTAL_ROUNDS = 100;

    public XOR() {}

    @Override
    public void run() {
        PrintStream output = new PrintStream(new FileOutputStream(FileDescriptor.out));
        NEATController controller = new NEATController(
                Encoding.DIRECT_ENCODING,
                3, 1
        );

        controller.createFirstGeneration();

        for(int g = 0; g < 1000; ++g) {
            boolean foundWinner = controller.assesGeneration(new XORScore());
            final Genome best = controller.getBestIndividual();
            System.out.println(String.format("Gen %d: %.2f, %.1f", controller.getGenerationNum(), controller.getFitness(), best.getFitness()));
            if(foundWinner) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                System.out.println(gson.toJson(best));
                return;
            }
            controller.nextGeneration();
        }
    }

    public static void main(String[] args) {
        new XOR().run();
    }


    private static class XORScore implements ScoringFunction {
        private float error = 0;
        private int correct = 0;
        private boolean expected;
        private int last = 0;
        private int[] order;
        private static final float inputs[][] = new float[][] {
                { 1.0f, 0.0f, 0.0f },
                { 1.0f, 0.0f, 1.0f },
                { 1.0f, 1.0f, 0.0f },
                { 1.0f, 1.0f, 1.0f }
        };

        XORScore() {
            List<Integer> shuffle = new ArrayList<>();
            for (int i = 0; i < 4; i++)
                shuffle.add(i % 4);
            Collections.shuffle(shuffle);
            order = shuffle.stream().mapToInt(i -> i).toArray();
        }

        /**
         * This will be called to determine how many simultaneous instances of the function can exist.
         *
         * @return The maximum number of threads or 0 if there is no reasonable limit.
         */
        @Override
        public int getMaxThreads() {
            return 1;
        }


        /**
         * This will be called to determine if the neural network should be flushed between inputs.
         * It will only be called once.
         *
         * @return True if the network should be flushed between inputs.
         */
        @Override
        public boolean flushBetween() {
            return true;
        }


        /**
         * This function will be called for once for every individual which is being evaluated. Each scoring function
         * can thus use its own data and know that it will be called with information about only one individual even in a
         * multithreaded context. This should act as a constructor since the caller will not know what subtype it is.
         *
         * @return A new scoring function in the initial state.
         */
        @Override
        public ScoringFunction createNew() {
            return new XORScore();
        }


        /**
         * This function will be called to retrieve the inputs which should be used by the network. This will be called
         * until it returns null, signaling the end of inputs.
         *
         * @return An array of output values.
         */
        @Override
        public float[] generateInput() {
            float[] input = null;
            if(last < 4) {
                input = inputs[order[last++]];
                expected = ((int)input[1] ^ (int)input[2]) == 1;
            }
            return input;
//            if(--rounds < 0) return null;
//
//            float a = Math.round(Math.random());
//            float b = Math.round(Math.random());
//            expected = ((int)a ^ (int)b) == 1;
//
//            return new float[]{1.0f, a, b};
        }


        /**
         * This function will be called with the outputs generated by the neural network after being feed the input from
         * generateInput().
         *
         * @param output Array of output values generated by the network.
         */
        @Override
        public void acceptOutput(float[] output) {
            error += Math.abs((expected ? 1.0f : 0.0f) - output[0]);
            if(output[0] >= 0.5 == expected)
                correct++;
        }


        /**
         * This function will be called to asses the performance of the neural network. This function will not be called
         * until generateInput() returns null.
         *
         * @return A score which can be used to asses the fitness of the specified individual.
         */
        @Override
        public double getScore() {
            //return (score / 4.0f) * 100.0f;
            return Math.pow(4.0f - error, 2);
        }


        /**
         * Check if all 4 possibilities were solved.
         *
         * @return True if the assessment was passed.
         */
        @Override
        public boolean isWinner() {
            return correct == 4;
        }
    }
}
