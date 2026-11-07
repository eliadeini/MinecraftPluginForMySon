package dev.ultraenchanting.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RulesTest {
    @Test void normalization(){assertEquals("unbreaking",Rules.normalize("  Un-Breaking! "));assertEquals("עמידות",Rules.normalize("עֲמִידוּת"));}
    @ParameterizedTest @ValueSource(strings={"Unbreaking","unbreak","אמבריקינג","אנברייקינג","עמידות","אמברי"})
    void bilingualSearch(String query){assertTrue(Rules.score(query,List.of("Unbreaking","אמבריקינג","אנברייקינג","עמידות"))>0);}
    @Test void smallTypos(){assertTrue(Rules.score("unbreking",List.of("Unbreaking"))>0);assertEquals(0,Rules.score("xyz",List.of("Unbreaking")));}
    @Test void substring(){assertTrue(Rules.score("break",List.of("Unbreaking"))>0);}
    @Test void ranking(){assertTrue(Rules.score("mending",List.of("Mending"))>Rules.score("mending",List.of("Mending Curse")));}
    @ParameterizedTest @ValueSource(strings={"0","-1","100000001","2147483648","99999999999999999999","1.0","1e3","+1"," 2","2 ","","NaN","١"})
    void rejectsInvalidLevels(String value){assertThrows(IllegalArgumentException.class,()->Rules.level(value));}
    @Test void limits(){assertEquals(1,Rules.level("1"));assertEquals(100_000_000,Rules.level("100000000"));assertThrows(IllegalArgumentException.class,()->Rules.level(null));}
    @Test void conflictsAreSymmetricAndOnlyExplicit(){
        var graph=Map.of("minecraft:unbreaking",Set.of("ultraenchanting:curse_of_fragility"));
        assertEquals(Optional.of("minecraft:unbreaking"),Rules.conflict("ultraenchanting:curse_of_fragility",Set.of("minecraft:unbreaking"),graph));
        assertEquals(Optional.of("ultraenchanting:curse_of_fragility"),Rules.conflict("minecraft:unbreaking",Set.of("ultraenchanting:curse_of_fragility"),graph));
        assertTrue(Rules.conflict("minecraft:mending",Set.of("minecraft:infinity"),graph).isEmpty());
        assertTrue(Rules.conflict("minecraft:sharpness",Set.of("minecraft:smite"),graph).isEmpty());
        assertTrue(Rules.conflict("minecraft:unbreaking",Set.of("minecraft:unbreaking"),graph).isEmpty());
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"mending,infinity","sharpness,smite","sharpness,bane_of_arthropods","silk_touch,fortune","multishot,piercing","riptide,loyalty","riptide,channeling"})
    void allowsAllRequestedVanillaPairs(String first,String second){
        assertTrue(Rules.conflict("minecraft:"+first,Set.of("minecraft:"+second),Map.of()).isEmpty());
    }
    @Test void replacePreservesOthersAndDoesNotMutateOriginal(){
        Map<String,Integer> old=Map.of("minecraft:sharpness",5,"minecraft:smite",4);
        var result=Rules.replace(old,"minecraft:sharpness",100_000_000);
        assertEquals(100_000_000,result.get("minecraft:sharpness"));assertEquals(4,result.get("minecraft:smite"));assertEquals(5,old.get("minecraft:sharpness"));
    }
    @Test void finiteSafety(){assertEquals(1,Rules.bounded(Double.NaN,1,5));assertEquals(5,Rules.bounded(100_000_000,1,5));assertEquals(1,Rules.bounded(Double.POSITIVE_INFINITY,1,5));}
}