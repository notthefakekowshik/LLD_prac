package com.kowshik.Graphs;

public class DSUDemo {

    public static void main(String[] args) {
        int n = 10;

        System.out.println("--- 1. Naive DSU ---");
        NaiveDSU naive = new NaiveDSU(n);
        naive.union(1, 2);
        naive.union(2, 3);
        System.out.println("Find(3): " + naive.find(3)); // Traverses 3 -> 2 -> 1

        System.out.println("\n--- 2. Path Compression Only ---");
        PathCompressionOnlyDSU pc = new PathCompressionOnlyDSU(n);
        pc.union(1, 2);
        pc.union(2, 3);
        System.out.println("Find(3) (First time): " + pc.find(3)); // Compresses path
        System.out.println("Find(3) (Second time): " + pc.find(3)); // Instant

        System.out.println("\n--- 3. Union by Rank Only ---");
        UnionByRankOnlyDSU rankOnly = new UnionByRankOnlyDSU(n);
        rankOnly.union(1, 2);
        rankOnly.union(3, 4);
        rankOnly.union(1, 3); // Merges two trees smartly based on height
        System.out.println("Find(4): " + rankOnly.find(4)); // Standard logN traversal

        System.out.println("\n--- 4. Fully Optimized ---");
        FullyOptimizedDSU opt = new FullyOptimizedDSU(n);
        opt.union(1, 2);
        opt.union(3, 4);
        opt.union(1, 3);
        System.out.println("Find(4): " + opt.find(4)); // Smart merge + Path compression
    }

    // ==========================================
    // 1. Brute Force DSU (Naive)
    // Bottleneck: Creates skewed trees (linked lists). O(N) per operation.
    // ==========================================
    static class NaiveDSU {

        int[] parent;

        public NaiveDSU(int n) {
            parent = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                parent[i] = i;
            }
        }

        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            // Simple recursion without updating parent[i]
            return find(parent[i]);
        }

        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                // Arbitrary assignment: Always attach i's tree to j
                // This is dangerous and causes skewing
                parent[rootI] = rootJ;
            }
        }

    }

    // ==========================================
    // 2. Path Compression Only (No Rank)
    // Bottleneck: Vulnerable to "Expensive First Access".
    // Worst case construction can still be O(N) before compression kicks in.
    // ==========================================
    static class PathCompressionOnlyDSU {

        int[] parent;

        public PathCompressionOnlyDSU(int n) {
            parent = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                parent[i] = i;
            }
        }

        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            // Optimization: Path Compression
            // Points node directly to root for future lookups
            parent[i] = find(parent[i]);
            return parent[i];
        }

        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                // Still using arbitrary assignment (Blind Union)
                parent[rootI] = rootJ;
            }
        }

    }

    // ==========================================
    // 3. Union by Rank Only (No Path Compression)
    // Bottleneck: Tree height is limited to Log(N), but we pay that
    // Log(N) cost on EVERY find operation. No caching.
    // ==========================================
    static class UnionByRankOnlyDSU {

        int[] parent;
        int[] rank;

        public UnionByRankOnlyDSU(int n) {
            parent = new int[n + 1];
            rank = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                parent[i] = i;
                rank[i] = 0; // Height of tree
            }
        }

        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            // No Path Compression: Just traverse
            return find(parent[i]);
        }

        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);

            if (rootI == rootJ) {
                return;
            }

            // Optimization: Union by Rank
            // Attach smaller tree to larger tree
            if (rank[rootI] < rank[rootJ]) {
                parent[rootI] = rootJ;
            } else if (rank[rootJ] < rank[rootI]) {
                parent[rootJ] = rootI;
            } else {
                parent[rootJ] = rootI;
                rank[rootI]++;
            }
        }

    }

    // ==========================================
    // 4. Fully Optimized (Rank + Path Compression)
    // Efficiency: O(alpha(N)) - Amortized Constant Time
    // ==========================================
    static class FullyOptimizedDSU {

        int[] parent;
        int[] rank;

        public FullyOptimizedDSU(int n) {
            parent = new int[n + 1];
            rank = new int[n + 1];
            for (int i = 0; i <= n; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }

        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            // Optimization 1: Path Compression
            parent[i] = find(parent[i]);
            return parent[i];
        }

        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);

            if (rootI == rootJ) {
                return;
            }

            // Optimization 2: Union by Rank
            if (rank[rootI] < rank[rootJ]) {
                parent[rootI] = rootJ;
            } else if (rank[rootJ] < rank[rootI]) {
                parent[rootJ] = rootI;
            } else {
                parent[rootJ] = rootI;
                rank[rootI]++;
            }
        }

    }

}