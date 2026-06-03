package farvix.solution.api.util.other;

import lombok.experimental.UtilityClass;
import farvix.solution.api.interfaces.QuickImports;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class StringUtility implements QuickImports {
    public String randomString(int size) {
        return IntStream.range(0, size)
                .mapToObj(operand -> String.valueOf((char) new Random().nextInt('a', 'z' + 1)))
                .collect(Collectors.joining());
    }
}
