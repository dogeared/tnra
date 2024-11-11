package com.afitnerd.tnra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//public class PhoneNumberMatcher {
//    public static List<Map<String, String>> matchPhoneNumbers(String[] phoneNumbers) {
//        List<Map<String, String>> pairsList = new ArrayList<>();
//        if (phoneNumbers == null || phoneNumbers.length < 2) {
//            return pairsList; // Return empty list if input is empty, null, or too short
//        }
//
//        // Loop over each phone number
//        for (int i = 0; i < phoneNumbers.length; i++) {
//            // Pair it with every other phone number
//            for (int j = 0; j < phoneNumbers.length; j++) {
//                if (i != j) { // Ensure we don't pair a number with itself
//                    Map<String, String> pair = new HashMap<>();
//                    pair.put("caller", phoneNumbers[i]);
//                    pair.put("receiver", phoneNumbers[j]);
//                    pairsList.add(pair);
//                }
//            }
//        }
//
//        return pairsList;
//    }
//
//    public static void main(String[] args) {
//        String[] phoneNumbers = {"Ale", "Andrew", "Lawrence", "Micah", "Moshe", "Peter"};
//        List<Map<String, String>> matchedNumbers = matchPhoneNumbers(phoneNumbers);
//
//        for (Map<String, String> pair : matchedNumbers) {
//            System.out.println("Caller: " + pair.get("caller") + ", Receiver: " + pair.get("receiver"));
//        }
//    }
//}
public class PhoneNumberMatcher {
    static List<String[]> createPairs(String[] names) {
        List<String[]> pairs = new ArrayList<>();
        int n = names.length;

        // Determine the number of rounds based on the number of participants
        int rounds = n % 2 == 0 ? n - 1 : n;  // If odd, each person needs to be included as a caller and receiver

        // We will shift everyone in the list cyclically
        for (int i = 0; i < rounds; i++) {
            for (int j = 0; j < n / 2; j++) {
                // Calculate pair indices for this round
                int first = j;
                int second = (n - j - 1 + i) % n;

                if (first != second) {  // Ensure no one calls themselves
                    pairs.add(new String[]{names[first], names[second]});
                }
            }

            // Rotate the array elements for the next round
            String temp = names[n - 1];
            System.arraycopy(names, 0, names, 1, n - 1);
            names[0] = temp;
        }

        return pairs;
    }

    public static void main(String[] args) {
        String[] names = {"Alice", "Bob", "Charlie", "David", "Eve"};
        List<String[]> pairs = createPairs(names);
        for (String[] pair : pairs) {
            System.out.println(pair[0] + " calls " + pair[1]);
        }
    }
}