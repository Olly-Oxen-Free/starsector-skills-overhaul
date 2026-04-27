package skills_overhaul.zealot;

import java.util.HashMap;
import java.util.Map;

public class OathServiceTest {
    public static void main(String[] args) throws Exception {
        Map<String,Object> mem = new HashMap<>();
        assert OathService.getCurrentOath(mem) == null;

        OathService.setOath(mem, Oath.BLACK_FLAG);
        assert OathService.getCurrentOath(mem) == Oath.BLACK_FLAG;

        OathService.setOath(mem, Oath.SINGULARITY);
        assert OathService.getCurrentOath(mem) == Oath.SINGULARITY;

        assert OathService.SWAP_SP_COST == 3;

        OathService.setOath(mem, Oath.MANDATE);
        assert !OathService.allowsAICoreOfficer(mem);
        OathService.setOath(mem, Oath.SINGULARITY);
        assert OathService.allowsAICoreOfficer(mem);
        OathService.setOath(mem, Oath.BLACK_FLAG);
        assert OathService.allowsAICoreOfficer(mem);

        System.out.println("ALL PASSED");
    }
}
