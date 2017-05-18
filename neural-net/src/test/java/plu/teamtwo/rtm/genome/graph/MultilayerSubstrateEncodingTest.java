package plu.teamtwo.rtm.genome.graph;

import org.junit.Test;
import plu.teamtwo.rtm.neat.GAController;
import plu.teamtwo.rtm.neat.ScoringFunction;

import static org.junit.Assert.assertEquals;


public class MultilayerSubstrateEncodingTest {
//    @Test
//    public void test() {
//        GAController controller = new GAController(
//             new MultilayerSubstrateEncodingBuilder()
//                     .inputs(new int[]{96, 96, 3})
//                     .outputs(new int[]{3})
//                     .addLayer(new int[]{96, 96, 3})
//        );
//
//        controller.createFirstGeneration();
//        controller.assesGeneration(new ScoreFunction());
//    }


    private class ScoreFunction implements ScoringFunction {
        private int count = 0;

        /**
         * This function will be called for once for every individual which is being evaluated. Each scoring function
         * can thus use its own data and know that it will be called with information about only one individual even in a
         * multithreaded context. This should act as a constructor since the caller will not know what subtype it is.
         *
         * @return A new scoring function in the initial state.
         */
        @Override
        public ScoringFunction createNew() {
            return new ScoreFunction();
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
         * This function will be called to retrieve the inputs which should be used by the network. This will be called
         * until it returns null, signaling the end of inputs.
         *
         * @return An array of output values.
         */
        @Override
        public float[] generateInput() {
            if(count++ < 5)
                return new float[]{1, 2, 3, 4};
            return null;
        }


        /**
         * This function will be called with the outputs generated by the neural network after being feed the input from
         * generateInput().
         *
         * @param output Array of output values generated by the network.
         */
        @Override
        public void acceptOutput(float[] output) {
            assertEquals(3, output.length);
        }


        /**
         * This function will be called to asses the performance of the neural network. This function will not be called
         * until generateInput() returns null.
         *
         * @return A score which can be used to asses the fitness of the specified individual.
         */
        @Override
        public double getScore() {
            return 0.0;
        }


        /**
         * Check if the task was successfully completed. Some tasks may never qualify as completed.
         *
         * @return True if the assessment was passed.
         */
        @Override
        public boolean isWinner() {
            return false;
        }
    }
}
