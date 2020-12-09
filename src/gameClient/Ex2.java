package gameClient;

import Server.Game_Server_Ex2;
import api.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Ex2 implements Runnable {
    private static MyFrame _win;
    private static Arena _ar;
    private static HashMap<Integer, Integer> busy = new HashMap<>();
    private static String lg;
    private static long dt;

    public static void main(String[] a) {
        Thread client = new Thread(new Ex2());
        client.start();
    }

    @Override
    public void run() {
        int scenario_num = 23;
        game_service game = Game_Server_Ex2.getServer(scenario_num); // you have [0,23] games
        //	int id = 999;
        //	game.login(id);
        String g = game.getGraph();
        String pks = game.getPokemons();

        try {
            File f = new File("my_graph");
            FileWriter fileWriter = new FileWriter(f);
            fileWriter.write(g);
            fileWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        dw_graph_algorithms temp = new DWGraph_Algo();
        temp.load("my_graph");
        directed_weighted_graph gg = temp.getGraph();
        init(game);


        //Start game

        game.startGame();

        _win.setTitle("Ex2 - OOP: " + game.toString());
        int ind = 0;
        dt = 120;

        while (game.isRunning()) {
            if (dt == 50)
                dt = 90;
            else if (dt == 70)
                dt = 100;
            else if (game.timeToEnd() <= 10000)
                dt = 85;
            else if (game.timeToEnd() <= 20000)
                dt = 90;
            else if (game.timeToEnd() <= 30000)
                dt = 96;
            else if (game.timeToEnd() <= 40000)
                dt = 100;
            _win.setTimeToEnd(game.timeToEnd() / 10);
            _win.setTitle("Ex2 - OOP: " + game.toString());
            moveAgents(game, gg);
            try {
                if (ind % 1 == 0) {
                    _win.repaint();
                }
                Thread.sleep(dt);
                ind++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String res = game.toString();

        System.out.println(res);
        System.exit(0);
    }

    /**
     * Moves each of the agents along the edge,
     * in case the agent is on a node the next destination (next edge) is chosen (randomly).
     *
     * @param game
     * @param gg
     * @param
     */
    private void moveAgents(game_service game, directed_weighted_graph gg) {
        lg = game.move();
        List<CL_Agent> log = Arena.getAgents(lg, gg);
        _ar.setAgents(log);
        //ArrayList<OOP_Point3D> rs = new ArrayList<OOP_Point3D>();
        String fs = game.getPokemons();
        List<CL_Pokemon> ffs = Arena.json2Pokemons(fs);
        _ar.setPokemons(ffs);
        for (int i = 0; i < log.size(); i++) {
            CL_Agent ag = log.get(i);
            int id = ag.getID();
            int dest = ag.getNextNode();
            int src = ag.getSrcNode();
            double v = ag.getValue();
            if (dest == -1) {
                if (busy.containsKey(id)) {
                    busy.remove(id);
                }
                dest = nextNode(gg, src, game, id);
                busy.put(id, dest);
                game.chooseNextEdge(ag.getID(), dest);
                System.out.println("Agent: " + id + ", val: " + v + "   turned to node: " + dest);
                if (game.timeToEnd() < 40000) {
                    for (edge_data j : gg.getE(src)) {
                        if (j.getWeight() < 0.4) {
                            dt = 50;
                            break;

                        } else if (j.getWeight() < 0.5) {
                            dt = 70;
                        }
                    }
                }
            }
        }
    }


    /**
     * a very simple random walk implementation!
     *
     * @param g
     * @param src
     * @return
     */
    private int nextNode(directed_weighted_graph g, int src, game_service game, int id) {
        int ans = -1;
        List<CL_Pokemon> pkList = _ar.json2Pokemons(game.getPokemons());
        dw_graph_algorithms gA = new DWGraph_Algo();
        gA.init(g);
        int pokDest;
        double min = Double.POSITIVE_INFINITY;
        int minDest = src;
        int minSrc = src;
        double path;
        if (busy.containsKey(id)) minSrc = busy.get(id);
        else {
            for (int i = 0; i < pkList.size(); i++) {
                _ar.updateEdge(pkList.get(i), g);
                if (pkList.get(i).get_edge() != null) {
                    int thisPokSRC = pkList.get(i).get_edge().getSrc();
                    if (thisPokSRC == src) {
                        return pkList.get(i).get_edge().getDest();
                    }

                    if (!busy.containsValue(thisPokSRC)) {
                        pokDest = pkList.get(i).get_edge().getDest();
                        path = gA.shortestPathDist(src, pokDest);
                        if (path < min) {
                            min = path;
                            minSrc = thisPokSRC;
                            minDest = pokDest;
                        }
                    }
                }


            }
            if (minSrc == src)
                return nextNode(g, src);
        }


        List<node_data> finalList;
        if (minSrc == src) {
            finalList = gA.shortestPath(src, minDest);
        } else {
            finalList = gA.shortestPath(src, minSrc);
        }
        if (finalList.size() == 1)
            ans = minSrc;
        else ans = finalList.get(1).getKey();
        return ans;
    }

    private static int nextNode(directed_weighted_graph g, int src) {
        int ans = -1;
        Collection<edge_data> ee = g.getE(src);
        Iterator<edge_data> itr = ee.iterator();
        int s = ee.size();
        int r = (int) (Math.random() * s);
        int i = 0;
        while (i < r) {
            itr.next();
            i++;
        }
        ans = itr.next().getDest();
        return ans;
    }

    private void init(game_service game) {
        String g = game.getGraph();
        String fs = game.getPokemons();
        directed_weighted_graph gg = game.getJava_Graph_Not_to_be_used();
        //gg.init(g);
        _ar = new Arena();
        _ar.setGraph(gg);
        _ar.setPokemons(Arena.json2Pokemons(fs));
        _win = new MyFrame("test Ex2");
        _win.setSize(1000, 700);
        _win.update(_ar);


        _win.show();
        String info = game.toString();
        JSONObject line;
        try {
            line = new JSONObject(info);
            JSONObject ttt = line.getJSONObject("GameServer");
            int rs = ttt.getInt("agents");
            System.out.println(info);
            System.out.println(game.getPokemons());
            int src_node = 0;  // arbitrary node, you should start at one of the pokemon
            ArrayList<CL_Pokemon> cl_fs = Arena.json2Pokemons(game.getPokemons());
            for (int a = 0; a < cl_fs.size(); a++) {
                Arena.updateEdge(cl_fs.get(a), gg);
            }
            for (int a = 0; a < rs; a++) {
                int ind = a % cl_fs.size();
                CL_Pokemon c = cl_fs.get(ind);
                int nn = c.get_edge().getDest();
                if (c.getType() < 0) {
                    nn = c.get_edge().getSrc();
                }

                game.addAgent(nn);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
