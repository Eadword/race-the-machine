package plu.teamtwo.rtm.neural;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubstrateNetworkTest {

    @Test
    public void testEmptyCalculate() {
        NeuralNetwork net = new SubstrateNetwork(
            new int[][]{{2, 2}, {2, 2}, {1}},
            new float[][]{
                {
                    0.0f,  0.0f,  0.0f,  0.0f,
                    0.0f,  0.0f,  0.0f,  0.0f,
                    0.0f,  0.0f,  0.0f,  0.0f,
                    0.0f,  0.0f,  0.0f,  0.0f
                },
                {
                    0.0f,  0.0f,  0.0f,  0.0f
                }
            }
        );

        float[] output = net.calculate(1.0f, 2.0f, -4.0f, -2.0f);
        assertEquals(1, output.length);
        assertEquals(0.5f, output[0], 1e-4);
    }

    @Test
    public void testSimpleCalculate() {
        NeuralNetwork net = new SubstrateNetwork(
                new int[][]{{2, 2}, {2, 2}, {1}},
                new float[][]{
                        {
                                0.0f,  0.0f,  0.0f,  0.0f,
                                0.0f,  0.0f,  0.0f,  0.0f,
                                0.0f,  1.0f,  0.0f,  0.0f,
                                0.0f,  0.0f,  0.0f,  0.0f
                        },
                        {
                                0.0f,  0.0f, 10.0f,  0.0f
                        }
                }
        );

        float[] output = net.calculate(1.0f, 1.0f, 1.0f, 1.0f);
        assertEquals(1, output.length);
        assertEquals(1.0f, output[0], 1e-4);
    }
}