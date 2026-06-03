package farvix.solution;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;

import com.google.gson.*;

public class MouseMovementPredictor {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int FPS = 200;
    private static final int TARGET_RADIUS = 50;
    private static final boolean TRAINING_MODE = false;
    private static final int MAX_POINTS = 100;
    private static final int MOUSE_STEP_SIZE = 15;

    public static void main(String[] args) {
        MousePredictor predictor = new MousePredictor();

        if (TRAINING_MODE) {
            System.out.println("Collecting training data... Move your mouse towards the targets!");
            int numSamples = predictor.collectData();
            System.out.println("Collected " + numSamples + " samples. Training model...");
            predictor.train();
            System.out.println("Training complete! Model saved.");
        } else {
            System.out.println("Loading model and starting test mode...");
            System.out.println("Press 'V' to toggle automatic mouse movement");
            predictor.loadModel("mouse_predictor.json");
            predictor.test();
        }
    }

    static class NeuralNetwork {
        private List<Layer> layers = new ArrayList<>();

        public NeuralNetwork() {

            layers.add(new DenseLayer(4, 64, "relu"));
            layers.add(new DropoutLayer(0.2));
            layers.add(new DenseLayer(64, 32, "relu"));
            layers.add(new DenseLayer(32, 2, "tanh"));
        }

        public double[] forward(double[] input) {
            double[] current = input.clone();
            for (Layer layer : layers) {
                current = layer.forward(current);
            }
            return current;
        }

        public void train(List<double[]> X, List<double[]> Y, int epochs, double learningRate) {
            for (int epoch = 0; epoch < epochs; epoch++) {
                double totalLoss = 0.0;
                for (int i = 0; i < X.size(); i++) {
                    double[] output = forward(X.get(i));
                    double[] error = new double[output.length];
                    for (int j = 0; j < output.length; j++) {
                        error[j] = output[j] - Y.get(i)[j];
                        totalLoss += error[j] * error[j];
                    }
                    double[] backpropError = error;
                    for (int l = layers.size() - 1; l >= 0; l--) {
                        backpropError = layers.get(l).backpropagate(backpropError, learningRate);
                    }
                }
                totalLoss /= X.size();
                if (epoch % 10 == 0) {
                    System.out.println("Epoch " + epoch + " - Loss: " + totalLoss);
                }
            }
        }

        public void save(String filename) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject data = new JsonObject();
            JsonArray layerArray = new JsonArray();
            for (Layer layer : layers) {
                layerArray.add(layer.toJson());
            }
            data.add("layers", layerArray);
            try (FileWriter writer = new FileWriter(filename)) {
                gson.toJson(data, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void load(String filename) {
            Gson gson = new Gson();
            InputStream is = this.getClass().getResourceAsStream("/" + filename);
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + filename);
            }
            try (InputStreamReader reader = new InputStreamReader(is)) {
                JsonObject data = gson.fromJson(reader, JsonObject.class);
                JsonArray layerArray = data.getAsJsonArray("layers");
                layers.clear();
                for (JsonElement elem : layerArray) {
                    JsonObject obj = elem.getAsJsonObject();
                    String type = obj.get("type").getAsString();
                    Layer layer;
                    if ("DenseLayer".equals(type)) {
                        layer = new DenseLayer(0, 0, "");
                        ((DenseLayer) layer).loadJson(obj);
                    } else if ("DropoutLayer".equals(type)) {
                        layer = new DropoutLayer(0.0);
                        ((DropoutLayer) layer).loadJson(obj);
                    } else {
                        throw new IllegalArgumentException("Unknown layer type");
                    }
                    layers.add(layer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    interface Layer {
        double[] forward(double[] input);
        double[] backpropagate(double[] error, double learningRate);
        JsonObject toJson();
        void loadJson(JsonObject obj);
    }

    static class DenseLayer implements Layer {
        private int inputSize;
        private int outputSize;
        private String activation;
        private double[][] weights;
        private double[] biases;
        private double[] input;
        private double[] output;

        public DenseLayer(int inputSize, int outputSize, String activation) {
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            this.activation = activation;
            weights = new double[inputSize][outputSize];
            biases = new double[outputSize];

            Random rand = new Random(0); 
            for (int i = 0; i < inputSize; i++) {
                for (int j = 0; j < outputSize; j++) {
                    weights[i][j] = rand.nextDouble() * 2 - 1;
                }
            }
        }

        @Override
        public double[] forward(double[] input) {
            this.input = input;
            output = new double[outputSize];
            for (int j = 0; j < outputSize; j++) {
                double sum = biases[j];
                for (int i = 0; i < inputSize; i++) {
                    sum += input[i] * weights[i][j];
                }
                output[j] = sum;
            }
            if ("relu".equals(activation)) {
                for (int j = 0; j < outputSize; j++) {
                    output[j] = Math.max(0, output[j]);
                }
            } else if ("tanh".equals(activation)) {
                for (int j = 0; j < outputSize; j++) {
                    output[j] = Math.tanh(output[j]);
                }
            }
            return output;
        }

        @Override
        public double[] backpropagate(double[] error, double learningRate) {
            double[] grad = new double[outputSize];
            if ("relu".equals(activation)) {
                for (int j = 0; j < outputSize; j++) {
                    grad[j] = error[j] * (output[j] > 0 ? 1 : 0);
                }
            } else if ("tanh".equals(activation)) {
                for (int j = 0; j < outputSize; j++) {
                    grad[j] = error[j] * (1 - output[j] * output[j]);
                }
            }
            double[] errorWrtInput = new double[inputSize];
            for (int i = 0; i < inputSize; i++) {
                for (int j = 0; j < outputSize; j++) {
                    errorWrtInput[i] += grad[j] * weights[i][j];
                }
            }

            for (int i = 0; i < inputSize; i++) {
                for (int j = 0; j < outputSize; j++) {
                    weights[i][j] -= learningRate * input[i] * grad[j];
                }
            }
            for (int j = 0; j < outputSize; j++) {
                biases[j] -= learningRate * grad[j];
            }
            return errorWrtInput;
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "DenseLayer");
            json.addProperty("input_size", inputSize);
            json.addProperty("output_size", outputSize);
            json.addProperty("activation", activation);
            JsonArray weightsArray = new JsonArray();
            for (double[] row : weights) {
                JsonArray rowArray = new JsonArray();
                for (double val : row) {
                    rowArray.add(val);
                }
                weightsArray.add(rowArray);
            }
            json.add("weights", weightsArray);
            JsonArray biasesArray = new JsonArray();
            for (double val : biases) {
                biasesArray.add(val);
            }
            json.add("biases", biasesArray);
            return json;
        }

        @Override
        public void loadJson(JsonObject json) {
            inputSize = json.get("input_size").getAsInt();
            outputSize = json.get("output_size").getAsInt();
            activation = json.get("activation").getAsString();
            JsonArray weightsArray = json.getAsJsonArray("weights");
            weights = new double[inputSize][outputSize];
            for (int i = 0; i < inputSize; i++) {
                JsonArray row = weightsArray.get(i).getAsJsonArray();
                for (int j = 0; j < outputSize; j++) {
                    weights[i][j] = row.get(j).getAsDouble();
                }
            }
            JsonArray biasesArray = json.getAsJsonArray("biases");
            biases = new double[outputSize];
            for (int j = 0; j < outputSize; j++) {
                biases[j] = biasesArray.get(j).getAsDouble();
            }
        }
    }

    static class DropoutLayer implements Layer {
        private double dropoutRate;
        private double[] mask;

        public DropoutLayer(double dropoutRate) {
            this.dropoutRate = dropoutRate;
        }

        @Override
        public double[] forward(double[] input) {
            mask = new double[input.length];
            double[] output = new double[input.length];
            Random rand = new Random();
            for (int i = 0; i < input.length; i++) {
                mask[i] = rand.nextDouble() < dropoutRate ? 0 : 1;
                output[i] = input[i] * mask[i];
            }
            return output;
        }

        @Override
        public double[] backpropagate(double[] error, double learningRate) {
            double[] output = new double[error.length];
            for (int i = 0; i < error.length; i++) {
                output[i] = error[i] * mask[i];
            }
            return output;
        }

        @Override
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "DropoutLayer");
            json.addProperty("dropout_rate", dropoutRate);
            return json;
        }

        @Override
        public void loadJson(JsonObject json) {
            dropoutRate = json.get("dropout_rate").getAsDouble();
        }
    }

    static class MousePredictor {
        private List<double[]> dataX = new ArrayList<>();
        private List<double[]> dataY = new ArrayList<>();
        private Deque<Point> mousePath = new ArrayDeque<>(MAX_POINTS);
        private Point targetPos = randomTarget();
        private NeuralNetwork model = new NeuralNetwork();
        private boolean autoMove = false;
        private Robot robot;
        private Point currPos;

        public MousePredictor() {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }

        private Point randomTarget() {
            Random rand = new Random();
            int x = rand.nextInt(WIDTH - 2 * TARGET_RADIUS) + TARGET_RADIUS;
            int y = rand.nextInt(HEIGHT - 2 * TARGET_RADIUS) + TARGET_RADIUS;
            return new Point(x, y);
        }

        public double[] predictNextPoint(Point curr) {
            double dx = targetPos.x - curr.x;
            double dy = targetPos.y - curr.y;
            double mag = Math.sqrt(dx * dx + dy * dy);
            double nx = mag > 0 ? dx / mag : 0;
            double ny = mag > 0 ? dy / mag : 0;
            double[] features = {dx / (double) WIDTH, dy / (double) HEIGHT, nx, ny};
            double[] prediction = model.forward(features);
            double nextX = curr.x + prediction[0] * MOUSE_STEP_SIZE;
            double nextY = curr.y + prediction[1] * MOUSE_STEP_SIZE;
            nextX = Math.max(0, Math.min(WIDTH, nextX));
            nextY = Math.max(0, Math.min(HEIGHT, nextY));
            return new double[]{nextX, nextY};
        }

        public List<Point> drawPredictedPath(Point curr) {
            List<Point> path = new ArrayList<>();
            path.add(curr);
            for (int i = 0; i < 100; i++) {
                double[] nextD = predictNextPoint(path.get(path.size() - 1));
                Point next = new Point((int) nextD[0], (int) nextD[1]);
                path.add(next);
                if (distance(next, targetPos) < TARGET_RADIUS) {
                    break;
                }
            }
            return path;
        }

        private double distance(Point a, Point b) {
            return Math.hypot(a.x - b.x, a.y - b.y);
        }

        public void test() {
            JFrame frame = new JFrame("Mouse Movement Predictor");
            DrawingPanel panel = new DrawingPanel(this);
            frame.add(panel);
            frame.setSize(WIDTH, HEIGHT);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            panel.requestFocusInWindow();

            Timer timer = new Timer(1000 / FPS, e -> {
                panel.updateCurrPos();
                panel.repaint();
            });
            timer.start();
        }

        public int collectData() {
            JFrame frame = new JFrame("Mouse Movement Predictor - Training");
            TrainingPanel panel = new TrainingPanel(this);
            frame.add(panel);
            frame.setSize(WIDTH, HEIGHT);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            panel.requestFocusInWindow();

            Timer timer = new Timer(1000 / FPS, e -> {
                panel.updateCurrPos();
                panel.repaint();
            });
            timer.start();


            while (frame.isVisible()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return dataX.size();
        }

        public void train() {
            if (dataX.isEmpty()) {
                System.out.println("No training data available!");
                return;
            }
            model.train(dataX, dataY, 600, 0.01);
            model.save("mouse_predictor.json");
        }

        public void loadModel(String filename) {
            model.load(filename);
        }


        public void updateTraining(Point prevPos, Point currPos) {
            mousePath.add(currPos);

            double dx = targetPos.x - currPos.x;
            double dy = targetPos.y - currPos.y;
            double magD = Math.sqrt(dx * dx + dy * dy);
            double nxD = magD > 0 ? dx / magD : 0;
            double nyD = magD > 0 ? dy / magD : 0;

            double mx = currPos.x - prevPos.x;
            double my = currPos.y - prevPos.y;
            double magM = Math.sqrt(mx * mx + my * my);
            if (magM > 0) {
                double nxM = mx / magM;
                double nyM = my / magM;
                double[] features = {dx / (double) WIDTH, dy / (double) HEIGHT, nxM, nyM};
                double[] target = {nxD, nyD};
                dataX.add(features);
                dataY.add(target);
            }

            if (magD < TARGET_RADIUS) {
                targetPos = randomTarget();
            }
        }


        public void updateTest(Point curr, boolean isAutoMove, Point locOnScreen) {
            this.currPos = curr;
            if (isAutoMove) {
                double[] nextD = predictNextPoint(curr);
                int nextX = (int) nextD[0];
                int nextY = (int) nextD[1];
                robot.mouseMove(locOnScreen.x + nextX, locOnScreen.y + nextY);
                this.currPos = new Point(nextX, nextY);
            }

            if (distance(this.currPos, targetPos) < TARGET_RADIUS) {
                targetPos = randomTarget();
            }
        }

        public Point getCurrPos() {
            return currPos;
        }

        public Point getTargetPos() {
            return targetPos;
        }

        public Deque<Point> getMousePath() {
            return mousePath;
        }

        public boolean isAutoMove() {
            return autoMove;
        }

        public void toggleAutoMove() {
            autoMove = !autoMove;
        }
    }

    static class DrawingPanel extends JPanel {
        private MousePredictor predictor;
        private Point currPos = new Point(WIDTH / 2, HEIGHT / 2); 

        public DrawingPanel(MousePredictor predictor) {
            this.predictor = predictor;
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_V) {
                        predictor.toggleAutoMove();
                        System.out.println("Auto-move: " + (predictor.isAutoMove() ? "ON" : "OFF"));
                    }
                }
            });
            setFocusable(true);
        }

        public void updateCurrPos() {
            Point mouseScreen = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(mouseScreen, this);
            predictor.updateTest(new Point(mouseScreen.x, mouseScreen.y), predictor.isAutoMove(), getLocationOnScreen());
            currPos = predictor.getCurrPos();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());


            g.setColor(Color.RED);
            g.fillOval(predictor.getTargetPos().x - TARGET_RADIUS, predictor.getTargetPos().y - TARGET_RADIUS, 2 * TARGET_RADIUS, 2 * TARGET_RADIUS);


            g.setColor(Color.BLUE);
            g.fillOval(currPos.x - 5, currPos.y - 5, 10, 10);


            g.setColor(Color.GREEN);
            List<Point> path = predictor.drawPredictedPath(currPos);
            for (int i = 0; i < path.size() - 1; i++) {
                g.drawLine(path.get(i).x, path.get(i).y, path.get(i + 1).x, path.get(i + 1).y);
            }
        }
    }

    static class TrainingPanel extends JPanel {
        private MousePredictor predictor;
        private Point prevPos = new Point(WIDTH / 2, HEIGHT / 2);
        private Point currPos;

        public TrainingPanel(MousePredictor predictor) {
            this.predictor = predictor;
        }

        public void updateCurrPos() {
            Point mouseScreen = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(mouseScreen, this);
            currPos = new Point(mouseScreen.x, mouseScreen.y);
            predictor.updateTraining(prevPos, currPos);
            prevPos = currPos;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());


            g.setColor(Color.RED);
            g.fillOval(predictor.getTargetPos().x - TARGET_RADIUS, predictor.getTargetPos().y - TARGET_RADIUS, 2 * TARGET_RADIUS, 2 * TARGET_RADIUS);


            Deque<Point> path = predictor.getMousePath();
            if (path.size() > 1) {
                Iterator<Point> it = path.iterator();
                Point prev = it.next();
                int index = 1;
                while (it.hasNext()) {
                    Point curr = it.next();
                    int colorIntensity = (int) (255 * (index / (double) path.size()));
                    g.setColor(new Color(0, colorIntensity, 0));
                    g.drawLine(prev.x, prev.y, curr.x, curr.y);
                    prev = curr;
                    index++;
                }
            }
        }
    }
}
