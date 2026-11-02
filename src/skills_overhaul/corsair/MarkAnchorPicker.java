package skills_overhaul.corsair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure anchor rule for the Underground Intel fallback mark: only a market within the
 * search radius may anchor it. Kept free of game types so it can be unit-tested.
 */
public final class MarkAnchorPicker {

    /**
     * Returns the index of a candidate whose distance is at most {@code maxLY}, chosen
     * uniformly with {@code rng}, or -1 when none is in range.
     */
    public static int pick(List<Float> distancesLY, float maxLY, Random rng) {
        List<Integer> inRange = new ArrayList<>();
        for (int i = 0; i < distancesLY.size(); i++) {
            Float d = distancesLY.get(i);
            if (d != null && d <= maxLY) inRange.add(i);
        }
        if (inRange.isEmpty()) return -1;
        return inRange.get(rng.nextInt(inRange.size()));
    }

    private MarkAnchorPicker() {}
}
