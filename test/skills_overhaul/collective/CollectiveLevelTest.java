package skills_overhaul.collective;

public class CollectiveLevelTest {
    public static void main(String[] args) {
        assert CollectiveLevel.fromAlphaCount(0) == 0;
        assert CollectiveLevel.fromAlphaCount(1) == 1;
        assert CollectiveLevel.fromAlphaCount(3) == 3;
        assert CollectiveLevel.fromAlphaCount(99) == 3 : "Should cap at 3";
        assert CollectiveLevel.getInterfaceUses(0) == 0;
        assert CollectiveLevel.getInterfaceUses(1) == 1;
        assert CollectiveLevel.getInterfaceUses(3) == 3;
        System.out.println("CollectiveLevelTest: all passed.");
    }
}
