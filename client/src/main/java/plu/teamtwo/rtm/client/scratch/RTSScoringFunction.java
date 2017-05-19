package plu.teamtwo.rtm.client.scratch;

import plu.teamtwo.rtm.client.InputController;
import plu.teamtwo.rtm.client.Main;
import plu.teamtwo.rtm.ii.ProcessedData;
import plu.teamtwo.rtm.ii.RTSProcessor;
import plu.teamtwo.rtm.neat.ScoringFunction;

import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class RTSScoringFunction implements ScoringFunction, RTSProcessor.ProcessingListener {
    private final int INPUT_WIDTH, INPUT_HEIGHT;
    private BlockingQueue<float[]> observations;


    RTSScoringFunction(int width, int height) {
        INPUT_WIDTH = width;
        INPUT_HEIGHT = height;
        observations = new LinkedBlockingQueue<float[]>();
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
        return new RTSScoringFunction(INPUT_WIDTH, INPUT_HEIGHT);
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
        while(InputController.getInstance().isGameRunning()) {
            float[] inputs = null;
            try {
                inputs = observations.poll(10, TimeUnit.MILLISECONDS);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            if(inputs == null) continue;
            return inputs;
        }
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
        final InputController ic = InputController.getInstance();
        ic.setPressed(InputController.Key.LEFT, output[0] > 0.5f);
        ic.setPressed(InputController.Key.SPACE, output[1] > 0.5f);
        ic.setPressed(InputController.Key.RIGHT, output[2] > 0.5f);
        ic.updateInputs();
    }


    /**
     * This function will be called to asses the performance of the neural network. This function will not be called
     * until generateInput() returns null.
     *
     * @return A score which can be used to asses the fitness of the specified individual.
     */
    @Override
    public double getScore() {
        return InputController.getInstance().getScore();
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


    @Override
    public void frameProcessed(ProcessedData data) {
        final int INPUT_SIZE = INPUT_WIDTH * INPUT_HEIGHT;
        float[] observeration = new float[INPUT_SIZE];
        Rectangle captureArea = Main.rtsp.getArea();

        final float xratio = captureArea.x / INPUT_WIDTH;
        final float yratio = captureArea.y / INPUT_HEIGHT;

        for(int y = 0; y < INPUT_HEIGHT; ++y) {
            final int yindex = y * INPUT_WIDTH;
            final float ycord =  y * yratio;

            for(int x = 0; x < INPUT_WIDTH; ++x) {
                final float xcord = x * xratio;
                observeration[yindex + x] = data.checkPoint(new plu.teamtwo.rtm.core.util.Point(xcord, ycord)) ? 1.0f : 0.0f;
            }
        }

        observations.add(observeration);
    }
}
