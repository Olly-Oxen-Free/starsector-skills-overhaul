package skills_overhaul.zealot;

public class ZealKillListenerTest {
    public static void main(String[] args) throws Exception {
        // Black Flag: lawful factions grant stacks, pirates don't
        assert ZealKillListener.stacksForOath(Oath.BLACK_FLAG, "hegemony", 50f) == 1;
        assert ZealKillListener.stacksForOath(Oath.BLACK_FLAG, "hegemony", 150f) == 2;
        assert ZealKillListener.stacksForOath(Oath.BLACK_FLAG, "pirates", 50f) == 0;
        assert ZealKillListener.stacksForOath(Oath.BLACK_FLAG, "tritachyon", 200f) == 2;

        // Defender: pirates/luddic_path grant stacks, remnants always 2, lawful 0
        assert ZealKillListener.stacksForOath(Oath.DEFENDER, "pirates", 50f) == 1;
        assert ZealKillListener.stacksForOath(Oath.DEFENDER, "pirates", 150f) == 2;
        assert ZealKillListener.stacksForOath(Oath.DEFENDER, "luddic_path", 50f) == 1;
        assert ZealKillListener.stacksForOath(Oath.DEFENDER, "remnants", 50f) == 2;
        assert ZealKillListener.stacksForOath(Oath.DEFENDER, "hegemony", 50f) == 0;

        // Mandate: tritachyon scales by fp, remnants always 2
        assert ZealKillListener.stacksForOath(Oath.MANDATE, "tritachyon", 50f) == 1;
        assert ZealKillListener.stacksForOath(Oath.MANDATE, "tritachyon", 150f) == 2;
        assert ZealKillListener.stacksForOath(Oath.MANDATE, "remnants", 50f) == 2;

        // Singularity: anti-AI factions always 1; capital gives 2
        assert ZealKillListener.stacksForOath(Oath.SINGULARITY, "hegemony", 50f) == 1;
        assert ZealKillListener.stacksForOathCapital(Oath.SINGULARITY, "hegemony") == 2;

        System.out.println("ALL PASSED");
    }
}
