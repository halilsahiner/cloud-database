package de.tum.i13.shared;

import java.util.Comparator;

public class AddressComparator implements Comparator<String> {
    @Override
    public int compare(String s1, String s2) {
        return HashConverter.getMd5(s1).compareTo(HashConverter.getMd5(s2));
    }
}
