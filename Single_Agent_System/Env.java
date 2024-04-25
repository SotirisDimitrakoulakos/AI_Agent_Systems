package example;

import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;


import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import java.lang.Math;

import java.util.HashMap;
import java.util.Map;

public class Env extends Environment {

    public static final int NbAgs = 2;
    public static final int GSize = 5;
    public static final int CUST  = 16; // customer mask
    public static final int MAX_ACTIONS = 150;
    public static int CURRENT_ACTIONS = 0;

    public static final Term    ns = Literal.parseLiteral("next(slot)");
    public static final Term    pg = Literal.parseLiteral("pick(cust)");
    public static final Term    dg = Literal.parseLiteral("drop(cust)");
    public static final Term    bg = Literal.parseLiteral("remove(cust)");
    public static final Literal gr1 = Literal.parseLiteral("customer(taxi)");
    public static final Literal gd = Literal.parseLiteral("customer(d)");

    public Random random = new Random(System.currentTimeMillis());
    static Logger logger = Logger.getLogger(Env.class.getName());

    private MarsModel model;
    private MarsView  view;

    @Override
    public void init(String[] args) {
        model = new MarsModel();
        view  = new MarsView(model);
        model.setView(view);
        updatePercepts();
    }

    @Override
    public boolean executeAction(String ag, Structure action) {
        if (CURRENT_ACTIONS==MAX_ACTIONS){
          //System.out.println("Reached maxed actions. Exiting...");
          System.exit(0);
        }
        logger.info(ag+" doing: "+ action);
        try {
            if (action.equals(ns)) {
                model.nextSlot();
                CURRENT_ACTIONS++;
            } else if (action.getFunctor().equals("move_towards")) {
                int x = (int)((NumberTerm)action.getTerm(0)).solve();
                int y = (int)((NumberTerm)action.getTerm(1)).solve();
                model.moveTowards(x,y);
                CURRENT_ACTIONS++;
            } else if (action.equals(pg)) {
                model.pickCust();
                CURRENT_ACTIONS++;
            } else if (action.equals(dg)) {
                model.dropCust();
                CURRENT_ACTIONS++;
            } else if (action.equals(bg)) {
                model.removeCust();
                CURRENT_ACTIONS++;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updatePercepts();

        try {
            if (random.nextBoolean() && model.countObjects(CUST) < 2) {
                model.generateRandomCustage();
            }
            Thread.sleep(700);
        } catch (Exception e) {}
        informAgsEnvironmentChanged();
        return true;
    }

    void updatePercepts() {
        clearPercepts();

        // 0 and 1 are agents' taxi and d masks respectively
        Location taxiLoc = model.getAgPos(0);
        Location dLoc = model.getAgPos(1);

        Literal posTaxi = Literal.parseLiteral("pos(taxi," + taxiLoc.x + "," + taxiLoc.y + ")");
        Literal posD = Literal.parseLiteral("pos(d," + dLoc.x + "," + dLoc.y + ")");

        addPercept(posTaxi);
        addPercept(posD);

        if (model.hasObject(CUST, taxiLoc)) {
            addPercept(gr1);
        }
        if (model.hasObject(CUST, dLoc)) {
            addPercept(gd);
        }
    }

    class MarsModel extends GridWorldModel {

        public static final int MErr = 2; // max error in pick cust
        int nerr; // number of tries of pick cust
        boolean taxiHasCust = false;
        Object[][] walls;
        int[][][] costs;
        //Route currentRoute;


        List<int[][]> customers = new ArrayList<>();
        Dictionary<String, String> routes;

        int[][] current_customer;
        Cosmos cosmos;


        //Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
            super(GSize, GSize, NbAgs);

            // initial location of agents
            try {
                routes = new Hashtable<>();
                routes.put("RY", "");
                routes.put("RG", "");
                routes.put("RB", "");
                routes.put("YG", "");
                routes.put("YB", "");
                routes.put("BG", "");
                setAgPos(0, 0, 0);

                int[] taxi_location = getRandomLocation(0);
                setAgPos(0, taxi_location[0], taxi_location[1]);

                Location dLoc = new Location(taxi_location[0], taxi_location[1]);
                setAgPos(1, dLoc);

                createWalls();

                //simulateWalls();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void generateRandomCustage() {
          int[] location = getRandomLocation(1);
          int[] destination = getRandomLocation(1);

          for (int[][] c : customers) {
              if (Arrays.equals(c[0], location)) {
                  return;
              }
          }
          //System.out.println("lllllll "+location[0]+" "+location[1]);
          add(CUST, location[0], location[1]);
          addCustomerToWorld(location, destination);
        }


        boolean checkForWall(int[] location, char direction) {
            for (Object[] wall : this.walls) {
                int[] wallLocation = (int[]) wall[0];
                char wallDirection = (char) wall[1];
                try {
                    // Use Arrays.equals to compare arrays
                    if (Arrays.equals(location, wallLocation) && direction == wallDirection) {
                        return true;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            return false;
        }


        void createWalls() {
            this.walls = new Object[][]{
                    {new int[]{0, 0}, 'E'},
                    {new int[]{0, 0}, 'A'},
                    {new int[]{0, 1}, 'A'},
                    {new int[]{0, 2}, 'A'},
                    {new int[]{0, 3}, 'A'},
                    {new int[]{0, 4}, 'A'},
                    {new int[]{0, 4}, 'K'},
                    {new int[]{1, 0}, 'E'},
                    {new int[]{1, 4}, 'K'},
                    {new int[]{2, 0}, 'E'},
                    {new int[]{2, 4}, 'K'},
                    {new int[]{3, 0}, 'E'},
                    {new int[]{3, 4}, 'K'},
                    {new int[]{4, 0}, 'E'},
                    {new int[]{4, 4}, 'K'},
                    {new int[]{4, 0}, 'D'},
                    {new int[]{4, 1}, 'D'},
                    {new int[]{4, 2}, 'D'},
                    {new int[]{4, 3}, 'D'},
                    {new int[]{4, 4}, 'D'},

                    {new int[]{0, 0}, 'D'},
                    {new int[]{0, 1}, 'D'},
                    {new int[]{1, 0}, 'A'},
                    {new int[]{1, 1}, 'A'},
                    {new int[]{1, 3}, 'D'},
                    {new int[]{1, 4}, 'D'},
                    {new int[]{2, 3}, 'A'},
                    {new int[]{2, 4}, 'A'},
                    {new int[]{2, 0}, 'D'},
                    {new int[]{2, 1}, 'D'},
                    {new int[]{3, 0}, 'A'},
                    {new int[]{3, 1}, 'A'}
            };


        }

        private void addCustomerToWorld(int[] location, int[] destination){
          this.customers.add(new int[][]{location, destination});
        }

        private int[] getRandomLocation(int type){
          Random random = new Random();
          int x,y;

          if (type==1){
            int randomLoc = random.nextInt(4);
            x=0;
            y=0;
            if (randomLoc == 1){
                x=3;
                y=0;
            }else if (randomLoc == 2){
                x=0;
                y=4;
            } else if (randomLoc == 3){
                x=4;
                y=4;
            }
          }else{
            x = random.nextInt(5);
            y = random.nextInt(5);
          }

          return new int[]{x,y};
        }

        int[] LocationToArray(Location loc){return new int[]{loc.x, loc.y};}

        int manhattan (int[] pos1, int[] pos2){
          //System.out.println("\n\n\n**************** Doing manhattan");
          //System.out.println("Point 1:"+pos1[0]+" "+pos1[1]);
          //System.out.println("Point 2:"+pos2[0]+" "+pos2[1]);
          //System.out.println("Result: "+(Math.abs(pos2[0]-pos1[0]) + Math.abs(pos2[1]-pos1[1])));
          //System.out.println("****************");
          return Math.abs(pos2[0]-pos1[0]) + Math.abs(pos2[1]-pos1[1]);
        }

        boolean getNextBestCustomer(){
          int[] current_taxi_location = LocationToArray(getAgPos(0));

          int min_distance = 25;
          int[][] min_customer = null;

          int d;

          for (int[][] c : customers) {
            if ((d = manhattan(current_taxi_location, c[0])) < min_distance) {
              min_distance = d;
              min_customer = c;
            } else if (d==min_distance){
              int d2 = manhattan(c[0], c[1]);
              int d3 = manhattan(min_customer[0], min_customer[1]);

              if (d2<d3){
                min_distance = d;
                min_customer = c;
              }
            }
          }

          if (min_customer==null) return false;
          this.current_customer = min_customer;

          Location a2 = getAgPos(1);
          a2.x = min_customer[1][0];
          a2.y = min_customer[1][1];
          setAgPos(1, a2);

          return true;
        }

        int[] performActionToArray(int[] ar, char c){
          //System.out.println("Performing "+c+" with array: "+ar[0]+ar[1]);
          int[] r = Arrays.copyOf(ar, ar.length);
          if (c=='E'){
            r[1]--;
          }else if (c=='K'){
            r[1]++;
          }else if (c=='A'){
            r[0]--;
          }else if (c=='D'){
            r[0]++;
          }
          return r;
        }

        class Cosmos{

          Object[][][] data = new Object[25][4][2];
          int[] destination;
          String current_route = "";
          boolean reverse_read = false;

          public Cosmos(int[] taxi_location, int[] destination){

            this.destination = destination;


            int distance;
            boolean blocked;
            int[] loc;
            for (int i=0; i<25; i++){
              int[] new_location = new int[]{i/5, i%5};

              for (int action=0; action<4; action++){
                loc = Arrays.copyOf(new_location, new_location.length);
                if (action==0) loc[1]--;
                else if (action==1) loc[1]++;
                else if (action==2) loc[0]--;
                else if (action==3) loc[0]++;

                //System.out.println("\n[*] For action "+action+", we have "+loc[0]+" "+loc[1]);

                if (loc[0]<0 || loc[1]<0 || loc[0]>4 || loc[1]>4){
                  //System.out.println("Found out of bounds: "+loc[0]+" "+loc[1]);
                  distance = 10000;
                  blocked=true;
                }else{
                  //System.out.println("Creating manhattan distance");
                  distance =manhattan(loc, destination);
                  blocked = false;
                }
                data[i][action] = new Object[]{distance, blocked};
              }

                if (i<5){
                  for (int j=0; j<5;j++){
                    this.data[i][2][0] = 10000;
                    this.data[i][2][1] = true;
                  }
                }

                if (i>19){
                  for (int j=0; j<5;j++){
                    this.data[i][3][0] = 10000;
                    this.data[i][3][1] = true;
                  }
                }

                if (i==0 || i==5 || i==10 || i==15 || i==20){
                  for (int j=0; j<5;j++){
                    this.data[i][0][0] = 10000;
                    this.data[i][0][1] = true;
                  }
                }

                if (i==4 || i==9 || i==14 || i==19 || i==24){
                  for (int j=0; j<5;j++){
                    this.data[i][1][0] = 10000;
                    this.data[i][1][1] = true;
                  }
                }

            }

            for (int i=0; i<25; i++){
              //System.out.println("\n* Cell at: "+i/5+" "+i%5);
              for (int j=0; j<4; j++){
                //System.out.println("Action "+j+": cost, blocked = "+ this.data[i][j][0]+" "+this.data[i][j][1]);
              }
            }
          }

          public Object[] getNextAction(int[] taxi_location){
            //System.out.println("\n\nInside getNextAction of cosmos");
            //System.out.println("\nDestination is: "+this.destination[0]+" "+this.destination[1]);
            Object[][] cell = this.data[5*taxi_location[0] + taxi_location[1]];

            int min = 10000;
            int min_index=-1;

            //System.out.println("\nGetting action of cell at "+taxi_location[0]+" "+taxi_location[1]);
            for (int i=0; i<4; i++){
              //System.out.println("Action "+i+": Found cost "+(int)cell[i][0] +" and blocked: "+(boolean)cell[i][1]);

              if ((int)cell[i][0]<min && (boolean)cell[i][1]==false){
                min = (int)cell[i][0];
                min_index = i;
              }
            }

            if (min_index==-1){
              return null;
            }

            // set use_cost_instead_of_action_block to true if for each action increase the cost to do it again or go back
            // set use_cost_instead_of_action_block to false to remove the used action from the available ones
            //the inc_weight_factor is the weight to be added when an action is repeated. used when use_cost_instead_of_action_block is set to true
            boolean use_cost_instead_of_action_block = true;
            int inc_weight_factor = 10;

            if (use_cost_instead_of_action_block) cell[min_index][0] = ((int) cell[min_index][0]) + 3;
            else cell[min_index][1]=true;

            int op_move = min_index+1;
            if (op_move==2) op_move = 0;
            if (op_move==4) op_move = 2;

            //System.out.println("Index: "+op_move);



            if (min_index==0){
              Object[][] new_cell = this.data[5*taxi_location[0] + taxi_location[1]-1];

              if (use_cost_instead_of_action_block) new_cell[op_move][0] = ((int) new_cell[op_move][0]) + inc_weight_factor;
              else new_cell[op_move][1]=true;

              return new Object[]{'E', new int[]{taxi_location[0], taxi_location[1]-1}};

           } else if (min_index==1){
             Object[][] new_cell = this.data[5*taxi_location[0] + taxi_location[1]+1];

             if (use_cost_instead_of_action_block) new_cell[op_move][0] = ((int) new_cell[op_move][0]) + inc_weight_factor;
             else new_cell[op_move][1]=true;

             return new Object[]{'K', new int[]{taxi_location[0], taxi_location[1]+1}};

           } else if (min_index==2){
             Object[][] new_cell = this.data[5*(taxi_location[0]-1) + taxi_location[1]];

             if (use_cost_instead_of_action_block) new_cell[op_move][0] = ((int) new_cell[op_move][0]) + inc_weight_factor;
             else new_cell[op_move][1]=true;

             return new Object[]{'A', new int[]{taxi_location[0]-1, taxi_location[1]}};

           } else{
             Object[][] new_cell = this.data[5*(taxi_location[0]+1) + taxi_location[1]];

             if (use_cost_instead_of_action_block) new_cell[op_move][0] = ((int) new_cell[op_move][0]) + inc_weight_factor;
             else new_cell[op_move][1]=true;

             return new Object[]{'D', new int[]{taxi_location[0]+1, taxi_location[1]}};
           }

          }

        }

        private char locationToLetter(int x, int y){
            if (x==0 && y==0){
              return 'Y';
            }else if (x==3 && y==0){
                return 'B';
            }else if (x==0 && y==4){
                return 'R';
            } else if (x==4 && y==4){
                return 'G';
            }

            return '.';
        }

        void printCustomers(){
          //System.out.println("\n");
          for (int[][] c: customers){
            //System.out.println("Customer: "+c[0][0]+" "+c[0][1]+" "+c[1][0]+" "+c[1][1]);
          }
          //System.out.println("\n");
        }

        private int actionToNumber(char action){
          if (action=='E') return 0;
          else if (action=='K') return 1;
          else if (action=='A') return 2;
          else if (action=='D') return 3;
          else return -1;
        }

        void nextSlot() throws Exception {
            Location taxi;
            String route_name="";
            if (current_customer==null) {

              //System.out.println("customer is null");
              boolean r = getNextBestCustomer();
              if (!r) return;
              taxi = getAgPos(0);
              this.cosmos = new Cosmos(new int[]{taxi.x, taxi.y}, current_customer[0]);

              taxi = getAgPos(0);
              char start = locationToLetter(taxi.x, taxi.y);
              char destination = locationToLetter(current_customer[0][0], current_customer[0][1]);
              if (start!='.' && destination!='.'){
                route_name = start < destination ? String.valueOf(start) + destination : String.valueOf(destination) + start;
                if (this.routes.get(route_name)!=null){
                  this.cosmos.current_route = this.routes.get(route_name);
                }
              }
            }

            if (this.cosmos==null){
              System.out.println("Cosmos is null");
              System.out.println(current_customer);
              taxi = getAgPos(0);
              this.cosmos = new Cosmos(new int[]{taxi.x, taxi.y}, current_customer[0]);
            }

            taxi = getAgPos(0);
            int[] old_location = new int[]{taxi.x, taxi.y};

            char move='.';
            int[] new_location=null;


            if (this.cosmos.current_route.length()>0){
              if (this.cosmos.current_route.charAt(this.cosmos.current_route.length()-1) == '.'){ //complete route, so read from
                move = this.cosmos.current_route.charAt(this.cosmos.current_route.length()-1);
                new_location = performActionToArray(old_location, move);
              }
            }

            if (new_location == null){
              Object[] result = cosmos.getNextAction(new int[]{taxi.x, taxi.y});
              move = (char)result[0];
              new_location = (int[])result[1];
            }


            System.out.println("Got customer "+current_customer[0][0]+" "+current_customer[0][1]);

            //System.out.println("Doing best move");

            System.out.println("Move: "+move);

            System.out.println("New location: "+new_location[0]+" "+new_location[1]);

            taxi = new Location(new_location[0], new_location[1]);

            setAgPos(0, taxi);
            this.cosmos.current_route+=move;
            System.out.println("\n[Next Slot]: Dictionary now is: "+this.cosmos.current_route);

            boolean passed_throught_wall = checkForWall(old_location, move);
            if (passed_throught_wall){
              this.cosmos.data[5*old_location[0]+old_location[1]][actionToNumber(move)][1] = true;
              this.cosmos.data[5*old_location[0]+old_location[1]][actionToNumber(move)][0] = 10000;
              if (move=='A'){
                this.cosmos.data[5*old_location[0] -5 +old_location[1]][3][1] = true;
                this.cosmos.data[5*old_location[0] -5 +old_location[1]][3][0] = 10000;
              }else if (move=='D'){
                this.cosmos.data[5*old_location[0] +5 +old_location[1]][2][1] = true;
                this.cosmos.data[5*old_location[0] +5 +old_location[1]][2][0] = 10000;
              }
              //System.out.println("\n\nHit wall on: "+taxi.x+" "+taxi.y);
              setAgPos(0,new Location(old_location[0], old_location[1]));
              this.cosmos.current_route = this.cosmos.current_route.substring(0, this.cosmos.current_route.length() - 1);
              System.out.println("\n[Next Slot]: Removed move from Dictionary. Dictionary now is: "+this.cosmos.current_route);


              //System.out.println("\n\nWent back on: "+getAgPos(0).x+" "+getAgPos(0).y);
              //System.out.println("\nAgent 1 - destination is on: "+getAgPos(1).x+" "+getAgPos(1).y);

            }

            if (taxi.x == current_customer[0][0] && taxi.y==current_customer[0][1]) {
              //System.out.println("In the same location so i must leave");
              printCustomers();

              for (int[][] customer: customers){
                if (Arrays.equals(customer, current_customer)){
                  //System.out.println("^^^^^^^^^^^^^^^^ Removed current customer");
                  customers.remove(customer);

                  if (this.routes.get(route_name)==null){
                    this.routes.put(route_name, this.cosmos.current_route);
                  }
                  this.cosmos = null;
                  break;
                }
            }
            //System.out.println("Finished removing customer");
          }
        }

        void moveTowards(int x, int y) throws Exception {
            Location taxi = getAgPos(0);
            int[] old_location = new int[]{taxi.x, taxi.y};

            if (this.cosmos == null){
              this.cosmos = new Cosmos(new int[]{taxi.x, taxi.y}, current_customer[1]);
            }

            char move='.';
            int[] new_location = null;

            if (this.cosmos.current_route.length()>0){
              if (this.cosmos.current_route.charAt(this.cosmos.current_route.length()-1) == '.'){ //complete route, so read from
                move = this.cosmos.current_route.charAt(this.cosmos.current_route.length()-1);
                new_location = performActionToArray(old_location, move);
              }
            }

            if (this.cosmos.current_route.length()>0){
              if (this.cosmos.current_route.charAt(this.cosmos.current_route.length()-1) == '.'){ //complete route, so read from
                move = this.cosmos.current_route.charAt(this.cosmos.current_route.length()-1);
                new_location = performActionToArray(old_location, move);
              }

            }

            if (new_location==null){
              Object[] result = cosmos.getNextAction(new int[]{taxi.x, taxi.y});
              move = (char)result[0];
              new_location = (int[])result[1];
            }

            /*{ Object[] result = cosmos.getNextAction(new int[]{taxi.x, taxi.y});
              if (result==null){
                this.cosmos = new Cosmos(new int[]{taxi.x, taxi.y}, current_customer[1]);
                result = cosmos.getNextAction(new int[]{taxi.x, taxi.y});
                move = (char)result[0];
                new_location = (int[])result[1];
              }
            }*/


            //System.out.println("Move: "+move);

            //System.out.println("New location: "+new_location[0]+" "+new_location[1]);

            taxi = new Location(new_location[0], new_location[1]);

            setAgPos(0, taxi);
            this.cosmos.current_route+=move;
            System.out.println("\n[Move Forward]: Dictionary now is: "+this.cosmos.current_route);

            boolean passed_throught_wall = checkForWall(old_location, move);
            if (passed_throught_wall){
              this.cosmos.data[5*old_location[0]+old_location[1]][actionToNumber(move)][1] = true;
              this.cosmos.data[5*old_location[0]+old_location[1]][actionToNumber(move)][0] = 10000;
              if (move=='A'){
                this.cosmos.data[5*old_location[0] -5 +old_location[1]][3][1] = true;
                this.cosmos.data[5*old_location[0] -5 +old_location[1]][3][0] = 10000;
              }else if (move=='D'){
                this.cosmos.data[5*old_location[0] +5 +old_location[1]][2][1] = true;
                this.cosmos.data[5*old_location[0] +5 +old_location[1]][2][0] = 10000;
              }

             //System.out.println("\n\nHit wall on: "+taxi.x+" "+taxi.y);
             setAgPos(0,new Location(old_location[0], old_location[1]));
             this.cosmos.current_route = this.cosmos.current_route.substring(0, this.cosmos.current_route.length() - 1);
             System.out.println("\n[Move Forward]: Removed move from Dictionary. Dictionary now is: "+this.cosmos.current_route);


             //System.out.println("\n\nWent back on: "+getAgPos(0).x+" "+getAgPos(0).y);
             //System.out.println("\nAgent 1 - destination is on: "+getAgPos(1).x+" "+getAgPos(1).y);

           }

            if (taxi.x == getAgPos(1).x && taxi.y==getAgPos(1).y){
              current_customer=null;
            }


        }

        void pickCust() {
            if (model.hasObject(CUST, getAgPos(0))) {
                // sometimes the "picking" action doesn't work but never more than MErr times
                if (random.nextBoolean() || nerr == MErr) {
                    remove(CUST, getAgPos(0));
                    nerr = 0;
                    taxiHasCust = true;
                } else {
                    nerr++;
                }
            }
            Location taxi = getAgPos(0);
        }
        void dropCust() {
            if (taxiHasCust) {
                taxiHasCust = false;
                add(CUST, getAgPos(0));
                this.cosmos = null;
                this.current_customer = null;
            }
        }
        void removeCust() {
          if (current_customer == null){
              if (model.hasObject(CUST, getAgPos(1))) {
                  remove(CUST, getAgPos(1));
              }
            }
        }


    }

    class MarsView extends GridWorldView {
      MarsModel model;

        public MarsView(MarsModel model) {
            super(model, "Mars World", 600);
            this.model = model;
            defaultFont = new Font("Arial", Font.BOLD, 18);
            setVisible(true);
            repaint();
        }

        @Override
        public void draw(Graphics g, int x, int y, int object) {
            switch (object) {
            case Env.CUST:
                drawCust(g, x, y);
                break;
            }
        }

        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {

            String label = "D";
            c = Color.blue;
            if (id == 0) {
                label = "R"+(id+1);
                c = Color.yellow;
                if (((MarsModel)model).taxiHasCust) {
                    label += " - G";
                    c = Color.orange;
                }
            }
            super.drawAgent(g, x, y, c, -1);
            if (id == 0) {
                g.setColor(Color.black);
            } else {
                g.setColor(Color.white);
            }
            super.drawString(g, x, y, defaultFont, label);
            repaint();
        }

        public void drawCust(Graphics g, int x, int y) {
            super.drawObstacle(g, x, y);
            g.setColor(Color.white);
            drawString(g, x, y, defaultFont, "G");
        }

    }

  }
