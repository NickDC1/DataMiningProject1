import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Algorithm {

    // Computes the Jaccard similarity for two boolean vectors represented as double arrays.
    private static double jaccardSimilarity(double[] a, double[] b) {
        int intersection = 0, union = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == 1 || b[i] == 1) union++;
            if (a[i] == 1 && b[i] == 1) intersection++;
        }
        return union == 0 ? 0.0 : ((double) intersection / union);
    }

    // Writes a list of strings to a file.
    private static void writeResultsToFile(List<String> results, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        for (String line : results) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
    }

    // Computes the Jaccard coefficient for two records (skipping the first element, which is assumed to be the label).
    public static double jaccardCoefficient(List<Integer> primaryRecord, List<Integer> comparedRecord) {
        double p = 0, q = 0, r = 0;
        for (int i = 1; i < primaryRecord.size(); i++) {
            int a = primaryRecord.get(i);
            int b = comparedRecord.get(i);
            if (a == 1 && b == 1) p++;
            else if (a == 1 && b == 0) q++;
            else if (a == 0 && b == 1) r++;
        }
        return (p / (p + q + r));
    }

    public static Map<String, List<Integer>> readCSV(String filePath, boolean skipHeader) {
        String line;
        Map<String, List<Integer>> records = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            if (skipHeader) {
                br.readLine();
            }
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                // Ensure there is at least one data value after the key.
                if (values.length < 2) {
                    System.out.println("Skipping line (not enough columns): " + line);
                    continue;
                }

                // Check if the first data column (values[1]) is numeric.
                try {
                    Integer.parseInt(values[1].trim());
                } catch(NumberFormatException e) {
                    System.out.println("Skipping line (non-numeric data found): " + line);
                    continue;
                }

                List<Integer> temporary = new ArrayList<>();
                String key = values[0];
                // If key already exists, append a counter to make it unique.
                if (records.containsKey(key)) {
                    int counter = 1;
                    String newKey = key + "_" + counter;
                    while (records.containsKey(newKey)) {
                        counter++;
                        newKey = key + "_" + counter;
                    }
                    key = newKey;
                }
                // Parse all the remaining values as integers.
                for (int i = 1; i < values.length; i++) {
                    try {
                        temporary.add(Integer.parseInt(values[i].trim()));
                    } catch (NumberFormatException e) {
                        System.out.println("Skipping line (error parsing integer): " + line);
                        temporary = null;
                        break;
                    }
                }
                if (temporary != null) {
                    records.put(key, temporary);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
        return records;
    }

    // Predicts the outcome for a record using the k-nearest neighbors approach based on the Jaccard coefficient.
    public static String predictRecord(Map.Entry<String, List<Integer>> predictRecord, Map<String, List<Integer>> comparisonDataSet, int K) {
        Map<Map.Entry<String, List<Integer>>, Double> coefficients = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> iterator : comparisonDataSet.entrySet()) {
            coefficients.put(iterator, jaccardCoefficient(predictRecord.getValue(), iterator.getValue()));
        }
        List<Map.Entry<Map.Entry<String, List<Integer>>, Double>> sortedList = coefficients.entrySet()
                .stream()
                .sorted(Map.Entry.<Map.Entry<String, List<Integer>>, Double>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
        int plusCount = 0, minusCount = 0;
        // Ensure K does not exceed the size of the sorted list.
        int limit = Math.min(K, sortedList.size());
        for (int i = 0; i < limit; i++) {
            if (sortedList.get(i).getKey().getValue().get(0) == 1) plusCount++;
            else minusCount++;
        }
        if (plusCount > minusCount) return predictRecord.getKey() + " : 90+";
        else if (plusCount < minusCount) return predictRecord.getKey() + " : 90-";
        else return predictRecord.getKey() + "Neutral";
    }

    // Calculates the accuracy given the testing data and the predicted results.
    public static Double calculateAccuracy(List<Map.Entry<String, List<Integer>>> testingData, List<Integer> predictedResults) {
        double amountAccurate = 0;
        for (int i = 0; i < testingData.size(); i++) {
            if (testingData.get(i).getValue().get(0) == predictedResults.get(i)) amountAccurate++;
        }
        return (amountAccurate / testingData.size());
    }

    // Writes a list of records (folds) to a CSV file.
    public static void writeCSVFileForFold(String fileName, List<Map.Entry<String, List<Integer>>> records) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, List<Integer>> entry : records) {
                StringBuilder sb = new StringBuilder();
                sb.append(entry.getKey());
                for (Integer val : entry.getValue()) {
                    sb.append(",").append(val);
                }
                writer.println(sb.toString());
            }
            System.out.println("Wrote CSV file: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method to execute the program.
    public static void main(String[] args) {
        // For training and testing datasets, assume they have a header.
        Map<String, List<Integer>> trainingDataSet = readCSV("Training dataset.csv", true);
        Map<String, List<Integer>> testingDataSet = readCSV("Testing dataset.csv", true);

        List<Map.Entry<String, List<Integer>>> indexTrainingReference = new ArrayList<>(trainingDataSet.entrySet());
        List<Map.Entry<String, List<Integer>>> indexTestingReference = new ArrayList<>(testingDataSet.entrySet());

        List<String> results = new ArrayList<>();
        List<Integer> valuesBool = new ArrayList<>();

        // Run predictions for the testing dataset.
        for (int i = 0; i < indexTestingReference.size(); i++) {
            String rawPrediction = predictRecord(indexTestingReference.get(i), trainingDataSet, 7);
            String predictionWithLabel = rawPrediction.replace(" : ", " - Predicted: ");
            int actualValue = indexTestingReference.get(i).getValue().get(0);
            String actualLabel = (actualValue == 1) ? "90+" : "90-";

            String combinedLine = String.format("%-60s   Actual: %s", predictionWithLabel, actualLabel);
            results.add(combinedLine);

            if (rawPrediction.contains("90+")) {
                valuesBool.add(1);
            } else if (rawPrediction.contains("90-")) {
                valuesBool.add(0);
            }
        }

        // Write the prediction results to a file.
        try {
            writeResultsToFile(results, "resultFile.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Prepare a file comparing real vs predicted labels.
        results.clear();
        results.add("Real Grade\tPredicted Grade");
        for (int i = 0; i < indexTestingReference.size(); i++) {
            String temp = "";
            if (predictRecord(indexTestingReference.get(i), trainingDataSet, 7).contains("90+")) {
                temp += "1\t\t";
            } else if (predictRecord(indexTestingReference.get(i), trainingDataSet, 7).contains("90-")) {
                temp += "0\t\t";
            }
            temp += indexTestingReference.get(i).getValue().get(0);
            results.add(temp);
        }

        try {
            writeResultsToFile(results, "requiredOutput.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        double originalAccuracy = calculateAccuracy(indexTestingReference, valuesBool);
        System.out.printf("Accuracy on original test dataset: %.2f%%%n", originalAccuracy * 100);

        // Begin 5-fold cross-validation
        Map<String, List<Integer>> completeDataSet = readCSV("Full Wine Data.csv", false); //skip header
        List<Map.Entry<String, List<Integer>>> dataList = new ArrayList<>(completeDataSet.entrySet());

        // Shuffle data with a fixed seed for reproducibility.
        Collections.shuffle(dataList, new Random(42));

        int foldCount = 5;
        int foldSize = dataList.size() / foldCount;
        List<List<Map.Entry<String, List<Integer>>>> folds = new ArrayList<>();

        // Split data into 5 folds.
        for (int i = 0; i < foldCount; i++) {
            int start = i * foldSize;
            int end = (i == foldCount - 1) ? dataList.size() : (i + 1) * foldSize;
            folds.add(new ArrayList<>(dataList.subList(start, end)));
        }

        // Write each fold to an individual CSV file.
        for (int i = 0; i < foldCount; i++) {
            String foldCSVFile = "fold_" + (i + 1) + ".csv";
            writeCSVFileForFold(foldCSVFile, folds.get(i));
        }

        List<Double> foldAccuracies = new ArrayList<>();

        // Perform cross-validation across each fold.
        for (int currentFoldIndex = 0; currentFoldIndex < foldCount; currentFoldIndex++) {
            // Use the current fold as the test set.
            List<Map.Entry<String, List<Integer>>> testSet = folds.get(currentFoldIndex);

            // Build the training set by combining all other folds.
            List<Map.Entry<String, List<Integer>>> trainingSet = new ArrayList<>();
            for (int foldIndex = 0; foldIndex < foldCount; foldIndex++) {
                if (foldIndex == currentFoldIndex) continue;
                trainingSet.addAll(folds.get(foldIndex));
            }

            // Convert the training set list into a map for the prediction function.
            Map<String, List<Integer>> trainingData = new LinkedHashMap<>();
            for (Map.Entry<String, List<Integer>> entry : trainingSet) {
                trainingData.put(entry.getKey(), entry.getValue());
            }

            // Begin Confusion Matrix Tracking for the Current Fold
            int truePositives = 0;
            int falsePositives = 0;
            int trueNegatives = 0;
            int falseNegatives = 0;

            // Predict outcomes for the test set.
            List<Integer> predictedResults = new ArrayList<>();
            for (Map.Entry<String, List<Integer>> testRecord : testSet) {
                String prediction = predictRecord(testRecord, trainingData, 9);
                int predictedLabel = prediction.contains("90+") ? 1 : 0;
                int actualLabel = testRecord.getValue().get(0);

                // Update confusion matrix counts.
                if (actualLabel == 1) {
                    if (predictedLabel == 1) {
                        truePositives++;
                    } else {
                        falseNegatives++;
                    }
                } else { // actualLabel == 0
                    if (predictedLabel == 0) {
                        trueNegatives++;
                    } else {
                        falsePositives++;
                    }
                }

                predictedResults.add(predictedLabel);
            }

            // Calculate and print accuracy for the current fold.
            double accuracy = calculateAccuracy(testSet, predictedResults);
            foldAccuracies.add(accuracy);
            System.out.printf("Accuracy for Fold %d: %.2f%%%n", currentFoldIndex + 1, accuracy * 100);

            // Output the confusion matrix for this fold.
            System.out.println("Confusion Matrix for Fold " + (currentFoldIndex + 1) + ":");
            System.out.println("          Predicted");
            System.out.println("          1      0");
            System.out.println("Actual 1: " + truePositives + "      " + falseNegatives);
            System.out.println("Actual 0: " + falsePositives + "      " + trueNegatives);
        }

        // Compute and print average accuracy across all folds.
        double totalAccuracy = 0;
        for (Double acc : foldAccuracies) {
            totalAccuracy += acc;
        }
        double averageAccuracy = totalAccuracy / foldCount;
        System.out.printf("Average accuracy after 5-fold cross-validation: %.2f%%%n", averageAccuracy * 100);
    }
}
