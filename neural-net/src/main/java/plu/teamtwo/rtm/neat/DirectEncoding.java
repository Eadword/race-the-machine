package plu.teamtwo.rtm.neat;

import plu.teamtwo.rtm.neural.NeuralNetwork;

import java.security.InvalidParameterException;
import java.util.*;

import static plu.teamtwo.rtm.core.util.Rand.getRandomNum;
import static plu.teamtwo.rtm.core.util.Rand.iWill;

class DirectEncoding extends Genome {
    /// Chance to mutate the edge weights.
    private static final float MUTATE_EDGE_WEIGHTS = 0.8f;
    /// Chance that an edge weight which is being mutated will be reinitialized.
    private static final float MUTATE_RESET_WEIGHT = 0.10f;
    /// Chance for a new edge to be added to the system.
    private static final float MUTATE_NEW_EDGE = 0.05f;
    // Number of times it should try to mutate a new edge
    private static final int MUTATE_NEW_EDGE_TRIES = 30;
    /// Chance for a node to be added to the system.
    private static final float MUTATE_NEW_NODE = 0.03f;
    /// Chance for an edge's enabled status to be flipped.
    private static final float MUTATE_EDGE_TOGGLE = 0.04f;
    /// True if the genome is allowed to mutate recurrent edges
    private static final boolean MUTATE_RECURRENT_EDGES = false;
    /// Chance to disable an edge if either parent had it disabled.
    private static final float CROSS_DISABLE_EDGE = 0.75f;
    /// Absolute value of the initial range for an edge weight.
    private static final float EDGE_WEIGHT_INIT_RANGE = 2.0f;
    /// Amount up or down an edge weight can be stepped.
    private static final float EDGE_WEIGHT_STEP_MAX = 2.0f;
    /// Cost of having excess nodes on distance function (c1).
    private static final float DISTANCE_EXCESS_COST = 1.0f;
    /// Cost of having disjoint nodes in distance function (c2).
    private static final float DISTANCE_DISJOINT_COST = 1.0f;
    /// Cost of average weight difference on matching edges (including disabled) in distance function (c3).
    private static final float DISTANCE_WEIGHT_DIFFERENCE_COST = 0.4f;

    private NavigableMap<Integer, Node> nodeGenes = new TreeMap<>();
    private NavigableMap<Integer, Edge> edgeGenes = new TreeMap<>();


    /**
     * Create a new DirectEncoding with the correct input and output nodes.
     *
     * @param gCache  Cached information about the nodes and edges.
     * @param inputs  Number of inputs the system should accept.
     * @param outputs Number of outputs the system should generate.
     */
    DirectEncoding(GenomeCache gCache, int inputs, int outputs) {
        if(inputs <= 0 || outputs <= 0)
            throw new InvalidParameterException("Inputs and outputs must be greater than 0.");
        DirectEncodingCache cache = (DirectEncodingCache) gCache;

        for(int i = 0; i < inputs; ++i) {
            final Node n = new Node(cache.nextNodeID(), NodeType.INPUT);
            nodeGenes.put(n.id, n);
        }
        for(int i = 0; i < outputs; ++i) {
            final Node n = new Node(cache.nextNodeID(), NodeType.OUTPUT);
            nodeGenes.put(n.id, n);
        }
    }


    /**
     * Make a deep copy of another DirectEncoding.
     *
     * @param other DirectEncoding to copy.
     */
    DirectEncoding(DirectEncoding other) {
        for(Node n : other.nodeGenes.values())
            nodeGenes.put(n.id, new Node(n));
        for(Edge e : other.edgeGenes.values())
            edgeGenes.put(e.id, new Edge(e));
    }


    /**
     * Used to create a new, empty DirectEncoding. If this is used, make sure to initialize the list
     * of nodes to include at minimum the input and output nodes.
     */
    private DirectEncoding() {
    }


    /**
     * Copies over the input and output nodes only.
     *
     * @param nodes The nodes to copy from.
     */
    private DirectEncoding(Collection<Node> nodes) {
        for(Node n : nodes)
            if(n.nodeType == NodeType.INPUT || n.nodeType == NodeType.OUTPUT)
                nodeGenes.put(n.id, new Node(n));
    }


    /**
     * Cross the genomes of two parents to create a child. This will take the disjoint and excess genes from the most
     * fit parent and randomly choose between the matching ones.
     *
     * @param cache Cached information about the nodes and edges.
     * @param p1    First parent.
     * @param p2    Second parent.
     * @return A child which is the result of crossing the genomes
     */
    public static DirectEncoding crossMultipoint(DirectEncodingCache cache, DirectEncoding p1, DirectEncoding p2, boolean average) {
        //make p1 the most fit parent, or if equal, the one with the least genes
        if(p1.getFitness() < p2.getFitness() || (p1.getFitness() == p2.getFitness() &&
                p1.edgeGenes.size() + p1.nodeGenes.size() >
                p2.edgeGenes.size() + p2.nodeGenes.size()) )
        {
            DirectEncoding t = p1;
            p1 = p2;
            p2 = t;
        }

        //go through both parents and line up innovation numbers
        // always sorted because it is a TreeSet
        DirectEncoding child = new DirectEncoding();
        Iterator<Edge> i1 = p1.edgeGenes.values().iterator();
        Iterator<Edge> i2 = p2.edgeGenes.values().iterator();

        Edge e1 = i1.hasNext() ? i1.next() : null;
        Edge e2 = i2.hasNext() ? i2.next() : null;
        while(e1 != null || e2 != null) { //run until we hit the end of both lists
            boolean step1 = false, step2 = false;
            //get the next node, or null if we have reached the end.
            if(e1 != null && e2 == null) { //e1 is an excess node
                child.edgeGenes.put(e1.id, new Edge(e1));
                step1 = true;
            }
            else if(e1 == null && e2 != null) { //e2 is an excess node
                step2 = true;
            }
            else if(e1.id < e2.id) { //e1 is a disjoint node
                child.edgeGenes.put(e1.id, new Edge(e1));
                step1 = true;
            }
            else if(e1.id == e2.id) {
                //choose either randomly from the parents or average the weight
                Edge edge = average ?
                        new Edge(e1.id, e1.fromNode, e1.toNode, (e1.weight + e2.weight) / 2.0f) :
                        new Edge(iWill(0.5f) ? e1 : e2);

                //chance to disable child if either parent is disabled
                if(!e1.enabled || !e2.enabled)
                    if(iWill(CROSS_DISABLE_EDGE))
                        edge.enabled = false;

                //add the new edge to the list
                child.edgeGenes.put(edge.id, edge);
                step1 = step2 = true;
            }
            else { // e1.id > e2.id //e2 is a disjoint node
                step2 = true;
            }

            if(step1) e1 = i1.hasNext() ? i1.next() : null;
            if(step2) e2 = i2.hasNext() ? i2.next() : null;
        }

        //Find what nodes are used
        BitSet discovered = new BitSet();
        for(Edge e : child.edgeGenes.values()) {
            discovered.set(e.toNode);
            discovered.set(e.fromNode);
        }

        //add nodes which are used by the child from either parent (must go through both lists)
        // we make the assumption that both have the same input and output nodes, so just copy
        // from the first parent.
        for(int i = discovered.nextSetBit(0); i >= 0; i = discovered.nextSetBit(i+1)) {
            Node n = p1.nodeGenes.get(i);
            if(n == null) n = p2.nodeGenes.get(i);
            if(n == null) throw new IllegalArgumentException("In DirectEncoding crossMultipoint, one of the parents " +
                    "had an edge for which it did not have the corresponding nodes.");
            child.nodeGenes.put(n.id, new Node(n));
        }

        return child;
    }


    /**
     * Compute the compatibility distance function δ. The value represents how different the two Genomes are by counting
     * the disjoint and excess edges, and the the average difference in the weights.
     *
     * @param d1 First genome.
     * @param d2 Second genome.
     * @return The compatibility distance.
     */
    public static float compatibilityDistance(DirectEncoding d1, DirectEncoding d2) {
        //go through both parents and line up innovation numbers
        // always sorted because it is a TreeSet
        Iterator<Edge> i1 = d1.edgeGenes.values().iterator();
        Iterator<Edge> i2 = d2.edgeGenes.values().iterator();

        Edge e1 = i1.hasNext() ? i1.next() : null;
        Edge e2 = i2.hasNext() ? i2.next() : null;

        int disjoint = 0, excess = 0, matching = 0;
        float matchingDiff = 0;

        while(e1 != null || e2 != null) { //run until we hit the end of both lists
            boolean step1 = false, step2 = false;
            //get the next node, or null if we have reached the end.
            if(e1 != null && e2 == null) { //e1 is an excess node
                excess++;
                step1 = true;
            } else if(e1 == null && e2 != null) { //e2 is an excess node
                excess++;
                step2 = true;
            } else if(e1.id < e2.id) { //e1 is a disjoint node
                disjoint++;
                step1 = true;
            } else if(e1.id == e2.id) {
                matchingDiff += Math.abs(e1.weight - e2.weight);
                matching++;
                step1 = step2 = true;
            } else { // e1.id > e2.id //e2 is a disjoint node
                disjoint++;
                step2 = true;
            }

            if(step1) e1 = i1.hasNext() ? i1.next() : null;
            if(step2) e2 = i2.hasNext() ? i2.next() : null;
        }
        matchingDiff /= (float) matching;

        final float n = 1.0f; //disjoint + excess + matching; //TODO: re-enable normalizing the distance?
        float distance = matchingDiff * DISTANCE_WEIGHT_DIFFERENCE_COST;
        distance += ((float) excess / n) * DISTANCE_EXCESS_COST;
        distance += ((float) disjoint / n) * DISTANCE_DISJOINT_COST;
        return distance;
    }


    /**
     * Cross the genomes of two parents to create a child. This will take the disjoint and excess genes from the most
     * fit parent and randomly choose between the matching ones.
     *
     * @param cache Cached information about the nodes and edges.
     * @param other The other parent.
     * @return A child which is the result of crossing the genomes
     */
    @Override
    public DirectEncoding crossMultipoint(GenomeCache cache, Genome other) {
        return crossMultipoint((DirectEncodingCache) cache, this, (DirectEncoding) other, false);
    }


    /**
     * Cross the genomes of two parents to create a child. This will take the disjoint and excess genes from the most
     * fit parent and average the values of the matching ones.
     *
     * @param cache Cached information about the genome.
     * @param other The other parent.
     * @return A child which is the result of crossing the genomes
     */
    @Override
    Genome crossMultipointAvg(GenomeCache cache, Genome other) {
        return crossMultipoint((DirectEncodingCache) cache, this, (DirectEncoding) other, true);
    }


    /**
     * Compute the compatibility distance function δ. The value represents how different this genome is from the other
     * one by counting the disjoint and excess edges, and the the average difference in the weights.
     *
     * @param gOther The genome to compare this one against.
     * @return The compatibility distance.
     */
    @Override
    public float compatibilityDistance(Genome gOther) {
        return compatibilityDistance(this, (DirectEncoding) gOther);
    }


    /**
     * Used for initial members of the first generation to create connections between the inputs and outputs. This
     * should not be needed after the first generation. It is reccomended that mutate be called after this function to
     * give the initial species some variation.
     *
     * @param gCache Cached information about the nodes and edges.
     */
    @Override
    public void initialize(GenomeCache gCache) {
        DirectEncodingCache cache = (DirectEncodingCache) gCache;
        //create an edge from every input to every output
        for(Node from : nodeGenes.values()) {
            for(Node to : nodeGenes.values()) {
                if(from == to || from.nodeType != NodeType.INPUT || to.nodeType != NodeType.OUTPUT) continue;
                addEdge(cache, from.id, to.id);
            }
        }
    }


    /**
     * Create a deep copy of the genome. This will enable the copy to be modified without altering the original.
     *
     * @return A duplicate of the current instance.
     */
    @Override
    public DirectEncoding duplicate() {
        return new DirectEncoding(this);
    }


    /**
     * Used to create a new DirectEncoding Cache.
     *
     * @return A new DirectEncoding Cache.
     */
    @Override
    public GenomeCache createCache() {
        return new DirectEncodingCache();
    }


    /**
     * Make random alterations to the genome (i.e. mutations). The primary changes are, 1. altering an edge weight,
     * 2. adding an edge, 3. toggling an edge, and 4. adding a new node.
     *
     * @param gCache Cached information about the nodes and edges.
     */
    @Override
    public void mutate(GenomeCache gCache) {
        DirectEncodingCache cache = (DirectEncodingCache) gCache;

        //Perform structural mutations first
        if(iWill(MUTATE_NEW_NODE)) {
            mutateNode(cache);
        }
        else if(iWill(MUTATE_NEW_EDGE)) {
            mutateEdge(cache);
        }
        else { //perform non-structural modifications
            //TODO: add traits (including activation function)

            if(iWill(MUTATE_EDGE_WEIGHTS))
                mutateWeights();

            if(iWill(MUTATE_EDGE_TOGGLE))
                mutateToggleEdge(1);
        }
    }


    private void mutateToggleEdge(int times) {
        for(int x = 0; x < times; ++x) {
            final int rand = getRandomNum(0, edgeGenes.size() - 1);
            final int id = edgeIndexToID(rand);
            final Edge e = edgeGenes.get(id);
            e.enabled = !e.enabled;
        }
    }


    /**
     * Mutates a new node along a random edge.
     * @param cache
     */
    private void mutateNode(DirectEncodingCache cache) {
        final int index = getRandomNum(0, edgeGenes.size() - 1);
        final int edge = edgeIndexToID(index);
        addNode(cache, edge);
    }


    /**
     * Mutate a new edge between two random nodes.
     *
     * @param cache Cached information about the nodes and edges.
     */
    private void mutateEdge(DirectEncodingCache cache) {
        for(int x = 0; x < MUTATE_NEW_EDGE_TRIES; ++x) {
            int from, to;
            do {
                final int fromIndex = getRandomNum(0, nodeGenes.size() - 1);
                final int toIndex = getRandomNum(0, nodeGenes.size() - 1);
                from = nodeIndexToID(fromIndex);
                to = nodeIndexToID(toIndex);
                if(++x > MUTATE_NEW_EDGE_TRIES) return;
            } while(!MUTATE_RECURRENT_EDGES && isRecurrent(from, to));

            //add the edge and make sure it is enabled if it was already there.
            if(addEdge(cache, from, to)) break;
        }
    }


    /**
     * Checks if after adding an edge between two nodes the graph would be recurrent. Note that this will always return
     * true if there was already a cycle in the graph no matter the connection which is added.
     * TODO: store information about which edges are recurrent so we can ignore those and figure out which new ones would be?
     *
     * @param from Starting node of the edge.
     * @param to   Ending node of the edge.
     * @return True if the graph has a cycle after the new edge is added.
     */
    private boolean isRecurrent(int from, int to) {
        BitSet gray = new BitSet();
        BitSet black = new BitSet();

        //start from all input nodes
        for(Node n : nodeGenes.values()) {
            if(n.nodeType == NodeType.INPUT) {
                if(isRecurrent(n.id, from, to, gray, black)) return true;
            }
        }

        return false;
    }


    /**
     * Perform DFS to check if the graph is cyclic given an extra edge not in the edge list. This will consider a
     * connection to an input node as recurrent.
     * @param node  ID of the current node.
     * @param from  ID of the starting node for then new edge.
     * @param to    ID of the ending node for the new edge.
     * @param gray  Set of grey nodes.
     * @param black Set of back nodes.
     * @return True if the graph is cyclic or connecting to an input, otherwise false.
     */
    private boolean isRecurrent(int node, int from, int to, BitSet gray, BitSet black) {
        //visit the node
        gray.set(node);

        boolean foundEdge = false;
        //go through edge genes
        for(Edge e : edgeGenes.values()) {
            if(e.fromNode == node) {
                //recurrent if it is part of our past or if it is an input node
                if(gray.get(e.toNode) || nodeGenes.get(e.toNode).nodeType == NodeType.INPUT)
                    return true;
                //if it is not already processed, perform DFS on it
                if(!black.get(e.toNode) && isRecurrent(e.toNode, from, to, gray, black))
                    return true;
            }
            //will not need to handle the extra edge
            if(e.fromNode == from && e.toNode == to) foundEdge = true;
        }
        //treat from and to as an extra edge
        if(from == node && !foundEdge) { //prevent returning true if there is another identical edge
            //recurrent if it is part of our past or if it is an input node
            if(gray.get(to) || nodeGenes.get(to).nodeType == NodeType.INPUT)
                return true;
            //if it is not already processed, perform DFS on it
            if(!black.get(to) && isRecurrent(to, from, to, gray, black))
                return true;
        }

        //leave this node
        gray.set(node, false);
        black.set(node);
        return false;
    }


    /**
     * Alter the weight on edge e. Either step it or reset it depending on chance.
     */
    private void mutateWeights() {
        for(Edge e : edgeGenes.values()) {
            if(iWill(MUTATE_RESET_WEIGHT))
                e.weight = getRandomNum(-EDGE_WEIGHT_INIT_RANGE, EDGE_WEIGHT_INIT_RANGE);
            else
                e.weight += getRandomNum(-EDGE_WEIGHT_STEP_MAX, EDGE_WEIGHT_STEP_MAX);
        }
    }


    /**
     * Add an edge between two nodes. If the edge already exists, it will return false and make no change.
     *
     * @param cache    Cached information about the nodes and edges.
     * @param nodeFrom Origin node for the edge.
     * @param nodeTo   Termination node for the edge.
     * @return True if the Edge was added successfully.
     */
    private boolean addEdge(DirectEncodingCache cache, int nodeFrom, int nodeTo) {
        //check if the edge already exists
        for(Edge e : edgeGenes.values())
            if(e.fromNode == nodeFrom && e.toNode == nodeTo)
                return false;

        //it does not already exist, check if it has been mutated before, if so, use same ID
        int id = cache.getMutatedEdge(nodeFrom, nodeTo);
        Edge e;
        if(id >= 0)
            e = new Edge(id, nodeFrom, nodeTo, 1.0f);
        else {
            e = new Edge(cache.nextEdgeID(), nodeFrom, nodeTo, 1.0f);
            cache.addMutatedEdge(e.id, nodeFrom, nodeTo);
        }

        edgeGenes.put(e.id, e);
        return true;
    }


    /**
     * Create a new node along the specified edge. This will create a new node and connect it to the input and output
     * nodes of the edge, and then disable the edge. Thus making the change as minimal as possible.
     *
     * @param cache Cached information about the nodes and edges.
     * @param edge The edge along which to add a node.
     */
    private void addNode(DirectEncodingCache cache, int edge) {
        Edge oldEdge = edgeGenes.get(edge);
        oldEdge.enabled = false;

        int ids[] = cache.getMutatedNode(oldEdge.id);
        Node newNode;
        Edge edgeTo, edgeFrom;
        if(ids == null) {
            newNode = new Node(cache.nextNodeID(), NodeType.HIDDEN);
            edgeTo = new Edge(cache.nextEdgeID(), oldEdge.fromNode, newNode.id, oldEdge.weight);
            edgeFrom = new Edge(cache.nextEdgeID(), newNode.id, oldEdge.toNode, 1);
            cache.addMutatedNode(newNode.id, edgeTo.id, edgeFrom.id, oldEdge.id);
        } else {
            newNode = new Node(ids[0], NodeType.HIDDEN);
            edgeTo = new Edge(ids[1], oldEdge.fromNode, newNode.id, oldEdge.weight);
            edgeFrom = new Edge(ids[2], newNode.id, oldEdge.toNode, 1);
        }

        nodeGenes.put(newNode.id, newNode);
        edgeGenes.put(edgeTo.id, edgeTo);
        edgeGenes.put(edgeFrom.id, edgeFrom);
    }


    /**
     * Get the nth edge from edge genes.
     * @param index Index from edgeGenes to get the ID of.
     * @return ID of the edge at index.
     */
    private int edgeIndexToID(int index) {
        int i = 0;
        for(Edge e : edgeGenes.values())
            if(i++ == index) return e.id;
        return -1;
    }


    /**
     * Get the nth node from node genes.
     * @param index Index from nodeGenes to get the ID of.
     * @return ID of the node at index.
     */
    private int nodeIndexToID(int index) {
        int i = 0;
        for(Node n : nodeGenes.values())
            if(i++ == index) return n.id;
        return -1;
    }


    /**
     * Create a runnable ANN which is represented by the genome.
     *
     * @return The ANN represented by the genome.
     */
    @Override
    public NeuralNetwork getANN() {
        //TODO: use modified sigmoid function described in paper?
        //create lists of each type of node
        Map<Integer, Integer> nodes = new HashMap<>(nodeGenes.size()); //Map the node ID to the ANN Index
        int inputs = 0, outputs = 0, hidden = 0, count = 0;

        //count and add inputs, must add each type separately because there is no grantee of order or number
        for(Node n : nodeGenes.values()) {
            switch(n.nodeType) {
                case INPUT:
                    nodes.put(n.id, count++);
                    inputs++;
                    break;
                case OUTPUT:
                    outputs++;
                    break;
                case HIDDEN:
                    hidden++;
                    break;
            }
        }
        for(Node n : nodeGenes.values())
            if(n.nodeType == NodeType.OUTPUT)
                nodes.put(n.id, count++);
        for(Node n : nodeGenes.values())
            if(n.nodeType == NodeType.HIDDEN)
                nodes.put(n.id, count++);

        //construct a neural network now that we know the sizes
        NeuralNetwork.Builder net = new NeuralNetwork.Builder()
                .inputs(inputs)
                .outputs(outputs)
                .hidden(hidden);

        //set the activation functions (not needed presently)
        /*{
            int i = 0;
            for(Node n : inputs)
                net.setFunction(i++, n.function)
            for(Node n : outputs)
                net.setFunction(i++, n.function)
            for(Node n : hidden)
                net.setFunction(i++, n.function)
        }*/

        //TODO: make sure there are not duplicate edges making their way into the system
        //create the connections
        for(Edge e : edgeGenes.values())
            if(e.enabled)
                net.connect(nodes.get(e.fromNode), nodes.get(e.toNode), e.weight);

        return net.create();
    }
}
