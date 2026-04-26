package skills_overhaul.commandant;

import java.util.Arrays;
import java.util.List;

public class ObjectiveTrackerTest {
    public static void main(String[] args) throws Exception {
        List<Integer> owners = Arrays.asList(0, 0, 1, -1);
        assert ObjectiveTracker.countOwnedBy(owners, 0) == 2;
        assert ObjectiveTracker.countOwnedBy(owners, 1) == 1;
        assert ObjectiveTracker.countOwnedBy(owners, -1) == 1;

        assert ObjectiveTracker.total(owners) == 4;

        assert ObjectiveTracker.countOwnedBy(java.util.Collections.<Integer>emptyList(), 0) == 0;
        assert ObjectiveTracker.total(java.util.Collections.<Integer>emptyList()) == 0;

        System.out.println("ALL PASSED");
    }
}
