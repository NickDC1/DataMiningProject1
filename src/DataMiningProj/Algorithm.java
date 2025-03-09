//test
package DataMiningProj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class Algorithm {
	//placeholder
	// Calculates Jaccard similarity between two sets of features.
	private static double jaccardSimilarity(double[] a, double[] b) {
	    int intersection = 0, union = 0; // Counts shared features (intersection) and total unique features (union).
	    for (int i = 0; i < a.length; i++) { // Loop through each feature, ignoring the last column (wine rating).
	        if (a[i] == 1 || b[i] == 1) union++; // Count total occurrences of a feature in either instance.
	        if (a[i] == 1 && b[i] == 1) intersection++; // Count features that appear in both instances.
	    }
	    return union == 0 ? 0.0 : ((double) intersection / union); // Compute Jaccard similarity; 1.0 = identical, 0.0 = no common features.
	}
	// Writes classification results to a file.
	private static void writeResultsToFile(List<String> results, String filename) throws IOException {
	        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
	        for (String line : results) {
	            writer.write(line);
	            writer.newLine();
	        }
	        writer.close();
	}
	
	public static double jaccardCoefficient(List<Integer> primaryRecord, List<Integer> comparedRecord) {
		//Primary Record is the record that we are comparing comparedRecord to to see how close comparedRecord is to it.
		int a;
		int b;
		
		double p = 0;
		double q = 0;
		double r = 0;
		
		for (int i = 1; i < primaryRecord.size(); i++) {
			
			a = primaryRecord.get(i);
			b = comparedRecord.get(i);
			
			if (a == 1 && b == 1) {
				//If Both are 1 (union)
				p++;
			} else if (a == 1 && b == 0) {
				//Only if first data value is 1 and other is 0
				q++;
			} else if (a == 0 && b == 1) {
				//Only if second data value is 1 and other is 0
				r++;
			}
			
		}
		
		return (p/(p+q+r));
	}
	
	//Read file
	public static Map<String, List<Integer>> readCSV(String filePath) {
        String line;
        Map<String, List<Integer>> test = new LinkedHashMap();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
        	br.readLine();
            while ((line = br.readLine()) != null) {
            	List<Integer> temporary = new ArrayList<Integer>();
                String[] values = line.split(","); // Splitting CSV by commas
                for (int i = 0; i < values.length; i++) {
                	if (i != 0) {
                		temporary.add(Integer.parseInt(values[i]));
                	}
                }
                test.put(values[0], temporary);
            }
            
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
        return test;
    }
	
	/*
	 * Method predicts rather "predictRecord" will be 90+ or 90- based on the records in "comparisonDataSet"
	 */
	
	public static String predictRecord(Map.Entry<String, List<Integer>> predictRecord, Map<String, List<Integer>> comparisonDataSet, int K) {
		
		//Calculate coefficients for record and comparison dataset, place it in hashmap where key is the coefficient, value is the entryset of the comparison data set
		Map<Map.Entry<String, List<Integer>>, Double> coefficients = new LinkedHashMap<>();
		
		for (Map.Entry<String, List<Integer>> iterator : comparisonDataSet.entrySet()) {
			coefficients.put(iterator, jaccardCoefficient(predictRecord.getValue(), iterator.getValue()));
		}
		
		//Sort for prediction with K
		List<Map.Entry<Map.Entry<String, List<Integer>>, Double>> sortedList = coefficients.entrySet()
                .stream()
                .sorted(Map.Entry.<Map.Entry<String, List<Integer>>, Double>comparingByValue(Comparator.reverseOrder())) // Sort by values
                .collect(Collectors.toList());
		
		int plusCount = 0;
		int minusCount = 0;
		
		for (int i = 0; i < K; i++) {
			if (sortedList.get(i).getKey().getValue().get(0) == 1) {
				plusCount++;
			} else {
				minusCount++;
			}
		}
		
		String returnString = "";
		if (plusCount > minusCount) {
			returnString = predictRecord.getKey() + " : 90+";
		} else if (plusCount < minusCount) {
			returnString = predictRecord.getKey() + " : 90-";
		} else {
			returnString = predictRecord.getKey() + "Neutral";
		}
		
		return returnString;
		
	}
	
	public static Double calculateAccuracy(List<Map.Entry<String, List<Integer>>> testingData, List<Integer> predictedResults) {
		
		double amountAccurate = 0;
		
		for (int i = 0; i < testingData.size(); i++) {
			if (testingData.get(i).getValue().get(0) == predictedResults.get(i)) {
				amountAccurate++;
			}
		}
		
		System.out.println(amountAccurate/testingData.size());
		
		return (amountAccurate/testingData.size());
	}
	
	public static void main(String[] args) {
		Map<String, List<Integer>> trainingDataSet = readCSV("Training dataset.csv");
		Map<String, List<Integer>> testingDataSet = readCSV("Testing dataset.csv");
		
        List<Map.Entry<String, List<Integer>>> indexTrainingReference = new ArrayList<>();
        List<Map.Entry<String, List<Integer>>> indexTestingReference = new ArrayList<>();
        
        for (Map.Entry<String, List<Integer>> entry : trainingDataSet.entrySet()) {
            indexTrainingReference.add(entry);
        }
        
        for (Map.Entry<String, List<Integer>> entry : testingDataSet.entrySet()) {
        	indexTestingReference.add(entry);
        }
        
        
        
        //Delete later
        List<Map.Entry<String, List<Integer>>> entryList = new ArrayList<>(trainingDataSet.entrySet());
        List<Map.Entry<String, List<Integer>>> secondEntryList = new ArrayList<>(testingDataSet.entrySet());
        List<String> results = new ArrayList<>();
        List<Integer> valuesBool = new ArrayList<>();
        /*
        for (int i = 0; i < secondEntryList.size(); i++) {
        	results.add(predictRecord(secondEntryList.get(i), trainingDataSet, 7));
        	if (results.get(i).contains("90+")) {
        		valuesBool.add(1);
        	} else if (results.get(i).contains("90-")) {
        		valuesBool.add(0);
        	}
        }
        */
        // Header for Results file
        results.add("PREDICTED LABEL                                  ACTUAL LABEL");
        results.add("--------------------------------------------------------------------------");

        // Output results with Prediction vs Actual
        for (int i = 0; i < secondEntryList.size(); i++) {
            // Predict
            String rawPrediction = predictRecord(secondEntryList.get(i), trainingDataSet, 9);

            // Transform "WineName : 90+" => "WineName - Predicted: 90+"
            String predictionWithLabel = rawPrediction.replace(" : ", " - Predicted: ");

            //  Actual label
            int actualValue = secondEntryList.get(i).getValue().get(0);
            String actualLabel = (actualValue == 1) ? "90+" : "90-";

            String combinedLine = String.format("%-60s   Actual: %s", predictionWithLabel, actualLabel);
            results.add(combinedLine);

            // Track predicted label for accuracy
            if (rawPrediction.contains("90+")) {
                valuesBool.add(1);
            } else if (rawPrediction.contains("90-")) {
                valuesBool.add(0);
            }
        }
        try {
			writeResultsToFile(results, "resultFile.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
        /*
         * Generate word document required format
         */
        results.clear();
        results.add("Real Grade\tPredicted Grade");
        for (int i = 0; i < secondEntryList.size(); i++) {
        	String temp = "";
        	if (predictRecord(secondEntryList.get(i), trainingDataSet, 7).contains("90+")) {
        		temp += "1\t\t";
        	} else if (predictRecord(secondEntryList.get(i), trainingDataSet, 7).contains("90-")) {
        		temp += "0\t\t";
        	}
        	
        	temp += secondEntryList.get(i).getValue().get(0);
        	
        	results.add(temp);
        	
        }
        
        try {
        	writeResultsToFile(results, "requiredOutput.txt");
        } catch (IOException e) {
        	e.printStackTrace();
        }
        
        calculateAccuracy(secondEntryList, valuesBool);
        
        
        
	}

}
