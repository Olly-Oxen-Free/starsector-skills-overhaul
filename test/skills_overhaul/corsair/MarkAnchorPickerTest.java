package skills_overhaul.corsair;

import java.util.Arrays;
import java.util.Random;

public class MarkAnchorPickerTest {
    public static void main(String[] args) {
        // test 1: the only hostile market is 40 LY away -> no anchor (caller refunds)
        assert MarkAnchorPicker.pick(Arrays.asList(40f), 10f, new Random(1)) == -1
                : "a market beyond SEARCH_LY must not anchor a mark";

        // test 2: no candidates at all -> no anchor
        assert MarkAnchorPicker.pick(Arrays.<Float>asList(), 10f, new Random(1)) == -1;

        // test 3: the first market in economy order is far; only the near one may be picked
        for (long seed = 0; seed < 50; seed++) {
            int i = MarkAnchorPicker.pick(Arrays.asList(40f, 6f, 25f), 10f, new Random(seed));
            assert i == 1 : "picked index " + i + " for seed " + seed;
        }

        // test 4: the boundary (exactly SEARCH_LY) is in range
        assert MarkAnchorPicker.pick(Arrays.asList(10f), 10f, new Random(1)) == 0;

        // test 5: several in range -> every one is reachable, and the choice follows rng
        boolean saw0 = false, saw2 = false;
        Random shared = new Random(42);
        for (int n = 0; n < 200; n++) {
            int i = MarkAnchorPicker.pick(Arrays.asList(3f, 50f, 8f), 10f, shared);
            assert i == 0 || i == 2 : "out-of-range index " + i;
            if (i == 0) saw0 = true;
            if (i == 2) saw2 = true;
        }
        assert saw0 && saw2;
        assert MarkAnchorPicker.pick(Arrays.asList(3f, 50f, 8f), 10f, new Random(7))
                == MarkAnchorPicker.pick(Arrays.asList(3f, 50f, 8f), 10f, new Random(7));

        System.out.println("MarkAnchorPickerTest: all passed.");
    }
}
