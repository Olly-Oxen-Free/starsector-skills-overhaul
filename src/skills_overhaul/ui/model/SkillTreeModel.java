package skills_overhaul.ui.model;

import skills_overhaul.level.PurchasePlan;
import skills_overhaul.level.SpendService;
import skills_overhaul.state.SkillMemory;
import skills_overhaul.state.Tree;
import skills_overhaul.tree.Node;
import skills_overhaul.tree.PrereqValidator;
import skills_overhaul.tree.TreeDef;

import java.util.*;

/**
 * Headless state of one skills tab: classifies every node as OWNED, PENDING,
 * UNLOCKABLE or BLOCKED(reason) from a memory snapshot, the tab's TreeDefs and a
 * pending set, and owns the pending set's add/remove rules. Never touches Global.
 *
 * <p>Reasons are checked in order PREREQ, TIER, BUDGET. Budget is cumulative: a
 * candidate is affordable when the whole pending set, dry-run in topological order,
 * leaves enough points for it ({@link PurchasePlan}, {@link SpendService} pool order).
 */
public final class SkillTreeModel {

    /** Why {@link #add} refused a node. {@code status} is null for an unknown id. */
    public static final class Rejection {
        public final String nodeId;
        public final NodeStatus status;

        Rejection(String nodeId, NodeStatus status) {
            this.nodeId = nodeId;
            this.status = status;
        }

        /** Blocking reason, or null when the node was unknown, owned or already pending. */
        public NodeStatus.Reason getReason() {
            return status == null ? null : status.reason;
        }

        @Override
        public String toString() { return "Rejection(" + nodeId + ", " + status + ")"; }
    }

    private final Tree tab;
    private final List<TreeDef> defs;
    private final Map<String,Object> snapshot;
    private final Set<String> pending = new LinkedHashSet<String>();

    /**
     * @param tab      tab whose point pools pay for nodes
     * @param defs     every TreeDef of the tab
     * @param snapshot whole-tab memory snapshot; read only, never modified
     */
    public SkillTreeModel(Tree tab, List<TreeDef> defs, Map<String,Object> snapshot) {
        this.tab = tab;
        this.defs = defs;
        this.snapshot = snapshot;
    }

    public Tree getTab() { return tab; }

    /** Pending node ids in insertion order (read only). */
    public Set<String> getPending() { return Collections.unmodifiableSet(pending); }

    /** Status of {@code nodeId}, or null when no TreeDef of the tab has it. */
    public NodeStatus status(String nodeId) {
        TreeDef def = defOf(nodeId);
        if (def == null) return null;
        if (Boolean.TRUE.equals(snapshot.get(SkillMemory.purchasedKey(nodeId)))) {
            return NodeStatus.OWNED;
        }
        if (pending.contains(nodeId)) return NodeStatus.PENDING;

        PrereqValidator.Check c = PrereqValidator.check(def, snapshot, pending, nodeId);
        if (c == PrereqValidator.Check.PREREQ) return NodeStatus.blocked(NodeStatus.Reason.PREREQ);
        if (c == PrereqValidator.Check.TIER) return NodeStatus.blocked(NodeStatus.Reason.TIER);

        int cost = def.getNode(nodeId).getCost();
        PurchasePlan.Result plan = PurchasePlan.dryRun(snapshot, tab, defs, pending);
        if (!plan.success) {
            // Pending set no longer fits the snapshot; report the batch's own failure.
            if (plan.reason == PurchasePlan.Reason.BUDGET) {
                return NodeStatus.budget(plan.shortBy + cost);
            }
            if (plan.reason == PurchasePlan.Reason.TIER) {
                return NodeStatus.blocked(NodeStatus.Reason.TIER);
            }
            return NodeStatus.blocked(NodeStatus.Reason.PREREQ);
        }
        SpendService.Result r = SpendService.spend(plan.after, tab, nodeId, cost);
        if (!r.success) return NodeStatus.budget(PurchasePlan.shortBy(r, cost));
        return NodeStatus.UNLOCKABLE;
    }

    /** Status of every node of every TreeDef of the tab, keyed by node id. */
    public Map<String, NodeStatus> statuses() {
        Map<String, NodeStatus> out = new LinkedHashMap<String, NodeStatus>();
        for (TreeDef d : defs) {
            for (Node n : d.getNodes()) out.put(n.getId(), status(n.getId()));
        }
        return out;
    }

    /**
     * Adds {@code nodeId} to pending when it is UNLOCKABLE. Otherwise returns a
     * rejection naming the node and its status and leaves pending unchanged.
     *
     * @return null on success
     */
    public Rejection add(String nodeId) {
        NodeStatus s = status(nodeId);
        if (s == null || s.state != NodeStatus.State.UNLOCKABLE) {
            return new Rejection(nodeId, s);
        }
        pending.add(nodeId);
        return null;
    }

    /**
     * Removes {@code nodeId} and every pending node that no longer passes
     * {@link PrereqValidator#check} (clauses and tier gate) against owned plus the
     * remaining pending set, repeated until stable.
     *
     * @return removed ids, {@code nodeId} first; empty when it was not pending
     */
    public List<String> remove(String nodeId) {
        List<String> removed = new ArrayList<String>();
        if (!pending.remove(nodeId)) return removed;
        removed.add(nodeId);
        boolean changed = true;
        while (changed) {
            changed = false;
            Iterator<String> it = pending.iterator();
            while (it.hasNext()) {
                String id = it.next();
                TreeDef def = defOf(id);
                Set<String> others = new LinkedHashSet<String>(pending);
                others.remove(id);
                if (def == null
                        || PrereqValidator.check(def, snapshot, others, id) != PrereqValidator.Check.OK) {
                    it.remove();
                    removed.add(id);
                    changed = true;
                }
            }
        }
        return removed;
    }

    private TreeDef defOf(String nodeId) {
        for (TreeDef d : defs) if (d.getNode(nodeId) != null) return d;
        return null;
    }
}
