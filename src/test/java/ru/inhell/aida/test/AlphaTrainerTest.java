package ru.inhell.aida.test;

import org.testng.annotations.Test;
import ru.inhell.aida.oracle.AlphaOracle;

import java.io.IOException;
import java.text.ParseException;

/**
 * @author Anatoly A. Ivanov java@inhell.ru
 *         Date: 05.12.10 21:15
 */
public class AlphaTrainerTest {

    @Test
    public void train1() throws IOException, ParseException {
        AlphaOracle alphaOracle = new AlphaOracle();
        for (int l=100; l <= 150; l+=25){
            for (int p = 21; p <= 25; p++){
                alphaOracle.train(1000, l, p, 5);
            }
        }
    }

    @Test
    public void train2() throws IOException, ParseException {
        AlphaOracle alphaOracle = new AlphaOracle();
        for (int l=200; l <= 250; l+=25){
            for (int p = 5; p <= 25; p++){
                alphaOracle.train(1000, l, p, 5);
            }
        }
    }
}
