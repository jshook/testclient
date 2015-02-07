/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.statgen;

import java.security.InvalidParameterException;

public class NatoPhoneticAlphabet {
    private final String[][] phoneticAlphabetNATO = {
            {"0", "dash-dash-dash-dash-dash", "zero", "ZEE-RO"},
            {"1", "dot-dash-dash-dash-dash", "one", "WUN"},
            {"2", "dot-dot-dash-dash-dash", "two", "TOO"},
            {"3", "dot-dot-dot-dash-dash", "three", "TREE"},
            {"4", "dot-dot-dot-dot-dash", "four", "FOW-ER"},
            {"5", "dot-dot-dot-dot-dot", "five", "FIFE"},
            {"6", "dash-dot-dot-dot-dot", "six", "SIX"},
            {"7", "dash-dash-dot-dot-dot", "seven", "SEV-EN"},
            {"8", "dash-dash-dash-dot-dot", "eight", "AIT"},
            {"9", "dash-dash-dash-dash-dot", "nine", "NIN-ER"},
            {"A", "dot-dash", "alfa", "AL-FAH"},
            {"B", "dash-dot-dot-dot", "bravo", "BRAH-VOH"},
            {"C", "dash-dot-dash-dot", "charlie", "CHAR-LEE"},
            {"D", "dash-dot-dot", "delta", "DELL-TAH"},
            {"E", "dot", "echo", "ECK-OH"},
            {"F", "dot-dot-dash-dot", "Foxtrot", "FOKS-TROT"},
            {"G", "dash-dash-dot", "golf", "GOLF"},
            {"H", "dot-dot-dot-dot", "hotel", "HOH-TEL"},
            {"I", "dot-dot", "india", "IN-DEE-AH"},
            {"J", "dot-dash-dash", "juliett", "JEW-LEE-ETT"},
            {"K", "dash-dot-dash", "kilo", "KEY-LOH"},
            {"L", "dot-dash-dot-dot", "lima", "LEE-MAH"},
            {"M", "dash-dash", "mike", "MIKE"},
            {"N", "dash-dot", "november", "NO-VEM-BER"},
            {"O", "dash-dash-dash", "Oscar", "OSS-CAH"},
            {"P", "dot-dash-dash-dot", "papa", "PAH-PAH"},
            {"Q", "dash-dash-dot-dash", "quebec", "KEH-BECK"},
            {"R", "dot-dash-dot", "romeo", "ROW-ME-OH"},
            {"S", "dot-dot-dot", "sierra", "SEE-AIR-RAH"},
            {"T", "dash", "Tango", "tANG-GO"},
            {"U", "dot-dot-dash", "uniform", "YOU-NEE-FORM"},
            {"V", "dot-dot-dot-dash", "victor", "VIK-TAH"},
            {"W", "dot-dash-dash", "whiskey", "WISS-KEY"},
            {"X", "dash-dot-dot-dash", "xray", "ECKS-RAY"},
            {"Y", "dash-dot-dash-dash", "yankee", "YANG-KEY"},
            {"Z", "dash-dash-dot-dot", "zulu", "ZOO-LOO"}
    };

    /**
     * Return the word associated with offset in the NATO alphabet.
     * @param offset - from 0 to 35, representing 0-9,A-Z
     * @return
     */
    public String getWord(int offset) {
        return phoneticAlphabetNATO[offset][2];
    }

    /**
     * Return the {character-string,morse,word,phonic} array tuple of the letter in the position specified,
     * between 1 and 26 inclusive
     * @param letterBetweenOneAndTwentySixInclusive the enumeration of the letter requested
     * @return A String[] containing the letter, morse, word, and phonic representations
     */
    public String[] getAlpha(int letterBetweenOneAndTwentySixInclusive) {
        if (letterBetweenOneAndTwentySixInclusive<1 || letterBetweenOneAndTwentySixInclusive>26) {
            throw new InvalidParameterException("You can only used 1-26, you used "+letterBetweenOneAndTwentySixInclusive);
        }
        return phoneticAlphabetNATO[letterBetweenOneAndTwentySixInclusive+9];
    }
    /**
     * Return the {character-string,morse,word,phonic} array tuple of the number in the position specified,
     * between 1 and 26 inclusive
     * @param digit - the actual number
     * @return A String[] containing the character-string, morse, word, and phonic representations
     */
    public String[] getDigit(int digit) {
        if (digit<0 || digit>9) {
            throw new InvalidParameterException("You can only used 0-9, you used "+digit);
        }
        return phoneticAlphabetNATO[digit];
    }

    public String[][] getAlphabet() {
        return phoneticAlphabetNATO;
    }

}
