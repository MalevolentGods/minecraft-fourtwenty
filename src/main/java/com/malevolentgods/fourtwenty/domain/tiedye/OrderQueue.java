package com.malevolentgods.fourtwenty.domain.tiedye;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A colony's tie-dye orders (contracts/orders.md): worked head first, a batch at a time. A batch's supplies are fixed when it starts
 * and delivered line by line, as the Weed College's head study. Pure and tested.
 */
public final class OrderQueue {
    /** One order. {@code wood} names a bed's wood, otherwise empty. */
    public record Order(int id, int pattern, String colourway, TieDyeRules.Kind kind, String wood, int count, int made, UUID by) {
        public int left() { return count - made; }
    }

    private final List<Order> orders = new ArrayList<>();
    private int nextId = 1;
    private List<TieDyeRules.Supply> batch;          // the head's current batch, or null
    private int batchItems;
    private final List<Integer> delivered = new ArrayList<>();
    private long totalMade;

    public List<Order> orders() { return Collections.unmodifiableList(orders); }

    public Order head() { return orders.isEmpty() ? null : orders.getFirst(); }

    public long totalMade() { return totalMade; }

    public Order add(int pattern, String colourway, TieDyeRules.Kind kind, String wood, int count, UUID by) {
        Order o = new Order(nextId++, pattern, colourway, kind, wood == null ? "" : wood, count, 0, by);
        orders.add(o);
        return o;
    }

    /** Cancels an order; the head only while its batch has nothing delivered. Returns whether it went. */
    public boolean cancel(int id) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).id() != id) continue;
            if (i == 0 && batchUnderWay()) return false;
            orders.remove(i);
            if (i == 0) clearBatch();
            return true;
        }
        return false;
    }

    public boolean batchStarted() { return batch != null; }

    public boolean batchUnderWay() {
        for (int d : delivered) if (d > 0) return true;
        return false;
    }

    public int batchItems() { return batchItems; }

    public List<TieDyeRules.Supply> batch() { return batch == null ? null : List.copyOf(batch); }

    public List<Integer> delivered() { return List.copyOf(delivered); }

    /** Fixes the head's next batch: {@code items} of it and what they use. */
    public void startBatch(int items, List<TieDyeRules.Supply> supplies) {
        if (head() == null || batch != null) return;
        batch = new ArrayList<>(supplies);
        batchItems = items;
        delivered.clear();
        for (int i = 0; i < batch.size(); i++) delivered.add(0);
    }

    public List<TieDyeRules.Supply> missing() {
        if (batch == null) return List.of();
        List<TieDyeRules.Supply> out = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            int left = batch.get(i).count() - delivered.get(i);
            if (left > 0) out.add(new TieDyeRules.Supply(batch.get(i).item(), left));
        }
        return out;
    }

    public void deliver(int line, int count) {
        if (batch == null || line < 0 || line >= batch.size()) return;
        delivered.set(line, Math.min(batch.get(line).count(), delivered.get(line) + Math.max(0, count)));
    }

    /** The batch is dyed: the head counts its items made, and leaves the queue when it's complete. Returns the order as it stood. */
    public Order finishBatch() {
        Order h = head();
        if (h == null || batch == null) return null;
        Order done = new Order(h.id(), h.pattern(), h.colourway(), h.kind(), h.wood(), h.count(), Math.min(h.count(), h.made() + batchItems), h.by());
        totalMade += done.made() - h.made();
        if (done.left() <= 0) orders.removeFirst();
        else orders.set(0, done);
        clearBatch();
        return done;
    }

    private void clearBatch() {
        batch = null;
        batchItems = 0;
        delivered.clear();
    }

    /** Restores a saved queue. */
    public void restore(List<Order> saved, int next, List<TieDyeRules.Supply> savedBatch, int items, List<Integer> savedDelivered, long made) {
        orders.clear();
        orders.addAll(saved);
        nextId = Math.max(next, saved.stream().mapToInt(Order::id).max().orElse(0) + 1);
        batch = savedBatch == null ? null : new ArrayList<>(savedBatch);
        batchItems = items;
        delivered.clear();
        if (batch != null) {
            for (int i = 0; i < batch.size(); i++) delivered.add(i < savedDelivered.size() ? savedDelivered.get(i) : 0);
        }
        totalMade = made;
    }

    public int nextId() { return nextId; }
}
