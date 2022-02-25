package com.aspicereporting.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public final class CsvFileUtils {
    public static Character detectDelimiter(InputStream is)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<Character> possibleDelimiters = Arrays.asList(';',',');

        String headerLine = null;
        try {
            headerLine = reader.readLine();
            //is.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (var possibleDelimiter : possibleDelimiters)
        {
            if (headerLine.contains(Character.toString(possibleDelimiter)))
            {
                return possibleDelimiter;
            }
        }

        return possibleDelimiters.get(0);
    }
}
